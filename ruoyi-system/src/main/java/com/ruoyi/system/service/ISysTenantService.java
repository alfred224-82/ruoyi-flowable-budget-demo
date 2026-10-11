package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.SysTenantBo;
import com.ruoyi.system.domain.bo.SysTenantConfigBo;
import com.ruoyi.system.domain.vo.SysTenantConfigVo;
import com.ruoyi.system.domain.vo.SysTenantVersionLogVo;
import com.ruoyi.system.domain.vo.SysTenantVo;

import java.util.List;
import java.util.Map;

/**
 * 租户管理Service接口
 * 设计文档 Phase 16 Task 16.4
 *
 * @author ruoyi
 * @date 2026-08-01
 */
public interface ISysTenantService {

    /**
     * 分页查询租户列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 租户分页列表
     */
    TableDataInfo<SysTenantVo> queryPageList(SysTenantBo bo, PageQuery pageQuery);

    /**
     * 根据ID查询租户详情
     *
     * @param id 租户ID
     * @return 租户详情
     */
    SysTenantVo queryById(Long id);

    /**
     * 创建租户（含初始化管理员+默认角色）
     *
     * @param bo 租户信息
     * @return 影响行数
     */
    int insertTenant(SysTenantBo bo);

    /**
     * 更新租户信息
     *
     * @param bo 租户信息
     * @return 影响行数
     */
    int updateTenant(SysTenantBo bo);

    /**
     * 更新租户状态（启用/停用）
     *
     * @param id     租户ID
     * @param status 目标状态
     * @return 影响行数
     */
    int updateStatus(Long id, String status);

    /**
     * 分配版本（记录日志+刷新缓存）
     *
     * @param id      租户ID
     * @param edition 目标版本
     * @return 影响行数
     */
    int assignVersion(Long id, String edition);

    /**
     * ★T15.26 分配/变更版本档位（version_type）：lite→pro 升级 = UPDATE version_type + 刷新菜单缓存，零迁移。
     * <p>校验档位必须属于本租户产品线（micro→micro_lite/micro_pro；enterprise→basic/advanced/pro）；记录版本日志。
     *
     * @param id          租户ID
     * @param versionType 目标版本档位
     * @return 影响行数（档位未变化时返回 0）
     */
    int assignVersionType(Long id, String versionType);

    /**
     * 查询租户版本变更日志
     *
     * @param tenantId 租户ID
     * @return 日志列表
     */
    List<SysTenantVersionLogVo> getVersionLogs(Long tenantId);

    /**
     * 获取租户功能配置
     *
     * @param tenantId  租户ID
     * @param configKey 配置键
     * @return 配置信息
     */
    SysTenantConfigVo getConfig(Long tenantId, String configKey);

    /**
     * 保存租户功能配置
     *
     * @param tenantId 租户ID
     * @param bo       配置信息
     * @return 影响行数
     */
    int saveConfig(Long tenantId, SysTenantConfigBo bo);

    /**
     * 获取租户用量统计
     *
     * @param tenantId 租户ID
     * @return 统计信息
     */
    Map<String, Object> getUsageStats(Long tenantId);
}
