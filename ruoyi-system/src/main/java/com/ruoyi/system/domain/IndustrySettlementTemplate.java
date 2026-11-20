package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 行业结算模板 industry_settlement_template（M36 T36.2）
 * <p>创建活动时按行业自动带出常用收入项条目：
 * <ul>
 *   <li>桌游/剧本杀 → 场次费 + 饮料零食 + 道具租赁</li>
 *   <li>旅游/户外 → 成人团费 + 儿童团费 + 单房差 + 装备租赁 + 保险 + 代收门票</li>
 *   <li>按摩/足浴 → 服务费 + 精油升级 + 茶水点心</li>
 * </ul>
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("industry_settlement_template")
public class IndustrySettlementTemplate extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 行业编码（对齐 industry_template.industry_code） */
    private String industryCode;

    /** 行业名称（冗余展示） */
    private String industryName;

    /** 收入项名称 */
    private String itemName;

    /** TICKET/ADDON/COLLECTED */
    private String itemType;

    /** 默认数量 */
    private Integer defaultQuantity;

    /** 默认是否计入分账 */
    private Integer includeInCommission;

    /** 默认是否计入利润 */
    private Integer includeInProfit;

    /** 是否默认票面行 */
    private Integer isDefaultTicket;

    /** 排序 */
    private Integer sortOrder;

    /** 0=启用 1=停用 */
    private String status;

    /** 备注 */
    private String remark;

    /** 删除标志（0存在 2删除） */
    @TableLogic
    private String delFlag;
}
