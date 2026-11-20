package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.UserTenantRoleBo;
import com.ruoyi.system.domain.vo.UserTenantRoleVo;

import java.util.List;

/**
 * 用户 × 租户 × 角色关联服务接口（T15.9 分配用户）
 * <p>支持「一人多租户、一租户多角色」；分配即写入 user_tenant_role（同租户同用户同角色幂等）。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
public interface IUserTenantRoleService {

    /**
     * 分页查询本租户的用户角色分配（回填用户账号/昵称/手机号 + 租户内角色显示名）
     */
    TableDataInfo<UserTenantRoleVo> queryPageList(UserTenantRoleBo bo, PageQuery pageQuery);

    /**
     * 查询指定用户在本租户下的全部角色（回填显示名）
     */
    List<UserTenantRoleVo> listByUser(Long userId);

    /**
     * 解析指定用户在本租户下的主角色显示名（tenant_role_mapping 动态映射：
     * 优先取分配时选中的映射行，如司机/导游；空回退 role_code 首位业务名）；
     * 无业务角色或无映射时返回 null，由调用方回退通用名
     */
    String resolveRoleDisplayName(Long userId);

    /**
     * 分配用户角色（支持单角色 roleCode 或批量 roleCodes，幂等 upsert；用户首个角色自动置为主角色）
     *
     * @return 影响条数
     */
    int assign(UserTenantRoleBo bo);

    /**
     * 取消分配（按关联ID逻辑删除）
     */
    int removeById(Long id);

    /**
     * 设置主角色（同一用户同一租户下仅一条 is_primary=1，其余置 0）
     */
    int setPrimary(Long id);
}
