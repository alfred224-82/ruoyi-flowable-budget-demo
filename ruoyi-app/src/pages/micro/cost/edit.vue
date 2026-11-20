<template>
    <view class="page">
        <view class="card">
            <view class="form-item">
                <text class="label">支出项名称</text>
                <input class="input" v-model="form.name" placeholder="如：房租、水电费" />
            </view>
            <view class="form-item">
                <text class="label">分类</text>
                <picker :range="categories" range-key="label" @change="onCategoryChange">
                    <view class="picker">{{ form.categoryName || '请选择分类' }}</view>
                </picker>
            </view>
            <view class="form-item">
                <text class="label">金额（元）</text>
                <input class="input" v-model="form.amount" type="digit" placeholder="0.00" />
            </view>
            <view class="form-item">
                <text class="label">生效月份</text>
                <picker mode="date" fields="month" :value="form.effectiveMonth" @change="onMonthChange">
                    <view class="picker">{{ form.effectiveMonth || '请选择月份' }}</view>
                </picker>
            </view>
            <view class="form-item">
                <text class="label">每月重复</text>
                <switch :checked="form.isRecurring === 1" @change="onRecurringChange" />
            </view>
            <view class="form-item">
                <text class="label">备注</text>
                <input class="input" v-model="form.remark" placeholder="选填" />
            </view>
        </view>
        <button class="btn" @click="handleSave">保 存</button>
    </view>
</template>

<script>
import { get, post, put } from '../../../utils/request'
import config from '../../../config'

export default {
    data() {
        return {
            isEdit: false,
            form: { name: '', category: 'other', categoryName: '其他', amount: '', isRecurring: 1, effectiveMonth: '', remark: '' },
            categories: [
                { label: '房租', value: 'rent' },
                { label: '水电', value: 'utility' },
                { label: '员工工资', value: 'staff_salary' },
                { label: '员工提成', value: 'staff_reward' },
                { label: '其他', value: 'other' }
            ]
        }
    },
    onLoad(options) {
        if (options.id) {
            this.isEdit = true
            this.loadDetail(options.id)
        }
    },
    methods: {
        async loadDetail(id) {
            try {
                const res = await get(config.api.microCostList + '/' + id)
                if (res && res.data) {
                    this.form = { ...res.data }
                }
            } catch (e) { /* ignore */ }
        },
        onCategoryChange(e) {
            const item = this.categories[e.detail.value]
            this.form.category = item.value
            this.form.categoryName = item.label
        },
        onMonthChange(e) { this.form.effectiveMonth = e.detail.value },
        onRecurringChange(e) { this.form.isRecurring = e.detail.value ? 1 : 0 },
        async handleSave() {
            if (!this.form.name) return uni.showToast({ title: '请输入名称', icon: 'none' })
            if (!this.form.amount) return uni.showToast({ title: '请输入金额', icon: 'none' })
            if (!this.form.effectiveMonth) return uni.showToast({ title: '请选择月份', icon: 'none' })
            try {
                if (this.isEdit) {
                    await put(config.api.microCostUpdate, this.form)
                } else {
                    await post(config.api.microCostAdd, this.form)
                }
                uni.showToast({ title: '保存成功' })
                setTimeout(() => uni.navigateBack(), 500)
            } catch (e) {
                uni.showToast({ title: '保存失败', icon: 'none' })
            }
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; min-height: 100vh; background: #f5f6f8; }
.card { background: #fff; border-radius: 16rpx; padding: 28rpx; margin-bottom: 24rpx; }
.form-item { display: flex; align-items: center; justify-content: space-between; padding: 24rpx 0; border-bottom: 1rpx solid #f0f2f5; }
.form-item:last-child { border-bottom: none; }
.label { font-size: 28rpx; color: #606266; width: 180rpx; flex-shrink: 0; }
.input { flex: 1; font-size: 28rpx; text-align: right; }
.picker { flex: 1; font-size: 28rpx; text-align: right; color: #409eff; }
.btn { background: #409eff; color: #fff; border-radius: 12rpx; font-size: 32rpx; margin-top: 24rpx; }
</style>
