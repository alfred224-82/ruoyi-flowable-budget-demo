package com.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.FeeOrg;
import com.ruoyi.system.domain.FeeOrgUser;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.domain.bo.FeeOrgBo;
import com.ruoyi.system.domain.bo.FeeOrgUserBo;
import com.ruoyi.system.domain.vo.FeeOrgUserVo;
import com.ruoyi.system.domain.vo.FeeOrgVo;
import com.ruoyi.system.mapper.FeeOrgMapper;
import com.ruoyi.system.mapper.FeeOrgUserMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.IFeeOrgService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 组织管理Service实现（T7.1 / FEAT_MULTI_ORG）
 * 组织树 + 用户挂组织 + 数据范围（本组织+下级组织）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeOrgServiceImpl implements IFeeOrgService {

    private final FeeOrgMapper orgMapper;
    private final FeeOrgUserMapper orgUserMapper;
    private final SysUserMapper userMapper;

    @Override
    public List<FeeOrgVo> selectOrgTree() {
        List<FeeOrgVo> all = orgMapper.selectVoList(Wrappers.<FeeOrg>lambdaQuery()
            .orderByAsc(FeeOrg::getSortOrder));
        fillUserCount(all);
        fillLeaderName(all);
        return buildTree(all);
    }

    @Override
    public Long insertOrg(FeeOrgBo bo) {
        checkOrgCodeUnique(bo.getOrgCode(), null);
        FeeOrg org = BeanUtil.copyProperties(bo, FeeOrg.class);
        long parentId = bo.getParentId() == null ? 0L : bo.getParentId();
        org.setParentId(parentId);
        org.setAncestors(buildAncestors(parentId));
        if (StrUtil.isBlank(org.getStatus())) {
            org.setStatus("0");
        }
        orgMapper.insert(org);
        return org.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateOrg(FeeOrgBo bo) {
        FeeOrg exist = orgMapper.selectById(bo.getId());
        if (exist == null) {
            throw new ServiceException("组织不存在");
        }
        checkOrgCodeUnique(bo.getOrgCode(), bo.getId());
        FeeOrg org = BeanUtil.copyProperties(bo, FeeOrg.class);
        long parentId = bo.getParentId() == null ? 0L : bo.getParentId();
        if (Objects.equals(parentId, bo.getId())) {
            throw new ServiceException("父组织不能是自身");
        }
        org.setParentId(parentId);
        String newAncestors = buildAncestors(parentId);
        org.setAncestors(newAncestors);
        // 父级变化时同步修正下级祖级列表
        if (!Objects.equals(parentId, exist.getParentId())) {
            if (parentId != 0L) {
                FeeOrg parent = orgMapper.selectById(parentId);
                if (parent == null) {
                    throw new ServiceException("父组织不存在");
                }
                if (StrUtil.splitTrim(parent.getAncestors(), ",").contains(String.valueOf(bo.getId()))) {
                    throw new ServiceException("不能将组织移动到其下级组织下");
                }
            }
            updateChildrenAncestors(exist, newAncestors);
        }
        return orgMapper.updateById(org) > 0;
    }

    @Override
    public Boolean deleteOrg(Long id) {
        long childCount = orgMapper.selectCount(Wrappers.<FeeOrg>lambdaQuery().eq(FeeOrg::getParentId, id));
        if (childCount > 0) {
            throw new ServiceException("存在下级组织，不允许删除");
        }
        long userCount = orgUserMapper.selectCount(Wrappers.<FeeOrgUser>lambdaQuery().eq(FeeOrgUser::getOrgId, id));
        if (userCount > 0) {
            throw new ServiceException("组织下存在成员，请先移除成员");
        }
        return orgMapper.deleteById(id) > 0;
    }

    @Override
    public List<FeeOrgUserVo> listOrgUsers(Long orgId) {
        List<FeeOrgUserVo> list = orgUserMapper.selectVoList(Wrappers.<FeeOrgUser>lambdaQuery()
            .eq(FeeOrgUser::getOrgId, orgId));
        FeeOrg org = orgMapper.selectById(orgId);
        list.forEach(vo -> vo.setOrgName(org == null ? null : org.getOrgName()));
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveOrgUsers(FeeOrgUserBo bo) {
        FeeOrg org = orgMapper.selectById(bo.getOrgId());
        if (org == null) {
            throw new ServiceException("组织不存在");
        }
        // 全量保存：先删后插
        orgUserMapper.delete(Wrappers.<FeeOrgUser>lambdaQuery().eq(FeeOrgUser::getOrgId, bo.getOrgId()));
        List<FeeOrgUserBo.OrgUserItem> users = bo.getUsers();
        if (users == null || users.isEmpty()) {
            return true;
        }
        Set<Long> seen = new HashSet<>();
        for (FeeOrgUserBo.OrgUserItem item : users) {
            if (!seen.add(item.getUserId())) {
                continue;
            }
            FeeOrgUser rel = new FeeOrgUser();
            rel.setOrgId(bo.getOrgId());
            rel.setUserId(item.getUserId());
            rel.setUserName(item.getUserName());
            rel.setIsPrimary(item.getIsPrimary() == null ? 1 : item.getIsPrimary());
            orgUserMapper.insert(rel);
        }
        return true;
    }

    @Override
    public List<Long> visibleOrgIds() {
        // admin 全量可见
        if (LoginHelper.isAdmin()) {
            return null;
        }
        Long userId = LoginHelper.getUserId();
        List<FeeOrgUser> myOrgs = orgUserMapper.selectList(Wrappers.<FeeOrgUser>lambdaQuery()
            .eq(FeeOrgUser::getUserId, userId));
        // 未挂组织用户视为集团视角（存量数据默认挂总部，不影响既有流程，设计文档 6.3）
        if (myOrgs.isEmpty()) {
            return null;
        }
        List<Long> myOrgIds = myOrgs.stream().map(FeeOrgUser::getOrgId).distinct().collect(Collectors.toList());
        Set<Long> visible = new HashSet<>(myOrgIds);
        // 下级组织：ancestors 包含任一本人组织ID
        List<FeeOrg> all = orgMapper.selectList(Wrappers.lambdaQuery());
        for (FeeOrg org : all) {
            if (visible.contains(org.getId())) {
                continue;
            }
            List<String> ancestorIds = StrUtil.splitTrim(org.getAncestors(), ",");
            for (Long myOrgId : myOrgIds) {
                if (ancestorIds.contains(String.valueOf(myOrgId))) {
                    visible.add(org.getId());
                    break;
                }
            }
        }
        return new ArrayList<>(visible);
    }

    /**
     * 构建祖级列表：父级祖级 + 父级ID（根节点为 "0"）
     */
    private String buildAncestors(long parentId) {
        if (parentId == 0L) {
            return "0";
        }
        FeeOrg parent = orgMapper.selectById(parentId);
        if (parent == null) {
            throw new ServiceException("父组织不存在");
        }
        return parent.getAncestors() + "," + parent.getId();
    }

    /**
     * 父级变化时批量修正下级祖级列表
     */
    private void updateChildrenAncestors(FeeOrg old, String newAncestors) {
        String oldPrefix = old.getAncestors() + "," + old.getId();
        String newPrefix = newAncestors + "," + old.getId();
        List<FeeOrg> children = orgMapper.selectList(Wrappers.<FeeOrg>lambdaQuery()
            .likeRight(FeeOrg::getAncestors, oldPrefix));
        for (FeeOrg child : children) {
            child.setAncestors(newPrefix + child.getAncestors().substring(oldPrefix.length()));
            orgMapper.updateById(child);
        }
    }

    private void checkOrgCodeUnique(String orgCode, Long excludeId) {
        if (StrUtil.isBlank(orgCode)) {
            return;
        }
        long count = orgMapper.selectCount(Wrappers.<FeeOrg>lambdaQuery()
            .eq(FeeOrg::getOrgCode, orgCode)
            .ne(excludeId != null, FeeOrg::getId, excludeId));
        if (count > 0) {
            throw new ServiceException("组织编码已存在：" + orgCode);
        }
    }

    private void fillUserCount(List<FeeOrgVo> list) {
        if (list.isEmpty()) {
            return;
        }
        List<FeeOrgUser> rels = orgUserMapper.selectList(Wrappers.lambdaQuery());
        Map<Long, Long> countMap = rels.stream()
            .collect(Collectors.groupingBy(FeeOrgUser::getOrgId, Collectors.counting()));
        list.forEach(vo -> vo.setUserCount(countMap.getOrDefault(vo.getId(), 0L).intValue()));
    }

    private void fillLeaderName(List<FeeOrgVo> list) {
        List<Long> leaderIds = list.stream().map(FeeOrgVo::getLeaderUserId)
            .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (leaderIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = userMapper.selectBatchIds(leaderIds).stream()
            .collect(Collectors.toMap(SysUser::getUserId, SysUser::getNickName, (a, b) -> a));
        list.forEach(vo -> vo.setLeaderUserName(nameMap.get(vo.getLeaderUserId())));
    }

    private List<FeeOrgVo> buildTree(List<FeeOrgVo> list) {
        Map<Long, FeeOrgVo> map = list.stream().collect(Collectors.toMap(FeeOrgVo::getId, v -> v));
        List<FeeOrgVo> roots = new ArrayList<>();
        for (FeeOrgVo vo : list) {
            FeeOrgVo parent = vo.getParentId() == null ? null : map.get(vo.getParentId());
            if (parent == null || vo.getParentId() == 0L) {
                roots.add(vo);
            } else {
                parent.getChildren().add(vo);
            }
        }
        roots.sort(Comparator.comparing(v -> v.getSortOrder() == null ? 0 : v.getSortOrder()));
        return roots;
    }
}
