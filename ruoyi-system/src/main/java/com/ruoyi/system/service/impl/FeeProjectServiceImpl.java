package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.FeeAssetDepreciation;
import com.ruoyi.system.domain.FeeProject;
import com.ruoyi.system.domain.FeeProjectMember;
import com.ruoyi.system.domain.FeeProjectSharing;
import com.ruoyi.system.domain.FeeProjectSubject;
import com.ruoyi.system.domain.SysUserRole;
import com.ruoyi.system.domain.bo.FeeProjectBo;
import com.ruoyi.system.domain.vo.FeeMemberLoadVo;
import com.ruoyi.system.domain.vo.FeeProjectMemberVo;
import com.ruoyi.system.domain.vo.FeeProjectProfitVo;
import com.ruoyi.system.domain.vo.FeeProjectSubjectVo;
import com.ruoyi.system.domain.vo.FeeProjectVo;
import com.ruoyi.system.domain.vo.FeeUserOptionVo;
import com.ruoyi.system.mapper.FeeAssetDepreciationMapper;
import com.ruoyi.system.mapper.FeeProjectMapper;
import com.ruoyi.system.mapper.FeeProjectMemberMapper;
import com.ruoyi.system.mapper.FeeProjectSharingMapper;
import com.ruoyi.system.mapper.FeeProjectSubjectMapper;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.mapper.SysUserRoleMapper;
import com.ruoyi.system.service.IFeeProjectService;
import com.ruoyi.system.service.ISysMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 项目信息Service实现（T5.1）
 * 立项/审批/预算按大类分配/科目/成员；项目经理数据范围按成员过滤（T2.3）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeProjectServiceImpl implements IFeeProjectService {

    private final FeeProjectMapper projectMapper;
    private final FeeProjectSubjectMapper subjectMapper;
    private final FeeProjectMemberMapper memberMapper;
    private final FeeAssetDepreciationMapper depreciationMapper;
    private final FeeProjectSharingMapper sharingMapper;
    private final ISysMessageService sysMessageService;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserMapper userMapper;

    /** 报销人角色标识（M2_01 五角色体系） */
    private static final String ROLE_KEY_EMPLOYEE = "employee";
    /** 项目经理角色标识（M2_01 五角色体系） */
    private static final String ROLE_KEY_MANAGER = "manager";

    @Override
    public TableDataInfo<FeeProjectVo> queryPageList(FeeProjectBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<FeeProject> wrapper = Wrappers.<FeeProject>lambdaQuery()
            .like(StrUtil.isNotBlank(bo.getProjectName()), FeeProject::getProjectName, bo.getProjectName())
            .like(StrUtil.isNotBlank(bo.getClientName()), FeeProject::getClientName, bo.getClientName())
            .eq(StrUtil.isNotBlank(bo.getProjectManagerName()), FeeProject::getProjectManagerName, bo.getProjectManagerName())
            .eq(StrUtil.isNotBlank(bo.getStatus()), FeeProject::getStatus, bo.getStatus())
            .orderByDesc(FeeProject::getCreateTime);
        // 数据范围：非老板/财务/管理员仅可见本人参与的项目（T2.3）
        List<Long> scopedProjectIds = scopedProjectIds();
        if (scopedProjectIds != null) {
            if (scopedProjectIds.isEmpty()) {
                return TableDataInfo.build(new Page<>());
            }
            wrapper.in(FeeProject::getId, scopedProjectIds);
        }
        Page<FeeProjectVo> page = projectMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public FeeProjectVo queryById(Long id) {
        FeeProjectVo vo = projectMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("项目不存在");
        }
        checkDataScope(id);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertProject(FeeProjectBo bo) {
        FeeProject project = BeanCopyUtils.copy(bo, FeeProject.class);
        project.setStatus("PENDING_APPROVAL");
        project.setCurrentContractAmount(bo.getContractAmount());
        project.setTotalBudget(bo.getTotalBudget() == null ? BigDecimal.ZERO : bo.getTotalBudget());
        project.setUsedBudget(BigDecimal.ZERO);
        project.setVerifiedIncome(BigDecimal.ZERO);
        project.setPendingIncome(BigDecimal.ZERO);
        project.setTotalExpense(BigDecimal.ZERO);
        project.setBadDebtProvision(BigDecimal.ZERO);
        project.setChangeCount(0);
        projectMapper.insert(project);

        // 立项同时配置科目与成员（审批通过前允许调整）
        if (CollUtil.isNotEmpty(bo.getSubjects())) {
            saveSubjects(project.getId(), bo.getSubjects());
        }
        if (CollUtil.isNotEmpty(bo.getMembers())) {
            saveMembers(project.getId(), bo.getMembers());
        } else if (project.getProjectManagerId() != null) {
            // 未显式添加成员时，项目经理默认入组
            FeeProjectBo.FeeProjectMemberBo manager = new FeeProjectBo.FeeProjectMemberBo();
            manager.setUserId(project.getProjectManagerId());
            manager.setUserName(project.getProjectManagerName());
            manager.setRole("MANAGER");
            saveMembers(project.getId(), List.of(manager));
        }
        return project.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateProject(FeeProjectBo bo) {
        FeeProject project = projectMapper.selectById(bo.getId());
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        if (!"DRAFT".equals(project.getStatus()) && !"PENDING_APPROVAL".equals(project.getStatus())) {
            throw new ServiceException("仅草稿/待审批状态的项目可编辑");
        }
        FeeProject update = BeanCopyUtils.copy(bo, FeeProject.class);
        int rows = projectMapper.updateById(update);
        if (bo.getSubjects() != null) {
            saveSubjects(bo.getId(), bo.getSubjects());
        }
        if (bo.getMembers() != null) {
            saveMembers(bo.getId(), bo.getMembers());
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int approveProject(Long id) {
        FeeProject project = projectMapper.selectById(id);
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        if (!"PENDING_APPROVAL".equals(project.getStatus())) {
            throw new ServiceException("仅待审批状态的项目可审批");
        }
        FeeProject update = new FeeProject();
        update.setId(id);
        update.setStatus("ACTIVE");
        update.setCurrentContractAmount(
            project.getCurrentContractAmount() != null ? project.getCurrentContractAmount() : project.getContractAmount());
        int rows = projectMapper.updateById(update);

        // 未配置科目时生成默认项目科目（预算额度=总预算，后续可拆分到大类）
        Long subjectCount = subjectMapper.selectCount(Wrappers.<FeeProjectSubject>lambdaQuery()
            .eq(FeeProjectSubject::getProjectId, id));
        if (subjectCount == 0 && project.getTotalBudget() != null
            && project.getTotalBudget().compareTo(BigDecimal.ZERO) > 0) {
            FeeProjectSubject subject = new FeeProjectSubject();
            subject.setProjectId(id);
            subject.setSubjectName("项目预算");
            subject.setBudgetAmount(project.getTotalBudget());
            subject.setUsedAmount(BigDecimal.ZERO);
            subject.setIsActive(1);
            subjectMapper.insert(subject);
        }

        // 通知项目经理与财务
        notifyProjectApproved(project);
        return rows;
    }

    @Override
    public int rejectProject(Long id, String reason) {
        FeeProject project = projectMapper.selectById(id);
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        if (!"PENDING_APPROVAL".equals(project.getStatus())) {
            throw new ServiceException("仅待审批状态的项目可驳回");
        }
        FeeProject update = new FeeProject();
        update.setId(id);
        update.setStatus("DRAFT");
        update.setRemark(StrUtil.isNotBlank(reason) ? "驳回：" + reason : project.getRemark());
        int rows = projectMapper.updateById(update);
        if (project.getProjectManagerId() != null) {
            sysMessageService.sendMessage(project.getProjectManagerId(), "项目立项被驳回",
                "您参与的项目【" + project.getProjectName() + "】立项被驳回，原因：" + StrUtil.nullToEmpty(reason),
                "REJECT", "PROJECT", id);
        }
        return rows;
    }

    @Override
    public int submitProject(Long id) {
        FeeProject project = projectMapper.selectById(id);
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        if (!"DRAFT".equals(project.getStatus())) {
            throw new ServiceException("仅草稿状态的项目可提交审批");
        }
        FeeProject update = new FeeProject();
        update.setId(id);
        update.setStatus("PENDING_APPROVAL");
        return projectMapper.updateById(update);
    }

    @Override
    public int closeProject(Long id) {
        FeeProject project = projectMapper.selectById(id);
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        if ("CLOSED".equals(project.getStatus()) || "LOST".equals(project.getStatus())) {
            throw new ServiceException("项目已关闭，请勿重复操作");
        }
        FeeProject update = new FeeProject();
        update.setId(id);
        update.setStatus("CLOSED");
        return projectMapper.updateById(update);
    }

    @Override
    public List<FeeProjectVo> listActiveProjects() {
        LambdaQueryWrapper<FeeProject> wrapper = Wrappers.<FeeProject>lambdaQuery()
            .eq(FeeProject::getStatus, "ACTIVE")
            .orderByDesc(FeeProject::getCreateTime);
        List<Long> scopedProjectIds = scopedProjectIds();
        if (scopedProjectIds != null) {
            if (scopedProjectIds.isEmpty()) {
                return new ArrayList<>();
            }
            wrapper.in(FeeProject::getId, scopedProjectIds);
        }
        return projectMapper.selectVoList(wrapper);
    }

    @Override
    public FeeProjectProfitVo getProfitDetail(Long projectId) {
        FeeProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        checkDataScope(projectId);
        FeeProjectProfitVo vo = new FeeProjectProfitVo();
        vo.setProjectId(projectId);
        vo.setProjectName(project.getProjectName());
        vo.setCurrentContractAmount(zero(project.getCurrentContractAmount()));
        vo.setVerifiedIncome(zero(project.getVerifiedIncome()));
        vo.setPendingIncome(zero(project.getPendingIncome()));
        vo.setTotalExpense(zero(project.getTotalExpense()));
        vo.setBadDebtProvision(zero(project.getBadDebtProvision()));

        // 折旧分摊（项目级资产）
        List<FeeAssetDepreciation> deps = depreciationMapper.selectList(
            Wrappers.<FeeAssetDepreciation>lambdaQuery().eq(FeeAssetDepreciation::getProjectId, projectId));
        BigDecimal depreciationShare = deps.stream()
            .map(d -> zero(d.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setDepreciationShare(depreciationShare);

        // 项目分成（非PENDING全部计入）
        List<FeeProjectSharing> sharings = sharingMapper.selectList(
            Wrappers.<FeeProjectSharing>lambdaQuery()
                .eq(FeeProjectSharing::getProjectId, projectId)
                .ne(FeeProjectSharing::getStatus, "PENDING"));
        BigDecimal sharingAmount = sharings.stream()
            .map(s -> zero(s.getShareAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setSharingAmount(sharingAmount);

        // 纸面利润 = 合同额 - (已核验+待核验收入) - 支出
        BigDecimal paperProfit = vo.getCurrentContractAmount()
            .subtract(vo.getVerifiedIncome()).subtract(vo.getPendingIncome())
            .subtract(vo.getTotalExpense());
        vo.setPaperProfit(paperProfit);
        // 实际利润 = 已核验收入 - 支出 - 折旧分摊 - 分成 - 坏账计提
        BigDecimal actualProfit = vo.getVerifiedIncome()
            .subtract(vo.getTotalExpense()).subtract(depreciationShare)
            .subtract(sharingAmount).subtract(vo.getBadDebtProvision());
        vo.setActualProfit(actualProfit);
        if (vo.getVerifiedIncome().compareTo(BigDecimal.ZERO) > 0) {
            vo.setProfitRate(actualProfit.divide(vo.getVerifiedIncome(), 4, RoundingMode.HALF_UP));
        }
        return vo;
    }

    @Override
    public List<FeeProjectSubjectVo> listSubjects(Long projectId) {
        List<FeeProjectSubjectVo> list = subjectMapper.selectVoList(
            Wrappers.<FeeProjectSubject>lambdaQuery()
                .eq(FeeProjectSubject::getProjectId, projectId)
                .orderByAsc(FeeProjectSubject::getId));
        list.forEach(s -> s.setRemainAmount(zero(s.getBudgetAmount()).subtract(zero(s.getUsedAmount()))));
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveSubjects(Long projectId, List<FeeProjectBo.FeeProjectSubjectBo> subjects) {
        // 先删后插（已用金额>0的科目保护：先校验）
        Long usedCount = subjectMapper.selectCount(Wrappers.<FeeProjectSubject>lambdaQuery()
            .eq(FeeProjectSubject::getProjectId, projectId)
            .gt(FeeProjectSubject::getUsedAmount, BigDecimal.ZERO));
        if (usedCount > 0) {
            throw new ServiceException("项目科目已发生使用，不允许整体重建，请逐条调整");
        }
        subjectMapper.delete(Wrappers.<FeeProjectSubject>lambdaQuery()
            .eq(FeeProjectSubject::getProjectId, projectId));
        BigDecimal total = BigDecimal.ZERO;
        for (FeeProjectBo.FeeProjectSubjectBo bo : subjects) {
            FeeProjectSubject subject = BeanCopyUtils.copy(bo, FeeProjectSubject.class);
            subject.setId(null);
            subject.setProjectId(projectId);
            subject.setUsedAmount(BigDecimal.ZERO);
            subject.setIsActive(bo.getIsActive() == null ? 1 : bo.getIsActive());
            subjectMapper.insert(subject);
            total = total.add(zero(bo.getBudgetAmount()));
        }
        // 同步项目总预算
        FeeProject update = new FeeProject();
        update.setId(projectId);
        update.setTotalBudget(total);
        return projectMapper.updateById(update);
    }

    @Override
    public List<FeeProjectMemberVo> listMembers(Long projectId) {
        return memberMapper.selectVoList(Wrappers.<FeeProjectMember>lambdaQuery()
            .eq(FeeProjectMember::getProjectId, projectId)
            .orderByAsc(FeeProjectMember::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveMembers(Long projectId, List<FeeProjectBo.FeeProjectMemberBo> members) {
        memberMapper.delete(Wrappers.<FeeProjectMember>lambdaQuery()
            .eq(FeeProjectMember::getProjectId, projectId));
        int count = 0;
        for (FeeProjectBo.FeeProjectMemberBo bo : members) {
            FeeProjectMember member = new FeeProjectMember();
            member.setProjectId(projectId);
            member.setUserId(bo.getUserId());
            member.setUserName(bo.getUserName());
            member.setRole(StrUtil.isNotBlank(bo.getRole()) ? bo.getRole() : "MEMBER");
            member.setInvestRatio(bo.getInvestRatio());
            member.setIsActive(1);
            memberMapper.insert(member);
            count++;
        }
        return count;
    }

    @Override
    public List<FeeUserOptionVo> listReimbursers() {
        return listUsersByRoleKey(ROLE_KEY_EMPLOYEE);
    }

    @Override
    public List<FeeUserOptionVo> listManagers() {
        return listUsersByRoleKey(ROLE_KEY_MANAGER);
    }

    /**
     * 指定角色标识的启用状态用户下拉（含共享预设角色与租户克隆角色；租户隔离由用户过滤保证）
     */
    private List<FeeUserOptionVo> listUsersByRoleKey(String roleKey) {
        // 角色（含共享预设角色与租户克隆角色）；sys_role 不做租户隔离，隔离由后续用户过滤保证
        List<SysRole> roles = roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
            .eq(SysRole::getRoleKey, roleKey)
            .eq(SysRole::getStatus, "0"));
        if (CollUtil.isEmpty(roles)) {
            return new ArrayList<>();
        }
        Set<Long> roleIds = roles.stream().map(SysRole::getRoleId).collect(Collectors.toSet());
        Set<Long> userIds = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                .in(SysUserRole::getRoleId, roleIds)).stream()
            .map(SysUserRole::getUserId).collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.<SysUser>lambdaQuery()
            .in(SysUser::getUserId, userIds)
            .eq(SysUser::getStatus, "0")
            .orderByAsc(SysUser::getUserId);
        // 租户隔离：sys_user 在 IGNORE_TENANT_TABLES 中，非超管需手工按当前租户过滤（T2.4）
        if (!LoginHelper.isAdmin()) {
            Long tenantId = LoginHelper.getTenantId();
            if (tenantId != null) {
                wrapper.eq(SysUser::getTenantId, tenantId);
            }
        }
        return userMapper.selectList(wrapper).stream().map(u -> {
            FeeUserOptionVo vo = new FeeUserOptionVo();
            vo.setUserId(u.getUserId());
            vo.setUserName(u.getUserName());
            vo.setNickName(u.getNickName());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public BigDecimal getAvailableBudget(Long projectId, Long categoryId) {
        LambdaQueryWrapper<FeeProjectSubject> wrapper = Wrappers.<FeeProjectSubject>lambdaQuery()
            .eq(FeeProjectSubject::getProjectId, projectId)
            .eq(categoryId != null, FeeProjectSubject::getCategoryId, categoryId)
            .eq(FeeProjectSubject::getIsActive, 1);
        return subjectMapper.selectList(wrapper).stream()
            .map(s -> zero(s.getBudgetAmount()).subtract(zero(s.getUsedAmount())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductBudget(Long projectId, Long categoryId, BigDecimal amount, boolean addExpense) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        LambdaQueryWrapper<FeeProjectSubject> wrapper = Wrappers.<FeeProjectSubject>lambdaQuery()
            .eq(FeeProjectSubject::getProjectId, projectId)
            .eq(categoryId != null, FeeProjectSubject::getCategoryId, categoryId)
            .eq(FeeProjectSubject::getIsActive, 1)
            .orderByAsc(FeeProjectSubject::getId);
        List<FeeProjectSubject> subjects = subjectMapper.selectList(wrapper);

        // 未配置科目 → 不阻断（公司级兜底），仅累计支出
        if (CollUtil.isNotEmpty(subjects)) {
            BigDecimal totalAvailable = subjects.stream()
                .map(s -> zero(s.getBudgetAmount()).subtract(zero(s.getUsedAmount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalAvailable.compareTo(amount) < 0) {
                return false;
            }
            BigDecimal remaining = amount;
            for (FeeProjectSubject s : subjects) {
                BigDecimal avail = zero(s.getBudgetAmount()).subtract(zero(s.getUsedAmount()));
                if (avail.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal deduct = remaining.min(avail);
                FeeProjectSubject update = new FeeProjectSubject();
                update.setId(s.getId());
                update.setUsedAmount(zero(s.getUsedAmount()).add(deduct));
                subjectMapper.updateById(update);
                remaining = remaining.subtract(deduct);
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
            }
        }

        FeeProject project = projectMapper.selectById(projectId);
        if (project != null) {
            FeeProject update = new FeeProject();
            update.setId(projectId);
            if (CollUtil.isNotEmpty(subjects)) {
                update.setUsedBudget(zero(project.getUsedBudget()).add(amount));
            }
            if (addExpense) {
                update.setTotalExpense(zero(project.getTotalExpense()).add(amount));
            }
            projectMapper.updateById(update);
        }
        return true;
    }

    @Override
    public void addIncome(Long projectId, BigDecimal amount, boolean verified) {
        FeeProject project = projectMapper.selectById(projectId);
        if (project == null) {
            return;
        }
        FeeProject update = new FeeProject();
        update.setId(projectId);
        if (verified) {
            update.setVerifiedIncome(zero(project.getVerifiedIncome()).add(amount));
        } else {
            update.setPendingIncome(zero(project.getPendingIncome()).add(amount));
        }
        projectMapper.updateById(update);
    }

    @Override
    public void addPendingIncome(Long projectId, BigDecimal amount) {
        FeeProject project = projectMapper.selectById(projectId);
        if (project == null) {
            return;
        }
        FeeProject update = new FeeProject();
        update.setId(projectId);
        update.setPendingIncome(zero(project.getPendingIncome()).add(amount));
        projectMapper.updateById(update);
    }

    @Override
    public void addExpense(Long projectId, BigDecimal amount) {
        FeeProject project = projectMapper.selectById(projectId);
        if (project == null) {
            return;
        }
        FeeProject update = new FeeProject();
        update.setId(projectId);
        update.setTotalExpense(zero(project.getTotalExpense()).add(amount));
        projectMapper.updateById(update);
    }

    @Override
    public boolean isProjectMember(Long projectId, Long userId) {
        Long count = memberMapper.selectCount(Wrappers.<FeeProjectMember>lambdaQuery()
            .eq(FeeProjectMember::getProjectId, projectId)
            .eq(FeeProjectMember::getUserId, userId)
            .eq(FeeProjectMember::getIsActive, 1));
        return count != null && count > 0;
    }

    @Override
    public FeeMemberLoadVo memberLoadCheck(Long userId) {
        FeeMemberLoadVo vo = new FeeMemberLoadVo();
        vo.setUserId(userId);
        vo.setProjectCount(0);
        vo.setTotalRatio(0);
        vo.setOverLoad(false);
        if (userId == null) {
            return vo;
        }
        List<FeeProjectMember> members = memberMapper.selectList(Wrappers.<FeeProjectMember>lambdaQuery()
            .eq(FeeProjectMember::getUserId, userId)
            .eq(FeeProjectMember::getIsActive, 1));
        if (members.isEmpty()) {
            return vo;
        }
        List<Long> projectIds = members.stream().map(FeeProjectMember::getProjectId)
            .distinct().collect(Collectors.toList());
        java.util.Map<Long, FeeProject> projectMap = projectMapper.selectBatchIds(projectIds).stream()
            .collect(Collectors.toMap(FeeProject::getId, p -> p, (a, b) -> a));
        int totalRatio = 0;
        for (FeeProjectMember member : members) {
            FeeProject project = projectMap.get(member.getProjectId());
            if (project == null || "CLOSED".equals(project.getStatus()) || "LOST".equals(project.getStatus())) {
                continue;
            }
            FeeMemberLoadVo.ProjectPart part = new FeeMemberLoadVo.ProjectPart();
            part.setProjectId(member.getProjectId());
            part.setProjectName(project.getProjectName());
            part.setRole(member.getRole());
            part.setInvestRatio(member.getInvestRatio() == null ? 0 : member.getInvestRatio());
            part.setProjectStatus(project.getStatus());
            vo.getProjects().add(part);
            totalRatio += part.getInvestRatio();
        }
        vo.setUserName(members.get(0).getUserName());
        vo.setProjectCount(vo.getProjects().size());
        vo.setTotalRatio(totalRatio);
        // 投入比例合计>100% 提示负载超限（仅提示不拦截，设计文档 4.2.5）
        vo.setOverLoad(totalRatio > 100);
        return vo;
    }

    /**
     * 数据范围：老板/财务/管理员返回 null（不限），其他角色返回本人参与的项目ID集合（T6.5 开放供利润报表复用）
     */
    @Override
    public List<Long> scopedProjectIds() {
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
     * 详情/利润查询的数据范围校验（项目经理查他人项目返回异常）
     */
    private void checkDataScope(Long projectId) {
        List<Long> scoped = scopedProjectIds();
        if (scoped != null && !scoped.contains(projectId)) {
            throw new ServiceException("无权查看该项目");
        }
    }

    /**
     * 审批通过通知（合并通知财务角色，T5.12）
     */
    private void notifyProjectApproved(FeeProject project) {
        try {
            if (project.getProjectManagerId() != null) {
                sysMessageService.sendMessage(project.getProjectManagerId(), "项目立项审批通过",
                    "项目【" + project.getProjectName() + "】立项审批通过，已进入进行中状态",
                    "APPROVAL", "PROJECT", project.getId());
            }
        } catch (Exception e) {
            log.warn("项目审批通知发送失败（不阻断主流程）: {}", e.getMessage());
        }
    }

    private BigDecimal zero(BigDecimal value) {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO);
    }
}
