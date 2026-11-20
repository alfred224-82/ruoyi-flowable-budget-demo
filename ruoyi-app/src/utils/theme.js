/**
 * 小程序全局主题（T15.36 · M27 命名对齐 · 20261016 四主题视觉重设计）
 * 设计源：docs/推广文案/20261016/阿杰小账本H5界面主题设计方案_全端.docx + 主题样式交付包/theme_tokens.css
 * PC「小程序主题」选定后写入 sys_tenant.app_theme_key；小程序端页面 onShow 调
 * GET /micro/platform/theme/current 拉取当前租户色板并缓存，首屏先用缓存同步渲染。
 *
 * 4 套预设（key 与后端 MicroPlatformThemeController 保持一致）：
 *   theme_fresh     主题1 清新简约风  #4F8FF7 / #F6F8FC  线性图标   卡片圆角 12px
 *   theme_vivid     主题2 活力渐变风  #F5455C→#FF7A45    圆润面状   卡片圆角 16px
 *   theme_dark      主题3 暗黑科技风  #5B8FFF·#00E5FF    面状发光   卡片圆角 10px
 *   theme_handdrawn 主题4 温暖手绘风  #FFB347 / #FFF9F0  手绘涂鸦   卡片圆角 18px
 *
 * themeCssVars() 输出「完整设计令牌 + 旧版别名」两套 CSS 变量，页面根节点 :style 绑定后，
 * 变量沿 DOM 继承到全部子元素与子组件（含 scoped 样式），实现一处换肤、全页联动。
 */
import { get } from './request'
import { getToken } from './auth'
import config from '../config'

const THEME_KEY = 'app_tenant_theme'

/**
 * 4 套预设主题令牌（对齐 theme_tokens.css t1~t4）。字段说明：
 *  pageBg/cardBg/text/textSub/brand/brand2/brandWeak/danger/border/borderHi —— 配色
 *  brandGrad —— 主按钮/头部渐变（无渐变的主题为 'none'，用 brand 纯色兜底）
 *  radiusCard/radiusBtn/radiusImg/radiusTag —— 圆角
 *  shadowCard/shadowBtn —— 阴影
 *  gradientStart/gradientEnd/gradText/gradSubText —— 封面占位渐变与其上前景文字色
 *  iconStyle/isDark —— 图标风格与深浅色标记（深色主题下部分组件反色）
 */
const PRESET_THEMES = {
    theme_fresh: {
        key: 'theme_fresh', name: '清新简约风',
        primaryColor: '#4F8FF7', accentColor: '#7AB8FF', bgColor: '#F6F8FC',
        pageBg: '#F6F8FC', cardBg: '#FFFFFF', text: '#1D2129', textSub: '#86909C',
        brand: '#4F8FF7', brandSolid: '#4F8FF7', brand2: '#7AB8FF', brandWeak: 'rgba(79,143,247,.10)',
        danger: '#E54545', border: 'rgba(29,33,41,.08)', borderHi: 'rgba(79,143,247,.35)',
        brandGrad: 'none',
        radiusCard: '12px', radiusBtn: '24px', radiusImg: '12px', radiusTag: '10px',
        shadowCard: '0 2px 8px rgba(29,33,41,.06)', shadowBtn: '0 6px 16px rgba(79,143,247,.18)',
        gradientStart: '#4F8FF7', gradientEnd: '#7AB8FF', gradText: '#FFFFFF', gradSubText: 'rgba(255,255,255,.85)',
        iconStyle: 'line', iconColor: '#4F8FF7', isDark: false
    },
    theme_vivid: {
        key: 'theme_vivid', name: '活力渐变风',
        primaryColor: '#F5455C', accentColor: '#FFD166', bgColor: '#FFF1F0',
        pageBg: '#FFF1F0', cardBg: '#FFFFFF', text: '#2B2B2B', textSub: '#8A6E6E',
        brand: '#F5455C', brandSolid: '#F5455C', brand2: '#FFD166', brandWeak: 'rgba(245,69,92,.10)',
        danger: '#E54545', border: 'rgba(43,43,43,.08)', borderHi: 'rgba(245,69,92,.35)',
        brandGrad: 'linear-gradient(135deg, #F5455C 0%, #FF7A45 100%)',
        radiusCard: '16px', radiusBtn: '28px', radiusImg: '16px', radiusTag: '12px',
        shadowCard: '0 2px 10px rgba(245,69,92,.10)', shadowBtn: '0 6px 18px rgba(245,69,92,.28)',
        gradientStart: '#F5455C', gradientEnd: '#FF7A45', gradText: '#FFFFFF', gradSubText: 'rgba(255,255,255,.85)',
        iconStyle: 'solid-round', iconColor: '#F5455C', isDark: false
    },
    theme_dark: {
        key: 'theme_dark', name: '暗黑科技风',
        primaryColor: '#5B8FFF', accentColor: '#00E5FF', bgColor: '#0B0F19',
        pageBg: '#0B0F19', cardBg: '#151A26', text: '#E8ECF4', textSub: '#8B95A5',
        brand: '#5B8FFF', brandSolid: '#5B8FFF', brand2: '#00E5FF', brandWeak: 'rgba(91,143,255,.14)',
        danger: '#FF5C5C', border: 'rgba(91,143,255,.25)', borderHi: 'rgba(0,229,255,.55)',
        brandGrad: 'linear-gradient(135deg, #5B8FFF 0%, #00E5FF 100%)',
        radiusCard: '10px', radiusBtn: '24px', radiusImg: '10px', radiusTag: '8px',
        shadowCard: '0 2px 10px rgba(0,0,0,.40)', shadowBtn: '0 0 12px rgba(91,143,255,.45)',
        gradientStart: '#1A2138', gradientEnd: '#0B0F19', gradText: '#FFFFFF', gradSubText: 'rgba(255,255,255,.75)',
        iconStyle: 'solid-glow', iconColor: '#00E5FF', isDark: true
    },
    theme_handdrawn: {
        key: 'theme_handdrawn', name: '温暖手绘风',
        primaryColor: '#FFB347', accentColor: '#FF8A8A', bgColor: '#FFF9F0',
        pageBg: '#FFF9F0', cardBg: '#FFFFFF', text: '#3A3A3A', textSub: '#9E9E9E',
        brand: '#FFB347', brandSolid: '#FFB347', brand2: '#FF8A8A', brandWeak: 'rgba(255,179,71,.14)',
        danger: '#E5544B', border: 'rgba(58,58,58,.08)', borderHi: 'rgba(255,179,71,.40)',
        brandGrad: 'none',
        radiusCard: '18px', radiusBtn: '22px', radiusImg: '16px', radiusTag: '14px',
        shadowCard: '0 3px 10px rgba(255,179,71,.14)', shadowBtn: '0 6px 16px rgba(255,179,71,.22)',
        gradientStart: '#FFB347', gradientEnd: '#FF8A8A', gradText: '#7C4A21', gradSubText: 'rgba(124,74,33,.78)',
        iconStyle: 'hand-drawn', iconColor: '#F08A24', isDark: false
    }
}

