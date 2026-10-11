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
import com.ruoyi.system.domain.FeeBudgetTrackConfig;
import com.ruoyi.system.mapper.FeeBudgetTrackConfigMapper;
import com.ruoyi.system.service.ITrackControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 三轨预算管控Controller
 * 设计文档 Phase 5 Task 5.2
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/track")
@RequireFeature("FEAT_EXPENSE_CONTROL")
public class TrackControlController extends BaseController {

    private final ITrackControlService trackControlService;
    private final FeeBudgetTrackConfigMapper trackConfigMapper;

    /**
     * 三轨管控配置列表
     */
    @SaCheckPermission("fee:track:list")
    @GetMapping("/config/list")
    public TableDataInfo<FeeBudgetTrackConfig> configList(
            @RequestParam(required = false) String industryCode, PageQuery pageQuery) {
        Page<FeeBudgetTrackConfig> page = trackConfigMapper.selectPage(
            pageQuery.build(),
            Wrappers.<FeeBudgetTrackConfig>lambdaQuery()
                .eq(industryCode != null, FeeBudgetTrackConfig::getIndustryCode, industryCode)
                .orderByAsc(FeeBudgetTrackConfig::getBudgetSubjectCode)
        );
        return TableDataInfo.build(page);
    }

    /**
     * 三轨管控校验（前端提交报销前调用）
     */
    @PostMapping("/check")
    public R<ITrackControlService.TrackCheckResult> checkTrack(
            @RequestParam String budgetSubjectCode,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false, defaultValue = "*") String industryCode) {
        return R.ok(trackControlService.checkTrack(budgetSubjectCode, amount, industryCode));
    }

    /**
     * 查询科目所属轨道
     */
    @GetMapping("/type")
    public R<String> getTrackType(
            @RequestParam String budgetSubjectCode,
            @RequestParam(required = false, defaultValue = "*") String industryCode) {
        return R.ok(trackControlService.getTrackType(budgetSubjectCode, industryCode));
    }

    /**
     * 批量配置科目轨道
     */
    @RequireFeature("FEE_TRACK_CONTROL")
    @SaCheckPermission("fee:track:edit")
    @Log(title = "三轨管控配置", businessType = BusinessType.UPDATE)
    @PostMapping("/config/batchSet")
    public R<Void> batchConfig(@RequestBody BatchTrackRequest request) {
        for (String code : request.getSubjectCodes()) {
            FeeBudgetTrackConfig config = trackConfigMapper.selectOne(
                Wrappers.<FeeBudgetTrackConfig>lambdaQuery()
                    .eq(FeeBudgetTrackConfig::getBudgetSubjectCode, code)
                    .eq(FeeBudgetTrackConfig::getTrackType, request.getTrackType())
                    .eq(FeeBudgetTrackConfig::getIndustryCode, request.getIndustryCode() != null ? request.getIndustryCode() : "*")
            );
            if (config != null) {
                config.setControlMode(request.getControlMode());
                if (request.getWarningThreshold() != null) {
                    config.setWarningThreshold(request.getWarningThreshold());
                }
                trackConfigMapper.updateById(config);
            } else {
                FeeBudgetTrackConfig newConfig = new FeeBudgetTrackConfig();
                newConfig.setBudgetSubjectCode(code);
                newConfig.setTrackType(request.getTrackType());
                newConfig.setControlMode(request.getControlMode());
                newConfig.setWarningThreshold(request.getWarningThreshold() != null ? request.getWarningThreshold() : new BigDecimal("80.00"));
                newConfig.setIndustryCode(request.getIndustryCode() != null ? request.getIndustryCode() : "*");
                newConfig.setStatus("ENABLED");
                trackConfigMapper.insert(newConfig);
            }
        }
        return R.ok();
    }

    @lombok.Data
    public static class BatchTrackRequest {
        private List<String> subjectCodes;
        private String trackType;
        private String controlMode;
        private BigDecimal warningThreshold;
        private String industryCode;
    }
}
