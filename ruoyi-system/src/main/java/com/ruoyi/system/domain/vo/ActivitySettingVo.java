package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 活动设置视图对象（M19 T19.1~T19.5）
 * <p>包含活动设置保存后回显的完整信息：选择的项目/房间/员工名称 + 自动标题 + 背景图/语音/二维码URL。
 *
 * @author ruoyi
 * @date 2026-09-21
 */
@Data
public class ActivitySettingVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 活动ID */
    private Long activityId;

    /** 活动标题（原始 title） */
    private String title;

    /** 自动生成的标题（auto_title） */
    private String autoTitle;

    /** 背景图片URL */
    private String bgImageUrl;

    /** 语音URL */
    private String voiceUrl;

    /** 推荐小程序码URL */
    private String qrcodeUrl;

    /** 选中的项目ID */
    private Long selectedProjectId;

    /** 选中的项目名 */
    private String projectName;

    /** 选中的房间ID列表 */
    private List<Long> selectedRoomIdList;

    /** 选中的房间名列表（[{id, name}]） */
    private List<Map<String, Object>> selectedRoomList;

    /** 选中的员工ID列表 */
    private List<Long> selectedStaffIdList;

    /** 选中的员工名列表（[{id, name}]） */
    private List<Map<String, Object>> selectedStaffList;

    /** 标题预览（manualTitle 优先，否则 autoTitle） */
    private String titlePreview;

    /** 活动其他成本（M35 T35.4 定价页算账用） */
    private BigDecimal otherCost;
}
