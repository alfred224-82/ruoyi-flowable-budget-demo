package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 三张报表对账校验视图对象（M39 T39.5）
 * <p>对账铁律：日报月累计毛利 = 活动明细同月毛利合计 = 月报月毛利（三者同源于 settlement.profit）。
 * <p>每条规则返回 {name,leftValue,rightValue,passed}；overallPassed=全部通过。
 *
 * @author ruoyi
 * @date 2026-10-20
 */
@Data
public class ReconcileVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 月份（yyyy-MM） */
    private String month;

    /** 校验规则结果列表 */
    private List<Rule> rules;

    /** 是否全部通过 */
    private Boolean overallPassed;

    /**
     * 单条对账规则结果
     */
    @Data
    public static class Rule implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 规则名称 */
        private String name;

        /** 左值 */
        private BigDecimal leftValue;

        /** 右值 */
        private BigDecimal rightValue;

        /** 是否通过（左右值一致） */
        private Boolean passed;

        public Rule() {
        }

        public Rule(String name, BigDecimal leftValue, BigDecimal rightValue) {
            this.name = name;
            this.leftValue = leftValue;
            this.rightValue = rightValue;
            BigDecimal l = leftValue != null ? leftValue : BigDecimal.ZERO;
            BigDecimal r = rightValue != null ? rightValue : BigDecimal.ZERO;
            this.passed = l.compareTo(r) == 0;
        }
    }
}
