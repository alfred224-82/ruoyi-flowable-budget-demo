package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.UserRatingBo;
import com.ruoyi.system.domain.vo.UserRatingVo;
import com.ruoyi.system.service.IUserRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 评价与点赞Controller（T17.2/T17.3/T17.4/T17.5，小微版 M17）
 * <p>评价提交/列表/详情/回复/屏蔽/综合评分聚合/排行 + 点赞 toggle。
 * <p>★全部接口 @RequireFeature("FEAT_RATING") 类级标注，非 micro_pro 租户返回「当前版本不支持」。
 * <p>perms：micro:rating:*（M17_01 种子菜单 7160~7164）。
 *
 * @author ruoyi
 * @date 2026-09-16
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/rating")
@RequireFeature("FEAT_RATING")
public class MicroRatingController extends BaseController {

    private final IUserRatingService ratingService;

    // ===== T17.2 评价提交 =====

    /**
     * ★提交评价（顾客对服务人员 1~5 星评价；禁止自评 + UNIQUE 防重）
     */
    @SaCheckPermission("micro:rating:list")
    @Log(title = "评价", businessType = BusinessType.INSERT)
    @PostMapping("/submit")
    public R<Long> submit(@Validated @RequestBody UserRatingBo bo) {
        return R.ok(ratingService.submitRating(bo));
    }

    // ===== T17.4 老板后台评价管理 =====

    /**
     * 评价列表（老板 PC 管理，分页 + 筛选）
     */
    @SaCheckPermission("micro:rating:list")
    @GetMapping("/list")
    public TableDataInfo<UserRatingVo> list(UserRatingBo bo, PageQuery pageQuery) {
        return ratingService.queryRatingPage(bo, pageQuery);
    }

    /**
     * 评价详情（含图片）
     */
    @SaCheckPermission("micro:rating:detail")
    @GetMapping("/{id}")
    public R<UserRatingVo> detail(@PathVariable Long id) {
        return R.ok(ratingService.queryRatingById(id));
    }

    /**
     * ★老板回复评价（一条评价仅能回复一次）
     */
    @SaCheckPermission("micro:rating:reply")
    @Log(title = "评价回复", businessType = BusinessType.UPDATE)
    @PutMapping("/reply")
    public R<Void> reply(@RequestParam Long id, @RequestParam String replyContent) {
        return toAjax(ratingService.replyRating(id, replyContent));
    }

    /**
     * ★屏蔽不当评价（NORMAL→SHIELDED，屏蔽后不计入综合评分）
     */
    @SaCheckPermission("micro:rating:shield")
    @Log(title = "评价屏蔽", businessType = BusinessType.UPDATE)
    @PutMapping("/shield/{id}")
    public R<Void> shield(@PathVariable Long id) {
        return toAjax(ratingService.shieldRating(id));
    }

    // ===== T17.3 综合评分聚合 =====

    /**
     * ★综合评分查询（按 targetUserId 跨场景聚合：平均评分 + 评价总数 + 各星级分布 + 点赞数）
     */
    @SaCheckPermission("micro:rating:list")
    @GetMapping("/summary/{userId}")
    public R<Map<String, Object>> summary(@PathVariable Long userId) {
        return R.ok(ratingService.ratingSummary(userId));
    }

    // ===== T17.4 综合评分排行 =====

    /**
     * ★综合评分排行（按 targetUserId 聚合排行，支持按 scene 筛选 + topN）
     */
    @SaCheckPermission("micro:rating:ranking")
    @GetMapping("/ranking")
    public R<List<Map<String, Object>>> ranking(
        @RequestParam(required = false) String scene,
        @RequestParam(defaultValue = "20") int topN) {
        return R.ok(ratingService.ratingRanking(scene, topN));
    }

    // ===== T17.2 点赞 toggle =====

    /**
     * ★点赞 toggle（已点赞→取消 DELETE / 未点赞→新增 INSERT）
     *
     * @return true=已点赞（新增） false=已取消（删除）
     */
    @SaCheckPermission("micro:rating:list")
    @Log(title = "点赞", businessType = BusinessType.UPDATE)
    @PostMapping("/like/toggle")
    public R<Boolean> likeToggle(@RequestParam Long targetUserId,
                                  @RequestParam(defaultValue = "service") String scene) {
        return R.ok(ratingService.toggleLike(targetUserId, scene));
    }

    /**
     * 查询某人点赞数
     */
    @SaCheckPermission("micro:rating:list")
    @GetMapping("/like/count")
    public R<Long> likeCount(@RequestParam Long targetUserId,
                              @RequestParam(required = false) String scene) {
        return R.ok(ratingService.countLikes(targetUserId, scene));
    }

    /**
     * 查询当前用户是否已点赞
     */
    @SaCheckPermission("micro:rating:list")
    @GetMapping("/like/isLiked")
    public R<Boolean> isLiked(@RequestParam Long targetUserId,
                               @RequestParam(defaultValue = "service") String scene) {
        return R.ok(ratingService.isLiked(targetUserId, scene));
    }
}
