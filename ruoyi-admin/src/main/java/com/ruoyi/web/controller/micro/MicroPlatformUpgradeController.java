package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.constant.EditionFeatureConfig;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.vo.SysTenantVo;
import com.ruoyi.system.service.ILicenseService;
import com.ruoyi.system.service.ISysTenantService;
import com.ruoyi.system.utils.LicenseGenerateTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * platform_admin 版本升级Controller（T15.33，租户管理员三件套之一）
 * <p>platform_admin 查看当前租户版本信息 + 导入 License 密钥升级 micro_lite → micro_pro。
 * <p>perms：micro:platform:upgrade（M15_17 种子菜单）。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/platform/upgrade")
public class MicroPlatformUpgradeController extends BaseController {

    private final ISysTenantService tenantService;
    private final ILicenseService licenseService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 当前租户版本信息（版本号/档位/到期日/功能开关）
     */
    @SaCheckPermission("micro:platform:upgrade")
    @GetMapping("/info")
    public R<Map<String, Object>> info() {
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            return R.fail("无法获取当前租户信息");
        }
        SysTenantVo tenant = tenantService.queryById(tenantId);
        if (tenant == null) {
            return R.fail("租户不存在");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantId", tenant.getId());
        result.put("companyName", tenant.getCompanyName());
        result.put("versionType", tenant.getVersionType());
        result.put("versionLabel", resolveVersionLabel(tenant.getVersionType()));
        result.put("licenseType", tenant.getLicenseType());
        result.put("expireDate", tenant.getExpireDate());
        result.put("status", tenant.getStatus());
        return R.ok(result);
    }

