package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.UserRatingBo;
import com.ruoyi.system.domain.vo.UserRatingVo;

import java.util.List;
import java.util.Map;

/**
 * 评价与点赞服务接口（T17.2/T17.3/T17.4，小微版 M17）
 *
 * @author ruoyi
 * @date 2026-09-16
 */
public interface IUserRatingService {

    /**
     * ★提交评价（T17.2：禁止自评 + UNIQUE 防重 + 图片校验）
     */
    Long submitRating(UserRatingBo bo);

    /**
     * 评价列表（老板 PC 管理，分页）
     */
    TableDataInfo<UserRatingVo> queryRatingPage(UserRatingBo bo, PageQuery pageQuery);

    /**
     * 评价详情
     */
    UserRatingVo queryRatingById(Long id);

    /**
     * ★老板回复评价（一条评价仅能回复一次）
     */
    int replyRating(Long id, String replyContent);

    /**
     * ★屏蔽不当评价（NORMAL→SHIELDED）
     */
    int shieldRating(Long id);

    /**
     * ★综合评分聚合（T17.3：按 targetUserId 跨场景聚合平均评分/评价总数/各星级分布）
     */
    Map<String, Object> ratingSummary(Long targetUserId);

    /**
     * ★综合评分排行（T17.4：按 targetUserId 聚合排行）
     */
    List<Map<String, Object>> ratingRanking(String scene, int topN);

    /**
     * ★点赞 toggle（T17.2：已点赞→取消 DELETE / 未点赞→新增 INSERT）
     */
    boolean toggleLike(Long targetUserId, String scene);

    /**
     * 查询某人点赞数
     */
    long countLikes(Long targetUserId, String scene);

    /**
     * 查询当前用户是否已点赞
     */
    boolean isLiked(Long targetUserId, String scene);
}
