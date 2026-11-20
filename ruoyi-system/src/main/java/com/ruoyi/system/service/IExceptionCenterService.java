package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.ExceptionCenterVo;

/**
 * 业务异常处理中心服务接口（T18.8，小微版 M18）
 *
 * @author ruoyi
 * @date 2026-09-17
 */
public interface IExceptionCenterService {

    /**
     * 获取当前租户的业务异常列表
     * <p>聚合：预约冲突 / 预约失败 / 固定支出异常 / 合规命中
     */
    ExceptionCenterVo getExceptions();

    /**
     * 标记异常为已处理
     *
     * @param exceptionType 异常类型
     * @param sourceId      来源记录ID
     */
    void resolveException(String exceptionType, Long sourceId);
}
