package com.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.system.domain.ExpenseBill;
import com.ruoyi.system.domain.bo.ExpenseBillBo;
import com.ruoyi.system.domain.vo.ExpenseBillVo;

import com.ruoyi.system.domain.vo.ExpenseBillImportVo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 报销单Service接口（V5.4增强版）
 * 集成：多币种、发票合规校验、三轨预算管控、补助核算、盈利提示
 *
 * @author ruoyi
 * @date 2026-07-23
 */
public interface IExpenseBillService {

    /**
     * 查询报销单详情
     *
     * @param id 主键ID
     * @return 报销单VO
     */
    ExpenseBillVo queryById(Long id);

    /**
     * 分页查询报销单列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    Page<ExpenseBillVo> selectPageList(ExpenseBillBo bo, PageQuery pageQuery);

    /**
     * 新增报销单（创建即提交，无草稿状态）
     * V5.4增强：集成多币种转换、合规校验、三轨管控、补助核算
     *
     * @param bo 业务对象
     * @return 报销单实体
     */
    ExpenseBill insertByBo(ExpenseBillBo bo);

    /**
     * 试算补助金额
     *
     * @param subsidyType 补助类型（TRAVEL/MEAL/TRANSPORT）
     * @param days        天数
     * @return 补助核算结果
     */
    ISubsidyService.SubsidyCalcResult calculateSubsidy(String subsidyType, int days);

    /**
     * 获取盈利提示
     *
     * @param budgetSubjectCode 预算科目编码
     * @param amount            金额
     * @return 盈利提示结果
     */
    IProfitTipService.ProfitTipResult getProfitTip(String budgetSubjectCode, BigDecimal amount);

    /**
     * 获取汇率
     *
     * @param currencyCode 币种编码
     * @param period       汇率期间（YYYY-MM）
     * @return 汇率
     */
    BigDecimal getExchangeRate(String currencyCode, String period);

    /**
     * 批量导入报销单
     *
     * @param list 导入数据列表
     * @return 导入结果描述
     */
    String importBills(List<ExpenseBillImportVo> list);
}
