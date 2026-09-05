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
import com.ruoyi.system.domain.Project;
import com.ruoyi.system.domain.ProjectSignup;
import com.ruoyi.system.domain.bo.ProjectSignupBo;
import com.ruoyi.system.domain.vo.ProjectSignupVo;
import com.ruoyi.system.mapper.ProjectMapper;
import com.ruoyi.system.mapper.ProjectSignupMapper;
import com.ruoyi.system.service.IProjectSignupService;
import com.ruoyi.system.service.strategy.TeamContext;
import com.ruoyi.system.service.strategy.TeamResult;
import com.ruoyi.system.service.strategy.TeamStrategyDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目活动报名服务实现（T11.3 / T11.4）
 * <p>报名采集 + 满员候补 + 成团判定；成团规则委派 {@link TeamStrategyDispatcher} 按业态策略 Bean 分发，
 * 本类不出现 if(bizCode==...) 业态分支（策略模式，设计文档 §1.8.5）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectSignupServiceImpl implements IProjectSignupService {

    private final ProjectSignupMapper signupMapper;
    private final ProjectMapper projectMapper;
    private final TeamStrategyDispatcher teamStrategyDispatcher;
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
    public TeamResult signup(ProjectSignupBo bo) {
        Project project = projectMapper.selectById(bo.getProjectId());
        if (project == null) {
            throw new ServiceException("项目容器不存在");
        }
        if ("CLOSED".equals(project.getStatus()) || "CANCELLED".equals(project.getStatus())
            || "FINISHED".equals(project.getStatus())) {
            throw new ServiceException("容器已结束/关闭，不可报名");
        }
        Map<String, Object> extra = parseExtra(project.getExtraConfig());
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

        // 成团判定（候补不计入成团进度）
        TeamContext ctx = buildContext(project, extra, quota, waitlist ? accepted : accepted + people);
        TeamResult result = teamStrategyDispatcher.evaluate(project.getBizCode(), ctx);
        if (waitlist) {
            result.setMessage("名额已满，已进入候补；" + result.getMessage());
        }
        log.info("[Signup] 容器 {} 报名 {} 人，成团判定：{}", bo.getProjectId(), people, result.getMessage());
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
    public TeamResult teamStatus(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ServiceException("项目容器不存在");
        }
        Map<String, Object> extra = parseExtra(project.getExtraConfig());
        Integer quota = toInteger(extra.get("quota"));
        TeamContext ctx = buildContext(project, extra, quota, sumAcceptedPeople(projectId));
        return teamStrategyDispatcher.evaluate(project.getBizCode(), ctx);
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

    private TeamContext buildContext(Project project, Map<String, Object> extra, Integer quota, int totalPeople) {
        TeamContext ctx = new TeamContext();
        ctx.setProjectId(project.getId());
        ctx.setBizCode(project.getBizCode());
        ctx.setTotalPeople(totalPeople);
        ctx.setQuota(quota);
        ctx.setExtra(extra);
        return ctx;
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
}
