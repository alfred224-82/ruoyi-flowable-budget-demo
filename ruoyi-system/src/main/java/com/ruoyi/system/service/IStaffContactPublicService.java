package com.ruoyi.system.service;

import com.ruoyi.system.domain.StaffContactPublic;

import java.util.List;
import java.util.Map;

/**
 * 员工联系方式公开开关服务接口（T31.5，设计方案 §八之二）
 * <p>按员工单独设置 show_phone / show_wechat 开关，新入职默认全关。
 * <p>顾客端点头像弹名片时，按开关决定显示「打电话」/「加微信」入口。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
public interface IStaffContactPublicService {

    /**
     * 查询员工联系方式公开设置
     *
     * @param staffId 员工用户ID
     * @return 开关配置；不存在时返回默认全关对象（不返回 null）
     */
    StaffContactPublic getByStaffId(Long staffId);

    /**
     * 更新员工联系方式公开开关（不存在则新建）
     *
     * @param entity 开关配置（staffId 必传）
     * @return 影响行数
     */
    int saveOrUpdate(StaffContactPublic entity);

    /**
     * 判断员工手机号是否公开
     *
     * @param staffId 员工用户ID
     * @return true=公开
     */
    boolean isPhonePublic(Long staffId);

    /**
     * 判断员工微信号是否公开
     *
     * @param staffId 员工用户ID
     * @return true=公开
     */
    boolean isWechatPublic(Long staffId);

    /**
     * 列出本租户全部业务员及其联系方式公开开关状态（M34 T34.5 老板 PC 端配置页）。
     * <p>★默认全关：无记录的员工返回 showPhone=0 / showWechat=0。
     *
     * @return 每项含 userId / nickName / avatar / showPhone / showWechat / wechatQrcodeUrl
     */
    List<Map<String, Object>> listAllWithStaff();
}
