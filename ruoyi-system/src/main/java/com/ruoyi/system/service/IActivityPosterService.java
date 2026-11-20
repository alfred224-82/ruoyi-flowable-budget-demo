package com.ruoyi.system.service;

import java.util.Map;

/**
 * 活动推广海报服务接口（M37 T37.1~T37.4）
 * <p>核心能力：
 * <ul>
 *   <li>T37.1 员工端一人一码海报：二维码 URL 含隐形员工推广码（ref_u），扫码自动归因到该员工</li>
 *   <li>T37.2 老板门店码：二维码 URL 不带员工 ID（无 ref_u），扫码计入门店自然流量，不参与员工拉新业绩</li>
 *   <li>T37.3 实时剩余名额：海报显示 = 房间容量(quota) − 已确认人数(signedCount)</li>
 *   <li>T37.4 海报五要素模板 + 服务端 Canvas（Java2D）渲染 → 输出 PNG data URL</li>
 * </ul>
 * <p>★约束：三名员工各生成各的海报，图面一致仅二维码参数不同（ref_u 不同）。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
public interface IActivityPosterService {

    /**
     * 海报数据聚合（前端 html-to-image 渲染主路径 / 服务端渲染数据源）。
     * <p>五要素：活动主视觉 + 标题/时间/地点/价格 + 实时剩余名额 + 二维码 + 行动指令 + 隐形推广码。
     *
     * @param activityId 活动ID
     * @param codeType   码类型：staff=员工一人一码（含 ref_u 归因）/ store=老板门店码（不归因）
     * @return 海报渲染所需的全部字段（含 qrcodeUrl base64 data URL）
     */
    Map<String, Object> getPosterData(Long activityId, String codeType);

    /**
     * ★T37.4 服务端 Canvas（Java2D）渲染海报 → 输出 PNG base64 data URL。
     * <p>渲染失败（如服务器缺 CJK 字体）时返回 null，前端降级用 {@link #getPosterData} + html-to-image。
     *
     * @param activityId 活动ID
     * @param codeType   码类型：staff / store
     * @return data:image/png;base64,... 或 null（渲染失败）
     */
    String renderPoster(Long activityId, String codeType);

    /**
     * ★T37.3 实时剩余名额查询。
     *
     * @param activityId 活动ID
     * @return {quota 名额上限, signedCount 已确认人数, remaining 剩余名额}
     */
    Map<String, Object> getRemaining(Long activityId);
}
