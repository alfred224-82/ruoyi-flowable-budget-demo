package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 账户数据操作留痕对象 sys_account_action_log（T14.4 数据导出/账号注销，7 日内完成并留痕）
 * <p>表含 apply_time/finish_time（无标准审计列），故不继承 BaseEntity。
 * <p>状态机：PENDING(申请) → PROCESSING(处理中) → DONE(完成) / REJECTED(拒绝)。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@TableName("sys_account_action_log")
public class SysAccountActionLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 用户ID */
    private Long userId;

    /** 操作类型：DATA_EXPORT(数据导出)/ACCOUNT_DELETE(账号注销) */
    private String actionType;

    /** 状态：PENDING/PROCESSING/DONE/REJECTED */
    private String status;

    /** 结果引用(导出文件URL/注销凭证) */
    private String resultRef;

    /** 完成截止时间(申请后 7 日内) */
    private Date deadline;

    /** 处理人 */
    private String operator;

    /** 申请时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date applyTime;

    /** 完成时间 */
    private Date finishTime;

    /** 备注 */
    private String remark;
}
