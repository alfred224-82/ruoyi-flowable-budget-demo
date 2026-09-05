package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.system.domain.FeeDesensitizeRule;
import com.ruoyi.system.mapper.FeeDesensitizeRuleMapper;
import com.ruoyi.system.service.IDesensitizeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 数据脱敏Service业务层处理
 * 设计文档 Phase 10 Task 10.3
 *
 * 脱敏方式：
 * PHONE: 138****1234
 * ID_CARD: 110***********1234
 * BANK_CARD: 6222***********1234
 * NAME: 张*
 * CUSTOM: 按自定义正则替换
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DesensitizeServiceImpl implements IDesensitizeService {

    private final FeeDesensitizeRuleMapper desensitizeRuleMapper;

    @Override
    public String desensitize(String rawText) {
        if (StrUtil.isBlank(rawText)) {
            return rawText;
        }
        String result = rawText;
        List<FeeDesensitizeRule> rules = listEnabledRules();
        for (FeeDesensitizeRule rule : rules) {
            if ("MASK".equals(rule.getInterceptAction())) {
                result = applyMask(result, rule);
            }
        }
        return result;
    }

    @Override
    public boolean containsSensitiveWord(String text, String industryCode) {
        if (StrUtil.isBlank(text)) {
            return false;
        }
        List<FeeDesensitizeRule> rules = listEnabledRules();
        for (FeeDesensitizeRule rule : rules) {
            if (StrUtil.isNotBlank(rule.getSensitiveWords())) {
                // sensitiveWords 是 JSON 数组格式 ["词1","词2"]
                String words = rule.getSensitiveWords().replaceAll("[\\[\\]\"]", "");
                String[] wordArr = words.split(",");
                for (String word : wordArr) {
                    if (StrUtil.isNotBlank(word) && text.contains(word.trim())) {
                        log.warn("[Desensitize] 检测到敏感词: {}", word.trim());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public List<FeeDesensitizeRule> listRules() {
        return desensitizeRuleMapper.selectList(
            Wrappers.<FeeDesensitizeRule>lambdaQuery()
                .orderByAsc(FeeDesensitizeRule::getId)
        );
    }

    @Override
    public Boolean saveRule(FeeDesensitizeRule rule) {
        if (rule.getId() != null) {
            return desensitizeRuleMapper.updateById(rule) > 0;
        }
        if (rule.getStatus() == null) {
            rule.setStatus("ENABLED");
        }
        return desensitizeRuleMapper.insert(rule) > 0;
    }

    @Override
    public Boolean deleteRule(Long id) {
        return desensitizeRuleMapper.deleteById(id) > 0;
    }

    private List<FeeDesensitizeRule> listEnabledRules() {
        return desensitizeRuleMapper.selectList(
            Wrappers.<FeeDesensitizeRule>lambdaQuery()
                .eq(FeeDesensitizeRule::getStatus, "ENABLED")
        );
    }

    /**
     * 应用脱敏规则
     */
    private String applyMask(String text, FeeDesensitizeRule rule) {
        switch (rule.getMaskType()) {
            case "PHONE":
                return text.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
            case "ID_CARD":
                return text.replaceAll("(\\d{3})\\d{11}(\\d{4})", "$1***********$2");
            case "BANK_CARD":
                return text.replaceAll("(\\d{4})\\d{11}(\\d{4})", "$1***********$2");
            case "NAME":
                return text.replaceAll("(\\S)(\\S+)", "$1*");
            case "CUSTOM":
                if (StrUtil.isNotBlank(rule.getMaskPattern())) {
                    try {
                        return Pattern.compile(rule.getMaskPattern()).matcher(text).replaceAll("***");
                    } catch (Exception e) {
                        log.warn("[Desensitize] 自定义正则无效: {}", rule.getMaskPattern());
                    }
                }
                return text;
            default:
                return text;
        }
    }
}
