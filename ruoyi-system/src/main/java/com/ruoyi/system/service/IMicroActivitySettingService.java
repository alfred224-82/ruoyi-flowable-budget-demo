package com.ruoyi.system.service;

import com.ruoyi.system.domain.bo.ActivitySettingBo;
import com.ruoyi.system.domain.vo.ActivitySettingVo;
import com.ruoyi.system.domain.vo.ServiceItemVo;

import java.util.List;
import java.util.Map;

/**
 * 活动设置服务接口（M19 T19.1~T19.5）
 * <p>老板 PC「活动设置」页面：项目/房间/员工选择 + 自动标题生成 + 背景图/语音。
 * <p>★背景图仅对当前活动生效，不跨活动复用（对齐需求草稿 §3.6）。
 *
 * @author ruoyi
 * @date 2026-09-21
 */
public interface IMicroActivitySettingService {

    /**
     * T19.2 项目选择器数据源：本租户 service_item 排除 space 分类（单选）
     *
     * @return 上架状态的项目列表（id + name）
     */
    List<Map<String, Object>> listProjects();

    /**
     * T19.2 房间选择器数据源：本租户 service_item 仅 space 分类（单/多选）
     *
     * @return 上架状态的房间列表（id + name）
     */
    List<Map<String, Object>> listRooms();

    /**
     * T19.2 员工选择器数据源：本租户 salesman 角色的用户（单/多选）
     *
     * @return 启用的 salesman 用户列表（userId + nickName）
     */
    List<Map<String, Object>> listStaff();

    /**
     * T19.1 查询活动设置（回显已保存的选择 + 解析名称）
     *
     * @param activityId 活动ID
     * @return 活动设置视图
     */
    ActivitySettingVo getSetting(Long activityId);

    /**
     * T19.1 保存活动设置（项目/房间/员工选择 + 自动标题 + 背景图/语音）
     *
     * @param bo 活动设置保存对象
     * @return 保存后的活动设置视图
     */
    ActivitySettingVo saveSetting(ActivitySettingBo bo);

    /**
     * T19.3 自动生成活动标题
     * <p>规则：{房间名}玩{项目名}，由{员工名}主持
     * <p>多选超 3 个时「首个+等N个」
     *
     * @param projectId    选中的项目ID
     * @param roomIdList   选中的房间ID列表
     * @param staffIdList  选中的员工ID列表
     * @return 自动生成的标题
     */
    String generateAutoTitle(Long projectId, List<Long> roomIdList, List<Long> staffIdList);

}