const DEFAULT_THEME = PRESET_THEMES.theme_fresh

/** 预置人员固定橙色（M27 T27.3：不跟随主题） */
export const PRESET_COLOR = '#FF9800'

/** 按 key 取完整色板（未命中回退默认） */
export function getTheme(themeKey) {
    return PRESET_THEMES[themeKey] || DEFAULT_THEME
}

/** 全部预设主题（T27.5 登录页 4 风格变体渲染用） */
export function listThemes() {
    return Object.keys(PRESET_THEMES).map(k => PRESET_THEMES[k])
}

/** 同步读取缓存主题（首屏立即渲染用；未命中回退默认色板） */
export function getThemeSync() {
    const cached = uni.getStorageSync(THEME_KEY)
    return cached || DEFAULT_THEME
}

/** 拉取并缓存当前租户主题；接口失败 fail-soft 回退本地缓存 */
export function loadTenantTheme() {
    // ★未登录时不发请求：GET /micro/platform/theme/current 需登录态（后端无 @SaIgnore），
    //   匿名调用返回 401 → request 封装会 clearAuth() 跳登录页，
    //   与登录流程页（租户号输入/门店点选/登录页）自身的跳转并发，触发
    //   「reLaunch:fail Navigation cancelled ... with a new navigation」。
    //   此处直接回退本地缓存主题（首屏渲染色板不受影响）。
    const hasToken = !!getToken()
    if (!hasToken) {
        return Promise.resolve(getThemeSync())
    }
    return get(config.api.microThemeCurrent).then(res => {
        const data = (res && res.data) || {}
        const theme = PRESET_THEMES[data.key] || DEFAULT_THEME
        uni.setStorageSync(THEME_KEY, theme)
        return theme
    }).catch(() => {
        return getThemeSync()
    })
}

/**
 * ★T29.5 按 tenantId 加载主题（匿名接口，登录前可用）：
 * 门店选择页选定门店后调用，缓存主题供登录页使用。
 * @param {number} tenantId 租户 ID
 */
export function loadThemeByTenantId(tenantId) {
    if (!tenantId) {
        return Promise.resolve(getThemeSync())
    }
    return get(config.api.microThemeByTenant, { tenantId }).then(res => {
        const data = (res && res.data) || {}
        const theme = PRESET_THEMES[data.key] || DEFAULT_THEME
        uni.setStorageSync(THEME_KEY, theme)
        return theme
    }).catch(() => {
        return getThemeSync()
    })
}

