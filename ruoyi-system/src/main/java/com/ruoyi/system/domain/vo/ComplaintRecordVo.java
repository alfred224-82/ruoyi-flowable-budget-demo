package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 投诉记录视图对象（T25.1，小微版 M25）
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Data
public class ComplaintRecordVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 租户名称（回填，非表字段） */
    private String tenantName;

    /** 关联活动ID */
    private Long activityId;

    /** 活动名称（回填，非表字段） */
    private String activityTitle;

    /** 被投诉员工 user_id */
    private Long staffId;

    /** 被投诉员工姓名 */
    private String staffName;

    /** 投诉人手机号 */
    private String complainantPhone;

    /** 投诉人姓名 */
    private String complainantName;

    /** 投诉渠道：BOSS / PLATFORM */
    private String complaintChannel;

    /** 投诉类别 */
    private String category;

    /** 投诉文字描述 */
    private String contentText;

    /** 附加图片URL列表（JSON 数组） */
    private String photoUrls;

    /** 取证照片URL列表（JSON 数组） */
    private String evidencePhotos;

    /** 状态：PENDING / PROCESSING / RESOLVED / CLOSED */
    private String status;

    /** 处理回复 */
    private String handlerReply;

    /** 处理人 user_id */
    private Long handlerUserId;

    /** 处理人姓名 */
    private String handlerName;

    /** 提交时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submitTime;

    /** 处理时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date handleTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
