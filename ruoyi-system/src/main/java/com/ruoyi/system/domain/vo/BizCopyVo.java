package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 业态文案映射视图对象 biz_copy（T10.5）
 *
 * @author ruoyi
 * @date 2026-09-16
 */
@Data
public class BizCopyVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    private Long id;

    /** 业态包编码 */
    private String bizCode;

    /** 文案 key */
    private String i18nKey;

    /** 语言编码（BCP-47） */
    private String lang;

    /** 文案值 */
    private String copyValue;

    /** 所属模块 */
    private String module;

    /** 状态（0启用 1停用） */
    private String status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}
