package com.ruoyi.system.service;

import com.ruoyi.system.domain.BizBundle;
import com.ruoyi.system.domain.vo.BizBundleVo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 业态包服务（T10.2 多业态平台底座）
 * <p>按 biz_code 加载 menus/copy/schema_blocks/engine_strategy/feature_flags，本地缓存 + 变更失效。
 * 新增业态包只需插数据 + 加文案，不动核心表结构、不改核心代码。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IBizBundleService {

    /**
     * 按业态编码加载业态包（带本地缓存）
     *
     * @param bizCode 业态编码，为空时回落默认包 fund_control
     * @return 业态包，不存在返回 null
     */
    BizBundle getBundle(String bizCode);

    /**
     * 业态包声明的开关编码集合（参与三维交集，未声明的能力即使版本/License 授权也不开放）
     *
     * @param bizCode 业态编码
     * @return 开关编码集合；业态包缺失时返回空集（调用方据此决定是否跳过业态过滤）
     */
    Set<String> getFeatureFlags(String bizCode);

    /**
     * 业态包文案映射（key→业态文案，前端 t(key) 取值）
     *
     * @param bizCode 业态编码
     * @return 文案映射，缺失返回空 Map
     */
    Map<String, String> getCopy(String bizCode);

    /**
     * 全部业态包列表（管理页面用）
     */
    List<BizBundleVo> listBundles();

    /**
     * 新增/修改业态包（platform_admin；保存后失效缓存）
     */
    int saveBundle(BizBundleVo vo);

    /**
     * 失效业态包缓存
     *
     * @param bizCode 业态编码，为空时清空全部缓存
     */
    void evictCache(String bizCode);
}
