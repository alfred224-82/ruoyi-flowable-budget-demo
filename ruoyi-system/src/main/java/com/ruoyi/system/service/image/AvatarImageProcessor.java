package com.ruoyi.system.service.image;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.oss.core.OssClient;
import com.ruoyi.oss.factory.OssFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 头像图片多档处理器（T26 头像优化）
 * <p>用户上传头像后，服务端立即生成 sm(80px)/md(120px)/lg(200px) 三档 JPEG 缩略图，
 * 原图归档到 OSS 但不直接下发；前端按显示场景选用对应档位，避免手机端加载原图（294KB~2.9MB）。
 * <p>命名约定：原图 uuid.jpg → uuid_sm.jpg / uuid_md.jpg / uuid_lg.jpg
 * <p>★不使用 Thumbnailator（项目规范：不引入新依赖；且 Thumbnailator 不原生支持 WebP）。
 *   改用 JDK 内置 BufferedImage + Graphics2D 双立方插值缩放 + ImageIO JPEG 编码。
 *   缩尺寸带来的体积收益远大于调质量参数（文档实测：400px→6KB，120px→2KB）。
 *
 * @author ruoyi
 * @date 2026-10-17
 */
@Slf4j
@Service
public class AvatarImageProcessor {

    /** 三档尺寸：sm 顶栏小图 / md 列表常用 / lg 个人中心大图 */
    private static final int SM = 80;
    private static final int MD = 120;
    private static final int LG = 200;

    /** JPEG 编码质量（0~1），80 为体积/质量最佳平衡点 */
    private static final float JPEG_QUALITY = 0.80f;

    /**
     * 处理头像上传：生成三档缩略图并上传 OSS，返回各档 URL
     *
     * @param originalUrl  原图 OSS URL（已上传）
     * @param originalPath 原图 OSS 相对路径（如 avatar/2026/10/17/uuid.jpg）
     * @return Map: {original, sm, md, lg} 四档 URL
     */
    public Map<String, String> processAndUpload(String originalUrl, String originalPath) {
        if (StrUtil.isBlank(originalUrl) || StrUtil.isBlank(originalPath)) {
            throw new ServiceException("原图 URL 或路径为空");
        }

        // ① 下载原图字节
        byte[] srcBytes;
        try {
            OssClient client = OssFactory.instance();
            srcBytes = readObjectContent(client, originalPath);
        } catch (Exception e) {
            log.error("[AvatarProcessor] 下载原图失败: {}", originalUrl, e);
            throw new ServiceException("原图下载失败: " + e.getMessage());
        }
        if (srcBytes == null || srcBytes.length == 0) {
            throw new ServiceException("原图数据为空");
        }

        // ② 解析原图
        BufferedImage src;
        try {
            src = ImageIO.read(new ByteArrayInputStream(srcBytes));
        } catch (IOException e) {
            log.error("[AvatarProcessor] 原图解析失败: {}", originalUrl, e);
            throw new ServiceException("原图解析失败: " + e.getMessage());
        }
        if (src == null) {
            throw new ServiceException("原图非图片或格式不支持");
        }

        // ③ 生成三档缩略图 + 上传
        OssClient client = OssFactory.instance();
        String baseUrl = extractBaseUrl(originalUrl, originalPath);
        Map<String, String> result = new LinkedHashMap<>(4);
        result.put("original", originalUrl);

        int[] dimensions = { SM, MD, LG };
        String[] suffixes = { "sm", "md", "lg" };
        for (int i = 0; i < dimensions.length; i++) {
            int dimension = dimensions[i];
            String suffix = suffixes[i];
            try {
                BufferedImage resized = resizeCenterCrop(src, dimension);
                byte[] jpegBytes = encodeJpeg(resized);
                String variantPath = insertSizeSuffix(originalPath, suffix);
                uploadBytes(client, jpegBytes, variantPath, "image/jpeg");
                String variantUrl = baseUrl + "/" + variantPath;
                result.put(suffix, variantUrl);
            } catch (Exception e) {
                log.error("[AvatarProcessor] 生成 {} 档失败: {}", suffix, originalUrl, e);
                // 单档失败不阻断其他档，该档 URL 留空
                result.put(suffix, "");
            }
        }

        log.info("[AvatarProcessor] 头像三档生成完成: original={}KB, sm={}KB, md={}KB, lg={}KB",
            srcBytes.length / 1024,
            estimateSize(result.get("sm")),
            estimateSize(result.get("md")),
            estimateSize(result.get("lg")));

        return result;
    }

