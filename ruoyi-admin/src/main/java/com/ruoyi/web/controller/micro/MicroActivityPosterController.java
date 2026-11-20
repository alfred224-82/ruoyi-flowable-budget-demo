package com.ruoyi.web.controller.micro;

import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.service.IActivityPosterService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 活动推广海报Controller（M37 T37.1~T37.4，小微版 H5 界面开发任务文档 v1.2）
 * <p>员工端一人一码海报（8.4）+ 老板门店码（7.10）共用本 Controller。
 * <p>码类型 codeType：
 * <ul>
 *   <li>staff — 员工一人一码，二维码含 ref_u，扫码自动归因到该员工（拉新业绩）</li>
 *   <li>store — 老板门店码，二维码不含 ref_u，扫码计入门店自然流量（不归因）</li>
 * </ul>
 * <p>登录即可访问（老板/员工均需生成各自海报），无额外 perms 校验。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/poster")
@RequireFeature("FEAT_PROMOTION_DISTRIBUTION")
public class MicroActivityPosterController extends BaseController {

    private final IActivityPosterService posterService;

    /**
     * ★T37.1/T37.2/T37.3 海报数据聚合（前端 html-to-image 渲染主路径）
     * <p>返回五要素：活动主视觉 + 标题/时间/地点/价格 + 实时剩余名额 + 二维码（含隐形推广码）+ 行动指令。
     *
     * @param activityId 活动ID
     * @param codeType   staff=员工一人一码 / store=老板门店码
     */
    @GetMapping("/data/{activityId}")
    public R<Map<String, Object>> posterData(@PathVariable Long activityId,
                                             @RequestParam(defaultValue = "staff") String codeType) {
        return R.ok(posterService.getPosterData(activityId, codeType));
    }

    /**
     * ★T37.4 服务端 Canvas（Java2D）渲染海报 → PNG base64 data URL。
     * <p>渲染失败（缺 CJK 字体等）返回 null，前端降级用 /data + html-to-image。
     *
     * @param activityId 活动ID
     * @param codeType   staff / store
     */
    @GetMapping("/render/{activityId}")
    public R<String> renderPoster(@PathVariable Long activityId,
                                  @RequestParam(defaultValue = "staff") String codeType) {
        return R.ok(posterService.renderPoster(activityId, codeType));
    }

    /**
     * ★T37.3 实时剩余名额（= 房间容量 − 已确认人数）
     * <p>海报轮询/进入页面时调用，群内喊话无法获得该信息，是顾客愿意扫码的唯一理由。
     *
     * @param activityId 活动ID
     */
    @GetMapping("/remaining/{activityId}")
    public R<Map<String, Object>> remaining(@PathVariable Long activityId) {
        return R.ok(posterService.getRemaining(activityId));
    }
}
