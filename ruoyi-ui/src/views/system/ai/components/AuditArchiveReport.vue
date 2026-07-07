<template>
  <div class="report-wrapper">
    <!-- 报表标题 -->
    <div class="report-header">
      <h2>{{ reportTitle }}</h2>
      <div class="report-meta">
        <span><i class="el-icon-document" /> {{ sheetInfo }}</span>
        <span><i class="el-icon-date" /> {{ generateTime }}</span>
      </div>
    </div>

    <!-- 一、预算编制概况 -->
    <div class="report-section">
      <div class="section-title">
        <i class="el-icon-info" /> 一、预算编制概况
      </div>
      <el-descriptions :column="2" border class="overview-descriptions">
        <el-descriptions-item label="预算单号">
          <el-tag size="small">{{ overview.sheetNo }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="编制部门">
          {{ overview.orgName }}
        </el-descriptions-item>
        <el-descriptions-item label="预算期间">
          {{ overview.budgetPeriod }}
        </el-descriptions-item>
        <el-descriptions-item label="编制人">
          {{ overview.creator }}
        </el-descriptions-item>
        <el-descriptions-item label="审批状态">
          <el-tag :type="getStatusType(overview.status)" size="small">
            {{ getStatusLabel(overview.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="编制日期">
          {{ overview.createTime }}
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- 二、预算执行情况汇总 -->
    <div class="report-section">
      <div class="section-title">
        <i class="el-icon-s-data" /> 二、预算执行情况汇总
      </div>
      
      <!-- 汇总卡片 -->
      <el-row :gutter="20" class="summary-cards">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card card-blue">
            <div class="stat-content">
              <div class="stat-label">预算总额</div>
              <div class="stat-value">¥{{ formatNumber(summary.totalBudget) }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card card-green">
            <div class="stat-content">
              <div class="stat-label">实际总额</div>
              <div class="stat-value">¥{{ formatNumber(summary.totalActual) }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card card-orange">
            <div class="stat-content">
              <div class="stat-label">差异金额</div>
              <div class="stat-value" :class="summary.difference > 0 ? 'positive' : 'negative'">
                ¥{{ formatNumber(summary.difference) }}
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card card-purple">
            <div class="stat-content">
              <div class="stat-label">执行率</div>
              <div class="stat-value">{{ summary.executionRate }}%</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 按科目类型分组表格 -->
      <el-table
        :data="subjectSummaryData"
        border
        stripe
        class="report-table"
        default-expand-all
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      >
        <el-table-column prop="subjectTypeName" label="科目类型" width="150">
          <template slot-scope="scope">
            <el-tag :type="getTypeTagColor(scope.row.subjectType)" size="small">
              {{ scope.row.subjectTypeName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalCount" label="条目数" width="100" align="center" />
        <el-table-column prop="totalBudget" label="预算合计" width="150" align="right">
          <template slot-scope="scope">
            <span v-if="!scope.row.isSummary">¥{{ formatNumber(scope.row.totalBudget) }}</span>
            <strong v-else>¥{{ formatNumber(scope.row.totalBudget) }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="totalActual" label="实际合计" width="150" align="right">
          <template slot-scope="scope">
            <span v-if="!scope.row.isSummary">¥{{ formatNumber(scope.row.totalActual) }}</span>
            <strong v-else>¥{{ formatNumber(scope.row.totalActual) }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="difference" label="差异合计" width="140" align="right">
          <template slot-scope="scope">
            <span :class="getDifferenceClass(scope.row.difference)">
              {{ scope.row.difference > 0 ? '+' : '' }}¥{{ formatNumber(scope.row.difference) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="executionRate" label="执行率" width="100" align="center">
          <template slot-scope="scope">
            <el-progress 
              :percentage="Math.min(100, Math.max(0, scope.row.executionRate))" 
              :color="getExecutionRateColor(scope.row.executionRate)"
              :stroke-width="8"
            />
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 三、重大差异说明 -->
    <div v-if="significantDifferences.length > 0" class="report-section">
      <div class="section-title warning-title">
        <i class="el-icon-warning" /> 三、重大差异说明（差异率超过15%）
      </div>
      <el-alert
        v-for="(item, index) in significantDifferences"
        :key="index"
        :title="`${item.subjectCode} - ${item.subjectName}`"
        type="warning"
        :closable="false"
        show-icon
        class="anomaly-item"
      >
        <template #default>
          <p><strong>科目类型：</strong>{{ item.subjectTypeName }}</p>
          <p><strong>预算金额：</strong>¥{{ formatNumber(item.budgetAmount) }}</p>
          <p><strong>实际金额：</strong>¥{{ formatNumber(item.actualAmount) }}</p>
          <p><strong>差异金额：</strong>
            <span :class="getDifferenceClass(item.difference)">
              {{ item.difference > 0 ? '+' : '' }}¥{{ formatNumber(item.difference) }}
            </span>
          </p>
          <p><strong>差异率：</strong>
            <el-tag :type="getDifferenceRateType(item.differenceRate)" size="small">
              {{ item.differenceRate }}%
            </el-tag>
          </p>
          <p v-if="item.remark"><strong>备注：</strong>{{ item.remark }}</p>
        </template>
      </el-alert>
    </div>

    <!-- 四、审批流程记录 -->
    <div class="report-section">
      <div class="section-title">
        <i class="el-icon-s-check" /> 四、审批流程记录
      </div>
      <el-timeline>
        <el-timeline-item
          v-for="(approval, index) in approvalHistory"
          :key="index"
          :timestamp="approval.time"
          placement="top"
          :color="getApprovalStatusColor(approval.status)"
        >
          <el-card shadow="hover">
            <div class="approval-item">
              <div class="approval-header">
                <span class="approval-stage">{{ approval.stage }}</span>
                <el-tag :type="getApprovalTagType(approval.status)" size="mini">
                  {{ getApprovalStatusLabel(approval.status) }}
                </el-tag>
              </div>
              <div class="approval-content">
                <p><strong>审批人：</strong>{{ approval.approver }}</p>
                <p v-if="approval.opinion"><strong>审批意见：</strong>{{ approval.opinion }}</p>
                <p><strong>审批时间：</strong>{{ approval.time }}</p>
              </div>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </div>

    <!-- 五、合规性检查 -->
    <div class="report-section">
      <div class="section-title">
        <i class="el-icon-circle-check" /> 五、合规性检查
      </div>
      <el-table
        :data="complianceChecks"
        border
        stripe
        class="report-table"
      >
        <el-table-column prop="checkItem" label="检查项" min-width="250" />
        <el-table-column prop="result" label="检查结果" width="120" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.result === '通过' ? 'success' : 'danger'" size="small">
              {{ scope.row.result }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
      </el-table>
    </div>

    <!-- 底部信息 -->
    <div class="report-footer">
      <el-row :gutter="20">
        <el-col :span="12">
          <p><strong>归档日期：</strong>{{ archiveDate }}</p>
        </el-col>
        <el-col :span="12">
          <p><strong>审计备注：</strong>{{ auditRemark }}</p>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AuditArchiveReport',
  props: {
    rawContent: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      reportTitle: '审计归档标准报表',
      sheetInfo: '',
      generateTime: '',
      archiveDate: '',
      auditRemark: '',
      overview: {
        sheetNo: '',
        orgName: '',
        budgetPeriod: '',
        creator: '',
        status: '',
        createTime: ''
      },
      summary: {
        totalBudget: 0,
        totalActual: 0,
        difference: 0,
        executionRate: 0
      },
      subjectSummaryData: [],
      significantDifferences: [],
      approvalHistory: [],
      complianceChecks: []
    }
  },
  created() {
    this.parseReportContent()
  },
  methods: {
    parseReportContent() {
      const lines = this.rawContent.split('\n').filter(line => line.trim())
      
      let currentSection = ''
      let currentType = null
      
      for (let i = 0; i < lines.length; i++) {
        const line = lines[i].trim()
        
        // 识别章节
        if (line.includes('一、') || line.includes('预算编制概况')) {
          currentSection = 'overview'
          continue
        }
        if (line.includes('二、') || line.includes('预算执行情况')) {
          currentSection = 'summary'
          continue
        }
        if (line.includes('三、') || line.includes('重大差异')) {
          currentSection = 'differences'
          continue
        }
        if (line.includes('四、') || line.includes('审批流程')) {
          currentSection = 'approval'
          continue
        }
        if (line.includes('五、') || line.includes('合规性')) {
          currentSection = 'compliance'
          continue
        }
        
        // 解析概况信息
        if (currentSection === 'overview') {
          const sheetNoMatch = line.match(/预算单号[：:]?\s*([^\s]+)/)
          if (sheetNoMatch) this.overview.sheetNo = sheetNoMatch[1]
          
          const orgMatch = line.match(/编制部门[：:]?\s*([^\s]+)/)
          if (orgMatch) this.overview.orgName = orgMatch[1]
          
          const periodMatch = line.match(/预算期间[：:]?\s*(\d{4}年\d{1,2}月)/)
          if (periodMatch) this.overview.budgetPeriod = periodMatch[1]
          
          const creatorMatch = line.match(/编制人[：:]?\s*([^\s]+)/)
          if (creatorMatch) this.overview.creator = creatorMatch[1]
          
          const statusMatch = line.match(/审批状态[：:]?\s*([^\s]+)/)
          if (statusMatch) this.overview.status = statusMatch[1]
          
          const timeMatch = line.match(/编制日期[：:]?\s*(\d{4}-\d{2}-\d{2})/)
          if (timeMatch) this.overview.createTime = timeMatch[1]
        }
        
        // 解析科目汇总数据
        if (currentSection === 'summary' && line.startsWith('|') && line.endsWith('|')) {
          const cells = line.split('|').map(c => c.trim()).filter((c, idx) => idx !== 0 && idx !== line.split('|').length - 1)
          
          if (cells.length >= 6) {
            const [typeName, count, budget, actual, diff, rate] = cells
            
            if (typeName && !typeName.includes('---') && !typeName.includes('科目类型')) {
              this.subjectSummaryData.push({
                subjectType: typeName.replace(/[|]/g, '').trim(),
                subjectTypeName: typeName.replace(/[|]/g, '').trim(),
                totalCount: parseInt(count) || 0,
                totalBudget: this.parseAmount(budget),
                totalActual: this.parseAmount(actual),
                difference: this.parseAmount(diff),
                executionRate: parseFloat(rate) || 0,
                isSummary: false
              })
            }
          }
        }
        
        // 解析重大差异
        if (currentSection === 'differences' && line.includes('差异率')) {
          const match = line.match(/([^：]+)[：:]\s*([\d.]+)%/)
          if (match) {
            this.significantDifferences.push({
              subjectCode: '未知',
              subjectName: match[1].trim(),
              subjectTypeName: '未知',
              budgetAmount: 0,
              actualAmount: 0,
              difference: 0,
              differenceRate: parseFloat(match[2]) || 0,
              remark: ''
            })
          }
        }
        
        // 解析审批历史
        if (currentSection === 'approval' && line.includes('审批')) {
          const statusMatch = line.match(/([^：]+)[：:]\s*([^\s]+)/)
          if (statusMatch) {
            this.approvalHistory.push({
              stage: statusMatch[1].trim(),
              status: statusMatch[2].trim(),
              approver: '系统',
              opinion: '',
              time: new Date().toLocaleString('zh-CN')
            })
          }
        }
        
        // 解析合规性检查
        if (currentSection === 'compliance' && line.includes('检查')) {
          const match = line.match(/([^：]+)[：:]\s*([^\s]+)/)
          if (match) {
            this.complianceChecks.push({
              checkItem: match[1].trim(),
              result: match[2].trim(),
              remark: ''
            })
          }
        }
      }
      
      // 计算汇总
      this.calculateSummary()
      
      // 设置生成时间和归档日期
      this.generateTime = new Date().toLocaleDateString('zh-CN')
      this.archiveDate = new Date().toLocaleDateString('zh-CN')
      this.auditRemark = '无'
    },
    
    calculateSummary() {
      const items = this.subjectSummaryData.filter(item => !item.isSummary)
      this.summary.totalBudget = items.reduce((sum, item) => sum + (item.totalBudget || 0), 0)
      this.summary.totalActual = items.reduce((sum, item) => sum + (item.totalActual || 0), 0)
      this.summary.difference = this.summary.totalBudget - this.summary.totalActual
      this.summary.executionRate = this.summary.totalBudget !== 0
        ? ((this.summary.totalActual / this.summary.totalBudget) * 100).toFixed(2)
        : 0
    },
    
    parseAmount(str) {
      if (!str) return 0
      const cleaned = str.replace(/[¥,，]/g, '')
      return parseFloat(cleaned) || 0
    },
    
    formatNumber(num) {
      if (num === null || num === undefined) return '0.00'
      return Number(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    },
    
    getTypeTagColor(type) {
      const colorMap = {
        '收入类': 'success',
        '成本类': 'warning',
        '费用类': 'danger',
        '资产类': 'info',
        '负债类': '',
        '权益类': 'primary'
      }
      return colorMap[type] || 'info'
    },
    
    getDifferenceClass(value) {
      if (value > 0) return 'text-success'
      if (value < 0) return 'text-danger'
      return ''
    },
    
    getDifferenceRateType(rate) {
      if (Math.abs(rate) > 20) return 'danger'
      if (Math.abs(rate) > 15) return 'warning'
      return 'success'
    },
    
    getExecutionRateColor(rate) {
      if (rate >= 90) return '#67c23a'
      if (rate >= 70) return '#e6a23c'
      return '#f56c6c'
    },
    
    getStatusType(status) {
      const map = {
        'Completed': 'success',
        'Approved': 'success',
        'Pending_Review': 'warning',
        'Rejected': 'danger'
      }
      return map[status] || 'info'
    },
    
    getStatusLabel(status) {
      const map = {
        'Completed': '已完成',
        'Approved': '已批准',
        'Pending_Review': '待审核',
        'Rejected': '已驳回'
      }
      return map[status] || status
    },
    
    getApprovalStatusColor(status) {
      const colorMap = {
        'approved': '#67c23a',
        'rejected': '#f56c6c',
        'pending': '#e6a23c'
      }
      return colorMap[status] || '#909399'
    },
    
    getApprovalTagType(status) {
      const typeMap = {
        'approved': 'success',
        'rejected': 'danger',
        'pending': 'warning'
      }
      return typeMap[status] || 'info'
    },
    
    getApprovalStatusLabel(status) {
      const map = {
        'approved': '已通过',
        'rejected': '已驳回',
        'pending': '待审批'
      }
      return map[status] || status
    }
  }
}
</script>

<style scoped>
.report-wrapper {
  padding: 10px;
}

.report-header {
  text-align: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 2px solid #e4e7ed;
}

.report-header h2 {
  font-size: 24px;
  color: #303133;
  margin-bottom: 15px;
  font-weight: 600;
}

.report-meta {
  display: flex;
  justify-content: center;
  gap: 30px;
  font-size: 14px;
  color: #606266;
}

.report-meta i {
  margin-right: 5px;
}

.report-section {
  margin-bottom: 30px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  padding: 10px 0;
  border-left: 4px solid #409EFF;
  padding-left: 12px;
  margin-bottom: 15px;
}

.warning-title {
  border-left-color: #f56c6c;
  color: #f56c6c;
}

.section-title i {
  margin-right: 8px;
}

.overview-descriptions {
  margin: 15px 0;
}

.summary-cards {
  margin-bottom: 20px;
}

.stat-card {
  position: relative;
  overflow: hidden;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
}

.card-blue {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.card-green {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.card-orange {
  background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);
  color: #333;
}

.card-purple {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  color: #333;
}

.stat-content {
  z-index: 2;
  position: relative;
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 10px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
}

.stat-value.positive {
  color: #b3f0b3;
}

.stat-value.negative {
  color: #ffd6d6;
}

.report-table {
  margin: 15px 0;
}

.report-table ::v-deep .el-table__header th {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: 600;
}

.text-success {
  color: #67c23a;
  font-weight: 600;
}

.text-danger {
  color: #f56c6c;
  font-weight: 600;
}

.anomaly-item {
  margin-bottom: 10px;
}

.approval-item {
  padding: 10px;
}

.approval-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.approval-stage {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.approval-content p {
  margin: 5px 0;
  font-size: 14px;
  color: #606266;
}

.report-footer {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 2px solid #e4e7ed;
  color: #606266;
  font-size: 14px;
}

.report-footer p {
  margin: 8px 0;
}
</style>
