package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.bo.FeeWelfareConfigBo;
import com.ruoyi.system.domain.vo.FeeWelfareConfigVo;
import com.ruoyi.system.service.IFeeWelfareConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 公司福利政策配置Controller
 * 设计文档 Phase 17 Task 17.4
 *
 * @author ruoyi
 * @date 2026-08-01
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/welfare")
public class FeeWelfareConfigController extends BaseController {

    private final IFeeWelfareConfigService welfareConfigService;

    /**
     * 福利配置列表
     */
    @RequireFeature("FEE_WELFARE_CONFIG")
    @SaCheckPermission("fee:welfare:list")
    @GetMapping("/list")
    public TableDataInfo<FeeWelfareConfigVo> list(FeeWelfareConfigBo bo, PageQuery pageQuery) {
        return welfareConfigService.queryPageList(bo, pageQuery);
    }

    /**
     * 福利配置详情
     */
    @RequireFeature("FEE_WELFARE_CONFIG")
    @SaCheckPermission("fee:welfare:query")
    @GetMapping("/{id}")
    public R<FeeWelfareConfigVo> getInfo(@PathVariable Long id) {
        return R.ok(welfareConfigService.queryById(id));
    }

    /**
     * 新增福利配置
     */
    @RequireFeature("FEE_WELFARE_CONFIG")
    @SaCheckPermission("fee:welfare:add")
    @Log(title = "福利配置", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody FeeWelfareConfigBo bo) {
        return toAjax(welfareConfigService.insert(bo));
    }

    /**
     * 编辑福利配置
     */
    @RequireFeature("FEE_WELFARE_CONFIG")
    @SaCheckPermission("fee:welfare:edit")
    @Log(title = "福利配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody FeeWelfareConfigBo bo) {
        return toAjax(welfareConfigService.update(bo));
    }

    /**
     * 删除福利配置
     */
    @RequireFeature("FEE_WELFARE_CONFIG")
    @SaCheckPermission("fee:welfare:remove")
    @Log(title = "福利配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(welfareConfigService.deleteByIds(ids));
    }

    /**
     * 导入福利配置
     */
    @RequireFeature("FEE_WELFARE_CONFIG")
    @SaCheckPermission("fee:welfare:import")
    @Log(title = "福利配置", businessType = BusinessType.IMPORT)
    @PostMapping("/import")
    public R<Void> importData(@RequestParam("file") MultipartFile file) throws Exception {
        List<FeeWelfareConfigBo> list = ExcelUtil.importExcel(file.getInputStream(), FeeWelfareConfigBo.class);
        int count = welfareConfigService.importWelfare(list);
        return R.ok("成功导入" + count + "条福利配置");
    }

    /**
     * 导出福利配置
     */
    @RequireFeature("FEE_WELFARE_CONFIG")
    @SaCheckPermission("fee:welfare:export")
    @Log(title = "福利配置", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public void export(FeeWelfareConfigBo bo, HttpServletResponse response) {
        List<FeeWelfareConfigVo> list = welfareConfigService.exportWelfare(bo);
        ExcelUtil.exportExcel(list, "福利配置数据", FeeWelfareConfigVo.class, response);
    }

    /**
     * 匹配福利标准（根据出差信息）
     */
    @RequireFeature("FEE_WELFARE_CONFIG")
    @SaCheckPermission("fee:welfare:list")
    @GetMapping("/match")
    public R<List<FeeWelfareConfigVo>> match(
            @RequestParam String region,
            @RequestParam @NotNull(message = "出差天数不能为空") Integer tripDays,
            @RequestParam(required = false) String cityLevel,
            @RequestParam(required = false) String countryGroup) {
        return R.ok(welfareConfigService.matchWelfare(region, tripDays, cityLevel, countryGroup));
    }
}
