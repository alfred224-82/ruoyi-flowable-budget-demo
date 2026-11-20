package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 活动/预告/海报视图对象 promotion_activity（T15.28）
 * <p>老板 PC 管理列表 + lite 档活动浏览页（micro:activity:browse）只读展示；
 * ★浏览页返回体不含任何预约/支付/打卡动作字段（纯只读数据源）。
 *
 * @author ruoyi
 * @date 2026-09-08
 */
@Data
public class PromotionActivityVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 活动标题 */
    private String title;

    /** 类型：activity(活动)/notice(预告)/poster(海报) */
    private String activityType;

    /** 封面图URL */
    private String coverUrl;

    /** 分享海报URL（带租户号二维码） */
    private String posterUrl;

    /** 活动logo（方形标识图，T15.35） */
    private String logoUrl;

    /** 背景主题key（T15.35） */
    private String themeKey;

    /** 活动正文/详情 */
    private String content;

    /** 活动开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 活动结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 状态：DRAFT(草稿)/PUBLISHED(已发布)/ENDED(已结束) */
    private String status;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date publishTime;

    /** 备注 */
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    // ===== M19 活动设置增强字段（v1.52） =====

    /** 自动生成的活动标题（支持手动修改覆盖） */
    private String autoTitle;

    /** 背景图片URL（★仅对当前活动生效，不跨活动复用） */
    private String bgImageUrl;

    /** 活动介绍语音URL（约3分钟，单向播放） */
    private String voiceUrl;

    /** 推荐微信小程序码URL */
    private String qrcodeUrl;

    /** 选中的项目ID */
    private Long selectedProjectId;

    /** 选中的房间ID列表（JSON数组） */
    private String selectedRoomIds;

    /** 选中的员工ID列表（JSON数组） */
    private String selectedStaffIds;

    // ===== M22 活动费用与参与增强字段（v1.79） =====

    /** Boss自定义总价 */
    private BigDecimal customPrice;

    /** 系统自动计算总价（房间+助教×时长） */
    private BigDecimal autoCalculatedPrice;

    /** Boss设定的参与人数（用于计算人均费用） */
    private Integer participantCount;

    /** 是否锁定 */
    private Boolean isLocked;

    /** 人均费用（服务端计算回填：amount / participantCount） */
    private BigDecimal perPersonPrice;

    // ===== REQ-02 活动应收/实收拆分字段（M29_02，v1.3） =====

    /** 应收金额（活动发布时锁定=活动总价；NULL=草稿/无价格） */
    private BigDecimal receivableAmount;

    /** 实收金额（活动结束后回写；未结束为0） */
    private BigDecimal receivedAmount;

    /** 实收回写确认时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date receivedConfirmedTime;

    // ===== 活动统计字段（boss 小程序「活动情况」页展示，暂默认 0） =====

    /** 浏览量 */
    private Integer viewCount;

    /** 分享次数 */
    private Integer shareCount;

    // ===== M19 解析后的展示字段（非表字段，服务端回填） =====

    /** 选中项目名（解析回填） */
    private String projectName;

    /** 选中房间名列表（解析回填，如 [{"id":1,"name":"房间A"}]） */
    private String roomNames;

    /** 选中员工名列表（解析回填，如 [{"id":100,"name":"张三"}]） */
    private String staffNames;
}
