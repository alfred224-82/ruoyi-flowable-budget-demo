package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.SensitiveWordBo;
import com.ruoyi.system.domain.vo.SensitiveWordHitLogVo;
import com.ruoyi.system.domain.vo.SensitiveWordVo;

/**
 * 租户合规管理服务（M18 T18.6，租户管理员 + 老板只读）
 * <p>租户自定义词 CRUD（tenant_id=当前租户）+ 全局底库只读 + 命中记录只读。
 * <p>写操作后调 {@link ISensitiveWordEngine#evictWordCache(Long)} 刷新缓存。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
public interface ISensitiveWordTenantService {

    /** 租户自定义词分页列表（tenant_id=当前租户） */
    TableDataInfo<SensitiveWordVo> queryTenantWordPage(SensitiveWordBo bo, PageQuery pageQuery);

    /** 全局底库只读分页列表（tenant_id=0） */
    TableDataInfo<SensitiveWordVo> queryGlobalWordPage(SensitiveWordBo bo, PageQuery pageQuery);

    /** 租户自定义词详情 */
    SensitiveWordVo queryWordById(Long id);

    /** 新增租户自定义词 */
    Long insertWord(SensitiveWordBo bo);

    /** 修改租户自定义词 */
    int updateWord(SensitiveWordBo bo);

    /** 删除租户自定义词 */
    int deleteWord(Long id);

    /** 命中记录只读分页列表（当前租户） */
    TableDataInfo<SensitiveWordHitLogVo> queryHitLogPage(SensitiveWordHitLogVo vo, PageQuery pageQuery);
}
