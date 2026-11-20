package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 评价视图对象 user_rating（T17.2，小微版 M17）
 *
 * @author ruoyi
 * @date 2026-09-16
 */
@Data
public class UserRatingVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long targetUserId;
    /** 被评价人昵称（回填，非表字段） */
    private String targetUserName;
    private Long authorUserId;
    /** 评价人昵称（回填，非表字段） */
    private String authorUserName;
    private Integer rating;
    private String content;
    private String images;
    private String scene;
    private Long orderId;
    private String status;
    private String replyContent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date replyTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
