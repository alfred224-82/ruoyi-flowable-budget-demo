# M42 PWA 启用与 License 加密部署指南

> **版本**：v1.0  
> **日期**：2026-10-25  
> **适用范围**：小微版生产环境（https://xiaozhangben.com.cn）  
> **前置条件**：域名已备案、HTTPS 证书已部署、后端服务正常运行

---

## 一、变更概述

本次部署包含三大能力启用：

### 1.1 PWA（Progressive Web App）
- **功能**：用户可将 H5 页面"添加到主屏幕"，获得桌面图标 + 全屏启动 + 离线兜底
- **影响范围**：`ruoyi-app`（uni-app H5，base=/h5/）
- **扫码白名单**：不受影响（SW 对匿名接口 Network-First 不缓存）

### 1.2 License RSA 数字签名（hard 模式）
- **功能**：License 从明文（soft 模式 signature=LOCAL-GENERATED）改为 RSA-SHA256 数字签名
- **影响范围**：`application-prod.yml` + `LicenseServiceImpl` + 前端 License 管理页
- **安全提升**：禁止明文切换版本，签发/导入强制验签

### 1.3 Service Worker 缓存策略
- **功能**：nginx 补齐 sw.js no-cache + manifest.webmanifest MIME 类型
- **影响范围**：部署 nginx.conf 的 `/h5/` location

---

## 二、配置文件变更清单

### 2.1 后端配置

#### application-prod.yml（License 加密）
**文件路径**：`ruoyi-admin/src/main/resources/application-prod.yml`

**变更内容**：
```yaml
--- # License授权配置（生产环境加固模式）
license:
  # 生产环境强制使用hard模式：RSA签名验证 + 机器指纹绑定，禁止明文切换版本
  enforce-mode: hard
  # RSA公钥(Base64) - 用于验证 License 签名（由 LicenseGenerateTool keygen 生成）。
  #   公钥可安全入库；对应私钥通过环境变量注入，切勿提交仓库。
  public-key: MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzX28ANnP2yQqR9LeODLHMH5kekqSCJD+xWfHQ6t3kLb9Vt9OQAPMQuWrqtMvDvmdnasRptB1IumYTke9oAMXXEvoAXN3W3ShbbpMQ4Rcz1UFlBF5Rr8UjesC+vpr9gUN3eLS6BE06y1hVzu32P5PhkdD9/3Q3Dc7/cByZWXx4lZHGnMMuL/Rapdd/Xa11h5zw9qAvuqBWteM86rTZFTjxbEf4BjM687M72T7Ve6E20DUQwffJAHQkG0XEpEoMVHW4vPwsCcIHo0t2fkyFa2IhSOuG5Y933hMheF5MhoHpDTxTyuRl2WVvdv/9z0HAouo0LoduWFHZHRSRkWnUyMUmwIDAQAB
  # RSA私钥(Base64) - 仅 Admin 侧签发/重签 License 使用。
  #   ★安全：禁止硬编码或提交仓库，部署时通过环境变量 LICENSE_PRIVATE_KEY 注入
  #   （Linux: export LICENSE_PRIVATE_KEY=xxx ；Windows: setx LICENSE_PRIVATE_KEY xxx）。
  #   缺省为空时，hard 模式签发/重签接口会 fail-fast 报错，不影响验签与正常登录。
  private-key: ${LICENSE_PRIVATE_KEY:}
  verify-fingerprint: true
  allow-plain-switch: false
```

**关键说明**：
- `public-key`：已填入新生成的 RSA-2048 公钥（可安全提交到 Git）
- `private-key`：使用 `${LICENSE_PRIVATE_KEY:}` 占位符，运行时从环境变量读取
- `enforce-mode: hard`：生产环境强制验签（原本已是 hard，但公钥为空等于失效）
- `allow-plain-switch: false`：禁止明文切换版本

### 2.2 前端配置

#### vite.config.js（PWA 启用）
**文件路径**：`ruoyi-app/vite.config.js`

