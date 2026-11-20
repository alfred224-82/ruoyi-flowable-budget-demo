package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.CashFlowBo;
import com.ruoyi.system.domain.vo.CashFlowVo;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 收支流水服务接口（T15.5/T15.14，纯记账口径，非支付流水）
 * <p>功能：① 收入/支出流水分页查询；② 手动补录收入/支出；③ 系统自动记账（供预约/结算服务调用）。
 * <p>★V1 零支付合规：cash_flow 仅记账，不涉及任何支付/转账接口调用。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
public interface ICashFlowService {

    /**
     * 分页查询收支流水（按 flowType/category/relatedType + occurDate 范围过滤，occur_date 倒序）
     */
    TableDataInfo<CashFlowVo> queryPageList(CashFlowBo bo, PageQuery pageQuery);

    /**
     * 查询收支流水列表（不分页，供收支明细导出用）
     */
    List<CashFlowVo> queryList(CashFlowBo bo);

    /**
     * 手动补录收支（老板 PC：工资/房租/水电/采购...）
     *
     * @return 流水ID
     */
    Long insert(CashFlowBo bo);

    /**
     * 逻辑删除收支流水（可批量）
     */
    int deleteByIds(Collection<Long> ids);

    /**
     * 系统自动记账（内部调用）：写一条 cash_flow。
     * <p>tenant_id 由登录上下文自动填充；create_by 取当前登录人；occurDate 为空则取当天。
     *
     * @param flowType       income/expense
     * @param amount         金额
     * @param category       收支分类
     * @param relatedOrderId 关联单据ID
     * @param relatedType    关联单据类型（booking/settlement/manual）
     * @param occurDate      发生日期
     * @param remark         备注
     * @return 流水ID
     */
    Long record(String flowType, BigDecimal amount, String category, Long relatedOrderId,
                String relatedType, Date occurDate, String remark);
}
