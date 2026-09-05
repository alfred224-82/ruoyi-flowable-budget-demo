package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.service.FeatureChecker;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.ExpenseBill;
import com.ruoyi.system.domain.vo.BatchApprovalResultVo;
import com.ruoyi.system.domain.vo.ExpenseBillVo;
import com.ruoyi.system.mapper.ExpenseBillMapper;
import com.ruoyi.system.service.IEmployeeCreditService;
import com.ruoyi.system.service.IExpenseApprovalService;
import com.ruoyi.system.service.IFeeOperationLogService;
import com.ruoyi.system.service.ISysMessageService;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 报销单审批Service业务层处理
 *
 * @author ruoyi
 * @date 2026-07-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseApprovalServiceImpl implements IExpenseApprovalService {

    private final ExpenseBillMapper expenseBillMapper;
    private final IFeeOperationLogService feeOperationLogService;
    private final ISysMessageService sysMessageService;
    // M6 T6.3 信用体系：优秀等级后补单免老板审批（仅财务审）
    private final IEmployeeCreditService creditService;
    // T6.7 证据链分级筛选开关门控
    private final FeatureChecker featureChecker;
    // T6.7 批量审核：经代理调用单条事务方法，避免 self-invocation 事务失效
    // 使用 Setter 注入 + @Lazy 打破自引用循环依赖
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private IExpenseApprovalService self;
    
    @Override
    public Page<ExpenseBillVo> selectPendingApprovalPage(PageQuery pageQuery) {
        return selectPendingApprovalPage(pageQuery, null);
    }
    
    @Override
    public Page<ExpenseBillVo> selectPendingApprovalPage(PageQuery pageQuery, String evidenceLevel) {
        Set<String> roleKeys = LoginHelper.getLoginUser().getRolePermission();
    
        QueryWrapper<ExpenseBill> wrapper = Wrappers.<ExpenseBill>query()
            .eq("e.status", "SUBMITTED");
    
        // 老板/部门经理可审批（资金管控体系老板为终审人，T5.8）；多角色用集合判断避免单角色取值缺陷；
        // deptManager 只看本部门，boss/admin 等上级角色可看全部，其余角色仅看本人单据兜底
        if (roleKeys == null || (!roleKeys.contains("boss") && !roleKeys.contains("deptManager")
            && !roleKeys.contains("finance") && !roleKeys.contains("admin"))) {
            wrapper.eq("e.applicant_id", LoginHelper.getUserId());
        } else if (roleKeys.contains("deptManager") && !roleKeys.contains("boss") && !roleKeys.contains("admin")) {
            Long deptId = LoginHelper.getDeptId();
            wrapper.eq("e.dept_id", deptId);
        }
        // T6.7 证据链分级：开关开启时支持按证据等级筛选异常单据（开关关闭时忽略入参）
        if (featureChecker.isFeatureEnabled("FEAT_EVIDENCE_CHAIN") && StrUtil.isNotBlank(evidenceLevel)) {
            wrapper.eq("e.evidence_level", evidenceLevel);
        }
    
        return expenseBillMapper.selectExpenseBillPage(pageQuery.build(), wrapper);
    }
    
    @Override
    public BatchApprovalResultVo batchApprove(List<Long> billIds, String opinion) {
        if (billIds == null || billIds.isEmpty()) {
            throw new ServiceException("请选择需要审批的报销单");
        }
        BatchApprovalResultVo result = new BatchApprovalResultVo();
        for (Long billId : billIds) {
            try {
                self.approve(billId, opinion);
                result.setSuccessNum(result.getSuccessNum() + 1);
            } catch (Exception e) {
                log.warn("批量审批失败，单据ID：{}，原因：{}", billId, e.getMessage());
                result.getFailures().add("单据[" + billId + "]：" + e.getMessage());
            }
        }
        return result;
    }
    
    @Override
    public BatchApprovalResultVo batchReject(List<Long> billIds, String reason, String opinion) {
        if (billIds == null || billIds.isEmpty()) {
            throw new ServiceException("请选择需要驳回的报销单");
        }
        BatchApprovalResultVo result = new BatchApprovalResultVo();
        for (Long billId : billIds) {
            try {
                self.reject(billId, reason, opinion);
                result.setSuccessNum(result.getSuccessNum() + 1);
            } catch (Exception e) {
                log.warn("批量驳回失败，单据ID：{}，原因：{}", billId, e.getMessage());
                result.getFailures().add("单据[" + billId + "]：" + e.getMessage());
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean approve(Long billId, String opinion) {
        ExpenseBill bill = expenseBillMapper.selectById(billId);
        if (bill == null) {
            throw new ServiceException("报销单不存在");
        }
        if (!"SUBMITTED".equals(bill.getStatus())) {
            throw new ServiceException("报销单当前状态不允许审批");
        }
        // 校验审批权限：仅部门经理或admin
        validateApprover(bill);

        String beforeStatus = bill.getStatus();
        bill.setStatus("APPROVED");
        bill.setApproverId(LoginHelper.getUserId());
        bill.setApproveTime(new Date());
        // T6.1 审批通过即实扣提交人部门额度（已用金额按部门汇总统计）
        bill.setDeptBudgetDeducted(1);
        expenseBillMapper.updateById(bill);

        // 写操作日志
        Map<String, Object> detail = new HashMap<>();
        detail.put("opinion", opinion);
        detail.put("amount", bill.getAmount());
        feeOperationLogService.log("EXPENSE", "APPROVE", billId, bill.getBillNo(),
            null, beforeStatus, "APPROVED", JSONUtil.toJsonStr(detail));

        // 清除预算余额缓存
        RedisUtils.deleteKeys("fee:budget_balance:*");

        // 发送站内通知给报销人
        sysMessageService.sendMessage(bill.getApplicantId(), "报销单审批通过",
            "您的报销单（" + bill.getBillNo() + "）已通过审批，金额：¥" + bill.getAmount(),
            "APPROVAL_RESULT", "EXPENSE", billId);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean reject(Long billId, String reason, String opinion) {
        if (StrUtil.isBlank(reason)) {
            throw new ServiceException("驳回原因不能为空");
        }
        if (reason.length() < 5 || reason.length() > 500) {
            throw new ServiceException("驳回原因需5-500字符");
        }

        ExpenseBill bill = expenseBillMapper.selectById(billId);
        if (bill == null) {
            throw new ServiceException("报销单不存在");
        }
        if (!"SUBMITTED".equals(bill.getStatus())) {
            throw new ServiceException("报销单当前状态不允许审批");
        }
        validateApprover(bill);

        String beforeStatus = bill.getStatus();
        bill.setStatus("REJECTED");
        bill.setRejectReason(reason);
        bill.setRejectTime(new Date());
        expenseBillMapper.updateById(bill);

        // 写操作日志
        Map<String, Object> detail = new HashMap<>();
        detail.put("rejectReason", reason);
        detail.put("opinion", opinion);
        feeOperationLogService.log("EXPENSE", "REJECT", billId, bill.getBillNo(),
            null, beforeStatus, "REJECTED", JSONUtil.toJsonStr(detail));

        // 通知报销人
        sysMessageService.sendMessage(bill.getApplicantId(), "报销单被驳回",
            "您的报销单（" + bill.getBillNo() + "）被驳回，原因：" + reason + "，请修改后重新提交。",
            "REJECTION", "EXPENSE", billId);

        return true;
    }

    /**
     * 校验审批人权限（多角色取并集：admin/boss/deptManager 任一即可，资金管控体系老板为报销终审人）
     */
    private void validateApprover(ExpenseBill bill) {
        Set<String> roleKeys = LoginHelper.getLoginUser().getRolePermission();
        if (roleKeys == null || roleKeys.isEmpty()) {
            throw new ServiceException("仅老板或部门经理可执行审批操作");
        }
        boolean isAdmin = roleKeys.contains("admin");
        boolean isBoss = roleKeys.contains("boss");
        boolean isDeptLeader = roleKeys.contains("deptManager");
        if (!isAdmin && !isBoss && !isDeptLeader) {
            // T6.3 信用体系：优秀信用后补单免老板审批，财务可直接审批（开关关闭时内部返回 false）
            boolean creditExempt = roleKeys.contains("finance")
                && bill.getInvoiceDefer() != null && bill.getInvoiceDefer() == 1
                && creditService.skipBossApproval(bill.getApplicantId());
            if (!creditExempt) {
                throw new ServiceException("仅老板或部门经理可执行审批操作");
            }
            return;
        }
        // 部门经理需校验审批人部门与报销人部门一致（admin/老板不受此限制）
        if (!isAdmin && !isBoss) {
            Long approverDeptId = LoginHelper.getDeptId();
            if (!approverDeptId.equals(bill.getDeptId())) {
                throw new ServiceException("您不是该报销人所属部门的部门经理");
            }
        }
    }
}
