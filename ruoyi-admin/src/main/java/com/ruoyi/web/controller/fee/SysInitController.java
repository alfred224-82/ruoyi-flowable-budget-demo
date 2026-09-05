package com.ruoyi.web.controller.fee;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.FundPoolConfigBo;
import com.ruoyi.system.domain.bo.WorkflowConfigBo;
import com.ruoyi.system.domain.vo.FundPoolConfigVo;
import com.ruoyi.system.domain.vo.WorkflowConfigVo;
import com.ruoyi.system.service.IFundPoolConfigService;
import com.ruoyi.system.service.IWorkflowConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统初始化配置
 *
 * @author ruoyi
 * @date 2026-07-24
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/init")
public class SysInitController extends BaseController {

    private final IWorkflowConfigService workflowConfigService;
    private final IFundPoolConfigService fundPoolConfigService;

    // ==================== 工作流配置 ====================

    /**
     * 获取全部工作流配置（含节点）
     */
    @GetMapping("/workflow/list")
    public R<List<WorkflowConfigVo>> workflowList() {
        return R.ok(workflowConfigService.listAll());
    }

    /**
     * 获取单条工作流配置
     */
    @GetMapping("/workflow/{workflowKey}")
    public R<WorkflowConfigVo> workflowDetail(@PathVariable String workflowKey) {
        return R.ok(workflowConfigService.getByKey(workflowKey));
    }

    /**
     * 保存工作流配置（主表+节点）
     */
    @Log(title = "工作流配置", businessType = BusinessType.UPDATE)
    @PostMapping("/workflow/save")
    public R<Void> saveWorkflow(@Validated @RequestBody WorkflowConfigBo bo) {
        return toAjax(workflowConfigService.saveConfig(bo));
    }

    /**
     * 部署工作流
     */
    @Log(title = "工作流部署", businessType = BusinessType.UPDATE)
    @PostMapping("/workflow/deploy/{id}")
    public R<Void> deployWorkflow(@PathVariable Long id) {
        return toAjax(workflowConfigService.deployWorkflow(id));
    }

    /**
     * 停用工作流
     */
    @Log(title = "工作流停用", businessType = BusinessType.UPDATE)
    @PostMapping("/workflow/disable/{id}")
    public R<Void> disableWorkflow(@PathVariable Long id) {
        return toAjax(workflowConfigService.disableWorkflow(id));
    }

    /**
     * 启用工作流
     */
    @Log(title = "工作流启用", businessType = BusinessType.UPDATE)
    @PostMapping("/workflow/enable/{id}")
    public R<Void> enableWorkflow(@PathVariable Long id) {
        return toAjax(workflowConfigService.enableWorkflow(id));
    }

    // ==================== 资金池配置 ====================

    /**
     * 获取资金池初始额度配置
     */
    @GetMapping("/fundPool/list")
    public R<List<FundPoolConfigVo>> fundPoolList() {
        return R.ok(fundPoolConfigService.listAll());
    }

    /**
     * 保存资金池配置（批量）
     */
    @Log(title = "资金池配置", businessType = BusinessType.UPDATE)
    @PostMapping("/fundPool/save")
    public R<Void> saveFundPool(@RequestBody List<FundPoolConfigBo> boList) {
        return toAjax(fundPoolConfigService.saveConfigs(boList));
    }
}
