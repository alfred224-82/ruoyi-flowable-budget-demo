package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI引导话术配置对象 fee_ai_prompt_config
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_ai_prompt_config")
public class FeeAiPromptConfig extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /**
     * 场景编码（EXPENSE_BASIC/ASSET/LOAN/FOREIGN）
     */
    private String sceneCode;

    /**
     * 场景名称
     */
    private String sceneName;

    /**
     * AI引导话术模板
     */
    private String promptTemplate;

    /**
     * 提问顺序JSON
     */
    private String questionSequence;

    /**
     * 行业编码（*=通用）
     */
    private String industryCode;

    /**
     * 状态（ENABLED/DISABLED）
     */
    private String status;
}
