package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.UserLike;
import com.ruoyi.system.domain.UserRating;
import com.ruoyi.system.domain.bo.UserRatingBo;
import com.ruoyi.system.domain.compliance.ImageComplianceResult;
import com.ruoyi.system.domain.compliance.SensitiveWordCheckResult;
import com.ruoyi.system.domain.vo.UserRatingVo;
import com.ruoyi.system.mapper.UserLikeMapper;
import com.ruoyi.system.mapper.UserRatingMapper;
import com.ruoyi.system.service.IImageComplianceService;
import com.ruoyi.system.service.ISensitiveWordEngine;
import com.ruoyi.system.service.IUserRatingService;
import com.ruoyi.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 评价与点赞服务实现（T17.2/T17.3/T17.4，小微版 M17）
 * <p>★全部接口由 Controller 层 @RequireFeature("FEAT_RATING") 统一校验版本。
 * <p>★禁止自评：target_user_id ≠ author_user_id。
 * <p>★一条评价仅能回复一次（reply_content 非空即已回复）。
 *
 * @author ruoyi
 * @date 2026-09-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRatingServiceImpl implements IUserRatingService {

    private final UserRatingMapper ratingMapper;
    private final UserLikeMapper likeMapper;
    private final SysUserMapper userMapper;
    /** M18 统一敏感词引擎（T18.6 评价文本收口） */
    private final ISensitiveWordEngine sensitiveWordEngine;
    /** M18 图片合规服务（T18.6 评价图片收口） */
    private final IImageComplianceService imageComplianceService;

    /** 合法评价场景白名单 */
    private static final Set<String> VALID_SCENES = new HashSet<>(
        Arrays.asList("booking_complete", "service_done", "visit_done"));

    // ===== T17.2 评价提交 =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitRating(UserRatingBo bo) {
        // ★fail-fast：入口判空
        if (bo == null) {
            throw new ServiceException("评价参数不能为空");
        }
        if (bo.getTargetUserId() == null) {
            throw new ServiceException("被评价人不能为空");
        }
        Long authorUserId = LoginHelper.getUserId();
        if (authorUserId == null) {
            throw new ServiceException("当前用户未登录");
        }
        // ★禁止自评
        if (authorUserId.equals(bo.getTargetUserId())) {
            throw new ServiceException("不能评价自己");
        }
        // ★场景白名单校验
        if (!VALID_SCENES.contains(bo.getScene())) {
            throw new ServiceException("评价场景不合法：" + bo.getScene());
        }
        // ★UNIQUE 防重：同一顾客对同一服务人员同一场景同一订单只能评价一次
        LambdaQueryWrapper<UserRating> existWrapper = Wrappers.<UserRating>lambdaQuery()
            .eq(UserRating::getTargetUserId, bo.getTargetUserId())
            .eq(UserRating::getAuthorUserId, authorUserId)
            .eq(UserRating::getScene, bo.getScene());
        if (bo.getOrderId() != null) {
            existWrapper.eq(UserRating::getOrderId, bo.getOrderId());
        } else {
            existWrapper.isNull(UserRating::getOrderId);
        }
        Long existCount = ratingMapper.selectCount(existWrapper);
        if (existCount > 0) {
            throw new ServiceException("您已评价过，不可重复评价");
        }
        // ★图片数量校验（最多 9 张）
        if (StrUtil.isNotBlank(bo.getImages())) {
            // images 为 JSON 数组字符串，简单按逗号计数
            long imgCount = bo.getImages().split(",").length;
            if (imgCount > 9) {
                throw new ServiceException("评价图片最多9张");
            }
        }
        // ★M18 T18.6：评价内容合规收口（文本敏感词 + 图片两路），任一 FAIL 即拒绝
        checkRatingCompliance(bo, authorUserId);
        UserRating entity = new UserRating();
        entity.setTargetUserId(bo.getTargetUserId());
        entity.setAuthorUserId(authorUserId);
        entity.setRating(bo.getRating());
        entity.setContent(bo.getContent());
        entity.setImages(bo.getImages());
        entity.setScene(bo.getScene());
        entity.setOrderId(bo.getOrderId());
        entity.setStatus("NORMAL");
        ratingMapper.insert(entity);
        log.info("[submitRating] author={}, target={}, rating={}, scene={}",
            authorUserId, bo.getTargetUserId(), bo.getRating(), bo.getScene());
        return entity.getId();
    }

    /**
     * 评价内容合规收口（M18 T18.6）：文本敏感词 + 图片逐个检测，任一 FAIL 抛 ServiceException。
     * <p>开关关闭时引擎/图片服务内部直接放行，不阻断评价提交。
     */
    private void checkRatingCompliance(UserRatingBo bo, Long authorUserId) {
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            tenantId = 0L;
        }
        // 文本敏感词（inputChannel=text，bizType=rating）
        if (StrUtil.isNotBlank(bo.getContent())) {
            SensitiveWordCheckResult textResult =
                sensitiveWordEngine.check(bo.getContent(), tenantId, "text", "rating", null);
            if (textResult != null && !textResult.isPass()) {
                String hitWords = textResult.getHitWords().stream()
                    .map(SensitiveWordCheckResult.HitItem::getWord)
                    .collect(Collectors.joining("、"));
                log.warn("[submitRating] 评价文本命中敏感词 author={}, hits=[{}]", authorUserId, hitWords);
                throw new ServiceException("评价内容含违规信息：" + hitWords);
            }
        }
        // 图片逐个合规检测（bizType=rating）
        if (StrUtil.isNotBlank(bo.getImages())) {
            for (String rawUrl : bo.getImages().split(",")) {
                // 去除 JSON 数组的括号/引号/空白，得到纯 URL
                String url = rawUrl.replaceAll("[\\[\\]\"\\s]", "");
                if (StrUtil.isBlank(url)) {
                    continue;
                }
                ImageComplianceResult imgResult =
                    imageComplianceService.checkImageByUrl(url, tenantId, "rating", null);
                if (imgResult != null && !imgResult.isPass()) {
                    log.warn("[submitRating] 评价图片命中违规 author={}, reason={}", authorUserId, imgResult.getFailReason());
                    throw new ServiceException("评价图片含违规信息：" + imgResult.getFailReason());
                }
            }
        }
    }

    // ===== T17.4 老板后台评价管理 =====

    @Override
    public TableDataInfo<UserRatingVo> queryRatingPage(UserRatingBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<UserRating> wrapper = Wrappers.<UserRating>lambdaQuery()
            .eq(bo.getTargetUserId() != null, UserRating::getTargetUserId, bo.getTargetUserId())
            .eq(bo.getScene() != null && !bo.getScene().isEmpty(), UserRating::getScene, bo.getScene())
            .eq(StrUtil.isNotBlank(bo.getStatus()), UserRating::getStatus, bo.getStatus())
            .ge(bo.getRating() != null, UserRating::getRating, bo.getRating() != null ? bo.getRating() : null)
            .orderByDesc(UserRating::getCreateTime);
        Page<UserRatingVo> page = ratingMapper.selectVoPage(pageQuery.build(), wrapper);
        // 回填昵称
        for (UserRatingVo vo : page.getRecords()) {
            fillUserNames(vo);
        }
        return TableDataInfo.build(page);
    }

    @Override
    public UserRatingVo queryRatingById(Long id) {
        UserRatingVo vo = ratingMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("评价不存在");
        }
        fillUserNames(vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int replyRating(Long id, String replyContent) {
        // ★fail-fast：入口判空
        if (id == null) {
            throw new ServiceException("评价ID不能为空");
        }
        if (StrUtil.isBlank(replyContent)) {
            throw new ServiceException("回复内容不能为空");
        }
        UserRating exist = ratingMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("评价不存在");
        }
        if (StrUtil.isNotBlank(exist.getReplyContent())) {
            throw new ServiceException("该评价已回复，不可重复回复");
        }
        LambdaUpdateWrapper<UserRating> wrapper = Wrappers.<UserRating>lambdaUpdate()
            .eq(UserRating::getId, id)
            .set(UserRating::getReplyContent, replyContent)
            .set(UserRating::getReplyTime, new Date());
        return ratingMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int shieldRating(Long id) {
        // ★fail-fast：入口判空
        if (id == null) {
            throw new ServiceException("评价ID不能为空");
        }
        UserRating exist = ratingMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("评价不存在");
        }
        if ("SHIELDED".equals(exist.getStatus())) {
            return 0; // 已屏蔽，幂等
        }
        LambdaUpdateWrapper<UserRating> wrapper = Wrappers.<UserRating>lambdaUpdate()
            .eq(UserRating::getId, id)
            .set(UserRating::getStatus, "SHIELDED");
        return ratingMapper.update(null, wrapper);
    }

    // ===== T17.3 综合评分聚合 =====

    @Override
    public Map<String, Object> ratingSummary(Long targetUserId) {
        // ★fail-fast：入口判空
        if (targetUserId == null) {
            throw new ServiceException("被评价人ID不能为空");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        // 仅统计 NORMAL 状态评价（SHIELDED 不计入）
        LambdaQueryWrapper<UserRating> wrapper = Wrappers.<UserRating>lambdaQuery()
            .eq(UserRating::getTargetUserId, targetUserId)
            .eq(UserRating::getStatus, "NORMAL");
        List<UserRating> ratings = ratingMapper.selectList(wrapper);
        // ★DB 返回保护：selectList 不应返回 null，但 fail-fast 兜底
        if (ratings == null) {
            ratings = Collections.emptyList();
        }
        result.put("targetUserId", targetUserId);
        result.put("totalCount", ratings.size());
        if (ratings.isEmpty()) {
            result.put("avgRating", 0.0);
            result.put("starDistribution", emptyStarDist());
        } else {
            double avg = ratings.stream().mapToInt(UserRating::getRating).average().orElse(0.0);
            result.put("avgRating", Math.round(avg * 10.0) / 10.0);
            // 各星级分布
            Map<Integer, Long> dist = ratings.stream()
                .collect(Collectors.groupingBy(UserRating::getRating, Collectors.counting()));
            Map<String, Long> starDist = new LinkedHashMap<>();
            for (int i = 1; i <= 5; i++) {
                starDist.put(i + "star", dist.getOrDefault(i, 0L));
            }
            result.put("starDistribution", starDist);
        }
        // 点赞数
        long likeCount = likeMapper.selectCount(Wrappers.<UserLike>lambdaQuery()
            .eq(UserLike::getTargetUserId, targetUserId));
        result.put("likeCount", likeCount);
        return result;
    }

    @Override
    public List<Map<String, Object>> ratingRanking(String scene, int topN) {
        if (topN <= 0) {
            topN = 20;
        }
        LambdaQueryWrapper<UserRating> wrapper = Wrappers.<UserRating>lambdaQuery()
            .eq(UserRating::getStatus, "NORMAL")
            .eq(StrUtil.isNotBlank(scene), UserRating::getScene, scene);
        List<UserRating> allRatings = ratingMapper.selectList(wrapper);
        // ★DB 返回保护
        if (allRatings == null) {
            allRatings = Collections.emptyList();
        }
        // 按 targetUserId 分组聚合
        Map<Long, List<UserRating>> grouped = allRatings.stream()
            .collect(Collectors.groupingBy(UserRating::getTargetUserId));
        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Map.Entry<Long, List<UserRating>> entry : grouped.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("targetUserId", entry.getKey());
            SysUser user = userMapper.selectById(entry.getKey());
            row.put("targetUserName", user != null ? user.getNickName() : "-");
            List<UserRating> list = entry.getValue();
            double avg = list.stream().mapToInt(UserRating::getRating).average().orElse(0.0);
            row.put("avgRating", Math.round(avg * 10.0) / 10.0);
            row.put("totalCount", list.size());
            ranking.add(row);
        }
        // 按 avgRating 降序
        ranking.sort((a, b) -> Double.compare((double) b.get("avgRating"), (double) a.get("avgRating")));
        if (ranking.size() > topN) {
            ranking = ranking.subList(0, topN);
        }
        return ranking;
    }

    // ===== T17.2 点赞 toggle =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleLike(Long targetUserId, String scene) {
        // ★fail-fast：入口判空
        if (targetUserId == null) {
            throw new ServiceException("被点赞人ID不能为空");
        }
        if (StrUtil.isBlank(scene)) {
            throw new ServiceException("点赞场景不能为空");
        }
        Long authorUserId = LoginHelper.getUserId();
        if (authorUserId == null) {
            throw new ServiceException("当前用户未登录");
        }
        // ★禁止自赞
        if (authorUserId.equals(targetUserId)) {
            throw new ServiceException("不能给自己点赞");
        }
        LambdaQueryWrapper<UserLike> wrapper = Wrappers.<UserLike>lambdaQuery()
            .eq(UserLike::getTargetUserId, targetUserId)
            .eq(UserLike::getAuthorUserId, authorUserId)
            .eq(UserLike::getScene, scene);
        UserLike exist = likeMapper.selectOne(wrapper);
        if (exist != null) {
            // 已点赞 → 取消
            likeMapper.deleteById(exist.getId());
            log.info("[toggleLike] 取消点赞：author={}, target={}, scene={}", authorUserId, targetUserId, scene);
            return false; // false=已取消
        } else {
            // 未点赞 → 新增
            UserLike like = new UserLike();
            like.setTargetUserId(targetUserId);
            like.setAuthorUserId(authorUserId);
            like.setScene(scene);
            likeMapper.insert(like);
            log.info("[toggleLike] 新增点赞：author={}, target={}, scene={}", authorUserId, targetUserId, scene);
            return true; // true=已点赞
        }
    }

    @Override
    public long countLikes(Long targetUserId, String scene) {
        return likeMapper.selectCount(Wrappers.<UserLike>lambdaQuery()
            .eq(UserLike::getTargetUserId, targetUserId)
            .eq(StrUtil.isNotBlank(scene), UserLike::getScene, scene));
    }

    @Override
    public boolean isLiked(Long targetUserId, String scene) {
        Long authorUserId = LoginHelper.getUserId();
        Long count = likeMapper.selectCount(Wrappers.<UserLike>lambdaQuery()
            .eq(UserLike::getTargetUserId, targetUserId)
            .eq(UserLike::getAuthorUserId, authorUserId)
            .eq(UserLike::getScene, scene));
        return count > 0;
    }

    // ===== 私有方法 =====

    private void fillUserNames(UserRatingVo vo) {
        if (vo.getTargetUserId() != null) {
            SysUser target = userMapper.selectById(vo.getTargetUserId());
            if (target != null) {
                vo.setTargetUserName(target.getNickName());
            }
        }
        if (vo.getAuthorUserId() != null) {
            SysUser author = userMapper.selectById(vo.getAuthorUserId());
            if (author != null) {
                vo.setAuthorUserName(author.getNickName());
            }
        }
    }

    private Map<String, Long> emptyStarDist() {
        Map<String, Long> dist = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            dist.put(i + "star", 0L);
        }
        return dist;
    }
}
