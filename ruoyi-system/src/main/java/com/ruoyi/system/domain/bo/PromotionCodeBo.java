package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 推广码业务对象 promotion_code（T16.1/T16.5，小微版 M16）
 * <p>首次生成后永久复用；code_type=store/personal；scene_param ≤ 32 字符。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Data
public class PromotionCodeBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 推广人用户ID（查询过滤用） */
    private Long userId;

    /** 推广人角色（查询过滤用）：boss/salesman/customer */
    private String roleCode;

    /** 码类型：store/personal */
    private String codeType;

    /** 小程序码 scene 参数(≤32字符) */
    private String sceneParam;

    /** 推广码图片URL */
    private String qrCodeUrl;

    /** 状态：0启用 1禁用 */
    private String status;

    /** 备注 */
    private String remark;
}
