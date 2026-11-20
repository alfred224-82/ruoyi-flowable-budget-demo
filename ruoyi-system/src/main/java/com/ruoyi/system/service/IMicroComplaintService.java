package com.ruoyi.system.service;

import com.ruoyi.system.domain.bo.ComplaintBo;
import com.ruoyi.system.domain.bo.ComplaintReplyBo;
import com.ruoyi.system.domain.vo.ComplaintRecordVo;

import java.util.List;
import java.util.Map;

/**
 * 投诉服务接口（T25.1，小微版 M25）
 * <p>双渠道投诉：BOSS=渠道一（Boss 门店内部）/ PLATFORM=渠道二（平台 Admin 违法违规）。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
public interface IMicroComplaintService {

    /**
     * 投诉提交（T25.1 双渠道）
     * <p>渠道一（BOSS）：无额外门槛。
     * <p>渠道二（PLATFORM）：必须拍照取证+文字≥20字。
     *
     * @param bo 投诉提交 BO
     * @return 投诉记录ID
     */
    Long submitComplaint(ComplaintBo bo);

    /**
     * 本人投诉记录（顾客小程序）
     *
     * @param channel 渠道筛选（可空=全部）
     */
    List<ComplaintRecordVo> myComplaints(String channel);

    /**
     * Boss 查看本租户投诉列表（T25.4 Boss PC）
     *
     * @param channel 渠道筛选（可空）
     * @param status  状态筛选（可空）
     */
    List<ComplaintRecordVo> listForBoss(String channel, String status);

    /**
     * Boss 处理投诉（T25.4）
     *
     * @param id  投诉记录ID
     * @param bo  处理回复 BO
     */
    void replyComplaint(Long id, ComplaintReplyBo bo);

    /**
     * 投诉页员工下拉数据源（本租户全部 salesman 角色用户，不过滤空闲状态）
     *
     * @return 员工列表（userId + nickName）
     */
    List<Map<String, Object>> listStaffForComplaint();
}
