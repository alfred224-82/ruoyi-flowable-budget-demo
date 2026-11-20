package com.ruoyi.web.controller.fee;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.ExpenseBill;
import com.ruoyi.system.mapper.ExpenseBillMapper;
import com.ruoyi.system.service.IInvoiceVerifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 发票验真独立 API（T5.18，高级版专属）
 * <p>
 * 报销提交流程内已自动触发验真（{@link com.ruoyi.system.service.impl.ExpenseBillServiceImpl}），
 * 本 Controller 提供独立的「手动验真」「发票查重」接口供前端按需调用（如编辑页实时校验）。
 * <p>
 * 功能开关：FEAT_INVOICE_VERIFY（高级版+），类级 @RequireFeature 拦截。
 *
 * @author ruoyi
 * @date 2026-09-10
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/fee/invoice/verify")
@RequiredArgsConstructor
@RequireFeature("FEAT_INVOICE_VERIFY")
public class InvoiceVerifyController extends BaseController {

    private final IInvoiceVerifyService invoiceVerifyService;
    private final ExpenseBillMapper expenseBillMapper;

    /**
     * 手动触发单张报销单的发票验真
     *
     * @param billId 报销单ID
     * @return 验真结果（VERIFIED / MISMATCH / UNVERIFIED）
     */
    @Log(title = "发票验真", businessType = BusinessType.OTHER)
    @PostMapping("/{billId}")
    public R<String> verify(@PathVariable Long billId) {
        ExpenseBill bill = expenseBillMapper.selectById(billId);
        if (bill == null) {
            return R.fail("报销单不存在");
        }
        if (StrUtil.isBlank(bill.getInvoiceNo())) {
            return R.ok("未填写发票号，跳过验真", IInvoiceVerifyService.STATUS_UNVERIFIED);
        }
        try {
            invoiceVerifyService.verify(bill);
            // 回写验真状态
            expenseBillMapper.updateById(bill);
            return R.ok("发票验真通过", bill.getInvoiceVerifyStatus());
        } catch (ServiceException e) {
            // 回写异常状态
            expenseBillMapper.updateById(bill);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 发票号查重预检（不修改数据，仅查询）
     *
     * @param invoiceNo 发票号码
     * @param excludeBillId 排除的报销单ID（编辑场景排除自身）
     * @return true=无重复 / false=已存在
     */
    @GetMapping("/duplicate")
    public R<Boolean> checkDuplicate(@RequestParam String invoiceNo,
                                     @RequestParam(required = false) Long excludeBillId) {
        if (StrUtil.isBlank(invoiceNo)) {
            return R.ok("发票号为空，无需查重", true);
        }
        Long dupCount = expenseBillMapper.countByInvoiceNo(invoiceNo, excludeBillId);
        boolean noDup = dupCount == null || dupCount == 0;
        String msg = noDup ? "发票号无重复" : "发票号已被其他报销单使用";
        return R.ok(msg, noDup);
    }
}
