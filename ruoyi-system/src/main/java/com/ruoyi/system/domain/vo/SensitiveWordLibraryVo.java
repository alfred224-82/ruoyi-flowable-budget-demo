package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 敏感词库视图对象 sensitive_word_library（T18.1/T18.2/T18.3，小微版 M18）
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Data
public class SensitiveWordLibraryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    /** 租户ID（NULL=平台级） */
    private Long tenantId;
    private String word;
    private String category;
    private String level;
    private String source;
    private String status;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
