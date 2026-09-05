package com.ruoyi.system.service.security;

/**
 * 内容安全审核抽象（T12.4 imgSecCheck / T14.4 msgSecCheck）
 * <p>扩展点模式（与既有 InvoiceVerifier 一致）：定义 Provider 接口，默认提供桩实现（放行/降级），
 * 接入微信真实 imgSecCheck/msgSecCheck 时新增实现并替换 Bean 即可，业务侧不感知。
 * <p>审核不通过一律拒绝并留痕（由 {@link com.ruoyi.system.service.IContentSecurityService} 统一写日志）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IContentSecurity {

    /**
     * 图片内容安全审核（微信 imgSecCheck）
     *
     * @param scene    场景：logo/poster/ugc_image
     * @param imageUrl 图片URL
     * @return 审核结果
     */
    ContentCheckResult checkImage(String scene, String imageUrl);

    /**
     * 文本内容安全审核（微信 msgSecCheck）
     *
     * @param scene 场景：ugc_text/nickname
     * @param text  待审文本
     * @return 审核结果
     */
    ContentCheckResult checkText(String scene, String text);
}
