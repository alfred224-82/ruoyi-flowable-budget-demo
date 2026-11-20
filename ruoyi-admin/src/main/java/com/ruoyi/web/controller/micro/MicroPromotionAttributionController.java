package com.ruoyi.web.controller.micro;

import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.PromotionAttributionBo;
import com.ruoyi.system.domain.bo.PromotionScanBo;
import com.ruoyi.system.domain.vo.PromotionAttributionVo;
import com.ruoyi.system.service.IPromotionDistributionService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 扫码归因Controller（T16.6，小微版 M16 推广分销）
 * <p>扫码归因（解析 scene_param → upsert promotion_attribution）；老板 PC 端查看消费归因列表。
 * <p>★权限：scan 由顾客小程序扫码触发、list 由业务员小程序「我的顾客」触发，
 * 两者均非老板 PC 页面；customer/salesman 角色未授予 promotion 菜单权限，故沿用 M20_03 先例
 * （同 generate/my）去除 @SaCheckPermission，仅靠登录态 + 菜单可见性控制访问。
 * ★不可用 micro:distribution:* 通配符——无对应 sys_menu.perms，任何角色都拿不到该权限串。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/promo/attribution")
@RequireFeature("FEAT_PROMOTION_DISTRIBUTION")
public class MicroPromotionAttributionController extends BaseController {

    private final IPromotionDistributionService distributionService;

    /**
     * ★扫码归因（顾客端扫码登录后调用，解析 scene_param → upsert promotion_attribution）
     * <p>★契约：顾客端（H5/小程序）post 发送 JSON 请求体，必须用 @RequestBody 接收；
     * 若用 @RequestParam 则 JSON 体不绑定，抛 MissingServletRequestParameterException
     * （2026-10 生产事故：顾客扫码登录后弹窗英文报错即此因）。
     *
     * @param bo 扫码归因请求参数
     */
    @PostMapping("/scan")
    public R<Void> scan(@Validated @RequestBody PromotionScanBo bo) {
        distributionService.scanAttribution(bo.getSceneParam(), bo.getCustomerUserId(), bo.getCustomerPhone());
        return R.ok();
    }

    /**
     * 消费归因列表（老板 PC 查看）
     */
    @GetMapping("/list")
    public TableDataInfo<PromotionAttributionVo> list(PromotionAttributionBo bo, PageQuery pageQuery) {
        return distributionService.queryAttributionPage(bo, pageQuery);
    }
}
