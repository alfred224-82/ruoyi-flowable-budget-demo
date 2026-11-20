package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.RoleTemplateBo;
import com.ruoyi.system.domain.vo.RoleTemplateVo;

import java.util.Collection;
import java.util.List;

/**
 * 角色模板服务接口（T15.7，admin 维护系统级共享配方）
 * <p>按 version_type × role_code 预置菜单 + 区块配方；运行时供菜单加载按角色 + 版本取配方。
 * ★系统级共享表（无 tenant_id、无 del_flag），仅 admin 可维护，平台管理员无权修改。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
public interface IRoleTemplateService {

    /**
     * 分页查询角色模板（支持按版本类型/角色编码/状态过滤）
     */
    TableDataInfo<RoleTemplateVo> queryPageList(RoleTemplateBo bo, PageQuery pageQuery);

    /**
     * 查询模板详情
     */
    RoleTemplateVo queryById(Long id);

    /**
     * 按版本类型列出全部配方（如 micro 版 boss/salesman/customer 三配方）
     */
    List<RoleTemplateVo> listByVersion(String versionType);

    /**
     * 运行时按「版本类型 + 标准角色」取唯一配方（菜单加载数据源）
     */
    RoleTemplateVo getByVersionAndRole(String versionType, String roleCode);

    /**
     * 新建模板（version_type + role_code 唯一，重复抛异常）
     *
     * @return 模板ID
     */
    Long insert(RoleTemplateBo bo);

    /**
     * 编辑模板
     */
    int update(RoleTemplateBo bo);

    /**
     * 绑定菜单/区块（仅更新 menu_keys / block_keys）
     */
    int bindMenu(RoleTemplateBo bo);

    /**
     * 删除模板（可批量，物理删除——系统级表无 del_flag）
     */
    int deleteByIds(Collection<Long> ids);
}
