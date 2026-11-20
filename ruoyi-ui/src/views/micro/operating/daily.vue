<template>
  <div class="app-container">
    <!-- 筛选条 -->
    <el-card shadow="never" style="margin-bottom:12px;">
      <el-form :inline="true" size="small">
        <el-form-item label="日期">
          <el-date-picker v-model="queryDate" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width:180px;" @change="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div v-loading="loading">
      <!-- 当日汇总 -->
      <el-row :gutter="16" v-if="data.date" style="margin-bottom:12px;">
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="metric">
              <div class="metric-label">已结算场次</div>
              <div class="metric-value">{{ data.sessionCount || 0 }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="metric">
              <div class="metric-label">当日实收</div>
              <div class="metric-value income">¥{{ fmt(data.revenue) }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="metric">
              <div class="metric-label">当日成本</div>
              <div class="metric-value expense">¥{{ fmt(data.cost) }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="metric">
              <div class="metric-label">当日毛利</div>
              <div class="metric-value" :style="{color:(data.grossProfit||0)>=0?'#67c23a':'#f56c6c'}">¥{{ fmt(data.grossProfit) }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 当日活动结算单明细 -->
      <el-card shadow="never" v-if="data.sessions && data.sessions.length">
        <div slot="header" style="display:flex;align-items:center;justify-content:space-between;">
          <span style="font-weight:600;">当日活动结算明细</span>
          <span style="color:#909399;font-size:12px;">{{ fmtDate(data.date) }}</span>
        </div>
        <el-table :data="data.sessions" border size="small">
          <el-table-column label="活动" min-width="160" show-overflow-tooltip>
            <template slot-scope="scope">
              <el-link type="primary" @click="goSettlement(scope.row.activityId)">{{ scope.row.activityTitle || '-' }}</el-link>
            </template>
          </el-table-column>
          <el-table-column label="结束时间" width="160">
            <template slot-scope="scope">{{ scope.row.endTime || '-' }}</template>
          </el-table-column>
          <el-table-column label="人数" width="70" align="center">
            <template slot-scope="scope">{{ scope.row.participantCount || 0 }}</template>
          </el-table-column>
          <el-table-column label="票面收入" width="110" align="right">
            <template slot-scope="scope">¥{{ fmt(scope.row.ticketRevenue) }}</template>
          </el-table-column>
          <el-table-column label="追加收入" width="110" align="right">
            <template slot-scope="scope">¥{{ fmt(scope.row.addonRevenue) }}</template>
          </el-table-column>
          <el-table-column label="代收" width="100" align="right">
            <template slot-scope="scope"><span style="color:#909399;">¥{{ fmt(scope.row.collectedAmount) }}</span></template>
          </el-table-column>
          <el-table-column label="收入合计" width="110" align="right">
            <template slot-scope="scope" style="font-weight:600;">¥{{ fmt(scope.row.totalRevenue) }}</template>
          </el-table-column>
          <el-table-column label="员工成本" width="110" align="right">
            <template slot-scope="scope"><span style="color:#f56c6c;">¥{{ fmt(scope.row.employeeCost) }}</span></template>
          </el-table-column>
          <el-table-column label="其他成本" width="110" align="right">
            <template slot-scope="scope"><span style="color:#f56c6c;">¥{{ fmt(scope.row.otherCost) }}</span></template>
          </el-table-column>
          <el-table-column label="利润" width="110" align="right">
            <template slot-scope="scope">
              <span :style="{fontWeight:'600',color:(scope.row.profit||0)>=0?'#67c23a':'#f56c6c'}">¥{{ fmt(scope.row.profit) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="结算状态" width="90" align="center">
            <template slot-scope="scope">
              <el-tag :type="statusTag(scope.row.settlementStatus)" size="mini">{{ statusText(scope.row.settlementStatus) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 无数据 -->
      <el-card v-else-if="!loading && data.date" shadow="never">
        <div style="text-align:center;color:#c0c4cc;padding:48px 0;">
          <i class="el-icon-document" style="font-size:48px;"></i>
          <p style="margin-top:12px;">{{ fmtDate(data.date) }} 无已结算活动</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
import { dailyReport } from '@/api/micro/operating'

export default {
  name: 'MicroOperatingDaily',
  data() {
    return {
      loading: false,
      queryDate: this.todayStr(),
      data: {}
    }
  },
  created() {
    this.handleQuery()
  },
  methods: {
    todayStr() {
      const d = new Date()
      return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
    },
    fmtDate(d) { return d || '-' },
    fmt(v) { return (Number(v) || 0).toFixed(2) },
    statusText(s) { return { DRAFT: '草稿', SUBMITTED: '已提交', LOCKED: '已锁定' }[s] || '草稿' },
    statusTag(s) { return { DRAFT: 'info', SUBMITTED: '', LOCKED: 'success' }[s] || 'info' },

    handleQuery() {
      this.loading = true
      dailyReport(this.queryDate).then(res => {
        this.data = res.data || {}
      }).catch(() => {
        this.$modal.msgError('日报加载失败')
      }).finally(() => { this.loading = false })
    },
    resetQuery() {
      this.queryDate = this.todayStr()
      this.handleQuery()
    },
    goSettlement(activityId) {
      if (activityId) this.$router.push({ path: '/micro/settlement', query: { activityId } })
    }
  }
}
</script>

<style scoped>
.metric { text-align: center; padding: 8px 0; }
.metric-label { font-size: 13px; color: #909399; margin-bottom: 8px; }
.metric-value { font-size: 22px; font-weight: 700; color: #303133; }
.metric-value.income { color: #67c23a; }
.metric-value.expense { color: #f56c6c; }
</style>