    /**
     * ★导入 License 升级（T15.33+ 改造：接受完整 License JSON；v1.29 兼容 LIC1- 密文）
     * <p>租户管理员将 Admin 侧「导出 License」生成的密文（或存量明文 JSON）粘贴到此处，
     * 后端委托 {@link ILicenseService#importLicenseForTenant(Long, String)} 完成：
     * <ol>
     *   <li>hard 模式签名校验 / soft 模式跳过；</li>
     *   <li>写入 sys_license（租户级）+ 同步 sys_tenant.edition/expire_date/license_type；</li>
     *   <li>清除租户 License 缓存 + 写版本变更日志。</li>
     * </ol>
     * <p>若 JSON 中携带 {@code versionType}（如 micro_pro），额外同步 sys_tenant.version_type
     * 以驱动前端菜单/按钮的档位过滤。
     * <p>★Bug修复：导入前 fail-fast 校验（租户归属/产品线），并按实际结果返回变更结论
     * （UPGRADE=升档 / RENEW=同档续费 / DOWNGRADE=降档）与目标档位、到期日，
     * 供前端按实际结果提示，避免硬编码「升级至专业版」文案与实际行为不符。
     *
     * @param licenseJson License 密文（LIC1- 单行串，含复制丢前缀容错）或明文 JSON 字符串（由 Admin 侧 /system/license/generate 签发）
     * @return 变更结果：changeType / fromVersionType / toVersionType / versionLabel / expiresAt
     */
    @SaCheckPermission("micro:platform:upgrade")
    @Log(title = "版本升级", businessType = BusinessType.UPDATE)
    @PostMapping("/license")
    public R<Map<String, Object>> importLicense(@RequestBody String licenseJson) {
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            return R.fail("无法获取当前租户信息");
        }
        if (StrUtil.isBlank(licenseJson)) {
            return R.fail("License 内容不能为空");
        }
        SysTenantVo tenant = tenantService.queryById(tenantId);
        if (tenant == null) {
            return R.fail("租户不存在");
        }
        String fromVersionType = tenant.getVersionType();
        // ① 密文解包 + 预解析 JSON：LIC1- 密文（含复制丢前缀）先解密为明文 JSON（复用 LicenseGenerateTool，与 service 层口径一致），
        //    格式错误 fail-fast，不让脏数据流入导入逻辑
        String licensePlain;
        try {
            licensePlain = LicenseGenerateTool.toPlainJson(licenseJson);
        } catch (Exception e) {
            return R.fail("License 密文无效或已被篡改，请向平台管理员重新获取");
        }
        JsonNode node;
        try {
            node = objectMapper.readTree(licensePlain);
        } catch (Exception e) {
            return R.fail("License 格式错误，请粘贴 Admin 侧导出的完整 License 密文（LIC1- 开头单行串）或 JSON 内容");
        }
        String versionType = getJsonText(node, "versionType");
        String expiresAt = getJsonText(node, "expiresAt");
        // ①-a 租户归属校验：License 声明 tenantId 时必须与当前租户一致，防跨租户导入
        String licenseTenantId = getJsonText(node, "tenantId");
        if (StrUtil.isNotBlank(licenseTenantId) && !licenseTenantId.equals(String.valueOf(tenantId))) {
            log.warn("[Upgrade] License导入被拒绝 | exceptionType=BUSINESS | errorCode=LICENSE_TENANT_MISMATCH | reason=License租户{}与当前租户{}不一致 | operation=importLicense",
                licenseTenantId, tenantId);
            return R.fail("该 License 归属租户[" + licenseTenantId + "]与当前租户不一致，禁止导入其他租户的 License");
        }
        // ①-b 产品线校验：本页仅接受小微版档位（micro_lite/micro_pro）；
        //     企业线或未声明小微档位的 License 直接拒绝，避免导入后 edition 已改而档位同步失败的半更新状态
        if (StrUtil.isNotBlank(versionType)) {
            if (!"micro_lite".equals(versionType) && !"micro_pro".equals(versionType)) {
                log.warn("[Upgrade] License导入被拒绝 | exceptionType=BUSINESS | errorCode=LICENSE_LINE_MISMATCH | reason=档位{}不属于micro产品线 | operation=importLicense | tenantId={}",
                    versionType, tenantId);
                return R.fail("该 License 档位[" + versionType + "]不属于小微版产品线(micro_lite/micro_pro)，禁止跨产品线导入");
            }
        } else if (!EditionFeatureConfig.EDITION_MICRO.equals(
            EditionFeatureConfig.normalizeEdition(getJsonText(node, "edition")))) {
            log.warn("[Upgrade] License导入被拒绝 | exceptionType=BUSINESS | errorCode=LICENSE_LINE_MISMATCH | reason=License未声明小微档位且edition非MICRO | operation=importLicense | tenantId={}",
                tenantId);
            return R.fail("该 License 未声明小微版档位(versionType)且版本(edition)非 MICRO，无法在本页面导入");
        }
        // ② 委托 LicenseService 完成导入（签名校验 + 落库 + 缓存刷新 + 版本日志；传解包后明文，service 内解包幂等）
        licenseService.importLicenseForTenant(tenantId, licensePlain);
        // ③ 若 JSON 携带 versionType，同步租户档位（驱动前端菜单过滤；同档导入在 assignVersionType 内幂等返回）
        if (StrUtil.isNotBlank(versionType)) {
            tenantService.assignVersionType(tenantId, versionType);
        }
        // ④ 返回变更结论：前端据此提示（升级/续费），替代原硬编码文案
        String toVersionType = StrUtil.isNotBlank(versionType) ? versionType : fromVersionType;
        String changeType;
        if (toVersionType.equals(fromVersionType)) {
            changeType = "RENEW";
        } else if (microRank(toVersionType) > microRank(fromVersionType)) {
            changeType = "UPGRADE";
        } else {
            changeType = "DOWNGRADE";
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("changeType", changeType);
        result.put("fromVersionType", fromVersionType);
        result.put("toVersionType", toVersionType);
        result.put("versionLabel", resolveVersionLabel(toVersionType));
        result.put("expiresAt", expiresAt);
        log.info("[Upgrade] License导入完成 | tenantId={} | changeType={} | 档位={} -> {} | expiresAt={}",
            tenantId, changeType, fromVersionType, toVersionType, expiresAt);
        return R.ok(result);
    }

    /** 读取 JSON 文本字段（缺失或 null 时返回 null，防 NPE） */
    private String getJsonText(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    /** 小微档位等级（用于升降级判定）：micro_lite(1) < micro_pro(2)，未知档位按 0 */
    private int microRank(String versionType) {
        if ("micro_pro".equals(versionType)) {
            return 2;
        }
        if ("micro_lite".equals(versionType)) {
            return 1;
        }
        return 0;
    }

    /** 版本档位显示名 */
    private String resolveVersionLabel(String versionType) {
        if ("micro_lite".equals(versionType)) {
            return "小微版·免费版";
        } else if ("micro_pro".equals(versionType)) {
            return "小微版·专业版";
        }
        return versionType != null ? versionType : "未分配";
    }
}
