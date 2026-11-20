package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.PromotionRewardConfigBo;
import com.ruoyi.system.domain.bo.PromotionRewardRecordBo;
import com.ruoyi.system.domain.vo.PromotionRewardConfigVo;
import com.ruoyi.system.domain.vo.PromotionRewardRecordVo;
import com.ruoyi.system.service.IPromotionRewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 推广奖励Controller（T16.2/T16.7/T16.8/T16.9，小微版 M16 推广分销）
 * <p>奖励配置管理 / 拉新奖励结算 / 首单识别 / 奖励发放记录 / 标记已线下发放。
 * <p>perms：奖励配置 micro:distribution:rewardConfig（菜单 7184）；奖励记录按渠道由业务员/顾客两个老板 PC 页面共用，
 * 故取 OR(micro:distribution:salesman, micro:distribution:customer)（菜单 7182/7183）；标记线下发放属业务员现金奖励 micro:distribution:salesman。
 * ★不可用 micro:distribution:* 通配符——无对应 sys_menu.perms，role_template 授权时无法命中菜单，任何角色都拿不到该权限串。
 * <p>★V1 零支付：员工现金奖励走「标记已线下发放」记账口径，不调任何支付接口。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/promo/reward")
@RequireFeature("FEAT_PROMOTION_DISTRIBUTION")
public class MicroPromotionRewardController extends BaseController {

    private final IPromotionRewardService rewardService;

    // ===== T16.2 奖励配置 =====

    /**
     * 奖励配置列表（按 reward_channel 分流：salesman/customer）
     */
    @SaCheckPermission("micro:distribution:rewardConfig")
    @GetMapping("/config/list")
    public TableDataInfo<PromotionRewardConfigVo> configList(PromotionRewardConfigBo bo, PageQuery pageQuery) {
        return rewardService.queryConfigPage(bo, pageQuery);
    }

    /**
     * 更新奖励配置（★trigger_rule 恒 NEW_CUSTOMER 不可改；仅 reward_value/coupon_id/status 可变）
     */
    @SaCheckPermission("micro:distribution:rewardConfig")
    @Log(title = "奖励配置", businessType = BusinessType.UPDATE)
    @PutMapping("/config")
    public R<Void> updateConfig(@RequestBody PromotionRewardConfigBo bo) {
        return toAjax(rewardService.updateConfig(bo));
    }

    // ===== T16.7 拉新奖励结算 =====

    /**
     * ★拉新奖励结算（被推广顾客首次消费触发，仅一次）
     * <p>★由消费/结算业务流触发（非老板 PC 手动），沿用 M20_03 先例去除 @SaCheckPermission，仅靠登录态控制。
     *
     * @param customerUserId 被拉新顾客用户ID
     * @param firstOrderId   触发拉新的首次消费订单ID
     */
    @Log(title = "拉新奖励", businessType = BusinessType.INSERT)
    @PostMapping("/settle")
    public R<Void> settle(@RequestParam Long customerUserId, @RequestParam Long firstOrderId) {
        rewardService.settleNewCustomerReward(customerUserId, firstOrderId);
        return R.ok();
    }

    /**
     * 奖励发放记录列表（老板 PC，按 reward_channel 分流）
     */
    @SaCheckPermission(value = {"micro:distribution:salesman", "micro:distribution:customer"}, mode = SaMode.OR)
    @GetMapping("/records")
    public TableDataInfo<PromotionRewardRecordVo> records(PromotionRewardRecordBo bo, PageQuery pageQuery) {
        return rewardService.queryRewardRecordPage(bo, pageQuery);
    }

    /**
     * ★老板标记现金奖励已线下发放（issue_status=PENDING→PAID_OFFLINE）
     */
    @SaCheckPermission("micro:distribution:salesman")
    @Log(title = "奖励发放", businessType = BusinessType.UPDATE)
    @PutMapping("/markPaid/{id}")
    public R<Void> markPaid(@PathVariable Long id) {
        return toAjax(rewardService.markPaidOffline(id));
    }

    // ===== T16.8 首单识别 =====

    /**
     * ★首单判定（手机号+活动独立判断是否首次）
     * <p>★下单流程中由顾客/系统触发（只读），沿用 M20_03 先例去除 @SaCheckPermission，仅靠登录态控制。
     *
     * @return true=首单(可享优惠) false=老客
     */
    @GetMapping("/firstOrder/check")
    public R<Boolean> checkFirstOrder(@RequestParam Long activityId,
                                       @RequestParam String customerPhone,
                                       @RequestParam(required = false) Long customerUserId,
                                       @RequestParam(required = false) Long orderId) {
        return R.ok(rewardService.checkFirstOrder(activityId, customerPhone, customerUserId, orderId));
    }
}
