package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评价对象 user_rating（T17.1/T17.2，小微版 M17 通用评价与点赞）
 * <p>顾客对服务人员 1~5 星评价；★禁止自评（target_user_id ≠ author_user_id）。
 * <p>一条评价仅能回复一次（reply_content 非空即已回复）。
 * <p>图片须走 imgSecCheck 安全校验，URL 存入 images（JSON 数组，最多 9 张）。
 *
 * @author ruoyi
 * @date 2026-09-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_rating")
public class UserRating extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 被评价人用户ID（服务人员） */
    private Long targetUserId;

    /** 评价人用户ID（顾客） */
    private Long authorUserId;

    /** 评分（1~5 星） */
    private Integer rating;

    /** 评价内容 */
    private String content;

    /** 评价图片URL列表（JSON数组，最多9张） */
    private String images;

    /** 评价场景：booking_complete/service_done/visit_done */
    private String scene;

    /** 关联订单ID */
    private Long orderId;

    /** 状态：NORMAL/SHIELDED */
    private String status;

    /** 老板回复内容 */
    private String replyContent;

    /** 老板回复时间 */
    private java.util.Date replyTime;

    @TableLogic
    private String delFlag;

    private String remark;
}
