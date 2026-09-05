package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.WecomSyncLog;
import com.ruoyi.system.domain.vo.WecomSyncLogVo;
import com.ruoyi.system.mapper.WecomSyncLogMapper;
import com.ruoyi.system.service.IWecomSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 企微通讯录同步服务实现（T13.2）
 * <p>桩：不请求企微 API，登记一次空同步（total=0，SUCCESS）并留痕；接入真实通讯录读取后，
 * 在 doSync 内按部门/成员幂等 upsert 到 sys_dept/sys_user，并回填真实计数与 PARTIAL/FAILED 状态。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WecomSyncServiceImpl implements IWecomSyncService {

    private final WecomSyncLogMapper syncLogMapper;

    @Override
    public WecomSyncLogVo syncContacts(String syncType) {
        String type = StrUtil.isBlank(syncType) ? SYNC_ALL : syncType;
        long start = System.currentTimeMillis();
        WecomSyncLog entity = new WecomSyncLog();
        entity.setSyncType(type);
        try {
            int[] counts = doSync(type);
            entity.setStatus(counts[2] > 0 ? "PARTIAL" : "SUCCESS");
            entity.setTotalCount(counts[0]);
            entity.setSuccessCount(counts[1]);
            entity.setFailCount(counts[2]);
        } catch (Exception e) {
            entity.setStatus("FAILED");
            entity.setTotalCount(0);
            entity.setSuccessCount(0);
            entity.setFailCount(0);
            entity.setErrorMsg(StrUtil.maxLength(e.getMessage(), 2000));
            log.warn("[WecomSync] 通讯录同步失败 type={}", type, e);
        }
        entity.setCostMs(System.currentTimeMillis() - start);
        syncLogMapper.insert(entity);
        return syncLogMapper.selectVoById(entity.getId());
    }

    @Override
    public TableDataInfo<WecomSyncLogVo> queryPageList(String syncType, String status, PageQuery pageQuery) {
        LambdaQueryWrapper<WecomSyncLog> wrapper = Wrappers.<WecomSyncLog>lambdaQuery()
            .eq(StrUtil.isNotBlank(syncType), WecomSyncLog::getSyncType, syncType)
            .eq(StrUtil.isNotBlank(status), WecomSyncLog::getStatus, status)
            .orderByDesc(WecomSyncLog::getCreateTime);
        Page<WecomSyncLogVo> page = syncLogMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    /**
     * 执行同步，返回 [total, success, fail]。
     * <p>桩实现：不请求企微 API，返回空同步；真实实现应在此读取通讯录并幂等 upsert 组织架构。
     */
    private int[] doSync(String type) {
        log.info("[WecomSync] 触发通讯录同步（桩，未请求企微 API）type={}", type);
        return new int[]{0, 0, 0};
    }
}
