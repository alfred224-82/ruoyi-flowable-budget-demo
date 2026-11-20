<template>
    <view class="block staff-photo-list">
        <view class="block-header">
            <text class="block-title">员工照片墙</text>
        </view>
        <view v-if="loading" class="block-loading">加载中…</view>
        <view v-else-if="!staffList.length" class="block-empty">暂无员工数据</view>
        <view v-else class="photo-grid">
            <view v-for="s in staffList" :key="s.userId" class="photo-item" @click="$emit('select', s)">
                <view class="avatar" :style="{ background: avatarColor(s.nickName || s.userName || '') }">
                    {{ (s.nickName || s.userName || '员').substring(0, 1) }}
                </view>
                <text class="staff-name">{{ s.nickName || s.userName || '员工' }}</text>
                <text class="staff-role" v-if="s.roleName">{{ s.roleName }}</text>
            </view>
        </view>
    </view>
</template>

<script>
/**
 * 区块组件：员工照片墙（T15.16，blockKey=staff_photo_list）
 * 数据源：GET /micro/tenantRole/mappings（复用角色映射接口，取 salesman 角色）
 */
import { get } from '../../utils/request'

export default {
    name: 'BlockStaffPhotoList',
    props: { blockParams: { type: Object, default: () => ({}) } },
    data() { return { loading: false, staffList: [] } },
    created() { this.loadData() },
    methods: {
        avatarColor(name) {
            const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#07c160']
            let hash = 0
            for (let i = 0; i < (name || '').length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash)
            return colors[Math.abs(hash) % colors.length]
        },
        async loadData() {
            this.loading = true
            try {
                const res = await get('/micro/tenantRole/mappings', { roleCode: 'salesman', pageNum: 1, pageSize: 20 })
                this.staffList = (res && res.rows) || []
            } catch (e) { /* 静默降级 */ }
            this.loading = false
        }
    }
}
</script>

<style scoped>
.block { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 20rpx; }
.block-header { margin-bottom: 16rpx; }
.block-title { font-size: 30rpx; font-weight: bold; }
.block-loading, .block-empty { text-align: center; color: #909399; padding: 30rpx 0; font-size: 24rpx; }
.photo-grid { display: flex; flex-wrap: wrap; }
.photo-item { width: 33.33%; text-align: center; padding: 16rpx 0; }
.avatar { width: 80rpx; height: 80rpx; border-radius: 50%; color: #fff; font-size: 32rpx; display: flex; align-items: center; justify-content: center; margin: 0 auto 8rpx; }
.staff-name { font-size: 24rpx; color: #303133; display: block; }
.staff-role { font-size: 20rpx; color: #909399; display: block; }
</style>
