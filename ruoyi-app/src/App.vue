<script>
import { applyNavBarTitle } from './utils/roles'
// #ifdef H5
import { applyPwaTheme } from './utils/pwaTheme'
// #endif

export default {
    onLaunch() {
        // ★导航栏标题固定「阿杰小账本」（T29.1）：四类页面跳转完成后统一重写标题
        ;['navigateTo', 'redirectTo', 'reLaunch', 'switchTab'].forEach(api => {
            uni.addInterceptor(api, { complete: () => applyNavBarTitle() })
        })

        // ★入口路由（扫码 scene / 登录态 / 角色分流）统一由首页 pages/micro/tenant/input 的
        //   onLoad/onShow 决策。原因：H5 端框架启动时会先导航到 pages.json 第一页，
        //   若在 onLaunch 内 reLaunch 会与之并发，触发
        //   「reLaunch:fail Navigation cancelled ... with a new navigation」。
    },
    onShow() {
        // 冷启动首屏/后台回前台时刷新导航栏标题「阿杰小账本」（首屏无跳转事件，拦截器覆盖不到）
        applyNavBarTitle()
        // #ifdef H5
        // M27 T27.4：H5 PWA 运行时按租户主题动态注入 theme_color / manifest（fail-soft，不阻断）
        applyPwaTheme()
        // #endif
    }
}
</script>

<style>
/* 20261016 四主题重设计：全局主题组件库（令牌兜底 + 共享 th- 组件类 + 图标/动效）。
   具体色板由页面根节点 :style="pageStyle"（themeMixin）注入并沿 DOM 继承。 */
@import './styles/theme.css';

page {
    background-color: var(--page-bg, #F6F8FC);
    font-size: 28rpx;
    color: var(--text, #1D2129);
}
</style>
