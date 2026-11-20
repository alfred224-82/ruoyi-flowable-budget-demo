package com.ruoyi.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.system.domain.ShareScene;
import com.ruoyi.system.domain.vo.ShareSceneVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 小程序场景/分享Mapper接口（T15.3/T15.19/T15.24）
 *
 * @author ruoyi
 * @date 2026-09-07
 */
public interface ShareSceneMapper extends BaseMapperPlus<ShareSceneMapper, ShareScene, ShareSceneVo> {

    /**
     * 跨租户按 sceneId 反查场景（T15.19 扫码/分享进入锁定租户）。
     * <p>扫码时尚未知晓租户，故 {@code @InterceptorIgnore(tenantLine = "true")} 跳过多租户过滤；scene_id 全局唯一（uk_scene）。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM share_scene WHERE scene_id = #{sceneId} LIMIT 1")
    ShareScene selectBySceneIdIgnoreTenant(@Param("sceneId") String sceneId);
}
