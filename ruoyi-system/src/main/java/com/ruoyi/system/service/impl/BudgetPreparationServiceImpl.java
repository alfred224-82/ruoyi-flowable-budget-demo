package com.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.domain.bo.BudgetPreparationBo;
import com.ruoyi.system.domain.vo.*;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.IBudgetPreparationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 预算编制主表Service业务层处理
 *
 * @author ruoyi
 * @date 2026-06-19
 */
@RequiredArgsConstructor
@Service
public class BudgetPreparationServiceImpl implements IBudgetPreparationService {

    private final BudgetPreparationMapper baseMapper;
    private final BudgetRejectHistoryMapper rejectHistoryMapper;
    private final BudgetValidationRuleMapper validationRuleMapper;
    private final BudgetPreparationDetailMapper detailMapper;
    private final BudgetSubjectMapper subjectMapper;

    /**
     * 查询预算编制
     */
    @Override
    public BudgetPreparationVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询预算编制列表
     */
    @Override
    public TableDataInfo<BudgetPreparationVo> queryPageList(BudgetPreparationBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<BudgetPreparation> lqw = buildQueryWrapper(bo);
        Page<BudgetPreparationVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询预算编制列表
     */
    @Override
    public List<BudgetPreparationVo> queryList(BudgetPreparationBo bo) {
        LambdaQueryWrapper<BudgetPreparation> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<BudgetPreparation> buildQueryWrapper(BudgetPreparationBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<BudgetPreparation> lqw = Wrappers.lambdaQuery();
        
        // 年度筛选
        lqw.eq(bo.getBudgetYear() != null, BudgetPreparation::getBudgetYear, bo.getBudgetYear());
        
        // 月份筛选
        lqw.eq(bo.getBudgetMonth() != null, BudgetPreparation::getBudgetMonth, bo.getBudgetMonth());
        
        // 状态筛选
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), BudgetPreparation::getStatus, bo.getStatus());
        
        // 组织ID筛选（部门权限控制）
        lqw.eq(bo.getOrgId() != null, BudgetPreparation::getOrgId, bo.getOrgId());
        
        // 预算单号模糊查询
        lqw.like(StringUtils.isNotBlank(bo.getSheetNo()), BudgetPreparation::getSheetNo, bo.getSheetNo());
        
        // 按创建时间倒序
        lqw.orderByDesc(BudgetPreparation::getCreateTime);
        
        return lqw;
    }

