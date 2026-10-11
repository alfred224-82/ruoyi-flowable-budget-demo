package com.ruoyi.common.constant;

import java.util.*;

/**
 * 版本功能开关定义
 * 定义三个版本（BASIC/ADVANCED/PROFESSIONAL）各自包含的功能列表（T4.1 版本编码统一）
 * 代码无分支：所有版本共用一套代码，差异仅通过 Feature 注解 + 前端 v-if 控制可见性（ADR 一套系统+版本分级）
 *
 * 开关新旧关系说明（T4.2 / v1.12 扩展至 33 项，设计文档 5.1 组 A~D）：
 * - FEE_* / BUDGET_* 为历史版本遗留开关，保留兼容；
 * - FEAT_* 为设计文档 5.1 定义的新开关，共 33 项：
 *   组 A（11 项，v1.12 由基础版下移至高级版）：FEAT_BUDGET_PREPARE 等资金管控重能力；
 *   组 B（9 项，高级版原有增量）：FEAT_TIME_CLOCK 等；
 *   组 C（6 项，多业态底座新增）：FEAT_FLOW_MULTI_LEVEL/FEAT_OWN_MINIAPP/FEAT_WECOM 高级版+，
 *     FEAT_FLOW_CUSTOM/FEAT_MULTI_ACCOUNT_SET/FEAT_OPEN_API 专业版；
 *   组 D（7 项，专业版原有增量）：FEAT_FACE_RECOGNITION 等。
 * - 本常量为【兜底】：feature_flag 表驱动优先（T10.3），表缺失时启用本常量并告警。
 *
 * 版本编码兼容（T10.7）：存量 edition 为大写全名（BASIC/ADVANCED/PROFESSIONAL/ENTERPRISE），
 * 需求文档使用小写短码（basic/advanced/pro）。统一由 {@link #normalizeEdition(String)} 归一化，
 * 双向兼容、不写迁移脚本、不改存量数据。
 *
 * @author ruoyi
 * @date 2026-07-24
 */
public class EditionFeatureConfig {

    private EditionFeatureConfig() {}

