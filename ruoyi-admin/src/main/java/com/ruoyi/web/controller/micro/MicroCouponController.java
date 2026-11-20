package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.CouponBo;
import com.ruoyi.system.domain.vo.CouponRecordVo;
import com.ruoyi.system.domain.vo.CouponVo;
import com.ruoyi.system.service.IPromotionDistributionService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 优惠券管理Controller（T16.3，小微版 M16 推广分销）
 * <p>优惠券模板 CRUD（老板 PC）+ 顾客我的优惠券（小程序端）。
 * <p>perms：老板 PC 优惠券模板 CRUD 用 micro:distribution:customer（M16_01 菜单 7183 顾客奖励记录）；
 * 顾客小程序「我的优惠券」用 micro:customer:promotion（M16_01 菜单 7440 我的推广码，含优惠券到账）。
 * ★不可用 micro:distribution:* 通配符——无对应 sys_menu.perms，role_template 授权时无法命中菜单，任何角色都拿不到该权限串。
 *
 * @author ruoyi
 * @date 2026-09-13
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/coupon")
@RequireFeature("FEAT_PROMOTION_DISTRIBUTION")
public class MicroCouponController extends BaseController {

    private final IPromotionDistributionService distributionService;

    /**
     * 优惠券模板分页列表（老板 PC）
     */
    @SaCheckPermission("micro:distribution:customer")
    @GetMapping("/list")
    public TableDataInfo<CouponVo> list(CouponBo bo, PageQuery pageQuery) {
        return distributionService.queryCouponPage(bo, pageQuery);
    }

    /**
     * 优惠券模板详情
     */
    @SaCheckPermission("micro:distribution:customer")
    @GetMapping("/{id}")
    public R<CouponVo> getInfo(@PathVariable Long id) {
        return R.ok(distributionService.queryCouponById(id));
    }

    /**
     * 新增优惠券模板
     */
    @SaCheckPermission("micro:distribution:customer")
    @Log(title = "优惠券模板", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated @RequestBody CouponBo bo) {
        return R.ok(distributionService.insertCoupon(bo));
    }

    /**
     * 编辑优惠券模板
     */
    @SaCheckPermission("micro:distribution:customer")
    @Log(title = "优惠券模板", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody CouponBo bo) {
        return toAjax(distributionService.updateCoupon(bo));
    }

    /**
     * 删除优惠券模板
     */
    @SaCheckPermission("micro:distribution:customer")
    @Log(title = "优惠券模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(distributionService.deleteCoupon(ids));
    }

    /**
     * ★我的优惠券（顾客小程序端）
     */
    @SaCheckPermission("micro:customer:promotion")
    @GetMapping("/my")
    public R<List<CouponRecordVo>> myCoupons() {
        return R.ok(distributionService.myCoupons());
    }
}
