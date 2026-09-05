package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.system.domain.ExpenseBill;
import com.ruoyi.system.domain.FeeRuleVersion;
import com.ruoyi.system.domain.MappingRule;
import com.ruoyi.system.domain.bo.FeeRulePublishBo;
import com.ruoyi.system.domain.vo.FeeRuleGrayscaleCompareVo;
import com.ruoyi.system.domain.vo.FeeRuleVersionVo;
import com.ruoyi.system.domain.vo.MappingRuleVo;
import com.ruoyi.system.mapper.ExpenseBillMapper;
import com.ruoyi.system.mapper.FeeRuleVersionMapper;
import com.ruoyi.system.mapper.MappingRuleMapper;
import com.ruoyi.system.service.IFeeRuleVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 规则引擎版本快照Service实现（T7.4 / FEAT_RULE_GRAYSCALE）
 * 灰度发布 + 命中率比对 + 版本快照（锁定预算用当时快照，规则迭代不腐蚀历史）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeRuleVersionServiceImpl implements IFeeRuleVersionService {

    /** 命中率比对样本上限 */
    private static final int SAMPLE_LIMIT = 200;
    /** 语义推荐返回上限 */
    private static final int RECOMMEND_LIMIT = 5;

    private final FeeRuleVersionMapper versionMapper;
    private final MappingRuleMapper mappingRuleMapper;
    private final ExpenseBillMapper billMapper;

    @Override
    public List<FeeRuleVersionVo> listVersions() {
        List<FeeRuleVersionVo> list = versionMapper.selectVoList(Wrappers.<FeeRuleVersion>lambdaQuery()
            .orderByDesc(FeeRuleVersion::getVersionNo));
        list.forEach(vo -> vo.setRuleCount(countRules(vo.getRuleSnapshot())));
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publish(FeeRulePublishBo bo) {
        List<MappingRule> rules = mappingRuleMapper.selectList(Wrappers.<MappingRule>lambdaQuery()
            .eq(MappingRule::getStatus, "ENABLED")
            .orderByDesc(MappingRule::getPriority));
        if (rules.isEmpty()) {
            throw new ServiceException("当前无启用的映射规则，无法发布版本");
        }
        Integer maxVersion = versionMapper.selectList(Wrappers.lambdaQuery()).stream()
            .map(FeeRuleVersion::getVersionNo).max(Integer::compareTo).orElse(0);
        boolean full = bo.getGrayscalePct().compareTo(new BigDecimal("100")) >= 0;

        FeeRuleVersion version = new FeeRuleVersion();
        version.setVersionNo(maxVersion + 1);
        version.setRuleSnapshot(JsonUtils.toJsonString(rules));
        version.setGrayscalePct(bo.getGrayscalePct());
        version.setGrayscaleStatus(full ? "FULL" : "GRAYSCALE");
        version.setPublishBy(LoginHelper.getUserId());
        version.setPublishAt(new Date());
        versionMapper.insert(version);
        // 直接全量发布时，旧 FULL 版本降级为 SUPERSEDED
        if (full) {
            markPreviousFullSuperseded(version.getId());
        }
        log.info("规则版本发布成功：versionNo={}, 灰度={}%, 规则数={}",
            version.getVersionNo(), bo.getGrayscalePct(), rules.size());
        return version.getId();
    }

    @Override
    public FeeRuleGrayscaleCompareVo compareHitRate(Long versionId) {
        FeeRuleVersion version = versionMapper.selectById(versionId);
        if (version == null) {
            throw new ServiceException("规则版本不存在");
        }
        // 近期已审批单据样本（带物资小类才可参与匹配）
        List<ExpenseBill> samples = billMapper.selectList(Wrappers.<ExpenseBill>lambdaQuery()
                .eq(ExpenseBill::getStatus, "APPROVED")
                .isNotNull(ExpenseBill::getItemCategory)
                .orderByDesc(ExpenseBill::getCreateTime)
                .last("LIMIT " + SAMPLE_LIMIT));
        // 灰度分组：按单据ID哈希取模（与线上灰度分流一致）
        int pct = version.getGrayscalePct() == null ? 100 : version.getGrayscalePct().intValue();
        List<ExpenseBill> grayscaleGroup = samples.stream()
            .filter(b -> Math.abs(b.getId().hashCode() % 100) < pct)
            .collect(Collectors.toList());

        List<MappingRuleVo> newRules = parseSnapshot(version.getRuleSnapshot());
        FeeRuleGrayscaleCompareVo vo = new FeeRuleGrayscaleCompareVo();
        vo.setVersionId(versionId);
        vo.setVersionNo(version.getVersionNo());
        vo.setSampleCount(samples.size());
        vo.setGrayscaleSampleCount(grayscaleGroup.size());
        vo.setNewHitRate(hitRate(grayscaleGroup, newRules));
        // 上一版本命中率（同一灰度分组口径）
        FeeRuleVersion previous = selectPreviousVersion(version.getVersionNo());
        if (previous != null) {
            BigDecimal oldRate = hitRate(grayscaleGroup, parseSnapshot(previous.getRuleSnapshot()));
            vo.setOldHitRate(oldRate);
            if (vo.getNewHitRate() != null && oldRate != null) {
                vo.setHitRateDiff(vo.getNewHitRate().subtract(oldRate));
            }
        }
        // 回写灰度期命中率
        FeeRuleVersion update = new FeeRuleVersion();
        update.setId(versionId);
        update.setHitRate(vo.getNewHitRate());
        versionMapper.updateById(update);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean fullRelease(Long versionId) {
        FeeRuleVersion version = versionMapper.selectById(versionId);
        if (version == null) {
            throw new ServiceException("规则版本不存在");
        }
        if ("FULL".equals(version.getGrayscaleStatus())) {
            throw new ServiceException("该版本已是全量发布状态");
        }
        if ("ROLLBACK".equals(version.getGrayscaleStatus())) {
            throw new ServiceException("已回滚的版本不能全量发布");
        }
        FeeRuleVersion update = new FeeRuleVersion();
        update.setId(versionId);
        update.setGrayscaleStatus("FULL");
        update.setGrayscalePct(new BigDecimal("100"));
        markPreviousFullSuperseded(versionId);
        return versionMapper.updateById(update) > 0;
    }

    @Override
    public Boolean rollback(Long versionId) {
        FeeRuleVersion version = versionMapper.selectById(versionId);
        if (version == null) {
            throw new ServiceException("规则版本不存在");
        }
        FeeRuleVersion update = new FeeRuleVersion();
        update.setId(versionId);
        update.setGrayscaleStatus("ROLLBACK");
        return versionMapper.updateById(update) > 0;
    }

    @Override
    public List<MappingRuleVo> recommendSubject(String text) {
        if (StrUtil.isBlank(text)) {
            return new ArrayList<>();
        }
        // 轻量语义推荐：对规则各字段做关键词相似度打分（AI 模型接入后可替换此处实现）
        List<MappingRule> rules = mappingRuleMapper.selectList(Wrappers.<MappingRule>lambdaQuery()
            .eq(MappingRule::getStatus, "ENABLED"));
        Set<String> tokens = tokenize(text);
        Map<Long, Integer> scores = new HashMap<>();
        for (MappingRule rule : rules) {
            int score = 0;
            score += scoreTokens(tokens, rule.getItemCategory());
            score += scoreTokens(tokens, rule.getPurposeTag());
            score += scoreTokens(tokens, rule.getBudgetSubject());
            score += scoreTokens(tokens, rule.getAccountingClass());
            if (score > 0) {
                scores.put(rule.getId(), score);
            }
        }
        return rules.stream()
            .filter(r -> scores.containsKey(r.getId()))
            .sorted(Comparator.<MappingRule>comparingInt(r -> scores.get(r.getId())).reversed()
                .thenComparing(r -> r.getPriority() == null ? 0 : r.getPriority(), Comparator.reverseOrder()))
            .limit(RECOMMEND_LIMIT)
            .map(r -> cn.hutool.core.bean.BeanUtil.copyProperties(r, MappingRuleVo.class))
            .collect(Collectors.toList());
    }

    @Override
    public Long currentActiveVersionId() {
        FeeRuleVersion full = versionMapper.selectOne(Wrappers.<FeeRuleVersion>lambdaQuery()
            .eq(FeeRuleVersion::getGrayscaleStatus, "FULL")
            .orderByDesc(FeeRuleVersion::getVersionNo)
            .last("LIMIT 1"));
        if (full != null) {
            return full.getId();
        }
        FeeRuleVersion grayscale = versionMapper.selectOne(Wrappers.<FeeRuleVersion>lambdaQuery()
            .eq(FeeRuleVersion::getGrayscaleStatus, "GRAYSCALE")
            .orderByDesc(FeeRuleVersion::getVersionNo)
            .last("LIMIT 1"));
        return grayscale == null ? null : grayscale.getId();
    }

    /**
     * 新 FULL 版本生效时，旧 FULL 版本标记为 SUPERSEDED（历史保留不删除）
     */
    private void markPreviousFullSuperseded(Long excludeId) {
        List<FeeRuleVersion> oldFulls = versionMapper.selectList(Wrappers.<FeeRuleVersion>lambdaQuery()
            .eq(FeeRuleVersion::getGrayscaleStatus, "FULL")
            .ne(FeeRuleVersion::getId, excludeId));
        for (FeeRuleVersion old : oldFulls) {
            FeeRuleVersion update = new FeeRuleVersion();
            update.setId(old.getId());
            update.setGrayscaleStatus("SUPERSEDED");
            versionMapper.updateById(update);
        }
    }

    private FeeRuleVersion selectPreviousVersion(Integer versionNo) {
        return versionMapper.selectOne(Wrappers.<FeeRuleVersion>lambdaQuery()
            .lt(FeeRuleVersion::getVersionNo, versionNo)
            .orderByDesc(FeeRuleVersion::getVersionNo)
            .last("LIMIT 1"));
    }

    /**
     * 命中率：样本单据中被规则集匹配到的比例
     */
    private BigDecimal hitRate(List<ExpenseBill> samples, List<MappingRuleVo> rules) {
        if (samples.isEmpty()) {
            return null;
        }
        long hits = samples.stream().filter(b -> matches(b, rules)).count();
        return new BigDecimal(hits)
            .divide(new BigDecimal(samples.size()), 4, RoundingMode.HALF_UP);
    }

    /**
     * 模拟规则匹配（与线上匹配口径一致：物资小类 + 用途标签）
     */
    private boolean matches(ExpenseBill bill, List<MappingRuleVo> rules) {
        for (MappingRuleVo rule : rules) {
            if (!StrUtil.equals(rule.getItemCategory(), bill.getItemCategory())) {
                continue;
            }
            if (StrUtil.isBlank(rule.getPurposeTag()) || StrUtil.equals(rule.getPurposeTag(), bill.getPurposeTag())) {
                return true;
            }
        }
        return false;
    }

    private List<MappingRuleVo> parseSnapshot(String snapshot) {
        if (StrUtil.isBlank(snapshot)) {
            return new ArrayList<>();
        }
        List<MappingRuleVo> rules = JsonUtils.parseArray(snapshot, MappingRuleVo.class);
        return rules == null ? new ArrayList<>() : rules;
    }

    private Integer countRules(String snapshot) {
        return parseSnapshot(snapshot).size();
    }

    /**
     * 分词：按非字母数字切分 + 中文二元组
     */
    private Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        for (String word : text.toLowerCase().split("[^a-z0-9\\u4e00-\\u9fa5]+")) {
            if (StrUtil.isBlank(word)) {
                continue;
            }
            tokens.add(word);
            // 中文二元组
            if (word.matches(".*[\\u4e00-\\u9fa5].*")) {
                for (int i = 0; i < word.length() - 1; i++) {
                    tokens.add(word.substring(i, i + 2));
                }
            }
        }
        return tokens;
    }

    private int scoreTokens(Set<String> tokens, String field) {
        if (StrUtil.isBlank(field)) {
            return 0;
        }
        String lower = field.toLowerCase();
        int score = 0;
        for (String token : tokens) {
            if (lower.contains(token)) {
                score++;
            }
        }
        return score;
    }
}
