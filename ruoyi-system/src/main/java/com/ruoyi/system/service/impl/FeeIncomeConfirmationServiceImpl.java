package com.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.FeeIncomeConfirmation;
import com.ruoyi.system.domain.FeeProject;
import com.ruoyi.system.domain.FeeProjectMember;
import com.ruoyi.system.domain.bo.FeeIncomeConfirmationBo;
import com.ruoyi.system.domain.vo.FeeIncomeConfirmationVo;
import com.ruoyi.system.mapper.FeeIncomeConfirmationMapper;
import com.ruoyi.system.mapper.FeeProjectMapper;
import com.ruoyi.system.mapper.FeeProjectMemberMapper;
import com.ruoyi.system.service.IFeeIncomeConfirmationService;
import com.ruoyi.system.service.IFeeNotificationService;
import com.ruoyi.system.service.IFeeProjectService;
import com.ruoyi.system.service.ISysMessageService;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 收入确认Service实现（T5.4）
 * 三态流转：项目经理创建→待核验；财务核验通过（待核验转已核验）/驳回（冲销待核验）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeIncomeConfirmationServiceImpl implements IFeeIncomeConfirmationService {

    private final FeeIncomeConfirmationMapper confirmationMapper;
    private final FeeProjectMapper projectMapper;
    private final FeeProjectMemberMapper memberMapper;
    private final IFeeProjectService projectService;
    private final IFeeNotificationService notificationService;
    private final ISysMessageService sysMessageService;

    @Override
    public TableDataInfo<FeeIncomeConfirmationVo> queryPageList(FeeIncomeConfirmationBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<FeeIncomeConfirmation> wrapper = Wrappers.<FeeIncomeConfirmation>lambdaQuery()
            .eq(bo.getProjectId() != null, FeeIncomeConfirmation::getProjectId, bo.getProjectId())
            .eq(StrUtil.isNotBlank(bo.getVerifyStatus()), FeeIncomeConfirmation::getVerifyStatus, bo.getVerifyStatus())
            .eq(StrUtil.isNotBlank(bo.getBasisType()), FeeIncomeConfirmation::getBasisType, bo.getBasisType())
            .orderByDesc(FeeIncomeConfirmation::getCreateTime);
        // 数据范围：项目经理仅本人参与项目
        List<Long> scopedProjectIds = scopedProjectIds();
        if (scopedProjectIds != null) {
            if (scopedProjectIds.isEmpty()) {
                return TableDataInfo.build(new Page<>());
            }
            wrapper.in(FeeIncomeConfirmation::getProjectId, scopedProjectIds);
        }
        Page<FeeIncomeConfirmationVo> page = confirmationMapper.selectVoPage(pageQuery.build(), wrapper);
        fillProjectName(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public FeeIncomeConfirmationVo queryById(Long id) {
        FeeIncomeConfirmationVo vo = confirmationMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("收入确认记录不存在");
        }
        List<Long> scoped = scopedProjectIds();
        if (scoped != null && !scoped.contains(vo.getProjectId())) {
            throw new ServiceException("无权查看该收入确认记录");
        }
        fillProjectName(List.of(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertConfirmation(FeeIncomeConfirmationBo bo) {
        FeeProject project = projectMapper.selectById(bo.getProjectId());
        if (project == null) {
            throw new ServiceException("关联项目不存在");
        }
        if (!"ACTIVE".equals(project.getStatus())) {
            throw new ServiceException("仅进行中的项目可创建收入确认");
        }
        FeeIncomeConfirmation confirmation = BeanCopyUtils.copy(bo, FeeIncomeConfirmation.class);
        confirmation.setVerifyStatus("PENDING");
        confirmation.setConfirmedBy(LoginHelper.getUserId());
        confirmation.setConfirmedByName(LoginHelper.getLoginUser().getNickName());
        if (confirmation.getConfirmDate() == null) {
            confirmation.setConfirmDate(new Date());
        }
        confirmationMapper.insert(confirmation);

        // 增加项目待核验收入
        projectService.addPendingIncome(bo.getProjectId(), bo.getConfirmedAmount());

        // 通知财务核验（角色广播，T5.12）
        try {
            notificationService.notifyRole("finance", "新收入确认待核验",
                "项目【" + project.getProjectName() + "】提交收入确认，金额 " + bo.getConfirmedAmount()
                    + " 元，请及时核验",
                "APPROVAL", "INCOME_CONFIRMATION", confirmation.getId());
        } catch (Exception e) {
            log.warn("收入确认待核验通知发送失败（不阻断主流程）: {}", e.getMessage());
        }
        return confirmation.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int verifyConfirmation(Long id) {
        FeeIncomeConfirmation confirmation = confirmationMapper.selectById(id);
        if (confirmation == null) {
            throw new ServiceException("收入确认记录不存在");
        }
        if (!"PENDING".equals(confirmation.getVerifyStatus())) {
            throw new ServiceException("仅待核验状态的收入确认可核验通过");
        }
        FeeIncomeConfirmation update = new FeeIncomeConfirmation();
        update.setId(id);
        update.setVerifyStatus("VERIFIED");
        update.setVerifyBy(LoginHelper.getUserId());
        update.setVerifyAt(new Date());
        int rows = confirmationMapper.updateById(update);

        // 待核验收入 → 已核验收入
        projectService.addPendingIncome(confirmation.getProjectId(), confirmation.getConfirmedAmount().negate());
        projectService.addIncome(confirmation.getProjectId(), confirmation.getConfirmedAmount(), true);

        notifyCreator(confirmation, "收入确认核验通过",
            "收入确认【金额 " + confirmation.getConfirmedAmount() + " 元】已核验通过，已计入项目已核验收入");
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int rejectConfirmation(Long id, String reason) {
        FeeIncomeConfirmation confirmation = confirmationMapper.selectById(id);
        if (confirmation == null) {
            throw new ServiceException("收入确认记录不存在");
        }
        if (!"PENDING".equals(confirmation.getVerifyStatus())) {
            throw new ServiceException("仅待核验状态的收入确认可驳回");
        }
        FeeIncomeConfirmation update = new FeeIncomeConfirmation();
        update.setId(id);
        update.setVerifyStatus("REJECTED");
        update.setVerifyBy(LoginHelper.getUserId());
        update.setVerifyAt(new Date());
        update.setRejectReason(reason);
        int rows = confirmationMapper.updateById(update);

        // 冲销项目待核验收入
        projectService.addPendingIncome(confirmation.getProjectId(), confirmation.getConfirmedAmount().negate());

        notifyCreator(confirmation, "收入确认核验驳回",
            "收入确认【金额 " + confirmation.getConfirmedAmount() + " 元】被驳回"
                + (StrUtil.isNotBlank(reason) ? "，原因：" + reason : ""));
        return rows;
    }

    /**
     * 数据范围：老板/财务/管理员返回 null（不限），其他角色返回本人参与的项目ID集合
     */
    private List<Long> scopedProjectIds() {
        LoginUser loginUser = LoginHelper.getLoginUser();
        Set<String> roles = loginUser.getRolePermission();
        if (LoginHelper.isAdmin() || roles == null) {
            return null;
        }
        if (roles.contains("admin") || roles.contains("boss") || roles.contains("finance")
            || roles.contains("platform_admin")) {
            return null;
        }
        List<FeeProjectMember> members = memberMapper.selectList(
            Wrappers.<FeeProjectMember>lambdaQuery()
                .eq(FeeProjectMember::getUserId, loginUser.getUserId())
                .eq(FeeProjectMember::getIsActive, 1));
        return members.stream().map(FeeProjectMember::getProjectId).distinct().collect(Collectors.toList());
    }

    /**
     * 批量填充项目名称
     */
    private void fillProjectName(List<FeeIncomeConfirmationVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> projectIds = list.stream().map(FeeIncomeConfirmationVo::getProjectId)
            .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        if (projectIds.isEmpty()) {
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
     * 通知收入确认创建人
     */
    private void notifyCreator(FeeIncomeConfirmation confirmation, String title, String content) {
        try {
            if (confirmation.getConfirmedBy() != null) {
                sysMessageService.sendMessage(confirmation.getConfirmedBy(), title, content,
                    "APPROVAL", "INCOME_CONFIRMATION", confirmation.getId());
            }
        } catch (Exception e) {
            log.warn("收入确认通知发送失败（不阻断主流程）: {}", e.getMessage());
        }
    }
}
