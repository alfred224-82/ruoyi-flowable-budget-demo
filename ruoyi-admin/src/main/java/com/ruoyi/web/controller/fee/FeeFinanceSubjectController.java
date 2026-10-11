package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.excel.ExcelResult;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.vo.FeeFinanceSubjectImportVo;
import com.ruoyi.system.domain.vo.FeeFinanceSubjectVo;
import com.ruoyi.system.service.IFeeFinanceSubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * 财务科目导入Controller（T5.16 / 设计文档 2.2 #1）
 * 客户自有财务科目导入 + 关键字自动匹配建立映射A + 人工补完
 *
 * @author ruoyi
 * @date 2026-08-30
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/financeSubject")
@RequireFeature("FEAT_SUBJECT_3LAYER")
public class FeeFinanceSubjectController extends BaseController {

    private final IFeeFinanceSubjectService financeSubjectService;

    /**
     * 财务科目列表（分页，含映射A结果）
     */
    @SaCheckPermission("fee:financeSubject:list")
    @GetMapping("/list")
    public TableDataInfo<FeeFinanceSubjectVo> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sourceSystem,
            @RequestParam(required = false) String keyword,
            PageQuery pageQuery) {
        return financeSubjectService.queryPageList(status, sourceSystem, keyword, pageQuery);
    }

    /**
     * 导入客户财务科目（金蝶/用友等导出文件，导入后自动关键字匹配）
     */
    @SaCheckPermission("fee:financeSubject:import")
    @Log(title = "财务科目导入", businessType = BusinessType.IMPORT)
    @PostMapping(value = "/importData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Void> importData(@RequestPart("file") MultipartFile file,
                              @RequestParam(required = false, defaultValue = "GENERAL") String sourceSystem) throws Exception {
        ExcelResult<FeeFinanceSubjectImportVo> excelResult = ExcelUtil.importExcel(
            file.getInputStream(), FeeFinanceSubjectImportVo.class, true);
        String message = financeSubjectService.importSubjects(excelResult.getList(), sourceSystem);
        return R.ok(message);
    }

    /**
     * 下载导入模板
     */
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) {
        ExcelUtil.exportExcel(new ArrayList<>(), "财务科目导入模板", FeeFinanceSubjectImportVo.class, response);
    }

    /**
     * 关键字自动匹配（对所有未映射科目建立映射A）
     */
    @SaCheckPermission("fee:financeSubject:match")
    @Log(title = "财务科目自动匹配", businessType = BusinessType.UPDATE)
    @PostMapping("/match")
    public R<Integer> match() {
        return R.ok("自动匹配完成", financeSubjectService.autoMatch());
    }

    /**
     * 人工补完/纠偏映射A
     */
    @SaCheckPermission("fee:financeSubject:map")
    @Log(title = "财务科目人工映射", businessType = BusinessType.UPDATE)
    @PostMapping("/map/{id}")
    public R<Void> map(@PathVariable Long id, @RequestParam String mappedSubjectCode) {
        financeSubjectService.manualMap(id, mappedSubjectCode);
        return R.ok();
    }

    /**
     * 忽略科目（不参与映射）
     */
    @SaCheckPermission("fee:financeSubject:map")
    @Log(title = "财务科目忽略", businessType = BusinessType.UPDATE)
    @PutMapping("/ignore/{id}")
    public R<Void> ignore(@PathVariable Long id) {
        return toAjax(financeSubjectService.ignore(id));
    }

    /**
     * 删除导入的财务科目
     */
    @SaCheckPermission("fee:financeSubject:remove")
    @Log(title = "财务科目删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(financeSubjectService.deleteByIds(Arrays.asList(ids)));
    }
}
