package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.ThemePresetVo;

import java.util.List;
import java.util.Map;

/**
 * 主题预设服务接口（T12.2 四套 Design Token）
 * <p>Token 以 CSS 变量键值下发，PC 与小程序共用，禁止硬编码色值。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IThemePresetService {

    /** 全部启用主题预设（按 sort 升序） */
    List<ThemePresetVo> listPresets();

    /** 按 key 取预设，缺失返回 null */
    ThemePresetVo getPreset(String presetKey);

    /**
     * 解析预设为 Design Token 键值（CSS 变量），缺失/非法返回空 Map
     */
    Map<String, String> getTokens(String presetKey);
}
