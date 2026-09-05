package com.ruoyi.system.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;

/**
 * License签发工具（独立运行）
 * 用于生成RSA密钥对 和 签发带数字签名的License文件
 *
 * 使用方式：
 * 1. 生成密钥对：java -cp ... com.ruoyi.system.utils.LicenseGenerateTool keygen
 *    → 输出 publicKey（配置到application-prod.yml）和 privateKey（妥善保管）
 *
 * 2. 签发License：java -cp ... com.ruoyi.system.utils.LicenseGenerateTool sign
 *    → 交互式输入参数，输出签名后的License JSON（导入到生产系统）
 *
 * 安全说明：
 * - privateKey 绝不能放入应用代码或配置中
 * - 仅部署人员/授权管理员持有 privateKey
 * - 应用只配置 publicKey 用于验签
 *
 * @author ruoyi
 * @date 2026-07-25
 */
public class LicenseGenerateTool {

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }
        switch (args[0].toLowerCase()) {
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
     * 生成RSA-2048密钥对
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
     * 签发License（交互式）
     * 用法: sign <privateKey> [licenseId] [edition] [licenseType] [customer] [durationDays] [fingerprint]
     */
    private static void signLicense(String[] args) {
        Scanner scanner = new Scanner(System.in);

        String privateKey;
        String licenseId;
        String edition;
        String licenseType;
        String customer;
        String features;
        String industries;
        String expiresAt;
        String machineFingerprint;
        int licenseVersion;

        if (args.length >= 2) {
            privateKey = args[1];
        } else {
            System.out.print("请输入私钥(Base64): ");
            privateKey = scanner.nextLine().trim();
        }

        System.out.println();
        System.out.println("====== 签发License ======");

        licenseId = prompt(scanner, "授权编号 (如 LIC-2026-000001)", "LIC-" + new SimpleDateFormat("yyyy").format(new Date()) + "-000001");
        edition = prompt(scanner, "版本等级 (BASIC/ADVANCED/PROFESSIONAL，兼容存量ENTERPRISE)", "PROFESSIONAL");
        licenseType = prompt(scanner, "授权类型 (TRIAL/FORMAL)", "FORMAL");
        customer = prompt(scanner, "客户名称", "默认客户");
        String tenantIdStr = prompt(scanner, "租户ID (回车=不绑定租户，系统级)", "");
        features = prompt(scanner, "功能列表JSON (回车=按版本默认)", "");
        industries = prompt(scanner, "行业列表JSON (回车=不限)", "");
        String durationStr = prompt(scanner, "授权天数 (回车=永久)", "");
        machineFingerprint = prompt(scanner, "机器指纹 (回车=不绑定)", "");
        String versionStr = prompt(scanner, "License版本号", "1");
        licenseVersion = Integer.parseInt(versionStr);

        // 计算到期日
        if (!durationStr.isEmpty()) {
            int days = Integer.parseInt(durationStr);
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, days);
            expiresAt = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
        } else {
            expiresAt = "";
        }

        // 如果features为空，使用版本默认（此处简化，实际应引用EditionFeatureConfig）
        if (features.isEmpty()) {
            features = getDefaultFeatures(edition);
        }

        // 构建签名内容
        String signContent = LicenseSignUtil.buildSignContent(
            licenseId, edition, licenseType, customer, features, industries,
            expiresAt, machineFingerprint, licenseVersion
        );

        // 签名
        String signature = LicenseSignUtil.sign(signContent, privateKey);

        // 输出License JSON
        System.out.println();
        System.out.println("====== 签发成功！以下JSON导入生产系统 ======");
        System.out.println("{");
        System.out.println("  \"licenseId\": \"" + licenseId + "\",");
        if (!tenantIdStr.isEmpty()) {
            System.out.println("  \"tenantId\": " + tenantIdStr + ",");
        }
        System.out.println("  \"edition\": \"" + edition + "\",");
        System.out.println("  \"licenseType\": \"" + licenseType + "\",");
        System.out.println("  \"customer\": \"" + customer + "\",");
        System.out.println("  \"features\": \"" + escapeJson(features) + "\",");
        if (!industries.isEmpty()) {
            System.out.println("  \"industries\": \"" + escapeJson(industries) + "\",");
        }
        if (!expiresAt.isEmpty()) {
            System.out.println("  \"expiresAt\": \"" + expiresAt + "\",");
        }
        if (!machineFingerprint.isEmpty()) {
            System.out.println("  \"machineFingerprint\": \"" + machineFingerprint + "\",");
        }
        System.out.println("  \"licenseVersion\": " + licenseVersion + ",");
        System.out.println("  \"signature\": \"" + signature + "\"");
        System.out.println("}");
        System.out.println("====== 完成 ======");
        scanner.close();
    }

    private static String prompt(Scanner scanner, String label, String defaultVal) {
        System.out.print(label + " [" + defaultVal + "]: ");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? defaultVal : input;
    }

    private static String getDefaultFeatures(String edition) {
        // 简化版，与EditionFeatureConfig保持一致
        switch (edition.toUpperCase()) {
            case "PROFESSIONAL":
            // 兼容映射：存量 ENTERPRISE 等价于 PROFESSIONAL（T4.1）
            case "ENTERPRISE":
                return "[\"FEE_MULTI_CURRENCY\",\"FEE_THREE_TRACK\",\"FEE_ACCRUAL_ENGINE\",\"FEE_LOAN_LEDGER\",\"FEE_CROSS_DEPT_ALLOCATE\",\"FEE_DESENSITIZE_CUSTOM\",\"FEE_SUBSIDY_CONFIG\",\"FEE_PROFIT_CUSTOM_TIP\",\"FEE_COMPLIANCE_CUSTOM\",\"FEE_TRACK_CONTROL\",\"FEE_INDUSTRY_SINGLE\",\"FEE_INDUSTRY_MULTI\",\"BUDGET_BATCH_APPROVE\",\"BUDGET_PERIOD_CONTROL\",\"BUDGET_MULTI_LEVEL_APPROVAL\",\"BUDGET_THREE_LEVEL_APPROVAL\",\"BUDGET_SEND_BACK\",\"BUDGET_DEADLINE_REMIND\",\"BUDGET_TIMEOUT_ALERT\",\"BUDGET_RISK_WARNING\",\"BUDGET_RISK_CUSTOM\",\"BUDGET_RISK_FORMULA\",\"BUDGET_RISK_AI\",\"BUDGET_AI_CHAT\",\"BUDGET_AI_REPORT\",\"BUDGET_AI_PRIVATE\",\"BUDGET_REPORT_EXPORT\",\"BUDGET_REPORT_SCHEDULE\",\"BUDGET_WORKFLOW_CUSTOM\",\"BUDGET_DATA_ISOLATION\"]";
            case "ADVANCED":
                return "[\"FEE_MULTI_CURRENCY\",\"FEE_THREE_TRACK\",\"FEE_BUDGET_LINK\",\"FEE_EMAIL_NOTIFY\",\"FEE_RISK_WARNING\",\"FEE_RISK_CUSTOM\",\"FEE_APPROVAL_REMIND\",\"FEE_REPORT_EXPORT\",\"FEE_WORKFLOW_CUSTOM\",\"BUDGET_BATCH_APPROVE\",\"BUDGET_PERIOD_CONTROL\",\"BUDGET_MULTI_LEVEL_APPROVAL\",\"BUDGET_THREE_LEVEL_APPROVAL\",\"BUDGET_SEND_BACK\",\"BUDGET_DEADLINE_REMIND\",\"BUDGET_TIMEOUT_ALERT\",\"BUDGET_RISK_WARNING\",\"BUDGET_AI_CHAT\"]";
            default:
                return "[]";
        }
    }

    private static String escapeJson(String val) {
        return val.replace("\"", "\\\"");
    }

    private static void printUsage() {
        System.out.println("License签发工具");
        System.out.println("用法:");
        System.out.println("  keygen          - 生成RSA-2048密钥对");
        System.out.println("  sign [privKey]  - 交互式签发License");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  java -cp ruoyi-system.jar com.ruoyi.system.utils.LicenseGenerateTool keygen");
        System.out.println("  java -cp ruoyi-system.jar com.ruoyi.system.utils.LicenseGenerateTool sign");
    }
}
