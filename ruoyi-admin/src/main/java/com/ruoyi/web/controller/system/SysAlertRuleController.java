package com.ruoyi.web.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.SysAlertRule;
import com.ruoyi.system.mapper.SysAlertRuleMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.Set;

/**
 * 告警规则配置Controller（T18.15，小微版 M18）
 * <p>「告警规则配置」菜单（system/alertRule）：
 * <ul>
 *   <li>告警规则 CRUD</li>
 *   <li>规则启用/停用</li>
 * </ul>
 * <p>★Admin 专属，平台级操作。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/alertRule")
public class SysAlertRuleController extends BaseController {

    private final SysAlertRuleMapper alertRuleMapper;

    /** ★合法规则类型白名单 */
    private static final Set<String> VALID_RULE_TYPES = Set.of("LICENSE_EXPIRE", "COMPLIANCE_HIT", "TENANT_DISABLED", "SERVICE_DOWN");
    /** ★合法严重程度白名单 */
    private static final Set<String> VALID_SEVERITIES = Set.of("INFO", "WARNING", "CRITICAL");
    /** ★合法通知渠道白名单 */
    private static final Set<String> VALID_CHANNELS = Set.of("SYSTEM", "EMAIL", "SMS");

    /**
     * 告警规则列表（分页）
     */
    @SaCheckPermission("system:alertRule:list")
    @GetMapping("/list")
    public TableDataInfo<SysAlertRule> list(SysAlertRule query, PageQuery pageQuery) {
        var wrapper = Wrappers.<SysAlertRule>lambdaQuery()
            .eq(StrUtil.isNotBlank(query.getRuleType()), SysAlertRule::getRuleType, query.getRuleType())
            .eq(StrUtil.isNotBlank(query.getSeverity()), SysAlertRule::getSeverity, query.getSeverity())
            .eq(query.getEnabled() != null, SysAlertRule::getEnabled, query.getEnabled())
            .like(StrUtil.isNotBlank(query.getRuleName()), SysAlertRule::getRuleName, query.getRuleName())
            .orderByDesc(SysAlertRule::getCreateTime);
        var page = alertRuleMapper.selectPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    /**
     * 告警规则详情
     */
    @SaCheckPermission("system:alertRule:query")
    @GetMapping("/{id}")
    public R<SysAlertRule> detail(@PathVariable Long id) {
        if (id == null) {
            throw new ServiceException("规则ID不能为空");
        }
        SysAlertRule rule = alertRuleMapper.selectById(id);
        if (rule == null) {
            throw new ServiceException("规则不存在");
        }
        return R.ok(rule);
    }

    /**
     * 新增告警规则
     */
    @SaCheckPermission("system:alertRule:add")
    @Log(title = "告警规则", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody AlertRuleBo bo) {
        validateBo(bo);
        SysAlertRule entity = new SysAlertRule();
        copyBoToEntity(bo, entity);
        alertRuleMapper.insert(entity);
        log.info("[告警规则] 新增规则：{}", bo.getRuleName());
        return R.ok();
    }

    /**
     * 修改告警规则
     */
    @SaCheckPermission("system:alertRule:edit")
    @Log(title = "告警规则", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody AlertRuleBo bo) {
        if (bo == null || bo.getId() == null) {
            throw new ServiceException("规则ID不能为空");
        }
        SysAlertRule exist = alertRuleMapper.selectById(bo.getId());
        if (exist == null) {
            throw new ServiceException("规则不存在");
        }
        validateBo(bo);
        copyBoToEntity(bo, exist);
        alertRuleMapper.updateById(exist);
        log.info("[告警规则] 更新规则ID={}", bo.getId());
        return R.ok();
    }

    /**
     * 删除告警规则
     */
    @SaCheckPermission("system:alertRule:remove")
    @Log(title = "告警规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        if (ids == null || ids.length == 0) {
            throw new ServiceException("删除ID不能为空");
        }
        for (Long id : ids) {
            alertRuleMapper.deleteById(id);
        }
        log.info("[告警规则] 删除规则IDs={}", (Object) ids);
        return R.ok();
    }

    /**
     * 切换规则启用状态
     */
    @SaCheckPermission("system:alertRule:edit")
    @Log(title = "告警规则", businessType = BusinessType.UPDATE)
    @PutMapping("/toggle")
    public R<Void> toggle(@RequestParam Long id, @RequestParam Integer enabled) {
        if (id == null) {
            throw new ServiceException("规则ID不能为空");
        }
        if (enabled != 0 && enabled != 1) {
            throw new ServiceException("启用值不合法：0=停用/1=启用");
        }
        SysAlertRule rule = alertRuleMapper.selectById(id);
        if (rule == null) {
            throw new ServiceException("规则不存在");
        }
        rule.setEnabled(enabled);
        alertRuleMapper.updateById(rule);
        return R.ok();
    }

    // ===== 私有方法 =====

    private void validateBo(AlertRuleBo bo) {
        if (bo == null) {
            throw new ServiceException("参数不能为空");
        }
        if (StrUtil.isBlank(bo.getRuleName())) {
            throw new ServiceException("规则名称不能为空");
        }
        if (StrUtil.isBlank(bo.getRuleType()) || !VALID_RULE_TYPES.contains(bo.getRuleType())) {
            throw new ServiceException("规则类型不合法，可选：LICENSE_EXPIRE/COMPLIANCE_HIT/TENANT_DISABLED/SERVICE_DOWN");
        }
        if (bo.getThresholdValue() == null || bo.getThresholdValue() < 1) {
            throw new ServiceException("触发阈值必须>=1");
        }
        if (StrUtil.isBlank(bo.getSeverity()) || !VALID_SEVERITIES.contains(bo.getSeverity())) {
            throw new ServiceException("严重程度不合法，可选：INFO/WARNING/CRITICAL");
        }
        if (StrUtil.isBlank(bo.getNotifyChannel()) || !VALID_CHANNELS.contains(bo.getNotifyChannel())) {
            throw new ServiceException("通知渠道不合法，可选：SYSTEM/EMAIL/SMS");
        }
    }

    private void copyBoToEntity(AlertRuleBo bo, SysAlertRule entity) {
        entity.setRuleName(bo.getRuleName());
        entity.setRuleType(bo.getRuleType());
        entity.setThresholdValue(bo.getThresholdValue());
        entity.setSeverity(bo.getSeverity());
        entity.setNotifyChannel(bo.getNotifyChannel());
        entity.setEnabled(bo.getEnabled() != null ? bo.getEnabled() : 1);
        entity.setRemark(bo.getRemark());
    }

    // ===== BO =====

    @Data
    public static class AlertRuleBo implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String ruleName;
        private String ruleType;
        private Integer thresholdValue;
        private String severity;
        private String notifyChannel;
        private Integer enabled;
        private String remark;
    }
}
