package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 活动座位图视图对象（T26.7，小微版 M26）
 * <p>顾客小程序选座页 / Boss 手机查看座位图共用。
 * <p>★坐标以背景图百分比存储（0~1），前端按容器实际尺寸换算像素，保证 PC / 手机 / 平板三端渲染一致。
 * <p>★不使用颜色区分状态，用图标+文字表达（空闲=加号 / 已占=头像+小字座位号 / 预置=橙色标记）。
 * <p>★座位标记 40px（PC 32px）。
 * <p>★Vo 约定：仅用 @Data + Serializable，禁用 @AutoMapper。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Data
public class SeatMapVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 活动ID */
    private Long activityId;

    /** 活动标题（回填，便于前端展示） */
    private String activityTitle;

    /** 房间ID（FK→service_item.id，category_type=space） */
    private Long roomId;

    /** 房间名称（回填） */
    private String roomName;

    /** 布局ID（FK→room_seat_layout.id；NULL=房间未配置座位布局，has_seat_layout=0） */
    private Long layoutId;

    /** 背景图 OSS URL（NULL=未配置布局） */
    private String bgImageUrl;

    /** 背景图来源：UPLOAD=Boss 自传 / PRESET=平台默认模板 */
    private String bgImageSource;

    /** 布局宽度（像素，默认 1920） */
    private Integer layoutWidth;

    /** 布局高度（像素，默认 1080） */
    private Integer layoutHeight;

    /** 活动是否已锁定（M28 T28.1；锁定后所有座位只读） */
    private Boolean activityLocked;

    /** 座位总数（含空闲+已占+预置） */
    private Integer totalSeats;

    /** 已占座位数 */
    private Integer occupiedSeats;

    /** 空闲座位数 */
    private Integer freeSeats;

    /** 预置人员座位数（导游/司机，橙色渲染，不参与顾客选座） */
    private Integer presetSeats;

    /** 当前登录用户已占座位ID（NULL=未选座；顾客端用于高亮自己的座位） */
    private Long mySeatId;

    /** 座位列表（含空闲 / 已占 / 预置三态） */
    private List<SeatVo> seats;

    /**
     * 单个座位视图对象
     */
    @Data
    public static class SeatVo implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 座位ID（FK→room_seat.id；NULL=未配置布局时占位） */
        private Long seatId;

        /** 座位号（如 A1 / 1 / 前排-左1） */
        private String seatNo;

        /** X 坐标百分比 0~1（前端按容器实际尺寸换算像素） */
        private BigDecimal posXPercent;

        /** Y 坐标百分比 0~1 */
        private BigDecimal posYPercent;

        /** 是否预置人员（导游/司机）：0=否 1=是 */
        private Integer isPreset;

        /** 预置角色：guide/driver/assistant（isPreset=1 时有值） */
        private String presetRole;

        /** 预置人员 user_id（isPreset=1 时有值） */
        private Long presetUserId;

        /** 座位状态：FREE=空闲 / OCCUPIED=已占 / PRESET=预置人员 */
        private String status;

        /** 占座记录ID（activity_seat_assignment.id；status != FREE 时有值；顾客取消座位时使用） */
        private Long assignmentId;

        /** 占座人 user_id（status != FREE 时有值） */
        private Long userId;

        /** 占座人昵称（回填；顾客端点击头像仅显示姓名，不显示手机号） */
        private String userNickName;

        /** 占座人头像 URL（回填；顾客端仅显示小图） */
        private String userAvatar;

        /** 占座类型：PRESET / BOSS_ASSIGN / CUSTOMER_PICK */
        private String assignmentType;

        /** ★T26.10 占座人角色显示名（tenant_role_mapping 动态映射：salesman→导游/司机，customer→游客）；顾客端弹窗展示，避免硬编码角色术语 */
        private String roleDisplayName;

        /** 备注 */
        private String remark;
    }
}
