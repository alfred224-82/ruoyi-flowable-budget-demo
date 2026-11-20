package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.RoomSeatLayoutTemplateBo;
import com.ruoyi.system.domain.vo.RoomSeatLayoutTemplateVo;
import com.ruoyi.system.domain.vo.SysOssVo;
import com.ruoyi.system.service.IRoomSeatLayoutService;
import com.ruoyi.system.service.ISysOssService;
import com.ruoyi.system.service.image.RoomBgImageValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台 Admin 默认房间模板 Controller（T26.6，小微版 M26）
 * <p>平台 Admin 在 PC 端配置行业下的默认房间模板（模板名称 + 默认背景图，简洁线条示意图 SVG）；
 *   Boss 新建房间时按 industry_code 匹配，可应用模板快速起步。
 * <p>权限：{@code system:platformRoomTemplate:view}（菜单 7285 主权限）+ {@code system:platformRoomTemplate:*}（F 按钮 7286）。
 * <p>角色：类级 {@code @SaCheckRole("platform_admin")}，仅平台超管可访问。
 * <p>功能开关：类级 {@code @RequireFeature("FEAT_ROOM_SEAT_LAYOUT")}，随 M26_02 SQL 落表启用。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/platformRoomTemplate")
@SaCheckRole("platform_admin")
@RequireFeature("FEAT_ROOM_SEAT_LAYOUT")
public class MicroPlatformRoomTemplateController extends BaseController {

    private final IRoomSeatLayoutService roomSeatLayoutService;
    private final ISysOssService sysOssService;
    private final RoomBgImageValidator roomBgImageValidator;

    /**
     * ★M27 T27.2：上传平台默认模板背景图（必须为透明底 SVG / PNG，简洁线条示意图）。
     * <p>与 Boss 自传背景图（{@code /micro/room/uploadBgImage}，JPG/PNG/WebP）区分：默认图需透明底以便
     *   前端按主题主色 20% 铺底联动渲染，故单独校验放行矢量 SVG。仅 platform_admin 可访问。
     *
     * @param file 图片文件（SVG/PNG，≤2MB）
     * @return url=OSS 访问地址；fileName=原始文件名；ossId=OSS 主键
     */
    @SaCheckPermission("system:platformRoomTemplate:add")
    @Log(title = "平台默认模板背景图上传", businessType = BusinessType.INSERT)
    @PostMapping(value = "/uploadImage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Map<String, String>> uploadImage(@RequestPart("file") MultipartFile file) {
        if (ObjectUtil.isNull(file)) {
            return R.fail("上传文件不能为空");
        }
        // 前置校验（透明底 SVG/PNG + 大小），不合规抛 ServiceException，OSS 不落库
        roomBgImageValidator.validateDefaultTemplate(file);
        SysOssVo oss = sysOssService.upload(file);
        Map<String, String> map = new HashMap<>(3);
        map.put("url", oss.getUrl());
        map.put("fileName", oss.getOriginalName());
        map.put("ossId", oss.getOssId().toString());
        return R.ok(map);
    }

    /**
     * 平台默认模板列表（tenant_id=1 且 is_industry_default=1）。
     *
     * @param industryCode 行业编码过滤（可空）
     */
    @SaCheckPermission("system:platformRoomTemplate:view")
    @GetMapping("/list")
    public R<List<RoomSeatLayoutTemplateVo>> list(@RequestParam(required = false) String industryCode) {
        return R.ok(roomSeatLayoutService.listPlatformTemplates(industryCode));
    }

    /**
     * 平台默认模板详情。
     */
    @SaCheckPermission("system:platformRoomTemplate:view")
    @GetMapping("/{id}")
    public R<RoomSeatLayoutTemplateVo> getInfo(@PathVariable Long id) {
        return R.ok(roomSeatLayoutService.getPlatformTemplate(id));
    }

    /**
     * 新增平台默认模板（tenant_id=1 强制，is_industry_default=1 强制）。
     * <p>★默认背景图必须为透明底 PNG/SVG（M27 T27.2）；industry_code 必填且需在 industry_template 中存在。
     */
    @SaCheckPermission("system:platformRoomTemplate:add")
    @Log(title = "平台默认房间模板新增", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody RoomSeatLayoutTemplateBo bo) {
        return R.ok(roomSeatLayoutService.savePlatformTemplate(bo));
    }

    /**
     * 编辑平台默认模板。
     */
    @SaCheckPermission("system:platformRoomTemplate:edit")
    @Log(title = "平台默认房间模板编辑", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody RoomSeatLayoutTemplateBo bo) {
        return toAjax(roomSeatLayoutService.updatePlatformTemplate(bo));
    }

    /**
     * 删除平台默认模板。
     */
    @SaCheckPermission("system:platformRoomTemplate:remove")
    @Log(title = "平台默认房间模板删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        return toAjax(roomSeatLayoutService.deletePlatformTemplate(id));
    }
}
