package com.ruoyi.system.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.system.domain.MappingRule;
import com.ruoyi.system.domain.vo.MappingRuleVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 映射规则Mapper接口
 *
 * @author ruoyi
 * @date 2026-07-13
 */
public interface MappingRuleMapper extends BaseMapperPlus<MappingRuleMapper, MappingRule, MappingRuleVo> {

    /**
     * 精确匹配映射规则（物资小类 + 用途标签）
     *
     * @param itemCategory 物资小类编码
     * @param purposeTag   用途标签编码
     * @return 匹配的规则
     */
    @Select("SELECT * FROM fee_mapping_rule WHERE item_category = #{itemCategory} " +
            "AND purpose_tag = #{purposeTag} " +
            "AND status = 'ENABLED' " +
            "AND (publish_status IS NULL OR publish_status = 'PUBLISHED') " +
            "ORDER BY priority DESC LIMIT 1")
    MappingRule matchExact(@Param("itemCategory") String itemCategory,
                           @Param("purposeTag") String purposeTag);
}
