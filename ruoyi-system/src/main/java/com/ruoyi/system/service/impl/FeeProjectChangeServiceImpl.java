package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.system.domain.FeeProject;
import com.ruoyi.system.domain.FeeProjectChange;
import com.ruoyi.system.domain.FeeProjectSubject;
import com.ruoyi.system.domain.bo.FeeProjectChangeBo;
import com.ruoyi.system.domain.vo.FeeProjectChangeVo;
import com.ruoyi.system.mapper.FeeProjectChangeMapper;
import com.ruoyi.system.mapper.FeeProjectMapper;
import com.ruoyi.system.mapper.FeeProjectSubjectMapper;
import com.ruoyi.system.service.IFeeNotificationService;
import com.ruoyi.system.service.IFeeProjectChangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 合同变更Service实现（T5.5 / ADR-022）
 * 增量叠加不回溯：审批通过时合同额/大类预算增量叠加到项目，基线合同额不变
 * 分步生效：审批通过→待收款确认（财务确认收款后 CONFIRMED 闭环）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeProjectChangeServiceImpl implements IFeeProjectChangeService {

    private final FeeProjectChangeMapper changeMapper;
    private final FeeProjectMapper projectMapper;
    private final FeeProjectSubjectMapper subjectMapper;
    private final IFeeNotificationService notificationService;

    @Override
    public TableDataInfo<FeeProjectChangeVo> queryPageList(FeeProjectChangeBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<FeeProjectChange> wrapper = Wrappers.<FeeProjectChange>lambdaQuery()
            .eq(bo.getProjectId() != null, FeeProjectChange::getProjectId, bo.getProjectId())
            .eq(StrUtil.isNotBlank(bo.getChangeStatus()), FeeProjectChange::getChangeStatus, bo.getChangeStatus())
            .eq(StrUtil.isNotBlank(bo.getChangeType()), FeeProjectChange::getChangeType, bo.getChangeType())
            .orderByDesc(FeeProjectChange::getCreateTime);
        Page<FeeProjectChangeVo> page = changeMapper.selectVoPage(pageQuery.build(), wrapper);
        fillProjectName(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public FeeProjectChangeVo queryById(Long id) {
        FeeProjectChangeVo vo = changeMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("合同变更记录不存在");
        }
        fillProjectName(List.of(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertChange(FeeProjectChangeBo bo) {
        FeeProject project = projectMapper.selectById(bo.getProjectId());
        if (project == null) {
            throw new ServiceException("关联项目不存在");
        }
        if (!"ACTIVE".equals(project.getStatus())) {
            throw new ServiceException("仅进行中的项目可发起合同变更");
        }
        boolean hasContract = bo.getContractDelta() != null
            && bo.getContractDelta().compareTo(BigDecimal.ZERO) != 0;
        boolean hasBudget = MapUtil.isNotEmpty(bo.getBudgetDeltas());
        if (!hasContract && !hasBudget) {
            throw new ServiceException("合同变更必须包含合同额变更量或大类预算变更");
        }
        FeeProjectChange change = new FeeProjectChange();
        change.setProjectId(bo.getProjectId());
        change.setChangeType(StrUtil.isNotBlank(bo.getChangeType()) ? bo.getChangeType()
            : (hasContract && hasBudget ? "COMPREHENSIVE" : (hasContract ? "CONTRACT_INCREASE" : "BUDGET_INCREASE")));
        change.setChangeReason(bo.getChangeReason());
        change.setOldContractAmount(zero(project.getCurrentContractAmount()));
        change.setContractDelta(zero(bo.getContractDelta()));
        change.setNewContractAmount(change.getOldContractAmount().add(change.getContractDelta()));
        change.setBudgetDeltas(hasBudget ? JsonUtils.toJsonString(bo.getBudgetDeltas()) : null);
        change.setBasisUrls(bo.getBasisUrls());
        change.setChangeStatus("PENDING");
        change.setCreatedBy(LoginHelper.getUserId());
        changeMapper.insert(change);

        notifyBossNewChange(change, project);
        return change.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int approveChange(Long id) {
        FeeProjectChange change = changeMapper.selectById(id);
        if (change == null) {
            throw new ServiceException("合同变更记录不存在");
        }
        if (!"PENDING".equals(change.getChangeStatus())) {
            throw new ServiceException("仅待审批状态的变更单可审批");
        }
        FeeProject project = projectMapper.selectById(change.getProjectId());
        if (project == null) {
            throw new ServiceException("关联项目不存在");
        }

        // 1. 合同额增量叠加（基线 contractAmount 不回溯，仅叠加 currentContractAmount）
        FeeProject update = new FeeProject();
        update.setId(project.getId());
        if (change.getContractDelta() != null && change.getContractDelta().compareTo(BigDecimal.ZERO) != 0) {
            update.setCurrentContractAmount(zero(project.getCurrentContractAmount()).add(change.getContractDelta()));
            update.setChangeCount(zeroInt(project.getChangeCount()) + 1);
        }

        // 2. 大类预算增量叠加（按大类名称匹配项目科目，不存在则新增）
        BigDecimal budgetTotal = applyBudgetDeltas(change);
        if (budgetTotal.compareTo(BigDecimal.ZERO) != 0) {
            update.setTotalBudget(zero(project.getTotalBudget()).add(budgetTotal));
        }
        projectMapper.updateById(update);

        FeeProjectChange updateChange = new FeeProjectChange();
        updateChange.setId(id);
        updateChange.setChangeStatus("APPROVED");
        updateChange.setApprovedBy(LoginHelper.getUserId());
        updateChange.setApprovedAt(new Date());
        int rows = changeMapper.updateById(updateChange);

        // 3. 合同额增加 → 通知财务跟进收款确认（角色广播，T5.12）
        if (change.getContractDelta() != null && change.getContractDelta().compareTo(BigDecimal.ZERO) > 0) {
            try {
                notificationService.notifyRole("finance", "合同变更已审批，待收款确认",
                    "项目【" + project.getProjectName() + "】合同变更【合同额增加 " + change.getContractDelta()
                        + " 元】已审批通过，请跟进收款确认",
                    "APPROVAL", "PROJECT_CHANGE", id);
            } catch (Exception e) {
                log.warn("合同变更审批通知发送失败（不阻断主流程）: {}", e.getMessage());
            }
        }
        return rows;
    }

    @Override
    public int rejectChange(Long id, String reason) {
        FeeProjectChange change = changeMapper.selectById(id);
        if (change == null) {
            throw new ServiceException("合同变更记录不存在");
        }
        if (!"PENDING".equals(change.getChangeStatus())) {
            throw new ServiceException("仅待审批状态的变更单可驳回");
        }
        FeeProjectChange update = new FeeProjectChange();
        update.setId(id);
        update.setChangeStatus("REJECTED");
        update.setApprovedBy(LoginHelper.getUserId());
        update.setApprovedAt(new Date());
        update.setChangeReason(StrUtil.isNotBlank(reason)
            ? StrUtil.nullToEmpty(change.getChangeReason()) + " | 驳回：" + reason
            : change.getChangeReason());
        return changeMapper.updateById(update);
    }

    @Override
    public int confirmChange(Long id) {
        FeeProjectChange change = changeMapper.selectById(id);
        if (change == null) {
            throw new ServiceException("合同变更记录不存在");
        }
        if (!"APPROVED".equals(change.getChangeStatus()) && !"OVERDUE".equals(change.getChangeStatus())) {
            throw new ServiceException("仅已审批（待收款确认）状态的变更单可收款确认");
        }
        FeeProjectChange update = new FeeProjectChange();
        update.setId(id);
        update.setChangeStatus("CONFIRMED");
        update.setConfirmedAt(new Date());
        return changeMapper.updateById(update);
    }

    @Override
    public int scanOverdueWarnings() {
        // 待收款确认的变更单（APPROVED/已标记 OVERDUE 且未确认）
        List<FeeProjectChange> waiting = changeMapper.selectList(
            Wrappers.<FeeProjectChange>lambdaQuery()
                .in(FeeProjectChange::getChangeStatus, "APPROVED", "OVERDUE")
                .isNull(FeeProjectChange::getConfirmedAt)
                .isNotNull(FeeProjectChange::getApprovedAt));
        if (CollUtil.isEmpty(waiting)) {
            return 0;
        }
        List<Long> projectIds = waiting.stream().map(FeeProjectChange::getProjectId)
            .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, FeeProject> projectMap = CollUtil.isEmpty(projectIds) ? MapUtil.newHashMap()
            : projectMapper.selectBatchIds(projectIds).stream()
                .collect(Collectors.toMap(FeeProject::getId, Function.identity()));
        long now = System.currentTimeMillis();
        int hits = 0;
        for (FeeProjectChange change : waiting) {
            long days = (now - change.getApprovedAt().getTime()) / (24L * 3600 * 1000);
            FeeProject project = projectMap.get(change.getProjectId());
            String projectName = project == null ? String.valueOf(change.getProjectId()) : project.getProjectName();
            if (days >= 90) {
                // ≥90 天：状态转 OVERDUE（幂等），仅状态变更时通知一次（老板+财务）
                if (!"OVERDUE".equals(change.getChangeStatus())) {
                    FeeProjectChange update = new FeeProjectChange();
                    update.setId(change.getId());
                    update.setChangeStatus("OVERDUE");
                    changeMapper.updateById(update);
                    warnOverdue(change, projectName, "boss", "合同变更收款确认已逾期≥90天（高风险）");
                    warnOverdue(change, projectName, "finance", "合同变更收款确认已逾期≥90天，请尽快闭环");
                }
                hits++;
            } else if (days == 60) {
                // 60 天阶梯当天：升级通知老板 + 财务（防轰炸）
                warnOverdue(change, projectName, "boss", "合同变更收款确认已逾期60天，请关注");
                warnOverdue(change, projectName, "finance", "合同变更收款确认已逾期60天，请跟进收款");
                hits++;
            } else if (days == 30) {
                // 30 天阶梯当天：提醒财务（防轰炸）
                warnOverdue(change, projectName, "finance", "合同变更收款确认已逾期30天，请跟进收款");
                hits++;
            }
        }
        return hits;
    }

    /**
     * 发送逾期预警（角色广播，不阻断主流程）
     */
    private void warnOverdue(FeeProjectChange change, String projectName, String roleKey, String title) {
        try {
            notificationService.notifyRole(roleKey, title,
                "项目【" + projectName + "】合同变更【合同额变更量 " + change.getContractDelta()
                    + " 元】审批通过后收款确认已逾期，" + title,
                "WARNING", "PROJECT_CHANGE", change.getId());
        } catch (Exception e) {
            log.warn("合同变更逾期预警发送失败（不阻断主流程）: {}", e.getMessage());
        }
    }

    /**
     * 大类预算增量叠加：按大类名称匹配项目科目，存在则累加预算，不存在则新建科目
     *
     * @return 预算净变更合计
     */
    private BigDecimal applyBudgetDeltas(FeeProjectChange change) {
        if (StrUtil.isBlank(change.getBudgetDeltas())) {
            return BigDecimal.ZERO;
        }
        Map<String, BigDecimal> deltas = JsonUtils.parseMap(change.getBudgetDeltas()).entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey,
                e -> new BigDecimal(String.valueOf(e.getValue()))));
        if (deltas.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<FeeProjectSubject> subjects = subjectMapper.selectList(
            Wrappers.<FeeProjectSubject>lambdaQuery()
                .eq(FeeProjectSubject::getProjectId, change.getProjectId()));
        Map<String, FeeProjectSubject> subjectByName = subjects.stream()
            .filter(s -> StrUtil.isNotBlank(s.getCategoryName()))
            .collect(Collectors.toMap(FeeProjectSubject::getCategoryName, Function.identity(), (a, b) -> a));

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : deltas.entrySet()) {
            BigDecimal delta = zero(entry.getValue());
            if (delta.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            FeeProjectSubject subject = subjectByName.get(entry.getKey());
            if (subject != null) {
                FeeProjectSubject update = new FeeProjectSubject();
                update.setId(subject.getId());
                update.setBudgetAmount(zero(subject.getBudgetAmount()).add(delta));
                subjectMapper.updateById(update);
            } else {
                FeeProjectSubject add = new FeeProjectSubject();
                add.setProjectId(change.getProjectId());
                add.setCategoryName(entry.getKey());
                add.setSubjectName(entry.getKey());
                add.setBudgetAmount(delta);
                add.setUsedAmount(BigDecimal.ZERO);
                add.setIsActive(1);
                subjectMapper.insert(add);
            }
            total = total.add(delta);
        }
        return total;
    }

    /**
     * 批量填充项目名称
     */
    private void fillProjectName(List<FeeProjectChangeVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> projectIds = list.stream().map(FeeProjectChangeVo::getProjectId)
            .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (CollUtil.isEmpty(projectIds)) {
            return;
        }
        Map<Long, FeeProject> projectMap = projectMapper.selectBatchIds(projectIds).stream()
            .collect(Collectors.toMap(FeeProject::getId, Function.identity()));
        list.forEach(vo -> {
            FeeProject project = projectMap.get(vo.getProjectId());
            vo.setProjectName(project == null ? null : project.getProjectName());
        });
    }

    /**
     * 通知老板（角色 boss）有新变更单待审批
     */
    private void notifyBossNewChange(FeeProjectChange change, FeeProject project) {
        try {
            notificationService.notifyRole("boss", "新合同变更待审批",
                "项目【" + project.getProjectName() + "】发起合同变更，合同额变更量 "
                    + change.getContractDelta() + " 元，请及时审批",
                "APPROVAL", "PROJECT_CHANGE", change.getId());
        } catch (Exception e) {
            log.warn("合同变更待审批通知发送失败（不阻断主流程）: {}", e.getMessage());
        }
    }

    private BigDecimal zero(BigDecimal value) {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO);
    }

    private int zeroInt(Integer value) {
        return Objects.requireNonNullElse(value, 0);
    }
}
