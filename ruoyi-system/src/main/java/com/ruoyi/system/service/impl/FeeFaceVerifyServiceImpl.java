package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.system.domain.ExpenseBill;
import com.ruoyi.system.domain.FeeFaceVerifyRecord;
import com.ruoyi.system.domain.SysTenantConfig;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.domain.bo.FeeFaceConfigBo;
import com.ruoyi.system.domain.bo.FeeFaceVerifyBo;
import com.ruoyi.system.domain.vo.FeeFaceTriggerVo;
import com.ruoyi.system.domain.vo.FeeFaceVerifyRecordVo;
import com.ruoyi.system.mapper.ExpenseBillMapper;
import com.ruoyi.system.mapper.FeeFaceVerifyRecordMapper;
import com.ruoyi.system.mapper.SysTenantConfigMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.IFeeFaceVerifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 人脸核身Service实现（T7.2 / FEAT_FACE_RECOGNITION）
 * 配置存 sys_tenant_config（configKey=face_verify_config）；
 * 不一致仅标记"人脸存疑"进异常池，不拦截单据（设计文档 4.2.2）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeFaceVerifyServiceImpl implements IFeeFaceVerifyService {

    /** 租户配置键 */
    private static final String CONFIG_KEY = "face_verify_config";
    /** 默认大额触发阈值 */
    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("2000");
    /** 默认存疑相似度阈值 */
    private static final BigDecimal DEFAULT_SUSPICIOUS_SIMILARITY = new BigDecimal("0.6");

    private final SysTenantConfigMapper tenantConfigMapper;
    private final FeeFaceVerifyRecordMapper recordMapper;
    private final ExpenseBillMapper billMapper;
    private final SysUserMapper userMapper;

    @Override
    public FeeFaceConfigBo getConfig() {
        SysTenantConfig config = selectConfig();
        FeeFaceConfigBo bo = config == null || StrUtil.isBlank(config.getConfigValue())
            ? null : JsonUtils.parseObject(config.getConfigValue(), FeeFaceConfigBo.class);
        if (bo == null) {
            bo = new FeeFaceConfigBo();
        }
        // 默认值兜底
        if (bo.getEnabled() == null) {
            bo.setEnabled(false);
        }
        if (bo.getAmountThreshold() == null) {
            bo.setAmountThreshold(DEFAULT_THRESHOLD);
        }
        if (bo.getSuspiciousSimilarity() == null) {
            bo.setSuspiciousSimilarity(DEFAULT_SUSPICIOUS_SIMILARITY);
        }
        return bo;
    }

    @Override
    public Boolean saveConfig(FeeFaceConfigBo bo) {
        Long tenantId = LoginHelper.getTenantId();
        SysTenantConfig existing = selectConfig();
        String json = JsonUtils.toJsonString(bo);
        if (existing == null) {
            SysTenantConfig config = new SysTenantConfig();
            config.setTenantId(tenantId);
            config.setConfigKey(CONFIG_KEY);
            config.setConfigValue(json);
            config.setConfigType("fee");
            config.setRemark("人脸核身配置（T7.2）");
            return tenantConfigMapper.insert(config) > 0;
        }
        existing.setConfigValue(json);
        return tenantConfigMapper.updateById(existing) > 0;
    }

    @Override
    public FeeFaceTriggerVo triggerCheck(BigDecimal amount, String category) {
        FeeFaceConfigBo config = getConfig();
        FeeFaceTriggerVo vo = new FeeFaceTriggerVo();
        vo.setAmountThreshold(config.getAmountThreshold());
        vo.setRequired(false);
        vo.setTriggerType("NONE");
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            vo.setReason("人脸核身未启用");
            return vo;
        }
        // 大额触发（默认≥2000）
        if (amount != null && config.getAmountThreshold() != null
            && amount.compareTo(config.getAmountThreshold()) >= 0) {
            vo.setRequired(true);
            vo.setTriggerType("AMOUNT_THRESHOLD");
            vo.setReason("报销金额达到大额阈值（≥" + config.getAmountThreshold() + "），需人脸核身");
            return vo;
        }
        // 敏感大类触发
        if (StrUtil.isNotBlank(category) && config.getSensitiveCategories() != null
            && config.getSensitiveCategories().contains(category)) {
            vo.setRequired(true);
            vo.setTriggerType("SENSITIVE_CATEGORY");
            vo.setReason("费用大类【" + category + "】为敏感大类，需人脸核身");
            return vo;
        }
        vo.setReason("未触发人脸核身规则");
        return vo;
    }

    @Override
    public Long submitVerify(FeeFaceVerifyBo bo) {
        FeeFaceConfigBo config = getConfig();
        FeeFaceVerifyRecord record = new FeeFaceVerifyRecord();
        record.setBillId(bo.getBillId());
        record.setUserId(bo.getUserId() == null ? LoginHelper.getUserId() : bo.getUserId());
        record.setAmount(bo.getAmount());
        record.setTriggerType(bo.getTriggerType());
        record.setFaceImageUrl(bo.getFaceImageUrl());
        record.setReferenceImageUrl(bo.getReferenceImageUrl());
        record.setResult(bo.getResult());
        record.setSimilarity(bo.getSimilarity());
        // 存疑判定：比对不一致 或 相似度低于阈值（仅标记不拦截）
        boolean suspicious = "MISMATCH".equals(bo.getResult())
            || (bo.getSimilarity() != null && config.getSuspiciousSimilarity() != null
                && bo.getSimilarity().compareTo(config.getSuspiciousSimilarity()) < 0);
        record.setIsSuspicious(suspicious ? 1 : 0);
        record.setEvidenceLevel("strong");
        record.setVerifyTime(new Date());
        recordMapper.insert(record);
        if (suspicious) {
            log.info("人脸核身存疑（进异常池）：recordId={}, userId={}, result={}, similarity={}",
                record.getId(), record.getUserId(), bo.getResult(), bo.getSimilarity());
        }
        return record.getId();
    }

    @Override
    public List<FeeFaceVerifyRecordVo> listRecords(Boolean suspiciousOnly) {
        List<FeeFaceVerifyRecordVo> list = recordMapper.selectVoList(Wrappers.<FeeFaceVerifyRecord>lambdaQuery()
            .eq(Boolean.TRUE.equals(suspiciousOnly), FeeFaceVerifyRecord::getIsSuspicious, 1)
            .orderByDesc(FeeFaceVerifyRecord::getCreateTime));
        fillRecordExtra(list);
        return list;
    }

    /**
     * 填充用户姓名与报销单号
     */
    private void fillRecordExtra(List<FeeFaceVerifyRecordVo> list) {
        if (list.isEmpty()) {
            return;
        }
        List<Long> userIds = list.stream().map(FeeFaceVerifyRecordVo::getUserId)
            .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (!userIds.isEmpty()) {
            Map<Long, String> nameMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getUserId, SysUser::getNickName, (a, b) -> a));
            list.forEach(vo -> vo.setUserName(nameMap.get(vo.getUserId())));
        }
        List<Long> billIds = list.stream().map(FeeFaceVerifyRecordVo::getBillId)
            .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (!billIds.isEmpty()) {
            Map<Long, String> billNoMap = billMapper.selectBatchIds(billIds).stream()
                .collect(Collectors.toMap(ExpenseBill::getId, ExpenseBill::getBillNo, (a, b) -> a));
            list.forEach(vo -> vo.setBillNo(billNoMap.get(vo.getBillId())));
        }
    }

    private SysTenantConfig selectConfig() {
        return tenantConfigMapper.selectOne(Wrappers.<SysTenantConfig>lambdaQuery()
            .eq(SysTenantConfig::getTenantId, LoginHelper.getTenantId())
            .eq(SysTenantConfig::getConfigKey, CONFIG_KEY)
            .last("LIMIT 1"));
    }
}
