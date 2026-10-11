package com.ruoyi.system.utils;

import com.ruoyi.common.constant.EditionFeatureConfig;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;

/**
 * License 签发工具（独立运行）
 * 用于生成 RSA 密钥对 和 签发带数字签名的 License 文件
 *
 * <p>使用方式：
 * <ol>
 *   <li>生成密钥对：{@code java -cp ... com.ruoyi.system.utils.LicenseGenerateTool keygen}
 *       → 输出 publicKey（配置到 application-prod.yml）和 privateKey（妥善保管）</li>
 *   <li>交互式签发：{@code java -cp ... LicenseGenerateTool sign}
 *       → 逐项 prompt，输出签名后的 License JSON</li>
 *   <li>命令行签发（T3.15 v1.28 新增，脚本化批量签发）：
 *       {@code java -cp ... LicenseGenerateTool sign --private-key=<base64> --edition=pro
 *       --biz-code=fund_control --plan=yearly --tenant-id=1001 --customer=某某公司}</li>
 * </ol>
 *
 * <p><b>T3.15 v1.28 缺口封闭</b>：
 * <ul>
 *   <li>新增 {@code --biz-code} 命令行参数（此前仅交互式 prompt）；</li>
 *   <li>新增 {@code --plan} 套餐类型入参（trial/first_month/monthly/yearly），自动映射默认天数；</li>
 *   <li>{@link #getDefaultFeatures(String)} 由硬编码列表改为真正引用 {@link EditionFeatureConfig}，
 *       switch 兼容 {@code pro/PROFESSIONAL/ENTERPRISE/adv/ADVANCED/base/BASIC/null} 全部别名（走 normalizeEdition）；</li>
 *   <li>套餐与到期天数联动：trial=7 天、first_month=30 天、monthly=30 天、yearly=365 天，
 *       显式 {@code --days=} 覆盖套餐默认。</li>
 * </ul>
 *
 * <p>安全说明：privateKey 绝不能放入应用代码或配置中；仅部署人员/授权管理员持有；应用只配置 publicKey 用于验签。
 *
 * @author ruoyi
 * @date 2026-07-25
 */
public class LicenseGenerateTool {

