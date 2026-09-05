package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.ExpenseBill;
import com.ruoyi.system.mapper.ExpenseBillMapper;
import com.ruoyi.system.service.IInvoiceVerifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 发票验真服务实现（T5.18）
 * <p>
 * 查重 + 外部查验（预留扩展点，默认通过）+ 金额比对拦截。
 *
 * @author ruoyi
 * @date 2026-08-30
 */
@Slf4j
@Service
public class InvoiceVerifyServiceImpl implements IInvoiceVerifyService {

    private final ExpenseBillMapper expenseBillMapper;
    /** 外部查验接口可选注入：未配置实现时默认查验通过 */
    private final ObjectProvider<InvoiceVerifier> verifierProvider;

    public InvoiceVerifyServiceImpl(ExpenseBillMapper expenseBillMapper,
                                    ObjectProvider<InvoiceVerifier> verifierProvider) {
        this.expenseBillMapper = expenseBillMapper;
        this.verifierProvider = verifierProvider;
    }

    @Override
    public void verify(ExpenseBill bill) {
        // 1. 未填发票号（后补通道等场景）→ 不验真放行
        if (StrUtil.isBlank(bill.getInvoiceNo())) {
            bill.setInvoiceVerifyStatus(STATUS_UNVERIFIED);
            return;
        }

        // 2. 发票查重：同一发票号重复报销拦截
        Long dupCount = expenseBillMapper.countByInvoiceNo(bill.getInvoiceNo(), bill.getId());
        if (dupCount != null && dupCount > 0) {
            bill.setInvoiceVerifyStatus(STATUS_MISMATCH);
            throw new ServiceException("发票[" + bill.getInvoiceNo() + "]已被其他报销单使用，疑似重复报销，提交被拦截");
        }

        // 3. 调用查验接口验真（未接入外部接口时默认通过，预留扩展点）
        InvoiceVerifier verifier = verifierProvider.getIfAvailable();
        boolean verified;
        if (verifier != null) {
            try {
                verified = verifier.check(bill.getInvoiceNo(), bill.getInvoiceAmount());
            } catch (Exception e) {
                log.warn("发票外部查验接口异常（降级为通过，不阻断主流程）: {}", e.getMessage());
                verified = true;
            }
        } else {
            verified = true;
        }
        if (!verified) {
            bill.setInvoiceVerifyStatus(STATUS_MISMATCH);
            throw new ServiceException("发票[" + bill.getInvoiceNo() + "]查验不通过，疑似虚假发票，提交被拦截");
        }

        // 4. 发票金额与报销金额比对，不一致拦截标记
        if (bill.getInvoiceAmount() != null && bill.getAmount() != null
            && bill.getInvoiceAmount().compareTo(bill.getAmount()) != 0) {
            bill.setInvoiceVerifyStatus(STATUS_MISMATCH);
            throw new ServiceException("发票金额(" + bill.getInvoiceAmount() + ")与报销金额("
                + bill.getAmount() + ")不一致，请核对后重新提交");
        }

        bill.setInvoiceVerifyStatus(STATUS_VERIFIED);
        log.info("发票验真通过: billNo={}, invoiceNo={}", bill.getBillNo(), bill.getInvoiceNo());
    }
}
