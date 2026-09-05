package com.ruoyi.system.service.strategy;

/**
 * 组队/成团引擎策略接口（T11.4 策略模式）
 * <p>不同业态的成团规则由独立策略 Bean 实现，通过 biz_bundle.engine_strategy 声明的 Bean 名分发，
 * 引擎与业务源码严禁出现 if(bizCode==...) 分支（设计文档 §1.8.5 防串层）。
 * <p>新增业态成团规则 = 新增一个实现类 + 在 biz_bundle 配置 engine_strategy，不改核心代码。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface ITeamStrategy {

    /**
     * 策略 Bean 名（须与 biz_bundle.engine_strategy 一致，如 FullHouseStrategy/GroupBuyStrategy）
     */
    String strategyName();

    /**
     * 判定当前报名聚合是否成团/满员
     *
     * @param context 组队上下文（报名人数 + 业态差异字段）
     * @return 判定结果
     */
    TeamResult evaluate(TeamContext context);
}
