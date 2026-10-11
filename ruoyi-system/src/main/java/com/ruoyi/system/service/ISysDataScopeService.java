package com.ruoyi.system.service;

/**
 * 通用 数据权限 服务
 *
 * @author Lion Li
 */
public interface ISysDataScopeService {

    /**
     * 获取角色自定义权限
     *
     * @param roleId 角色id
     * @return 部门id组
     */
    String getRoleCustom(Long roleId);

    /**
     * 获取部门及以下权限
     *
     * @param deptId 部门id
     * @return 部门id组
     */
    String getDeptAndChild(Long deptId);

    /**
     * 获取用户可访问的项目ID列表（T2.3 项目维度数据范围过滤）
     * <p>按 project_member.role 五枚举：owner/manager 可看全部，member 看参与项目，participant/signup 仅看自己。
     *
     * @param userId 用户ID
     * @return 项目ID组（逗号分隔），null=无权限
     */
    String getProjectIds(Long userId);

    /**
     * 获取用户作为 owner/manager 的项目ID列表（T2.3 管理维度）
     *
     * @param userId 用户ID
     * @return 项目ID组（逗号分隔），null=无管理权限
     */
    String getManagedProjectIds(Long userId);

}
