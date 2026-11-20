package com.ruoyi.system.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.system.domain.CustomerCredit;
import com.ruoyi.system.domain.vo.CustomerCreditVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 顾客信誉Mapper接口（T15.31 防爽约/黑名单）
 * <p>★爽约累计 + 满阈值自动拉黑走 DB 层原子 UPDATE（{@code no_show_count = no_show_count + 1}，
 * 行锁保证并发安全），避免「读-改-写」竞态导致重复计数或漏拉黑。
 *
 * @author ruoyi
 * @date 2026-09-08
 */
public interface CustomerCreditMapper extends BaseMapperPlus<CustomerCreditMapper, CustomerCredit, CustomerCreditVo> {

    /**
     * 原子累加爽约次数，达阈值时自动置黑名单（T15.32 noShowCheckJob 调用）。
     * <p>单条 UPDATE 由 DB 行锁保证原子性：no_show_count+1 后若 &ge; threshold 则 is_blacklisted=1 并记 blacklisted_at。
     *
     * @param tenantId  租户ID（跨租户隔离）
     * @param userId    顾客用户ID
     * @param threshold 自动拉黑阈值（默认 3）
     * @return 影响行数（0=信誉记录不存在，需先初始化）
     */
    @Update("UPDATE customer_credit SET no_show_count = no_show_count + 1, "
        + "is_blacklisted = CASE WHEN no_show_count + 1 >= #{threshold} THEN 1 ELSE is_blacklisted END, "
        + "blacklisted_at = CASE WHEN no_show_count + 1 >= #{threshold} AND is_blacklisted = 0 THEN NOW() ELSE blacklisted_at END, "
        + "update_time = NOW() "
        + "WHERE tenant_id = #{tenantId} AND user_id = #{userId}")
    int incrementNoShowAndAutoBlacklist(@Param("tenantId") Long tenantId,
                                        @Param("userId") Long userId,
                                        @Param("threshold") int threshold);
}
