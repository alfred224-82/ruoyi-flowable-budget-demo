package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.FeeBadDebtBo;
import com.ruoyi.system.domain.vo.FeeBadDebtVo;

/**
 * 烂账管理Service接口（T5.6）
 * 风险标记 + 坏账计提 + 项目状态 LOST + 看板风险预警区联动
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IFeeBadDebtService {

    /**
     * 分页查询
     */
    TableDataInfo<FeeBadDebtVo> queryPageList(FeeBadDebtBo bo, PageQuery pageQuery);

    /**
     * 详情
     */
    FeeBadDebtVo queryById(Long id);

    /**
     * 风险标记（状态=OPEN），带计提金额时同步计提坏账
     */
    Long insertBadDebt(FeeBadDebtBo bo);

    /**
     * 追加坏账计提（累加项目坏账计提与支出）
     */
    int provisionBadDebt(Long id, java.math.BigDecimal amount);

    /**
     * 跟进（OPEN→FOLLOWING，追加跟进记录）
     */
    int followUp(Long id, String notes);

    /**
     * 关闭烂账（→CLOSED），项目状态置 LOST
     */
    int closeBadDebt(Long id);
}
