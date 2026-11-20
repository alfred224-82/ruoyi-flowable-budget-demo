package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.service.IPromotionDistributionService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 推广分销管理Controller（T16.10，小微版 M16 boss PC 端推广看板）
 * <p>boss PC 端推广分销管理首页（推广数据统计/推广业绩统计/优惠券管理）。
 * <p>perms：micro:distribution:stat（M16_01 种子菜单 7181 推广数据看板；★不可用 micro:distribution:* 通配符——
 * 无对应 sys_menu.perms，role_template 授权时无法命中菜单，任何角色都拿不到该权限串）。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/distribution")
@RequireFeature("FEAT_PROMOTION_DISTRIBUTION")
public class MicroDistributionController extends BaseController {

    private static final List<String> VALID_PERIODS = Arrays.asList("day", "week", "month");

    private final IPromotionDistributionService distributionService;

    /**
     * 推广数据统计（按推广人汇总：带来人数/已消费/拉新成功数）
     *
     * @param period 统计周期：day/week/month
     */
    @SaCheckPermission("micro:distribution:stat")
    @GetMapping("/stat/promotion")
    public R<List<Map<String, Object>>> promotionStat(@RequestParam(defaultValue = "month") String period) {
        if (!VALID_PERIODS.contains(period)) {
            return R.fail("统计周期参数非法，仅支持 day/week/month");
        }
        return R.ok(distributionService.queryPromotionStat(period));
    }

    /**
     * 推广业绩统计（按天/周/月）
     *
     * @param period 统计周期：day/week/month
     */
    @SaCheckPermission("micro:distribution:stat")
    @GetMapping("/stat/performance")
    public R<List<Map<String, Object>>> performanceStat(@RequestParam(defaultValue = "month") String period) {
        if (!VALID_PERIODS.contains(period)) {
            return R.fail("统计周期参数非法，仅支持 day/week/month");
        }
        return R.ok(distributionService.queryPerformanceStat(period));
    }
}
