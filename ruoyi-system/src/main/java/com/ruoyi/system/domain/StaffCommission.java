package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 员工提成比例配置对象 staff_commission（REQ-03，v1.3 需求清单，仅 PC）
 * <p>为每个员工配置「提成比例 + 基础工资」，员工当月薪酬成本 = 基础工资 + 提成；
 * 提成 = 当月基数（员工当月已完成订单金额）× 提成比例。
 * <p>租户级表（含 tenant_id），多租户插件自动隔离；含 del_flag 逻辑删除；
 * UNIQUE(tenant_id, staff_user_id, del_flag) 保证每员工一条配置。
 *
 * @author ruoyi
 * @date 2026-10-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("staff_commission")
public class StaffCommission extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 员工用户ID（sys_user.user_id）
     */
    private Long staffUserId;

    /**
     * 员工姓名（冗余，便于列表展示）
     */
    private String staffName;

    /**
     * 提成比例（0~1 小数，如 0.1000=10%）
     */
    private BigDecimal commissionRate;

    /**
     * 基础工资（元/月）
     */
    private BigDecimal baseSalary;

    /**
     * 生效日期
     */
    private Date effectiveDate;

    /**
     * 状态（0-正常；1-停用）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 删除标志（0存在 2删除）
     */
    @TableLogic
    private String delFlag;
}
