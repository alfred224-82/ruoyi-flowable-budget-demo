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
import com.ruoyi.system.domain.bo.StaffCommissionBo;
import com.ruoyi.system.domain.vo.StaffCommissionVo;
import com.ruoyi.system.service.IStaffCommissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 员工提成比例配置Controller（REQ-03，v1.3 需求清单，仅 PC）
 * <p>「员工提成」菜单（micro:staffCommission:*）：为每个员工配置提成比例 + 基础工资，
 * 员工当月薪酬成本 = 基础工资 + 提成（提成 = 当月已完成订单金额 × 比例）。
 * <p>★租户管理员（platform_admin）专属，与预约策略（MicroBookingPolicyController）同目录。
 * <p>★App 端本次不接入（仅 PC）。
 *
 * @author ruoyi
 * @date 2026-10-16
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/staffCommission")
public class MicroStaffCommissionController extends BaseController {

    private final IStaffCommissionService staffCommissionService;

    /**
     * 员工提成配置列表（分页，支持按姓名/状态过滤；含实时回填当月基数/提成/人力成本）
     */
    @SaCheckPermission("micro:staffCommission:list")
    @GetMapping("/list")
    public TableDataInfo<StaffCommissionVo> list(StaffCommissionBo bo, PageQuery pageQuery) {
        return staffCommissionService.queryPageList(bo, pageQuery);
    }

    /**
     * 员工提成配置详情
     */
    @SaCheckPermission("micro:staffCommission:list")
    @GetMapping("/{id}")
    public R<StaffCommissionVo> getInfo(@PathVariable Long id) {
        return R.ok(staffCommissionService.queryById(id));
    }

    /**
     * 新增员工提成配置
     */
    @SaCheckPermission("micro:staffCommission:add")
    @Log(title = "员工提成", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody StaffCommissionBo bo) {
        return R.ok(staffCommissionService.insert(bo));
    }

    /**
     * 修改员工提成配置
     */
    @SaCheckPermission("micro:staffCommission:edit")
    @Log(title = "员工提成", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody StaffCommissionBo bo) {
        return toAjax(staffCommissionService.update(bo));
    }

    /**
     * 删除员工提成配置（逻辑删除，可批量）
     */
    @SaCheckPermission("micro:staffCommission:remove")
    @Log(title = "员工提成", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(staffCommissionService.deleteByIds(Arrays.asList(ids)));
    }

    /**
     * 导出员工提成配置 Excel
     */
    @SaCheckPermission("micro:staffCommission:remove")
    @Log(title = "员工提成", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(StaffCommissionBo bo, HttpServletResponse response) {
        List<StaffCommissionVo> list = staffCommissionService.queryList(bo);
        ExcelUtil.exportExcel(list, "员工提成", StaffCommissionVo.class, response);
    }

    /**
     * 员工下拉选项（新增/编辑选择器用；本租户 salesman/助教）
     */
    @SaCheckPermission("micro:staffCommission:list")
    @GetMapping("/staffOptions")
    public R<List<Map<String, Object>>> staffOptions() {
        return R.ok(staffCommissionService.listStaffOptions());
    }

    /**
     * ★试算某员工某月提成（新增/编辑弹窗预览用）
     *
     * @param staffUserId 员工用户ID
     * @param month       月份（yyyy-MM，缺省为当月）
     */
    @SaCheckPermission("micro:staffCommission:list")
    @GetMapping("/calc")
    public R<BigDecimal> calc(@RequestParam Long staffUserId,
                              @RequestParam(required = false) String month) {
        return R.ok(staffCommissionService.calcMonthlyCommission(staffUserId, month));
    }
}