    /**
     * 新增预算编制
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(BudgetPreparationBo bo) {
        BudgetPreparation add = BeanUtil.toBean(bo, BudgetPreparation.class);
        
        // 从登录用户信息自动填充组织ID和组织名称
        if (add.getOrgId() == null) {
            add.setOrgId(LoginHelper.getDeptId());
        }
        if (StringUtils.isBlank(add.getOrgName())) {
            add.setOrgName(LoginHelper.getLoginUser().getDeptName());
        }
        
        // 生成预算单号：BG-YYYYMM-XXX
        if (StringUtils.isBlank(add.getSheetNo())) {
            add.setSheetNo(generateSheetNo(add.getBudgetYear(), add.getBudgetMonth()));
        }
        
        // budget_period 是数据库生成列，置 null 由数据库自动计算
        add.setBudgetPeriod(null);
        
        // 设置初始状态为草稿
        if (StringUtils.isBlank(add.getStatus())) {
            add.setStatus("Draft");
        }
        
        // 设置当前处理人为创建人
        if (StringUtils.isBlank(add.getCurrentHandler())) {
            add.setCurrentHandler(LoginHelper.getUsername());
        }
        
        // 初始化金额为0
        if (add.getTotalBudget() == null) {
            add.setTotalBudget(BigDecimal.ZERO);
        }
        if (add.getTotalActual() == null) {
            add.setTotalActual(BigDecimal.ZERO);
        }
        if (add.getVarianceRate() == null) {
            add.setVarianceRate(BigDecimal.ZERO);
        }
        
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改预算编制
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(BudgetPreparationBo bo) {
        BudgetPreparation update = BeanUtil.toBean(bo, BudgetPreparation.class);
        
        // 校验状态：只有草稿和驳回状态可以修改
        BudgetPreparation existPreparation = baseMapper.selectById(bo.getId());
        if (existPreparation != null) {
            String status = existPreparation.getStatus();
            if (!"Draft".equals(status) && !"Rejected".equals(status)) {
                throw new ServiceException("只有草稿和驳回状态的预算编制可以修改");
            }
        }
        
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(BudgetPreparation entity) {
        // TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除预算编制
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 校验：只有草稿和驳回状态可以删除
            for (Long id : ids) {
                BudgetPreparation preparation = baseMapper.selectById(id);
                if (preparation != null) {
                    String status = preparation.getStatus();
                    if (!"Draft".equals(status) && !"Rejected".equals(status)) {
                        throw new ServiceException("预算单号[" + preparation.getSheetNo() + "]不是草稿或驳回状态，不能删除");
                    }
                }
            }
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 提交审核（单个）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean submitForReview(Long id) {
        BudgetPreparation preparation = baseMapper.selectById(id);
        if (preparation == null) {
            throw new ServiceException("预算编制不存在");
        }
        
        // 校验状态：只有草稿和驳回状态可以提交
        String status = preparation.getStatus();
        if (!"Draft".equals(status) && !"Rejected".equals(status)) {
            throw new ServiceException("预算编制状态不是草稿或驳回，不能提交审核");
        }
        
        // 校验明细数据完整性
        List<BudgetPreparationDetail> details = detailMapper.selectList(
            Wrappers.<BudgetPreparationDetail>lambdaQuery()
                .eq(BudgetPreparationDetail::getSheetId, id));

        if (details == null || details.isEmpty()) {
            throw new ServiceException("预算编制明细为空，请先填写预算数据");
        }

        // 校验是否存在金额为空的科目
        boolean hasEmptyAmount = details.stream()
            .anyMatch(d -> d.getBudgetAmount() == null);
        if (hasEmptyAmount) {
            throw new ServiceException("存在未填写预算金额的科目，请补充完整");
        }
        
        // 更新状态为待审核
        preparation.setStatus("Pending_Review");
        preparation.setRejectLevel("None");
        preparation.setRejectReason(null);
        
        return baseMapper.updateById(preparation) > 0;
    }

    /**
     * 批量提交审核
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchSubmitForReview(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择要提交的预算编制");
        }
        
        for (Long id : ids) {
            submitForReview(id);
        }
        return true;
    }

    /**
     * 批量审批通过
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchApprove(List<Long> ids, String remark) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择要审批的预算编制");
        }
        
        for (Long id : ids) {
            approve(id, remark);
        }
        return true;
    }

    /**
     * 审批通过（单个）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean approve(Long id, String remark) {
        BudgetPreparation preparation = baseMapper.selectById(id);
        if (preparation == null) {
            throw new ServiceException("预算编制不存在");
        }
        
        // 校验状态：只有待审核状态可以审批
        if (!"Pending_Review".equals(preparation.getStatus())) {
            throw new ServiceException("预算编制状态不是待审核，不能审批");
        }
        
        // 更新状态为已通过
        preparation.setStatus("Approved");
        preparation.setRemark(remark);
        
        return baseMapper.updateById(preparation) > 0;
    }

    /**
     * 审批驳回（单个）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean reject(Long id, String reason) {
        BudgetPreparation preparation = baseMapper.selectById(id);
        if (preparation == null) {
            throw new ServiceException("预算编制不存在");
        }
        
        // 校验状态：只有待审核状态可以驳回
        if (!"Pending_Review".equals(preparation.getStatus())) {
            throw new ServiceException("预算编制状态不是待审核，不能驳回");
        }
        
        // 校验驳回理由
        if (StringUtils.isBlank(reason)) {
            throw new ServiceException("驳回时必须填写驳回意见");
        }
        
        // 更新主表状态
        preparation.setStatus("Rejected");
        preparation.setRejectReason(reason);
        preparation.setRejectLevel("HQ");
        preparation.setCurrentHandler(LoginHelper.getUsername());
        boolean result = baseMapper.updateById(preparation) > 0;
        
        // 写入驳回历史
        if (result) {
            BudgetRejectHistory history = new BudgetRejectHistory();
            history.setSheetId(id);
            history.setSheetNo(preparation.getSheetNo());
            history.setRejectFromLevel("HQ");
            history.setRejectFromUser(LoginHelper.getUsername());
            history.setRejectFromName(LoginHelper.getNickName());
            history.setRejectToLevel("Branch");
            history.setRejectReason(reason);
            history.setRejectTime(new Date());
            history.setDeadlineTime(calculateDeadline("HQ"));
            rejectHistoryMapper.insert(history);
        }
        
        return result;
    }

    /**
     * 获取校验结果
     */
    @Override
    public List<ValidationResultVo> getValidationResults(Long sheetId) {
        List<ValidationResultVo> results = new ArrayList<>();
        
        // 1. 获取启用的校验规则
        List<BudgetValidationRule> rules = validationRuleMapper.selectList(
            Wrappers.<BudgetValidationRule>lambdaQuery()
                .eq(BudgetValidationRule::getIsActive, 1)
        );
        
        // 2. 获取预算明细
        List<BudgetPreparationDetail> details = detailMapper.selectList(
            Wrappers.<BudgetPreparationDetail>lambdaQuery()
                .eq(BudgetPreparationDetail::getSheetId, sheetId)
        );
        
        // 3. 获取主表数据
        BudgetPreparation sheet = baseMapper.selectById(sheetId);
        if (sheet == null) {
            return results;
        }
        
        // 4. 逐条规则执行校验
        for (BudgetValidationRule rule : rules) {
            List<ValidationResultVo> ruleResults = executeRule(rule, details, sheet);
            results.addAll(ruleResults);
        }
        
        return results;
    }

