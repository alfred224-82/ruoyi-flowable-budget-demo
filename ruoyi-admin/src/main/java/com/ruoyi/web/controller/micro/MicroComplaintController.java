package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.ComplaintBo;
import com.ruoyi.system.domain.bo.ComplaintReplyBo;
import com.ruoyi.system.domain.vo.ComplaintRecordVo;
import com.ruoyi.system.domain.vo.SysOssVo;
import com.ruoyi.system.service.IMicroComplaintService;
import com.ruoyi.system.service.ISysOssService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 投诉管理 Controller（T25.1，小微版 M25）
 * <p>双渠道投诉：BOSS=渠道一 / PLATFORM=渠道二。
 * <p>★投诉管理为小微版核心功能，micro_pro/micro_lite 均开放，不使用 @RequireFeature 功能开关。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/complaint")
public class MicroComplaintController extends BaseController {

    private final IMicroComplaintService complaintService;
    private final ISysOssService sysOssService;

    /**
     * 双渠道投诉提交（顾客小程序）
     * <p>★顾客端操作，登录即可
     */
    @SaCheckLogin
    @Log(title = "投诉-提交", businessType = BusinessType.INSERT)
    @PostMapping("/submit")
    public R<Long> submit(@Validated(AddGroup.class) @RequestBody ComplaintBo bo) {
        return R.ok(complaintService.submitComplaint(bo));
    }

    /**
     * 本人投诉记录（顾客小程序）
     * <p>★顾客端操作，登录即可
     */
    @SaCheckLogin
    @GetMapping("/my")
    public R<List<ComplaintRecordVo>> my(
        @RequestParam(required = false) String channel) {
        return R.ok(complaintService.myComplaints(channel));
    }

    /**
     * 投诉页员工下拉数据源（本租户全部 salesman，不过滤空闲状态）
     * <p>★顾客端操作，登录即可
     */
    @SaCheckLogin
    @GetMapping("/staffOptions")
    public R<List<Map<String, Object>>> staffOptions() {
        return R.ok(complaintService.listStaffForComplaint());
    }

    /**
     * 投诉图片上传（顾客小程序，仅需登录，无需 system:oss:upload 权限）
     * <p>★顾客端操作，登录即可
     */
    @SaCheckLogin
    @Log(title = "投诉-图片上传", businessType = BusinessType.INSERT)
    @PostMapping(value = "/uploadImage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Map<String, String>> uploadImage(@RequestPart("file") MultipartFile file) {
        if (ObjectUtil.isNull(file)) {
            return R.fail("上传文件不能为空");
        }
        SysOssVo oss = sysOssService.upload(file);
        Map<String, String> map = new HashMap<>(2);
        map.put("url", oss.getUrl());
        map.put("fileName", oss.getOriginalName());
        return R.ok(map);
    }

    /**
     * Boss 查看本租户投诉列表（Boss PC）
     */
    @SaCheckPermission("micro:complaint:view")
    @GetMapping("/list")
    public R<List<ComplaintRecordVo>> list(
        @RequestParam(required = false) String channel,
        @RequestParam(required = false) String status) {
        return R.ok(complaintService.listForBoss(channel, status));
    }

    /**
     * Boss 处理投诉（Boss PC）
     */
    @SaCheckPermission("micro:complaint:handle")
    @Log(title = "投诉-处理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/reply")
    public R<Void> reply(@PathVariable Long id, @Validated @RequestBody ComplaintReplyBo bo) {
        complaintService.replyComplaint(id, bo);
        return R.ok();
    }
}
