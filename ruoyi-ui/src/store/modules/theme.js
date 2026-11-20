import { getCurrentTheme } from '@/api/micro/platformTheme'

/**
 * 小微版主题下发（M27 T27.4：PC Boss 端与小程序 / H5 共用 /micro/platform/theme/current 同一端点）。
 * 读取当前租户主题色板后，将主色 / 柔和底色 / 圆角 / 文字色写入 :root CSS 变量，
 * PC 端各小微页面（座位画布 / 装修预览等）用 var(--micro-theme-*) 消费，保证三端联动一致。
 * 未登录或接口失败时 fail-soft 回退默认色板（theme_fresh 清新简约风），不阻断页面。
 */

// 默认色板（与后端 MicroPlatformThemeController.DEFAULT_THEME_KEY 对齐）
const DEFAULT_THEME = {
  key: 'theme_fresh',
  name: '清新简约风',
  primaryColor: '#4A90E2',
  bgColor: '#F5F9FF',
  gradientStart: '#E3F2FD',
  gradientEnd: '#FFFFFF',
  iconStyle: 'linear',
  radius: '8px',
  isDark: 'false',
  textPrimary: '#1A1A1A',
  textSecondary: '#666666'
}

/** HEX → rgba（默认图容器按主色 20% 透明度铺底，与小程序 theme.js#hexToRgba 一致） */
function hexToRgba(hex, alpha = 1) {
  if (!hex || typeof hex !== 'string') return `rgba(0,0,0,${alpha})`
  let h = hex.replace('#', '').trim()
  if (h.length === 3) h = h.split('').map(c => c + c).join('')
  if (h.length !== 6) return `rgba(0,0,0,${alpha})`
  const r = parseInt(h.slice(0, 2), 16)
  const g = parseInt(h.slice(2, 4), 16)
  const b = parseInt(h.slice(4, 6), 16)
  return `rgba(${r},${g},${b},${alpha})`
}

/** 将主题色板写入 :root CSS 变量，供 PC 端小微页面消费 */
function applyCssVars(theme) {
  if (typeof document === 'undefined') return
  const root = document.documentElement
  const primary = theme.primaryColor || DEFAULT_THEME.primaryColor
  root.style.setProperty('--micro-theme-primary', primary)
  root.style.setProperty('--micro-theme-primary-soft', hexToRgba(primary, 0.2))
  root.style.setProperty('--micro-theme-bg', theme.bgColor || DEFAULT_THEME.bgColor)
  root.style.setProperty('--micro-theme-gradient', `linear-gradient(135deg, ${theme.gradientStart || DEFAULT_THEME.gradientStart} 0%, ${theme.gradientEnd || DEFAULT_THEME.gradientEnd} 100%)`)
  root.style.setProperty('--micro-theme-radius', theme.radius || DEFAULT_THEME.radius)
  root.style.setProperty('--micro-theme-text', theme.textPrimary || DEFAULT_THEME.textPrimary)
  root.style.setProperty('--micro-theme-text-secondary', theme.textSecondary || DEFAULT_THEME.textSecondary)
}

const state = {
  themeKey: DEFAULT_THEME.key,
  theme: { ...DEFAULT_THEME },
  loaded: false
}

const mutations = {
  SET_THEME(state, theme) {
    const t = theme && theme.key ? theme : DEFAULT_THEME
    state.theme = { ...t }
    state.themeKey = t.key
    state.loaded = true
    applyCssVars(t)
  }
}

const actions = {
  /** 拉取当前租户主题并应用；接口失败 fail-soft 回退默认色板 */
  loadCurrentTheme({ commit, state }, force = false) {
    if (state.loaded && !force) return Promise.resolve(state.theme)
    return getCurrentTheme().then(res => {
      const theme = (res && res.data) || DEFAULT_THEME
      commit('SET_THEME', theme)
      return theme
    }).catch(() => {
      commit('SET_THEME', DEFAULT_THEME)
      return DEFAULT_THEME
    })
  },
  /** 保存主题后强制刷新（platform/theme 页面保存成功回调） */
  refreshTheme({ dispatch }) {
    return dispatch('loadCurrentTheme', true)
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
