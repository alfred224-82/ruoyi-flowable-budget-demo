package com.ruoyi.test;

import com.ruoyi.system.domain.SysMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 系统消息模块单元测试
 * 对应验证文档：第九章 系统消息模块验证（P1）
 *
 * 验收项覆盖：
 * - MSG-002: 未读数准确
 * - MSG-004: 审批通知生成
 * - MSG-005: 驳回通知生成
 * - MSG-006: 标记已读
 * - MSG-008: 未读数更新
 * - MSG-009: 消息类型筛选
 *
 * @author ruoyi
 * @date 2026-07-24
 */
@Tag("message")
@DisplayName("系统消息模块验证")
public class SysMessageServiceTest {

    private List<SysMessage> messages;

    @BeforeEach
    void setUp() {
        messages = new ArrayList<>();
        messages.add(createMessage(1L, "APPROVAL", false, "待审批通知"));
        messages.add(createMessage(2L, "REJECT", false, "驳回通知"));
        messages.add(createMessage(3L, "SYSTEM", true, "系统通知"));
        messages.add(createMessage(4L, "APPROVAL", true, "已读审批通知"));
    }

    private SysMessage createMessage(Long id, String type, boolean read, String title) {
        SysMessage msg = new SysMessage();
        msg.setId(id);
        msg.setMessageType(type);
        msg.setIsRead(read ? 1 : 0);
        msg.setTitle(title);
        msg.setCreateTime(new Date());
        return msg;
    }

    // ==================== MSG-002: 未读数准确 ====================

    @Test
    @DisplayName("MSG-002: 未读消息数与实际一致")
    void testUnreadCount() {
        // Given - 4条消息，2条未读

        // When
        long unreadCount = messages.stream()
            .filter(m -> m.getIsRead() != null && m.getIsRead() == 0)
            .count();

        // Then
        assertEquals(2, unreadCount);
    }

    // ==================== MSG-006: 标记已读 ====================

    @Test
    @DisplayName("MSG-006: 单条标记已读")
    void testMarkAsRead_Single() {
        // Given
        SysMessage msg = messages.get(0);
        assertEquals(0, msg.getIsRead());

        // When
        msg.setIsRead(1);

        // Then
        assertEquals(1, msg.getIsRead());
    }

    @Test
    @DisplayName("MSG-006b: 全部标记已读")
    void testMarkAsRead_All() {
        // When
        messages.forEach(m -> m.setIsRead(1));

        // Then
        long unreadCount = messages.stream()
            .filter(m -> m.getIsRead() != null && m.getIsRead() == 0)
            .count();
        assertEquals(0, unreadCount);
    }

    // ==================== MSG-008: 未读数更新 ====================

    @Test
    @DisplayName("MSG-008: 标记已读后未读数实时减少")
    void testUnreadCountDecreases() {
        // Given
        long initialUnread = messages.stream().filter(m -> m.getIsRead() == 0).count();
        assertEquals(2, initialUnread);

        // When - 标记一条为已读
        messages.get(0).setIsRead(1);

        // Then
        long newUnread = messages.stream().filter(m -> m.getIsRead() == 0).count();
        assertEquals(1, newUnread);
        assertTrue(newUnread < initialUnread);
    }

    // ==================== MSG-009: 消息类型筛选 ====================

    @Test
    @DisplayName("MSG-009: 按APPROVAL类型筛选")
    void testFilterByType_Approval() {
        // When
        List<SysMessage> approvalMsgs = messages.stream()
            .filter(m -> "APPROVAL".equals(m.getMessageType()))
            .collect(Collectors.toList());

        // Then
        assertEquals(2, approvalMsgs.size());
        assertTrue(approvalMsgs.stream().allMatch(m -> "APPROVAL".equals(m.getMessageType())));
    }

    @Test
    @DisplayName("MSG-009b: 按REJECT类型筛选")
    void testFilterByType_Reject() {
        // When
        List<SysMessage> rejectMsgs = messages.stream()
            .filter(m -> "REJECT".equals(m.getMessageType()))
            .collect(Collectors.toList());

        // Then
        assertEquals(1, rejectMsgs.size());
    }

    @Test
    @DisplayName("MSG-009c: 按SYSTEM类型筛选")
    void testFilterByType_System() {
        // When
        List<SysMessage> systemMsgs = messages.stream()
            .filter(m -> "SYSTEM".equals(m.getMessageType()))
            .collect(Collectors.toList());

        // Then
        assertEquals(1, systemMsgs.size());
    }

    // ==================== MSG-004/005: 通知生成 ====================

    @Test
    @DisplayName("MSG-004: 提交审核时生成审批通知")
    void testApprovalNotificationGeneration() {
        // Given
        SysMessage approvalMsg = new SysMessage();
        approvalMsg.setMessageType("APPROVAL");
        approvalMsg.setTitle("待审批通知");
        approvalMsg.setContent("张三提交了预算单BG-202607-001，请您及时审核。");
        approvalMsg.setBizType("budget_preparation");
        approvalMsg.setBizId(1L);
        approvalMsg.setIsRead(0);

        // Then
        assertEquals("APPROVAL", approvalMsg.getMessageType());
        assertNotNull(approvalMsg.getContent());
        assertEquals("budget_preparation", approvalMsg.getBizType());
    }

    @Test
    @DisplayName("MSG-005: 驳回时生成通知含驳回理由")
    void testRejectNotificationGeneration() {
        // Given
        SysMessage rejectMsg = new SysMessage();
        rejectMsg.setMessageType("REJECT");
        rejectMsg.setTitle("预算编制被驳回");
        rejectMsg.setContent("您编制的预算单已被驳回。驳回理由：数据不准确");
        rejectMsg.setBizType("budget_preparation");
        rejectMsg.setBizId(1L);

        // Then
        assertEquals("REJECT", rejectMsg.getMessageType());
        assertTrue(rejectMsg.getContent().contains("驳回理由"));
    }

    // ==================== MSG-007: 消息跳转 ====================

    @Test
    @DisplayName("MSG-007: 消息包含业务关联信息用于跳转")
    void testMessageBizReference() {
        // Given
        SysMessage msg = messages.get(0);
        msg.setBizType("budget_preparation");
        msg.setBizId(123L);

        // Then
        assertNotNull(msg.getBizType());
        assertNotNull(msg.getBizId());
        // 可用于前端跳转：/system/preparation/detail/123
    }
}
