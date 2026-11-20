package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 小程序场景/分享对象 share_scene（T15.3，扫码/分享带租户号）
 * <p>scene_id 为小程序 scene 参数，扫码/分享进入即锁定租户（T15.19）；生成分享海报（T15.24，micro:promo:poster）写入本表。
 * <p>uk_scene(scene_id) 全局唯一。
 * <p>表含 create_by/create_time/update_time 但【无 update_by、无 del_flag】，故不继承 BaseEntity（避免 updateFill 写入不存在的 update_by 列），
 * 且不加 @TableLogic（物理删除）；tenantId 标注 {@code @TableField(fill = INSERT)} 由登录上下文填充，create_by 由服务层显式设置，
 * create_time/update_time 依赖 DB DEFAULT CURRENT_TIMESTAMP/ON UPDATE 兜底。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
@TableName("share_scene")
public class ShareScene implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /**
     * 场景标识(小程序 scene 参数，扫码/分享进入锁定租户)
     */
    private String sceneId;

    /**
     * 业务类型(shop店铺/activity活动/booking预约)
     */
    private String bizType;

    /**
     * 业务ID(活动ID/服务ID等)
     */
    private Long bizId;

    /**
     * 二维码/海报图URL(OSS)
     */
    private String qrCodeUrl;

    /**
     * 过期时间(NULL=永久)
     */
    private Date expireTime;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
