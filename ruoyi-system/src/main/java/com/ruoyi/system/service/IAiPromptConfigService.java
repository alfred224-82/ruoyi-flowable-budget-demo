package com.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.system.domain.bo.FeeAiPromptConfigBo;
import com.ruoyi.system.domain.vo.FeeAiPromptConfigVo;

import java.util.List;

/**
 * AI引导话术配置Service接口
 *
 * @author ruoyi
 * @date 2026-07-23
 */
public interface IAiPromptConfigService {

    /**
     * 按场景+行业获取话术模板
     * 优先匹配行业专属配置，未找到则回退通用配置（industry_code='*'）
     *
     * @param sceneCode    场景编码
     * @param industryCode 行业编码
     * @return 话术配置，未配置返回null
     */
    FeeAiPromptConfigVo getPrompt(String sceneCode, String industryCode);

    /**
     * 查询话术配置列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    Page<FeeAiPromptConfigVo> selectPageList(FeeAiPromptConfigBo bo, PageQuery pageQuery);

    /**
     * 查询话术配置详情
     *
     * @param id 主键ID
     * @return 配置详情
     */
    FeeAiPromptConfigVo queryById(Long id);

    /**
     * 查询全部启用的话术配置
     *
     * @return 启用配置列表
     */
    List<FeeAiPromptConfigVo> listEnabled();

    /**
     * 新增话术配置
     *
     * @param bo 业务对象
     * @return 是否成功
     */
    Boolean insertByBo(FeeAiPromptConfigBo bo);

    /**
     * 修改话术配置
     *
     * @param bo 业务对象
     * @return 是否成功
     */
    Boolean updateByBo(FeeAiPromptConfigBo bo);

    /**
     * 删除话术配置
     *
     * @param ids 主键集合
     * @return 是否成功
     */
    Boolean deleteWithValidByIds(List<Long> ids);
}
