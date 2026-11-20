<template>
    <view v-if="hasIcp || hasPolice" class="icp-footer" :class="{ 'is-fixed': fixed }">
        <view v-if="hasIcp" class="icp-item" hover-class="icp-hover" :hover-stay-time="80" @click="openLink(icp.link)">
            <text class="icp-text">{{ icp.number }}</text>
        </view>
        <text v-if="hasIcp && hasPolice" class="icp-sep">|</text>
        <view v-if="hasPolice" class="icp-item" hover-class="icp-hover" :hover-stay-time="80" @click="openLink(icp.policeLink)">
            <image v-if="icp.policeIcon" class="police-badge" :src="icp.policeIcon" mode="aspectFit" />
            <text class="icp-text">{{ icp.policeNumber }}</text>
        </view>
    </view>
</template>

<script>
/**
 * 备案信息页脚（ICP 备案号 + 公安徽章及公安备案号）
 * 数据唯一来源：src/config/index.js 的 icp 配置（改配置即全站生效，页面不硬编码备案号）。
 * 展示位置：手机端登录/入口页底部，与 PC 端 .env.development/.env.production 的
 *          VUE_APP_ICP_NUMBER / VUE_APP_POLICE_NUMBER / VUE_APP_POLICE_ICON 内容保持一致。
 * 点击跳转仅 H5 有效（小程序不支持打开外部网页），小程序端只展示备案文案。
 */
import config from '../../config'

export default {
    name: 'IcpFooter',
    props: {
        // true = 吸附页面底部；false（默认）= 跟随文档流排在内容之后，避免遮挡按钮
        fixed: { type: Boolean, default: false }
    },
    computed: {
        icp() { return config.icp || {} },
        hasIcp() { return !!this.icp.number },
        hasPolice() { return !!this.icp.policeNumber }
    },
    methods: {
        openLink(url) {
            // #ifdef H5
            if (url) window.open(url, '_blank')
            // #endif
        }
    }
}
</script>

<style scoped>
.icp-footer { display: flex; align-items: center; justify-content: center; flex-wrap: wrap; row-gap: 6rpx; padding: 24rpx 0 calc(24rpx + env(safe-area-inset-bottom)); font-size: 22rpx; color: var(--ttext2, #c0c4cc); }
.icp-footer.is-fixed { position: fixed; left: 0; right: 0; bottom: 0; }
.icp-item { display: flex; align-items: center; cursor: pointer; }
.icp-text { color: inherit; }
.icp-sep { margin: 0 12rpx; }
/* 官方公安徽章：原图 62×67，按字号等比缩小与备案号同行 */
.police-badge { width: 30rpx; height: 32rpx; margin-right: 8rpx; flex-shrink: 0; }
.icp-hover { opacity: 0.55; }
</style>
