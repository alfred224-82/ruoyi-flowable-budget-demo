package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.FeeOperationLog;
import com.ruoyi.system.mapper.FeeOperationLogMapper;
import com.ruoyi.system.service.IFeeOperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 费用管控操作日志Service业务层处理
 *
 * @author ruoyi
 * @date 2026-07-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeOperationLogServiceImpl implements IFeeOperationLogService {

    private final FeeOperationLogMapper feeOperationLogMapper;

    @Override
    public void log(String module, String operationType, Long billId, String billNo,
                    Long relatedAppId, String beforeStatus, String afterStatus,
                    String operationDetail) {
        FeeOperationLog logEntry = new FeeOperationLog();
        logEntry.setModule(module);
        logEntry.setOperationType(operationType);
        logEntry.setBillId(billId);
        logEntry.setBillNo(billNo);
        logEntry.setRelatedAppId(relatedAppId);
        logEntry.setOperatorId(LoginHelper.getUserId());
        logEntry.setOperatorName(LoginHelper.getNickName());
        Set<String> roleKeys = LoginHelper.getLoginUser().getRolePermission();
        logEntry.setOperatorRole(roleKeys != null ? roleKeys.toString() : "");
        logEntry.setBeforeStatus(beforeStatus);
        logEntry.setAfterStatus(afterStatus);
        logEntry.setOperationDetail(operationDetail);
        logEntry.setOperationTime(new Date());
        feeOperationLogMapper.insert(logEntry);
    }

    @Override
    public Page<FeeOperationLog> selectPageList(FeeOperationLog query, PageQuery pageQuery) {
        Wrapper<FeeOperationLog> wrapper = buildQueryWrapper(query);
        return feeOperationLogMapper.selectPage(pageQuery.build(), wrapper);
    }

    @Override
    public List<FeeOperationLog> selectTimeline(String billNo) {
        return feeOperationLogMapper.selectList(
            Wrappers.<FeeOperationLog>lambdaQuery()
                .eq(FeeOperationLog::getBillNo, billNo)
                .orderByAsc(FeeOperationLog::getOperationTime));
    }

    @Override
    public List<FeeOperationLog> selectExportList(FeeOperationLog query) {
        return feeOperationLogMapper.selectList(buildQueryWrapper(query));
    }

    private Wrapper<FeeOperationLog> buildQueryWrapper(FeeOperationLog query) {
        return Wrappers.<FeeOperationLog>lambdaQuery()
            .eq(StrUtil.isNotBlank(query.getModule()), FeeOperationLog::getModule, query.getModule())
            .eq(StrUtil.isNotBlank(query.getOperationType()), FeeOperationLog::getOperationType, query.getOperationType())
            .eq(StrUtil.isNotBlank(query.getBillNo()), FeeOperationLog::getBillNo, query.getBillNo())
            .eq(query.getOperatorId() != null, FeeOperationLog::getOperatorId, query.getOperatorId())
            .orderByDesc(FeeOperationLog::getOperationTime);
    }
}
