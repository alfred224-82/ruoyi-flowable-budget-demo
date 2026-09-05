<template>
    <view class="page">
        <view class="card">
            <text class="title">👤 {{ user.nickName || '未登录' }}</text>
            <text class="desc">角色：{{ roleName }}　版本：{{ editionText }}</text>
            <text v-if="org" class="desc">当前组织：{{ org.orgName || org.name }}</text>
        </view>

        <!-- 高级版增量：信用分卡片（5.3，FEAT_CREDIT_SCORE，T8.5；后端就绪前优雅降级） -->
        <view v-if="hasFeature('FEAT_CREDIT_SCORE')" class="card">
            <text class="title">信用分</text>
            <template v-if="credit">
                <text class="credit-num">{{ credit.creditScore }}</text>
                <text class="desc">信用等级：{{ creditLevelText }}</text>
            </template>
            <text v-else class="desc">{{ creditTip }}</text>
        </view>

        <!-- 专业版增量：多组织切换（5.3，FEAT_MULTI_ORG，T8.5） -->
        <view v-if="hasFeature('FEAT_MULTI_ORG')" class="card" @click="switchOrg">
            <text class="title">当前组织</text>
            <text class="desc">{{ org ? (org.orgName || org.name) : '点击选择组织' }}</text>
        </view>

        <button class="btn" @click="handleLogout">退出登录</button>
    </view>
</template>

<script>
import { getUser, clearAuth, getOrg, setOrg } from '../../utils/auth'
import { getPrimaryRole, TAB_CONFIG, ROLES } from '../../utils/roles'
import { hasFeature, getEdition } from '../../utils/features'
import config from '../../config'
import { get, post } from '../../utils/request'

const EDITION_TEXT = { BASIC: '基础版', ADVANCED: '高级版', PROFESSIONAL: '专业版' }
const CREDIT_LEVEL = { EXCELLENT: '优秀（免老板审批/后补14天）', STANDARD: '标准', RESTRICTED: '受限（禁用无发票报销）' }

export default {
    data() {
        return { user: {}, roleName: '', edition: 'BASIC', org: null, credit: null, creditTip: '加载中…' }
    },
    computed: {
        editionText() {
            return EDITION_TEXT[this.edition] || this.edition
        },
        creditLevelText() {
            return CREDIT_LEVEL[this.credit.creditLevel] || this.credit.creditLevel || '-'
        }
    },
    onShow() {
        this.user = getUser()
        const role = getPrimaryRole()
        this.roleName = (TAB_CONFIG[role] || {}).name || role
        this.edition = getEdition()
        this.org = getOrg()
        if (hasFeature('FEAT_CREDIT_SCORE')) {
            this.loadCredit()
        }
    },
    methods: {
        hasFeature,
        /** 本人信用分（后端接口就绪前优雅降级，不影响基础功能） */
        async loadCredit() {
            this.creditTip = '加载中…'
            try {
                const res = await get(config.api.creditMine)
                this.credit = res.data || null
                if (!this.credit) {
                    this.creditTip = '暂无信用分数据'
                }
            } catch (e) {
                this.credit = null
                this.creditTip = '信用分服务即将开通'
            }
        },
        /** 专业版组织切换 */
        async switchOrg() {
            try {
                const res = await get(config.api.orgMyList)
                const orgs = (res && res.data) || []
                if (!orgs.length) {
                    uni.showToast({ title: '暂无可切换的组织', icon: 'none' })
                    return
                }
                uni.showActionSheet({
                    itemList: orgs.map(o => o.orgName || o.name),
                    success: (r) => {
                        setOrg(orgs[r.tapIndex])
                        this.org = orgs[r.tapIndex]
                    }
                })
            } catch (e) {
                // 组织接口未就绪：优雅降级
            }
        },
        async handleLogout() {
            try {
                await post(config.api.logout)
            } catch (e) { /* 忽略 */ }
            clearAuth()
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 32rpx; margin-bottom: 24rpx; }
.title { font-size: 32rpx; font-weight: bold; display: block; }
.desc { font-size: 24rpx; color: #909399; margin-top: 12rpx; display: block; }
.credit-num { font-size: 64rpx; font-weight: bold; color: #409eff; display: block; margin-top: 12rpx; }
.btn { margin-top: 60rpx; background: #fff; color: #f56c6c; border-radius: 12rpx; }
</style>
