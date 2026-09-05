package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.FeeComplianceConfig;
import com.ruoyi.system.domain.FeeComplianceRecord;
import com.ruoyi.system.mapper.FeeComplianceConfigMapper;
import com.ruoyi.system.service.IComplianceService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 发票合规配置Controller
 * 设计文档 Phase 3 Task 3.4
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/compliance")
public class ComplianceController extends BaseController {

    private final IComplianceService complianceService;
    private final FeeComplianceConfigMapper complianceConfigMapper;

    /**
     * 合规配置列表（按科目）
     */
    @SaCheckPermission("fee:compliance:list")
    @GetMapping("/config/list")
    public TableDataInfo<FeeComplianceConfig> configList(
            @RequestParam(required = false) String industryCode, PageQuery pageQuery) {
        Page<FeeComplianceConfig> page = complianceConfigMapper.selectPage(
            pageQuery.build(),
            Wrappers.<FeeComplianceConfig>lambdaQuery()
                .eq(industryCode != null, FeeComplianceConfig::getIndustryCode, industryCode)
                .orderByAsc(FeeComplianceConfig::getBudgetSubjectCode)
        );
        return TableDataInfo.build(page);
    }

    /**
     * 批量设置合规级别
     */
    @RequireFeature("FEE_COMPLIANCE_CUSTOM")
    @SaCheckPermission("fee:compliance:edit")
    @Log(title = "合规配置", businessType = BusinessType.UPDATE)
    @PostMapping("/config/batchSet")
    public R<Void> batchSet(@RequestBody BatchSetRequest request) {
        for (String code : request.getSubjectCodes()) {
            FeeComplianceConfig config = complianceConfigMapper.selectOne(
                Wrappers.<FeeComplianceConfig>lambdaQuery()
                    .eq(FeeComplianceConfig::getBudgetSubjectCode, code)
                    .eq(FeeComplianceConfig::getIndustryCode, request.getIndustryCode() != null ? request.getIndustryCode() : "*")
            );
            if (config != null) {
                config.setComplianceLevel(request.getLevel());
                complianceConfigMapper.updateById(config);
            } else {
                FeeComplianceConfig newConfig = new FeeComplianceConfig();
                newConfig.setBudgetSubjectCode(code);
                newConfig.setComplianceLevel(request.getLevel());
                newConfig.setIndustryCode(request.getIndustryCode() != null ? request.getIndustryCode() : "*");
                newConfig.setStatus("ENABLED");
                complianceConfigMapper.insert(newConfig);
            }
        }
        return R.ok();
    }

    /**
     * 执行合规校验
     */
    @PostMapping("/check/{billId}")
    public R<FeeComplianceRecord> check(@PathVariable Long billId,
                                         @RequestParam String billNo,
                                         @RequestParam String budgetSubjectCode,
                                         @RequestParam(required = false, defaultValue = "*") String industryCode) {
        return R.ok(complianceService.checkCompliance(billId, billNo, budgetSubjectCode, industryCode));
    }

    /**
     * 财务盖章确认
     */
    @SaCheckPermission("fee:compliance:seal")
    @Log(title = "合规盖章确认", businessType = BusinessType.UPDATE)
    @PostMapping("/confirmSeal/{recordId}")
    public R<Void> confirmSeal(@PathVariable Long recordId) {
        return toAjax(complianceService.confirmSeal(recordId));
    }

    /**
     * 查询报销单合规记录
     */
    @GetMapping("/record/{billId}")
    public R<FeeComplianceRecord> getRecord(@PathVariable Long billId) {
        return R.ok(complianceService.getRecordByBillId(billId));
    }

    /**
     * 批量设置请求体
     */
    @lombok.Data
    public static class BatchSetRequest {
        private List<String> subjectCodes;
        private String level;
        private String industryCode;
    }
}
