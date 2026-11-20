package com.ruoyi.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Date;

/**
 * 小程序场景/分享业务对象 share_scene（T15.3/T15.24 生成分享海报写入；T15.19 扫码锁定租户查询）
 * <p>sceneId 全局唯一（uk_scene）；扫码进入时按 sceneId 反查 tenantId + 业务信息，实现「扫码/分享进入即锁定租户」。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class ShareSceneBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 场景标识(小程序 scene 参数) */
    @NotBlank(message = "场景标识不能为空", groups = {AddGroup.class})
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
}
