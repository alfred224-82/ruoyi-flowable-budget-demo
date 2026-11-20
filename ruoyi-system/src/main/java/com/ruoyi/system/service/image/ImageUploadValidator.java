package com.ruoyi.system.service.image;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.ruoyi.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Iterator;

/**
 * 图片上传前三重校验器（T15.35 §6.1 第 1~3 重：MIME / 尺寸比例 / 大小）。
 * <p>第 4 重「微信 imgSecCheck」由 {@code IContentSecurityService}（@Primary WechatImgSecCheckSecurity）承担，
 * 前三重与第四重均在 {@link com.ruoyi.system.service.impl.ThemeDecorationServiceImpl#decorate} 统一编排。
 * <p>校验失败抛 {@link ServiceException}，message 以 {@code [拒绝码]} 前缀携带业务码 + 中文原因，
 * 供 PC 表单红字提示；校验通过后方可落 OSS / 写库（不落 OSS、不写活动表的约束由编排层在校验失败时中断事务保证）。
 * <p>基于 JDK 内置 ImageIO + Hutool HttpUtil，无额外依赖，与 {@code OssRenderImagePipeline} 下载/解析风格一致。
 *
 * @author ruoyi
 * @date 2026-09-16
 */
@Slf4j
@Component
public class ImageUploadValidator {

    /** 拒绝码：MIME 非法（第 1 重） */
    public static final String IMG_MIME_INVALID = "IMG_MIME_INVALID";
    /** 拒绝码：尺寸比例非法（第 2 重） */
    public static final String IMG_RATIO_INVALID = "IMG_RATIO_INVALID";
    /** 拒绝码：大小超限（第 3 重） */
    public static final String IMG_SIZE_EXCEED = "IMG_SIZE_EXCEED";

    /** 场景：Logo（强制透明底 PNG，1:1，最小 200×200） */
    public static final String SCENE_LOGO = "logo";
    /** 场景：海报（jpg/png，750×1000，容差 ±5%） */
    public static final String SCENE_POSTER = "poster";

    /** 大小上限：200KB（v1.31 Logo/海报统一，对齐说明书 5.2） */
    private static final int MAX_BYTES = 200 * 1024;
    /** 尺寸比例容差 ±5% */
    private static final double RATIO_TOLERANCE = 0.05;
    /** Logo 最小边长（px） */
    private static final int LOGO_MIN_SIDE = 200;
    /** Logo 标准宽高比 1:1 */
    private static final double LOGO_RATIO = 1.0;
    /** 海报标准宽高比 750:1000 = 0.75 */
    private static final double POSTER_RATIO = 750.0 / 1000.0;

    /**
     * 执行前三重校验（MIME / 尺寸比例 / 大小）。素材 URL 为空时跳过（缺失兜底见 §6.4）。
     *
     * @param scene    场景（{@link #SCENE_LOGO} / {@link #SCENE_POSTER}）
     * @param imageUrl 原图 URL（已上传至 OSS/临时存储的地址）
     * @throws ServiceException 任一重校验未通过
     */
    public void validate(String scene, String imageUrl) {
        if (StrUtil.isBlank(imageUrl)) {
            return;
        }
        byte[] bytes;
        try {
            bytes = HttpUtil.downloadBytes(imageUrl);
        } catch (Exception e) {
            throw new ServiceException(StrUtil.format("[{}] 图片下载失败：{}", IMG_MIME_INVALID, e.getMessage()));
        }
        if (bytes == null || bytes.length == 0) {
            throw new ServiceException(StrUtil.format("[{}] 图片内容为空：{}", IMG_MIME_INVALID, imageUrl));
        }

        // 第 3 重：大小 ≤ 200KB
        if (bytes.length > MAX_BYTES) {
            throw new ServiceException(StrUtil.format("[{}] 图片大小 {}KB 超过上限 200KB，请压缩后重试",
                IMG_SIZE_EXCEED, bytes.length / 1024));
        }

        // 解析格式与尺寸
        String format = detectFormat(bytes);
        BufferedImage img;
        try {
            img = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            throw new ServiceException(StrUtil.format("[{}] 图片解析失败：{}", IMG_MIME_INVALID, e.getMessage()));
        }
        if (img == null) {
            throw new ServiceException(StrUtil.format("[{}] 非图片或不支持的格式：{}", IMG_MIME_INVALID, imageUrl));
        }

        // 第 1 重：MIME
        checkMime(scene, format);
        // 第 2 重：尺寸比例
        checkRatio(scene, img.getWidth(), img.getHeight());
    }

    /**
     * 第 1 重 MIME 校验：Logo 强制 png（透明底）；海报允许 png/jpeg。
     */
    private void checkMime(String scene, String format) {
        if (StrUtil.isBlank(format)) {
            throw new ServiceException(StrUtil.format("[{}] 无法识别图片格式，仅支持 PNG/JPEG", IMG_MIME_INVALID));
        }
        if (SCENE_LOGO.equals(scene)) {
            if (!"png".equals(format)) {
                throw new ServiceException(StrUtil.format("[{}] Logo 必须为透明底 PNG 格式，当前为 {}",
                    IMG_MIME_INVALID, format.toUpperCase()));
            }
            return;
        }
        if (!"png".equals(format) && !"jpeg".equals(format) && !"jpg".equals(format)) {
            throw new ServiceException(StrUtil.format("[{}] 海报仅支持 PNG/JPEG 格式，当前为 {}",
                IMG_MIME_INVALID, format.toUpperCase()));
        }
    }

    /**
     * 第 2 重尺寸比例校验：Logo 1:1（±5%）且最小边 ≥200；海报 750:1000（±5%）。
     */
    private void checkRatio(String scene, int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new ServiceException(StrUtil.format("[{}] 图片尺寸非法：{}×{}", IMG_RATIO_INVALID, width, height));
        }
        double ratio = (double) width / height;
        if (SCENE_LOGO.equals(scene)) {
            if (Math.abs(ratio - LOGO_RATIO) > RATIO_TOLERANCE) {
                throw new ServiceException(StrUtil.format("[{}] Logo 宽高比须为 1:1（±5%），当前 {}×{}",
                    IMG_RATIO_INVALID, width, height));
            }
            if (width < LOGO_MIN_SIDE || height < LOGO_MIN_SIDE) {
                throw new ServiceException(StrUtil.format("[{}] Logo 最小尺寸须 ≥200×200，当前 {}×{}",
                    IMG_RATIO_INVALID, width, height));
            }
            return;
        }
        // 海报：相对容差 |ratio - 0.75| / 0.75 ≤ 5%
        if (Math.abs(ratio - POSTER_RATIO) / POSTER_RATIO > RATIO_TOLERANCE) {
            throw new ServiceException(StrUtil.format("[{}] 海报宽高比须为 750:1000（±5%），当前 {}×{}",
                IMG_RATIO_INVALID, width, height));
        }
    }

    /**
     * 通过 ImageIO 读取真实图片格式名（png/jpeg/...），不依赖 URL 后缀（防伪装扩展名）。
     */
    private String detectFormat(byte[] bytes) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                return readers.next().getFormatName().toLowerCase();
            }
        } catch (Exception e) {
            log.warn("[ImageUploadValidator] 图片格式探测失败：{}", e.getMessage());
        }
        return null;
    }
}
