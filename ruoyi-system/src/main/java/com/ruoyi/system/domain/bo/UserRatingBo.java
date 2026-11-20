package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 评价业务对象 user_rating（T17.2，小微版 M17）
 *
 * @author ruoyi
 * @date 2026-09-16
 */
@Data
public class UserRatingBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 被评价人用户ID */
    @NotNull(message = "被评价人不能为空", groups = {AddGroup.class})
    private Long targetUserId;

    /** 评分（1~5） */
    @NotNull(message = "评分不能为空", groups = {AddGroup.class})
    @Min(value = 1, message = "评分最小1")
    @Max(value = 5, message = "评分最大5")
    private Integer rating;

    /** 评价内容 */
    @NotBlank(message = "评价内容不能为空", groups = {AddGroup.class})
    private String content;

    /** 评价图片URL列表（JSON数组） */
    private String images;

    /** 评价场景 */
    @NotBlank(message = "评价场景不能为空", groups = {AddGroup.class})
    private String scene;

    /** 关联订单ID */
    private Long orderId;

    /** 状态：NORMAL/SHIELDED */
    private String status;

    /** 老板回复内容 */
    private String replyContent;

    /** 备注 */
    private String remark;
}
