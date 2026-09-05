package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.ProjectTaskBo;
import com.ruoyi.system.domain.vo.ProjectTaskVo;

import java.util.Collection;

/**
 * 项目企业任务服务接口（T11.3，type=general 容器）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IProjectTaskService {

    /** 分页查询任务（按容器/名称/状态/指派人过滤） */
    TableDataInfo<ProjectTaskVo> queryPageList(ProjectTaskBo bo, PageQuery pageQuery);

    /** 任务详情 */
    ProjectTaskVo queryById(Long id);

    /** 新建任务 */
    Long insertTask(ProjectTaskBo bo);

    /** 编辑任务 */
    int updateTask(ProjectTaskBo bo);

    /** 指派 + 进度更新（T11.3） */
    int assign(Long id, Long assigneeUserId, Integer progress);

    /** 逻辑删除（可批量） */
    int deleteByIds(Collection<Long> ids);
}
