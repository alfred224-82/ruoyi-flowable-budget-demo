package com.ruoyi.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 报销单业务对象 fee_expense_bill
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExpenseBillBo extends BaseEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 单据编号（查询用）
     */
    private String billNo;

    /**
     * 物资小类编码
     */
    @NotBlank(message = "物资小类不能为空", groups = {AddGroup.class})
    private String itemCategory;

    /**
     * 用途标签编码
     */
    @NotBlank(message = "用途标签不能为空", groups = {AddGroup.class})
    private String purposeTag;

    /**
     * 项目编号（用途=项目交付时必填）
     */
    private String projectCode;

    /**
     * 关联资金管控项目ID（T5.7 项目关联规则；0=公司级，未选默认公司级）
     */
    private Long projectId;

    /**
     * 是否发票后补(0否 1是，T5.8 无发票后补通道)
     */
    private Integer invoiceDefer;

    /**
     * 发票号码（T5.18 发票验真；后补单不填）
     */
    private String invoiceNo;

    /**
     * 发票金额（T5.18 与报销金额比对，不一致拦截）
     */
    private BigDecimal invoiceAmount;

    /**
     * 报销金额（人民币）
     */
    @NotNull(message = "报销金额不能为空", groups = {AddGroup.class})
    @DecimalMin(value = "0.01", message = "报销金额必须大于0", groups = {AddGroup.class})
    private BigDecimal amount;

    /**
     * 币种编码（CNY/USD/EUR/GBP/JPY）
     */
    private String currencyCode;

    /**
     * 原币金额（外币时填写）
     */
    private BigDecimal originalAmount;

    /**
     * 出差天数（用于补助核算）
     */
    private Integer travelDays;

    /**
     * 费用发生日期
     */
    @NotNull(message = "费用发生日期不能为空", groups = {AddGroup.class})
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date expenseDate;

    /**
     * 报销年月（格式：YYYY-MM，对应预算编制期间）
     */
    @NotBlank(message = "报销年月不能为空", groups = {AddGroup.class})
    private String expensePeriod;

    /**
     * 使用年限(年)，仅固定资产时填写
     */
    private Integer usefulLife;

    /**
     * 费用确认方式(DIRECT/DEPRECIATION/ACCRUAL/FEE_RATE)
     */
    private String costMethod;

    /**
     * 状态(DRAFT/SUBMITTED/APPROVED/REJECTED/OVER_BUDGET_PENDING)
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 证据等级筛选（T6.7 / FEAT_EVIDENCE_CHAIN，strong/medium/weak）
     */
    private String evidenceLevel;

    /**
     * 打卡信息（T6.2 / FEAT_TIME_CLOCK：触发规则命中时提交必带）
     */
    private ClockInfo clockInfo;

    /**
     * 打卡信息内嵌对象（报销提交时一并上报定位与照片）
     */
    @Data
    public static class ClockInfo {
        /** 经度 */
        private BigDecimal longitude;
        /** 纬度 */
        private BigDecimal latitude;
        /** 定位地址 */
        private String address;
        /** 定位精度(米) */
        private BigDecimal accuracy;
        /** 现场照片 */
        private String photoUrls;
    }
}
