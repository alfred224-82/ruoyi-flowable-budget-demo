package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.WithdrawalApplyBo;
import com.ruoyi.system.domain.bo.WithdrawalAuditBo;
import com.ruoyi.system.domain.vo.SysWithdrawalRequestVo;

/**
 * 提现服务接口（T14.3 提现申请与人工审核）
 * <p>状态机：apply → PENDING（冻结虚拟账户余额）；audit 通过 → APPROVED（扣减冻结、累计提现+）/ 驳回 → REJECTED（解冻回余额）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IWithdrawalService {

    /**
     * 提现申请分页查询
     *
     * @param status   状态（可空）
     * @param tenantId 租户ID（可空；平台超管跨租户筛查）
     */
    TableDataInfo<SysWithdrawalRequestVo> queryPageList(String status, Long tenantId, PageQuery pageQuery);

    /**
     * 提现申请详情
     */
    SysWithdrawalRequestVo queryById(Long id);

    /**
     * 发起提现申请（当前登录租户）：校验可用余额并冻结，写入 PENDING
     *
     * @return 影响行数
     */
    int apply(WithdrawalApplyBo bo);

    /**
     * 人工审核：通过则扣减冻结余额、驳回则解冻
     *
     * @return 影响行数
     */
    int audit(WithdrawalAuditBo bo);
}
