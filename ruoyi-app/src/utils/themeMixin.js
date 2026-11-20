/**
 * 主题混入（20261016 四主题重设计）
 * 统一提供 themeKey / pageStyle / themeClass 与 onShow 自动加载租户主题，
 * 避免每个页面重复主题样板代码。
 *
 * 用法：
 *   import themeMixin from '@/utils/themeMixin'   // 或相对路径 ../../../utils/themeMixin
 *   export default { mixins: [themeMixin], ... }
 *   模板根节点：<view class="page" :class="themeClass" :style="pageStyle">
 *
 * 说明：
 *  - Vue/uni-app 会合并混入与页面的同名生命周期钩子（混入先执行），页面自身 onShow 不受影响。
 *  - 页面若需自行控制主题加载（如登录前按 tenantId 匿名拉取），在 data 中设 themeAutoLoad:false 即可跳过混入的自动加载。
 *  - 支持 URL ?theme=theme_fresh|theme_vivid|theme_dark|theme_handdrawn 显式预览（供 UI 评审选型），显式预览时锁定不再自动切换。
 */
import { getThemeSync, loadTenantTheme, resolveThemeKey, themeCssVars } from './theme'

export default {
    data() {
        return {
            themeKey: resolveThemeKey(null),
            themePinned: null
        }
    },
    computed: {
        /** 页面根节点注入的完整主题 CSS 变量集 */
        pageStyle() {
            return themeCssVars(this.themeKey)
        },
        /** 主题类名（供全局 theme.css 中 .theme-xxx 差异化钩子使用） */
        themeClass() {
            return 'theme-' + this.themeKey
        }
    },
    onLoad(options) {
        if (options && options.theme) {
            this.themePinned = resolveThemeKey(options.theme)
            this.themeKey = this.themePinned
        }
    },
    onShow() {
        // 页面显式关闭自动加载，或处于 ?theme= 预览锁定态时跳过
        if (this.themeAutoLoad === false || this.themePinned) return
        // 首屏先用缓存同步渲染，再拉取最新租户主题
        const cached = getThemeSync()
        if (cached && cached.key) this.themeKey = cached.key
        loadTenantTheme().then(t => {
            if (t && t.key && !this.themePinned) this.themeKey = t.key
        }).catch(() => { /* fail-soft：保留缓存主题 */ })
    }
}
