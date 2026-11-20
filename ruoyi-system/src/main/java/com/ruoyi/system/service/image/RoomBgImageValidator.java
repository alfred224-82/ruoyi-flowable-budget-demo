package com.ruoyi.system.service.image;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 房间背景图上传前置校验器（T26.3，小微版 M26 房间座位布局）。
 * <p>三重前置校验：① 大小 ≤ 2MB；② 真实格式（magic bytes，防伪装扩展名）∈ {JPEG, PNG, WebP}；
 * ③ 宽高 ≤ 1920×1080。任一重不通过抛 {@link ServiceException}，message 以「图片不合规：{原因}」呈现，供 PC 表单红字提示。
 * <p>★不做任何滤镜/蒙版/压缩加工（T26.3 明确约束），仅校验后由调用方落 OSS。
 * <p>尺寸读取：JPEG/PNG 走 JDK 内置 {@link ImageIO}；WebP（ImageIO 原生不支持）手动解析 RIFF 头
 * （VP8 / VP8L / VP8X 三种 chunk），无额外依赖，与 {@link ImageUploadValidator} 风格一致。
 * <p>说明：文档 T26.3 提及 {@code Thumbnailator}，但本代码库未引入该依赖，且其底层同样基于 ImageIO 不支持 WebP，
 * 故采用 ImageIO + RIFF 解析实现等价能力。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Slf4j
@Component
public class RoomBgImageValidator {

    /** 大小上限：2MB */
    private static final long MAX_BYTES = 2L * 1024 * 1024;
    /** 宽度上限（px） */
    private static final int MAX_WIDTH = 1920;
    /** 高度上限（px） */
    private static final int MAX_HEIGHT = 1080;

    /** 真实格式标识 */
    private static final String FMT_JPEG = "jpeg";
    private static final String FMT_PNG = "png";
    private static final String FMT_WEBP = "webp";

