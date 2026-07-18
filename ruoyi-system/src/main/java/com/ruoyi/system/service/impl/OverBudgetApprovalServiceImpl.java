package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.system.domain.BudgetAdjustmentLog;
import com.ruoyi.system.domain.ExpenseBill;
import com.ruoyi.system.domain.OverBudgetApproval;
import com.ruoyi.system.domain.bo.OverBudgetApprovalBo;
import com.ruoyi.system.domain.vo.BudgetAdjustmentLogVo;
import com.ruoyi.system.domain.vo.OverBudgetApprovalVo;
import com.ruoyi.system.mapper.BudgetAdjustmentLogMapper;
import com.ruoyi.system.mapper.BudgetPreparationDetailMapper;
import com.ruoyi.system.mapper.ExpenseBillMapper;
import com.ruoyi.system.mapper.OverBudgetApprovalMapper;
import com.ruoyi.system.service.IFeeOperationLogService;
import com.ruoyi.system.service.IOverBudgetApprovalService;
import com.ruoyi.system.service.ISysMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 超预算审批Service业务层处理
 *
 * @author ruoyi
 * @date 2026-07-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OverBudgetApprovalServiceImpl implements IOverBudgetApprovalService {

    private final OverBudgetApprovalMapper approvalMapper;
    private final BudgetAdjustmentLogMapper adjustmentLogMapper;
    private final ExpenseBillMapper expenseBillMapper;
    private final BudgetPreparationDetailMapper budgetDetailMapper;
    private final IFeeOperationLogService feeOperationLogService;
    private final ISysMessageService sysMessageService;

    @Override
    public Page<OverBudgetApprovalVo> selectPageList(OverBudgetApprovalBo bo, PageQuery pageQuery) {
        Wrapper<OverBudgetApproval> wrapper = buildQueryWrapper(bo);
        return approvalMapper.selectVoPage(pageQuery.build(), wrapper);
    }

    @Override
    public OverBudgetApprovalVo queryById(Long id) {
        return approvalMapper.selectVoById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean processApproval(Long approvalId, String action, String adjustFromSubject,
                                   BigDecimal adjustAmount, String opinion) {
        OverBudgetApproval approval = approvalMapper.selectById(approvalId);
        if (approval == null) {
            throw new ServiceException("审批单不存在");
        }
        if (!"PENDING".equals(approval.getStatus())) {
            throw new ServiceException("该审批单已处理");
        }

        String beforeStatus = approval.getStatus();

        switch (action) {
            case "APPROVE":
                doApprove(approval, opinion, beforeStatus);
                break;
            case "REJECT":
                doReject(approval, opinion, beforeStatus);
                break;
            case "ADJUST":
                doAdjust(approval, adjustFromSubject, adjustAmount, opinion, beforeStatus);
                break;
            default:
                throw new ServiceException("无效的审批动作");
        }

        // 清除预算余额缓存
        RedisUtils.deleteKeys("fee:budget_balance:*");
        return true;
    }

    @Override
    public List<BudgetAdjustmentLogVo> queryAdjustmentLog(Long approvalId) {
        return adjustmentLogMapper.selectVoList(
            Wrappers.<BudgetAdjustmentLog>lambdaQuery()
                .eq(BudgetAdjustmentLog::getRelatedApprovalId, approvalId)
                .orderByDesc(BudgetAdjustmentLog::getOperateTime));
    }

    /**
     * 审批通过：追加科目预算额度
     */
    private void doApprove(OverBudgetApproval approval, String opinion, String beforeStatus) {
        // 获取报销单信息以拿到 deptId
        ExpenseBill bill = expenseBillMapper.selectById(approval.getBillId());
        Long billDeptId = (bill != null) ? bill.getDeptId() : null;

        // 追加科目预算（按部门+期间隔离）
        java.time.LocalDate now = java.time.LocalDate.now();
        budgetDetailMapper.addBudgetBySubject(
            approval.getBudgetSubject(), billDeptId,
            approval.getShortageAmount(), now.getYear(), now.getMonthValue());

        // 更新报销单状态
        if (bill != null) {
            bill.setStatus("APPROVED");
            bill.setIsOverBudget(1);
            bill.setOverBudgetApprovalId(approval.getId());
            expenseBillMapper.updateById(bill);
        }

        approval.setApprovalAction("APPROVE");
        approval.setApprovalOpinion(opinion);
        approval.setApproverId(LoginHelper.getUserId());
        approval.setApproverName(LoginHelper.getNickName());
        approval.setApprovalTime(new Date());
        approval.setStatus("APPROVED");
        approvalMapper.updateById(approval);

        // 操作日志
        Map<String, Object> detail = new HashMap<>();
        detail.put("shortageAmount", approval.getShortageAmount());
        detail.put("budgetSubject", approval.getBudgetSubject());
        feeOperationLogService.log("OVER_BUDGET", "OB_APPROVE", approval.getBillId(),
            approval.getBillNo(), approval.getId(), beforeStatus, "APPROVED", JSONUtil.toJsonStr(detail));

        // 通知报销人
        if (bill != null) {
            sysMessageService.sendMessage(bill.getApplicantId(), "超预算审批通过",
                "您的超预算报销单（" + approval.getBillNo() + "）已通过，追加预算 ¥" + approval.getShortageAmount(),
                "APPROVAL_RESULT", "OVER_BUDGET", approval.getId());
        }
    }

    /**
     * 审批驳回
     */
    private void doReject(OverBudgetApproval approval, String reason, String beforeStatus) {
        // 退回报销单
        expenseBillMapper.updateStatusById(approval.getBillId(), "REJECTED");

        approval.setApprovalAction("REJECT");
        approval.setApprovalOpinion(reason);
        approval.setApproverId(LoginHelper.getUserId());
        approval.setApproverName(LoginHelper.getNickName());
        approval.setApprovalTime(new Date());
        approval.setStatus("REJECTED");
        approvalMapper.updateById(approval);

        // 操作日志
        feeOperationLogService.log("OVER_BUDGET", "OB_REJECT", approval.getBillId(),
            approval.getBillNo(), approval.getId(), beforeStatus, "REJECTED",
            JSONUtil.toJsonStr(Collections.singletonMap("rejectReason", reason)));

        // 通知报销人
        ExpenseBill bill = expenseBillMapper.selectById(approval.getBillId());
        if (bill != null) {
            sysMessageService.sendMessage(bill.getApplicantId(), "超预算审批驳回",
                "您的超预算报销单（" + approval.getBillNo() + "）被驳回，原因：" + reason,
                "REJECTION", "OVER_BUDGET", approval.getId());
        }
    }

    /**
     * 调整科目预算：从调出科目调剂到调入科目
     */
    private void doAdjust(OverBudgetApproval approval, String adjustFromSubject,
                          BigDecimal adjustAmount, String opinion, String beforeStatus) {
        validateAdjustment(adjustFromSubject, adjustAmount, approval);

        // 执行调剂
        budgetDetailMapper.transferBudget(adjustFromSubject,
            approval.getBudgetSubject(), approval.getCostCenterGroup(), adjustAmount);

        // 记录调剂流水
        BudgetAdjustmentLog adjLog = new BudgetAdjustmentLog();
        adjLog.setCostCenterCode(approval.getCostCenterGroup());
        adjLog.setFromSubject(adjustFromSubject);
        adjLog.setToSubject(approval.getBudgetSubject());
        adjLog.setAdjustAmount(adjustAmount);
        adjLog.setRelatedApprovalId(approval.getId());
        adjLog.setRelatedBillNo(approval.getBillNo());
        adjLog.setOperatorId(LoginHelper.getUserId());
        adjLog.setOperatorName(LoginHelper.getNickName());
        adjLog.setOperateTime(new Date());
        adjustmentLogMapper.insert(adjLog);

        // 更新报销单状态
        ExpenseBill bill = expenseBillMapper.selectById(approval.getBillId());
        if (bill != null) {
            bill.setStatus("APPROVED");
            bill.setIsOverBudget(1);
            bill.setOverBudgetApprovalId(approval.getId());
            expenseBillMapper.updateById(bill);
        }

        approval.setApprovalAction("ADJUST");
        approval.setAdjustFromSubject(adjustFromSubject);
        approval.setAdjustAmount(adjustAmount);
        approval.setApprovalOpinion(opinion);
        approval.setApproverId(LoginHelper.getUserId());
        approval.setApproverName(LoginHelper.getNickName());
        approval.setApprovalTime(new Date());
        approval.setStatus("APPROVED");
        approvalMapper.updateById(approval);

        // 操作日志
        Map<String, Object> detail = new HashMap<>();
        detail.put("fromSubject", adjustFromSubject);
        detail.put("toSubject", approval.getBudgetSubject());
        detail.put("adjustAmount", adjustAmount);
        feeOperationLogService.log("OVER_BUDGET", "OB_ADJUST", approval.getBillId(),
            approval.getBillNo(), approval.getId(), beforeStatus, "APPROVED", JSONUtil.toJsonStr(detail));

        // 通知报销人
        if (bill != null) {
            sysMessageService.sendMessage(bill.getApplicantId(), "预算调剂完成",
                "预算已调剂至科目" + approval.getBudgetSubject() + "，金额 ¥" + adjustAmount,
                "APPROVAL_RESULT", "OVER_BUDGET", approval.getId());
        }
    }

    /**
     * 校验调剂合法性
     */
    private void validateAdjustment(String adjustFromSubject, BigDecimal adjustAmount,
                                    OverBudgetApproval approval) {
        if (adjustFromSubject == null || adjustAmount == null) {
            throw new ServiceException("调整科目预算时，调出科目和调剂金额必填");
        }
        // 获取报销单部门ID
        ExpenseBill bill = expenseBillMapper.selectById(approval.getBillId());
        Long billDeptId = (bill != null) ? bill.getDeptId() : null;

        java.time.LocalDate now = java.time.LocalDate.now();
        BigDecimal fromRemaining = budgetDetailMapper.getRemainingBudgetByPeriod(
            adjustFromSubject, billDeptId, now.getYear(), now.getMonthValue());
        if (fromRemaining.compareTo(adjustAmount) < 0) {
            throw new ServiceException("调出科目可用余额不足");
        }
        BigDecimal fromTotal = budgetDetailMapper.getTotalBudget(
            adjustFromSubject, billDeptId);
        BigDecimal minRemaining = fromTotal.multiply(new BigDecimal("0.1"));
        if (fromRemaining.subtract(adjustAmount).compareTo(minRemaining) < 0) {
            throw new ServiceException("调出科目调剂后剩余金额不得低于预算的10%");
        }
    }

    /**
     * 构建查询条件
     */
    private Wrapper<OverBudgetApproval> buildQueryWrapper(OverBudgetApprovalBo bo) {
        return Wrappers.<OverBudgetApproval>lambdaQuery()
            .eq(StrUtil.isNotBlank(bo.getStatus()), OverBudgetApproval::getStatus, bo.getStatus())
            .orderByDesc(OverBudgetApproval::getCreateTime);
    }
}
