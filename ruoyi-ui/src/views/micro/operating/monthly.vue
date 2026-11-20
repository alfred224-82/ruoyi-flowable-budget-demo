<template>
  <div class="app-container">
    <!-- 筛选条 -->
    <el-card shadow="never" style="margin-bottom:12px;">
      <el-form :inline="true" size="small">
        <el-form-item label="月份">
          <el-date-picker v-model="queryMonth" type="month" value-format="yyyy-MM" placeholder="选择月份" style="width:160px;" @change="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
          <el-button type="warning" plain icon="el-icon-download" @click="handleExport">导出 CSV</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div v-loading="loading">
      <!-- 月经营汇总 -->
      <el-row :gutter="16" v-if="data.month" style="margin-bottom:12px;">
        <el-col :span="4">
          <el-card shadow="hover"><div class="metric"><div class="metric-label">已结算场次</div><div class="metric-value">{{ data.activityCount || 0 }}</div></div></el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover"><div class="metric"><div class="metric-label">月实收</div><div class="metric-value income">¥{{ fmt(data.monthRevenue) }}</div></div></el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover"><div class="metric"><div class="metric-label">月变动成本</div><div class="metric-value expense">¥{{ fmt(data.monthCost) }}</div></div></el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover"><div class="metric"><div class="metric-label">月毛利</div><div class="metric-value" :style="{color:(data.monthGrossProfit||0)>=0?'#67c23a':'#f56c6c'}">¥{{ fmt(data.monthGrossProfit) }}</div></div></el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover"><div class="metric"><div class="metric-label">场均毛利</div><div class="metric-value">¥{{ fmt(data.avgSessionProfit) }}</div></div></el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover"><div class="metric"><div class="metric-label">未结算</div><div class="metric-value" style="color:#e6a23c;">{{ data.unsettledCount || 0 }}</div></div></el-card>
        </el-col>
      </el-row>

      <!-- 固定成本构成 + 税费净利 -->
      <el-row :gutter="16" v-if="data.month" style="margin-bottom:12px;">
        <!-- 固定成本构成 -->
        <el-col :span="12">
          <el-card shadow="never">
            <div slot="header"><span style="font-weight:600;">固定成本构成（含人力分摊）</span></div>
            <el-table v-if="data.fixedCostComposition && data.fixedCostComposition.length" :data="data.fixedCostComposition" border size="small">
              <el-table-column label="类别" prop="category" min-width="100" />
              <el-table-column label="名称" prop="name" min-width="120" />
              <el-table-column label="金额" width="120" align="right">
                <template slot-scope="scope"><span style="color:#f56c6c;">¥{{ fmt(scope.row.amount) }}</span></template>
              </el-table-column>
              <el-table-column label="占比" width="80" align="right">
                <template slot-scope="scope">{{ scope.row.percent ? scope.row.percent + '%' : '-' }}</template>
              </el-table-column>
            </el-table>
            <div v-else style="text-align:center;color:#c0c4cc;padding:24px 0;">暂无固定成本记录</div>
            <div style="margin-top:8px;text-align:right;font-weight:600;">合计：<span style="color:#f56c6c;">¥{{ fmt(data.fixedCostTotal) }}</span></div>
          </el-card>
        </el-col>
        <!-- 税费与净利 -->
        <el-col :span="12">
          <el-card shadow="never">
            <div slot="header"><span style="font-weight:600;">税费与净利</span></div>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="税前利润"><span style="font-weight:600;">¥{{ fmt(data.pretaxProfit) }}</span></el-descriptions-item>
              <el-descriptions-item label="计税方式">{{ data.taxMode === 'MANUAL' ? '手动填写' : (data.taxMode === 'RATE' ? '按率 ' + data.taxRate + '%' : '未设置') }}</el-descriptions-item>
              <el-descriptions-item label="税费"><span style="color:#f56c6c;">¥{{ fmt(data.taxAmount) }}</span></el-descriptions-item>
              <el-descriptions-item label="月结状态">
                <el-tag :type="data.closingStatus === 'LOCKED' ? 'success' : 'info'" size="mini">{{ data.closingStatus === 'LOCKED' ? '已锁定' : '开放' }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="净利润" :span="2">
                <span :style="{fontSize:'18px',fontWeight:'bold',color:(data.netProfit||0)>=0?'#67c23a':'#f56c6c'}">¥{{ fmt(data.netProfit) }}</span>
              </el-descriptions-item>
            </el-descriptions>
            <!-- 对比上月 -->
            <div v-if="data.prevMonthGrossProfit != null" style="margin-top:12px;padding-top:12px;border-top:1px dashed #e4e7ed;font-size:13px;color:#909399;">
              上月毛利 ¥{{ fmt(data.prevMonthGrossProfit) }} · 上月净利 ¥{{ fmt(data.prevNetProfit) }}
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 保本线 -->
      <el-card v-if="data.month && data.breakevenGrossProfit != null" shadow="never" style="margin-bottom:12px;">
        <div slot="header"><span style="font-weight:600;">保本线</span></div>
        <el-row :gutter="16">
          <el-col :span="6"><div class="metric"><div class="metric-label">月保本毛利</div><div class="metric-value">¥{{ fmt(data.breakevenGrossProfit) }}</div></div></el-col>
          <el-col :span="6"><div class="metric"><div class="metric-label">保本场次</div><div class="metric-value">{{ data.breakevenSessions || 0 }} 场</div></div></el-col>
          <el-col :span="6"><div class="metric"><div class="metric-label">保本进度</div><div class="metric-value" :style="{color:(data.passedBreakeven)?'#67c23a':'#e6a23c'}">{{ fmt(data.breakevenProgress) }}%</div></div></el-col>
          <el-col :span="6"><div class="metric"><div class="metric-label">状态</div><div class="metric-value" :style="{color:data.passedBreakeven?'#67c23a':'#f56c6c',fontSize:'16px'}">{{ data.passedBreakeven ? '已过保本线' : '未达保本线' }}</div></div></el-col>
        </el-row>
      </el-card>
    </div>
  </div>
</template>

<script>
import { monthlyReport, exportMonthlyCsv } from '@/api/micro/operating'

export default {
  name: 'MicroOperatingMonthly',
  data() {
    return {
      loading: false,
      queryMonth: this.currentMonth(),
      data: {}
    }
  },
  created() {
    this.handleQuery()
  },
  methods: {
    currentMonth() {
      const d = new Date()
      return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0')
    },
    fmt(v) { return (Number(v) || 0).toFixed(2) },

    handleQuery() {
      this.loading = true
      monthlyReport(this.queryMonth).then(res => {
        this.data = res.data || {}
      }).catch(() => {
        this.$modal.msgError('月报加载失败')
      }).finally(() => { this.loading = false })
    },
    resetQuery() {
      this.queryMonth = this.currentMonth()
      this.handleQuery()
    },
    handleExport() {
      if (!this.queryMonth) { this.$modal.msgWarning('请先选择月份'); return }
      exportMonthlyCsv(this.queryMonth).then(res => {
        const blob = new Blob([res], { type: 'text/csv;charset=utf-8' })
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = this.queryMonth + '_月度经营报表.csv'
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
        window.URL.revokeObjectURL(url)
        this.$modal.msgSuccess('已导出')
      }).catch(() => { this.$modal.msgError('导出失败') })
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
