package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 点赞对象 user_like（T17.1，小微版 M17 通用评价与点赞）
 * <p>顾客对服务人员的点赞；toggle 切换（已点赞→取消 DELETE / 未点赞→新增 INSERT）。
 * <p>★禁止自赞（target_user_id ≠ author_user_id）。
 *
 * @author ruoyi
 * @date 2026-09-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_like")
public class UserLike extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 被点赞人用户ID（服务人员） */
    private Long targetUserId;

    /** 点赞人用户ID（顾客） */
    private Long authorUserId;

    /** 点赞场景：service/staff */
    private String scene;
}
