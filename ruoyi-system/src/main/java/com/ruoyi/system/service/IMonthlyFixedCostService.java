package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.MonthlyFixedCostBo;
import com.ruoyi.system.domain.vo.MonthlyFixedCostVo;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * 每月固定支出服务接口（T15.37，小微版 pro 档 boss PC 端管理）
 * <p>boss 管理每月固定支出项（房租/水电/工资等），首页盈亏三指标中「支出」= 现金流流水支出 + 本月固定支出合计。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
public interface IMonthlyFixedCostService {

    /**
     * 分页查询固定支出项（按 tenant_id 自动隔离，支持按名称/分类/月份/启停过滤）
     */
    TableDataInfo<MonthlyFixedCostVo> queryPageList(MonthlyFixedCostBo bo, PageQuery pageQuery);

    /**
     * 查询全部固定支出项（不分页，用于小程序端展示）
     */
    List<MonthlyFixedCostVo> queryList(MonthlyFixedCostBo bo);

    /**
     * 查询固定支出详情
     */
    MonthlyFixedCostVo queryById(Long id);

    /**
     * 新增固定支出项
     *
     * @return 支出项ID
     */
    Long insert(MonthlyFixedCostBo bo);

    /**
     * 修改固定支出项
     */
    int update(MonthlyFixedCostBo bo);

    /**
     * 逻辑删除固定支出项（可批量）
     */
    int deleteByIds(Collection<Long> ids);

    /**
     * ★聚合本月活跃项的每月分摊成本合计（盈亏三指标「支出」子项，T30.6 分摊口径）
     * <p>口径：启用项中覆盖当月的记录，按 每月分摊成本 = 总额 ÷ 周期总月数 累加；
     * 无周期规则项按 is_recurring 决定全额计入当月或每月重复计入。
     *
     * @return 本月固定支出合计金额（分摊后）
     */
    BigDecimal sumCurrentMonth();

    /**
     * 查询指定月份活跃的固定支出项（T30.6 周期分摊覆盖口径）
     * <p>覆盖规则：有周期规则 → [生效月, 生效月+分摊月数-1] 区间内每月计入分摊额；
     * 无周期规则 → is_recurring=1 自生效月起每月全额计入，=0 仅生效月计入。
     * <p>返回 Vo 已回填 monthlyAmount（每月分摊成本）/ amortizeMonths / categoryName（标签名）。
     *
     * @param month 月份（yyyy-MM）
     * @return 当月活跃支出项列表（空集合不返回 null）
     */
    List<MonthlyFixedCostVo> listActiveInMonth(String month);

    /**
     * 指定月份活跃固定支出项数（固定支出异常判定口径，T30.6）
     *
     * @param month 月份（yyyy-MM）
     * @return 当月活跃项数
     */
    long countActiveInMonth(String month);

    /**
     * ★T38.2「沿用上月」：把上月生效的一次性支出项复制到目标月，幂等跳过已存在项。
     * <p>已按月自动覆盖目标月的重复项（is_recurring=1 或周期规则仍覆盖）不复制，避免重复计入；
     * 目标月已存在同名同分类项亦跳过。
     *
     * @param month 目标月份（yyyy-MM），空则取当月
     * @return 实际复制的支出项数
     */
    int copyFromPreviousMonth(String month);
}
