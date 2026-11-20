package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.PromotionActivity;
import com.ruoyi.system.domain.ServiceCategory;
import com.ruoyi.system.domain.ServiceItem;
import com.ruoyi.system.domain.UserTenantRole;
import com.ruoyi.system.domain.bo.ActivitySettingBo;
import com.ruoyi.system.domain.vo.ActivitySettingVo;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.IMicroActivitySettingService;
import com.ruoyi.system.service.IIndustryTemplateService;
import com.ruoyi.system.service.IVoiceComplianceService;
import com.ruoyi.system.service.IActivityStaffSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 活动设置服务实现（M19 T19.1~T19.5）
 * <p>★背景图仅对当前活动生效，不跨活动复用。
 * <p>★语音为单向播放（约 3 分钟），不做语音互动。
 *
 * @author ruoyi
 * @date 2026-09-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MicroActivitySettingServiceImpl implements IMicroActivitySettingService {

    private final PromotionActivityMapper activityMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final ServiceCategoryMapper serviceCategoryMapper;
    private final SysUserMapper sysUserMapper;
    private final UserTenantRoleMapper userTenantRoleMapper;
    private final ObjectMapper objectMapper;
    private final IIndustryTemplateService industryTemplateService;
    /** M20 T18 增强：语音合规校验服务 */
    private final IVoiceComplianceService voiceComplianceService;

    /** M35 T35.3 活动员工分成快照服务（活动保存时按当前规则重建快照） */
    private final IActivityStaffSnapshotService activityStaffSnapshotService;

    /** space 分类类型标识（T15.38 原子资源：房间/空间） */
    private static final String CATEGORY_TYPE_SPACE = "space";

    /** salesman 标准角色编码 */
    private static final String ROLE_KEY_SALESMAN = "salesman";

    // ===== T19.2 选择器数据源 =====

    @Override
    public List<Map<String, Object>> listProjects() {
        // ★自愈：确保本租户项目/房间数据已从行业模板初始化
        industryTemplateService.selfHealCurrentTenantResources();
        // 查出本租户 space 分类的 category_id 集合
        Set<Long> spaceCategoryIds = getSpaceCategoryIds();
        // 查上架的 service_item，排除 space 分类
        LambdaQueryWrapper<ServiceItem> wrapper = Wrappers.<ServiceItem>lambdaQuery()
            .eq(ServiceItem::getStatus, "0")
            .orderByAsc(ServiceItem::getSortOrder)
            .orderByAsc(ServiceItem::getId);
        List<ServiceItem> items = serviceItemMapper.selectList(wrapper);
        // 排除 space 分类下的项目，并按名称去重（自愈多次触发可能产生同名记录）
        return items.stream()
            .filter(item -> !spaceCategoryIds.contains(item.getCategoryId()))
            .collect(Collectors.collectingAndThen(
                Collectors.toMap(
                    ServiceItem::getName,
                    item -> {
                        Map<String, Object> map = new LinkedHashMap<>(3);
                        map.put("id", item.getId());
                        map.put("name", item.getName());
                        map.put("categoryId", item.getCategoryId());
                        return map;
                    },
                    (existing, replacement) -> existing,
                    LinkedHashMap::new
                ),
                map -> new ArrayList<>(map.values())
            ));
    }

    @Override
    public List<Map<String, Object>> listRooms() {
        // ★自愈：确保本租户项目/房间数据已从行业模板初始化
        industryTemplateService.selfHealCurrentTenantResources();
        // 查出本租户 space 分类的 category_id 集合
        Set<Long> spaceCategoryIds = getSpaceCategoryIds();
        if (spaceCategoryIds.isEmpty()) {
            return Collections.emptyList();
        }
        // ★查上架的 service_item，仅 space 分类（不过滤 resourceStatus，已选房间可能非空闲）
        LambdaQueryWrapper<ServiceItem> wrapper = Wrappers.<ServiceItem>lambdaQuery()
            .in(ServiceItem::getCategoryId, spaceCategoryIds)
            .eq(ServiceItem::getStatus, "0")
            .orderByAsc(ServiceItem::getSortOrder)
            .orderByAsc(ServiceItem::getId);
        List<ServiceItem> items = serviceItemMapper.selectList(wrapper);
        // 按名称去重（自愈多次触发可能产生同名记录）
        return items.stream()
            .collect(Collectors.collectingAndThen(
                Collectors.toMap(
                    ServiceItem::getName,
                    item -> {
                        Map<String, Object> map = new LinkedHashMap<>(3);
                        map.put("id", item.getId());
                        map.put("name", item.getName());
                        map.put("categoryId", item.getCategoryId());
                        return map;
                    },
                    (existing, replacement) -> existing,
                    LinkedHashMap::new
                ),
                map -> new ArrayList<>(map.values())
            ));
    }

    @Override
    public List<Map<String, Object>> listStaff() {
        // ★从 user_tenant_role 查询本租户 salesman 角色用户（而非 sys_user_role，
        //   避免 sys_user_role 中存在未通过人员管理分配的脏数据导致下拉多出员工）
        List<UserTenantRole> utrList = userTenantRoleMapper.selectList(
            Wrappers.<UserTenantRole>lambdaQuery()
                .eq(UserTenantRole::getRoleCode, ROLE_KEY_SALESMAN)
                .eq(UserTenantRole::getStatus, "0"));
        if (CollUtil.isEmpty(utrList)) {
            return Collections.emptyList();
        }
        List<Long> staffIds = utrList.stream()
            .map(UserTenantRole::getUserId).distinct().collect(Collectors.toList());
        // 查 sys_user 获取昵称
        List<SysUser> users = sysUserMapper.selectBatchIds(staffIds);
        // ★不过滤空闲状态：已选员工可能非空闲（工作中/休息中），需在下拉中回显名称
        return users.stream()
            .filter(u -> "0".equals(u.getStatus()))
            .collect(Collectors.collectingAndThen(
                Collectors.toMap(
                    SysUser::getUserId,
                    u -> {
                        Map<String, Object> map = new LinkedHashMap<>(3);
                        map.put("id", u.getUserId());
                        map.put("name", u.getNickName());
                        map.put("userName", u.getUserName());
                        return map;
                    },
                    (existing, replacement) -> existing,
                    LinkedHashMap::new
                ),
                map -> new ArrayList<>(map.values())
            ));
    }


    // ===== T19.1 查询/保存活动设置 =====

    @Override
    public ActivitySettingVo getSetting(Long activityId) {
        if (activityId == null) {
            throw new ServiceException("活动ID不能为空");
        }
        PromotionActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        return buildSettingVo(activity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivitySettingVo saveSetting(ActivitySettingBo bo) {
        // ★fail-fast
        if (bo == null || bo.getActivityId() == null) {
            throw new ServiceException("活动ID不能为空");
        }
        PromotionActivity activity = activityMapper.selectById(bo.getActivityId());
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }

        // 更新选择的项目/房间/员工
        PromotionActivity update = new PromotionActivity();
        update.setId(bo.getActivityId());
        update.setSelectedProjectId(bo.getSelectedProjectId());

        // JSON 序列化列表
        if (CollUtil.isNotEmpty(bo.getSelectedRoomIdList())) {
            update.setSelectedRoomIds(toJson(bo.getSelectedRoomIdList()));
        } else {
            update.setSelectedRoomIds(null);
        }
        if (CollUtil.isNotEmpty(bo.getSelectedStaffIdList())) {
            update.setSelectedStaffIds(toJson(bo.getSelectedStaffIdList()));
        } else {
            update.setSelectedStaffIds(null);
        }

        // T19.3 自动生成标题（manualTitle 优先）
        if (StrUtil.isNotBlank(bo.getManualTitle())) {
            update.setTitle(bo.getManualTitle());
            update.setAutoTitle(bo.getManualTitle());
        } else {
            String autoTitle = generateAutoTitle(
                bo.getSelectedProjectId(),
                bo.getSelectedRoomIdList(),
                bo.getSelectedStaffIdList());
            update.setAutoTitle(autoTitle);
            // 如果活动还没有手动标题，用自动标题作为 title
            if (StrUtil.isBlank(activity.getTitle())) {
                update.setTitle(autoTitle);
            }
        }

        // T19.4 背景图（★仅对当前活动生效）
        update.setBgImageUrl(bo.getBgImageUrl());

        // M35 T35.4 其他成本（定价页算账用）
        update.setOtherCost(bo.getOtherCost());

        // 语音：保存前触发合规校验（M20 T18 增强）
        if (StrUtil.isNotBlank(bo.getVoiceUrl())) {
            Long tenantId = LoginHelper.getTenantId();
            if (tenantId == null) {
                throw new ServiceException("租户ID不能为空");
            }
            // 调用合规校验流程：ASR 转写 + 敏感词检测 + 写记录
            var checkResult = voiceComplianceService.checkVoiceCompliance(bo.getVoiceUrl(), tenantId, bo.getActivityId());
            // 如果违规，打回重录
            if (checkResult != null && !checkResult.isPass()) {
                log.warn("[saveSetting] tenantId={}, activityId={} 语音合规检测失败 → 拒绝保存", tenantId, bo.getActivityId());
                throw new ServiceException("语音内容包含敏感词，请重新录制");
            }
        }
        update.setVoiceUrl(bo.getVoiceUrl());

        activityMapper.updateById(update);

        // M35 T35.3 按当前选中员工集合重建分成快照（先删后插，幂等）
        // ★历史账不可变：员工规则变更后历史活动快照不受影响；
        //   事实变更（删除缺席员工）通过重新保存活动覆盖快照，报表按实际重算。
        try {
            activityStaffSnapshotService.rebuildSnapshots(bo.getActivityId(), bo.getSelectedStaffIdList());
        } catch (Exception e) {
            // 快照生成失败不阻断活动保存（可能尚无分成规则配置）
            log.warn("[M35] 活动分成快照重建失败 activityId={}", bo.getActivityId(), e);
        }

        // 重新查询并构建返回
        PromotionActivity updated = activityMapper.selectById(bo.getActivityId());
        return buildSettingVo(updated);
    }

    // ===== T19.3 自动标题生成 =====

    @Override
    public String generateAutoTitle(Long projectId, List<Long> roomIdList, List<Long> staffIdList) {
        log.info("generateAutoTitle 被调用：projectId={}, roomIdList={}, staffIdList={}", projectId, roomIdList, staffIdList);
        
        StringBuilder sb = new StringBuilder();

        // 房间部分
        String roomPart = resolveNames(roomIdList, true);
        log.info("房间部分解析结果：{}", roomPart);
        
        // 项目部分
        String projectPart = resolveProjectName(projectId);
        log.info("项目部分解析结果：{}", projectPart);
        
        // 员工部分
        String staffPart = resolveNames(staffIdList, false);
        log.info("员工部分解析结果：{}", staffPart);

        // 拼接规则：{房间名}玩{项目名}，由{员工名}主持
        // ★业务规则：项目必须选择，房间和员工可以不选
        if (StrUtil.isNotBlank(roomPart)) {
            sb.append(roomPart);
        }
        if (StrUtil.isNotBlank(projectPart)) {
            if (sb.length() > 0) {
                sb.append("玩");
            }
            sb.append(projectPart);
        }
        if (StrUtil.isNotBlank(staffPart)) {
            if (sb.length() > 0) {
                sb.append("，由");
            }
            sb.append(staffPart);
            sb.append("主持");
        }

        String result = sb.length() > 0 ? sb.toString() : null;
        log.info("generateAutoTitle 最终结果：{}", result);
        return result;
    }

    // ===== 内部辅助方法 =====

    /**
     * 获取本租户 space 分类的 category_id 集合
     */
    private Set<Long> getSpaceCategoryIds() {
        LambdaQueryWrapper<ServiceCategory> catWrapper = Wrappers.<ServiceCategory>lambdaQuery()
            .eq(ServiceCategory::getCategoryType, CATEGORY_TYPE_SPACE)
            .eq(ServiceCategory::getStatus, "0");
        List<ServiceCategory> spaceCategories = serviceCategoryMapper.selectList(catWrapper);
        return spaceCategories.stream()
            .map(ServiceCategory::getId)
            .collect(Collectors.toSet());
    }

    /**
     * 构建活动设置视图（含名称解析）
     */
    private ActivitySettingVo buildSettingVo(PromotionActivity activity) {
        ActivitySettingVo vo = new ActivitySettingVo();
        vo.setActivityId(activity.getId());
        vo.setTitle(activity.getTitle());
        vo.setAutoTitle(activity.getAutoTitle());
        vo.setBgImageUrl(activity.getBgImageUrl());
        vo.setVoiceUrl(activity.getVoiceUrl());
        vo.setQrcodeUrl(activity.getQrcodeUrl());
        vo.setSelectedProjectId(activity.getSelectedProjectId());

        // 解析 JSON 列表
        List<Long> roomIdList = parseJsonList(activity.getSelectedRoomIds());
        List<Long> staffIdList = parseJsonList(activity.getSelectedStaffIds());
        vo.setSelectedRoomIdList(roomIdList);
        vo.setSelectedStaffIdList(staffIdList);

        // 解析名称
        vo.setProjectName(resolveProjectName(activity.getSelectedProjectId()));
        vo.setSelectedRoomList(resolveIdNamePairs(roomIdList, true));
        vo.setSelectedStaffList(resolveIdNamePairs(staffIdList, false));

        // 标题预览：manualTitle(即 title，如果和 autoTitle 不同说明手动修改过) 优先
        vo.setTitlePreview(StrUtil.isNotBlank(activity.getAutoTitle()) ? activity.getAutoTitle() : activity.getTitle());

        // M35 T35.4 其他成本回显
        vo.setOtherCost(activity.getOtherCost());

        return vo;
    }

    /**
     * 解析 JSON 数组字符串为 Long 列表
     */
    private List<Long> parseJsonList(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (JsonProcessingException e) {
            log.warn("JSON 解析失败: {}", json, e);
            return Collections.emptyList();
        }
    }

    /**
     * Long 列表转 JSON 字符串
     */
    private String toJson(List<Long> list) {
        if (CollUtil.isEmpty(list)) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.warn("JSON 序列化失败: {}", list, e);
            return null;
        }
    }

    /**
     * 解析项目名称（单选）
     */
    private String resolveProjectName(Long projectId) {
        if (projectId == null) {
            log.warn("resolveProjectName: projectId 为 null");
            return null;
        }
        ServiceItem item = serviceItemMapper.selectById(projectId);
        if (item == null) {
            log.warn("resolveProjectName: 未找到 projectId={} 对应的项目记录", projectId);
        } else {
            log.info("resolveProjectName: projectId={} -> name={}", projectId, item.getName());
        }
        return item != null ? item.getName() : null;
    }

    /**
     * 解析 ID 列表为 [{id, name}] 对
     *
     * @param ids     ID 列表
     * @param isRoom  true=查 service_item（房间），false=查 sys_user（员工）
     */
    private List<Map<String, Object>> resolveIdNamePairs(List<Long> ids, boolean isRoom) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        if (isRoom) {
            // 查 service_item
            List<ServiceItem> items = serviceItemMapper.selectBatchIds(ids);
            Map<Long, String> nameMap = items.stream()
                .collect(Collectors.toMap(ServiceItem::getId, ServiceItem::getName, (a, b) -> a));
            for (Long id : ids) {
                Map<String, Object> map = new LinkedHashMap<>(2);
                map.put("id", id);
                map.put("name", nameMap.getOrDefault(id, "已删除"));
                result.add(map);
            }
        } else {
            // 查 sys_user
            for (Long id : ids) {
                SysUser user = sysUserMapper.selectUserById(id);
                Map<String, Object> map = new LinkedHashMap<>(2);
                map.put("id", id);
                map.put("name", user != null ? user.getNickName() : "已删除");
                result.add(map);
            }
        }
        return result;
    }

    /**
     * 解析名称列表（用于标题生成）
     * <p>多选超 3 个时「首个+等N个」
     *
     * @param ids    ID 列表
     * @param isRoom true=查 service_item，false=查 sys_user
     */
    private String resolveNames(List<Long> ids, boolean isRoom) {
        if (CollUtil.isEmpty(ids)) {
            return null;
        }
        List<String> names = new ArrayList<>();
        if (isRoom) {
            List<ServiceItem> items = serviceItemMapper.selectBatchIds(ids);
            Map<Long, String> nameMap = items.stream()
                .collect(Collectors.toMap(ServiceItem::getId, ServiceItem::getName, (a, b) -> a));
            for (Long id : ids) {
                String name = nameMap.get(id);
                if (name != null) {
                    names.add(name);
                }
            }
        } else {
            for (Long id : ids) {
                SysUser user = sysUserMapper.selectUserById(id);
                if (user != null) {
                    names.add(user.getNickName());
                }
            }
        }
        if (names.isEmpty()) {
            return null;
        }
        // 超 3 个时「首个+等N个」
        if (names.size() > 3) {
            return names.get(0) + "等" + names.size() + "个" + (isRoom ? "房间" : "员工");
        }
        return String.join("、", names);
    }
}
