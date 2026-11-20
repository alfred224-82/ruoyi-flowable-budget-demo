package com.ruoyi.system.service;

import com.ruoyi.system.domain.ActivityStaffSnapshot;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * 活动员工分成快照服务接口（M35 T35.3）
 * <p>活动创建/编辑选中员工时按当前 {@link com.ruoyi.system.domain.StaffCommissionRule} 生成快照；
 * 规则变更后历史活动仍用旧快照，历史账不可变。
 * <p>事实变更（删除缺席员工）允许：重新生成快照时按最新选中员工集合覆盖，
 * 报表按实际重算。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
public interface IActivityStaffSnapshotService {

    /**
     * 按活动当前选中员工集合重建快照（先删后插，幂等）。
     * <p>活动创建/编辑保存时调用；员工规则变更后历史活动不受影响（因快照已固化）。
     *
     * @param activityId 活动ID
     * @param staffIds   选中员工ID列表（可空表示清空）
     * @return 生成的快照条数
     */
    int rebuildSnapshots(Long activityId, Collection<Long> staffIds);

    /** 查询某活动的所有快照 */
    List<ActivityStaffSnapshot> listByActivityId(Long activityId);

    /** 查询某活动某员工的快照（唯一） */
    ActivityStaffSnapshot getByActivityAndStaff(Long activityId, Long staffId);

    /**
     * 汇总某活动的员工成本（快照 calculated_amount 之和；未结算时按试算值估算）。
     * <p>供 M36 活动结算单成本项自动生成 + T35.7 报表使用。
     */
    BigDecimal sumEmployeeCost(Long activityId);

    /**
     * 删除某活动的所有快照（活动删除时级联清理）。
     */
    int deleteByActivityId(Long activityId);
}
