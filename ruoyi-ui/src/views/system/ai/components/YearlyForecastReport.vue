<template>
  <div class="report-wrapper">
    <!-- 报表标题 -->
    <div class="report-header">
      <h2>{{ reportTitle }}</h2>
      <div class="report-meta">
        <span><i class="el-icon-date" /> {{ periodInfo }}</span>
        <span><i class="el-icon-trend-charts" /> 预测月数：{{ forecastMonths }} 个月</span>
      </div>
    </div>

    <!-- 执行率概览 -->
    <div class="report-summary">
      <div class="report-summary-item">
        <div class="report-summary-label">年度预算总额</div>
        <div class="report-summary-value">¥{{ formatNumber(summary.totalBudget) }}</div>
      </div>
      <div class="report-summary-item">
        <div class="report-summary-label">已执行金额</div>
        <div class="report-summary-value">¥{{ formatNumber(summary.executedAmount) }}</div>
      </div>
      <div class="report-summary-item">
        <div class="report-summary-label">执行率</div>
        <div class="report-summary-value">{{ summary.executionRate }}%</div>
      </div>
      <div class="report-summary-item">
        <div class="report-summary-label">剩余预算</div>
        <div class="report-summary-value">¥{{ formatNumber(summary.remainingBudget) }}</div>
      </div>
    </div>

    <!-- 科目类型月度趋势 -->
    <div class="report-section">
      <div class="section-title">
        <i class="el-icon-data-line" /> 科目类型月度趋势分析
      </div>
      <el-table
        :data="trendData"
        border
        stripe
        class="report-table"
        default-expand-all
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      >
        <el-table-column prop="subjectTypeName" label="科目类型" width="150" fixed>
          <template slot-scope="scope">
            <el-tag :type="getTypeTagColor(scope.row.subjectType)" size="small">
              {{ scope.row.subjectTypeName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="avgBudget" label="月均预算" width="140" align="right">
          <template slot-scope="scope">
            <span v-if="!scope.row.isSummary">¥{{ formatNumber(scope.row.avgBudget) }}</span>
            <strong v-else>¥{{ formatNumber(scope.row.avgBudget) }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="avgActual" label="月均实际" width="140" align="right">
          <template slot-scope="scope">
            <span v-if="!scope.row.isSummary">¥{{ formatNumber(scope.row.avgActual) }}</span>
            <strong v-else>¥{{ formatNumber(scope.row.avgActual) }}</strong>
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
        <el-table-column label="未来3个月预测" min-width="300">
          <template slot-scope="scope">
            <div v-if="!scope.row.isSummary && scope.row.forecast" class="forecast-container">
              <div v-for="(month, idx) in scope.row.forecast" :key="idx" class="forecast-item">
                <span class="forecast-month">{{ month.month }}</span>
                <span class="forecast-value">¥{{ formatNumber(month.amount) }}</span>
              </div>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 预测说明 -->
    <div class="report-section">
      <div class="section-title">
        <i class="el-icon-info" /> 预测方法说明
      </div>
      <el-card shadow="hover" class="info-card">
        <div class="info-content">
          <p><strong>预测方法：</strong>简单移动平均法（SMA）</p>
          <p><strong>计算逻辑：</strong>基于最近3个月的实际发生额计算平均值，作为未来月份的预测值</p>
          <p><strong>适用范围：</strong>适用于具有稳定趋势的科目，对于波动较大的科目仅供参考</p>
          <p><strong>注意事项：</strong></p>
          <ul>
            <li>历史数据不足3个月的科目，按当前预算金额比例推算</li>
            <li>季节性因素未纳入本次预测模型</li>
            <li>建议结合业务实际情况调整预测结果</li>
          </ul>
        </div>
      </el-card>
    </div>

    <!-- 底部信息 -->
    <div class="report-footer">
      <p><strong>生成日期：</strong>{{ generateTime }}</p>
      <p><strong>数据来源：</strong>预算管理系统 - 历史预算数据</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'YearlyForecastReport',
  props: {
    rawContent: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      reportTitle: '年度预算滚动预测报表',
      periodInfo: '',
      forecastMonths: 3,
      generateTime: '',
      summary: {
        totalBudget: 0,
        executedAmount: 0,
        executionRate: 0,
        remainingBudget: 0
      },
      trendData: []
    }
  },
  created() {
    this.parseReportContent()
  },
  methods: {
    parseReportContent() {
      const lines = this.rawContent.split('\n').filter(line => line.trim())
      
      let currentType = null
      let typeId = 0
      let itemId = 0
      
      // 提取期间信息
      const yearMatch = this.rawContent.match(/(\d{4})年/)
      if (yearMatch) this.periodInfo = yearMatch[1] + '年度'
      
      // 提取预测月数
      const monthsMatch = this.rawContent.match(/预测.*?(\d+)\s*个?月/)
      if (monthsMatch) this.forecastMonths = parseInt(monthsMatch[1]) || 3
      
      for (let i = 0; i < lines.length; i++) {
        const line = lines[i].trim()
        
        // 跳过表头
        if (line.startsWith('|---') || line.includes('科目类型')) {
          continue
        }
        
        // 解析表格行
        if (line.startsWith('|') && line.endsWith('|')) {
          const cells = line.split('|').map(c => c.trim()).filter((c, idx) => idx !== 0 && idx !== line.split('|').length - 1)
          
          if (cells.length >= 4) {
            const [type, avgBudget, avgActual, rate] = cells
            
            // 新类型
            if (type && !avgBudget && !avgActual) {
              currentType = type.replace(/[|]/g, '').trim()
              typeId++
              continue
            }
            
            // 数据行
            if (avgBudget && avgActual && !type.includes('小计')) {
              itemId++
              const subjectType = type.replace(/[|]/g, '').trim()
              
              this.trendData.push({
                id: `item-${itemId}`,
                subjectType: subjectType,
                subjectTypeName: subjectType,
                avgBudget: this.parseAmount(avgBudget),
                avgActual: this.parseAmount(avgActual),
                executionRate: parseFloat(rate) || 0,
                isSummary: false,
                forecast: this.generateForecast(this.parseAmount(avgActual))
              })
            }
          }
        }
      }
      
      // 计算汇总
      this.calculateSummary()
      
      // 提取生成时间
      const timeMatch = this.rawContent.match(/生成日期[：:]?\s*(\d{4}-\d{2}-\d{2})/)
      if (timeMatch) this.generateTime = timeMatch[1]
      else this.generateTime = new Date().toLocaleDateString('zh-CN')
    },
    
    generateForecast(avgActual) {
      if (!avgActual || avgActual === 0) return null
      
      const forecast = []
      const now = new Date()
      
      for (let i = 1; i <= this.forecastMonths; i++) {
        const futureDate = new Date(now.getFullYear(), now.getMonth() + i, 1)
        const monthStr = `${futureDate.getFullYear()}年${String(futureDate.getMonth() + 1).padStart(2, '0')}月`
        
        forecast.push({
          month: monthStr,
          amount: avgActual
        })
      }
      
      return forecast
    },
    
    calculateSummary() {
      const items = this.trendData.filter(item => !item.isSummary)
      this.summary.totalBudget = items.reduce((sum, item) => sum + (item.avgBudget || 0) * 12, 0)
      this.summary.executedAmount = items.reduce((sum, item) => sum + (item.avgActual || 0) * 12, 0)
      this.summary.executionRate = this.summary.totalBudget !== 0
        ? ((this.summary.executedAmount / this.summary.totalBudget) * 100).toFixed(2)
        : 0
      this.summary.remainingBudget = this.summary.totalBudget - this.summary.executedAmount
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
    
    getExecutionRateColor(rate) {
      if (rate >= 90) return '#67c23a'
      if (rate >= 70) return '#e6a23c'
      return '#f56c6c'
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

.forecast-container {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.forecast-item {
  display: flex;
  justify-content: space-between;
  padding: 4px 8px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
}

.forecast-month {
  color: #606266;
}

.forecast-value {
  color: #409EFF;
  font-weight: 600;
}

.info-card {
  background: #fafafa;
}

.info-content {
  padding: 10px;
}

.info-content p {
  margin: 10px 0;
  line-height: 1.8;
}

.info-content ul {
  margin-left: 20px;
  line-height: 2;
}

.info-content li {
  margin: 5px 0;
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
