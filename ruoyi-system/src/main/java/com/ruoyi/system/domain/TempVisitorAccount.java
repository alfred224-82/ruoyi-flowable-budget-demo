package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 临时游客账号对象 temp_visitor_account（M31 T31.3/T31.5）
 * <p>游客扫码登录后自动生成临时账号，30 天有效期，到期自动废弃。
 * <p>同一手机号后 4 位在同一租户下只能生成一个有效账号（幂等性保证）。
 * <p>租户级表（含 tenant_id 由 {@link TenantEntity} 自动填充）。
 *
 * @author ruoyi
 * @date 2026-10-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("temp_visitor_account")
public class TempVisitorAccount extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 状态：有效 */
    public static final int STATUS_ACTIVE = 0;
    /** 状态：已补全 */
    public static final int STATUS_COMPLETED = 1;
    /** 状态：已废弃 */
    public static final int STATUS_EXPIRED = 2;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 手机号后 4 位（与 tenant_id 联合唯一） */
    private String phoneLast4;

    /** 关联 sys_user.user_id */
    private Long userId;

    /** 完整手机号（脱敏存储） */
    private String phone;

    /** 过期时间（创建时间 + 30 天） */
    private Date expireTime;

    /** 状态：0=有效 1=已补全 2=已废弃 */
    private Integer status;
}
