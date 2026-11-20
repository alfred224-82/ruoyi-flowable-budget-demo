package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 活动/预告/海报对象 promotion_activity（T15.28，小微版 lite 档活动浏览页只读数据源）
 * <p>老板 PC「推广发布」(micro:promo:*) 创建/发布；lite 档顾客/助教「活动浏览页」(micro:activity:browse)
 * 按 tenant_id 只读本店 status=PUBLISHED 的活动（★无需登录可浏览）。
 * <p>租户级表（含 tenant_id），多租户插件自动隔离；含 del_flag 逻辑删除。
 * <p>★lite 档浏览页无任何预约/支付/打卡动作（纯只读数据源；pro 档在同源数据上扩展预约动作）。
 *
 * @author ruoyi
 * @date 2026-09-08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("promotion_activity")
public class PromotionActivity extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 活动标题
     */
    private String title;

    /**
     * 类型：activity(活动)/notice(预告)/poster(海报)
     */
    private String activityType;

    /**
     * 封面图URL
     */
    private String coverUrl;

    /**
     * 分享海报URL（带租户号二维码）
     */
    private String posterUrl;

    /**
     * 活动logo（方形标识图，列表卡片左上角图标，T15.35）
     */
    private String logoUrl;

    /**
     * 背景主题key（T15.35；M27 T27.1 命名对齐：theme_fresh/theme_vivid/theme_dark/theme_handdrawn）
     */
    private String themeKey;

    /**
     * 活动正文/详情
     */
    private String content;

    /**
     * 活动开始时间
     */
    private Date startTime;

    /**
     * 活动结束时间
     */
    private Date endTime;

    /**
     * 状态：DRAFT(草稿)/PUBLISHED(已发布)/ENDED(已结束)
     */
    private String status;

    /**
     * 发布时间
     */
    private Date publishTime;

    /**
     * 删除标志（0存在 2删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注
     */
    private String remark;

    // ===== M19 活动设置增强字段（v1.52） =====

    /**
     * 自动生成的活动标题（支持手动修改覆盖）
     * 拼接规则：{房间名}玩{项目名}，由{员工名}主持
     */
    private String autoTitle;

    /**
     * 背景图片URL（★仅对当前活动生效，不跨活动复用）
     */
    private String bgImageUrl;

    /**
     * 活动介绍语音URL（约3分钟，单向播放）
     */
    private String voiceUrl;

    /**
     * 推荐微信小程序码URL
     */
    private String qrcodeUrl;

    /**
     * 选中的项目ID（service_item，排除space分类）
     */
    private Long selectedProjectId;

    /**
     * 选中的房间ID列表（JSON数组，service_item仅space分类）
     */
    private String selectedRoomIds;

    /**
     * 选中的员工ID列表（JSON数组，salesman角色用户）
     */
    private String selectedStaffIds;

    // ===== M22 活动费用与参与增强字段（v1.79） =====

    /**
     * Boss自定义总价（为空则用auto_calculated_price）
     */
    private BigDecimal customPrice;

    /**
     * 系统自动计算总价（房间+助教×时长）
     */
    private BigDecimal autoCalculatedPrice;

    /**
     * Boss设定的参与人数（用于计算人均费用）
     */
    private Integer participantCount;

    /**
     * 活动其他成本（场地/物料等，M35 T35.4 定价页算账用；
     * 员工成本走 activity_staff_snapshot 快照，不在此列）
     */
    private BigDecimal otherCost;

    /**
     * 是否锁定（0=未锁定,1=已锁定；锁定后不可报名/取消）
     */
    private Boolean isLocked;

    // ===== REQ-02 活动应收/实收拆分字段（M29_02，v1.3） =====

    /**
     * 应收金额（活动发布时锁定=活动总价 COALESCE(customPrice, autoCalculatedPrice)；NULL=草稿/无价格）
     */
    private BigDecimal receivableAmount;

    /**
     * 实收金额（活动结束后回写=人均应付×已支付报名人数；未结束为0）
     */
    private BigDecimal receivedAmount;

    /**
     * 实收回写确认时间（活动结束时置当前时间）
     */
    private Date receivedConfirmedTime;
}