/** 主题渐变背景（CSS linear-gradient，欢迎区/封面占位等） */
export function themeGradient(themeKey) {
    const t = PRESET_THEMES[themeKey] || DEFAULT_THEME
    return `linear-gradient(135deg, ${t.gradientStart} 0%, ${t.gradientEnd} 100%)`
}

/**
 * 主题渐变上的前景文字色（欢迎区标题/渐变占位封面标题等）：
 * 浅色渐变（handdrawn）返回同系深色字，深色/高饱和渐变（fresh/vivid/dark）返回白字，
 * 调用方以 :style 绑定，禁止在 CSS 写死白色（浅色底上不可读）。
 * @returns {{text: string, sub: string}} text=主标题色 sub=副文案色
 */
export function themeGradientText(themeKey) {
    const t = PRESET_THEMES[themeKey] || DEFAULT_THEME
    return { text: t.gradText, sub: t.gradSubText }
}

/** 主题主色（链接/数字/徽标等） */
export function themePrimary(themeKey) {
    const t = PRESET_THEMES[themeKey] || DEFAULT_THEME
    return t.primaryColor
}

/**
 * HEX → rgba（M27 T27.3：默认图容器按主色 20% 透明度铺底）。
 * 支持 #RGB / #RRGGBB；非法值回退透明。
 */
export function hexToRgba(hex, alpha = 1) {
    if (!hex || typeof hex !== 'string') return `rgba(0,0,0,${alpha})`
    let h = hex.replace('#', '').trim()
    if (h.length === 3) h = h.split('').map(c => c + c).join('')
    if (h.length !== 6) return `rgba(0,0,0,${alpha})`
    const r = parseInt(h.slice(0, 2), 16)
    const g = parseInt(h.slice(2, 4), 16)
    const b = parseInt(h.slice(4, 6), 16)
    return `rgba(${r},${g},${b},${alpha})`
}

/** 主题主色的柔和底色（默认图 PRESET 容器铺底，透明底线条 SVG 下透出主题色） */
export function themeSoftBg(themeKey, alpha = 0.2) {
    const t = PRESET_THEMES[themeKey] || DEFAULT_THEME
    return hexToRgba(t.primaryColor, alpha)
}

/**
 * 主题 CSS 变量集（20261016 四主题重设计）：绑定到页面根节点 :style，
 * 变量沿 DOM 继承到全部子元素/子组件（含 scoped 样式）。
 * 同时输出：① 完整设计令牌（--page-bg / --brand / --radius-card …，对齐 theme_tokens.css）
 *          ② 旧版别名（--tp / --tbg / --tgrad / --tradius / --ttext …，兼容已接入页面）。
 */
export function themeCssVars(themeKey) {
    const t = PRESET_THEMES[themeKey] || DEFAULT_THEME
    const grad = `linear-gradient(135deg, ${t.gradientStart} 0%, ${t.gradientEnd} 100%)`
    return {
        /* ---------- 完整设计令牌 ---------- */
        '--page-bg': t.pageBg,
        '--card-bg': t.cardBg,
        '--text': t.text,
        '--text-sub': t.textSub,
        '--brand': t.brand,
        '--brand-solid': t.brandSolid,
        '--brand-2': t.brand2,
        '--brand-weak': t.brandWeak,
        '--brand-grad': t.brandGrad || 'none',
        '--danger': t.danger,
        '--border': t.border,
        '--border-hi': t.borderHi,
        '--radius-card': t.radiusCard,
        '--radius-btn': t.radiusBtn,
        '--radius-img': t.radiusImg,
        '--radius-tag': t.radiusTag,
        '--shadow-card': t.shadowCard,
        '--shadow-btn': t.shadowBtn,
        '--font-title': '28px',
        '--font-sub': '18px',
        '--font-body': '15px',
        '--lh-body': t.key === 'theme_handdrawn' ? '1.7' : '1.6',
        '--gutter': '16px',
        '--icon-style': t.iconStyle,
        '--icon-color': t.iconColor,
        /* ---------- 旧版别名（兼容 4 个已接入页面） ---------- */
        '--tp': t.primaryColor,
        '--tp-soft': t.brandWeak,
        '--tbg': t.bgColor,
        '--tgrad': grad,
        '--tradius': t.radiusCard,
        '--ttext': t.text,
        '--ttext2': t.textSub,
        '--tdark': t.isDark ? '1' : '0'
    }
}

/**
 * 解析当前应渲染的主题 key（M27 T27.5）：
 * URL ?theme=xxx 显式指定（4 风格评审预览）优先，否则回退租户已缓存主题。
 */
export function resolveThemeKey(queryTheme) {
    if (queryTheme && PRESET_THEMES[queryTheme]) return queryTheme
    const sync = getThemeSync() || DEFAULT_THEME
    return sync.key
}
