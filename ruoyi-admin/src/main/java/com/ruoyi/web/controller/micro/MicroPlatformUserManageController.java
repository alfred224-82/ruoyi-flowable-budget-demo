package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.UserTenantRoleBo;
import com.ruoyi.system.domain.vo.SysOssVo;
import com.ruoyi.system.domain.vo.UserTenantRoleVo;
import com.ruoyi.system.service.ISysOssService;
import com.ruoyi.system.service.IUserTenantRoleService;
import com.ruoyi.system.service.image.AvatarImageProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * platform_admin 人员管理Controller（T15.33，租户管理员三件套之三）
 * <p>platform_admin 新增人员 + 分配角色 / 编辑 / 移除，复用 UserTenantRoleService。
 * <p>perms：micro:platform:userManage（M15_17 种子菜单）。
 * <p>★与 /micro/userRole 共用底层 Service，仅 URL 路径不同（platform_admin 菜单独立入口）。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/platform/userManage")
public class MicroPlatformUserManageController extends BaseController {

    private final IUserTenantRoleService userTenantRoleService;
    private final ISysOssService sysOssService;
    private final AvatarImageProcessor avatarImageProcessor;

    /**
     * 本租户人员列表（含角色分配信息）
     */
    @SaCheckPermission("micro:platform:userManage")
    @GetMapping("/list")
    public TableDataInfo<UserTenantRoleVo> list(UserTenantRoleBo bo, PageQuery pageQuery) {
        return userTenantRoleService.queryPageList(bo, pageQuery);
    }

    /**
     * 新增人员 + 分配角色（手机号为唯一标识，自动创建用户后分配角色）
     * <p>★platform_admin 入口传 phonenumber 而非 userId，Service 层自动查找/创建用户。
     */
    @SaCheckPermission("micro:platform:userManage")
    @Log(title = "平台人员管理", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public R<Void> add(@RequestBody UserTenantRoleBo bo) {
        return toAjax(userTenantRoleService.assign(bo));
    }

    /**
     * 编辑人员角色（修改角色分配）
     */
    @SaCheckPermission("micro:platform:userManage")
    @Log(title = "平台人员管理", businessType = BusinessType.UPDATE)
    @PutMapping("/edit")
    public R<Void> edit(@RequestBody UserTenantRoleBo bo) {
        return toAjax(userTenantRoleService.assign(bo));
    }

    /**
     * 移除人员（取消角色分配）
     */
    @SaCheckPermission("micro:platform:userManage")
    @Log(title = "平台人员管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/remove/{id}")
    public R<Void> remove(@PathVariable Long id) {
        return toAjax(userTenantRoleService.removeById(id));
    }

    /**
     * 人员头像上传（复用 micro:platform:userManage 权限，无需 system:oss:upload）
     * <p>★T26 头像优化：上传后自动生成 sm(80px)/md(120px)/lg(200px) 三档 JPEG 缩略图，
     *   原图归档 OSS 但不直接下发；前端按显示场景选用对应档位，手机端避免加载原图（294KB~2.9MB）。
     * <p>返回：{ url: md档URL, smUrl, lgUrl, originalUrl, fileName, ossId }
     */
    @SaCheckPermission("micro:platform:userManage")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Map<String, String>> upload(@RequestPart("file") MultipartFile file) {
        if (ObjectUtil.isNull(file)) {
            return R.fail("上传文件不能为空");
        }
        SysOssVo oss = sysOssService.upload(file);
        String originalUrl = oss.getUrl();
        String originalPath = oss.getFileName();
        // 生成三档缩略图（失败不阻断，降级返回原图）
        Map<String, String> variants;
        try {
            variants = avatarImageProcessor.processAndUpload(originalUrl, originalPath);
        } catch (Exception e) {
            log.warn("[UserManage] 头像三档生成失败，降级返回原图: {}", e.getMessage());
            variants = new HashMap<>();
            variants.put("original", originalUrl);
            variants.put("sm", originalUrl);
            variants.put("md", originalUrl);
            variants.put("lg", originalUrl);
        }
        Map<String, String> map = new HashMap<>(8);
        // md 档作为默认 url（列表/弹窗预览最常用）
        map.put("url", variants.getOrDefault("md", originalUrl));
        map.put("smUrl", variants.getOrDefault("sm", originalUrl));
        map.put("mdUrl", variants.getOrDefault("md", originalUrl));
        map.put("lgUrl", variants.getOrDefault("lg", originalUrl));
        map.put("originalUrl", variants.getOrDefault("original", originalUrl));
        map.put("fileName", oss.getOriginalName());
        map.put("ossId", oss.getOssId().toString());
        return R.ok(map);
    }
}
