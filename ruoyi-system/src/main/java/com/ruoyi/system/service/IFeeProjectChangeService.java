package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.FeeProjectChangeBo;
import com.ruoyi.system.domain.vo.FeeProjectChangeVo;

/**
 * 合同变更Service接口（T5.5 / ADR-022）
 * 变更单→老板审批→合同额/预算增量叠加（基线不回溯）→待收款确认→财务闭环
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IFeeProjectChangeService {

    /**
     * 分页查询
     */
    TableDataInfo<FeeProjectChangeVo> queryPageList(FeeProjectChangeBo bo, PageQuery pageQuery);

    /**
     * 详情
     */
    FeeProjectChangeVo queryById(Long id);

    /**
     * 发起合同变更（状态=待审批）
     */
    Long insertChange(FeeProjectChangeBo bo);

    /**
     * 老板审批通过 → 合同额/预算增量叠加，转入待收款确认
     */
    int approveChange(Long id);

    /**
     * 老板驳回
     */
    int rejectChange(Long id, String reason);

    /**
     * 财务收款确认（APPROVED→CONFIRMED 闭环）
     */
    int confirmChange(Long id);

    /**
     * 扫描待收款确认变更单的逾期升级预警（定时任务入口）
     * 30/60/90 天阶梯：仅在逾期天数命中阶梯当天发送通知（防轰炸）；
     * ≥90 天状态转 OVERDUE（幂等，状态变更时通知一次）
     *
     * @return 命中预警的记录数
     */
    int scanOverdueWarnings();
}
