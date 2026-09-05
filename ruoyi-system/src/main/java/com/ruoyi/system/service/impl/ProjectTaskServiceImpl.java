package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.ProjectTask;
import com.ruoyi.system.domain.bo.ProjectTaskBo;
import com.ruoyi.system.domain.vo.ProjectTaskVo;
import com.ruoyi.system.mapper.ProjectTaskMapper;
import com.ruoyi.system.service.IProjectTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * 项目企业任务服务实现（T11.3）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectTaskServiceImpl implements IProjectTaskService {

    private final ProjectTaskMapper taskMapper;

    @Override
    public TableDataInfo<ProjectTaskVo> queryPageList(ProjectTaskBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ProjectTask> wrapper = Wrappers.<ProjectTask>lambdaQuery()
            .eq(bo.getProjectId() != null, ProjectTask::getProjectId, bo.getProjectId())
            .like(StrUtil.isNotBlank(bo.getTaskName()), ProjectTask::getTaskName, bo.getTaskName())
            .eq(StrUtil.isNotBlank(bo.getStatus()), ProjectTask::getStatus, bo.getStatus())
            .eq(bo.getAssigneeUserId() != null, ProjectTask::getAssigneeUserId, bo.getAssigneeUserId())
            .orderByDesc(ProjectTask::getCreateTime);
        Page<ProjectTaskVo> page = taskMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public ProjectTaskVo queryById(Long id) {
        ProjectTaskVo vo = taskMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("任务不存在");
        }
        return vo;
    }

    @Override
    public Long insertTask(ProjectTaskBo bo) {
        ProjectTask task = BeanCopyUtils.copy(bo, ProjectTask.class);
        task.setId(null);
        task.setStatus(StrUtil.isNotBlank(bo.getStatus()) ? bo.getStatus() : "PENDING");
        task.setProgress(bo.getProgress() == null ? 0 : bo.getProgress());
        taskMapper.insert(task);
        return task.getId();
    }

    @Override
    public int updateTask(ProjectTaskBo bo) {
        ProjectTask exist = taskMapper.selectById(bo.getId());
        if (exist == null) {
            throw new ServiceException("任务不存在");
        }
        ProjectTask update = BeanCopyUtils.copy(bo, ProjectTask.class);
        // 进度到 100 自动置 DONE（未完成保持原状态或按传入）
        if (update.getProgress() != null && update.getProgress() >= 100 && StrUtil.isBlank(update.getStatus())) {
            update.setStatus("DONE");
        }
        return taskMapper.updateById(update);
    }

    @Override
    public int assign(Long id, Long assigneeUserId, Integer progress) {
        ProjectTask exist = taskMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("任务不存在");
        }
        ProjectTask update = new ProjectTask();
        update.setId(id);
        update.setAssigneeUserId(assigneeUserId);
        if (progress != null) {
            update.setProgress(progress);
            update.setStatus(progress >= 100 ? "DONE" : "DOING");
        } else if (assigneeUserId != null) {
            update.setStatus("DOING");
        }
        return taskMapper.updateById(update);
    }

    @Override
    public int deleteByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        return taskMapper.deleteBatchIds(ids);
    }
}
