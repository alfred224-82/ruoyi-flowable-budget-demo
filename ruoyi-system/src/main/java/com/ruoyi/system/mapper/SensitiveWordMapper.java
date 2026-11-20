package com.ruoyi.system.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.system.domain.SensitiveWord;
import com.ruoyi.system.domain.vo.SensitiveWordVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 敏感词 Mapper（M18 T18.1）
 * <p>sensitive_word 已加入 IGNORE_TENANT_TABLES，全局底库 tenant_id=0 与租户词在此手工合并查询。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Mapper
public interface SensitiveWordMapper extends BaseMapperPlus<SensitiveWordMapper, SensitiveWord, SensitiveWordVo> {

    /**
     * 查询生效词库：全局底库(tenant_id=0) + 指定租户词，仅启用状态，且行业范围命中（NULL=全行业通用）。
     *
     * @param tenantId 当前租户ID（非空）
     * @param bizCode  行业编码（可空，空则仅按 industry_scope IS NULL 过滤）
     * @return 生效敏感词列表
     */
    @Select("<script>" +
        "SELECT * FROM sensitive_word " +
        "WHERE status = '0' " +
        "  AND (tenant_id = 0 OR tenant_id = #{tenantId}) " +
        "  AND (industry_scope IS NULL OR industry_scope = '' " +
        "<if test='bizCode != null and bizCode != \"\"'>" +
        "    OR JSON_CONTAINS(industry_scope, JSON_QUOTE(#{bizCode})) " +
        "</if>" +
        "  ) " +
        "</script>")
    List<SensitiveWord> selectEffectiveWords(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode);
}
