package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.bo.CheckInBo;
import com.ruoyi.system.domain.bo.CheckInSupplementBo;
import com.ruoyi.system.domain.vo.CheckInConfigVo;
import com.ruoyi.system.domain.vo.CheckInRecordVo;
import com.ruoyi.system.service.ICheckInService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;

/**
 * 员工打卡管理 Controller（T24.1，小微版 M24）
 * <p>两端接入：Boss PC 管理 / 员工 MP 打卡。
 * <p>类级 {@code @RequireFeature("FEAT_CHECK_IN")} 版本开关控制；
 * micro_lite 免费版不启用，micro_pro 收费版启用。
 * <p>★三级开关链路：industry_template.check_in_enabled → sys_tenant.check_in_enabled → check_in_config.enabled
 * <p>★打卡不受活动锁定影响，锁定后仍可打卡。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Validated
@RequireFeature("FEAT_CHECK_IN")
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/checkIn")
public class MicroCheckInController extends BaseController {

    private final ICheckInService checkInService;

    // ============================================================
    // 员工小程序（T24.4）
    // ============================================================

    /**
     * 员工打卡提交（拍照+时间+地点+水印）
     */
    @SaCheckPermission("micro:checkIn:submit")
    @Log(title = "员工打卡-提交", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> submit(@Validated(AddGroup.class) @RequestBody CheckInBo bo) {
        return R.ok(checkInService.submitCheckIn(bo));
    }

    /**
     * 员工查看本人打卡记录
     */
    @SaCheckPermission("micro:checkIn:my")
    @GetMapping("/my")
    public R<List<CheckInRecordVo>> my(
        @RequestParam(required = false) Long activityId,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return R.ok(checkInService.myRecords(activityId, startDate, endDate));
    }

    // ============================================================
    // Boss PC 管理端（T24.5）
    // ============================================================

    /**
     * 活动打卡列表（Boss 查看指定活动的打卡记录）
     */
    @SaCheckPermission("micro:checkIn:view")
    @GetMapping("/activity/{activityId}")
    public R<List<CheckInRecordVo>> activityRecords(
        @PathVariable Long activityId,
        @RequestParam(required = false) Long staffUserId) {
        return R.ok(checkInService.activityRecords(activityId, staffUserId));
    }

    /**
     * Boss 补录打卡（代员工补录，员工忘打时）
     */
    @SaCheckPermission("micro:checkIn:supplement")
    @Log(title = "打卡管理-补录", businessType = BusinessType.INSERT)
    @PostMapping("/supplement")
    public R<Long> supplement(@Validated(AddGroup.class) @RequestBody CheckInSupplementBo bo) {
        return R.ok(checkInService.supplementCheckIn(bo));
    }

    /**
     * 导出打卡记录 Excel
     */
    @SaCheckPermission("micro:checkIn:export")
    @Log(title = "打卡管理-导出", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(
        @RequestParam(required = false) Long activityId,
        @RequestParam(required = false) Long staffUserId,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
        HttpServletResponse response) {
        List<CheckInRecordVo> list = checkInService.exportRecords(activityId, staffUserId, startDate, endDate);
        ExcelUtil.exportExcel(list, "打卡记录", CheckInRecordVo.class, response);
    }

    // ============================================================
    // 打卡配置
    // ============================================================

    /**
     * 获取打卡配置
     */
    @SaCheckPermission("micro:checkIn:view")
    @GetMapping("/config")
    public R<CheckInConfigVo> getConfig() {
        return R.ok(checkInService.getConfig());
    }

    /**
     * 更新打卡配置（Boss 设置拍照要求/迟到阈值）
     */
    @SaCheckPermission("micro:checkIn:view")
    @Log(title = "打卡管理-配置", businessType = BusinessType.UPDATE)
    @PutMapping("/config")
    public R<Void> updateConfig(@RequestBody CheckInConfigVo configVo) {
        checkInService.updateConfig(configVo);
        return R.ok();
    }

    /**
     * 查询三级开关状态（前端判断是否显示打卡入口）
     */
    @SaCheckPermission("micro:checkIn:submit")
    @GetMapping("/enabled")
    public R<Boolean> enabled() {
        return R.ok(checkInService.isCheckInEnabled());
    }
}
