package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.vo.FeeFinanceSubjectImportVo;
import com.ruoyi.system.domain.vo.FeeFinanceSubjectVo;

import java.util.Collection;
import java.util.List;

/**
 * 财务科目导入服务接口（T5.16，设计文档 1.3 / 2.2 #1）
 * <p>
 * 客户自有财务科目导入 → 关键字自动匹配建立映射A → 人工补完/纠偏。
 * 科目三层链路：财务科目 --映射A--> 预算科目 --映射B--> 费用大类。
 *
 * @author ruoyi
 * @date 2026-08-30
 */
public interface IFeeFinanceSubjectService {

    /**
     * 分页查询导入的财务科目（含映射A结果）
     *
     * @param status       状态筛选（UNMAPPED/MAPPED/IGNORED，可空）
     * @param sourceSystem 来源系统筛选（可空）
     * @param keyword      科目编码/名称关键字（可空）
     */
    TableDataInfo<FeeFinanceSubjectVo> queryPageList(String status, String sourceSystem,
                                                     String keyword, PageQuery pageQuery);

    /**
     * 导入客户财务科目（同租户同编码幂等覆盖），导入后自动执行关键字匹配
     *
     * @param list         导入数据
     * @param sourceSystem 来源系统（KINGDEE/YONYOU/.../GENERAL）
     * @return 导入结果消息
     */
    String importSubjects(List<FeeFinanceSubjectImportVo> list, String sourceSystem);

    /**
     * 关键字自动匹配：对所有未映射科目按名称关键字匹配预算科目建立映射A
     *
     * @return 本次匹配成功的科目数
     */
    int autoMatch();

    /**
     * 人工补完/纠偏映射A
     *
     * @param id                财务科目ID
     * @param mappedSubjectCode 目标预算科目编码（必须存在）
     */
    void manualMap(Long id, String mappedSubjectCode);

    /**
     * 忽略科目（不参与映射）
     */
    Boolean ignore(Long id);

    /**
     * 删除导入的财务科目
     */
    Boolean deleteByIds(Collection<Long> ids);
}
