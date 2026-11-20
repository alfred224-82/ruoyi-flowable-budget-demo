package com.ruoyi.web.controller.micro;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.PromotionRewardConfig;
import com.ruoyi.system.mapper.PromotionRewardConfigMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * 老板拉新奖励配置Controller（T18.12，小微版 M18）
 * <p>「拉新奖励配置」页面（micro/distribution/rewardConfig）：
 * <ul>
 *   <li>① 开启/关闭拉新奖励（全局开关）</li>
 *   <li>② 奖励形式（现金/抵扣券/积分，三选一）</li>
 *   <li>③ 触发条件（新客绑定/新客首单，二选一）</li>
 *   <li>④ 奖励上限设置（可选，每日/每月上限）</li>
 * </ul>
 * <p>★复用既有 promotion_reward_config 表，仅老板可配置（唯一入口）。
 * <p>★V1 零支付延续：现金=标记已线下发放。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/rewardConfig")
@RequireFeature("FEAT_PROMOTION_DISTRIBUTION")
public class MicroRewardConfigController extends BaseController {

    private final PromotionRewardConfigMapper configMapper;

    /** ★合法奖励形式白名单 */
    private static final Set<String> VALID_REWARD_TYPES = Set.of("cash", "coupon", "points");
    /** ★合法触发条件白名单 */
    private static final Set<String> VALID_TRIGGER_CONDITIONS = Set.of("NEW_CUSTOMER_BIND", "NEW_CUSTOMER_FIRST_ORDER");
    /** ★合法奖励上限周期白名单 */
    private static final Set<String> VALID_LIMIT_TYPES = Set.of("NONE", "DAILY", "MONTHLY");

    /**
     * 获取拉新奖励配置（聚合 staff + customer 两条配置为统一 VO）
     * <p>★M20_03 去除权限校验：拉新奖励配置为老板公用功能（免费版+付费版均可见），菜单可见性控制访问。
     */
    @GetMapping
    public R<RewardConfigSettingVo> getSetting() {
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户ID不能为空");
        }
        // ★查询当前租户全部奖励配置
        List<PromotionRewardConfig> configs = configMapper.selectList(
            Wrappers.<PromotionRewardConfig>lambdaQuery()
                .eq(PromotionRewardConfig::getTenantId, tenantId));

        RewardConfigSettingVo vo = new RewardConfigSettingVo();
        // ★默认值
        vo.setEnabled(false);
        vo.setStaffConfig(new RoleRewardItem());
        vo.setCustomerConfig(new RoleRewardItem());

