package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.FeeCrossDeptAllocationLog;
import com.ruoyi.system.service.ICrossDeptAllocService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 跨部门分摊Controller
 * 设计文档 Phase 11 Task 11.4
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/crossAlloc")
public class CrossDeptAllocController extends BaseController {

    private final ICrossDeptAllocService crossDeptAllocService;

    /**
     * 手动触发跨部门分摊
     */
    @RequireFeature("FEE_CROSS_DEPT_ALLOCATE")
    @SaCheckPermission("fee:crossAlloc:execute")
    @Log(title = "跨部门分摊", businessType = BusinessType.OTHER)
    @PostMapping("/execute")
    public R<Integer> execute(
            @RequestParam String period,
            @RequestParam Long sourceDeptId,
            @RequestParam(required = false, defaultValue = "HEADCOUNT") String basis) {
        return R.ok(crossDeptAllocService.executeAllocation(period, sourceDeptId, basis));
    }

    /**
     * 分摊记录查询
     */
    @RequireFeature("FEE_CROSS_DEPT_ALLOCATE")
    @SaCheckPermission("fee:crossAlloc:list")
    @GetMapping("/logs")
    public TableDataInfo<FeeCrossDeptAllocationLog> logs(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Long sourceDeptId,
            PageQuery pageQuery) {
        Page<FeeCrossDeptAllocationLog> page = crossDeptAllocService.listLogs(period, sourceDeptId, pageQuery);
        return TableDataInfo.build(page);
    }
}
