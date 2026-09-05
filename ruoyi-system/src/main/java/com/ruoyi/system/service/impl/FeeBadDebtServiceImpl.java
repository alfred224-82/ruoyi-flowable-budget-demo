package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.FeeBadDebt;
import com.ruoyi.system.domain.FeeProject;
import com.ruoyi.system.domain.bo.FeeBadDebtBo;
import com.ruoyi.system.domain.vo.FeeBadDebtVo;
import com.ruoyi.system.mapper.FeeBadDebtMapper;
import com.ruoyi.system.mapper.FeeProjectMapper;
import com.ruoyi.system.service.IFeeBadDebtService;
import com.ruoyi.system.service.IFeeNotificationService;
import com.ruoyi.system.service.IFeeProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 烂账管理Service实现（T5.6）
 * 风险标记 + 坏账计提（累加项目坏账计提与支出）+ 关闭时项目状态 LOST
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeBadDebtServiceImpl implements IFeeBadDebtService {

    private final FeeBadDebtMapper badDebtMapper;
    private final FeeProjectMapper projectMapper;
    private final IFeeProjectService projectService;
    private final IFeeNotificationService notificationService;

    @Override
    public TableDataInfo<FeeBadDebtVo> queryPageList(FeeBadDebtBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<FeeBadDebt> wrapper = Wrappers.<FeeBadDebt>lambdaQuery()
            .eq(bo.getProjectId() != null, FeeBadDebt::getProjectId, bo.getProjectId())
            .eq(StrUtil.isNotBlank(bo.getType()), FeeBadDebt::getType, bo.getType())
            .eq(StrUtil.isNotBlank(bo.getStatus()), FeeBadDebt::getStatus, bo.getStatus())
            .orderByDesc(FeeBadDebt::getCreateTime);
        Page<FeeBadDebtVo> page = badDebtMapper.selectVoPage(pageQuery.build(), wrapper);
        fillProjectName(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public FeeBadDebtVo queryById(Long id) {
        FeeBadDebtVo vo = badDebtMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("烂账记录不存在");
        }
        fillProjectName(Collections.singletonList(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertBadDebt(FeeBadDebtBo bo) {
        FeeProject project = projectMapper.selectById(bo.getProjectId());
        if (project == null) {
            throw new ServiceException("关联项目不存在");
        }
        FeeBadDebt badDebt = new FeeBadDebt();
        badDebt.setProjectId(bo.getProjectId());
        badDebt.setReason(bo.getReason());
        badDebt.setType(StrUtil.isNotBlank(bo.getType()) ? bo.getType() : "OTHER");
        badDebt.setProposal(StrUtil.isNotBlank(bo.getProposal()) ? bo.getProposal() : "FOLLOW_UP");
        badDebt.setProvisionAmount(zero(bo.getProvisionAmount()));
        badDebt.setStatus("OPEN");
        badDebt.setMarkedBy(LoginHelper.getUserId());
        badDebt.setMarkedAt(new Date());
        badDebtMapper.insert(badDebt);

        // 带计提金额时同步计提坏账
        if (badDebt.getProvisionAmount().compareTo(BigDecimal.ZERO) > 0) {
            applyProvision(project, badDebt.getProvisionAmount());
        }
        // 风险标记联动看板风险预警区（看板按 fee_bad_debt OPEN/FOLLOWING 实时聚合）
        try {
            notificationService.notifyRole("boss", "项目烂账风险标记",
                "项目【" + project.getProjectName() + "】被标记烂账风险，类型：" + badDebt.getType()
                    + "，原因：" + badDebt.getReason(),
                "SYSTEM", "BAD_DEBT", badDebt.getId());
        } catch (Exception e) {
            log.warn("烂账标记通知发送失败（不阻断主流程）: {}", e.getMessage());
        }
        return badDebt.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int provisionBadDebt(Long id, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("计提金额必须大于0");
        }
        FeeBadDebt badDebt = badDebtMapper.selectById(id);
        if (badDebt == null) {
            throw new ServiceException("烂账记录不存在");
        }
        if ("CLOSED".equals(badDebt.getStatus())) {
            throw new ServiceException("已关闭的烂账记录不允许追加计提");
        }
        FeeProject project = projectMapper.selectById(badDebt.getProjectId());
        if (project == null) {
            throw new ServiceException("关联项目不存在");
        }
        FeeBadDebt update = new FeeBadDebt();
        update.setId(id);
        update.setProvisionAmount(zero(badDebt.getProvisionAmount()).add(amount));
        int rows = badDebtMapper.updateById(update);

        applyProvision(project, amount);
        return rows;
    }

    @Override
    public int followUp(Long id, String notes) {
        FeeBadDebt badDebt = badDebtMapper.selectById(id);
        if (badDebt == null) {
            throw new ServiceException("烂账记录不存在");
        }
        if ("CLOSED".equals(badDebt.getStatus())) {
            throw new ServiceException("已关闭的烂账记录不允许跟进");
        }
        FeeBadDebt update = new FeeBadDebt();
        update.setId(id);
        update.setStatus("FOLLOWING");
        update.setFollowUpNotes(StrUtil.isBlank(notes) ? badDebt.getFollowUpNotes()
            : StrUtil.nullToEmpty(badDebt.getFollowUpNotes())
                + StrUtil.format("\n[{}] {}", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()), notes));
        return badDebtMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int closeBadDebt(Long id) {
        FeeBadDebt badDebt = badDebtMapper.selectById(id);
        if (badDebt == null) {
            throw new ServiceException("烂账记录不存在");
        }
        if ("CLOSED".equals(badDebt.getStatus())) {
            throw new ServiceException("烂账记录已关闭，请勿重复操作");
        }
        FeeBadDebt update = new FeeBadDebt();
        update.setId(id);
        update.setStatus("CLOSED");
        int rows = badDebtMapper.updateById(update);

        // 项目状态置 LOST（烂账核销闭环）
        FeeProject project = projectMapper.selectById(badDebt.getProjectId());
        if (project != null && !"LOST".equals(project.getStatus())) {
            FeeProject updateProject = new FeeProject();
            updateProject.setId(project.getId());
            updateProject.setStatus("LOST");
            updateProject.setLossType(badDebt.getType());
            updateProject.setLossReason(badDebt.getReason());
            projectMapper.updateById(updateProject);
        }
        return rows;
    }

    /**
     * 计提坏账联动：累加项目坏账计提 + 计入项目支出（实际利润扣减项）
     */
    private void applyProvision(FeeProject project, BigDecimal amount) {
        FeeProject update = new FeeProject();
        update.setId(project.getId());
        update.setBadDebtProvision(zero(project.getBadDebtProvision()).add(amount));
        projectMapper.updateById(update);
        projectService.addExpense(project.getId(), amount);
    }

    /**
     * 批量填充项目名称
     */
    private void fillProjectName(List<FeeBadDebtVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> projectIds = list.stream().map(FeeBadDebtVo::getProjectId)
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

    private BigDecimal zero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
