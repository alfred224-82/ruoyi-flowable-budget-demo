package com.ruoyi.system.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.system.domain.SensitiveWordHitLog;
import com.ruoyi.system.domain.vo.SensitiveWordHitLogVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 敏感词命中日志 Mapper（M18 T18.1，看板聚合）
 * <p>sensitive_word_hit_log 走标准租户隔离：租户查询自动过滤，Admin(isAdmin) 免过滤可跨租户聚合。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Mapper
public interface SensitiveWordHitLogMapper extends BaseMapperPlus<SensitiveWordHitLogMapper, SensitiveWordHitLog, SensitiveWordHitLogVo> {

    /**
     * 命中趋势：按日统计近 N 天 FAIL 命中次数。
     *
     * @param days 统计天数
     * @return 每行 {stat_date, hit_count}
     */
    @Select("SELECT DATE(create_time) AS stat_date, COUNT(*) AS hit_count " +
        "FROM sensitive_word_hit_log " +
        "WHERE result = 'FAIL' AND create_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
        "GROUP BY DATE(create_time) ORDER BY stat_date ASC")
    List<Map<String, Object>> selectTrend(@Param("days") int days);

    /**
     * 命中词 TOP10：近 N 天 FAIL 命中次数最高的敏感词。
     *
     * @param days 统计天数
     * @return 每行 {hit_word, hit_category, hit_count}
     */
    @Select("SELECT hit_word, hit_category, COUNT(*) AS hit_count " +
        "FROM sensitive_word_hit_log " +
        "WHERE result = 'FAIL' AND hit_word IS NOT NULL " +
        "  AND create_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
        "GROUP BY hit_word, hit_category ORDER BY hit_count DESC LIMIT 10")
    List<Map<String, Object>> selectTop10(@Param("days") int days);

    /**
     * 高风险租户：近 N 天 FAIL 命中次数最多的租户排行（仅 Admin 跨租户聚合使用）。
     *
     * @param days 统计天数
     * @return 每行 {tenant_id, hit_count}
     */
    @Select("SELECT tenant_id, COUNT(*) AS hit_count " +
        "FROM sensitive_word_hit_log " +
        "WHERE result = 'FAIL' AND create_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
        "GROUP BY tenant_id ORDER BY hit_count DESC LIMIT 10")
    List<Map<String, Object>> selectHighRiskTenants(@Param("days") int days);
}
