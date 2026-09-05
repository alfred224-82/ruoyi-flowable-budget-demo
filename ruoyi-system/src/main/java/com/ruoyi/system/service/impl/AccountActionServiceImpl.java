package com.ruoyi.system.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.SysAccountActionLog;
import com.ruoyi.system.domain.bo.AccountActionBo;
import com.ruoyi.system.domain.vo.SysAccountActionLogVo;
import com.ruoyi.system.mapper.SysAccountActionLogMapper;
import com.ruoyi.system.service.IAccountActionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 用户数据操作服务实现（T14.4）
 * <p>sys_account_action_log 含 apply_time/finish_time（无标准审计列），故实体不继承 BaseEntity，
 * tenantId/userId/applyTime/operator 等由服务层显式写入。
 * <p>合规要求：申请后 7 日内完成（deadline 留痕），处理结果可追溯。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountActionServiceImpl implements IAccountActionService {

    /** 完成时限（天）：合规要求 7 日内 */
    private static final int DEADLINE_DAYS = 7;

    private final SysAccountActionLogMapper actionMapper;

    @Override
    public int apply(AccountActionBo bo) {
        if (!ACTION_DATA_EXPORT.equals(bo.getActionType()) && !ACTION_ACCOUNT_DELETE.equals(bo.getActionType())) {
            throw new ServiceException("操作类型须为 DATA_EXPORT 或 ACCOUNT_DELETE");
        }
        Date now = new Date();
        SysAccountActionLog entity = new SysAccountActionLog();
        entity.setTenantId(LoginHelper.getTenantId());
        entity.setUserId(bo.getUserId() != null ? bo.getUserId() : LoginHelper.getUserId());
        entity.setActionType(bo.getActionType());
        entity.setStatus("PENDING");
        entity.setApplyTime(now);
        entity.setDeadline(DateUtil.offsetDay(now, DEADLINE_DAYS));
        entity.setRemark(bo.getRemark());
        int rows = actionMapper.insert(entity);
        log.info("[AccountAction] 用户 {} 发起 {} 申请，截止 {}", entity.getUserId(), bo.getActionType(),
            DateUtil.formatDateTime(entity.getDeadline()));
        return rows;
    }

    @Override
    public int handle(AccountActionBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("申请ID不能为空");
        }
        SysAccountActionLog exist = actionMapper.selectById(bo.getId());
        if (exist == null) {
            throw new ServiceException("申请不存在");
        }
        if ("DONE".equals(exist.getStatus()) || "REJECTED".equals(exist.getStatus())) {
            throw new ServiceException("该申请已处理，无法重复操作");
        }
        String status = bo.getStatus();
        if (!"DONE".equals(status) && !"REJECTED".equals(status)) {
            throw new ServiceException("处理状态须为 DONE 或 REJECTED");
        }
        SysAccountActionLog update = new SysAccountActionLog();
        update.setId(bo.getId());
        update.setStatus(status);
        update.setResultRef(bo.getResultRef());
        update.setOperator(LoginHelper.getUsername());
        update.setFinishTime(new Date());
        if (StrUtil.isNotBlank(bo.getRemark())) {
            update.setRemark(bo.getRemark());
        }
        int rows = actionMapper.updateById(update);
        log.info("[AccountAction] 申请 {} 处理为 {}", bo.getId(), status);
        return rows;
    }

    @Override
    public TableDataInfo<SysAccountActionLogVo> queryPageList(String actionType, String status, PageQuery pageQuery) {
        LambdaQueryWrapper<SysAccountActionLog> wrapper = Wrappers.<SysAccountActionLog>lambdaQuery()
            .eq(StrUtil.isNotBlank(actionType), SysAccountActionLog::getActionType, actionType)
            .eq(StrUtil.isNotBlank(status), SysAccountActionLog::getStatus, status)
            .orderByDesc(SysAccountActionLog::getApplyTime);
        Page<SysAccountActionLogVo> page = actionMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public SysAccountActionLogVo queryById(Long id) {
        SysAccountActionLogVo vo = actionMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("申请不存在");
        }
        return vo;
    }
}
