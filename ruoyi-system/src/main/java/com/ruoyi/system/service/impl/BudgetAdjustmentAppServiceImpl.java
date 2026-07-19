package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.domain.bo.BudgetAdjustmentAppBo;
import com.ruoyi.system.domain.vo.BudgetAdjustmentAppVo;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

/**
 * 预算调整申请Service业务层处理
 *
 * @author ruoyi
 * @date 2026-07-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetAdjustmentAppServiceImpl implements IBudgetAdjustmentAppService {

    private final BudgetAdjustmentAppMapper appMapper;
    private final ReservedBudgetMapper reservedBudgetMapper;
    private final ReservedAllocationLogMapper allocationLogMapper;
    private final BudgetPreparationDetailMapper budgetDetailMapper;
    private final ExpenseBillMapper expenseBillMapper;
    private final IFeeOperationLogService feeOperationLogService;
    private final ISysMessageService sysMessageService;
    private final IReservedBudgetService reservedBudgetService;
    private final ISysDeptService sysDeptService;
    private final SysDeptMapper sysDeptMapper;
    private final IMappingRuleService mappingRuleService;
    private final BudgetSubjectMapper budgetSubjectMapper;

    @Override
    public BudgetAdjustmentAppVo queryById(Long id) {
        BudgetAdjustmentAppVo vo = appMapper.selectVoById(id);
        // 动态从sys_dept获取最新部门名称
        if (vo != null && vo.getDeptId() != null) {
            com.ruoyi.common.core.domain.entity.SysDept dept = sysDeptService.selectDeptById(vo.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getDeptName());
            }
        }
        return vo;
    }

    @Override
    public Page<BudgetAdjustmentAppVo> selectPageList(BudgetAdjustmentAppBo bo, PageQuery pageQuery) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BudgetAdjustmentApp> wrapper =
            (com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BudgetAdjustmentApp>) buildQueryWrapper(bo);

        // 基于角色的数据可见性过滤
        applyRoleBasedFilter(wrapper);

        Page<BudgetAdjustmentAppVo> page = appMapper.selectVoPage(pageQuery.build(), wrapper);
        // 动态填充部门名称和实时预算可用余额
        for (BudgetAdjustmentAppVo vo : page.getRecords()) {
            if (vo.getDeptId() != null) {
                com.ruoyi.common.core.domain.entity.SysDept dept = sysDeptService.selectDeptById(vo.getDeptId());
                if (dept != null) {
                    vo.setDeptName(dept.getDeptName());
                }
            }
            // 动态查询科目实时可用余额（审批通过后余额已更新，此处反映最新值）
            if (vo.getTargetSubject() != null && vo.getDeptId() != null) {
                LocalDate now = LocalDate.now();
                BigDecimal remaining = budgetDetailMapper.getRemainingBudgetByPeriod(
                    vo.getTargetSubject(), vo.getDeptId(), now.getYear(), now.getMonthValue());
                vo.setCurrentBalance(remaining != null ? remaining : BigDecimal.ZERO);
            }
        }
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BudgetAdjustmentAppVo apply(BudgetAdjustmentAppBo bo) {
        // 1. 生成申请单号 BA-YYYYMMDD-XXX
        String appNo = "BA-" + new SimpleDateFormat("yyyyMMdd").format(new Date())
            + "-" + String.format("%03d", (int) (Math.random() * 900) + 100);

        // 2. 自动从登录人部门获取部门ID
        Long deptId = LoginHelper.getDeptId();

        // 3. 通过映射规则定位目标预算科目（物资小类 + 用途标签 → 预算科目）
        com.ruoyi.system.domain.vo.MappingRuleVo rule = mappingRuleService.matchRule(
            bo.getItemCategory(), bo.getPurposeTag(), deptId);
        String targetSubject = rule.getBudgetSubject();

        // 查询科目名称
        String targetSubjectName = targetSubject;
        BudgetSubject subjectEntity = budgetSubjectMapper.selectOne(
            com.baomidou.mybatisplus.core.toolkit.Wrappers.<BudgetSubject>lambdaQuery()
                .eq(BudgetSubject::getSubjectCode, targetSubject)
                .last("LIMIT 1"));
        if (subjectEntity != null) {
            targetSubjectName = subjectEntity.getSubjectName();
        }

        // 4. 构建申请实体
        BudgetAdjustmentApp app = BeanCopyUtils.copy(bo, BudgetAdjustmentApp.class);
        app.setAppNo(appNo);
        app.setApplicantId(LoginHelper.getUserId());
        app.setApplicantName(LoginHelper.getNickName());
        app.setDeptId(deptId);
        // 设置部门名称
        com.ruoyi.common.core.domain.entity.SysDept dept = sysDeptService.selectDeptById(deptId);
        if (dept != null) {
            app.setDeptName(dept.getDeptName());
        }
        // 填入映射规则解析出的目标科目
        app.setTargetSubject(targetSubject);
        app.setTargetSubjectName(targetSubjectName);
        app.setCurrentLevel(1);
        app.setStatus("DEPT_APPROVING");

        // 5. 查询申请时科目余额（作为预算金额，按部门+期间隔离）
        LocalDate now = LocalDate.now();
        BigDecimal currentBalance = budgetDetailMapper.getRemainingBudgetByPeriod(
            targetSubject, deptId, now.getYear(), now.getMonthValue());
        app.setCurrentBalance(currentBalance != null ? currentBalance : BigDecimal.ZERO);
        app.setBudgetAmount(currentBalance != null ? currentBalance : BigDecimal.ZERO);

        // 6. 关联部门预留预算ID
        ReservedBudget deptReserved = reservedBudgetService.findByLevelAndDept(
            getCurrentPeriod(), "DEPT", deptId);
        if (deptReserved != null) {
            app.setDeptReservedId(deptReserved.getId());
        }

        appMapper.insert(app);

        // 7. 写操作日志（BA_APPLY）
        Map<String, Object> detail = new HashMap<>();
        detail.put("itemCategory", bo.getItemCategory());
        detail.put("purposeTag", bo.getPurposeTag());
        detail.put("targetSubject", targetSubject);
        detail.put("requestAmount", bo.getRequestAmount());
        detail.put("reason", bo.getReason());
        feeOperationLogService.log("BUDGET_ADJUST", "BA_APPLY",
            bo.getBillId(), null,
            app.getId(), "PENDING", "DEPT_APPROVING", JSONUtil.toJsonStr(detail));

        // 8. 通知部门领导
        log.info("预算调整申请已提交，申请单号：{}，通知部门领导审批", appNo);

        return BeanCopyUtils.copy(app, BudgetAdjustmentAppVo.class);
    }

    /**
     * 获取当前用户部门下指定科目的预算余额
     */
    public BigDecimal getSubjectBudget(String subjectCode) {
        Long deptId = LoginHelper.getDeptId();
        LocalDate now = LocalDate.now();
        BigDecimal balance = budgetDetailMapper.getRemainingBudgetByPeriod(
            subjectCode, deptId, now.getYear(), now.getMonthValue());
        return balance != null ? balance : BigDecimal.ZERO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancel(Long id) {
        BudgetAdjustmentApp app = appMapper.selectById(id);
        if (app == null) {
            throw new ServiceException("申请单不存在");
        }
        // 只有申请人本人可以撤回
        if (!app.getApplicantId().equals(LoginHelper.getUserId())) {
            throw new ServiceException("只能撤回自己提交的申请");
        }
        // 只有审批中的可以撤回
        if (!Arrays.asList("DEPT_APPROVING", "BRANCH_APPROVING", "HQ_APPROVING").contains(app.getStatus())) {
            throw new ServiceException("只能撤回审批中的申请");
        }
        String beforeStatus = app.getStatus();
        app.setStatus("CANCELLED");
        app.setCompleteTime(new Date());
        appMapper.updateById(app);

        // 写操作日志
        feeOperationLogService.log("BUDGET_ADJUST", "BA_CANCEL", app.getBillId(), app.getBillNo(),
            app.getId(), beforeStatus, "CANCELLED",
            JSONUtil.toJsonStr(Collections.singletonMap("reason", "申请人主动撤回")));
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean approve(Long id, BigDecimal allocateAmount, String opinion) {
        BudgetAdjustmentApp app = appMapper.selectById(id);
        validateApproving(app);

        int currentLevel = app.getCurrentLevel();
        String beforeStatus = app.getStatus();

        // 根据审批层级执行不同的分摊逻辑
        switch (currentLevel) {
            case 1:
                approveAtDeptLevel(app, beforeStatus);
                break;
            case 2:
                approveAtBranchLevel(app, allocateAmount, beforeStatus);
                break;
            case 3:
                approveAtHqLevel(app, allocateAmount, beforeStatus);
                break;
            default:
                throw new ServiceException("无效的审批层级");
        }

        // 清除预算余额缓存
        RedisUtils.deleteKeys("fee:budget_balance:*");
        return true;
    }

    /**
     * 部门级审批通过：从部门资金池扣减差额，增加科目预算余额（原有逻辑）
     */
    private void approveAtDeptLevel(BudgetAdjustmentApp app, String beforeStatus) {
        // 计算实际需从资金池分摊的金额 = 申请金额 - 预算可用余额（差额分摊）
        BigDecimal currentBalance = app.getCurrentBalance();
        if (currentBalance == null) {
            currentBalance = BigDecimal.ZERO;
        }
        BigDecimal allocateAmount = app.getRequestAmount().subtract(currentBalance);
        if (allocateAmount.compareTo(BigDecimal.ZERO) < 0) {
            allocateAmount = BigDecimal.ZERO;
        }
        log.info("部门级审批通过：申请金额={}, 当前余额={}, 实际分摊金额={}",
            app.getRequestAmount(), currentBalance, allocateAmount);

        // 获取或创建部门预留预算
        Long reservedId = getOrCreateReservedByLevel(app);
        ReservedBudget reserved = reservedBudgetMapper.selectForUpdate(reservedId);
        if (reserved == null) {
            throw new ServiceException("部门预留预算不存在");
        }

        // 校验预留剩余额度
        BigDecimal available = reserved.getTotalAmount().subtract(reserved.getAllocatedAmount());
        if (available.compareTo(allocateAmount) < 0) {
            throw new ServiceException("部门资金池剩余额度不足（需" + allocateAmount + "，可用" + available + "），请向上级转审");
        }

        // 更新预留预算已分摊金额
        BigDecimal beforeReservedAvailable = available;
        reserved.setAllocatedAmount(reserved.getAllocatedAmount().add(allocateAmount));
        reservedBudgetMapper.updateById(reserved);

        // 查询审批前后可用余额（按部门+期间隔离）
        LocalDate now = LocalDate.now();
        int budgetYear = now.getYear();
        int budgetMonth = now.getMonthValue();
        BigDecimal beforeAvailable = budgetDetailMapper.getRemainingBudgetByPeriod(
            app.getTargetSubject(), app.getDeptId(), budgetYear, budgetMonth);
        if (beforeAvailable == null) {
            beforeAvailable = BigDecimal.ZERO;
        }
        // 直接将预算编制金额更新为request_amount（目标预算金额）
        budgetDetailMapper.setBudgetAmount(
            app.getTargetSubject(), app.getDeptId(), app.getRequestAmount(), budgetYear, budgetMonth);
        BigDecimal afterAvailable = budgetDetailMapper.getRemainingBudgetByPeriod(
            app.getTargetSubject(), app.getDeptId(), budgetYear, budgetMonth);
        if (afterAvailable == null) {
            afterAvailable = BigDecimal.ZERO;
        }

        // 写入分摊记录
        writeAllocationLog(reserved.getId(), app, allocateAmount,
            beforeAvailable, afterAvailable, beforeReservedAvailable,
            beforeReservedAvailable.subtract(allocateAmount));

        // 更新申请单状态
        updateAppStatus(app, "APPROVED", allocateAmount, reservedId);

        // 写操作日志
        Map<String, Object> detail = new HashMap<>();
        detail.put("level", 1);
        detail.put("allocateAmount", allocateAmount);
        detail.put("targetSubject", app.getTargetSubject());
        feeOperationLogService.log("BUDGET_ADJUST", "BA_ALLOCATE", app.getBillId(), app.getBillNo(),
            app.getId(), beforeStatus, "APPROVED", JSONUtil.toJsonStr(detail));

        // 通知报销人
        sysMessageService.sendMessage(app.getApplicantId(), "预算调整申请已通过",
            "您的预算调整申请（" + app.getAppNo() + "）已审批通过，科目预算余额已调整为：¥" + app.getRequestAmount()
                + "，从部门资金池分摊：¥" + allocateAmount,
            "APPROVAL_RESULT", "BUDGET_ADJUST", app.getId());

        // 预留预算预警检查
        reservedBudgetService.checkWarning(reservedId);
    }

    /**
     * 分公司级审批通过：从分公司资金池扣减，加到申请部门的部门资金池
     */
    private void approveAtBranchLevel(BudgetAdjustmentApp app, BigDecimal allocateAmount, String beforeStatus) {
        if (allocateAmount == null || allocateAmount.compareTo(BigDecimal.ZERO) <= 0) {
            allocateAmount = app.getRequestAmount();
        }
        log.info("分公司级审批通过：申请金额={}, 分摊到部门资金池金额={}",
            app.getRequestAmount(), allocateAmount);

        // 1. 从分公司资金池扣减
        Long branchReservedId = getOrCreateReservedByLevel(app);
        ReservedBudget branchReserved = reservedBudgetMapper.selectForUpdate(branchReservedId);
        if (branchReserved == null) {
            throw new ServiceException("分公司预留预算不存在");
        }

        BigDecimal branchAvailable = branchReserved.getTotalAmount().subtract(branchReserved.getAllocatedAmount());
        if (branchAvailable.compareTo(allocateAmount) < 0) {
            throw new ServiceException("分公司资金池剩余额度不足（需" + allocateAmount + "，可用" + branchAvailable + "），请向上级转审");
        }

        BigDecimal beforeBranchAvailable = branchAvailable;
        branchReserved.setAllocatedAmount(branchReserved.getAllocatedAmount().add(allocateAmount));
        reservedBudgetMapper.updateById(branchReserved);

        // 2. 增加申请部门的部门资金池
        String period = getCurrentPeriod();
        ReservedBudget deptReserved = getOrCreateDeptFundPool(app.getDeptId(), app.getDeptName(), period);
        BigDecimal beforeDeptTotal = deptReserved.getTotalAmount();
        deptReserved.setTotalAmount(deptReserved.getTotalAmount().add(allocateAmount));
        reservedBudgetMapper.updateById(deptReserved);

        // 3. 写入分摊记录
        writeAllocationLog(branchReserved.getId(), app, allocateAmount,
            beforeDeptTotal, deptReserved.getTotalAmount(),
            beforeBranchAvailable, beforeBranchAvailable.subtract(allocateAmount));

        // 4. 更新申请单状态
        updateAppStatus(app, "APPROVED", allocateAmount, branchReservedId);

        // 5. 写操作日志
        Map<String, Object> detail = new HashMap<>();
        detail.put("level", 2);
        detail.put("allocateAmount", allocateAmount);
        detail.put("targetDeptId", app.getDeptId());
        detail.put("targetDeptName", app.getDeptName());
        feeOperationLogService.log("BUDGET_ADJUST", "BA_ALLOCATE", app.getBillId(), app.getBillNo(),
            app.getId(), beforeStatus, "APPROVED", JSONUtil.toJsonStr(detail));

        // 6. 通知报销人
        sysMessageService.sendMessage(app.getApplicantId(), "预算调整已审批（分公司）",
            "分公司已审批通过您的预算调整申请（" + app.getAppNo() + "），已分摊 ¥" + allocateAmount
                + " 到部门资金池，请部门经理继续分配科目预算。",
            "APPROVAL_RESULT", "BUDGET_ADJUST", app.getId());

        // 7. 预留预算预警检查
        reservedBudgetService.checkWarning(branchReservedId);
    }

    /**
     * 总公司级审批通过：从总公司资金池扣减，加到申请分公司的分公司资金池
     */
    private void approveAtHqLevel(BudgetAdjustmentApp app, BigDecimal allocateAmount, String beforeStatus) {
        if (allocateAmount == null || allocateAmount.compareTo(BigDecimal.ZERO) <= 0) {
            allocateAmount = app.getRequestAmount();
        }
        log.info("总公司级审批通过：申请金额={}, 分摊到分公司资金池金额={}",
            app.getRequestAmount(), allocateAmount);

        // 1. 从总公司资金池扣减
        Long hqReservedId = getOrCreateReservedByLevel(app);
        ReservedBudget hqReserved = reservedBudgetMapper.selectForUpdate(hqReservedId);
        if (hqReserved == null) {
            throw new ServiceException("总公司预留预算不存在");
        }

        BigDecimal hqAvailable = hqReserved.getTotalAmount().subtract(hqReserved.getAllocatedAmount());
        if (hqAvailable.compareTo(allocateAmount) < 0) {
            throw new ServiceException("总公司资金池剩余额度不足（需" + allocateAmount + "，可用" + hqAvailable + "）");
        }

        BigDecimal beforeHqAvailable = hqAvailable;
        hqReserved.setAllocatedAmount(hqReserved.getAllocatedAmount().add(allocateAmount));
        reservedBudgetMapper.updateById(hqReserved);

        // 2. 找到申请部门所在的分公司，增加分公司的资金池
        Long branchDeptId = findBranchDeptId(app.getDeptId());
        String period = getCurrentPeriod();
        SysDept branchDept = sysDeptMapper.selectById(branchDeptId);
        String branchDeptName = (branchDept != null) ? branchDept.getDeptName() : "分公司";
        ReservedBudget branchReserved = getOrCreateBranchFundPool(branchDeptId, branchDeptName, period);
        BigDecimal beforeBranchTotal = branchReserved.getTotalAmount();
        branchReserved.setTotalAmount(branchReserved.getTotalAmount().add(allocateAmount));
        reservedBudgetMapper.updateById(branchReserved);

        // 3. 写入分摊记录
        writeAllocationLog(hqReserved.getId(), app, allocateAmount,
            beforeBranchTotal, branchReserved.getTotalAmount(),
            beforeHqAvailable, beforeHqAvailable.subtract(allocateAmount));

        // 4. 更新申请单状态
        updateAppStatus(app, "APPROVED", allocateAmount, hqReservedId);

        // 5. 写操作日志
        Map<String, Object> detail = new HashMap<>();
        detail.put("level", 3);
        detail.put("allocateAmount", allocateAmount);
        detail.put("targetBranchDeptId", branchDeptId);
        detail.put("targetBranchName", branchDeptName);
        feeOperationLogService.log("BUDGET_ADJUST", "BA_ALLOCATE", app.getBillId(), app.getBillNo(),
            app.getId(), beforeStatus, "APPROVED", JSONUtil.toJsonStr(detail));

        // 6. 通知报销人
        sysMessageService.sendMessage(app.getApplicantId(), "预算调整已审批（总公司）",
            "总公司已审批通过您的预算调整申请（" + app.getAppNo() + "），已分摊 ¥" + allocateAmount
                + " 到分公司资金池。",
            "APPROVAL_RESULT", "BUDGET_ADJUST", app.getId());

        // 7. 预留预算预警检查
        reservedBudgetService.checkWarning(hqReservedId);
    }

    /**
     * 获取或创建部门级资金池
     */
    private ReservedBudget getOrCreateDeptFundPool(Long deptId, String deptName, String period) {
        ReservedBudget reserved = reservedBudgetService.findByLevelAndDept(period, "DEPT", deptId);
        if (reserved == null) {
            reserved = new ReservedBudget();
            reserved.setBudgetPeriod(period);
            reserved.setLevelType("DEPT");
            reserved.setDeptId(deptId);
            reserved.setDeptName(deptName);
            reserved.setTotalAmount(BigDecimal.ZERO);
            reserved.setAllocatedAmount(BigDecimal.ZERO);
            reserved.setWarningThreshold(new BigDecimal("80.00"));
            reserved.setCreatorId(LoginHelper.getUserId());
            reserved.setStatus("ACTIVE");
            reserved.setRemark("分公司审批时自动创建的部门资金池");
            reservedBudgetMapper.insert(reserved);
            log.info("自动创建部门资金池：deptId={}, id={}", deptId, reserved.getId());
        }
        return reserved;
    }

    /**
     * 获取或创建分公司级资金池
     */
    private ReservedBudget getOrCreateBranchFundPool(Long branchDeptId, String branchDeptName, String period) {
        ReservedBudget reserved = reservedBudgetService.findByLevelAndDept(period, "BRANCH", branchDeptId);
        if (reserved == null) {
            reserved = new ReservedBudget();
            reserved.setBudgetPeriod(period);
            reserved.setLevelType("BRANCH");
            reserved.setDeptId(branchDeptId);
            reserved.setDeptName(branchDeptName);
            reserved.setTotalAmount(BigDecimal.ZERO);
            reserved.setAllocatedAmount(BigDecimal.ZERO);
            reserved.setWarningThreshold(new BigDecimal("80.00"));
            reserved.setCreatorId(LoginHelper.getUserId());
            reserved.setStatus("ACTIVE");
            reserved.setRemark("总公司审批时自动创建的分公司资金池");
            reservedBudgetMapper.insert(reserved);
            log.info("自动创建分公司资金池：branchDeptId={}, id={}", branchDeptId, reserved.getId());
        }
        return reserved;
    }

    /**
     * 写入分摊记录
     */
    private void writeAllocationLog(Long reservedId, BudgetAdjustmentApp app,
                                    BigDecimal allocateAmount,
                                    BigDecimal beforeAvailable, BigDecimal afterAvailable,
                                    BigDecimal beforeReservedAvailable, BigDecimal afterReservedAvailable) {
        ReservedAllocationLog allocLog = new ReservedAllocationLog();
        allocLog.setReservedId(reservedId);
        allocLog.setApplicationId(app.getId());
        allocLog.setAppNo(app.getAppNo());
        allocLog.setTargetSubject(app.getTargetSubject());
        allocLog.setTargetSubjectName(app.getTargetSubjectName());
        allocLog.setAllocateAmount(allocateAmount);
        allocLog.setBeforeAvailable(beforeAvailable);
        allocLog.setAfterAvailable(afterAvailable);
        allocLog.setBeforeReservedAvailable(beforeReservedAvailable);
        allocLog.setAfterReservedAvailable(afterReservedAvailable);
        allocLog.setOperatorId(LoginHelper.getUserId());
        allocLog.setOperatorName(LoginHelper.getNickName());
        allocLog.setOperateTime(new Date());
        allocationLogMapper.insert(allocLog);
    }

    /**
     * 更新申请单状态和分摊信息
     */
    private void updateAppStatus(BudgetAdjustmentApp app, String status,
                                BigDecimal allocateAmount, Long reservedId) {
        app.setStatus(status);
        app.setAllocateAmount(allocateAmount);
        app.setAllocateFromReservedId(reservedId);
        app.setCompleteTime(new Date());
        appMapper.updateById(app);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean reject(Long id, String reason) {
        if (StrUtil.isBlank(reason)) {
            throw new ServiceException("驳回原因不能为空");
        }

        BudgetAdjustmentApp app = appMapper.selectById(id);
        validateApproving(app);

        String beforeStatus = app.getStatus();
        app.setStatus("REJECTED");
        app.setRejectReason(reason);
        app.setCompleteTime(new Date());
        appMapper.updateById(app);

        // 写操作日志（BA_REJECT）
        feeOperationLogService.log("BUDGET_ADJUST", "BA_REJECT", app.getBillId(), app.getBillNo(),
            app.getId(), beforeStatus, "REJECTED",
            JSONUtil.toJsonStr(Collections.singletonMap("rejectReason", reason)));

        // 通知报销人
        sysMessageService.sendMessage(app.getApplicantId(), "预算调整申请被驳回",
            "您的预算调整申请（" + app.getAppNo() + "）被驳回，原因：" + reason,
            "REJECTION", "BUDGET_ADJUST", app.getId());

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean escalate(Long id, String reason) {
        BudgetAdjustmentApp app = appMapper.selectById(id);
        validateApproving(app);

        int nextLevel = app.getCurrentLevel() + 1;
        if (nextLevel > 3) {
            throw new ServiceException("已是最高审批层级，无法继续转审");
        }

        String beforeStatus = app.getStatus();
        String afterStatus;
        switch (nextLevel) {
            case 2:
                afterStatus = "BRANCH_APPROVING";
                // 关联分公司预留预算ID
                ReservedBudget branchReserved = reservedBudgetService.findByLevelAndDept(
                    getCurrentPeriod(), "BRANCH", app.getDeptId());
                if (branchReserved != null) {
                    app.setBranchReservedId(branchReserved.getId());
                }
                break;
            case 3:
                afterStatus = "HQ_APPROVING";
                // 关联总公司预留预算ID
                ReservedBudget hqReserved = reservedBudgetService.findByLevelAndDept(
                    getCurrentPeriod(), "HQ", null);
                if (hqReserved != null) {
                    app.setHqReservedId(hqReserved.getId());
                }
                break;
            default:
                throw new ServiceException("无效的审批层级");
        }

        app.setCurrentLevel(nextLevel);
        app.setStatus(afterStatus);
        appMapper.updateById(app);

        // 写操作日志（BA_ESCALATE）
        Map<String, Object> detail = new HashMap<>();
        detail.put("fromLevel", app.getCurrentLevel() - 1);
        detail.put("toLevel", nextLevel);
        detail.put("reason", reason);
        feeOperationLogService.log("BUDGET_ADJUST", "BA_ESCALATE", app.getBillId(), app.getBillNo(),
            app.getId(), beforeStatus, afterStatus, JSONUtil.toJsonStr(detail));

        // 通知上级审批人（简化处理）
        log.info("预算调整申请已转审至第{}级，申请单号：{}", nextLevel, app.getAppNo());

        return true;
    }

    @Override
    public Page<BudgetAdjustmentAppVo> pendingList(PageQuery pageQuery) {
        // 根据当前用户角色查询待审批列表
        Set<String> roleKeys = LoginHelper.getLoginUser().getRolePermission();
        String roleKey = (roleKeys != null && !roleKeys.isEmpty()) ? roleKeys.iterator().next() : "";
        String statusFilter;

        if ("admin".equals(roleKey)) {
            // admin可看到所有待审批
            statusFilter = null;
        } else if ("deptManager".equals(roleKey)) {
            statusFilter = "DEPT_APPROVING";
        } else if ("branch".equals(roleKey)) {
            statusFilter = "BRANCH_APPROVING";
        } else if ("headquarters".equals(roleKey)) {
            statusFilter = "HQ_APPROVING";
        } else {
            statusFilter = "DEPT_APPROVING";
        }

        Wrapper<BudgetAdjustmentApp> wrapper = Wrappers.<BudgetAdjustmentApp>lambdaQuery()
            .in(BudgetAdjustmentApp::getStatus, "DEPT_APPROVING", "BRANCH_APPROVING", "HQ_APPROVING")
            .eq(StrUtil.isNotBlank(statusFilter) && !"admin".equals(roleKey),
                BudgetAdjustmentApp::getStatus, statusFilter)
            .orderByAsc(BudgetAdjustmentApp::getCreateTime);

        Page<BudgetAdjustmentAppVo> page = appMapper.selectVoPage(pageQuery.build(), wrapper);
        // 动态从sys_dept获取最新部门名称
        for (BudgetAdjustmentAppVo vo : page.getRecords()) {
            if (vo.getDeptId() != null) {
                com.ruoyi.common.core.domain.entity.SysDept dept = sysDeptService.selectDeptById(vo.getDeptId());
                if (dept != null) {
                    vo.setDeptName(dept.getDeptName());
                }
            }
        }
        return page;
    }

    /**
     * 校验申请单处于审批中状态
     */
    private void validateApproving(BudgetAdjustmentApp app) {
        if (app == null) {
            throw new ServiceException("申请单不存在");
        }
        if (!"DEPT_APPROVING".equals(app.getStatus())
            && !"BRANCH_APPROVING".equals(app.getStatus())
            && !"HQ_APPROVING".equals(app.getStatus())) {
            throw new ServiceException("该申请单已处理完毕");
        }
    }

    /**
     * 根据当前审批层级获取对应的预留预算ID
     */
    private Long getReservedIdByLevel(BudgetAdjustmentApp app) {
        switch (app.getCurrentLevel()) {
            case 1:
                return app.getDeptReservedId();
            case 2:
                return app.getBranchReservedId();
            case 3:
                return app.getHqReservedId();
            default:
                return null;
        }
    }

    /**
     * 根据当前审批层级获取或自动创建默认预留预算，并关联到申请单
     * 默认额度：部门10万 / 分公司50万 / 总公司100万
     */
    private Long getOrCreateReservedByLevel(BudgetAdjustmentApp app) {
        Long reservedId = getReservedIdByLevel(app);
        if (reservedId != null) {
            // 已有预留预算记录，校验是否仍然存在
            ReservedBudget existing = reservedBudgetMapper.selectById(reservedId);
            if (existing != null) {
                return reservedId;
            }
        }

        // 预留预算不存在，自动创建默认记录
        String period = getCurrentPeriod();
        String levelType;
        Long deptId = app.getDeptId();
        String deptName = app.getDeptName();
        BigDecimal defaultTotal;

        switch (app.getCurrentLevel()) {
            case 1:
                levelType = "DEPT";
                defaultTotal = new BigDecimal("100000.00"); // 部门10万
                break;
            case 2:
                levelType = "BRANCH";
                defaultTotal = new BigDecimal("500000.00"); // 分公司50万
                break;
            case 3:
                levelType = "HQ";
                defaultTotal = new BigDecimal("1000000.00"); // 总公司100万
                deptId = null;
                break;
            default:
                throw new ServiceException("无效的审批层级");
        }

        // 再次查询是否已有同层级同期间的预留预算（可能被其他人先创建了）
        ReservedBudget existing = reservedBudgetService.findByLevelAndDept(period, levelType, deptId);
        if (existing != null) {
            // 已存在，关联到申请单并返回
            linkReservedToApp(app, existing.getId());
            return existing.getId();
        }

        // 创建默认预留预算记录
        ReservedBudget newReserved = new ReservedBudget();
        newReserved.setBudgetPeriod(period);
        newReserved.setLevelType(levelType);
        newReserved.setDeptId(deptId);
        newReserved.setDeptName(deptName);
        newReserved.setTotalAmount(defaultTotal);
        newReserved.setAllocatedAmount(BigDecimal.ZERO);
        newReserved.setWarningThreshold(new BigDecimal("80.00"));
        newReserved.setCreatorId(LoginHelper.getUserId());
        newReserved.setStatus("ACTIVE");
        newReserved.setRemark("审批时自动创建的默认资金池");
        reservedBudgetMapper.insert(newReserved);
        log.info("自动创建默认预留预算：levelType={}, deptId={}, totalAmount={}, id={}",
            levelType, deptId, defaultTotal, newReserved.getId());

        // 关联到申请单
        linkReservedToApp(app, newReserved.getId());
        return newReserved.getId();
    }

    /**
     * 将预留预算ID关联到申请单对应层级字段
     */
    private void linkReservedToApp(BudgetAdjustmentApp app, Long reservedId) {
        switch (app.getCurrentLevel()) {
            case 1:
                app.setDeptReservedId(reservedId);
                break;
            case 2:
                app.setBranchReservedId(reservedId);
                break;
            case 3:
                app.setHqReservedId(reservedId);
                break;
        }
        appMapper.updateById(app);
    }

    /**
     * 获取当前预算期间（简化：使用当前年月）
     */
    private String getCurrentPeriod() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.YEAR) + "-" + String.format("%02d", cal.get(Calendar.MONTH) + 1);
    }

    /**
     * 根据当前登录用户角色，追加数据可见性过滤条件：
     * - admin / 总公司领导：查看所有申请单
     * - 分公司领导：查看本分公司下属所有部门的申请单
     * - 部门领导：查看本部门的申请单
     * - 编制人员（默认）：仅查看自己提交的申请单
     */
    private void applyRoleBasedFilter(
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BudgetAdjustmentApp> wrapper) {
        Set<String> roleKeys = LoginHelper.getLoginUser().getRolePermission();
        Long currentDeptId = LoginHelper.getDeptId();
        String roleKey = (roleKeys != null && !roleKeys.isEmpty()) ? roleKeys.iterator().next() : "";

        if (LoginHelper.isAdmin() || "admin".equals(roleKey)
            || "headquarters".equals(roleKey)) {
            // 总公司 / admin：查看所有，不加过滤
            return;
        }

        if ("branch".equals(roleKey)) {
            // 分公司领导：查看本分公司下属所有部门
            if (currentDeptId != null) {
                Long branchDeptId = findBranchDeptId(currentDeptId);
                if (branchDeptId != null) {
                    List<Long> childDeptIds = getChildDeptIds(branchDeptId);
                    if (!childDeptIds.isEmpty()) {
                        wrapper.in(BudgetAdjustmentApp::getDeptId, childDeptIds);
                    }
                }
            }
        } else if ("deptManager".equals(roleKey)) {
            // 部门领导：仅查看本部门
            if (currentDeptId != null) {
                wrapper.eq(BudgetAdjustmentApp::getDeptId, currentDeptId);
            }
        } else {
            // 编制人员（默认）：仅查看自己提交的申请
            wrapper.eq(BudgetAdjustmentApp::getApplicantId, LoginHelper.getUserId());
        }
    }

    /**
     * 向上查找分公司级别的部门ID（parentId为0的顶级部门）
     */
    private Long findBranchDeptId(Long deptId) {
        SysDept dept = sysDeptMapper.selectById(deptId);
        if (dept == null) return deptId;
        if (dept.getParentId() == null || dept.getParentId() == 0) {
            return dept.getDeptId();
        }
        return findBranchDeptId(dept.getParentId());
    }

    /**
     * 获取指定部门及其所有下属部门的ID列表
     */
    private List<Long> getChildDeptIds(Long branchDeptId) {
        List<SysDept> allDepts = sysDeptMapper.selectList(
            Wrappers.<SysDept>lambdaQuery().eq(SysDept::getDelFlag, "0"));
        List<Long> result = new ArrayList<>();
        result.add(branchDeptId);
        for (SysDept dept : allDepts) {
            if (dept.getAncestors() != null) {
                // ancestors格式如 "0,100,200"，用逗号分割后匹配
                String[] parts = dept.getAncestors().split(",");
                for (String part : parts) {
                    if (String.valueOf(branchDeptId).equals(part.trim())) {
                        result.add(dept.getDeptId());
                        break;
                    }
                }
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByIds(Collection<Long> ids) {
        // 仅未审批通过的申请单可删除（APPROVED状态不可删）
        for (Long id : ids) {
            BudgetAdjustmentApp app = appMapper.selectById(id);
            if (app != null && "APPROVED".equals(app.getStatus())) {
                throw new ServiceException("已审批通过的预算调整申请不可删除");
            }
            // 写操作日志
            if (app != null) {
                feeOperationLogService.log("BUDGET_ADJUST", "BA_DELETE", app.getBillId(), app.getBillNo(),
                    app.getId(), app.getStatus(), "DELETED",
                    cn.hutool.json.JSONUtil.toJsonStr(Collections.singletonMap("reason", "申请人删除")));
            }
        }
        return appMapper.deleteBatchIds(ids) > 0;
    }

    private Wrapper<BudgetAdjustmentApp> buildQueryWrapper(BudgetAdjustmentAppBo bo) {
        return Wrappers.<BudgetAdjustmentApp>lambdaQuery()
            .eq(StrUtil.isNotBlank(bo.getStatus()), BudgetAdjustmentApp::getStatus, bo.getStatus())
            .eq(bo.getBillId() != null, BudgetAdjustmentApp::getBillId, bo.getBillId())
            .like(StrUtil.isNotBlank(bo.getAppNo()), BudgetAdjustmentApp::getAppNo, bo.getAppNo())
            .orderByDesc(BudgetAdjustmentApp::getCreateTime);
    }
}
