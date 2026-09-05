package com.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.service.FeatureChecker;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.domain.bo.BudgetPreparationBo;
import com.ruoyi.system.domain.bo.BudgetCategoryQuotaBo;
import com.ruoyi.system.domain.vo.*;
import com.ruoyi.system.config.BudgetProcessDeployer;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.IBudgetNotificationService;
import com.ruoyi.system.service.IBudgetPreparationService;
import com.ruoyi.system.service.ISysMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 预算编制主表Service业务层处理
 *
 * @author ruoyi
 * @date 2026-06-19
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class BudgetPreparationServiceImpl implements IBudgetPreparationService {

    private final BudgetPreparationMapper baseMapper;
    private final BudgetRejectHistoryMapper rejectHistoryMapper;
    private final BudgetValidationRuleMapper validationRuleMapper;
    private final BudgetPreparationDetailMapper detailMapper;
    private final BudgetSubjectMapper subjectMapper;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final RepositoryService repositoryService;
    private final BudgetProcessDeployer budgetProcessDeployer;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final ISysMessageService sysMessageService;
    private final IBudgetNotificationService budgetNotificationService;
    private final ExpenseBillMapper expenseBillMapper;
    private final FeeRuleVersionMapper feeRuleVersionMapper;
    private final FeatureChecker featureChecker;
    private final FeeExpenseCategoryMapper feeExpenseCategoryMapper;
    private final FeeCategorySplitRatioMapper feeCategorySplitRatioMapper;
    private final FeeTenantCategoryMapper feeTenantCategoryMapper;
    private final SysDeptMapper sysDeptMapper;

    /**
     * 查询预算编制
     */
    @Override
    public BudgetPreparationVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询预算编制列表
     */
    @Override
    public TableDataInfo<BudgetPreparationVo> queryPageList(BudgetPreparationBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<BudgetPreparation> lqw = buildQueryWrapper(bo);
        Page<BudgetPreparationVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询预算编制列表
     */
    @Override
    public List<BudgetPreparationVo> queryList(BudgetPreparationBo bo) {
        LambdaQueryWrapper<BudgetPreparation> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<BudgetPreparation> buildQueryWrapper(BudgetPreparationBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<BudgetPreparation> lqw = Wrappers.lambdaQuery();
        
        // 年度筛选
        lqw.eq(bo.getBudgetYear() != null, BudgetPreparation::getBudgetYear, bo.getBudgetYear());
        
        // 月份筛选
        lqw.eq(bo.getBudgetMonth() != null, BudgetPreparation::getBudgetMonth, bo.getBudgetMonth());
        
        // 状态筛选
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), BudgetPreparation::getStatus, bo.getStatus());
        
        // 审批阶段筛选
        lqw.eq(StringUtils.isNotBlank(bo.getApprovalStage()), BudgetPreparation::getApprovalStage, bo.getApprovalStage());
        
        // 组织ID筛选（部门权限控制）
        lqw.eq(bo.getOrgId() != null, BudgetPreparation::getOrgId, bo.getOrgId());
        
        // 预算单号模糊查询
        lqw.like(StringUtils.isNotBlank(bo.getSheetNo()), BudgetPreparation::getSheetNo, bo.getSheetNo());
        
        // 按创建时间倒序
        lqw.orderByDesc(BudgetPreparation::getCreateTime);
        
        return lqw;
    }

    /**
     * 新增预算编制
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(BudgetPreparationBo bo) {
        BudgetPreparation add = BeanUtil.toBean(bo, BudgetPreparation.class);
        
        // 从登录用户信息自动填充组织ID和组织名称
        if (add.getOrgId() == null) {
            add.setOrgId(LoginHelper.getDeptId());
        }
        if (StringUtils.isBlank(add.getOrgName())) {
            add.setOrgName(LoginHelper.getLoginUser().getDeptName());
        }
        
        // 生成预算单号：BG-YYYYMM-XXX
        if (StringUtils.isBlank(add.getSheetNo())) {
            add.setSheetNo(generateSheetNo(add.getBudgetYear(), add.getBudgetMonth()));
        }
        
        // budget_period 是数据库生成列，置 null 由数据库自动计算
        add.setBudgetPeriod(null);
        
        // 设置初始状态为草稿
        if (StringUtils.isBlank(add.getStatus())) {
            add.setStatus("Draft");
        }
        if (StringUtils.isBlank(add.getApprovalStage())) {
            add.setApprovalStage("None");
        }
        
        // 设置当前处理人为创建人
        if (StringUtils.isBlank(add.getCurrentHandler())) {
            add.setCurrentHandler(LoginHelper.getUsername());
        }
        
        // 初始化金额为0
        if (add.getTotalBudget() == null) {
            add.setTotalBudget(BigDecimal.ZERO);
        }
        if (add.getTotalActual() == null) {
            add.setTotalActual(BigDecimal.ZERO);
        }
        if (add.getVarianceRate() == null) {
            add.setVarianceRate(BigDecimal.ZERO);
        }
        
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改预算编制
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(BudgetPreparationBo bo) {
        BudgetPreparation update = BeanUtil.toBean(bo, BudgetPreparation.class);
        
        // 校验状态：只有草稿和驳回状态可以修改
        BudgetPreparation existPreparation = baseMapper.selectById(bo.getId());
        if (existPreparation != null) {
            String status = existPreparation.getStatus();
            if (!"Draft".equals(status) && !"Rejected".equals(status)) {
                throw new ServiceException("只有草稿和驳回状态的预算编制可以修改");
            }
            // 编辑后重置状态为草稿，需要重新完成编制后才能提交审核
            update.setStatus("Draft");
        }
        
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(BudgetPreparation entity) {
        // TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除预算编制
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 校验：只有草稿状态可以删除
            for (Long id : ids) {
                BudgetPreparation preparation = baseMapper.selectById(id);
                if (preparation != null) {
                    String status = preparation.getStatus();
                    if (!"Draft".equals(status)) {
                        throw new ServiceException("预算单号[" + preparation.getSheetNo() + "]不是草稿状态，不能删除");
                    }
                }
            }
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 完成编制（将状态设置为 Completed）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean completePreparation(Long id) {
        BudgetPreparation preparation = baseMapper.selectById(id);
        if (preparation == null) {
            throw new ServiceException("预算编制不存在");
        }
        
        // 只有草稿和驳回状态可以执行完成编制
        String status = preparation.getStatus();
        if (!"Draft".equals(status) && !"Rejected".equals(status)) {
            throw new ServiceException("只有草稿和驳回状态的预算编制可以执行完成编制");
        }
        
        // 校验明细数据完整性
        List<BudgetPreparationDetail> details = detailMapper.selectList(
            Wrappers.<BudgetPreparationDetail>lambdaQuery()
                .eq(BudgetPreparationDetail::getSheetId, id));
        if (details == null || details.isEmpty()) {
            throw new ServiceException("预算编制明细为空，请先填写预算数据");
        }
        
        // 更新状态为完成编制
        preparation.setStatus("Completed");
        return baseMapper.updateById(preparation) > 0;
    }

    /**
     * 提交审核（单个）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean submitForReview(Long id) {
        BudgetPreparation preparation = baseMapper.selectById(id);
        if (preparation == null) {
            throw new ServiceException("预算编制不存在");
        }
        
        // 校验状态：只有完成编制状态可以提交
        String status = preparation.getStatus();
        if (!"Completed".equals(status)) {
            throw new ServiceException("预算编制状态不是完成编制，不能提交审核");
        }
        
        // 校验明细数据完整性
        List<BudgetPreparationDetail> details = detailMapper.selectList(
            Wrappers.<BudgetPreparationDetail>lambdaQuery()
                .eq(BudgetPreparationDetail::getSheetId, id));

        if (details == null || details.isEmpty()) {
            throw new ServiceException("预算编制明细为空，请先填写预算数据");
        }

        // 校验是否存在金额为空的科目
        boolean hasEmptyAmount = details.stream()
            .anyMatch(d -> d.getBudgetAmount() == null);
        if (hasEmptyAmount) {
            throw new ServiceException("存在未填写预算金额的科目，请补充完整");
        }
        
        // 查询最近的驳回记录，确定重新提交后应从哪个审批阶段恢复
        String resubmitStage = getResubmitStage(id);

        // 启动 Flowable 审批流程（驳回后重新提交不新增流程，复用原有审批链路）
        startApprovalProcess(preparation);

        // 驳回后重新提交：将 Flowable 流程跳转到被驳回的审批级别，跳过已通过的审批
        if (resubmitStage != null && !"Dept".equals(resubmitStage)) {
            moveFlowToStage(preparation.getProcessInstanceId(), resubmitStage);
        }

        // 更新状态为待审核，审批阶段为驳回级别（非驳回重新提交则为部门领导）
        preparation.setStatus("Pending_Review");
        preparation.setApprovalStage(resubmitStage != null ? resubmitStage : "Dept");
        preparation.setRejectLevel("None");
        preparation.setRejectReason(null);
        
        boolean result = baseMapper.updateById(preparation) > 0;

        // 给对应审批阶段的角色用户发送系统消息，通知其进行审核
        if (result) {
            String approvalStage = preparation.getApprovalStage();
            sendApprovalNotifyToApprovers(preparation, approvalStage);
        }

        return result;
    }

    /**
     * 提交审核时给对应审批角色的用户发送系统消息通知
     *
     * @param preparation     预算编制单
     * @param approvalStage   当前审批阶段（Dept/Branch/HQ）
     */
    private void sendApprovalNotifyToApprovers(BudgetPreparation preparation, String approvalStage) {
        try {
            String roleKey = getRoleKeyByStage(approvalStage);
            if (roleKey == null) {
                return;
            }

            // 根据 roleKey 查询角色
            SysRole role = sysRoleMapper.selectOne(
                Wrappers.<SysRole>lambdaQuery()
                    .eq(SysRole::getRoleKey, roleKey)
                    .eq(SysRole::getStatus, "0"));
            if (role == null) {
                log.warn("提交审核通知：未找到角色 {}", roleKey);
                return;
            }

            // 根据角色ID查询关联的用户ID列表
            List<Long> userIds = sysUserRoleMapper.selectUserIdsByRoleId(role.getRoleId());
            if (userIds == null || userIds.isEmpty()) {
                log.warn("提交审核通知：角色 {} 下没有用户", roleKey);
                return;
            }

            // 构建消息内容
            String stageLabel = getStageLabel(approvalStage);
            String submitter = LoginHelper.getNickName();
            String title = "待审批通知";
            String content = submitter + " 提交了预算单 " + preparation.getSheetNo()
                + "（" + preparation.getOrgName() + "），请您及时审核。";

            sysMessageService.sendBatchMessage(userIds, title, content, "APPROVAL", "budget_preparation", preparation.getId());
            log.info("提交审核通知已发送: sheetNo={}, stage={}, userIds={}", preparation.getSheetNo(), stageLabel, userIds);
        } catch (Exception e) {
            log.error("发送审批通知消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 根据审批阶段获取对应的角色key
     */
    private String getRoleKeyByStage(String stage) {
        if ("Dept".equals(stage)) return "deptManager";
        if ("Branch".equals(stage)) return "branch";
        if ("HQ".equals(stage)) return "headquarters";
        return null;
    }

    /**
     * 根据审批阶段获取中文标签
     */
    private String getStageLabel(String stage) {
        if ("Dept".equals(stage)) return "部门领导";
        if ("Branch".equals(stage)) return "分公司领导";
        if ("HQ".equals(stage)) return "总公司领导";
        return stage;
    }

    /**
     * 启动预算审批流程
     */
    private void startApprovalProcess(BudgetPreparation preparation) {
        try {
            // 确保流程定义处于激活状态（防止因被挂起而无法启动流程实例）
            ensureProcessDefinitionActive();

            Map<String, Object> variables = new HashMap<>();
            // 设置三级审批候选组（ROLE + roleId）
            variables.put("deptLeaderGroup", Collections.singletonList("ROLE3"));   // 部门领导
            variables.put("branchLeaderGroup", Collections.singletonList("ROLE4")); // 分公司领导
            variables.put("hqLeaderGroup", Collections.singletonList("ROLE5"));     // 总公司领导
            // 设置流程变量
            variables.put("sheetId", preparation.getId());
            variables.put("sheetNo", preparation.getSheetNo());
            variables.put("createBy", LoginHelper.getUsername());
            variables.put("orgId", preparation.getOrgId());
            variables.put("totalBudget", preparation.getTotalBudget());

            // 启动流程实例
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                budgetProcessDeployer.getProcessKey(),
                preparation.getSheetNo(),
                variables
            );

            // 保存流程实例ID
            preparation.setProcessInstanceId(processInstance.getId());
        } catch (Exception e) {
            throw new ServiceException("启动审批流程失败：" + e.getMessage());
        }
    }

    /**
     * 确保预算审批流程定义处于激活状态
     * 如果流程定义被挂起（例如通过工作流管理界面操作），则自动激活
     */
    private void ensureProcessDefinitionActive() {
        ProcessDefinition processDef = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey(budgetProcessDeployer.getProcessKey())
            .latestVersion()
            .singleResult();
        if (processDef != null && processDef.isSuspended()) {
            repositoryService.activateProcessDefinitionById(processDef.getId(), true, null);
            log.info("流程定义 {} 已被挂起，已自动激活", processDef.getId());
        }
    }

    /**
     * 批量提交审核
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchSubmitForReview(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择要提交的预算编制");
        }
        
        for (Long id : ids) {
            submitForReview(id);
        }
        return true;
    }

    /**
     * 批量审批通过
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchApprove(List<Long> ids, String remark) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择要审批的预算编制");
        }
        
        for (Long id : ids) {
            approve(id, remark);
        }
        return true;
    }

    /**
     * 校验当前用户是否为当前审批阶段对应的角色
     * 部门领导(deptManager) → Dept阶段
     * 分公司领导(branch) → Branch阶段
     * 总公司领导(headquarters) → HQ阶段
     * admin 可审批所有阶段
     */
    private void checkCurrentUserCanApprove(BudgetPreparation preparation) {
        if (LoginHelper.isAdmin()) {
            return;
        }
        Set<String> roleKeys = LoginHelper.getLoginUser().getRolePermission();
        if (roleKeys == null) {
            roleKeys = Collections.emptySet();
        }
        String stage = preparation.getApprovalStage();
        boolean canApprove = false;
        if ("Dept".equals(stage) && roleKeys.contains("deptManager")) {
            canApprove = true;
        } else if ("Branch".equals(stage) && roleKeys.contains("branch")) {
            canApprove = true;
        } else if ("HQ".equals(stage) && roleKeys.contains("headquarters")) {
            canApprove = true;
        }
        if (!canApprove) {
            throw new ServiceException("当前用户不是该审批阶段的审批人，无权操作");
        }
    }

    /**
     * 审批通过（单个）- 集成 Flowable 任务完成
     * 三级审批流转：部门领导 → 分公司领导 → 总公司领导
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean approve(Long id, String remark) {
        BudgetPreparation preparation = baseMapper.selectById(id);
        if (preparation == null) {
            throw new ServiceException("预算编制不存在");
        }
        
        String status = preparation.getStatus();
        // 校验状态：只有待审核状态可以审批
        if (!"Pending_Review".equals(status)) {
            throw new ServiceException("预算编制状态不是待审核，不能审批");
        }
        
        // 校验当前用户是否为当前审批阶段的审批人
        checkCurrentUserCanApprove(preparation);
        
        // 完成 Flowable 任务（通过）
        completeFlowableTask(preparation.getProcessInstanceId(), "pass", remark);
        
        // 审批阶段流转：Dept → Branch → HQ → Approved
        String stage = preparation.getApprovalStage();
        if ("Dept".equals(stage)) {
            preparation.setApprovalStage("Branch");
            preparation.setCurrentHandler("branch");
        } else if ("Branch".equals(stage)) {
            preparation.setApprovalStage("HQ");
            preparation.setCurrentHandler("headquarters");
        } else if ("HQ".equals(stage)) {
            preparation.setStatus("Approved");
            preparation.setApprovalStage("None");
            preparation.setCurrentHandler(null);
            // 锁定预算时记录当时规则快照（T7.4 / FEAT_RULE_GRAYSCALE，规则迭代不腐蚀历史）
            stampRuleVersion(preparation);
        }
        
        if (StringUtils.isNotBlank(remark)) {
            preparation.setRemark(remark);
        }
        
        return baseMapper.updateById(preparation) > 0;
    }

    /**
     * 审批通过时记录当时生效的规则版本快照（仅专业版开关开启时生效，失败不阻断审批）
     */
    private void stampRuleVersion(BudgetPreparation preparation) {
        try {
            if (!featureChecker.isFeatureEnabled("FEAT_RULE_GRAYSCALE")) {
                return;
            }
            FeeRuleVersion active = feeRuleVersionMapper.selectOne(
                Wrappers.<FeeRuleVersion>lambdaQuery()
                    .in(FeeRuleVersion::getGrayscaleStatus, "FULL", "GRAYSCALE")
                    .orderByDesc(FeeRuleVersion::getVersionNo)
                    .last("LIMIT 1"));
            if (active != null) {
                preparation.setRuleVersionId(active.getId());
            }
        } catch (Exception e) {
            log.warn("记录规则版本快照失败（不阻断审批）: {}", e.getMessage());
        }
    }

    /**
     * 审批驳回（单个）- 集成 Flowable 任务完成
     * 任何级别驳回都回退到编制人员（状态重置为 Draft）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean reject(Long id, String reason) {
        BudgetPreparation preparation = baseMapper.selectById(id);
        if (preparation == null) {
            throw new ServiceException("预算编制不存在");
        }
        
        String status = preparation.getStatus();
        // 校验状态：只有待审核状态可以驳回
        if (!"Pending_Review".equals(status)) {
            throw new ServiceException("预算编制状态不是待审核，不能驳回");
        }
        
        // 校验当前用户是否为当前审批阶段的审批人
        checkCurrentUserCanApprove(preparation);
        
        // 校验驳回理由
        if (StringUtils.isBlank(reason)) {
            throw new ServiceException("驳回时必须填写驳回意见");
        }
        
        // 完成 Flowable 任务（驳回）
        completeFlowableTask(preparation.getProcessInstanceId(), "reject", reason);
        
        // 驳回后状态回退到已驳回，由编制人员重新修改提交
        String currentStage = preparation.getApprovalStage();
        preparation.setStatus("Rejected");
        preparation.setApprovalStage("None");
        preparation.setRejectReason(reason);
        preparation.setRejectLevel(currentStage);
        preparation.setCurrentHandler(LoginHelper.getUsername());
        boolean result = baseMapper.updateById(preparation) > 0;
        
        // 写入驳回历史
        if (result) {
            BudgetRejectHistory history = new BudgetRejectHistory();
            history.setSheetId(id);
            history.setSheetNo(preparation.getSheetNo());
            history.setRejectFromLevel(currentStage);
            history.setRejectFromUser(LoginHelper.getUsername());
            history.setRejectFromName(LoginHelper.getNickName());
            history.setRejectToLevel("Dept");
            history.setRejectReason(reason);
            history.setRejectTime(new Date());
            history.setDeadlineTime(calculateDeadline(currentStage));
            rejectHistoryMapper.insert(history);
        }

        // 发送驳回通知（系统消息 + 邮件）
        if (result) {
            sendRejectNotification(preparation, currentStage, reason);
        }
        
        return result;
    }

    /**
     * 发送驳回通知：系统消息 + 邮件通知
     */
    private void sendRejectNotification(BudgetPreparation preparation, String rejectStage, String reason) {
        try {
            // 查找编制人员
            com.ruoyi.common.core.domain.entity.SysUser creator = sysUserMapper.selectUserByUserName(preparation.getCreateBy());
            if (creator == null) {
                log.warn("驳回通知：未找到编制人员 {}", preparation.getCreateBy());
                return;
            }

            // 驳回级别中文映射
            String stageLabel = "Dept".equals(rejectStage) ? "部门领导" : "Branch".equals(rejectStage) ? "分公司领导" : "总公司领导";
            String rejectBy = LoginHelper.getNickName();

            // 1. 发送系统消息（站内信）
            String title = "预算编制被驳回";
            String content = "您编制的预算单 " + preparation.getSheetNo() + " 已被" + stageLabel + "（" + rejectBy + "）驳回。\n驳回理由：" + reason;
            sysMessageService.sendMessage(creator.getUserId(), title, content, "REJECT", "budget_preparation", preparation.getId());

            // 2. 发送邮件通知
            Date deadline = calculateDeadline(rejectStage);
            budgetNotificationService.sendRejectNotify(
                preparation.getSheetNo(), stageLabel, reason, deadline, preparation.getId()
            );

            log.info("驳回通知已发送: sheetNo={}, creator={}, stage={}", preparation.getSheetNo(), creator.getNickName(), stageLabel);
        } catch (Exception e) {
            // 通知发送失败不影响主流程
            log.error("发送驳回通知失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取校验结果
     */
    @Override
    public List<ValidationResultVo> getValidationResults(Long sheetId) {
        List<ValidationResultVo> results = new ArrayList<>();
        
        // 1. 获取启用的校验规则
        List<BudgetValidationRule> rules = validationRuleMapper.selectList(
            Wrappers.<BudgetValidationRule>lambdaQuery()
                .eq(BudgetValidationRule::getIsActive, 1)
        );
        
        // 2. 获取预算明细
        List<BudgetPreparationDetail> details = detailMapper.selectList(
            Wrappers.<BudgetPreparationDetail>lambdaQuery()
                .eq(BudgetPreparationDetail::getSheetId, sheetId)
        );
        
        // 3. 获取主表数据
        BudgetPreparation sheet = baseMapper.selectById(sheetId);
        if (sheet == null) {
            return results;
        }
        
        // 4. 逐条规则执行校验
        for (BudgetValidationRule rule : rules) {
            List<ValidationResultVo> ruleResults = executeRule(rule, details, sheet);
            results.addAll(ruleResults);
        }
        
        return results;
    }

    /**
     * 执行单条校验规则
     */
    private List<ValidationResultVo> executeRule(BudgetValidationRule rule,
        List<BudgetPreparationDetail> details, BudgetPreparation sheet) {
        List<ValidationResultVo> results = new ArrayList<>();
        
        switch (rule.getRuleType()) {
            case "BUDGET_OVER":
                BigDecimal threshold = rule.getThresholdValue();
                if (threshold == null) {
                    break;
                }
                for (BudgetPreparationDetail d : details) {
                    if (d.getActualAmount() != null && d.getActualAmount().compareTo(BigDecimal.ZERO) > 0
                        && d.getVarianceAmount() != null) {
                        BigDecimal rate = d.getVarianceAmount().abs()
                            .divide(d.getActualAmount().abs(), 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));
                        if (rate.compareTo(threshold) > 0) {
                            results.add(buildResult(rule, d, "偏差" + rate + "%超过阈值" + threshold + "%"));
                        }
                    }
                }
                break;
            case "LOGIC_ERROR":
                BigDecimal totalBudget = sheet.getTotalBudget() != null ? sheet.getTotalBudget() : BigDecimal.ZERO;
                BigDecimal detailSum = details.stream()
                    .map(d -> d.getBudgetAmount() != null ? d.getBudgetAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (detailSum.compareTo(totalBudget) != 0) {
                    results.add(buildResult(rule, null, "明细合计" + detailSum + "与总额" + totalBudget + "不一致"));
                }
                break;
            case "SUBJECT_ERROR":
                // 批量预查询科目编码，避免N+1查询
                Set<String> subjectCodes = details.stream()
                    .map(BudgetPreparationDetail::getSubjectCode)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());
                Set<String> validCodes = Collections.emptySet();
                if (!subjectCodes.isEmpty()) {
                    List<BudgetSubject> subjects = subjectMapper.selectList(
                        Wrappers.<BudgetSubject>lambdaQuery()
                            .in(BudgetSubject::getSubjectCode, subjectCodes)
                            .eq(BudgetSubject::getIsActive, 1));
                    validCodes = subjects.stream()
                        .map(BudgetSubject::getSubjectCode)
                        .collect(Collectors.toSet());
                }
                for (BudgetPreparationDetail d : details) {
                    if (StringUtils.isNotBlank(d.getSubjectCode()) && !validCodes.contains(d.getSubjectCode())) {
                        results.add(buildResult(rule, d, "科目编码" + d.getSubjectCode() + "不存在"));
                    }
                }
                break;
            case "MISSING_DATA":
                for (BudgetPreparationDetail d : details) {
                    if (d.getBudgetAmount() == null || d.getSubjectCode() == null) {
                        results.add(buildResult(rule, d, "必填字段缺失"));
                    }
                }
                break;
            case "FORMULA_CHECK":
                for (BudgetPreparationDetail d : details) {
                    if ("INCOME".equals(d.getSubjectType()) && d.getBudgetAmount() != null
                        && d.getBudgetAmount().compareTo(BigDecimal.ZERO) < 0) {
                        results.add(buildResult(rule, d, "收入类科目金额不能为负"));
                    }
                }
                break;
            default:
                break;
        }
        return results;
    }

    /**
     * 构建校验结果
     */
    private ValidationResultVo buildResult(BudgetValidationRule rule, BudgetPreparationDetail detail, String message) {
        ValidationResultVo vo = new ValidationResultVo();
        vo.setRuleCode(rule.getRuleCode());
        vo.setRuleName(rule.getRuleName());
        vo.setSeverityLevel(rule.getSeverityLevel());
        vo.setMessage(message);
        vo.setPassed(false);
        if (detail != null) {
            vo.setSubjectCode(detail.getSubjectCode());
            vo.setSubjectName(detail.getSubjectName());
            vo.setBudgetAmount(detail.getBudgetAmount());
        }
        return vo;
    }

    /**
     * 分公司打回至部门
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean sendBackToDept(Long id, Long deptId, String reason) {
        BudgetPreparation preparation = baseMapper.selectById(id);
        if (preparation == null) {
            throw new ServiceException("预算编制不存在");
        }
        
        // 校验状态：必须是待审核且审批阶段为分公司
        if (!"Pending_Review".equals(preparation.getStatus()) || !"Branch".equals(preparation.getApprovalStage())) {
            throw new ServiceException("只有分公司待审核状态才能打回部门");
        }
        if (StringUtils.isBlank(reason)) {
            throw new ServiceException("打回理由不能为空");
        }
        
        // 更新状态为待修订
        preparation.setStatus("Pending_Revision");
        preparation.setApprovalStage("None");
        preparation.setRejectLevel("Branch");
        preparation.setRejectReason(reason);
        preparation.setCurrentHandler(LoginHelper.getUsername());
        boolean result = baseMapper.updateById(preparation) > 0;
        
        // 写入驳回历史
        if (result) {
            BudgetRejectHistory history = new BudgetRejectHistory();
            history.setSheetId(id);
            history.setSheetNo(preparation.getSheetNo());
            history.setRejectFromLevel("Branch");
            history.setRejectFromUser(LoginHelper.getUsername());
            history.setRejectFromName(LoginHelper.getNickName());
            history.setRejectToLevel("Dept");
            history.setRejectToDeptId(deptId);
            history.setRejectReason(reason);
            history.setRejectTime(new Date());
            history.setDeadlineTime(calculateDeadline("Branch"));
            rejectHistoryMapper.insert(history);
        }
        
        return result;
    }

    /**
     * 查询驳回历史
     */
    @Override
    public List<BudgetRejectHistoryVo> queryRejectHistory(Long sheetId) {
        LambdaQueryWrapper<BudgetRejectHistory> lqw = Wrappers.lambdaQuery();
        lqw.eq(BudgetRejectHistory::getSheetId, sheetId);
        lqw.orderByDesc(BudgetRejectHistory::getRejectTime);
        return rejectHistoryMapper.selectVoList(lqw);
    }

    /**
     * 计算截止时间
     */
    private Date calculateDeadline(String rejectLevel) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 1);
        cal.set(Calendar.DAY_OF_MONTH, 7);
        if ("HQ".equals(rejectLevel)) {
            cal.set(Calendar.HOUR_OF_DAY, 12);
        } else {
            cal.set(Calendar.HOUR_OF_DAY, 18);
        }
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTime();
    }

    /**
     * 生成预算单号（并发/存量安全）：循环直到找到不存在的单号，避免 uk_sheet_no 冲突
     * 格式：BG-YYYYMM-XXX
     */
    private String generateSheetNo(Integer year, Integer month) {
        String dateStr = String.format("%04d%02d", year, month);
        // 当月已有单据数量作为起始序号（逻辑删除的单据不参与统计，可能产生空洞，由 exists 兜底跳过）
        long count = baseMapper.selectCount(Wrappers.<BudgetPreparation>lambdaQuery()
            .likeRight(BudgetPreparation::getSheetNo, "BG-" + dateStr));
        long seq = count + 1;
        // 兜底：跳过已存在的单号，防止 count 因空洞/并发算出重复序号触发唯一键冲突；上限 1000 张防死循环
        for (int i = 0; i < 1000; i++) {
            String candidate = "BG-" + dateStr + "-" + String.format("%03d", seq);
            boolean exists = baseMapper.exists(Wrappers.<BudgetPreparation>lambdaQuery()
                .eq(BudgetPreparation::getSheetNo, candidate));
            if (!exists) {
                return candidate;
            }
            seq++;
        }
        throw new ServiceException("预算单号生成失败：当月单据数量已达上限");
    }

    /**
     * 查询驳回后重新提交应恢复的审批阶段
     * 驳回后重新提交时不从头开始，而是从被驳回的级别继续审批
     *
     * @param sheetId 预算单ID
     * @return 应恢复的审批阶段（Dept/Branch/HQ），null 表示首次提交
     */
    private String getResubmitStage(Long sheetId) {
        List<BudgetRejectHistory> histories = rejectHistoryMapper.selectList(
            Wrappers.<BudgetRejectHistory>lambdaQuery()
                .eq(BudgetRejectHistory::getSheetId, sheetId)
                .orderByDesc(BudgetRejectHistory::getRejectTime)
                .last("LIMIT 1")
        );
        if (histories != null && !histories.isEmpty()) {
            return histories.get(0).getRejectFromLevel();
        }
        return null;
    }

    /**
     * 将 Flowable 流程实例跳转到指定审批阶段
     * 用于驳回后重新提交时跳过已通过的审批节点，直接到达被驳回的审批级别
     *
     * @param processInstanceId 流程实例ID
     * @param targetStage 目标审批阶段（Branch/HQ）
     */
    private void moveFlowToStage(String processInstanceId, String targetStage) {
        String targetActivityId;
        if ("Branch".equals(targetStage)) {
            targetActivityId = "branchLeaderApproval";
        } else if ("HQ".equals(targetStage)) {
            targetActivityId = "hqLeaderApproval";
        } else {
            return; // Dept 不需要跳转，新流程默认就在部门领导节点
        }

        // 将流程从部门领导审批节点跳转到目标审批节点
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstanceId)
            .moveActivityIdTo("deptLeaderApproval", targetActivityId)
            .changeState();
        log.info("流程实例 {} 已从部门领导审批跳转至 {} 审批", processInstanceId, targetStage);
    }

    /**
     * 完成 Flowable 任务（审批通过或驳回）
     * @param processInstanceId 流程实例ID
     * @param result 审批结果：pass/reject
     * @param comment 审批意见
     */
    private void completeFlowableTask(String processInstanceId, String result, String comment) {
        if (StringUtils.isBlank(processInstanceId)) {
            throw new ServiceException("流程实例ID为空，无法完成审批任务");
        }
        
        Task task = taskService.createTaskQuery()
            .processInstanceId(processInstanceId)
            .singleResult();
            
        if (task == null) {
            throw new ServiceException("未找到待审批的任务");
        }
        
        // 添加任务评论（审批意见）
        if (StringUtils.isNotBlank(comment)) {
            taskService.addComment(task.getId(), processInstanceId, comment);
        }
        
        // 完成任务，设置流程变量 result
        Map<String, Object> variables = new HashMap<>();
        variables.put("result", result);
        taskService.complete(task.getId(), variables);
    }

    /**
     * 查询老板大类预算额度（T5.14 ①）：启用大类主数据 + 当期已录额度 + 单据状态
     * 支持年月+部门双维度（T5.13/T5.14）
     */
    @Override
    public Map<String, Object> getCategoryQuotas(Integer budgetYear, Integer budgetMonth, Long departmentId) {
        Map<String, Object> result = new HashMap<>();
        // 确定目标部门：传入 departmentId 优先，否则取用户所属部门
        Long targetDeptId = departmentId != null ? departmentId : LoginHelper.getDeptId();
        Long orgId = LoginHelper.getDeptId();

        // 当期编制单（同期间+同部门可能多单，取最近更新的一张）
        // 按部门维度隔离：不同部门各自独立编制单（兼容存量记录 department_id 为 NULL 时按 org_id 定位）
        BudgetPreparation preparation = baseMapper.selectOne(Wrappers.<BudgetPreparation>lambdaQuery()
            .and(w -> w.eq(BudgetPreparation::getDepartmentId, targetDeptId)
                .or().eq(BudgetPreparation::getOrgId, targetDeptId))
            .eq(BudgetPreparation::getBudgetYear, budgetYear)
            .eq(BudgetPreparation::getBudgetMonth, budgetMonth)
            .orderByDesc(BudgetPreparation::getUpdateTime)
            .orderByDesc(BudgetPreparation::getId)
            .last("LIMIT 1"));

        // 启用费用大类主数据（三层角色分工：仅返回当前租户已启用的大类）
        List<FeeExpenseCategory> categories = queryTenantEnabledCategories();

        // 已保存额度按大类编码合并到主数据行（大类名称以主数据为准）
        Map<String, BigDecimal> savedAmount = new HashMap<>();
        if (preparation != null && StringUtils.isNotBlank(preparation.getCategoryQuotas())) {
            List<BudgetCategoryQuotaItemVo> parsed =
                JsonUtils.parseArray(preparation.getCategoryQuotas(), BudgetCategoryQuotaItemVo.class);
            if (parsed != null) {
                for (BudgetCategoryQuotaItemVo item : parsed) {
                    if (StringUtils.isNotBlank(item.getCategoryCode())) {
                        savedAmount.put(item.getCategoryCode(), item.getAmount());
                    }
                }
            }
        }

        List<BudgetCategoryQuotaItemVo> quotas = new ArrayList<>();
        for (FeeExpenseCategory category : categories) {
            BudgetCategoryQuotaItemVo item = new BudgetCategoryQuotaItemVo();
            item.setCategoryCode(category.getCategoryCode());
            item.setCategoryName(category.getCategoryName());
            item.setAmount(savedAmount.get(category.getCategoryCode()));
            quotas.add(item);
        }

        // 只有草稿/驳回状态可继续编辑大类额度（与既有编制状态模型一致）
        boolean editable = preparation == null
            || "Draft".equals(preparation.getStatus())
            || "Rejected".equals(preparation.getStatus());
        result.put("preparationId", preparation != null ? preparation.getId() : null);
        result.put("sheetNo", preparation != null ? preparation.getSheetNo() : null);
        result.put("status", preparation != null ? preparation.getStatus() : null);
        result.put("editable", editable);
        result.put("quotas", quotas);
        // 返回当前部门信息（年月+部门维度）
        result.put("departmentId", targetDeptId);
        String deptName = resolveDeptName(targetDeptId);
        result.put("departmentName", deptName);
        return result;
    }

    /**
     * 老板保存大类预算额度（粗粒度，T5.14 ①）
     * 额度以 JSON 挂 budget_preparation 主表，total_budget 同步为大类额度合计；
     * 科目级拆分初稿由后续编制步骤生成，不在此处理（只管粗粒度）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveCategoryQuotas(BudgetCategoryQuotaBo bo) {
        // 1. 对照大类主数据校验：编码必须存在且租户已启用、不可重复；名称以主数据为准回填
        List<FeeExpenseCategory> categories = queryTenantEnabledCategories();
        Map<String, String> codeNameMap = categories.stream()
            .collect(Collectors.toMap(FeeExpenseCategory::getCategoryCode,
                FeeExpenseCategory::getCategoryName, (a, b) -> a));

        Set<String> seenCodes = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        List<BudgetCategoryQuotaItemVo> normalized = new ArrayList<>();
        for (BudgetCategoryQuotaItemVo item : bo.getQuotas()) {
            String code = item.getCategoryCode();
            if (!codeNameMap.containsKey(code)) {
                throw new ServiceException("费用大类不存在或已停用：" + code);
            }
            if (!seenCodes.add(code)) {
                throw new ServiceException("费用大类重复：" + code);
            }
            BudgetCategoryQuotaItemVo row = new BudgetCategoryQuotaItemVo();
            row.setCategoryCode(code);
            row.setCategoryName(codeNameMap.get(code));
            row.setAmount(item.getAmount());
            normalized.add(row);
            total = total.add(item.getAmount());
        }

        // 2. 复用当期编制单；无单则新建草稿单（单号/状态与既有新增逻辑一致）
        // 确定目标部门（年月+部门维度）
        Long targetDeptId = bo.getDepartmentId() != null ? bo.getDepartmentId() : LoginHelper.getDeptId();
        String targetDeptName = resolveDeptName(targetDeptId);
        // 当期编制单：按部门维度隔离，不同部门各自独立编制单
        // 兼容存量记录 department_id 为 NULL 时按 org_id 定位
        BudgetPreparation preparation = baseMapper.selectOne(Wrappers.<BudgetPreparation>lambdaQuery()
            .and(w -> w.eq(BudgetPreparation::getDepartmentId, targetDeptId)
                .or().eq(BudgetPreparation::getOrgId, targetDeptId))
            .eq(BudgetPreparation::getBudgetYear, bo.getBudgetYear())
            .eq(BudgetPreparation::getBudgetMonth, bo.getBudgetMonth())
            .orderByDesc(BudgetPreparation::getUpdateTime)
            .orderByDesc(BudgetPreparation::getId)
            .last("LIMIT 1"));

        String quotasJson = JsonUtils.toJsonString(normalized);
        if (preparation != null) {
            String status = preparation.getStatus();
            if (!"Draft".equals(status) && !"Rejected".equals(status)) {
                throw new ServiceException("预算单[" + preparation.getSheetNo() + "]当前状态不可修改大类额度，如需变更请走预算调整申请");
            }
            preparation.setCategoryQuotas(quotasJson);
            preparation.setTotalBudget(total);
            if (StringUtils.isNotBlank(bo.getRemark())) {
                preparation.setRemark(bo.getRemark());
            }
            baseMapper.updateById(preparation);
            return preparation.getId();
        }

        BudgetPreparation add = new BudgetPreparation();
        add.setOrgId(LoginHelper.getDeptId());
        add.setOrgName(LoginHelper.getLoginUser().getDeptName());
        add.setDepartmentId(targetDeptId);
        add.setBudgetYear(bo.getBudgetYear());
        add.setBudgetMonth(bo.getBudgetMonth());
        add.setSheetNo(generateSheetNo(bo.getBudgetYear(), bo.getBudgetMonth()));
        add.setStatus("Draft");
        add.setApprovalStage("None");
        add.setCurrentHandler(LoginHelper.getUsername());
        add.setTotalBudget(total);
        add.setTotalActual(BigDecimal.ZERO);
        add.setVarianceRate(BigDecimal.ZERO);
        add.setCategoryQuotas(quotasJson);
        add.setRemark(bo.getRemark());
        baseMapper.insert(add);
        return add.getId();
    }

    /**
     * 大类额度按比例拆分到预算科目生成初稿（T5.14 ②）
     * 拆分依据优先级：近3个月历史实绩比例 → 财务预设默认拆分比例 → 均分（设计文档 2.2 #2）；
     * 尾差修正到首行，保证科目明细合计 = 大类额度。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateSplitDraft(Long sheetId) {
        BudgetPreparation preparation = baseMapper.selectById(sheetId);
        if (preparation == null) {
            throw new ServiceException("预算编制单不存在");
        }
        String status = preparation.getStatus();
        if (!"Draft".equals(status) && !"Rejected".equals(status)) {
            throw new ServiceException("只有草稿/驳回状态的编制单可生成拆分初稿");
        }
        if (StringUtils.isBlank(preparation.getCategoryQuotas())) {
            throw new ServiceException("请先录入大类预算额度，再生成拆分初稿");
        }
        List<BudgetCategoryQuotaItemVo> quotas =
            JsonUtils.parseArray(preparation.getCategoryQuotas(), BudgetCategoryQuotaItemVo.class);
        if (quotas == null || quotas.isEmpty()) {
            throw new ServiceException("大类预算额度为空，无法生成拆分初稿");
        }

        // 映射B：启用预算科目按费用大类分组（只拆叶子科目）
        List<BudgetSubject> allSubjects = subjectMapper.selectList(Wrappers.<BudgetSubject>lambdaQuery()
            .eq(BudgetSubject::getIsActive, 1)
            .eq(BudgetSubject::getIsLeaf, 1));
        Map<String, List<BudgetSubject>> subjectsByCategory = allSubjects.stream()
            .filter(s -> StringUtils.isNotBlank(s.getCategoryCode()))
            .collect(Collectors.groupingBy(BudgetSubject::getCategoryCode));

        // 历史实绩：编制期前近3个月已审批通过报销（按科目汇总）
        LocalDate periodStart = LocalDate.of(preparation.getBudgetYear(), preparation.getBudgetMonth(), 1);
        Date sinceDate = Date.from(periodStart.minusMonths(3)
            .atStartOfDay(ZoneId.systemDefault()).toInstant());
        Map<String, BigDecimal> historyBySubject = new HashMap<>();
        List<Map<String, Object>> historyRows = expenseBillMapper
            .sumApprovedExpenseBySubjectSince(preparation.getOrgId(), sinceDate);
        if (historyRows != null) {
            for (Map<String, Object> row : historyRows) {
                String code = getStringVal(row, "subjectCode");
                if (StringUtils.isNotBlank(code)) {
                    historyBySubject.put(code, toBigDecimal(row.get("totalAmount")));
                }
            }
        }

        // 财务预设默认拆分比例（无历史实绩时的兜底）
        List<FeeCategorySplitRatio> allRatios = feeCategorySplitRatioMapper.selectList(
            Wrappers.<FeeCategorySplitRatio>lambdaQuery());
        Map<String, List<FeeCategorySplitRatio>> ratiosByCategory = allRatios.stream()
            .filter(r -> StringUtils.isNotBlank(r.getCategoryCode()))
            .collect(Collectors.groupingBy(FeeCategorySplitRatio::getCategoryCode));

        List<BudgetPreparationDetail> drafts = new ArrayList<>();
        List<String> skippedCategories = new ArrayList<>();
        int sortOrder = 1;
        for (BudgetCategoryQuotaItemVo quotaItem : quotas) {
            BigDecimal quota = quotaItem.getAmount();
            if (quota == null || quota.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            List<BudgetSubject> subjects = subjectsByCategory.get(quotaItem.getCategoryCode());
            if (subjects == null || subjects.isEmpty()) {
                skippedCategories.add(quotaItem.getCategoryCode());
                continue;
            }

            // 确定权重：历史实绩优先 → 默认拆分比例 → 均分
            LinkedHashMap<String, BigDecimal> weights = new LinkedHashMap<>();
            BigDecimal historyTotal = BigDecimal.ZERO;
            for (BudgetSubject s : subjects) {
                BigDecimal h = historyBySubject.getOrDefault(s.getSubjectCode(), BigDecimal.ZERO);
                weights.put(s.getSubjectCode(), h);
                historyTotal = historyTotal.add(h);
            }
            String splitSource;
            if (historyTotal.compareTo(BigDecimal.ZERO) > 0) {
                splitSource = "HISTORY";
            } else {
                List<FeeCategorySplitRatio> ratios = ratiosByCategory.get(quotaItem.getCategoryCode());
                if (ratios != null && !ratios.isEmpty()) {
                    weights.replaceAll((k, v) -> BigDecimal.ZERO);
                    for (FeeCategorySplitRatio r : ratios) {
                        if (weights.containsKey(r.getSubjectCode()) && r.getRatioPercent() != null) {
                            weights.put(r.getSubjectCode(), r.getRatioPercent());
                        }
                    }
                    splitSource = "RATIO";
                } else {
                    weights.replaceAll((k, v) -> BigDecimal.ONE);
                    splitSource = "EVEN";
                }
            }
            BigDecimal weightTotal = weights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            if (weightTotal.compareTo(BigDecimal.ZERO) <= 0) {
                weights.replaceAll((k, v) -> BigDecimal.ONE);
                weightTotal = new BigDecimal(weights.size());
                splitSource = "EVEN";
            }

            // 按权重拆金额（保留2位），尾差修正到首行保证合计=额度
            Map<String, BudgetSubject> subjectMap = subjects.stream()
                .collect(Collectors.toMap(BudgetSubject::getSubjectCode, s -> s, (a, b) -> a));
            BigDecimal allocated = BigDecimal.ZERO;
            List<BudgetPreparationDetail> catDrafts = new ArrayList<>();
            for (Map.Entry<String, BigDecimal> e : weights.entrySet()) {
                BigDecimal amount = quota.multiply(e.getValue())
                    .divide(weightTotal, 2, RoundingMode.HALF_UP);
                BudgetSubject subject = subjectMap.get(e.getKey());
                BudgetPreparationDetail detail = new BudgetPreparationDetail();
                detail.setSheetId(sheetId);
                detail.setSubjectCode(subject.getSubjectCode());
                detail.setSubjectName(subject.getSubjectName());
                detail.setSubjectType(subject.getSubjectType());
                detail.setBudgetAmount(amount);
                detail.setActualAmount(BigDecimal.ZERO);
                detail.setDeptId(preparation.getDepartmentId() != null ? preparation.getDepartmentId() : preparation.getOrgId());
                detail.setDeptName(preparation.getDepartmentId() != null ? resolveDeptName(preparation.getDepartmentId()) : preparation.getOrgName());
                detail.setSortOrder(sortOrder++);
                catDrafts.add(detail);
                allocated = allocated.add(amount);
            }
            BigDecimal diff = quota.subtract(allocated);
            if (diff.compareTo(BigDecimal.ZERO) != 0 && !catDrafts.isEmpty()) {
                BudgetPreparationDetail first = catDrafts.get(0);
                first.setBudgetAmount(first.getBudgetAmount().add(diff));
            }
            drafts.addAll(catDrafts);
            log.info("拆分初稿: 大类={}, 额度={}, 科目数={}, 拆分依据={}",
                quotaItem.getCategoryCode(), quota, catDrafts.size(), splitSource);
        }

        if (drafts.isEmpty()) {
            throw new ServiceException("未生成任何拆分初稿：请确认预算科目已启用且已配置费用大类（映射B）");
        }

        // 先删后插覆盖旧明细，保持与大类额度一致（初稿可由财务继续微调）
        detailMapper.delete(Wrappers.<BudgetPreparationDetail>lambdaQuery()
            .eq(BudgetPreparationDetail::getSheetId, sheetId));
        for (BudgetPreparationDetail detail : drafts) {
            detailMapper.insert(detail);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("sheetId", sheetId);
        result.put("detailCount", drafts.size());
        result.put("skippedCategories", skippedCategories);
        result.put("details", drafts);
        return result;
    }

    /**
     * 获取上月已审批通过的报销实际数据或预算编制数据（用于初始化本月预算金额）
     * 优先取报销单，无报销单时回退到上月预算编制科目金额
     * 若有报销单但只覆盖部分科目，则无报销单的科目从上期预算编制金额补充
     *
     * @param orgId 部门ID
     * @param budgetYear 预算年度（上月，已由前端计算好）
     * @param budgetMonth 预算月份（上月，已由前端计算好）
     * @return Map包含 dataSource(EXPENSE/BUDGET/MIXED/NONE) + details(明细列表)
     */
    @Override
    public Map<String, Object> getPreviousMonthApprovedDetails(Long orgId, Integer budgetYear, Integer budgetMonth) {
        log.info("[上月数据] 查询参数: orgId={}, prevYear={}, prevMonth={}", orgId, budgetYear, budgetMonth);
        Map<String, Object> result = new HashMap<>();

        // === 第一步：查询上月本部门已审批通过的报销单，按预算科目汇总金额 ===
        List<Map<String, Object>> aggregated = expenseBillMapper.sumApprovedExpenseGroupBySubject(
            orgId, budgetYear, budgetMonth);
        log.info("[上月报销数据] 查询结果: {} 条科目汇总记录", aggregated != null ? aggregated.size() : 0);

        // === 第二步：查询上月本部门已审批通过的预算编制记录（用于补充无报销单的科目） ===
        List<BudgetPreparationDetail> prevBudgetDetails = queryPreviousMonthBudgetDetails(orgId, budgetYear, budgetMonth);
        log.info("[上月预算数据] 查询结果: {} 条科目记录", prevBudgetDetails != null ? prevBudgetDetails.size() : 0);

        // === 第三步：按科目合并，优先报销金额，无报销则用上月预算金额 ===
        boolean hasExpense = aggregated != null && !aggregated.isEmpty();
        boolean hasBudget = prevBudgetDetails != null && !prevBudgetDetails.isEmpty();

        if (!hasExpense && !hasBudget) {
            log.info("[上月数据] 报销单和预算编制均无数据");
            result.put("dataSource", "NONE");
            result.put("details", Collections.emptyList());
            return result;
        }

        // 用 subjectCode 作为合并 key，报销优先覆盖预算
        Map<String, BudgetPreparationDetailVo> mergedMap = new LinkedHashMap<>();

        // 先放入上月预算数据（低优先级）
        if (hasBudget) {
            for (BudgetPreparationDetail d : prevBudgetDetails) {
                BudgetPreparationDetailVo vo = new BudgetPreparationDetailVo();
                vo.setSubjectCode(d.getSubjectCode());
                vo.setSubjectName(d.getSubjectName());
                vo.setSubjectType(d.getSubjectType());
                vo.setBudgetAmount(d.getBudgetAmount());
                mergedMap.put(d.getSubjectCode(), vo);
            }
        }

        // 再用报销数据覆盖（高优先级）
        if (hasExpense) {
            for (Map<String, Object> row : aggregated) {
                String subjectCode = getStringVal(row, "subjectCode");
                BudgetPreparationDetailVo vo = new BudgetPreparationDetailVo();
                vo.setSubjectCode(subjectCode);
                vo.setSubjectName(getStringVal(row, "subjectName"));
                vo.setSubjectType(getStringVal(row, "subjectType"));
                vo.setBudgetAmount(toBigDecimal(row.get("totalAmount")));
                mergedMap.put(subjectCode, vo);
            }
        }

        List<BudgetPreparationDetailVo> details = new ArrayList<>(mergedMap.values());

        // 确定数据源标签
        String dataSource;
        if (hasExpense && hasBudget) {
            dataSource = "MIXED";
        } else if (hasExpense) {
            dataSource = "EXPENSE";
        } else {
            dataSource = "BUDGET";
        }
        result.put("dataSource", dataSource);
        result.put("details", details);
        log.info("[上月数据] 合并返回 {} 条科目数据，数据源: {}", details.size(), dataSource);
        return result;
    }

    /**
     * 查询上月本部门已审批通过的预算编制明细
     *
     * @return 明细列表，无数据时返回 null
     */
    private List<BudgetPreparationDetail> queryPreviousMonthBudgetDetails(Long orgId, Integer budgetYear, Integer budgetMonth) {
        LambdaQueryWrapper<BudgetPreparation> lqw = Wrappers.lambdaQuery();
        lqw.eq(BudgetPreparation::getOrgId, orgId);
        lqw.eq(BudgetPreparation::getBudgetYear, budgetYear);
        lqw.eq(BudgetPreparation::getBudgetMonth, budgetMonth);
        lqw.eq(BudgetPreparation::getStatus, "Approved");
        lqw.orderByDesc(BudgetPreparation::getUpdateTime);
        lqw.last("LIMIT 1");

        BudgetPreparation prevPreparation = baseMapper.selectOne(lqw);
        if (prevPreparation == null) {
            log.info("[上月预算数据] 未找到上月已审批的预算编制记录");
            return null;
        }
        log.info("[上月预算数据] 找到上月预算单ID: {}", prevPreparation.getId());

        return detailMapper.selectList(
            Wrappers.<BudgetPreparationDetail>lambdaQuery()
                .eq(BudgetPreparationDetail::getSheetId, prevPreparation.getId())
        );
    }

    /**
     * 从 Map 中安全取字符串值
     */
    private String getStringVal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    /**
     * 将对象安全转为 BigDecimal
     */
    private BigDecimal toBigDecimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal) {
            return (BigDecimal) v;
        }
        return new BigDecimal(v.toString());
    }

    /**
     * 查询当前租户已启用的费用大类（三层角色分工：admin 主数据 ∩ platform_admin 勾选）
     * 存量兼容：租户无配置记录时视为全部启用
     */
    private List<FeeExpenseCategory> queryTenantEnabledCategories() {
        List<FeeTenantCategory> tenantCategories = feeTenantCategoryMapper.selectList(
            Wrappers.<FeeTenantCategory>lambdaQuery()
                .eq(FeeTenantCategory::getIsEnabled, 1));
        boolean hasConfig = feeTenantCategoryMapper.selectCount(
            Wrappers.<FeeTenantCategory>lambdaQuery().select(FeeTenantCategory::getId)) > 0;
        if (!hasConfig) {
            // 存量兼容：无配置时全部启用
            return feeExpenseCategoryMapper.selectList(
                Wrappers.<FeeExpenseCategory>lambdaQuery()
                    .eq(FeeExpenseCategory::getIsActive, 1)
                    .orderByAsc(FeeExpenseCategory::getSortOrder)
                    .orderByAsc(FeeExpenseCategory::getId));
        }
        Set<String> enabledCodes = tenantCategories.stream()
            .map(FeeTenantCategory::getCategoryCode)
            .collect(Collectors.toSet());
        return feeExpenseCategoryMapper.selectList(
            Wrappers.<FeeExpenseCategory>lambdaQuery()
                .eq(FeeExpenseCategory::getIsActive, 1)
                .in(FeeExpenseCategory::getCategoryCode, enabledCodes)
                .orderByAsc(FeeExpenseCategory::getSortOrder)
                .orderByAsc(FeeExpenseCategory::getId));
    }

    /**
     * 根据部门ID解析部门名称（年月+部门维度辅助方法）
     */
    private String resolveDeptName(Long deptId) {
        if (deptId == null) {
            return LoginHelper.getLoginUser().getDeptName();
        }
        try {
            SysDept dept = sysDeptMapper.selectById(deptId);
            if (dept != null) {
                return dept.getDeptName();
            }
        } catch (Exception e) {
            log.warn("解析部门名称失败: deptId={}, error={}", deptId, e.getMessage());
        }
        return LoginHelper.getLoginUser().getDeptName();
    }
}
