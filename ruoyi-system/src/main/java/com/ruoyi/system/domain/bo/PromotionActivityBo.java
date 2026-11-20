package com.ruoyi.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 活动/预告/海报业务对象 promotion_activity（T15.28）
 * <p>老板 PC「推广发布」创建/编辑/发布活动；兼作查询过滤：title/activityType/status。
 *
 * @author ruoyi
 * @date 2026-09-08
 */
@Data
public class PromotionActivityBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @NotNull(message = "活动ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 活动标题 */
    @NotBlank(message = "活动标题不能为空", groups = {AddGroup.class, EditGroup.class})
    private String title;

    /** 类型：activity(活动)/notice(预告)/poster(海报) */
    private String activityType;

    /** 封面图URL */
    private String coverUrl;

    /** 分享海报URL（带租户号二维码） */
    private String posterUrl;

    /** 活动logo（方形标识图，T15.35） */
    private String logoUrl;

    /** 背景主题key（T15.35；M27 T27.1 命名对齐：theme_fresh/theme_vivid/theme_dark/theme_handdrawn） */
    private String themeKey;

    /** 活动正文/详情 */
    private String content;

    /** 活动开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 活动结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 状态（查询过滤用）：DRAFT/PUBLISHED/ENDED */
    private String status;

    /** 备注 */
    private String remark;

    // ===== M19 活动设置增强字段（v1.52） =====

    /** 自动生成的活动标题（支持手动修改覆盖） */
    private String autoTitle;

    /** 背景图片URL（★仅对当前活动生效，不跨活动复用） */
    private String bgImageUrl;

    /** 活动介绍语音URL（约3分钟，单向播放） */
    private String voiceUrl;

    /** 推荐微信小程序码URL */
    private String qrcodeUrl;

    /** 选中的项目ID（service_item，排除space分类） */
    private Long selectedProjectId;

    /** 选中的房间ID列表（JSON数组） */
    private String selectedRoomIds;

    /** 选中的员工ID列表（JSON数组） */
    private String selectedStaffIds;

    // ===== M22 活动费用与参与增强字段（v1.79） =====

    /** Boss自定义总价（为空则用auto_calculated_price） */
    private BigDecimal customPrice;

    /** Boss设定的参与人数（用于计算人均费用） */
    private Integer participantCount;

    /** 是否锁定 */
    private Boolean isLocked;
}
