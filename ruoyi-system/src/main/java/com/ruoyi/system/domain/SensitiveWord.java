package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 敏感词对象 sensitive_word（M18 T18.1，图片合规与全局敏感词）
 * <p>全局底库（tenant_id=0，Admin 唯一维护）+ 租户自定义（tenant_id=当前租户）合并检测。
 * <p>★不继承 TenantEntity：tenant_id=0 为平台级全局记录，需跨租户读取（走条件查询），
 *   sensitive_word 已加入 MybatisPlusConfig.IGNORE_TENANT_TABLES，租户过滤在 Service/Mapper 层手工完成。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sensitive_word")
public class SensitiveWord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户ID（0=全局底库，非0=租户自定义） */
    private Long tenantId;

    /** 敏感词文本 */
    private String word;

    /** 分类：POLITICAL/PORN/VIOLENCE/AD/SUPERSTITION/ABUSE/RUMOR/DIALECT/CUSTOM */
    private String category;

    /** 匹配模式：exact(精确/contains)/fuzzy(编辑距离≤2)/regex(正则) */
    private String matchMode;

    /** 行业范围（JSON 数组文本，NULL=全行业通用；非空=仅指定 biz_code 生效） */
    private String industryScope;

    /** 状态（0启用 1禁用） */
    private String status;

    /** 来源：global(平台底库)/tenant(租户自定义) */
    private String source;

    private String remark;
}
