package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.vo.ExceptionCenterVo;
import com.ruoyi.system.service.IExceptionCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 业务异常处理中心Controller（T18.8，小微版 M18）
 * <p>聚合：预约冲突处理 / 预约失败通知（一键重新预约）/ 固定支出异常提醒 / 合规命中处理。
 * <p>★老板（boss）专属。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/exceptionCenter")
public class MicroExceptionCenterController extends BaseController {

    private final IExceptionCenterService exceptionCenterService;

    /**
     * 获取异常列表（聚合）
     */
    @SaCheckPermission("micro:exception:list")
    @GetMapping
    public R<ExceptionCenterVo> list() {
        return R.ok(exceptionCenterService.getExceptions());
    }

    /**
     * 标记异常为已处理
     *
     * @param exceptionType 异常类型
     * @param sourceId      来源记录ID
     */
    @SaCheckPermission("micro:exception:resolve")
    @Log(title = "异常处理", businessType = BusinessType.UPDATE)
    @PutMapping("/resolve")
    public R<Void> resolve(@RequestParam String exceptionType, @RequestParam Long sourceId) {
        exceptionCenterService.resolveException(exceptionType, sourceId);
        return R.ok();
    }
}
