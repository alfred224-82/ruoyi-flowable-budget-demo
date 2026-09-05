package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * AI引导话术配置业务对象 fee_ai_prompt_config
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FeeAiPromptConfigBo extends BaseEntity {

    /**
     * 主键ID
     */
    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 场景编码（EXPENSE_BASIC/ASSET/LOAN/FOREIGN）
     */
    @NotBlank(message = "场景编码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String sceneCode;

    /**
     * 场景名称
     */
    @NotBlank(message = "场景名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String sceneName;

    /**
     * AI引导话术模板
     */
    @NotBlank(message = "话术模板不能为空", groups = {AddGroup.class, EditGroup.class})
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
