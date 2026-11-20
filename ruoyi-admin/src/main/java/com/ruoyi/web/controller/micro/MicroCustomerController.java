package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.domain.bo.BookingOrderBo;
import com.ruoyi.system.domain.vo.BookingOrderVo;
import com.ruoyi.system.domain.vo.MyActivityVo;
import com.ruoyi.system.domain.vo.PromotionActivityVo;
import com.ruoyi.system.domain.vo.ServiceCategoryVo;
import com.ruoyi.system.domain.vo.ServiceItemVo;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.service.IBookingOrderService;
import com.ruoyi.system.service.ICustomerCreditService;
import com.ruoyi.system.service.IMicroActivitySettingService;
import com.ruoyi.system.service.IPromotionActivityService;
import com.ruoyi.system.service.IServiceCategoryService;
import com.ruoyi.system.service.IServiceItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 顾客小程序Controller（T15.12，client_type=mini）
 * <p>发现页（V3.7）：最新活动 + 自助预约（项目类→房间→助教→手机号→提交）。
 * <p>我的预约 → 取消预约 → 查看收款方式。
 * <p>★V1 零支付合规：仅有 micro:customer:payInfo（收款方式提示文案），【无 micro:customer:pay、无在线支付入口、不调支付接口】。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/customer")
public class MicroCustomerController extends BaseController {

    private final IServiceItemService serviceItemService;
    private final IBookingOrderService bookingOrderService;
    private final ICustomerCreditService customerCreditService;
    private final IServiceCategoryService serviceCategoryService;
    private final IMicroActivitySettingService activitySettingService;
    private final IPromotionActivityService promotionActivityService;
    private final SysTenantMapper sysTenantMapper;

    /**
     * 浏览店铺：上架服务项目列表（categoryId 可空=全部）
     */
    @SaCheckPermission("micro:customer:shop")
    @GetMapping("/shop")
    public R<List<ServiceItemVo>> shop(@RequestParam(required = false) Long categoryId) {
        return R.ok(serviceItemService.listOnSale(categoryId));
    }

    // ===== V3.7 发现页：自助预约数据源 =====

    /**
     * ★V3.7 顾客发现页——项目分类列表（category_type='service'，排除房间/空间分类）
     * <p>顾客先选分类，再选分类下的具体项目。
     */
    @SaCheckPermission("micro:customer:shop")
    @GetMapping("/categories")
    public R<List<ServiceCategoryVo>> categories() {
        return R.ok(serviceCategoryService.listEnabledByType("service"));
    }

    /**
     * ★V3.7 顾客发现页——可选房间/空间列表（复用活动设置 listRooms，含自愈初始化）
     */
    @SaCheckPermission("micro:customer:shop")
    @GetMapping("/rooms")
    public R<List<Map<String, Object>>> rooms() {
        return R.ok(activitySettingService.listRooms());
    }

    /**
     * ★V3.7 顾客发现页——可选助教列表（复用活动设置 listStaff，salesman 角色用户）
     */
    @SaCheckPermission("micro:customer:shop")
    @GetMapping("/staff")
    public R<List<Map<String, Object>>> staff() {
        return R.ok(activitySettingService.listStaff());
    }

    /**
     * 提交预约（生成「待到店付」订单，★无在线支付）
     */
    @SaCheckPermission("micro:customer:submitBook")
    @Log(title = "顾客预约", businessType = BusinessType.INSERT)
    @PostMapping("/submitBook")
    public R<Long> submitBook(@Validated(AddGroup.class) @RequestBody BookingOrderBo bo) {
        return R.ok(bookingOrderService.submitByCustomer(bo));
    }

    /**
     * 我的预约
     */
    @SaCheckPermission("micro:customer:myBook")
    @GetMapping("/myBook")
    public R<List<BookingOrderVo>> myBook() {
        return R.ok(bookingOrderService.myBookings());
    }

    /**
     * ★我的预约详情（顾客域接口，仅限本人订单；/micro/booking/{id} 为 boss 域权限，顾客不可用）
     */
    @SaCheckPermission("micro:customer:myBook")
    @GetMapping("/myBook/{id}")
    public R<BookingOrderVo> myBookDetail(@PathVariable Long id) {
        BookingOrderVo vo = bookingOrderService.queryById(id);
        Long uid = LoginHelper.getUserId();
        if (vo == null || vo.getCustomerUserId() == null || !vo.getCustomerUserId().equals(uid)) {
            return R.fail("预约记录不存在");
        }
        return R.ok(vo);
    }

