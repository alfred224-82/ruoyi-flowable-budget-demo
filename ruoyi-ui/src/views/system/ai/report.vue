<template>
  <div class="app-container">
    <!-- 报表生成区域 -->
    <el-card class="box-card" shadow="hover">
      <div slot="header" class="clearfix">
        <i class="el-icon-document" style="color: #409EFF; margin-right: 8px;" />
        <span>AI 智能报表生成</span>
      </div>

      <!-- 报表类型选择 -->
      <el-form ref="reportForm" :model="reportForm" :rules="rules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="报表类型" prop="reportType">
              <el-select v-model="reportForm.reportType" placeholder="请选择报表类型" style="width: 100%;">
                <el-option
                  v-for="item in reportTypes"
                  :key="item.code"
                  :label="item.name"
                  :value="item.code"
                >
                  <span style="float: left">{{ item.name }}</span>
                  <span style="float: right; color: #8492a6; font-size: 13px;">{{ item.trigger }}</span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="导出格式" prop="exportFormat">
              <el-select v-model="reportForm.exportFormat" placeholder="请选择导出格式" style="width: 100%;">
                <el-option label="HTML（在线预览）" value="HTML" />
                <el-option label="Excel" value="EXCEL" />
                <el-option label="PDF" value="PDF" />
                <el-option label="Word" value="WORD" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="预算单" prop="sheetIds">
              <el-select
                v-model="reportForm.sheetIds"
                multiple
                filterable
                placeholder="选择预算单（不选则取最近50条）"
                style="width: 100%;"
              >
                <el-option
                  v-for="item in budgetSheets"
                  :key="item.id"
                  :label="item.sheetNo + ' - ' + item.orgName + ' (' + item.budgetYear + '年' + item.budgetMonth + '月)'"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="reportForm.reportType === 'YEARLY_FORECAST'">
            <el-form-item label="预测月数">
              <el-input-number v-model="reportForm.months" :min="1" :max="12" :step="1" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item>
          <el-button type="primary" :loading="generating" @click="handleGenerate" icon="el-icon-magic-stick">
            {{ generating ? 'AI 正在生成报表...' : '生成报表' }}
          </el-button>
          <el-button @click="handleReset" icon="el-icon-refresh">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 报表内容展示区域 -->
    <el-card v-if="reportContent" class="box-card" style="margin-top: 20px;" shadow="hover">
      <div slot="header" class="clearfix">
        <span>{{ currentReport.reportName || '报表预览' }}</span>
        <div style="float: right;">
          <el-tag :type="getStatusType(currentReport.status)" size="small">{{ getStatusLabel(currentReport.status) }}</el-tag>
          <el-button type="text" icon="el-icon-download" style="margin-left: 10px;" @click="handleDownload">下载</el-button>
        </div>
      </div>

      <!-- 结构化报表展示 -->
      <div class="report-container">
        <!-- 月度明细报表 -->
        <template v-if="currentReport.reportType === 'MONTHLY_DETAIL'">
          <monthly-detail-report :raw-content="reportContent" />
        </template>

        <!-- 审批汇总报表 -->
        <template v-else-if="currentReport.reportType === 'APPROVAL_SUMMARY'">
          <approval-summary-report :raw-content="reportContent" />
        </template>

        <!-- 年度预测报表 -->
        <template v-else-if="currentReport.reportType === 'YEARLY_FORECAST'">
          <yearly-forecast-report :raw-content="reportContent" />
        </template>

        <!-- 审计归档报表 -->
        <template v-else-if="currentReport.reportType === 'AUDIT_ARCHIVE'">
          <audit-archive-report :raw-content="reportContent" />
        </template>

        <!-- 默认Markdown展示 -->
        <template v-else>
          <div class="report-content" v-html="renderedContent" />
        </template>
      </div>
    </el-card>

    <!-- 报表历史 -->
    <el-card class="box-card" style="margin-top: 20px;" shadow="hover">
      <div slot="header" class="clearfix">
        <i class="el-icon-time" style="color: #E6A23C; margin-right: 8px;" />
        <span>报表生成历史</span>
        <el-button type="text" icon="el-icon-refresh" style="float: right;" @click="loadHistory">刷新</el-button>
      </div>

      <!-- 筛选 -->
      <el-form :inline="true" size="small">
        <el-form-item label="报表类型">
          <el-select v-model="historyQuery.reportType" placeholder="全部" clearable @change="loadHistory">
            <el-option label="部门月度预算明细" value="MONTHLY_DETAIL" />
            <el-option label="审批汇总分析" value="APPROVAL_SUMMARY" />
            <el-option label="年度滚动预测" value="YEARLY_FORECAST" />
            <el-option label="审计归档标准" value="AUDIT_ARCHIVE" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-table :data="historyList" v-loading="historyLoading" border stripe>
        <el-table-column label="报表名称" prop="reportName" min-width="200" show-overflow-tooltip />
        <el-table-column label="报表类型" prop="reportType" width="140">
          <template slot-scope="scope">
            <el-tag size="mini" :type="getTypeTagColor(scope.row.reportType)">{{ getTypeLabel(scope.row.reportType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="100">
          <template slot-scope="scope">
            <el-tag size="mini" :type="getStatusType(scope.row.status)">{{ getStatusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" prop="costTimeMs" width="100">
          <template slot-scope="scope">
            {{ scope.row.costTimeMs ? (scope.row.costTimeMs / 1000).toFixed(1) + 's' : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="生成时间" prop="generateTime" width="160" />
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="handleViewReport(scope.row)" :disabled="scope.row.status !== 'COMPLETED'">
              <i class="el-icon-view" /> 查看
            </el-button>
            <el-button type="text" size="mini" @click="handleDeleteReport(scope.row)" style="color: #F56C6C;">
              <i class="el-icon-delete" /> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="historyTotal > 0"
        :current-page="historyQuery.pageNum"
        :page-size="historyQuery.pageSize"
        :total="historyTotal"
        layout="total, prev, pager, next"
        @current-change="handleHistoryPageChange"
        style="margin-top: 15px; text-align: right;"
      />
    </el-card>
  </div>
</template>

<script>
import { generateReport, listReportHistory, getReportDetail, deleteReport, getReportTypes } from '@/api/system/report'
import { listPreparation } from '@/api/system/preparation'
import MonthlyDetailReport from './components/MonthlyDetailReport'
import ApprovalSummaryReport from './components/ApprovalSummaryReport'
import YearlyForecastReport from './components/YearlyForecastReport'
import AuditArchiveReport from './components/AuditArchiveReport'

export default {
  name: 'AiReport',
  components: {
    MonthlyDetailReport,
    ApprovalSummaryReport,
    YearlyForecastReport,
    AuditArchiveReport
  },
  data() {
    return {
      // 报表类型列表
      reportTypes: [],
      // 预算单列表
      budgetSheets: [],
      // 表单
      reportForm: {
        reportType: '',
        exportFormat: 'HTML',
        sheetIds: [],
        months: 3
      },
      rules: {
        reportType: [{ required: true, message: '请选择报表类型', trigger: 'change' }]
      },
      // 生成状态
      generating: false,
      // 当前报表内容
      reportContent: '',
      currentReport: {},
      // 历史列表
      historyList: [],
      historyLoading: false,
      historyTotal: 0,
      historyQuery: {
        reportType: '',
        pageNum: 1,
        pageSize: 10
      }
    }
  },
  computed: {
    renderedContent() {
      if (!this.reportContent) return ''
      // 简单 Markdown -> HTML 转换
      return this.reportContent
        .replace(/### (.*)/g, '<h3>$1</h3>')
        .replace(/## (.*)/g, '<h2>$1</h2>')
        .replace(/# (.*)/g, '<h1>$1</h1>')
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/\n/g, '<br/>')
    }
  },
  created() {
    this.loadReportTypes()
    this.loadBudgetSheets()
    this.loadHistory()
  },
  methods: {
    /** 加载报表类型 */
    loadReportTypes() {
      getReportTypes().then(res => {
        this.reportTypes = res.data || []
      })
    },
    /** 加载预算单列表 */
    loadBudgetSheets() {
      listPreparation({ pageNum: 1, pageSize: 100 }).then(res => {
        this.budgetSheets = (res.rows || []).map(item => ({
          id: item.id,
          sheetNo: item.sheetNo,
          orgName: item.orgName,
          budgetYear: item.budgetYear,
          budgetMonth: item.budgetMonth
        }))
      })
    },
    /** 生成报表 */
    handleGenerate() {
      this.$refs.reportForm.validate(valid => {
        if (!valid) return

        this.generating = true
        const params = {
          exportFormat: this.reportForm.exportFormat
        }
        if (this.reportForm.reportType === 'YEARLY_FORECAST') {
          params.months = this.reportForm.months
        }

        const data = {
          reportType: this.reportForm.reportType,
          sheetIds: this.reportForm.sheetIds.length > 0 ? this.reportForm.sheetIds : undefined,
          parameters: params
        }

        generateReport(data).then(res => {
          this.generating = false
          if (res.code === 200) {
            this.currentReport = res.data
            this.reportContent = res.data.reportContent || ''
            this.$message.success('报表生成成功！')
            this.loadHistory()
          } else {
            this.$message.error(res.msg || '报表生成失败')
          }
        }).catch(err => {
          this.generating = false
          this.$message.error('报表生成失败：' + (err.message || '未知错误'))
        })
      })
    },
    /** 重置表单 */
    handleReset() {
      this.reportForm = {
        reportType: '',
        exportFormat: 'HTML',
        sheetIds: [],
        months: 3
      }
      this.reportContent = ''
      this.currentReport = {}
    },
    /** 加载历史 */
    loadHistory() {
      this.historyLoading = true
      listReportHistory(this.historyQuery).then(res => {
        this.historyList = res.rows || []
        this.historyTotal = res.total || 0
        this.historyLoading = false
      }).catch(() => {
        this.historyLoading = false
      })
    },
    /** 查看报表 */
    handleViewReport(row) {
      getReportDetail(row.id).then(res => {
        if (res.code === 200) {
          this.currentReport = res.data
          this.reportContent = res.data.reportContent || ''
          // 滚动到报表内容区域
          this.$nextTick(() => {
            window.scrollTo({ top: 300, behavior: 'smooth' })
          })
        }
      })
    },
    /** 删除报表 */
    handleDeleteReport(row) {
      this.$confirm('确认删除该报表记录？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        deleteReport(row.id).then(res => {
          if (res.code === 200) {
            this.$message.success('删除成功')
            this.loadHistory()
          }
        })
      }).catch(() => {})
    },
    /** 下载报表 */
    handleDownload() {
      if (!this.currentReport || !this.currentReport.reportContent) {
        this.$message.warning('没有可下载的内容')
        return
      }
      // 创建 Blob 下载
      const blob = new Blob([this.currentReport.reportContent], { type: 'text/html;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = (this.currentReport.reportName || '报表') + '.html'
      link.click()
      URL.revokeObjectURL(url)
    },
    /** 历史分页 */
    handleHistoryPageChange(page) {
      this.historyQuery.pageNum = page
      this.loadHistory()
    },
    /** 报表类型标签 */
    getTypeLabel(type) {
      const map = {
        MONTHLY_DETAIL: '月度明细',
        APPROVAL_SUMMARY: '审批汇总',
        YEARLY_FORECAST: '滚动预测',
        AUDIT_ARCHIVE: '审计归档'
      }
      return map[type] || type
    },
    getTypeTagColor(type) {
      const map = {
        MONTHLY_DETAIL: '',
        APPROVAL_SUMMARY: 'warning',
        YEARLY_FORECAST: 'success',
        AUDIT_ARCHIVE: 'danger'
      }
      return map[type] || 'info'
    },
    /** 状态标签 */
    getStatusLabel(status) {
      const map = {
        GENERATING: '生成中',
        COMPLETED: '已完成',
        FAILED: '失败'
      }
      return map[status] || status
    },
    getStatusType(status) {
      const map = {
        GENERATING: 'warning',
        COMPLETED: 'success',
        FAILED: 'danger'
      }
      return map[status] || 'info'
    }
  }
}
</script>

<style scoped>
.report-container {
  padding: 20px;
}

.report-content {
  padding: 20px;
  line-height: 1.8;
  font-size: 14px;
  background: #fafafa;
  border-radius: 4px;
  overflow-x: auto;
}
.report-content ::v-deep table {
  width: 100%;
  border-collapse: collapse;
  margin: 15px 0;
}
.report-content ::v-deep table th,
.report-content ::v-deep table td {
  border: 1px solid #ebeef5;
  padding: 8px 12px;
  text-align: left;
}
.report-content ::v-deep table th {
  background: #f5f7fa;
  font-weight: 600;
}
.report-content ::v-deep h1,
.report-content ::v-deep h2,
.report-content ::v-deep h3 {
  margin: 15px 0 10px;
  color: #303133;
}

/* 美化报表通用样式 */
.report-section {
  margin-bottom: 30px;
}

.report-section-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  padding: 10px 0;
  border-left: 4px solid #409EFF;
  padding-left: 12px;
  margin-bottom: 15px;
}

.report-table {
  margin: 15px 0;
}

.report-summary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 20px;
  border-radius: 8px;
  margin: 20px 0;
}

.report-summary-item {
  display: inline-block;
  margin-right: 40px;
  margin-bottom: 10px;
}

.report-summary-label {
  font-size: 12px;
  opacity: 0.9;
}

.report-summary-value {
  font-size: 24px;
  font-weight: bold;
  margin-top: 5px;
}

.report-warning {
  background: #fef0f0;
  border-left: 4px solid #f56c6c;
  padding: 12px 15px;
  margin: 10px 0;
  border-radius: 4px;
}

.report-success {
  background: #f0f9eb;
  border-left: 4px solid #67c23a;
  padding: 12px 15px;
  margin: 10px 0;
  border-radius: 4px;
}
</style>
