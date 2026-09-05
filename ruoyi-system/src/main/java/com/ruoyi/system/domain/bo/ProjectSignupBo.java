package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 项目活动报名业务对象 project_signup（T11.3）
 * <p>满员进候补、成团判定由 ITeamStrategy 策略驱动，本对象仅承载报名采集字段。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class ProjectSignupBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @NotNull(message = "报名ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 项目容器ID */
    @NotNull(message = "项目容器ID不能为空", groups = {AddGroup.class})
    private Long projectId;

    /** 报名用户ID（可空=游客报名，记 openid） */
    private Long userId;

    /** 报名微信 openid（游客报名） */
    private String openid;

    /** 报名人数 */
    private Integer peopleCount;

    /** 签到状态：NOT_SIGNED/SIGNED/ABSENT（签到接口使用） */
    private String signInStatus;
}
