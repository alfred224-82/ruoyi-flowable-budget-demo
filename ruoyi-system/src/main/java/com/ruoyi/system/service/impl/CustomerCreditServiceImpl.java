package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.helper.TenantHelper;
import com.ruoyi.system.domain.BookingOrder;
import com.ruoyi.system.domain.CustomerCredit;
import com.ruoyi.system.domain.NoShowRecord;
import com.ruoyi.system.domain.enums.BookingOrderStatus;
import com.ruoyi.system.domain.vo.CustomerCreditVo;
import com.ruoyi.system.domain.vo.NoShowRecordVo;
import com.ruoyi.system.mapper.BookingOrderMapper;
import com.ruoyi.system.mapper.CustomerCreditMapper;
import com.ruoyi.system.mapper.NoShowRecordMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.ICustomerCreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 顾客信誉/防爽约服务实现（T15.31 建表 + T15.32 判定与黑名单，小微版，★仅 micro_pro 生效）
 * <p>customer_credit / no_show_record 均为租户级表，多租户插件自动隔离——A 店黑名单不影响该用户在 B 店预约。
 * <p>★防爽约判定：扫描 expire_time 已过仍 PENDING_SERVICE 的预约 → 标记 NO_SHOW →
 * customer_credit.no_show_count 走 DB 层原子 UPDATE +1（满 3 次自动拉黑，防并发重复计数）→ 写 no_show_record(system)。
 * ★主动取消(CANCELLED)不算爽约；游客预约（无 customerUserId）仅关单不计信誉。
 * <p>★黑名单唯一解除出口 = 老板手动 {@link #unblacklist}（走 @Version 乐观锁），no_show_count 保留不清零。
 *
 * @author ruoyi
 * @date 2026-09-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerCreditServiceImpl implements ICustomerCreditService {

    /** 自动拉黑阈值：累计爽约满 3 次 */
    private static final int BLACKLIST_THRESHOLD = 3;
    /** 爽约标记来源：定时任务自动 */
    private static final String MARKED_BY_SYSTEM = "system";
    /** 黑名单标志：1 已拉黑 */
    private static final int BLACKLISTED = 1;
    /** 黑名单标志：0 正常 */
    private static final int NORMAL = 0;

    private final CustomerCreditMapper creditMapper;
    private final NoShowRecordMapper noShowRecordMapper;
    private final BookingOrderMapper bookingMapper;
    private final SysUserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int processNoShow() {
        Long tenantId = TenantHelper.getTenantId();
        Date now = new Date();
        // 扫描本租户 expire_time 已过且仍「待服务」的预约（租户隔离由插件自动完成；主动取消 CANCELLED 不在扫描范围）
        List<BookingOrder> overdue = bookingMapper.selectList(Wrappers.<BookingOrder>lambdaQuery()
            .eq(BookingOrder::getOrderStatus, BookingOrderStatus.PENDING_SERVICE.getCode())
            .isNotNull(BookingOrder::getExpireTime)
            .lt(BookingOrder::getExpireTime, now));
        if (CollUtil.isEmpty(overdue)) {
            return 0;
        }
        int count = 0;
        for (BookingOrder order : overdue) {
            // 1. 订单标记爽约（NO_SHOW）
            BookingOrder update = new BookingOrder();
            update.setId(order.getId());
            update.setOrderStatus(BookingOrderStatus.NO_SHOW.getCode());
            update.setUpdateTime(now);
            update.setUpdateBy("noShowCheckJob");
            bookingMapper.updateById(update);
            count++;
            // 游客预约（无 customerUserId）仅关单，无法按用户累计信誉
            Long userId = order.getCustomerUserId();
            if (userId == null) {
                continue;
            }
            // 2. 确保信誉记录存在（uk_tenant_user 幂等）
            ensureCredit(userId);
            // 3. 原子累加爽约次数 + 满阈值自动拉黑（DB 行锁保证并发安全）
            creditMapper.incrementNoShowAndAutoBlacklist(tenantId, userId, BLACKLIST_THRESHOLD);
            // 4. 写爽约明细（可追溯，marked_by=system）
            NoShowRecord record = new NoShowRecord();
            record.setUserId(userId);
            record.setOrderNo(order.getOrderNo());
            record.setBookTime(order.getBookingTime());
            record.setMarkedAt(now);
            record.setMarkedBy(MARKED_BY_SYSTEM);
            noShowRecordMapper.insert(record);
        }
        return count;
    }

    @Override
    public boolean isBlacklisted(Long userId) {
        if (userId == null) {
            return false;
        }
        CustomerCredit credit = creditMapper.selectOne(Wrappers.<CustomerCredit>lambdaQuery()
            .eq(CustomerCredit::getUserId, userId));
        return credit != null && Integer.valueOf(BLACKLISTED).equals(credit.getIsBlacklisted());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int unblacklist(Long userId) {
        if (userId == null) {
            throw new ServiceException("顾客用户ID不能为空");
        }
        CustomerCredit credit = creditMapper.selectOne(Wrappers.<CustomerCredit>lambdaQuery()
            .eq(CustomerCredit::getUserId, userId));
        if (credit == null) {
            throw new ServiceException("顾客信誉记录不存在");
        }
        // 幂等：未拉黑无需移出
        if (!Integer.valueOf(BLACKLISTED).equals(credit.getIsBlacklisted())) {
            return 0;
        }
        CustomerCredit update = new CustomerCredit();
        update.setId(credit.getId());
        update.setIsBlacklisted(NORMAL);
        update.setUnblacklistedAt(new Date());
        update.setOperatorId(LoginHelper.getUserId());
        // ★乐观锁：携带当前版本号，防并发覆盖；no_show_count 不设置=保留不清零（历史可追溯）
        update.setVersion(credit.getVersion());
        return creditMapper.updateById(update);
    }

    @Override
    public TableDataInfo<CustomerCreditVo> queryPageList(Long userId, Integer isBlacklisted, PageQuery pageQuery) {
        LambdaQueryWrapper<CustomerCredit> wrapper = Wrappers.<CustomerCredit>lambdaQuery()
            .eq(userId != null, CustomerCredit::getUserId, userId)
            .eq(isBlacklisted != null, CustomerCredit::getIsBlacklisted, isBlacklisted)
            .orderByDesc(CustomerCredit::getIsBlacklisted)
            .orderByDesc(CustomerCredit::getNoShowCount)
            .orderByDesc(CustomerCredit::getId);
        Page<CustomerCreditVo> page = creditMapper.selectVoPage(pageQuery.build(), wrapper);
        fillCustomerInfo(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public List<NoShowRecordVo> listNoShowRecords(Long userId) {
        return noShowRecordMapper.selectVoList(Wrappers.<NoShowRecord>lambdaQuery()
            .eq(userId != null, NoShowRecord::getUserId, userId)
            .orderByDesc(NoShowRecord::getMarkedAt)
            .orderByDesc(NoShowRecord::getId));
    }

    @Override
    public Set<Long> listBlacklistedUserIds() {
        // 本租户已拉黑顾客（租户隔离由插件自动完成），推送名单构建时据此排除
        List<CustomerCredit> blacklisted = creditMapper.selectList(Wrappers.<CustomerCredit>lambdaQuery()
            .eq(CustomerCredit::getIsBlacklisted, BLACKLISTED));
        return blacklisted.stream().map(CustomerCredit::getUserId)
            .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
    }

    /**
     * 确保顾客信誉记录存在（不存在则初始化 no_show_count=0/is_blacklisted=0）；租户上下文由插件与 insertFill 处理
     */
    private void ensureCredit(Long userId) {
        Long exists = creditMapper.selectCount(Wrappers.<CustomerCredit>lambdaQuery()
            .eq(CustomerCredit::getUserId, userId));
        if (exists != null && exists > 0) {
            return;
        }
        CustomerCredit credit = new CustomerCredit();
        credit.setUserId(userId);
        credit.setNoShowCount(0);
        credit.setIsBlacklisted(NORMAL);
        credit.setVersion(0);
        creditMapper.insert(credit);
    }

    /**
     * 批量回填顾客昵称/手机号（sys_user.nick_name / phonenumber）
     */
    private void fillCustomerInfo(List<CustomerCreditVo> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<Long> userIds = list.stream().map(CustomerCreditVo::getUserId)
            .filter(java.util.Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        List<SysUser> users = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
            .in(SysUser::getUserId, userIds));
        Map<Long, SysUser> userMap = users.stream()
            .collect(Collectors.toMap(SysUser::getUserId, u -> u, (a, b) -> a));
        list.forEach(vo -> {
            SysUser u = userMap.get(vo.getUserId());
            if (u != null) {
                vo.setCustomerName(u.getNickName());
                vo.setCustomerPhone(u.getPhonenumber());
            }
        });
    }
}
