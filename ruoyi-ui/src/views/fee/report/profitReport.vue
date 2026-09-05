<template>
  <div class="app-container">
    <el-alert title="项目利润报表（高级版）：纸面/实际利润分离，利润拆解与收支时间线；项目经理仅可见本人参与项目。" type="info" :closable="false" style="margin-bottom: 15px;" />

    <el-form :inline="true" size="small">
      <el-form-item label="项目名称">
        <el-input v-model="queryParams.projectName" placeholder="请输入项目名称" clearable style="width: 200px;" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="项目状态">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 140px;">
          <el-option label="进行中" value="ONGOING" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已终止" value="TERMINATED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        <el-button type="warning" plain icon="el-icon-download" @click="handleExport" v-hasPermi="['fee:profitReport:query']">导出</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="reportList" border size="small" v-loading="loading">
      <el-table-column label="项目名称" min-width="160" prop="projectName" :show-overflow-tooltip="true" />
      <el-table-column label="状态" width="90" align="center">
        <template slot-scope="scope">
          <el-tag size="small">{{ projectStatusText(scope.row.projectStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="项目经理" width="100" align="center" prop="projectManagerName" />
      <el-table-column label="合同额" width="130" align="right">
        <template slot-scope="scope">{{ formatAmount(scope.row.currentContractAmount) }}</template>
      </el-table-column>
      <el-table-column label="已核验收入" width="130" align="right">
        <template slot-scope="scope">{{ formatAmount(scope.row.verifiedIncome) }}</template>
      </el-table-column>
      <el-table-column label="总支出" width="120" align="right">
        <template slot-scope="scope">{{ formatAmount(scope.row.totalExpense) }}</template>
      </el-table-column>
      <el-table-column label="纸面利润" width="120" align="right">
        <template slot-scope="scope">
          <span :style="profitStyle(scope.row.paperProfit)">{{ formatAmount(scope.row.paperProfit) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="实际利润" width="120" align="right">
        <template slot-scope="scope">
          <span :style="profitStyle(scope.row.actualProfit)">{{ formatAmount(scope.row.actualProfit) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="利润率" width="90" align="center">
        <template slot-scope="scope">{{ scope.row.profitRate == null ? '-' : Number(scope.row.profitRate).toFixed(1) + '%' }}</template>
      </el-table-column>
      <el-table-column label="收入确认进度" width="150" align="center">
        <template slot-scope="scope">
          <el-progress :percentage="progressPct(scope.row.incomeProgress)" :stroke-width="10" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" align="center">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleDetail(scope.row)" v-hasPermi="['fee:profitReport:query']">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="loadList" />

    <!-- 利润详情：拆解 + 收支时间线 -->
    <el-dialog :title="'项目利润详情 - ' + (detail.projectName || '')" :visible.sync="detailVisible" width="820px" append-to-body>
      <el-descriptions :column="3" border size="small" style="margin-bottom: 15px;">
        <el-descriptions-item label="合同额">{{ formatAmount(detail.currentContractAmount) }}</el-descriptions-item>
        <el-descriptions-item label="已核验收入">{{ formatAmount(detail.verifiedIncome) }}</el-descriptions-item>
        <el-descriptions-item label="待核验收入">{{ formatAmount(detail.pendingIncome) }}</el-descriptions-item>
        <el-descriptions-item label="总支出">{{ formatAmount(detail.totalExpense) }}</el-descriptions-item>
        <el-descriptions-item label="折旧分摊">{{ formatAmount(detail.depreciationShare) }}</el-descriptions-item>
        <el-descriptions-item label="项目分成">{{ formatAmount(detail.sharingAmount) }}</el-descriptions-item>
        <el-descriptions-item label="坏账计提">{{ formatAmount(detail.badDebtProvision) }}</el-descriptions-item>
        <el-descriptions-item label="纸面利润"><span :style="profitStyle(detail.paperProfit)">{{ formatAmount(detail.paperProfit) }}</span></el-descriptions-item>
        <el-descriptions-item label="实际利润"><span :style="profitStyle(detail.actualProfit)">{{ formatAmount(detail.actualProfit) }}</span></el-descriptions-item>
      </el-descriptions>

      <div style="font-weight: bold; margin-bottom: 8px;">收支时间线</div>
      <el-timeline v-loading="timelineLoading">
        <el-timeline-item v-for="(item, idx) in timeline" :key="idx" :timestamp="item.occurDate" placement="top">
          <el-card shadow="never" :body-style="{ padding: '8px 12px' }">
            <div>
              <el-tag size="mini" :type="eventTagType(item.eventType)">{{ eventTypeText(item.eventType) }}</el-tag>
              <span style="margin-left: 8px;">{{ item.eventLabel }}</span>
              <span v-if="item.refNo" style="color: #909399; margin-left: 8px;">{{ item.refNo }}</span>
              <span style="float: right; font-weight: bold;" :style="{ color: eventAmountColor(item.eventType) }">
                {{ formatAmount(item.amount) }}
              </span>
            </div>
            <div v-if="item.status" style="color: #909399; font-size: 12px; margin-top: 4px;">状态：{{ item.status }}</div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-if="!timelineLoading && timeline.length === 0" description="暂无收支记录" />
    </el-dialog>

    <!-- 导出参数 -->
    <el-dialog title="导出利润报表" :visible.sync="exportVisible" width="420px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="导出范围">
          <span style="color: #909399;">当前数据范围内全部项目</span>
        </el-form-item>
        <el-form-item label="附加月份明细">
          <el-date-picker v-model="exportMonth" type="month" value-format="yyyy-MM" placeholder="可空；选择后附加当月收支明细" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="exportVisible = false">取消</el-button>
        <el-button type="primary" :loading="exportLoading" @click="doExport">导出</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listProfitReport, getProfitDetail, getProfitTimeline, exportProfitReport } from '@/api/system/fee/fundProfitReport'

export default {
  name: 'FeeProfitReport',
  data() {
    return {
      loading: false,
      reportList: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10, projectName: null, status: null },
      detailVisible: false,
      detail: {},
      timeline: [],
      timelineLoading: false,
      exportVisible: false,
      exportLoading: false,
      exportMonth: null
    }
  },
  created() {
    this.loadList()
  },
  methods: {
    formatAmount(val) {
      if (val === null || val === undefined || val === '') return '-'
      return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    },
    profitStyle(val) {
      const num = Number(val || 0)
      if (num > 0) return 'color:#67c23a;font-weight:bold;'
      if (num < 0) return 'color:#f56c6c;font-weight:bold;'
      return ''
    },
    progressPct(val) {
      const num = Number(val || 0)
      return Math.min(100, Math.round(num * 100))
    },
    projectStatusText(status) {
      const map = { ONGOING: '进行中', COMPLETED: '已完成', TERMINATED: '已终止' }
      return map[status] || status || '-'
    },
    eventTypeText(type) {
      const map = { CONTRACT: '合同', INCOME_CONFIRM: '收入确认', PAYMENT: '付款', RECEIPT: '收款', EXPENSE: '报销支出' }
      return map[type] || type
    },
    eventTagType(type) {
      const map = { CONTRACT: '', INCOME_CONFIRM: 'success', PAYMENT: 'danger', RECEIPT: 'success', EXPENSE: 'warning' }
      return map[type] || 'info'
    },
    eventAmountColor(type) {
      return (type === 'PAYMENT' || type === 'EXPENSE') ? '#f56c6c' : '#67c23a'
    },
    loadList() {
      this.loading = true
      listProfitReport(this.queryParams).then(res => {
        this.reportList = res.rows || []
        this.total = res.total || 0
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.loadList()
    },
    resetQuery() {
      this.queryParams = { pageNum: 1, pageSize: 10, projectName: null, status: null }
      this.loadList()
    },
    handleDetail(row) {
      this.detail = { ...row }
      this.timeline = []
      this.detailVisible = true
      this.timelineLoading = true
      getProfitTimeline(row.projectId).then(res => {
        this.timeline = res.data || []
        this.timelineLoading = false
      }).catch(() => { this.timelineLoading = false })
    },
    handleExport() {
      this.exportMonth = null
      this.exportVisible = true
    },
    doExport() {
      this.exportLoading = true
      exportProfitReport({ month: this.exportMonth }).then(res => {
        const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const link = document.createElement('a')
        link.href = URL.createObjectURL(blob)
        link.download = '项目利润报表' + (this.exportMonth ? '_' + this.exportMonth : '') + '.xlsx'
        link.click()
        URL.revokeObjectURL(link.href)
        this.exportVisible = false
        this.$modal.msgSuccess('导出成功')
      }).finally(() => { this.exportLoading = false })
    }
  }
}
</script>
