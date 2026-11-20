package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 房间座位布局模板 room_seat_layout_template（T26.1，小微版 M26）
 * <p>两类模板：
 * <ul>
 *   <li>Boss 私有模板（is_industry_default=0）：由 Boss 存为模板（T26.5），本租户可见</li>
 *   <li>平台行业默认模板（is_industry_default=1，tenant_id=1）：由平台 Admin 配置（T26.6），
 *       全租户共享，Boss 新建房间时按 industry_code 匹配</li>
 * </ul>
 * <p>★默认背景图必须为透明底 PNG/SVG（简洁线条示意图，非真实照片，M27 T27.2）；
 *   切换主题时容器背景色 / 座位标记 / 按钮高亮跟随主题色，预置人员固定橙色。
 * <p>租户级表（含 tenant_id，平台默认模板 tenant_id=1），多租户插件自动隔离；
 *   ★Boss 查询模板列表时需通过 {@code @TenantIgnore} 显式声明例外，才能读到 tenant_id=1 的平台模板。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("room_seat_layout_template")
public class RoomSeatLayoutTemplate extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 模板名称 */
    private String name;

    /** 模板默认背景图 OSS URL */
    private String bgImageUrl;

    /** 是否行业默认模板：0=否（Boss 私有）1=是（平台配置） */
    private Integer isIndustryDefault;

    /** 行业编码（is_industry_default=1 时必填，FK→industry_template.industry_code） */
    private String industryCode;
}
