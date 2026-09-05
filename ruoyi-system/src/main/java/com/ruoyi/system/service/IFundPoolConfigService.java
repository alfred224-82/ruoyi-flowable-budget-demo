package com.ruoyi.system.service;

import com.ruoyi.system.domain.bo.FundPoolConfigBo;
import com.ruoyi.system.domain.vo.FundPoolConfigVo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 资金池配置服务接口
 *
 * @author ruoyi
 * @date 2026-07-24
 */
public interface IFundPoolConfigService {

    /**
     * 查询全部资金池配置
     */
    List<FundPoolConfigVo> listAll();

    /**
     * 保存/更新资金池配置（批量）
     */
    Boolean saveConfigs(List<FundPoolConfigBo> boList);

    /**
     * 获取指定层级的初始额度（供资金池自动创建时调用）
     */
    BigDecimal getInitialAmount(String levelType);

    /**
     * 获取指定层级的预警阈值
     */
    BigDecimal getWarningThreshold(String levelType);

    /**
     * 根据角色Key获取对应层级类型
     */
    String getLevelTypeByRoleKey(String roleKey);

    /**
     * 按配置自动创建当期资金池（每月预算编制启动时调用）
     *
     * @param period    预算期间（YYYY-MM）
     * @param levelType 层级
     * @param deptId    部门ID（HQ级为null）
     * @param deptName  部门名称
     * @return 创建的 ReservedBudget ID（已存在则返回现有ID）
     */
    Long autoCreateFundPool(String period, String levelType, Long deptId, String deptName);
}
