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
import com.ruoyi.system.domain.FeeIncomeConfirmation;
import com.ruoyi.system.domain.FeeProject;
import com.ruoyi.system.domain.FeeReceipt;
import com.ruoyi.system.domain.bo.FeeReceiptBo;
import com.ruoyi.system.domain.vo.FeeReceiptVo;
import com.ruoyi.system.mapper.FeeIncomeConfirmationMapper;
import com.ruoyi.system.mapper.FeeProjectMapper;
import com.ruoyi.system.mapper.FeeReceiptMapper;
import com.ruoyi.system.service.IFeeNotificationService;
import com.ruoyi.system.service.IFeeProjectService;
import com.ruoyi.system.service.IFeeReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 收款单Service实现（T5.3）
 * 财务创建审核 → 通过更新项目已核验收入 + 联动生成"已核验"收入确认
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeReceiptServiceImpl implements IFeeReceiptService {

    private final FeeReceiptMapper receiptMapper;
    private final FeeProjectMapper projectMapper;
    private final FeeIncomeConfirmationMapper confirmationMapper;
    private final IFeeProjectService projectService;
    private final IFeeNotificationService notificationService;

    @Override
    public TableDataInfo<FeeReceiptVo> queryPageList(FeeReceiptBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<FeeReceipt> wrapper = Wrappers.<FeeReceipt>lambdaQuery()
            .like(StrUtil.isNotBlank(bo.getReceiptNo()), FeeReceipt::getReceiptNo, bo.getReceiptNo())
            .eq(bo.getProjectId() != null, FeeReceipt::getProjectId, bo.getProjectId())
            .like(StrUtil.isNotBlank(bo.getPayer()), FeeReceipt::getPayer, bo.getPayer())
            .eq(StrUtil.isNotBlank(bo.getStatus()), FeeReceipt::getStatus, bo.getStatus())
            .orderByDesc(FeeReceipt::getCreateTime);
        Page<FeeReceiptVo> page = receiptMapper.selectVoPage(pageQuery.build(), wrapper);
        fillProjectName(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public FeeReceiptVo queryById(Long id) {
        FeeReceiptVo vo = receiptMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("收款单不存在");
        }
        fillProjectName(List.of(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertReceipt(FeeReceiptBo bo) {
        FeeProject project = projectMapper.selectById(bo.getProjectId());
        if (project == null) {
            throw new ServiceException("关联项目不存在");
        }
        if (!"ACTIVE".equals(project.getStatus())) {
            throw new ServiceException("仅进行中的项目可创建收款单");
        }
        FeeReceipt receipt = BeanCopyUtils.copy(bo, FeeReceipt.class);
        receipt.setReceiptNo(generateReceiptNo());
        receipt.setStatus("PENDING");
        if (receipt.getReceivedDate() == null) {
            receipt.setReceivedDate(new Date());
        }
        if (StrUtil.isBlank(receipt.getReceiptType())) {
            receipt.setReceiptType("PROJECT_PAYMENT");
        }
        receiptMapper.insert(receipt);
        return receipt.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int approveReceipt(Long id) {
        FeeReceipt receipt = receiptMapper.selectById(id);
        if (receipt == null) {
            throw new ServiceException("收款单不存在");
        }
        if (!"PENDING".equals(receipt.getStatus())) {
            throw new ServiceException("仅待审核状态的收款单可审核通过");
        }
        FeeReceipt update = new FeeReceipt();
        update.setId(id);
        update.setStatus("APPROVED");
        update.setApprovedBy(LoginHelper.getUserId());
        update.setApprovedAt(new Date());
        int rows = receiptMapper.updateById(update);

        // 1. 更新项目已核验收入
        projectService.addIncome(receipt.getProjectId(), receipt.getAmount(), true);

        // 2. 联动生成"已核验"收入确认（财务审核即核验）
        FeeIncomeConfirmation confirmation = new FeeIncomeConfirmation();
        confirmation.setProjectId(receipt.getProjectId());
        confirmation.setConfirmedAmount(receipt.getAmount());
        confirmation.setConfirmDate(receipt.getReceivedDate() != null ? receipt.getReceivedDate() : new Date());
        confirmation.setBasisType("RECEIPT");
        confirmation.setBasisNo(receipt.getReceiptNo());
        confirmation.setBasisUrls(receipt.getProofUrls());
        confirmation.setConfirmedBy(LoginHelper.getUserId());
        confirmation.setConfirmedByName(LoginHelper.getLoginUser().getNickName());
        confirmation.setVerifyStatus("VERIFIED");
        confirmation.setVerifyBy(LoginHelper.getUserId());
        confirmation.setVerifyAt(new Date());
        confirmationMapper.insert(confirmation);

        // 通知项目相关人（角色广播，T5.12）
        try {
            notificationService.notifyRole("finance", "收款单审核通过",
                "收款单【" + receipt.getReceiptNo() + "】已审核通过，金额 " + receipt.getAmount()
                    + " 元，已计入项目已核验收入并生成收入确认",
                "APPROVAL", "RECEIPT", id);
        } catch (Exception e) {
            log.warn("收款单审核通知发送失败（不阻断主流程）: {}", e.getMessage());
        }
        return rows;
    }

    @Override
    public int rejectReceipt(Long id, String reason) {
        FeeReceipt receipt = receiptMapper.selectById(id);
        if (receipt == null) {
            throw new ServiceException("收款单不存在");
        }
        if (!"PENDING".equals(receipt.getStatus())) {
            throw new ServiceException("仅待审核状态的收款单可驳回");
        }
        FeeReceipt update = new FeeReceipt();
        update.setId(id);
        update.setStatus("REJECTED");
        update.setApprovedBy(LoginHelper.getUserId());
        update.setApprovedAt(new Date());
        return receiptMapper.updateById(update);
    }

    /**
     * 生成收款单号 RCP-YYYYMMDD-XXX
     */
    private String generateReceiptNo() {
        String prefix = "RCP-" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "-";
        Long count = receiptMapper.selectCount(Wrappers.<FeeReceipt>lambdaQuery()
            .likeRight(FeeReceipt::getReceiptNo, prefix));
        return prefix + String.format("%03d", (count == null ? 0 : count) + 1);
    }

    /**
     * 批量填充项目名称
     */
    private void fillProjectName(List<FeeReceiptVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> projectIds = list.stream().map(FeeReceiptVo::getProjectId)
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
}
