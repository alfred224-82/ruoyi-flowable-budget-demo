package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.vo.CustomerCreditVo;
import com.ruoyi.system.domain.vo.NoShowRecordVo;

import java.util.List;
import java.util.Set;

/**
 * 顾客信誉/防爽约服务接口（T15.31 建表 + T15.32 判定与黑名单，小微版，★仅 micro_pro 生效）
 * <p>防爽约闭环：noShowCheckJob 扫描 expire_time 已过仍「待服务」的预约 → 标记 NO_SHOW →
 * customer_credit.no_show_count 原子 +1（满 3 次自动拉黑）→ 写 no_show_record（marked_by=system）。
 * <p>黑名单双重限制：禁止新预约（{@link #isBlacklisted} 供预约提交校验）+ 活动推送过滤（is_blacklisted=0）。
 * 唯一解除出口 = 老板手动 {@link #unblacklist}（no_show_count 保留不清零，跨租户隔离）。
 *
 * @author ruoyi
 * @date 2026-09-08
 */
public interface ICustomerCreditService {

    /**
     * ★防爽约判定（T15.32 noShowCheckJob 调用）：扫描本租户 expire_time 已过且仍 PENDING_SERVICE 的预约，
     * 逐单标记 NO_SHOW + 累计爽约（满 3 次原子拉黑）+ 写爽约明细。
     * <p>租户上下文由 {@code @TenantJob} 切面设置，多租户插件自动隔离；★主动取消(CANCELLED)不算爽约。
     *
     * @return 本次判定为爽约的订单条数
     */
    int processNoShow();

    /**
     * 黑名单校验（T15.32）：判断顾客在本租户是否已被拉黑（禁止新预约 + 推送过滤依据）。
     *
     * @param userId 顾客用户ID
     * @return true=已拉黑
     */
    boolean isBlacklisted(Long userId);

    /**
     * ★老板手动移出黑名单（T15.32，micro:customer:unblacklist，唯一解除出口）：
     * is_blacklisted=0 + unblacklisted_at=now + operator_id=当前老板；★no_show_count 保留不清零（历史可追溯）。
     *
     * @param userId 顾客用户ID
     * @return 影响行数（0=无信誉记录或未拉黑）
     */
    int unblacklist(Long userId);

    /**
     * 顾客管理列表（T15.32，老板 PC micro:customer:list）：分页查询本租户顾客信誉，回填顾客昵称/手机号。
     *
     * @param userId        可选：按顾客用户ID精确过滤
     * @param isBlacklisted 可选：0正常/1已拉黑
     */
    TableDataInfo<CustomerCreditVo> queryPageList(Long userId, Integer isBlacklisted, PageQuery pageQuery);

    /**
     * 顾客爽约明细（T15.32，micro:customer:creditDetail）：本租户该顾客完整爽约历史（no_show_record）。
     *
     * @param userId 顾客用户ID
     */
    List<NoShowRecordVo> listNoShowRecords(Long userId);

    /**
     * ★活动推送黑名单过滤（T15.32 黑名单双重限制之二）：返回本租户已拉黑顾客用户ID集合，
     * 活动推送名单构建时据此排除（默认不推 is_blacklisted=1 的顾客）。租户上下文由调用方（切面/登录）保证。
     *
     * @return 已拉黑顾客用户ID集合
     */
    Set<Long> listBlacklistedUserIds();
}
