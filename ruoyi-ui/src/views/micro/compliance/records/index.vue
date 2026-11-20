<template>
  <div class="app-container">
    <el-alert title="语音合规记录（★只读，仅 micro_pro）" type="info"
      description="查看合规命中记录，支持按时间/类型筛选。无编辑操作。" show-icon :closable="false" class="mb16" />

    <!-- 筛选 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="80px">
      <el-form-item label="命中类型" prop="hitType">
        <el-select v-model="queryParams.hitType" placeholder="全部" clearable>
          <el-option label="敏感词" value="SENSITIVE_WORD" />
          <el-option label="未转写" value="NO_TRANSCRIPTION" />
          <el-option label="策略违规" value="POLICY_VIOLATION" />
        </el-select>
      </el-form-item>
      <el-form-item label="处理状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable>
          <el-option label="待处理" value="PENDING" />
          <el-option label="已确认" value="ACKNOWLEDGED" />
          <el-option label="已忽略" value="DISMISSED" />
        </el-select>
      </el-form-item>
      <el-form-item label="命中时间">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期" value-format="yyyy-MM-dd" size="small" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 命中记录列表 -->
    <el-table v-loading="loading" :data="hitList">
      <el-table-column label="ID" align="center" prop="id" width="60" />
      <el-table-column label="命中时间" align="center" prop="hitTime" width="160" />
      <el-table-column label="命中类型" align="center" width="100">
        <template slot-scope="scope">
          <el-tag :type="hitTypeTag(scope.row.hitType)" size="mini">{{ hitTypeMap[scope.row.hitType] || scope.row.hitType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="内容摘要" align="left" prop="contentSummary" min-width="200" show-overflow-tooltip />
      <el-table-column label="关联规则" align="center" prop="ruleId" width="80" />
      <el-table-column label="状态" align="center" width="80">
        <template slot-scope="scope">
          <el-tag :type="statusTag(scope.row.status)" size="mini">{{ statusMap[scope.row.status] || scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="left" prop="remark" min-width="120" show-overflow-tooltip />
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { listComplianceHits } from '@/api/micro/compliance'

export default {
  name: 'ComplianceRecords',
  data() {
    return {
      loading: true,
      total: 0,
      hitList: [],
      dateRange: [],
      queryParams: { pageNum: 1, pageSize: 10, hitType: null, status: null, hitTimeStart: null, hitTimeEnd: null },
      hitTypeMap: { SENSITIVE_WORD: '敏感词', NO_TRANSCRIPTION: '未转写', POLICY_VIOLATION: '策略违规' },
      statusMap: { PENDING: '待处理', ACKNOWLEDGED: '已确认', DISMISSED: '已忽略', RESOLVED: '已解决' }
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      if (this.dateRange && this.dateRange.length === 2) {
        this.queryParams.hitTimeStart = this.dateRange[0]
        this.queryParams.hitTimeEnd = this.dateRange[1]
      } else {
        this.queryParams.hitTimeStart = null
        this.queryParams.hitTimeEnd = null
      }
      listComplianceHits(this.queryParams).then(res => {
        this.hitList = res.rows; this.total = res.total; this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.dateRange = []; this.resetForm('queryForm'); this.handleQuery() },
    hitTypeTag(type) {
      if (type === 'SENSITIVE_WORD') return 'danger'
      if (type === 'POLICY_VIOLATION') return 'warning'
      return 'info'
    },
    statusTag(status) {
      if (status === 'PENDING') return 'warning'
      if (status === 'ACKNOWLEDGED' || status === 'RESOLVED') return 'success'
      return 'info'
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
</style>
