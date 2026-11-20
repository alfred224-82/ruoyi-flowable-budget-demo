package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 活动设置保存业务对象（M19 T19.1~T19.5）
 * <p>老板 PC「活动设置」页面保存：项目/房间/员工选择 + 背景图 + 语音 + 手动标题覆盖。
 * <p>★背景图仅对当前活动生效，不跨活动复用（对齐需求草稿 §3.6）。
 *
 * @author ruoyi
 * @date 2026-09-21
 */
@Data
public class ActivitySettingBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 活动ID（必须已存在的活动） */
    @NotNull(message = "活动ID不能为空", groups = {EditGroup.class})
    private Long activityId;

    /** 选中的项目ID（service_item.id，排除 space 分类；单选） */
    private Long selectedProjectId;

    /** 选中的房间ID列表（service_item.id，仅 space 分类；单/多选） */
    private List<Long> selectedRoomIdList;

    /** 选中的员工ID列表（sys_user.user_id，salesman 角色；单/多选） */
    private List<Long> selectedStaffIdList;

    /** 手动修改的活动标题（覆盖自动生成；为空时由后端自动生成） */
    private String manualTitle;

    /** 背景图片URL（★仅对当前活动生效，不跨活动复用） */
    private String bgImageUrl;

    /** 活动介绍语音URL（约3分钟，单向播放） */
    private String voiceUrl;

    /** 活动其他成本（场地/物料等，M35 T35.4 定价页算账用；员工成本走快照不在此列） */
    private BigDecimal otherCost;
}
