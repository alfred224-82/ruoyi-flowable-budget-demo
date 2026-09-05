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
 * 订阅消息授权次数对象 sys_subscribe_quota（T14.4 微信一次性订阅消息合规）
 * <p>表仅含 create_time/update_time，故不继承 BaseEntity；每租户+openid+模板唯一。
 * <p>授权一次 remaining_count+1，发送一次 -1；不足则不发送并引导重新授权（不报错）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@TableName("sys_subscribe_quota")
public class SysSubscribeQuota implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 微信 openid */
    private String openid;

    /** 订阅消息模板ID */
    private String templateId;

    /** 剩余可发送次数(授权一次+1，发送一次-1) */
    private Integer remainingCount;

    /** 累计授权次数 */
    private Integer totalGranted;

    /** 累计发送次数 */
    private Integer totalSent;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
