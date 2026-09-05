<template>
    <view class="page">
        <!-- ========== 基础信息 ========== -->
        <view class="card">
            <view class="form-item">
                <text class="label required">报销金额</text>
                <input class="input amount-input" type="digit" v-model="form.amount" placeholder="0.00" @blur="onAmountChange" />
            </view>
            <picker mode="date" :value="form.expenseDate" @change="e => onDateChange(e.detail.value)">
                <view class="form-item">
                    <text class="label required">费用日期</text>
                    <text class="value">{{ form.expenseDate }}</text>
                    <text class="arrow">›</text>
                </view>
            </picker>
            <view class="form-item">
                <text class="label">报销年月</text>
                <text class="value">{{ form.expensePeriod }}</text>
            </view>
        </view>

        <!-- ========== 费用归类 ========== -->
        <view class="section-title">费用归类</view>
        <view class="card">
            <!-- 物资小类（字典 fee_item_category，映射规则匹配键） -->
            <picker :range="itemCategories" range-key="dictLabel" @change="e => onItemCategory(e.detail.value)">
                <view class="form-item">
                    <text class="label required">物资小类</text>
                    <text class="value" :class="{ placeholder: !itemCategoryText }">{{ itemCategoryText || '请选择' }}</text>
                    <text class="arrow">›</text>
                </view>
            </picker>
            <!-- 用途标签（登录即可接口） -->
            <picker :range="purposeTags" range-key="tagName" @change="e => onPurposeTag(e.detail.value)">
                <view class="form-item">
                    <text class="label required">用途标签</text>
                    <text class="value" :class="{ placeholder: !purposeTagText }">{{ purposeTagText || '请选择' }}</text>
                    <text class="arrow">›</text>
                </view>
            </picker>
        </view>

        <!-- ========== 项目关联（T5.7 硬/软规则） ========== -->
        <view class="section-title">项目关联</view>
        <!-- 硬规则提示：必须关联项目 -->
        <view v-if="projectRequired" class="banner hard">
            <text class="banner-text">⚠️ 按项目关联规则，本次报销必须选择所属项目</text>
        </view>
        <!-- 软提示：建议关联 -->
        <view v-else-if="projectOptional" class="banner soft">
            <text class="banner-text">💡 建议关联所属项目，便于项目利润核算；不选则计入公司级</text>
        </view>
        <view class="card">
            <view class="form-item" @click="showProjectPicker = true">
                <text class="label">{{ projectRequired ? '所属项目' : '所属项目' }}</text>
                <text class="value" :class="{ placeholder: !selectedProject }">
                    {{ selectedProject ? selectedProject.projectName : '公司级（未关联项目）' }}
                </text>
                <text class="arrow">›</text>
            </view>
        </view>

        <!-- ========== 发票后补通道（T5.8） ========== -->
        <view class="section-title">发票</view>
        <view class="card">
            <view class="form-item">
                <view class="label-wrap">
                    <text class="label">发票后补</text>
                    <text class="sub">暂无发票，先报销后补票</text>
                </view>
                <switch :checked="form.invoiceDefer === 1" color="#409eff" @change="e => form.invoiceDefer = e.detail.value ? 1 : 0" />
            </view>
            <view v-if="form.invoiceDefer === 1" class="defer-tip">
                <text class="desc">⏰ 需在 <text class="strong">{{ deferDeadline }}</text> 前补传发票，超期将释放预占额度并标记异常；后补单据需财务审核理由 + 老板必批。</text>
            </view>
        </view>

        <!-- ========== 现场打卡（高级版，FEAT_TIME_CLOCK，T6.2/T8.5） ========== -->
        <view v-if="featClock" class="section-title">现场打卡</view>
        <view v-if="featClock" class="card">
            <view v-if="!clockInfo" class="clock-row">
                <view class="clock-info">
                    <text class="label">现场定位打卡</text>
                    <text class="sub">{{ clockRequired ? '本单命中打卡触发规则，提交前需完成打卡' : '差旅招待/大额报销建议打卡（证据等级 medium）' }}</text>
                </view>
                <view class="clock-btn" @click="doClock"><text class="clock-btn-text">定位打卡</text></view>
            </view>
            <view v-else class="clock-done">
                <text class="label">✅ 已打卡</text>
                <text class="desc">{{ clockInfo.address || '定位成功' }}</text>
                <text class="link" @click="doClock">重新打卡</text>
            </view>
        </view>

        <!-- ========== 大额人脸核身（专业版，FEAT_FACE_RECOGNITION，T7.2/T8.5） ========== -->
        <view v-if="featFace && faceTriggered" class="banner face">
            <text class="banner-text">🧑 大额报销（≥2000）将触发人脸核身，提交时请配合采集</text>
        </view>

        <!-- ========== 备注 ========== -->
        <view class="card">
            <textarea class="remark" v-model="form.remark" placeholder="备注（选填）" maxlength="200" />
        </view>

        <button class="submit" :loading="submitting" @click="onSubmit">提交报销</button>

        <!-- ========== 项目选择弹层 ========== -->
        <view v-if="showProjectPicker" class="mask" @click="showProjectPicker = false">
            <view class="picker-panel" @click.stop>
                <view class="picker-title"><text>选择项目</text></view>
                <scroll-view scroll-y class="picker-scroll">
                    <view class="picker-item" :class="{ active: form.projectId === 0 }" @click="chooseProject(null)">
                        <text class="picker-name">公司级（不关联项目）</text>
                        <text class="desc" v-if="projectRequired">⚠️ 硬规则下不可选</text>
                    </view>
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
import { hasFeature } from '../../utils/features'
import { today, currentPeriod, addDays } from '../../utils/format'

