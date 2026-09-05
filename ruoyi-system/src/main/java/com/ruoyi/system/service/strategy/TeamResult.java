package com.ruoyi.system.service.strategy;

import java.io.Serializable;

/**
 * 组队/成团引擎判定结果（T11.4 策略模式出参）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public class TeamResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否已成团/满员 */
    private boolean formed;

    /** 当前进度（已报名人数） */
    private int current;

    /** 成团目标人数（-1 表示不限/无明确目标） */
    private int target;

    /** 判定说明（用于前端提示与留痕） */
    private String message;

    public TeamResult() {
    }

    public TeamResult(boolean formed, int current, int target, String message) {
        this.formed = formed;
        this.current = current;
        this.target = target;
        this.message = message;
    }

    public static TeamResult notFormed(int current, int target, String message) {
        return new TeamResult(false, current, target, message);
    }

    public static TeamResult formed(int current, int target, String message) {
        return new TeamResult(true, current, target, message);
    }

    public boolean isFormed() {
        return formed;
    }

    public void setFormed(boolean formed) {
        this.formed = formed;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public int getTarget() {
        return target;
    }

    public void setTarget(int target) {
        this.target = target;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
