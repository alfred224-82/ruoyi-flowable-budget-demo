package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.system.domain.ThemePreset;
import com.ruoyi.system.domain.vo.ThemePresetVo;
import com.ruoyi.system.mapper.ThemePresetMapper;
import com.ruoyi.system.service.IThemePresetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 主题预设服务实现（T12.2 Design Token）
 * <p>系统级共享表 biz_theme_preset 无 tenant_id，本地按 presetKey 缓存解析后的 Token。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThemePresetServiceImpl implements IThemePresetService {

    private final ThemePresetMapper themePresetMapper;
    private final ObjectMapper objectMapper;

    /** Token 缓存：presetKey → CSS 变量键值 */
    private final ConcurrentHashMap<String, Map<String, String>> tokenCache = new ConcurrentHashMap<>();

    @Override
    public List<ThemePresetVo> listPresets() {
        return themePresetMapper.selectVoList(
            Wrappers.<ThemePreset>lambdaQuery()
                .eq(ThemePreset::getStatus, "0")
                .orderByAsc(ThemePreset::getSort));
    }

    @Override
    public ThemePresetVo getPreset(String presetKey) {
        if (StrUtil.isBlank(presetKey)) {
            return null;
        }
        return themePresetMapper.selectVoOne(
            Wrappers.<ThemePreset>lambdaQuery().eq(ThemePreset::getPresetKey, presetKey));
    }

    @Override
    public Map<String, String> getTokens(String presetKey) {
        if (StrUtil.isBlank(presetKey)) {
            return Collections.emptyMap();
        }
        Map<String, String> cached = tokenCache.get(presetKey);
        if (cached != null) {
            return cached;
        }
        ThemePreset preset = themePresetMapper.selectOne(
            Wrappers.<ThemePreset>lambdaQuery().eq(ThemePreset::getPresetKey, presetKey));
        Map<String, String> tokens = preset == null ? Collections.emptyMap() : parseTokens(preset.getTokens());
        tokenCache.put(presetKey, tokens);
        return tokens;
    }

    private Map<String, String> parseTokens(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyMap();
        }
        try {
            Map<String, String> map = objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
            return map != null ? map : Collections.emptyMap();
        } catch (Exception e) {
            log.warn("[ThemePreset] tokens JSON 解析失败: {}", json, e);
            return Collections.emptyMap();
        }
    }
}