**变更内容**：
```javascript
/**
 * PWA 总开关（已启用 · 域名 + HTTPS 就绪）
 *
 * 现状：https://xiaozhangben.com.cn 已上线且 HTTPS 可用，PWA 默认开启。
 *       public/ 下 manifest.webmanifest / sw.js / icon-*.png 齐备，构建时复制到 dist/build/h5/，
 *       生产构建（NODE_ENV=production）会自动注入 <link rel="manifest"> 与 SW 注册脚本，
 *       用户即可「添加到主屏幕」获得桌面图标 + 全屏启动 + 离线兜底。
 *       如需临时关闭，构建时设置环境变量 VITE_PWA_ENABLED=false 即可，无需改代码。
 */
const PWA_ENABLED = String(process.env.VITE_PWA_ENABLED || 'true').toLowerCase() === 'true'
```

**关键说明**：
- 默认值从 `'false'` 改为 `'true'`
- 仍可通过 `VITE_PWA_ENABLED=false` 临时关闭
- 仅生产构建（`npm run build:h5`）注入 manifest 和 SW

#### sw.js（缓存版本）
**文件路径**：`ruoyi-app/public/sw.js`

**变更内容**：
```javascript
const CACHE_VERSION = 'v6'  // 从 v5 升级到 v6
```

