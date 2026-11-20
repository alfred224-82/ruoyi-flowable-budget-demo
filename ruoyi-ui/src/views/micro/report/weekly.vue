<template>
  <div class="app-container">
    <el-card shadow="never" class="mb12">
      <div slot="header" class="card-header">
        <span>经营周报</span>
        <span class="period">统计周期：{{ summary.startDate || '—' }} ~ {{ summary.endDate || '—' }}</span>
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
            <div class="stat-label">订单数</div>
            <div class="stat-value">{{ summary.orderCount || 0 }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="never">
      <div slot="header"><span>近 7 天营收趋势</span></div>
      <revenue-trend :data="trend" />
    </el-card>
  </div>
</template>

<script>
import { weeklyReport, trendReport } from '@/api/micro/report'
import RevenueTrend from '@/views/micro/components/RevenueTrend'

export default {
  name: 'MicroReportWeekly',
  components: { RevenueTrend },
  data() {
    return {
      summary: {},
      trend: []
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    loadData() {
      weeklyReport().then(res => {
        this.summary = res.data || {}
        if (this.summary.startDate && this.summary.endDate) {
          trendReport({ beginDate: this.summary.startDate, endDate: this.summary.endDate }).then(t => {
            this.trend = t.data || []
          })
        }
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
