package com.ruoyi.system.service;

import com.ruoyi.system.domain.FeeComplianceRecord;

/**
 * 发票合规校验Service接口
 *
 * @author ruoyi
 * @date 2026-07-23
 */
public interface IComplianceService {

    /**
     * 对报销单执行合规校验（提交前调用）
     *
     * @param billId            报销单ID
     * @param budgetSubjectCode 预算科目编码
     * @param industryCode      行业编码
     * @return 合规校验记录
     */
    FeeComplianceRecord checkCompliance(Long billId, String billNo, String budgetSubjectCode, String industryCode);

    /**
     * 按科目查询合规级别
     *
     * @param budgetSubjectCode 预算科目编码
     * @param industryCode      行业编码
     * @return 合规级别（SIMPLE/NORMAL/STRICT）
     */
    String getComplianceLevel(String budgetSubjectCode, String industryCode);

    /**
     * 财务确认盖章（严格模式）
     *
     * @param recordId 记录ID
     * @return 是否成功
     */
    Boolean confirmSeal(Long recordId);

    /**
     * 查询报销单合规记录
     *
     * @param billId 报销单ID
     * @return 合规记录
     */
    FeeComplianceRecord getRecordByBillId(Long billId);
}
