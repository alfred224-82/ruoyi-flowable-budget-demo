package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.system.domain.BudgetPreparation;
import com.ruoyi.system.domain.BudgetPreparationDetail;
import com.ruoyi.system.domain.FeeOrg;
import com.ruoyi.system.domain.FeeOrgBudgetVersion;
import com.ruoyi.system.domain.vo.BudgetPreparationVo;
import com.ruoyi.system.domain.vo.FeeOrgBudgetVersionVo;
import com.ruoyi.system.domain.vo.FeeOrgConsolidatedVo;
import com.ruoyi.system.mapper.BudgetPreparationDetailMapper;
import com.ruoyi.system.mapper.BudgetPreparationMapper;
import com.ruoyi.system.mapper.FeeOrgBudgetVersionMapper;
import com.ruoyi.system.mapper.FeeOrgMapper;
import com.ruoyi.system.service.IFeeOrgBudgetService;
import com.ruoyi.system.service.IFeeOrgService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 组织预算Service实现（T7.1 / FEAT_MULTI_ORG + FEAT_ORG_SNAPSHOT）
 * 按组织分别编制与锁定快照、集团合并视角；组织间预算互不干扰
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeOrgBudgetServiceImpl implements IFeeOrgBudgetService {

    private final BudgetPreparationMapper preparationMapper;
    private final BudgetPreparationDetailMapper detailMapper;
    private final FeeOrgMapper orgMapper;
    private final FeeOrgBudgetVersionMapper versionMapper;
    private final IFeeOrgService orgService;

    @Override
    public List<BudgetPreparationVo> listOrgPreparations(Long orgId, String period) {
        if (orgId == null) {
            throw new ServiceException("请选择组织");
        }
        checkOrgVisible(orgId);
        return preparationMapper.selectVoList(Wrappers.<BudgetPreparation>lambdaQuery()
            .eq(BudgetPreparation::getOrgId, orgId)
            .eq(StrUtil.isNotBlank(period), BudgetPreparation::getBudgetPeriod, period)
            .orderByDesc(BudgetPreparation::getBudgetYear)
            .orderByDesc(BudgetPreparation::getBudgetMonth));
    }

    @Override
    public List<FeeOrgConsolidatedVo> consolidated(String period) {
        List<Long> visible = orgService.visibleOrgIds();
        List<BudgetPreparation> list = preparationMapper.selectList(Wrappers.<BudgetPreparation>lambdaQuery()
            .eq(StrUtil.isNotBlank(period), BudgetPreparation::getBudgetPeriod, period));
        // 数据范围过滤（本组织+下级组织）
        if (visible != null) {
            list = list.stream().filter(p -> p.getOrgId() != null && visible.contains(p.getOrgId()))
                .collect(Collectors.toList());
        }
        Map<Long, FeeOrg> orgMap = orgMapper.selectList(Wrappers.lambdaQuery()).stream()
            .collect(Collectors.toMap(FeeOrg::getId, o -> o));
        // 各组织最新锁定快照版本号
        Map<Long, Integer> latestVersion = new HashMap<>();
        versionMapper.selectList(Wrappers.<FeeOrgBudgetVersion>lambdaQuery()
                .eq(FeeOrgBudgetVersion::getStatus, "LOCKED"))
            .forEach(v -> latestVersion.merge(v.getOrgId(), v.getVersionNo(), Math::max));

        Map<Long, List<BudgetPreparation>> byOrg = list.stream()
            .collect(Collectors.groupingBy(p -> p.getOrgId() == null ? 0L : p.getOrgId()));
        List<FeeOrgConsolidatedVo> result = new ArrayList<>();
        for (Map.Entry<Long, List<BudgetPreparation>> entry : byOrg.entrySet()) {
            Long orgId = entry.getKey() == 0L ? null : entry.getKey();
            FeeOrgConsolidatedVo vo = new FeeOrgConsolidatedVo();
            vo.setOrgId(orgId);
            FeeOrg org = orgId == null ? null : orgMap.get(orgId);
            vo.setOrgName(org == null ? "未挂组织（公司级）" : org.getOrgName());
            vo.setSheetCount(entry.getValue().size());
            vo.setTotalBudget(entry.getValue().stream()
                .map(p -> zero(p.getTotalBudget())).reduce(BigDecimal.ZERO, BigDecimal::add));
            vo.setTotalActual(entry.getValue().stream()
                .map(p -> zero(p.getTotalActual())).reduce(BigDecimal.ZERO, BigDecimal::add));
            vo.setApprovedCount((int) entry.getValue().stream()
                .filter(p -> "Approved".equals(p.getStatus())).count());
            vo.setLatestSnapshotVersion(orgId == null ? null : latestVersion.get(orgId));
            vo.setTotalRow(false);
            result.add(vo);
        }
        result.sort(Comparator.comparing(v -> v.getOrgId() == null ? Long.MAX_VALUE : v.getOrgId()));
        // 集团合计行
        FeeOrgConsolidatedVo total = new FeeOrgConsolidatedVo();
        total.setOrgName("集团合计");
        total.setSheetCount(result.stream().mapToInt(FeeOrgConsolidatedVo::getSheetCount).sum());
        total.setTotalBudget(result.stream().map(FeeOrgConsolidatedVo::getTotalBudget)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        total.setTotalActual(result.stream().map(FeeOrgConsolidatedVo::getTotalActual)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        total.setApprovedCount(result.stream().mapToInt(FeeOrgConsolidatedVo::getApprovedCount).sum());
        total.setTotalRow(true);
        result.add(total);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FeeOrgBudgetVersionVo lockSnapshot(Long orgId, String period) {
        if (orgId == null || StrUtil.isBlank(period)) {
            throw new ServiceException("请选择组织与预算期间");
        }
        checkOrgVisible(orgId);
        FeeOrg org = orgMapper.selectById(orgId);
        if (org == null) {
            throw new ServiceException("组织不存在");
        }
        List<BudgetPreparation> sheets = preparationMapper.selectList(Wrappers.<BudgetPreparation>lambdaQuery()
            .eq(BudgetPreparation::getOrgId, orgId)
            .eq(BudgetPreparation::getBudgetPeriod, period));
        if (sheets.isEmpty()) {
            throw new ServiceException("该组织在当前期间无预算编制，无法锁定快照");
        }

        // 组装快照（锁定后不可变）
        List<Map<String, Object>> sheetSnapshots = new ArrayList<>();
        for (BudgetPreparation sheet : sheets) {
            Map<String, Object> sheetMap = new LinkedHashMap<>();
            sheetMap.put("sheetNo", sheet.getSheetNo());
            sheetMap.put("status", sheet.getStatus());
            sheetMap.put("totalBudget", sheet.getTotalBudget());
            sheetMap.put("totalActual", sheet.getTotalActual());
            List<BudgetPreparationDetail> details = detailMapper.selectList(
                Wrappers.<BudgetPreparationDetail>lambdaQuery().eq(BudgetPreparationDetail::getSheetId, sheet.getId()));
            List<Map<String, Object>> detailMaps = new ArrayList<>();
            for (BudgetPreparationDetail detail : details) {
                Map<String, Object> detailMap = new LinkedHashMap<>();
                detailMap.put("subjectCode", detail.getSubjectCode());
                detailMap.put("subjectName", detail.getSubjectName());
                detailMap.put("subjectType", detail.getSubjectType());
                detailMap.put("budgetAmount", detail.getBudgetAmount());
                detailMaps.add(detailMap);
            }
            sheetMap.put("details", detailMaps);
            sheetSnapshots.add(sheetMap);
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("orgId", orgId);
        snapshot.put("orgName", org.getOrgName());
        snapshot.put("period", period);
        snapshot.put("lockedAt", new Date());
        snapshot.put("sheets", sheetSnapshots);

        // 版本号+1，不覆盖历史版本（组织合并/拆分时新旧版本隔离）
        Integer maxVersion = versionMapper.selectList(Wrappers.<FeeOrgBudgetVersion>lambdaQuery()
                .eq(FeeOrgBudgetVersion::getOrgId, orgId)
                .eq(FeeOrgBudgetVersion::getPeriod, period))
            .stream().map(FeeOrgBudgetVersion::getVersionNo).max(Integer::compareTo).orElse(0);
        FeeOrgBudgetVersion version = new FeeOrgBudgetVersion();
        version.setOrgId(orgId);
        version.setPeriod(period);
        version.setVersionNo(maxVersion + 1);
        version.setStatus("LOCKED");
        version.setSnapshotData(JsonUtils.toJsonString(snapshot));
        version.setLockedBy(LoginHelper.getUserId());
        version.setLockedAt(new Date());
        versionMapper.insert(version);
        log.info("组织预算快照锁定成功：orgId={}, period={}, versionNo={}", orgId, period, version.getVersionNo());
        return toVo(version, org.getOrgName());
    }

    @Override
    public List<FeeOrgBudgetVersionVo> listVersions(Long orgId) {
        if (orgId != null) {
            checkOrgVisible(orgId);
        }
        List<FeeOrgBudgetVersionVo> list = versionMapper.selectVoList(Wrappers.<FeeOrgBudgetVersion>lambdaQuery()
            .eq(orgId != null, FeeOrgBudgetVersion::getOrgId, orgId)
            .orderByDesc(FeeOrgBudgetVersion::getOrgId)
            .orderByDesc(FeeOrgBudgetVersion::getVersionNo));
        fillOrgName(list);
        return list;
    }

    @Override
    public FeeOrgBudgetVersionVo getVersion(Long id) {
        FeeOrgBudgetVersionVo vo = versionMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("快照版本不存在");
        }
        checkOrgVisible(vo.getOrgId());
        fillOrgName(java.util.Collections.singletonList(vo));
        return vo;
    }

    /**
     * 组织数据范围校验（本组织+下级组织；admin/未挂组织全量）
     */
    private void checkOrgVisible(Long orgId) {
        List<Long> visible = orgService.visibleOrgIds();
        if (visible != null && !visible.contains(orgId)) {
            throw new ServiceException("无权查看该组织的预算数据");
        }
    }

    private void fillOrgName(List<FeeOrgBudgetVersionVo> list) {
        List<Long> orgIds = list.stream().map(FeeOrgBudgetVersionVo::getOrgId)
            .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (orgIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = orgMapper.selectBatchIds(orgIds).stream()
            .collect(Collectors.toMap(FeeOrg::getId, FeeOrg::getOrgName, (a, b) -> a));
        list.forEach(vo -> vo.setOrgName(nameMap.get(vo.getOrgId())));
    }

    private FeeOrgBudgetVersionVo toVo(FeeOrgBudgetVersion version, String orgName) {
        FeeOrgBudgetVersionVo vo = new FeeOrgBudgetVersionVo();
        vo.setId(version.getId());
        vo.setOrgId(version.getOrgId());
        vo.setOrgName(orgName);
        vo.setPeriod(version.getPeriod());
        vo.setVersionNo(version.getVersionNo());
        vo.setStatus(version.getStatus());
        vo.setSnapshotData(version.getSnapshotData());
        vo.setLockedBy(version.getLockedBy());
        vo.setLockedAt(version.getLockedAt());
        return vo;
    }

    private BigDecimal zero(BigDecimal value) {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO);
    }
}
