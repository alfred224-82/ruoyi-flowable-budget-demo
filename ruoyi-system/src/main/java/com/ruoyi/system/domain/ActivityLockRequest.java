package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 活动锁定申请审批对象 activity_lock_request（M33 §九 老板/员工权限分工）
 * <p>员工端全员确认后「申请锁定」→ 写入本表 PENDING；老板端 GET /micro/boss/lockRequest/list 读取待审批，
 * approve 触发 {@code IPromotionActivityService#lockActivity} 真正锁定，reject 驳回。
 * <p>确认下放给员工、锁定审批由老板把关（§九 权限分工表）。
 * <p>租户级表（含 tenant_id 由 {@link TenantEntity} 自动填充）。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activity_lock_request")
public class ActivityLockRequest extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 状态：待审批 */
    public static final String STATUS_PENDING = "PENDING";
    /** 状态：已通过并锁定 */
    public static final String STATUS_APPROVED = "APPROVED";
    /** 状态：已驳回 */
    public static final String STATUS_REJECTED = "REJECTED";

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 活动ID（FK→promotion_activity.id） */
    private Long activityId;

    /** 活动标题快照（列表展示，避免联表） */
    private String activityTitle;

    /** 申请人员工ID（FK→sys_user.user_id） */
    private Long applyStaffId;

    /** 申请人姓名快照 */
    private String applyStaffName;

    /** 申请说明（可选） */
    private String reason;

    /** 状态：PENDING=待审批 / APPROVED=已通过并锁定 / REJECTED=已驳回 */
    private String status;

    /** 审批人（老板）用户ID */
    private Long approveBy;

    /** 审批人姓名快照 */
    private String approveByName;

    /** 审批时间 */
    private Date approveTime;

    /** 驳回原因 */
    private String rejectReason;

    /** 删除标志：0=存在 2=删除 */
    @TableLogic
    private String delFlag;
}
