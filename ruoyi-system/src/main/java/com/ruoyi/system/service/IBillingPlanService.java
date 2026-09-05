package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.SysBillingPlanBo;
import com.ruoyi.system.domain.vo.SysBillingPlanVo;

import java.util.List;

/**
 * 计费套餐服务接口（T14.1 四档套餐：试用/首月特惠/月付/包年）
 * <p>sys_billing_plan 为系统级共享表（无 tenant_id），平台超管统一维护。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IBillingPlanService {

    /**
     * 套餐分页列表
     *
     * @param planCode 套餐编码（可空，模糊）
     * @param status   状态（可空，0上架 1下架）
     */
    TableDataInfo<SysBillingPlanVo> queryPageList(String planCode, String status, PageQuery pageQuery);

    /**
     * 上架套餐列表（供下单选择，status=0，按 sort 升序）
     */
    List<SysBillingPlanVo> listEnabled();

    /**
     * 按套餐编码查询
     */
    SysBillingPlanVo queryByCode(String planCode);

    /**
     * 新增套餐
     */
    int insert(SysBillingPlanBo bo);

    /**
     * 编辑套餐
     */
    int update(SysBillingPlanBo bo);
}
