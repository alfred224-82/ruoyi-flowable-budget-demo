package com.ruoyi.system.service;

import com.ruoyi.system.domain.bo.FeeRulePublishBo;
import com.ruoyi.system.domain.vo.FeeRuleGrayscaleCompareVo;
import com.ruoyi.system.domain.vo.FeeRuleVersionVo;
import com.ruoyi.system.domain.vo.MappingRuleVo;

import java.util.List;

/**
 * 规则引擎版本快照Service接口（T7.4 / FEAT_RULE_GRAYSCALE）
 * 灰度发布（默认10%比对命中率）+ 版本快照（锁定预算用当时快照）+ AI语义推荐
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IFeeRuleVersionService {

    /**
     * 规则版本列表（版本号倒序）
     */
    List<FeeRuleVersionVo> listVersions();

    /**
     * 发布当前映射规则为新版本（快照），按灰度百分比生效
     *
     * @return 新版本ID
     */
    Long publish(FeeRulePublishBo bo);

    /**
     * 灰度命中率比对：新版本规则对灰度分组单据的命中率 vs 上一版本
     */
    FeeRuleGrayscaleCompareVo compareHitRate(Long versionId);

    /**
     * 全量发布（灰度验证通过后）
     */
    Boolean fullRelease(Long versionId);

    /**
     * 回滚（标记该版本回滚，不删除历史）
     */
    Boolean rollback(Long versionId);

    /**
     * AI 语义推荐科目：未命中关键字的单据由语义模型推荐（轻量关键词相似度实现）
     *
     * @param text 报销摘要/用途描述
     * @return 候选规则（按相似度降序，最多5条）
     */
    List<MappingRuleVo> recommendSubject(String text);

    /**
     * 当前生效的规则版本ID（最新 FULL 版本，无则最新灰度版本；用于锁定预算关联快照）
     *
     * @return 规则版本ID；无版本时返回 null
     */
    Long currentActiveVersionId();
}
