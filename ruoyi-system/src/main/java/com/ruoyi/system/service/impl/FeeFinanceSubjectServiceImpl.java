package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.BudgetSubject;
import com.ruoyi.system.domain.FeeFinanceSubject;
import com.ruoyi.system.domain.vo.FeeFinanceSubjectImportVo;
import com.ruoyi.system.domain.vo.FeeFinanceSubjectVo;
import com.ruoyi.system.mapper.BudgetSubjectMapper;
import com.ruoyi.system.mapper.FeeFinanceSubjectMapper;
import com.ruoyi.system.service.IFeeFinanceSubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 财务科目导入服务实现（T5.16）
 * <p>
 * 导入 → 关键字自动匹配建立映射A → 人工补完/纠偏。
 * 匹配规则：名称完全一致优先；否则取名称互相包含且重叠最长的预算科目。
 *
 * @author ruoyi
 * @date 2026-08-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeFinanceSubjectServiceImpl implements IFeeFinanceSubjectService {

    private final FeeFinanceSubjectMapper baseMapper;
    private final BudgetSubjectMapper subjectMapper;

    @Override
    public TableDataInfo<FeeFinanceSubjectVo> queryPageList(String status, String sourceSystem,
                                                            String keyword, PageQuery pageQuery) {
        Page<FeeFinanceSubjectVo> page = baseMapper.selectVoPage(pageQuery.build(),
            Wrappers.<FeeFinanceSubject>lambdaQuery()
                .eq(StrUtil.isNotBlank(status), FeeFinanceSubject::getStatus, status)
                .eq(StrUtil.isNotBlank(sourceSystem), FeeFinanceSubject::getSourceSystem, sourceSystem)
                .and(StrUtil.isNotBlank(keyword), w -> w
                    .like(FeeFinanceSubject::getSubjectCode, keyword)
                    .or()
                    .like(FeeFinanceSubject::getSubjectName, keyword))
                .orderByAsc(FeeFinanceSubject::getSubjectLevel)
                .orderByAsc(FeeFinanceSubject::getSubjectCode));
        enrichMappedName(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importSubjects(List<FeeFinanceSubjectImportVo> list, String sourceSystem) {
        if (list == null || list.isEmpty()) {
            throw new ServiceException("导入数据不能为空");
        }
        String batchNo = "FS-" + System.currentTimeMillis();
        String source = StrUtil.isNotBlank(sourceSystem) ? sourceSystem : "GENERAL";

        // 同租户同编码幂等覆盖（uk_tenant_code）
        Map<String, FeeFinanceSubject> existMap = new HashMap<>();
        for (FeeFinanceSubject s : baseMapper.selectList(Wrappers.<FeeFinanceSubject>lambdaQuery())) {
            existMap.put(s.getSubjectCode(), s);
        }

        int insertNum = 0;
        int updateNum = 0;
        Set<String> seen = new HashSet<>();
        for (FeeFinanceSubjectImportVo vo : list) {
            String code = StrUtil.trim(vo.getSubjectCode());
            if (StrUtil.isBlank(code) || StrUtil.isBlank(vo.getSubjectName())) {
                continue;
            }
            if (!seen.add(code)) {
                continue; // 文件内重复编码只取首行
            }
            FeeFinanceSubject exist = existMap.get(code);
            if (exist != null) {
                exist.setSubjectName(StrUtil.trim(vo.getSubjectName()));
                exist.setParentCode(vo.getParentCode());
                exist.setSubjectLevel(vo.getSubjectLevel());
                exist.setImportBatchNo(batchNo);
                exist.setSourceSystem(source);
                baseMapper.updateById(exist);
                updateNum++;
            } else {
                FeeFinanceSubject add = new FeeFinanceSubject();
                add.setImportBatchNo(batchNo);
                add.setSourceSystem(source);
                add.setSubjectCode(code);
                add.setSubjectName(StrUtil.trim(vo.getSubjectName()));
                add.setParentCode(vo.getParentCode());
                add.setSubjectLevel(vo.getSubjectLevel());
                add.setStatus("UNMAPPED");
                baseMapper.insert(add);
                insertNum++;
            }
        }

        // 导入后自动执行关键字匹配建立映射A，未命中的留给人工补完
        int matched = autoMatch();
        String message = "导入完成：新增 " + insertNum + " 条，更新 " + updateNum
            + " 条（批次号 " + batchNo + "），自动匹配成功 " + matched + " 条";
        log.info("财务科目导入: {}", message);
        return message;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoMatch() {
        List<FeeFinanceSubject> unmapped = baseMapper.selectList(
            Wrappers.<FeeFinanceSubject>lambdaQuery()
                .eq(FeeFinanceSubject::getStatus, "UNMAPPED"));
        if (unmapped.isEmpty()) {
            return 0;
        }
        // 启用的叶子预算科目作为匹配候选
        List<BudgetSubject> candidates = subjectMapper.selectList(Wrappers.<BudgetSubject>lambdaQuery()
            .eq(BudgetSubject::getIsActive, 1)
            .eq(BudgetSubject::getIsLeaf, 1));
        if (candidates.isEmpty()) {
            return 0;
        }
        int matched = 0;
        for (FeeFinanceSubject fs : unmapped) {
            BudgetSubject hit = findCandidate(fs.getSubjectName(), candidates);
            if (hit != null) {
                fs.setMappedSubjectCode(hit.getSubjectCode());
                fs.setMatchType("AUTO");
                fs.setStatus("MAPPED");
                baseMapper.updateById(fs);
                matched++;
            }
        }
        log.info("财务科目自动匹配: 待匹配 {} 条，成功 {} 条", unmapped.size(), matched);
        return matched;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualMap(Long id, String mappedSubjectCode) {
        FeeFinanceSubject fs = baseMapper.selectById(id);
        if (fs == null) {
            throw new ServiceException("财务科目不存在");
        }
        if (StrUtil.isBlank(mappedSubjectCode)) {
            throw new ServiceException("请选择要映射的预算科目");
        }
        BudgetSubject target = subjectMapper.selectOne(Wrappers.<BudgetSubject>lambdaQuery()
            .eq(BudgetSubject::getSubjectCode, mappedSubjectCode)
            .last("LIMIT 1"));
        if (target == null) {
            throw new ServiceException("预算科目不存在：" + mappedSubjectCode);
        }
        fs.setMappedSubjectCode(target.getSubjectCode());
        fs.setMatchType("MANUAL");
        fs.setStatus("MAPPED");
        baseMapper.updateById(fs);
        log.info("财务科目人工映射: {} -> {}", fs.getSubjectCode(), target.getSubjectCode());
    }

    @Override
    public Boolean ignore(Long id) {
        FeeFinanceSubject fs = baseMapper.selectById(id);
        if (fs == null) {
            throw new ServiceException("财务科目不存在");
        }
        fs.setStatus("IGNORED");
        fs.setMappedSubjectCode(null);
        fs.setMatchType(null);
        return baseMapper.updateById(fs) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByIds(Collection<Long> ids) {
        return baseMapper.delete(Wrappers.<FeeFinanceSubject>lambdaQuery()
            .in(FeeFinanceSubject::getId, ids)) > 0;
    }

    /**
     * 关键字匹配：名称完全一致优先，否则取名称互相包含且重叠最长的候选科目
     */
    private BudgetSubject findCandidate(String financeName, List<BudgetSubject> candidates) {
        if (StrUtil.isBlank(financeName)) {
            return null;
        }
        String name = StrUtil.trim(financeName);
        BudgetSubject best = null;
        int bestLen = 0;
        for (BudgetSubject bs : candidates) {
            String bsName = bs.getSubjectName();
            if (StrUtil.isBlank(bsName)) {
                continue;
            }
            if (name.equals(bsName)) {
                return bs; // 完全一致直接命中
            }
            if (name.contains(bsName) || bsName.contains(name)) {
                int overlap = Math.min(name.length(), bsName.length());
                if (overlap > bestLen) {
                    bestLen = overlap;
                    best = bs;
                }
            }
        }
        return best;
    }

    /**
     * 列表填充映射的预算科目名称
     */
    private void enrichMappedName(List<FeeFinanceSubjectVo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<String> codes = new HashSet<>();
        for (FeeFinanceSubjectVo vo : records) {
            if (StrUtil.isNotBlank(vo.getMappedSubjectCode())) {
                codes.add(vo.getMappedSubjectCode());
            }
        }
        if (codes.isEmpty()) {
            return;
        }
        List<BudgetSubject> subjects = subjectMapper.selectList(Wrappers.<BudgetSubject>lambdaQuery()
            .in(BudgetSubject::getSubjectCode, codes));
        Map<String, String> nameMap = new HashMap<>();
        for (BudgetSubject s : subjects) {
            nameMap.put(s.getSubjectCode(), s.getSubjectName());
        }
        for (FeeFinanceSubjectVo vo : records) {
            vo.setMappedSubjectName(nameMap.get(vo.getMappedSubjectCode()));
        }
    }
}
