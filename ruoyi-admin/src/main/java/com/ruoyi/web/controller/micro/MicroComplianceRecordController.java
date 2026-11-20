package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.VoiceComplianceHitBo;
import com.ruoyi.system.domain.vo.VoiceComplianceHitVo;
import com.ruoyi.system.service.IVoiceComplianceService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 老板语音合规记录Controller（T18.4，小微版 M18）
 * <p>「语音合规记录」菜单（micro:compliance:records）：
 * <ul>
 *   <li>只读查看合规命中记录，支持按时间/类型筛选</li>
 *   <li>★仅只读，无编辑操作</li>
 *   <li>★仅 micro_pro 可用</li>
 * </ul>
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/compliance/records")
public class MicroComplianceRecordController extends BaseController {

    private final IVoiceComplianceService complianceService;

    /**
     * ★合规命中记录列表（老板只读查看，分页 + 按时间/类型筛选）
     */
    @SaCheckPermission("micro:compliance:records")
    @GetMapping("/list")
    public TableDataInfo<VoiceComplianceHitVo> list(VoiceComplianceHitBo bo, PageQuery pageQuery) {
        return complianceService.queryHitPage(bo, pageQuery);
    }
}
