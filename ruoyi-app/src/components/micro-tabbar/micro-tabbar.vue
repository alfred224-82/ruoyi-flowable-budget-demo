<template>
    <!-- V3.7 小微版底部导航：micro 页面非原生 tabBar 页，用自定义组件 + reLaunch 实现角色化底部 Tab -->
    <view class="micro-tabbar">
        <view v-for="tab in tabs" :key="tab.index" class="tab-item"
              :class="{ active: tab.page === current }" @click="go(tab)">
            <text class="tab-icon">{{ tab.icon }}</text>
            <text class="tab-text">{{ tab.text }}</text>
        </view>
    </view>
</template>

<script>
import { getMicroTabConfig } from '../../utils/roles'

export default {
    name: 'MicroTabbar',
    props: {
        // 当前页面路径（如 /pages/micro/bossHome/index），用于高亮选中 Tab
        current: { type: String, default: '' }
    },
    data() {
        return { tabs: [] }
    },
    created() {
        this.tabs = (getMicroTabConfig().tabs || []).filter(t => t.page)
    },
    methods: {
        go(tab) {
            if (tab.page === this.current) return
            uni.reLaunch({ url: tab.page })
        }
    }
}
</script>

<style scoped>
/* 20261016 四主题重设计：底部导航消费页面根节点注入的主题变量（沿 DOM 继承） */
.micro-tabbar { position: fixed; left: 0; right: 0; bottom: 0; z-index: 900; display: flex; background: var(--card-bg, #fff); border-top: 1rpx solid var(--border, #e4e7ed); padding-bottom: env(safe-area-inset-bottom); }
.tab-item { flex: 1; text-align: center; padding: 10rpx 0 12rpx; }
.tab-icon { font-size: 40rpx; display: block; line-height: 1.2; }
.tab-text { font-size: 22rpx; color: var(--text-sub, #909399); display: block; margin-top: 4rpx; }
.tab-item.active .tab-text { color: var(--brand, #409eff); font-weight: bold; }
/* 主题3：选中态用强调青色 */
.theme-dark .tab-item.active .tab-text { color: var(--brand-2, #00E5FF); }
</style>
