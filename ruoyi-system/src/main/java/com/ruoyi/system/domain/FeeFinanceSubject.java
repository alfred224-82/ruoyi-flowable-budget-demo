package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 财务科目导入对象 fee_finance_subject（T5.16，映射A载体）
 * 客户自有财务科目（金蝶/用友等导出）导入存储 + 映射到系统预算科目
 *
 * @author ruoyi
 * @date 2026-08-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_finance_subject")
public class FeeFinanceSubject extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 导入批次号
     */
    private String importBatchNo;

    /**
     * 来源系统 KINGDEE/YONYOU/CHANJET/SUPERDATA/GRASP/LEMONCLOUD/GENERAL
     */
    private String sourceSystem;

    /**
     * 财务科目编码(客户自有)
     */
    private String subjectCode;

    /**
     * 财务科目名称
     */
    private String subjectName;

    /**
     * 父级科目编码
     */
    private String parentCode;

    /**
     * 科目层级
     */
    private Integer subjectLevel;

    /**
     * 映射A：对应预算科目编码
     */
    private String mappedSubjectCode;

    /**
     * 映射方式 AUTO(关键字自动)/MANUAL(人工补完)
     */
    private String matchType;

    /**
     * 状态 UNMAPPED未映射/MAPPED已映射/IGNORED忽略
     */
    private String status;

    /**
     * 删除标志（0存在 2删除）
     */
    @TableLogic
    private String delFlag;
}
