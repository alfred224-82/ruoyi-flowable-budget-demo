package com.ruoyi.framework.satoken.service;

import cn.dev33.satoken.stp.StpInterface;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.enums.UserType;
import com.ruoyi.common.helper.LoginHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * sa-token 权限管理实现类
 * <p>
 * admin 超级管理员（userId=1）拥有全部权限，不受角色/菜单权限校验。
 *
 * @author Lion Li
 */
public class SaPermissionImpl implements StpInterface {

    /**
     * 获取菜单权限列表
     * admin 返回通配符，绕过所有 @SaCheckPermission 校验
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        if (LoginHelper.isAdmin()) {
            return Collections.singletonList("*:*:*");
        }
        LoginUser loginUser = LoginHelper.getLoginUser();
        UserType userType = UserType.getUserType(loginUser.getUserType());
        if (userType == UserType.SYS_USER) {
            return new ArrayList<>(loginUser.getMenuPermission());
        } else if (userType == UserType.APP_USER) {
            // 其他端 自行根据业务编写
        }
        return new ArrayList<>();
    }

    /**
     * 获取角色权限列表
     * admin 返回全量角色标识，绕过所有 @SaCheckRole 校验
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (LoginHelper.isAdmin()) {
            // admin 拥有所有角色，确保任意 @SaCheckRole 均放行
            return Arrays.asList("admin", "platform_admin", "boss", "finance",
                "manager", "employee", "deptManager", "deptPlan",
                "branch", "headquarters", "common");
        }
        LoginUser loginUser = LoginHelper.getLoginUser();
        UserType userType = UserType.getUserType(loginUser.getUserType());
        if (userType == UserType.SYS_USER) {
            return new ArrayList<>(loginUser.getRolePermission());
        } else if (userType == UserType.APP_USER) {
            // 其他端 自行根据业务编写
        }
        return new ArrayList<>();
    }
}
