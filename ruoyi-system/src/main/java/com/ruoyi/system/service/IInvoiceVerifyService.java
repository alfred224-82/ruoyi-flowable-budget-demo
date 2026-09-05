package com.ruoyi.system.service;

import com.ruoyi.system.domain.ExpenseBill;

/**
 * 发票验真服务（T5.18，设计文档 2.2 #6 / ADR-014）
 * <p>
 * 验真铁三角：发票识别（录入/OCR 字段）→ 查验验真 → 发票金额与报销金额比对。
 * 金额不一致或重复发票直接拦截标记；外部查验接口未接入时默认查验通过（预留扩展点）。
 *
 * @author ruoyi
 * @date 2026-08-30
 */
public interface IInvoiceVerifyService {

    /** 验真状态：未验真 */
    String STATUS_UNVERIFIED = "UNVERIFIED";
    /** 验真状态：验真通过 */
    String STATUS_VERIFIED = "VERIFIED";
    /** 验真状态：不一致/异常（拦截标记） */
    String STATUS_MISMATCH = "MISMATCH";

    /**
     * 报销提交时执行发票验真（写回 bill 的验真字段）
     * <ol>
     *   <li>未填发票号（如后补单）→ UNVERIFIED 放行；</li>
     *   <li>发票号查重：同一发票号已存在非驳回单据 → 拦截；</li>
     *   <li>调用查验接口验真（当前预留实现默认通过，替换 {@link InvoiceVerifier} 即可接入外部税局/第三方接口）；</li>
     *   <li>发票金额与报销金额比对，不一致 → 抛异常拦截并标记 MISMATCH。</li>
     * </ol>
     *
     * @param bill 报销单（已填充 invoiceNo/invoiceAmount/amount）
     */
    void verify(ExpenseBill bill);

    /**
     * 外部查验接口扩展点（发票号码 + 金额 + 开票日期 → 查验结果）
     */
    interface InvoiceVerifier {
        /**
         * @return true=查验通过
         */
        boolean check(String invoiceNo, java.math.BigDecimal invoiceAmount);
    }
}
