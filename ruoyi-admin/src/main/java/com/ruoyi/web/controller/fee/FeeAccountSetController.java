package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.FeeAccountSetBo;
import com.ruoyi.system.domain.vo.FeeAccountSetReportVo;
import com.ruoyi.system.domain.vo.FeeAccountSetVo;
import com.ruoyi.system.service.IFeeAccountSetService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 多账套管理Controller（T7.7 / FEAT_MULTI_ACCOUNT_SET）
 * <p>专业版专属：账套 CRUD + 默认账套 + 切换 + 合并报表。
 * 类级 @RequireFeature 拦截非专业版租户（防串第②层：接口层）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/accountSet")
@RequireFeature("FEAT_MULTI_ACCOUNT_SET")
public class FeeAccountSetController extends BaseController {

    private final IFeeAccountSetService accountSetService;

    /**
     * 账套分页列表
     */
    @SaCheckPermission("fee:accountSet:list")
    @GetMapping("/list")
    public TableDataInfo<FeeAccountSetVo> list(FeeAccountSetBo bo, PageQuery pageQuery) {
        Page<FeeAccountSetVo> page = accountSetService.queryPageList(bo, pageQuery);
        return TableDataInfo.build(page);
    }

    /**
     * 全部账套（下拉/切换用）
     */
    @SaCheckPermission("fee:accountSet:list")
    @GetMapping("/listAll")
    public R<List<FeeAccountSetVo>> listAll() {
        return R.ok(accountSetService.listAll());
    }

    /**
     * 账套详情
     */
    @SaCheckPermission("fee:accountSet:query")
    @GetMapping("/{id}")
    public R<FeeAccountSetVo> getInfo(@PathVariable Long id) {
        return R.ok(accountSetService.queryById(id));
    }

    /**
     * 新增账套
     */
    @SaCheckPermission("fee:accountSet:add")
    @Log(title = "账套管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody FeeAccountSetBo bo) {
        return R.ok(accountSetService.insertByBo(bo));
    }

    /**
     * 修改账套
     */
    @SaCheckPermission("fee:accountSet:edit")
    @Log(title = "账套管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody FeeAccountSetBo bo) {
        return toAjax(accountSetService.updateByBo(bo));
    }

    /**
     * 删除账套（有单据归属或为默认账套时禁止）
     */
    @SaCheckPermission("fee:accountSet:remove")
    @Log(title = "账套管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(accountSetService.deleteByIds(Arrays.asList(ids)));
    }

    /**
     * 设为默认账套
     */
    @SaCheckPermission("fee:accountSet:edit")
    @Log(title = "账套管理", businessType = BusinessType.UPDATE)
    @PutMapping("/default/{id}")
    public R<Void> setDefault(@PathVariable Long id) {
        return toAjax(accountSetService.setDefault(id));
    }

    /**
     * 切换当前操作账套上下文
     */
    @SaCheckPermission("fee:accountSet:switch")
    @Log(title = "账套切换", businessType = BusinessType.UPDATE)
    @PutMapping("/switch/{id}")
    public R<Void> switchAccountSet(@PathVariable Long id) {
        return toAjax(accountSetService.switchAccountSet(id));
    }

    /**
     * 合并报表：各账套已审批单据汇总（集团视角）
     */
    @SaCheckPermission("fee:accountSet:mergeReport")
    @GetMapping("/consolidatedReport")
    public R<List<FeeAccountSetReportVo>> consolidatedReport(@RequestParam(required = false) String period) {
        return R.ok(accountSetService.consolidatedReport(period));
    }
}
