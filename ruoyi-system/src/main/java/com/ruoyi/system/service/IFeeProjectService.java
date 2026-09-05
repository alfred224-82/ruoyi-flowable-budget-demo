package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.FeeProjectBo;
import com.ruoyi.system.domain.vo.FeeMemberLoadVo;
import com.ruoyi.system.domain.vo.FeeProjectMemberVo;
import com.ruoyi.system.domain.vo.FeeProjectProfitVo;
import com.ruoyi.system.domain.vo.FeeProjectSubjectVo;
import com.ruoyi.system.domain.vo.FeeProjectVo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 项目信息Service接口（T5.1）
 * 立项（老板建/财务代建/项目经理申请）、审批、项目预算按大类分配、项目科目、成员管理
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IFeeProjectService {

    /**
     * 分页查询项目列表（数据范围：项目经理仅本人参与项目）
     */
    TableDataInfo<FeeProjectVo> queryPageList(FeeProjectBo bo, PageQuery pageQuery);

    /**
     * 项目详情
     */
    FeeProjectVo queryById(Long id);

    /**
     * 创建项目（状态=待审批），含科目与成员
     */
    Long insertProject(FeeProjectBo bo);

    /**
     * 编辑项目（仅 待审批/草稿 状态）
     */
    int updateProject(FeeProjectBo bo);

    /**
     * 老板审批通过 → ACTIVE + 生成项目科目并汇入大类
     */
    int approveProject(Long id);

    /**
     * 审批驳回
     */
    int rejectProject(Long id, String reason);

    /**
     * 关闭项目
     */
    int closeProject(Long id);

    /**
     * 进行中项目下拉
     */
    List<FeeProjectVo> listActiveProjects();

    /**
     * 项目利润拆解（纸面/实际分离）
     */
    FeeProjectProfitVo getProfitDetail(Long projectId);

    /**
     * 项目科目列表
     */
    List<FeeProjectSubjectVo> listSubjects(Long projectId);

    /**
     * 批量设置项目科目（按大类分配预算）
     */
    int saveSubjects(Long projectId, List<FeeProjectBo.FeeProjectSubjectBo> subjects);

    /**
     * 项目成员列表
     */
    List<FeeProjectMemberVo> listMembers(Long projectId);

    /**
     * 批量保存项目成员
     */
    int saveMembers(Long projectId, List<FeeProjectBo.FeeProjectMemberBo> members);

    /**
     * 查询大类下可用项目预算余额（科目合计剩余）
     */
    BigDecimal getAvailableBudget(Long projectId, Long categoryId);

    /**
     * 扣减项目预算（按科目顺序扣减），成功更新项目已用预算与总支出
     *
     * @param categoryId 可空=不限大类按项目全部科目扣减
     * @param addExpense 是否同时计入项目总支出
     */
    boolean deductBudget(Long projectId, Long categoryId, BigDecimal amount, boolean addExpense);

    /**
     * 追加项目收入（收款/收入核验联动）
     *
     * @param verified true=已核验收入，false=待核验收入
     */
    void addIncome(Long projectId, BigDecimal amount, boolean verified);

    /**
     * 待核验收入变动（正数新增、负数冲销）
     */
    void addPendingIncome(Long projectId, BigDecimal amount);

    /**
     * 追加项目支出（不扣预算场景，如坏账计提）
     */
    void addExpense(Long projectId, BigDecimal amount);

    /**
     * 当前用户是否为指定项目成员（数据范围用）
     */
    boolean isProjectMember(Long projectId, Long userId);

    /**
     * 当前用户的项目数据范围（null=不限；非空=仅可见项目ID集，可能为空集）
     * 供利润报表等下游服务复用（T6.5）
     */
    List<Long> scopedProjectIds();

    /**
     * 成员跨项目负载检查（T7.5 / FEAT_MEMBER_LOAD）
     * 拉人进项目时展示“已在哪些项目” + 投入比例合计 + 是否超限（仅提示不拦截）
     */
    FeeMemberLoadVo memberLoadCheck(Long userId);
}
