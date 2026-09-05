<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="项目" prop="projectId">
        <el-select v-model="queryParams.projectId" placeholder="全部项目" clearable filterable>
          <el-option v-for="p in activeProjects" :key="p.id" :label="p.projectName" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-alert title="财务核验工作台：默认展示待核验收入确认，核验通过转已核验收入，驳回则冲销。" type="info" :closable="false" style="margin-bottom: 10px;" />

    <el-table v-loading="loading" :data="incomeList">
      <el-table-column label="项目" align="left" prop="projectName" min-width="160" show-overflow-tooltip />
      <el-table-column label="确认金额" align="right" prop="confirmedAmount" width="120" />
      <el-table-column label="确认日期" align="center" prop="confirmDate" width="110">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.confirmDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="依据类型" align="center" prop="basisType" width="100">
        <template slot-scope="scope">
          {{ basisText(scope.row.basisType) }}
        </template>
      </el-table-column>
      <el-table-column label="依据编号" align="center" prop="basisNo" width="140" show-overflow-tooltip />
      <el-table-column label="核验状态" align="center" prop="verifyStatus" width="100">
        <template slot-scope="scope">
          <el-tag :type="verifyTag(scope.row.verifyStatus)" size="small">{{ verifyText(scope.row.verifyStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template slot-scope="scope">
          <template v-if="scope.row.verifyStatus === 'PENDING'">
            <el-button size="mini" type="text" icon="el-icon-check" @click="handleVerify(scope.row)" v-hasPermi="['fee:income:verify']">通过</el-button>
            <el-button size="mini" type="text" icon="el-icon-close" @click="handleReject(scope.row)" v-hasPermi="['fee:income:reject']">驳回</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { listIncome, verifyIncome, rejectIncome } from '@/api/system/fee/fundIncome'
import { listActiveProject } from '@/api/system/fee/fundProject'

export default {
  name: 'FundIncomeVerify',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      incomeList: [],
      activeProjects: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        projectId: null,
        verifyStatus: 'PENDING'
      }
    }
  },
  created() {
    this.getList()
    listActiveProject().then(res => { this.activeProjects = res.data || [] })
  },
  methods: {
    getList() {
      this.loading = true
      listIncome(this.queryParams).then(response => {
        this.incomeList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    verifyText(status) {
      const map = { PENDING: '待核验', VERIFIED: '已核验', REJECTED: '已驳回' }
      return map[status] || status
    },
    verifyTag(status) {
      const map = { PENDING: 'warning', VERIFIED: 'success', REJECTED: 'danger' }
      return map[status] || 'info'
    },
    basisText(type) {
      const map = { CONTRACT: '合同', INVOICE: '发票', RECEIPT: '回单', OTHER: '其他' }
      return map[type] || type
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.queryParams.verifyStatus = 'PENDING'
      this.handleQuery()
    },
    handleVerify(row) {
      this.$modal.confirm('核验通过【' + row.projectName + '】收入 ' + row.confirmedAmount + ' 元？').then(() => {
        return verifyIncome(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('核验通过')
      }).catch(() => {})
    },
    handleReject(row) {
      this.$prompt('请输入驳回原因', '驳回收入确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }).then(({ value }) => {
        return rejectIncome(row.id, value)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('已驳回')
      }).catch(() => {})
    }
  }
}
</script>
