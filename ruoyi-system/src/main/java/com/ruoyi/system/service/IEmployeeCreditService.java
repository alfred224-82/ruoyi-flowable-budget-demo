package com.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.system.domain.FeeCreditRule;
import com.ruoyi.system.domain.FeeEmployeeCredit;

import java.util.List;

/**
 * 员工信用体系Service接口（T6.3 / FEAT_CREDIT_SCORE）
 * 信用分规则：按时补票+2 / 逾期-5 / 连续3次按时+10（租户级可覆盖内置模板）
 * 信用等级：EXCELLENT优秀≥80（免老板审批+补票期限14天）/ NORMAL正常50-79 / RESTRICTED受限&lt;50（禁用无发票报销）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IEmployeeCreditService {

    /**
     * 获取员工信用（不存在则初始化，默认100分）
     */
    FeeEmployeeCredit getOrInit(Long userId);

    /**
     * 信用分→等级换算（≥80优秀 / 50-79正常 / <50受限）
     */
    String calcLevel(int creditScore);

    /**
     * 是否允许无发票后补（受限等级禁止；开关关闭时恒允许）
     */
    boolean allowInvoiceDefer(Long userId);

    /**
     * 补票期限天数（优秀14天 / 其余7天；开关关闭时固定7天）
     */
    int getSupplementDays(Long userId);

    /**
     * 是否免老板审批（优秀等级；开关关闭时返回 false 走标准流程）
     */
    boolean skipBossApproval(Long userId);

    /**
     * 补票按时事件（+2，连续3次按时追加+10奖励）
     */
    void recordOnTime(Long userId);

    /**
     * 补票逾期事件（-5，清零连续按时次数）
     */
    void recordOverdue(Long userId);

    /**
     * 财务手工调整信用分
     *
     * @param userId 用户
     * @param delta  加减分
     * @param reason 调整原因
     */
    boolean adjustScore(Long userId, int delta, String reason);

    /**
     * 等级重算（creditScoreCalcJob 每周一）：跨租户重算等级，基础版租户跳过
     *
     * @return 重算记录数
     */
    int recalcLevels();

    /**
     * 员工信用分分页查询
     */
    Page<FeeEmployeeCredit> pageCredits(Long userId, String creditLevel, PageQuery pageQuery);

    /**
     * 信用规则配置列表（内置模板 + 租户级覆盖）
     */
    List<FeeCreditRule> listRules();

    /**
     * 新增租户级信用规则
     */
    boolean saveRule(FeeCreditRule rule);

    /**
     * 修改信用规则（仅限本租户行）
     */
    boolean updateRule(FeeCreditRule rule);

    /**
     * 删除信用规则（仅限本租户行）
     */
    boolean deleteRule(Long id);
}
