package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.ShareSceneBo;
import com.ruoyi.system.domain.vo.ShareSceneVo;

import java.util.Collection;

/**
 * 小程序场景/分享服务接口（T15.3 建表 / T15.19 扫码锁定租户 / T15.24 生成分享海报写入）
 * <p>scene_id 全局唯一（uk_scene）；扫码/分享进入即锁定租户，是多租户单小程序架构的入口锚点。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
public interface IShareSceneService {

    /**
     * T15.19 扫码/分享进入：跨租户按 sceneId 反查场景（含 tenantId + 业务信息），用于锁定租户；过期场景抛异常。
     */
    ShareSceneVo resolveBySceneId(String sceneId);

    /**
     * T15.24 生成分享海报写入：按 sceneId 幂等 upsert（本租户）；返回场景ID。
     */
    Long save(ShareSceneBo bo);

    /** 分页查询本租户场景（按 bizType/sceneId 过滤） */
    TableDataInfo<ShareSceneVo> queryPageList(ShareSceneBo bo, PageQuery pageQuery);

    /** 场景详情 */
    ShareSceneVo queryById(Long id);

    /** 批量物理删除（share_scene 无 del_flag） */
    int deleteByIds(Collection<Long> ids);
}
