package com.ruoyi.system.service;

import com.ruoyi.system.domain.bo.CheckInBo;
import com.ruoyi.system.domain.bo.CheckInSupplementBo;
import com.ruoyi.system.domain.vo.CheckInConfigVo;
import com.ruoyi.system.domain.vo.CheckInRecordVo;

import java.util.Date;
import java.util.List;

/**
 * 员工打卡服务接口（T24.1，小微版 M24）
 * <p>三级开关链路：industry_template.check_in_enabled → sys_tenant.check_in_enabled → check_in_config.enabled
 * <p>★打卡不受活动锁定影响，锁定后仍可打卡。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
public interface ICheckInService {

    // ===== 员工小程序 =====

    /**
     * 员工打卡提交（T24.4）
     * <p>校验三级开关 → 写入 check_in_record → 比对活动开始时间判断迟到 → 生成水印文字。
     *
     * @param bo 活动ID + 照片 + 位置 + 水印
     * @return 打卡记录ID
     */
    Long submitCheckIn(CheckInBo bo);

    /**
     * 员工查看本人打卡记录（T24.4）
     *
     * @param activityId 活动ID（可空=全部活动）
     * @param startDate  开始日期（可空）
     * @param endDate    结束日期（可空）
     */
    List<CheckInRecordVo> myRecords(Long activityId, Date startDate, Date endDate);

    // ===== Boss PC 管理端 =====

    /**
     * 活动打卡列表（T24.5 Boss PC）
     *
     * @param activityId 活动ID
     * @param staffUserId 员工ID（可空=全部员工）
     */
    List<CheckInRecordVo> activityRecords(Long activityId, Long staffUserId);

    /**
     * Boss 补录打卡（T24.5）：代员工补录，source=SUPPLEMENT
     *
     * @param bo 员工ID + 活动ID + 打卡时间 + 地点 + 补录原因
     * @return 补录记录ID
     */
    Long supplementCheckIn(CheckInSupplementBo bo);

    /**
     * 导出打卡记录（T24.5）
     *
     * @param activityId  活动ID（可空）
     * @param staffUserId 员工ID（可空）
     * @param startDate   开始日期（可空）
     * @param endDate     结束日期（可空）
     */
    List<CheckInRecordVo> exportRecords(Long activityId, Long staffUserId, Date startDate, Date endDate);

    // ===== 打卡配置 =====

    /**
     * 获取打卡配置（T24.5）
     */
    CheckInConfigVo getConfig();

    /**
     * 更新打卡配置（T24.5 Boss 设置拍照要求/迟到阈值）
     */
    void updateConfig(CheckInConfigVo configVo);

    // ===== 三级开关校验 =====

    /**
     * 校验三级打卡开关链路是否全部开启
     *
     * @return true=全部开启可打卡；false=任一级别关闭
     */
    boolean isCheckInEnabled();
}
