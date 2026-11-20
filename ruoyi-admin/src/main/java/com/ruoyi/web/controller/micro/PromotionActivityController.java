package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.PromotionActivityBo;
import com.ruoyi.system.domain.vo.PromotionActivityVo;
import com.ruoyi.system.service.IPosterGenerationService;
import com.ruoyi.system.service.IPromotionActivityService;
import com.ruoyi.system.service.ISysOssService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 推广发布Controller（T15.28，老板 PC 活动/预告/海报管理）
 * <p>perms 对齐 M15_06 种子菜单：列表 micro:promo:list；发布 micro:promo:add；编辑 micro:promo:edit；
 * 删除 micro:promo:remove；海报 micro:promo:poster。
 * <p>★lite 档老板仅此推广能力（无预约/结算/收支/报表）；发布的活动在「活动浏览页」(micro:activity:browse) 只读展示。
 *
 * @author ruoyi
 * @date 2026-09-08
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/promo")
public class PromotionActivityController extends BaseController {

    private final IPromotionActivityService promotionActivityService;
    private final IPosterGenerationService posterGenerationService;
    private final ISysOssService sysOssService;

    /**
     * 本店活动列表（老板 PC 管理）
     */
    @SaCheckPermission("micro:promo:list")
    @GetMapping("/list")
    public TableDataInfo<PromotionActivityVo> list(PromotionActivityBo bo, PageQuery pageQuery) {
        return promotionActivityService.queryPageList(bo, pageQuery);
    }

    /**
     * 活动详情
     */
    @SaCheckPermission("micro:promo:list")
    @GetMapping("/{id}")
    public R<PromotionActivityVo> getInfo(@PathVariable Long id) {
        return R.ok(promotionActivityService.queryById(id));
    }

    /**
     * 发布活动（新建，默认草稿；status=PUBLISHED 时即时发布）
     */
    @SaCheckPermission("micro:promo:add")
    @Log(title = "推广发布", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody PromotionActivityBo bo) {
        return R.ok(promotionActivityService.insert(bo));
    }

    /**
     * 编辑活动
     */
    @SaCheckPermission("micro:promo:edit")
    @Log(title = "推广发布", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PromotionActivityBo bo) {
        return toAjax(promotionActivityService.update(bo));
    }

    /**
     * 发布/下架/结束活动（status：PUBLISHED/DRAFT/ENDED）
     */
    @SaCheckPermission("micro:promo:edit")
    @Log(title = "推广发布", businessType = BusinessType.UPDATE)
    @PutMapping("/status/{id}/{status}")
    public R<Void> changeStatus(@PathVariable Long id, @PathVariable String status) {
        return toAjax(promotionActivityService.changeStatus(id, status));
    }

    /**
     * 生成分享海报（micro:promo:poster，海报带租户号二维码）
     * <p>返回活动的海报 URL 与标题，供前端渲染/下载分享；二维码写 share_scene（前端合成）。
     */
    @SaCheckPermission("micro:promo:poster")
    @Log(title = "推广发布", businessType = BusinessType.OTHER)
    @GetMapping("/poster/{id}")
    public R<PromotionActivityVo> poster(@PathVariable Long id) {
        return R.ok(promotionActivityService.queryById(id));
    }

    /**
     * 删除活动（逻辑删除，可批量）
     */
    @SaCheckPermission("micro:promo:remove")
    @Log(title = "推广发布", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(promotionActivityService.deleteByIds(Arrays.asList(ids)));
    }

    /**
     * ★锁定/解锁活动（M22，锁定后不可报名/取消）
     * <p>★M28 T28.1 起委派到 {@code lockActivity(id)} / {@code unlockActivity(id)}，使旧端点也经
     *   {@code @SeatOperation} 切面自动记录操作日志（含锁定前后座位快照）。
     * <p>★新入口推荐用 {@code /micro/activity/{id}/lock} 与 {@code /micro/activity/{id}/unlock}
     *   （perms micro:activity:lock / micro:activity:unlock）；本端点保留以兼容存量前端。
     */
    @SaCheckPermission("micro:promo:lock")
    @Log(title = "活动锁定", businessType = BusinessType.UPDATE)
    @PutMapping("/lock/{id}/{locked}")
    public R<Void> lock(@PathVariable Long id, @PathVariable boolean locked) {
        if (locked) {
            promotionActivityService.lockActivity(id);
        } else {
            promotionActivityService.unlockActivity(id);
        }
        return R.ok();
    }

    // ===== ★M31 T31.1/T31.2 活动海报生成与二维码 =====
    // ★权限策略：海报生成/上传/查询复用 micro:promo:edit（活动编辑权限），
    //   不引入独立权限码——避免租户克隆角色 sys_role_menu 未授权导致 403（参照 M20_03 先例）。
    //   前端按钮已由 v-hasPermi="['micro:promo:edit']" 控制可见性。

    /**
     * ★M31 T31.1 上传海报底图（JPG/PNG/WebP，≤5MB）
     */
    @SaCheckPermission("micro:promo:edit")
    @Log(title = "海报底图上传", businessType = BusinessType.INSERT)
    @PostMapping(value = "/{id}/uploadPosterBg", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Map<String, String>> uploadPosterBg(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        if (ObjectUtil.isNull(file)) {
            return R.fail("上传文件不能为空");
        }
        // 前置校验：格式 + 大小
        String contentType = file.getContentType();
        if (contentType == null || !contentType.matches("image/(jpeg|png|webp)")) {
            return R.fail("仅支持 JPG/PNG/WebP 格式");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            return R.fail("海报底图不能超过 5MB");
        }
        var oss = sysOssService.upload(file);
        Map<String, String> map = new HashMap<>(3);
        map.put("url", oss.getUrl());
        map.put("fileName", oss.getOriginalName());
        map.put("ossId", oss.getOssId().toString());
        return R.ok(map);
    }

    /**
     * ★M31 T31.2 触发海报合成（底图 + 右下角二维码）
     */
    @SaCheckPermission("micro:promo:edit")
    @Log(title = "海报合成", businessType = BusinessType.OTHER)
    @PostMapping("/{id}/generatePoster")
    public R<Map<String, String>> generatePoster(@PathVariable Long id,
                                                  @RequestParam String bgImageUrl) {
        String posterUrl = posterGenerationService.generateActivityPoster(id, bgImageUrl);
        Map<String, String> map = new HashMap<>(2);
        map.put("posterUrl", posterUrl);
        map.put("status", "COMPLETED");
        return R.ok(map);
    }

    /**
     * ★M31 T31.2 查询合成任务状态（异步模式预留）
     */
    @SaCheckPermission("micro:promo:edit")
    @GetMapping("/poster/task/{taskId}")
    public R<Map<String, Object>> getTaskStatus(@PathVariable String taskId) {
        return R.ok(posterGenerationService.getTaskStatus(taskId));
    }
}
