<template>
  <div class="app-container">
    <!-- 活动选择器 -->
    <el-card shadow="never" style="margin-bottom:12px;">
      <div slot="header">
        <span style="font-weight:600;">活动结算单</span>
        <span style="color:#909399;font-size:12px;margin-left:8px;">选择活动 → 编辑收入/成本项 → 保存草稿/提交/锁定</span>
      </div>
      <el-form :inline="true" size="small" label-width="80px">
        <el-form-item label="选择活动">
          <el-select v-model="activityId" placeholder="请选择要结算的活动" clearable filterable style="width:360px;" @change="onActivityChange">
            <el-option v-for="a in activityList" :key="a.id" :label="a.title || '活动'" :value="a.id">
              <span>{{ a.title || '活动' }}</span>
              <span style="color:#909399;margin-left:8px;font-size:12px;">{{ a.startTime }}</span>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <div v-if="!loaded" v-loading="true" style="min-height:200px;"></div>

    <div v-else-if="sd.id">
      <!-- ① 顶部活动信息（只读） -->
      <el-card shadow="never" style="margin-bottom:12px;">
        <div slot="header" style="display:flex;align-items:center;justify-content:space-between;">
          <span style="font-weight:600;">{{ sd.activityTitle || '活动结算' }}</span>
          <el-tag :type="statusTagType(sd.status)" size="small">{{ statusText(sd.status) }}</el-tag>
        </div>
        <el-row :gutter="16">
          <el-col :span="8"><span style="color:#909399;">活动时间：</span>{{ timeText }}</el-col>
          <el-col :span="8"><span style="color:#909399;">结算单号：</span>{{ sd.settlementNo || '（提交后生成）' }}</el-col>
          <el-col :span="8"><span style="color:#909399;">参与人数：</span>{{ sd.participantCount || 0 }}</el-col>
        </el-row>
        <el-alert v-if="sd.isAdjustment" type="warning" :closable="false" show-icon title="本单为调整单（原单已锁定）" style="margin-top:12px;" />
      </el-card>

      <!-- ② 收入项列表 -->
      <el-card shadow="never" style="margin-bottom:12px;">
        <div slot="header" style="display:flex;align-items:center;justify-content:space-between;">
          <span style="font-weight:600;">收入项</span>
          <el-button v-if="editable" type="text" icon="el-icon-plus" @click="addIncome">添加收入项</el-button>
        </div>
        <el-table :data="incomeItems" border size="small">
          <el-table-column label="名称" min-width="160">
            <template slot-scope="scope">
              <el-input v-if="editable && !scope.row.isDefaultTicket" v-model="scope.row.itemName" placeholder="收入项名称" size="mini" />
              <span v-else>{{ scope.row.itemName }}<el-tag v-if="scope.row.isDefaultTicket" type="info" size="mini" style="margin-left:6px;">默认票面</el-tag></span>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="130">
            <template slot-scope="scope">
              <el-select v-if="editable" v-model="scope.row.itemType" size="mini" @change="onTypeChange(scope.row)">
                <el-option v-for="t in INCOME_TYPES" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
              <el-tag v-else :type="incomeTypeTag(scope.row.itemType)" size="small">{{ incomeTypeText(scope.row.itemType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="数量" width="100">
            <template slot-scope="scope">
              <el-input-number v-if="editable" v-model="scope.row.quantity" :min="0" :precision="0" size="mini" style="width:100%;" @change="recalc" />
              <span v-else>{{ scope.row.quantity }}</span>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="120">
            <template slot-scope="scope">
              <el-input-number v-if="editable" v-model="scope.row.unitPrice" :min="0" :precision="2" size="mini" style="width:100%;" @change="onUnitPrice(scope.row)" />
              <span v-else>{{ fmtMoney(scope.row.unitPrice) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="130">
            <template slot-scope="scope">
              <el-input-number v-if="editable" v-model="scope.row.amount" :min="0" :precision="2" size="mini" style="width:100%;" @change="recalc" />
              <span v-else style="font-weight:600;">¥{{ fmtMoney(scope.row.amount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="计入分账" width="90" align="center">
            <template slot-scope="scope">
              <el-switch v-if="editable && scope.row.itemType !== 'COLLECTED'" v-model="scope.row.includeInCommission" :active-value="1" :inactive-value="0" />
              <span v-else>{{ scope.row.includeInCommission == 1 ? '是' : '否' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="计入利润" width="90" align="center">
            <template slot-scope="scope">
              <el-switch v-if="editable && scope.row.itemType !== 'COLLECTED'" v-model="scope.row.includeInProfit" :active-value="1" :inactive-value="0" />
              <span v-else>{{ scope.row.includeInProfit == 1 ? '是' : '否' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center" fixed="right">
            <template slot-scope="scope">
              <el-button v-if="editable && !scope.row.isDefaultTicket" type="text" style="color:#f56c6c;" @click="removeIncome(scope.$index)">删除</el-button>
              <span v-else style="color:#c0c4cc;">—</span>
            </template>
          </el-table-column>
        </el-table>
        <el-alert v-if="hasCollected" type="info" :closable="false" show-icon title="代收金额不计入收入 / 利润 / 分账基数（仅记现金流）" style="margin-top:8px;" />
        <div v-if="!incomeItems.length" style="text-align:center;color:#c0c4cc;padding:24px 0;">暂无收入项</div>
      </el-card>

      <!-- ③ 成本项列表 -->
      <el-card shadow="never" style="margin-bottom:12px;">
        <div slot="header" style="display:flex;align-items:center;justify-content:space-between;">
          <span style="font-weight:600;">成本项</span>
          <el-button v-if="editable" type="text" icon="el-icon-plus" @click="addCost">添加其他成本</el-button>
        </div>
        <!-- 员工成本（只读自动生成） -->
        <div v-if="employeeCosts.length" style="margin-bottom:12px;">
          <div style="font-size:13px;color:#e6a23c;font-weight:600;margin-bottom:8px;">员工成本（快照自动生成，不可手填）</div>
          <el-table :data="employeeCosts" border size="small">
            <el-table-column label="名称" min-width="200">
              <template slot-scope="scope">{{ scope.row.itemName }}<span v-if="scope.row.staffName" style="color:#909399;margin-left:6px;font-size:12px;">· {{ scope.row.staffName }}</span></template>
            </el-table-column>
            <el-table-column label="说明" min-width="260">
              <template slot-scope="scope"><span style="color:#606266;font-size:12px;">{{ scope.row.remark || '—' }}</span></template>
            </el-table-column>
            <el-table-column label="金额" width="130">
              <template slot-scope="scope"><span style="color:#f56c6c;font-weight:600;">¥{{ fmtMoney(scope.row.amount) }}</span></template>
            </el-table-column>
            <el-table-column label="自动生成" width="100" align="center">
              <template><el-tag type="info" size="mini">是</el-tag></template>
            </el-table-column>
          </el-table>
        </div>
        <!-- 其他成本（可添加） -->
        <el-table v-if="otherCosts.length" :data="otherCosts" border size="small">
          <el-table-column label="名称" min-width="200">
            <template slot-scope="scope">
              <el-input v-if="editable" v-model="scope.row.itemName" placeholder="成本名称（如：场地/物料/代付）" size="mini" />
              <span v-else>{{ scope.row.itemName }}</span>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="150">
            <template slot-scope="scope">
              <el-input-number v-if="editable" v-model="scope.row.amount" :min="0" :precision="2" size="mini" style="width:100%;" @change="recalc" />
              <span v-else style="color:#f56c6c;font-weight:600;">¥{{ fmtMoney(scope.row.amount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template slot-scope="scope">
              <el-button v-if="editable" type="text" style="color:#f56c6c;" @click="removeCost(scope.$index)">删除</el-button>
              <span v-else style="color:#c0c4cc;">—</span>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="!employeeCosts.length && !otherCosts.length" style="text-align:center;color:#c0c4cc;padding:24px 0;">暂无成本项</div>
      </el-card>

      <!-- ④ 合计与利润区 -->
      <el-card shadow="never" style="margin-bottom:12px;background:linear-gradient(180deg,#fff,#f7faff);">
        <div slot="header"><span style="font-weight:600;">合计与利润</span></div>
        <el-row :gutter="16">
          <el-col :span="6"><div class="sum-cell"><span class="sum-k">票面收入</span><span class="sum-v">¥{{ fmtMoney(sum.ticket) }}</span></div></el-col>
          <el-col :span="6"><div class="sum-cell"><span class="sum-k">追加收入</span><span class="sum-v">¥{{ fmtMoney(sum.addon) }}</span></div></el-col>
          <el-col :span="6"><div class="sum-cell"><span class="sum-k">代收金额</span><span class="sum-v" style="color:#909399;">¥{{ fmtMoney(sum.collected) }}</span></div></el-col>
          <el-col :span="6"><div class="sum-cell"><span class="sum-k">收入合计</span><span class="sum-v" style="font-weight:bold;">¥{{ fmtMoney(sum.totalRevenue) }}</span></div></el-col>
        </el-row>
        <el-row :gutter="16" style="margin-top:8px;">
          <el-col :span="6"><div class="sum-cell"><span class="sum-k">员工成本</span><span class="sum-v" style="color:#f56c6c;">¥{{ fmtMoney(sum.employee) }}</span></div></el-col>
          <el-col :span="6"><div class="sum-cell"><span class="sum-k">其他成本</span><span class="sum-v" style="color:#f56c6c;">¥{{ fmtMoney(sum.other) }}</span></div></el-col>
          <el-col :span="6"><div class="sum-cell"><span class="sum-k">成本合计</span><span class="sum-v" style="color:#f56c6c;font-weight:bold;">¥{{ fmtMoney(sum.totalCost) }}</span></div></el-col>
          <el-col :span="6"><div class="sum-cell"><span class="sum-k">利润</span><span class="sum-v" :style="{fontWeight:'bold',color:sum.profit>=0?'#67c23a':'#f56c6c'}">¥{{ fmtMoney(sum.profit) }}</span></div></el-col>
        </el-row>
        <div v-if="sd.estimatedProfit != null" style="margin-top:12px;padding-top:12px;border-top:1px dashed #e4e7ed;display:flex;align-items:center;justify-content:space-between;">
          <span style="font-size:13px;color:#909399;">预估利润 ¥{{ fmtMoney(sd.estimatedProfit) }} · 差额</span>
          <span :style="{fontWeight:'600',color:sum.profitDiff>=0?'#67c23a':'#f56c6c'}">{{ sum.profitDiff >= 0 ? '+' : '' }}¥{{ fmtMoney(sum.profitDiff) }}</span>
        </div>
      </el-card>

      <!-- 备注 -->
      <el-card shadow="never" style="margin-bottom:12px;">
        <div slot="header"><span style="font-weight:600;">备注</span></div>
        <el-input v-if="editable" v-model="remark" type="textarea" :rows="3" placeholder="结算说明（可选）" />
        <span v-else style="color:#606266;">{{ remark || '—' }}</span>
      </el-card>

      <!-- ⑤ 底部操作区 -->
      <el-card shadow="never">
        <!-- DRAFT 状态 -->
        <template v-if="editable">
          <el-button type="primary" icon="el-icon-document" @click="handleSaveDraft" :loading="saving">保存草稿</el-button>
          <el-button type="success" icon="el-icon-upload2" @click="handleSubmit" :loading="saving">提交并生成报表</el-button>
          <el-button icon="el-icon-download" @click="handleExport">导出 CSV</el-button>
        </template>
        <!-- SUBMITTED 状态 -->
        <template v-else-if="sd.status === 'SUBMITTED'">
          <el-alert type="info" :closable="false" show-icon title="已提交，可锁定归档。锁定后如需修改请追加调整单。" style="margin-bottom:12px;" />
          <el-button type="primary" icon="el-icon-lock" @click="handleLock" :loading="saving">锁定结算</el-button>
          <el-button icon="el-icon-download" @click="handleExport">导出 CSV</el-button>
        </template>
        <!-- LOCKED 状态 -->
        <template v-else>
          <el-alert type="success" :closable="false" show-icon title="已锁定归档，不可编辑。发现错误请追加调整单（不覆盖原报表）。" style="margin-bottom:12px;" />
          <el-button type="warning" icon="el-icon-edit" @click="handleAdjustment">追加调整单</el-button>
          <el-button icon="el-icon-download" @click="handleExport">导出 CSV</el-button>
        </template>
      </el-card>
    </div>

    <div v-else style="text-align:center;color:#909399;padding:80px 0;">
      <i class="el-icon-document" style="font-size:48px;color:#c0c4cc;"></i>
      <p style="margin-top:12px;">请选择活动以查看结算单</p>
    </div>
  </div>
</template>

<script>
import { listPromo } from '@/api/micro/promo'
import {
  getSettlementByActivity,
  getSettlement,
  saveDraft,
  submitSettlement,
  lockSettlement,
  createAdjustment,
  exportSettlement,
  needAddonReminder
} from '@/api/micro/settlement'

const INCOME_TYPES = [
  { value: 'TICKET', label: '票面收入' },
  { value: 'ADDON', label: '追加消费' },
  { value: 'COLLECTED', label: '代收金额' }
]

export default {
  name: 'MicroActivitySettlement',
  data() {
    return {
      INCOME_TYPES,
      activityId: null,
      activityList: [],
      loaded: false,
      saving: false,
      sd: {},
      incomeItems: [],
      otherCosts: [],
      employeeCosts: [],
      remark: '',
      addonReminded: false
    }
  },
  computed: {
    editable() { return (this.sd.status || 'DRAFT') === 'DRAFT' },
    timeText() {
      const s = this.sd.activityStartTime || ''
      const e = this.sd.activityEndTime || ''
      return s && e ? s + ' ~ ' + e : (s || e || '')
    },
    hasCollected() { return this.incomeItems.some(it => it.itemType === 'COLLECTED') },
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
  created() {
    // ★不自动查询，等用户选择活动后再加载结算单
    this.loadActivityList()
  },
  methods: {
    fmtMoney(v) { return (Number(v) || 0).toFixed(2) },
    statusText(s) { return { DRAFT: '草稿', SUBMITTED: '已提交', LOCKED: '已锁定' }[s] || '草稿' },
    statusTagType(s) { return { DRAFT: 'info', SUBMITTED: '', LOCKED: 'success' }[s] || 'info' },
    incomeTypeText(t) { const f = INCOME_TYPES.find(x => x.value === t); return f ? f.label : '票面收入' },
    incomeTypeTag(t) { return { TICKET: '', ADDON: 'warning', COLLECTED: 'info' }[t] || 'info' },

    /** 加载活动列表 */
    loadActivityList() {
      listPromo({ pageNum: 1, pageSize: 100 }).then(res => {
        this.activityList = res.rows || []
        // ★从活动设置页跳转带入 activityId 时自动选中
        const qId = this.$route.query.activityId
        if (qId && this.activityList.length) {
          const found = this.activityList.find(a => String(a.id) === String(qId))
          if (found) {
            this.activityId = found.id
            this.loadSettlement(found.id)
          }
        }
      })
    },

    /** 选择活动变化 */
    onActivityChange(id) {
      this.addonReminded = false
      if (!id) { this.sd = {}; this.incomeItems = []; this.otherCosts = []; this.employeeCosts = []; this.loaded = false; return }
      this.loadSettlement(id)
    },

    /** 加载结算单（不存在则后端自动初始化草稿） */
    loadSettlement(activityId) {
      this.loaded = false
      getSettlementByActivity(activityId).then(res => {
        this.applyVo(res.data || {})
        this.loaded = true
        this.maybeAddonReminder()
      }).catch(() => {
        this.sd = {}
        this.incomeItems = []
        this.otherCosts = []
        this.employeeCosts = []
        this.loaded = true
        this.$modal.msgError('结算单加载失败')
      })
    },

    applyVo(vo) {
      this.sd = vo || {}
      this.remark = vo.remark || ''
      this.incomeItems = (vo.incomeItems || []).map(it => ({
        ...it,
        amount: Number(it.amount) || 0,
        quantity: Number(it.quantity) || 1,
        unitPrice: it.unitPrice != null ? Number(it.unitPrice) : undefined,
        includeInCommission: it.includeInCommission != null ? it.includeInCommission : 1,
        includeInProfit: it.includeInProfit != null ? it.includeInProfit : 1
      }))
      const costs = vo.costItems || []
      this.employeeCosts = costs.filter(c => c.costType === 'EMPLOYEE').map(c => ({ ...c }))
      this.otherCosts = costs.filter(c => c.costType !== 'EMPLOYEE').map(c => ({
        ...c,
        amount: Number(c.amount) || 0
      }))
    },

    /** 单价×数量→金额联动 */
    onUnitPrice(row) {
      const q = Number(row.quantity) || 0
      const p = Number(row.unitPrice) || 0
      if (row.unitPrice !== undefined && row.unitPrice !== null) { row.amount = Number((q * p).toFixed(2)) }
      this.recalc()
    },
    recalc() { /* computed sum 自动重算 */ },

    onTypeChange(row) {
      if (row.itemType === 'COLLECTED') {
        row.includeInCommission = 0
        row.includeInProfit = 0
      }
    },

    addIncome() {
      this.incomeItems.push({
        itemName: '', itemType: 'ADDON', amount: undefined, quantity: 1, unitPrice: undefined,
        includeInCommission: 1, includeInProfit: 1, isDefaultTicket: 0,
        sortOrder: this.incomeItems.length + 1, remark: ''
      })
    },
    removeIncome(i) {
      const it = this.incomeItems[i]
      if (it.isDefaultTicket) { this.$modal.msgWarning('默认票面行不可删除'); return }
      this.incomeItems.splice(i, 1)
    },
    addCost() {
      this.otherCosts.push({ itemName: '', amount: undefined, sortOrder: this.otherCosts.length + 1, remark: '' })
    },
    removeCost(i) { this.otherCosts.splice(i, 1) },

    /** 组装保存 BO */
    buildBo() {
      for (const it of this.incomeItems) {
        if (Number(it.amount) < 0 && !(it.remark && it.remark.trim())) {
          this.$modal.msgWarning('退款（负向金额）需填备注')
          return null
        }
        if (!it.itemName || !it.itemName.trim()) {
          this.$modal.msgWarning('收入项名称不能为空')
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
          unitPrice: it.unitPrice != null ? Number(it.unitPrice) : undefined,
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

    /** 保存草稿 */
    handleSaveDraft() {
      const bo = this.buildBo()
      if (!bo) return
      this.saving = true
      saveDraft(bo).then(res => {
        const id = res.data || this.sd.id
        this.$modal.msgSuccess('草稿已保存')
        // 重新拉取以同步后端汇总
        return getSettlement(id)
      }).then(res => {
        if (res.data) this.applyVo(res.data)
      }).catch(() => {
        this.$modal.msgError('保存失败')
      }).finally(() => { this.saving = false })
    },

    /** 提交结算 */
    handleSubmit() {
      this.$modal.confirm('提交后结算单将锁定编辑（如需修改须追加调整单）。确认提交并生成报表？').then(() => {
        // 先保存草稿再提交
        const bo = this.buildBo()
        if (!bo) return this.$modal.msgError('请先完善收入项')
        this.saving = true
        return saveDraft(bo).then(res => {
          const id = res.data || this.sd.id
          return submitSettlement(id)
        })
      }).then(() => {
        this.$modal.msgSuccess('已提交')
        return getSettlement(this.sd.id)
      }).then(res => {
        if (res.data) this.applyVo(res.data)
      }).catch(() => {}).finally(() => { this.saving = false })
    },

    /** 锁定结算 */
    handleLock() {
      this.$modal.confirm('锁定后归档不可编辑，发现错误只能追加调整单。确认锁定？').then(() => {
        this.saving = true
        return lockSettlement(this.sd.id)
      }).then(() => {
        this.$modal.msgSuccess('已锁定')
        return getSettlement(this.sd.id)
      }).then(res => {
        if (res.data) this.applyVo(res.data)
      }).catch(() => {}).finally(() => { this.saving = false })
    },

    /** 追加调整单 */
    handleAdjustment() {
      this.$modal.confirm('将基于原锁定单新建一张调整单（不覆盖原报表）。确认创建？').then(() => {
        return createAdjustment(this.sd.id)
      }).then(res => {
        const newId = res.data
        this.$modal.msgSuccess('调整单已创建')
        if (newId) return getSettlement(newId)
      }).then(res => {
        if (res && res.data) this.applyVo(res.data)
      }).catch(() => {})
    },

    /** 导出 CSV */
    handleExport() {
      exportSettlement(this.sd.id).then(res => {
        const blob = new Blob([res], { type: 'text/csv;charset=utf-8' })
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = (this.sd.activityTitle || '活动') + '_结算单.csv'
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
        window.URL.revokeObjectURL(url)
        this.$modal.msgSuccess('已导出')
      }).catch(() => {
        this.$modal.msgError('导出失败')
      })
    },

    /** ★T36.6 收工时追加消费提醒 */
    maybeAddonReminder() {
      if (this.addonReminded || !this.sd || !this.sd.id) return
      if (!this.editable) return
      needAddonReminder(this.sd.id).then(res => {
        if (res.data) {
          this.addonReminded = true
          this.$modal.confirm('有饮料零食等店内消费吗？可在收入项中添加「追加消费」记录（不强制填写）。', '收工提醒', '去添加', '暂不需要').then(() => {
            this.incomeItems.push({
              itemName: '饮料零食', itemType: 'ADDON', amount: 0, quantity: 1, unitPrice: null,
              includeInCommission: 1, includeInProfit: 1, isDefaultTicket: 0,
              sortOrder: this.incomeItems.length + 1, remark: ''
            })
          }).catch(() => {})
        }
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.sum-cell { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; }
.sum-k { font-size: 13px; color: #606266; }
.sum-v { font-size: 14px; color: #303133; }
</style>
