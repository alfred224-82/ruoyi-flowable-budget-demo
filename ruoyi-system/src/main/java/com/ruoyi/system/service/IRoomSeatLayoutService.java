package com.ruoyi.system.service;

import com.ruoyi.system.domain.bo.CopyLayoutBo;
import com.ruoyi.system.domain.bo.RoomSeatLayoutBo;
import com.ruoyi.system.domain.bo.RoomSeatLayoutTemplateBo;
import com.ruoyi.system.domain.bo.SaveAsTemplateBo;
import com.ruoyi.system.domain.vo.RoomSeatLayoutTemplateVo;
import com.ruoyi.system.domain.vo.RoomSeatLayoutVo;

import java.util.List;

/**
 * 房间座位布局服务接口（T26.1 / T26.4 / T26.5 / T26.6，小微版 M26）
 * <p>覆盖：布局 CRUD、拖拽定位保存、复制布局、存为模板、应用模板、平台默认模板 CRUD。
 * <p>★坐标以背景图百分比 0~1 存储，前端按容器实际尺寸换算像素，保证 PC / 手机 / 平板三端一致。
 * <p>★复制/应用后布局与源脱钩（V3.5 §16.4）：源修改不影响目标。
 * <p>★联动 T26.7：座位图查询由 {@link ISeatAssignmentService#getSeatMap(Long)} 承载，本接口聚焦布局管理。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
public interface IRoomSeatLayoutService {

    // ============ Boss 侧：房间布局管理（T26.1 / T26.4 / T26.5） ============

    /**
     * 按房间ID查询布局（含座位点批量列表）
     *
     * @param roomId 房间ID（FK→service_item.id）
     * @return 布局 VO；若房间未配置布局返回 null（前端提示"请先配置座位布局"）
     */
    RoomSeatLayoutVo getLayoutByRoomId(Long roomId);

    /**
     * 保存布局（新增 / 编辑；一次事务写入 room_seat_layout + room_seat 全量替换）
     * <p>★UNIQUE(tenant_id, room_id) 兜底：同房间重复保存走 UPDATE + 座位全量删插。
     * <p>★同步刷新 service_item.has_seat_layout=1 + seat_capacity=座位数（T26.2 联动）。
     *
     * @param bo 布局保存业务对象（含座位点列表）
     * @return 布局ID
     */
    Long saveLayout(RoomSeatLayoutBo bo);

    /**
     * 复制布局：从源房间复制到目标房间（T26.5）
     * <p>★脱钩原则：目标房间拥有独立 room_seat_layout + room_seat 记录；源房间修改不影响目标。
     * <p>★目标房间若已有布局，先删后建（幂等）。
     *
     * @param bo 复制参数（sourceRoomId + targetRoomId）
     * @return 目标房间新布局ID
     */
    Long copyFromRoom(CopyLayoutBo bo);

    /**
     * 存为模板：将当前布局存为本租户私有模板（T26.5，is_industry_default=0）
     * <p>★模板仅记录布局主表 + 背景图，不含具体座位点（应用模板后由 Boss 自行拖拽落位）。
     * <p>★UNIQUE(tenant_id, name) 兜底：同租户下模板名唯一，重名抛 ServiceException。
     *
     * @param bo 存为模板参数（layoutId + templateName）
     * @return 模板ID
     */
    Long saveAsTemplate(SaveAsTemplateBo bo);

    /**
     * 应用模板到指定房间（T26.5 / T26.6）
     * <p>★应用后 room_seat_layout.template_id 记录模板ID（留痕），后续修改与模板脱钩。
     * <p>★应用后 bg_image_source=PRESET（若模板为平台默认）或 UPLOAD（若为 Boss 私有）。
     *
     * @param roomId     目标房间ID
     * @param templateId 模板ID
     * @return 新布局ID
     */
    Long applyTemplate(Long roomId, Long templateId);

    /**
     * 查询 Boss 可见的模板列表（本租户私有 + 平台行业默认，T26.5 / T26.6）
     *
     * @param industryCode 行业编码（可空；传入时按行业过滤平台默认模板）
     * @return 模板列表（含 industryName 回填）
     */
    List<RoomSeatLayoutTemplateVo> listVisibleTemplates(String industryCode);

    /**
     * 删除房间布局（连带删除座位点）
     *
     * @param layoutId 布局ID
     * @return 影响行数
     */
    int deleteLayout(Long layoutId);

    // ============ 平台 Admin 侧：行业默认模板 CRUD（T26.6） ============

    /**
     * 平台默认模板列表（tenant_id=1 且 is_industry_default=1）
     *
     * @param industryCode 行业编码过滤（可空）
     * @return 模板列表
     */
    List<RoomSeatLayoutTemplateVo> listPlatformTemplates(String industryCode);

    /**
     * 平台默认模板详情
     *
     * @param id 模板ID
     * @return 模板 VO
     */
    RoomSeatLayoutTemplateVo getPlatformTemplate(Long id);

    /**
     * 新增平台默认模板（tenant_id=1，is_industry_default=1）
     *
     * @param bo 模板业务对象（industryCode 必填）
     * @return 模板ID
     */
    Long savePlatformTemplate(RoomSeatLayoutTemplateBo bo);

    /**
     * 编辑平台默认模板
     *
     * @param bo 模板业务对象（id 必填）
     * @return 影响行数
     */
    int updatePlatformTemplate(RoomSeatLayoutTemplateBo bo);

    /**
     * 删除平台默认模板
     *
     * @param id 模板ID
     * @return 影响行数
     */
    int deletePlatformTemplate(Long id);
}
