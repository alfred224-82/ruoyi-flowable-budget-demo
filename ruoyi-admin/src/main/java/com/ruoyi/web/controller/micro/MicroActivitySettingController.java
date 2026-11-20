package com.ruoyi.web.controller.micro;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.IndustryTemplate;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.domain.VoiceComplianceRule;
import com.ruoyi.system.domain.bo.ActivitySettingBo;
import com.ruoyi.system.domain.vo.ActivitySettingVo;
import com.ruoyi.system.mapper.IndustryTemplateMapper;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.mapper.VoiceComplianceRuleMapper;
import com.ruoyi.system.service.ILlmImageService;
import com.ruoyi.system.service.IMicroActivitySettingService;
import com.ruoyi.system.service.wx.IWxMiniappService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 微信推广码 Controller（M19 T19.5 推广码生成 + 活动设置查询）
 * <p>perms 对齐 M19_09 菜单：
 * <ul>
 *   <li>micro:wechat:qrcode:list —— 查看推广码（C 类型，menu_id=7156）</li>
 *   <li>micro:wechat:qrcode:edit —— 生成推广码（F 类型，menu_id=7158）</li>
 * </ul>
 * <p>★背景图仅对当前活动生效，不跨活动复用（对齐需求草稿 §3.6）。
 * <p>★语音为单向播放（约 3 分钟），不做语音互动。
 *
 * @author ruoyi
 * @date 2026-09-21
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/activity/setting")
public class MicroActivitySettingController extends BaseController {

    private final IMicroActivitySettingService activitySettingService;
    private final VoiceComplianceRuleMapper complianceRuleMapper;
    private final IndustryTemplateMapper industryTemplateMapper;
    private final SysTenantMapper tenantMapper;
    private final IWxMiniappService wxMiniappService;
    private final ILlmImageService llmImageService;

    /**
     * T19.2 项目选择器数据源（单选下拉，排除 space 分类）
     */
    @GetMapping("/projects")
    public R<List<Map<String, Object>>> listProjects() {
        return R.ok(activitySettingService.listProjects());
    }

    /**
     * T19.2 房间选择器数据源（单/多选下拉，仅 space 分类）
     */
    @GetMapping("/rooms")
    public R<List<Map<String, Object>>> listRooms() {
        return R.ok(activitySettingService.listRooms());
    }

    /**
     * T19.2 员工选择器数据源（单/多选下拉，salesman 角色用户）
     */
    @GetMapping("/staff")
    public R<List<Map<String, Object>>> listStaff() {
        return R.ok(activitySettingService.listStaff());
    }

    /**
     * T19.1 查询活动设置（回显已保存的选择 + 解析名称）
     */
    @GetMapping("/{activityId}")
    public R<ActivitySettingVo> getSetting(@PathVariable Long activityId) {
        return R.ok(activitySettingService.getSetting(activityId));
    }

