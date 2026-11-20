package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 敏感词视图对象 sensitive_word（M18 T18.1）
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Data
public class SensitiveWordVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    /** 租户ID（0=全局底库） */
    private Long tenantId;
    private String word;
    private String category;
    /** 匹配模式：exact/fuzzy/regex */
    private String matchMode;
    /** 行业范围（JSON 数组文本，NULL=全行业通用） */
    private String industryScope;
    /** 状态（0启用 1禁用） */
    private String status;
    /** 来源：global/tenant */
    private String source;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
