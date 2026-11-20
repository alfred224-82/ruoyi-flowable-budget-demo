package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 租户管理员企业概览首页 VO（T18.6，小微版 M18）
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Data
public class TenantOverviewVo implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===== 企业基本信息 =====

    /** 公司名称 */
    private String companyName;

    /** 行业 */
    private String industry;

    /** 版本档位 */
    private String versionType;

    /** 租户状态 */
    private String tenantStatus;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date createTime;

    /** License 状态：ACTIVE/EXPIRED/NONE */
    private String licenseStatus;

    /** License 到期日 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date licenseExpireDate;

    /** License 剩余天数（-1=永久，0=已过期） */
    private Integer licenseRemainingDays;

    // ===== 配置健康度自检 =====

    /** 整体健康状态：GREEN / YELLOW / RED */
    private String configHealth;

    /** 自检项列表 */
    private List<ConfigCheckItem> configChecks;

    /**
     * 配置自检项
     */
    @Data
    public static class ConfigCheckItem implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 检查项名称 */
        private String name;

        /** 检查项描述 */
        private String description;

        /** 状态：GREEN(正常) / YELLOW(警告) / RED(异常) */
        private String status;

        /** 建议操作（RED/YELLOW 时有值） */
        private String suggestion;
    }
}
