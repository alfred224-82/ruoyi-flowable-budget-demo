<template>
  <div class="app-container">
    <el-alert title="员工业绩榜（★仅 micro_pro 版本，FEAT_RATING）" type="info"
      description="员工综合业绩排行（接单数 / 营收 / 评分），支持按周期筛选。" show-icon :closable="false" class="mb16" />

    <!-- 筛选 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="80px">
      <el-form-item label="周期" prop="period">
        <el-radio-group v-model="queryParams.period" @change="handlePeriodChange">
          <el-radio-button label="week">本周</el-radio-button>
          <el-radio-button label="month">本月</el-radio-button>
          <el-radio-button label="custom">自定义</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="queryParams.period === 'custom'" label="起止日期" prop="dateRange">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
          start-placeholder="开始日期" end-placeholder="结束日期" value-format="yyyy-MM-dd"
          size="small" style="width: 240px;" />
      </el-form-item>
      <el-form-item label="排名数" prop="topN">
        <el-select v-model="queryParams.topN" size="small" style="width: 80px;">
          <el-option :label="10" :value="10" />
          <el-option :label="20" :value="20" />
          <el-option :label="50" :value="50" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">查询</el-button>
      </el-form-item>
    </el-form>

    <!-- 排行表格 -->
    <el-table v-loading="loading" :data="rankingList" border stripe highlight-current-row>
      <el-table-column label="排名" align="center" width="70">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.rank <= 3" :type="rankTagType(scope.row.rank)" size="small" effect="dark" round>
            {{ scope.row.rank }}
          </el-tag>
          <span v-else>{{ scope.row.rank }}</span>
        </template>
      </el-table-column>
      <el-table-column label="员工" align="center" prop="staffName" min-width="100" />
      <el-table-column label="接单数" align="center" prop="orderCount" width="90" sortable />
      <el-table-column label="营收总额" align="center" width="120" sortable>
        <template slot-scope="scope">
          ¥{{ formatMoney(scope.row.totalRevenue) }}
        </template>
      </el-table-column>
      <el-table-column label="平均评分" align="center" width="140" sortable>
        <template slot-scope="scope">
          <span v-if="scope.row.ratingCount > 0">
            <span v-for="n in Math.round(scope.row.avgRating)" :key="n" style="color:#f7ba2a;">★</span>
            <span v-for="n in (5 - Math.round(scope.row.avgRating))" :key="'e'+n" style="color:#ddd;">★</span>
            <span class="ml4 text-muted">({{ scope.row.avgRating }})</span>
          </span>
          <span v-else class="text-muted">暂无评价</span>
        </template>
      </el-table-column>
      <el-table-column label="评价数" align="center" prop="ratingCount" width="80" />
      <el-table-column label="综合得分" align="center" width="100" sortable>
        <template slot-scope="scope">
          <span style="font-weight:600;">{{ scope.row.compositeScore }}</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 空状态 -->
    <el-empty v-if="!loading && rankingList.length === 0" description="当前周期暂无业绩数据" />
  </div>
</template>

<script>
import { getStaffRanking } from '@/api/micro/staffRanking'

export default {
  name: 'StaffRanking',
  data() {
    return {
      loading: false,
      rankingList: [],
      queryParams: { period: 'month', topN: 20 },
      dateRange: []
    }
  },
  created() { this.loadData() },
  methods: {
    loadData() {
      this.loading = true
      const params = { period: this.queryParams.period, topN: this.queryParams.topN }
      if (this.queryParams.period === 'custom' && this.dateRange && this.dateRange.length === 2) {
        params.startDate = this.dateRange[0]
        params.endDate = this.dateRange[1]
      }
      getStaffRanking(params).then(res => {
        this.rankingList = res.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handlePeriodChange() {
      if (this.queryParams.period !== 'custom') {
        this.dateRange = []
      }
      this.loadData()
    },
    handleQuery() {
      if (this.queryParams.period === 'custom' && (!this.dateRange || this.dateRange.length !== 2)) {
        this.$modal.msgWarning('自定义周期请选择起止日期')
        return
      }
      this.loadData()
    },
    rankTagType(rank) {
      return rank === 1 ? 'danger' : rank === 2 ? 'warning' : 'success'
    },
    formatMoney(val) {
      if (val == null) return '0.00'
      return Number(val).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.ml4 { margin-left: 4px; }
.text-muted { color: #c0c4cc; font-size: 12px; }
</style>
