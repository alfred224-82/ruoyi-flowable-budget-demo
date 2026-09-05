package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.vo.ContentSecurityLogVo;
import com.ruoyi.system.service.security.ContentCheckResult;

/**
 * 内容安全审核服务接口（T12.4）
 * <p>统一封装 Provider 调用 + 审核留痕（写 biz_content_security_log）；
 * 审核不通过（REJECT/ERROR）一律拒绝，调用方据此拦截上传/发布。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IContentSecurityService {

    /**
     * 图片审核并留痕
     *
     * @param scene    场景：logo/poster/ugc_image
     * @param imageUrl 图片URL
     * @return 审核结果
     */
    ContentCheckResult checkImage(String scene, String imageUrl);

    /**
     * 文本审核并留痕
     *
     * @param scene 场景：ugc_text/nickname
     * @param text  待审文本
     * @return 审核结果
     */
    ContentCheckResult checkText(String scene, String text);

    /**
     * 审核日志分页查询
     */
    TableDataInfo<ContentSecurityLogVo> queryPageList(String scene, String result, PageQuery pageQuery);
}
