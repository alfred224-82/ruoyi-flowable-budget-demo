package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.system.domain.CostCenter;
import com.ruoyi.system.domain.CostCenterDept;
import com.ruoyi.system.domain.bo.CostCenterBo;
import com.ruoyi.system.domain.vo.CostCenterVo;
import com.ruoyi.system.mapper.CostCenterDeptMapper;
import com.ruoyi.system.mapper.CostCenterMapper;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.system.service.ICostCenterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 责任中心Service业务层处理
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CostCenterServiceImpl implements ICostCenterService {

    private final CostCenterMapper costCenterMapper;
    private final CostCenterDeptMapper costCenterDeptMapper;
    private final SysDeptMapper sysDeptMapper;

    @Override
    public CostCenterVo queryById(Long id) {
        return costCenterMapper.selectVoById(id);
    }

    @Override
    public Page<CostCenterVo> selectPageList(CostCenterBo bo, PageQuery pageQuery) {
        Wrapper<CostCenter> wrapper = buildQueryWrapper(bo);
        return costCenterMapper.selectVoPage(pageQuery.build(), wrapper);
    }

    @Override
    public Boolean insertByBo(CostCenterBo bo) {
        CostCenter costCenter = BeanCopyUtils.copy(bo, CostCenter.class);
        boolean flag = costCenterMapper.insert(costCenter) > 0;
        if (flag) {
            bo.setId(costCenter.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(CostCenterBo bo) {
        CostCenter costCenter = BeanCopyUtils.copy(bo, CostCenter.class);
        return costCenterMapper.updateById(costCenter) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        // 检查是否有部门关联
        for (Long id : ids) {
            CostCenter costCenter = costCenterMapper.selectById(id);
            if (costCenter != null) {
                Long count = costCenterDeptMapper.selectCount(
                    Wrappers.<CostCenterDept>lambdaQuery()
                        .eq(CostCenterDept::getCenterCode, costCenter.getCenterCode()));
                if (count > 0) {
                    throw new ServiceException("责任中心[" + costCenter.getCenterName() + "]已关联部门，无法删除");
                }
            }
        }
        return costCenterMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveDeptMapping(String centerCode, List<Long> deptIds) {
        // 1. 删除该责任中心旧关联
        costCenterDeptMapper.delete(
            Wrappers.<CostCenterDept>lambdaQuery()
                .eq(CostCenterDept::getCenterCode, centerCode));

        // 2. 批量插入新关联
        if (deptIds != null && !deptIds.isEmpty()) {
            for (Long deptId : deptIds) {
                CostCenterDept dept = new CostCenterDept();
                dept.setCenterCode(centerCode);
                dept.setDeptId(deptId);
                // 查询部门名称（冗余存储）
                SysDept sysDept = sysDeptMapper.selectById(deptId);
                if (sysDept != null) {
                    dept.setDeptName(sysDept.getDeptName());
                }
                costCenterDeptMapper.insert(dept);
            }
        }

        // 3. 清除缓存
        RedisUtils.deleteKeys("fee:cost_center:dept:*");
        return true;
    }

    @Override
    public List<SysDept> queryDeptList(String centerCode) {
        List<CostCenterDept> deptList = costCenterDeptMapper.selectList(
            Wrappers.<CostCenterDept>lambdaQuery()
                .eq(CostCenterDept::getCenterCode, centerCode));

        List<SysDept> result = new ArrayList<>();
        for (CostCenterDept ccd : deptList) {
            SysDept dept = sysDeptMapper.selectById(ccd.getDeptId());
            if (dept != null) {
                result.add(dept);
            }
        }
        return result;
    }

    @Override
    public String getCenterCodeByDeptId(Long deptId) {
        if (deptId == null) {
            return null;
        }
        // 优先从缓存获取
        String cached = RedisUtils.getCacheObject("fee:cost_center:dept:" + deptId);
        if (cached != null) {
            return cached;
        }

        CostCenterDept dept = costCenterDeptMapper.selectOne(
            Wrappers.<CostCenterDept>lambdaQuery()
                .eq(CostCenterDept::getDeptId, deptId));
        if (dept != null) {
            RedisUtils.setCacheObject("fee:cost_center:dept:" + deptId,
                dept.getCenterCode(), Duration.ofHours(1));
            return dept.getCenterCode();
        }
        return null; // 未映射
    }

    /**
     * 构建查询条件
     */
    private Wrapper<CostCenter> buildQueryWrapper(CostCenterBo bo) {
        return Wrappers.<CostCenter>lambdaQuery()
            .like(StrUtil.isNotBlank(bo.getCenterCode()), CostCenter::getCenterCode, bo.getCenterCode())
            .like(StrUtil.isNotBlank(bo.getCenterName()), CostCenter::getCenterName, bo.getCenterName())
            .eq(StrUtil.isNotBlank(bo.getStatus()), CostCenter::getStatus, bo.getStatus())
            .orderByAsc(CostCenter::getCenterCode);
    }
}
