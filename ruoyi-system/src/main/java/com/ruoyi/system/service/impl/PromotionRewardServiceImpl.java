package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantHelper;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.domain.bo.FirstOrderRecordBo;
import com.ruoyi.system.domain.bo.PromotionRewardConfigBo;
import com.ruoyi.system.domain.bo.PromotionRewardRecordBo;
import com.ruoyi.system.domain.vo.FirstOrderRecordVo;
import com.ruoyi.system.domain.vo.PromotionRewardConfigVo;
import com.ruoyi.system.domain.vo.PromotionRewardRecordVo;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.IPromotionRewardService;
import com.ruoyi.system.service.message.MessageContext;
import com.ruoyi.system.service.message.MessageProviderDispatcher;
import com.ruoyi.system.service.message.MessageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 推广奖励服务实现（T16.2/T16.7/T16.8/T16.9，小微版 M16）
 * <p>奖励配置管理 / 拉新奖励结算 / 首单识别 / 消费回传通知。
 * <p>★V1 零支付：员工现金奖励走「标记已线下发放」记账口径，不调任何支付接口。
 * <p>★T16.9 消费回传通知：结算后通过 MessageProviderDispatcher 发送微信订阅消息，
 *   授权不足时不发不报错（SKIPPED），不阻塞主流程。
 *
 * @author ruoyi
 * @date 2026-09-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionRewardServiceImpl implements IPromotionRewardService {

    private final PromotionRewardConfigMapper configMapper;
    private final PromotionRewardRecordMapper recordMapper;
    private final FirstOrderRecordMapper firstOrderMapper;
    private final PromotionAttributionMapper attrMapper;
    private final CouponMapper couponMapper;
    private final CouponRecordMapper couponRecordMapper;
    private final SysUserMapper userMapper;
    private final MessageProviderDispatcher messageDispatcher;

    // ===== T16.2 奖励配置 =====

    @Override
    public TableDataInfo<PromotionRewardConfigVo> queryConfigPage(PromotionRewardConfigBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PromotionRewardConfig> wrapper = Wrappers.<PromotionRewardConfig>lambdaQuery()
            .eq(StrUtil.isNotBlank(bo.getTargetRole()), PromotionRewardConfig::getTargetRole, bo.getTargetRole())
            .eq(StrUtil.isNotBlank(bo.getStatus()), PromotionRewardConfig::getStatus, bo.getStatus())
            .orderByAsc(PromotionRewardConfig::getId);
        Page<PromotionRewardConfigVo> page = configMapper.selectVoPage(pageQuery.build(), wrapper);
        // 回填优惠券名称
        for (PromotionRewardConfigVo vo : page.getRecords()) {
            if (vo.getCouponId() != null) {
                Coupon coupon = couponMapper.selectById(vo.getCouponId());
                if (coupon != null) {
                    vo.setCouponName(coupon.getName());
                }
            }
        }
        return TableDataInfo.build(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateConfig(PromotionRewardConfigBo bo) {
        // ★fail-fast：入口判空
        if (bo == null || bo.getId() == null) {
            throw new ServiceException("奖励配置ID不能为空");
        }
        PromotionRewardConfig exist = configMapper.selectById(bo.getId());
        if (exist == null) {
            throw new ServiceException("奖励配置不存在");
        }
        // ★trigger_rule 恒 NEW_CUSTOMER 不可改；仅 reward_value/coupon_id/status 可变
        LambdaUpdateWrapper<PromotionRewardConfig> wrapper = Wrappers.<PromotionRewardConfig>lambdaUpdate()
            .eq(PromotionRewardConfig::getId, bo.getId());
        if (bo.getRewardValue() != null) {
            wrapper.set(PromotionRewardConfig::getRewardValue, bo.getRewardValue());
        }
        if (bo.getCouponId() != null) {
            wrapper.set(PromotionRewardConfig::getCouponId, bo.getCouponId());
        }
        if (StrUtil.isNotBlank(bo.getStatus())) {
            wrapper.set(PromotionRewardConfig::getStatus, bo.getStatus());
        }
        if (StrUtil.isNotBlank(bo.getRemark())) {
            wrapper.set(PromotionRewardConfig::getRemark, bo.getRemark());
        }
        return configMapper.update(null, wrapper);
    }

    // ===== T16.7 拉新奖励结算 =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleNewCustomerReward(Long customerUserId, Long firstOrderId) {
        // ★fail-fast：入口判空
        if (customerUserId == null) {
            throw new ServiceException("顾客用户ID不能为空");
        }
        if (firstOrderId == null) {
            throw new ServiceException("首单ID不能为空");
        }
        // 1. 查 promotion_attribution 取当前推广人
        LambdaQueryWrapper<PromotionAttribution> attrWrapper = Wrappers.<PromotionAttribution>lambdaQuery()
            .eq(PromotionAttribution::getCustomerUserId, customerUserId)
            .eq(PromotionAttribution::getStatus, "0");
        PromotionAttribution attr = attrMapper.selectOne(attrWrapper);
        if (attr == null || attr.getPromoterUserId() == null) {
            log.info("[settleNewCustomerReward] 顾客 {} 无推广归因，跳过奖励结算", customerUserId);
            return;
        }
        Long promoterUserId = attr.getPromoterUserId();
        String promoterRoleCode = attr.getPromoterRoleCode();

        // 2. 幂等校验：同一顾客只触发一次
        Long existCount = recordMapper.selectCount(Wrappers.<PromotionRewardRecord>lambdaQuery()
            .eq(PromotionRewardRecord::getCustomerUserId, customerUserId));
        if (existCount > 0) {
            log.info("[settleNewCustomerReward] 顾客 {} 已触发过拉新奖励，跳过", customerUserId);
            return;
        }

        // 3. 按推广人角色查奖励配置
        String targetRole = "customer".equals(promoterRoleCode) ? "customer" : "staff";
        LambdaQueryWrapper<PromotionRewardConfig> configWrapper = Wrappers.<PromotionRewardConfig>lambdaQuery()
            .eq(PromotionRewardConfig::getTargetRole, targetRole)
            .eq(PromotionRewardConfig::getStatus, "0")
            .last("LIMIT 1");
        PromotionRewardConfig config = configMapper.selectOne(configWrapper);
        if (config == null) {
            log.info("[settleNewCustomerReward] 无启用的奖励配置(targetRole={})，跳过", targetRole);
            return;
        }

        // 4. 写入奖励发放记录
        PromotionRewardRecord record = new PromotionRewardRecord();
        record.setPromoterUserId(promoterUserId);
        record.setPromoterRoleCode(promoterRoleCode);
        record.setCustomerUserId(customerUserId);
        record.setFirstOrderId(firstOrderId);
        record.setRewardType(config.getRewardType());

        if ("cash".equals(config.getRewardType())) {
            // 员工现金奖励 → issue_status=PENDING（V1 零支付，等老板标记已线下发放）
            record.setRewardAmount(config.getRewardValue());
            record.setIssueStatus("PENDING");
        } else {
            // 顾客优惠券 → 写 coupon_record + record(ISSUED)
            Long couponRecordId = issueCouponToCustomer(promoterUserId, config.getCouponId());
            record.setCouponRecordId(couponRecordId);
            record.setIssueStatus("ISSUED");
            record.setIssuedTime(new Date());
        }
        // ★T16.9 消费回传通知：通过 MessageProviderDispatcher 发送微信订阅消息
        record.setNotified(sendRewardNotification(promoterUserId, promoterRoleCode, customerUserId, config.getRewardType()) ? 1 : 0);
        recordMapper.insert(record);

        log.info("[settleNewCustomerReward] 拉新奖励已结算：promoter={}, customer={}, rewardType={}, firstOrder={}",
            promoterUserId, customerUserId, config.getRewardType(), firstOrderId);
    }

    /**
     * ★T16.9 消费回传通知：走微信一次性订阅消息，统一走 IMessageProvider（按 access_channel 分发）
     * <p>授权不足时不发不报错（SKIPPED），不阻塞主流程。
     *
     * @return true=已发送/已跳过 false=发送失败
     */
    private boolean sendRewardNotification(Long promoterUserId, String promoterRoleCode, Long customerUserId, String rewardType) {
        try {
            SysUser promoter = userMapper.selectById(promoterUserId);
            if (promoter == null || StrUtil.isBlank(promoter.getPhonenumber())) {
                log.warn("[T16.9 notify] 推广人 {} 无手机号，跳过通知", promoterUserId);
                return false;
            }
            MessageContext ctx = new MessageContext();
            ctx.setChannel(MessageProviderDispatcher.CHANNEL_WECHAT);
            ctx.setBizType("promotion_reward");
            ctx.setReceiver(promoter.getPhonenumber());
            ctx.setTitle("拉新奖励到账通知");
            String rewardLabel = "cash".equals(rewardType) ? "现金奖励（请联系老板线下领取）" : "优惠券已到账";
            ctx.setContent("您成功邀请了一位新顾客，" + rewardLabel + "，感谢您的推广！");
            ctx.getData().put("rewardType", rewardLabel);
            ctx.getData().put("customerUserId", customerUserId);
            MessageResult result = messageDispatcher.send(ctx);
            if (result == null) {
                log.warn("[T16.9 notify] 消息分发器返回 null，promoter={}", promoterUserId);
                return false;
            }
            log.info("[T16.9 notify] promoter={} result={}", promoterUserId, result);
            return result.isSuccess();
        } catch (Exception e) {
            log.warn("[T16.9 notify] 通知异常 promoter={}", promoterUserId, e);
            return false;
        }
    }

    /**
     * 给顾客发放优惠券（拉新奖励）
     */
    private Long issueCouponToCustomer(Long customerUserId, Long couponId) {
        if (couponId == null) {
            return null;
        }
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            log.warn("[issueCouponToCustomer] 优惠券模板 {} 不存在", couponId);
            return null;
        }
        CouponRecord cr = new CouponRecord();
        cr.setUserId(customerUserId);
        cr.setCouponId(couponId);
        cr.setSource("promotion");
        cr.setStatus("UNUSED");
        cr.setReceiveTime(new Date());
        // 过期时间 = 领取时间 + valid_days
        if (coupon.getValidDays() != null && coupon.getValidDays() > 0) {
            cr.setExpireTime(new Date(System.currentTimeMillis() + (long) coupon.getValidDays() * 86400000L));
        }
        couponRecordMapper.insert(cr);
        // 更新已发放数量
        LambdaUpdateWrapper<Coupon> updateWrapper = Wrappers.<Coupon>lambdaUpdate()
            .eq(Coupon::getId, couponId)
            .setSql("issued_count = issued_count + 1");
        couponMapper.update(null, updateWrapper);
        return cr.getId();
    }

    @Override
    public TableDataInfo<PromotionRewardRecordVo> queryRewardRecordPage(PromotionRewardRecordBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PromotionRewardRecord> wrapper = Wrappers.<PromotionRewardRecord>lambdaQuery()
            .eq(bo.getPromoterUserId() != null, PromotionRewardRecord::getPromoterUserId, bo.getPromoterUserId())
            .eq(StrUtil.isNotBlank(bo.getPromoterRoleCode()), PromotionRewardRecord::getPromoterRoleCode, bo.getPromoterRoleCode())
            .eq(bo.getCustomerUserId() != null, PromotionRewardRecord::getCustomerUserId, bo.getCustomerUserId())
            .eq(StrUtil.isNotBlank(bo.getIssueStatus()), PromotionRewardRecord::getIssueStatus, bo.getIssueStatus())
            .eq(StrUtil.isNotBlank(bo.getRewardType()), PromotionRewardRecord::getRewardType, bo.getRewardType())
            .orderByDesc(PromotionRewardRecord::getCreateTime);
        Page<PromotionRewardRecordVo> page = recordMapper.selectVoPage(pageQuery.build(), wrapper);
        // 回填推广人昵称 + 顾客昵称
        for (PromotionRewardRecordVo vo : page.getRecords()) {
            if (vo.getPromoterUserId() != null) {
                SysUser promoter = userMapper.selectById(vo.getPromoterUserId());
                if (promoter != null) {
                    vo.setPromoterName(promoter.getNickName());
                }
            }
            if (vo.getCustomerUserId() != null) {
                SysUser customer = userMapper.selectById(vo.getCustomerUserId());
                if (customer != null) {
                    vo.setCustomerName(customer.getNickName());
                }
            }
        }
        return TableDataInfo.build(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markPaidOffline(Long id) {
        // ★fail-fast：入口判空
        if (id == null) {
            throw new ServiceException("奖励记录ID不能为空");
        }
        PromotionRewardRecord exist = recordMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("奖励记录不存在");
        }
        if (!"PENDING".equals(exist.getIssueStatus())) {
            throw new ServiceException("仅待发放状态可标记线下发放");
        }
        LambdaUpdateWrapper<PromotionRewardRecord> wrapper = Wrappers.<PromotionRewardRecord>lambdaUpdate()
            .eq(PromotionRewardRecord::getId, id)
            .set(PromotionRewardRecord::getIssueStatus, "PAID_OFFLINE")
            .set(PromotionRewardRecord::getIssuedTime, new Date());
        return recordMapper.update(null, wrapper);
    }

    // ===== T16.8 首单识别 =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean checkFirstOrder(Long activityId, String customerPhone, Long customerUserId, Long orderId) {
        // ★fail-fast：入口判空
        if (activityId == null) {
            throw new ServiceException("活动ID不能为空");
        }
        if (StrUtil.isBlank(customerPhone)) {
            return false;
        }
        // 按 activityId + customerPhone 查首单记录
        LambdaQueryWrapper<FirstOrderRecord> wrapper = Wrappers.<FirstOrderRecord>lambdaQuery()
            .eq(FirstOrderRecord::getActivityId, activityId)
            .eq(FirstOrderRecord::getCustomerPhone, customerPhone);
        FirstOrderRecord exist = firstOrderMapper.selectOne(wrapper);
        if (exist != null) {
            // 已有记录 → 老客
            return false;
        }
        // 无记录 → 首客 → 写入首单记录
        FirstOrderRecord record = new FirstOrderRecord();
        record.setActivityId(activityId);
        record.setCustomerPhone(customerPhone);
        record.setCustomerUserId(customerUserId);
        record.setFirstOrderId(orderId);
        record.setConsumeTime(new Date());
        firstOrderMapper.insert(record);
        log.info("[checkFirstOrder] 首单识别：activityId={}, phone={}, orderId={}", activityId, customerPhone, orderId);
        return true;
    }

    @Override
    public TableDataInfo<FirstOrderRecordVo> queryFirstOrderPage(FirstOrderRecordBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<FirstOrderRecord> wrapper = Wrappers.<FirstOrderRecord>lambdaQuery()
            .eq(bo.getActivityId() != null, FirstOrderRecord::getActivityId, bo.getActivityId())
            .like(StrUtil.isNotBlank(bo.getCustomerPhone()), FirstOrderRecord::getCustomerPhone, bo.getCustomerPhone())
            .orderByDesc(FirstOrderRecord::getConsumeTime);
        Page<FirstOrderRecordVo> page = firstOrderMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }
}
