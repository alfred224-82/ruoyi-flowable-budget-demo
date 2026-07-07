package com.ruoyi.report.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.report.domain.dto.ReportGenerateDto;
import com.ruoyi.report.domain.vo.ReportRecordVo;

/**
 * AI报表服务接口
 *
 * @author ruoyi
 */
public interface IAiReportService {

    /**
     * 生成报表（同步）
     *
     * @param dto 报表生成请求
     * @return 报表记录VO（包含生成的内容）
     */
    ReportRecordVo generateReport(ReportGenerateDto dto);

    /**
     * 查询报表生成历史
     *
     * @param reportType 报表类型（可选）
     * @param pageQuery  分页参数
     * @return 报表记录列表
     */
    TableDataInfo<ReportRecordVo> queryReportHistory(String reportType, PageQuery pageQuery);

    /**
     * 获取报表详情
     *
     * @param id 报表记录ID
     * @return 报表记录VO
     */
    ReportRecordVo getReportDetail(Long id);

    /**
     * 删除报表记录
     *
     * @param id 报表记录ID
     * @return 是否成功
     */
    Boolean deleteReport(Long id);

    /**
     * 获取报表类型列表
     *
     * @return 报表类型列表
     */
    java.util.List<java.util.Map<String, String>> getReportTypes();
}
