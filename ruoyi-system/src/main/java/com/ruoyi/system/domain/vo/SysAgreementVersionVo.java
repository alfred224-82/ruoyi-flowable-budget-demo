package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 协议版本视图对象 sys_agreement_version（T14.4）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class SysAgreementVersionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 协议类型：privacy/user */
    private String agreementType;

    /** 版本号 */
    private String version;

    /** 协议标题 */
    private String title;

    /** 协议正文URL */
    private String contentUrl;

    /** 正文哈希 */
    private String contentHash;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date publishTime;

    /** 状态（0生效 1历史） */
    private String status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