**关键说明**：
- 首次启用 PWA，缓存版本升级确保干净部署
- SW 策略：导航 Network-First、/prod-api/* Network-First 不缓存、同源静态 Cache-First

### 2.3 Nginx 配置

#### nginx.conf（SW/manifest 配置）
**文件路径**：`docs/最新整理好的需求/小微版/上线后新增的修改文档/nginx.conf`

**变更内容**：在 `/h5/` location 前新增两条精确匹配：

```nginx
# ========== H5移动端页面：https://xxx/h5 ==========
# PWA: sw.js 必须 no-cache，否则浏览器会长期沿用旧 Service Worker，更新无法生效
location = /h5/sw.js {
    root   C:/tools/ruoyi-demo/nginx;
    default_type application/javascript;
    add_header Cache-Control "no-cache, no-store, must-revalidate";
    add_header Service-Worker-Allowed "/h5/";
}
# PWA: manifest 必须以 application/manifest+json 返回，否则浏览器拒绝解析、无法「添加到主屏幕」
location = /h5/manifest.webmanifest {
    root   C:/tools/ruoyi-demo/nginx;
    default_type application/manifest+json;
    add_header Cache-Control "no-cache";
}
location /h5/ {
    root   C:/tools/ruoyi-demo/nginx;
    index  index.html index.htm;
    try_files $uri $uri/ /h5/index.html;
}
```

**关键说明**：
- `= /h5/sw.js`：强制 no-cache，确保 SW 更新即时生效
- `= /h5/manifest.webmanifest`：显式设置 MIME 类型（nginx 默认 mime.types 无此类型）
- 两条精确匹配 location 放在 `/h5/` 前，nginx 优先匹配

### 2.4 后端代码变更

#### LicenseServiceImpl（存量重签迁移）
**文件路径**：`ruoyi-system/src/main/java/com/ruoyi/system/service/impl/LicenseServiceImpl.java`

**新增方法**：`resignAllLicenses()`

**功能**：
- 遍历 sys_license 中 ACTIVE/TAMPERED/EXPIRED 的记录
- 按存量字段重算签名内容并用私钥重新签名
- 对被误判 TAMPERED 的记录复位为 ACTIVE
- 清除相关租户缓存并重载系统级 License

**触发方式**：前端「重签存量License」按钮 → `POST /system/license/resign`

#### LicenseController（重签接口）
**文件路径**：`ruoyi-admin/src/main/java/com/ruoyi/web/controller/system/LicenseController.java`

**新增端点**：
```java
@SaCheckPermission("system:license:manage")
@PostMapping("/resign")
public R<Integer> resign() {
    int count = licenseService.resignAllLicenses();
    return R.ok("重签完成，共处理 " + count + " 条 License", count);
}
```

### 2.5 前端代码变更

#### license/index.vue（重签按钮）
**文件路径**：`ruoyi-ui/src/views/system/license/index.vue`

**变更内容**：
- 授权信息卡片头部新增红色「重签存量License」按钮
- 新增 `handleResignLicense()` 方法（带确认对话框）
- 新增 `resignLoading` 状态

#### api/system/license.js（重签 API）
**文件路径**：`ruoyi-ui/src/api/system/license.js`

**新增函数**：
```javascript
export function resignLicense() {
  return request({
    url: '/system/license/resign',
    method: 'post'
  })
}
```

---

## 三、部署步骤（按顺序执行）

### 步骤 1：设置私钥环境变量

#### 方式 A：永久生效（推荐，重启后仍在）

**PowerShell（管理员权限）**：
```powershell
# 设置系统级环境变量（所有用户可见）
[System.Environment]::SetEnvironmentVariable("LICENSE_PRIVATE_KEY", "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDNfbwA2c/bJCpH0t44MscwfmR6SpIIkP7FZ8dDq3eQtv1W305AA8xC5auq0y8O+Z2dqxGm0HUi6ZhOR72gAxdcS+gBc3dbdKFtukxDhFzPVQWUEXlGvxSN6wL6+mv2BQ3d4tLoETTrLWFXO7fY/k+GR0P3/dDcNzv9wHJlZfHiVkcacwy4v9Fql139drXWHnPD2oC+6oFa14zzqtNkVOPFsR/gGMzrzszvZPtV7oTbQNRDB98kAdCQbRcSkSgxUdbi8/CwJwgejS3Z+TIVrYiFI64blj3feEyF4XkyGgekNPFPK5GXZZW92//3PQcCi6jQuh25YUdkdFJGRadTIxSbAgMBAAECggEAW8ngIpkoiYCcCj0c2gmEhBrREvcgSAdMZi03CvkajV/PxfA1ac7IfNFOXx26FXxWdUkmt1bCG/gv+jd9cm4vDyEw3wUOuaFdQkBHNJl5Q3t85GjxvTI5IjNpv2YAt4IGcaiSAOs+shYxNuAL/TeLmjVXHP0ZQGTDvVMrRjPFWg3WzWZCQUuJrN1UqtMHDGtfvRHD4+/ilSSUP2zlfUqGMgUmbQm8z541n+90IeWa0ksl1m7bMTUn8nxz2p6bPgxNqKmsL9O0+7/17M8Jxa63umdcLsgl0K7tK1glV5gtITAHpdo0f0TTFLSom8S2IImW2O1ksn7Kj/I4mfNWi3TcMQKBgQDwoE7eUbN5QVBrHs6ISFU4/68bi7f7+sG9nG1oDpl4j2Idm26DmFbPM2Q1LYE4wJwSUwl1HzCXi6Fy6aGp0niKrtlY8XtT5/z3X/xzXyyBjLgXV8VJc27fSkKGWhZ5r/Oq22qbFmeyX9jzYbAqQe2vu0dA1FSVX73xvPrnazl4nwKBgQDansGGDGZWyyM4jFofHKzOms9oTF4ICJcnOkQ/9U9e3AgH5fkq1EpiUVKS5o/rybfNjfBxZiVJZHY8J8tuPTPCE8DcrU3dChQFCxNEVGGaXZuBlVe1m8KlcXGA+Byy3Qkkikyv1OoRqsuB1Tgxy4QKcl7bkQPWE4CFVWZhHv5WhQKBgBE962jSweF2ZKTIWFQ7mp+y8oEuycgsd6V2iABky2pwG7+zW3/icpX2QL/uusuuQYCuoobZ7Ulun+r0ArmAc1K8pKZS51tjzCSNpDFAKJg/1REXIPPaaZ9mWs31tpaRnQwpHMJwII4PgCQwY8cjYjytVrW2gTkS2zFvMStT65JRAoGBALRU3dcf5U7Us1HS3Afr1FN7+XfT/oBr7ZrLAj8rr3PU1b8GIPkGMfFVmWJZUwAw3S9uVJaFpl4awzitrq1ZSJdfSef+AEtDI+5hC1yCqhv1+O7z7Gx+dhmYMQMsJigYEZGVjto/K3C1TLbk1Tyl3l3rp3k/7kUowwu4vQGB2lQtAoGBALt7dsX42ijI8bErwQD5WSM4JKBu9NC5HzmY1m2XKp1pHoD4q0/savx+oFO5dZaugEdXZrS1ObKm8zMftEutqu7PrbONi5recwm1kTowN88VGwIPqYS5eLVVOFOSnUe7WEgQKoaemYWxGqwxnDafppkP/szrWFe66Nq9L+u9+kD8", "Machine")

# 验证是否设置成功
[System.Environment]::GetEnvironmentVariable("LICENSE_PRIVATE_KEY", "Machine")
```

**或者 CMD**：
```cmd
setx LICENSE_PRIVATE_KEY "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDNfbwA2c/bJCpH0t44MscwfmR6SpIIkP7FZ8dDq3eQtv1W305AA8xC5auq0y8O+Z2dqxGm0HUi6ZhOR72gAxdcS+gBc3dbdKFtukxDhFzPVQWUEXlGvxSN6wL6+mv2BQ3d4tLoETTrLWFXO7fY/k+GR0P3/dDcNzv9wHJlZfHiVkcacwy4v9Fql139drXWHnPD2oC+6oFa14zzqtNkVOPFsR/gGMzrzszvZPtV7oTbQNRDB98kAdCQbRcSkSgxUdbi8/CwJwgejS3Z+TIVrYiFI64blj3feEyF4XkyGgekNPFPK5GXZZW92//3PQcCi6jQuh25YUdkdFJGRadTIxSbAgMBAAECggEAW8ngIpkoiYCcCj0c2gmEhBrREvcgSAdMZi03CvkajV/PxfA1ac7IfNFOXx26FXxWdUkmt1bCG/gv+jd9cm4vDyEw3wUOuaFdQkBHNJl5Q3t85GjxvTI5IjNpv2YAt4IGcaiSAOs+shYxNuAL/TeLmjVXHP0ZQGTDvVMrRjPFWg3WzWZCQUuJrN1UqtMHDGtfvRHD4+/ilSSUP2zlfUqGMgUmbQm8z541n+90IeWa0ksl1m7bMTUn8nxz2p6bPgxNqKmsL9O0+7/17M8Jxa63umdcLsgl0K7tK1glV5gtITAHpdo0f0TTFLSom8S2IImW2O1ksn7Kj/I4mfNWi3TcMQKBgQDwoE7eUbN5QVBrHs6ISFU4/68bi7f7+sG9nG1oDpl4j2Idm26DmFbPM2Q1LYE4wJwSUwl1HzCXi6Fy6aGp0niKrtlY8XtT5/z3X/xzXyyBjLgXV8VJc27fSkKGWhZ5r/Oq22qbFmeyX9jzYbAqQe2vu0dA1FSVX73xvPrnazl4nwKBgQDansGGDGZWyyM4jFofHKzOms9oTF4ICJcnOkQ/9U9e3AgH5fkq1EpiUVKS5o/rybfNjfBxZiVJZHY8J8tuPTPCE8DcrU3dChQFCxNEVGGaXZuBlVe1m8KlcXGA+Byy3Qkkikyv1OoRqsuB1Tgxy4QKcl7bkQPWE4CFVWZhHv5WhQKBgBE962jSweF2ZKTIWFQ7mp+y8oEuycgsd6V2iABky2pwG7+zW3/icpX2QL/uusuuQYCuoobZ7Ulun+r0ArmAc1K8pKZS51tjzCSNpDFAKJg/1REXIPPaaZ9mWs31tpaRnQwpHMJwII4PgCQwY8cjYjytVrW2gTkS2zFvMStT65JRAoGBALRU3dcf5U7Us1HS3Afr1FN7+XfT/oBr7ZrLAj8rr3PU1b8GIPkGMfFVmWJZUwAw3S9uVJaFpl4awzitrq1ZSJdfSef+AEtDI+5hC1yCqhv1+O7z7Gx+dhmYMQMsJigYEZGVjto/K3C1TLbk1Tyl3l3rp3k/7kUowwu4vQGB2lQtAoGBALt7dsX42ijI8bErwQD5WSM4JKBu9NC5HzmY1m2XKp1pHoD4q0/savx+oFO5dZaugEdXZrS1ObKm8zMftEutqu7PrbONi5recwm1kTowN88VGwIPqYS5eLVVOFOSnUe7WEgQKoaemYWxGqwxnDafppkP/szrWFe66Nq9L+u9+kD8"

# 验证
echo %LICENSE_PRIVATE_KEY%
```

**注意**：`setx` 设置的环境变量对**新打开的命令行窗口**生效，当前窗口需要重启才能读到。

#### 方式 B：当前会话临时生效（调试用）

**PowerShell**：
```powershell
$env:LICENSE_PRIVATE_KEY = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDNfbwA2c/bJCpH0t44MscwfmR6SpIIkP7FZ8dDq3eQtv1W305AA8xC5auq0y8O+Z2dqxGm0HUi6ZhOR72gAxdcS+gBc3dbdKFtukxDhFzPVQWUEXlGvxSN6wL6+mv2BQ3d4tLoETTrLWFXO7fY/k+GR0P3/dDcNzv9wHJlZfHiVkcacwy4v9Fql139drXWHnPD2oC+6oFa14zzqtNkVOPFsR/gGMzrzszvZPtV7oTbQNRDB98kAdCQbRcSkSgxUdbi8/CwJwgejS3Z+TIVrYiFI64blj3feEyF4XkyGgekNPFPK5GXZZW92//3PQcCi6jQuh25YUdkdFJGRadTIxSbAgMBAAECggEAW8ngIpkoiYCcCj0c2gmEhBrREvcgSAdMZi03CvkajV/PxfA1ac7IfNFOXx26FXxWdUkmt1bCG/gv+jd9cm4vDyEw3wUOuaFdQkBHNJl5Q3t85GjxvTI5IjNpv2YAt4IGcaiSAOs+shYxNuAL/TeLmjVXHP0ZQGTDvVMrRjPFWg3WzWZCQUuJrN1UqtMHDGtfvRHD4+/ilSSUP2zlfUqGMgUmbQm8z541n+90IeWa0ksl1m7bMTUn8nxz2p6bPgxNqKmsL9O0+7/17M8Jxa63umdcLsgl0K7tK1glV5gtITAHpdo0f0TTFLSom8S2IImW2O1ksn7Kj/I4mfNWi3TcMQKBgQDwoE7eUbN5QVBrHs6ISFU4/68bi7f7+sG9nG1oDpl4j2Idm26DmFbPM2Q1LYE4wJwSUwl1HzCXi6Fy6aGp0niKrtlY8XtT5/z3X/xzXyyBjLgXV8VJc27fSkKGWhZ5r/Oq22qbFmeyX9jzYbAqQe2vu0dA1FSVX73xvPrnazl4nwKBgQDansGGDGZWyyM4jFofHKzOms9oTF4ICJcnOkQ/9U9e3AgH5fkq1EpiUVKS5o/rybfNjfBxZiVJZHY8J8tuPTPCE8DcrU3dChQFCxNEVGGaXZuBlVe1m8KlcXGA+Byy3Qkkikyv1OoRqsuB1Tgxy4QKcl7bkQPWE4CFVWZhHv5WhQKBgBE962jSweF2ZKTIWFQ7mp+y8oEuycgsd6V2iABky2pwG7+zW3/icpX2QL/uusuuQYCuoobZ7Ulun+r0ArmAc1K8pKZS51tjzCSNpDFAKJg/1REXIPPaaZ9mWs31tpaRnQwpHMJwII4PgCQwY8cjYjytVrW2gTkS2zFvMStT65JRAoGBALRU3dcf5U7Us1HS3Afr1FN7+XfT/oBr7ZrLAj8rr3PU1b8GIPkGMfFVmWJZUwAw3S9uVJaFpl4awzitrq1ZSJdfSef+AEtDI+5hC1yCqhv1+O7z7Gx+dhmYMQMsJigYEZGVjto/K3C1TLbk1Tyl3l3rp3k/7kUowwu4vQGB2lQtAoGBALt7dsX42ijI8bErwQD5WSM4JKBu9NC5HzmY1m2XKp1pHoD4q0/savx+oFO5dZaugEdXZrS1ObKm8zMftEutqu7PrbONi5recwm1kTowN88VGwIPqYS5eLVVOFOSnUe7WEgQKoaemYWxGqwxnDafppkP/szrWFe66Nq9L+u9+kD8"

# 验证
echo $env:LICENSE_PRIVATE_KEY
```

**CMD**：
```cmd
set LICENSE_PRIVATE_KEY=MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDNfbwA2c/bJCpH0t44MscwfmR6SpIIkP7FZ8dDq3eQtv1W305AA8xC5auq0y8O+Z2dqxGm0HUi6ZhOR72gAxdcS+gBc3dbdKFtukxDhFzPVQWUEXlGvxSN6wL6+mv2BQ3d4tLoETTrLWFXO7fY/k+GR0P3/dDcNzv9wHJlZfHiVkcacwy4v9Fql139drXWHnPD2oC+6oFa14zzqtNkVOPFsR/gGMzrzszvZPtV7oTbQNRDB98kAdCQbRcSkSgxUdbi8/CwJwgejS3Z+TIVrYiFI64blj3feEyF4XkyGgekNPFPK5GXZZW92//3PQcCi6jQuh25YUdkdFJGRadTIxSbAgMBAAECggEAW8ngIpkoiYCcCj0c2gmEhBrREvcgSAdMZi03CvkajV/PxfA1ac7IfNFOXx26FXxWdUkmt1bCG/gv+jd9cm4vDyEw3wUOuaFdQkBHNJl5Q3t85GjxvTI5IjNpv2YAt4IGcaiSAOs+shYxNuAL/TeLmjVXHP0ZQGTDvVMrRjPFWg3WzWZCQUuJrN1UqtMHDGtfvRHD4+/ilSSUP2zlfUqGMgUmbQm8z541n+90IeWa0ksl1m7bMTUn8nxz2p6bPgxNqKmsL9O0+7/17M8Jxa63umdcLsgl0K7tK1glV5gtITAHpdo0f0TTFLSom8S2IImW2O1ksn7Kj/I4mfNWi3TcMQKBgQDwoE7eUbN5QVBrHs6ISFU4/68bi7f7+sG9nG1oDpl4j2Idm26DmFbPM2Q1LYE4wJwSUwl1HzCXi6Fy6aGp0niKrtlY8XtT5/z3X/xzXyyBjLgXV8VJc27fSkKGWhZ5r/Oq22qbFmeyX9jzYbAqQe2vu0dA1FSVX73xvPrnazl4nwKBgQDansGGDGZWyyM4jFofHKzOms9oTF4ICJcnOkQ/9U9e3AgH5fkq1EpiUVKS5o/rybfNjfBxZiVJZHY8J8tuPTPCE8DcrU3dChQFCxNEVGGaXZuBlVe1m8KlcXGA+Byy3Qkkikyv1OoRqsuB1Tgxy4QKcl7bkQPWE4CFVWZhHv5WhQKBgBE962jSweF2ZKTIWFQ7mp+y8oEuycgsd6V2iABky2pwG7+zW3/icpX2QL/uusuuQYCuoobZ7Ulun+r0ArmAc1K8pKZS51tjzCSNpDFAKJg/1REXIPPaaZ9mWs31tpaRnQwpHMJwII4PgCQwY8cjYjytVrW2gTkS2zFvMStT65JRAoGBALRU3dcf5U7Us1HS3Afr1FN7+XfT/oBr7ZrLAj8rr3PU1b8GIPkGMfFVmWJZUwAw3S9uVJaFpl4awzitrq1ZSJdfSef+AEtDI+5hC1yCqhv1+O7z7Gx+dhmYMQMsJigYEZGVjto/K3C1TLbk1Tyl3l3rp3k/7kUowwu4vQGB2lQtAoGBALt7dsX42ijI8bErwQD5WSM4JKBu9NC5HzmY1m2XKp1pHoD4q0/savx+oFO5dZaugEdXZrS1ObKm8zMftEutqu7PrbONi5recwm1kTowN88VGwIPqYS5eLVVOFOSnUe7WEgQKoaemYWxGqwxnDafppkP/szrWFe66Nq9L+u9+kD8

echo %LICENSE_PRIVATE_KEY%
```

### 步骤 2：更新 Nginx 配置

将 `docs/最新整理好的需求/小微版/上线后新增的修改文档/nginx.conf` 中新增的两条 location 合并到线上 nginx.conf：

```nginx
# 在 location /h5/ 前添加
location = /h5/sw.js {
    root   C:/tools/ruoyi-demo/nginx;  # 替换为你的实际路径
    default_type application/javascript;
    add_header Cache-Control "no-cache, no-store, must-revalidate";
    add_header Service-Worker-Allowed "/h5/";
}
location = /h5/manifest.webmanifest {
    root   C:/tools/ruoyi-demo/nginx;  # 替换为你的实际路径
    default_type application/manifest+json;
    add_header Cache-Control "no-cache";
}
```

**重载 nginx**：
```bash
nginx -t  # 测试配置
nginx -s reload  # 重载
```

### 步骤 3：重新构建前端

```powershell
cd D:/workspace/RuoYi-Flowable-Plus-0.8.X/ruoyi-app
npm run build:h5
```

**构建产物**：`dist/build/h5/`，包含：
- `index.html`（已注入 manifest link 和 SW 注册脚本）
- `manifest.webmanifest`
- `sw.js`（CACHE_VERSION=v6）
- `icon-*.png`（8 个尺寸）

**部署**：将 `dist/build/h5/` 内容复制到 nginx 的 `/h5/` 目录（如 `C:/tools/ruoyi-demo/nginx/h5/`）。

### 步骤 4：重启后端服务

确保环境变量 `LICENSE_PRIVATE_KEY` 已设置（步骤 1），然后重启 Java 服务：

```powershell
# 如果用 app.ps1 启动
./app.ps1

# 或直接启动
java -jar ruoyi-admin.jar --spring.profiles.active=prod
```

**验证环境变量是否被加载**：
```powershell
# 查看后端启动日志，应看到：
# [License] 加载成功 | 模式=hard | 版本=MICRO | 功能数=XX | 行业=[...]
```

### 步骤 5：执行存量 License 重签迁移

1. 登录 admin 后台（https://xiaozhangben.com.cn/）
2. 进入「授权管理/License」页面
3. 点击红色**「重签存量License」**按钮
4. 确认对话框点"确定"
5. 等待提示"重签完成，共处理 X 条 License"

**后端日志应看到**：
```
[License] 存量 License 重签迁移完成，共重签 X 条
```

### 步骤 6：验证

#### 6.1 PWA 验证
1. 手机浏览器打开 `https://xiaozhangben.com.cn/h5/`
2. 浏览器应弹"添加到主屏幕"提示（或手动点菜单"添加到主屏幕"）
3. 桌面出现"小账本"图标
4. 点开全屏启动（无浏览器地址栏）
5. 断网后仍可访问（离线兜底）

#### 6.2 License 加密验证
1. Admin 后台 → License 页 → 导出 License
2. 生成的 JSON 里 `signature` 字段应为真实 RSA 签名（Base64，约 344 字符，不是 "LOCAL-GENERATED"）
3. 租户导入该 License → 成功激活

#### 6.3 扫码白名单验证
1. 手机扫描活动二维码
2. 正常进入活动详情页（无需登录）
3. 后端日志无 401/403 错误

---

## 四、回滚方案

### 4.1 关闭 PWA
```powershell
cd D:/workspace/RuoYi-Flowable-Plus-0.8.X/ruoyi-app
$env:VITE_PWA_ENABLED = "false"
npm run build:h5
# 重新部署 dist/build/h5/
```

### 4.2 回退 License 到 soft 模式
修改 `application-prod.yml`：
```yaml
license:
  enforce-mode: soft
  public-key:
  verify-fingerprint: false
  allow-plain-switch: true
```
重启后端。

### 4.3 回退 nginx
移除 `/h5/sw.js` 和 `/h5/manifest.webmanifest` 两条 location，`nginx -s reload`。

---

## 五、安全注意事项

1. **私钥保管**：`LICENSE_PRIVATE_KEY` 环境变量**绝对不要提交到 Git 仓库**
2. **公钥安全**：`application-prod.yml` 中的 `public-key` 可安全提交（公钥本身不敏感）
3. **密钥轮换**：如需更换密钥对，重新运行 `LicenseGenerateTool keygen`，替换公钥、设置新私钥环境变量，然后重跑「重签存量License」
4. **审计日志**：所有 License 签发/导入/重签操作均写入 `sys_tenant_version_log`，可追溯

---

## 六、常见问题

### Q1: 设置环境变量后重启后端，日志报"未配置 license.private-key"？
**原因**：`setx` 设置的环境变量对**新打开的命令行窗口**生效，当前窗口需重启。  
**解决**：关闭当前 PowerShell/CMD，重新打开，再启动后端。

### Q2: 手机浏览器不弹"添加到主屏幕"？
**排查**：
1. 检查 nginx 是否正确返回 `manifest.webmanifest`（F12 网络面板，Content-Type 应为 `application/manifest+json`）
2. 检查 `sw.js` 是否正确注册（F12 Console，应看到 `[PWA] SW registered`）
3. 检查 HTTPS 证书是否有效（SW 仅在安全上下文注册）
4. 清除浏览器缓存，强制刷新（Ctrl+Shift+R）

### Q3: 点「重签存量License」报错"未配置 license.private-key"？
**原因**：后端启动时未读取到 `LICENSE_PRIVATE_KEY` 环境变量。  
**解决**：确认步骤 1 的环境变量设置正确，重启后端。

### Q4: 重签后系统级 License 仍显示"已篡改"？
**原因**：系统级 License 绑定了机器指纹，当前机器指纹与签发时不一致。  
**解决**：检查 `application-prod.yml` 的 `verify-fingerprint: true`，如不需要指纹校验可改为 `false`，重启后端。

### Q5: 扫码进活动详情白名单失效？
**排查**：
1. 检查 `application.yml` 的 `security.excludes` 是否包含 `/micro/activity/browse`、`/micro/activity/signupDetail`
2. 检查 SW 是否正确拦截 `/prod-api/*`（F12 网络面板，应看到 Network-First 策略）
3. 检查后端日志是否有 401/403 错误

---

## 七、附录：密钥对生成记录

**生成时间**：2026-10-25  
**算法**：RSA-2048  
**格式**：
- 公钥：X.509 SubjectPublicKeyInfo (SPKI) DER Base64
- 私钥：PKCS#8 DER Base64

**公钥**（已配置到 application-prod.yml）：
```
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzX28ANnP2yQqR9LeODLHMH5kekqSCJD+xWfHQ6t3kLb9Vt9OQAPMQuWrqtMvDvmdnasRptB1IumYTke9oAMXXEvoAXN3W3ShbbpMQ4Rcz1UFlBF5Rr8UjesC+vpr9gUN3eLS6BE06y1hVzu32P5PhkdD9/3Q3Dc7/cByZWXx4lZHGnMMuL/Rapdd/Xa11h5zw9qAvuqBWteM86rTZFTjxbEf4BjM687M72T7Ve6E20DUQwffJAHQkG0XEpEoMVHW4vPwsCcIHo0t2fkyFa2IhSOuG5Y933hMheF5MhoHpDTxTyuRl2WVvdv/9z0HAouo0LoduWFHZHRSRkWnUyMUmwIDAQAB
```

**私钥**（已设置为环境变量 LICENSE_PRIVATE_KEY）：
见步骤 1。

---

## 八、变更文件清单

| 文件路径 | 变更类型 | 说明 |
|---------|---------|------|
| `ruoyi-admin/src/main/resources/application-prod.yml` | 修改 | License 配置填入公钥 + 私钥占位符 |
| `ruoyi-app/vite.config.js` | 修改 | PWA_ENABLED 默认 true |
| `ruoyi-app/public/sw.js` | 修改 | CACHE_VERSION v5→v6 + 注释更新 |
| `ruoyi-app/index.html` | 修改 | 注释更新 |
| `docs/.../nginx.conf` | 修改 | 新增 sw.js + manifest location |
| `ruoyi-system/.../LicenseServiceImpl.java` | 新增 | resignAllLicenses() 方法 |
| `ruoyi-admin/.../LicenseController.java` | 新增 | POST /system/license/resign 端点 |
| `ruoyi-ui/src/api/system/license.js` | 新增 | resignLicense() 函数 |
| `ruoyi-ui/src/views/system/license/index.vue` | 修改 | 新增重签按钮 + 方法 |

---

**文档结束**
