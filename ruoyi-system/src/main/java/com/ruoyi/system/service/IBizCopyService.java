package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.BizCopyVo;

import java.util.List;
import java.util.Map;

/**
 * 业态文案映射服务（T10.5 v1.29 文案映射 i18n）
 * <p>三维合并优先级：租户 module_overrides.copy &gt; biz_copy(biz_code,lang) &gt; biz_bundle.copy &gt; 前端硬编码兜底。
 * <p>本地缓存 (biz_code, lang) → Map&lt;i18n_key, copy_value&gt;，保存/删除时失效缓存。
 *
 * @author ruoyi
 * @date 2026-09-16
 */
public interface IBizCopyService {

    /**
     * 获取指定业态包 + 语言的文案映射（带本地缓存）
     *
     * @param bizCode 业态包编码（null/空 → 默认包 fund_control）
     * @param lang    语言编码（null/空 → zh-CN）
     * @return key → value 文案映射，无数据返回空 Map
     */
    Map<String, String> getCopy(String bizCode, String lang);

    /**
     * 获取全部语言的文案映射（供前端一次性加载）
     *
     * @param bizCode 业态包编码
     * @return lang → (key → value) 二级映射
     */
    Map<String, Map<String, String>> getCopyAllLangs(String bizCode);

    /**
     * 获取支持的语言列表（去重）
     */
    List<String> listLangs();

    /**
     * 分页查询文案列表
     *
     * @param query 查询条件（bizCode/lang/i18nKey/module/status）
     * @return 文案列表
     */
    List<BizCopyVo> listCopies(BizCopyVo query);

    /**
     * 新增或更新文案（按 (biz_code, i18n_key, lang) 唯一键幂等）
     */
    int saveCopy(BizCopyVo vo);

    /**
     * 批量新增/更新文案
     */
    int batchSave(List<BizCopyVo> list);

    /**
     * 按 ID 删除文案
     */
    int deleteById(Long id);

    /**
     * 按 (bizCode, lang) 批量删除
     */
    int deleteByBizAndLang(String bizCode, String lang);

    /**
     * 失效缓存（bizCode/lang 为空时清全部）
     */
    void evictCache(String bizCode, String lang);
}
