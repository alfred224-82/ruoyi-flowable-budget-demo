package com.ruoyi.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * Boss 补录打卡业务对象（T24.1，小微版 M24）
 * <p>Boss 代员工补录打卡（员工忘打时），source=SUPPLEMENT。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Data
public class CheckInSupplementBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 被补录员工 user_id */
    @NotNull(message = "员工ID不能为空", groups = {AddGroup.class})
    private Long staffUserId;

    /** 活动ID */
    @NotNull(message = "活动ID不能为空", groups = {AddGroup.class})
    private Long activityId;

    /** 补录打卡时间（Boss 指定） */
    @NotNull(message = "打卡时间不能为空", groups = {AddGroup.class})
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date checkInTime;

    /** 打卡地点名称 */
    private String locationName;

    /** 补录原因 */
    private String supplementReason;
}
