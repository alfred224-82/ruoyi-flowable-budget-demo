package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.domain.vo.PromotionActivityVo;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.IActivityPosterService;
import com.ruoyi.system.service.IPromotionActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * 活动推广海报服务实现（M37 T37.1~T37.4）
 * <p>五要素：① 活动主视觉 ② 标题/时间/地点/价格 ③ 实时剩余名额 ④ 二维码（含隐形推广码）⑤ 行动指令。
 * <p>二维码归因规则：
 * <ul>
 *   <li>staff（一人一码）：URL 带 ref_u=当前员工 → 扫码计入该员工拉新业绩；</li>
 *   <li>store（门店码）：URL 不带 ref_u → 扫码计入门店自然流量，不归因任何员工。</li>
 * </ul>
 * <p>★服务端 Canvas（Java2D）渲染为 T37.4 交付；渲染失败（缺 CJK 字体等）返回 null，
 * 前端降级用 {@link #getPosterData} + html-to-image 渲染（v2 交付包 lib/）。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityPosterServiceImpl implements IActivityPosterService {

    private final IPromotionActivityService promotionActivityService;
    private final SysUserMapper userMapper;
    private final SysTenantMapper tenantMapper;

    /** H5 推广码基础 URL（二维码指向的登录页地址，与 PromotionDistributionServiceImpl 一致） */
    @Value("${promo.h5-base-url:https://xiaozhangben.com.cn/h5/#/pages/login/index}")
    private String h5BaseUrl;

    /** 码类型 */
    private static final String CODE_STAFF = "staff";
    private static final String CODE_STORE = "store";

    /** 海报尺寸（竖版，2 倍图便于清晰） */
    private static final int POSTER_W = 750;
    private static final int POSTER_H = 1200;

    // ===================================================================
    // T37.1/T37.2/T37.3 海报数据聚合
    // ===================================================================

    @Override
    public Map<String, Object> getPosterData(Long activityId, String codeType) {
        // ★fail-fast
        if (activityId == null) {
            throw new ServiceException("活动ID不能为空");
        }
        String type = StrUtil.isBlank(codeType) ? CODE_STAFF : codeType.trim().toLowerCase();
        Long tenantId = LoginHelper.getTenantId();
        Long userId = LoginHelper.getUserId();
        if (tenantId == null) {
            throw new ServiceException("当前租户上下文缺失，无法生成海报");
        }
        // 一站式取活动详情 + 报名/名额（含 roomNames/quota/remaining/perPersonPrice）
        Map<String, Object> detail = promotionActivityService.getSignupDetail(activityId);
        PromotionActivityVo activity = (PromotionActivityVo) detail.get("activity");
        if (activity == null) {
            throw new ServiceException("活动不存在: " + activityId);
        }

        // ★T37.1/T37.2 二维码 URL：staff 带 ref_u 归因，store 不带（门店自然流量）
        String h5Url = buildH5Url(tenantId, userId, activityId, type);
        String qrcodeUrl = generateQrDataUrl(h5Url);

        // 五要素文本
        String title = StrUtil.isNotBlank(activity.getTitle()) ? activity.getTitle()
            : (StrUtil.isNotBlank(activity.getAutoTitle()) ? activity.getAutoTitle() : "精彩活动");
        String timeText = formatTimeRange(activity.getStartTime(), activity.getEndTime());
        String locationText = resolveLocation(detail, tenantId);
        String priceText = resolvePrice(detail, activity);

        // 实时剩余名额（T37.3）
        Object remaining = detail.get("remaining");
        Object quota = detail.get("quota");
        Object signedCount = detail.get("signedCount");

        // 主题色（供前端 html-to-image / 服务端渲染共用）
        String themeKey = StrUtil.isNotBlank(activity.getThemeKey()) ? activity.getThemeKey() : "theme_fresh";

        Map<String, Object> result = new LinkedHashMap<>(16);
        result.put("activityId", activityId);
        result.put("codeType", type);
        result.put("title", title);
        result.put("timeText", timeText);
        result.put("locationText", locationText);
        result.put("priceText", priceText);
        result.put("coverUrl", activity.getCoverUrl());
        result.put("bgImageUrl", activity.getBgImageUrl());
        result.put("themeKey", themeKey);
        result.put("quota", quota);
        result.put("signedCount", signedCount);
        result.put("remaining", remaining);
        result.put("remainingText", remaining == null ? "名额充足" : ("仅剩 " + remaining + " 个名额"));
        result.put("actionText", "扫码看详情并报名");
        result.put("qrcodeUrl", qrcodeUrl);
        result.put("h5Url", h5Url);
        // 归属信息（前端展示：谁的码）
        if (CODE_STAFF.equals(type)) {
            result.put("ownerName", resolveUserName(userId));
            result.put("ownerType", "员工推广码");
        } else {
            result.put("ownerName", resolveStoreName(tenantId));
            result.put("ownerType", "门店码（自然流量）");
        }
        result.put("footer", "由「小账本」生成");
        return result;
    }

    @Override
    public Map<String, Object> getRemaining(Long activityId) {
        if (activityId == null) {
            throw new ServiceException("活动ID不能为空");
        }
        Map<String, Object> detail = promotionActivityService.getSignupDetail(activityId);
        Map<String, Object> result = new LinkedHashMap<>(4);
        result.put("quota", detail.get("quota"));
        result.put("signedCount", detail.get("signedCount"));
        result.put("remaining", detail.get("remaining"));
        return result;
    }

    // ===================================================================
    // T37.4 服务端 Canvas（Java2D）渲染
    // ===================================================================

    @Override
    public String renderPoster(Long activityId, String codeType) {
        Map<String, Object> data;
        try {
            data = getPosterData(activityId, codeType);
        } catch (Exception e) {
            log.warn("[renderPoster] 海报数据聚合失败: activityId={}, err={}", activityId, e.getMessage());
            return null;
        }
        try {
            int[] theme = resolveThemeColors(String.valueOf(data.get("themeKey")));
            Color brand = new Color(theme[0]);
            Color brand2 = new Color(theme[1]);
            Color pageBg = new Color(theme[2]);
            Color textColor = new Color(theme[3]);

            BufferedImage canvas = new BufferedImage(POSTER_W, POSTER_H, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = canvas.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                // 背景
                g.setColor(pageBg);
                g.fillRect(0, 0, POSTER_W, POSTER_H);

                // ① 活动主视觉区（顶部 520px）：优先加载封面图，失败则主题渐变
                int visualH = 520;
                BufferedImage cover = loadImage(String.valueOf(data.get("coverUrl")));
                if (cover != null) {
                    drawCover(g, cover, 0, 0, POSTER_W, visualH);
                } else {
                    g.setPaint(new GradientPaint(0, 0, brand, POSTER_W, visualH, brand2));
                    g.fillRect(0, 0, POSTER_W, visualH);
                }
                // 主视觉底部渐变遮罩，保证标题可读
                g.setPaint(new GradientPaint(0, visualH - 160, new Color(0, 0, 0, 0), 0, visualH, new Color(0, 0, 0, 140)));
                g.fillRect(0, visualH - 160, POSTER_W, 160);

                // ② 标题（叠加在主视觉底部）
                g.setColor(Color.WHITE);
                g.setFont(boldFont(46));
                String title = wrapText(String.valueOf(data.get("title")), g.getFontMetrics(), POSTER_W - 80);
                drawMultiline(g, title, 40, visualH - 90, 54);

                // 信息卡片
                int cardX = 32, cardY = visualH + 28, cardW = POSTER_W - 64, cardH = 340;
                g.setColor(Color.WHITE);
                g.fill(new RoundRectangle2D.Float(cardX, cardY, cardW, cardH, 28, 28));

                // 时间 / 地点 / 价格 三行
                g.setFont(boldFont(30));
                int rowY = cardY + 62;
                rowY = drawInfoRow(g, "🕐 时间", String.valueOf(data.get("timeText")), cardX + 32, rowY, textColor, cardW - 64);
                rowY = drawInfoRow(g, "📍 地点", String.valueOf(data.get("locationText")), cardX + 32, rowY, textColor, cardW - 64);
                rowY = drawInfoRow(g, "💰 费用", String.valueOf(data.get("priceText")), cardX + 32, rowY, textColor, cardW - 64);

                // ③ 实时剩余名额徽标
                String remainingText = String.valueOf(data.get("remainingText"));
                g.setFont(boldFont(34));
                FontMetrics rfm = g.getFontMetrics();
                int badgeW = rfm.stringWidth(remainingText) + 56;
                int badgeX = cardX + 32;
                int badgeY = cardY + cardH - 78;
                g.setColor(brand);
                g.fill(new RoundRectangle2D.Float(badgeX, badgeY, badgeW, 56, 28, 28));
                g.setColor(Color.WHITE);
                g.drawString(remainingText, badgeX + 28, badgeY + 39);

                // ④ 二维码 + ⑤ 行动指令
                int qrY = cardY + cardH + 40;
                String qrDataUrl = String.valueOf(data.get("qrcodeUrl"));
                BufferedImage qr = decodeDataUrl(qrDataUrl);
                int qrSize = 220;
                int qrX = 48;
                if (qr != null) {
                    g.setColor(Color.WHITE);
                    g.fill(new RoundRectangle2D.Float(qrX - 12, qrY - 12, qrSize + 24, qrSize + 24, 20, 20));
                    g.drawImage(qr, qrX, qrY, qrSize, qrSize, null);
                }
                // 行动指令 + 归属
                int textX = qrX + qrSize + 48;
                g.setColor(textColor);
                g.setFont(boldFont(38));
                g.drawString(String.valueOf(data.get("actionText")), textX, qrY + 60);
                g.setFont(plainFont(28));
                g.setColor(new Color(0x86, 0x90, 0x9C));
                g.drawString(String.valueOf(data.get("ownerType")), textX, qrY + 110);
                g.drawString(String.valueOf(data.get("ownerName")), textX, qrY + 150);

                // 页脚
                g.setFont(plainFont(24));
                g.setColor(new Color(0xA0, 0xA6, 0xB0));
                String footer = String.valueOf(data.get("footer"));
                FontMetrics ffm = g.getFontMetrics();
                g.drawString(footer, (POSTER_W - ffm.stringWidth(footer)) / 2, POSTER_H - 40);
            } finally {
                g.dispose();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(canvas, "png", baos);
            byte[] bytes = baos.toByteArray();
            String dataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
            log.info("[renderPoster] 服务端海报渲染成功: activityId={}, codeType={}, size={}KB",
                activityId, codeType, bytes.length / 1024);
            return dataUrl;
        } catch (Throwable e) {
            // ★缺 CJK 字体 / headless 异常等 → 返回 null，前端降级 html-to-image
            log.warn("[renderPoster] 服务端渲染失败，前端降级 html-to-image: activityId={}, err={}", activityId, e.getMessage());
            return null;
        }
    }

    // ===================================================================
    // 私有工具
    // ===================================================================

    /** 构造 H5 推广链接（staff 带 ref_u 归因；store 不带 → 门店自然流量） */
    private String buildH5Url(Long tenantId, Long userId, Long activityId, String codeType) {
        StringBuilder sb = new StringBuilder(h5BaseUrl);
        sb.append("?ref_t=").append(tenantId);
        if (CODE_STAFF.equals(codeType) && userId != null) {
            sb.append("&ref_u=").append(userId);
        }
        sb.append("&act=").append(activityId);
        return sb.toString();
    }

    /** ZXing 生成二维码 base64 data URL */
    private String generateQrDataUrl(String content) {
        BufferedImage img = generateQrImage(content, 430);
        if (img == null) {
            return null;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.warn("[generateQrDataUrl] 二维码编码失败: {}", e.getMessage());
            return null;
        }
    }

    /** ZXing 生成二维码 BufferedImage（供服务端渲染直接绘制） */
    private BufferedImage generateQrImage(String content, int size) {
        if (StrUtil.isBlank(content)) {
            return null;
        }
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            return MatrixToImageWriter.toBufferedImage(matrix);
        } catch (Exception e) {
            log.warn("[generateQrImage] 二维码生成失败: content={}, err={}", content, e.getMessage());
            return null;
        }
    }

    /** 解码 base64 data URL → BufferedImage */
    private BufferedImage decodeDataUrl(String dataUrl) {
        if (StrUtil.isBlank(dataUrl) || !dataUrl.contains(",")) {
            return null;
        }
        try {
            String base64 = dataUrl.substring(dataUrl.indexOf(',') + 1);
            byte[] bytes = Base64.getDecoder().decode(base64);
            return ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        } catch (Exception e) {
            return null;
        }
    }

    /** 加载远程图片（带超时，失败返回 null） */
    private BufferedImage loadImage(String url) {
        if (StrUtil.isBlank(url) || "null".equals(url)) {
            return null;
        }
        try {
            URLConnection conn = new URL(url).openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(4000);
            try (java.io.InputStream in = conn.getInputStream()) {
                return ImageIO.read(in);
            }
        } catch (Exception e) {
            log.debug("[loadImage] 封面加载失败，降级渐变背景: url={}, err={}", url, e.getMessage());
            return null;
        }
    }

    /** 等比裁剪绘制封面（居中裁满目标区域） */
    private void drawCover(Graphics2D g, BufferedImage cover, int x, int y, int w, int h) {
        double scale = Math.max((double) w / cover.getWidth(), (double) h / cover.getHeight());
        int dw = (int) (cover.getWidth() * scale);
        int dh = (int) (cover.getHeight() * scale);
        int dx = x - (dw - w) / 2;
        int dy = y - (dh - h) / 2;
        g.drawImage(cover, dx, dy, dw, dh, null);
    }

    /** 绘制信息行（图标标签 + 值），返回下一行 Y 坐标 */
    private int drawInfoRow(Graphics2D g, String label, String value, int x, int y, Color textColor, int maxW) {
        g.setColor(new Color(0x86, 0x90, 0x9C));
        g.setFont(plainFont(28));
        g.drawString(label, x, y);
        g.setColor(textColor);
        g.setFont(boldFont(30));
        String v = wrapText(value, g.getFontMetrics(), maxW - 130);
        int lineY = y;
        for (String line : v.split("\n")) {
            g.drawString(line, x + 120, lineY);
            lineY += 40;
        }
        return Math.max(y + 62, lineY + 22);
    }

    /** 主题色解析：返回 [brand, brand2, pageBg, text] 的 RGB */
    private int[] resolveThemeColors(String themeKey) {
        if (themeKey == null) {
            themeKey = "";
        }
        switch (themeKey) {
            case "theme_vivid":
            case "t2":
                return new int[]{0xF5455C, 0xFF7A45, 0xFFF1F0, 0x2B2B2B};
            case "theme_dark":
            case "t3":
                return new int[]{0x5B8FFF, 0x00E5FF, 0x0B0F19, 0xE8ECF4};
            case "theme_handdrawn":
            case "t4":
                return new int[]{0xFFB347, 0xFF8A8A, 0xFFF9F0, 0x3A3A3A};
            case "theme_fresh":
            case "t1":
            default:
                return new int[]{0x4F8FF7, 0x7AB8FF, 0xF6F8FC, 0x1D2129};
        }
    }

    /** 时间区间格式化 */
    private String formatTimeRange(Date start, Date end) {
        if (start == null) {
            return "时间待定";
        }
        SimpleDateFormat dayFmt = new SimpleDateFormat("MM月dd日 HH:mm");
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm");
        StringBuilder sb = new StringBuilder(dayFmt.format(start));
        if (end != null) {
            sb.append(" ~ ").append(timeFmt.format(end));
        }
        return sb.toString();
    }

    /** 地点：优先房间名，兜底门店地址/城市 */
    @SuppressWarnings("unchecked")
    private String resolveLocation(Map<String, Object> detail, Long tenantId) {
        Object roomNames = detail.get("roomNames");
        if (roomNames instanceof List && !((List<Object>) roomNames).isEmpty()) {
            return String.join("、", ((List<Object>) roomNames).stream().map(String::valueOf).toArray(String[]::new));
        }
        try {
            SysTenant tenant = tenantMapper.selectById(tenantId);
            if (tenant != null) {
                if (StrUtil.isNotBlank(tenant.getAddress())) {
                    return tenant.getAddress();
                }
                if (StrUtil.isNotBlank(tenant.getCity())) {
                    return tenant.getCity();
                }
                if (StrUtil.isNotBlank(tenant.getCompanyName())) {
                    return tenant.getCompanyName();
                }
            }
        } catch (Exception e) {
            log.debug("[resolveLocation] 读取门店地址失败: {}", e.getMessage());
        }
        return "门店现场";
    }

    /** 价格文案：人均 + 参与人数 */
    private String resolvePrice(Map<String, Object> detail, PromotionActivityVo activity) {
        Object perPerson = detail.get("perPersonPrice");
        BigDecimal price = null;
        if (perPerson instanceof BigDecimal) {
            price = (BigDecimal) perPerson;
        } else if (perPerson != null) {
            try {
                price = new BigDecimal(String.valueOf(perPerson));
            } catch (NumberFormatException ignored) {
            }
        }
        if (price == null) {
            BigDecimal total = activity.getCustomPrice() != null ? activity.getCustomPrice() : activity.getAutoCalculatedPrice();
            price = total;
        }
        if (price == null) {
            return "详询门店";
        }
        String priceStr = "¥" + price.stripTrailingZeros().toPlainString() + "/人";
        Integer count = activity.getParticipantCount();
        if (count != null && count > 0) {
            priceStr += "（共 " + count + " 人）";
        }
        return priceStr;
    }

    private String resolveUserName(Long userId) {
        if (userId == null) {
            return "-";
        }
        SysUser user = userMapper.selectById(userId);
        return user != null && StrUtil.isNotBlank(user.getNickName()) ? user.getNickName() : "-";
    }

    private String resolveStoreName(Long tenantId) {
        try {
            SysTenant tenant = tenantMapper.selectById(tenantId);
            if (tenant != null && StrUtil.isNotBlank(tenant.getCompanyName())) {
                return tenant.getCompanyName();
            }
        } catch (Exception ignored) {
        }
        return "门店";
    }

    private Font boldFont(int size) {
        return new Font(Font.SANS_SERIF, Font.BOLD, size);
    }

    private Font plainFont(int size) {
        return new Font(Font.SANS_SERIF, Font.PLAIN, size);
    }

    /** 按最大宽度折行（中英文混排简易处理） */
    private String wrapText(String text, FontMetrics fm, int maxWidth) {
        if (StrUtil.isBlank(text) || "null".equals(text)) {
            return "";
        }
        if (maxWidth <= 0) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (fm.stringWidth(line.toString() + c) > maxWidth && line.length() > 0) {
                sb.append(line).append('\n');
                line.setLength(0);
            }
            line.append(c);
        }
        if (line.length() > 0) {
            sb.append(line);
        }
        return sb.toString();
    }

    /** 绘制多行文本 */
    private void drawMultiline(Graphics2D g, String text, int x, int y, int lineGap) {
        int lineY = y;
        for (String line : text.split("\n")) {
            g.drawString(line, x, lineY);
            lineY += lineGap;
        }
    }
}
