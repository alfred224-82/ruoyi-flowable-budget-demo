package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 租户信息视图对象
 *
 * @author ruoyi
 * @date 2026-08-01
 */
@Data
public class SysTenantVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 租户编码
     */
    private String tenantCode;

    /**
     * 企业名称
     */
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
     * 规模
     */
    private String scale;

    /**
     * 分配版本
     */
    private String edition;

    /**
     * 业态包编码：fund_control/boardgame/fresh_food（T10.1）
     */
    private String bizCode;

    /**
     * 访问通道：wechat/wecom（T10.1）
     */
    private String accessChannel;

    /**
     * 企业微信 CorpID（T10.1）
     */
    private String wecomCorpId;

    /**
     * 企业微信自建应用 AgentId（T10.1）
     */
    private String wecomAgentId;

    /**
     * 企业微信自建应用 Secret（接口返回脱敏，T10.1）
     */
    private String wecomSecret;

    /**
     * 微信公众号 AppID（T10.1）
     */
    private String mpAppId;

    /**
     * 授权类型
     */
    private String licenseType;

    /**
     * 授权到期日
     */
    private Date expireDate;

    /**
     * 最大用户数
     */
    private Integer maxUsers;

    /**
     * 功能开关JSON
     */
    private String features;

    /**
     * 状态
     */
    private String status;

    /**
     * 租户管理员用户ID
     */
    private Long adminUserId;

    /**
     * 隔离模式：SHARED共享表/DEDICATED独立Schema（预留，FEAT_ISOLATED_SCHEMA）
     */
    private String isolationMode;

    /**
     * 备注
     */
    private String remark;

    private Date createTime;

    private Date updateTime;
}
