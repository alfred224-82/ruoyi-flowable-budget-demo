package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.FeeLoan;
import com.ruoyi.system.domain.vo.FeeLoanInterestVo;
import com.ruoyi.system.domain.vo.FeeLoanVo;

/**
 * 贷款Service接口（T5.9 / ADR-027）
 * 财务设参数，老板只读；每月利息计提
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IFeeLoanService {

    /**
     * 贷款分页查询
     */
    TableDataInfo<FeeLoanVo> queryPageList(String status, PageQuery pageQuery);

    /**
     * 新增/修改贷款
     */
    Long saveLoan(FeeLoan loan);

    /**
     * 结清贷款（停止计息）
     */
    int settleLoan(Long id);

    /**
     * 利息计提记录分页查询
     */
    TableDataInfo<FeeLoanInterestVo> queryInterestPageList(String accrualMonth, PageQuery pageQuery);

    /**
     * 月度利息计提（幂等：同月同贷款仅一条），月息 = 贷款余额 * 年利率 / 12
     *
     * @param accrualMonth 计提月份 yyyy-MM，空=上一自然月
     * @return 计提条数
     */
    int accrueInterests(String accrualMonth);
}
