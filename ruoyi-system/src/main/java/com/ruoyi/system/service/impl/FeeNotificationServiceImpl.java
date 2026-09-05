package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.system.domain.SysUserRole;
import com.ruoyi.system.domain.FeeIncomeConfirmation;
import com.ruoyi.system.domain.FeePayment;
import com.ruoyi.system.domain.FeeProject;
import com.ruoyi.system.domain.FeeProjectChange;
import com.ruoyi.system.domain.FeeProjectSharing;
import com.ruoyi.system.domain.FeeReceipt;
import com.ruoyi.system.domain.SysMessage;
import com.ruoyi.system.mapper.FeeIncomeConfirmationMapper;
import com.ruoyi.system.mapper.FeePaymentMapper;
import com.ruoyi.system.mapper.FeeProjectChangeMapper;
import com.ruoyi.system.mapper.FeeProjectMapper;
import com.ruoyi.system.mapper.FeeProjectSharingMapper;
import com.ruoyi.system.mapper.FeeReceiptMapper;
import com.ruoyi.system.mapper.SysMessageMapper;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysUserRoleMapper;
import com.ruoyi.system.service.IFeeNotificationService;
import com.ruoyi.system.service.ISysMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 资金管控通知体系Service实现（T5.12）
 * 角色广播（审批人 N→1 合并触达）+ 老板每日简报（限频每日≤1条）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeNotificationServiceImpl implements IFeeNotificationService {

    private static final String BRIEFING_BIZ_TYPE = "DAILY_BRIEFING";

    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMessageMapper messageMapper;
    private final ISysMessageService sysMessageService;
    private final FeeProjectMapper projectMapper;
    private final FeePaymentMapper paymentMapper;
    private final FeeProjectChangeMapper changeMapper;
    private final FeeProjectSharingMapper sharingMapper;
    private final FeeIncomeConfirmationMapper confirmationMapper;
    private final FeeReceiptMapper receiptMapper;

    @Override
    public void notifyRole(String roleKey, String title, String content, String messageType, String bizType, Long bizId) {
        List<Long> userIds = selectUserIdsByRoleKey(roleKey);
        if (CollUtil.isEmpty(userIds)) {
            log.warn("角色[{}]无正常用户，跳过通知: {}", roleKey, title);
            return;
        }
        sysMessageService.sendBatchMessage(userIds, title, content, messageType, bizType, bizId);
    }

    @Override
    public int sendBossDailyBriefing() {
        // 限频：当日已发过简报则不再发送（老板每日≤1条）
        Long sentToday = messageMapper.selectCount(Wrappers.<SysMessage>lambdaQuery()
            .eq(SysMessage::getBizType, BRIEFING_BIZ_TYPE)
            .ge(SysMessage::getCreateTime, DateUtil.beginOfDay(new Date())));
        if (sentToday != null && sentToday > 0) {
            log.info("今日已发送过老板简报，跳过（限频每日≤1条）");
            return 0;
        }

        // 合并各类待办为一条简报（N→1）
        long pendingProjects = projectMapper.selectCount(
            Wrappers.<FeeProject>lambdaQuery().eq(FeeProject::getStatus, "PENDING_APPROVAL"));
        long pendingPayments = paymentMapper.selectCount(
            Wrappers.<FeePayment>lambdaQuery().eq(FeePayment::getStatus, "PENDING"));
        long pendingChanges = changeMapper.selectCount(
            Wrappers.<FeeProjectChange>lambdaQuery().eq(FeeProjectChange::getChangeStatus, "PENDING"));
        long pendingSharings = sharingMapper.selectCount(
            Wrappers.<FeeProjectSharing>lambdaQuery().eq(FeeProjectSharing::getStatus, "PENDING"));
        long pendingConfirmations = confirmationMapper.selectCount(
            Wrappers.<FeeIncomeConfirmation>lambdaQuery().eq(FeeIncomeConfirmation::getVerifyStatus, "PENDING"));
        long pendingReceipts = receiptMapper.selectCount(
            Wrappers.<FeeReceipt>lambdaQuery().eq(FeeReceipt::getStatus, "PENDING"));
        long total = pendingProjects + pendingPayments + pendingChanges
            + pendingSharings + pendingConfirmations + pendingReceipts;
        if (total == 0) {
            log.info("无待办事项，不发送老板简报");
            return 0;
        }
        StringBuilder content = new StringBuilder("今日待办 ").append(total).append(" 项：");
        if (pendingProjects > 0) {
            content.append("立项审批 ").append(pendingProjects).append(" 项；");
        }
        if (pendingPayments > 0) {
            content.append("付款审批 ").append(pendingPayments).append(" 项；");
        }
        if (pendingChanges > 0) {
            content.append("合同变更审批 ").append(pendingChanges).append(" 项；");
        }
        if (pendingSharings > 0) {
            content.append("分成审批 ").append(pendingSharings).append(" 项；");
        }
        if (pendingConfirmations > 0) {
            content.append("收入核验 ").append(pendingConfirmations).append(" 项；");
        }
        if (pendingReceipts > 0) {
            content.append("收款审核 ").append(pendingReceipts).append(" 项；");
        }
        notifyRole("boss", "每日经营简报", content.toString(), "SYSTEM", BRIEFING_BIZ_TYPE, null);
        return 1;
    }

    /**
     * 查询角色下全部正常用户ID
     */
    private List<Long> selectUserIdsByRoleKey(String roleKey) {
        List<SysRole> roles = roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
            .eq(SysRole::getRoleKey, roleKey)
            .eq(SysRole::getStatus, "0"));
        if (CollUtil.isEmpty(roles)) {
            return CollUtil.newArrayList();
        }
        List<Long> roleIds = roles.stream().map(SysRole::getRoleId).collect(Collectors.toList());
        List<SysUserRole> userRoles = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
            .in(SysUserRole::getRoleId, roleIds));
        return userRoles.stream().map(SysUserRole::getUserId).distinct().collect(Collectors.toList());
    }
}
