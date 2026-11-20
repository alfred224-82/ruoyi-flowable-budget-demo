package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 推广码对象 promotion_code（T16.1/T16.5，小微版 M16 推广分销）
 * <p>门店码 + 个人推广码；一人一码、永久有效（无过期字段）；按 tenant_id 隔离。
 * <p>code_type：store(门店码 scene=t=租户ID) / personal(个人码 scene=t=租户ID&r=角色&u=用户ID)。
 * <p>★禁用后新扫码不再归因，禁用前消费照常算（status=1 禁用）。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("promotion_code")
public class PromotionCode extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 推广人用户ID */
    private Long userId;

    /** 推广人标准角色：boss/salesman/customer */
    private String roleCode;

    /** 码类型：store(门店码)/personal(个人推广码) */
    private String codeType;

    /** 小程序码 scene 参数(≤32字符) */
    private String sceneParam;

    /** 推广码图片URL */
    private String qrCodeUrl;

    /** 状态：0启用 1禁用 */
    private String status;

    @TableLogic
    private String delFlag;

    private String remark;
}
