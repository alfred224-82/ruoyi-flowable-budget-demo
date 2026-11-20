<template>
  <div class="app-container">
    <el-alert title="Admin 平台驾驶舱" type="info"
      description="租户大盘 / 系统健康度 / 全局合规风险监控（Admin 专属，跨租户聚合）。" show-icon :closable="false" class="mb16" />

    <div v-loading="loading">
      <!-- ===== 顶部指标卡片 ===== -->
      <el-row :gutter="16" class="mb16">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">租户总数</div>
            <div class="stat-value">{{ data.tenantTotal || 0 }}</div>
            <div class="stat-sub">正常 {{ data.tenantActive || 0 }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">到期/宽限期</div>
            <div class="stat-value" style="color:#e6a23c;">{{ data.tenantExpired || 0 }}</div>
            <div class="stat-sub">30天内到期 {{ data.expiringSoonCount || 0 }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">合规命中（7天）</div>
            <div class="stat-value" style="color:#f56c6c;">{{ data.complianceHits7d || 0 }}</div>
            <div class="stat-sub">待处理 {{ data.compliancePendingCount || 0 }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">系统健康度</div>
            <div class="stat-value">
              <el-tag :type="healthTagType" size="medium">{{ healthLabel }}</el-tag>
            </div>
            <div class="stat-sub">禁用 {{ data.disabledTenantCount || 0 }}</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- ===== 图表行 ===== -->
      <el-row :gutter="16" class="mb16">
        <el-col :span="12">
          <el-card shadow="hover">
            <div slot="header"><span style="font-weight:600;">租户新增趋势（近 7 天）</span></div>
            <div ref="tenantTrendChart" style="height:280px;"></div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="hover">
            <div slot="header"><span style="font-weight:600;">行业分布</span></div>
            <div ref="industryChart" style="height:280px;"></div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-card shadow="hover">
            <div slot="header"><span style="font-weight:600;">版本档位分布</span></div>
            <div ref="versionChart" style="height:280px;"></div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="hover">
            <div slot="header"><span style="font-weight:600;">合规命中趋势（近 7 天）</span></div>
            <div ref="complianceTrendChart" style="height:280px;"></div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { getAdminDashboard } from '@/api/micro/adminDashboard'

export default {
  name: 'AdminDashboard',
  data() {
    return {
      loading: true,
      data: {},
      charts: []
    }
  },
  computed: {
    healthLabel() {
      const map = { HEALTH: '健康', WARNING: '警告', CRITICAL: '严重' }
      return map[this.data.systemHealth] || '未知'
    },
    healthTagType() {
      const map = { HEALTH: 'success', WARNING: 'warning', CRITICAL: 'danger' }
      return map[this.data.systemHealth] || 'info'
    }
  },
  created() { this.loadData() },
  beforeDestroy() { this.charts.forEach(c => c.dispose()) },
  methods: {
    loadData() {
      this.loading = true
      getAdminDashboard().then(res => {
        this.data = res.data || {}
        this.loading = false
        this.$nextTick(() => { this.renderCharts() })
      }).catch(() => { this.loading = false })
    },
    renderCharts() {
      this.renderTenantTrend()
      this.renderIndustryChart()
      this.renderVersionChart()
      this.renderComplianceTrend()
    },
    renderTenantTrend() {
      const trend = this.data.tenantTrend7d || []
      const chart = echarts.init(this.$refs.tenantTrendChart)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: trend.map(i => i.date) },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{ type: 'bar', data: trend.map(i => i.count), itemStyle: { color: '#409EFF' } }],
        grid: { left: 40, right: 20, top: 20, bottom: 30 }
      })
      this.charts.push(chart)
    },
    renderIndustryChart() {
      const dist = this.data.industryDistribution || []
      const chart = echarts.init(this.$refs.industryChart)
      chart.setOption({
        tooltip: { trigger: 'item' },
        series: [{
          type: 'pie', radius: ['40%', '70%'],
          data: dist.map(i => ({ name: i.industry, value: i.count })),
          emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } }
        }]
      })
      this.charts.push(chart)
    },
    renderVersionChart() {
      const dist = this.data.versionDistribution || []
      const versionMap = { micro_lite: '小微免费版', micro_pro: '小微专业版', basic: '基础版', advanced: '高级版', professional: '专业版' }
      const chart = echarts.init(this.$refs.versionChart)
      chart.setOption({
        tooltip: { trigger: 'item' },
        legend: { bottom: 0 },
        series: [{
          type: 'pie', radius: '65%',
          data: dist.map(i => ({ name: versionMap[i.versionType] || i.versionType, value: i.count }))
        }]
      })
      this.charts.push(chart)
    },
    renderComplianceTrend() {
      const trend = this.data.complianceTrend7d || []
      const chart = echarts.init(this.$refs.complianceTrendChart)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: trend.map(i => i.date) },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{ type: 'line', data: trend.map(i => i.count), smooth: true,
          itemStyle: { color: '#f56c6c' }, areaStyle: { color: 'rgba(245,108,108,0.15)' } }],
        grid: { left: 40, right: 20, top: 20, bottom: 30 }
      })
      this.charts.push(chart)
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.stat-card { text-align: center; }
.stat-label { font-size: 14px; color: #909399; margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: bold; color: #303133; }
.stat-sub { font-size: 12px; color: #909399; margin-top: 4px; }
</style>
