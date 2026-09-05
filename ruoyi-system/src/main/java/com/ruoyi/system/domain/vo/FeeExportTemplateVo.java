package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 凭证导出模板视图对象 fee_export_template（T5.11）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeExportTemplateVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 模板编码 */
    private String templateCode;
    /** 模板名称 */
    private String templateName;
    /** 适用财务系统 */
    private String targetSystem;
    /** 列头定义 JSON */
    private String columnConfig;
    /** 分隔符 */
    private String delimiter;
    /** 是否内置 */
    private Integer isBuiltin;
    /** 排序 */
    private Integer sortOrder;
    /** 创建时间 */
    private Date createTime;
}
