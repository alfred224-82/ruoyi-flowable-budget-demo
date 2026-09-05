package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.FeeFaceConfigBo;
import com.ruoyi.system.domain.bo.FeeFaceVerifyBo;
import com.ruoyi.system.domain.vo.FeeFaceTriggerVo;
import com.ruoyi.system.domain.vo.FeeFaceVerifyRecordVo;
import com.ruoyi.system.service.IFeeFaceVerifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 人脸核身Controller（T7.2 / FEAT_FACE_RECOGNITION）
 * 大额(默认≥2000)/敏感大类触发 → 采集比对 → 存疑仅标记不拦截（strong 级证据）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/face")
@RequireFeature("FEAT_FACE_RECOGNITION")
public class FeeFaceVerifyController extends BaseController {

    private final IFeeFaceVerifyService faceVerifyService;

    /**
     * 获取人脸核身配置（未配置返回默认值）
     */
    @SaCheckPermission("fee:face:config")
    @GetMapping("/config")
    public R<FeeFaceConfigBo> getConfig() {
        return R.ok(faceVerifyService.getConfig());
    }

    /**
     * 保存人脸核身配置
     */
    @SaCheckPermission("fee:face:edit")
    @Log(title = "人脸核身配置", businessType = BusinessType.UPDATE)
    @PutMapping("/config")
    public R<Void> saveConfig(@Validated @RequestBody FeeFaceConfigBo bo) {
        return toAjax(faceVerifyService.saveConfig(bo));
    }

    /**
     * 触发判定：报销提交前判断是否需要人脸核身
     *
     * @param amount   报销金额
     * @param category 会计大类（可空）
     */
    @SaCheckPermission("fee:expense:submit")
    @GetMapping("/trigger")
    public R<FeeFaceTriggerVo> trigger(@RequestParam BigDecimal amount,
                                       @RequestParam(required = false) String category) {
        return R.ok(faceVerifyService.triggerCheck(amount, category));
    }

    /**
     * 提交核身结果（不一致仅标记存疑不拦截）
     */
    @SaCheckPermission("fee:expense:submit")
    @Log(title = "人脸核身", businessType = BusinessType.INSERT)
    @PostMapping("/submit")
    public R<Long> submit(@Validated @RequestBody FeeFaceVerifyBo bo) {
        return R.ok(faceVerifyService.submitVerify(bo));
    }

    /**
     * 核身记录列表（异常池=仅存疑记录）
     *
     * @param suspiciousOnly 是否仅查存疑记录
     */
    @SaCheckPermission("fee:face:config")
    @GetMapping("/records")
    public R<List<FeeFaceVerifyRecordVo>> records(@RequestParam(required = false) Boolean suspiciousOnly) {
        return R.ok(faceVerifyService.listRecords(suspiciousOnly));
    }
}
