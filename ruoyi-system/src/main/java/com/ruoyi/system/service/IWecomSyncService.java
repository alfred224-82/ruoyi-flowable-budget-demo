package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.vo.WecomSyncLogVo;

/**
 * 企微通讯录同步服务接口（T13.2 幂等/失败可重试）
 * <p>同步部门/成员到本租户组织架构，全程写 wecom_sync_log 留痕；桩实现不请求企微 API，仅记录一次空同步。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IWecomSyncService {

    /** 同步类型：部门 */
    String SYNC_DEPT = "dept";
    /** 同步类型：成员 */
    String SYNC_USER = "user";
    /** 同步类型：全量 */
    String SYNC_ALL = "all";

    /**
     * 触发通讯录同步（幂等：可重复执行，每次留痕一条 wecom_sync_log）
     *
     * @param syncType dept/user/all
     * @return 同步日志
     */
    WecomSyncLogVo syncContacts(String syncType);

    /**
     * 同步日志分页查询
     */
    TableDataInfo<WecomSyncLogVo> queryPageList(String syncType, String status, PageQuery pageQuery);
}
