/**
 * 主题图标资源引用（20261016 四主题重设计）
 * 资源来源：主题样式交付包/stickers/*.svg（已复制到 src/static/theme/stickers/）。
 * SVG 内部使用 fill="currentColor"，配合全局 theme.css 的 .th-ico（CSS mask 方案）即可随主题主色变色。
 *
 * 用法一（组件，推荐）：<theme-icon name="ico-home" size="lg" />
 * 用法二（直接绑定样式）：<text class="th-ico" :style="iconStyle('ico-home')"></text>
 *
 * 说明：本次仅提供接入能力，不强制替换各页面既有的 emoji 图标。
 */

/** 图标资源基路径（uni-app static 目录，H5/小程序均可直接引用） */
export const ICON_BASE = '/static/theme/stickers'

/** 可用图标清单（T31.3 扩充至 21+15=30 个，含设计方案 §十 全部功能图标） */
export const THEME_ICONS = [
    // 底部导航 & 基础（原有 15 个）
    'ico-home', 'ico-activity', 'ico-todo', 'ico-qrcode', 'ico-mine', 'ico-workbench',
    'ico-booking', 'ico-setting', 'ico-feedback', 'ico-logout', 'ico-refresh',
    'ico-location', 'ico-search', 'ico-arrow', 'ico-check',
    // T31.3 新增（设计方案 §十 21 个功能图标中缺失的 15 个）
    'ico-phone-mobile', 'ico-wechat', 'ico-seat', 'ico-delete', 'ico-share',
    'ico-export', 'ico-plus', 'ico-lock', 'ico-unlock', 'ico-edit',
    'ico-calendar', 'ico-amount', 'ico-people', 'ico-report', 'ico-dial'
]

/** 手绘贴纸清单（自带颜色，用 <image> 直引） */
export const THEME_STICKERS = [
    'hand-star', 'hand-bubble', 'hand-cloud', 'hand-heart',
    'hand-sparkle', 'hand-sun', 'hand-flower', 'hand-leaf'
]

/** 图标/贴纸完整路径 */
export function iconUrl(name) {
    return `${ICON_BASE}/${name}.svg`
}

/**
 * 生成 .th-ico 所需的内联样式（注入 --ico 遮罩地址，可选覆盖 --icon-color）。
 * @param {string} name 图标名（如 ico-home）
 * @param {string} [color] 可选，覆盖默认主题取色
 */
export function iconStyle(name, color) {
    const style = { '--ico': `url(${iconUrl(name)})` }
    if (color) style['--icon-color'] = color
    return style
}
