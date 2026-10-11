package com.ruoyi.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * 租户信息业务对象
 *
 * @author ruoyi
 * @date 2026-08-01
 */
@Data
public class SysTenantBo {

    /**
     * 主键（编辑时传入）
     */
    private Long id;

    /**
     * 租户编码（唯一，系统自动生成）
     */
    private String tenantCode;

    /**
     * 企业名称
     */
    @NotBlank(message = "企业名称不能为空")
    private String companyName;

    /**
     * 联系人
     */
    private String contactName;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 行业
     */
    private String industry;

    /**
     * 分配版本：BASIC/ADVANCED/PROFESSIONAL（兼容存量 ENTERPRISE，T4.1）
     */
    private String edition;

    /**
     * 产品线（T15.25 硬隔离 + T15.29 新建租户级联）：micro(小微版)/enterprise(企业版)，默认 enterprise
     */
    private String tenantType;

    /**
     * 版本档位（T15.26 小微版分档 + T15.29 级联）：micro_lite/micro_pro/basic/advanced/pro
     */
    private String versionType;

    /**
     * 业态包编码：fund_control/boardgame/fresh_food，默认 fund_control（T10.1）
     */
    private String bizCode;

    /**
     * 访问通道：wechat/wecom，默认 wechat（T10.1）
     */
    private String accessChannel;

    /**
     * 企业微信 CorpID（FEAT_WECOM，T10.1）
     */
    private String wecomCorpId;

    /**
     * 企业微信自建应用 AgentId（FEAT_WECOM，T10.1）
     */
    private String wecomAgentId;

    /**
     * 企业微信自建应用 Secret（明文入参，落库加密，T10.1）
     */
    private String wecomSecret;

    /**
     * 微信公众号 AppID（引导态反查租户，T10.1）
     */
    private String mpAppId;

    /**
     * 授权类型：TRIAL/FORMAL
     */
    private String licenseType;

    /**
     * 授权到期日
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date expireDate;

    /**
     * 最大用户数
     */
    @NotNull(message = "最大用户数不能为空")
    private Integer maxUsers;

    /**
     * 隔离模式：SHARED共享表/DEDICATED独立Schema（预留，FEAT_ISOLATED_SCHEMA）
     */
    private String isolationMode;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态（查询条件用）
     */
    private String status;

    // ====== 创建租户时传入的管理员账号信息 ======

    /**
     * 租户管理员登录账号
     */
    private String username;

    /**
     * 租户管理员初始密码
     */
    private String password;
}
