package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * Boss 新增活动「背景画布加人」批量保存业务对象（T26.10，小微版 M26）
 * <p>Boss 发布/编辑活动时，在房间座位布局背景画布上点座位「＋」号选人：
 * 候选人为租户下全部员工（导游/司机=salesman）与注册游客（customer），选中后座位显示该人头像。
 * <p>★全量替换语义：仅替换本活动 PRESET/BOSS_ASSIGN 记录，顾客自选（CUSTOMER_PICK）不受影响；
 *   员工（salesman）落 PRESET（橙色，不占顾客名额），游客落 BOSS_ASSIGN（已占）。
 * <p>★服务端校验：活动未锁定 / 人员存在 / 座位与人员不重复 / 目标座位未被顾客自选。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
@Data
public class SeatCanvasPersonBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 活动ID */
    @NotNull(message = "活动ID不能为空", groups = {AddGroup.class})
    private Long activityId;

    /** 画布加人明细（空列表=清空本活动全部预置/指派人员，顾客自选保留） */
    private List<Item> items;

    /**
     * 单个座位的加人明细
     */
    @Data
    public static class Item implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 座位ID（FK→room_seat.id） */
        @NotNull(message = "座位ID不能为空", groups = {AddGroup.class})
        private Long seatId;

        /** 座位号（前端传入，服务端以 room_seat 为准兜底回填） */
        private String seatNo;

        /** 被加人 user_id（FK→sys_user.user_id；员工或游客） */
        @NotNull(message = "被加人不能为空", groups = {AddGroup.class})
        private Long userId;

        /** 备注 */
        private String remark;
    }
}
