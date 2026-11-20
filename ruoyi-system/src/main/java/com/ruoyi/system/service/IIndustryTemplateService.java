package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.IndustryRoleMapBo;
import com.ruoyi.system.domain.bo.IndustryTemplateBo;
import com.ruoyi.system.domain.vo.IndustryRoleMapVo;
import com.ruoyi.system.domain.vo.IndustryTemplateVo;

import java.util.Collection;
import java.util.List;

/**
 * 行业模板服务接口（T15.2/T15.10，admin 维护系统级共享模板，小微版专属）
 * <p>功能：① 行业模板 CRUD；② 配置角色映射（industry_role_map：业务角色名→标准角色 code）；
 * ③ 预置数据（preset_config）随模板 CRUD 维护；④ 租户创建时按行业模板自动初始化 tenant_role_mapping。
 * <p>★新增行业只需配行业模板，小程序零改动；企业版无需行业模板。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
public interface IIndustryTemplateService {

    /**
     * 分页查询行业模板（支持按行业编码/名称/状态过滤，按 sort_order 升序）
     */
    TableDataInfo<IndustryTemplateVo> queryPageList(IndustryTemplateBo bo, PageQuery pageQuery);

    /**
     * 查询行业模板详情
     */
    IndustryTemplateVo queryById(Long id);

    /**
     * 列出全部启用行业（新建小微版租户时的行业选择下拉）
     */
    List<IndustryTemplateVo> listEnabled();

    /**
     * 新建行业模板（industry_code 唯一，重复抛异常）
     *
     * @return 模板ID
     */
    Long insert(IndustryTemplateBo bo);

    /**
     * 编辑行业模板（含 preset_config 预置数据；industry_code 身份字段不可改）
     */
    int update(IndustryTemplateBo bo);

    /**
     * 逻辑删除行业模板（可批量）
     */
    int deleteByIds(Collection<Long> ids);

    /**
     * ② 查询指定行业的角色映射列表（业务角色名→标准角色 code）
     */
    List<IndustryRoleMapVo> listRoleMaps(String industryCode);

    /**
     * ② 保存指定行业的角色映射（整表替换：先删该行业全部映射再批量插入，事务保证）
     *
     * @return 影响条数
     */
    int saveRoleMaps(String industryCode, List<IndustryRoleMapBo> roleMaps);

    /**
     * ④ 租户创建初始化：按行业模板的 industry_role_map 初始化目标租户的 tenant_role_mapping
     * （业务角色名作为 display_name，default_flag→status）。
     * <p>★须在 admin/系统上下文调用（跨租户写入，Service 层显式 setTenantId + 手动按 tenantId 过滤，幂等跳过已存在角色）。
     *
     * @return 新增映射条数
     */
    int initTenantByIndustry(Long tenantId, String industryCode);

    /**
     * ⑤ ★V3.7 原子资源（T15.38）：按行业模板 preset_config 初始化目标租户的项目/房间原子资源
     * （service_categories[]→service_category，projects[]→service_item，rooms[]→空间分类下 service_item）。
     * <p>幂等：目标租户 service_category / service_item 已有数据时分别跳过（V3.5 §16.4 脱钩原则，不覆盖存量）。
     * <p>★须在 admin/系统上下文或本租户上下文调用（跨租户写入，Service 层显式 setTenantId + 手动按 tenantId 过滤）。
     *
     * @return 新增条数（分类 + 项目 + 房间）
     */
    int initTenantResources(Long tenantId, String industryCode);

    /**
     * ⑤ ★V3.7 原子资源自愈（T15.38）：当前登录租户 service_item 全空时，按 sys_tenant.industry
     * 从 admin 行业模板预置数据（preset_config.projects/rooms）拉取落库。
     * <p>幂等仅空表补录；失败 fail-soft 不阻断列表查询；行业名→code 回退兼容自由输入。
     */
    void selfHealCurrentTenantResources();
}
