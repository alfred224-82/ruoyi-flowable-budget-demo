package com.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.domain.bo.BudgetPreparationBo;
import com.ruoyi.system.domain.vo.*;
import com.ruoyi.system.config.BudgetProcessDeployer;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.IBudgetPreparationService;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 预算编制主表Service业务层处理
 *
 * @author ruoyi
 * @date 2026-06-19
 */
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
            // 校验：只有草稿和驳回状态可以删除
            for (Long id : ids) {
                BudgetPreparation preparation = baseMapper.selectById(id);
                if (preparation != null) {
                    String status = preparation.getStatus();
                    if (!"Draft".equals(status) && !"Rejected".equals(status)) {
                        throw new ServiceException("预算单号[" + preparation.getSheetNo() + "]不是草稿或驳回状态，不能删除");
                    }
                }
            }
        }
        return baseMapper.deleteBatchIds(ids) > 0;
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
        
        // 校验状态：只有草稿和驳回状态可以提交
        String status = preparation.getStatus();
        if (!"Draft".equals(status) && !"Rejected".equals(status)) {
            throw new ServiceException("预算编制状态不是草稿或驳回，不能提交审核");
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
        
        // 启动 Flowable 审批流程
        startApprovalProcess(preparation);
        
        // 更新状态为待部门领导审核（第1级）
        preparation.setStatus("Pending_Dept_Review");
        preparation.setRejectLevel("None");
        preparation.setRejectReason(null);
        
        return baseMapper.updateById(preparation) > 0;
    }

    /**
     * 启动预算审批流程
     */
    private void startApprovalProcess(BudgetPreparation preparation) {
        try {
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
                BudgetProcessDeployer.PROCESS_KEY,
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
        // 校验状态：只有三级待审核状态可以审批
        if (!isPendingReviewStatus(status)) {
            throw new ServiceException("预算编制状态不是待审核，不能审批");
        }
        
        // 完成 Flowable 任务（通过）
        completeFlowableTask(preparation.getProcessInstanceId(), "pass", remark);
        
        // 状态流转：部门领导 → 分公司领导 → 总公司领导 → 已通过
        if ("Pending_Dept_Review".equals(status)) {
            preparation.setStatus("Pending_Branch_Review");
            preparation.setCurrentHandler("branch_leader");
        } else if ("Pending_Branch_Review".equals(status)) {
            preparation.setStatus("Pending_HQ_Review");
            preparation.setCurrentHandler("hq_leader");
        } else if ("Pending_HQ_Review".equals(status)) {
            preparation.setStatus("Approved");
            preparation.setCurrentHandler(null);
        }
        
        if (StringUtils.isNotBlank(remark)) {
            preparation.setRemark(remark);
        }
        
        return baseMapper.updateById(preparation) > 0;
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
        // 校验状态：只有三级待审核状态可以驳回
        if (!isPendingReviewStatus(status)) {
            throw new ServiceException("预算编制状态不是待审核，不能驳回");
        }
        
        // 校验驳回理由
        if (StringUtils.isBlank(reason)) {
            throw new ServiceException("驳回时必须填写驳回意见");
        }
        
        // 完成 Flowable 任务（驳回）
        completeFlowableTask(preparation.getProcessInstanceId(), "reject", reason);
        
        // 驳回后状态回退到草稿，由编制人员重新修改提交
        preparation.setStatus("Draft");
        preparation.setRejectReason(reason);
        preparation.setRejectLevel(getRejectLevelFromStatus(status));
        preparation.setCurrentHandler(LoginHelper.getUsername());
        boolean result = baseMapper.updateById(preparation) > 0;
        
        // 写入驳回历史
        if (result) {
            BudgetRejectHistory history = new BudgetRejectHistory();
            history.setSheetId(id);
            history.setSheetNo(preparation.getSheetNo());
            history.setRejectFromLevel(getRejectLevelFromStatus(status));
            history.setRejectFromUser(LoginHelper.getUsername());
            history.setRejectFromName(LoginHelper.getNickName());
            history.setRejectToLevel("Dept");
            history.setRejectReason(reason);
            history.setRejectTime(new Date());
            history.setDeadlineTime(calculateDeadline(getRejectLevelFromStatus(status)));
            rejectHistoryMapper.insert(history);
        }
        
        return result;
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
        
        // 校验状态：必须是 Branch_Pending
        if (!"Branch_Pending".equals(preparation.getStatus())) {
            throw new ServiceException("只有分公司待处理状态才能打回部门");
        }
        if (StringUtils.isBlank(reason)) {
            throw new ServiceException("打回理由不能为空");
        }
        
        // 更新状态
        preparation.setStatus("Pending_Revision");
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
     * 生成预算单号
     * 格式：BG-YYYYMM-XXX
     */
    private String generateSheetNo(Integer year, Integer month) {
        String dateStr = String.format("%04d%02d", year, month);
        
        // 查询当月已有的单据数量
        LambdaQueryWrapper<BudgetPreparation> lqw = Wrappers.lambdaQuery();
        lqw.likeRight(BudgetPreparation::getSheetNo, "BG-" + dateStr);
        long count = baseMapper.selectCount(lqw);
        
        // 生成序号（3位）
        String seqNo = String.format("%03d", count + 1);
        
        return "BG-" + dateStr + "-" + seqNo;
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
     * 判断是否为待审核状态（三级审批中的任意一级）
     */
    private boolean isPendingReviewStatus(String status) {
        return "Pending_Dept_Review".equals(status) 
            || "Pending_Branch_Review".equals(status) 
            || "Pending_HQ_Review".equals(status);
    }

    /**
     * 根据当前状态获取驳回来源级别
     */
    private String getRejectLevelFromStatus(String status) {
        if ("Pending_Dept_Review".equals(status)) {
            return "Dept";
        } else if ("Pending_Branch_Review".equals(status)) {
            return "Branch";
        } else if ("Pending_HQ_Review".equals(status)) {
            return "HQ";
        }
        return "Unknown";
    }

    /**
     * 获取上月已审批通过的预算明细（用于初始化本月预算金额）
     * 实现「上月实绩驱动下月预算」：取本部门上月审核通过的科目金额作为本月初始值
     *
     * @param orgId 部门ID
     * @param budgetYear 预算年度
     * @param budgetMonth 预算月份
     * @return 上月已审批的预算明细列表
     */
    @Override
    public List<BudgetPreparationDetailVo> getPreviousMonthApprovedDetails(Long orgId, Integer budgetYear, Integer budgetMonth) {
        // 计算上月的年度和月份
        Integer prevYear = budgetYear;
        Integer prevMonth = budgetMonth - 1;
        if (prevMonth <= 0) {
            prevMonth = 12;
            prevYear = budgetYear - 1;
        }

        // 查询上月本部门已审批通过的编制记录（状态为 Approved）
        LambdaQueryWrapper<BudgetPreparation> lqw = Wrappers.lambdaQuery();
        lqw.eq(BudgetPreparation::getOrgId, orgId);
        lqw.eq(BudgetPreparation::getBudgetYear, prevYear);
        lqw.eq(BudgetPreparation::getBudgetMonth, prevMonth);
        lqw.eq(BudgetPreparation::getStatus, "Approved");
        lqw.orderByDesc(BudgetPreparation::getUpdateTime);
        lqw.last("LIMIT 1");

        BudgetPreparation prevPreparation = baseMapper.selectOne(lqw);
        if (prevPreparation == null) {
            return Collections.emptyList();
        }

        // 查询该编制记录的明细数据
        List<BudgetPreparationDetail> prevDetails = detailMapper.selectList(
            Wrappers.<BudgetPreparationDetail>lambdaQuery()
                .eq(BudgetPreparationDetail::getSheetId, prevPreparation.getId())
        );

        // 转换为 VO 列表
        List<BudgetPreparationDetailVo> result = new ArrayList<>();
        for (BudgetPreparationDetail detail : prevDetails) {
            BudgetPreparationDetailVo vo = new BudgetPreparationDetailVo();
            vo.setSubjectCode(detail.getSubjectCode());
            vo.setSubjectName(detail.getSubjectName());
            vo.setSubjectType(detail.getSubjectType());
            vo.setBudgetAmount(detail.getBudgetAmount());
            result.add(vo);
        }

        return result;
    }
}
