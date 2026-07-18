package com.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.system.domain.FeeOperationLog;

import java.util.List;

/**
 * 费用管控操作日志Service接口
 *
 * @author ruoyi
 * @date 2026-07-15
 */
public interface IFeeOperationLogService {

    /**
     * 记录操作日志（仅INSERT）
     *
     * @param module          所属模块
     * @param operationType   操作类型
     * @param billId          关联报销单ID
     * @param billNo          报销单编号
     * @param relatedAppId    关联预算调整申请ID
     * @param beforeStatus    操作前状态
     * @param afterStatus     操作后状态
     * @param operationDetail 操作详情（JSON）
     */
    void log(String module, String operationType, Long billId, String billNo,
             Long relatedAppId, String beforeStatus, String afterStatus,
             String operationDetail);

    /**
     * 查询操作日志列表
     *
     * @param query     查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    Page<FeeOperationLog> selectPageList(FeeOperationLog query, PageQuery pageQuery);

    /**
     * 查询单据操作时间线
     *
     * @param billNo 报销单编号
     * @return 操作日志列表
     */
    List<FeeOperationLog> selectTimeline(String billNo);

    /**
     * 导出操作日志
     *
     * @param query 查询条件
     * @return 操作日志列表
     */
    List<FeeOperationLog> selectExportList(FeeOperationLog query);
}
