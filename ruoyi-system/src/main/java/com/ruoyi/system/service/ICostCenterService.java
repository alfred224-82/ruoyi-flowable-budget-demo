package com.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.system.domain.bo.CostCenterBo;
import com.ruoyi.system.domain.vo.CostCenterVo;

import java.util.Collection;
import java.util.List;

/**
 * 责任中心Service接口
 *
 * @author ruoyi
 * @date 2026-07-13
 */
public interface ICostCenterService {

    /**
     * 查询责任中心详情
     *
     * @param id 主键ID
     * @return 责任中心VO
     */
    CostCenterVo queryById(Long id);

    /**
     * 分页查询责任中心列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    Page<CostCenterVo> selectPageList(CostCenterBo bo, PageQuery pageQuery);

    /**
     * 新增责任中心
     *
     * @param bo 业务对象
     * @return 影响行数
     */
    Boolean insertByBo(CostCenterBo bo);

    /**
     * 修改责任中心
     *
     * @param bo 业务对象
     * @return 影响行数
     */
    Boolean updateByBo(CostCenterBo bo);

    /**
     * 批量删除责任中心
     *
     * @param ids 主键集合
     * @return 影响行数
     */
    Boolean deleteWithValidByIds(Collection<Long> ids);

    /**
     * 批量设置责任中心-部门关联
     *
     * @param centerCode 责任中心编码
     * @param deptIds    部门ID列表
     * @return 影响行数
     */
    Boolean saveDeptMapping(String centerCode, List<Long> deptIds);

    /**
     * 查询责任中心下的部门列表
     *
     * @param centerCode 责任中心编码
     * @return 部门列表
     */
    List<SysDept> queryDeptList(String centerCode);

    /**
     * 根据部门ID获取责任中心编码
     *
     * @param deptId 部门ID
     * @return 责任中心编码
     */
    String getCenterCodeByDeptId(Long deptId);
}