    /** 基础版功能列表 */
    public static final Set<String> BASIC_FEATURES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        // 基础版无额外高级功能，仅包含核心编审闭环（单级审批、固定5条风控、结果通知）
        // === M18 平台合规底线（全版本默认启用，T18.8） ===
        "FEAT_IMAGE_COMPLIANCE",     // 图片合规（OCR 文字路 + 视觉审核路两路并行）
        "FEAT_SENSITIVE_WORD"        // 全局敏感词引擎（text/voice_ocr/image_ocr 三路共用）
    )));

    /** 高级版功能列表（包含基础版全部 + 以下） */
    public static final Set<String> ADVANCED_FEATURES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        // === 费用模块（旧开关，兼容保留） ===
        "FEE_MULTI_CURRENCY",        // 多币种报销
        "FEE_COMPLIANCE_CUSTOM",     // 合规级别自定义配置
        "FEE_INDUSTRY_SINGLE",       // 单行业配置
        "FEE_TRACK_CONTROL",         // 三轨预算管控
        "FEE_PROFIT_CUSTOM_TIP",     // 盈利提示自定义
        "FEE_SUBSIDY_CONFIG",        // 补助标准配置
        "FEE_FUND_POOL",             // 资金池（部门级单层）
        "FEE_AI_CUSTOM_PROMPT",      // AI话术自定义
        "FEE_AI_CONFIDENCE_REVIEW",  // AI置信度转人工
        "FEE_WELFARE_CONFIG",        // 公司福利政策配置
        // === 预算模块 ===
        "BUDGET_BATCH_SUBMIT",       // 批量提交审核
        "BUDGET_BATCH_APPROVE",      // 批量审批
        "BUDGET_PERIOD_CONTROL",     // 编制期管控+提醒
        "BUDGET_MULTI_LEVEL_APPROVAL", // 可配置多级审批
        "BUDGET_DEADLINE_REMIND",    // 截止前提醒
        "BUDGET_RISK_WARNING",       // WARNING级风控
        "BUDGET_RISK_CUSTOM",        // 风控规则自定义
        "BUDGET_AI_CHAT",            // AI对话
        "BUDGET_AI_REPORT",          // AI报表生成
        // === 组 A：资金管控下移开关（11 项，v1.12 由基础版下移至高级版，T6.8） ===
        "FEAT_BUDGET_PREPARE",       // 预算编制闭环+预算调整+公司级预算（T5.14/T5.15）
        "FEAT_SUBJECT_3LAYER",       // 科目三层架构+费用大类主数据+财务科目导入（T5.13/T5.16）
        "FEAT_EXPENSE_CONTROL",      // 报销管控+大类硬拦截+三级预警+追溯时间线（T5.17）
        "FEAT_RULE_ENGINE_BASE",     // 规则引擎(内置关键字规则)（T5.18）
        "FEAT_INVOICE_VERIFY",       // 发票 OCR+验真+金额比对（T5.18）
        "FEAT_INVOICE_SUPPLEMENT",   // 无发票后补通道（T5.8）
        "FEAT_BAD_DEBT",             // 烂账标记+坏账计提（T5.6）
        "FEAT_LINK_RULE",            // 项目关联规则(内置三层)（T5.7）
        "FEAT_HIDDEN_COST",          // 隐形成本四项(薪资/折旧/分成/利息)（T5.9）
        "FEAT_BOSS_DASHBOARD",       // 老板看板+利润基础展示+固定成本卡片（T5.10）
        "FEAT_VOUCHER_EXPORT",       // 凭证导出(内置 7 套模板)（T5.11）
        // === 组 B：高级版原有增量（9 项，设计文档 5.1） ===
        "FEAT_TIME_CLOCK",           // 考勤打卡（打卡配置/记录：3219/3220）
        "FEAT_CREDIT_SCORE",         // 员工信用分（信用规则/信用分：3221/3222）
        "FEAT_DEPT_BUDGET",          // 部门预算编制（3217）
        "FEAT_ELASTIC_QUOTA",        // 弹性额度（无发票后补额度预占）
        "FEAT_RULE_ENGINE_CUSTOM",   // 规则引擎自定义（映射规则影响分析/发布：3296/3297）
        "FEAT_PROFIT_REPORT",        // 项目利润报表（3218）
        "FEAT_EXPORT_TEMPLATE_CUSTOM", // 自定义导出模板（3223）
        "FEAT_EVIDENCE_CHAIN",       // 凭证证据链（证据等级强/弱管控）
        "FEAT_LINK_RULE_CUSTOM",     // 项目关联规则自定义（3216 解锁大类强制/项目严格模式）
        // === 组 C：多业态底座新增（高级版+ 3 项，T13.2/T8.11/T13.4） ===
        "FEAT_FLOW_MULTI_LEVEL",     // 完整多级审批流(按金额/部门/职级条件)
        "FEAT_OWN_MINIAPP",          // 独立小程序(客户自有 AppID)
        "FEAT_WECOM",                // 企业微信自建应用+OAuth 免登
        // === M18 平台合规底线（全版本默认启用，T18.8） ===
        "FEAT_IMAGE_COMPLIANCE",     // 图片合规（OCR 文字路 + 视觉审核路两路并行）
        "FEAT_SENSITIVE_WORD"        // 全局敏感词引擎（text/voice_ocr/image_ocr 三路共用）
    )));

    /** 专业版功能列表（包含高级版全部 + 以下；原 ENTERPRISE_FEATURES，T4.1 统一编码） */
    public static final Set<String> PROFESSIONAL_FEATURES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        // === 费用模块（旧开关，兼容保留） ===
        "FEE_MULTI_CURRENCY",        // 多币种报销
        "FEE_COMPLIANCE_CUSTOM",     // 合规级别自定义配置
        "FEE_INDUSTRY_SINGLE",       // 单行业配置
        "FEE_INDUSTRY_PACK_IO",      // 行业配置包导入导出
        "FEE_TRACK_CONTROL",         // 三轨预算管控
        "FEE_ACCRUAL_ENGINE",        // 周期计提引擎（资产折旧+合同摊销）
        "FEE_LOAN_LEDGER",           // 借款台账+利息计提
        "FEE_PROFIT_CUSTOM_TIP",     // 盈利提示自定义
        "FEE_SUBSIDY_CONFIG",        // 补助标准配置
        "FEE_AI_CUSTOM_PROMPT",      // AI话术自定义
        "FEE_AI_CONFIDENCE_REVIEW",  // AI置信度转人工
        "FEE_AI_PRIVATE_DEPLOY",     // AI私有化部署
        "FEE_DESENSITIZE_CUSTOM",    // 数据脱敏自定义
        "FEE_CROSS_DEPT_ALLOCATE",   // 跨部门分摊
        "FEE_FUND_POOL",             // 资金池（三级）
        "FEE_AUDIT_FULL_EXPORT",     // 全节点审计导出
        "FEE_WELFARE_CONFIG",        // 公司福利政策配置
        // === 预算模块 ===
        "BUDGET_BATCH_SUBMIT",       // 批量提交审核
        "BUDGET_BATCH_APPROVE",      // 批量审批
        "BUDGET_PERIOD_CONTROL",     // 编制期管控+提醒
        "BUDGET_MULTI_LEVEL_APPROVAL", // 可配置多级审批
        "BUDGET_THREE_LEVEL_APPROVAL", // 固定三级审批
        "BUDGET_SEND_BACK",          // 打回至部门
        "BUDGET_DEADLINE_REMIND",    // 截止前提醒
        "BUDGET_TIMEOUT_ALERT",      // 超时告警
        "BUDGET_RISK_WARNING",       // WARNING级风控
        "BUDGET_RISK_CUSTOM",        // 风控规则自定义
        "BUDGET_RISK_FORMULA",       // 公式/建议性规则
        "BUDGET_RISK_AI",            // AI辅助风控
        "BUDGET_AI_CHAT",            // AI对话
        "BUDGET_AI_REPORT",          // AI报表生成（全类型）
        // === 组 A：资金管控下移开关（11 项，v1.12 高级版+，T6.8） ===
        "FEAT_BUDGET_PREPARE",       // 预算编制闭环+预算调整
        "FEAT_SUBJECT_3LAYER",       // 科目三层架构+费用大类主数据+财务科目导入
        "FEAT_EXPENSE_CONTROL",      // 报销管控+大类硬拦截+三级预警
        "FEAT_RULE_ENGINE_BASE",     // 规则引擎(内置关键字规则)
        "FEAT_INVOICE_VERIFY",       // 发票 OCR+验真+金额比对
        "FEAT_INVOICE_SUPPLEMENT",   // 无发票后补通道
        "FEAT_BAD_DEBT",             // 烂账标记+坏账计提
        "FEAT_LINK_RULE",            // 项目关联规则(内置三层)
        "FEAT_HIDDEN_COST",          // 隐形成本四项
        "FEAT_BOSS_DASHBOARD",       // 老板看板
        "FEAT_VOUCHER_EXPORT",       // 凭证导出(内置 7 套模板)
        // === 新功能开关：高级版 9 项全部继承（设计文档 5.1） ===
        "FEAT_TIME_CLOCK",           // 考勤打卡（打卡配置/记录：3219/3220）
        "FEAT_CREDIT_SCORE",         // 员工信用分（信用规则/信用分：3221/3222）
        "FEAT_DEPT_BUDGET",          // 部门预算编制（3217）
        "FEAT_ELASTIC_QUOTA",        // 弹性额度（无发票后补额度预占）
        "FEAT_RULE_ENGINE_CUSTOM",   // 规则引擎自定义（映射规则影响分析/发布：3296/3297）
        "FEAT_PROFIT_REPORT",        // 项目利润报表（3218）
        "FEAT_EXPORT_TEMPLATE_CUSTOM", // 自定义导出模板（3223）
        "FEAT_EVIDENCE_CHAIN",       // 凭证证据链（证据等级强/弱管控）
        "FEAT_LINK_RULE_CUSTOM",     // 项目关联规则自定义（3216 解锁大类强制/项目严格模式）
        // === 组 C：多业态底座新增（6 项，专业版全开，T13.2/T8.11/T7.7/T7.8） ===
        "FEAT_FLOW_MULTI_LEVEL",     // 完整多级审批流
        "FEAT_OWN_MINIAPP",          // 独立小程序(客户自有 AppID)
        "FEAT_WECOM",                // 企业微信自建应用+OAuth 免登
        "FEAT_FLOW_CUSTOM",          // 自定义流程设计器
        "FEAT_MULTI_ACCOUNT_SET",    // 多账套（T7.7）
        "FEAT_OPEN_API",             // 开放平台 API（T7.8）
        // === 新功能开关：专业版增量 7 项 ===
        "FEAT_FACE_RECOGNITION",     // 人脸核身（打卡/大额付款核身：3226）
        "FEAT_MULTI_ORG",            // 多组织管理（组织管理/组织预算编制：3224/3225）
        "FEAT_ORG_SNAPSHOT",         // 组织预算快照（3298）
        "FEAT_EXPORT_2D",            // 二维导出（科目×项目矩阵：3295）
        "FEAT_RULE_GRAYSCALE",       // 规则灰度发布（专业版发布模式）
        "FEAT_MEMBER_LOAD",          // 团队人力分布（看板跨项目负载卡片：3294）
        "FEAT_ISOLATED_SCHEMA",      // 隔离模式（预留：独立Schema级数据隔离）
        // === M18 平台合规底线（全版本默认启用，T18.8） ===
        "FEAT_IMAGE_COMPLIANCE",     // 图片合规（OCR 文字路 + 视觉审核路两路并行）
        "FEAT_SENSITIVE_WORD"        // 全局敏感词引擎（text/voice_ocr/image_ocr 三路共用）
    )));

    /**
     * @deprecated 旧版企业版别名，仅为兼容存量代码引用；新代码请使用 {@link #PROFESSIONAL_FEATURES}
     */
    @Deprecated
    public static final Set<String> ENTERPRISE_FEATURES = PROFESSIONAL_FEATURES;

    /**
     * 小微版功能列表（T3.18 v1.29 新增）
     * <p>micro_lite（免费推广档）与 micro_pro（收费经营档）两档共用本集合；
     * 具体档位区分由 {@code sys_tenant.version_type} 控制（前端菜单/接口通过 @RequireFeature + versionTypes 二次过滤）。
     * 与企业版分属两条产品线，不互相升级/降级；企业版能力（组 A~D）不注入小微版。
     */
    public static final Set<String> MICRO_FEATURES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        // === 小微版专属模块（micro_lite + micro_pro 均可用） ===
        "MICRO_PROMOTION_PUBLISH",       // 推广发布（活动+海报）
        "MICRO_SHOP_MANAGE",             // 店铺管理（营业时间/收款方式）
        "MICRO_ACTIVITY_BROWSE",         // 活动浏览页（lite 档唯一菜单）
        // === micro_pro 专属（后端由 @TenantJob versionTypes={"micro_pro"} + Controller @RequireFeature 二次过滤） ===
        "MICRO_BOOKING_ORDER",           // 预约订单
        "MICRO_STAFF_SETTLE",            // 人员结算（只记账不打款）
        "MICRO_DAILY_REVENUE",           // 收支记账+每日营收快照
        "MICRO_REPORT_CENTER",           // 报表中心（周报/月报/利润）
        "MICRO_SERVICE_PRICING",         // 服务定价（五种计价+改价留痕）
        "MICRO_NO_SHOW_BLACKLIST",       // 防爽约/黑名单
        "MICRO_CUSTOMER_CREDIT",         // 顾客管理（信誉/爽约/黑名单）
        // === 小微版专属开关（feature_flag 组 E，与企业版组 A~D 独立） ===
        "FEAT_PROMOTION_DISTRIBUTION",   // 推广分销与激励（推广码/归因/拉新奖励/首单）
        "FEAT_RATING",                   // 通用评价与点赞（scene=SERVICE）
        "FEAT_SELF_BOOKING",             // 自助预约（项目/房间/助教三类预约入口）
        "FEAT_STAFF_SCHEDULE",           // 员工排班管理（周/月+早晚班+Boss 自定时间+发布后可修正，M22/T22.6）
        // === M18 平台合规底线（全版本默认启用，T18.8） ===
        "FEAT_IMAGE_COMPLIANCE",         // 图片合规（OCR 文字路 + 视觉审核路两路并行）
        "FEAT_SENSITIVE_WORD"            // 全局敏感词引擎（text/voice_ocr/image_ocr 三路共用）
    )));

    /** 小微版版本等级常量（T3.18 v1.29 新增，独立产品线不参与企业版等级排序） */
    public static final String EDITION_MICRO = "MICRO";

    /**
     * 版本编码归一化唯一入口（T10.7 + T3.18 v1.29 扩展 MICRO）
     * <p>将存量大写全名与需求文档小写短码统一为标准编码，大小写不敏感：
     * <ul>
     *   <li>{@code pro} / {@code professional} / {@code ENTERPRISE} → {@code PROFESSIONAL}</li>
     *   <li>{@code adv} / {@code advanced} → {@code ADVANCED}</li>
     *   <li>{@code micro} / {@code micro_lite} / {@code micro_pro} → {@code MICRO}（T3.18 v1.29 新增）</li>
     *   <li>{@code base} / {@code basic} / null / 未知值 → {@code BASIC}</li>
     * </ul>
     * 双向兼容，不写迁移脚本、不改存量数据。新代码禁止直接写死版本编码字符串做比较，必须走本入口。
     *
     * @param edition 任意版本编码（大小写/长短码均可）
     * @return 归一化后的标准编码 BASIC/ADVANCED/PROFESSIONAL/MICRO
     */
    public static String normalizeEdition(String edition) {
        if (edition == null) {
            return EDITION_BASIC;
        }
        String v = edition.trim().toUpperCase(Locale.ROOT);
        switch (v) {
            case "PRO":
            case "PROFESSIONAL":
            case "ENTERPRISE":
                return EDITION_PROFESSIONAL;
            case "ADV":
            case "ADVANCED":
                return EDITION_ADVANCED;
            case "MICRO":
            case "MICRO_LITE":
            case "MICRO_PRO":
                return EDITION_MICRO;
            case "BASE":
            case "BASIC":
            default:
                return EDITION_BASIC;
        }
    }

    /**
     * 根据版本等级获取功能列表（入参先经 {@link #normalizeEdition(String)} 归一化）
     */
    public static Set<String> getFeaturesByEdition(String edition) {
        switch (normalizeEdition(edition)) {
            case EDITION_PROFESSIONAL:
                return PROFESSIONAL_FEATURES;
            case EDITION_ADVANCED:
                return ADVANCED_FEATURES;
            case EDITION_MICRO:
                return MICRO_FEATURES;
            case EDITION_BASIC:
            default:
                return BASIC_FEATURES;
        }
    }

    /**
     * 版本等级比较（归一化后）：PROFESSIONAL &gt; ADVANCED &gt; BASIC；MICRO 独立产品线不参与排序，返回 -1
     *
     * @return 等级序号（0=BASIC,1=ADVANCED,2=PROFESSIONAL,-1=MICRO）
     */
    public static int editionLevel(String edition) {
        switch (normalizeEdition(edition)) {
            case EDITION_PROFESSIONAL:
                return 2;
            case EDITION_ADVANCED:
                return 1;
            case EDITION_MICRO:
                return -1;
            case EDITION_BASIC:
            default:
                return 0;
        }
    }

    /**
     * 获取所有功能编码（用于License生成工具）
     */
    public static Set<String> getAllFeatures() {
        return PROFESSIONAL_FEATURES;
    }

    /**
     * 版本等级常量（T4.1：统一为 BASIC/ADVANCED/PROFESSIONAL）
     */
    public static final String EDITION_BASIC = "BASIC";
    public static final String EDITION_ADVANCED = "ADVANCED";
    public static final String EDITION_PROFESSIONAL = "PROFESSIONAL";
    /** @deprecated 旧版企业版编码，仅为兼容存量数据；新代码请使用 {@link #EDITION_PROFESSIONAL} */
    @Deprecated
    public static final String EDITION_ENTERPRISE = "ENTERPRISE";
}
