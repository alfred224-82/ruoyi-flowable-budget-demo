<template>
  <div class="app-container">
    <el-alert title="业务异常处理中心（老板专属）" type="warning"
      description="预约冲突处理 / 预约失败通知（一键重新预约）/ 固定支出异常提醒 / 合规命中处理。" show-icon :closable="false" class="mb16" />

    <div v-loading="loading">
      <!-- 统计栏 -->
      <el-row :gutter="16" class="mb16">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">异常总数</div>
            <div class="stat-value">{{ data.totalCount || 0 }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">预约冲突</div>
            <div class="stat-value" style="color:#f56c6c;">{{ countByType('BOOKING_CONFLICT') }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">预约失败</div>
            <div class="stat-value" style="color:#e6a23c;">{{ countByType('BOOKING_FAILED') }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">合规命中</div>
            <div class="stat-value" style="color:#909399;">{{ countByType('COMPLIANCE_HIT') }}</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 筛选 -->
      <el-form :inline="true" size="small" class="mb16">
        <el-form-item label="异常类型">
          <el-select v-model="filterType" placeholder="全部" clearable @change="loadData">
            <el-option label="预约冲突" value="BOOKING_CONFLICT" />
            <el-option label="预约失败" value="BOOKING_FAILED" />
            <el-option label="固定支出异常" value="FIXED_COST_ANOMALY" />
            <el-option label="合规命中" value="COMPLIANCE_HIT" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重程度">
          <el-select v-model="filterSeverity" placeholder="全部" clearable @change="loadData">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-refresh" size="mini" @click="loadData">刷新</el-button>
        </el-form-item>
      </el-form>

      <!-- 异常列表 -->
      <el-table :data="filteredItems" :row-class-name="rowClassName">
        <el-table-column label="严重程度" width="80" align="center">
          <template slot-scope="scope">
            <el-tag :type="severityTagType(scope.row.severity)" size="mini">
              {{ severityLabel(scope.row.severity) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="异常类型" width="120" prop="exceptionTypeLabel" />
        <el-table-column label="标题" min-width="200" prop="title" show-overflow-tooltip />
        <el-table-column label="订单号" width="140" prop="orderNo">
          <template slot-scope="scope">{{ scope.row.orderNo || '-' }}</template>
        </el-table-column>
        <el-table-column label="发生时间" width="160" prop="occurTime" />
        <el-table-column label="建议操作" min-width="160" prop="suggestedAction" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button v-if="scope.row.canRetryBooking" size="mini" type="primary" @click="handleRetryBooking(scope.row)">
              重新预约
            </el-button>
            <el-button size="mini" type="success" @click="handleResolve(scope.row)">已处理</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="filteredItems.length === 0" style="text-align:center;padding:40px;color:#909399;">
        <i class="el-icon-circle-check" style="font-size:48px;color:#67c23a;"></i>
        <p style="margin-top:12px;font-size:16px;">暂无异常，一切正常！</p>
      </div>
    </div>
  </div>
</template>

<script>
import { getExceptionList, resolveException } from '@/api/micro/exceptionCenter'

export default {
  name: 'ExceptionCenter',
  data() {
    return {
      loading: true,
      data: { items: [], totalCount: 0 },
      filterType: null,
      filterSeverity: null
    }
  },
  computed: {
    filteredItems() {
      let items = this.data.items || []
      if (this.filterType) {
        items = items.filter(i => i.exceptionType === this.filterType)
      }
      if (this.filterSeverity) {
        items = items.filter(i => i.severity === this.filterSeverity)
      }
      return items
    }
  },
  created() { this.loadData() },
  methods: {
    loadData() {
      this.loading = true
      getExceptionList().then(res => {
        this.data = res.data || { items: [], totalCount: 0 }
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    countByType(type) {
      return (this.data.items || []).filter(i => i.exceptionType === type).length
    },
    severityTagType(severity) {
      return severity === 'HIGH' ? 'danger' : severity === 'MEDIUM' ? 'warning' : 'info'
    },
    severityLabel(severity) {
      return severity === 'HIGH' ? '高' : severity === 'MEDIUM' ? '中' : '低'
    },
    rowClassName({ row }) {
      return 'severity-' + (row.severity || 'LOW').toLowerCase()
    },
    handleRetryBooking(row) {
      this.$router.push({
        path: '/micro/booking/add',
        query: { retryFrom: row.orderNo }
      })
    },
    handleResolve(row) {
      this.$modal.confirm('确认将此异常标记为已处理？').then(() => {
        resolveException(row.exceptionType, row.id).then(() => {
          this.$modal.msgSuccess('已标记为已处理')
          this.loadData()
        })
      })
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.stat-card { text-align: center; }
.stat-label { font-size: 13px; color: #909399; margin-bottom: 6px; }
.stat-value { font-size: 24px; font-weight: bold; }
::v-deep .severity-high { background: #fef0f0; }
::v-deep .severity-medium { background: #fdf6ec; }
</style>
