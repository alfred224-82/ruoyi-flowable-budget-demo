<template>
  <div class="report-wrapper">
    <!-- 报表标题 -->
    <div class="report-header">
      <h2>{{ reportTitle }}</h2>
      <div class="report-meta">
        <span><i class="el-icon-document" /> 预算单总数：{{ totalCount }} 份</span>
        <span><i class="el-icon-date" /> {{ generateTime }}</span>
      </div>
    </div>

    <!-- 汇总统计卡片 -->
    <div class="report-summary">
      <div class="report-summary-item">
        <div class="report-summary-label">预算总额</div>
        <div class="report-summary-value">¥{{ formatNumber(summary.totalBudget) }}</div>
      </div>
      <div class="report-summary-item">
        <div class="report-summary-label">实际总额</div>
        <div class="report-summary-value">¥{{ formatNumber(summary.totalActual) }}</div>
      </div>
      <div class="report-summary-item">
        <div class="report-summary-label">差异金额</div>
        <div class="report-summary-value" :class="summary.difference > 0 ? 'positive' : 'negative'">
          ¥{{ formatNumber(summary.difference) }}
        </div>
      </div>
      <div class="report-summary-item">
        <div class="report-summary-label">平均差异率</div>
        <div class="report-summary-value">{{ summary.avgDifferenceRate }}%</div>
      </div>
    </div>

    <!-- 部门分组汇总 -->
    <div class="report-section">
      <div class="section-title">
        <i class="el-icon-office-building" /> 按部门分组汇总
      </div>
      <el-table
        :data="deptSummaryData"
        border
        stripe
        class="report-table"
        default-sort="{ prop: 'totalBudget', order: 'descending' }"
      >
        <el-table-column prop="deptName" label="部门名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="sheetCount" label="预算单数" width="100" align="center" sortable />
        <el-table-column prop="totalBudget" label="预算总额" width="150" align="right" sortable>
          <template slot-scope="scope">
            ¥{{ formatNumber(scope.row.totalBudget) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalActual" label="实际总额" width="150" align="right" sortable>
          <template slot-scope="scope">
            ¥{{ formatNumber(scope.row.totalActual) }}
          </template>
        </el-table-column>
        <el-table-column prop="difference" label="差异金额" width="140" align="right" sortable>
          <template slot-scope="scope">
            <span :class="getDifferenceClass(scope.row.difference)">
              {{ scope.row.difference > 0 ? '+' : '' }}¥{{ formatNumber(scope.row.difference) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="differenceRate" label="差异率" width="100" align="center" sortable>
          <template slot-scope="scope">
            <el-tag :type="getDifferenceRateType(scope.row.differenceRate)" size="small">
              {{ scope.row.differenceRate }}%
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 科目类型汇总 -->
    <div class="report-section">
      <div class="section-title">
        <i class="el-icon-s-grid" /> 按科目类型汇总
      </div>
      <el-table
        :data="subjectTypeSummaryData"
        border
        stripe
        class="report-table"
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
            ¥{{ formatNumber(scope.row.totalBudget) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalActual" label="实际合计" width="150" align="right">
          <template slot-scope="scope">
            ¥{{ formatNumber(scope.row.totalActual) }}
          </template>
        </el-table-column>
        <el-table-column prop="difference" label="差异合计" width="140" align="right">
          <template slot-scope="scope">
            <span :class="getDifferenceClass(scope.row.difference)">
              {{ scope.row.difference > 0 ? '+' : '' }}¥{{ formatNumber(scope.row.difference) }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 异常分析 -->
    <div v-if="anomalies.length > 0" class="report-section">
      <div class="section-title warning-title">
        <i class="el-icon-warning" /> 异常风险提示
      </div>
      <el-alert
        v-for="(item, index) in anomalies"
        :key="index"
        :title="item.title"
        :description="item.description"
        type="warning"
        :closable="false"
        show-icon
        class="anomaly-item"
      />
    </div>

    <!-- 审批状态分布 -->
    <div class="report-section">
      <div class="section-title">
        <i class="el-icon-s-operation" /> 审批状态分布
      </div>
      <el-row :gutter="20">
        <el-col v-for="status in statusDistribution" :key="status.status" :span="6">
          <el-card shadow="hover" class="status-card">
            <div class="status-info">
              <div class="status-label">{{ status.label }}</div>
              <div class="status-count">{{ status.count }} 份</div>
              <el-progress 
                :percentage="status.percentage" 
                :color="getStatusColor(status.status)"
                :stroke-width="8"
              />
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ApprovalSummaryReport',
  props: {
    rawContent: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      reportTitle: '审批汇总分析报表',
      generateTime: '',
      totalCount: 0,
      summary: {
        totalBudget: 0,
        totalActual: 0,
        difference: 0,
        avgDifferenceRate: 0
      },
      deptSummaryData: [],
      subjectTypeSummaryData: [],
      anomalies: [],
      statusDistribution: []
    }
  },
  created() {
    this.parseReportContent()
  },
  methods: {
    parseReportContent() {
      const lines = this.rawContent.split('\n').filter(line => line.trim())
      
      // 提取生成时间
      const timeMatch = this.rawContent.match(/生成日期[：:]?\s*(\d{4}-\d{2}-\d{2})/)
      if (timeMatch) this.generateTime = timeMatch[1]
      else this.generateTime = new Date().toLocaleDateString('zh-CN')
      
      let currentSection = ''
      let currentDept = null
      let currentType = null
      
      for (let i = 0; i < lines.length; i++) {
        const line = lines[i].trim()
        
        // 识别章节
        if (line.includes('部门分组') || line.includes('按部门')) {
          currentSection = 'dept'
          continue
        }
        if (line.includes('科目类型') || line.includes('按科目')) {
          currentSection = 'type'
          continue
        }
        if (line.includes('异常') || line.includes('风险')) {
          currentSection = 'anomaly'
          continue
        }
        if (line.includes('状态分布') || line.includes('审批状态')) {
          currentSection = 'status'
          continue
        }
        
        // 解析表格行
        if (line.startsWith('|') && line.endsWith('|')) {
          const cells = line.split('|').map(c => c.trim()).filter((c, idx) => idx !== 0 && idx !== line.split('|').length - 1)
          
          if (currentSection === 'dept' && cells.length >= 6) {
            const [deptName, sheetCount, budget, actual, diff, rate] = cells
            if (deptName && !deptName.includes('---')) {
              this.deptSummaryData.push({
                deptName: deptName.replace(/[|]/g, '').trim(),
                sheetCount: parseInt(sheetCount) || 0,
                totalBudget: this.parseAmount(budget),
                totalActual: this.parseAmount(actual),
                difference: this.parseAmount(diff),
                differenceRate: parseFloat(rate) || 0
              })
            }
          }
          
          if (currentSection === 'type' && cells.length >= 6) {
            const [typeName, count, budget, actual, diff] = cells
            if (typeName && !typeName.includes('---')) {
              this.subjectTypeSummaryData.push({
                subjectType: typeName.replace(/[|]/g, '').trim(),
                subjectTypeName: typeName.replace(/[|]/g, '').trim(),
                totalCount: parseInt(count) || 0,
                totalBudget: this.parseAmount(budget),
                totalActual: this.parseAmount(actual),
                difference: this.parseAmount(diff)
              })
            }
          }
        }
        
        // 解析异常信息
        if (currentSection === 'anomaly' && (line.includes('差异率') || line.includes('超过'))) {
          this.anomalies.push({
            title: '差异异常',
            description: line
          })
        }
        
        // 解析状态分布
        if (currentSection === 'status' && line.includes('：')) {
          const match = line.match(/([^：]+)[：:]\s*(\d+)/)
          if (match) {
            this.statusDistribution.push({
              status: match[1].trim(),
              label: match[1].trim(),
              count: parseInt(match[2]) || 0
            })
          }
        }
      }
      
      // 计算汇总
      this.calculateSummary()
      
      // 计算状态百分比
      if (this.statusDistribution.length > 0) {
        const total = this.statusDistribution.reduce((sum, s) => sum + s.count, 0)
        this.statusDistribution.forEach(s => {
          s.percentage = total > 0 ? Math.round((s.count / total) * 100) : 0
        })
      }
    },
    
    calculateSummary() {
      this.totalCount = this.deptSummaryData.reduce((sum, d) => sum + d.sheetCount, 0)
      this.summary.totalBudget = this.deptSummaryData.reduce((sum, d) => sum + d.totalBudget, 0)
      this.summary.totalActual = this.deptSummaryData.reduce((sum, d) => sum + d.totalActual, 0)
      this.summary.difference = this.summary.totalBudget - this.summary.totalActual
      
      const rates = this.deptSummaryData.map(d => d.differenceRate).filter(r => r !== 0)
      this.summary.avgDifferenceRate = rates.length > 0
        ? (rates.reduce((sum, r) => sum + r, 0) / rates.length).toFixed(2)
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
      if (Math.abs(rate) > 10) return 'warning'
      return 'success'
    },
    
    getStatusColor(status) {
      const colorMap = {
        '已完成': '#67c23a',
        '待审核': '#e6a23c',
        '已批准': '#409eff',
        '已驳回': '#f56c6c'
      }
      return colorMap[status] || '#909399'
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

.report-summary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 25px;
  border-radius: 8px;
  margin: 20px 0;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.report-summary-item {
  display: inline-block;
  margin-right: 50px;
  margin-bottom: 10px;
  vertical-align: top;
}

.report-summary-label {
  font-size: 13px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.report-summary-value {
  font-size: 26px;
  font-weight: bold;
}

.report-summary-value.positive {
  color: #b3f0b3;
}

.report-summary-value.negative {
  color: #ffd6d6;
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

.status-card {
  text-align: center;
  padding: 15px;
}

.status-info {
  padding: 10px 0;
}

.status-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.status-count {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 15px;
}
</style>
