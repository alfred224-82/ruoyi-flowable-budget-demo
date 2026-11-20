package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 拉新奖励配置业务对象 promotion_reward_config（T16.2/T16.7，小微版 M16）
 * <p>★trigger_rule 恒为 NEW_CUSTOMER 不可改；仅 reward_value/coupon_id/status 可变。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Data
public class PromotionRewardConfigBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 奖励对象：staff/customer（查询过滤用） */
    private String targetRole;

    /** 奖励形式：cash/coupon */
    private String rewardType;

    /** 奖励内容(唯一可变项) */
    private BigDecimal rewardValue;

    /** 顾客奖励关联的优惠券ID */
    private Long couponId;

    /** 状态：0启用 1停用 */
    private String status;

    private String remark;
}
