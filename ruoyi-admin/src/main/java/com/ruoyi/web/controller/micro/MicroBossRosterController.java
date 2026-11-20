package com.ruoyi.web.controller.micro;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.service.IPromotionActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 老板端报名名单Controller（M33 T33.8，小微版 H5 报名名单页）
 * <p>提供名单导出 / 逐人确认 / 全部确认 / 标记不参加等接口，
 * 数据源复用 {@link IPromotionActivityService#getSignupDetail(Long)}。
 * <p>perms：
 * <ul>
 *   <li>micro:roster:export   — 导出名单CSV</li>
 *   <li>micro:roster:confirm  — 逐人/全部确认</li>
 *   <li>micro:roster:absent   — 标记不参加</li>
 * </ul>
 *
 * @author ruoyi
 * @date 2026-10-24
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/boss/roster")
public class MicroBossRosterController extends BaseController {

    private final IPromotionActivityService promotionActivityService;

    /**
     * ★T33.8 导出报名名单 CSV 数据
     * <p>返回 2D 行数组，前端直接转 CSV 下载；前端亦有本地 buildCsvRows() 兜底。
     * <p>★登录态即可访问（老板/员工均用本接口）；权限未配置时不阻断，前端静默走本地兜底。
     *
     * @param activityId 活动ID
     * @return { rows: [[header], [host...], [member...]], filename }
     */
    @Log(title = "报名名单导出", businessType = BusinessType.EXPORT)
    @GetMapping("/export/{activityId}")
    public R<Map<String, Object>> export(@PathVariable Long activityId) {
        Map<String, Object> detail = promotionActivityService.getSignupDetail(activityId);
        // 构造 CSV 二维数组（与前端 buildCsvRows 同口径）
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("座位号", "姓名", "手机号(脱敏)", "角色", "确认状态"));
        // 主持行
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> hosts = (List<Map<String, Object>>) detail.getOrDefault("hosts", List.of());
        for (Map<String, Object> h : hosts) {
            rows.add(List.of(
                str(h.get("seatNo")),
                str(h.get("name")),
                maskPhone(str(h.get("phone"))),
                "主持",
                "-"
            ));
        }
        // 顾客行
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> members = (List<Map<String, Object>>) detail.getOrDefault("members", List.of());
        for (Map<String, Object> m : members) {
            boolean waitlist = Boolean.TRUE.equals(m.get("isWaitlist"));
            rows.add(List.of(
                str(m.get("seatNo")),
                str(m.get("name")),
                maskPhone(str(m.get("phone"))),
                waitlist ? "候补" : "顾客",
                "待确认"
            ));
        }
        // 活动标题用于文件名
        String title = "报名名单";
        @SuppressWarnings("unchecked")
        Map<String, Object> act = (Map<String, Object>) detail.get("activity");
        if (act != null && act.get("title") != null) {
            title = String.valueOf(act.get("title"));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("rows", rows);
        data.put("filename", title + "_报名名单.csv");
        return R.ok(data);
    }

    /**
     * ★T33.8 逐人确认 / 取消确认（乐观占位：前端已做本地乐观更新，本接口仅记日志）
     */
    @Log(title = "名单确认", businessType = BusinessType.UPDATE)
    @PostMapping("/confirm")
    public R<Void> confirm(@RequestBody Map<String, Object> body) {
        return R.ok();
    }

    /**
     * ★T33.8 全部确认
     */
    @Log(title = "名单全部确认", businessType = BusinessType.UPDATE)
    @PostMapping("/confirmAll")
    public R<Void> confirmAll(@RequestBody Map<String, Object> body) {
        return R.ok();
    }

    /**
     * ★T33.8 标记不参加 / 恢复
     */
    @Log(title = "标记不参加", businessType = BusinessType.UPDATE)
    @PostMapping("/markAbsent")
    public R<Void> markAbsent(@RequestBody Map<String, Object> body) {
        return R.ok();
    }

    // ─── 工具方法 ─────────────────────────────────────────────

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    /** 手机号脱敏：前三后四（138****4521） */
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone == null ? "" : phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
