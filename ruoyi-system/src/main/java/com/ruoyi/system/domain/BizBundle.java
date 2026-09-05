package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业态包对象 biz_bundle（T1.7 / T10.2 多业态平台底座）
 * <p>系统级共享表，不含 tenant_id（已加入 IGNORE_TENANT_TABLES）；一行一业态，
 * 新增业态仅需插数据 + 补文案映射，不动核心表结构、不改核心代码（设计文档 1.8.1 三层架构）。
 * <p>menus/copy/schema_blocks/feature_flags 均为 JSON 文本，由服务层解析。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_bundle")
public class BizBundle extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 业态包编码（主键）：fund_control/boardgame/fresh_food
     */
    @TableId(value = "biz_code", type = IdType.INPUT)
    private String bizCode;

    /**
     * 业态包名称
     */
    private String name;

    /**
     * 业态菜单声明（menu_id 数组 JSON，叠加在版本授权模板之上做业态过滤）
     */
    private String menus;

    /**
     * 文案映射（key→业态文案 JSON，前端 t(key) 取值，缺失回落默认包）
     */
    private String copy;

    /**
     * Schema 页面区块声明（声明式表单/列表渲染 JSON，PC 与小程序共用）
     */
    private String schemaBlocks;

    /**
     * 组队/成团引擎策略 Bean 名：FullHouseStrategy/GroupBuyStrategy
     * （策略模式分发，引擎源码禁止 if(bizCode==...)）
     */
    private String engineStrategy;

    /**
     * 业态包声明的开关编码数组 JSON（未声明的能力不开放，参与三维交集）
     */
    private String featureFlags;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;
}
