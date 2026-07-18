package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.CostCenterBo;
import com.ruoyi.system.domain.vo.CostCenterVo;
import com.ruoyi.system.service.ICostCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

/**
 * 责任中心管理Controller
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/costCenter")
public class CostCenterController extends BaseController {

    private final ICostCenterService costCenterService;

    /**
     * 查询责任中心列表
     */
    @SaCheckPermission("fee:costCenter:list")
    @GetMapping("/list")
    public TableDataInfo<CostCenterVo> list(CostCenterBo bo, PageQuery pageQuery) {
        Page<CostCenterVo> page = costCenterService.selectPageList(bo, pageQuery);
        return TableDataInfo.build(page);
    }

    /**
     * 获取责任中心详细信息
     */
    @SaCheckPermission("fee:costCenter:query")
    @GetMapping("/{id}")
    public R<CostCenterVo> getInfo(@PathVariable Long id) {
        return R.ok(costCenterService.queryById(id));
    }

    /**
     * 新增责任中心
     */
    @SaCheckPermission("fee:costCenter:add")
    @Log(title = "责任中心", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody CostCenterBo bo) {
        return toAjax(costCenterService.insertByBo(bo));
    }

    /**
     * 修改责任中心
     */
    @SaCheckPermission("fee:costCenter:edit")
    @Log(title = "责任中心", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody CostCenterBo bo) {
        return toAjax(costCenterService.updateByBo(bo));
    }

    /**
     * 删除责任中心
     */
    @SaCheckPermission("fee:costCenter:remove")
    @Log(title = "责任中心", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(costCenterService.deleteWithValidByIds(Arrays.asList(ids)));
    }

    /**
     * 设置责任中心-部门关联
     */
    @SaCheckPermission("fee:costCenter:edit")
    @Log(title = "责任中心部门关联", businessType = BusinessType.UPDATE)
    @PostMapping("/deptMapping/{centerCode}")
    public R<Void> deptMapping(@PathVariable String centerCode, @RequestBody List<Long> deptIds) {
        return toAjax(costCenterService.saveDeptMapping(centerCode, deptIds));
    }

    /**
     * 查询责任中心下的部门列表
     */
    @SaCheckPermission("fee:costCenter:query")
    @GetMapping("/deptList/{centerCode}")
    public R<List<SysDept>> deptList(@PathVariable String centerCode) {
        return R.ok(costCenterService.queryDeptList(centerCode));
    }
}
