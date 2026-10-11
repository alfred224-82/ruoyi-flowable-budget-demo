package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.ProjectSignup;
import com.ruoyi.system.domain.PromotionActivity;
import com.ruoyi.system.domain.ServiceItem;
import com.ruoyi.system.domain.bo.ProjectSignupBo;
import com.ruoyi.system.domain.vo.ProjectSignupVo;
import com.ruoyi.system.mapper.ProjectSignupMapper;
import com.ruoyi.system.mapper.PromotionActivityMapper;
import com.ruoyi.system.mapper.ServiceItemMapper;
import com.ruoyi.system.service.IProjectSignupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目活动报名服务实现（T11.3）
 * <p>报名采集 + 满员候补；成团判定已移除（M22 简化），前端根据 quota/remaining 控制按钮灰显。
 * <p>取消报名：活动开始时间前 1 小时可取消，之后不可取消。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectSignupServiceImpl implements IProjectSignupService {

    private final ProjectSignupMapper signupMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final PromotionActivityMapper activityMapper;
    private final ObjectMapper objectMapper;

    @Override
    public TableDataInfo<ProjectSignupVo> queryPageList(ProjectSignupBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ProjectSignup> wrapper = Wrappers.<ProjectSignup>lambdaQuery()
            .eq(bo.getProjectId() != null, ProjectSignup::getProjectId, bo.getProjectId())
            .eq(StrUtil.isNotBlank(bo.getSignInStatus()), ProjectSignup::getSignInStatus, bo.getSignInStatus())
            .orderByDesc(ProjectSignup::getSignupTime);
        Page<ProjectSignupVo> page = signupMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> signup(ProjectSignupBo bo) {
        // ★M22 锁定校验：关联活动已锁定则不可报名
        assertActivityNotLocked(bo.getProjectId());
        // ★M22 修复：selected_project_id 实际指向 service_item.id（Boss 活动管理页从 service_item 选择）
        ServiceItem item = serviceItemMapper.selectById(bo.getProjectId());
        if (item == null) {
            throw new ServiceException("服务项目不存在");
        }
        if ("1".equals(item.getStatus())) {
            throw new ServiceException("服务项目已下架，不可报名");
        }
        Map<String, Object> extra = parseExtra(item.getExtraConfig());
        Integer quota = toInteger(extra.get("quota"));
        int people = bo.getPeopleCount() == null || bo.getPeopleCount() < 1 ? 1 : bo.getPeopleCount();

        // 已接受（非候补）报名累计人数
        int accepted = sumAcceptedPeople(bo.getProjectId());
        boolean waitlist = quota != null && quota > 0 && accepted + people > quota;

        ProjectSignup signup = new ProjectSignup();
        signup.setProjectId(bo.getProjectId());
        signup.setUserId(bo.getUserId());
        signup.setOpenid(bo.getOpenid());
        signup.setSignupNo(genSignupNo(bo.getProjectId()));
        signup.setPeopleCount(people);
        signup.setQuota(quota);
        signup.setIsWaitlist(waitlist ? 1 : 0);
        signup.setSignInStatus("NOT_SIGNED");
        signup.setSignupTime(new Date());
        signupMapper.insert(signup);

        // 返回报名结果（前端据此切换按钮态：已报名/候补中）
        Map<String, Object> result = new LinkedHashMap<>(4);
        result.put("signupId", signup.getId());
        result.put("waitlist", waitlist);
        result.put("quota", quota);
        result.put("message", waitlist ? "名额已满，已进入候补" : "报名成功");
        log.info("[Signup] 服务项目 {} 报名 {} 人，候补={}", bo.getProjectId(), people, waitlist);
        return result;
    }

    @Override
    public int cancel(Long id) {
        ProjectSignup signup = signupMapper.selectById(id);
        if (signup == null) {
            throw new ServiceException("报名记录不存在");
        }
        return signupMapper.deleteById(id);
    }

    /**
     * 顾客端取消报名（活动开始前 1 小时可取消）
     *
     * @param signupId   报名记录ID
     * @param activityId 活动ID（用于获取活动开始时间）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelByActivityId(Long signupId, Long activityId) {
        ProjectSignup signup = signupMapper.selectById(signupId);
        if (signup == null) {
            throw new ServiceException("报名记录不存在");
        }
        // 校验报名归属（防止取消他人报名）
        Long currentUserId = com.ruoyi.common.helper.LoginHelper.getUserId();
        if (currentUserId != null && !currentUserId.equals(signup.getUserId())) {
            throw new ServiceException("只能取消自己的报名");
        }
        // 活动开始前 1 小时才可取消
        PromotionActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        // ★M22 锁定校验：活动已锁定不可取消
        if (Boolean.TRUE.equals(activity.getIsLocked())) {
            throw new ServiceException("活动已锁定，不可取消报名");
        }
        if (activity.getStartTime() != null) {
            long oneHourBefore = activity.getStartTime().getTime() - 3600_000L;
            if (System.currentTimeMillis() >= oneHourBefore) {
                throw new ServiceException("活动开始前1小时内不可取消报名");
            }
        }
        signupMapper.deleteById(signupId);
        log.info("[Cancel] 报名 {} 已取消（活动 {}）", signupId, activityId);
    }

    @Override
    public int checkin(Long id, String signInStatus) {
        ProjectSignup signup = signupMapper.selectById(id);
        if (signup == null) {
            throw new ServiceException("报名记录不存在");
        }
        String status = StrUtil.isBlank(signInStatus) ? "SIGNED" : signInStatus;
        if (!"SIGNED".equals(status) && !"ABSENT".equals(status) && !"NOT_SIGNED".equals(status)) {
            throw new ServiceException("非法签到状态");
        }
        ProjectSignup update = new ProjectSignup();
        update.setId(id);
        update.setSignInStatus(status);
        return signupMapper.updateById(update);
    }

    @Override
    public Map<String, Object> teamStatus(Long projectId) {
        ServiceItem item = serviceItemMapper.selectById(projectId);
        if (item == null) {
            throw new ServiceException("服务项目不存在");
        }
        Map<String, Object> extra = parseExtra(item.getExtraConfig());
        Integer quota = toInteger(extra.get("quota"));
        int accepted = sumAcceptedPeople(projectId);
        boolean formed = quota == null || quota <= 0 || accepted >= quota;
        Map<String, Object> result = new LinkedHashMap<>(4);
        result.put("formed", formed);
        result.put("current", accepted);
        result.put("target", quota != null ? quota : -1);
        result.put("message", formed ? "已成团" : "未成团，还差 " + (quota - accepted) + " 人");
        return result;
    }

    /** 累计已接受（非候补）报名人数 */
    private int sumAcceptedPeople(Long projectId) {
        List<ProjectSignup> list = signupMapper.selectList(
            Wrappers.<ProjectSignup>lambdaQuery()
                .eq(ProjectSignup::getProjectId, projectId)
                .eq(ProjectSignup::getIsWaitlist, 0));
        int sum = 0;
        for (ProjectSignup s : list) {
            sum += s.getPeopleCount() == null ? 1 : s.getPeopleCount();
        }
        return sum;
    }

    private Map<String, Object> parseExtra(String json) {
        if (StrUtil.isBlank(json)) {
            return new HashMap<>();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            return map != null ? map : new HashMap<>();
        } catch (Exception e) {
            log.warn("[Signup] extra_config JSON 解析失败: {}", json, e);
            return new HashMap<>();
        }
    }

    private Integer toInteger(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String genSignupNo(Long projectId) {
        return "SU" + projectId + System.currentTimeMillis();
    }

    /**
     * ★M22 关联活动锁定校验：查找引用该 projectId 的活动，若已锁定则拒绝报名。
     * <p>未找到关联活动时不阻断（可能为非活动类报名）。
     */
    private void assertActivityNotLocked(Long projectId) {
        if (projectId == null) {
            return;
        }
        List<PromotionActivity> activities = activityMapper.selectList(
            Wrappers.<PromotionActivity>lambdaQuery()
                .eq(PromotionActivity::getSelectedProjectId, projectId)
                .eq(PromotionActivity::getIsLocked, true)
                .last("LIMIT 1"));
        if (!activities.isEmpty()) {
            throw new ServiceException("活动已锁定，不可报名");
        }
    }
}
