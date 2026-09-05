package com.ruoyi.system.service.strategy;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 组队/成团引擎上下文（T11.4 策略模式入参）
 * <p>由报名服务采集当前容器的报名聚合数据 + 解析 extra_config 业态差异字段后构造，
 * 交由具体 ITeamStrategy 判定，引擎源码不感知业态（禁止 if(bizCode==...)）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public class TeamContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 项目容器ID */
    private Long projectId;

    /** 业态包编码（仅用于日志/留痕，策略判定不得依赖此字段做分支） */
    private String bizCode;

    /** 当前累计报名人数（含本次） */
    private int totalPeople;

    /** 名额上限（NULL=不限，取自 project_signup.quota 或容器配置） */
    private Integer quota;

    /** 业态差异字段（解析自 project.extra_config，如桌游:tableCount/perTable；生鲜:targetCount/ladder） */
    private Map<String, Object> extra = new HashMap<>();

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getBizCode() {
        return bizCode;
    }

    public void setBizCode(String bizCode) {
        this.bizCode = bizCode;
    }

    public int getTotalPeople() {
        return totalPeople;
    }

    public void setTotalPeople(int totalPeople) {
        this.totalPeople = totalPeople;
    }

    public Integer getQuota() {
        return quota;
    }

    public void setQuota(Integer quota) {
        this.quota = quota;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    public void setExtra(Map<String, Object> extra) {
        this.extra = extra == null ? new HashMap<>() : extra;
    }

    /**
     * 读取整型业态参数，缺失或非法回落默认值
     */
    public int getInt(String key, int defaultValue) {
        Object v = extra.get(key);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
