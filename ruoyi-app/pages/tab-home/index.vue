<template>
    <view class="page">
        <!-- ================= 老板：看板摘要 + 入口（5.1 Tab1） ================= -->
        <template v-if="role === ROLES.BOSS">
            <view class="card" @click="go('/pages/dashboard/category')">
                <text class="title">🅰 大类看板</text>
                <text class="desc">总览 / 大类卡片下钻 / 固定成本 / 风险预警</text>
            </view>
            <view class="grid">
                <view class="stat">
                    <text class="num">{{ dash.pendingApprovalCount || 0 }}</text>
                    <text class="label">待审批</text>
                </view>
                <view class="stat">
                    <text class="num">{{ dash.activeProjectCount || 0 }}</text>
                    <text class="label">进行中项目</text>
                </view>
                <view class="stat">
                    <text class="num">{{ formatWan(dash.totalActualProfit) }}</text>
                    <text class="label">实际利润</text>
                </view>
                <view class="stat">
                    <text class="num">{{ formatWan(dash.totalExpense) }}</text>
                    <text class="label">总支出</text>
                </view>
            </view>
            <view v-if="dash.pendingApprovalCount > 0" class="card warn" @click="go('/pages/approval/list')">
                <text class="desc">📋 您有 {{ dash.pendingApprovalCount }} 项审批待处理，点击查看</text>
            </view>
        </template>

        <!-- ================= 项目经理：我的项目 + 收入确认（5.1 Tab1） ================= -->
        <template v-else-if="role === ROLES.MANAGER">
            <view class="card" @click="go('/pages/project/list')">
                <text class="title">🅰 我的项目</text>
                <text class="desc">项目卡片列表（数据限本人项目，后端按成员关系过滤）</text>
            </view>
            <view class="card" @click="go('/pages/project/income')">
                <text class="title">收入确认</text>
                <text class="desc">三态流转：待核验 → 财务核验 → 已核验（待核验不计利润）</text>
            </view>
            <view v-if="pendingIncome > 0" class="card warn" @click="go('/pages/project/income')">
                <text class="desc">⏳ 您有 {{ pendingIncome }} 笔收入确认待财务核验</text>
            </view>
            <view v-if="rejectedBills.length" class="card warn" @click="go('/pages/expense/mine')">
                <text class="desc">❌ 您有 {{ rejectedBills.length }} 张报销单被驳回，请修改后重新提交</text>
            </view>
        </template>

        <!-- ================= 报销人：首页待办（5.1 Tab1，T8.4） ================= -->
        <template v-else-if="role === ROLES.EMPLOYEE">
            <view class="section-title">待办事项</view>
            <view v-if="!todoLoaded" class="card"><text class="desc">加载中…</text></view>
            <template v-else>
                <view v-if="unreadCount > 0" class="card" @click="goMessages">
                    <text class="title">🔔 未读通知 {{ unreadCount }} 条</text>
                    <text class="desc">审批结果 / 补票提醒</text>
                </view>
                <view v-for="bill in rejectedBills" :key="bill.id" class="card warn" @click="go('/pages/expense/mine')">
                    <text class="title">❌ 报销单被驳回：{{ bill.billNo }}</text>
                    <text class="desc">金额 ¥{{ formatAmount(bill.amount) }}　原因：{{ bill.rejectReason || '-' }}</text>
                </view>
                <view v-for="sup in pendingSupplements" :key="sup.id" class="card warn" @click="go('/pages/expense/supplement')">
                    <text class="title">🧾 发票待补传：{{ sup.billNo }}</text>
                    <text class="desc">预占 ¥{{ formatAmount(sup.reservedAmount) }}　预计补票日 {{ formatDate(sup.estimatedDate) }}</text>
                </view>
                <view v-if="unreadCount === 0 && !rejectedBills.length && !pendingSupplements.length" class="card">
                    <text class="desc">✅ 暂无待办，去发起报销吧</text>
                </view>
            </template>
        </template>

        <!-- ================= 财务：审批通知与预警（5.1 备注 / T8.6，不开放业务 Tab） ================= -->
        <template v-else>
            <view class="section-title">
                <text>审批通知 / 预警</text>
                <text class="link" @click="readAll">全部已读</text>
            </view>
            <view v-if="!messages.length" class="card"><text class="desc">暂无通知（重操作请在 PC 端完成）</text></view>
            <view v-for="msg in messages" :key="msg.id" class="card" :class="{ unread: !msg.isRead }" @click="readMsg(msg)">
                <text class="title">{{ msg.title }}</text>
                <text class="desc">{{ msg.content }}</text>
                <text class="time">{{ formatDateTime(msg.createTime) }}</text>
            </view>
        </template>
    </view>
</template>

<script>
import { getPrimaryRole, ROLES } from '../../utils/roles'
import { getToken } from '../../utils/auth'
import { get, put } from '../../utils/request'
import config from '../../config'
import { formatAmount, formatWan, formatDate, formatDateTime } from '../../utils/format'

