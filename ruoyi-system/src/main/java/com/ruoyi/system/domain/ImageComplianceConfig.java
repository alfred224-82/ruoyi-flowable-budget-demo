package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 图片合规配置对象 image_compliance_config（M18 T18.1）
 * <p>tenant_id=0 存全局默认三行（porn/violence/politics）；租户可覆盖。
 * <p>★不继承 TenantEntity：tenant_id=0 全局默认需跨租户读取（走条件查询），
 *   image_compliance_config 已加入 MybatisPlusConfig.IGNORE_TENANT_TABLES，租户过滤手工完成。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("image_compliance_config")
public class ImageComplianceConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户ID（0=全局默认，非0=租户覆盖） */
    private Long tenantId;

    /** 违规类型：porn(色情)/violence(暴恐)/politics(政治标志) */
    private String violationType;

    /** 是否启用该类型检测（0关闭 1启用） */
    private Integer enabled;

    /** 置信度阈值（0.00~1.00，低于此值打回） */
    private BigDecimal confidenceThreshold;

    /** 是否启用 OCR 文字提取（0关闭 1启用） */
    private Integer ocrEnabled;

    /** 是否启用视觉审核 API（0关闭 1启用） */
    private Integer visualApiEnabled;

    /** 最大文件大小（MB） */
    private Integer maxFileSizeMb;

    /** 允许的格式（逗号分隔） */
    private String allowedFormats;

    private String remark;
}
