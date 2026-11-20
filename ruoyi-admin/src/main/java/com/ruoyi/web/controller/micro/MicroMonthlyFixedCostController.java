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
import com.ruoyi.system.domain.bo.MonthlyFixedCostBo;
import com.ruoyi.system.domain.vo.MonthlyFixedCostVo;
import com.ruoyi.system.service.IMonthlyFixedCostService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 固定支出管理Controller（T15.37，小微版 pro 档 boss PC 端）
 * <p>boss 管理每月固定支出项（房租/水电/工资等），首页盈亏三指标中「支出」= 现金流流水支出 + 本月固定支出合计。
 * <p>perms 对齐 M15_21 种子菜单：列表 micro:pro:cost:list；新增 micro:pro:cost:add；
 * 编辑 micro:pro:cost:edit；删除 micro:pro:cost:remove。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/cost")
public class MicroMonthlyFixedCostController extends BaseController {

    private final IMonthlyFixedCostService costService;

    /**
     * 固定支出列表（分页，支持按名称/分类/月份/启停过滤）
     */
    @SaCheckPermission("micro:pro:cost:list")
    @GetMapping("/list")
    public TableDataInfo<MonthlyFixedCostVo> list(MonthlyFixedCostBo bo, PageQuery pageQuery) {
        return costService.queryPageList(bo, pageQuery);
    }

    /**
     * 固定支出列表（不分页，小程序端用）
     */
    @SaCheckPermission("micro:pro:cost:list")
    @GetMapping("/all")
    public R<List<MonthlyFixedCostVo>> all(MonthlyFixedCostBo bo) {
        return R.ok(costService.queryList(bo));
    }

    /**
     * 固定支出详情
     */
    @SaCheckPermission("micro:pro:cost:list")
    @GetMapping("/{id}")
    public R<MonthlyFixedCostVo> getInfo(@PathVariable Long id) {
        return R.ok(costService.queryById(id));
    }

    /**
     * 新增固定支出项
     */
    @SaCheckPermission("micro:pro:cost:add")
    @Log(title = "固定支出", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody MonthlyFixedCostBo bo) {
        return R.ok(costService.insert(bo));
    }

    /**
     * 修改固定支出项
     */
    @SaCheckPermission("micro:pro:cost:edit")
    @Log(title = "固定支出", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody MonthlyFixedCostBo bo) {
        return toAjax(costService.update(bo));
    }

    /**
     * 删除固定支出项（逻辑删除，可批量）
     */
    @SaCheckPermission("micro:pro:cost:remove")
    @Log(title = "固定支出", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(costService.deleteByIds(Arrays.asList(ids)));
    }

    /**
     * ★T38.2 按月覆盖口径查询固定支出（含每月重复/周期分摊覆盖到该月的项），小程序月份切换用。
     *
     * @param month 目标月份（yyyy-MM），空则取当月
     */
    @SaCheckPermission("micro:pro:cost:list")
    @GetMapping("/activeList")
    public R<List<MonthlyFixedCostVo>> activeList(@RequestParam(required = false) String month) {
        String target = (month == null || month.trim().isEmpty())
            ? java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
            : month.trim();
        return R.ok(costService.listActiveInMonth(target));
    }

    /**
     * ★本月固定支出合计（盈亏三指标「支出」子项）
     */
    @SaCheckPermission("micro:pro:cost:list")
    @GetMapping("/monthSum")
    public R<BigDecimal> monthSum() {
        return R.ok(costService.sumCurrentMonth());
    }

    /**
     * ★T38.2「沿用上月」：把上月生效的一次性支出项一键复制到目标月（幂等跳过已存在/已自动覆盖项）。
     *
     * @param month 目标月份（yyyy-MM），空则取当月
     * @return 实际复制的支出项数
     */
    @SaCheckPermission("micro:pro:cost:add")
    @Log(title = "固定支出", businessType = BusinessType.INSERT)
    @PostMapping("/copyLastMonth")
    public R<Integer> copyLastMonth(@RequestParam(required = false) String month) {
        return R.ok(costService.copyFromPreviousMonth(month));
    }
}
