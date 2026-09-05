<template>
    <view class="page">
        <!-- 老板：审批（5.1 Tab3） -->
        <template v-if="role === ROLES.BOSS">
            <view class="card" @click="go('/pages/approval/list')">
                <text class="title">📋 审批</text>
                <text class="desc">报销 / 付款 / 变更 / 立项</text>
            </view>
            <view v-if="pendingCount > 0" class="card warn" @click="go('/pages/approval/list')">
                <text class="desc">您有 {{ pendingCount }} 张报销单待审批</text>
            </view>
        </template>

        <!-- 报销人（含项目经理兼任）：我的报销（5.1 Tab3） -->
        <template v-else-if="role === ROLES.EMPLOYEE || role === ROLES.MANAGER">
            <view class="card" @click="go('/pages/expense/mine')">
                <text class="title">📋 我的报销</text>
                <text class="desc">本人单据与审批进度</text>
            </view>
            <view class="card" @click="go('/pages/expense/supplement')">
                <text class="title">🧾 补票</text>
                <text class="desc">后补通道单据发票补传（预占转实扣）</text>
            </view>
        </template>

        <!-- 财务/其他：不开放业务审批（5.1），仅展示说明 -->
        <view v-else class="card">
            <text class="title">📋 暂无待办</text>
            <text class="desc">财务角色小程序端不开放业务操作（权限文档 5.1），重操作请在 PC 端完成</text>
        </view>
    </view>
</template>

<script>
import { getPrimaryRole, ROLES } from '../../utils/roles'
import { get } from '../../utils/request'
import config from '../../config'

export default {
    data() {
        return { role: '', ROLES, pendingCount: 0 }
    },
    onShow() {
        this.role = getPrimaryRole()
        if (this.role === ROLES.BOSS) {
            this.loadPending()
        }
    },
    methods: {
        go(url) {
            uni.navigateTo({ url })
        },
        /** 老板：待审批报销单数量 */
        async loadPending() {
            try {
                const res = await get(config.api.expensePending, { pageNum: 1, pageSize: 1 })
                this.pendingCount = (res && res.total) || 0
            } catch (e) { /* 已统一提示 */ }
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 32rpx; margin-bottom: 24rpx; }
.card.warn { border-left: 8rpx solid #e6a23c; }
.title { font-size: 32rpx; font-weight: bold; display: block; }
.desc { font-size: 24rpx; color: #909399; margin-top: 12rpx; display: block; }
</style>
