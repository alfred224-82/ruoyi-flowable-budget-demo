package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.util.Date;

/**
 * 费用大类主数据视图对象 fee_expense_category（T5.13）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeExpenseCategoryVo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 大类编码(租户内唯一，如 TRAVEL)
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

    /**
     * 创建时间
     */
    private Date createTime;
}
