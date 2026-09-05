package com.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.system.domain.FeeCrossDeptAllocationLog;

/**
 * 跨部门分摊Service接口
 * 设计文档 Phase 11 Task 11.2
 *
 * @author ruoyi
 * @date 2026-07-23
 */
public interface ICrossDeptAllocService {

    /**
     * 月末执行跨部门分摊
     *
     * @param period       分摊期间（YYYY-MM）
     * @param sourceDeptId 虚拟待分摊部门ID
     * @param basis        分摊依据（HEADCOUNT/WORKHOURS）
     * @return 分摊记录数
     */
    int executeAllocation(String period, Long sourceDeptId, String basis);

    /**
     * 查询分摊记录
     */
    Page<FeeCrossDeptAllocationLog> listLogs(String period, Long sourceDeptId, PageQuery pageQuery);
}
