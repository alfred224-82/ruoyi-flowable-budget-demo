package com.ruoyi.system.service;

import com.ruoyi.system.domain.bo.FeeFaceConfigBo;
import com.ruoyi.system.domain.bo.FeeFaceVerifyBo;
import com.ruoyi.system.domain.vo.FeeFaceTriggerVo;
import com.ruoyi.system.domain.vo.FeeFaceVerifyRecordVo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 人脸核身Service接口（T7.2 / FEAT_FACE_RECOGNITION）
 * 大额(默认≥2000)/敏感大类触发 → 采集比对 → 存疑仅标记不拦截（strong 级证据）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IFeeFaceVerifyService {

    /**
     * 获取人脸核身配置（未配置返回默认值）
     */
    FeeFaceConfigBo getConfig();

    /**
     * 保存人脸核身配置
     */
    Boolean saveConfig(FeeFaceConfigBo bo);

    /**
     * 触发判定：报销提交前判断是否需要人脸核身
     *
     * @param amount   报销金额
     * @param category 会计大类（可空）
     */
    FeeFaceTriggerVo triggerCheck(BigDecimal amount, String category);

    /**
     * 提交核身结果（不一致仅标记存疑不拦截）
     */
    Long submitVerify(FeeFaceVerifyBo bo);

    /**
     * 核身记录列表（异常池=仅存疑）
     *
     * @param suspiciousOnly 是否仅查存疑记录（异常池）
     */
    List<FeeFaceVerifyRecordVo> listRecords(Boolean suspiciousOnly);
}
