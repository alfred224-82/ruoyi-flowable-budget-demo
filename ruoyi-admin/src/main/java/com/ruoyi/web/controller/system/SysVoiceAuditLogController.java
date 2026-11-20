package com.ruoyi.web.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.VoiceComplianceHit;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.mapper.VoiceComplianceHitMapper;
import com.ruoyi.system.mapper.SysTenantMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 语音处理审计日志Controller（T18.15，小微版 M18）
 * <p>「语音处理日志」菜单（system/voiceAuditLog）：
 * <ul>
 *   <li>跨租户语音合规命中日志查询（分页 + 筛选）</li>
 *   <li>按命中类型/状态/租户/时间范围筛选</li>
 * </ul>
 * <p>★Admin 专属，跨租户审计。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/voiceAuditLog")
public class SysVoiceAuditLogController extends BaseController {

    private final VoiceComplianceHitMapper hitMapper;
    private final SysTenantMapper tenantMapper;

    /**
     * 语音处理审计日志列表（跨租户，分页）
     */
    @SaCheckPermission("system:voiceAuditLog:list")
    @GetMapping("/list")
    public TableDataInfo<VoiceAuditLogVo> list(
            @RequestParam(required = false) String hitType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            PageQuery pageQuery) {

        // ★构建查询条件
        var wrapper = Wrappers.<VoiceComplianceHit>lambdaQuery()
            .eq(StrUtil.isNotBlank(hitType), VoiceComplianceHit::getHitType, hitType)
            .eq(StrUtil.isNotBlank(status), VoiceComplianceHit::getStatus, status)
            .eq(tenantId != null, VoiceComplianceHit::getTenantId, tenantId)
            .ge(startDate != null, VoiceComplianceHit::getHitTime, startDate)
            .le(endDate != null, VoiceComplianceHit::getHitTime, endDate)
            .orderByDesc(VoiceComplianceHit::getHitTime);

        var page = hitMapper.selectPage(pageQuery.build(), wrapper);

        // ★回填租户名称
        Set<Long> tenantIds = page.getRecords().stream()
            .map(VoiceComplianceHit::getTenantId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, String> tenantNameMap = new HashMap<>();
        if (!tenantIds.isEmpty()) {
            List<SysTenant> tenants = tenantMapper.selectList(
                Wrappers.<SysTenant>lambdaQuery().in(SysTenant::getId, tenantIds));
            if (tenants != null) {
                for (SysTenant t : tenants) {
                    tenantNameMap.put(t.getId(), t.getCompanyName());
                }
            }
        }

        // ★转换为 VO
        List<VoiceAuditLogVo> voList = new ArrayList<>();
        for (VoiceComplianceHit hit : page.getRecords()) {
            VoiceAuditLogVo vo = new VoiceAuditLogVo();
            vo.setId(hit.getId());
            vo.setTenantId(hit.getTenantId());
            vo.setTenantName(tenantNameMap.getOrDefault(hit.getTenantId(), "未知租户"));
            vo.setHitTime(hit.getHitTime());
            vo.setHitType(hit.getHitType());
            vo.setContentSummary(hit.getContentSummary());
            vo.setRuleId(hit.getRuleId());
            vo.setActivityId(hit.getActivityId());
            vo.setStatus(hit.getStatus());
            vo.setRemark(hit.getRemark());
            voList.add(vo);
        }

        TableDataInfo<VoiceAuditLogVo> rspData = new TableDataInfo<>();
        rspData.setCode(200);
        rspData.setMsg("查询成功");
        rspData.setRows(voList);
        rspData.setTotal(page.getTotal());
        return rspData;
    }

    /**
     * 审计统计摘要（按命中类型/状态分组）
     */
    @SaCheckPermission("system:voiceAuditLog:list")
    @GetMapping("/summary")
    public com.ruoyi.common.core.domain.R<Map<String, Object>> summary() {
        List<VoiceComplianceHit> allHits = hitMapper.selectList(Wrappers.emptyWrapper());
        if (allHits == null) {
            allHits = Collections.emptyList();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalHits", allHits.size());

        // 按类型分组
        Map<String, Long> byType = allHits.stream()
            .collect(Collectors.groupingBy(h -> h.getHitType() != null ? h.getHitType() : "UNKNOWN", Collectors.counting()));
        result.put("byType", byType);

        // 按状态分组
        Map<String, Long> byStatus = allHits.stream()
            .collect(Collectors.groupingBy(h -> h.getStatus() != null ? h.getStatus() : "UNKNOWN", Collectors.counting()));
        result.put("byStatus", byStatus);

        return com.ruoyi.common.core.domain.R.ok(result);
    }

    // ===== VO =====

    @Data
    public static class VoiceAuditLogVo implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private Long tenantId;
        private String tenantName;
        private Date hitTime;
        private String hitType;
        private String contentSummary;
        private Long ruleId;
        private Long activityId;
        private String status;
        private String remark;
    }
}
