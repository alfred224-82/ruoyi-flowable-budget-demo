package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.ImageComplianceConfigBo;
import com.ruoyi.system.domain.bo.SensitiveWordBo;
import com.ruoyi.system.domain.vo.ImageComplianceConfigVo;
import com.ruoyi.system.domain.vo.SensitiveWordVo;

import java.util.List;

/**
 * Admin 全局合规管理服务（M18 T18.3/T18.4，platform_admin 专用）
 * <p>全局敏感词库 CRUD + 批量导入 + 总开关；图片违规类型配置 CRUD + 总开关。
 * <p>所有写操作后调 {@link ISensitiveWordEngine#evictWordCache(Long)} 刷新缓存。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
public interface ISensitiveWordAdminService {

    /** 全局敏感词分页列表（tenant_id=0） */
    TableDataInfo<SensitiveWordVo> queryWordPage(SensitiveWordBo bo, PageQuery pageQuery);

    /** 全局敏感词详情 */
    SensitiveWordVo queryWordById(Long id);

    /** 新增全局敏感词 */
    Long insertWord(SensitiveWordBo bo);

    /** 修改全局敏感词 */
    int updateWord(SensitiveWordBo bo);

    /** 删除全局敏感词 */
    int deleteWord(Long id);

    /** 批量导入全局敏感词 */
    int importWords(List<String> words, String category, String matchMode);

    /** 敏感词总开关（启用/禁用全局底库） */
    int toggleSensitiveWord(Integer status);

    /** 图片违规类型配置列表（三种：porn/violence/politics） */
    List<ImageComplianceConfigVo> queryImageConfigs();

    /** 保存图片违规类型配置 */
    int saveImageConfig(ImageComplianceConfigBo bo);

    /** 图片合规总开关（按违规类型启用/禁用） */
    int toggleImageConfig(String violationType, Integer enabled);
}
