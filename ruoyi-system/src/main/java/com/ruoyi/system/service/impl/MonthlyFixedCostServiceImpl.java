package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.FixedCostTag;
import com.ruoyi.system.domain.MonthlyFixedCost;
import com.ruoyi.system.domain.PeriodRule;
import com.ruoyi.system.domain.bo.MonthlyFixedCostBo;
import com.ruoyi.system.domain.vo.MonthlyFixedCostVo;
import com.ruoyi.system.mapper.FixedCostTagMapper;
import com.ruoyi.system.mapper.MonthlyFixedCostMapper;
import com.ruoyi.system.mapper.PeriodRuleMapper;
import com.ruoyi.system.service.IMonthlyFixedCostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 每月固定支出服务实现（T15.37，小微版 pro 档 boss PC 端管理）
 * <p>monthly_fixed_cost 为租户级表，多租户插件自动隔离。
 * <p>T30.6 周期分摊口径：关联周期规则的支出项，每月分摊成本 = 总额 ÷ 周期总月数
 * （如 5 年 50 万 → 500000 ÷ (5×12) = 8333.33/月），并在覆盖月区间内每月计入；
 * 未关联周期规则的项按 is_recurring 全额计入。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyFixedCostServiceImpl implements IMonthlyFixedCostService {

    private final MonthlyFixedCostMapper costMapper;
    private final PeriodRuleMapper periodRuleMapper;
    private final FixedCostTagMapper tagMapper;

    /** 分类显示名映射 */
    private static final Map<String, String> CATEGORY_NAME_MAP = new HashMap<String, String>() {{
        put("rent", "房租");
        put("utility", "水电");
        put("staff_salary", "员工工资");
        put("staff_reward", "员工提成");
        put("other", "其他");
    }};

    @Override
    public TableDataInfo<MonthlyFixedCostVo> queryPageList(MonthlyFixedCostBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<MonthlyFixedCost> wrapper = buildQueryWrapper(bo);
        Page<MonthlyFixedCostVo> page = costMapper.selectVoPage(pageQuery.build(), wrapper);
        fillExtraInfo(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public List<MonthlyFixedCostVo> queryList(MonthlyFixedCostBo bo) {
        LambdaQueryWrapper<MonthlyFixedCost> wrapper = buildQueryWrapper(bo);
        List<MonthlyFixedCostVo> list = costMapper.selectVoList(wrapper);
        fillExtraInfo(list);
        return list;
    }

    @Override
    public MonthlyFixedCostVo queryById(Long id) {
        MonthlyFixedCostVo vo = costMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("固定支出项不存在");
        }
        fillSingleExtraInfo(vo, loadTagNameMap());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insert(MonthlyFixedCostBo bo) {
        MonthlyFixedCost entity = BeanCopyUtils.copy(bo, MonthlyFixedCost.class);
        entity.setId(null);
        if (entity.getIsRecurring() == null) {
            entity.setIsRecurring(1);
        }
        if (entity.getEnabled() == null) {
            entity.setEnabled(1);
        }
        costMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(MonthlyFixedCostBo bo) {
        MonthlyFixedCost exist = costMapper.selectById(bo.getId());
        if (exist == null) {
            throw new ServiceException("固定支出项不存在");
        }
        MonthlyFixedCost update = BeanCopyUtils.copy(bo, MonthlyFixedCost.class);
        return costMapper.updateById(update);
    }

    @Override
    public int deleteByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        return costMapper.deleteBatchIds(ids);
    }

    @Override
    public BigDecimal sumCurrentMonth() {
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return listActiveInMonth(currentMonth).stream()
            .map(MonthlyFixedCostVo::getMonthlyAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public List<MonthlyFixedCostVo> listActiveInMonth(String month) {
        if (StrUtil.isBlank(month)) {
            throw new ServiceException("月份不能为空");
        }
        // 生效月 ≤ 当月的启用项才可能覆盖当月（租户隔离由多租户插件自动追加）
        List<MonthlyFixedCost> list = costMapper.selectList(Wrappers.<MonthlyFixedCost>lambdaQuery()
            .le(MonthlyFixedCost::getEffectiveMonth, month)
            .eq(MonthlyFixedCost::getEnabled, 1));
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Map<Long, PeriodRule> ruleMap = loadRuleMap(list);
        Map<String, String> tagNameMap = loadTagNameMap();
        List<MonthlyFixedCostVo> result = new ArrayList<>();
        for (MonthlyFixedCost cost : list) {
            PeriodRule rule = cost.getPeriodRuleId() != null ? ruleMap.get(cost.getPeriodRuleId()) : null;
            if (!coversMonth(cost, rule, month)) {
                continue;
            }
            MonthlyFixedCostVo vo = BeanCopyUtils.copy(cost, MonthlyFixedCostVo.class);
            fillVoExtra(vo, rule, tagNameMap);
            result.add(vo);
        }
        return result;
    }

    @Override
    public long countActiveInMonth(String month) {
        return listActiveInMonth(month).size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int copyFromPreviousMonth(String month) {
        String target = StrUtil.isNotBlank(month) ? month.trim()
            : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String prev;
        try {
            prev = YearMonth.parse(target).minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (Exception e) {
            throw new ServiceException("目标月份格式非法（应为 yyyy-MM）: " + month);
        }
        // 上月生效的支出项
        List<MonthlyFixedCost> prevItems = costMapper.selectList(Wrappers.<MonthlyFixedCost>lambdaQuery()
            .eq(MonthlyFixedCost::getEffectiveMonth, prev));
        if (CollUtil.isEmpty(prevItems)) {
            return 0;
        }
        // 目标月已存在项（同名同分类去重）
        List<MonthlyFixedCost> targetItems = costMapper.selectList(Wrappers.<MonthlyFixedCost>lambdaQuery()
            .eq(MonthlyFixedCost::getEffectiveMonth, target));
        Set<String> existKeys = new HashSet<>();
        if (targetItems != null) {
            for (MonthlyFixedCost t : targetItems) {
                existKeys.add(costKey(t));
            }
        }
        Map<Long, PeriodRule> ruleMap = loadRuleMap(prevItems);
        int copied = 0;
        for (MonthlyFixedCost item : prevItems) {
            PeriodRule rule = item.getPeriodRuleId() != null ? ruleMap.get(item.getPeriodRuleId()) : null;
            // ★已自动覆盖目标月的重复项（每月重复/周期分摊仍覆盖）不复制，避免重复计入
            if (coversMonth(item, rule, target)) {
                continue;
            }
            String key = costKey(item);
            if (existKeys.contains(key)) {
                continue;
            }
            MonthlyFixedCost copy = new MonthlyFixedCost();
            copy.setTenantId(item.getTenantId());
            copy.setName(item.getName());
            copy.setCategory(item.getCategory());
            copy.setAmount(item.getAmount());
            copy.setIsRecurring(item.getIsRecurring());
            copy.setPeriodRuleId(item.getPeriodRuleId());
            copy.setEnabled(item.getEnabled() != null ? item.getEnabled() : 1);
            copy.setRemark(item.getRemark());
            copy.setEffectiveMonth(target);
            costMapper.insert(copy);
            existKeys.add(key);
            copied++;
        }
        log.info("[FixedCost] 沿用上月完成: prev={}, target={}, copied={}", prev, target, copied);
        return copied;
    }

    /** 去重键：名称|分类 */
    private static String costKey(MonthlyFixedCost c) {
        return (c.getName() != null ? c.getName() : "") + "|" + (c.getCategory() != null ? c.getCategory() : "");
    }

    // ===== 私有方法 =====

    private LambdaQueryWrapper<MonthlyFixedCost> buildQueryWrapper(MonthlyFixedCostBo bo) {
        return Wrappers.<MonthlyFixedCost>lambdaQuery()
            .like(StrUtil.isNotBlank(bo.getName()), MonthlyFixedCost::getName, bo.getName())
            .eq(StrUtil.isNotBlank(bo.getCategory()), MonthlyFixedCost::getCategory, bo.getCategory())
            .eq(StrUtil.isNotBlank(bo.getEffectiveMonth()), MonthlyFixedCost::getEffectiveMonth, bo.getEffectiveMonth())
            .eq(bo.getEnabled() != null, MonthlyFixedCost::getEnabled, bo.getEnabled())
            .orderByDesc(MonthlyFixedCost::getEffectiveMonth)
            .orderByDesc(MonthlyFixedCost::getCreateTime);
    }

    /** 填充分类名 + 周期规则名 + 每月分摊成本 */
    private void fillExtraInfo(List<MonthlyFixedCostVo> records) {
        if (CollUtil.isEmpty(records)) {
            return;
        }
        Map<String, String> tagNameMap = loadTagNameMap();
        for (MonthlyFixedCostVo vo : records) {
            fillSingleExtraInfo(vo, tagNameMap);
        }
    }

    /** 填充单条记录的扩展信息 */
    private void fillSingleExtraInfo(MonthlyFixedCostVo vo, Map<String, String> tagNameMap) {
        PeriodRule rule = null;
        if (vo.getPeriodRuleId() != null) {
            rule = periodRuleMapper.selectById(vo.getPeriodRuleId());
            if (rule != null) {
                vo.setPeriodRuleName(rule.getRuleName());
            }
        }
        fillVoExtra(vo, rule, tagNameMap);
    }

    /** 回填分类显示名 + 每月分摊成本 + 分摊月数（T30.6） */
    private void fillVoExtra(MonthlyFixedCostVo vo, PeriodRule rule, Map<String, String> tagNameMap) {
        String category = vo.getCategory();
        String categoryName = null;
        if (StrUtil.isNotBlank(category)) {
            // 1) 优先从标签映射表查找（tagCode / id 字符串）
            categoryName = tagNameMap.get(category);
            // 2) 兜底：category 可能是零填充的标签 ID（如 "0001" → id=1），尝试按 ID 查库
            if (categoryName == null) {
                try {
                    Long tagId = Long.parseLong(category);
                    FixedCostTag tag = tagMapper.selectById(tagId);
                    if (tag != null && StrUtil.isNotBlank(tag.getTagName())) {
                        categoryName = tag.getTagName();
                    }
                } catch (NumberFormatException ignored) {
                    // 非数字 category，跳过 ID 查找
                }
            }
            // 3) 硬编码分类名兜底
            if (categoryName == null) {
                categoryName = CATEGORY_NAME_MAP.get(category);
            }
        }
        vo.setCategoryName(categoryName != null ? categoryName : category);
        vo.setMonthlyAmount(monthlyAmount(vo.getAmount(), rule));
        vo.setAmortizeMonths(amortizeMonths(rule));
    }

    /** 批量加载周期规则（避免 N+1） */
    private Map<Long, PeriodRule> loadRuleMap(List<MonthlyFixedCost> list) {
        List<Long> ruleIds = list.stream()
            .map(MonthlyFixedCost::getPeriodRuleId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        if (CollUtil.isEmpty(ruleIds)) {
            return Collections.emptyMap();
        }
        List<PeriodRule> rules = periodRuleMapper.selectBatchIds(ruleIds);
        if (CollUtil.isEmpty(rules)) {
            return Collections.emptyMap();
        }
        return rules.stream().collect(Collectors.toMap(PeriodRule::getId, Function.identity(), (a, b) -> a));
    }

    /** 加载分类标签名映射（tagCode → tagName，兼用 id 字符串兼容存量数据） */
    private Map<String, String> loadTagNameMap() {
        List<FixedCostTag> tags = tagMapper.selectList(null);
        Map<String, String> map = new HashMap<>();
        if (tags != null) {
            for (FixedCostTag tag : tags) {
                if (tag.getId() == null) {
                    continue;
                }
                if (StrUtil.isNotBlank(tag.getTagCode())) {
                    map.put(tag.getTagCode(), tag.getTagName());
                }
                map.put(String.valueOf(tag.getId()), tag.getTagName());
            }
        }
        return map;
    }

    // ===== T30.6 周期分摊计算 =====

    /**
     * 周期规则换算总月数（如 5 年 → 60 个月）
     * <p>YEAR×12 / QUARTER×3 / MONTH×1 精确换算；WEEK×7÷30 / DAY÷30 近似换算。
     *
     * @return 总月数；规则缺失/数值非法/单位未知返回 null（视为不分摊）
     */
    private static BigDecimal periodTotalMonths(PeriodRule rule) {
        if (rule == null || rule.getPeriodValue() == null
            || rule.getPeriodValue().compareTo(BigDecimal.ZERO) <= 0 || rule.getPeriodUnit() == null) {
            return null;
        }
        BigDecimal value = rule.getPeriodValue();
        switch (rule.getPeriodUnit()) {
            case "YEAR":
                return value.multiply(BigDecimal.valueOf(12));
            case "QUARTER":
                return value.multiply(BigDecimal.valueOf(3));
            case "MONTH":
                return value;
            case "WEEK":
                return value.multiply(BigDecimal.valueOf(7)).divide(BigDecimal.valueOf(30), 4, RoundingMode.HALF_UP);
            case "DAY":
                return value.divide(BigDecimal.valueOf(30), 4, RoundingMode.HALF_UP);
            default:
                return null;
        }
    }

    /**
     * 每月分摊成本 = 总额 ÷ 周期总月数（如 500000 ÷ 60 = 8333.33）
     * <p>无周期规则或周期 ≤ 1 个月时不分摊，全额计入。
     */
    private static BigDecimal monthlyAmount(BigDecimal amount, PeriodRule rule) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalMonths = periodTotalMonths(rule);
        if (totalMonths == null || totalMonths.compareTo(BigDecimal.ONE) <= 0) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.divide(totalMonths, 2, RoundingMode.HALF_UP);
    }

    /**
     * 分摊月数（展示 + 覆盖区间用）：总月数向上取整，最小 1；无周期规则或周期 ≤ 1 个月返回 0（不分摊）
     */
    private static int amortizeMonths(PeriodRule rule) {
        BigDecimal totalMonths = periodTotalMonths(rule);
        if (totalMonths == null || totalMonths.compareTo(BigDecimal.ONE) <= 0) {
            return 0;
        }
        return Math.max(1, totalMonths.setScale(0, RoundingMode.CEILING).intValue());
    }

    /**
     * 判断支出项是否覆盖指定月份（T30.6 覆盖口径）
     * <p>有周期规则：[生效月, 生效月+分摊月数-1] 区间内每月计入分摊额；
     * 无周期规则：is_recurring=1 自生效月起每月全额计入，=0 仅生效月计入。
     */
    private static boolean coversMonth(MonthlyFixedCost cost, PeriodRule rule, String month) {
        String effective = cost.getEffectiveMonth();
        if (StrUtil.isBlank(effective) || month.compareTo(effective) < 0) {
            return false;
        }
        if (rule == null) {
            return Integer.valueOf(1).equals(cost.getIsRecurring()) || month.equals(effective);
        }
        int months = amortizeMonths(rule);
        String endMonth = plusMonths(effective, Math.max(1, months) - 1);
        return month.compareTo(endMonth) <= 0;
    }

    /** 生效月份（yyyy-MM）加 n 个月，格式非法时 fail-soft 返回原月份 */
    private static String plusMonths(String month, int n) {
        try {
            return YearMonth.parse(month).plusMonths(n).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (Exception e) {
            log.warn("[FixedCost] 生效月份格式非法，按单月覆盖处理: month={}", month);
            return month;
        }
    }
}
