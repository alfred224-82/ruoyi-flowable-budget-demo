package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.CopyLayoutBo;
import com.ruoyi.system.domain.bo.RoomSeatLayoutBo;
import com.ruoyi.system.domain.bo.SaveAsTemplateBo;
import com.ruoyi.system.domain.vo.RoomSeatLayoutTemplateVo;
import com.ruoyi.system.domain.vo.RoomSeatLayoutVo;
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
 * 小微版房间布局 Controller（M26 房间座位布局与选座）
 * <p>覆盖任务：
 * <ul>
 *   <li>T26.3 背景图上传前置校验 {@code POST /micro/room/uploadBgImage}（v1.1 已交付）</li>
 *   <li>T26.4 拖拽定位保存 {@code GET|POST /micro/room/layout/*}（v1.3 新增）</li>
 *   <li>T26.5 复制布局 / 存为模板 / 应用模板（v1.3 新增）</li>
 * </ul>
 * <p>权限：{@code micro:room:layout} 主权限（菜单 7280）；上传按钮 {@code micro:room:uploadBg}（7281）；
 *   复制布局 {@code micro:room:copyLayout}（7282）；存为模板 {@code micro:room:saveTemplate}（7283）。
 * <p>功能开关：类级 {@code @RequireFeature("FEAT_ROOM_SEAT_LAYOUT")}，micro_pro=1 / micro_lite=0；
 *   FEAT_ROOM_SEAT_LAYOUT 由 M26_02 SQL 落 feature_flag 表（第 41 项，组 E）。
 * <p>★T26.6 平台默认房间模板 CRUD 归 {@link MicroPlatformRoomTemplateController}，本 Controller 仅暴露
 *   Boss 侧只读的模板列表 / 应用模板端点。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/room")
@RequireFeature("FEAT_ROOM_SEAT_LAYOUT")
public class MicroRoomLayoutController extends BaseController {

    private final ISysOssService sysOssService;
    private final RoomBgImageValidator roomBgImageValidator;
    private final IRoomSeatLayoutService roomSeatLayoutService;

    // ============================================================
    // T26.3 背景图上传（v1.1 已交付，保持不变）
    // ============================================================

    /**
     * 上传房间背景图（T26.3，前置校验格式/大小/尺寸后落 OSS，返回 URL）。
     *
     * @param file 图片文件（JPG/PNG/WebP，≤2MB，≤1920×1080）
     * @return url=OSS 访问地址；fileName=原始文件名；ossId=OSS 主键
     */
    @SaCheckPermission("micro:room:uploadBg")
    @Log(title = "房间背景图上传", businessType = BusinessType.INSERT)
    @PostMapping(value = "/uploadBgImage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Map<String, String>> uploadBgImage(@RequestPart("file") MultipartFile file) {
        if (ObjectUtil.isNull(file)) {
            return R.fail("上传文件不能为空");
        }
        // 前置校验（大小 / 格式 / 尺寸），不合规抛 ServiceException，OSS 不落库
        roomBgImageValidator.validate(file);
        SysOssVo oss = sysOssService.upload(file);
        Map<String, String> map = new HashMap<>(3);
        map.put("url", oss.getUrl());
        map.put("fileName", oss.getOriginalName());
        map.put("ossId", oss.getOssId().toString());
        return R.ok(map);
    }

    // ============================================================
    // T26.4 布局查询 / 保存（拖拽定位）
    // ============================================================

    /**
     * 查询房间布局（含座位点批量列表）。
     * <p>★房间未配置布局时返回 data=null，前端提示"请先配置座位布局"。
     *
     * @param roomId 房间ID（FK→service_item.id）
     */
    @SaCheckPermission("micro:room:layout")
    @GetMapping("/layout/{roomId}")
    public R<RoomSeatLayoutVo> getLayout(@PathVariable Long roomId) {
        return R.ok(roomSeatLayoutService.getLayoutByRoomId(roomId));
    }

    /**
     * 保存布局（新增 / 编辑；一次事务写入 room_seat_layout + room_seat 全量替换）。
     * <p>★保存成功后同步刷新 service_item.has_seat_layout=1 + seat_capacity=座位数（T26.2 联动）。
     * <p>★座位号 UNIQUE(layout_id, seat_no) 由 Service 层预校验 + DB 兜底。
     */
    @SaCheckPermission("micro:room:layout")
    @Log(title = "房间座位布局保存", businessType = BusinessType.INSERT)
    @PostMapping("/layout/save")
    public R<Long> saveLayout(@Validated({AddGroup.class, EditGroup.class}) @RequestBody RoomSeatLayoutBo bo) {
        return R.ok(roomSeatLayoutService.saveLayout(bo));
    }

    /**
     * 删除房间布局（连带删除座位点，房间回退 has_seat_layout=0）。
     */
    @SaCheckPermission("micro:room:layout")
    @Log(title = "房间座位布局删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/layout/{layoutId}")
    public R<Void> deleteLayout(@PathVariable Long layoutId) {
        return toAjax(roomSeatLayoutService.deleteLayout(layoutId));
    }

    // ============================================================
    // T26.5 复制布局 / 存为模板 / 应用模板 / 模板列表
    // ============================================================

    /**
     * 从已有房间复制布局（T26.5，Boss 专属）。
     * <p>★脱钩原则：目标房间拥有独立布局记录；源房间修改不影响目标。
     */
    @SaCheckPermission("micro:room:copyLayout")
    @Log(title = "复制房间布局", businessType = BusinessType.INSERT)
    @PostMapping("/layout/copyFromRoom")
    public R<Long> copyFromRoom(@Validated(AddGroup.class) @RequestBody CopyLayoutBo bo) {
        return R.ok(roomSeatLayoutService.copyFromRoom(bo));
    }

    /**
     * 存为模板（T26.5，Boss 私有模板，is_industry_default=0）。
     * <p>★UNIQUE(tenant_id, name) 兜底：同租户下模板名唯一，重名抛 ServiceException。
     */
    @SaCheckPermission("micro:room:saveTemplate")
    @Log(title = "存为布局模板", businessType = BusinessType.INSERT)
    @PostMapping("/layout/saveAsTemplate")
    public R<Long> saveAsTemplate(@Validated(AddGroup.class) @RequestBody SaveAsTemplateBo bo) {
        return R.ok(roomSeatLayoutService.saveAsTemplate(bo));
    }

    /**
     * 查询 Boss 可见的模板列表（本租户私有 + 平台行业默认，T26.5 / T26.6）。
     *
     * @param industryCode 行业编码（可空；传入时按行业过滤平台默认模板）
     */
    @SaCheckPermission("micro:room:layout")
    @GetMapping("/layout/templates")
    public R<List<RoomSeatLayoutTemplateVo>> listTemplates(@RequestParam(required = false) String industryCode) {
        return R.ok(roomSeatLayoutService.listVisibleTemplates(industryCode));
    }

    /**
     * 应用模板到指定房间（T26.5 / T26.6）。
     * <p>★应用后 room_seat_layout.template_id 记录模板ID（留痕），后续修改与模板脱钩。
     *
     * @param roomId     目标房间ID
     * @param templateId 模板ID
     */
    @SaCheckPermission("micro:room:layout")
    @Log(title = "应用房间布局模板", businessType = BusinessType.INSERT)
    @PostMapping("/layout/applyTemplate")
    public R<Long> applyTemplate(@RequestParam Long roomId, @RequestParam Long templateId) {
        return R.ok(roomSeatLayoutService.applyTemplate(roomId, templateId));
    }
}