    /**
     * 执行单条校验规则
     */
    private List<ValidationResultVo> executeRule(BudgetValidationRule rule,
        List<BudgetPreparationDetail> details, BudgetPreparation sheet) {
        List<ValidationResultVo> results = new ArrayList<>();
        
        switch (rule.getRuleType()) {
            case "BUDGET_OVER":
                BigDecimal threshold = rule.getThresholdValue();
                if (threshold == null) {
                    break;
                }
                for (BudgetPreparationDetail d : details) {
                    if (d.getActualAmount() != null && d.getActualAmount().compareTo(BigDecimal.ZERO) > 0
                        && d.getVarianceAmount() != null) {
                        BigDecimal rate = d.getVarianceAmount().abs()
                            .divide(d.getActualAmount().abs(), 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));
                        if (rate.compareTo(threshold) > 0) {
                            results.add(buildResult(rule, d, "偏差" + rate + "%超过阈值" + threshold + "%"));
                        }
                    }
                }
                break;
            case "LOGIC_ERROR":
                BigDecimal totalBudget = sheet.getTotalBudget() != null ? sheet.getTotalBudget() : BigDecimal.ZERO;
                BigDecimal detailSum = details.stream()
                    .map(d -> d.getBudgetAmount() != null ? d.getBudgetAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (detailSum.compareTo(totalBudget) != 0) {
                    results.add(buildResult(rule, null, "明细合计" + detailSum + "与总额" + totalBudget + "不一致"));
                }
                break;
            case "SUBJECT_ERROR":
                // 批量预查询科目编码，避免N+1查询
                Set<String> subjectCodes = details.stream()
                    .map(BudgetPreparationDetail::getSubjectCode)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());
                Set<String> validCodes = Collections.emptySet();
                if (!subjectCodes.isEmpty()) {
                    List<BudgetSubject> subjects = subjectMapper.selectList(
                        Wrappers.<BudgetSubject>lambdaQuery()
                            .in(BudgetSubject::getSubjectCode, subjectCodes)
                            .eq(BudgetSubject::getIsActive, 1));
                    validCodes = subjects.stream()
                        .map(BudgetSubject::getSubjectCode)
                        .collect(Collectors.toSet());
                }
                for (BudgetPreparationDetail d : details) {
                    if (StringUtils.isNotBlank(d.getSubjectCode()) && !validCodes.contains(d.getSubjectCode())) {
                        results.add(buildResult(rule, d, "科目编码" + d.getSubjectCode() + "不存在"));
                    }
                }
                break;
            case "MISSING_DATA":
                for (BudgetPreparationDetail d : details) {
                    if (d.getBudgetAmount() == null || d.getSubjectCode() == null) {
                        results.add(buildResult(rule, d, "必填字段缺失"));
                    }
                }
                break;
            case "FORMULA_CHECK":
                for (BudgetPreparationDetail d : details) {
                    if ("INCOME".equals(d.getSubjectType()) && d.getBudgetAmount() != null
                        && d.getBudgetAmount().compareTo(BigDecimal.ZERO) < 0) {
                        results.add(buildResult(rule, d, "收入类科目金额不能为负"));
                    }
                }
                break;
            default:
                break;
        }
        return results;
    }

    /**
     * 构建校验结果
     */
    private ValidationResultVo buildResult(BudgetValidationRule rule, BudgetPreparationDetail detail, String message) {
        ValidationResultVo vo = new ValidationResultVo();
        vo.setRuleCode(rule.getRuleCode());
        vo.setRuleName(rule.getRuleName());
        vo.setSeverityLevel(rule.getSeverityLevel());
        vo.setMessage(message);
        vo.setPassed(false);
        if (detail != null) {
            vo.setSubjectCode(detail.getSubjectCode());
            vo.setSubjectName(detail.getSubjectName());
            vo.setBudgetAmount(detail.getBudgetAmount());
        }
        return vo;
    }

    /**
     * 分公司打回至部门
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean sendBackToDept(Long id, Long deptId, String reason) {
        BudgetPreparation preparation = baseMapper.selectById(id);
        if (preparation == null) {
            throw new ServiceException("预算编制不存在");
        }
        
        // 校验状态：必须是 Branch_Pending
        if (!"Branch_Pending".equals(preparation.getStatus())) {
            throw new ServiceException("只有分公司待处理状态才能打回部门");
        }
        if (StringUtils.isBlank(reason)) {
            throw new ServiceException("打回理由不能为空");
        }
        
        // 更新状态
        preparation.setStatus("Pending_Revision");
        preparation.setRejectLevel("Branch");
        preparation.setRejectReason(reason);
        preparation.setCurrentHandler(LoginHelper.getUsername());
        boolean result = baseMapper.updateById(preparation) > 0;
        
        // 写入驳回历史
        if (result) {
            BudgetRejectHistory history = new BudgetRejectHistory();
            history.setSheetId(id);
            history.setSheetNo(preparation.getSheetNo());
            history.setRejectFromLevel("Branch");
            history.setRejectFromUser(LoginHelper.getUsername());
            history.setRejectFromName(LoginHelper.getNickName());
            history.setRejectToLevel("Dept");
            history.setRejectToDeptId(deptId);
            history.setRejectReason(reason);
            history.setRejectTime(new Date());
            history.setDeadlineTime(calculateDeadline("Branch"));
            rejectHistoryMapper.insert(history);
        }
        
        return result;
    }

    /**
     * 查询驳回历史
     */
    @Override
    public List<BudgetRejectHistoryVo> queryRejectHistory(Long sheetId) {
        LambdaQueryWrapper<BudgetRejectHistory> lqw = Wrappers.lambdaQuery();
        lqw.eq(BudgetRejectHistory::getSheetId, sheetId);
        lqw.orderByDesc(BudgetRejectHistory::getRejectTime);
        return rejectHistoryMapper.selectVoList(lqw);
    }

    /**
     * 计算截止时间
     */
    private Date calculateDeadline(String rejectLevel) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 1);
        cal.set(Calendar.DAY_OF_MONTH, 7);
        if ("HQ".equals(rejectLevel)) {
            cal.set(Calendar.HOUR_OF_DAY, 12);
        } else {
            cal.set(Calendar.HOUR_OF_DAY, 18);
        }
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTime();
    }

    /**
     * 生成预算单号
     * 格式：BG-YYYYMM-XXX
     */
    private String generateSheetNo(Integer year, Integer month) {
        String dateStr = String.format("%04d%02d", year, month);
        
        // 查询当月已有的单据数量
        LambdaQueryWrapper<BudgetPreparation> lqw = Wrappers.lambdaQuery();
        lqw.likeRight(BudgetPreparation::getSheetNo, "BG-" + dateStr);
        long count = baseMapper.selectCount(lqw);
        
        // 生成序号（3位）
        String seqNo = String.format("%03d", count + 1);
        
        return "BG-" + dateStr + "-" + seqNo;
    }
}
