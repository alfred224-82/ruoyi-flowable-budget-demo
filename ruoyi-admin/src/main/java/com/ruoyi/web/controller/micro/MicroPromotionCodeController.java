package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.PromotionCodeBo;
import com.ruoyi.system.domain.vo.PromotionCodeVo;
import com.ruoyi.system.service.IPromotionDistributionService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 推广码管理Controller（T16.5/T16.11，小微版 M16 推广分销）
 * <p>boss PC 端管理推广码（生成/启用/禁用/列表）；业务员/顾客小程序端「我的推广码」。
 * <p>perms：老板 PC 推广码管理（list/toggle）DB 无专属菜单，复用老板已有的推广数据看板权限 micro:distribution:stat（菜单 7181）；
 * generate/my 为公用功能，沿用 M20_03 先例无权限校验。
 * ★不可用 micro:distribution:* 通配符——无对应 sys_menu.perms，任何角色都拿不到该权限串。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/promo/code")
@RequireFeature("FEAT_PROMOTION_DISTRIBUTION")
public class MicroPromotionCodeController extends BaseController {

    private final IPromotionDistributionService distributionService;

    /**
     * ★生成推广码（门店码/个人码；首次生成、之后复用；一人一码永久有效）
     * <p>★M20_03 去除权限校验：推广码生成为老板公用功能，菜单可见性控制访问。
     *
     * @param codeType store/personal
     */
    @Log(title = "推广码", businessType = BusinessType.INSERT)
    @PostMapping("/generate")
    public R<PromotionCodeVo> generate(@RequestParam String codeType) {
        return R.ok(distributionService.generateCode(codeType));
    }

    /**
     * 推广码列表（老板 PC 管理）
     */
    @SaCheckPermission("micro:distribution:stat")
    @GetMapping("/list")
    public TableDataInfo<PromotionCodeVo> list(PromotionCodeBo bo, PageQuery pageQuery) {
        return distributionService.queryCodePage(bo, pageQuery);
    }

    /**
     * ★启用/禁用推广码（禁用后新扫码不再归因；禁用前消费照常算）
     */
    @SaCheckPermission("micro:distribution:stat")
    @Log(title = "推广码", businessType = BusinessType.UPDATE)
    @PutMapping("/toggle/{id}/{status}")
    public R<Void> toggle(@PathVariable Long id, @PathVariable String status) {
        return toAjax(distributionService.toggleCode(id, status));
    }

    /**
     * ★我的推广码 + 推广数据 + 奖励/优惠券到账（小程序端 + 老板 PC 推广码页面）
     * <p>★M20_03 去除权限校验：推广数据查看为老板公用功能，菜单可见性控制访问。
     */
    @GetMapping("/my")
    public R<Map<String, Object>> my() {
        return R.ok(distributionService.myPromotion());
    }

    /**
     * ★我的H5推广码（顾客手机浏览器扫码直接进入登录页，带 ref_t/ref_u 参数用于登录后归因）
     * <p>后端用 ZXing 生成 H5 URL 的二维码（base64 图片），替代原微信小程序码；
     * 二维码内容 = promo.h5-base-url + ?ref_t=租户ID&ref_u=用户ID；
     * 顾客手机浏览器扫码 → 打开 H5 登录页 → 登录后自动调用归因接口记录推荐关系。
     * <p>★沿用 M20_03 先例无权限校验，仅保留功能开关。
     */
    @GetMapping("/myQrcode")
    public R<Map<String, Object>> myQrcode() {
        return R.ok(distributionService.generateMyQrcode());
    }
}
