<template>
  <div class="app-container">
    <el-form :inline="true" size="small" class="mb12">
      <el-form-item label="统计周期">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="yyyy-MM-dd"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :clearable="false"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="loadData">查询</el-button>
      </el-form-item>
    </el-form>

    <el-card shadow="never" class="mb12">
      <div slot="header" class="card-header">
        <span>利润报表（收入 - 支出）</span>
        <span class="period">统计周期：{{ dateRange[0] }} ~ {{ dateRange[1] }}</span>
      </div>
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat">
            <div class="stat-label">总收入</div>
            <div class="stat-value income">￥{{ summary.grossIncome || 0 }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat">
            <div class="stat-label">总支出</div>
            <div class="stat-value expense">￥{{ summary.totalExpense || 0 }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat">
            <div class="stat-label">净利润</div>
            <div class="stat-value profit">￥{{ summary.netProfit || 0 }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat">
            <div class="stat-label">利润率</div>
            <div class="stat-value">{{ profitRate() }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="never">
      <div slot="header"><span>周期内营收趋势</span></div>
      <revenue-trend :data="trend" />
    </el-card>
  </div>
</template>

<script>
import { profitReport, trendReport } from '@/api/micro/report'
import RevenueTrend from '@/views/micro/components/RevenueTrend'

export default {
  name: 'MicroReportProfit',
  components: { RevenueTrend },
  data() {
    return {
      dateRange: [],
      summary: {},
      trend: []
    }
  },
  created() {
    const end = new Date()
    const start = new Date()
    start.setDate(start.getDate() - 29)
    this.dateRange = [this.parseTime(start, '{y}-{m}-{d}'), this.parseTime(end, '{y}-{m}-{d}')]
    this.loadData()
  },
  methods: {
    profitRate() {
      const g = Number(this.summary.grossIncome || 0)
      const n = Number(this.summary.netProfit || 0)
      if (g <= 0) {
        return '—'
      }
      return (n / g * 100).toFixed(1) + '%'
    },
    loadData() {
      if (!this.dateRange || this.dateRange.length !== 2) {
        this.$modal.msgWarning('请选择统计周期')
        return
      }
      const params = { beginDate: this.dateRange[0], endDate: this.dateRange[1] }
      profitReport(params).then(res => {
        this.summary = res.data || {}
      })
      trendReport(params).then(res => {
        this.trend = res.data || []
      })
    }
  }
}
</script>

<style scoped>
.mb12 { margin-bottom: 12px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-header .period { color: #909399; font-size: 13px; }
.stat { text-align: center; padding: 12px 0; }
.stat-label { color: #909399; font-size: 13px; margin-bottom: 10px; }
.stat-value { font-size: 26px; font-weight: 600; color: #303133; }
.stat-value.income { color: #67c23a; }
.stat-value.expense { color: #f56c6c; }
.stat-value.profit { color: #409eff; }
</style>
