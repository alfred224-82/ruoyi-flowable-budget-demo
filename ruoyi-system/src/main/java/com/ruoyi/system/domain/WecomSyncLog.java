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
 * 企微通讯录同步日志对象 wecom_sync_log（T13.2 同步幂等/失败可重试留痕）
 * <p>表仅含 create_time，故不继承 BaseEntity。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@TableName("wecom_sync_log")
public class WecomSyncLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 同步类型：dept(部门)/user(成员)/all */
    private String syncType;

    /** 状态：SUCCESS/PARTIAL/FAILED */
    private String status;

    /** 总数 */
    private Integer totalCount;

    /** 成功数 */
    private Integer successCount;

    /** 失败数 */
    private Integer failCount;

    /** 错误详情 */
    private String errorMsg;

    /** 耗时(毫秒) */
    private Long costMs;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
