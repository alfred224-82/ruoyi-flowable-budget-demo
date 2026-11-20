<template>
    <!-- ★M36 T36.3 老板端活动结算单页面（§7.11）
         五区布局：① 顶部活动信息（只读）② 收入项列表（默认票面行不可删 + 类型下拉 + 计入分账/利润开关 + 添加收入项）
         ③ 成本项列表（员工成本行只读自动生成 + 其他成本可添加）④ 合计与利润区（实时计算）⑤ 底部操作（保存草稿/提交/调整单/导出CSV）
         ★约束：代收(COLLECTED)不计入收入/利润/分账基数；员工成本快照自动生成不可手填；提交锁定后不可编辑走调整单；退款记负向收入项(需备注) -->
    <view class="page" :class="'theme-' + theme.key" :style="rootStyle">
        <scroll-view class="scroll" scroll-y>
            <view v-if="!loaded" class="empty">加载中…</view>
            <block v-else>
                <!-- 活动选择器 -->
                <view class="card">
                    <text class="card-title">选择要结算的活动</text>
                    <view v-if="!activities.length" class="empty-sm">暂无可结算的活动</view>
                    <view v-for="a in activities" :key="a.bizId" class="act-item"
                          :class="{ active: String(a.bizId) === String(activityId) }"
                          :style="String(a.bizId) === String(activityId) ? { borderColor: theme.primaryColor } : {}"
                          @click="selectActivity(a.bizId)">
                        <view class="act-info">
                            <text class="act-title">{{ a.title || '活动' }}</text>
                            <text class="act-time">{{ fmtTime(a.startTime) }}</text>
                        </view>
                        <text v-if="String(a.bizId) === String(activityId)" class="act-check" :style="{ color: theme.primaryColor }">✓</text>
                    </view>
                </view>

                <block v-if="sd.id">
                    <!-- ① 顶部活动信息（只读） -->
                    <view class="card head-card">
                        <view class="head-top">
                            <text class="title">{{ sd.activityTitle || '活动结算' }}</text>
                            <text class="status-tag" :class="'st-' + (sd.status || 'DRAFT').toLowerCase()">{{ statusText(sd.status) }}</text>
                        </view>
                        <text class="sub">{{ timeText }}</text>
                        <text class="sub">结算单号：{{ sd.settlementNo || '（提交后生成）' }} · 参与人数：{{ sd.participantCount || 0 }}</text>
                        <text v-if="sd.isAdjustment" class="sub adjust-note">📌 本单为调整单（原单已锁定）</text>
                    </view>

                    <!-- ② 收入项列表 -->
                    <view class="card">
                        <view class="card-head">
                            <text class="card-title no-mb">收入项</text>
                            <text v-if="editable" class="link" :style="{ color: theme.primaryColor }" @click="addIncome">+ 添加收入项</text>
                        </view>
                        <view v-for="(it, i) in incomeItems" :key="'in' + i" class="item-row">
                            <view class="item-line1">
                                <input v-if="editable && !it.isDefaultTicket" class="item-name-input" v-model="it.itemName" placeholder="收入项名称" />
                                <text v-else class="item-name">{{ it.itemName }}<text v-if="it.isDefaultTicket" class="default-tag">默认票面</text></text>
                                <!-- 类型下拉 -->
                                <picker v-if="editable" mode="selector" :range="incomeTypeLabels" :value="incomeTypeIndex(it.itemType)"
                                        @change="onTypeChange(i, $event)">
                                    <view class="type-picker" :class="'tp-' + (it.itemType || 'TICKET').toLowerCase()">
                                        {{ incomeTypeText(it.itemType) }} ▾
                                    </view>
                                </picker>
                                <text v-else class="type-tag" :class="'tp-' + (it.itemType || 'TICKET').toLowerCase()">{{ incomeTypeText(it.itemType) }}</text>
                            </view>
                            <view class="item-line2">
                                <view class="num-field">
                                    <text class="num-label">数量</text>
                                    <input class="num-input" type="number" :disabled="!editable" v-model="it.quantity" @input="recalc" />
                                </view>
                                <view class="num-field">
                                    <text class="num-label">单价</text>
                                    <input class="num-input" type="digit" :disabled="!editable" v-model="it.unitPrice" @blur="onUnitPrice(i)" />
                                </view>
                                <view class="num-field">
                                    <text class="num-label">金额</text>
                                    <input class="num-input amount" type="digit" :disabled="!editable" v-model="it.amount" @input="recalc" />
                                </view>
                                <text v-if="editable && !it.isDefaultTicket" class="del-btn" @click="removeIncome(i)">删除</text>
                            </view>
                            <!-- 代收强制不计入；其余可开关 -->
                            <view v-if="it.itemType !== 'COLLECTED'" class="item-line3">
                                <view class="switch-item">
                                    <text class="switch-label">计入分账</text>
                                    <switch :disabled="!editable" :checked="it.includeInCommission == 1" color="#4F8FF7"
                                            @change="it.includeInCommission = $event.detail.value ? 1 : 0" />
                                </view>
                                <view class="switch-item">
                                    <text class="switch-label">计入利润</text>
                                    <switch :disabled="!editable" :checked="it.includeInProfit == 1" color="#4F8FF7"
                                            @change="it.includeInProfit = $event.detail.value ? 1 : 0" />
                                </view>
                            </view>
                            <view v-else class="collected-hint">代收金额不计入收入 / 利润 / 分账基数（仅记现金流）</view>
                            <!-- 退款/负向备注 -->
                            <input v-if="editable && Number(it.amount) < 0" class="remark-input warn" v-model="it.remark" placeholder="负向金额（退款）必填备注说明原因" />
                        </view>
                        <view v-if="!incomeItems.length" class="empty-sm">暂无收入项</view>
                    </view>

                    <!-- ③ 成本项列表 -->
                    <view class="card">
                        <view class="card-head">
                            <text class="card-title no-mb">成本项</text>
                            <text v-if="editable" class="link" :style="{ color: theme.primaryColor }" @click="addCost">+ 添加其他成本</text>
                        </view>
                        <!-- 员工成本（只读自动生成） -->
                        <view v-if="employeeCosts.length" class="cost-group">
                            <text class="group-title">员工成本（快照自动生成，不可手填）</text>
                            <view v-for="(c, i) in employeeCosts" :key="'ec' + i" class="cost-row readonly">
                                <text class="cost-name">{{ c.itemName }}<text v-if="c.staffName" class="staff-name">· {{ c.staffName }}</text></text>
                                <text class="cost-amount">¥{{ fmtMoney(c.amount) }}</text>
                            </view>
                        </view>
                        <!-- 其他成本（可添加） -->
                        <view v-for="(c, i) in otherCosts" :key="'oc' + i" class="cost-row">
                            <input v-if="editable" class="item-name-input" v-model="c.itemName" placeholder="成本名称（如：场地/物料/代付）" />
                            <text v-else class="cost-name">{{ c.itemName }}</text>
                            <input class="num-input amount cost-amt" type="digit" :disabled="!editable" v-model="c.amount" @input="recalc" />
                            <text v-if="editable" class="del-btn" @click="removeCost(i)">删除</text>
                        </view>
                        <view v-if="!employeeCosts.length && !otherCosts.length" class="empty-sm">暂无成本项</view>
                    </view>

                    <!-- ④ 合计与利润区（实时计算） -->
                    <view class="card summary-card">
                        <text class="card-title">合计与利润</text>
                        <view class="sum-grid">
                            <view class="sum-cell"><text class="sum-k">票面收入</text><text class="sum-v">¥{{ fmtMoney(sum.ticket) }}</text></view>
                            <view class="sum-cell"><text class="sum-k">追加收入</text><text class="sum-v">¥{{ fmtMoney(sum.addon) }}</text></view>
                            <view class="sum-cell"><text class="sum-k">代收金额</text><text class="sum-v muted">¥{{ fmtMoney(sum.collected) }}</text></view>
                            <view class="sum-cell"><text class="sum-k">收入合计</text><text class="sum-v strong">¥{{ fmtMoney(sum.totalRevenue) }}</text></view>
                            <view class="sum-cell"><text class="sum-k">员工成本</text><text class="sum-v cost">¥{{ fmtMoney(sum.employee) }}</text></view>
                            <view class="sum-cell"><text class="sum-k">其他成本</text><text class="sum-v cost">¥{{ fmtMoney(sum.other) }}</text></view>
                            <view class="sum-cell"><text class="sum-k">成本合计</text><text class="sum-v cost strong">¥{{ fmtMoney(sum.totalCost) }}</text></view>
                        </view>
                        <view class="profit-line">
                            <text class="profit-label">利润</text>
                            <text class="profit-value" :class="{ negative: sum.profit < 0 }" :style="sum.profit >= 0 ? { color: theme.primaryColor } : {}">¥{{ fmtMoney(sum.profit) }}</text>
                        </view>
                        <view v-if="sd.estimatedProfit != null" class="diff-line">
                            <text class="diff-label">预估利润 ¥{{ fmtMoney(sd.estimatedProfit) }} · 差额</text>
                            <text class="diff-value" :class="{ negative: sum.profitDiff < 0 }">{{ sum.profitDiff >= 0 ? '+' : '' }}¥{{ fmtMoney(sum.profitDiff) }}</text>
                        </view>
                    </view>

                    <!-- 备注 -->
                    <view class="card">
                        <text class="card-title">备注</text>
                        <textarea class="remark-area" :disabled="!editable" v-model="remark" placeholder="结算说明（可选）" />
                    </view>
                </block>
            </block>
        </scroll-view>

        <!-- ⑤ 底部操作区 -->
        <view v-if="loaded && sd.id" class="footer">
            <block v-if="editable">
                <button class="btn-main" :style="{ background: theme.primaryColor }" :disabled="saving" @click="saveDraft">保存草稿</button>
                <button class="btn-sub" :disabled="saving" @click="submitSettlement">提交并生成报表</button>
                <view class="btn-row">
                    <button class="btn-soft gray" @click="exportCsv">导出 CSV</button>
                </view>
            </block>
            <block v-else-if="sd.status === 'SUBMITTED'">
                <text class="foot-hint">已提交，可锁定归档。锁定后如需修改请追加调整单。</text>
                <button class="btn-main" :style="{ background: theme.primaryColor }" :disabled="saving" @click="lockSettlement">锁定结算</button>
                <view class="btn-row"><button class="btn-soft gray" @click="exportCsv">导出 CSV</button></view>
            </block>
            <block v-else>
                <text class="foot-hint">已锁定归档，不可编辑。发现错误请追加调整单（不覆盖原报表）。</text>
                <view class="btn-row">
                    <button class="btn-soft" :style="{ color: theme.primaryColor }" @click="createAdjustment">追加调整单</button>
                    <button class="btn-soft gray" @click="exportCsv">导出 CSV</button>
                </view>
            </block>
        </view>
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get, post } from '../../../utils/request'
import config from '../../../config'
import { applyNavBarTitle } from '../../../utils/roles'
import { getThemeSync, loadTenantTheme, themeCssVars } from '../../../utils/theme'

