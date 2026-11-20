package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 扫码 scene 解析结果（T29.3 员工/顾客扫码带租户直接登录）
 * <p>二维码 scene 编码 {@code t={tenantId}&s={storeId}&r={role}}，扫码进入小程序后由
 * {@code POST /micro/login/resolveScene} 校验合法性（租户存在/未停用/未过期、门店归属该租户），
 * 校验通过后返回本 Vo，前端据此锁定门店上下文并跳过「输入租户号 + 门店选择」两步，直接进入登录页。
 * <p>遵循 Vo 约定：仅 {@code @Data} + {@code Serializable}，禁用 {@code @AutoMapper}。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Data
public class MicroSceneVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 租户ID（scene t 参数，校验通过后回填） */
    private Long tenantId;

    /** 租户号（sys_tenant.tenant_code，前端持久化用于后续免输） */
    private String tenantCode;

    /** 租户/门店名称（sys_tenant.company_name，登录页大字号展示） */
    private String tenantName;

    /** 门店ID（scene s 参数；本系统租户即门店，缺省等于 tenantId） */
    private Long storeId;

    /** 角色（scene r 参数：boss/salesman/customer，可空，仅作前端引导提示） */
    private String role;

    /** 门店Logo（引导态公开展示） */
    private String logoUrl;

    /** 所在城市（引导态公开展示） */
    private String city;

}
