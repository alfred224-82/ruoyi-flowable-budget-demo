package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.domain.SysUserRole;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.ComplaintRecord;
import com.ruoyi.system.domain.PromotionActivity;
import com.ruoyi.system.domain.bo.ComplaintBo;
import com.ruoyi.system.domain.bo.ComplaintReplyBo;
import com.ruoyi.system.domain.compliance.SensitiveWordCheckResult;
import com.ruoyi.system.domain.vo.ComplaintRecordVo;
import com.ruoyi.system.mapper.ComplaintRecordMapper;
import com.ruoyi.system.mapper.PromotionActivityMapper;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.mapper.SysUserRoleMapper;
import com.ruoyi.system.service.IMicroComplaintService;
import com.ruoyi.system.service.ISensitiveWordEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 投诉服务实现（T25.1，小微版 M25）
 * <p>★渠道一（BOSS）无额外门槛；渠道二（PLATFORM）必须拍照取证+文字≥20字。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MicroComplaintServiceImpl implements IMicroComplaintService {

    private final ComplaintRecordMapper complaintMapper;
    private final PromotionActivityMapper promotionActivityMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysUserMapper sysUserMapper;
    private final ISensitiveWordEngine sensitiveWordEngine;

    /** 渠道一：Boss 门店内部 */
    private static final String CHANNEL_BOSS = "BOSS";
    /** 渠道二：平台 Admin 违法违规 */
    private static final String CHANNEL_PLATFORM = "PLATFORM";

    /** 状态常量 */
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_RESOLVED = "RESOLVED";
    private static final String STATUS_CLOSED = "CLOSED";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitComplaint(ComplaintBo bo) {
        // ① 校验渠道
        if (!CHANNEL_BOSS.equals(bo.getComplaintChannel()) && !CHANNEL_PLATFORM.equals(bo.getComplaintChannel())) {
            throw new ServiceException("投诉渠道非法：" + bo.getComplaintChannel());
        }
        // ② 渠道二额外校验
        if (CHANNEL_PLATFORM.equals(bo.getComplaintChannel())) {
            // 文字≥20字
            if (StrUtil.isBlank(bo.getContentText()) || bo.getContentText().trim().length() < 20) {
                throw new ServiceException("渠道二投诉文字描述不得少于20字");
            }
            // 必须拍照取证
            if (StrUtil.isBlank(bo.getEvidencePhotos())) {
                throw new ServiceException("渠道二投诉必须拍照取证");
            }
            // 类别必填
            if (StrUtil.isBlank(bo.getCategory())) {
                throw new ServiceException("渠道二投诉类别不能为空");
            }
        }
        // ★T32.8 敏感词终检（§十二 客诉预防，二档：命中即拒收）
        //   FEAT_SENSITIVE_WORD 关闭时引擎直接 PASS；开启时按平台底库+租户自定义库合并检测
        if (StrUtil.isNotBlank(bo.getContentText())) {
            SensitiveWordCheckResult check = sensitiveWordEngine.check(
                bo.getContentText(), LoginHelper.getTenantId(), "text", "complaint", null);
            if (check != null && !check.isPass()) {
                String hitWords = (check.getHitWords() == null ? new ArrayList<SensitiveWordCheckResult.HitItem>() : check.getHitWords())
                    .stream().map(SensitiveWordCheckResult.HitItem::getWord)
                    .filter(StrUtil::isNotBlank).distinct().collect(Collectors.joining("、"));
                log.warn("[complaint] 敏感词拦截：channel={}, tenantId={}, hitWords={}",
                    bo.getComplaintChannel(), LoginHelper.getTenantId(), hitWords);
                throw new ServiceException("文案包含易引发客诉的用语：" + (StrUtil.isBlank(hitWords) ? "敏感词" : hitWords) + "，请修改后重试");
            }
        }
        // ③ 写入记录
        ComplaintRecord record = new ComplaintRecord();
        record.setActivityId(bo.getActivityId());
        record.setStaffId(bo.getStaffId());
        record.setStaffName(bo.getStaffName());
        record.setComplainantPhone(bo.getComplainantPhone() != null ? bo.getComplainantPhone() : LoginHelper.getUsername());
        record.setComplainantName(bo.getComplainantName() != null ? bo.getComplainantName() : LoginHelper.getNickName());
        record.setComplaintChannel(bo.getComplaintChannel());
        record.setCategory(bo.getCategory());
        record.setContentText(bo.getContentText());
        record.setPhotoUrls(bo.getPhotoUrls());
        record.setEvidencePhotos(bo.getEvidencePhotos());
        record.setStatus(STATUS_PENDING);
        record.setSubmitTime(new Date());
        complaintMapper.insert(record);
        log.info("[complaint] 投诉提交：channel={}, tenantId={}, id={}",
            bo.getComplaintChannel(), LoginHelper.getTenantId(), record.getId());
        return record.getId();
    }

    @Override
    public List<ComplaintRecordVo> myComplaints(String channel) {
        // 顾客小程序：按 create_by（框架自动填充的登录用户名）匹配本人投诉记录
        // ★不能用 complainantPhone 匹配 LoginHelper.getUsername()，因为 getUsername() 返回的是用户名而非手机号
        String username = LoginHelper.getUsername();
        LambdaQueryWrapper<ComplaintRecord> wrapper = Wrappers.<ComplaintRecord>lambdaQuery()
            .eq(ComplaintRecord::getCreateBy, username);
        if (StrUtil.isNotBlank(channel)) {
            wrapper.eq(ComplaintRecord::getComplaintChannel, channel);
        }
        wrapper.orderByDesc(ComplaintRecord::getSubmitTime);
        return toVoList(complaintMapper.selectList(wrapper));
    }

    @Override
    public List<ComplaintRecordVo> listForBoss(String channel, String status) {
        LambdaQueryWrapper<ComplaintRecord> wrapper = Wrappers.<ComplaintRecord>lambdaQuery();
        if (StrUtil.isNotBlank(channel)) {
            wrapper.eq(ComplaintRecord::getComplaintChannel, channel);
        }
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(ComplaintRecord::getStatus, status);
        }
        wrapper.orderByDesc(ComplaintRecord::getSubmitTime);
        return toVoList(complaintMapper.selectList(wrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replyComplaint(Long id, ComplaintReplyBo bo) {
        ComplaintRecord record = complaintMapper.selectById(id);
        if (record == null) {
            throw new ServiceException("投诉记录不存在");
        }
        if (STATUS_RESOLVED.equals(record.getStatus()) || STATUS_CLOSED.equals(record.getStatus())) {
            throw new ServiceException("该投诉已处理/已关闭，不可重复处理");
        }
        String newStatus = StrUtil.isNotBlank(bo.getStatus()) ? bo.getStatus() : STATUS_RESOLVED;
        if (!STATUS_RESOLVED.equals(newStatus) && !STATUS_CLOSED.equals(newStatus)) {
            throw new ServiceException("处理状态非法：" + newStatus);
        }
        ComplaintRecord update = new ComplaintRecord();
        update.setId(id);
        update.setHandlerReply(bo.getHandlerReply());
        update.setHandlerUserId(LoginHelper.getUserId());
        update.setHandlerName(LoginHelper.getNickName());
        update.setHandleTime(new Date());
        update.setStatus(newStatus);
        complaintMapper.updateById(update);
        log.info("[complaint] 投诉处理：id={}, status={}, handler={}", id, newStatus, LoginHelper.getNickName());
    }

    @Override
    public List<Map<String, Object>> listStaffForComplaint() {
        // ① 查 salesman 角色（全局共享 tenant_id=NULL + 租户克隆角色）
        LambdaQueryWrapper<SysRole> roleWrapper = Wrappers.<SysRole>lambdaQuery()
            .eq(SysRole::getRoleKey, "salesman")
            .eq(SysRole::getStatus, "0");
        if (!LoginHelper.isAdmin()) {
            Long tenantId = LoginHelper.getTenantId();
            if (tenantId != null) {
                roleWrapper.and(w -> w
                    .isNull(SysRole::getTenantId)
                    .or().eq(SysRole::getTenantId, tenantId));
            } else {
                roleWrapper.isNull(SysRole::getTenantId);
            }
        } else {
            roleWrapper.isNull(SysRole::getTenantId);
        }
        List<SysRole> salesmanRoles = sysRoleMapper.selectList(roleWrapper);
        if (CollUtil.isEmpty(salesmanRoles)) {
            return Collections.emptyList();
        }
        // ② 收集 salesman role_id
        Set<Long> salesmanRoleIds = salesmanRoles.stream()
            .map(SysRole::getRoleId)
            .collect(Collectors.toSet());
        // ③ 查 sys_user_role 找到拥有 salesman 角色的 user_id
        LambdaQueryWrapper<SysUserRole> urWrapper = Wrappers.<SysUserRole>lambdaQuery()
            .in(SysUserRole::getRoleId, salesmanRoleIds);
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(urWrapper);
        if (CollUtil.isEmpty(userRoles)) {
            return Collections.emptyList();
        }
        List<Long> userIds = userRoles.stream()
            .map(SysUserRole::getUserId)
            .distinct()
            .collect(Collectors.toList());
        //  查 sys_user 获取昵称，按 userId 去重（★不过滤空闲状态，投诉需要全部员工）
        List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
        return users.stream()
            .filter(u -> "0".equals(u.getStatus()))
            .collect(Collectors.collectingAndThen(
                Collectors.toMap(
                    SysUser::getUserId,
                    u -> {
                        Map<String, Object> map = new LinkedHashMap<>(2);
                        map.put("id", u.getUserId());
                        map.put("name", u.getNickName());
                        return map;
                    },
                    (existing, replacement) -> existing,
                    LinkedHashMap::new
                ),
                map -> new ArrayList<>(map.values())
            ));
    }

    // ============================================================
    // 私有工具方法
    // ============================================================

    private List<ComplaintRecordVo> toVoList(List<ComplaintRecord> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        // ★批量收集有 activityId 的记录，一次性查活动名称
        Set<Long> activityIds = records.stream()
            .map(ComplaintRecord::getActivityId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, String> activityTitleMap = Collections.emptyMap();
        if (!activityIds.isEmpty()) {
            List<PromotionActivity> activities = promotionActivityMapper.selectBatchIds(activityIds);
            activityTitleMap = activities.stream()
                .collect(Collectors.toMap(PromotionActivity::getId, PromotionActivity::getTitle, (a, b) -> a));
        }
        final Map<Long, String> titleMap = activityTitleMap;
        return records.stream().map(entity -> {
            ComplaintRecordVo vo = new ComplaintRecordVo();
            BeanUtils.copyProperties(entity, vo);
            if (entity.getActivityId() != null) {
                vo.setActivityTitle(titleMap.get(entity.getActivityId()));
            }
            return vo;
        }).collect(Collectors.toList());
    }
}
