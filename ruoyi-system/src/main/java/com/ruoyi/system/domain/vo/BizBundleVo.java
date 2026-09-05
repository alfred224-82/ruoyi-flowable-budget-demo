package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 业态包视图对象 biz_bundle（T10.2）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class BizBundleVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业态包编码 */
    private String bizCode;

    /** 业态包名称 */
    private String name;

    /** 业态菜单声明 JSON */
    private String menus;

    /** 文案映射 JSON */
    private String copy;

    /** Schema 页面区块声明 JSON */
    private String schemaBlocks;

    /** 组队/成团引擎策略 Bean 名 */
    private String engineStrategy;

    /** 业态包声明的开关编码数组 JSON */
    private String featureFlags;

    /** 状态（0正常 1停用） */
    private String status;

    /** 备注 */
    private String remark;

    private Date createTime;

    private Date updateTime;
}
