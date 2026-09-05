package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.FeeReceiptBo;
import com.ruoyi.system.domain.vo.FeeReceiptVo;

/**
 * 收款单Service接口（T5.3）
 * 必选项目、财务创建审核、联动生成"已核验"收入确认
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IFeeReceiptService {

    /**
     * 分页查询
     */
    TableDataInfo<FeeReceiptVo> queryPageList(FeeReceiptBo bo, PageQuery pageQuery);

    /**
     * 详情
     */
    FeeReceiptVo queryById(Long id);

    /**
     * 财务创建收款单（状态=待审核）
     */
    Long insertReceipt(FeeReceiptBo bo);

    /**
     * 财务审核通过 → 更新项目收入 + 联动生成已核验收入确认
     */
    int approveReceipt(Long id);

    /**
     * 审核驳回
     */
    int rejectReceipt(Long id, String reason);
}