export default {
    data() {
        return {
            role: '',
            ROLES,
            // 老板看板摘要
            dash: {},
            // 项目经理：待核验收入数
            pendingIncome: 0,
            // 报销人/项目经理：驳回单据 + 待补票
            rejectedBills: [],
            pendingSupplements: [],
            unreadCount: 0,
            todoLoaded: false,
            // 财务：通知列表
            messages: []
        }
    },
    onShow() {
        if (!getToken()) {
            uni.reLaunch({ url: '/pages/login/index' })
            return
        }
        this.role = getPrimaryRole()
        this.loadData()
    },
    methods: {
        formatAmount, formatWan, formatDate, formatDateTime,
        go(url) {
            uni.navigateTo({ url })
        },
        goMessages() {
            // 报销人无独立消息页：用财务同款列表就地提示（消息中心 3114 同源）
            this.loadMessages()
        },
        async loadData() {
            if (this.role === ROLES.BOSS) {
                this.loadDashboard()
            } else if (this.role === ROLES.MANAGER) {
                this.loadManagerTodo()
            } else if (this.role === ROLES.EMPLOYEE) {
                await this.loadEmployeeTodo()
            } else {
                this.loadMessages()
            }
        },
        /** 老板：看板摘要（与 /fee/dashboard 同源，与 PC 看板数据一致） */
        async loadDashboard() {
            try {
                const res = await get(config.api.dashboard)
                this.dash = res.data || {}
            } catch (e) { /* 已统一提示 */ }
        },
        /** 项目经理：待核验收入 + 本人驳回单 */
        async loadManagerTodo() {
            try {
                const [incomeRes, billRes] = await Promise.all([
                    get(config.api.incomeList, { verifyStatus: 'PENDING', pageNum: 1, pageSize: 1 }),
                    get(config.api.expenseList, { status: 'REJECTED', pageNum: 1, pageSize: 5 })
                ])
                this.pendingIncome = (incomeRes && incomeRes.total) || 0
                this.rejectedBills = (billRes && billRes.rows) || []
            } catch (e) { /* 已统一提示 */ }
        },
        /** 报销人：首页待办（未读通知 + 驳回单 + 待补票，T8.4） */
        async loadEmployeeTodo() {
            this.todoLoaded = false
            try {
                const [unreadRes, billRes, supRes] = await Promise.all([
                    get(config.api.messageUnread),
                    get(config.api.expenseList, { status: 'REJECTED', pageNum: 1, pageSize: 5 }),
                    get(config.api.supplementList, { status: 'PENDING', pageNum: 1, pageSize: 5 })
                ])
                this.unreadCount = (unreadRes && unreadRes.data) || 0
                this.rejectedBills = (billRes && billRes.rows) || []
                this.pendingSupplements = (supRes && supRes.rows) || []
            } catch (e) { /* 已统一提示 */ } finally {
                this.todoLoaded = true
            }
        },
        /** 财务：通知预警列表（消息中心 3114 同源，T8.6） */
        async loadMessages() {
            try {
                const res = await get(config.api.messageList, { pageNum: 1, pageSize: 20 })
                this.messages = (res && res.rows) || []
            } catch (e) { /* 已统一提示 */ }
        },
        async readMsg(msg) {
            if (!msg.isRead) {
                try {
                    await put(config.api.messageRead + '/' + msg.id)
                    msg.isRead = 1
                } catch (e) { /* 已统一提示 */ }
            }
        },
        async readAll() {
            try {
                await put(config.api.messageReadAll)
                this.messages.forEach(m => { m.isRead = 1 })
                uni.showToast({ title: '已全部标记为已读', icon: 'none' })
            } catch (e) { /* 已统一提示 */ }
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 32rpx; margin-bottom: 24rpx; }
.card.warn { border-left: 8rpx solid #e6a23c; }
.card.unread { border-left: 8rpx solid #409eff; }
.title { font-size: 32rpx; font-weight: bold; display: block; }
.desc { font-size: 24rpx; color: #909399; margin-top: 12rpx; display: block; }
.time { font-size: 22rpx; color: #c0c4cc; margin-top: 12rpx; display: block; }
.section-title { font-size: 30rpx; font-weight: bold; margin: 8rpx 0 24rpx; display: flex; justify-content: space-between; align-items: center; }
.link { font-size: 24rpx; color: #409eff; font-weight: normal; }
.grid { display: flex; flex-wrap: wrap; margin-bottom: 24rpx; }
.stat { width: 50%; box-sizing: border-box; padding: 12rpx; }
.stat .num { display: block; font-size: 40rpx; font-weight: bold; color: #303133; background: #fff; border-radius: 16rpx 16rpx 0 0; padding: 24rpx 24rpx 0; }
.stat .label { display: block; font-size: 24rpx; color: #909399; background: #fff; border-radius: 0 0 16rpx 16rpx; padding: 8rpx 24rpx 24rpx; }
</style>
