<template>
  <div class="app-container">
    <el-alert
      title="预估 vs 实际报表（M35 T35.7）"
      type="info"
      description="预计利润取自定价页试算（单人售价×预计人数-员工成本-其他成本）；实际利润取自 M36 活动结算单。★M36 未交付前实际利润与差额留空。"
      show-icon
      :closable="false"
      class="mb8"
    />

    <!-- 筛选条 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="80px">
      <el-form-item label="日期范围">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="yyyy-MM-dd"
          style="width: 260px;"
          @change="handleQuery"
        />
      </el-form-item>
      <el-form-item label="活动状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 140px;" @change="handleQuery">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="已结束" value="ENDED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['micro:commissionRule:report']">导出</el-button>
      </el-form-item>
    </el-form>

    <!-- 汇总卡片 -->
    <el-row :gutter="16" class="mb8">
      <el-col :span="6">
        <el-card shadow="never" class="sum-card">
          <div class="sum-label">活动场次</div>
          <div class="sum-value">{{ summary.count }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="sum-card">
          <div class="sum-label">预计利润合计</div>
          <div class="sum-value" style="color:#409eff;">¥ {{ fmt(summary.estimatedProfit) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="sum-card">
          <div class="sum-label">实际利润合计</div>
          <div class="sum-value" style="color:#67c23a;">{{ summary.actualProfit == null ? '-' : '¥ ' + fmt(summary.actualProfit) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="sum-card">
          <div class="sum-label">差额合计</div>
          <div class="sum-value" :style="{ color: diffColor(summary.diff) }">{{ summary.diff == null ? '-' : sign(summary.diff) + '¥ ' + fmt(Math.abs(summary.diff)) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 列表 -->
    <el-table v-loading="loading" :data="dataList" show-summary :summary-method="getSummaries">
      <el-table-column label="活动" align="left" prop="activityTitle" min-width="160" show-overflow-tooltip />
      <el-table-column label="开始时间" align="center" prop="startTime" width="150" />
      <el-table-column label="状态" align="center" width="90">
        <template slot-scope="scope">
          <el-tag size="mini" :type="statusTag(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="预计人数" align="center" prop="expectedHeadcount" width="90" />
      <el-table-column label="实际人数" align="center" prop="actualHeadcount" width="90">
        <template slot-scope="scope">{{ scope.row.actualHeadcount == null ? '-' : scope.row.actualHeadcount }}</template>
      </el-table-column>
      <el-table-column label="单人售价" align="right" width="100">
        <template slot-scope="scope">¥ {{ fmt(scope.row.unitPrice) }}</template>
      </el-table-column>
      <el-table-column label="预计收入" align="right" width="110">
        <template slot-scope="scope">¥ {{ fmt(scope.row.expectedRevenue) }}</template>
      </el-table-column>
      <el-table-column label="员工成本" align="right" width="100">
        <template slot-scope="scope">¥ {{ fmt(scope.row.employeeCost) }}</template>
      </el-table-column>
      <el-table-column label="其他成本" align="right" width="100">
        <template slot-scope="scope">¥ {{ fmt(scope.row.otherCost) }}</template>
      </el-table-column>
      <el-table-column label="预计利润" align="right" width="110">
        <template slot-scope="scope"><span style="color:#409eff;font-weight:600;">¥ {{ fmt(scope.row.estimatedProfit) }}</span></template>
      </el-table-column>
      <el-table-column label="实际利润" align="right" width="110">
        <template slot-scope="scope">
          <span v-if="scope.row.actualProfit != null" style="color:#67c23a;font-weight:600;">¥ {{ fmt(scope.row.actualProfit) }}</span>
          <span v-else style="color:#c0c4cc;">未结算</span>
        </template>
      </el-table-column>
      <el-table-column label="差额" align="right" width="110">
        <template slot-scope="scope">
          <span v-if="scope.row.diff != null" :style="{ color: diffColor(scope.row.diff), fontWeight: 600 }">
            {{ sign(scope.row.diff) }}¥ {{ fmt(Math.abs(scope.row.diff)) }}
          </span>
          <span v-else style="color:#c0c4cc;">-</span>
        </template>
      </el-table-column>
      <el-table-column label="原因" align="left" prop="diffReason" min-width="120" show-overflow-tooltip>
        <template slot-scope="scope">{{ scope.row.diffReason || '-' }}</template>
      </el-table-column>
      <el-table-column label="结算状态" align="center" width="100">
        <template slot-scope="scope">
          <el-tag size="mini" :type="settleTag(scope.row.settlementStatus)">{{ settleLabel(scope.row.settlementStatus) }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import { queryEstimateVsActual } from '@/api/micro/commissionRule'

export default {
  name: 'CommissionRuleReport',
  data() {
    return {
      loading: false,
      dataList: [],
      dateRange: [],
      queryParams: { status: null }
    }
  },
  computed: {
    summary() {
      const s = { count: this.dataList.length, estimatedProfit: 0, actualProfit: null, diff: null }
      let hasActual = false
      let actualSum = 0
      let diffSum = 0
      let hasDiff = false
      this.dataList.forEach(r => {
        s.estimatedProfit += Number(r.estimatedProfit || 0)
        if (r.actualProfit != null) {
          hasActual = true
          actualSum += Number(r.actualProfit)
        }
        if (r.diff != null) {
          hasDiff = true
          diffSum += Number(r.diff)
        }
      })
      s.actualProfit = hasActual ? actualSum : null
      s.diff = hasDiff ? diffSum : null
      return s
    }
  },
  created() {
    this.getList()
  },
  methods: {
    fmt(v) {
      return (v == null ? 0 : Number(v)).toFixed(2)
    },
    sign(v) {
      return Number(v) > 0 ? '+' : (Number(v) < 0 ? '-' : '')
    },
    diffColor(v) {
      if (v == null) return '#909399'
      const n = Number(v)
      if (n > 0) return '#67c23a'
      if (n < 0) return '#f56c6c'
      return '#909399'
    },
    statusLabel(s) {
      return { DRAFT: '草稿', PUBLISHED: '已发布', ENDED: '已结束' }[s] || s || '-'
    },
    statusTag(s) {
      return { DRAFT: 'info', PUBLISHED: 'success', ENDED: '' }[s] || 'info'
    },
    settleLabel(s) {
      return { DRAFT: '草稿', SUBMITTED: '已提交', LOCKED: '已锁定', UNSETTLED: '未结算' }[s] || s || '-'
    },
    settleTag(s) {
      return { DRAFT: 'info', SUBMITTED: 'warning', LOCKED: 'success', UNSETTLED: 'info' }[s] || 'info'
    },
    getList() {
      this.loading = true
      const params = { ...this.queryParams }
      if (this.dateRange && this.dateRange.length === 2) {
        params.beginTime = this.dateRange[0]
        params.endTime = this.dateRange[1]
      }
      queryEstimateVsActual(params).then(res => {
        this.dataList = res.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() { this.getList() },
    resetQuery() {
      this.dateRange = []
      this.queryParams = { status: null }
      this.getList()
    },
    handleExport() {
      const params = { ...this.queryParams }
      if (this.dateRange && this.dateRange.length === 2) {
        params.beginTime = this.dateRange[0]
        params.endTime = this.dateRange[1]
      }
      this.download('micro/commissionRule/estimateVsActual/export', params, `预估vs实际报表_${new Date().getTime()}.xlsx`)
    },
    /** 合计行：仅对数值列求和，差额/实际利润未结算时显示 - */
    getSummaries(param) {
      const { columns, data } = param
      const sums = []
      columns.forEach((col, idx) => {
        if (idx === 0) { sums[idx] = '合计'; return }
        const prop = col.property
        if (['expectedHeadcount', 'actualHeadcount'].includes(prop)) {
          sums[idx] = data.reduce((a, r) => a + Number(r[prop] || 0), 0)
        } else if (['expectedRevenue', 'employeeCost', 'otherCost', 'estimatedProfit'].includes(prop)) {
          sums[idx] = '¥ ' + data.reduce((a, r) => a + Number(r[prop] || 0), 0).toFixed(2)
        } else if (prop === 'actualProfit') {
          const has = data.some(r => r.actualProfit != null)
          sums[idx] = has ? '¥ ' + data.reduce((a, r) => a + Number(r.actualProfit || 0), 0).toFixed(2) : '-'
        } else if (prop === 'diff') {
          const has = data.some(r => r.diff != null)
          sums[idx] = has ? data.reduce((a, r) => a + Number(r.diff || 0), 0).toFixed(2) : '-'
        } else {
          sums[idx] = ''
        }
      })
      return sums
    }
  }
}
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.sum-card { text-align: center; }
.sum-label { font-size: 13px; color: #909399; margin-bottom: 8px; }
.sum-value { font-size: 24px; font-weight: bold; color: #303133; }
</style>
