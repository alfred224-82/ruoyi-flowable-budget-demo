package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.FeeProjectSharing;
import com.ruoyi.system.domain.vo.FeeProjectSharingVo;

/**
 * 项目分成Service接口（T5.9 / ADR-025）
 * 老板定比例，项目经理仅可申请；审批通过后计入项目分成
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IFeeProjectSharingService {

    /**
     * 分页查询
     */
    TableDataInfo<FeeProjectSharingVo> queryPageList(Long projectId, String status, PageQuery pageQuery);

    /**
     * 详情
     */
    FeeProjectSharingVo queryById(Long id);

    /**
     * 申请分成（状态=待审批）
     */
    Long insertSharing(FeeProjectSharing sharing);

    /**
     * 老板审批（设定比例/基数，计算分成金额，状态=生效）
     */
    int approveSharing(Long id, java.math.BigDecimal ratio, java.math.BigDecimal baseAmount);

    /**
     * 老板驳回
     */
    int rejectSharing(Long id);

    /**
     * 标记已支付（状态=PAID）
     */
    int paySharing(Long id);
}
