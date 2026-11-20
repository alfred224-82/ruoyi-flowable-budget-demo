package com.ruoyi.system.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.system.domain.UserTenantRole;
import com.ruoyi.system.domain.vo.UserTenantRoleVo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 用户 × 租户 × 角色关联Mapper接口（T15.1）
 *
 * @author ruoyi
 * @date 2026-09-07
 */
public interface UserTenantRoleMapper extends BaseMapperPlus<UserTenantRoleMapper, UserTenantRole, UserTenantRoleVo> {

    /**
     * ★物理删除指定用户的所有软删除记录（绕过 @TableLogic）
     * <p>用于解决 uk_tenant_user_role 唯一约束冲突：软删除记录仍占用唯一键，
     * 重新分配同一用户+角色时 INSERT 会因 Duplicate entry 失败。
     *
     * @param userId 用户ID
     * @return 删除条数
     */
    @Delete("DELETE FROM user_tenant_role WHERE user_id = #{userId} AND del_flag <> '0'")
    int physicalDeleteSoftDeleted(@Param("userId") Long userId);
}