    /** 套餐类型 → 默认授权天数（T3.15） */
    private static final int PLAN_DAYS_TRIAL = 7;
    private static final int PLAN_DAYS_FIRST_MONTH = 30;
    private static final int PLAN_DAYS_MONTHLY = 30;
    private static final int PLAN_DAYS_YEARLY = 365;

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }
        String cmd = args[0].toLowerCase(Locale.ROOT);
        switch (cmd) {
            case "keygen":
                generateKeys();
                break;
            case "sign":
                signLicense(args);
                break;
            default:
                printUsage();
        }
    }

    /**
     * 生成 RSA-2048 密钥对
     */
    private static void generateKeys() {
        System.out.println("====== 生成RSA-2048密钥对 ======");
        String[] keys = LicenseSignUtil.generateKeyPair();
        System.out.println();
        System.out.println("【公钥 - 配置到 application-prod.yml 的 license.public-key】");
        System.out.println(keys[0]);
        System.out.println();
        System.out.println("【私钥 - 妥善保管，切勿泄露！用于签发License】");
        System.out.println(keys[1]);
        System.out.println();
        System.out.println("====== 完成 ======");
    }

    /**
     * 签发 License：支持交互式与命令行参数两种模式（T3.15）
     * <p>命令行参数（{@code --key=value} 或 {@code --key value}）：
     * <ul>
     *   <li>{@code --private-key} 私钥 Base64（脚本化必填）</li>
     *   <li>{@code --license-id} 授权编号（默认 LIC-{year}-000001）</li>
     *   <li>{@code --edition} 版本等级：BASIC/ADVANCED/PROFESSIONAL，兼容 pro/adv/base/ENTERPRISE 别名</li>
     *   <li>{@code --biz-code} 业态包编码（T3.15 新增）：fund_control/boardgame/fresh_food，留空=不限</li>
     *   <li>{@code --plan} 套餐类型（T3.15 新增）：trial/first_month/monthly/yearly，映射默认天数</li>
     *   <li>{@code --days} 授权天数，显式覆盖套餐默认，0 或空=永久</li>
     *   <li>{@code --license-type} TRIAL/FORMAL（默认 FORMAL；plan=trial 时自动 TRIAL）</li>
     *   <li>{@code --tenant-id} 绑定租户 ID（可选）</li>
     *   <li>{@code --customer} 客户名称</li>
     *   <li>{@code --features} 功能列表 JSON，留空=按版本从 EditionFeatureConfig 自动填充</li>
     *   <li>{@code --industries} 行业列表 JSON</li>
     *   <li>{@code --fingerprint} 机器指纹</li>
     *   <li>{@code --license-version} License 版本号（默认 1）</li>
     * </ul>
     * <p>检测到任何 {@code --} 参数即进入非交互模式；缺项走默认值。
     */
    private static void signLicense(String[] args) {
        CliOptions cli = CliOptions.parse(args);
        boolean nonInteractive = cli.hasAnyOption();

        Scanner scanner = nonInteractive ? null : new Scanner(System.in);
        try {
            String privateKey = nonInteractive
                ? cli.getOrDefault("private-key", "")
                : (args.length >= 2 && !args[1].startsWith("--")
                    ? args[1]
                    : promptLine(scanner, "请输入私钥(Base64)", ""));
            if (privateKey == null || privateKey.isEmpty()) {
                System.err.println("[错误] 私钥不能为空。命令行模式请传 --private-key=<base64>。");
                return;
            }

            System.out.println();
            System.out.println("====== 签发License ======");

            String defaultLicenseId = "LIC-" + new SimpleDateFormat("yyyy").format(new Date()) + "-000001";
            String licenseId = nonInteractive
                ? cli.getOrDefault("license-id", defaultLicenseId)
                : prompt(scanner, "授权编号 (如 LIC-2026-000001)", defaultLicenseId);

            String editionRaw = nonInteractive
                ? cli.getOrDefault("edition", "PROFESSIONAL")
                : prompt(scanner, "版本等级 (BASIC/ADVANCED/PROFESSIONAL，兼容 pro/adv/base/ENTERPRISE 别名)", "PROFESSIONAL");
            // T3.15：走 normalizeEdition 归一化，兼容 pro/adv/base/ENTERPRISE 全部别名（T10.7 一致）
            String edition = EditionFeatureConfig.normalizeEdition(editionRaw);

            String plan = nonInteractive
                ? cli.getOrDefault("plan", "")
                : prompt(scanner, "套餐类型 (trial/first_month/monthly/yearly，回车=不使用套餐)", "");
            int planDefaultDays = resolvePlanDays(plan);

            String licenseTypeDefault = "trial".equalsIgnoreCase(plan) ? "TRIAL" : "FORMAL";
            String licenseType = nonInteractive
                ? cli.getOrDefault("license-type", licenseTypeDefault)
                : prompt(scanner, "授权类型 (TRIAL/FORMAL)", licenseTypeDefault);

            String customer = nonInteractive
                ? cli.getOrDefault("customer", "默认客户")
                : prompt(scanner, "客户名称", "默认客户");

            String tenantIdStr = nonInteractive
                ? cli.getOrDefault("tenant-id", "")
                : prompt(scanner, "租户ID (回车=不绑定租户，系统级)", "");

            // T3.15：--biz-code 命令行参数（此前仅交互式 prompt）
            String bizCode = nonInteractive
                ? cli.getOrDefault("biz-code", "")
                : prompt(scanner, "业态包编码 (fund_control/boardgame/fresh_food，回车=不限)", "");

            String features = nonInteractive
                ? cli.getOrDefault("features", "")
                : prompt(scanner, "功能列表JSON (回车=按版本从 EditionFeatureConfig 自动填充)", "");

            String industries = nonInteractive
                ? cli.getOrDefault("industries", "")
                : prompt(scanner, "行业列表JSON (回车=不限)", "");

            String daysDefault = planDefaultDays > 0 ? String.valueOf(planDefaultDays) : "";
            String durationStr = nonInteractive
                ? cli.getOrDefault("days", daysDefault)
                : prompt(scanner, "授权天数 (回车=永久" + (daysDefault.isEmpty() ? "" : "，套餐默认 " + daysDefault) + ")", daysDefault);

            String machineFingerprint = nonInteractive
                ? cli.getOrDefault("fingerprint", "")
                : prompt(scanner, "机器指纹 (回车=不绑定)", "");

            String versionStr = nonInteractive
                ? cli.getOrDefault("license-version", "1")
                : prompt(scanner, "License版本号", "1");
            int licenseVersion;
            try {
                licenseVersion = Integer.parseInt(versionStr.trim());
            } catch (NumberFormatException nfe) {
                System.err.println("[错误] License版本号必须是整数: " + versionStr);
                return;
            }

            // 计算到期日
            String expiresAt = "";
            if (durationStr != null && !durationStr.trim().isEmpty()) {
                try {
                    int days = Integer.parseInt(durationStr.trim());
                    if (days > 0) {
                        Calendar cal = Calendar.getInstance();
                        cal.add(Calendar.DAY_OF_MONTH, days);
                        expiresAt = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
                    }
                } catch (NumberFormatException nfe) {
                    System.err.println("[错误] 授权天数必须是整数: " + durationStr);
                    return;
                }
            }

            // T3.15：features 为空时走 EditionFeatureConfig 归一化取值（不再硬编码）
            if (features == null || features.isEmpty()) {
                features = getDefaultFeatures(edition);
            }

            // 构建签名内容
            String signContent = LicenseSignUtil.buildSignContent(
                licenseId, edition, licenseType, customer, features, industries,
                expiresAt, machineFingerprint, licenseVersion
            );

            // 签名
            String signature;
            try {
                signature = LicenseSignUtil.sign(signContent, privateKey);
            } catch (RuntimeException re) {
                System.err.println("[错误] 签名失败: " + re.getMessage());
                return;
            }

            // 输出 License JSON
            System.out.println();
            System.out.println("====== 签发成功！以下JSON导入生产系统 ======");
            System.out.println("{");
            System.out.println("  \"licenseId\": \"" + licenseId + "\",");
            if (tenantIdStr != null && !tenantIdStr.isEmpty()) {
                System.out.println("  \"tenantId\": " + tenantIdStr + ",");
            }
            if (bizCode != null && !bizCode.isEmpty()) {
                System.out.println("  \"bizCode\": \"" + bizCode + "\",");
            }
            if (plan != null && !plan.isEmpty()) {
                System.out.println("  \"plan\": \"" + plan + "\",");
            }
            System.out.println("  \"edition\": \"" + edition + "\",");
            System.out.println("  \"licenseType\": \"" + licenseType + "\",");
            System.out.println("  \"customer\": \"" + customer + "\",");
            System.out.println("  \"features\": \"" + escapeJson(features) + "\",");
            if (industries != null && !industries.isEmpty()) {
                System.out.println("  \"industries\": \"" + escapeJson(industries) + "\",");
            }
            if (!expiresAt.isEmpty()) {
                System.out.println("  \"expiresAt\": \"" + expiresAt + "\",");
            }
            if (machineFingerprint != null && !machineFingerprint.isEmpty()) {
                System.out.println("  \"machineFingerprint\": \"" + machineFingerprint + "\",");
            }
            System.out.println("  \"licenseVersion\": " + licenseVersion + ",");
            System.out.println("  \"signature\": \"" + signature + "\"");
            System.out.println("}");
            System.out.println("====== 完成 ======");
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    /** 交互式 prompt（保留原行为） */
    private static String prompt(Scanner scanner, String label, String defaultVal) {
        System.out.print(label + " [" + defaultVal + "]: ");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? defaultVal : input;
    }

    /** 无默认值的输入 prompt（如私钥） */
    private static String promptLine(Scanner scanner, String label, String defaultVal) {
        return prompt(scanner, label, defaultVal);
    }

    /**
     * T3.15：按版本从 {@link EditionFeatureConfig} 取默认功能列表并序列化为 JSON 数组字符串。
     * <p>入参先经 {@link EditionFeatureConfig#normalizeEdition(String)} 归一化，
     * 兼容 {@code pro/PROFESSIONAL/ENTERPRISE/adv/ADVANCED/base/BASIC/null/未知值}。
     * <p>不再维护硬编码列表，杜绝与配置类漂移。
     */
    private static String getDefaultFeatures(String edition) {
        String std = EditionFeatureConfig.normalizeEdition(edition);
        Set<String> set = EditionFeatureConfig.getFeaturesByEdition(std);
        if (set == null || set.isEmpty()) {
            return "[]";
        }
        // 排序输出，便于 diff 与幂等
        List<String> sorted = new ArrayList<>(set);
        Collections.sort(sorted);
        StringBuilder sb = new StringBuilder(sorted.size() * 24 + 2);
        sb.append('[');
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(sorted.get(i).replace("\"", "\\\"")).append('"');
        }
        sb.append(']');
        return sb.toString();
    }

    /** T3.15：套餐类型 → 默认授权天数；未知/空套餐返回 0（走永久或 --days 显式指定） */
    private static int resolvePlanDays(String plan) {
        if (plan == null) {
            return 0;
        }
        switch (plan.trim().toLowerCase(Locale.ROOT)) {
            case "trial":
                return PLAN_DAYS_TRIAL;
            case "first_month":
            case "first-month":
                return PLAN_DAYS_FIRST_MONTH;
            case "monthly":
                return PLAN_DAYS_MONTHLY;
            case "yearly":
            case "annual":
                return PLAN_DAYS_YEARLY;
            default:
                return 0;
        }
    }

    private static String escapeJson(String val) {
        return val == null ? "" : val.replace("\"", "\\\"");
    }

    private static void printUsage() {
        System.out.println("License签发工具（v1.28）");
        System.out.println("用法:");
        System.out.println("  keygen                                       - 生成RSA-2048密钥对");
        System.out.println("  sign                                         - 交互式签发License（含业态包绑定）");
        System.out.println("  sign --private-key=<base64> [选项]           - 命令行签发（脚本化，T3.15）");
        System.out.println();
        System.out.println("命令行选项（sign 子命令，均可选，缺项走默认值）:");
        System.out.println("  --private-key=<base64>       私钥（脚本化必填）");
        System.out.println("  --license-id=<id>            授权编号，默认 LIC-{year}-000001");
        System.out.println("  --edition=<code>             版本等级：BASIC/ADVANCED/PROFESSIONAL（兼容 pro/adv/base/ENTERPRISE）");
        System.out.println("  --biz-code=<code>            业态包编码（T3.15 新增）：fund_control/boardgame/fresh_food，留空=不限");
        System.out.println("  --plan=<type>                套餐类型（T3.15 新增）：trial(7天)/first_month(30天)/monthly(30天)/yearly(365天)");
        System.out.println("  --days=<n>                   授权天数，显式覆盖套餐默认；0 或空=永久");
        System.out.println("  --license-type=<type>        TRIAL/FORMAL，默认 FORMAL（plan=trial 时自动 TRIAL）");
        System.out.println("  --tenant-id=<id>             绑定租户ID，可选");
        System.out.println("  --customer=<name>            客户名称");
        System.out.println("  --features=<json>            功能列表JSON，留空=按版本从 EditionFeatureConfig 自动填充");
        System.out.println("  --industries=<json>          行业列表JSON");
        System.out.println("  --fingerprint=<str>          机器指纹");
        System.out.println("  --license-version=<n>        License版本号，默认 1");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  java -cp ruoyi-system.jar com.ruoyi.system.utils.LicenseGenerateTool keygen");
        System.out.println("  java -cp ruoyi-system.jar com.ruoyi.system.utils.LicenseGenerateTool sign");
        System.out.println("  java -cp ruoyi-system.jar com.ruoyi.system.utils.LicenseGenerateTool sign \\");
        System.out.println("       --private-key=MIIE... --edition=pro --biz-code=fund_control --plan=yearly \\");
        System.out.println("       --tenant-id=1001 --customer=某某公司");
        System.out.println();
        System.out.println("v1.16：签发时可绑定业态包(bizCode)，导入时校验租户业态匹配");
        System.out.println("v1.28：T3.15 缺口封闭——新增 --biz-code / --plan 命令行参数；getDefaultFeatures 改为引用 EditionFeatureConfig；");
        System.out.println("       edition 走 normalizeEdition 归一化，兼容 pro/adv/base/ENTERPRISE 全部别名。");
    }

    /**
     * T3.15 命令行参数解析：支持 {@code --key=value} 与 {@code --key value} 两种形式。
     */
    private static final class CliOptions {
        private final java.util.Map<String, String> map = new java.util.HashMap<>();

        static CliOptions parse(String[] args) {
            CliOptions opts = new CliOptions();
            if (args == null) {
                return opts;
            }
            // args[0] 为子命令 sign，从 args[1] 起解析
            for (int i = 1; i < args.length; i++) {
                String a = args[i];
                if (a == null || !a.startsWith("--")) {
                    continue;
                }
                String body = a.substring(2);
                int eq = body.indexOf('=');
                if (eq >= 0) {
                    opts.map.put(body.substring(0, eq).trim().toLowerCase(Locale.ROOT), body.substring(eq + 1));
                } else if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    opts.map.put(body.trim().toLowerCase(Locale.ROOT), args[++i]);
                } else {
                    opts.map.put(body.trim().toLowerCase(Locale.ROOT), "");
                }
            }
            return opts;
        }

        boolean hasAnyOption() {
            return !map.isEmpty();
        }

        String getOrDefault(String key, String defVal) {
            String v = map.get(key.toLowerCase(Locale.ROOT));
            return v == null ? defVal : v;
        }
    }
}
