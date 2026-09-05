<template>
    <view class="page">
        <!-- 老板：项目卡片入口（5.1 Tab2） -->
        <template v-if="role === ROLES.BOSS">
            <view class="card" @click="go('/pages/project/list')">
                <text class="title">🅱 项目</text>
                <text class="desc">项目卡片 / 预算使用 / 利润速览</text>
            </view>
            <view class="card" @click="go('/pages/approval/list')">
                <text class="title">立项审批</text>
                <text class="desc">项目立项 / 付款 / 变更审批统一入口</text>
            </view>
        </template>

        <!-- 项目经理：收入确认（5.1 Tab2） -->
        <template v-else-if="role === ROLES.MANAGER">
            <view class="card" @click="go('/pages/project/income')">
                <text class="title">🅱 收入确认</text>
                <text class="desc">列表 / 新建（三态：待核验 → 财务核验 → 已核验）</text>
            </view>
            <view class="card" @click="go('/pages/expense/create')">
                <text class="title">发起报销</text>
                <text class="desc">项目经理兼任报销人（多角色并集，权限文档 6.6；项目必选-硬规则）</text>
            </view>
        </template>

        <!-- 报销人：发起报销（5.1 Tab2） -->
        <template v-else-if="role === ROLES.EMPLOYEE">
            <view class="card" @click="go('/pages/expense/create')">
                <text class="title">🅱 发起报销</text>
                <text class="desc">新建报销单（含发票后补通道 / 项目选择规则）</text>
            </view>
            <view class="card" @click="go('/pages/expense/supplement')">
                <text class="title">发票补传</text>
                <text class="desc">后补通道单据补传发票（预占转实扣）</text>
            </view>
        </template>

        <!-- 财务：预警查看（不开放业务 Tab，T8.6） -->
        <template v-else>
            <view class="section-title">未读预警</view>
            <view v-if="!messages.length" class="card"><text class="desc">暂无未读预警（重操作请在 PC 端完成）</text></view>
            <view v-for="msg in messages" :key="msg.id" class="card unread" @click="readMsg(msg)">
                <text class="title">{{ msg.title }}</text>
                <text class="desc">{{ msg.content }}</text>
                <text class="time">{{ formatDateTime(msg.createTime) }}</text>
            </view>
        </template>
    </view>
</template>

<script>
import { getPrimaryRole, ROLES } from '../../utils/roles'
import { get, put } from '../../utils/request'
import config from '../../config'
import { formatDateTime } from '../../utils/format'

export default {
    data() {
        return { role: '', ROLES, messages: [] }
    },
    onShow() {
        this.role = getPrimaryRole()
        if (this.role === ROLES.FINANCE || !this.role) {
            this.loadWarnings()
        }
    },
    methods: {
        formatDateTime,
        go(url) {
            uni.navigateTo({ url })
        },
        /** 财务：未读预警/通知（消息中心 3114 同源） */
        async loadWarnings() {
            try {
                const res = await get(config.api.messageList, { isRead: 0, pageNum: 1, pageSize: 20 })
                this.messages = (res && res.rows) || []
            } catch (e) { /* 已统一提示 */ }
        },
        async readMsg(msg) {
            try {
                await put(config.api.messageRead + '/' + msg.id)
                this.messages = this.messages.filter(m => m.id !== msg.id)
            } catch (e) { /* 已统一提示 */ }
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 32rpx; margin-bottom: 24rpx; }
.card.unread { border-left: 8rpx solid #f56c6c; }
.title { font-size: 32rpx; font-weight: bold; display: block; }
.desc { font-size: 24rpx; color: #909399; margin-top: 12rpx; display: block; }
.time { font-size: 22rpx; color: #c0c4cc; margin-top: 12rpx; display: block; }
.section-title { font-size: 30rpx; font-weight: bold; margin: 8rpx 0 24rpx; }
</style>
