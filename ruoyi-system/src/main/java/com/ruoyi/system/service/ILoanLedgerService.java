package com.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.system.domain.FeeLoanLedger;

/**
 * 借款台账Service接口
 * 设计文档 Phase 7 Task 7.2
 *
 * @author ruoyi
 * @date 2026-07-23
 */
public interface ILoanLedgerService {

    /**
     * 新增借款（AI录入或手工）
     */
    Boolean addLoan(FeeLoanLedger loan);

    /**
     * 月末利息计提
     *
     * @param period 计提期间（YYYY-MM）
     * @return 计提笔数
     */
    int executeMonthlyInterest(String period);

    /**
     * 结清借款
     */
    Boolean settle(Long id);

    /**
     * 逾期检测 + 预警
     */
    void checkOverdue();

    /**
     * 借款列表
     */
    Page<FeeLoanLedger> listLoans(String status, PageQuery pageQuery);
}
