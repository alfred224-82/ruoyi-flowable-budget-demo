package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 行业模板视图对象 industry_template（T15.2/T15.10）
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class IndustryTemplateVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 行业编码 */
    private String industryCode;

    /** 行业名称 */
    private String industryName;

    /** 行业图标 */
    private String icon;

    /** 预置数据 JSON（服务分类示例/结算规则/单位库/默认页面配置） */
    private String presetConfig;

    /** 排序 */
    private Integer sortOrder;

    /** 状态（0正常 1停用） */
    private String status;

    /** 是否可语音（0否 1是，三级控制总开关：admin→platform_admin→boss，T1.7/T18.1） */
    private Integer voiceEnabled;

    /** 备注 */
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
