package com.ruoyi.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 项目统一容器业务对象 project（T11.1）
 * <p>企业任务与活动报名共用；业态差异字段进 extraConfig JSON，不新增物理列。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class ProjectBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @NotNull(message = "项目容器ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 业态包编码（空则取当前租户 biz_code，默认 fund_control） */
    private String bizCode;

    /** 容器类型：activity/team_buy/general（ActivityController 自动填充，ProjectController 需前端传值，业务层校验） */
    private String type;

    /** 项目/活动名称 */
    @NotBlank(message = "名称不能为空", groups = {AddGroup.class})
    private String name;

    /** 状态：DRAFT/PUBLISHED/ONGOING/FINISHED/CLOSED/CANCELLED */
    private String status;

    /** 负责人用户ID（空则取当前登录用户） */
    private Long ownerUserId;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 业态差异字段 JSON */
    private String extraConfig;

    /** 备注 */
    private String remark;

    /** 初始成员（新建容器时一并写入，五枚举角色） */
    private List<ProjectMemberBo> members;

    /**
     * 项目成员项（五枚举角色）
     */
    @Data
    public static class ProjectMemberBo implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 用户ID */
        @NotNull(message = "成员用户不能为空", groups = {AddGroup.class})
        private Long userId;
        /** 容器角色：owner/manager/member/participant/signup */
        private String role;
    }
}
