package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户业态单店覆盖对象 tenant_biz_config（T1.7 / T10.4）
 * <p>module_overrides JSON 覆盖模块开关/文案/字段可见性；合并优先级 = 租户覆盖 &gt; 业态包默认。
 * 禁止 fork 业态包，单店差异一律走本表（设计文档 1.8.5）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tenant_biz_config")
public class TenantBizConfig extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 单店覆盖配置 JSON（模块开关/文案/字段可见性）
     */
    private String moduleOverrides;
}
