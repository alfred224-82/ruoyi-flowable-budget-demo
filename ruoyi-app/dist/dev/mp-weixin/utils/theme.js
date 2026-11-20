"use strict";
const common_vendor = require("../common/vendor.js");
const utils_request = require("./request.js");
const utils_auth = require("./auth.js");
const config_index = require("../config/index.js");
const THEME_KEY = "app_tenant_theme";
const PRESET_THEMES = {
  theme_fresh: {
    key: "theme_fresh",
    name: "清新简约风",
    primaryColor: "#4F8FF7",
    accentColor: "#7AB8FF",
    bgColor: "#F6F8FC",
    pageBg: "#F6F8FC",
    cardBg: "#FFFFFF",
    text: "#1D2129",
    textSub: "#86909C",
    brand: "#4F8FF7",
    brandSolid: "#4F8FF7",
    brand2: "#7AB8FF",
    brandWeak: "rgba(79,143,247,.10)",
    danger: "#E54545",
    border: "rgba(29,33,41,.08)",
    borderHi: "rgba(79,143,247,.35)",
    brandGrad: "none",
    radiusCard: "12px",
    radiusBtn: "24px",
    radiusImg: "12px",
    radiusTag: "10px",
    shadowCard: "0 2px 8px rgba(29,33,41,.06)",
    shadowBtn: "0 6px 16px rgba(79,143,247,.18)",
    gradientStart: "#4F8FF7",
    gradientEnd: "#7AB8FF",
    gradText: "#FFFFFF",
    gradSubText: "rgba(255,255,255,.85)",
    iconStyle: "line",
    iconColor: "#4F8FF7",
    isDark: false
  },
  theme_vivid: {
    key: "theme_vivid",
    name: "活力渐变风",
    primaryColor: "#F5455C",
    accentColor: "#FFD166",
    bgColor: "#FFF1F0",
    pageBg: "#FFF1F0",
    cardBg: "#FFFFFF",
    text: "#2B2B2B",
    textSub: "#8A6E6E",
    brand: "#F5455C",
    brandSolid: "#F5455C",
    brand2: "#FFD166",
    brandWeak: "rgba(245,69,92,.10)",
    danger: "#E54545",
    border: "rgba(43,43,43,.08)",
    borderHi: "rgba(245,69,92,.35)",
    brandGrad: "linear-gradient(135deg, #F5455C 0%, #FF7A45 100%)",
    radiusCard: "16px",
    radiusBtn: "28px",
    radiusImg: "16px",
    radiusTag: "12px",
    shadowCard: "0 2px 10px rgba(245,69,92,.10)",
    shadowBtn: "0 6px 18px rgba(245,69,92,.28)",
    gradientStart: "#F5455C",
    gradientEnd: "#FF7A45",
    gradText: "#FFFFFF",
    gradSubText: "rgba(255,255,255,.85)",
    iconStyle: "solid-round",
    iconColor: "#F5455C",
    isDark: false
  },
  theme_dark: {
    key: "theme_dark",
    name: "暗黑科技风",
    primaryColor: "#5B8FFF",
    accentColor: "#00E5FF",
    bgColor: "#0B0F19",
    pageBg: "#0B0F19",
    cardBg: "#151A26",
    text: "#E8ECF4",
    textSub: "#8B95A5",
    brand: "#5B8FFF",
    brandSolid: "#5B8FFF",
    brand2: "#00E5FF",
    brandWeak: "rgba(91,143,255,.14)",
    danger: "#FF5C5C",
    border: "rgba(91,143,255,.25)",
    borderHi: "rgba(0,229,255,.55)",
    brandGrad: "linear-gradient(135deg, #5B8FFF 0%, #00E5FF 100%)",
    radiusCard: "10px",
    radiusBtn: "24px",
    radiusImg: "10px",
    radiusTag: "8px",
    shadowCard: "0 2px 10px rgba(0,0,0,.40)",
    shadowBtn: "0 0 12px rgba(91,143,255,.45)",
    gradientStart: "#1A2138",
    gradientEnd: "#0B0F19",
    gradText: "#FFFFFF",
    gradSubText: "rgba(255,255,255,.75)",
    iconStyle: "solid-glow",
    iconColor: "#00E5FF",
    isDark: true
  },
  theme_handdrawn: {
    key: "theme_handdrawn",
    name: "温暖手绘风",
    primaryColor: "#FFB347",
    accentColor: "#FF8A8A",
    bgColor: "#FFF9F0",
    pageBg: "#FFF9F0",
    cardBg: "#FFFFFF",
    text: "#3A3A3A",
    textSub: "#9E9E9E",
    brand: "#FFB347",
    brandSolid: "#FFB347",
    brand2: "#FF8A8A",
    brandWeak: "rgba(255,179,71,.14)",
    danger: "#E5544B",
    border: "rgba(58,58,58,.08)",
    borderHi: "rgba(255,179,71,.40)",
    brandGrad: "none",
    radiusCard: "18px",
    radiusBtn: "22px",
    radiusImg: "16px",
    radiusTag: "14px",
    shadowCard: "0 3px 10px rgba(255,179,71,.14)",
    shadowBtn: "0 6px 16px rgba(255,179,71,.22)",
    gradientStart: "#FFB347",
    gradientEnd: "#FF8A8A",
    gradText: "#7C4A21",
    gradSubText: "rgba(124,74,33,.78)",
    iconStyle: "hand-drawn",
    iconColor: "#F08A24",
    isDark: false
  }
};
const DEFAULT_THEME = PRESET_THEMES.theme_fresh;
function getThemeSync() {
  const cached = common_vendor.index.getStorageSync(THEME_KEY);
  return cached || DEFAULT_THEME;
}
function loadTenantTheme() {
  const hasToken = !!utils_auth.getToken();
  if (!hasToken) {
    return Promise.resolve(getThemeSync());
  }
  return utils_request.get(config_index.config.api.microThemeCurrent).then((res) => {
    const data = res && res.data || {};
    const theme = PRESET_THEMES[data.key] || DEFAULT_THEME;
    common_vendor.index.setStorageSync(THEME_KEY, theme);
    return theme;
  }).catch(() => {
    return getThemeSync();
  });
}
function loadThemeByTenantId(tenantId) {
  if (!tenantId) {
    return Promise.resolve(getThemeSync());
  }
  return utils_request.get(config_index.config.api.microThemeByTenant, { tenantId }).then((res) => {
    const data = res && res.data || {};
    const theme = PRESET_THEMES[data.key] || DEFAULT_THEME;
    common_vendor.index.setStorageSync(THEME_KEY, theme);
    return theme;
  }).catch(() => {
    return getThemeSync();
  });
}
function themeGradient(themeKey) {
  const t = PRESET_THEMES[themeKey] || DEFAULT_THEME;
  return `linear-gradient(135deg, ${t.gradientStart} 0%, ${t.gradientEnd} 100%)`;
}
function themeGradientText(themeKey) {
  const t = PRESET_THEMES[themeKey] || DEFAULT_THEME;
  return { text: t.gradText, sub: t.gradSubText };
}
function hexToRgba(hex, alpha = 1) {
  if (!hex || typeof hex !== "string") return `rgba(0,0,0,${alpha})`;
  let h = hex.replace("#", "").trim();
  if (h.length === 3) h = h.split("").map((c) => c + c).join("");
  if (h.length !== 6) return `rgba(0,0,0,${alpha})`;
  const r = parseInt(h.slice(0, 2), 16);
  const g = parseInt(h.slice(2, 4), 16);
  const b = parseInt(h.slice(4, 6), 16);
  return `rgba(${r},${g},${b},${alpha})`;
}
function themeSoftBg(themeKey, alpha = 0.2) {
  const t = PRESET_THEMES[themeKey] || DEFAULT_THEME;
  return hexToRgba(t.primaryColor, alpha);
}
function themeCssVars(themeKey) {
  const t = PRESET_THEMES[themeKey] || DEFAULT_THEME;
  const grad = `linear-gradient(135deg, ${t.gradientStart} 0%, ${t.gradientEnd} 100%)`;
  return {
    /* ---------- 完整设计令牌 ---------- */
    "--page-bg": t.pageBg,
    "--card-bg": t.cardBg,
    "--text": t.text,
    "--text-sub": t.textSub,
    "--brand": t.brand,
    "--brand-solid": t.brandSolid,
    "--brand-2": t.brand2,
    "--brand-weak": t.brandWeak,
    "--brand-grad": t.brandGrad || "none",
    "--danger": t.danger,
    "--border": t.border,
    "--border-hi": t.borderHi,
    "--radius-card": t.radiusCard,
    "--radius-btn": t.radiusBtn,
    "--radius-img": t.radiusImg,
    "--radius-tag": t.radiusTag,
    "--shadow-card": t.shadowCard,
    "--shadow-btn": t.shadowBtn,
    "--font-title": "28px",
    "--font-sub": "18px",
    "--font-body": "15px",
    "--lh-body": t.key === "theme_handdrawn" ? "1.7" : "1.6",
    "--gutter": "16px",
    "--icon-style": t.iconStyle,
    "--icon-color": t.iconColor,
    /* ---------- 旧版别名（兼容 4 个已接入页面） ---------- */
    "--tp": t.primaryColor,
    "--tp-soft": t.brandWeak,
    "--tbg": t.bgColor,
    "--tgrad": grad,
    "--tradius": t.radiusCard,
    "--ttext": t.text,
    "--ttext2": t.textSub,
    "--tdark": t.isDark ? "1" : "0"
  };
}
function resolveThemeKey(queryTheme) {
  if (queryTheme && PRESET_THEMES[queryTheme]) return queryTheme;
  const sync = getThemeSync() || DEFAULT_THEME;
  return sync.key;
}
exports.getThemeSync = getThemeSync;
exports.loadTenantTheme = loadTenantTheme;
exports.loadThemeByTenantId = loadThemeByTenantId;
exports.resolveThemeKey = resolveThemeKey;
exports.themeCssVars = themeCssVars;
exports.themeGradient = themeGradient;
exports.themeGradientText = themeGradientText;
exports.themeSoftBg = themeSoftBg;
