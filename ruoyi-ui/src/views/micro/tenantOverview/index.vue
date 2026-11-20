<template>
  <div class="app-container">
    <el-alert title="企业概览（租户管理员工作台）" type="info"
      description="企业基本信息 / 配置健康度自检。" show-icon :closable="false" class="mb16" />

    <div v-loading="loading">
      <!-- ===== 企业基本信息 ===== -->
      <el-row :gutter="16" class="mb16">
        <el-col :span="16">
          <el-card shadow="hover">
            <div slot="header"><span style="font-weight:600;">企业基本信息</span></div>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="公司名称">{{ data.companyName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="行业">{{ data.industry || '-' }}</el-descriptions-item>
              <el-descriptions-item label="版本档位">
                <el-tag size="small">{{ versionLabel }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="租户状态">
                <el-tag :type="statusTagType" size="small">{{ statusLabel }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ data.createTime || '-' }}</el-descriptions-item>
              <el-descriptions-item label="License">
                <template v-if="data.licenseStatus === 'ACTIVE'">
                  <el-tag type="success" size="small">有效</el-tag>
                  <span v-if="data.licenseRemainingDays === -1" style="margin-left:8px;">永久授权</span>
                  <span v-else-if="data.licenseRemainingDays > 0" style="margin-left:8px;">
                    剩余 <b :style="{ color: data.licenseRemainingDays <= 30 ? '#e6a23c' : '#67c23a' }">{{ data.licenseRemainingDays }}</b> 天
                    <span v-if="data.licenseExpireDate" style="color:#909399;">（至 {{ data.licenseExpireDate }}）</span>
                  </span>
                </template>
                <el-tag v-else type="danger" size="small">未授权</el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="health-card">
            <div slot="header"><span style="font-weight:600;">配置健康度</span></div>
            <div class="health-center">
              <div class="health-dot" :class="'dot-' + (data.configHealth || 'GREEN').toLowerCase()"></div>
              <div class="health-text">{{ healthLabel }}</div>
            </div>
            <div style="text-align:center;color:#909399;font-size:12px;">
              {{ greenCount }}/{{ (data.configChecks || []).length }} 项通过
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- ===== 配置自检明细 ===== -->
      <el-card shadow="hover" class="mb16">
        <div slot="header"><span style="font-weight:600;">配置自检明细</span>
          <el-button size="mini" type="primary" style="float:right;" @click="loadData">刷新</el-button>
        </div>
        <el-table :data="data.configChecks || []" :show-header="true" size="small">
          <el-table-column label="检查项" prop="name" width="140" />
          <el-table-column label="描述" prop="description" min-width="200" />
          <el-table-column label="状态" width="80" align="center">
            <template slot-scope="scope">
              <el-tag :type="checkTagType(scope.row.status)" size="mini">
                {{ checkLabel(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="建议" prop="suggestion" min-width="240">
            <template slot-scope="scope">
              <span v-if="scope.row.suggestion" style="color:#e6a23c;">{{ scope.row.suggestion }}</span>
              <span v-else style="color:#909399;">-</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

    </div>
  </div>
</template>

<script>
import { getTenantOverview } from '@/api/micro/tenantOverview'

export default {
  name: 'TenantOverview',
  data() {
    return { loading: true, data: {} }
  },
  computed: {
    versionLabel() {
      const map = { micro_lite: '小微免费版', micro_pro: '小微专业版', basic: '基础版', advanced: '高级版', professional: '专业版' }
      return map[this.data.versionType] || this.data.versionType || '-'
    },
    statusLabel() {
      const map = { NORMAL: '正常', PENDING: '待激活', GRACE: '宽限期', EXPIRED: '已到期', DISABLED: '已禁用' }
      return map[this.data.tenantStatus] || this.data.tenantStatus || '-'
    },
    statusTagType() {
      const map = { NORMAL: 'success', PENDING: 'info', GRACE: 'warning', EXPIRED: 'danger', DISABLED: 'danger' }
      return map[this.data.tenantStatus] || 'info'
    },
    healthLabel() {
      const map = { GREEN: '健康', YELLOW: '需关注', RED: '异常' }
      return map[this.data.configHealth] || '-'
    },
    greenCount() {
      return (this.data.configChecks || []).filter(c => c.status === 'GREEN').length
    }
  },
  created() { this.loadData() },
  methods: {
    loadData() {
      this.loading = true
      getTenantOverview().then(res => {
        this.data = res.data || {}
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    checkTagType(status) {
      return status === 'GREEN' ? 'success' : status === 'YELLOW' ? 'warning' : 'danger'
    },
    checkLabel(status) {
      return status === 'GREEN' ? '正常' : status === 'YELLOW' ? '警告' : '异常'
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.health-card { text-align: center; }
.health-center { display: flex; align-items: center; justify-content: center; margin: 16px 0 8px; }
.health-dot { width: 40px; height: 40px; border-radius: 50%; margin-right: 12px; }
.dot-green { background: #67c23a; }
.dot-yellow { background: #e6a23c; }
.dot-red { background: #f56c6c; }
.health-text { font-size: 22px; font-weight: bold; }
</style>