    /**
     * T19.1 保存活动设置（项目/房间/员工选择 + 自动标题 + 背景图/语音）
     * <p>★T19.3 自动生成标题（manualTitle 为空时自动拼接；非空时覆盖）
     * <p>★T19.4 背景图仅对当前活动生效
     */
    @Log(title = "微信推广码", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<ActivitySettingVo> saveSetting(@Validated(EditGroup.class) @RequestBody ActivitySettingBo bo) {
        return R.ok(activitySettingService.saveSetting(bo));
    }

    /**
     * T19.3 预览自动生成的标题（不保存，仅预览）
     */
    @GetMapping("/previewTitle")
    public R<String> previewTitle(
        @RequestParam(required = false) Long projectId,
        @RequestParam(required = false) List<Long> roomIdList,
        @RequestParam(required = false) List<Long> staffIdList) {
        // ★必须用双参数版本，否则 R.ok(String) 匹配到 R.ok(String msg) 重载，标题会进入 msg 而非 data
        String title = activitySettingService.generateAutoTitle(projectId, roomIdList, staffIdList);
        return R.ok("操作成功", title);
    }

    /**
     * 当前租户语音配置（三级控制：admin 行业总开关 → platform_admin 租户规则 → 无规则继承行业默认）
     * <p>★发布活动页据此决定是否显示语音上传控件 + 最大语音时长限制：
     * <ol>
     *   <li>admin 行业总开关 industry_template.voice_enabled=0 → 一律关闭</li>
     *   <li>存在租户级 voice_compliance_rule → 按其 enabled + maxVoiceDuration</li>
     *   <li>无租户规则 → 继承行业默认（总开关开启即可用，默认 180 秒）</li>
     * </ol>
     * <p>返回 { enabled: boolean, maxVoiceDuration: int(秒) }，maxVoiceDuration=0 表示不限时长。
     */
    @GetMapping("/voiceEnabled")
    public R<Map<String, Object>> voiceEnabled() {
        Long tenantId = LoginHelper.getTenantId();
        SysTenant tenant = tenantMapper.selectById(tenantId);
        Map<String, Object> result = new LinkedHashMap<>(2);
        if (tenant == null || tenant.getIndustry() == null) {
            result.put("enabled", false);
            result.put("maxVoiceDuration", 0);
            return R.ok(result);
        }
        // 1. admin 行业总开关：行业模板未启用语音则一律关闭
        IndustryTemplate template = industryTemplateMapper.selectOne(
            Wrappers.<IndustryTemplate>lambdaQuery()
                .eq(IndustryTemplate::getIndustryCode, tenant.getIndustry())
                .last("LIMIT 1"));
        if (template == null || !Integer.valueOf(1).equals(template.getVoiceEnabled())) {
            result.put("enabled", false);
            result.put("maxVoiceDuration", 0);
            return R.ok(result);
        }
        // 2. platform_admin 配置了租户级规则则按规则开关 + 时长
        VoiceComplianceRule rule = complianceRuleMapper.selectOne(
            Wrappers.<VoiceComplianceRule>lambdaQuery()
                .eq(VoiceComplianceRule::getActivityType, tenant.getIndustry())
                .eq(VoiceComplianceRule::getStatus, "NORMAL")
                .last("LIMIT 1"));
        if (rule != null && rule.getEnabled() != null) {
            result.put("enabled", rule.getEnabled());
            result.put("maxVoiceDuration", rule.getMaxVoiceDuration() != null ? rule.getMaxVoiceDuration() : 180);
            return R.ok(result);
        }
        // 3. 无租户规则 → 继承行业默认（总开关已开启，默认 180 秒）
        result.put("enabled", true);
        result.put("maxVoiceDuration", 180);
        return R.ok(result);
    }

    /**
     * LLM 描述生成图片（★付费版 micro_pro 专属）
     * <p>接收文字描述，调用 DashScope 通义万相 API 生成图片，上传 OSS 后返回 URL。
     */
    @PostMapping("/llmImage")
    public R<String> llmGenerateImage(@RequestBody Map<String, String> body) {
        String description = body.get("description");
        if (StrUtil.isBlank(description)) {
            return R.fail("描述不能为空");
        }
        String imageUrl = llmImageService.generateImage(description);
        return R.ok("操作成功", imageUrl);
    }

    /**
     * 通用微信小程序码（不绑定活动，扫码直接进入小程序）
     * <p>scene = t=tenantId，小程序端据此识别租户并展示对应门店首页。
     * <p>★返回 base64 data URL，前端可直接作为 img src 渲染。
     */
    @GetMapping("/generalQrcode")
    public R<Map<String, String>> generalQrcode() {
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            return R.fail("租户ID不能为空");
        }
        // scene = t=tenantId（≤32 字符）
        String scene = "t=" + tenantId;
        String qrcodeUrl = wxMiniappService.generateWxacode(scene, null);
        Map<String, String> result = new java.util.LinkedHashMap<>(2);
        result.put("scene", scene);
        result.put("qrcodeUrl", qrcodeUrl);
        return R.ok(result);
    }
}
