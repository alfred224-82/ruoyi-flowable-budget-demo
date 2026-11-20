package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 顾客信誉对象 customer_credit（T15.31，小微版防爽约/黑名单，仅 micro_pro 生效）
 * <p>按 tenant_id + user_id 聚合（uk_tenant_user），记录累计爽约次数与黑名单状态；
 * 租户级表（含 tenant_id），多租户插件自动隔离——A 店黑名单不影响该用户在 B 店预约。
 * <p>★满 3 次自动拉黑为原子操作（{@code @Version} 乐观锁防并发重复计数）；
 * 老板手动移出黑名单为唯一解除出口，移出后 no_show_count 保留不清零（历史可追溯）。
 *
 * @author ruoyi
 * @date 2026-09-08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_credit")
public class CustomerCredit extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 顾客用户ID
     */
    private Long userId;

    /**
     * 累计爽约次数（保留历史，移出黑名单不清零）
     */
    private Integer noShowCount;

    /**
     * 是否拉黑：0正常 1已拉黑（满3次自动置1）
     */
    private Integer isBlacklisted;

    /**
     * 最近一次拉黑时间
     */
    private Date blacklistedAt;

    /**
     * 最近一次被老板移出黑名单时间
     */
    private Date unblacklistedAt;

    /**
     * 移出操作的老板用户ID
     */
    private Long operatorId;

    /**
     * 乐观锁版本号（防并发重复计数，T15.32 原子拉黑）
     */
    @Version
    private Integer version;

    /**
     * 备注
     */
    private String remark;
}
