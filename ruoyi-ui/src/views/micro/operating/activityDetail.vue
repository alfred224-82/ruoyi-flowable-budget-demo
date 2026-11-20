<template>
  <div class="app-container">
    <!-- 筛选条 -->
    <el-card shadow="never" style="margin-bottom:12px;">
      <el-form :inline="true" size="small">
        <el-form-item label="开始日期">
          <el-date-picker v-model="queryParams.beginDate" type="date" value-format="yyyy-MM-dd" style="width:150px;" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="queryParams.endDate" type="date" value-format="yyyy-MM-dd" style="width:150px;" />
        </el-form-item>
        <el-form-item label="结算状态">
          <el-select v-model="queryParams.settleStatus" placeholder="全部" clearable style="width:120px;">
            <el-option label="未结算" value="DRAFT" />
            <el-option label="已提交" value="SUBMITTED" />
            <el-option label="已锁定" value="LOCKED" />
          </el-select>
        </el-form-item>
        <el-form-item label="活动名称">
          <el-input v-model="queryParams.title" placeholder="模糊搜索" clearable style="width:160px;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
          <el-button type="warning" plain icon="el-icon-download" @click="handleExport">导出 CSV</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 未结算提醒 -->
    <el-alert v-if="data.unsettledCount > 0" type="warning" :closable="false" show-icon
      :title="'有 ' + data.unsettledCount + ' 场活动尚未结算（DRAFT），不计入合计'" style="margin-bottom:12px;" />

    <div v-loading="loading">
      <!-- 活动明细表 -->
      <el-card shadow="never" v-if="data.rows && data.rows.length">
        <el-table :data="tableData" border size="small" show-summary :summary-method="getSummary">
          <el-table-column label="活动" min-width="160" fixed>
            <template slot-scope="scope">
              <el-link v-if="scope.row.activityId" type="primary" @click="goSettlement(scope.row.activityId)">{{ scope.row.activityTitle || '-' }}</el-link>
              <span v-else style="color:#909399;">{{ scope.row.activityTitle || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="结束时间" width="160">
            <template slot-scope="scope">{{ scope.row.endTime || '-' }}</template>
          </el-table-column>
          <el-table-column label="员工" min-width="120" show-overflow-tooltip>
            <template slot-scope="scope">{{ scope.row.staffNames || '-' }}</template>
          </el-table-column>
          <el-table-column label="人数" width="60" align="center">
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
          <el-table-column label="成本合计" width="110" align="right">
            <template slot-scope="scope"><span style="color:#f56c6c;font-weight:600;">¥{{ fmt(scope.row.totalCost) }}</span></template>
          </el-table-column>
          <el-table-column label="利润" width="110" align="right">
            <template slot-scope="scope">
              <span :style="{fontWeight:'600',color:(scope.row.profit||0)>=0?'#67c23a':'#f56c6c'}">¥{{ fmt(scope.row.profit) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="预估利润" width="110" align="right">
            <template slot-scope="scope"><span style="color:#909399;">¥{{ fmt(scope.row.estimatedProfit) }}</span></template>
          </el-table-column>
          <el-table-column label="差额" width="100" align="right">
            <template slot-scope="scope">
              <span :style="{color:(scope.row.profitDiff||0)>=0?'#67c23a':'#f56c6c'}">{{ scope.row.profitDiff >= 0 ? '+' : '' }}¥{{ fmt(scope.row.profitDiff) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="结算状态" width="90" align="center" fixed="right">
            <template slot-scope="scope">
              <el-tag :type="statusTag(scope.row.settlementStatus)" size="mini">{{ statusText(scope.row.settlementStatus) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 无数据 -->
      <el-card v-else-if="!loading" shadow="never">
        <div style="text-align:center;color:#c0c4cc;padding:48px 0;">
          <i class="el-icon-document" style="font-size:48px;"></i>
          <p style="margin-top:12px;">暂无活动数据</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
import { activityDetail, exportActivityDetailCsv } from '@/api/micro/operating'

export default {
  name: 'MicroOperatingActivityDetail',
  data() {
    const now = new Date()
    const monthAgo = new Date(now)
    monthAgo.setMonth(monthAgo.getMonth() - 1)
    const fmt = d => d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
    return {
      loading: false,
      queryParams: {
        beginDate: fmt(monthAgo),
        endDate: fmt(now),
        settleStatus: '',
        title: ''
      },
      data: {}
    }
  },
  computed: {
    tableData() {
      const rows = this.data.rows || []
      const totalRow = this.data.totalRow
      return totalRow ? [...rows, { ...totalRow, _isTotal: true }] : rows
    }
  },
  created() {
    this.handleQuery()
  },
  methods: {
    fmt(v) { return (Number(v) || 0).toFixed(2) },
    statusText(s) { return { DRAFT: '草稿', SUBMITTED: '已提交', LOCKED: '已锁定' }[s] || '草稿' },
    statusTag(s) { return { DRAFT: 'info', SUBMITTED: '', LOCKED: 'success' }[s] || 'info' },

    handleQuery() {
      this.loading = true
      activityDetail(this.queryParams).then(res => {
        this.data = res.data || {}
      }).catch(() => {
        this.$modal.msgError('活动明细加载失败')
      }).finally(() => { this.loading = false })
    },
    resetQuery() {
      const now = new Date()
      const monthAgo = new Date(now)
      monthAgo.setMonth(monthAgo.getMonth() - 1)
      const fmt = d => d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
      this.queryParams = { beginDate: fmt(monthAgo), endDate: fmt(now), settleStatus: '', title: '' }
      this.handleQuery()
    },
    getSummary({ columns, data }) {
      // 使用后端返回的 totalRow 作为合计行
      const totalRow = this.data.totalRow
      if (!totalRow) return []
      const sums = []
      columns.forEach((col, idx) => {
        if (idx === 0) { sums[idx] = '合计'; return }
        const prop = col.property
        if (!prop) { sums[idx] = ''; return }
        const val = totalRow[prop]
        if (val != null && typeof val === 'number') {
          sums[idx] = '¥' + val.toFixed(2)
        } else {
          sums[idx] = ''
        }
      })
      return sums
    },
    handleExport() {
      exportActivityDetailCsv(this.queryParams).then(res => {
        const blob = new Blob([res], { type: 'text/csv;charset=utf-8' })
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = '活动明细_' + this.queryParams.beginDate + '_' + this.queryParams.endDate + '.csv'
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
        window.URL.revokeObjectURL(url)
        this.$modal.msgSuccess('已导出')
      }).catch(() => { this.$modal.msgError('导出失败') })
    },
    goSettlement(activityId) {
      if (activityId) this.$router.push({ path: '/micro/settlement', query: { activityId } })
    }
  }
}
</script>

<style scoped>
</style>
