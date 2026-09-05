package com.ruoyi.system.service.strategy.impl;

import com.ruoyi.system.service.strategy.ITeamStrategy;
import com.ruoyi.system.service.strategy.TeamContext;
import com.ruoyi.system.service.strategy.TeamResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 拼桌满员成团策略（T11.4，桌游轰趴业态 boardgame）
 * <p>规则：满员即成团、超时解散（超时由定时任务处理，本策略只做满员判定）。
 * 目标人数优先取 extra.tableCount×extra.perTable（桌数×每桌人数），缺失回落 quota；均无则视为不限（不成团）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Component("FullHouseStrategy")
public class FullHouseStrategy implements ITeamStrategy {

    public static final String BEAN_NAME = "FullHouseStrategy";

    @Override
    public String strategyName() {
        return BEAN_NAME;
    }

    @Override
    public TeamResult evaluate(TeamContext context) {
        int current = context.getTotalPeople();
        int tableCount = context.getInt("tableCount", 0);
        int perTable = context.getInt("perTable", 0);
        int target;
        if (tableCount > 0 && perTable > 0) {
            target = tableCount * perTable;
        } else if (context.getQuota() != null && context.getQuota() > 0) {
            target = context.getQuota();
        } else {
            target = -1;
        }
        if (target <= 0) {
            return TeamResult.notFormed(current, -1, "未设置桌数/名额，拼桌不限人数，暂不判定成团");
        }
        if (current >= target) {
            return TeamResult.formed(current, target, "拼桌已满员（" + current + "/" + target + "），成团成功");
        }
        return TeamResult.notFormed(current, target, "拼桌进行中（" + current + "/" + target + "），未满员");
    }
}
