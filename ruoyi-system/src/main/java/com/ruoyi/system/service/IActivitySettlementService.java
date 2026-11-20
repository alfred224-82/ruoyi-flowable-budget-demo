package com.ruoyi.system.service;

import com.ruoyi.system.domain.bo.ActivitySettlementBo;
import com.ruoyi.system.domain.vo.ActivitySettlementVo;

import java.util.List;

/**
 * 活动结算单服务接口（M36 T36.1~T36.6）
 * <p>核心能力：
 * <ul>
 *   <li>T36.1 结算单 CRUD + 收入项/成本项明细管理</li>
 *   <li>T36.2 按行业模板自动带出常用收入项</li>
 *   <li>T36.3 老板端结算单页面数据聚合</li>
 *   <li>T36.4 员工端只读视图（权限收窄：仅可见自己那条员工成本行）</li>
 *   <li>T36.5 状态机 DRAFT→SUBMITTED→LOCKED + 金额校验 + 代收校验</li>
 *   <li>T36.6 收工时追加消费提醒</li>
 * </ul>
 *
 * @author ruoyi
 * @date 2026-10-19
 */
public interface IActivitySettlementService {

    /**
     * 按活动ID查询结算单（不存在则自动初始化草稿）
     *
     * @param activityId 活动ID
     * @return 结算单聚合视图（含收入项/成本项列表 + 活动信息冗余）
     */
    ActivitySettlementVo getByActivityId(Long activityId);

    /**
     * 按结算单ID查询
     */
    ActivitySettlementVo getById(Long id);

    /**
     * 保存草稿（T36.5 状态机：仅 DRAFT 可编辑）
     *
     * @param bo 结算单入参（含收入项/其他成本项列表）
     * @return 结算单ID
     */
    Long saveDraft(ActivitySettlementBo bo);

    /**
     * 提交结算（T36.5 状态机：DRAFT→SUBMITTED）
     * <p>校验：① 金额不得为负 ② 收入合计为 0 禁止提交 ③ 代收存在但未配代付成本 → 弹提示不阻断
     *
     * @param id 结算单ID
     */
    void submit(Long id);

    /**
     * 锁定结算（T36.5 状态机：SUBMITTED→LOCKED）
     * <p>锁定后不可编辑，错误走调整单。
     *
     * @param id 结算单ID
     */
    void lock(Long id);

    /**
     * 追加调整单（T36.5：锁定后发现错误 → 新建调整单，不覆盖原报表）
     *
     * @param originId 原结算单ID
     * @param bo       调整单入参
     * @return 调整单ID
     */
    Long createAdjustment(Long originId, ActivitySettlementBo bo);

    /**
     * 员工端只读视图（T36.4：权限收窄，仅可见自己那条员工成本行）
     *
     * @param activityId 活动ID
     * @param staffUserId 当前登录员工用户ID
     * @return 结算单视图（成本项仅含自己的员工成本行；合计利润区不可见由前端控制）
     */
    ActivitySettlementVo getStaffReadonlyView(Long activityId, Long staffUserId);

    /**
     * 按行业模板初始化收入项（T36.2：创建活动时按行业自动带出常用条目）
     *
     * @param industryCode 行业编码
     * @return 收入项模板列表（前端渲染用）
     */
    List<ActivitySettlementBo.SettlementIncomeItemBo> initIncomeItemsByIndustry(String industryCode);

    /**
     * 重新计算员工成本（T36.1：从 activity_staff_snapshot 快照自动生成）
     * <p>活动选人变更 / 员工缺席删除后调用，即时重算。
     *
     * @param settlementId 结算单ID
     */
    void recalculateEmployeeCost(Long settlementId);

    /**
     * 导出结算单 CSV（T36.3 底部操作）
     *
     * @param id 结算单ID
     * @return CSV 字符串
     */
    String exportCsv(Long id);

    /**
     * ★T36.6 收工时追加消费提醒判断
     * <p>活动结算时若追加消费（ADDON 类型）为空 → 返回 true，前端弹提示
     * 「有饮料零食等店内消费吗？」→ 不强制填写。
     *
     * @param settlementId 结算单ID
     * @return true=需要提醒
     */
    boolean needAddonReminder(Long settlementId);
}
