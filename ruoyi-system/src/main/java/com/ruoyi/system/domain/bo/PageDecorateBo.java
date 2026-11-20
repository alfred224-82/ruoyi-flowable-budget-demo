package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

/**
 * 页面装修批量保存业务对象（T15.17：选择页面+角色 → 勾选区块/拖拽排序/设参数 → 整页持久化）
 * <p>提交某租户某角色某页面的完整有序区块清单，服务层按 (tenant,roleCode,pageKey[,moduleType]) 全量覆盖（先删后插），小程序实时生效。
 * <p>blocks 为空清单表示「清空该页所有区块」；恢复默认走独立 resetDefault 接口（从 tenant_id=0 系统模板克隆）。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class PageDecorateBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 标准角色编码(boss/salesman/customer) */
    @NotBlank(message = "角色编码不能为空", groups = {AddGroup.class})
    private String roleCode;

    /** 页面标识(home/mine/booking/report) */
    @NotBlank(message = "页面标识不能为空", groups = {AddGroup.class})
    private String pageKey;

    /** 区块来源：micro(小微版)/enterprise(企业版)，默认 micro */
    private String moduleType;

    /** 目标租户ID（仅超管可指定，0=系统默认模板；非超管忽略，强制本租户） */
    private Long tenantId;

    /** 整页区块清单（有序；每项含 blockKey/sortOrder/showFlag/blockParams，roleCode/pageKey 以本对象为准） */
    @Valid
    private List<PageBlockConfigBo> blocks;
}
