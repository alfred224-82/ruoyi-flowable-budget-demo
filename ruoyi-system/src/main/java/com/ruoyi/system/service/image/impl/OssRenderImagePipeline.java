package com.ruoyi.system.service.image.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.oss.entity.UploadResult;
import com.ruoyi.oss.factory.OssFactory;
import com.ruoyi.system.domain.ImageTask;
import com.ruoyi.system.domain.TenantTheme;
import com.ruoyi.system.mapper.ImageTaskMapper;
import com.ruoyi.system.mapper.TenantThemeMapper;
import com.ruoyi.system.service.IThemePresetService;
import com.ruoyi.system.service.image.IImagePipeline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 图片处理管线真实实现（T12.3 Logo/海报处理 + T12.7 衍生图，覆盖 {@link ImagePipelineStub} 桩）。
 * <p>扩展点落地：标注 {@code @Primary} 后，{@link com.ruoyi.system.service.impl.ThemeDecorationServiceImpl}
 * 注入的 {@link IImagePipeline} 即为本类，业务侧零改动。
 * <p>处理能力（基于 JDK 内置 java.awt / ImageIO，无额外依赖）：
 * <ul>
 *   <li>{@link IImagePipeline#TASK_LOGO_RENDER}：Logo 归一化到 80×80，按主题 {@code --primary} 主色加圆角/圆形衬底；</li>
 *   <li>{@link IImagePipeline#TASK_POSTER_CARD}/{@link IImagePipeline#TASK_POSTER_DETAIL}：海报 cover 填充到
 *       750×400 / 750×1000，叠加顶部渐变遮罩（高度=图高×25%，主色 α200→透明）；</li>
 *   <li>k-means（k=3）提取主色/辅色 hex，回写 {@code tenant_theme.poster_dominant_color/poster_accent_color}；</li>
 *   <li>成品 PNG 上传 OSS，回写 {@code biz_image_task.result_url} + {@code tenant_theme.logo_rendered_url/rendered_poster_url}。</li>
 * </ul>
 * <p>★契约与桩一致（同步）：submit 内联完成「登记任务→渲染→置 SUCCESS+result_url→回写主题」后返回 taskId，
 * 因此 {@code ThemeDecorationServiceImpl.submitAndFetch} 可即时读到成品 URL，无需改动编排。
 * 图片处理为 CPU 轻量操作（Logo 80×80、海报 750×1000、k-means 采样≤6000 像素），单次通常 <1s。
 * <p>★异步化预留：{@code biz_image_task} 的 status(PENDING/PROCESSING/SUCCESS/FAILED)/result_url/retry_count
 * 结构已支持改为「submit 仅登记 PENDING + @Async 后台渲染 + 回调回写」；届时需同步改造 ThemeDecorationServiceImpl
 * 的同步取值为轮询/WebSocket 通知，并处理事务可见性（任务行须先提交）。当前 V1 采用同步契约以最小化改动面。
 *
 * @author ruoyi
 * @date 2026-09-16
 */
@Slf4j
@Primary
@Component("ossRenderImagePipeline")
@RequiredArgsConstructor
public class OssRenderImagePipeline implements IImagePipeline {

    private final ImageTaskMapper imageTaskMapper;
    private final TenantThemeMapper tenantThemeMapper;
    private final IThemePresetService themePresetService;

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    /** 主题 Design Token：主色 / 圆角 */
    private static final String TOKEN_PRIMARY = "--primary";
    private static final String TOKEN_RADIUS = "--radius";

    /** Logo 默认边长（px） */
    private static final int DEFAULT_LOGO_SIZE = 80;
    /** 默认衬底圆角（px） */
    private static final int DEFAULT_RADIUS = 8;
    /** 顶部渐变遮罩高度占比 */
    private static final double MASK_HEIGHT_RATIO = 0.25;
    /** 渐变遮罩顶部主色不透明度 */
    private static final int MASK_TOP_ALPHA = 200;
    /** k-means 采样像素上限（避免大图耗时/占内存） */
    private static final int KMEANS_SAMPLE_CAP = 6000;
    /** k-means 迭代次数 */
    private static final int KMEANS_ITERATIONS = 10;

    @Override
    public Long submit(String taskType, Long themeId, String sourceUrl, String spec) {
        ImageTask task = new ImageTask();
        task.setThemeId(themeId);
        task.setTaskType(taskType);
        task.setSourceUrl(sourceUrl);
        task.setSpec(spec);
        task.setStatus(STATUS_PENDING);
        task.setRetryCount(0);
        imageTaskMapper.insert(task);

        try {
            task.setStatus(STATUS_PROCESSING);
            imageTaskMapper.updateById(task);

            RenderResult rr = render(taskType, themeId, sourceUrl, spec);

            task.setStatus(STATUS_SUCCESS);
            task.setResultUrl(rr.url);
            imageTaskMapper.updateById(task);
            writebackTheme(themeId, taskType, rr);
            log.info("[ImagePipeline] 衍生图任务 {} 生成成功：{} → {}", task.getId(), sourceUrl, rr.url);
        } catch (Exception e) {
            task.setStatus(STATUS_FAILED);
            task.setErrorMsg(StrUtil.maxLength(e.getMessage(), 1000));
            imageTaskMapper.updateById(task);
            log.warn("[ImagePipeline] 衍生图任务 {} 生成失败：{}", task.getId(), sourceUrl, e);
        }
        return task.getId();
    }

    /**
     * 渲染主流程：下载原图 → k-means 取色 → 按任务类型渲染 → 上传 OSS
     */
    private RenderResult render(String taskType, Long themeId, String sourceUrl, String spec) {
        if (StrUtil.isBlank(sourceUrl)) {
            throw new ServiceException("原图 URL 为空");
        }
        byte[] srcBytes = HttpUtil.downloadBytes(sourceUrl);
        if (srcBytes == null || srcBytes.length == 0) {
            throw new ServiceException("原图下载为空: " + sourceUrl);
        }
        BufferedImage src;
        try {
            src = ImageIO.read(new ByteArrayInputStream(srcBytes));
        } catch (Exception e) {
            throw new ServiceException("原图解析失败: " + e.getMessage());
        }
        if (src == null) {
            throw new ServiceException("原图非图片或不支持的格式: " + sourceUrl);
        }

        // k-means（k=3）主色/辅色
        Color[] palette = kMeans(src, 3);
        String dominantHex = toHex(palette[0]);
        String accentHex = palette.length > 1 ? toHex(palette[1]) : null;

        // 主题主色/圆角（缺失回退图片主色）
        TenantTheme theme = themeId != null ? tenantThemeMapper.selectById(themeId) : null;
        Color primary = resolvePrimary(theme, palette[0]);
        int radius = resolveRadius(theme);

        BufferedImage out;
        if (TASK_LOGO_RENDER.equals(taskType)) {
            int size = parseSpec(spec, new int[]{DEFAULT_LOGO_SIZE, DEFAULT_LOGO_SIZE})[0];
            out = renderLogo(src, size, primary, radius);
        } else {
            int[] def = TASK_POSTER_DETAIL.equals(taskType) ? new int[]{750, 1000} : new int[]{750, 400};
            int[] wh = parseSpec(spec, def);
            out = renderPoster(src, wh[0], wh[1], primary);
        }

        byte[] png = toPng(out);
        UploadResult upload = OssFactory.instance().uploadSuffix(png, ".png", "image/png");

        RenderResult rr = new RenderResult();
        rr.url = upload.getUrl();
        rr.dominantHex = dominantHex;
        rr.accentHex = accentHex;
        return rr;
    }

    /**
     * Logo 衬底与归一化：主题主色圆角/圆形衬底 + Logo 等比缩放居中（20% 内边距）
     */
    private BufferedImage renderLogo(BufferedImage src, int size, Color primary, int radius) {
        BufferedImage canvas = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        try {
            setQuality(g);
            // 衬底：radius>=size/2 视觉近似圆形
            g.setColor(primary);
            int arc = Math.min(radius * 2, size);
            g.fillRoundRect(0, 0, size, size, arc, arc);
            // Logo 等比缩放居中
            int pad = (int) (size * 0.2);
            int box = Math.max(1, size - pad * 2);
            int[] fit = fitWithin(src.getWidth(), src.getHeight(), box, box);
            int x = (size - fit[0]) / 2;
            int y = (size - fit[1]) / 2;
            g.drawImage(src, x, y, fit[0], fit[1], null);
        } finally {
            g.dispose();
        }
        return canvas;
    }

    /**
     * 海报：cover 填充到目标尺寸 + 顶部渐变遮罩（主色 α200 → 透明）
     */
    private BufferedImage renderPoster(BufferedImage src, int w, int h, Color primary) {
        BufferedImage canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        try {
            setQuality(g);
            // cover 填充：等比放大到覆盖画布后居中（超出部分自然裁剪）
            double scale = Math.max((double) w / src.getWidth(), (double) h / src.getHeight());
            int sw = (int) Math.round(src.getWidth() * scale);
            int sh = (int) Math.round(src.getHeight() * scale);
            int x = (w - sw) / 2;
            int y = (h - sh) / 2;
            g.drawImage(src, x, y, sw, sh, null);
            // 顶部渐变遮罩
            int maskH = Math.max(1, (int) (h * MASK_HEIGHT_RATIO));
            Color top = new Color(primary.getRed(), primary.getGreen(), primary.getBlue(), MASK_TOP_ALPHA);
            Color bottom = new Color(primary.getRed(), primary.getGreen(), primary.getBlue(), 0);
            g.setPaint(new GradientPaint(0, 0, top, 0, maskH, bottom));
            g.fillRect(0, 0, w, maskH);
        } finally {
            g.dispose();
        }
        return canvas;
    }

    /**
     * k-means（k 簇）主色提取：采样像素 → 迭代聚类 → 按簇大小降序返回 [主色, 辅色, ...]
     */
    private Color[] kMeans(BufferedImage src, int k) {
        int w = src.getWidth();
        int h = src.getHeight();
        int step = Math.max(1, (int) Math.sqrt((double) w * h / KMEANS_SAMPLE_CAP));
        List<int[]> samples = new ArrayList<>();
        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                int rgb = src.getRGB(x, y);
                int a = (rgb >> 24) & 0xFF;
                if (a < 128) {
                    // 跳过透明像素，避免透明底 Logo 干扰主色
                    continue;
                }
                samples.add(new int[]{(rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF});
            }
        }
        if (samples.isEmpty()) {
            return new Color[]{Color.GRAY};
        }
        int kk = Math.min(k, samples.size());
        double[][] centroids = new double[kk][3];
        for (int i = 0; i < kk; i++) {
            int[] p = samples.get((int) ((long) i * samples.size() / kk));
            centroids[i][0] = p[0];
            centroids[i][1] = p[1];
            centroids[i][2] = p[2];
        }
        int[] counts = new int[kk];
        double[][] sums = new double[kk][3];
        for (int iter = 0; iter < KMEANS_ITERATIONS; iter++) {
            Arrays.fill(counts, 0);
            for (int i = 0; i < kk; i++) {
                sums[i][0] = 0;
                sums[i][1] = 0;
                sums[i][2] = 0;
            }
            for (int[] p : samples) {
                int best = 0;
                double bestD = Double.MAX_VALUE;
                for (int i = 0; i < kk; i++) {
                    double dr = p[0] - centroids[i][0];
                    double dg = p[1] - centroids[i][1];
                    double db = p[2] - centroids[i][2];
                    double d = dr * dr + dg * dg + db * db;
                    if (d < bestD) {
                        bestD = d;
                        best = i;
                    }
                }
                counts[best]++;
                sums[best][0] += p[0];
                sums[best][1] += p[1];
                sums[best][2] += p[2];
            }
            for (int i = 0; i < kk; i++) {
                if (counts[i] > 0) {
                    centroids[i][0] = sums[i][0] / counts[i];
                    centroids[i][1] = sums[i][1] / counts[i];
                    centroids[i][2] = sums[i][2] / counts[i];
                }
            }
        }
        // 按簇大小降序（主色=最大簇）
        final int[] fcounts = counts;
        Integer[] order = new Integer[kk];
        for (int i = 0; i < kk; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Integer.compare(fcounts[b], fcounts[a]));
        Color[] result = new Color[kk];
        for (int i = 0; i < kk; i++) {
            int idx = order[i];
            result[i] = new Color(clamp(centroids[idx][0]), clamp(centroids[idx][1]), clamp(centroids[idx][2]));
        }
        return result;
    }

    /**
     * 回写主题成品：Logo 任务写 logo_rendered_url；海报任务写 rendered_poster_url + 主色/辅色
     */
    private void writebackTheme(Long themeId, String taskType, RenderResult rr) {
        if (themeId == null || rr == null || StrUtil.isBlank(rr.url)) {
            return;
        }
        TenantTheme exist = tenantThemeMapper.selectById(themeId);
        if (exist == null) {
            return;
        }
        TenantTheme update = new TenantTheme();
        update.setId(exist.getId());
        if (TASK_LOGO_RENDER.equals(taskType)) {
            update.setLogoRenderedUrl(rr.url);
        } else {
            update.setRenderedPosterUrl(rr.url);
            update.setPosterDominantColor(rr.dominantHex);
            update.setPosterAccentColor(rr.accentHex);
        }
        tenantThemeMapper.updateById(update);
    }

    private Color resolvePrimary(TenantTheme theme, Color fallback) {
        if (theme == null || StrUtil.isBlank(theme.getPresetKey())) {
            return fallback;
        }
        Map<String, String> tokens = themePresetService.getTokens(theme.getPresetKey());
        Color c = tokens == null ? null : parseHex(tokens.get(TOKEN_PRIMARY));
        return c != null ? c : fallback;
    }

    private int resolveRadius(TenantTheme theme) {
        if (theme == null || StrUtil.isBlank(theme.getPresetKey())) {
            return DEFAULT_RADIUS;
        }
        Map<String, String> tokens = themePresetService.getTokens(theme.getPresetKey());
        String r = tokens == null ? null : tokens.get(TOKEN_RADIUS);
        if (StrUtil.isBlank(r)) {
            return DEFAULT_RADIUS;
        }
        String digits = r.replaceAll("[^0-9]", "");
        if (StrUtil.isBlank(digits)) {
            return DEFAULT_RADIUS;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return DEFAULT_RADIUS;
        }
    }

    private void setQuality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private int[] fitWithin(int sw, int sh, int bw, int bh) {
        double scale = Math.min((double) bw / sw, (double) bh / sh);
        int w = Math.max(1, (int) Math.round(sw * scale));
        int h = Math.max(1, (int) Math.round(sh * scale));
        return new int[]{w, h};
    }

    private int[] parseSpec(String spec, int[] def) {
        if (StrUtil.isBlank(spec)) {
            return def;
        }
        String[] parts = spec.toLowerCase().split("x");
        if (parts.length != 2) {
            return def;
        }
        try {
            return new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private byte[] toPng(BufferedImage img) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new ServiceException("成品图编码失败: " + e.getMessage());
        }
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    private Color parseHex(String hex) {
        if (StrUtil.isBlank(hex)) {
            return null;
        }
        String s = hex.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.length() == 3) {
            s = "" + s.charAt(0) + s.charAt(0) + s.charAt(1) + s.charAt(1) + s.charAt(2) + s.charAt(2);
        }
        if (s.length() != 6) {
            return null;
        }
        try {
            int r = Integer.parseInt(s.substring(0, 2), 16);
            int g = Integer.parseInt(s.substring(2, 4), 16);
            int b = Integer.parseInt(s.substring(4, 6), 16);
            return new Color(r, g, b);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int clamp(double v) {
        if (v < 0) {
            return 0;
        }
        if (v > 255) {
            return 255;
        }
        return (int) Math.round(v);
    }

    /** 渲染结果载体（成品 URL + 主色/辅色 hex） */
    private static class RenderResult {
        private String url;
        private String dominantHex;
        private String accentHex;
    }
}