const INCOME_TYPES = [
    { value: 'TICKET', label: '票面收入' },
    { value: 'ADDON', label: '追加消费' },
    { value: 'COLLECTED', label: '代收金额' }
]

export default {
    data() {
        return {
            theme: getThemeSync(),
            loaded: false,
            saving: false,
            activityId: '',
            activities: [],
            sd: {},
            incomeItems: [],
            otherCosts: [],
            employeeCosts: [],
            remark: '',
            incomeTypeLabels: INCOME_TYPES.map(t => t.label),
            addonReminded: false
        }
    },
    computed: {
        rootStyle() { return themeCssVars(this.theme.key) },
        editable() { return (this.sd.status || 'DRAFT') === 'DRAFT' },
        timeText() {
            const s = this.sd.activityStartTime ? String(this.sd.activityStartTime).replace('T', ' ').substring(0, 16) : ''
            const e = this.sd.activityEndTime ? String(this.sd.activityEndTime).replace('T', ' ').substring(0, 16) : ''
            return s && e ? s + ' ~ ' + e : (s || e || '')
        },
        /** ④ 合计与利润（实时计算，镜像后端 recalculateSummary：代收不计入收入合计） */
        sum() {
            let ticket = 0, addon = 0, collected = 0
            this.incomeItems.forEach(it => {
                const amt = Number(it.amount) || 0
                if (it.itemType === 'TICKET') ticket += amt
                else if (it.itemType === 'ADDON') addon += amt
                else if (it.itemType === 'COLLECTED') collected += amt
            })
            let employee = 0, other = 0
            this.employeeCosts.forEach(c => { employee += Number(c.amount) || 0 })
            this.otherCosts.forEach(c => { other += Number(c.amount) || 0 })
            const totalRevenue = ticket + addon
            const totalCost = employee + other
            const profit = totalRevenue - totalCost
            const est = this.sd.estimatedProfit != null ? Number(this.sd.estimatedProfit) : null
            return {
                ticket, addon, collected, totalRevenue, employee, other, totalCost, profit,
                profitDiff: est != null ? profit - est : null
            }
        }
    },
    onLoad(options) {
        this.activityId = options.activityId || options.id || ''
    },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        applyNavBarTitle()
        this.theme = getThemeSync()
        loadTenantTheme().then(t => { this.theme = t })
        this.loadActivities()
    },
    methods: {
        fmtTime(t) { return t ? String(t).replace('T', ' ').substring(0, 16) : '' },
        fmtMoney(v) { const n = Number(v) || 0; return n.toFixed(2) },
        statusText(s) { return { DRAFT: '草稿', SUBMITTED: '已提交', LOCKED: '已锁定' }[s] || '草稿' },
        incomeTypeText(t) { const f = INCOME_TYPES.find(x => x.value === t); return f ? f.label : '票面收入' },
        incomeTypeIndex(t) { const i = INCOME_TYPES.findIndex(x => x.value === t); return i < 0 ? 0 : i },

        /** 加载老板活动列表 */
        async loadActivities() {
            try {
                const r = await get(config.api.microBossMyActivities)
                const list = (r && r.data) || []
                this.activities = list.map(x => Object.assign({}, x, {
                    bizId: x.bizId || x.id || x.activityId, title: x.title || x.activityTitle
                })).filter(x => x.bizId)
                if (!this.activityId && this.activities.length) this.activityId = this.activities[0].bizId
            } catch (e) { this.activities = [] }
            if (this.activityId) this.loadSettlement()
            else this.loaded = true
        },
        selectActivity(id) { this.activityId = id; this.addonReminded = false; this.loadSettlement() },

        /** 加载结算单（不存在则后端自动初始化草稿） */
        async loadSettlement() {
            if (!this.activityId) return
            this.loaded = false
            try {
                const r = await get(config.api.microSettlementByActivity + this.activityId)
                this.applyVo((r && r.data) || {})
            } catch (e) {
                this.sd = {}
                uni.showToast({ title: '结算单加载失败', icon: 'none' })
            } finally { this.loaded = true }
            this.maybeAddonReminder()
        },
        applyVo(vo) {
            this.sd = vo || {}
            this.remark = vo.remark || ''
            this.incomeItems = (vo.incomeItems || []).map(it => Object.assign({}, it, {
                amount: it.amount != null ? String(it.amount) : '0',
                quantity: it.quantity != null ? String(it.quantity) : '1',
                unitPrice: it.unitPrice != null ? String(it.unitPrice) : '',
                includeInCommission: it.includeInCommission != null ? it.includeInCommission : 1,
                includeInProfit: it.includeInProfit != null ? it.includeInProfit : 1
            }))
            const costs = vo.costItems || []
            this.employeeCosts = costs.filter(c => c.costType === 'EMPLOYEE').map(c => Object.assign({}, c))
            this.otherCosts = costs.filter(c => c.costType !== 'EMPLOYEE').map(c => Object.assign({}, c, {
                amount: c.amount != null ? String(c.amount) : '0'
            }))
        },

        /** 单价×数量→金额联动 */
        onUnitPrice(i) {
            const it = this.incomeItems[i]
            const q = Number(it.quantity) || 0
            const p = Number(it.unitPrice) || 0
            if (it.unitPrice !== '' && it.unitPrice != null) { it.amount = (q * p).toFixed(2) }
            this.recalc()
        },
        recalc() { /* computed sum 自动重算，此处预留手动触发点 */ },
        onTypeChange(i, e) {
            const t = INCOME_TYPES[e.detail.value].value
            this.incomeItems[i].itemType = t
            // 代收强制不计入分账/利润
            if (t === 'COLLECTED') { this.incomeItems[i].includeInCommission = 0; this.incomeItems[i].includeInProfit = 0 }
        },
        addIncome() {
            this.incomeItems.push({
                itemName: '', itemType: 'ADDON', amount: '0', quantity: '1', unitPrice: '',
                includeInCommission: 1, includeInProfit: 1, isDefaultTicket: 0,
                sortOrder: this.incomeItems.length + 1, remark: ''
            })
        },
        removeIncome(i) {
            const it = this.incomeItems[i]
            if (it.isDefaultTicket) { uni.showToast({ title: '默认票面行不可删除', icon: 'none' }); return }
            this.incomeItems.splice(i, 1)
        },
        addCost() {
            this.otherCosts.push({ itemName: '', amount: '0', sortOrder: this.otherCosts.length + 1, remark: '' })
        },
        removeCost(i) { this.otherCosts.splice(i, 1) },

        /** 组装保存 BO */
        buildBo() {
            // 负向金额（退款）必须填备注
            for (const it of this.incomeItems) {
                if (Number(it.amount) < 0 && !(it.remark && it.remark.trim())) {
                    uni.showToast({ title: '退款（负向金额）需填备注', icon: 'none' })
                    return null
                }
                if (!it.itemName || !it.itemName.trim()) {
                    uni.showToast({ title: '收入项名称不能为空', icon: 'none' })
                    return null
                }
            }
            return {
                id: this.sd.id,
                activityId: this.sd.activityId || this.activityId,
                remark: this.remark,
                estimatedProfit: this.sd.estimatedProfit,
                incomeItems: this.incomeItems.map((it, idx) => ({
                    id: it.id, itemName: it.itemName, itemType: it.itemType,
                    amount: Number(it.amount) || 0, quantity: Number(it.quantity) || 1,
                    unitPrice: it.unitPrice !== '' && it.unitPrice != null ? Number(it.unitPrice) : null,
                    includeInCommission: it.itemType === 'COLLECTED' ? 0 : (it.includeInCommission ? 1 : 0),
                    includeInProfit: it.itemType === 'COLLECTED' ? 0 : (it.includeInProfit ? 1 : 0),
                    isDefaultTicket: it.isDefaultTicket || 0, sortOrder: idx + 1, remark: it.remark || ''
                })),
                otherCostItems: this.otherCosts.map((c, idx) => ({
                    id: c.id, itemName: c.itemName || '其他成本', amount: Number(c.amount) || 0,
                    sortOrder: idx + 1, remark: c.remark || ''
                }))
            }
        },

        async saveDraft(silent) {
            const bo = this.buildBo()
            if (!bo) return false
            this.saving = true
            try {
                const r = await post(config.api.microSettlementSaveDraft, bo)
                const id = (r && r.data) || this.sd.id
                if (!silent) uni.showToast({ title: '草稿已保存', icon: 'success' })
                // 重新拉取以同步后端汇总
                const r2 = await get(config.api.microSettlementById + id)
                if (r2 && r2.data) this.applyVo(r2.data)
                return true
            } catch (e) {
                uni.showToast({ title: (e && e.msg) || '保存失败', icon: 'none' })
                return false
            } finally { this.saving = false }
        },

        submitSettlement() {
            uni.showModal({
                title: '提交结算',
                content: '提交后结算单将锁定编辑（如需修改须追加调整单）。确认提交并生成报表？',
                success: async (r) => {
                    if (!r.confirm) return
                    // 先保存草稿再提交
                    const ok = await this.saveDraft(true)
                    if (!ok) return
                    this.saving = true
                    try {
                        await post(config.api.microSettlementSubmit + this.sd.id)
                        uni.showToast({ title: '已提交', icon: 'success' })
                        const r2 = await get(config.api.microSettlementById + this.sd.id)
                        if (r2 && r2.data) this.applyVo(r2.data)
                    } catch (e) {
                        uni.showToast({ title: (e && e.msg) || '提交失败', icon: 'none' })
                    } finally { this.saving = false }
                }
            })
        },

        lockSettlement() {
            uni.showModal({
                title: '锁定结算',
                content: '锁定后归档不可编辑，发现错误只能追加调整单。确认锁定？',
                success: async (r) => {
                    if (!r.confirm) return
                    this.saving = true
                    try {
                        await post(config.api.microSettlementLock + this.sd.id)
                        uni.showToast({ title: '已锁定', icon: 'success' })
                        const r2 = await get(config.api.microSettlementById + this.sd.id)
                        if (r2 && r2.data) this.applyVo(r2.data)
                    } catch (e) {
                        uni.showToast({ title: (e && e.msg) || '锁定失败', icon: 'none' })
                    } finally { this.saving = false }
                }
            })
        },

        createAdjustment() {
            uni.showModal({
                title: '追加调整单',
                content: '将基于原锁定单新建一张调整单（不覆盖原报表）。确认创建？',
                success: async (r) => {
                    if (!r.confirm) return
                    try {
                        const res = await post(config.api.microSettlementAdjust + this.sd.id, {})
                        const newId = res && res.data
                        uni.showToast({ title: '调整单已创建', icon: 'success' })
                        if (newId) {
                            const r2 = await get(config.api.microSettlementById + newId)
                            if (r2 && r2.data) this.applyVo(r2.data)
                        }
                    } catch (e) {
                        uni.showToast({ title: (e && e.msg) || '创建失败', icon: 'none' })
                    }
                }
            })
        },

        /** 导出 CSV（前端本地生成，含 BOM，Excel 不乱码） */
        exportCsv() {
            const rows = []
            rows.push(['活动结算单'])
            rows.push(['结算单号', this.sd.settlementNo || ''])
            rows.push(['活动名称', this.sd.activityTitle || ''])
            rows.push(['状态', this.statusText(this.sd.status)])
            rows.push([])
            rows.push(['【收入项】'])
            rows.push(['名称', '类型', '数量', '单价', '金额', '计入分账', '计入利润'])
            this.incomeItems.forEach(it => rows.push([
                it.itemName, this.incomeTypeText(it.itemType), it.quantity || '', it.unitPrice || '',
                this.fmtMoney(it.amount), it.includeInCommission == 1 ? '是' : '否', it.includeInProfit == 1 ? '是' : '否'
            ]))
            rows.push([])
            rows.push(['【成本项】'])
            rows.push(['名称', '类型', '金额', '员工', '自动生成'])
            this.employeeCosts.forEach(c => rows.push([c.itemName, '员工成本', this.fmtMoney(c.amount), c.staffName || '', '是']))
            this.otherCosts.forEach(c => rows.push([c.itemName, '其他成本', this.fmtMoney(c.amount), '', '否']))
            rows.push([])
            rows.push(['【合计】'])
            rows.push(['票面收入', this.fmtMoney(this.sum.ticket)])
            rows.push(['追加收入', this.fmtMoney(this.sum.addon)])
            rows.push(['代收金额', this.fmtMoney(this.sum.collected)])
            rows.push(['收入合计', this.fmtMoney(this.sum.totalRevenue)])
            rows.push(['员工成本', this.fmtMoney(this.sum.employee)])
            rows.push(['其他成本', this.fmtMoney(this.sum.other)])
            rows.push(['成本合计', this.fmtMoney(this.sum.totalCost)])
            rows.push(['利润', this.fmtMoney(this.sum.profit)])
            this.downloadCsv(rows, (this.sd.activityTitle || '活动') + '_结算单.csv')
        },
        downloadCsv(rows, filename) {
            const csv = rows.map(r => r.map(c => '"' + String(c == null ? '' : c).replace(/"/g, '""') + '"').join(',')).join('\n')
            // #ifdef H5
            const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
            const url = URL.createObjectURL(blob)
            const a = document.createElement('a')
            a.href = url; a.download = filename
            document.body.appendChild(a); a.click(); document.body.removeChild(a)
            URL.revokeObjectURL(url)
            uni.showToast({ title: '已导出', icon: 'success' })
            // #endif
            // #ifndef H5
            uni.setClipboardData({ data: csv, success: () => uni.showToast({ title: 'CSV 已复制到剪贴板', icon: 'none' }) })
            // #endif
        },

        /** ★T36.6 收工时追加消费提醒：追加消费为空 → 提示（不强制填写） */
        maybeAddonReminder() {
            if (this.addonReminded || !this.sd || !this.sd.id) return
            if (!this.editable) return
            if (this.sd.needAddonReminder) {
                this.addonReminded = true
                uni.showModal({
                    title: '收工提醒',
                    content: '有饮料零食等店内消费吗？可在收入项中添加「追加消费」记录（不强制填写）。',
                    confirmText: '去添加',
                    cancelText: '暂不需要',
                    success: (r) => {
                        if (r.confirm) {
                            this.incomeItems.push({
                                itemName: '饮料零食', itemType: 'ADDON', amount: '0', quantity: '1', unitPrice: '',
                                includeInCommission: 1, includeInProfit: 1, isDefaultTicket: 0,
                                sortOrder: this.incomeItems.length + 1, remark: ''
                            })
                        }
                    }
                })
            }
        }
    }
}
</script>

<style scoped>
.page { position: fixed; inset: 0; background: var(--page-bg, #f5f6f8); display: flex; flex-direction: column; }
.scroll { flex: 1; min-height: 0; padding: 24rpx 24rpx 0; box-sizing: border-box; }
.empty { text-align: center; color: var(--text-sub, #909399); font-size: 26rpx; padding: 100rpx 0; }
.empty-sm { text-align: center; color: #c0c4cc; font-size: 24rpx; padding: 24rpx 0; }
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 20rpx); padding: 28rpx 32rpx; margin-bottom: 20rpx; box-shadow: var(--shadow-card, 0 2rpx 12rpx rgba(0,0,0,.04)); }
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16rpx; }
.card-title { font-size: 30rpx; font-weight: bold; color: var(--text, #303133); display: block; margin-bottom: 16rpx; }
.card-title.no-mb { margin-bottom: 0; }
.link { font-size: 24rpx; }

.act-item { display: flex; align-items: center; padding: 18rpx; border: 2rpx solid var(--border, #f0f2f5); border-radius: 16rpx; margin-bottom: 12rpx; }
.act-item.active { background: var(--brand-weak, #ecf5ff); }
.act-info { flex: 1; overflow: hidden; }
.act-title { display: block; font-size: 28rpx; color: var(--text, #1a1a1a); font-weight: 600; }
.act-time { display: block; font-size: 22rpx; color: var(--text-sub, #909399); margin-top: 6rpx; }
.act-check { font-size: 34rpx; font-weight: bold; }

.head-card { padding: 24rpx 32rpx; }
.head-top { display: flex; align-items: center; justify-content: space-between; }
.title { font-size: 32rpx; font-weight: bold; color: var(--text, #1a1a1a); }
.sub { display: block; font-size: 23rpx; color: var(--text-sub, #909399); margin-top: 8rpx; }
.adjust-note { color: #e6a23c; }
.status-tag { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 20rpx; color: #fff; }
.st-draft { background: #909399; }
.st-submitted { background: #4F8FF7; }
.st-locked { background: #67c23a; }

.item-row { padding: 20rpx 0; border-bottom: 1rpx solid var(--border, #f5f6f8); }
.item-row:last-child { border-bottom: none; }
.item-line1 { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12rpx; }
.item-name { font-size: 28rpx; color: var(--text, #1a1a1a); font-weight: 600; }
.default-tag { font-size: 20rpx; color: #4F8FF7; background: var(--brand-weak, #ecf5ff); padding: 2rpx 10rpx; border-radius: 8rpx; margin-left: 10rpx; }
.item-name-input { flex: 1; font-size: 28rpx; color: var(--text, #1a1a1a); border: 1rpx solid var(--border, #e4e7ed); border-radius: 10rpx; padding: 8rpx 14rpx; margin-right: 16rpx; }
.type-picker, .type-tag { font-size: 22rpx; padding: 6rpx 16rpx; border-radius: 20rpx; color: #fff; flex-shrink: 0; }
.tp-ticket { background: #4F8FF7; }
.tp-addon { background: #FFB347; }
.tp-collected { background: #909399; }
.item-line2 { display: flex; align-items: center; gap: 16rpx; }
.num-field { flex: 1; display: flex; flex-direction: column; }
.num-label { font-size: 20rpx; color: var(--text-sub, #909399); margin-bottom: 4rpx; }
.num-input { font-size: 26rpx; border: 1rpx solid var(--border, #e4e7ed); border-radius: 10rpx; padding: 8rpx 12rpx; height: 40rpx; }
.num-input.amount { font-weight: 600; color: var(--text, #1a1a1a); }
.del-btn { font-size: 22rpx; color: #f56c6c; flex-shrink: 0; padding: 0 8rpx; }
.item-line3 { display: flex; gap: 40rpx; margin-top: 12rpx; }
.switch-item { display: flex; align-items: center; gap: 10rpx; }
.switch-label { font-size: 22rpx; color: var(--text-sub, #606266); }
.collected-hint { font-size: 21rpx; color: #e6a23c; margin-top: 10rpx; }
.remark-input { font-size: 22rpx; border: 1rpx solid #f0c36d; background: #fdf6ec; border-radius: 10rpx; padding: 8rpx 14rpx; margin-top: 12rpx; }
.remark-input.warn { color: #e6a23c; }

.cost-group { margin-bottom: 16rpx; }
.group-title { display: block; font-size: 22rpx; color: #e6a23c; margin-bottom: 10rpx; font-weight: 600; }
.cost-row { display: flex; align-items: center; padding: 14rpx 0; border-bottom: 1rpx solid var(--border, #f5f6f8); gap: 16rpx; }
.cost-row.readonly { opacity: 0.9; }
.cost-name { flex: 1; font-size: 26rpx; color: var(--text, #303133); }
.staff-name { font-size: 22rpx; color: var(--text-sub, #909399); margin-left: 8rpx; }
.cost-amount { font-size: 26rpx; font-weight: 600; color: #f56c6c; }
.cost-amt { width: 180rpx; flex: none; text-align: right; }

.summary-card { background: linear-gradient(180deg, var(--card-bg, #fff), var(--brand-weak, #f7faff)); }
.sum-grid { display: flex; flex-wrap: wrap; }
.sum-cell { width: 50%; display: flex; justify-content: space-between; padding: 10rpx 0; box-sizing: border-box; }
.sum-cell:nth-child(odd) { padding-right: 20rpx; }
.sum-cell:nth-child(even) { padding-left: 20rpx; }
.sum-k { font-size: 24rpx; color: var(--text-sub, #606266); }
.sum-v { font-size: 24rpx; color: var(--text, #303133); }
.sum-v.muted { color: #909399; }
.sum-v.cost { color: #f56c6c; }
.sum-v.strong { font-weight: bold; }
.profit-line { display: flex; align-items: center; justify-content: space-between; margin-top: 16rpx; padding-top: 16rpx; border-top: 2rpx dashed var(--border, #e4e7ed); }
.profit-label { font-size: 28rpx; font-weight: bold; color: var(--text, #303133); }
.profit-value { font-size: 40rpx; font-weight: bold; }
.profit-value.negative { color: #f56c6c !important; }
.diff-line { display: flex; align-items: center; justify-content: space-between; margin-top: 8rpx; }
.diff-label { font-size: 22rpx; color: var(--text-sub, #909399); }
.diff-value { font-size: 24rpx; font-weight: 600; color: #67c23a; }
.diff-value.negative { color: #f56c6c; }
.remark-area { width: 100%; height: 120rpx; font-size: 26rpx; border: 1rpx solid var(--border, #e4e7ed); border-radius: 12rpx; padding: 14rpx; box-sizing: border-box; }

.footer { padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom)); background: var(--card-bg, #fff); box-shadow: 0 -2rpx 12rpx rgba(0,0,0,.05); }
.foot-hint { display: block; font-size: 22rpx; color: var(--text-sub, #909399); text-align: center; margin-bottom: 12rpx; line-height: 1.5; }
.btn-main { width: 100%; height: 88rpx; line-height: 88rpx; border-radius: 44rpx; color: #fff; font-size: 30rpx; font-weight: bold; border: none; margin-bottom: 12rpx; }
.btn-main::after { border: none; }
.btn-sub { width: 100%; height: 80rpx; line-height: 80rpx; border-radius: 40rpx; color: var(--text, #303133); font-size: 28rpx; border: 2rpx solid var(--border, #dcdfe6); background: #fff; margin-bottom: 12rpx; }
.btn-sub::after { border: none; }
.btn-row { display: flex; gap: 16rpx; }
.btn-soft { flex: 1; height: 72rpx; line-height: 72rpx; border-radius: 36rpx; font-size: 26rpx; border: 2rpx solid var(--border, #dcdfe6); background: #fff; color: var(--text, #303133); }
.btn-soft.gray { color: var(--text-sub, #606266); }
.btn-soft::after { border: none; }
</style>
