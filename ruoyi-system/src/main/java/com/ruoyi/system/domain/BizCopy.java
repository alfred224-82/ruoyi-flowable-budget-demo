package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业态文案映射对象 biz_copy（T10.5 v1.29 文案映射 i18n）
 * <p>系统级共享表，不含 tenant_id（须加入 IGNORE_TENANT_TABLES）；
 * 一行一 (biz_code, i18n_key, lang)，为业态包提供多语言精细化文案。
 * <p>与 {@link BizBundle#getCopy()}（JSON 单语言）互补：biz_bundle.copy 作为默认包（zh-CN 兜底），
 * biz_copy 提供多语言覆盖（zh-CN/en-US/zh-TW 等）。
 * <p>三维合并优先级：租户 module_overrides.copy &gt; biz_copy(biz_code,lang) &gt; biz_bundle.copy &gt; 前端硬编码兜底。
 *
 * @author ruoyi
 * @date 2026-09-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_copy")
public class BizCopy extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 业态包编码（与 biz_bundle.biz_code 对齐；NULL/空=全业态通用）
     */
    private String bizCode;

    /**
     * 文案 key（前端 t(key) 入参，如 project/expense/income/signup/team）
     */
    private String i18nKey;

    /**
     * 语言编码（BCP-47）：zh-CN/en-US/zh-TW/ja-JP 等
     */
    private String lang;

    /**
     * 文案值（当前语言的翻译文本）
     */
    private String copyValue;

    /**
     * 所属模块（可选，用于分组展示：project/expense/budget/micro/common 等）
     */
    private String module;

    /**
     * 状态（0启用 1停用）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;
}
