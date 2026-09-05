package com.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.system.domain.ExpenseBill;
import com.ruoyi.system.domain.FeeClockRecord;
import com.ruoyi.system.domain.FeeClockTriggerRule;
import com.ruoyi.system.domain.bo.ExpenseBillBo;

import java.util.List;

/**
 * 打卡定位Service接口（T6.2 / FEAT_TIME_CLOCK）
 * 触发规则：差旅招待大类 / 金额≥500 / 历史异常≥2
 * 定位存疑（精度>50米）仅标记不拦截（ADR-014 威慑非定罪）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IFeeClockService {

    /**
     * 评估报销单命中的打卡触发规则
     *
     * @param bill   报销单（金额/小类已填充）
     * @param userId 报销人
     * @return 命中的规则编码列表（未命中返回空列表）
     */
    List<String> evaluateTriggers(ExpenseBill bill, Long userId);

    /**
     * 是否需要打卡（任一触发规则命中）
     */
    boolean isClockRequired(ExpenseBill bill, Long userId);

    /**
     * 落打卡记录（定位存疑仅标记，证据等级 medium）
     *
     * @param billId        报销单ID
     * @param userId        打卡用户
     * @param triggerRules  命中的触发规则（逗号分隔）
     * @param clockInfo     前端上报的定位与照片
     * @return 打卡记录
     */
    FeeClockRecord recordClock(Long billId, Long userId, String triggerRules, ExpenseBillBo.ClockInfo clockInfo);

    /**
     * 统计用户历史定位存疑次数（历史异常规则依据）
     */
    int countSuspicious(Long userId);

    /**
     * 打卡记录分页查询
     */
    Page<FeeClockRecord> pageRecords(Long billId, Long userId, PageQuery pageQuery);

    /**
     * 打卡记录详情
     */
    FeeClockRecord getRecord(Long id);

    /**
     * 查询报销单关联的打卡记录（追溯时间线用）
     */
    List<FeeClockRecord> listByBill(Long billId);

    /**
     * 触发规则配置列表（内置模板 + 租户级覆盖）
     */
    List<FeeClockTriggerRule> listTriggerRuleConfigs();

    /**
     * 新增租户级触发规则（不允许覆盖内置模板行）
     */
    boolean saveTriggerRule(FeeClockTriggerRule rule);

    /**
     * 修改触发规则（仅限本租户行）
     */
    boolean updateTriggerRule(FeeClockTriggerRule rule);

    /**
     * 删除触发规则（仅限本租户行，内置模板可通过 isEnabled=0 停用）
     */
    boolean deleteTriggerRule(Long id);
}
