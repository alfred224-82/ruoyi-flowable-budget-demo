package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.oss.core.OssClient;
import com.ruoyi.oss.entity.UploadResult;
import com.ruoyi.oss.factory.OssFactory;
import com.ruoyi.system.domain.PromotionActivity;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.domain.vo.SysOssVo;
import com.ruoyi.system.mapper.PromotionActivityMapper;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.service.IPosterGenerationService;
import com.ruoyi.system.service.ISysOssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.awt.geom.AffineTransform;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 海报生成服务实现（M31 T31.1/T31.2）
 * <p>Java2D 合成底图 + 右下角二维码 + 全图水印 → 上传 OSS → 返回合成后海报 URL。
 * <p>★水印样式与小程序端 watermark.js#overlayWatermark 统一（租户名+时间戳、45°斜排6条）。
 * <p>★线程安全：synchronized 锁 activityId 避免并发冲突。
 *
 * @author ruoyi
 * @date 2026-10-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PosterGenerationServiceImpl implements IPosterGenerationService {

    private final PromotionActivityMapper activityMapper;
    private final ISysOssService sysOssService;
    private final SysTenantMapper tenantMapper;

    /** 二维码尺寸（px） */
    private static final int QR_SIZE = 150;
    /** 二维码距右下角留白（px） */
    private static final int QR_MARGIN = 20;
    /** 二维码内容 URL 前缀（★指向顾客端活动详情页，非预约详情页） */
    private static final String QR_URL_PREFIX = "https://xiaozhangben.com.cn/h5/#/pages/micro/activity/detail?scene=";

    /** 异步任务状态缓存（预留） */
    private final Map<String, Map<String, Object>> taskCache = new ConcurrentHashMap<>();

    @Override
    public synchronized String generateActivityPoster(Long activityId, String bgImageUrl) {
        log.info("[PosterGen] activityId={} bgImageUrl={}", activityId, bgImageUrl);

        // ① 查询活动信息
        PromotionActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        Long tenantId = LoginHelper.getTenantId();

        // ② 生成二维码内容（含 tenant_id + activity_id + user_type=visitor + phone_last4 占位）
        String sceneParam = "t" + tenantId + "a" + activityId + "uvp0000";
        String qrContent = QR_URL_PREFIX + sceneParam;
        byte[] qrBytes = generateQrCode(qrContent, QR_SIZE, QR_SIZE);

        // ③ 下载底图（bgImageUrl 可能是 OSS ID 或完整 URL，统一解析）
        String resolvedUrl = resolveImageUrl(bgImageUrl);
        BufferedImage bgImage;
        try {
            bgImage = downloadImage(resolvedUrl);
        } catch (Exception e) {
            log.error("[PosterGen] 底图下载失败: {}", resolvedUrl, e);
            throw new ServiceException("底图下载失败: " + e.getMessage());
        }

        // ④ Java2D 合成（右下角叠加二维码 + 全图水印）
        BufferedImage composite = compositePoster(bgImage, qrBytes, tenantId);

        // ⑤ 上传 OSS
        String posterUrl = uploadPoster(composite, activityId);
        log.info("[PosterGen] 合成成功 activityId={} posterUrl={}", activityId, posterUrl);
        return posterUrl;
    }

    @Override
    public byte[] generateQrCode(String content, int width, int height) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>(4);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", os);
            return os.toByteArray();
        } catch (Exception e) {
            log.error("[PosterGen] 二维码生成失败: {}", content, e);
            throw new ServiceException("二维码生成失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getTaskStatus(String taskId) {
        Map<String, Object> status = taskCache.get(taskId);
        if (status == null) {
            status = new HashMap<>(2);
            status.put("status", "NOT_FOUND");
        }
        return status;
    }

    /**
     * 解析图片地址：支持 OSS ID（纯数字）和完整 HTTP/HTTPS URL。
     * <p>前端 image-upload 组件存储的是 ossId，需通过 ISysOssService 解析为完整 URL。
     */
    private String resolveImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new ServiceException("底图地址不能为空");
        }
        // 已是完整 URL，直接使用
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl;
        }
        // 纯数字视为 OSS ID，解析为完整 URL
        if (imageUrl.matches("\\d+")) {
            Long ossId = Long.parseLong(imageUrl);
            SysOssVo ossVo = sysOssService.getById(ossId);
            if (ossVo == null || ossVo.getUrl() == null) {
                throw new ServiceException("底图文件不存在: ossId=" + ossId);
            }
            log.info("[PosterGen] OSS ID {} 解析为 URL: {}", ossId, ossVo.getUrl());
            return ossVo.getUrl();
        }
        throw new ServiceException("底图地址格式无效: " + imageUrl);
    }

    /**
     * 下载图片（支持 HTTP/HTTPS URL）
     */
    private BufferedImage downloadImage(String imageUrl) throws Exception {
        URL url = new URL(imageUrl);
        try (InputStream is = url.openStream()) {
            return ImageIO.read(is);
        }
    }

    /**
     * 合成海报（底图 + 右下角二维码 + 全图水印）
     */
    private BufferedImage compositePoster(BufferedImage bgImage, byte[] qrBytes, Long tenantId) {
        int bgWidth = bgImage.getWidth();
        int bgHeight = bgImage.getHeight();

        // 创建画布（与底图同尺寸）
        BufferedImage canvas = new BufferedImage(bgWidth, bgHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        try {
            // 绘制底图
            g.drawImage(bgImage, 0, 0, bgWidth, bgHeight, null);

            // 解析二维码图片
            BufferedImage qrImage;
            try {
                qrImage = ImageIO.read(new ByteArrayInputStream(qrBytes));
            } catch (Exception e) {
                log.error("[PosterGen] 二维码图片解析失败", e);
                throw new ServiceException("二维码图片解析失败");
            }

            // 计算二维码位置（右下角留白 QR_MARGIN px）
            int qrX = bgWidth - QR_SIZE - QR_MARGIN;
            int qrY = bgHeight - QR_SIZE - QR_MARGIN;

            // 绘制白色衬底（二维码下方，增强可读性）
            int padSize = 10;
            g.setColor(Color.WHITE);
            g.fillRoundRect(qrX - padSize, qrY - padSize,
                QR_SIZE + padSize * 2, QR_SIZE + padSize * 2, 12, 12);

            // 绘制二维码
            g.drawImage(qrImage, qrX, qrY, QR_SIZE, QR_SIZE, null);

            // ★水印：与小程序端 watermark.js 统一（租户名+时间戳、45°斜排6条），最后绘制覆盖全图
            drawWatermark(g, bgWidth, bgHeight, buildWatermarkText(tenantId));
        } finally {
            g.dispose();
        }
        return canvas;
    }

    /**
     * 水印文字：{租户名称}  {yyyyMMddHHmmss}（与小程序端 watermark.js#buildWatermarkText 一致）
     */
    private String buildWatermarkText(Long tenantId) {
        String name = "";
        try {
            SysTenant tenant = tenantMapper.selectById(tenantId);
            if (tenant != null && StrUtil.isNotBlank(tenant.getCompanyName())) {
                name = tenant.getCompanyName();
            }
        } catch (Exception e) {
            log.debug("[PosterGen] 读取租户名称失败: {}", e.getMessage());
        }
        String ts = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        return StrUtil.isBlank(name) ? ts : name + "  " + ts;
    }

    /**
     * 绘制水印（与小程序端 watermark.js#overlayWatermark 样式统一）：
     * 粗体、字号约画布宽 5.5%、rgba(0,0,0,0.07)、自 15%~85% 高度 6 条均布、45° 斜排。
     */
    private void drawWatermark(Graphics2D g, int width, int height, String watermarkText) {
        int fontSize = Math.round(width * 0.055f);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(watermarkFont(fontSize));
            g2.setColor(new Color(0, 0, 0, 18)); // rgba(0,0,0,0.07)
            FontMetrics fm = g2.getFontMetrics();
            int textW = fm.stringWidth(watermarkText);
            int yOff = (fm.getAscent() - fm.getDescent()) / 2; // 近似 textBaseline=middle
            AffineTransform origin = g2.getTransform();
            int count = 6;
            for (int i = 0; i < count; i++) {
                double py = height * (0.15 + 0.7 * i / (count - 1));
                g2.setTransform(origin);
                g2.translate(width / 2.0, py);
                g2.rotate(-Math.PI / 4);
                g2.drawString(watermarkText, -textW / 2f, yOff);
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * 水印字体：优先选可用的中文字体（避免 Linux 服务器中文变方块），均无时回退 SANS_SERIF
     */
    private Font watermarkFont(int size) {
        String[] candidates = {"Microsoft YaHei", "PingFang SC", "Noto Sans CJK SC", "WenQuanYi Micro Hei", "SimHei"};
        Set<String> available = new HashSet<>(Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String family : candidates) {
            if (available.contains(family)) {
                return new Font(family, Font.BOLD, size);
            }
        }
        return new Font(Font.SANS_SERIF, Font.BOLD, size);
    }

    /**
     * 上传合成海报到 OSS
     */
    private String uploadPoster(BufferedImage poster, Long activityId) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(poster, "PNG", os);
            byte[] pngBytes = os.toByteArray();

            OssClient client = OssFactory.instance();
            UploadResult result = client.uploadSuffix(pngBytes, ".png", "image/png");
            return result.getUrl();
        } catch (Exception e) {
            log.error("[PosterGen] 海报上传 OSS 失败 activityId={}", activityId, e);
            throw new ServiceException("海报上传失败: " + e.getMessage());
        }
    }
}
