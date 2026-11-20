package com.ruoyi.web.controller.compliance;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.vo.SensitiveWordHitLogVo;
import com.ruoyi.system.service.ISensitiveWordTenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 老板端合规命中记录只读 Controller（M18 T18.6，micro:boss:complianceHit:list）
 * <p>仅只读查看本租户命中记录；数据隔离由拦截器 + Service 层保证。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/compliance/tenant/hitLog")
public class TenantComplianceHitLogController extends BaseController {

    private final ISensitiveWordTenantService tenantService;

    /** 老板端只读命中记录（分页） */
    @SaCheckPermission("micro:boss:complianceHit:list")
    @GetMapping("/list")
    public TableDataInfo<SensitiveWordHitLogVo> list(SensitiveWordHitLogVo vo, PageQuery pageQuery) {
        return tenantService.queryHitLogPage(vo, pageQuery);
    }
}
