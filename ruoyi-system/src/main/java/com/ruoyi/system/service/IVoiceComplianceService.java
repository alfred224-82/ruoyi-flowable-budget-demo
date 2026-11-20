package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.VoiceComplianceHitBo;
import com.ruoyi.system.domain.bo.VoiceComplianceRuleBo;
import com.ruoyi.system.domain.bo.VoiceRecordingPolicyBo;
import com.ruoyi.system.domain.compliance.SensitiveWordCheckResult;
import com.ruoyi.system.domain.vo.VoiceComplianceHitVo;
import com.ruoyi.system.domain.vo.VoiceComplianceRuleVo;
import com.ruoyi.system.domain.vo.VoiceRecordingPolicyVo;

import java.util.List;
import java.util.Map;

/**
 * 语音合规服务接口（T18.1~T18.4，小微版 M18）
 * <p>覆盖合规规则 CRUD + 录音策略 + 命中记录查询 + 合规告警聚合。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
public interface IVoiceComplianceService {

    // ===== T18.3 租户管理员合规规则管理 =====

    /**
     * 合规规则列表（租户管理员 PC，分页）
     */
    TableDataInfo<VoiceComplianceRuleVo> queryRulePage(VoiceComplianceRuleBo bo, PageQuery pageQuery);

    /**
     * 合规规则详情
     */
    VoiceComplianceRuleVo queryRuleById(Long id);

    /**
     * 新增合规规则
     */
    Long insertRule(VoiceComplianceRuleBo bo);

    /**
     * 修改合规规则
     */
    int updateRule(VoiceComplianceRuleBo bo);

    /**
     * 删除合规规则
     */
    int deleteRule(Long id);

    // ===== T18.3 租户管理员录音策略管理 =====

    /**
     * 查询当前租户录音策略（每租户一条）
     */
    VoiceRecordingPolicyVo queryPolicy();

    /**
     * 保存/更新录音策略（upsert）
     */
    Long savePolicy(VoiceRecordingPolicyBo bo);

    // ===== T18.4 老板合规命中记录（只读） =====

    /**
     * 合规命中记录列表（老板/租户管理员只读查看，分页）
     */
    TableDataInfo<VoiceComplianceHitVo> queryHitPage(VoiceComplianceHitBo bo, PageQuery pageQuery);

    // ===== T18.2 Admin 平台合规管理 =====

    /**
     * ★Admin 全局录音留存上限天数
     */
    int getGlobalMaxRetentionDays();

    /**
     * ★Admin 设置全局录音留存上限天数
     */
    void setGlobalMaxRetentionDays(int days);

    /**
     * ★Admin 合规告警看板（近 7 天命中趋势）
     */
    List<Map<String, Object>> complianceAlertDashboard();

    // ===== T18.3 录音策略校验 =====

    /**
     * 校验租户录音留存天数不超过全局上限
     */
    void validateRetentionDays(int days);

    // ===== M18 T18.6 语音 ASR 转写文本合规收口 =====

    /**
     * 语音 ASR 转写文本合规检测（M18 T18.6  收口点）。
     * <p>ASR 转写完成后由语音处理链调用，委派统一敏感词引擎（inputChannel=voice_ocr）；
     *   命中（FAIL）时调用方应打回重录（与 V3.7 §6 一致）。
     *
     * @param asrText  ASR 转写文本（非空）
     * @param tenantId 租户ID（非空）
     * @param recordId 录音记录ID（业务主键，可空）
     * @return 检测结果（PASS/FAIL）
     */
    SensitiveWordCheckResult checkTranscription(String asrText, Long tenantId, String recordId);

    // ===== M20 T18 增强：语音上传后合规校验流程 =====

    /**
     * 语音上传后完整合规校验流程（M20 T18 增强）。
     * <p>流程：① ASR 转写 → ② 敏感词检测 → ③ 写 voice_compliance_hit 记录（含 result 列 PASS/FAIL）。
     * <p>无论合规通过与否均写入记录，便于审计追踪。
     *
     * @param voiceUrl   语音文件 OSS URL
     * @param tenantId   租户ID（非空）
     * @param activityId 活动ID（可空；insert 场景尚未落库时传 null）
     * @return 合规结果（PASS/FAIL + 命中详情）
     */
    SensitiveWordCheckResult checkVoiceCompliance(String voiceUrl, Long tenantId, Long activityId);
}
