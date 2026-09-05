package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.FeeReceiptBo;
import com.ruoyi.system.domain.vo.FeeReceiptVo;
import com.ruoyi.system.service.IFeeReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 收款单管理Controller（T5.3）
 * 必选项目、财务创建审核、联动生成"已核验"收入确认
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/receipt")
public class FeeReceiptController extends BaseController {

    private final IFeeReceiptService receiptService;

    /**
     * 收款单列表
     */
    @SaCheckPermission("fee:receipt:list")
    @GetMapping("/list")
    public TableDataInfo<FeeReceiptVo> list(FeeReceiptBo bo, PageQuery pageQuery) {
        return receiptService.queryPageList(bo, pageQuery);
    }

    /**
     * 收款单详情
     */
    @SaCheckPermission("fee:receipt:query")
    @GetMapping("/{id}")
    public R<FeeReceiptVo> getInfo(@PathVariable Long id) {
        return R.ok(receiptService.queryById(id));
    }

    /**
     * 财务创建收款单
     */
    @SaCheckPermission("fee:receipt:add")
    @Log(title = "收款单", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody FeeReceiptBo bo) {
        return R.ok(receiptService.insertReceipt(bo));
    }

    /**
     * 审核通过（更新项目收入 + 联动生成已核验收入确认）
     */
    @SaCheckPermission("fee:receipt:approve")
    @Log(title = "收款单审核", businessType = BusinessType.UPDATE)
    @PutMapping("/approve/{id}")
    public R<Void> approve(@PathVariable Long id) {
        return toAjax(receiptService.approveReceipt(id));
    }

    /**
     * 审核驳回
     */
    @SaCheckPermission("fee:receipt:approve")
    @Log(title = "收款单审核", businessType = BusinessType.UPDATE)
    @PutMapping("/reject/{id}")
    public R<Void> reject(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return toAjax(receiptService.rejectReceipt(id, reason));
    }
}
