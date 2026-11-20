package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 小程序场景/分享视图对象 share_scene（T15.3/T15.19/T15.24）
 * <p>扫码/分享进入锁定租户：resolve 接口按 sceneId 返回 tenantId + 业务信息（活动/服务/店铺）。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class ShareSceneVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 场景标识(小程序 scene 参数) */
    private String sceneId;

    /** 业务类型(shop店铺/activity活动/booking预约) */
    private String bizType;

    /** 业务ID(活动ID/服务ID等) */
    private Long bizId;

    /** 二维码/海报图URL(OSS) */
    private String qrCodeUrl;

    /** 过期时间(NULL=永久) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
