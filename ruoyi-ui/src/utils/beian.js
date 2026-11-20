/**
 * 备案信息（ICP 备案 + 公安备案）统一读取
 *
 * 配置来源：.env.development / .env.production
 *   VUE_APP_ICP_NUMBER    ICP 备案号
 *   VUE_APP_ICP_LINK      工信部查询地址
 *   VUE_APP_POLICE_NUMBER 公安备案号
 *   VUE_APP_POLICE_LINK   公安备案查询地址
 *   VUE_APP_POLICE_ICON   公安徽章图片（public 下的文件名或完整 URL，留空用内置官方徽章）
 *
 * 手机端同一份内容配置在 ruoyi-app/src/config/index.js 的 icp 字段，两边需保持一致。
 */

// 官方公安徽章，随仓库放在 public/police-badge.png
const DEFAULT_POLICE_ICON = 'police-badge.png'

/** 相对文件名按部署前缀 BASE_URL 拼接（支持子路径部署），http(s) 外链原样返回 */
function resolveIcon(icon) {
  if (!icon) return ''
  return /^(https?:)?\/\//.test(icon) ? icon : process.env.BASE_URL + icon
}

export function getBeianInfo() {
  return {
    icpNumber: process.env.VUE_APP_ICP_NUMBER || '',
    icpLink: process.env.VUE_APP_ICP_LINK || 'https://beian.miit.gov.cn/',
    policeNumber: process.env.VUE_APP_POLICE_NUMBER || '',
    policeLink: process.env.VUE_APP_POLICE_LINK || '',
    policeIcon: resolveIcon(process.env.VUE_APP_POLICE_ICON || DEFAULT_POLICE_ICON)
  }
}
