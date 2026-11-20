package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.VoiceComplianceHit;
import com.ruoyi.system.domain.VoiceComplianceRule;
import com.ruoyi.system.domain.VoiceRecordingPolicy;
import com.ruoyi.system.domain.bo.VoiceComplianceHitBo;
import com.ruoyi.system.domain.bo.VoiceComplianceRuleBo;
import com.ruoyi.system.domain.bo.VoiceRecordingPolicyBo;
import com.ruoyi.system.domain.compliance.SensitiveWordCheckResult;
import com.ruoyi.system.domain.vo.VoiceComplianceHitVo;
import com.ruoyi.system.domain.vo.VoiceComplianceRuleVo;
import com.ruoyi.system.domain.vo.VoiceRecordingPolicyVo;
import com.ruoyi.system.mapper.VoiceComplianceHitMapper;
import com.ruoyi.system.mapper.VoiceComplianceRuleMapper;
import com.ruoyi.system.mapper.VoiceRecordingPolicyMapper;
import com.ruoyi.system.service.ISensitiveWordEngine;
import com.ruoyi.system.service.IVoiceAsrProvider;
import com.ruoyi.system.service.IVoiceComplianceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 语音合规服务实现（T18.1~T18.4，小微版 M18）
 * <p>★fail-fast：入口判空 + DB 返回判空 + 业务校验。
 * <p>★全局留存上限存 Redis（key=voice:global_max_retention_days），默认 365 天。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceComplianceServiceImpl implements IVoiceComplianceService {

    private final VoiceComplianceRuleMapper ruleMapper;
    private final VoiceRecordingPolicyMapper policyMapper;
    private final VoiceComplianceHitMapper hitMapper;
    /** M18 统一敏感词引擎（T18.6 ASR 转写文本收口） */
    private final ISensitiveWordEngine sensitiveWordEngine;
    /** M20 T18 增强：语音 ASR 转写服务提供者（V1 桩实现） */
    private final IVoiceAsrProvider voiceAsrProvider;

    /** 合法敏感词策略白名单 */
    private static final Set<String> VALID_OVERRIDE_STRATEGIES = new HashSet<>(
        Arrays.asList("INHERIT", "OVERRIDE", "MERGE"));

    /** 全局留存上限 Redis key */
    private static final String GLOBAL_RETENTION_KEY = "voice:global_max_retention_days";

    /** 默认全局留存上限天数 */
    private static final int DEFAULT_GLOBAL_MAX_RETENTION = 365;

    /** 默认最大语音时长秒数（3 分钟，与 voice_compliance_rule.max_voice_duration 列默认值一致） */
    private static final int DEFAULT_MAX_VOICE_DURATION = 180;

    /** 最大语音时长上限秒数（60 分钟，与租户配置页前端上限一致） */
    private static final int MAX_VOICE_DURATION_LIMIT = 3600;

    // ===== T18.3 租户管理员合规规则管理 =====

    @Override
    public TableDataInfo<VoiceComplianceRuleVo> queryRulePage(VoiceComplianceRuleBo bo, PageQuery pageQuery) {
        // ★fail-fast
        if (bo == null) {
            bo = new VoiceComplianceRuleBo();
        }
        if (pageQuery == null) {
            throw new ServiceException("分页参数不能为空");
        }
        LambdaQueryWrapper<VoiceComplianceRule> wrapper = Wrappers.<VoiceComplianceRule>lambdaQuery()
            .eq(StrUtil.isNotBlank(bo.getActivityType()), VoiceComplianceRule::getActivityType, bo.getActivityType())
            .eq(bo.getEnabled() != null, VoiceComplianceRule::getEnabled, bo.getEnabled())
            .eq(StrUtil.isNotBlank(bo.getStatus()), VoiceComplianceRule::getStatus, bo.getStatus())
            .orderByDesc(VoiceComplianceRule::getCreateTime);
        Page<VoiceComplianceRuleVo> page = ruleMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public VoiceComplianceRuleVo queryRuleById(Long id) {
        // ★fail-fast
        if (id == null) {
            throw new ServiceException("规则ID不能为空");
        }
        VoiceComplianceRuleVo vo = ruleMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("合规规则不存在");
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertRule(VoiceComplianceRuleBo bo) {
        // ★fail-fast
        if (bo == null) {
            throw new ServiceException("规则参数不能为空");
        }
        if (StrUtil.isBlank(bo.getActivityType())) {
            throw new ServiceException("活动类型不能为空");
        }
        if (StrUtil.isNotBlank(bo.getSensitiveWordOverride())
            && !VALID_OVERRIDE_STRATEGIES.contains(bo.getSensitiveWordOverride())) {
            throw new ServiceException("敏感词策略不合法：" + bo.getSensitiveWordOverride());
        }
        // ★UNIQUE 防重：同租户同活动类型仅一条规则
        Long existCount = ruleMapper.selectCount(Wrappers.<VoiceComplianceRule>lambdaQuery()
            .eq(VoiceComplianceRule::getActivityType, bo.getActivityType()));
        if (existCount > 0) {
            throw new ServiceException("该活动类型已存在合规规则，请勿重复添加");
        }
        VoiceComplianceRule entity = new VoiceComplianceRule();
        entity.setActivityType(bo.getActivityType());
        entity.setEnabled(bo.getEnabled() != null ? bo.getEnabled() : false);
        entity.setSensitiveWordOverride(StrUtil.isNotBlank(bo.getSensitiveWordOverride()) ? bo.getSensitiveWordOverride() : "INHERIT");
        entity.setTranscriptionEnabled(bo.getTranscriptionEnabled() != null ? bo.getTranscriptionEnabled() : false);
        // ★最大语音时长必须落库（M20_07 补列）：否则发布页读取回退默认 180 秒，租户配置不生效
        entity.setMaxVoiceDuration(normalizeMaxVoiceDuration(bo.getMaxVoiceDuration()));
        entity.setStatus(StrUtil.isNotBlank(bo.getStatus()) ? bo.getStatus() : "NORMAL");
        entity.setRemark(bo.getRemark());
        ruleMapper.insert(entity);
        log.info("[insertRule] activityType={}, enabled={}, maxVoiceDuration={}", bo.getActivityType(), entity.getEnabled(), entity.getMaxVoiceDuration());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRule(VoiceComplianceRuleBo bo) {
        // ★fail-fast
        if (bo == null || bo.getId() == null) {
            throw new ServiceException("规则参数不能为空");
        }
        VoiceComplianceRule exist = ruleMapper.selectById(bo.getId());
        if (exist == null) {
            throw new ServiceException("合规规则不存在");
        }
        if (StrUtil.isNotBlank(bo.getSensitiveWordOverride())
            && !VALID_OVERRIDE_STRATEGIES.contains(bo.getSensitiveWordOverride())) {
            throw new ServiceException("敏感词策略不合法：" + bo.getSensitiveWordOverride());
        }
        VoiceComplianceRule entity = new VoiceComplianceRule();
        entity.setId(bo.getId());
        if (bo.getEnabled() != null) {
            entity.setEnabled(bo.getEnabled());
        }
        if (StrUtil.isNotBlank(bo.getSensitiveWordOverride())) {
            entity.setSensitiveWordOverride(bo.getSensitiveWordOverride());
        }
        if (bo.getTranscriptionEnabled() != null) {
            entity.setTranscriptionEnabled(bo.getTranscriptionEnabled());
        }
        // ★最大语音时长必须落库：租户管理员配置的分钟数（前端转秒）在此持久化
        if (bo.getMaxVoiceDuration() != null) {
            entity.setMaxVoiceDuration(normalizeMaxVoiceDuration(bo.getMaxVoiceDuration()));
        }
        if (StrUtil.isNotBlank(bo.getStatus())) {
            entity.setStatus(bo.getStatus());
        }
        if (bo.getRemark() != null) {
            entity.setRemark(bo.getRemark());
        }
        log.info("[updateRule] id={}, maxVoiceDuration={}", bo.getId(), entity.getMaxVoiceDuration());
        return ruleMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRule(Long id) {
        // ★fail-fast
        if (id == null) {
            throw new ServiceException("规则ID不能为空");
        }
        VoiceComplianceRule exist = ruleMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("合规规则不存在");
        }
        log.info("[deleteRule] id={}, activityType={}", id, exist.getActivityType());
        return ruleMapper.deleteById(id);
    }

    // ===== T18.3 租户管理员录音策略管理 =====

    @Override
    public VoiceRecordingPolicyVo queryPolicy() {
        // ★每租户一条，selectOne 保护
        LambdaQueryWrapper<VoiceRecordingPolicy> wrapper = Wrappers.<VoiceRecordingPolicy>lambdaQuery()
            .last("LIMIT 1");
        VoiceRecordingPolicyVo vo = policyMapper.selectVoOne(wrapper);
        if (vo == null) {
            // 返回默认策略
            VoiceRecordingPolicyVo defaultVo = new VoiceRecordingPolicyVo();
            defaultVo.setMaxRetentionDays(30);
            return defaultVo;
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long savePolicy(VoiceRecordingPolicyBo bo) {
        // ★fail-fast
        if (bo == null) {
            throw new ServiceException("策略参数不能为空");
        }
        if (bo.getMaxRetentionDays() != null) {
            validateRetentionDays(bo.getMaxRetentionDays());
        }
        // upsert：查现有记录
        LambdaQueryWrapper<VoiceRecordingPolicy> wrapper = Wrappers.<VoiceRecordingPolicy>lambdaQuery()
            .last("LIMIT 1");
        VoiceRecordingPolicy exist = policyMapper.selectOne(wrapper);
        if (exist != null) {
            // 更新
            VoiceRecordingPolicy entity = new VoiceRecordingPolicy();
            entity.setId(exist.getId());
            if (bo.getMaxRetentionDays() != null) {
                entity.setMaxRetentionDays(bo.getMaxRetentionDays());
            }
            if (bo.getStoragePath() != null) {
                entity.setStoragePath(bo.getStoragePath());
            }
            entity.setRemark(bo.getRemark());
            policyMapper.updateById(entity);
            log.info("[savePolicy] UPDATE id={}, maxRetentionDays={}", exist.getId(), bo.getMaxRetentionDays());
            return exist.getId();
        } else {
            // 新增
            VoiceRecordingPolicy entity = new VoiceRecordingPolicy();
            entity.setMaxRetentionDays(bo.getMaxRetentionDays() != null ? bo.getMaxRetentionDays() : 30);
            entity.setStoragePath(bo.getStoragePath());
            entity.setRemark(bo.getRemark());
            policyMapper.insert(entity);
            log.info("[savePolicy] INSERT id={}, maxRetentionDays={}", entity.getId(), entity.getMaxRetentionDays());
            return entity.getId();
        }
    }

    // ===== T18.4 老板合规命中记录（只读） =====

    @Override
    public TableDataInfo<VoiceComplianceHitVo> queryHitPage(VoiceComplianceHitBo bo, PageQuery pageQuery) {
        // ★fail-fast
        if (bo == null) {
            bo = new VoiceComplianceHitBo();
        }
        if (pageQuery == null) {
            throw new ServiceException("分页参数不能为空");
        }
        LambdaQueryWrapper<VoiceComplianceHit> wrapper = Wrappers.<VoiceComplianceHit>lambdaQuery()
            .eq(StrUtil.isNotBlank(bo.getHitType()), VoiceComplianceHit::getHitType, bo.getHitType())
            .eq(StrUtil.isNotBlank(bo.getStatus()), VoiceComplianceHit::getStatus, bo.getStatus())
            .eq(bo.getRuleId() != null, VoiceComplianceHit::getRuleId, bo.getRuleId())
            .ge(bo.getHitTimeStart() != null, VoiceComplianceHit::getHitTime, bo.getHitTimeStart())
            .le(bo.getHitTimeEnd() != null, VoiceComplianceHit::getHitTime, bo.getHitTimeEnd())
            .orderByDesc(VoiceComplianceHit::getHitTime);
        Page<VoiceComplianceHitVo> page = hitMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    // ===== T18.2 Admin 平台合规管理 =====

    @Override
    public int getGlobalMaxRetentionDays() {
        // ★V1 简化实现：直接返回默认值；后续可接 Redis
        return DEFAULT_GLOBAL_MAX_RETENTION;
    }

    @Override
    public void setGlobalMaxRetentionDays(int days) {
        // ★fail-fast
        if (days <= 0) {
            throw new ServiceException("全局留存上限天数必须大于0");
        }
        if (days > 3650) {
            throw new ServiceException("全局留存上限天数不能超过3650天");
        }
        // ★V1 简化实现：仅日志记录；后续接 Redis 持久化
        log.info("[setGlobalMaxRetentionDays] days={}", days);
    }

    @Override
    public List<Map<String, Object>> complianceAlertDashboard() {
        // ★近 7 天命中趋势：按 hit_type 分组统计
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -7);
        Date sevenDaysAgo = cal.getTime();

        LambdaQueryWrapper<VoiceComplianceHit> wrapper = Wrappers.<VoiceComplianceHit>lambdaQuery()
            .ge(VoiceComplianceHit::getHitTime, sevenDaysAgo);
        List<VoiceComplianceHit> hits = hitMapper.selectList(wrapper);
        // ★DB 返回保护
        if (hits == null) {
            hits = Collections.emptyList();
        }
        // 按 hitType 分组统计
        Map<String, Long> typeCount = hits.stream()
            .collect(Collectors.groupingBy(VoiceComplianceHit::getHitType, Collectors.counting()));
        // 按天分组统计
        Map<String, Long> dayCount = hits.stream()
            .collect(Collectors.groupingBy(
                h -> h.getHitTime() != null ? h.getHitTime().toString().substring(0, 10) : "unknown",
                Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalHits7d", hits.size());
        summary.put("byType", typeCount);
        summary.put("byDay", dayCount);
        summary.put("pendingCount", hits.stream().filter(h -> "PENDING".equals(h.getStatus())).count());
        result.add(summary);
        return result;
    }

    // ===== T18.3 录音策略校验 =====

    @Override
    public void validateRetentionDays(int days) {
        int globalMax = getGlobalMaxRetentionDays();
        if (days > globalMax) {
            throw new ServiceException("留存天数不能超过全局上限 " + globalMax + " 天");
        }
    }

    // ===== M18 T18.6 语音 ASR 转写文本合规收口 =====

    @Override
    public SensitiveWordCheckResult checkTranscription(String asrText, Long tenantId, String recordId) {
        // ★fail-fast
        if (StrUtil.isBlank(asrText)) {
            return SensitiveWordCheckResult.pass(0L);
        }
        if (tenantId == null) {
            throw new ServiceException("租户ID不能为空");
        }
        // 委派统一敏感词引擎，inputChannel=voice_ocr，bizType=voice
        SensitiveWordCheckResult result = sensitiveWordEngine.check(asrText, tenantId, "voice_ocr", "voice", recordId);
        if (result != null && !result.isPass()) {
            log.warn("[checkTranscription] tenantId={}, recordId={} ASR 转写命中敏感词 → 打回重录", tenantId, recordId);
        }
        return result;
    }

    // ===== M20 T18 增强：语音上传后合规校验流程 =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SensitiveWordCheckResult checkVoiceCompliance(String voiceUrl, Long tenantId, Long activityId) {
        // ★fail-fast
        if (StrUtil.isBlank(voiceUrl)) {
            throw new ServiceException("语音URL不能为空");
        }
        if (tenantId == null) {
            throw new ServiceException("租户ID不能为空");
        }
        // activityId 可空（insert 场景尚未落库时传 null）

        long startTime = System.currentTimeMillis();

        // ① ASR 转写
        String asrText = voiceAsrProvider.transcribe(voiceUrl, tenantId, activityId);
        if (StrUtil.isBlank(asrText)) {
            // ★转写留痕关闭时，空转写是预期行为，不记录 NO_TRANSCRIPTION 命中
            VoiceComplianceRule rule = ruleMapper.selectOne(Wrappers.<VoiceComplianceRule>lambdaQuery().last("LIMIT 1"));
            if (rule != null && Boolean.FALSE.equals(rule.getTranscriptionEnabled())) {
                log.info("[checkVoiceCompliance] tenantId={}, activityId={} 转写留痕已关闭，跳过合规记录", tenantId, activityId);
                return SensitiveWordCheckResult.pass(System.currentTimeMillis() - startTime);
            }
            log.warn("[checkVoiceCompliance] tenantId={}, activityId={} ASR 转写结果为空，视为合规通过", tenantId, activityId);
            // 转写留痕开启但 ASR 失败，仍记录为 PASS，但标记为 NO_TRANSCRIPTION
            recordHit(tenantId, activityId, "NO_TRANSCRIPTION", "ASR 转写失败或无内容", null, "PASS", null);
            return SensitiveWordCheckResult.pass(System.currentTimeMillis() - startTime);
        }

        // ② 敏感词检测
        SensitiveWordCheckResult checkResult = sensitiveWordEngine.check(asrText, tenantId, "voice_ocr", "voice", String.valueOf(activityId));
        boolean isPass = checkResult != null && checkResult.isPass();
        String result = isPass ? "PASS" : "FAIL";
        String hitType = isPass ? null : "SENSITIVE_WORD";

        // ③ 写 voice_compliance_hit 记录（无论 PASS/FAIL 均记录）
        String contentSummary = StrUtil.sub(asrText, 0, 200); // 脱敏截取前 200 字
        recordHit(tenantId, activityId, hitType, contentSummary, null, result, null);

        long cost = System.currentTimeMillis() - startTime;
        log.info("[checkVoiceCompliance] tenantId={}, activityId={}, result={}, cost={}ms", tenantId, activityId, result, cost);

        // 如果违规，返回 FAIL 结果供调用方处理
        if (!isPass) {
            log.warn("[checkVoiceCompliance] tenantId={}, activityId={} 语音合规检测失败 → 打回重录", tenantId, activityId);
        }

        return checkResult;
    }

    /**
     * 最大语音时长归一化（秒）：null → 默认 180 秒；0 = 不限；上限 3600 秒（60 分钟）。
     * <p>★fail-fast：越界直接抛业务异常，避免脏配置流入发布页时长校验。
     */
    private int normalizeMaxVoiceDuration(Integer seconds) {
        if (seconds == null) {
            return DEFAULT_MAX_VOICE_DURATION;
        }
        if (seconds < 0 || seconds > MAX_VOICE_DURATION_LIMIT) {
            throw new ServiceException("最大语音时长不合法：须在 0~" + MAX_VOICE_DURATION_LIMIT + " 秒之间（0=不限）");
        }
        return seconds;
    }

    /**
     * 写入 voice_compliance_hit 记录。
     *
     * @param tenantId       租户ID
     * @param activityId     活动ID
     * @param hitType        命中类型（PASS 时为 null）
     * @param contentSummary 内容摘要
     * @param ruleId         关联规则ID（可空）
     * @param result         合规结果：PASS/FAIL
     * @param remark         备注（可空）
     */
    private void recordHit(Long tenantId, Long activityId, String hitType, String contentSummary,
                           Long ruleId, String result, String remark) {
        VoiceComplianceHit hit = new VoiceComplianceHit();
        hit.setTenantId(tenantId);
        hit.setHitTime(new Date());
        hit.setHitType(hitType != null ? hitType : "PASS"); // PASS 时 hitType 设为 PASS
        hit.setResult(result);
        hit.setContentSummary(contentSummary);
        hit.setRuleId(ruleId);
        hit.setActivityId(activityId);
        hit.setStatus("PENDING"); // 默认待处理
        hit.setRemark(remark);
        hitMapper.insert(hit);
        log.debug("[recordHit] tenantId={}, activityId={}, result={}, hitType={}", tenantId, activityId, result, hitType);
    }
}
