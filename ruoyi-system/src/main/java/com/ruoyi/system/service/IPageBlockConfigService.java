package com.ruoyi.system.service;

import com.ruoyi.system.domain.bo.PageBlockConfigBo;
import com.ruoyi.system.domain.bo.PageDecorateBo;
import com.ruoyi.system.domain.vo.PageBlockConfigVo;

import java.util.Collection;
import java.util.List;

/**
 * 页面区块配置服务接口（T15.16 模块化页面架构 + T15.17 页面装修）
 * <p>核心约束（开发规范第 14 条）：小程序页面 = 单一页面壳 + 区块配置，禁止为 boss/salesman/customer 各写一套首页；
 * {@code /page/config} 返回区块清单 = page_block_config ∩ 该角色 perms，配置永远不能突破权限。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
public interface IPageBlockConfigService {

    /**
     * T15.16 运行时：按当前登录用户 tenantId + 主标准角色 查 page_block_config，
     * 返回有序区块清单（micro、show_flag=1），并做「配置 ∩ 权限」二次过滤（block_params.perm 未命中则剔除）；
     * 本租户无配置时回落 tenant_id=0 系统默认模板。
     */
    List<PageBlockConfigVo> getPageConfig(String pageKey);

    /**
     * T15.17 装修面板：查某角色某页面的原始区块配置（不过滤权限，按 sortOrder）。
     * tenantId 仅超管可指定（默认 0=系统默认模板）；非超管强制本租户（插件隔离）。
     */
    List<PageBlockConfigVo> listByRolePage(String roleCode, String pageKey, Long tenantId);

    /**
     * T15.17 批量保存：按 (目标租户,roleCode,pageKey,moduleType) 全量覆盖（先删后插），小程序实时生效。
     * bo.tenantId 仅超管可指定（默认 0=系统模板）；非超管强制本租户。
     */
    int saveConfig(PageDecorateBo bo);

    /**
     * T15.17 恢复默认：从 tenant_id=0 系统模板克隆到目标租户（先删目标租户该角色该页面配置，再插模板副本）。
     * tenantId 仅超管可指定；非超管=本租户。
     */
    int resetDefault(String roleCode, String pageKey, Long tenantId);

    /** 单区块新增（超管默认写入 tenant_id=0 系统模板；老板写入本租户） */
    Long insert(PageBlockConfigBo bo);

    /** 单区块编辑（排版：排序/显隐/参数） */
    int update(PageBlockConfigBo bo);

    /** 区块详情 */
    PageBlockConfigVo queryById(Long id);

    /** 批量物理删除（page_block_config 无 del_flag） */
    int deleteByIds(Collection<Long> ids);
}
