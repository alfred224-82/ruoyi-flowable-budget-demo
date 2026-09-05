package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.service.FeatureChecker;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.BudgetSubject;
import com.ruoyi.system.domain.ExpenseBill;
import com.ruoyi.system.domain.FeeComplianceRecord;
import com.ruoyi.system.domain.OverBudgetApproval;
import com.ruoyi.system.domain.bo.ExpenseBillBo;
import com.ruoyi.system.domain.vo.BudgetCheckResultVo;
import com.ruoyi.system.domain.vo.ExpenseBillImportVo;
import com.ruoyi.system.domain.vo.ExpenseBillVo;
import com.ruoyi.system.domain.vo.MappingRuleVo;
import com.ruoyi.system.mapper.BudgetSubjectMapper;
import com.ruoyi.system.mapper.ExpenseBillMapper;
import com.ruoyi.system.mapper.OverBudgetApprovalMapper;
import com.ruoyi.system.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 报销单Service业务层处理（V5.4增强版）
 * 集成：多币种转换、发票合规校验、三轨预算管控、补助核算、盈利提示
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseBillServiceImpl implements IExpenseBillService {

    private final ExpenseBillMapper expenseBillMapper;
    private final IMappingRuleService mappingRuleService;
    private final IBudgetCheckService budgetCheckService;
    private final OverBudgetApprovalMapper overBudgetApprovalMapper;
    private final IFeeOperationLogService feeOperationLogService;
    // V5.4 新增服务
    private final IExchangeRateService exchangeRateService;
    private final IComplianceService complianceService;
    private final ITrackControlService trackControlService;
    private final ISubsidyService subsidyService;
    private final IProfitTipService profitTipService;
    // M6 高级版增量服务（T6.2 打卡定位 / T6.3 信用体系 / T6.7 关联规则）
    private final IFeeClockService feeClockService;
    private final IEmployeeCreditService employeeCreditService;
    private final IFeeProjectLinkRuleService linkRuleService;
    private final FeatureChecker featureChecker;
    // M5 预算域增强（T5.17 大类管控 / T5.18 发票验真）
    private final ICategoryBudgetService categoryBudgetService;
    private final IInvoiceVerifyService invoiceVerifyService;
    private final BudgetSubjectMapper budgetSubjectMapper;

    @Override
    public ExpenseBillVo queryById(Long id) {
        return expenseBillMapper.selectVoById(id);
    }

    @Override
    public Page<ExpenseBillVo> selectPageList(ExpenseBillBo bo, PageQuery pageQuery) {
        Wrapper<ExpenseBill> wrapper = buildQueryWrapper(bo);
        return expenseBillMapper.selectExpenseBillPage(pageQuery.build(), wrapper);
    }

    /**
     * 新增报销单（创建即提交，无草稿状态）
     * V5.4增强流程：
     * 1. 多币种转换（外币→人民币）
     * 2. 映射规则匹配
     * 3.5 发票验真（查重+查验+金额比对，T5.18）
     * 3.6 大类额度硬拦截（超额瞬间阻断，T5.17）
     * 4. 三轨预算管控校验
     * 4. 发票合规校验
     * 5. 补助核算（如有出差天数）
     * 6. 预算余额校验（含超预算分流）
     * 7. 盈利提示（前端展示用）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseBill insertByBo(ExpenseBillBo bo) {
        ExpenseBill bill = BeanCopyUtils.copy(bo, ExpenseBill.class);
        // 生成报销单号
        bill.setBillNo(generateBillNo());
        // 设置当前用户部门
        bill.setDeptId(LoginHelper.getDeptId());
        bill.setApplicantId(LoginHelper.getUserId());
        bill.setApplicantName(LoginHelper.getNickName());
        bill.setAllocationStatus("NONE");
        bill.setIsOverBudget(0);
        // T5.7 项目关联兜底：未选项目默认公司级（project_id=0）
        boolean projectLinked = bill.getProjectId() != null && bill.getProjectId() > 0;
        if (bill.getProjectId() == null) {
            bill.setProjectId(0L);
        }
        // T6.7 关联规则硬校验：角色硬规则（基础版）+ 大类强制/项目严格模式（FEAT_LINK_RULE_CUSTOM）
        checkLinkRule(bill, projectLinked);
        // T5.8 发票后补标记兜底 + T6.3 信用体系（受限禁用后补，单据记信用等级）
        if (bill.getInvoiceDefer() == null) {
            bill.setInvoiceDefer(0);
        }
        if (bill.getInvoiceDefer() == 1 && !employeeCreditService.allowInvoiceDefer(bill.getApplicantId())) {
            throw new ServiceException("您的信用等级为受限，禁止无发票后补报销，请上传发票后提交");
        }
        if (featureChecker.isFeatureEnabled("FEAT_CREDIT_SCORE")) {
            bill.setCreditLevel(employeeCreditService.getOrInit(bill.getApplicantId()).getCreditLevel());
        }

        // 兜底：如果前端未传报销年月，从费用日期自动推导（格式 YYYY-MM）
        if (StrUtil.isBlank(bill.getExpensePeriod()) && bill.getExpenseDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
            bill.setExpensePeriod(sdf.format(bill.getExpenseDate()));
        }

        // ========== V5.4 增强：多币种转换 ==========
        String currencyCode = StrUtil.isBlank(bill.getCurrencyCode()) ? "CNY" : bill.getCurrencyCode();
        bill.setCurrencyCode(currencyCode);
        if (!"CNY".equalsIgnoreCase(currencyCode)) {
            // 外币报销：需要原币金额和汇率
            if (bill.getOriginalAmount() == null) {
                throw new ServiceException("外币报销必须填写原币金额");
            }
            BigDecimal rate = exchangeRateService.getRate(currencyCode, bill.getExpensePeriod());
            if (rate == null) {
                throw new ServiceException("未找到币种[" + currencyCode + "]在期间[" + bill.getExpensePeriod() + "]的汇率配置");
            }
            bill.setExchangeRate(rate);
            // 人民币金额 = 原币金额 × 汇率
            bill.setAmount(exchangeRateService.convertToCny(currencyCode, bill.getOriginalAmount(), bill.getExpensePeriod()));
        }

        // ========== T6.2 打卡定位校验（FEAT_TIME_CLOCK，命中触发规则必带定位） ==========
        List<String> clockHits = checkClockRequirement(bill, bo);

        // ========== 1. 映射规则匹配 ==========
        MappingRuleVo rule = mappingRuleService.matchRule(
            bill.getItemCategory(), bill.getPurposeTag(), bill.getDeptId());
        // T6.4 匹配链路追溯：记录命中规则快照（FEAT_RULE_ENGINE_CUSTOM 关闭时同样保留，便于审计）
        try {
            bill.setMatchTrace(cn.hutool.json.JSONUtil.toJsonStr(rule));
        } catch (Exception e) {
            log.warn("匹配链路快照序列化失败（不阻断主流程）: {}", e.getMessage());
        }

        // 2. 填充系统计算字段
        bill.setCalcBudgetSubject(rule.getBudgetSubject());
        bill.setCalcAccountingClass(rule.getAccountingClass());
        // ========== T5.17/T5.18：映射B 定位费用大类 + 记录匹配阶段 ==========
        bill.setMatchStage("EXACT");
        if (StrUtil.isNotBlank(bill.getCalcBudgetSubject())) {
            BudgetSubject subject = budgetSubjectMapper.selectOne(Wrappers.<BudgetSubject>lambdaQuery()
                .eq(BudgetSubject::getSubjectCode, bill.getCalcBudgetSubject())
                .last("LIMIT 1"));
            if (subject != null) {
                bill.setCategoryCode(subject.getCategoryCode());
            }
        }
        bill.setIsFixedAsset(rule.getIsFixedAsset());
        bill.setExpenditureType(rule.getIsFixedAsset() != null && rule.getIsFixedAsset() == 1
            ? "CAPITAL" : "OPERATING");
        // 费用确认方式（已经版本降级处理）
        String costMethod = StrUtil.isNotBlank(rule.getCostMethod()) ? rule.getCostMethod() : "DIRECT";
        bill.setCostMethod(costMethod);

        // 2.1 固定资产/折旧校验：使用年限必填
        if ("DEPRECIATION".equals(costMethod) || (bill.getIsFixedAsset() != null && bill.getIsFixedAsset() == 1)) {
            if (bill.getUsefulLife() == null || bill.getUsefulLife() <= 0) {
                throw new ServiceException("该报销单属于固定资产（折旧类），必须填写使用年限（年）");
            }
        }
        // 2.2 计提类校验：也需要填写摊销期数（复用usefulLife字段）
        if ("ACCRUAL".equals(costMethod)) {
            if (bill.getUsefulLife() == null || bill.getUsefulLife() <= 0) {
                throw new ServiceException("该报销单属于计提摊销类，必须填写摊销期数（月）");
            }
        }

        // ========== V5.4 增强：三轨预算管控校验 ==========
        String industryCode = "*"; // TODO: 从用户上下文获取行业编码
        ITrackControlService.TrackCheckResult trackResult =
            trackControlService.checkTrack(bill.getCalcBudgetSubject(), bill.getAmount(), industryCode);
        if (trackResult.isBlocked()) {
            throw new ServiceException(trackResult.getMessage());
        }
        // 软预警记录日志，不阻断
        if ("warning".equals(trackResult.getResult())) {
            log.warn("报销单三轨管控预警: {}", trackResult.getMessage());
        }

        // ========== V5.4 增强：补助核算 ==========
        if (bill.getTravelDays() != null && bill.getTravelDays() > 0) {
            try {
                ISubsidyService.SubsidyCalcResult subsidyResult =
                    subsidyService.calculate("TRAVEL", bill.getTravelDays(), industryCode);
                bill.setSubsidyAmount(subsidyResult.getAmount());
                log.info("补助核算: 类型=TRAVEL, 天数={}, 金额={}", bill.getTravelDays(), subsidyResult.getAmount());
            } catch (Exception e) {
                log.warn("补助核算失败（不阻断主流程）: {}", e.getMessage());
            }
        }

        // ========== T5.18 发票验真：查重 → 查验 → 发票金额与报销金额比对，不一致拦截标记 ==========
        invoiceVerifyService.verify(bill);

        // ========== 3. 预算校验（含超预算分流） ==========
        int budgetYear;
        int budgetMonth;
        if (StrUtil.isNotBlank(bill.getExpensePeriod()) && bill.getExpensePeriod().length() >= 7) {
            String[] parts = bill.getExpensePeriod().split("-");
            budgetYear = Integer.parseInt(parts[0]);
            budgetMonth = Integer.parseInt(parts[1]);
        } else {
            LocalDate now = LocalDate.now();
            budgetYear = now.getYear();
            budgetMonth = now.getMonthValue();
        }
        // ========== T5.17 大类额度硬拦截：提交瞬间同步校验，超额直接阻断 ==========
        ICategoryBudgetService.CategoryCheckResult categoryResult = categoryBudgetService
            .checkCategoryBudget(bill.getDeptId(), bill.getCategoryCode(), bill.getAmount(), budgetYear, budgetMonth);
        if (categoryResult.getWarningLevel() != null) {
            log.warn("报销单大类预算预警: {}", categoryResult.getMessage());
        }

        BudgetCheckResultVo checkResult = budgetCheckService.checkBudget(
            bill.getDeptId(), bill.getCalcBudgetSubject(), bill.getAmount(),
            budgetYear, budgetMonth);

        if (!checkResult.getPassed()) {
            if (checkResult.getOverBudget() != null && checkResult.getOverBudget()) {
                // 超预算：创建超预算审批单，单据状态变为 OVER_BUDGET_PENDING
                bill.setStatus("OVER_BUDGET_PENDING");
                expenseBillMapper.insert(bill);
                recordClockIfRequired(bill, clockHits, bo);

                OverBudgetApproval approval = new OverBudgetApproval();
                approval.setBillId(bill.getId());
                approval.setBillNo(bill.getBillNo());
                approval.setBudgetSubject(bill.getCalcBudgetSubject());
                approval.setOriginalBudget(checkResult.getTotalBudget());
                approval.setUsedAmount(checkResult.getUsedAmount());
                approval.setRequestAmount(bill.getAmount());
                approval.setShortageAmount(checkResult.getShortageAmount());
                approval.setStatus("PENDING");
                overBudgetApprovalMapper.insert(approval);

                bill.setIsOverBudget(1);
                bill.setOverBudgetApprovalId(approval.getId());

                // V5.4: 合规校验（超预算单据也需要）
                performComplianceCheck(bill, industryCode);

                expenseBillMapper.updateById(bill);

                feeOperationLogService.log("EXPENSE", "SUBMIT", bill.getId(), bill.getBillNo(),
                    null, "NEW", "OVER_BUDGET_PENDING", null);
                return bill;
            } else {
                // 无预算或其他原因 → 阻断
                throw new ServiceException(checkResult.getMessage());
            }
        }

        // ========== V5.4 增强：发票合规校验 ==========
        performComplianceCheck(bill, industryCode);

        // 4. 正常提交
        bill.setStatus("SUBMITTED");
        expenseBillMapper.insert(bill);
        recordClockIfRequired(bill, clockHits, bo);

        // 写操作日志
        feeOperationLogService.log("EXPENSE", "SUBMIT", bill.getId(), bill.getBillNo(),
            null, "NEW", "SUBMITTED", null);

        return bill;
    }

    /**
     * T6.2 打卡定位：开关开启且命中触发规则时，提交必须携带定位信息（返回命中规则）
     */
    private List<String> checkClockRequirement(ExpenseBill bill, ExpenseBillBo bo) {
        if (!featureChecker.isFeatureEnabled("FEAT_TIME_CLOCK")) {
            return Collections.emptyList();
        }
        List<String> hits = feeClockService.evaluateTriggers(bill, bill.getApplicantId());
        if (hits.isEmpty()) {
            return Collections.emptyList();
        }
        ExpenseBillBo.ClockInfo clockInfo = bo.getClockInfo();
        if (clockInfo == null || clockInfo.getLongitude() == null || clockInfo.getLatitude() == null) {
            throw new ServiceException("该报销单命中打卡触发规则[" + String.join(",", hits) + "]，请携带定位信息提交");
        }
        // 打卡单据证据等级统一记为 medium（定位存疑仅标记不拦截）
        bill.setEvidenceLevel("medium");
        return hits;
    }

    /**
     * T6.2 报销单落库后落打卡记录（入追溯时间线）
     */
    private void recordClockIfRequired(ExpenseBill bill, List<String> clockHits, ExpenseBillBo bo) {
        if (clockHits == null || clockHits.isEmpty()) {
            return;
        }
        feeClockService.recordClock(bill.getId(), bill.getApplicantId(),
            String.join(",", clockHits), bo.getClockInfo());
    }

    /**
     * 执行发票合规校验
     */
    private void performComplianceCheck(ExpenseBill bill, String industryCode) {
        try {
            FeeComplianceRecord record = complianceService.checkCompliance(
                bill.getId(), bill.getBillNo(), bill.getCalcBudgetSubject(), industryCode);
            bill.setComplianceStatus(record.getCheckResult());
            if ("FAIL".equals(record.getCheckResult())) {
                throw new ServiceException("发票合规校验不通过：" + record.getFailReason());
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("合规校验异常（不阻断主流程）: {}", e.getMessage());
            bill.setComplianceStatus("PASS"); // 异常时默认通过
        }
    }

    @Override
    public ISubsidyService.SubsidyCalcResult calculateSubsidy(String subsidyType, int days) {
        String industryCode = "*"; // TODO: 从用户上下文获取行业编码
        return subsidyService.calculate(subsidyType, days, industryCode);
    }

    @Override
    public IProfitTipService.ProfitTipResult getProfitTip(String budgetSubjectCode, BigDecimal amount) {
        String industryCode = "*"; // TODO: 从用户上下文获取行业编码
        return profitTipService.getTip(budgetSubjectCode, industryCode, amount);
    }

    @Override
    public BigDecimal getExchangeRate(String currencyCode, String period) {
        return exchangeRateService.getRate(currencyCode, period);
    }

    /**
     * 构建查询条件（仅查询当前登录用户自己编制的报销单）
     */
    private Wrapper<ExpenseBill> buildQueryWrapper(ExpenseBillBo bo) {
        // T6.7 证据链分级：开关开启时支持按证据等级筛选异常单据（strong/medium/weak）
        boolean evidenceFilter = featureChecker.isFeatureEnabled("FEAT_EVIDENCE_CHAIN")
            && StrUtil.isNotBlank(bo.getEvidenceLevel());
        return Wrappers.<ExpenseBill>lambdaQuery()
            .eq(ExpenseBill::getApplicantId, LoginHelper.getUserId())
            .like(StrUtil.isNotBlank(bo.getBillNo()), ExpenseBill::getBillNo, bo.getBillNo())
            .like(StrUtil.isNotBlank(bo.getItemCategory()), ExpenseBill::getItemCategory, bo.getItemCategory())
            .eq(StrUtil.isNotBlank(bo.getPurposeTag()), ExpenseBill::getPurposeTag, bo.getPurposeTag())
            .eq(StrUtil.isNotBlank(bo.getStatus()), ExpenseBill::getStatus, bo.getStatus())
            .eq(StrUtil.isNotBlank(bo.getCurrencyCode()), ExpenseBill::getCurrencyCode, bo.getCurrencyCode())
            .eq(evidenceFilter, ExpenseBill::getEvidenceLevel, bo.getEvidenceLevel());
    }

    /**
     * T6.7 项目关联规则硬校验：
     * 1. 角色级硬规则（基础版内置：项目经理必选项目，未关联 400 拦截）
     * 2. 开关开启后：大类强制关联（某大类所有角色必选项目）+ 项目严格模式（关联严格项目后不可再回退公司级）
     */
    private void checkLinkRule(ExpenseBill bill, boolean projectLinked) {
        java.util.Set<String> roleKeys = LoginHelper.getLoginUser().getRolePermission();
        com.ruoyi.system.domain.vo.FeeProjectLinkRuleVo roleRule = linkRuleService.matchRule(roleKeys, null);
        if (!projectLinked && roleRule != null && "REQUIRED".equals(roleRule.getRule())) {
            throw new ServiceException("您的角色要求报销必须关联项目，请选择项目后提交");
        }
        if (!featureChecker.isFeatureEnabled("FEAT_LINK_RULE_CUSTOM")) {
            return;
        }
        // 大类强制关联：开关开启后大类级 REQUIRED 对所有角色生效（小类前缀匹配大类编码）
        if (!projectLinked) {
            com.ruoyi.system.domain.vo.FeeProjectLinkRuleVo categoryRule =
                linkRuleService.matchCategoryRule(bill.getItemCategory());
            if (categoryRule != null && "REQUIRED".equals(categoryRule.getRule())) {
                throw new ServiceException("该费用大类要求所有角色必须关联项目，请选择项目后提交");
            }
        }
        // 项目严格模式：已关联项目若开启严格模式，不允许同时勾选后补以外的降级行为（仅记录证据等级供审批参考）
        if (projectLinked) {
            com.ruoyi.system.domain.vo.FeeProjectLinkRuleVo projectRule =
                linkRuleService.queryProjectRule(bill.getProjectId());
            if (projectRule != null && "REQUIRED".equals(projectRule.getRule())
                && StrUtil.isBlank(bill.getEvidenceLevel())) {
                bill.setEvidenceLevel("weak");
            }
        }
    }

    /**
     * 生成报销单号
     */
    private String generateBillNo() {
        return "EB-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    @Override
    public String importBills(List<ExpenseBillImportVo> list) {
        if (list == null || list.isEmpty()) {
            throw new ServiceException("导入数据不能为空");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();

        for (int i = 0; i < list.size(); i++) {
            ExpenseBillImportVo vo = list.get(i);
            int rowNum = i + 2; // Excel行号（第1行为表头）
            try {
                ExpenseBillBo bo = new ExpenseBillBo();
                bo.setItemCategory(vo.getItemCategory());
                bo.setPurposeTag(vo.getPurposeTag());
                bo.setProjectCode(vo.getProjectCode());
                bo.setAmount(vo.getAmount());
                bo.setCurrencyCode(vo.getCurrencyCode());
                bo.setOriginalAmount(vo.getOriginalAmount());
                bo.setTravelDays(vo.getTravelDays());
                bo.setExpenseDate(vo.getExpenseDate());
                bo.setExpensePeriod(vo.getExpensePeriod());
                bo.setUsefulLife(vo.getUsefulLife());
                bo.setRemark(vo.getRemark());

                insertByBo(bo);
                successNum++;
                successMsg.append("<br/>").append(rowNum).append("、导入成功");
            } catch (Exception e) {
                failureNum++;
                failureMsg.append("<br/>").append(rowNum).append("、导入失败：").append(e.getMessage());
            }
        }

        if (failureNum > 0) {
            failureMsg.insert(0, "很抱歉，部分数据导入失败！共 " + failureNum + " 条数据格式不正确：");
            if (successNum > 0) {
                failureMsg.append("<br/>成功导入 ").append(successNum).append(" 条数据");
            }
            return failureMsg.toString();
        }
        return "恭喜您，全部导入成功！共 " + successNum + " 条";
    }
}