        boolean hasEnabled = false;
        for (PromotionRewardConfig cfg : configs) {
            if ("staff".equals(cfg.getTargetRole())) {
                vo.setStaffConfig(toItem(cfg));
                if ("0".equals(cfg.getStatus())) {
                    hasEnabled = true;
                }
            } else if ("customer".equals(cfg.getTargetRole())) {
                vo.setCustomerConfig(toItem(cfg));
                if ("0".equals(cfg.getStatus())) {
                    hasEnabled = true;
                }
            }
        }
        // ★全局开关：任一配置启用即为 true
        vo.setEnabled(hasEnabled);
        return R.ok(vo);
    }

    /**
     * 保存拉新奖励配置
     * <p>★M20_03 去除权限校验：拉新奖励配置为老板公用功能，菜单可见性控制访问。
     */
    @Log(title = "拉新奖励配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> saveSetting(@RequestBody RewardConfigSettingVo setting) {
        if (setting == null) {
            throw new ServiceException("参数不能为空");
        }
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户ID不能为空");
        }
        // ★全局关闭：两条配置均设为停用
        if (!Boolean.TRUE.equals(setting.getEnabled())) {
            disableAll(tenantId);
            log.info("[拉新奖励配置] tenantId={} 全局关闭", tenantId);
            return R.ok();
        }
        // ★保存员工奖励配置
        if (setting.getStaffConfig() != null) {
            saveRoleConfig(tenantId, "staff", setting.getStaffConfig());
        }
        // ★保存顾客奖励配置
        if (setting.getCustomerConfig() != null) {
            saveRoleConfig(tenantId, "customer", setting.getCustomerConfig());
        }
        log.info("[拉新奖励配置] tenantId={} 配置已保存", tenantId);
        return R.ok();
    }

    // ===== 私有方法 =====

    /**
     * 保存单个角色的奖励配置（upsert 模式）
     */
    private void saveRoleConfig(Long tenantId, String targetRole, RoleRewardItem item) {
        // ★值域校验
        validateItem(item);
        // ★查现有配置
        PromotionRewardConfig exist = configMapper.selectOne(
            Wrappers.<PromotionRewardConfig>lambdaQuery()
                .eq(PromotionRewardConfig::getTenantId, tenantId)
                .eq(PromotionRewardConfig::getTargetRole, targetRole)
                .last("LIMIT 1"));
        if (exist != null) {
            // ★更新
            exist.setRewardType(item.getRewardType());
            exist.setRewardValue(item.getRewardValue());
            exist.setCouponId("coupon".equals(item.getRewardType()) ? item.getCouponId() : null);
            exist.setTriggerCondition(item.getTriggerCondition());
            exist.setRewardLimitType(item.getRewardLimitType());
            exist.setRewardLimitValue("NONE".equals(item.getRewardLimitType()) ? null : item.getRewardLimitValue());
            exist.setStatus(Boolean.TRUE.equals(item.getEnabled()) ? "0" : "1");
            configMapper.updateById(exist);
        } else {
            // ★新增
            PromotionRewardConfig cfg = new PromotionRewardConfig();
            cfg.setTenantId(tenantId);
            cfg.setTargetRole(targetRole);
            cfg.setRewardType(item.getRewardType());
            cfg.setRewardValue(item.getRewardValue());
            cfg.setCouponId("coupon".equals(item.getRewardType()) ? item.getCouponId() : null);
            cfg.setTriggerRule("NEW_CUSTOMER");
            cfg.setTriggerCondition(item.getTriggerCondition());
            cfg.setRewardLimitType(item.getRewardLimitType());
            cfg.setRewardLimitValue("NONE".equals(item.getRewardLimitType()) ? null : item.getRewardLimitValue());
            cfg.setStatus(Boolean.TRUE.equals(item.getEnabled()) ? "0" : "1");
            configMapper.insert(cfg);
        }
    }

    /**
     * ★值域校验（fail-fast）
     */
    private void validateItem(RoleRewardItem item) {
        if (item == null) {
            return;
        }
        // 奖励形式
        if (StrUtil.isBlank(item.getRewardType()) || !VALID_REWARD_TYPES.contains(item.getRewardType())) {
            throw new ServiceException("奖励形式不合法，可选：cash/coupon/points");
        }
        // 现金/积分必须有金额
        if ("cash".equals(item.getRewardType()) || "points".equals(item.getRewardType())) {
            if (item.getRewardValue() == null || item.getRewardValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException("奖励金额必须大于0");
            }
        }
        // 优惠券必须关联 couponId
        if ("coupon".equals(item.getRewardType()) && item.getCouponId() == null) {
            throw new ServiceException("优惠券奖励必须关联优惠券ID");
        }
        // 触发条件
        if (StrUtil.isBlank(item.getTriggerCondition()) || !VALID_TRIGGER_CONDITIONS.contains(item.getTriggerCondition())) {
            throw new ServiceException("触发条件不合法，可选：NEW_CUSTOMER_BIND/NEW_CUSTOMER_FIRST_ORDER");
        }
        // 奖励上限
        String limitType = StrUtil.blankToDefault(item.getRewardLimitType(), "NONE");
        item.setRewardLimitType(limitType);
        if (!VALID_LIMIT_TYPES.contains(limitType)) {
            throw new ServiceException("奖励上限周期不合法，可选：NONE/DAILY/MONTHLY");
        }
        if (!"NONE".equals(limitType)) {
            if (item.getRewardLimitValue() == null || item.getRewardLimitValue() < 1) {
                throw new ServiceException("奖励上限次数必须>=1");
            }
        }
    }

    /**
     * 全局关闭：将当前租户所有配置设为停用
     */
    private void disableAll(Long tenantId) {
        List<PromotionRewardConfig> configs = configMapper.selectList(
            Wrappers.<PromotionRewardConfig>lambdaQuery()
                .eq(PromotionRewardConfig::getTenantId, tenantId));
        for (PromotionRewardConfig cfg : configs) {
            cfg.setStatus("1");
            configMapper.updateById(cfg);
        }
    }

    /**
     * Entity → VO Item 转换
     */
    private RoleRewardItem toItem(PromotionRewardConfig cfg) {
        RoleRewardItem item = new RoleRewardItem();
        item.setEnabled("0".equals(cfg.getStatus()));
        item.setRewardType(cfg.getRewardType());
        item.setRewardValue(cfg.getRewardValue());
        item.setCouponId(cfg.getCouponId());
        item.setTriggerCondition(StrUtil.blankToDefault(cfg.getTriggerCondition(), "NEW_CUSTOMER_FIRST_ORDER"));
        item.setRewardLimitType(StrUtil.blankToDefault(cfg.getRewardLimitType(), "NONE"));
        item.setRewardLimitValue(cfg.getRewardLimitValue());
        return item;
    }

    // ===== 内部 VO =====

    @Data
    public static class RewardConfigSettingVo implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 全局开关：是否启用拉新奖励 */
        private Boolean enabled;
        /** 员工奖励配置 */
        private RoleRewardItem staffConfig;
        /** 顾客奖励配置 */
        private RoleRewardItem customerConfig;
    }

    @Data
    public static class RoleRewardItem implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 是否启用该角色奖励 */
        private Boolean enabled;
        /** 奖励形式：cash(现金)/coupon(抵扣券)/points(积分) */
        private String rewardType;
        /** 奖励金额/积分数(rewardType=cash/points时) */
        private BigDecimal rewardValue;
        /** 关联优惠券ID(rewardType=coupon时) */
        private Long couponId;
        /** 触发条件：NEW_CUSTOMER_BIND(新客绑定)/NEW_CUSTOMER_FIRST_ORDER(新客首单) */
        private String triggerCondition;
        /** 奖励上限周期：NONE(不限)/DAILY(每日)/MONTHLY(每月) */
        private String rewardLimitType;
        /** 奖励上限次数 */
        private Integer rewardLimitValue;
    }
}
