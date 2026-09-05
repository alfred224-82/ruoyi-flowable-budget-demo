package com.ruoyi.system.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.SysWithdrawalRequest;
import com.ruoyi.system.domain.bo.WithdrawalApplyBo;
import com.ruoyi.system.domain.bo.WithdrawalAuditBo;
import com.ruoyi.system.domain.vo.SysWithdrawalRequestVo;
import com.ruoyi.system.mapper.SysWithdrawalRequestMapper;
import com.ruoyi.system.service.IVirtualAccountService;
import com.ruoyi.system.service.IWithdrawalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 提现服务实现（T14.3）
 * <p>sys_withdrawal_request 仅含 create_by/create_time/update_time，故实体不继承 BaseEntity；
 * tenantId/createBy 由服务层显式写入，update_time 由 DB ON UPDATE 兜底。
 * <p>申请即冻结余额，审核通过扣减、驳回解冻，全流程 @Transactional 保证资金与状态一致。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalServiceImpl implements IWithdrawalService {

    private final SysWithdrawalRequestMapper withdrawalMapper;
    private final IVirtualAccountService virtualAccountService;

    @Override
    public TableDataInfo<SysWithdrawalRequestVo> queryPageList(String status, Long tenantId, PageQuery pageQuery) {
        LambdaQueryWrapper<SysWithdrawalRequest> wrapper = Wrappers.<SysWithdrawalRequest>lambdaQuery()
            .eq(StrUtil.isNotBlank(status), SysWithdrawalRequest::getStatus, status)
            .eq(tenantId != null, SysWithdrawalRequest::getTenantId, tenantId)
            .orderByDesc(SysWithdrawalRequest::getCreateTime);
        Page<SysWithdrawalRequestVo> page = withdrawalMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public SysWithdrawalRequestVo queryById(Long id) {
        SysWithdrawalRequestVo vo = withdrawalMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("提现申请不存在");
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int apply(WithdrawalApplyBo bo) {
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("无法识别当前租户，提现申请须由租户发起");
        }
        // 冻结可用余额（余额不足内部抛异常，事务回滚）
        virtualAccountService.freeze(tenantId, bo.getAmount());
        SysWithdrawalRequest req = new SysWithdrawalRequest();
        req.setWithdrawNo(genWithdrawNo());
        req.setTenantId(tenantId);
        req.setAmount(bo.getAmount());
        req.setAccountInfo(bo.getAccountInfo());
        req.setStatus("PENDING");
        req.setCreateBy(LoginHelper.getUsername());
        int rows = withdrawalMapper.insert(req);
        log.info("[Withdrawal] 租户 {} 发起提现 {} 单号 {}", tenantId, bo.getAmount(), req.getWithdrawNo());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int audit(WithdrawalAuditBo bo) {
        SysWithdrawalRequest req = withdrawalMapper.selectById(bo.getId());
        if (req == null) {
            throw new ServiceException("提现申请不存在");
        }
        if (!"PENDING".equals(req.getStatus())) {
            throw new ServiceException("该申请已处理，无法重复审核");
        }
        boolean approved = Boolean.TRUE.equals(bo.getApproved());
        Long tenantId = req.getTenantId();
        if (approved) {
            // 通过：扣减冻结余额，累计提现 +
            virtualAccountService.deduct(tenantId, req.getAmount());
        } else {
            // 驳回：解冻回可用余额
            virtualAccountService.unfreeze(tenantId, req.getAmount());
        }
        SysWithdrawalRequest update = new SysWithdrawalRequest();
        update.setId(req.getId());
        update.setStatus(approved ? "APPROVED" : "REJECTED");
        update.setAuditBy(LoginHelper.getUsername());
        update.setAuditTime(new Date());
        update.setAuditRemark(bo.getAuditRemark());
        int rows = withdrawalMapper.updateById(update);
        log.info("[Withdrawal] 提现单 {} 审核 {}（租户 {} 金额 {}）", req.getWithdrawNo(), approved ? "通过" : "驳回", tenantId, req.getAmount());
        return rows;
    }

    private String genWithdrawNo() {
        return "WD" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + RandomUtil.randomNumbers(4);
    }
}
