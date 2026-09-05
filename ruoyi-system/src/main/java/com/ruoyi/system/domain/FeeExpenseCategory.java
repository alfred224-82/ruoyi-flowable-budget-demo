package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 费用大类主数据对象 fee_expense_category（T5.13 / T1.5 三层角色分工）
 * admin 超管维护，系统级共享，不按 tenant_id 隔离（已加入 IGNORE_TENANT_TABLES）
 * 大类额度为预算编制与报销管控的上层限额（设计文档 2.2 #3）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_expense_category")
public class FeeExpenseCategory extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 大类编码(全局唯一，如 TRAVEL)
     */
    private String categoryCode;

    /**
     * 大类名称
     */
    private String categoryName;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 是否启用(0-禁用 1-启用)
     */
    private Integer isActive;

    /**
     * 是否内置(1-内置不可删除，可停用)
     */
    private Integer isBuiltin;

    /**
     * 备注
     */
    private String remark;
}
