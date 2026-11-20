package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 页面区块配置视图对象 page_block_config（T15.16 /page/config 返回结构 + T15.17 装修面板）
 * <p>/page/config 返回有序区块清单（blockKey + sortOrder + blockParams），小程序页面壳循环渲染 {@code <component is="{{item.blockKey}}" />}。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class PageBlockConfigVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID(0=系统默认模板) */
    private Long tenantId;

    /** 标准角色编码(boss/salesman/customer) */
    private String roleCode;

    /** 页面标识(home/mine/booking/report) */
    private String pageKey;

    /** 区块 key(指向组件库：revenue_week/quick_booking...) */
    private String blockKey;

    /** 区块来源：micro(小微版)/enterprise(企业版) */
    private String moduleType;

    /** 区块排序 */
    private Integer sortOrder;

    /** 是否显示(0隐藏 1显示) */
    private Integer showFlag;

    /** 区块自定义参数(JSON字符串) */
    private String blockParams;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
