package com.ruoyi.system.service.strategy;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.system.domain.BizBundle;
import com.ruoyi.system.service.IBizBundleService;
import com.ruoyi.system.service.strategy.impl.FullHouseStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 组队/成团策略分发器（T11.4 策略模式）
 * <p>按 biz_bundle.engine_strategy 声明的 Bean 名从策略注册表取对应实现，
 * 全程不出现 if(bizCode==...) 分支：新增业态只需新增策略 Bean + 配置 engine_strategy。
 * engine_strategy 为空（如 fund_control 通用容器无成团语义）或 Bean 缺失时，回落默认策略（FullHouseStrategy，
 * 其在无桌数/名额配置时返回"不限人数、暂不成团"，为安全默认）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Component
public class TeamStrategyDispatcher {

    private final IBizBundleService bizBundleService;
    private final List<ITeamStrategy> strategies;

    /** 策略注册表：strategyName → 实现 */
    private final Map<String, ITeamStrategy> strategyMap = new HashMap<>();

    public TeamStrategyDispatcher(IBizBundleService bizBundleService, List<ITeamStrategy> strategies) {
        this.bizBundleService = bizBundleService;
        this.strategies = strategies;
    }

    @PostConstruct
    public void init() {
        for (ITeamStrategy strategy : strategies) {
            strategyMap.put(strategy.strategyName(), strategy);
        }
        log.info("[TeamStrategy] 已注册成团策略 {} 个：{}", strategyMap.size(), strategyMap.keySet());
    }

    /**
     * 按业态解析成团策略（依据 biz_bundle.engine_strategy Bean 名分发）
     *
     * @param bizCode 业态包编码
     * @return 策略实现，缺省回落 FullHouseStrategy
     */
    public ITeamStrategy resolve(String bizCode) {
        BizBundle bundle = bizBundleService.getBundle(bizCode);
        String beanName = bundle == null ? null : bundle.getEngineStrategy();
        if (StrUtil.isBlank(beanName)) {
            return defaultStrategy();
        }
        ITeamStrategy strategy = strategyMap.get(beanName);
        if (strategy == null) {
            log.warn("[TeamStrategy] 业态 {} 声明的策略 Bean {} 未注册，回落默认策略", bizCode, beanName);
            return defaultStrategy();
        }
        return strategy;
    }

    /**
     * 直接判定：解析业态策略并执行 evaluate
     */
    public TeamResult evaluate(String bizCode, TeamContext context) {
        return resolve(bizCode).evaluate(context);
    }

    private ITeamStrategy defaultStrategy() {
        ITeamStrategy def = strategyMap.get(FullHouseStrategy.BEAN_NAME);
        if (def == null) {
            throw new IllegalStateException("默认成团策略 FullHouseStrategy 未注册");
        }
        return def;
    }
}
