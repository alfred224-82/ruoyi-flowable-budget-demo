package com.ruoyi.system.service.strategy.impl;

import com.ruoyi.system.service.strategy.ITeamStrategy;
import com.ruoyi.system.service.strategy.TeamContext;
import com.ruoyi.system.service.strategy.TeamResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 团购达量成团策略（T11.4，生鲜团购业态 fresh_food）
 * <p>规则：累计下单量达到成团阈值即成团，支持阶梯价（达量越高价格越优，阶梯明细由前端按 extra.ladder 渲染）。
 * 成团阈值优先取 extra.targetCount，缺失回落 quota；均无则视为不限（不成团）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Component("GroupBuyStrategy")
public class GroupBuyStrategy implements ITeamStrategy {

    public static final String BEAN_NAME = "GroupBuyStrategy";

    @Override
    public String strategyName() {
        return BEAN_NAME;
    }

    @Override
    public TeamResult evaluate(TeamContext context) {
        int current = context.getTotalPeople();
        int target = context.getInt("targetCount", 0);
        if (target <= 0 && context.getQuota() != null && context.getQuota() > 0) {
            target = context.getQuota();
        }
        if (target <= 0) {
            return TeamResult.notFormed(current, -1, "未设置成团阈值，团购不限数量，暂不判定成团");
        }
        if (current >= target) {
            // 阶梯价：达量越高越优，此处仅提示已达最高已知阶梯（具体价格档位由前端/结算侧解析 extra.ladder）
            return TeamResult.formed(current, target, "团购已达量成团（" + current + "/" + target + "），可享阶梯优惠价");
        }
        return TeamResult.notFormed(current, target, "团购进行中（" + current + "/" + target + "），距成团还差 " + (target - current) + " 份");
    }
}
