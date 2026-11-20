package com.ruoyi.system.service;

import java.util.Map;

/**
 * 海报生成服务接口（M31 T31.1/T31.2）
 * <p>Boss 上传海报底图 → 后端合成右下角二维码 + 全图水印 → 返回合成后海报 URL。
 * <p>二维码参数编码格式：{@code t={tenantId}&a={activityId}&u=visitor&p={phoneLast4}}。
 *
 * @author ruoyi
 * @date 2026-10-24
 */
public interface IPosterGenerationService {

    /**
     * 合成活动海报（底图 + 右下角二维码 + 全图水印）
     *
     * @param activityId  活动ID
     * @param bgImageUrl  底图 OSS URL
     * @return 合成后海报 OSS URL
     */
    String generateActivityPoster(Long activityId, String bgImageUrl);

    /**
     * 生成二维码图片（ZXing）
     *
     * @param content 二维码内容
     * @param width   宽度（px）
     * @param height  高度（px）
     * @return 二维码 PNG 字节数组
     */
    byte[] generateQrCode(String content, int width, int height);

    /**
     * 查询海报合成任务状态（异步模式预留）
     *
     * @param taskId 任务ID
     * @return {status: PENDING/COMPLETED/FAILED, url: 合成后 URL}
     */
    Map<String, Object> getTaskStatus(String taskId);
}
