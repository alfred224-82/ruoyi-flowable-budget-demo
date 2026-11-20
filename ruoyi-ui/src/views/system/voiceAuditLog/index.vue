<template>
  <div class="app-container">
    <el-alert title="语音处理审计日志（Admin 跨租户审计）" type="info"
      description="查看全平台语音合规命中记录，按命中类型/状态/租户/时间范围筛选。" show-icon :closable="false" class="mb16" />

    <!-- ===== 统计摘要 ===== -->
    <el-row :gutter="16" class="mb16">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">命中总数</div>
          <div class="stat-value">{{ summary.totalHits || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">敏感词命中</div>
          <div class="stat-value" style="color:#f56c6c;">{{ (summary.byType || {}).SENSITIVE_WORD || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">待处理</div>
          <div class="stat-value" style="color:#e6a23c;">{{ (summary.byStatus || {}).PENDING || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">已处理</div>
          <div class="stat-value" style="color:#67c23a;">{{ ((summary.byStatus || {}).ACKNOWLEDGED || 0) + ((summary.byStatus || {}).DISMISSED || 0) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ===== 筛选 ===== -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="90px">
      <el-form-item label="命中类型" prop="hitType">
        <el-select v-model="queryParams.hitType" placeholder="全部" clearable>
          <el-option label="敏感词" value="SENSITIVE_WORD" />
          <el-option label="未转写" value="NO_TRANSCRIPTION" />
          <el-option label="策略违规" value="POLICY_VIOLATION" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable>
          <el-option label="待处理" value="PENDING" />
          <el-option label="已确认" value="ACKNOWLEDGED" />
          <el-option label="已忽略" value="DISMISSED" />
        </el-select>
      </el-form-item>
      <el-form-item label="租户ID">
        <el-input v-model="queryParams.tenantId" placeholder="租户ID" clearable :maxlength="20" />
      </el-form-item>
      <el-form-item label="时间范围">
        <el-date-picker v-model="dateRange" type="daterange" value-format="yyyy-MM-dd"
          start-placeholder="开始日期" end-placeholder="结束日期" style="width:260px;" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- ===== 列表 ===== -->
    <el-table v-loading="loading" :data="logList" border stripe>
      <el-table-column label="ID" align="center" prop="id" width="60" />
      <el-table-column label="租户" align="center" prop="tenantName" width="130" />
      <el-table-column label="命中时间" align="center" width="160">
        <template slot-scope="scope">{{ formatDateTime(scope.row.hitTime) }}</template>
      </el-table-column>
      <el-table-column label="命中类型" align="center" width="110">
        <template slot-scope="scope">
          <el-tag :type="hitTypeTag(scope.row.hitType)" size="small">{{ hitTypeLabel(scope.row.hitType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="内容摘要" align="left" prop="contentSummary" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" align="center" width="90">
        <template slot-scope="scope">
          <el-tag :type="statusTag(scope.row.status)" size="small">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="规则ID" align="center" prop="ruleId" width="70" />
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { listVoiceAuditLog, getVoiceAuditSummary } from '@/api/system/voiceAuditLog'

export default {
  name: 'VoiceAuditLog',
  data() {
    return {
      loading: true,
      total: 0,
      logList: [],
      summary: {},
      dateRange: null,
      queryParams: { pageNum: 1, pageSize: 10, hitType: null, status: null, tenantId: null }
    }
  },
  created() { this.getList(); this.loadSummary() },
  methods: {
    getList() {
      this.loading = true
      const params = { ...this.queryParams }
      if (this.dateRange && this.dateRange.length === 2) {
        params.startDate = this.dateRange[0]
        params.endDate = this.dateRange[1]
      }
      listVoiceAuditLog(params).then(res => {
        this.logList = res.rows
        this.total = res.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    loadSummary() {
      getVoiceAuditSummary().then(res => { this.summary = res.data || {} })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.dateRange = null; this.resetForm('queryForm'); this.handleQuery() },
    formatDateTime(d) { return d ? d.substring(0, 19).replace('T', ' ') : '-' },
    hitTypeLabel(t) { return { SENSITIVE_WORD: '敏感词', NO_TRANSCRIPTION: '未转写', POLICY_VIOLATION: '策略违规' }[t] || t },
    hitTypeTag(t) { return { SENSITIVE_WORD: 'danger', NO_TRANSCRIPTION: 'warning', POLICY_VIOLATION: 'danger' }[t] || 'info' },
    statusLabel(s) { return { PENDING: '待处理', ACKNOWLEDGED: '已确认', DISMISSED: '已忽略' }[s] || s },
    statusTag(s) { return { PENDING: 'warning', ACKNOWLEDGED: 'success', DISMISSED: 'info' }[s] || '' }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.stat-card { text-align: center; }
.stat-label { color: #909399; font-size: 14px; margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: 700; }
</style>
