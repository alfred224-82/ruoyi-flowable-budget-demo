package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 固定支出标签对象 fixed_cost_tag（T18.9，小微版 M18）
 * <p>租户级标签管理：房租/水电/工资等，CRUD + 排序。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fixed_cost_tag")
public class FixedCostTag extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 标签名称 */
    private String tagName;

    /** 标签编码 */
    private String tagCode;

    /** 标签颜色 */
    private String color;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态：ENABLED/DISABLED */
    private String status;

    private String remark;
}
