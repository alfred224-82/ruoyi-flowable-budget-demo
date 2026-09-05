package com.ruoyi.system.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.system.domain.FeeAccountSet;
import com.ruoyi.system.domain.vo.FeeAccountSetVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 多账套Mapper接口（T7.7 / FEAT_MULTI_ACCOUNT_SET）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface FeeAccountSetMapper extends BaseMapperPlus<FeeAccountSetMapper, FeeAccountSet, FeeAccountSetVo> {

    /**
     * 合并报表：按账套汇总已审批通过的报销单数量与金额
     * <p>tenant_id 由多租户拦截器自动追加（admin 登录态下按当前租户隔离）；
     * account_set_id 为 NULL 的存量单据由 Service 归入默认账套。
     *
     * @param period 报销年月（YYYY-MM，空=全部期间）
     * @return 每行含 accountSetId / billCount / totalAmount
     */
    @Select("<script>" +
        "SELECT account_set_id AS accountSetId, COUNT(*) AS billCount, COALESCE(SUM(amount), 0) AS totalAmount " +
        "FROM fee_expense_bill WHERE status = 'APPROVED' " +
        "<if test='period != null and period != \"\"'> AND expense_period = #{period}</if> " +
        "GROUP BY account_set_id" +
        "</script>")
    List<Map<String, Object>> sumBillGroupByAccountSet(@Param("period") String period);

    /**
     * 统计归属指定账套的报销单数量（删除前置校验：有单据归属的账套禁止删除）
     *
     * @param accountSetId 账套ID
     * @return 单据数量
     */
    @Select("SELECT COUNT(*) FROM fee_expense_bill WHERE account_set_id = #{accountSetId}")
    long countBillsByAccountSet(@Param("accountSetId") Long accountSetId);
}
