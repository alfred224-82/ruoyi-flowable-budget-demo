package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.AccountActionBo;
import com.ruoyi.system.domain.vo.SysAccountActionLogVo;

/**
 * 用户数据操作服务接口（T14.4 数据导出 / 账号注销，7 日内完成并留痕）
 * <p>状态机：apply → PENDING（deadline = 申请后 7 日）；handle → DONE / REJECTED（回写结果与处理人）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IAccountActionService {

    /** 操作类型：数据导出 */
    String ACTION_DATA_EXPORT = "DATA_EXPORT";
    /** 操作类型：账号注销 */
    String ACTION_ACCOUNT_DELETE = "ACCOUNT_DELETE";

    /**
     * 发起数据导出/账号注销申请：写入 PENDING 并设置 7 日完成截止时间
     *
     * @return 影响行数
     */
    int apply(AccountActionBo bo);

    /**
     * 处理申请：DONE（完成，回填结果引用）/ REJECTED（拒绝），记录处理人与完成时间
     *
     * @return 影响行数
     */
    int handle(AccountActionBo bo);

    /**
     * 申请分页查询
     *
     * @param actionType 操作类型（可空）
     * @param status     状态（可空）
     */
    TableDataInfo<SysAccountActionLogVo> queryPageList(String actionType, String status, PageQuery pageQuery);

    /**
     * 申请详情
     */
    SysAccountActionLogVo queryById(Long id);
}
