package com.ruoyi.system.service;

import com.ruoyi.system.domain.compliance.ImageComplianceResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片合规服务（M18 T18.3，OCR 文字路 + 视觉审核路两路并行，任一 FAIL 即整体 FAIL）
 * <p>上传前硬性校验（大小/格式/非空）；FEAT_IMAGE_COMPLIANCE 开关关闭时直接放行。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
public interface IImageComplianceService {

    /**
     * 图片合规检测主入口（文件上传场景）。
     *
     * @param file     图片文件（非空）
     * @param tenantId 租户ID（非空）
     * @param bizType  业务类型（可空）
     * @param bizId    业务主键（可空）
     * @return 检测结果（PASS/FAIL + 失败原因 + OCR/视觉命中 + 耗时）
     */
    ImageComplianceResult checkImage(MultipartFile file, Long tenantId, String bizType, String bizId);

    /**
     * 图片合规检测（URL 场景，兼容既有 ContentSecurityBlacklistChecker 委派）。
     *
     * @param imageUrl 图片URL（非空）
     * @param tenantId 租户ID（非空）
     * @param bizType  业务类型（可空）
     * @param bizId    业务主键（可空）
     * @return 检测结果
     */
    ImageComplianceResult checkImageByUrl(String imageUrl, Long tenantId, String bizType, String bizId);
}
