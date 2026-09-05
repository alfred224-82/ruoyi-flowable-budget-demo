package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.vo.FeeProfitTimelineVo;
import com.ruoyi.system.domain.vo.FeeProjectProfitVo;

import java.util.List;

/**
 * 项目利润报表Service接口（T6.5 / FEAT_PROFIT_REPORT）
 * 纸面/实际分离、利润拆解、收支时间线、进度条、按项目/月导出
 * 数据范围：老板/财务/管理员不限，项目经理仅本人参与项目
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IFeeProfitReportService {

    /**
     * 利润报表分页列表（每项目一行：纸面/实际利润+收入确认进度）
     *
     * @param projectName 项目名称（模糊，可空）
     * @param status      项目状态（可空）
     * @param pageQuery   分页参数
     */
    TableDataInfo<FeeProjectProfitVo> reportList(String projectName, String status, PageQuery pageQuery);

    /**
     * 单项目利润详情（利润拆解，含数据范围校验）
     */
    FeeProjectProfitVo reportDetail(Long projectId);

    /**
     * 项目收支时间线（合同→收入确认→付款→收款全链路）
     */
    List<FeeProfitTimelineVo> timeline(Long projectId);

    /**
     * 导出利润报表（按项目/按月）
     *
     * @param projectId 可空=数据范围内全部项目
     * @param month     可空；非空时附加当月收支明细 Sheet（yyyy-MM）
     */
    byte[] exportReport(Long projectId, String month);
}
