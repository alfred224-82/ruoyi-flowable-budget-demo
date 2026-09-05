package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 财务科目导入视图对象 fee_finance_subject（T5.16）
 *
 * @author ruoyi
 * @date 2026-08-30
 */
@Data
public class FeeFinanceSubjectVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
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
     * 映射的预算科目名称（冗余展示）
     */
    private String mappedSubjectName;

    /**
     * 映射方式 AUTO(关键字自动)/MANUAL(人工补完)
     */
    private String matchType;

    /**
     * 状态 UNMAPPED未映射/MAPPED已映射/IGNORED忽略
     */
    private String status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
