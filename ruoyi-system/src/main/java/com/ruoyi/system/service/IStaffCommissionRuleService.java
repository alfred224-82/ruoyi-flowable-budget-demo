package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.StaffCommissionRule;
import com.ruoyi.system.domain.bo.StaffCommissionRuleBo;
import com.ruoyi.system.domain.vo.ActivityPricingVo;
import com.ruoyi.system.domain.vo.EstimateVsActualVo;
import com.ruoyi.system.domain.vo.StaffCommissionRuleVo;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 员工分成规则服务接口（M35 T35.2~T35.7）
 * <p>三种类型：FIXED_AMOUNT / PERCENTAGE / TIERED（预留）。
 * <p>规则变更后历史活动仍用 {@link com.ruoyi.system.domain.ActivityStaffSnapshot} 快照，历史账不可变。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
public interface IStaffCommissionRuleService {

    /** 分页查询分成规则（含员工姓名/部门回填） */
    TableDataInfo<StaffCommissionRuleVo> queryPageList(StaffCommissionRuleBo bo, PageQuery pageQuery);

    /** 查询分成规则列表（不分页，导出用） */
    List<StaffCommissionRuleVo> queryList(StaffCommissionRuleBo bo);

    /** 分成规则详情 */
    StaffCommissionRuleVo queryById(Long id);

    /** 按员工ID查询启用中的分成规则（活动创建/快照生成用）；无配置或停用返回 null */
    StaffCommissionRule getActiveByStaffId(Long staffId);

    /** 批量按员工ID查询启用中的分成规则 */
    Map<Long, StaffCommissionRule> listActiveByStaffIds(Collection<Long> staffIds);

    /** 新增分成规则（每员工唯一，重复则拒绝） */
    Long insert(StaffCommissionRuleBo bo);

    /** 修改分成规则 */
    int update(StaffCommissionRuleBo bo);

    /** 逻辑删除分成规则（可批量） */
    int deleteByIds(Collection<Long> ids);

    /** 员工下拉选项（本租户 salesman/助教，复用 IStaffCommissionService.listStaffOptions 口径） */
    List<Map<String, Object>> listStaffOptions();

    /**
     * T35.4 定价页自动算账：按已选员工 + 售价 + 预计人数 + 其他成本试算预估利润与保本人数。
     *
     * @param staffIds          已选员工ID列表
     * @param unitPrice         单人售价
     * @param expectedHeadcount 预计人数
     * @param otherCost         其他成本（场地/物料等）
     * @return 试算结果（含每员工明细）
     */
    ActivityPricingVo calcPricing(List<Long> staffIds, BigDecimal unitPrice,
                                  Integer expectedHeadcount, BigDecimal otherCost);

    /**
     * T35.7 预估 vs 实际报表：按时间范围查询活动列表，返回预计利润 + 实际利润 + 差额。
     * <p>★M36 活动结算单未交付前 actualProfit 为 null，差额留空。
     *
     * @param beginTime 起始时间（含）
     * @param endTime   结束时间（含）
     * @param status    活动状态过滤（可空）
     */
    List<EstimateVsActualVo> queryEstimateVsActual(Date beginTime, Date endTime, String status);
}
