package com.ruoyi.system.domain.vo;

import com.ruoyi.system.domain.ActivityLockRequest;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 活动锁定申请审批视图对象（M33 §九 老板端待审批列表 / 员工端申请回显）
 * <p>由 {@link ActivityLockRequest} 通过 MyBatis-Plus selectVoList 直接映射，字段同名。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Data
public class ActivityLockRequestVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 活动ID */
    private Long activityId;

    /** 活动标题快照 */
    private String activityTitle;

    /** 申请人员工ID */
    private Long applyStaffId;

    /** 申请人姓名快照 */
    private String applyStaffName;

    /** 申请说明 */
    private String reason;

    /** 状态：PENDING / APPROVED / REJECTED */
    private String status;

    /** 审批人（老板）用户ID */
    private Long approveBy;

    /** 审批人姓名快照 */
    private String approveByName;

    /** 审批时间 */
    private Date approveTime;

    /** 驳回原因 */
    private String rejectReason;

    /** 申请时间 */
    private Date createTime;
}