/** 人脸核身金额阈值（专业版默认，T7.2） */
const FACE_THRESHOLD = 2000
/** 打卡触发金额阈值（差旅招待/≥500/历史异常，T6.2 金额维度前端预判） */
const CLOCK_AMOUNT = 500

export default {
    data() {
        return {
            itemCategories: [],
            purposeTags: [],
            itemCategoryText: '',
            purposeTagText: '',
            projects: [],
            selectedProject: null,
            showProjectPicker: false,
            submitting: false,
            clockInfo: null,
            // 关联规则：REQUIRED=硬规则 / OPTIONAL=软提示 / NO_PROMPT=不打扰
            linkRule: null,
            featClock: hasFeature('FEAT_TIME_CLOCK'),
            featFace: hasFeature('FEAT_FACE_RECOGNITION'),
            form: {
                amount: '',
                expenseDate: today(),
                expensePeriod: currentPeriod(),
                itemCategory: '',
                purposeTag: '',
                projectId: 0,
                invoiceDefer: 0,
                remark: ''
            }
        }
    },
    computed: {
        /** 硬规则：必须关联项目（角色级 REQUIRED 或用途标签要求项目） */
        projectRequired() {
            const tag = this.purposeTags.find(t => t.tagCode === this.form.purposeTag)
            return (this.linkRule && this.linkRule.rule === 'REQUIRED') || (!!tag && tag.requireProject === 1)
        },
        /** 软提示：建议关联项目（大类级 OPTIONAL） */
        projectOptional() {
            return !this.projectRequired && !!this.linkRule && this.linkRule.rule === 'OPTIONAL'
        },
        /** 打卡必要性预判：金额≥500（完整规则含差旅招待/历史异常，后端提交时兜底校验） */
        clockRequired() {
            return Number(this.form.amount || 0) >= CLOCK_AMOUNT
        },
        /** 大额人脸触发（≥2000，T7.2） */
        faceTriggered() {
            return Number(this.form.amount || 0) >= FACE_THRESHOLD
        },
        deferDeadline() {
            return addDays(today(), 7)
        }
    },
    onLoad() {
        if (!guardPage('/pages/expense/create')) return
        this.loadOptions()
    },
    methods: {
        /** 并行加载：物资小类字典 / 用途标签 / 进行中项目 / 项目关联规则 */
        async loadOptions() {
            try {
                const [dict, tags, projs, rule] = await Promise.all([
                    get(config.api.dictData + '/fee_item_category').catch(() => ({ data: [] })),
                    get(config.api.purposeTagAll).catch(() => ({ data: [] })),
                    get(config.api.projectActiveList).catch(() => ({ data: [] })),
                    get(config.api.linkRuleMatch).catch(() => ({ data: null }))
                ])
                this.itemCategories = dict.data || []
                this.purposeTags = tags.data || []
                this.projects = projs.data || []
                this.linkRule = rule.data || null
                // 硬规则命中且仅一个可选项目时自动带出
                if (this.linkRule && this.linkRule.rule === 'REQUIRED'
                    && this.linkRule.scope === 'PROJECT' && this.linkRule.target) {
                    const hit = this.projects.find(p => String(p.id) === String(this.linkRule.target))
                    if (hit) this.chooseProject(hit)
                }
            } catch (e) { /* 已统一提示 */ }
        },
        onAmountChange() {
            const num = Number(this.form.amount)
            this.form.amount = isNaN(num) ? '' : num.toFixed(2)
        },
        /** 费用日期变更联动报销年月（对应预算编制期间） */
        onDateChange(date) {
            this.form.expenseDate = date
            this.form.expensePeriod = date.substring(0, 7)
        },
        onItemCategory(index) {
            const hit = this.itemCategories[index]
            if (hit) {
                this.form.itemCategory = hit.dictValue
                this.itemCategoryText = hit.dictLabel
            }
        },
        onPurposeTag(index) {
            const hit = this.purposeTags[index]
            if (hit) {
                this.form.purposeTag = hit.tagCode
                this.purposeTagText = hit.tagName
            }
        },
        chooseProject(p) {
            if (!p && this.projectRequired) {
                uni.showToast({ title: '本单必须关联项目', icon: 'none' })
                return
            }
            this.form.projectId = p ? p.id : 0
            this.selectedProject = p
            this.showProjectPicker = false
        },
        /** 现场打卡：获取定位（高级版，打卡记录随报销单上报） */
        doClock() {
            uni.getLocation({
                type: 'gcj02',
                success: (res) => {
                    this.clockInfo = {
                        longitude: res.longitude,
                        latitude: res.latitude,
                        accuracy: res.accuracy,
                        address: '经度 ' + res.longitude.toFixed(4) + ' / 纬度 ' + res.latitude.toFixed(4)
                    }
                    uni.showToast({ title: '打卡成功', icon: 'success' })
                },
                fail: () => {
                    uni.showToast({ title: '定位失败，请检查小程序位置权限', icon: 'none' })
                }
            })
        },
        /** 提交前校验 */
        validate() {
            const amount = Number(this.form.amount)
            if (isNaN(amount) || amount <= 0) {
                uni.showToast({ title: '请输入大于 0 的报销金额', icon: 'none' })
                return false
            }
            if (!this.form.itemCategory) {
                uni.showToast({ title: '请选择物资小类', icon: 'none' })
                return false
            }
            if (!this.form.purposeTag) {
                uni.showToast({ title: '请选择用途标签', icon: 'none' })
                return false
            }
            if (this.projectRequired && !this.form.projectId) {
                uni.showToast({ title: '按规则必须选择所属项目', icon: 'none' })
                return false
            }
            if (this.featClock && this.clockRequired && !this.clockInfo) {
                uni.showToast({ title: '本单需先完成现场打卡', icon: 'none' })
                return false
            }
            return true
        },
        /** 大额人脸核身（专业版）：不一致仅标记不拦截，采集流程未就绪时提示后可继续 */
        confirmFace() {
            return new Promise((resolve) => {
                if (!this.featFace || !this.faceTriggered) return resolve(true)
                uni.showModal({
                    title: '人脸核身',
                    content: '大额报销（≥2000元）需人脸核身，采集功能部署中，是否先提交？',
                    confirmText: '继续提交',
                    success: (res) => resolve(!!res.confirm)
                })
            })
        },
        /** 提交报销单（创建即提交，后端映射规则自动定位预算科目） */
        async onSubmit() {
            if (this.submitting || !this.validate()) return
            if (!(await this.confirmFace())) return
            this.submitting = true
            try {
                const body = {
                    itemCategory: this.form.itemCategory,
                    purposeTag: this.form.purposeTag,
                    amount: Number(this.form.amount),
                    expenseDate: this.form.expenseDate,
                    expensePeriod: this.form.expensePeriod,
                    projectId: this.form.projectId || 0,
                    invoiceDefer: this.form.invoiceDefer,
                    remark: this.form.remark.trim() || undefined
                }
                if (this.clockInfo) body.clockInfo = this.clockInfo
                const res = await post(config.api.expenseAdd, body)
                // T5.8 后补通道：勾选后补时同步创建补票记录（额度预占，预计补票日期默认 +7 天）
                if (this.form.invoiceDefer === 1 && res.data && res.data.id) {
                    try {
                        await post(config.api.supplementCreate + '?billId=' + res.data.id
                            + '&estimatedDate=' + this.deferDeadline)
                    } catch (e) {
                        uni.showToast({ title: '补票记录创建失败，请在补票页手动发起', icon: 'none' })
                    }
                }
                const msg = this.form.invoiceDefer === 1 ? '已提交（发票后补）' : '提交成功'
                uni.showToast({ title: msg, icon: 'success' })
                setTimeout(() => {
                    uni.switchTab({ url: '/pages/tab-work/index' })
                }, 800)
            } catch (e) { /* 已统一提示 */ }
            this.submitting = false
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 60rpx; }
.section-title { font-size: 30rpx; font-weight: bold; margin: 24rpx 0 16rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 8rpx 28rpx; margin-bottom: 20rpx; }
.form-item { display: flex; align-items: center; padding: 28rpx 0; border-bottom: 1rpx solid #f0f2f5; }
.form-item:last-child { border-bottom: none; }
.label { font-size: 28rpx; color: #303133; flex-shrink: 0; }
.label.required::before { content: '*'; color: #f56c6c; margin-right: 4rpx; }
.label-wrap { flex: 1; }
.sub { font-size: 22rpx; color: #909399; display: block; margin-top: 4rpx; }
.value { flex: 1; font-size: 28rpx; color: #303133; text-align: right; margin-left: 24rpx; }
.value.placeholder { color: #c0c4cc; }
.arrow { color: #c0c4cc; font-size: 32rpx; margin-left: 12rpx; }
.input { flex: 1; font-size: 28rpx; text-align: right; }
.amount-input { font-size: 36rpx; font-weight: bold; }
.banner { border-radius: 12rpx; padding: 16rpx 20rpx; margin-bottom: 16rpx; }
.banner.hard { background: rgba(245, 108, 108, .1); }
.banner.soft { background: rgba(230, 162, 60, .1); }
.banner.face { background: rgba(64, 158, 255, .08); }
.banner-text { font-size: 24rpx; color: #606266; }
.defer-tip { background: #f5f7fa; border-radius: 8rpx; padding: 16rpx; margin-bottom: 20rpx; }
.desc { font-size: 24rpx; color: #909399; display: block; }
.strong { color: #e6a23c; font-weight: bold; }
.clock-row { display: flex; align-items: center; padding: 20rpx 0; }
.clock-info { flex: 1; margin-right: 20rpx; }
.clock-btn { background: #409eff; border-radius: 12rpx; padding: 14rpx 28rpx; flex-shrink: 0; }
.clock-btn-text { color: #fff; font-size: 26rpx; }
.clock-done { padding: 20rpx 0; }
.link { color: #409eff; font-size: 24rpx; margin-top: 8rpx; display: inline-block; }
.remark { width: 100%; box-sizing: border-box; height: 140rpx; font-size: 28rpx; padding: 20rpx 0; }
.submit { margin-top: 24rpx; background: linear-gradient(135deg, #409eff, #5d7ce6); color: #fff; border-radius: 12rpx; font-size: 30rpx; font-weight: bold; }
.mask { position: fixed; left: 0; right: 0; top: 0; bottom: 0; background: rgba(0, 0, 0, .5); z-index: 99; display: flex; align-items: flex-end; }
.picker-panel { width: 100%; background: #fff; border-radius: 24rpx 24rpx 0 0; padding: 24rpx; box-sizing: border-box; }
.picker-title { font-size: 30rpx; font-weight: bold; text-align: center; padding-bottom: 16rpx; border-bottom: 1rpx solid #f0f2f5; }
.picker-scroll { max-height: 600rpx; }
.picker-item { padding: 24rpx 12rpx; border-bottom: 1rpx solid #f5f7fa; }
.picker-item.active .picker-name { color: #409eff; }
.picker-name { font-size: 28rpx; color: #303133; display: block; }
.picker-empty { text-align: center; padding: 40rpx 0; }
</style>
