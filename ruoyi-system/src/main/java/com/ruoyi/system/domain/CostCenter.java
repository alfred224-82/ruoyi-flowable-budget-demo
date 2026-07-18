package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 责任中心对象 fee_cost_center
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_cost_center")
public class CostCenter extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 责任中心编码
     */
    private String centerCode;

    /**
     * 责任中心名称
     */
    private String centerName;

    /**
     * 默认会计大类
     */
    private String defaultAccountingClass;

    /**
     * 是否待分摊中心(0否 1是)
     */
    private Integer isPending;

    /**
     * 状态(ENABLED/DISABLED)
     */
    private String status;
}
