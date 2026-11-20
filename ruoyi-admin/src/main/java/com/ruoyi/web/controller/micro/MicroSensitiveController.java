package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.compliance.SensitiveWordCheckResult;
import com.ruoyi.system.service.ISensitiveWordEngine;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 敏感词预检 Controller（T32.8，设计方案 §十二 客诉预防）
 * <p>供顾客端投诉表单/老板端活动创建/门店设置 等文本输入场景做前端预检；
 * 服务端仍为权威终检（各业务 Service 内部调用 {@link ISensitiveWordEngine#check}）。
 * <p>★本接口不做业务落库，只做「命中/放行」快速反馈，避免用户提交后才知违规。
 * <p>权限：登录即可（顾客/员工/老板通用），FEAT_SENSITIVE_WORD 关闭时直接 PASS。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/sensitive")
public class MicroSensitiveController extends BaseController {

    private final ISensitiveWordEngine sensitiveWordEngine;

    /** 输入渠道：文本 */
    private static final String CHANNEL_TEXT = "text";

    /** 单次预检文本长度上限（防滥用） */
    private static final int MAX_TEXT_LENGTH = 2000;

    /**
     * 敏感词预检（登录即可）
     * <p>请求体：{ text: "待检文本", bizType: "complaint|activity|store|other", bizId: "业务主键（可空）" }
     * <p>响应体：{ result: "PASS|FAIL", hitWords: [{ word, category, position }], cost: 毫秒 }
     */
    @SaCheckLogin
    @PostMapping("/check")
    public R<SensitiveCheckVo> check(@RequestBody SensitiveCheckBo bo) {
        SensitiveCheckVo vo = new SensitiveCheckVo();
        if (bo == null || StrUtil.isBlank(bo.getText())) {
            vo.setResult(SensitiveWordCheckResult.PASS);
            vo.setHitWords(new ArrayList<>());
            vo.setCost(0L);
            return R.ok(vo);
        }
        String text = bo.getText();
        if (text.length() > MAX_TEXT_LENGTH) {
            text = text.substring(0, MAX_TEXT_LENGTH);
        }
        Long tenantId = LoginHelper.getTenantId();
        String bizType = StrUtil.blankToDefault(bo.getBizType(), "other");
        SensitiveWordCheckResult result = sensitiveWordEngine.check(
            text, tenantId, CHANNEL_TEXT, bizType, bo.getBizId());
        vo.setResult(result.getResult());
        vo.setCost(result.getCost());
        List<SensitiveCheckVo.HitWord> hits = (result.getHitWords() == null ? new ArrayList<SensitiveWordCheckResult.HitItem>() : result.getHitWords())
            .stream().map(h -> {
                SensitiveCheckVo.HitWord hw = new SensitiveCheckVo.HitWord();
                hw.setWord(h.getWord());
                hw.setCategory(h.getCategory());
                hw.setPosition(h.getPosition());
                return hw;
            }).collect(Collectors.toList());
        vo.setHitWords(hits);
        return R.ok(vo);
    }

    /** 预检请求体 */
    @Data
    public static class SensitiveCheckBo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        /** 待检文本（非空，超长按 MAX_TEXT_LENGTH 截断） */
        private String text;
        /** 业务类型：complaint/activity/store/rating/other（可空，用于行业过滤） */
        private String bizType;
        /** 业务主键（可空，写命中日志用） */
        private String bizId;
    }

    /** 预检响应体 */
    @Data
    public static class SensitiveCheckVo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        /** PASS=放行 / FAIL=命中禁提交 */
        private String result;
        /** 命中词列表（PASS 时为空） */
        private List<HitWord> hitWords;
        /** 检测耗时（毫秒） */
        private Long cost;

        @Data
        public static class HitWord implements Serializable {
            @Serial
            private static final long serialVersionUID = 1L;
            private String word;
            private String category;
            private Integer position;
        }
    }
}
