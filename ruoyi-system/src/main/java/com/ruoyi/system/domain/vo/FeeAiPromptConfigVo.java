package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AI引导话术配置视图对象 fee_ai_prompt_config
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Data
@ExcelIgnoreUnannotated
public class FeeAiPromptConfigVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private Long id;

    /**
     * 场景编码（EXPENSE_BASIC/ASSET/LOAN/FOREIGN）
     */
    @ExcelProperty(value = "场景编码")
    private String sceneCode;

    /**
     * 场景名称
     */
    @ExcelProperty(value = "场景名称")
    private String sceneName;

    /**
     * AI引导话术模板
     */
    @ExcelProperty(value = "话术模板")
    private String promptTemplate;

    /**
     * 提问顺序JSON
     */
    private String questionSequence;

    /**
     * 行业编码（*=通用）
     */
    @ExcelProperty(value = "行业编码")
    private String industryCode;

    /**
     * 状态（ENABLED/DISABLED）
     */
    @ExcelProperty(value = "状态")
    private String status;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
