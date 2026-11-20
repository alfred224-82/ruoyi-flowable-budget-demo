package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.bo.ActivitySettlementBo;
import com.ruoyi.system.domain.vo.ActivitySettlementVo;
import com.ruoyi.system.service.IActivitySettlementService;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 活动结算单Controller（M36 T36.1~T36.6，小微版 H5 界面开发任务文档 v1.2）
 * <p>老板端结算单页面（7.11）+ 员工端只读页面（8.5）共用本 Controller，
 * 权限通过 @SaCheckPermission 与角色判断收窄。
 * <p>perms：
 * <ul>
 *   <li>micro:settlement:list   — 查看结算单（老板/员工）</li>
 *   <li>micro:settlement:save   — 保存草稿（老板）</li>
 *   <li>micro:settlement:submit — 提交结算（老板）</li>
 *   <li>micro:settlement:export — 导出CSV（老板）</li>
 *   <li>micro:settlement:adjust — 追加调整单（老板）</li>
 * </ul>
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/settlement")
@RequireFeature("FEAT_PROMOTION_DISTRIBUTION")
public class MicroActivitySettlementController extends BaseController {

    private final IActivitySettlementService settlementService;

    /**
     * ★T36.3 按活动ID查询结算单（老板端结算单页面数据聚合）
     * <p>不存在则自动初始化草稿（含行业模板收入项 + 员工成本快照）。
     */
    @SaCheckPermission("micro:settlement:list")
    @GetMapping("/activity/{activityId}")
    public R<ActivitySettlementVo> getByActivity(@PathVariable Long activityId) {
        return R.ok(settlementService.getByActivityId(activityId));
    }

    /**
     * 按结算单ID查询
     */
    @SaCheckPermission("micro:settlement:list")
    @GetMapping("/{id}")
    public R<ActivitySettlementVo> getById(@PathVariable Long id) {
        return R.ok(settlementService.getById(id));
    }

    /**
     * ★T36.4 员工端只读视图（权限收窄：仅可见自己那条员工成本行；合计利润区不可见）
     * <p>无 perms 校验，登录即可（员工/老板均可调用，服务端按当前登录用户过滤）。
     */
    @GetMapping("/staff/activity/{activityId}")
    public R<ActivitySettlementVo> getStaffView(@PathVariable Long activityId) {
        Long userId = LoginHelper.getUserId();
        return R.ok(settlementService.getStaffReadonlyView(activityId, userId));
    }

    /**
     * ★T36.5 保存草稿（仅 DRAFT 状态可编辑）
     */
    @SaCheckPermission("micro:settlement:save")
    @Log(title = "活动结算单", businessType = BusinessType.UPDATE)
    @PostMapping("/saveDraft")
    public R<Long> saveDraft(@Validated @RequestBody ActivitySettlementBo bo) {
        return R.ok(settlementService.saveDraft(bo));
    }

    /**
     * ★T36.5 提交结算（DRAFT→SUBMITTED）
     * <p>校验：① 金额不得为负 ② 收入合计为 0 禁止提交 ③ 代收未配代付 → 日志告警不阻断
     */
    @SaCheckPermission("micro:settlement:submit")
    @Log(title = "活动结算单", businessType = BusinessType.UPDATE)
    @PostMapping("/submit/{id}")
    public R<Void> submit(@PathVariable Long id) {
        settlementService.submit(id);
        return R.ok();
    }

    /**
     * ★T36.5 锁定结算（SUBMITTED→LOCKED）
     */
    @SaCheckPermission("micro:settlement:submit")
    @Log(title = "活动结算单", businessType = BusinessType.UPDATE)
    @PostMapping("/lock/{id}")
    public R<Void> lock(@PathVariable Long id) {
        settlementService.lock(id);
        return R.ok();
    }

    /**
     * ★T36.5 追加调整单（锁定后发现错误 → 新建调整单，不覆盖原报表）
     */
    @SaCheckPermission("micro:settlement:adjust")
    @Log(title = "活动结算单", businessType = BusinessType.INSERT)
    @PostMapping("/adjustment/{originId}")
    public R<Long> createAdjustment(@PathVariable Long originId, @RequestBody(required = false) ActivitySettlementBo bo) {
        return R.ok(settlementService.createAdjustment(originId, bo));
    }

    /**
     * ★T36.2 按行业模板初始化收入项（前端创建活动时调用，渲染默认收入项列表）
     */
    @GetMapping("/industryTemplate/{industryCode}")
    public R<List<ActivitySettlementBo.SettlementIncomeItemBo>> industryTemplate(@PathVariable String industryCode) {
        return R.ok(settlementService.initIncomeItemsByIndustry(industryCode));
    }

    /**
     * ★T36.1 重新计算员工成本（活动选人变更 / 员工缺席删除后调用）
     */
    @SaCheckPermission("micro:settlement:save")
    @Log(title = "活动结算单", businessType = BusinessType.UPDATE)
    @PostMapping("/recalculateEmployeeCost/{settlementId}")
    public R<Void> recalculateEmployeeCost(@PathVariable Long settlementId) {
        settlementService.recalculateEmployeeCost(settlementId);
        return R.ok();
    }

    /**
     * ★T36.6 收工时追加消费提醒判断（前端结算页 onShow 调用）
     */
    @GetMapping("/needAddonReminder/{settlementId}")
    public R<Boolean> needAddonReminder(@PathVariable Long settlementId) {
        return R.ok(settlementService.needAddonReminder(settlementId));
    }

    /**
     * ★T36.3 导出结算单 CSV
     */
    @SaCheckPermission("micro:settlement:export")
    @Log(title = "活动结算单", businessType = BusinessType.EXPORT)
    @GetMapping("/export/{id}")
    public void export(@PathVariable Long id, HttpServletResponse response) throws IOException {
        String csv = settlementService.exportCsv(id);
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=settlement_" + id + ".csv");
        // ★BOM 头，Excel 打开中文不乱码
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        PrintWriter writer = new PrintWriter(response.getOutputStream(), true, StandardCharsets.UTF_8);
        writer.write(csv);
        writer.flush();
    }
}
