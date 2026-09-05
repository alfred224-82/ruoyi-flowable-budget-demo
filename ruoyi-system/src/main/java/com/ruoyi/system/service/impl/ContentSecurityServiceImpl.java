package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.ContentSecurityLog;
import com.ruoyi.system.domain.vo.ContentSecurityLogVo;
import com.ruoyi.system.mapper.ContentSecurityLogMapper;
import com.ruoyi.system.service.IContentSecurityService;
import com.ruoyi.system.service.security.ContentCheckResult;
import com.ruoyi.system.service.security.IContentSecurity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 内容安全审核服务实现（T12.4）
 * <p>委派 {@link IContentSecurity} Provider（默认桩放行），统一写 biz_content_security_log 留痕。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentSecurityServiceImpl implements IContentSecurityService {

    private final IContentSecurity contentSecurity;
    private final ContentSecurityLogMapper logMapper;

    /** 文本留痕摘要最大长度（脱敏，避免存储完整敏感文本） */
    private static final int TEXT_REF_MAX = 100;

    @Override
    public ContentCheckResult checkImage(String scene, String imageUrl) {
        ContentCheckResult result = contentSecurity.checkImage(scene, imageUrl);
        writeLog(scene, "image", imageUrl, result);
        return result;
    }

    @Override
    public ContentCheckResult checkText(String scene, String text) {
        ContentCheckResult result = contentSecurity.checkText(scene, text);
        writeLog(scene, "text", StrUtil.maxLength(text, TEXT_REF_MAX), result);
        return result;
    }

    @Override
    public TableDataInfo<ContentSecurityLogVo> queryPageList(String scene, String result, PageQuery pageQuery) {
        LambdaQueryWrapper<ContentSecurityLog> wrapper = Wrappers.<ContentSecurityLog>lambdaQuery()
            .eq(StrUtil.isNotBlank(scene), ContentSecurityLog::getScene, scene)
            .eq(StrUtil.isNotBlank(result), ContentSecurityLog::getResult, result)
            .orderByDesc(ContentSecurityLog::getCreateTime);
        Page<ContentSecurityLogVo> page = logMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    private void writeLog(String scene, String contentType, String contentRef, ContentCheckResult result) {
        try {
            ContentSecurityLog entity = new ContentSecurityLog();
            entity.setScene(scene);
            entity.setContentType(contentType);
            entity.setContentRef(contentRef);
            entity.setProvider(result.getProvider());
            entity.setResult(result.getResult());
            entity.setLabel(result.getLabel());
            entity.setRawResponse(StrUtil.maxLength(result.getRawResponse(), 1000));
            logMapper.insert(entity);
        } catch (Exception e) {
            // 留痕失败不阻断主流程，仅告警
            log.warn("[ContentSecurity] 审核日志写入失败 scene={} result={}", scene, result.getResult(), e);
        }
    }
}
