package com.ruoyi.report.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.report.domain.dto.ReportGenerateDto;
import com.ruoyi.report.domain.vo.ReportRecordVo;
import com.ruoyi.report.service.IAiReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * AI报表生成控制器
 *
 * @author ruoyi
 */
@Validated
@RestController
@RequestMapping("/ai/report")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai", name = "enabled", havingValue = "true")
public class AiReportController extends BaseController {

    private final IAiReportService aiReportService;

    /**
     * 生成指定类型报表
     */
    @SaCheckPermission("system:preparation:query")
    @PostMapping("/generate")
    public R<ReportRecordVo> generateReport(@Valid @RequestBody ReportGenerateDto dto) {
        ReportRecordVo result = aiReportService.generateReport(dto);
        return R.ok(result);
    }

    /**
     * 查询报表生成历史
     */
    @SaCheckPermission("system:preparation:query")
    @GetMapping("/history")
    public TableDataInfo<ReportRecordVo> queryReportHistory(
        @RequestParam(required = false) String reportType,
        PageQuery pageQuery) {
        return aiReportService.queryReportHistory(reportType, pageQuery);
    }

    /**
     * 获取报表详情
     */
    @SaCheckPermission("system:preparation:query")
    @GetMapping("/detail/{id}")
    public R<ReportRecordVo> getReportDetail(@PathVariable Long id) {
        return R.ok(aiReportService.getReportDetail(id));
    }

    /**
     * 删除报表记录
     */
    @SaCheckPermission("system:preparation:query")
    @DeleteMapping("/{id}")
    public R<Void> deleteReport(@PathVariable Long id) {
        return toAjax(aiReportService.deleteReport(id));
    }

    /**
     * 获取报表类型列表
     */
    @SaCheckPermission("system:preparation:query")
    @GetMapping("/types")
    public R<List<Map<String, String>>> getReportTypes() {
        return R.ok(aiReportService.getReportTypes());
    }
}
