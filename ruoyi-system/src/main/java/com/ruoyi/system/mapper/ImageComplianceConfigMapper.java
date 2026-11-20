package com.ruoyi.system.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.system.domain.ImageComplianceConfig;
import com.ruoyi.system.domain.vo.ImageComplianceConfigVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 图片合规配置 Mapper（M18 T18.1）
 * <p>image_compliance_config 已加入 IGNORE_TENANT_TABLES，全局默认 tenant_id=0 与租户覆盖在此合并查询。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Mapper
public interface ImageComplianceConfigMapper extends BaseMapperPlus<ImageComplianceConfigMapper, ImageComplianceConfig, ImageComplianceConfigVo> {

    /**
     * 查询生效配置：优先租户覆盖，缺失回落全局默认(tenant_id=0)。
     * <p>返回租户配置 UNION 全局默认，Service 层按 violation_type 去重（租户优先）。
     *
     * @param tenantId 当前租户ID（非空）
     * @return 生效配置列表
     */
    @Select("SELECT * FROM image_compliance_config WHERE tenant_id = #{tenantId} " +
        "UNION ALL " +
        "SELECT * FROM image_compliance_config WHERE tenant_id = 0")
    List<ImageComplianceConfig> selectEffective(@Param("tenantId") Long tenantId);
}
