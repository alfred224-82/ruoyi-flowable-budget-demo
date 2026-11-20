package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.secure.BCrypt;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.system.service.IUserTenantRoleService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;

/**
 * 小程序端账号设置Controller（T18.19，老板/员工/顾客通用）
 * <p>功能：
 * <ul>
 *   <li>获取个人信息（昵称/手机号/邮箱）</li>
 *   <li>修改个人信息（昵称/手机号/邮箱）</li>
 *   <li>修改密码（旧密码 + 新密码）</li>
 * </ul>
 * <p>★开发规范遵循：
 * <ul>
 *   <li>fail-fast 入口判空（第六章）</li>
 *   <li>DB 返回判空（第六章）</li>
 *   <li>前端文本框 maxLength 对齐 DB 字段（第七章）</li>
 * </ul>
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/profile")
public class MicroProfileController extends BaseController {

    private final ISysUserService userService;

    /** ★身份显示名：租户角色映射动态名（司机/导游等），H5 我的页/账号设置按此展示身份 */
    private final IUserTenantRoleService userTenantRoleService;

    /**
     * 获取当前登录用户个人信息
     */
    @GetMapping
    public R<ProfileVo> getProfile() {
        // ★fail-fast：获取当前用户ID
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("用户未登录");
        }

        // ★DB 返回判空
        SysUser user = userService.selectUserById(userId);
        if (user == null) {
            return R.fail("用户信息不存在");
        }

        // 构建返回 VO
        ProfileVo vo = new ProfileVo();
        vo.setUserId(user.getUserId());
        vo.setNickName(user.getNickName());
        vo.setPhonenumber(user.getPhonenumber());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        // ★角色显示名：user_tenant_role.role_mapping_id → tenant_role_mapping.display_name（如司机），
        //   无业务角色时 null，前端回退通用名（老板/员工/顾客）
        vo.setRoleDisplayName(userTenantRoleService.resolveRoleDisplayName(userId));
        return R.ok(vo);
    }

    /**
     * 修改个人信息（昵称/手机号/邮箱）
     */
    @Log(title = "小程序个人信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> updateProfile(@RequestBody ProfileUpdateBo bo) {
        // ★fail-fast：参数对象判空
        if (bo == null) {
            return R.fail("请求参数不能为空");
        }

        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("用户未登录");
        }

        // ★DB 返回判空
        SysUser existUser = userService.selectUserById(userId);
        if (existUser == null) {
            return R.fail("用户信息不存在");
        }

        // ★手机号唯一性校验（防脏数据入库，第七章）
        if (StringUtils.isNotEmpty(bo.getPhonenumber()) && !bo.getPhonenumber().equals(existUser.getPhonenumber())) {
            SysUser checkUser = new SysUser();
            checkUser.setPhonenumber(bo.getPhonenumber());
            if (!userService.checkPhoneUnique(checkUser)) {
                return R.fail("手机号码已存在");
            }
        }

        // ★邮箱唯一性校验
        if (StringUtils.isNotEmpty(bo.getEmail()) && !bo.getEmail().equals(existUser.getEmail())) {
            SysUser checkUser = new SysUser();
            checkUser.setEmail(bo.getEmail());
            if (!userService.checkEmailUnique(checkUser)) {
                return R.fail("邮箱账号已存在");
            }
        }

        // 构建更新对象
        SysUser updateUser = new SysUser();
        updateUser.setUserId(userId);
        updateUser.setNickName(bo.getNickName());
        updateUser.setPhonenumber(bo.getPhonenumber());
        updateUser.setEmail(bo.getEmail());

        // ★禁止修改敏感字段（用户名/密码/头像/部门/状态）
        updateUser.setUserName(null);
        updateUser.setPassword(null);
        updateUser.setAvatar(null);
        updateUser.setDeptId(null);
        updateUser.setStatus(null);

        if (userService.updateUserProfile(updateUser) > 0) {
            return R.ok();
        }
        return R.fail("修改个人信息异常，请联系管理员");
    }

    /**
     * 修改密码（旧密码 + 新密码）
     */
    @Log(title = "小程序修改密码", businessType = BusinessType.UPDATE)
    @PutMapping("/updatePwd")
    public R<Void> updatePwd(@RequestBody PasswordUpdateBo bo) {
        // ★fail-fast：参数对象判空
        if (bo == null) {
            return R.fail("请求参数不能为空");
        }
        if (StringUtils.isEmpty(bo.getOldPassword())) {
            return R.fail("旧密码不能为空");
        }
        if (StringUtils.isEmpty(bo.getNewPassword())) {
            return R.fail("新密码不能为空");
        }
        // ★密码长度约束（4~20位）
        if (bo.getNewPassword().length() < 4 || bo.getNewPassword().length() > 20) {
            return R.fail("新密码长度必须在4~20位之间");
        }

        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("用户未登录");
        }

        // ★DB 返回判空
        SysUser user = userService.selectUserById(userId);
        if (user == null) {
            return R.fail("用户信息不存在");
        }

        String userName = user.getUserName();
        String password = user.getPassword();

        // ★校验旧密码
        if (!BCrypt.checkpw(bo.getOldPassword(), password)) {
            return R.fail("旧密码错误");
        }

        // ★新密码不能与旧密码相同
        if (BCrypt.checkpw(bo.getNewPassword(), password)) {
            return R.fail("新密码不能与旧密码相同");
        }

        // ★更新密码（BCrypt 加密）
        if (userService.resetUserPwd(userName, BCrypt.hashpw(bo.getNewPassword())) > 0) {
            return R.ok();
        }
        return R.fail("修改密码异常，请联系管理员");
    }

    /**
     * 个人信息 VO
     */
    @Data
    public static class ProfileVo implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 用户ID（小程序扫码归因等场景需要当前用户标识，v1.42 补充） */
        private Long userId;
        /** 用户昵称 */
        private String nickName;
        /** 手机号码 */
        private String phonenumber;
        /** 用户邮箱 */
        private String email;
        /** 头像地址 */
        private String avatar;
        /** 角色显示名（租户角色映射动态名，如司机/导游；无业务角色时 null，前端回退通用名） */
        private String roleDisplayName;
    }

    /**
     * 个人信息更新 BO
     */
    @Data
    public static class ProfileUpdateBo implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 用户昵称（maxLength=30，对齐 DB sys_user.nick_name） */
        private String nickName;
        /** 手机号码（maxLength=11，对齐 DB sys_user.phonenumber） */
        private String phonenumber;
        /** 用户邮箱（maxLength=50，对齐 DB sys_user.email） */
        private String email;
    }

    /**
     * 密码更新 BO
     */
    @Data
    public static class PasswordUpdateBo implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 旧密码 */
        private String oldPassword;
        /** 新密码（4~20位） */
        private String newPassword;
    }
}
