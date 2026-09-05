package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.vo.FeeProfitTimelineVo;
import com.ruoyi.system.domain.vo.FeeProjectProfitVo;
import com.ruoyi.system.service.IFeeProfitReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 项目利润报表Controller（T6.5 / FEAT_PROFIT_REPORT）
 * 纸面/实际分离、利润拆解、收支时间线、进度条、按项目/月导出
 * 数据范围：项目经理仅本人参与项目
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/profitReport")
public class FeeProfitReportController extends BaseController {

    private final IFeeProfitReportService profitReportService;

    /**
     * 利润报表分页列表
     */
    @RequireFeature("FEAT_PROFIT_REPORT")
    @SaCheckPermission("fee:profitReport:list")
    @GetMapping("/list")
    public TableDataInfo<FeeProjectProfitVo> list(@RequestParam(required = false) String projectName,
                                                  @RequestParam(required = false) String status,
                                                  PageQuery pageQuery) {
        return profitReportService.reportList(projectName, status, pageQuery);
    }

    /**
     * 单项目利润详情（利润拆解）
     */
    @RequireFeature("FEAT_PROFIT_REPORT")
    @SaCheckPermission("fee:profitReport:query")
    @GetMapping("/{projectId}")
    public R<FeeProjectProfitVo> detail(@PathVariable Long projectId) {
        return R.ok(profitReportService.reportDetail(projectId));
    }

    /**
     * 项目收支时间线（合同→收入确认→付款→收款）
     */
    @RequireFeature("FEAT_PROFIT_REPORT")
    @SaCheckPermission("fee:profitReport:query")
    @GetMapping("/timeline/{projectId}")
    public R<List<FeeProfitTimelineVo>> timeline(@PathVariable Long projectId) {
        return R.ok(profitReportService.timeline(projectId));
    }

    /**
     * 导出利润报表（按项目/按月）
     *
     * @param projectId 可空=数据范围内全部项目
     * @param month     可空；非空时附加当月收支明细 Sheet（yyyy-MM）
     */
    @RequireFeature("FEAT_PROFIT_REPORT")
    @SaCheckPermission("fee:profitReport:query")
    @Log(title = "项目利润报表", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public void export(@RequestParam(required = false) Long projectId,
                       @RequestParam(required = false) String month,
                       HttpServletResponse response) throws IOException {
        byte[] data = profitReportService.exportReport(projectId, month);
        String fileName = URLEncoder.encode("项目利润报表" + (month != null ? "_" + month : "") + ".xlsx",
                StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName);
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }
}
