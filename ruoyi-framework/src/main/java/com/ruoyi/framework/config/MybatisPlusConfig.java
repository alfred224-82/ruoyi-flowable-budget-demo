package com.ruoyi.framework.config;

import cn.hutool.core.net.NetUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.framework.handler.CreateAndUpdateMetaObjectHandler;
import com.ruoyi.framework.interceptor.PlusDataPermissionInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Arrays;
import java.util.List;

/**
 * mybatis-plus配置类(下方注释有插件介绍)
 *
 * @author Lion Li
 */
@EnableTransactionManagement(proxyTargetClass = true)
@Configuration
@MapperScan("${mybatis-plus.mapperPackage}")
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 多租户插件（必须在数据权限之前）
        interceptor.addInnerInterceptor(tenantLineInnerInterceptor());
        // 数据权限处理
        interceptor.addInnerInterceptor(dataPermissionInterceptor());
        // 分页插件
        interceptor.addInnerInterceptor(paginationInnerInterceptor());
        // 乐观锁插件
        interceptor.addInnerInterceptor(optimisticLockerInnerInterceptor());
        return interceptor;
    }

    /**
     * 多租户拦截器
     * 自动为业务表SQL追加 tenant_id 条件
     */
    public TenantLineInnerInterceptor tenantLineInnerInterceptor() {
        return new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                Long tenantId = LoginHelper.getTenantId();
                if (tenantId != null) {
                    return new LongValue(tenantId);
                }
                // 未登录或平台管理员时返回null，不追加租户条件
                return new NullValue();
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                // 平台管理员免租户过滤
                try {
                    if (LoginHelper.isAdmin()) {
                        return true;
                    }
                } catch (Exception ignored) {
                }
                // 未登录时不过滤
                Long tenantId = LoginHelper.getTenantId();
                if (tenantId == null) {
                    return true;
                }
                // 排除不需要租户隔离的系统表
                return IGNORE_TENANT_TABLES.contains(tableName);
            }
        });
    }

    /**
     * 不需要租户隔离的表（系统级/平台级）
     */
    private static final List<String> IGNORE_TENANT_TABLES = Arrays.asList(
        // 租户管理表
        "sys_tenant", "sys_tenant_version_log", "sys_tenant_config",
        // 系统基础表（sys_dept 已启用租户隔离，不在此列表中）
        "sys_user", "sys_role", "sys_menu", "sys_post",
        "sys_user_role", "sys_role_menu", "sys_role_dept", "sys_user_post",
        "sys_dict_type", "sys_dict_data", "sys_config", "sys_notice",
        "sys_oper_log", "sys_logininfor", "sys_oss", "sys_oss_config",
        "sys_message",
        // 代码生成
        "gen_table", "gen_table_column",
        // 工作流引擎表（Flowable自带TENANT_ID_字段）
        "act_evt_log", "act_ge_bytearray", "act_ge_property",
        "act_hi_actinst", "act_hi_attachment", "act_hi_comment",
        "act_hi_detail", "act_hi_entitylink", "act_hi_identitylink",
        "act_hi_procinst", "act_hi_taskinst", "act_hi_tsk_log", "act_hi_varinst",
        "act_id_bytearray", "act_id_group", "act_id_info", "act_id_membership",
        "act_id_priv", "act_id_priv_mapping", "act_id_property", "act_id_token", "act_id_user",
        "act_procdef_info", "act_re_deployment", "act_re_model", "act_re_procdef",
        "act_ru_actinst", "act_ru_deadletter_job", "act_ru_entitylink",
        "act_ru_event_subscr", "act_ru_execution", "act_ru_external_job",
        "act_ru_history_job", "act_ru_identitylink", "act_ru_job",
        "act_ru_suspended_job", "act_ru_task", "act_ru_timer_job", "act_ru_variable",
        "flw_channel_definition", "flw_ev_databasechangelog", "flw_ev_databasechangeloglock",
        "flw_event_definition", "flw_event_deployment", "flw_event_resource",
        "flw_ru_batch", "flw_ru_batch_part",
        // 工作流业务表
        "wf_category", "wf_copy", "wf_deploy_form", "wf_form",
        // 定时任务
        "xxl_job_group", "xxl_job_info", "xxl_job_lock", "xxl_job_log",
        "xxl_job_log_report", "xxl_job_logglue", "xxl_job_registry", "xxl_job_user",
        // 测试表
        "test_demo", "test_tree",
        // 资金管控：凭证导出模板（内置模板 tenant_id=0 需跨租户可见，服务层手工隔离）
        "fee_export_template",
        // 资金管控：信用分规则/打卡触发规则（内置默认模板 tenant_id=0，服务层手工隔离，T6.3/T6.2）
        "fee_credit_rule", "fee_clock_trigger_rule",
        // 资金管控：项目关联规则（内置三层模板 tenant_id=0，服务层手工隔离，T5.7/T6.7）
        "fee_project_link_rule",
        // 资金管控：费用大类主数据（admin 超管维护，系统级共享，不按 tenant_id 隔离，T1.5/T5.13 三层角色分工）
        "fee_expense_category",
        // 多业态平台底座：业态包/功能开关（系统级共享表，不含 tenant_id，T1.7/T10.2/T10.3）
        "biz_bundle", "feature_flag",
        // 多业态平台底座：业态文案映射 i18n（系统级共享表，不含 tenant_id，T10.5 v1.29）
        "biz_copy",
        // M12 主题预设（系统级共享 Design Token，不含 tenant_id，T12.2）
        "biz_theme_preset",
        // M14 计费套餐/协议版本（系统级共享，不含 tenant_id，T14.1/T14.4）
        "sys_billing_plan", "sys_agreement_version",
        // M8 引导态门户访问审计（系统级共享，跨租户无登录态，不含 tenant_id，T8.8）
        "portal_access_log",
        // M15 小微版：角色模板（系统级共享，version_type×role_code 配方，不含 tenant_id，T15.1/T15.7）
        "role_template",
        // M15 小微版：行业模板 / 行业角色映射（系统级共享，admin 维护，不含 tenant_id，T15.2/T15.10）
        "industry_template", "industry_role_map",
        // AI报表
        "ai_report_record",
        // M18 图片合规与全局敏感词：全局底库 tenant_id=0 需跨租户读取，服务层手工隔离（T18.1）
        "sensitive_word", "image_compliance_config"
    );

    /**
     * 数据权限拦截器
     */
    public PlusDataPermissionInterceptor dataPermissionInterceptor() {
        return new PlusDataPermissionInterceptor();
    }

    /**
     * 分页插件，自动识别数据库类型
     */
    public PaginationInnerInterceptor paginationInnerInterceptor() {
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
        // 设置最大单页限制数量，默认 500 条，-1 不受限制
        paginationInnerInterceptor.setMaxLimit(-1L);
        // 分页合理化
        paginationInnerInterceptor.setOverflow(true);
        return paginationInnerInterceptor;
    }

    /**
     * 乐观锁插件
     */
    public OptimisticLockerInnerInterceptor optimisticLockerInnerInterceptor() {
        return new OptimisticLockerInnerInterceptor();
    }

    /**
     * 元对象字段填充控制器
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new CreateAndUpdateMetaObjectHandler();
    }

    /**
     * 使用网卡信息绑定雪花生成器
     * 防止集群雪花ID重复
     */
    @Bean
    public IdentifierGenerator idGenerator() {
        return new DefaultIdentifierGenerator(NetUtil.getLocalhost());
    }

    /**
     * PaginationInnerInterceptor 分页插件，自动识别数据库类型
     * https://baomidou.com/pages/97710a/
     * OptimisticLockerInnerInterceptor 乐观锁插件
     * https://baomidou.com/pages/0d93c0/
     * MetaObjectHandler 元对象字段填充控制器
     * https://baomidou.com/pages/4c6bcf/
     * ISqlInjector sql注入器
     * https://baomidou.com/pages/42ea4a/
     * BlockAttackInnerInterceptor 如果是对全表的删除或更新操作，就会终止该操作
     * https://baomidou.com/pages/f9a237/
     * IllegalSQLInnerInterceptor sql性能规范插件(垃圾SQL拦截)
     * IdentifierGenerator 自定义主键策略
     * https://baomidou.com/pages/568eb2/
     * TenantLineInnerInterceptor 多租户插件
     * https://baomidou.com/pages/aef2f2/
     * DynamicTableNameInnerInterceptor 动态表名插件
     * https://baomidou.com/pages/2a45ff/
     */

}
