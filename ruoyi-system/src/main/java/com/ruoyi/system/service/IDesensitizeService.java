package com.ruoyi.system.service;

import com.ruoyi.system.domain.FeeDesensitizeRule;

import java.util.List;

/**
 * 数据脱敏Service接口
 * 设计文档 Phase 10 Task 10.3
 *
 * @author ruoyi
 * @date 2026-07-23
 */
public interface IDesensitizeService {

    /**
     * 对文本执行脱敏
     *
     * @param rawText 原始文本
     * @return 脱敏后文本
     */
    String desensitize(String rawText);

    /**
     * 敏感词检测（金融行业拦截）
     *
     * @param text         待检测文本
     * @param industryCode 行业编码
     * @return 是否包含敏感词
     */
    boolean containsSensitiveWord(String text, String industryCode);

    /**
     * 规则列表
     */
    List<FeeDesensitizeRule> listRules();

    /**
     * 保存规则
     */
    Boolean saveRule(FeeDesensitizeRule rule);

    /**
     * 删除规则
     */
    Boolean deleteRule(Long id);
}
