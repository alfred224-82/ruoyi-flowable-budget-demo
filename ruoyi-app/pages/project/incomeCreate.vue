<template>
    <view class="page">
        <view class="card">
            <!-- 所属项目（必选，数据限本人参与项目） -->
            <view class="form-item" @click="openProjectPicker">
                <text class="label required">所属项目</text>
                <text class="value" :class="{ placeholder: !selectedProject }">
                    {{ selectedProject ? selectedProject.projectName : '请选择进行中项目' }}
                </text>
                <text class="arrow">›</text>
            </view>
            <!-- 确认收入金额 -->
            <view class="form-item">
                <text class="label required">确认收入金额</text>
                <input class="input" type="digit" v-model="form.confirmedAmount" placeholder="请输入金额（>0）" />
            </view>
            <!-- 确认日期 -->
            <picker mode="date" :value="form.confirmDate" @change="e => form.confirmDate = e.detail.value">
                <view class="form-item">
                    <text class="label">确认日期</text>
                    <text class="value">{{ form.confirmDate }}</text>
                    <text class="arrow">›</text>
                </view>
            </picker>
            <!-- 依据类型 -->
            <picker :range="basisOptions" range-key="label" @change="e => form.basisType = basisOptions[e.detail.value].value">
                <view class="form-item">
                    <text class="label">依据类型</text>
                    <text class="value">{{ basisText }}</text>
                    <text class="arrow">›</text>
                </view>
            </picker>
            <!-- 依据编号 -->
            <view class="form-item">
                <text class="label">依据编号</text>
                <input class="input" v-model="form.basisNo" placeholder="合同号/发票号（选填）" />
            </view>
        </view>

        <view class="tip"><text class="desc">💡 提交后进入「待核验」状态，由财务核验通过后才计入已核验收入与利润（三态机制）</text></view>

        <button class="submit" :loading="submitting" @click="onSubmit">提交收入确认</button>

        <!-- ========== 项目选择弹层 ========== -->
        <view v-if="showPicker" class="mask" @click="showPicker = false">
            <view class="picker-panel" @click.stop>
                <view class="picker-title"><text>选择项目</text></view>
                <scroll-view scroll-y class="picker-scroll">
                    <view v-if="!projects.length" class="desc picker-empty">暂无进行中项目</view>
                    <view v-for="p in projects" :key="p.id" class="picker-item"
                        :class="{ active: form.projectId === p.id }" @click="chooseProject(p)">
                        <text class="picker-name">{{ p.projectName }}</text>
                        <text class="desc">{{ p.clientName || '-' }}</text>
                    </view>
                </scroll-view>
            </view>
        </view>
    </view>
</template>

<script>
import { guardPage } from '../../utils/permission'
import { get, post } from '../../utils/request'
import config from '../../config'
import { today } from '../../utils/format'

const BASIS_OPTIONS = [
    { label: '合同', value: 'CONTRACT' },
    { label: '发票', value: 'INVOICE' },
    { label: '回款单', value: 'RECEIPT' },
    { label: '其他', value: 'OTHER' }
]

export default {
    data() {
        return {
            basisOptions: BASIS_OPTIONS,
            projects: [],
            selectedProject: null,
            showPicker: false,
            submitting: false,
            form: {
                projectId: null,
                confirmedAmount: '',
                confirmDate: today(),
                basisType: 'CONTRACT',
                basisNo: ''
            }
        }
    },
    computed: {
        basisText() {
            const hit = BASIS_OPTIONS.find(b => b.value === this.form.basisType)
            return hit ? hit.label : '-'
        }
    },
    onLoad() {
        if (!guardPage('/pages/project/incomeCreate')) return
        this.loadProjects()
    },
    methods: {
        /** 进行中项目（后端按成员过滤，仅本人参与项目可选） */
        async loadProjects() {
            try {
                const res = await get(config.api.projectActiveList)
                this.projects = res.data || []
            } catch (e) { /* 已统一提示 */ }
        },
        openProjectPicker() {
            this.showPicker = true
        },
        chooseProject(p) {
            this.form.projectId = p.id
            this.selectedProject = p
            this.showPicker = false
        },
        /** 提交（后端校验：项目必选、金额>0.01） */
        async onSubmit() {
            if (this.submitting) return
            if (!this.form.projectId) {
                uni.showToast({ title: '请选择所属项目', icon: 'none' })
                return
            }
            const amount = Number(this.form.confirmedAmount)
            if (isNaN(amount) || amount <= 0) {
                uni.showToast({ title: '请输入大于 0 的金额', icon: 'none' })
                return
            }
            this.submitting = true
            try {
                await post(config.api.incomeAdd, {
                    projectId: this.form.projectId,
                    confirmedAmount: amount,
                    confirmDate: this.form.confirmDate,
                    basisType: this.form.basisType,
                    basisNo: this.form.basisNo.trim() || undefined
                })
                uni.showToast({ title: '已提交，待财务核验', icon: 'success' })
                setTimeout(() => uni.navigateBack(), 800)
            } catch (e) { /* 已统一提示 */ }
            this.submitting = false
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 60rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 8rpx 28rpx; }
.form-item { display: flex; align-items: center; padding: 28rpx 0; border-bottom: 1rpx solid #f0f2f5; }
.form-item:last-child { border-bottom: none; }
.label { width: 220rpx; font-size: 28rpx; color: #303133; flex-shrink: 0; }
.label.required::before { content: '*'; color: #f56c6c; margin-right: 4rpx; }
.value { flex: 1; font-size: 28rpx; color: #303133; text-align: right; }
.value.placeholder { color: #c0c4cc; }
.arrow { color: #c0c4cc; font-size: 32rpx; margin-left: 12rpx; }
.input { flex: 1; font-size: 28rpx; text-align: right; }
.tip { margin: 20rpx 8rpx; }
.desc { font-size: 24rpx; color: #909399; display: block; }
.submit { margin-top: 40rpx; background: linear-gradient(135deg, #409eff, #5d7ce6); color: #fff; border-radius: 12rpx; font-size: 30rpx; font-weight: bold; }
.mask { position: fixed; left: 0; right: 0; top: 0; bottom: 0; background: rgba(0, 0, 0, .5); z-index: 99; display: flex; align-items: flex-end; }
.picker-panel { width: 100%; background: #fff; border-radius: 24rpx 24rpx 0 0; padding: 24rpx; box-sizing: border-box; }
.picker-title { font-size: 30rpx; font-weight: bold; text-align: center; padding-bottom: 16rpx; border-bottom: 1rpx solid #f0f2f5; }
.picker-scroll { max-height: 600rpx; }
.picker-item { padding: 24rpx 12rpx; border-bottom: 1rpx solid #f5f7fa; }
.picker-item.active .picker-name { color: #409eff; }
.picker-name { font-size: 28rpx; color: #303133; display: block; }
.picker-empty { text-align: center; padding: 40rpx 0; }
</style>
