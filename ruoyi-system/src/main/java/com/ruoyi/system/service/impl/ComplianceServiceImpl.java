package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.FeeComplianceConfig;
import com.ruoyi.system.domain.FeeComplianceRecord;
import com.ruoyi.system.mapper.FeeComplianceConfigMapper;
import com.ruoyi.system.mapper.FeeComplianceRecordMapper;
import com.ruoyi.system.service.IComplianceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 发票合规校验Service业务层处理
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceServiceImpl implements IComplianceService {

    private final FeeComplianceConfigMapper complianceConfigMapper;
    private final FeeComplianceRecordMapper complianceRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FeeComplianceRecord checkCompliance(Long billId, String billNo, String budgetSubjectCode, String industryCode) {
        // 1. 查询该科目合规级别（先查行业专属，无则回退通用，再无则默认NORMAL）
        String requiredLevel = getComplianceLevel(budgetSubjectCode, industryCode);

        // 2. 智能识别票据类型（简化实现：默认FORMAL_INVOICE，实际应调用OCR）
        String invoiceType = detectInvoiceType(billId);

        // 3. 按级别判定
        String checkResult;
        String failReason = null;
        switch (requiredLevel) {
            case "SIMPLE":
                // 有附件即通过
                checkResult = "PASS";
                break;
            case "NORMAL":
                // RECEIPT 或 FORMAL_INVOICE 通过
                if ("FORMAL_INVOICE".equals(invoiceType) || "RECEIPT".equals(invoiceType)) {
                    checkResult = "PASS";
                } else {
                    checkResult = "FAIL";
                    failReason = "NORMAL级别要求至少提供收据或正式发票";
                }
                break;
            case "STRICT":
                // 必须 FORMAL_INVOICE + seal_confirmed
                if ("FORMAL_INVOICE".equals(invoiceType)) {
                    checkResult = "PENDING_SEAL"; // 等待财务盖章确认
                } else {
                    checkResult = "FAIL";
                    failReason = "STRICT级别要求提供正式发票";
                }
                break;
            default:
                checkResult = "PASS";
        }

        // 4. 写入 fee_compliance_record
        FeeComplianceRecord record = new FeeComplianceRecord();
        record.setBillId(billId);
        record.setBillNo(billNo);
        record.setInvoiceType(invoiceType);
        record.setRequiredLevel(requiredLevel);
        record.setActualLevel(invoiceType);
        record.setCheckResult(checkResult);
        record.setFailReason(failReason);
        record.setSealConfirmed(0);
        record.setCheckTime(new Date());
        complianceRecordMapper.insert(record);

        return record;
    }

    @Override
    public String getComplianceLevel(String budgetSubjectCode, String industryCode) {
        // 先查行业专属配置
        if (StrUtil.isNotBlank(industryCode) && !"*".equals(industryCode)) {
            FeeComplianceConfig config = complianceConfigMapper.selectOne(
                Wrappers.<FeeComplianceConfig>lambdaQuery()
                    .eq(FeeComplianceConfig::getBudgetSubjectCode, budgetSubjectCode)
                    .eq(FeeComplianceConfig::getIndustryCode, industryCode)
                    .eq(FeeComplianceConfig::getStatus, "ENABLED")
            );
            if (config != null) {
                return config.getComplianceLevel();
            }
        }
        // 回退通用配置
        FeeComplianceConfig config = complianceConfigMapper.selectOne(
            Wrappers.<FeeComplianceConfig>lambdaQuery()
                .eq(FeeComplianceConfig::getBudgetSubjectCode, budgetSubjectCode)
                .eq(FeeComplianceConfig::getIndustryCode, "*")
                .eq(FeeComplianceConfig::getStatus, "ENABLED")
        );
        return config != null ? config.getComplianceLevel() : "NORMAL";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean confirmSeal(Long recordId) {
        FeeComplianceRecord record = complianceRecordMapper.selectById(recordId);
        if (record == null) {
            throw new ServiceException("合规记录不存在");
        }
        if (!"PENDING_SEAL".equals(record.getCheckResult())) {
            throw new ServiceException("当前状态不允许盖章确认");
        }
        record.setSealConfirmed(1);
        record.setSealConfirmerId(LoginHelper.getUserId());
        record.setSealConfirmTime(new Date());
        record.setCheckResult("PASS");
        complianceRecordMapper.updateById(record);
        return true;
    }

    @Override
    public FeeComplianceRecord getRecordByBillId(Long billId) {
        return complianceRecordMapper.selectOne(
            Wrappers.<FeeComplianceRecord>lambdaQuery()
                .eq(FeeComplianceRecord::getBillId, billId)
                .orderByDesc(FeeComplianceRecord::getCheckTime)
                .last("LIMIT 1")
        );
    }

    /**
     * 智能识别票据类型（简化实现）
     * 实际应调用OCR服务或根据附件类型判断
     */
    private String detectInvoiceType(Long billId) {
        // 简化实现：默认返回FORMAL_INVOICE
        // TODO: 集成OCR服务后替换此逻辑
        return "FORMAL_INVOICE";
    }
}
