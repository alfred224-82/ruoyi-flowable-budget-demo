package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 页面区块配置对象 page_block_config（T15.3/T15.16/T15.17，模块化页面架构核心表）
 * <p>单一页面壳 + 区块组件库 + 后台配置：不存在 boss/home、salesman/home 多套页面，加角色只加配置不加分支。
 * <p>tenant_id=0 为「系统默认模板」，新建租户按行业模板克隆；module_type 区分区块来源(micro/enterprise)，企业版角色不误返回小微版区块。
 * <p>uk_tenant_role_page_block(tenant_id,role_code,page_key,block_key) 保证同租户同角色同页面同区块仅一条。
 * <p>表含完整审计字段(create_by/create_time/update_by/update_time)+tenant_id 但【无 del_flag】，故继承 TenantEntity 且不加 @TableLogic（物理删除）。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("page_block_config")
public class PageBlockConfig extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 标准角色编码(boss/salesman/customer)
     */
    private String roleCode;

    /**
     * 页面标识(home/mine/booking/report)
     */
    private String pageKey;

    /**
     * 区块 key(指向组件库：revenue_week/quick_booking...)
     */
    private String blockKey;

    /**
     * 区块来源：micro(小微版)/enterprise(企业版)
     */
    private String moduleType;

    /**
     * 区块排序
     */
    private Integer sortOrder;

    /**
     * 是否显示(0隐藏 1显示)
     */
    private Integer showFlag;

    /**
     * 区块自定义参数(JSON字符串，如 {"period":"week"}、{"role":"salesman"}；
     * 可选 {"perm":"micro:report:weekly"} 声明区块所需权限，供 /page/config 做「配置 ∩ 权限」二次过滤)
     */
    private String blockParams;
}
