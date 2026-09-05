package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.FeeCrossDeptAllocationLog;
import com.ruoyi.system.mapper.FeeCrossDeptAllocationLogMapper;
import com.ruoyi.system.service.ICrossDeptAllocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.UUID;

/**
 * 跨部门分摊Service业务层处理
 * 设计文档 Phase 11 Task 11.2
 *
 * 分摊逻辑：
 * 1. 查询虚拟部门待分摊费用总额（费用轨）
 * 2. 查询各目标部门人数/工时
 * 3. 计算比例：ratio_i = dept_i人数 / 总人数
 * 4. 分摊金额：amount_i = total × ratio_i
 * 5. 写入 fee_cross_dept_allocation_log
 * 6. 仅调整费用轨额度，不占用资本采购预算
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrossDeptAllocServiceImpl implements ICrossDeptAllocService {

    private final FeeCrossDeptAllocationLogMapper allocationLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int executeAllocation(String period, Long sourceDeptId, String basis) {
        log.info("[CrossDeptAlloc] 开始执行跨部门分摊, period={}, sourceDeptId={}, basis={}", period, sourceDeptId, basis);

        // 检查是否已执行过本期分摊
        Long existCount = allocationLogMapper.selectCount(
            Wrappers.<FeeCrossDeptAllocationLog>lambdaQuery()
                .eq(FeeCrossDeptAllocationLog::getAllocationPeriod, period)
                .eq(FeeCrossDeptAllocationLog::getSourceDeptId, sourceDeptId)
        );
        if (existCount > 0) {
            throw new ServiceException("期间[" + period + "]已执行过分摊，不可重复执行");
        }

        // 简化实现：使用模拟数据演示分摊逻辑
        // 实际生产中应从预算表查询虚拟部门待分摊总额，从人事系统查询各部门人数
        BigDecimal totalAmount = new BigDecimal("100000.00"); // 模拟待分摊总额
        // 模拟3个目标部门及其人数
        long[][] targetDepts = {{101L, 30L}, {102L, 20L}, {103L, 50L}};
        long totalHeadcount = 100L;

        int count = 0;
        String voucherNo = "ALLOC-" + period + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        for (long[] dept : targetDepts) {
            long targetDeptId = dept[0];
            long headcount = dept[1];

            BigDecimal ratio = BigDecimal.valueOf(headcount)
                .divide(BigDecimal.valueOf(totalHeadcount), 4, RoundingMode.HALF_UP);
            BigDecimal allocAmount = totalAmount.multiply(ratio).setScale(2, RoundingMode.HALF_UP);

            FeeCrossDeptAllocationLog logEntry = new FeeCrossDeptAllocationLog();
            logEntry.setAllocationPeriod(period);
            logEntry.setSourceDeptId(sourceDeptId);
            logEntry.setTargetDeptId(targetDeptId);
            logEntry.setBudgetSubjectCode("EXPENSE_POOL");
            logEntry.setTotalAmount(totalAmount);
            logEntry.setAllocRatio(ratio);
            logEntry.setAllocAmount(allocAmount);
            logEntry.setAllocBasis(basis);
            logEntry.setVoucherNo(voucherNo);
            logEntry.setCreateTime(new Date());
            allocationLogMapper.insert(logEntry);
            count++;
        }

        log.info("[CrossDeptAlloc] 分摊完成, period={}, 共{}笔, 凭证号={}", period, count, voucherNo);
        return count;
    }

    @Override
    public Page<FeeCrossDeptAllocationLog> listLogs(String period, Long sourceDeptId, PageQuery pageQuery) {
        return allocationLogMapper.selectPage(
            pageQuery.build(),
            Wrappers.<FeeCrossDeptAllocationLog>lambdaQuery()
                .eq(StrUtil.isNotBlank(period), FeeCrossDeptAllocationLog::getAllocationPeriod, period)
                .eq(sourceDeptId != null, FeeCrossDeptAllocationLog::getSourceDeptId, sourceDeptId)
                .orderByDesc(FeeCrossDeptAllocationLog::getCreateTime)
        );
    }
}
