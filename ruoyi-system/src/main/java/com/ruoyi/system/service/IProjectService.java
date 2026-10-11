package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.ProjectBo;
import com.ruoyi.system.domain.vo.FeeUserOptionVo;
import com.ruoyi.system.domain.vo.ProjectMemberVo;
import com.ruoyi.system.domain.vo.ProjectVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 项目统一容器服务接口（T11.1 / T11.2 / T11.5）
 * <p>企业任务与活动报名共用容器；成员五枚举角色；业态差异进 extra_config。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IProjectService {

    /**
     * 分页查询项目容器（支持按名称/类型/状态/业态过滤，业态分组工作台数据源）
     */
    TableDataInfo<ProjectVo> queryPageList(ProjectBo bo, PageQuery pageQuery);

    /**
     * 查询容器详情
     */
    ProjectVo queryById(Long id);

    /**
     * 按业态分组统计容器数量（T11.5 业态分组工作台）
     *
     * @return bizCode → 容器数量
     */
    Map<String, Long> countGroupByBiz();

    /**
     * 新建容器（负责人默认入组为 owner）
     *
     * @return 容器ID
     */
    Long insertProject(ProjectBo bo);

    /**
     * 编辑容器（含 extra_config）
     */
    int updateProject(ProjectBo bo);

    /**
     * 发布容器（DRAFT→PUBLISHED，发布后报名/任务可用）
     */
    int publish(Long id);

    /**
     * 逻辑删除容器（可批量）
     */
    int deleteByIds(Collection<Long> ids);

    /**
     * 容器成员列表（五枚举角色）
     */
    List<ProjectMemberVo> listMembers(Long projectId);

    /**
     * 批量保存成员（按 projectId+userId 幂等 upsert）
     */
    int saveMembers(Long projectId, List<ProjectBo.ProjectMemberBo> members);

    /**
     * 移除成员
     */
    int removeMember(Long projectId, Long userId);

    /**
     * 本租户启用状态用户下拉（容器成员/任务指派选择用）
     */
    List<FeeUserOptionVo> listUserOptions();
}
