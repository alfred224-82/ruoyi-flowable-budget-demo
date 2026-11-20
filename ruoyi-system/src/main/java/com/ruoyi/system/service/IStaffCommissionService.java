package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.StaffCommissionBo;
import com.ruoyi.system.domain.vo.StaffCommissionVo;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 员工提成比例配置服务接口（REQ-03，v1.3 需求清单，仅 PC）
 * <p>员工当月薪酬成本 = 基础工资 + 提成；提成 = 当月基数（员工当月已完成订单金额）× 提成比例。
 * <p>提成基数复用 StaffSettlement 口径：booking_order.staff_user_id + order_status=COMPLETED
 * + complete_time 落在当月，汇总订单金额（amount）。
 *
 * @author ruoyi
 * @date 2026-10-16
 */
public interface IStaffCommissionService {

    /**
     * 分页查询员工提成配置（含实时回填当月基数/当月提成/当月人力成本）
     */
    TableDataInfo<StaffCommissionVo> queryPageList(StaffCommissionBo bo, PageQuery pageQuery);

    /**
     * 查询员工提成配置列表（不分页，导出用；含实时回填当月基数/提成）
     */
    List<StaffCommissionVo> queryList(StaffCommissionBo bo);

    /**
     * 提成配置详情
     */
    StaffCommissionVo queryById(Long id);

    /**
     * 新增提成配置（每员工唯一，重复则拒绝）
     *
     * @return 主键ID
     */
    Long insert(StaffCommissionBo bo);

    /**
     * 修改提成配置
     */
    int update(StaffCommissionBo bo);

    /**
     * 逻辑删除提成配置（可批量）
     */
    int deleteByIds(Collection<Long> ids);

    /**
     * 计算某员工某月提成（= 当月已完成订单金额 × 提成比例）
     *
     * @param staffUserId 员工用户ID
     * @param month       月份（yyyy-MM）
     * @return 当月提成；无配置或比例为 0 返回 0
     */
    BigDecimal calcMonthlyCommission(Long staffUserId, String month);

    /**
     * 汇总某月「人力成本」= Σ(基础工资 + 当月提成)，仅统计启用(status=0)配置。
     * <p>供经营驾驶舱成本构成「人力成本」分类聚合使用。
     *
     * @param month 月份（yyyy-MM）
     * @return 当月人力成本合计（2 位 HALF_UP）
     */
    BigDecimal sumMonthlyLaborCost(String month);

    /**
     * 员工下拉选项（本租户 salesman 角色员工： userId/nickName/avatar），供新增/编辑选择器使用。
     */
    List<Map<String, Object>> listStaffOptions();
}
