package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 服务分类对象 service_category（T15.4/T15.11，小微版）
 * <p>房间/助教/蔬菜/肉菜/课程...；租户级表（含 tenant_id），多租户插件自动隔离。
 * industry_type 关联 industry_template.industry_code（可空，通用模型支持所有行业）。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("service_category")
public class ServiceCategory extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 分类名称（房间/助教/蔬菜/肉菜/课程...）
     */
    private String name;

    /**
     * 所属行业（industry_template.industry_code）
     */
    private String industryType;

    /**
     * ★分类类型（V3.7 原子资源）：service=服务分类（默认）/ space=房间·空间分类
     * <p>space 分类下的 service_item 即「房间/空间」原子资源（租户管理员·房间空间管理页维护）
     */
    private String categoryType;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 删除标志（0存在 2删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注
     */
    private String remark;
}
