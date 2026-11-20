package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.ServiceCategoryBo;
import com.ruoyi.system.domain.vo.ServiceCategoryVo;

import java.util.Collection;
import java.util.List;

/**
 * 服务分类服务接口（T15.4/T15.11，小微版）
 * <p>功能：① 服务分类 CRUD；② 停用/启用；③ 启用分类下拉（供服务项目表单选择）。
 * 租户级表，多租户插件自动隔离。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
public interface IServiceCategoryService {

    /**
     * 分页查询服务分类（按名称/行业/状态过滤，sort_order 升序）
     */
    TableDataInfo<ServiceCategoryVo> queryPageList(ServiceCategoryBo bo, PageQuery pageQuery);

    /**
     * 查询服务分类详情
     */
    ServiceCategoryVo queryById(Long id);

    /**
     * 列出本租户全部启用分类（服务项目表单下拉）
     */
    List<ServiceCategoryVo> listEnabled();

    /**
     * ★V3.7 原子资源：按分类类型列出本租户启用分类（租户管理员项目管理/房间空间管理页下拉）
     *
     * @param categoryType service=服务分类 / space=房间·空间分类（空=全部）
     */
    List<ServiceCategoryVo> listEnabledByType(String categoryType);

    /**
     * ★V3.7 原子资源：确保本租户存在空间分类（房间/空间），无则自动创建「房间/空间」默认分类
     *
     * @return 空间分类ID
     */
    Long ensureSpaceCategory();

    /**
     * 新建服务分类
     *
     * @return 分类ID
     */
    Long insert(ServiceCategoryBo bo);

    /**
     * 编辑服务分类
     */
    int update(ServiceCategoryBo bo);

    /**
     * 停用/启用服务分类（status：0正常 1停用）
     */
    int toggle(Long id, String status);

    /**
     * 逻辑删除服务分类（可批量）
     */
    int deleteByIds(Collection<Long> ids);
}
