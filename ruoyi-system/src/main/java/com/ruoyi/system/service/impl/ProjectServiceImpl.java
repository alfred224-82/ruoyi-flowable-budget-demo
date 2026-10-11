package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.Project;
import com.ruoyi.system.domain.ProjectMember;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.domain.bo.ProjectBo;
import com.ruoyi.system.domain.enums.ProjectMemberRole;
import com.ruoyi.system.domain.vo.ProjectMemberVo;
import com.ruoyi.system.domain.vo.FeeUserOptionVo;
import com.ruoyi.system.domain.vo.ProjectVo;
import com.ruoyi.system.mapper.ProjectMapper;
import com.ruoyi.system.mapper.ProjectMemberMapper;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.IProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 项目统一容器服务实现（T11.1 / T11.2 / T11.5）
 * <p>企业任务与活动报名共用容器；成员五枚举角色与 sys_role 解耦；业态差异进 extra_config。
 * 租户隔离由多租户插件自动处理（project/project_member 均含 tenant_id）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements IProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;
    private final SysTenantMapper tenantMapper;
    private final SysUserMapper userMapper;

    /** 默认业态包编码（存量租户回填值，见 M1_06） */
    private static final String DEFAULT_BIZ_CODE = "fund_control";

    @Override
    public TableDataInfo<ProjectVo> queryPageList(ProjectBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<Project> wrapper = Wrappers.<Project>lambdaQuery()
            .like(StrUtil.isNotBlank(bo.getName()), Project::getName, bo.getName())
            .eq(StrUtil.isNotBlank(bo.getType()), Project::getType, bo.getType())
            .eq(StrUtil.isNotBlank(bo.getStatus()), Project::getStatus, bo.getStatus())
            .eq(StrUtil.isNotBlank(bo.getBizCode()), Project::getBizCode, bo.getBizCode())
            .orderByDesc(Project::getCreateTime);
        Page<ProjectVo> page = projectMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public ProjectVo queryById(Long id) {
        ProjectVo vo = projectMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("项目容器不存在");
        }
        return vo;
    }

    @Override
    public Map<String, Long> countGroupByBiz() {
        QueryWrapper<Project> qw = new QueryWrapper<>();
        qw.select("biz_code AS bizCode", "COUNT(*) AS cnt").groupBy("biz_code");
        List<Map<String, Object>> rows = projectMapper.selectMaps(qw);
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object biz = row.get("bizCode");
            Object cnt = row.get("cnt");
            result.put(biz == null ? DEFAULT_BIZ_CODE : String.valueOf(biz),
                cnt == null ? 0L : ((Number) cnt).longValue());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertProject(ProjectBo bo) {
        // type 由 ActivityController 自动填充或 ProjectController 前端传入，在此统一做业务层非空校验
        if (StrUtil.isBlank(bo.getType())) {
            throw new ServiceException("容器类型不能为空");
        }
        Project project = BeanCopyUtils.copy(bo, Project.class);
        project.setId(null);
        project.setBizCode(resolveBizCode(bo.getBizCode()));
        project.setStatus(StrUtil.isNotBlank(bo.getStatus()) ? bo.getStatus() : "DRAFT");
        Long ownerUserId = bo.getOwnerUserId() != null ? bo.getOwnerUserId() : LoginHelper.getUserId();
        project.setOwnerUserId(ownerUserId);
        projectMapper.insert(project);

        // 负责人默认入组为 owner
        if (ownerUserId != null) {
            upsertMember(project.getId(), ownerUserId, ProjectMemberRole.OWNER.getCode());
        }
        // 初始成员
        if (CollUtil.isNotEmpty(bo.getMembers())) {
            saveMembers(project.getId(), bo.getMembers());
        }
        return project.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateProject(ProjectBo bo) {
        Project exist = projectMapper.selectById(bo.getId());
        if (exist == null) {
            throw new ServiceException("项目容器不存在");
        }
        Project update = BeanCopyUtils.copy(bo, Project.class);
        // 业态与负责人不允许通过编辑随意篡改（保持容器归属稳定）
        update.setBizCode(null);
        update.setOwnerUserId(null);
        int rows = projectMapper.updateById(update);
        if (bo.getMembers() != null) {
            saveMembers(bo.getId(), bo.getMembers());
        }
        return rows;
    }

    @Override
    public int publish(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new ServiceException("项目容器不存在");
        }
        if ("CLOSED".equals(project.getStatus()) || "CANCELLED".equals(project.getStatus())) {
            throw new ServiceException("已关闭/已取消的容器不可发布");
        }
        Project update = new Project();
        update.setId(id);
        update.setStatus("PUBLISHED");
        return projectMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        return projectMapper.deleteBatchIds(ids);
    }

    @Override
    public List<ProjectMemberVo> listMembers(Long projectId) {
        return memberMapper.selectVoList(
            Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .orderByAsc(ProjectMember::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveMembers(Long projectId, List<ProjectBo.ProjectMemberBo> members) {
        if (projectId == null || CollUtil.isEmpty(members)) {
            return 0;
        }
        int rows = 0;
        for (ProjectBo.ProjectMemberBo m : members) {
            if (m.getUserId() == null) {
                continue;
            }
            String role = ProjectMemberRole.of(m.getRole()).getCode();
            rows += upsertMember(projectId, m.getUserId(), role);
        }
        return rows;
    }

    @Override
    public int removeMember(Long projectId, Long userId) {
        return memberMapper.delete(
            Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId));
    }

    @Override
    public List<FeeUserOptionVo> listUserOptions() {
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.<SysUser>lambdaQuery()
            .eq(SysUser::getStatus, "0")
            .orderByAsc(SysUser::getUserId);
        // 租户隔离：sys_user 在 IGNORE_TENANT_TABLES 中，非超管需手工按当前租户过滤（T2.4）
        if (!LoginHelper.isAdmin()) {
            Long tenantId = LoginHelper.getTenantId();
            if (tenantId != null) {
                wrapper.eq(SysUser::getTenantId, tenantId);
            }
        }
        return userMapper.selectList(wrapper).stream().map(u -> {
            FeeUserOptionVo vo = new FeeUserOptionVo();
            vo.setUserId(u.getUserId());
            vo.setUserName(u.getUserName());
            vo.setNickName(u.getNickName());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 成员幂等 upsert（uk_project_user：同一容器一个用户仅一条）
     */
    private int upsertMember(Long projectId, Long userId, String role) {
        ProjectMember exist = memberMapper.selectOne(
            Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId));
        if (exist == null) {
            ProjectMember entity = new ProjectMember();
            entity.setProjectId(projectId);
            entity.setUserId(userId);
            entity.setRole(role);
            entity.setJoinTime(new Date());
            return memberMapper.insert(entity);
        }
        exist.setRole(role);
        return memberMapper.updateById(exist);
    }

    /**
     * 解析业态编码：显式指定优先，否则取当前租户 sys_tenant.biz_code，再兜底 fund_control
     */
    private String resolveBizCode(String bizCode) {
        if (StrUtil.isNotBlank(bizCode)) {
            return bizCode;
        }
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId != null) {
            SysTenant tenant = tenantMapper.selectById(tenantId);
            if (tenant != null && StrUtil.isNotBlank(tenant.getBizCode())) {
                return tenant.getBizCode();
            }
        }
        return DEFAULT_BIZ_CODE;
    }
}
