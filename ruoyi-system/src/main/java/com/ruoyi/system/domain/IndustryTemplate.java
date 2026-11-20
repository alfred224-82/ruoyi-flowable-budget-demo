package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 行业模板对象 industry_template（T15.2/T15.10，v1.13 小微版专属）
 * <p>系统级共享表（无 tenant_id，纳入 {@code MybatisPlusConfig.IGNORE_TENANT_TABLES}）；含 del_flag，继承 {@link BaseEntity}。
 * <p>内置 board_game(桌游店)/market(买菜生鲜)/gym(健身房)/spa(汤泉美容) 四行业；
 * {@code preset_config} JSON 预置服务分类示例/结算规则示例/单位库示例/默认页面配置。
 * <p>★新增行业只需配行业模板（industry_template + industry_role_map + 预置数据），小程序零改动；企业版无需行业模板。
 * uk_industry_code 保证行业编码唯一。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("industry_template")
public class IndustryTemplate extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 行业编码（board_game/market/gym/spa...）
     */
    private String industryCode;

    /**
     * 行业名称（桌游店/买菜/健身房/汤泉）
     */
    private String industryName;

    /**
     * 行业图标
     */
    private String icon;

    /**
     * 预置数据 JSON（服务分类示例/结算规则示例/单位库示例/默认页面配置）
     */
    private String presetConfig;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 是否可语音（0否 1是，三级控制总开关：admin→platform_admin→boss，T1.7/T18.1）
     */
    private Integer voiceEnabled;

    /**
     * 行业级打卡开关（三级开关之平台行业级，T24.2 M24）
     */
    private Integer checkInEnabled;

    /**
     * 删除标志（0存在 2删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注
     */
    private String remark;
}
