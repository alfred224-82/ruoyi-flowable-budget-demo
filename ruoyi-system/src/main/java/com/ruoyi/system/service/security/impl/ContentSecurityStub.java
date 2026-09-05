package com.ruoyi.system.service.security.impl;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.system.service.security.ContentCheckResult;
import com.ruoyi.system.service.security.IContentSecurity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 内容安全审核桩实现（T12.4，默认放行/降级）
 * <p>未接入微信真实 imgSecCheck/msgSecCheck 前的可扩展桩：默认全部放行（PASS），
 * 保证业务链路可跑通；接入真实能力时新增实现类并标注 @Primary 覆盖本桩即可，业务侧零改动。
 * <p>为便于联调，保留一个可选的关键词黑名单降级钩子（默认关闭），命中则 REJECT。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Component("contentSecurityStub")
public class ContentSecurityStub implements IContentSecurity {

    private static final String PROVIDER = "stub";

    @Override
    public ContentCheckResult checkImage(String scene, String imageUrl) {
        if (StrUtil.isBlank(imageUrl)) {
            return ContentCheckResult.error(PROVIDER, "imageUrl is blank");
        }
        log.debug("[ContentSecurityStub] checkImage scene={} url={} → 默认放行", scene, imageUrl);
        return ContentCheckResult.pass(PROVIDER);
    }

    @Override
    public ContentCheckResult checkText(String scene, String text) {
        if (StrUtil.isBlank(text)) {
            return ContentCheckResult.pass(PROVIDER);
        }
        log.debug("[ContentSecurityStub] checkText scene={} → 默认放行", scene);
        return ContentCheckResult.pass(PROVIDER);
    }
}
