package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.FeeOperationLog;
import com.ruoyi.system.service.IFeeOperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 费用管控操作日志Controller
 *
 * @author ruoyi
 * @date 2026-07-15
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/operationLog")
@RequireFeature("FEAT_EXPENSE_CONTROL")
public class FeeOperationLogController extends BaseController {

    private final IFeeOperationLogService feeOperationLogService;

    /**
     * 查询操作日志列表
     */
    @SaCheckPermission("fee:operationLog:list")
    @GetMapping("/list")
    public TableDataInfo<FeeOperationLog> list(FeeOperationLog query, PageQuery pageQuery) {
        Page<FeeOperationLog> page = feeOperationLogService.selectPageList(query, pageQuery);
        return TableDataInfo.build(page);
    }

    /**
     * 查询单据操作时间线
     */
    @GetMapping("/timeline/{billNo}")
    public R<List<FeeOperationLog>> timeline(@PathVariable String billNo) {
        return R.ok(feeOperationLogService.selectTimeline(billNo));
    }
}
