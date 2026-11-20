package com.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.SensitiveWordHitLog;
import com.ruoyi.system.mapper.SensitiveWordHitLogMapper;
import com.ruoyi.system.service.IComplianceDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 合规告警看板服务实现（M18 T18.5）
 * <p>聚合 sensitive_word_hit_log；Admin 跨租户，租户查询由拦截器自动过滤。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceDashboardServiceImpl implements IComplianceDashboardService {

    private final SensitiveWordHitLogMapper hitLogMapper;

    /** 默认统计天数 */
    private static final int DEFAULT_DAYS = 7;
    /** 最大统计天数 */
    private static final int MAX_DAYS = 90;

    @Override
    public Map<String, Object> dashboard(int days) {
        int d = normalizeDays(days);
        LocalDateTime since = LocalDate.now().minusDays(d).atStartOfDay();
        Long total = hitLogMapper.selectCount(timeWindow(since, null));
        Long fail = hitLogMapper.selectCount(timeWindow(since, "FAIL"));
        long totalVal = total != null ? total : 0L;
        long failVal = fail != null ? fail : 0L;
        double passRate = totalVal == 0 ? 100.0D : (double) (totalVal - failVal) * 100 / totalVal;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("days", d);
        result.put("totalHits", totalVal);
        result.put("failCount", failVal);
        result.put("passCount", totalVal - failVal);
        result.put("passRate", Math.round(passRate * 100) / 100.0D);
        return result;
    }

    @Override
    public List<Map<String, Object>> trend(int days) {
        return hitLogMapper.selectTrend(normalizeDays(days));
    }

    @Override
    public List<Map<String, Object>> top10(int days) {
        return hitLogMapper.selectTop10(normalizeDays(days));
    }

    @Override
    public List<Map<String, Object>> highRiskTenants(int days) {
        return hitLogMapper.selectHighRiskTenants(normalizeDays(days));
    }

    private int normalizeDays(int days) {
        if (days <= 0) {
            return DEFAULT_DAYS;
        }
        if (days > MAX_DAYS) {
            throw new ServiceException("统计天数不能超过 " + MAX_DAYS + " 天");
        }
        return days;
    }

    private LambdaQueryWrapper<SensitiveWordHitLog> timeWindow(LocalDateTime since, String result) {
        LambdaQueryWrapper<SensitiveWordHitLog> wrapper = Wrappers.<SensitiveWordHitLog>lambdaQuery()
            .ge(SensitiveWordHitLog::getCreateTime, since);
        if (result != null) {
            wrapper.eq(SensitiveWordHitLog::getResult, result);
        }
        return wrapper;
    }
}
