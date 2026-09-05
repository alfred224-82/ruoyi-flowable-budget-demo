# BUG-002 存量多租户迁移脚本非幂等 + ENTERPRISE 硬编码残留

**级别**：高（P1）
**发现时间**：2026-08-29
**影响范围**：数据库迁移（T1.4）、版本编码统一（T4.1）
**状态**：已提供修正方案

---

## 问题一：`docs/sql/phase18_multi_tenant.sql` 非幂等

**现象**：脚本为裸 `ALTER TABLE ... ADD COLUMN tenant_id`，重复执行报 `Duplicate column name 'tenant_id'`；且数据库快照（Dump20260723）中 36 张 `fee_*`/`budget_*` 表均无 `tenant_id`，无法确认线上库当前状态。

**修正**：已提供幂等版补齐脚本 `docs/newrequest/sql/M1_01_existing_tables_add_tenant_id.sql`（基于 `information_schema` 条件判断，可安全重复执行），本迭代以该脚本为准。

## 问题二：`ENTERPRISE` 硬编码残留（T4.1）

**现象**：设计要求版本编码统一为 `BASIC/ADVANCED/PROFESSIONAL`，当前代码存在 `ENTERPRISE` 硬编码：

| 位置 | 说明 |
|------|------|
| `EditionFeatureConfig` | `ENTERPRISE_FEATURES`/`EDITION_ENTERPRISE`/switch 分支 |
| `SysTenantServiceImpl` | edition switch 分支 |
| `SysLicense`/`SysTenant`/`SysTenantBo` | 注释与字段校验 |
| `LicenseGenerateTool` | 交互式生成工具 |
| `LicenseServiceTest` | 单元测试 |

**修正**：本迭代已将 `EditionFeatureConfig` 统一为 `PROFESSIONAL` 并保留 `ENTERPRISE → PROFESSIONAL` 兼容映射（带注释，符合任务文档"保留兼容映射并有注释"的验收口径）；存量数据迁移见 `M4_01_edition_enterprise_to_professional.sql`。其余引用点（生成工具/测试）建议后续迭代逐个替换，禁止新代码再写 `ENTERPRISE`。

## 问题三：租户到期拦截链路待确认（T3.4）

**现象**：`sys_tenant.status` 已定义 `EXPIRED` 状态，但登录拦截与全局接口拦截对 `EXPIRED` 的处理、到期每日检测 Job 此前不存在。

**修正**：本迭代已实现完整链路（T3.4）：
- `TenantStatusChecker`（ruoyi-system/service）：租户状态校验 + 被动到期检测兜底 + Redis 缓存；
- `SysLoginService.buildLoginUser`：登录时校验，到期/停用租户禁止登录；
- `SaTokenConfig` 拦截器链：已登录会话访问任意接口时校验（admin 豁免）；
- `TenantExpireCheckJob`（ruoyi-job，JobHandler=tenantExpireCheckJob，每日 00:30）：扫描到期租户转 `EXPIRED` 并踢出在线会话，注册脚本见 `M3_01_xxl_job_tenant_expire_check.sql`。
