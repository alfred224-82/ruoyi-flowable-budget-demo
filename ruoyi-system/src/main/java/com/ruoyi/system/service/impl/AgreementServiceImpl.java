package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.SysAgreementVersion;
import com.ruoyi.system.domain.SysUserAgreementLog;
import com.ruoyi.system.domain.bo.AgreementVersionBo;
import com.ruoyi.system.domain.vo.SysAgreementVersionVo;
import com.ruoyi.system.domain.vo.SysUserAgreementLogVo;
import com.ruoyi.system.mapper.SysAgreementVersionMapper;
import com.ruoyi.system.mapper.SysUserAgreementLogMapper;
import com.ruoyi.system.service.IAgreementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 隐私政策/用户协议服务实现（T14.4）
 * <p>sys_agreement_version 系统级共享（无 tenant_id）；sys_user_agreement_log 含 tenant_id（用户同意留痕）。
 * <p>两实体均不继承 BaseEntity，createBy/agreeTime 等由服务层显式写入，时间列 DB DEFAULT 兜底。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgreementServiceImpl implements IAgreementService {

    private final SysAgreementVersionMapper agreementMapper;
    private final SysUserAgreementLogMapper agreeLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int publish(AgreementVersionBo bo) {
        Long dup = agreementMapper.selectCount(Wrappers.<SysAgreementVersion>lambdaQuery()
            .eq(SysAgreementVersion::getAgreementType, bo.getAgreementType())
            .eq(SysAgreementVersion::getVersion, bo.getVersion()));
        if (dup != null && dup > 0) {
            throw new ServiceException("该协议版本已存在：" + bo.getAgreementType() + " " + bo.getVersion());
        }
        // 同类型旧版全部转历史
        agreementMapper.update(null, Wrappers.<SysAgreementVersion>lambdaUpdate()
            .eq(SysAgreementVersion::getAgreementType, bo.getAgreementType())
            .eq(SysAgreementVersion::getStatus, "0")
            .set(SysAgreementVersion::getStatus, "1"));

        SysAgreementVersion entity = new SysAgreementVersion();
        entity.setAgreementType(bo.getAgreementType());
        entity.setVersion(bo.getVersion());
        entity.setTitle(bo.getTitle());
        entity.setContentUrl(bo.getContentUrl());
        entity.setContentHash(SecureUtil.sha256(StrUtil.nullToEmpty(bo.getContentUrl()) + "|" + bo.getVersion()));
        entity.setPublishTime(new Date());
        entity.setStatus("0");
        entity.setCreateBy(LoginHelper.getUsername());
        entity.setRemark(bo.getRemark());
        int rows = agreementMapper.insert(entity);
        log.info("[Agreement] 发布协议 {} {} 生效", bo.getAgreementType(), bo.getVersion());
        return rows;
    }

    @Override
    public List<SysAgreementVersionVo> listActive() {
        return agreementMapper.selectVoList(Wrappers.<SysAgreementVersion>lambdaQuery()
            .eq(SysAgreementVersion::getStatus, "0")
            .orderByAsc(SysAgreementVersion::getAgreementType));
    }

    @Override
    public List<SysAgreementVersionVo> listVersions(String agreementType) {
        return agreementMapper.selectVoList(Wrappers.<SysAgreementVersion>lambdaQuery()
            .eq(StrUtil.isNotBlank(agreementType), SysAgreementVersion::getAgreementType, agreementType)
            .orderByDesc(SysAgreementVersion::getPublishTime));
    }

    @Override
    public SysAgreementVersionVo queryById(Long id) {
        SysAgreementVersionVo vo = agreementMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("协议版本不存在");
        }
        return vo;
    }

    @Override
    public int recordAgree(String agreementType, String version, String openid, String clientIp) {
        SysUserAgreementLog entity = new SysUserAgreementLog();
        entity.setTenantId(LoginHelper.getTenantId());
        entity.setUserId(LoginHelper.getUserId());
        entity.setOpenid(openid);
        entity.setAgreementType(agreementType);
        entity.setVersion(version);
        entity.setClientIp(clientIp);
        entity.setAgreeTime(new Date());
        return agreeLogMapper.insert(entity);
    }

    @Override
    public TableDataInfo<SysUserAgreementLogVo> queryAgreeLogPageList(String agreementType, Long userId, PageQuery pageQuery) {
        LambdaQueryWrapper<SysUserAgreementLog> wrapper = Wrappers.<SysUserAgreementLog>lambdaQuery()
            .eq(StrUtil.isNotBlank(agreementType), SysUserAgreementLog::getAgreementType, agreementType)
            .eq(userId != null, SysUserAgreementLog::getUserId, userId)
            .orderByDesc(SysUserAgreementLog::getAgreeTime);
        Page<SysUserAgreementLogVo> page = agreeLogMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }
}
