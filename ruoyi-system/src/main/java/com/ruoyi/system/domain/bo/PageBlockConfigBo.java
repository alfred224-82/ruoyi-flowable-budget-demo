package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 页面区块配置业务对象 page_block_config（T15.17 页面装修：单区块新增/编辑 + 查询过滤）
 * <p>批量保存（勾选/拖拽排序/设参数）用 {@link PageDecorateBo} 承载整页区块清单。
 * <p>★老板只做「个性化排版」，不涉及菜单/perms/区块权限配置；权限是安全底线，配置永不突破权限（见 /page/config 二次过滤）。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class PageBlockConfigBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @NotNull(message = "主键ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 标准角色编码(boss/salesman/customer) */
    @NotBlank(message = "角色编码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String roleCode;

    /** 页面标识(home/mine/booking/report) */
    @NotBlank(message = "页面标识不能为空", groups = {AddGroup.class, EditGroup.class})
    private String pageKey;

    /** 区块 key(指向组件库：revenue_week/quick_booking...) */
    @NotBlank(message = "区块key不能为空", groups = {AddGroup.class, EditGroup.class})
    private String blockKey;

    /** 区块来源：micro(小微版)/enterprise(企业版)，默认 micro */
    private String moduleType;

    /** 区块排序 */
    private Integer sortOrder;

    /** 是否显示(0隐藏 1显示) */
    private Integer showFlag;

    /** 区块自定义参数(JSON字符串) */
    private String blockParams;
}
