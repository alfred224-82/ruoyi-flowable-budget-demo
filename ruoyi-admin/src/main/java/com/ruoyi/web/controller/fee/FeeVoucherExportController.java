package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.vo.FeeExportTemplateVo;
import com.ruoyi.system.service.IFeeVoucherExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 凭证导出Controller（T5.11）
 * 选月 + 选模板（内置7套）+ 选范围 → 借贷两行 → Excel
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/export")
@RequireFeature("FEAT_VOUCHER_EXPORT")
public class FeeVoucherExportController extends BaseController {

    private final IFeeVoucherExportService exportService;

    /**
     * 模板列表（内置7套 + 租户自定义）
     */
    @SaCheckPermission("fee:export:query")
    @GetMapping("/templates")
    public R<List<FeeExportTemplateVo>> templates() {
        return R.ok(exportService.listTemplates());
    }

    /**
     * 执行凭证导出（Excel下载）
     *
     * @param month      导出月份 yyyy-MM（必选）
     * @param templateId 模板ID（必选）
     * @param projectId  项目范围（可空=全部，0=公司级）
     */
    @SaCheckPermission("fee:export:run")
    @Log(title = "凭证导出", businessType = BusinessType.EXPORT)
    @GetMapping("/run")
    public void run(@RequestParam String month,
                    @RequestParam Long templateId,
                    @RequestParam(required = false) Long projectId,
                    HttpServletResponse response) throws IOException {
        byte[] data = exportService.exportVouchers(month, templateId, projectId);
        String fileName = URLEncoder.encode("凭证导出_" + month + ".xlsx", StandardCharsets.UTF_8.name())
            .replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName);
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }

    /**
     * 二维导出：凭证导出 + 附加项目×科目矩阵 Sheet（T7.3 / FEAT_EXPORT_2D）
     *
     * @param month      导出月份 yyyy-MM（必选）
     * @param templateId 模板ID（必选）
     * @param projectId  项目范围（可空=全部，0=公司级）
     */
    @RequireFeature("FEAT_EXPORT_2D")
    @SaCheckPermission("fee:export:run2d")
    @Log(title = "凭证二维导出", businessType = BusinessType.EXPORT)
    @GetMapping("/run2d")
    public void run2d(@RequestParam String month,
                      @RequestParam Long templateId,
                      @RequestParam(required = false) Long projectId,
                      HttpServletResponse response) throws IOException {
        byte[] data = exportService.exportVouchers2d(month, templateId, projectId);
        String fileName = URLEncoder.encode("凭证二维导出_" + month + ".xlsx", StandardCharsets.UTF_8.name())
            .replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName);
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }
}
