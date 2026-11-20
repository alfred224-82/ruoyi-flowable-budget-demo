package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 投诉记录 complaint_record（T25.1，小微版 M25）
 * <p>顾客双渠道投诉：BOSS 门店内部 / PLATFORM 平台 Admin 违法违规。
 * <p>★渠道一（BOSS）无额外门槛；渠道二（PLATFORM）必须拍照取证+文字≥20字。
 * <p>租户级表（含 tenant_id），多租户插件自动隔离。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("complaint_record")
public class ComplaintRecord extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联活动ID（FK→promotion_activity.id，可空=不关联活动）
     */
    private Long activityId;

    /**
     * 被投诉员工 user_id（可空=不指定员工）
     */
    private Long staffId;

    /**
     * 被投诉员工姓名（冗余快照）
     */
    private String staffName;

    /**
     * 投诉人手机号
     */
    private String complainantPhone;

    /**
     * 投诉人姓名
     */
    private String complainantName;

    /**
     * 投诉渠道：BOSS=渠道一（Boss 门店内部）/ PLATFORM=渠道二（平台 Admin 违法违规）
     */
    private String complaintChannel;

    /**
     * 投诉类别（渠道二必填：黄赌毒/违法宣传/违规活动/其他）
     */
    private String category;

    /**
     * 投诉文字描述（渠道二要求≥20字）
     */
    private String contentText;

    /**
     * 附加图片URL列表（JSON 数组，渠道一可选≤3张，渠道二不使用此字段）
     */
    private String photoUrls;

    /**
     * 取证照片URL列表（JSON 数组，渠道二必填≥1张，仅摄像头拍摄）
     */
    private String evidencePhotos;

    /**
     * 状态：PENDING=待处理 / PROCESSING=处理中 / RESOLVED=已解决 / CLOSED=已关闭
     */
    private String status;

    /**
     * 处理回复（Boss/平台 Admin 填写）
     */
    private String handlerReply;

    /**
     * 处理人 user_id
     */
    private Long handlerUserId;

    /**
     * 处理人姓名（冗余快照）
     */
    private String handlerName;

    /**
     * 提交时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submitTime;

    /**
     * 处理时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date handleTime;
}
