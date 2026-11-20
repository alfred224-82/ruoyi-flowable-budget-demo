package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.SensitiveWordLibraryBo;
import com.ruoyi.system.domain.vo.SensitiveWordLibraryVo;

import java.util.List;

/**
 * 敏感词库服务接口（T18.1~T18.3，小微版 M18）
 * <p>Admin 管理平台级敏感词（tenant_id=NULL）；租户管理员管理自定义敏感词（tenant_id=当前租户）。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
public interface ISensitiveWordService {

    /**
     * 敏感词列表（分页）
     * <p>★Admin 查平台级（tenant_id IS NULL）；租户管理员查平台+自定义合并
     */
    TableDataInfo<SensitiveWordLibraryVo> queryWordPage(SensitiveWordLibraryBo bo, PageQuery pageQuery, boolean platformLevel);

    /**
     * 敏感词详情
     */
    SensitiveWordLibraryVo queryWordById(Long id);

    /**
     * 新增敏感词
     */
    Long insertWord(SensitiveWordLibraryBo bo, boolean platformLevel);

    /**
     * 修改敏感词
     */
    int updateWord(SensitiveWordLibraryBo bo);

    /**
     * 删除敏感词
     */
    int deleteWord(Long id);

    /**
     * ★批量导入敏感词
     */
    int importWords(List<String> words, String category, String level, boolean platformLevel);

    /**
     * 查询租户的有效敏感词列表（按策略合并平台底库+自定义）
     *
     * @param sensitiveWordOverride 敏感词策略：INHERIT/OVERRIDE/MERGE
     * @return 合并后的有效敏感词列表
     */
    List<SensitiveWordLibraryVo> getEffectiveWords(String sensitiveWordOverride);
}
