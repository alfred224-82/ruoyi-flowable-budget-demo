package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 引导态门户访问审计对象 portal_access_log（T8.8）
 * <p>系统级共享表（引导态跨租户、无登录态），不含 tenant_id，已纳入 IGNORE_TENANT_TABLES；仅追加。
 * <p>记录每一次匿名 /portal/** 门店反查：来源IP / 端点 / 入参 / openid / 命中租户 / 结果，
 * 用于安全审查（检测篡改 ?tenant= 探测、批量爬取公开展示数据）。
 * <p>非 BaseEntity：create_time 依赖 DB 默认值 CURRENT_TIMESTAMP（配合全局 insertStrategy=NOT_NULL）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@TableName("portal_access_log")
public class PortalAccessLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 来源IP */
    private String clientIp;

    /** 访问端点(requestURI) */
    private String endpoint;

    /** 请求参数(queryString,截断) */
    private String requestParam;

    /** 微信openid(按openid反查时记录) */
    private String openid;

    /** 命中的租户ID(未命中为NULL) */
    private Long resolvedTenantId;

    /** 是否成功命中(0否 1是) */
    private Integer success;

    /** 失败/异常原因 */
    private String errorMsg;

    /** 访问时间 */
    private Date createTime;
}
