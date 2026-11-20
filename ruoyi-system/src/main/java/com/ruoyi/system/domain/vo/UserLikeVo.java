package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 点赞视图对象 user_like（T17.1，小微版 M17）
 *
 * @author ruoyi
 * @date 2026-09-16
 */
@Data
public class UserLikeVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long targetUserId;
    private Long authorUserId;
    private String scene;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
