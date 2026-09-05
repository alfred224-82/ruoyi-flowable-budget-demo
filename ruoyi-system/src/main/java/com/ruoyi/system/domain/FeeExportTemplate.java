package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 凭证导出模板对象 fee_export_template（T5.11 / ADR-017/020）
 * 内置7套（tenant_id=0）；自定义=高级版（M6 T6.6）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_export_template")
public class FeeExportTemplate extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 模板编码
     */
    private String templateCode;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 适用财务系统
     */
    private String targetSystem;

    /**
     * 列头定义 JSON
     */
    private String columnConfig;

    /**
     * 分隔符
     */
    private String delimiter;

    /**
     * 是否内置(内置不可删)
     */
    private Integer isBuiltin;

    /**
     * 排序
     */
    private Integer sortOrder;
}
