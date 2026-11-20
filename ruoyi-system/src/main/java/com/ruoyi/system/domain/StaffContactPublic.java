package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 员工联系方式公开开关对象 staff_contact_public（T31.5，设计方案 §八之二）
 * <p>按员工单独设置，新入职默认全关（show_phone=0, show_wechat=0）。
 * <p>顾客端点头像弹名片时，按本表开关决定显示「打电话」/「加微信」入口。
 * <p>租户级表（含 tenant_id 由 {@link TenantEntity} 自动填充），
 * UNIQUE(tenant_id, staff_id) 保证每员工一条配置。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("staff_contact_public")
public class StaffContactPublic extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 员工用户ID（FK→sys_user.user_id） */
    private Long staffId;

    /** 公开手机号：0=关 1=开 */
    private Integer showPhone;

    /** 公开微信号：0=关 1=开 */
    private Integer showWechat;

    /** 员工微信二维码 OSS URL（顾客长按识别添加） */
    private String wechatQrcodeUrl;
}
