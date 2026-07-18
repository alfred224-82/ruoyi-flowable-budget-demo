package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.ExpenseBill;
import com.ruoyi.system.domain.vo.ExpenseBillVo;
import com.ruoyi.system.mapper.ExpenseBillMapper;
import com.ruoyi.system.service.IExpenseApprovalService;
import com.ruoyi.system.service.IFeeOperationLogService;
import com.ruoyi.system.service.ISysMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
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

    @Override
    public Page<ExpenseBillVo> selectPendingApprovalPage(PageQuery pageQuery) {
        Set<String> roleKeys = LoginHelper.getLoginUser().getRolePermission();
        String roleKey = (roleKeys != null && !roleKeys.isEmpty()) ? roleKeys.iterator().next() : "";

        QueryWrapper<ExpenseBill> wrapper = Wrappers.<ExpenseBill>query()
            .eq("e.status", "SUBMITTED");

        // deptManager 只看本部门；admin/其他上级角色 可看全部
        if ("deptManager".equals(roleKey)) {
            Long deptId = LoginHelper.getDeptId();
            wrapper.eq("e.dept_id", deptId);
        }

        return expenseBillMapper.selectExpenseBillPage(pageQuery.build(), wrapper);
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
     * 校验审批人权限
     */
    private void validateApprover(ExpenseBill bill) {
        Set<String> roleKeys = LoginHelper.getLoginUser().getRolePermission();
        if (roleKeys == null || roleKeys.isEmpty()) {
            throw new ServiceException("仅部门经理可执行审批操作");
        }
        boolean isAdmin = roleKeys.contains("admin");
        boolean isDeptLeader = roleKeys.contains("deptManager");
        if (!isAdmin && !isDeptLeader) {
            throw new ServiceException("仅部门经理可执行审批操作");
        }
        // 部门经理需校验审批人部门与报销人部门一致（admin不受此限制）
        if (!isAdmin) {
            Long approverDeptId = LoginHelper.getDeptId();
            if (!approverDeptId.equals(bill.getDeptId())) {
                throw new ServiceException("您不是该报销人所属部门的部门经理");
            }
        }
    }
}
