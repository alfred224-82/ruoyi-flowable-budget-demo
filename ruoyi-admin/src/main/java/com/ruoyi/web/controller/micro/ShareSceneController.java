package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.ShareSceneBo;
import com.ruoyi.system.domain.vo.ShareSceneVo;
import com.ruoyi.system.service.IShareSceneService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;

/**
 * 小程序场景/分享Controller（T15.3 建表 / T15.19 扫码锁定租户 / T15.24 生成分享海报）
 * <p>resolve：扫码/分享进入时跨租户按 sceneId 反查场景（含 tenantId），实现「扫码进入即锁定租户」——多租户单小程序架构入口锚点。
 * <p>推广发布（老板 PC，T15.24）：活动/分享场景 CRUD 对齐 M15_06 预置 perms——micro:promo:list（读）/ add（发布）/ edit（编辑）
 * / remove（删除）/ poster（生成带租户号二维码的分享海报，写入 share_scene）；save 为按 sceneId 幂等 upsert，含 id 即更新。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/share")
public class ShareSceneController extends BaseController {

    private final IShareSceneService shareSceneService;

    /**
     * 扫码/分享进入：跨租户按 sceneId 反查场景，锁定租户（仅需登录，任意 mini 用户；过期场景报错）
     */
    @SaCheckLogin
    @GetMapping("/resolve")
    public R<ShareSceneVo> resolve(@RequestParam @NotBlank(message = "场景标识不能为空") String sceneId) {
        return R.ok(shareSceneService.resolveBySceneId(sceneId));
    }

    /**
     * 场景列表（本租户）
     */
    @SaCheckPermission("micro:promo:list")
    @GetMapping("/list")
    public TableDataInfo<ShareSceneVo> list(ShareSceneBo bo, PageQuery pageQuery) {
        return shareSceneService.queryPageList(bo, pageQuery);
    }

    /**
     * 场景详情
     */
    @SaCheckPermission("micro:promo:list")
    @GetMapping("/{id}")
    public R<ShareSceneVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(shareSceneService.queryById(id));
    }

    /**
     * 发布活动/新增分享场景（按 sceneId 幂等 upsert，写入 share_scene）
     */
    @SaCheckPermission("micro:promo:add")
    @Log(title = "推广活动", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody ShareSceneBo bo) {
        return R.ok(shareSceneService.save(bo));
    }

    /**
     * 编辑活动/分享场景（save 为按 sceneId upsert，含 id/既有 sceneId 即更新）
     */
    @SaCheckPermission("micro:promo:edit")
    @Log(title = "推广活动", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Long> edit(@RequestBody ShareSceneBo bo) {
        return R.ok(shareSceneService.save(bo));
    }

    /**
     * 生成分享海报（T15.24）：海报带租户号二维码（sceneId 编码 tenantId、qrCodeUrl 为 OSS 海报图），写入 share_scene
     */
    @SaCheckPermission("micro:promo:poster")
    @Log(title = "分享海报", businessType = BusinessType.INSERT)
    @PostMapping("/poster")
    public R<Long> poster(@Validated(AddGroup.class) @RequestBody ShareSceneBo bo) {
        return R.ok(shareSceneService.save(bo));
    }

    /**
     * 删除活动/场景（share_scene 无 del_flag，物理删除，可批量）
     */
    @SaCheckPermission("micro:promo:remove")
    @Log(title = "推广活动", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(shareSceneService.deleteByIds(Arrays.asList(ids)));
    }
}
