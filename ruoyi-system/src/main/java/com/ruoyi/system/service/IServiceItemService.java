package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.ServiceItemBo;
import com.ruoyi.system.domain.vo.ServiceItemVo;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * 服务项目服务接口（T15.4/T15.11，小微版）
 * <p>功能：① 服务项目 CRUD（名称+价格+单位+计价方式）；② 停用/启用；③ 改价（自动写 price_change_log）。
 * <p>★改价留痕（T15.11）：{@link #update} 检测到 price 变更、或 {@link #changePrice}（老板小程序「改价确认」）
 * 均自动写入 price_change_log，记录 old/new 单价与操作人。租户级表，多租户插件自动隔离。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
public interface IServiceItemService {

    /**
     * 分页查询服务项目（按分类/名称/状态过滤，sort_order 升序；回填 categoryName）
     */
    TableDataInfo<ServiceItemVo> queryPageList(ServiceItemBo bo, PageQuery pageQuery);

    /**
     * ★V3.7 原子资源：按分类类型分页查询（租户管理员项目管理/房间空间管理页）
     * <p>spaceOnly=true → 仅空间分类（category_type='space'）下的项目（即房间/空间）；
     * spaceOnly=false → 排除空间分类（含未分类 category_id IS NULL），即普通项目。
     */
    TableDataInfo<ServiceItemVo> queryPageListByCategoryType(ServiceItemBo bo, PageQuery pageQuery, boolean spaceOnly);

    /**
     * 查询服务项目详情（回填 categoryName）
     */
    ServiceItemVo queryById(Long id);

    /**
     * 按分类列出上架服务项目（顾客小程序店铺页展示，status=0）
     *
     * @param categoryId 分类ID（可空=全部）
     */
    List<ServiceItemVo> listOnSale(Long categoryId);

    /**
     * 新建服务项目
     *
     * @return 项目ID
     */
    Long insert(ServiceItemBo bo);

    /**
     * 编辑服务项目（★price 变更时自动写 price_change_log）
     */
    int update(ServiceItemBo bo);

    /**
     * 改价确认（老板小程序 micro:service:editConfirm）：更新单价并写 price_change_log
     *
     * @param id       服务项目ID
     * @param newPrice 新单价
     * @param remark   改价说明（可空）
     */
    int changePrice(Long id, BigDecimal newPrice, String remark);

    /**
     * 上架/下架服务项目（status：0上架 1下架）
     */
    int toggle(Long id, String status);

    /**
     * 逻辑删除服务项目（可批量）
     */
    int deleteByIds(Collection<Long> ids);
}
