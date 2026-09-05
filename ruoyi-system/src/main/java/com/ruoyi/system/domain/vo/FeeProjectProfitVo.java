package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 项目利润拆解视图对象（T5.1 / T5.10，设计文档 27.2）
 * 项目利润 = 已核验收入 - 支出 - 折旧分摊 - 项目分成
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeProjectProfitVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 项目ID */
    private Long projectId;
    /** 项目名称 */
    private String projectName;
    /** 项目状态（利润报表列表用，T6.5） */
    private String projectStatus;
    /** 项目经理姓名（利润报表列表用，T6.5） */
    private String projectManagerName;
    /** 收入确认进度 = 已核验/合同额（0~1，进度条展示，T6.5） */
    private BigDecimal incomeProgress;
    /** 当前合同额 */
    private BigDecimal currentContractAmount;
    /** 已核验收入 */
    private BigDecimal verifiedIncome;
    /** 待核验收入(不计利润) */
    private BigDecimal pendingIncome;
    /** 总支出(报销+付款) */
    private BigDecimal totalExpense;
    /** 折旧分摊 */
    private BigDecimal depreciationShare;
    /** 项目分成 */
    private BigDecimal sharingAmount;
    /** 坏账计提 */
    private BigDecimal badDebtProvision;
    /** 纸面利润 = 合同额 - (已核验+待核验收入冲减) 含未到账 */
    private BigDecimal paperProfit;
    /** 实际利润 = 已核验收入 - 支出 - 折旧分摊 - 分成 - 坏账计提 */
    private BigDecimal actualProfit;
    /** 利润率(实际利润/已核验收入) */
    private BigDecimal profitRate;
}
