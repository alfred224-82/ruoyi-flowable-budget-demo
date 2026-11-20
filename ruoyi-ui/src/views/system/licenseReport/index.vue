<template>
  <div class="app-container">
    <el-alert title="License 使用报表（Admin 专属）" type="info"
      description="租户版本分布 / 到期预警 / License 健康度汇总。跨租户聚合数据，仅 Admin 可查看。"
      show-icon :closable="false" class="mb16" />

    <div v-loading="loading">
      <!-- ===== 总览卡片 ===== -->
      <el-row :gutter="16" class="mb16">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">租户总数</div>
            <div class="stat-value">{{ report.tenantTotal || 0 }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">License 总数</div>
            <div class="stat-value">{{ report.licenseTotal || 0 }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">有效 License</div>
            <div class="stat-value" style="color:#67c23a;">{{ report.activeLicenseCount || 0 }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">健康率</div>
            <div class="stat-value" :style="{ color: healthColor }">{{ report.healthRate || 0 }}%</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- ===== 版本分布 + License 状态分布 ===== -->
      <el-row :gutter="16" class="mb16">
        <el-col :span="12">
          <el-card shadow="hover">
            <div slot="header"><b>租户版本分布</b></div>
            <el-table :data="versionDistList" border size="small" :show-header="true">
              <el-table-column label="版本" align="center" prop="label" />
              <el-table-column label="租户数" align="center" prop="count" width="100" />
              <el-table-column label="占比" align="center" width="100">
                <template slot-scope="scope">{{ pct(scope.row.count, report.tenantTotal) }}</template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="hover">
            <div slot="header"><b>License 状态分布</b></div>
            <el-table :data="licenseStatusList" border size="small" :show-header="true">
              <el-table-column label="状态" align="center" prop="label" />
              <el-table-column label="数量" align="center" prop="count" width="100" />
              <el-table-column label="占比" align="center" width="100">
                <template slot-scope="scope">{{ pct(scope.row.count, report.licenseTotal) }}</template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>

      <!-- ===== 到期预警 ===== -->
      <el-card shadow="hover" class="mb16">
        <div slot="header">
          <b>到期预警</b>
          <el-tag :type="report.expiringSoonCount > 0 ? 'danger' : 'success'" size="small" style="margin-left:12px;">
            {{ report.expiringSoonCount || 0 }} 个租户 30 天内到期
          </el-tag>
        </div>
        <el-table :data="report.expiringTenants || []" border size="small"
          :row-class-name="rowClassName">
          <el-table-column label="租户ID" align="center" prop="tenantId" width="80" />
          <el-table-column label="公司名称" align="left" prop="companyName" min-width="160" show-overflow-tooltip />
          <el-table-column label="版本" align="center" width="110">
            <template slot-scope="scope">
              <el-tag size="mini">{{ versionLabel(scope.row.versionType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="到期日" align="center" width="120">
            <template slot-scope="scope">{{ formatDate(scope.row.licenseExpireDate) }}</template>
          </el-table-column>
          <el-table-column label="剩余天数" align="center" width="100">
            <template slot-scope="scope">
              <span :style="{ color: scope.row.remainingDays <= 7 ? '#f56c6c' : '#e6a23c', fontWeight: 700 }">
                {{ scope.row.remainingDays }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="紧急" align="center" width="70">
            <template slot-scope="scope">
              <el-tag v-if="scope.row.urgent" type="danger" size="mini">紧急</el-tag>
              <span v-else style="color:#909399;">—</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!report.expiringTenants || report.expiringTenants.length === 0"
          description="暂无到期预警租户" :image-size="60" />
      </el-card>
    </div>
  </div>
</template>

<script>
import { getLicenseReport } from '@/api/system/licenseReport'

export default {
  name: 'LicenseReport',
  data() {
    return {
      loading: true,
      report: {}
    }
  },
  computed: {
    healthColor() {
      const r = this.report.healthRate || 0
      if (r >= 80) return '#67c23a'
      if (r >= 50) return '#e6a23c'
      return '#f56c6c'
    },
    versionDistList() {
      const dist = this.report.versionDistribution || {}
      return Object.entries(dist).map(([key, count]) => ({
        label: this.versionLabel(key),
        key,
        count
      })).sort((a, b) => b.count - a.count)
    },
    licenseStatusList() {
      const dist = this.report.licenseStatusDistribution || {}
      return Object.entries(dist).map(([key, count]) => ({
        label: this.licenseStatusLabel(key),
        key,
        count
      })).sort((a, b) => b.count - a.count)
    }
  },
  created() { this.loadReport() },
  methods: {
    loadReport() {
      this.loading = true
      getLicenseReport().then(res => {
        this.report = res.data || {}
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    versionLabel(v) {
      const map = {
        'micro_lite': '小微免费版', 'micro_pro': '小微专业版',
        'BASIC': '基础版', 'ADVANCED': '高级版', 'PROFESSIONAL': '专业版'
      }
      return map[v] || v || '未知'
    },
    licenseStatusLabel(s) {
      const map = { 'ACTIVE': '有效', 'EXPIRED': '已过期', 'REVOKED': '已吊销' }
      return map[s] || s || '未知'
    },
    formatDate(d) {
      if (!d) return '-'
      return String(d).substring(0, 10)
    },
    pct(count, total) {
      if (!total || total === 0) return '0%'
      return (count * 100 / total).toFixed(1) + '%'
    },
    rowClassName({ row }) {
      if (row.urgent) return 'row-urgent'
      if (row.remainingDays <= 15) return 'row-warning'
      return ''
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.stat-card { text-align: center; }
.stat-label { color: #909399; font-size: 14px; margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: 700; }
</style>
<style>
.el-table .row-urgent td { background: #fef0f0 !important; }
.el-table .row-warning td { background: #fdf6ec !important; }
</style>