    /**
     * 取消预约（仅限本人订单）
     */
    @SaCheckPermission("micro:customer:cancelBook")
    @Log(title = "顾客取消预约", businessType = BusinessType.UPDATE)
    @PutMapping("/cancelBook/{id}")
    public R<Void> cancelBook(@PathVariable Long id) {
        BookingOrderVo vo = bookingOrderService.queryById(id);
        Long uid = LoginHelper.getUserId();
        // 归属校验：顾客只能取消本人的预约
        if (vo.getCustomerUserId() == null || !vo.getCustomerUserId().equals(uid)) {
            return R.fail("只能取消本人的预约");
        }
        return toAjax(bookingOrderService.cancel(id));
    }

    /**
     * 查看收款方式（★仅展示提示文案，不调支付接口、无在线支付）
     */
    @SaCheckPermission("micro:customer:payInfo")
    @GetMapping("/payInfo")
    public R<String> payInfo() {
        return R.ok("本店支持到店付款：服务完成后请向前台/老板付款，或扫描老板收款码支付。本小程序不提供在线支付。");
    }

    /**
     * ★查询当前登录顾客自己的信誉状态（T15.32 防爽约按钮置灰，仅 micro_pro 生效）
     * <p>返回 isBlacklisted（是否拉黑）+ blocked（是否禁止新预约）+ message（提示文案）。
     * <p>顾客端自查无需 boss 权限，登录即可（micro:customer:shop 同档）。
     */
    @SaCheckPermission("micro:customer:shop")
    @GetMapping("/myCredit")
    public R<Map<String, Object>> myCredit() {
        Long uid = LoginHelper.getUserId();
        boolean blacklisted = customerCreditService.isBlacklisted(uid);
        Map<String, Object> result = new HashMap<>(4);
        result.put("isBlacklisted", blacklisted);
        result.put("blocked", blacklisted);
        result.put("message", blacklisted
            ? "您已被本店列入黑名单（累计爽约达上限），暂时无法提交新预约，请联系老板解除。"
            : "");
        return R.ok(result);
    }

    /**
     * ★我的活动（M22，合并自助预约 + 活动报名）
     * @param status 状态过滤：空=全部，ONGOING=进行中，ENDED=已结束
     */
    @SaCheckPermission("micro:customer:myBook")
    @GetMapping("/myActivities")
    public R<List<MyActivityVo>> myActivities(
            @RequestParam(required = false) String status) {
        return R.ok(promotionActivityService.listCustomerActivities(status));
    }

    /**
     * ★T32.1 顾客首页（发现）聚合数据：门店名 + 最新活动 + 门店排班入口开关
     * <p>权限：登录即可（micro:customer:shop 同档，顾客首页数据）。
     * <p>返回体：
     * <ul>
     *   <li>tenantName  门店名称</li>
     *   <li>tenantCity  门店所在城市（用于「门店名 → 地区」头部展示）</li>
     *   <li>activities  最新活动（最多 3 条，status=PUBLISHED 且未过期）</li>
     *   <li>selfBookingEnabled  自助预约入口是否开启（FEAT_SELF_BOOKING + 租户策略）</li>
     *   <li>staffScheduleEnabled  门店排班入口是否开启（FEAT_STAFF_SCHEDULE）</li>
     * </ul>
     */
    @SaCheckPermission("micro:customer:shop")
    @GetMapping("/home")
    public R<CustomerHomeVo> home() {
        Long tenantId = LoginHelper.getTenantId();
        CustomerHomeVo vo = new CustomerHomeVo();
        // ① 门店名/城市
        if (tenantId != null) {
            SysTenant tenant = sysTenantMapper.selectById(tenantId);
            if (tenant != null) {
                vo.setTenantName(StrUtil.blankToDefault(tenant.getCompanyName(), "欢迎到店"));
                vo.setTenantCity(tenant.getCity());
                vo.setLogoUrl(tenant.getLogoUrl());
            }
        }
        if (StrUtil.isBlank(vo.getTenantName())) {
            vo.setTenantName("欢迎到店");
        }
        // ② 最新活动（复用 listPublished，仅取前 3 条；★防线：已结束/已锁定活动由前端再过滤）
        List<PromotionActivityVo> activities = new ArrayList<>();
        if (tenantId != null) {
            try {
                List<PromotionActivityVo> list = promotionActivityService.listPublished(tenantId, null, null, null, null);
                if (list != null) {
                    activities = list.stream().limit(3).collect(Collectors.toList());
                }
            } catch (Exception ignore) {
                // fail-soft：活动加载失败不阻塞首页
            }
        }
        vo.setActivities(activities);
        // ③ 入口开关由前端读本地 features 与租户策略，后端不重复计算
        return R.ok(vo);
    }

    /**
     * 顾客首页聚合 VO（T32.1）
     */
    @lombok.Data
    public static class CustomerHomeVo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        /** 门店名称 */
        private String tenantName;
        /** 门店所在城市 */
        private String tenantCity;
        /** 门店 Logo URL */
        private String logoUrl;
        /** 最新活动（最多 3 条） */
        private List<PromotionActivityVo> activities;
    }
}
