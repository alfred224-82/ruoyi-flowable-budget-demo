package com.ruoyi.web.controller.system;

import cn.dev33.satoken.secure.BCrypt;
import cn.hutool.core.io.FileUtil;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.system.domain.SysOss;
import com.ruoyi.system.domain.vo.SysOssVo;
import com.ruoyi.system.domain.vo.UserTenantRoleVo;
import com.ruoyi.system.service.ISysOssService;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.system.service.IUserTenantRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 个人信息 业务处理
 *
 * @author Lion Li
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/user/profile")
public class SysProfileController extends BaseController {

    private final ISysUserService userService;
    private final ISysOssService iSysOssService;
    private final IUserTenantRoleService userTenantRoleService;

    /**
     * 个人信息
     */
    @GetMapping
    public R<Map<String, Object>> profile() {
        SysUser user = userService.selectUserById(getUserId());
        Map<String, Object> ajax = new HashMap<>();
        ajax.put("user", user);
        // ★优先从 user_tenant_role（业务角色表）读取主角色显示名，
        //   解决人员管理分配 boss 等角色后个人中心仍显示 sys_user_role 中 platform_admin 的问题
        ajax.put("roleGroup", resolveProfileRoleGroup(user.getUserId(), user.getUserName()));
        ajax.put("postGroup", userService.selectUserPostGroup(user.getUserName()));
        return R.ok(ajax);
    }

    /**
     * 解析个人中心展示的角色组：
     * 优先取 user_tenant_role 中当前租户的主角色（isPrimary=1）显示名；
     * 无业务角色时回落 sys_user_role（兼容非小微版场景）。
     */
    private String resolveProfileRoleGroup(Long userId, String userName) {
        try {
            List<UserTenantRoleVo> bizRoles = userTenantRoleService.listByUser(userId);
            if (bizRoles != null && !bizRoles.isEmpty()) {
                // 优先主角色
                for (UserTenantRoleVo r : bizRoles) {
                    if (Integer.valueOf(1).equals(r.getIsPrimary()) && !"1".equals(r.getStatus())) {
                        return r.getRoleName();
                    }
                }
                // 无主角色则取第一个启用的
                for (UserTenantRoleVo r : bizRoles) {
                    if (!"1".equals(r.getStatus())) {
                        return r.getRoleName();
                    }
                }
            }
        } catch (Exception e) {
            // 业务角色查询失败不阻断，回落系统角色
        }
        return userService.selectUserRoleGroup(userName);
    }

    /**
     * 修改用户
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> updateProfile(@RequestBody SysUser user) {
        if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user)) {
            return R.fail("修改用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user)) {
            return R.fail("修改用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setUserId(getUserId());
        user.setUserName(null);
        user.setPassword(null);
        user.setAvatar(null);
        user.setDeptId(null);
        if (userService.updateUserProfile(user) > 0) {
            return R.ok();
        }
        return R.fail("修改个人信息异常，请联系管理员");
    }

    /**
     * 重置密码
     *
     * @param newPassword 旧密码
     * @param oldPassword 新密码
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping("/updatePwd")
    public R<Void> updatePwd(String oldPassword, String newPassword) {
        SysUser user = userService.selectUserById(LoginHelper.getUserId());
        String userName = user.getUserName();
        String password = user.getPassword();
        if (!BCrypt.checkpw(oldPassword, password)) {
            return R.fail("修改密码失败，旧密码错误");
        }
        if (BCrypt.checkpw(newPassword, password)) {
            return R.fail("新密码不能与旧密码相同");
        }

        if (userService.resetUserPwd(userName, BCrypt.hashpw(newPassword)) > 0) {
            return R.ok();
        }
        return R.fail("修改密码异常，请联系管理员");
    }

    /**
     * 头像上传
     *
     * @param avatarfile 用户头像
     */
    @Log(title = "用户头像", businessType = BusinessType.UPDATE)
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Map<String, Object>> avatar(@RequestPart("avatarfile") MultipartFile avatarfile) {
        Map<String, Object> ajax = new HashMap<>();
        if (!avatarfile.isEmpty()) {
            String extension = FileUtil.extName(avatarfile.getOriginalFilename());
            if (!StringUtils.equalsAnyIgnoreCase(extension, MimeTypeUtils.IMAGE_EXTENSION)) {
                return R.fail("文件格式不正确，请上传" + Arrays.toString(MimeTypeUtils.IMAGE_EXTENSION) + "格式");
            }
            SysOssVo oss = iSysOssService.upload(avatarfile);
            String avatar = oss.getUrl();
            if (userService.updateUserAvatar(getUsername(), avatar)) {
                ajax.put("imgUrl", avatar);
                return R.ok(ajax);
            }
        }
        return R.fail("上传图片异常，请联系管理员");
    }
}
