<template>
  <div class="report-wrapper">
    <!-- 报表标题 -->
    <div class="report-header">
      <h2>{{ reportTitle }}</h2>
      <div class="report-meta">
        <span><i class="el-icon-date" /> {{ periodInfo }}</span>
        <span><i class="el-icon-office-building" /> {{ deptInfo }}</span>
      </div>
    </div>

    <!-- 汇总信息 -->
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
        <div class="report-summary-label">差异率</div>
        <div class="report-summary-value">{{ summary.differenceRate }}%</div>
      </div>
    </div>

    <!-- Tree Table 展示 -->
    <el-table
      :data="tableData"
      row-key="id"
      border
      stripe
      default-expand-all
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      class="report-table"
    >
      <el-table-column prop="subjectType" label="科目类型" width="120" fixed>
        <template slot-scope="scope">
          <el-tag :type="getTypeTagColor(scope.row.subjectType)" size="small">
            {{ scope.row.subjectTypeName }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="subjectCode" label="科目编码" width="150" />
      <el-table-column prop="subjectName" label="科目名称" min-width="200" show-overflow-tooltip />
      <el-table-column prop="budgetAmount" label="预算金额" width="140" align="right">
        <template slot-scope="scope">
          <span v-if="!scope.row.isSummary">¥{{ formatNumber(scope.row.budgetAmount) }}</span>
          <strong v-else>¥{{ formatNumber(scope.row.budgetAmount) }}</strong>
        </template>
      </el-table-column>
      <el-table-column prop="actualAmount" label="实际金额" width="140" align="right">
        <template slot-scope="scope">
          <span v-if="!scope.row.isSummary">¥{{ formatNumber(scope.row.actualAmount) }}</span>
          <strong v-else>¥{{ formatNumber(scope.row.actualAmount) }}</strong>
        </template>
      </el-table-column>
      <el-table-column prop="difference" label="差异金额" width="140" align="right">
        <template slot-scope="scope">
          <span :class="getDifferenceClass(scope.row.difference)">
            {{ scope.row.difference > 0 ? '+' : '' }}¥{{ formatNumber(scope.row.difference) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="differenceRate" label="差异率" width="100" align="right">
        <template slot-scope="scope">
          <el-tag 
            v-if="!scope.row.isSummary"
            :type="getDifferenceRateType(scope.row.differenceRate)" 
            size="mini"
          >
            {{ scope.row.differenceRate }}%
          </el-tag>
          <strong v-else>{{ scope.row.differenceRate }}%</strong>
        </template>
      </el-table-column>
    </el-table>

    <!-- 底部说明 -->
    <div class="report-footer">
      <p><strong>编制日期：</strong>{{ generateTime }}</p>
      <p><strong>编制人：</strong>{{ currentUser }}</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MonthlyDetailReport',
  props: {
    rawContent: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      tableData: [],
      summary: {
        totalBudget: 0,
        totalActual: 0,
        difference: 0,
        differenceRate: 0
      },
      reportTitle: '',
      periodInfo: '',
      deptInfo: '',
      generateTime: '',
      currentUser: '系统管理员'
    }
  },
  created() {
    this.parseReportContent()
  },
  methods: {
    parseReportContent() {
      // 解析 Markdown 内容，提取表格数据
      const lines = this.rawContent.split('\n').filter(line => line.trim())
      
      let currentType = null
      let typeId = 0
      let itemId = 0
      
      const typeMap = {
        '收入类': 'INCOME',
        '成本类': 'COST',
        '费用类': 'EXPENSE',
        '资产类': 'ASSET',
        '负债类': 'LIABILITY',
        '权益类': 'EQUITY'
      }

      for (let i = 0; i < lines.length; i++) {
        const line = lines[i].trim()
        
        // 提取标题
        if (line.startsWith('#')) {
          if (!this.reportTitle) {
            this.reportTitle = line.replace(/^#+\s*/, '')
          }
          continue
        }
        
        // 提取期间和部门信息
        if (line.includes('预算期间') || line.includes('部门')) {
          const match = line.match(/预算期间[：:]?\s*(\d{4}年\d{1,2}月)/)
          if (match) this.periodInfo = match[1]
          
          const deptMatch = line.match(/部门[：:]?\s*([^\s]+)/)
          if (deptMatch) this.deptInfo = deptMatch[1]
          continue
        }
        
        // 跳过表头分隔线
        if (line.startsWith('|---') || line.startsWith('|科目类型')) {
          continue
        }
        
        // 解析表格行
        if (line.startsWith('|') && line.endsWith('|')) {
          const cells = line.split('|').map(c => c.trim()).filter((c, idx) => idx !== 0 && idx !== line.split('|').length - 1)
          
          if (cells.length >= 7) {
            const [type, code, name, budget, actual, diff, rate] = cells
            
            // 小计行
            if (name.includes('小计')) {
              if (currentType) {
                this.tableData.push({
                  id: `summary-${typeId}`,
                  subjectType: typeMap[currentType] || currentType,
                  subjectTypeName: currentType,
                  subjectCode: '-',
                  subjectName: name,
                  budgetAmount: this.parseAmount(budget),
                  actualAmount: this.parseAmount(actual),
                  difference: this.parseAmount(diff),
                  differenceRate: parseFloat(rate) || 0,
                  isSummary: true,
                  children: []
                })
              }
              continue
            }
            
            // 新类型
            if (type && !code && !name) {
              currentType = type.replace(/[|]/g, '').trim()
              typeId++
              continue
            }
            
            // 数据行
            if (code && name && !name.includes('小计')) {
              itemId++
              this.tableData.push({
                id: `item-${itemId}`,
                subjectType: typeMap[currentType] || currentType,
                subjectTypeName: currentType,
                subjectCode: code,
                subjectName: name,
                budgetAmount: this.parseAmount(budget),
                actualAmount: this.parseAmount(actual),
                difference: this.parseAmount(diff),
                differenceRate: parseFloat(rate) || 0,
                isSummary: false
              })
            }
          }
        }
      }
      
      // 计算汇总
      this.calculateSummary()
      
      // 提取生成时间
      const timeMatch = this.rawContent.match(/编制日期[：:]?\s*(\d{4}-\d{2}-\d{2})/)
      if (timeMatch) this.generateTime = timeMatch[1]
      else this.generateTime = new Date().toLocaleDateString('zh-CN')
    },
    
    calculateSummary() {
      const items = this.tableData.filter(item => !item.isSummary)
      this.summary.totalBudget = items.reduce((sum, item) => sum + (item.budgetAmount || 0), 0)
      this.summary.totalActual = items.reduce((sum, item) => sum + (item.actualAmount || 0), 0)
      this.summary.difference = this.summary.totalBudget - this.summary.totalActual
      this.summary.differenceRate = this.summary.totalActual !== 0 
        ? ((this.summary.difference / this.summary.totalActual) * 100).toFixed(2)
        : 0
    },
    
    parseAmount(str) {
      if (!str) return 0
      // 移除货币符号、千位符等
      const cleaned = str.replace(/[¥,，]/g, '')
      return parseFloat(cleaned) || 0
    },
    
    formatNumber(num) {
      if (num === null || num === undefined) return '0.00'
      return Number(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    },
    
    getTypeTagColor(type) {
      const colorMap = {
        'INCOME': 'success',
        'COST': 'warning',
        'EXPENSE': 'danger',
        'ASSET': 'info',
        'LIABILITY': '',
        'EQUITY': 'primary'
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

.report-table {
  margin: 20px 0;
}

.report-table ::v-deep .el-table__header th {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: 600;
}

.report-table ::v-deep .el-table__row--level-0:not(.is-summary) {
  background-color: #fafafa;
}

.text-success {
  color: #67c23a;
  font-weight: 600;
}

.text-danger {
  color: #f56c6c;
  font-weight: 600;
}

.report-footer {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
  color: #606266;
  font-size: 14px;
}

.report-footer p {
  margin: 8px 0;
}
</style>