    /**
     * 校验房间背景图（大小 / 格式 / 尺寸三重前置校验）。
     *
     * @param file 上传的图片文件
     * @throws ServiceException 任一重校验未通过，message 形如「图片不合规：{原因}」
     */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw fail("上传文件不能为空");
        }
        // 第 1 重：大小 ≤ 2MB（先校验，避免无谓解析）
        long size = file.getSize();
        if (size > MAX_BYTES) {
            throw fail(StrUtil.format("图片大小 {}KB 超过上限 2MB，请压缩后重试", size / 1024));
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            // 读取字节流属系统级异常，留痕后转为业务提示
            log.error("[RoomBgImageValidator] 读取上传文件字节失败：name={}", file.getOriginalFilename(), e);
            throw fail("图片读取失败，请重试");
        }
        if (bytes == null || bytes.length == 0) {
            throw fail("图片内容为空");
        }
        // 第 2 重：真实格式（magic bytes）∈ {jpeg, png, webp}
        String format = detectFormat(bytes);
        if (format == null) {
            throw fail("仅支持 JPG/PNG/WebP 格式");
        }
        // 第 3 重：宽高 ≤ 1920×1080
        int[] wh = readDimensions(bytes, format);
        if (wh == null) {
            throw fail("图片解析失败，无法读取尺寸");
        }
        int width = wh[0];
        int height = wh[1];
        if (width > MAX_WIDTH || height > MAX_HEIGHT) {
            throw fail(StrUtil.format("图片尺寸 {}×{} 超过上限 1920×1080，请缩小后重试", width, height));
        }
        log.info("[RoomBgImageValidator] 背景图校验通过：format={}, {}×{}, size={}KB",
            format, width, height, size / 1024);
    }

    /**
     * 通过 magic bytes 探测真实图片格式（不依赖扩展名/Content-Type，防伪装）。
     *
     * @return jpeg / png / webp；无法识别返回 null
     */
    private String detectFormat(byte[] b) {
        if (b == null || b.length < 12) {
            return null;
        }
        // JPEG：FF D8 FF
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return FMT_JPEG;
        }
        // PNG：89 50 4E 47 0D 0A 1A 0A
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
            && (b[4] & 0xFF) == 0x0D && (b[5] & 0xFF) == 0x0A && (b[6] & 0xFF) == 0x1A && (b[7] & 0xFF) == 0x0A) {
            return FMT_PNG;
        }
        // WebP：RIFF....WEBP（偏移 0="RIFF"，偏移 8="WEBP"）
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
            && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return FMT_WEBP;
        }
        return null;
    }

    /**
     * 读取图片宽高（px）。JPEG/PNG 走 ImageIO；WebP 走 RIFF 头解析。
     *
     * @return int[]{width, height}；解析失败返回 null
     */
    private int[] readDimensions(byte[] bytes, String format) {
        if (FMT_WEBP.equals(format)) {
            return readWebpDimensions(bytes);
        }
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) {
                return null;
            }
            return new int[]{img.getWidth(), img.getHeight()};
        } catch (Exception e) {
            log.warn("[RoomBgImageValidator] ImageIO 解析尺寸失败：format={}, err={}", format, e.getMessage());
            return null;
        }
    }

    /**
     * 解析 WebP RIFF 头获取宽高。WebP 结构：
     * <pre>
     *   0  "RIFF"  |  4 size  |  8 "WEBP"  |  12 chunkFourCC  |  16 chunkSize  |  20 payload
     * </pre>
     * 支持三种 chunk：VP8X（扩展）/ VP8L（无损）/ VP8 （有损）。
     *
     * @return int[]{width, height}；无法解析返回 null
     */
    private int[] readWebpDimensions(byte[] b) {
        try {
            String fourcc = new String(b, 12, 4, StandardCharsets.US_ASCII);
            if ("VP8X".equals(fourcc)) {
                // 扩展格式：payload 偏移 24 起 3 字节（24-bit LE）宽-1，偏移 27 起 3 字节高-1
                if (b.length < 30) {
                    return null;
                }
                int w = (b[24] & 0xFF) | ((b[25] & 0xFF) << 8) | ((b[26] & 0xFF) << 16);
                int h = (b[27] & 0xFF) | ((b[28] & 0xFF) << 8) | ((b[29] & 0xFF) << 16);
                return new int[]{w + 1, h + 1};
            }
            if ("VP8L".equals(fourcc)) {
                // 无损格式：payload 偏移 20 为签名 0x2F，偏移 21 起 4 字节位域（宽-1 占 14bit，高-1 占 14bit）
                if (b.length < 25 || (b[20] & 0xFF) != 0x2F) {
                    return null;
                }
                long bits = (b[21] & 0xFFL) | ((b[22] & 0xFFL) << 8) | ((b[23] & 0xFFL) << 16) | ((b[24] & 0xFFL) << 24);
                int w = (int) (bits & 0x3FFF) + 1;
                int h = (int) ((bits >> 14) & 0x3FFF) + 1;
                return new int[]{w, h};
            }
            if ("VP8 ".equals(fourcc)) {
                // 有损格式：payload 偏移 23 起为起始码 9D 01 2A，偏移 26 起 2 字节（低 14bit）宽，偏移 28 起高
                if (b.length < 30) {
                    return null;
                }
                if ((b[23] & 0xFF) != 0x9D || (b[24] & 0xFF) != 0x01 || (b[25] & 0xFF) != 0x2A) {
                    return null;
                }
                int w = ((b[26] & 0xFF) | ((b[27] & 0xFF) << 8)) & 0x3FFF;
                int h = ((b[28] & 0xFF) | ((b[29] & 0xFF) << 8)) & 0x3FFF;
                return new int[]{w, h};
            }
            log.warn("[RoomBgImageValidator] 未知 WebP chunk 类型：fourcc={}", fourcc);
        } catch (Exception e) {
            log.warn("[RoomBgImageValidator] WebP RIFF 头解析失败：err={}", e.getMessage());
        }
        return null;
    }

    /**
     * 构造统一前缀的业务异常（「图片不合规：{原因}」）。
     */
    private ServiceException fail(String reason) {
        return new ServiceException("图片不合规：" + reason);
    }

    /**
     * ★M27 T27.2：校验平台默认模板背景图（必须为透明底 SVG / PNG）。
     * <p>与 {@link #validate(MultipartFile)} 的区别：默认图为「简洁线条示意图」，允许矢量 SVG（分辨率无关，
     *   前端按主题主色铺底联动），不接受 JPG / WebP（不透明真实照片）。SVG 走内容嗅探（防扩展名伪装），
     *   PNG 走 magic bytes + 宽高 ≤1920×1080 校验；SVG 为矢量图跳过像素尺寸校验。
     *
     * @param file 上传的默认模板背景图
     * @throws ServiceException 非 SVG/PNG、超大小或 PNG 超尺寸时抛出
     */
    public void validateDefaultTemplate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw fail("上传文件不能为空");
        }
        long size = file.getSize();
        if (size > MAX_BYTES) {
            throw fail(StrUtil.format("图片大小 {}KB 超过上限 2MB，请压缩后重试", size / 1024));
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("[RoomBgImageValidator] 读取默认模板文件字节失败：name={}", file.getOriginalFilename(), e);
            throw fail("图片读取失败，请重试");
        }
        if (bytes == null || bytes.length == 0) {
            throw fail("图片内容为空");
        }
        // SVG：内容嗅探（去 BOM/空白后以 <?xml 或 <svg 开头，且含 <svg 标签）
        if (isSvg(bytes)) {
            log.info("[RoomBgImageValidator] 默认模板 SVG 校验通过：size={}KB", size / 1024);
            return;
        }
        // PNG：magic bytes + 尺寸校验（透明底位图）
        String format = detectFormat(bytes);
        if (!FMT_PNG.equals(format)) {
            throw fail("平台默认模板背景图必须为透明底 SVG 或 PNG（简洁线条示意图）");
        }
        int[] wh = readDimensions(bytes, format);
        if (wh == null) {
            throw fail("图片解析失败，无法读取尺寸");
        }
        if (wh[0] > MAX_WIDTH || wh[1] > MAX_HEIGHT) {
            throw fail(StrUtil.format("图片尺寸 {}×{} 超过上限 1920×1080，请缩小后重试", wh[0], wh[1]));
        }
        log.info("[RoomBgImageValidator] 默认模板 PNG 校验通过：{}×{}, size={}KB", wh[0], wh[1], size / 1024);
    }

    /**
     * 内容嗅探判断是否为 SVG（不依赖扩展名/Content-Type，防伪装）。
     */
    private boolean isSvg(byte[] b) {
        if (b == null || b.length < 4) {
            return false;
        }
        // 跳过 UTF-8 BOM 与前导空白
        int i = 0;
        if (b.length >= 3 && (b[0] & 0xFF) == 0xEF && (b[1] & 0xFF) == 0xBB && (b[2] & 0xFF) == 0xBF) {
            i = 3;
        }
        while (i < b.length && (b[i] == ' ' || b[i] == '\t' || b[i] == '\r' || b[i] == '\n')) {
            i++;
        }
        int len = Math.min(b.length - i, 512);
        if (len <= 0) {
            return false;
        }
        String head = new String(b, i, len, StandardCharsets.UTF_8).toLowerCase();
        return (head.startsWith("<?xml") || head.startsWith("<svg")) && head.contains("<svg");
    }
}
