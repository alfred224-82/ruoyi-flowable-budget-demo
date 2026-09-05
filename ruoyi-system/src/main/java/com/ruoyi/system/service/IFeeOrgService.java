package com.ruoyi.system.service;

import com.ruoyi.system.domain.bo.FeeOrgBo;
import com.ruoyi.system.domain.bo.FeeOrgUserBo;
import com.ruoyi.system.domain.vo.FeeOrgUserVo;
import com.ruoyi.system.domain.vo.FeeOrgVo;

import java.util.List;

/**
 * 组织管理Service接口（T7.1 / FEAT_MULTI_ORG）
 * 组织树 + 用户挂组织 + 数据范围（本组织+下级组织）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IFeeOrgService {

    /**
     * 组织树（含成员数统计）
     */
    List<FeeOrgVo> selectOrgTree();

    /**
     * 新增组织
     */
    Long insertOrg(FeeOrgBo bo);

    /**
     * 修改组织（父级变化时同步修正下级祖级列表）
     */
    Boolean updateOrg(FeeOrgBo bo);

    /**
     * 删除组织（存在子组织或成员时禁止）
     */
    Boolean deleteOrg(Long id);

    /**
     * 组织成员列表
     */
    List<FeeOrgUserVo> listOrgUsers(Long orgId);

    /**
     * 全量保存组织成员（用户挂组织）
     */
    Boolean saveOrgUsers(FeeOrgUserBo bo);

    /**
     * 当前用户可见组织ID集合（数据范围=本组织+下级组织）
     *
     * @return 可见组织ID；null 表示全量可见（admin 或未挂组织用户）
     */
    List<Long> visibleOrgIds();
}
