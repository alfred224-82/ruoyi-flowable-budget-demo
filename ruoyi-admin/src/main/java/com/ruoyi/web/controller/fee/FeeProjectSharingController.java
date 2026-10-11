package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.FeeProjectSharing;
import com.ruoyi.system.domain.vo.FeeProjectSharingVo;
import com.ruoyi.system.service.IFeeProjectSharingService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 项目分成Controller（T5.9）
 * 项目经理申请→老板定比例审批→生效计入分成→标记已支付
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/sharing")
@RequireFeature("FEAT_HIDDEN_COST")
public class FeeProjectSharingController extends BaseController {

    private final IFeeProjectSharingService sharingService;

    /**
     * 分成列表
     */
    @SaCheckPermission("fee:sharing:list")
    @GetMapping("/list")
    public TableDataInfo<FeeProjectSharingVo> list(
        @RequestParam(required = false) Long projectId,
        @RequestParam(required = false) String status,
        PageQuery pageQuery) {
        return sharingService.queryPageList(projectId, status, pageQuery);
    }

    /**
     * 分成详情
     */
    @SaCheckPermission("fee:sharing:query")
    @GetMapping("/{id}")
    public R<FeeProjectSharingVo> getInfo(@PathVariable Long id) {
        return R.ok(sharingService.queryById(id));
    }

    /**
     * 申请分成（项目经理，状态=待审批）
     */
    @SaCheckPermission("fee:sharing:edit")
    @Log(title = "项目分成申请", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@RequestBody FeeProjectSharing sharing) {
        return R.ok(sharingService.insertSharing(sharing));
    }

    /**
     * 老板审批通过（设定比例与基数，计算分成金额）
     */
    @SaCheckPermission("fee:sharing:edit")
    @Log(title = "项目分成审批", businessType = BusinessType.UPDATE)
    @PutMapping("/approve/{id}")
    public R<Void> approve(@PathVariable Long id,
                           @RequestParam BigDecimal ratio,
                           @RequestParam BigDecimal baseAmount) {
        return toAjax(sharingService.approveSharing(id, ratio, baseAmount));
    }

    /**
     * 老板驳回（删除待审批记录）
     */
    @SaCheckPermission("fee:sharing:edit")
    @Log(title = "项目分成审批", businessType = BusinessType.UPDATE)
    @PutMapping("/reject/{id}")
    public R<Void> reject(@PathVariable Long id) {
        return toAjax(sharingService.rejectSharing(id));
    }

    /**
     * 标记已支付
     */
    @SaCheckPermission("fee:sharing:edit")
    @Log(title = "项目分成支付", businessType = BusinessType.UPDATE)
    @PutMapping("/pay/{id}")
    public R<Void> pay(@PathVariable Long id) {
        return toAjax(sharingService.paySharing(id));
    }
}
