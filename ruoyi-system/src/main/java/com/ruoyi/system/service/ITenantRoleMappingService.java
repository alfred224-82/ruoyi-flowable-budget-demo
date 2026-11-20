package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.TenantRoleMappingBo;
import com.ruoyi.system.domain.vo.TenantRoleMappingVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 租户角色映射服务接口（T15.9 平台管理员角色映射管理）
 * <p>功能：① 从标准角色集勾选（挑选）；② 改名（display_name）；③ 启用/禁用（status）；
 * ④ 调整排序（sort_order）。★平台管理员不能新建角色 code、不能改角色权限（菜单/区块）。
 * <p>企业版高级版与小微版共用同一套映射界面；映射持久化到 tenant_role_mapping，改后无需重启即生效。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
public interface ITenantRoleMappingService {

    /**
     * 分页查询本租户已挑选的角色映射（支持按角色编码/显示名/状态过滤，按 sort_order 升序）
     */
    TableDataInfo<TenantRoleMappingVo> queryPageList(TenantRoleMappingBo bo, PageQuery pageQuery);

    /**
     * 标准角色集挑选列表：全版本共享标准角色（sys_role.tenant_id IS NULL）合并本租户映射状态；
     * 未映射的标准角色 {@code id=null}，前端据此渲染「已挑选/未挑选」勾选框。
     * <p>★T15.25 硬隔离：数据源层按本租户 tenant_type 过滤 {@code tenant_type_scope IN (tenant_type,'both')}。
     */
    List<TenantRoleMappingVo> listStandardRoles();

    /**
     * ★T15.25「可选角色列表」（角色可见性硬隔离）：按本租户 tenant_type 在【数据源层】过滤标准角色，
     * 仅返回 {@code tenant_type_scope IN (tenant_type, 'both')} 的启用标准角色。
     * <p>★硬隔离非前端隐藏：小微租户看不到 manager/employee/finance，企业租户看不到 salesman/customer。
     *
     * @return 可选标准角色清单（含 tenantTypeScope）
     */
    List<TenantRoleMappingVo> listOptionalRoles();

    /**
     * 挑选标准角色（批量写入 tenant_role_mapping，display_name 默认取 sys_role.role_name，幂等跳过已存在）
     *
     * @return 新增条数
     */
    int selectRoles(List<String> roleCodes);

    /**
     * 改名（更新 display_name，如 salesman→"助教"）
     */
    int rename(TenantRoleMappingBo bo);

    /**
     * 启用/禁用（更新 status：0启用 1禁用）
     */
    int toggle(Long id, String status);

    /**
     * 调整排序（更新 sort_order）
     */
    int updateSort(TenantRoleMappingBo bo);

    /**
     * 逻辑删除角色映射（可批量）
     */
    int deleteByIds(Collection<Long> ids);

    /**
     * ★本租户角色显示名映射（role_code → display_name）：供各端页面按角色映射显示行业术语（登录可读、不校验权限）
     * <p>如 salesman → 助教/导游/教练；仅返回启用（status=0）且显示名非空的映射；
     * 租户未配置的角色不在 map 中，由调用方回退通用名（员工/顾客等）。
     */
    Map<String, String> roleDisplayNameMap();
}
