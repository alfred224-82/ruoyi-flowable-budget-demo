package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.email.MailUtils;
import com.ruoyi.system.domain.BudgetPreparation;
import com.ruoyi.system.mapper.BudgetPreparationMapper;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.mapper.SysUserRoleMapper;
import com.ruoyi.system.service.IBudgetNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 预算邮件通知服务实现
 *
 * @author ruoyi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetNotificationServiceImpl implements IBudgetNotificationService {

    private final BudgetPreparationMapper preparationMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;

    @Value("${mail.enabled:false}")
    private Boolean mailEnabled;

    @Value("${budget.system-url:http://localhost:80}")
    private String systemUrl;

    private static final int MAX_RETRY = 3;
    private static final long RETRY_INTERVAL_MS = 5 * 60 * 1000L;

    @Override
    @Async
    public void sendPeriodStartNotify(String budgetPeriod, Date startDate, Date endDate) {
        // 查询全体部门编制人员（角色 key=dept_budget_compiler）
        List<SysUser> compilers = findUsersByRoleKey("dept_budget_compiler");
        if (compilers.isEmpty()) {
            log.warn("未找到部门编制人员，跳过编制期开始提醒");
            return;
        }

        String template = loadTemplate("period-start-notify.html");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        for (SysUser user : compilers) {
            if (StrUtil.isBlank(user.getEmail())) {
                log.warn("用户 {} 未配置邮箱，跳过", user.getNickName());
                continue;
            }

            String content = template
                .replace("${receiver_name}", user.getNickName())
                .replace("${budget_period}", budgetPeriod)
                .replace("${start_date}", sdf.format(startDate))
                .replace("${end_date}", sdf.format(endDate))
                .replace("${system_url}", systemUrl);

            sendMailWithRetry(user.getEmail(),
                "【预算系统】" + budgetPeriod + "预算编制期已开始",
                content);
        }
        log.info("编制期开始提醒已发送，接收人数：{}", compilers.size());
    }

    @Override
    @Async
    public void sendApprovalNotify(String sheetNo, String deptName, String createByName, String approvalLevel) {
        // 根据审批级别查找对应审批人
        String roleKey = getRoleKeyByApprovalLevel(approvalLevel);
        List<SysUser> approvers = findUsersByRoleKey(roleKey);
        if (approvers.isEmpty()) {
            log.warn("未找到审批级别 {} 对应的审批人", approvalLevel);
            return;
        }

        String template = loadTemplate("approval-notify.html");

        for (SysUser user : approvers) {
            if (StrUtil.isBlank(user.getEmail())) {
                continue;
            }

            String content = template
                .replace("${receiver_name}", user.getNickName())
                .replace("${sheet_no}", sheetNo)
                .replace("${dept_name}", deptName)
                .replace("${create_by_name}", createByName)
                .replace("${system_url}", systemUrl)
                .replace("${biz_url}", "/system/preparation/approval");

            sendMailWithRetry(user.getEmail(),
                "【预算系统】您有待审批的预算编制[" + sheetNo + "]",
                content);
        }
        log.info("待审批通知已发送: sheetNo={}, level={}, 接收人数: {}", sheetNo, approvalLevel, approvers.size());
    }

    @Override
    @Async
    public void sendRejectNotify(String sheetNo, String rejectLevel, String rejectReason, Date deadline, Long sheetId) {
        // 查找编制人员
        BudgetPreparation preparation = preparationMapper.selectById(sheetId);
        if (preparation == null) {
            return;
        }

        SysUser creator = userMapper.selectUserByUserName(preparation.getCreateBy());
        if (creator == null || StrUtil.isBlank(creator.getEmail())) {
            log.warn("编制人员 {} 未配置邮箱，跳过驳回通知", preparation.getCreateBy());
            return;
        }

        String template = loadTemplate("reject-notify.html");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        String content = template
            .replace("${receiver_name}", creator.getNickName())
            .replace("${sheet_no}", sheetNo)
            .replace("${budget_period}", preparation.getBudgetYear() + "年" + preparation.getBudgetMonth() + "月")
            .replace("${dept_name}", preparation.getOrgName())
            .replace("${reject_level}", rejectLevel)
            .replace("${reject_reason}", rejectReason != null ? rejectReason : "")
            .replace("${deadline_time}", sdf.format(deadline))
            .replace("${system_url}", systemUrl)
            .replace("${biz_url}", "/system/preparation/wizard?id=" + sheetId + "&viewOnly=false");

        sendMailWithRetry(creator.getEmail(),
            "【预算系统】您的预算编制[" + sheetNo + "]被驳回",
            content);
        log.info("驳回通知已发送: sheetNo={}, receiver={}", sheetNo, creator.getNickName());
    }

    @Override
    @Async
    public void sendApprovedNotify(String sheetNo, String budgetPeriod) {
        // 查找编制人员
        BudgetPreparation preparation = preparationMapper.selectOne(
            Wrappers.<BudgetPreparation>lambdaQuery()
                .eq(BudgetPreparation::getSheetNo, sheetNo));
        if (preparation == null) {
            return;
        }

        SysUser creator = userMapper.selectUserByUserName(preparation.getCreateBy());
        if (creator == null || StrUtil.isBlank(creator.getEmail())) {
            return;
        }

        String template = loadTemplate("approved-notify.html");

        String content = template
            .replace("${receiver_name}", creator.getNickName())
            .replace("${sheet_no}", sheetNo)
            .replace("${budget_period}", budgetPeriod)
            .replace("${system_url}", systemUrl);

        sendMailWithRetry(creator.getEmail(),
            "【预算系统】您的预算编制[" + sheetNo + "]已审批通过",
            content);
        log.info("审批通过通知已发送: sheetNo={}", sheetNo);
    }

    @Override
    @Async
    public void sendTimeoutRemind(String sheetNo, int remainingHours, Date deadline, Long sheetId) {
        BudgetPreparation preparation = preparationMapper.selectById(sheetId);
        if (preparation == null) {
            return;
        }

        SysUser creator = userMapper.selectUserByUserName(preparation.getCreateBy());
        if (creator == null || StrUtil.isBlank(creator.getEmail())) {
            return;
        }

        String templateName = remainingHours <= 1 ? "timeout-remind.html" : "timeout-remind.html";
        String template = loadTemplate(templateName);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        String content = template
            .replace("${receiver_name}", creator.getNickName())
            .replace("${sheet_no}", sheetNo)
            .replace("${remaining_time}", remainingHours + "小时")
            .replace("${deadline_time}", sdf.format(deadline))
            .replace("${system_url}", systemUrl)
            .replace("${biz_url}", "/system/preparation/wizard?id=" + sheetId);

        String title = remainingHours <= 1
            ? "【预算系统】预算编制[" + sheetNo + "]即将超时（紧急）"
            : "【预算系统】预算编制[" + sheetNo + "]即将超时";

        sendMailWithRetry(creator.getEmail(), title, content);
        log.info("超时提醒已发送: sheetNo={}, remainingHours={}", sheetNo, remainingHours);
    }

    @Override
    @Async
    public void sendTimeoutAlert(String sheetNo, int timeoutDurationHours, Date originalDeadline, Long sheetId) {
        BudgetPreparation preparation = preparationMapper.selectById(sheetId);
        if (preparation == null) {
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        // 发送给编制人员
        SysUser creator = userMapper.selectUserByUserName(preparation.getCreateBy());
        if (creator != null && StrUtil.isNotBlank(creator.getEmail())) {
            String template = loadTemplate("timeout-alert.html");

            String content = template
                .replace("${receiver_name}", creator.getNickName())
                .replace("${sheet_no}", sheetNo)
                .replace("${timeout_duration}", timeoutDurationHours + "小时")
                .replace("${deadline_time}", sdf.format(originalDeadline))
                .replace("${system_url}", systemUrl)
                .replace("${biz_url}", "/system/preparation/wizard?id=" + sheetId);

            sendMailWithRetry(creator.getEmail(),
                "【预算系统】预算编制[" + sheetNo + "]已超时",
                content);
        }

        // 同时发送给上级审批人（分公司本部）
        List<SysUser> branchApprovers = findUsersByRoleKey("branch_manager");
        for (SysUser approver : branchApprovers) {
            if (StrUtil.isBlank(approver.getEmail())) {
                continue;
            }
            String template = loadTemplate("timeout-alert.html");
            String content = template
                .replace("${receiver_name}", approver.getNickName())
                .replace("${sheet_no}", sheetNo)
                .replace("${timeout_duration}", timeoutDurationHours + "小时")
                .replace("${deadline_time}", sdf.format(originalDeadline))
                .replace("${system_url}", systemUrl)
                .replace("${biz_url}", "/system/preparation/approval");

            sendMailWithRetry(approver.getEmail(),
                "【预算系统】预算编制[" + sheetNo + "]已超时（告警）",
                content);
        }
        log.info("超时告警已发送: sheetNo={}, timeoutHours={}", sheetNo, timeoutDurationHours);
    }

    // ==================== 内部工具方法 ====================

    /**
     * 加载 HTML 模板文件
     */
    private String loadTemplate(String templateName) {
        try {
            String path = "templates/budget/" + templateName;
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
                if (is == null) {
                    log.error("邮件模板文件不存在: {}", templateName);
                    return getDefaultTemplate(templateName);
                }
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int len;
                while ((len = is.read(buf)) != -1) {
                    bos.write(buf, 0, len);
                }
                return bos.toString(StandardCharsets.UTF_8.name());
            }
        } catch (Exception e) {
            log.error("加载邮件模板失败: {}", templateName, e);
            return getDefaultTemplate(templateName);
        }
    }

    /**
     * 带重试的邮件发送
     */
    private void sendMailWithRetry(String to, String subject, String content) {
        if (!mailEnabled) {
            log.info("[邮件模拟发送] to={}, subject={}", to, subject);
            return;
        }

        for (int i = 1; i <= MAX_RETRY; i++) {
            try {
                MailUtils.sendHtml(to, subject, content);
                log.info("邮件发送成功: to={}, subject={}", to, subject);
                return;
            } catch (Exception e) {
                log.warn("邮件发送失败（第{}次）: to={}, error={}", i, to, e.getMessage());
                if (i < MAX_RETRY) {
                    try {
                        Thread.sleep(RETRY_INTERVAL_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                } else {
                    log.error("邮件发送最终失败: to={}, subject={}", to, subject, e);
                }
            }
        }
    }

    /**
     * 根据角色 key 查找用户列表
     */
    private List<SysUser> findUsersByRoleKey(String roleKey) {
        try {
            // 1. 根据 roleKey 查询角色
            SysRole role = roleMapper.selectOne(
                Wrappers.<SysRole>lambdaQuery()
                    .eq(SysRole::getRoleKey, roleKey)
                    .eq(SysRole::getStatus, "0"));
            if (role == null) {
                log.warn("未找到角色: {}", roleKey);
                return Collections.emptyList();
            }

            // 2. 根据 roleId 查询用户ID列表
            List<Long> userIds = userRoleMapper.selectUserIdsByRoleId(role.getRoleId());
            if (userIds == null || userIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 3. 批量查询用户信息
            List<SysUser> users = new ArrayList<>();
            for (Long userId : userIds) {
                SysUser user = userMapper.selectUserById(userId);
                if (user != null && "0".equals(user.getStatus())) {
                    users.add(user);
                }
            }
            return users;
        } catch (Exception e) {
            log.error("根据角色key查找用户失败: {}", roleKey, e);
            return Collections.emptyList();
        }
    }

    /**
     * 审批级别映射角色 key
     */
    private String getRoleKeyByApprovalLevel(String approvalLevel) {
        switch (approvalLevel) {
            case "Dept":
                return "dept_manager";
            case "Branch":
                return "branch_manager";
            case "HQ":
                return "hq_manager";
            default:
                return "dept_manager";
        }
    }

    /**
     * 默认模板（模板文件加载失败时的降级方案）
     */
    private String getDefaultTemplate(String templateName) {
        return "<html><body><p>尊敬的 ${receiver_name}：</p>"
            + "<p>${sheet_no} ${reject_reason}</p>"
            + "<p>截止时间：${deadline_time}</p>"
            + "<p><a href='${system_url}${biz_url}'>点击操作</a></p>"
            + "</body></html>";
    }
}
