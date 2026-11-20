package com.ruoyi.system.domain.bo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 扫码归因请求业务对象（T16.6，小微版 M16 推广分销）
 * <p>顾客端（H5/小程序）扫码登录后以 JSON 请求体提交，后端 @RequestBody 接收。
 *
 * @author ruoyi
 * @date 2026-10-17
 */
@Data
public class PromotionScanBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 小程序码 scene 参数(≤32字符)，格式 t={tenantId}&u={推广人ID} */
    @NotBlank(message = "scene参数不能为空")
    private String sceneParam;

    /** 扫码顾客用户ID */
    @NotNull(message = "顾客用户ID不能为空")
    private Long customerUserId;

    /** 顾客手机号（可选） */
    private String customerPhone;
}
