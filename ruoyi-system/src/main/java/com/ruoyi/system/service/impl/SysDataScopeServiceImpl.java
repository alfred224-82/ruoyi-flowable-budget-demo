package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.helper.DataBaseHelper;
import com.ruoyi.common.utils.StreamUtils;
import com.ruoyi.system.domain.FeeProjectMember;
import com.ruoyi.system.domain.ProjectMember;
import com.ruoyi.system.domain.SysRoleDept;
import com.ruoyi.system.mapper.FeeProjectMemberMapper;
import com.ruoyi.system.mapper.ProjectMemberMapper;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.system.mapper.SysRoleDeptMapper;
import com.ruoyi.system.service.ISysDataScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据权限 实现
 * <p>
 * 注意: 此Service内不允许调用标注`数据权限`注解的方法
 * 例如: deptMapper.selectList 此 selectList 方法标注了`数据权限`注解 会出现循环解析的问题
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service("sdss")
public class SysDataScopeServiceImpl implements ISysDataScopeService {

    private final SysRoleDeptMapper roleDeptMapper;
    private final SysDeptMapper deptMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final FeeProjectMemberMapper feeProjectMemberMapper;

    @Override
    public String getRoleCustom(Long roleId) {
        List<SysRoleDept> list = roleDeptMapper.selectList(
            new LambdaQueryWrapper<SysRoleDept>()
                .select(SysRoleDept::getDeptId)
                .eq(SysRoleDept::getRoleId, roleId));
        if (CollUtil.isNotEmpty(list)) {
            return StreamUtils.join(list, rd -> Convert.toStr(rd.getDeptId()));
        }
        return null;
    }

    @Override
    public String getDeptAndChild(Long deptId) {
        List<SysDept> deptList = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
            .select(SysDept::getDeptId)
            .apply(DataBaseHelper.findInSet(deptId, "ancestors")));
        List<Long> ids = StreamUtils.toList(deptList, SysDept::getDeptId);
        ids.add(deptId);
        List<SysDept> list = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
            .select(SysDept::getDeptId)
            .in(SysDept::getDeptId, ids));
        if (CollUtil.isNotEmpty(list)) {
            return StreamUtils.join(list, d -> Convert.toStr(d.getDeptId()));
        }
        return null;
    }

    @Override
    public String getProjectIds(Long userId) {
        // 1. 查询 project_member（新 5 枚举表）：所有角色均可访问对应项目
        List<ProjectMember> pmList = projectMemberMapper.selectList(
            new LambdaQueryWrapper<ProjectMember>()
                .select(ProjectMember::getProjectId)
                .eq(ProjectMember::getUserId, userId));
        // 2. 查询 fee_project_member（旧 3 枚举表）：兼容存量数据
        List<FeeProjectMember> fpmList = feeProjectMemberMapper.selectList(
            new LambdaQueryWrapper<FeeProjectMember>()
                .select(FeeProjectMember::getProjectId)
                .eq(FeeProjectMember::getUserId, userId)
                .eq(FeeProjectMember::getIsActive, 1));
        // 3. 合并去重
        List<Long> allIds = new java.util.ArrayList<>();
        if (CollUtil.isNotEmpty(pmList)) {
            allIds.addAll(StreamUtils.toList(pmList, ProjectMember::getProjectId));
        }
        if (CollUtil.isNotEmpty(fpmList)) {
            allIds.addAll(StreamUtils.toList(fpmList, FeeProjectMember::getProjectId));
        }
        if (CollUtil.isNotEmpty(allIds)) {
            return allIds.stream().distinct().map(Convert::toStr).collect(java.util.stream.Collectors.joining(","));
        }
        return null;
    }

    @Override
    public String getManagedProjectIds(Long userId) {
        // 1. project_member 中 owner/manager 角色
        List<ProjectMember> pmList = projectMemberMapper.selectList(
            new LambdaQueryWrapper<ProjectMember>()
                .select(ProjectMember::getProjectId)
                .eq(ProjectMember::getUserId, userId)
                .in(ProjectMember::getRole, "owner", "manager"));
        // 2. fee_project_member 中 MANAGER 角色
        List<FeeProjectMember> fpmList = feeProjectMemberMapper.selectList(
            new LambdaQueryWrapper<FeeProjectMember>()
                .select(FeeProjectMember::getProjectId)
                .eq(FeeProjectMember::getUserId, userId)
                .eq(FeeProjectMember::getRole, "MANAGER")
                .eq(FeeProjectMember::getIsActive, 1));
        List<Long> allIds = new java.util.ArrayList<>();
        if (CollUtil.isNotEmpty(pmList)) {
            allIds.addAll(StreamUtils.toList(pmList, ProjectMember::getProjectId));
        }
        if (CollUtil.isNotEmpty(fpmList)) {
            allIds.addAll(StreamUtils.toList(fpmList, FeeProjectMember::getProjectId));
        }
        if (CollUtil.isNotEmpty(allIds)) {
            return allIds.stream().distinct().map(Convert::toStr).collect(java.util.stream.Collectors.joining(","));
        }
        return null;
    }

}
