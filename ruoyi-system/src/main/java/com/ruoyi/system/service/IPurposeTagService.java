package com.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.system.domain.bo.PurposeTagBo;
import com.ruoyi.system.domain.vo.PurposeTagVo;

import java.util.Collection;
import java.util.List;

/**
 * 用途标签Service接口
 *
 * @author ruoyi
 * @date 2026-07-13
 */
public interface IPurposeTagService {

    /**
     * 查询用途标签详情
     *
     * @param id 主键ID
     * @return 用途标签VO
     */
    PurposeTagVo queryById(Long id);

    /**
     * 根据编码查询用途标签
     *
     * @param tagCode 标签编码
     * @return 用途标签VO
     */
    PurposeTagVo queryByCode(String tagCode);

    /**
     * 分页查询用途标签列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    Page<PurposeTagVo> selectPageList(PurposeTagBo bo, PageQuery pageQuery);

    /**
     * 查询全部启用标签（下拉用，走缓存）
     *
     * @return 标签列表
     */
    List<PurposeTagVo> queryAllEnabled();

    /**
     * 新增用途标签
     *
     * @param bo 业务对象
     * @return 影响行数
     */
    Boolean insertByBo(PurposeTagBo bo);

    /**
     * 修改用途标签
     *
     * @param bo 业务对象
     * @return 影响行数
     */
    Boolean updateByBo(PurposeTagBo bo);

    /**
     * 批量删除用途标签
     *
     * @param ids 主键集合
     * @return 影响行数
     */
    Boolean deleteWithValidByIds(Collection<Long> ids);
}
