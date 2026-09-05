package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.FeePayment;
import com.ruoyi.system.domain.FeeProject;
import com.ruoyi.system.domain.bo.FeePaymentBo;
import com.ruoyi.system.domain.vo.FeePaymentVo;
import com.ruoyi.system.mapper.FeePaymentMapper;
import com.ruoyi.system.mapper.FeeProjectMapper;
import com.ruoyi.system.service.IFeeNotificationService;
import com.ruoyi.system.service.IFeePaymentService;
import com.ruoyi.system.service.IFeeProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 付款单Service实现（T5.2）
 * 财务登记 → 老板审批 → 通过扣项目预算 + 记总支出
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeePaymentServiceImpl implements IFeePaymentService {

    private final FeePaymentMapper paymentMapper;
    private final FeeProjectMapper projectMapper;
    private final IFeeProjectService projectService;
    private final IFeeNotificationService notificationService;

    @Override
    public TableDataInfo<FeePaymentVo> queryPageList(FeePaymentBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<FeePayment> wrapper = Wrappers.<FeePayment>lambdaQuery()
            .like(StrUtil.isNotBlank(bo.getPaymentNo()), FeePayment::getPaymentNo, bo.getPaymentNo())
            .eq(bo.getProjectId() != null, FeePayment::getProjectId, bo.getProjectId())
            .like(StrUtil.isNotBlank(bo.getPayee()), FeePayment::getPayee, bo.getPayee())
            .eq(StrUtil.isNotBlank(bo.getStatus()), FeePayment::getStatus, bo.getStatus())
            .orderByDesc(FeePayment::getCreateTime);
        Page<FeePaymentVo> page = paymentMapper.selectVoPage(pageQuery.build(), wrapper);
        fillProjectName(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public FeePaymentVo queryById(Long id) {
        FeePaymentVo vo = paymentMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("付款单不存在");
        }
        fillProjectName(List.of(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertPayment(FeePaymentBo bo) {
        FeeProject project = projectMapper.selectById(bo.getProjectId());
        if (project == null) {
            throw new ServiceException("关联项目不存在");
        }
        if (!"ACTIVE".equals(project.getStatus())) {
            throw new ServiceException("仅进行中的项目可登记付款单");
        }
        FeePayment payment = BeanCopyUtils.copy(bo, FeePayment.class);
        payment.setPaymentNo(generatePaymentNo());
        payment.setStatus("PENDING");
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(new Date());
        }
        if (StrUtil.isBlank(payment.getInvoiceStatus())) {
            payment.setInvoiceStatus("NONE");
        }
        paymentMapper.insert(payment);

        // 通知老板审批（合并通知由消息中心聚合，T5.12）
        notifyBossNewPayment(payment, project);
        return payment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int approvePayment(Long id) {
        FeePayment payment = paymentMapper.selectById(id);
        if (payment == null) {
            throw new ServiceException("付款单不存在");
        }
        if (!"PENDING".equals(payment.getStatus())) {
            throw new ServiceException("仅待审批状态的付款单可审批");
        }
        // 扣项目预算（不足则阻断）
        boolean deducted = projectService.deductBudget(payment.getProjectId(), null, payment.getAmount(), true);
        if (!deducted) {
            throw new ServiceException("项目预算余额不足，无法审批通过该付款单");
        }
        FeePayment update = new FeePayment();
        update.setId(id);
        update.setStatus("APPROVED");
        update.setApprovedBy(LoginHelper.getUserId());
        update.setApprovedAt(new Date());
        int rows = paymentMapper.updateById(update);

        // 通知财务与登记人（角色广播，T5.12）
        try {
            notificationService.notifyRole("finance", "付款单审批通过",
                "付款单【" + payment.getPaymentNo() + "】已审批通过，金额 " + payment.getAmount()
                    + " 元，已扣减项目预算并计入项目总支出",
                "APPROVAL", "PAYMENT", id);
        } catch (Exception e) {
            log.warn("付款单审批通知发送失败（不阻断主流程）: {}", e.getMessage());
        }
        return rows;
    }

    @Override
    public int rejectPayment(Long id, String reason) {
        FeePayment payment = paymentMapper.selectById(id);
        if (payment == null) {
            throw new ServiceException("付款单不存在");
        }
        if (!"PENDING".equals(payment.getStatus())) {
            throw new ServiceException("仅待审批状态的付款单可驳回");
        }
        FeePayment update = new FeePayment();
        update.setId(id);
        update.setStatus("REJECTED");
        update.setApprovedBy(LoginHelper.getUserId());
        update.setApprovedAt(new Date());
        update.setDescription(StrUtil.isNotBlank(reason)
            ? StrUtil.nullToEmpty(payment.getDescription()) + " | 驳回：" + reason
            : payment.getDescription());
        return paymentMapper.updateById(update);
    }

    /**
     * 生成付款单号 PAY-YYYYMMDD-XXX
     */
    private String generatePaymentNo() {
        String prefix = "PAY-" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "-";
        Long count = paymentMapper.selectCount(Wrappers.<FeePayment>lambdaQuery()
            .likeRight(FeePayment::getPaymentNo, prefix));
        return prefix + String.format("%03d", (count == null ? 0 : count) + 1);
    }

    /**
     * 批量填充项目名称
     */
    private void fillProjectName(List<FeePaymentVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> projectIds = list.stream().map(FeePaymentVo::getProjectId)
            .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        if (projectIds.isEmpty()) {
            return;
        }
        Map<Long, FeeProject> projectMap = projectMapper.selectBatchIds(projectIds).stream()
            .collect(Collectors.toMap(FeeProject::getId, Function.identity()));
        list.forEach(vo -> {
            FeeProject project = projectMap.get(vo.getProjectId());
            vo.setProjectName(project == null ? null : project.getProjectName());
        });
    }

    /**
     * 通知老板（角色 boss）有新付款单待审批
     */
    private void notifyBossNewPayment(FeePayment payment, FeeProject project) {
        try {
            notificationService.notifyRole("boss", "新付款单待审批",
                "项目【" + project.getProjectName() + "】登记付款单【" + payment.getPaymentNo()
                    + "】，金额 " + payment.getAmount() + " 元，请及时审批",
                "APPROVAL", "PAYMENT", payment.getId());
        } catch (Exception e) {
            log.warn("付款单待审批通知发送失败（不阻断主流程）: {}", e.getMessage());
        }
    }
}
