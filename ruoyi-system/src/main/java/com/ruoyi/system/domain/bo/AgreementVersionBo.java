package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 协议版本业务对象 sys_agreement_version（T14.4 隐私政策/用户协议发布）
 * <p>发布新版本时同类型旧版本自动转历史（status=1）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class AgreementVersionBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 协议类型：privacy/user */
    @NotBlank(message = "协议类型不能为空", groups = {AddGroup.class})
    private String agreementType;

    /** 版本号(如 v1.1) */
    @NotBlank(message = "版本号不能为空", groups = {AddGroup.class})
    private String version;

    /** 协议标题 */
    private String title;

    /** 协议正文URL(OSS) */
    private String contentUrl;

    /** 备注 */
    private String remark;
}
