package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.FeeAsset;
import com.ruoyi.system.domain.FeeDepreciationRule;
import com.ruoyi.system.domain.vo.FeeAssetDepreciationVo;
import com.ruoyi.system.domain.vo.FeeAssetVo;
import com.ruoyi.system.domain.vo.FeeDepreciationRuleVo;

import java.util.List;

/**
 * 资产折旧Service接口（T5.9 / ADR-026）
 * 折旧规则财务设、系统自动算，含项目分摊
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IFeeAssetService {

    /**
     * 资产分页查询
     */
    TableDataInfo<FeeAssetVo> queryPageList(Long projectId, String status, PageQuery pageQuery);

    /**
     * 新增/修改资产（月折旧额自动计算）
     */
    Long saveAsset(FeeAsset asset);

    /**
     * 资产报废（停止折旧）
     */
    int scrapAsset(Long id);

    /**
     * 折旧规则列表
     */
    List<FeeDepreciationRuleVo> listRules();

    /**
     * 保存折旧规则（资产分类唯一）
     */
    Long saveRule(FeeDepreciationRule rule);

    /**
     * 折旧记录分页查询
     */
    TableDataInfo<FeeAssetDepreciationVo> queryDepreciationPageList(String accrualMonth, Long projectId, PageQuery pageQuery);

    /**
     * 月度折旧计提（幂等：同月同资产仅一条），项目资产记入项目分摊
     *
     * @param accrualMonth 计提月份 yyyy-MM，空=上一自然月
     * @return 计提条数
     */
    int accrueDepreciation(String accrualMonth);
}
