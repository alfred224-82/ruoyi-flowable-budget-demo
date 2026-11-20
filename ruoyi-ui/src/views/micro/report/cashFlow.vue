<template>
  <div class="app-container">
    <el-card shadow="never" class="mb12">
      <div slot="header"><span>资金水位（累计结余）</span></div>
      <div class="balance">
        <div class="balance-label">累计资金结余</div>
        <div class="balance-value">￥{{ summary.cashBalance || 0 }}</div>
        <div class="balance-tip">= 全周期累计收入 − 累计支出（纯记账口径，不含任何在线支付）</div>
      </div>
    </el-card>

    <el-card shadow="never" class="mb12">
      <div slot="header"><span>当日收支</span></div>
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat">
            <div class="stat-label">今日收入</div>
            <div class="stat-value income">￥{{ summary.grossIncome || 0 }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat">
            <div class="stat-label">今日支出</div>
            <div class="stat-value expense">￥{{ summary.totalExpense || 0 }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat">
            <div class="stat-label">今日净利</div>
            <div class="stat-value profit">￥{{ summary.netProfit || 0 }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat">
            <div class="stat-label">今日订单</div>
            <div class="stat-value">{{ summary.orderCount || 0 }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="never">
      <div slot="header"><span>近 30 天资金趋势</span></div>
      <revenue-trend :data="trend" />
    </el-card>
  </div>
</template>

<script>
import { cashPosition, trendReport } from '@/api/micro/report'
import RevenueTrend from '@/views/micro/components/RevenueTrend'

export default {
  name: 'MicroReportCashFlow',
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
      cashPosition().then(res => {
        this.summary = res.data || {}
      })
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 29)
      trendReport({
        beginDate: this.parseTime(start, '{y}-{m}-{d}'),
        endDate: this.parseTime(end, '{y}-{m}-{d}')
      }).then(res => {
        this.trend = res.data || []
      })
    }
  }
}
</script>

<style scoped>
.mb12 { margin-bottom: 12px; }
.balance { text-align: center; padding: 18px 0; }
.balance-label { color: #909399; font-size: 14px; margin-bottom: 12px; }
.balance-value { font-size: 40px; font-weight: 700; color: #409eff; }
.balance-tip { color: #c0c4cc; font-size: 12px; margin-top: 12px; }
.stat { text-align: center; padding: 12px 0; }
.stat-label { color: #909399; font-size: 13px; margin-bottom: 10px; }
.stat-value { font-size: 26px; font-weight: 600; color: #303133; }
.stat-value.income { color: #67c23a; }
.stat-value.expense { color: #f56c6c; }
.stat-value.profit { color: #409eff; }
</style>
