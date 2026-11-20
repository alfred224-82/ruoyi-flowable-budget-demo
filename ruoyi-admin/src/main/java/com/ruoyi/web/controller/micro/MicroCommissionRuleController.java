package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.ActivityStaffSnapshot;
import com.ruoyi.system.domain.bo.StaffCommissionRuleBo;
import com.ruoyi.system.domain.vo.ActivityPricingVo;
import com.ruoyi.system.domain.vo.EstimateVsActualVo;
import com.ruoyi.system.domain.vo.StaffCommissionRuleVo;
import com.ruoyi.system.service.IActivityStaffSnapshotService;
import com.ruoyi.system.service.IStaffCommissionRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 员工分成规则 Controller（M35 T35.2/T35.4/T35.6/T35.7，仅 PC）
 * <p>「分成规则」菜单（micro:commissionRule:*）：为每个员工配置分成规则（三种类型），
 * 活动创建/编辑时按当前规则生成快照（T35.3），定价页实时试算预估利润与保本人数（T35.4），
 * 预估 vs 实际报表（T35.7）。
 * <p>★与 M15 REQ-03「员工提成」（MicroStaffCommissionController，月度薪酬口径）独立：
 *   本 Controller 面向单场活动结算分账，快照不可变；REQ-03 面向月底薪+提成汇总。
 * <p>★App 端本次不接入（仅 PC）。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/commissionRule")
public class MicroCommissionRuleController extends BaseController {

    private final IStaffCommissionRuleService ruleService;
    private final IActivityStaffSnapshotService snapshotService;

    /**
     * 分成规则列表（分页，支持按员工姓名/类型/启用状态过滤）
     */
    @SaCheckPermission("micro:commissionRule:list")
    @GetMapping("/list")
    public TableDataInfo<StaffCommissionRuleVo> list(StaffCommissionRuleBo bo, PageQuery pageQuery) {
        return ruleService.queryPageList(bo, pageQuery);
    }

    /**
     * 分成规则详情
     */
    @SaCheckPermission("micro:commissionRule:list")
    @GetMapping("/{id}")
    public R<StaffCommissionRuleVo> getInfo(@PathVariable Long id) {
        return R.ok(ruleService.queryById(id));
    }

    /**
     * 新增分成规则
     */
    @SaCheckPermission("micro:commissionRule:add")
    @Log(title = "员工分成规则", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody StaffCommissionRuleBo bo) {
        return R.ok(ruleService.insert(bo));
    }

    /**
     * 修改分成规则
     */
    @SaCheckPermission("micro:commissionRule:edit")
    @Log(title = "员工分成规则", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody StaffCommissionRuleBo bo) {
        return toAjax(ruleService.update(bo));
    }

    /**
     * 删除分成规则（逻辑删除，可批量）
     */
    @SaCheckPermission("micro:commissionRule:remove")
    @Log(title = "员工分成规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(ruleService.deleteByIds(Arrays.asList(ids)));
    }

    /**
     * 导出分成规则 Excel
     */
    @SaCheckPermission("micro:commissionRule:export")
    @Log(title = "员工分成规则", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(StaffCommissionRuleBo bo, HttpServletResponse response) {
        List<StaffCommissionRuleVo> list = ruleService.queryList(bo);
        ExcelUtil.exportExcel(list, "员工分成规则", StaffCommissionRuleVo.class, response);
    }

    /**
     * 员工下拉选项（新增/编辑选择器用；本租户 salesman/助教）
     */
    @SaCheckPermission("micro:commissionRule:list")
    @GetMapping("/staffOptions")
    public R<List<Map<String, Object>>> staffOptions() {
        return R.ok(ruleService.listStaffOptions());
    }

    // ==================== T35.4 定价页自动算账 ====================

    /**
     * 定价试算：按已选员工 + 售价 + 预计人数 + 其他成本实时计算预估利润与保本人数。
     * <p>活动创建/编辑页 onChange 调用；不落库，纯试算。
     *
     * @param staffIds          已选员工ID列表（逗号分隔）
     * @param unitPrice         单人售价
     * @param expectedHeadcount 预计人数
     * @param otherCost         其他成本
     */
    @SaCheckPermission("micro:commissionRule:list")
    @GetMapping("/calcPricing")
    public R<ActivityPricingVo> calcPricing(
            @RequestParam(required = false) List<Long> staffIds,
            @RequestParam(required = false) BigDecimal unitPrice,
            @RequestParam(required = false) Integer expectedHeadcount,
            @RequestParam(required = false) BigDecimal otherCost) {
        return R.ok(ruleService.calcPricing(staffIds, unitPrice, expectedHeadcount, otherCost));
    }

    // ==================== T35.3 活动快照查询 ====================

    /**
     * 查询某活动的员工分成快照列表（活动详情页「本场员工成本」展示用）。
     */
    @SaCheckPermission("micro:commissionRule:list")
    @GetMapping("/snapshot/{activityId}")
    public R<List<ActivityStaffSnapshot>> snapshots(@PathVariable Long activityId) {
        return R.ok(snapshotService.listByActivityId(activityId));
    }

    /**
     * 汇总某活动的员工成本（快照 calculated_amount 之和）。
     */
    @SaCheckPermission("micro:commissionRule:list")
    @GetMapping("/snapshot/{activityId}/sumEmployeeCost")
    public R<BigDecimal> sumEmployeeCost(@PathVariable Long activityId) {
        return R.ok(snapshotService.sumEmployeeCost(activityId));
    }

    // ==================== T35.7 预估 vs 实际报表 ====================

    /**
     * 预估 vs 实际报表：按时间范围查询活动列表，返回预计利润 + 实际利润 + 差额。
     * <p>★M36 活动结算单未交付前 actualProfit 为 null，差额留空。
     *
     * @param beginTime 起始时间（yyyy-MM-dd）
     * @param endTime   结束时间（yyyy-MM-dd）
     * @param status    活动状态过滤（可空）
     */
    @SaCheckPermission("micro:commissionRule:report")
    @GetMapping("/estimateVsActual")
    public R<List<EstimateVsActualVo>> estimateVsActual(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date beginTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endTime,
            @RequestParam(required = false) String status) {
        return R.ok(ruleService.queryEstimateVsActual(beginTime, endTime, status));
    }

    /**
     * 导出预估 vs 实际报表 Excel
     */
    @SaCheckPermission("micro:commissionRule:report")
    @Log(title = "预估vs实际报表", businessType = BusinessType.EXPORT)
    @PostMapping("/estimateVsActual/export")
    public void exportEstimateVsActual(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date beginTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endTime,
            @RequestParam(required = false) String status,
            HttpServletResponse response) {
        List<EstimateVsActualVo> list = ruleService.queryEstimateVsActual(beginTime, endTime, status);
        ExcelUtil.exportExcel(list, "预估vs实际报表", EstimateVsActualVo.class, response);
    }
}