    /**
     * 中心裁切缩放：将原图等比缩放到至少 dimension×dimension，再居中裁切为正方形
     * <p>头像多为圆形显示，非正方形原图直接缩放会被拉变形。
     *
     * @param src       原图
     * @param dimension 目标边长（px）
     * @return 正方形缩略图
     */
    private BufferedImage resizeCenterCrop(BufferedImage src, int dimension) {
        int srcW = src.getWidth();
        int srcH = src.getHeight();

        // 等比缩放，使短边 >= dimension（^ 填充模式）
        double scale = Math.max((double) dimension / srcW, (double) dimension / srcH);
        int scaledW = Math.max(1, (int) Math.round(srcW * scale));
        int scaledH = Math.max(1, (int) Math.round(srcH * scale));

        BufferedImage scaled = new BufferedImage(scaledW, scaledH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();
        try {
            // 白色背景（防止透明 PNG 变黑）
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, scaledW, scaledH);
            // 双立方插值（质量最优）
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(src, 0, 0, scaledW, scaledH, null);
        } finally {
            g2d.dispose();
        }

        // 居中裁切为 dimension×dimension 正方形
        int cropX = (scaledW - dimension) / 2;
        int cropY = (scaledH - dimension) / 2;
        return scaled.getSubimage(cropX, cropY, dimension, dimension);
    }

    /**
     * JPEG 编码（quality=0.8，strip metadata）
     */
    private byte[] encodeJpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("无 JPEG 编码器");
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }

    /**
     * 在文件扩展名前插入尺寸后缀
     * <p>avatar/2026/10/17/uuid.jpg → avatar/2026/10/17/uuid_sm.jpg
     */
    private String insertSizeSuffix(String path, String suffix) {
        int dotIdx = path.lastIndexOf('.');
        if (dotIdx < 0) {
            return path + "_" + suffix;
        }
        return path.substring(0, dotIdx) + "_" + suffix + path.substring(dotIdx);
    }

    /**
     * 从完整 URL 提取 base URL（不含相对路径）
     * <p>originalUrl = baseUrl + "/" + originalPath
     * <p>https://domain/bucket/avatar/2026/10/17/uuid.jpg + avatar/2026/10/17/uuid.jpg → https://domain/bucket
     */
    private String extractBaseUrl(String originalUrl, String originalPath) {
        // originalUrl = baseUrl + "/" + originalPath → baseUrl = originalUrl 去掉 "/" + originalPath
        int tailLen = originalPath.length() + 1; // +1 为中间的 /
        if (originalUrl.length() <= tailLen) {
            return originalUrl;
        }
        return originalUrl.substring(0, originalUrl.length() - tailLen);
    }

    /**
     * 上传字节到 OSS 指定路径
     */
    private void uploadBytes(OssClient client, byte[] data, String path, String contentType) {
        client.upload(data, path, contentType);
    }

    /**
     * 从 OSS 读取对象字节
     */
    private byte[] readObjectContent(OssClient client, String path) throws IOException {
        try (java.io.InputStream is = client.getObjectContent(path)) {
            return cn.hutool.core.io.IoUtil.readBytes(is);
        }
    }

    /**
     * 估算 URL 对应文件大小（用于日志，失败返回 -1）
     */
    private long estimateSize(String url) {
        if (StrUtil.isBlank(url)) {
            return -1;
        }
        // 简单估算：URL 本身不携带大小信息，返回 -1
        // 实际大小需 HTTP HEAD 请求，此处省略
        return -1;
    }
}
