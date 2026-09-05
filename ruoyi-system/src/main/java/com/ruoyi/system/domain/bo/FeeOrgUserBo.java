package com.ruoyi.system.domain.bo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 用户挂组织业务对象（T7.1 / FEAT_MULTI_ORG）
 * 全量保存：以本次提交的成员列表为准
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeOrgUserBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 组织ID */
    @NotNull(message = "组织ID不能为空")
    private Long orgId;

    /** 组织成员列表 */
    private List<OrgUserItem> users;

    /**
     * 成员项
     */
    @Data
    public static class OrgUserItem implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 用户ID */
        @NotNull(message = "成员用户不能为空")
        private Long userId;
        /** 用户姓名 */
        private String userName;
        /** 是否主组织 */
        private Integer isPrimary;
    }
}
