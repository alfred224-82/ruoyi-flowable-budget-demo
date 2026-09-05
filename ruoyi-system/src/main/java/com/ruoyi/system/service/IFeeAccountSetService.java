package com.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.system.domain.bo.FeeAccountSetBo;
import com.ruoyi.system.domain.vo.FeeAccountSetReportVo;
import com.ruoyi.system.domain.vo.FeeAccountSetVo;

import java.util.List;

/**
 * 多账套Service接口（T7.7 / FEAT_MULTI_ACCOUNT_SET）
 * <p>专业版租户内多账套独立核算：账套 CRUD + 默认账套 + 切换上下文 + 合并报表。
 * 账套间隔离以 account_set_id 为界；合并报表 = 各账套已审批单据汇总。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IFeeAccountSetService {

    /**
     * 分页查询账套列表
     */
    Page<FeeAccountSetVo> queryPageList(FeeAccountSetBo bo, PageQuery pageQuery);

    /**
     * 查询全部账套（下拉/切换用，默认账套置顶）
     */
    List<FeeAccountSetVo> listAll();

    /**
     * 账套详情
     */
    FeeAccountSetVo queryById(Long id);

    /**
     * 新增账套（首个账套自动设为默认）
     *
     * @return 账套ID
     */
    Long insertByBo(FeeAccountSetBo bo);

    /**
     * 修改账套
     */
    Boolean updateByBo(FeeAccountSetBo bo);

    /**
     * 批量删除账套（有单据归属或为默认账套时禁止）
     */
    Boolean deleteByIds(List<Long> ids);

    /**
     * 设为默认账套（每租户至多一个默认）
     */
    Boolean setDefault(Long id);

    /**
     * 切换当前操作账套上下文（token 会话级）
     */
    Boolean switchAccountSet(Long id);

    /**
     * 当前操作账套ID（未切换时回退默认账套）
     *
     * @return 账套ID；无任何账套时返回 null
     */
    Long currentOrDefaultAccountSetId();

    /**
     * 合并报表：各账套已审批单据数量与金额汇总（集团视角）
     *
     * @param period 报销年月（YYYY-MM，空=全部期间）
     */
    List<FeeAccountSetReportVo> consolidatedReport(String period);
}
