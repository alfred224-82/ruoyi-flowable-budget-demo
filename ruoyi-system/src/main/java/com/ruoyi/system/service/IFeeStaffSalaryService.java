package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.FeeStaffSalary;
import com.ruoyi.system.domain.vo.FeeSalaryRecordVo;
import com.ruoyi.system.domain.vo.FeeStaffSalaryVo;

/**
 * 员工薪资Service接口（T5.9 / ADR-024，公司级方案A）
 * 薪资社保基数维护 + 每月计提
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IFeeStaffSalaryService {

    /**
     * 薪资基数分页查询
     */
    TableDataInfo<FeeStaffSalaryVo> queryPageList(String status, PageQuery pageQuery);

    /**
     * 新增/修改薪资基数（userId 唯一）
     */
    Long saveSalary(FeeStaffSalary salary);

    /**
     * 离职（状态=RESIGNED，停止计提）
     */
    int resign(Long id);

    /**
     * 计提记录分页查询
     */
    TableDataInfo<FeeSalaryRecordVo> queryRecordPageList(String accrualMonth, PageQuery pageQuery);

    /**
     * 月度薪资计提（幂等：同月同人仅一条）
     *
     * @param accrualMonth 计提月份 yyyy-MM，空=上一自然月
     * @return 计提条数
     */
    int accrueSalaries(String accrualMonth);
}
