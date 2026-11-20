<template>
    <!-- ★M33 7.10 老板推广码（§7.10）
         复用 M16/M19 已交付的 bossPromo/index.vue 页面（个人二维码 + 拉新进度 + 归属信息）
         本页仅做路由别名，便于老板端 H5 菜单按 §3.4 文档路径直达；onLoad 立即 redirectTo 真实页 -->
    <view class="page">
        <text class="tip">正在跳转…</text>
    </view>
</template>

<script>
export default {
    onLoad() {
        // 路由别名：redirectTo 真实推广码页（替换当前页，避免返回栈出现空壳）
        uni.redirectTo({
            url: '/pages/micro/bossPromo/index',
            fail: () => {
                uni.showToast({ title: '跳转失败', icon: 'none' })
                setTimeout(() => uni.navigateBack(), 800)
            }
        })
    }
}
</script>

<style scoped>
.page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #f5f6f8; }
.tip { font-size: 26rpx; color: #909399; }
</style>
