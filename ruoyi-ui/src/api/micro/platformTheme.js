import request from '@/utils/request'

// platform_admin 小程序主题管理（T15.36）

// 当前租户主题配置 + 预设主题列表
export function getThemeConfig() {
  return request({
    url: '/micro/platform/theme/config',
    method: 'get'
  })
}

// 预设主题列表
export function listThemes() {
  return request({
    url: '/micro/platform/theme/themes',
    method: 'get'
  })
}

// 保存选定主题
export function saveTheme(themeKey) {
  return request({
    url: '/micro/platform/theme/save',
    method: 'post',
    data: themeKey
  })
}

// 当前租户小程序主题色板（M27 T27.4：登录可读、不校验权限，PC Boss 端与三端共用同一端点）
export function getCurrentTheme() {
  return request({
    url: '/micro/platform/theme/current',
    method: 'get'
  })
}
