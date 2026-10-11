<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="项目名称" prop="projectName">
        <el-input v-model="queryParams.projectName" placeholder="请输入项目名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="草稿" value="DRAFT" />
          <el-option label="待审批" value="PENDING_APPROVAL" />
          <el-option label="进行中" value="ACTIVE" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已关闭" value="CLOSED" />
          <el-option label="烂账" value="LOST" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleCreate" v-hasPermi="['fee:project:add']">立项</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="projectList">
      <el-table-column label="项目名称" align="left" prop="projectName" min-width="180" show-overflow-tooltip />
      <el-table-column label="客户" align="center" prop="clientName" min-width="120" show-overflow-tooltip />
      <el-table-column label="项目经理" align="center" prop="projectManagerName" width="100" />
      <el-table-column label="初始合同额" align="right" prop="contractAmount" width="120" />
      <el-table-column label="当前合同额" align="right" prop="currentContractAmount" width="120" />
      <el-table-column label="总预算" align="right" prop="totalBudget" width="110" />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template slot-scope="scope">
          <el-tag :type="statusTag(scope.row.status)" size="small">{{ statusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="110">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="220">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleDetail(scope.row)" v-hasPermi="['fee:project:query']">详情</el-button>
          <el-button v-if="scope.row.status === 'DRAFT'" size="mini" type="text" icon="el-icon-upload2" @click="handleSubmit(scope.row)" v-hasPermi="['fee:project:edit']">提交审批</el-button>
          <template v-if="scope.row.status === 'PENDING_APPROVAL'">
            <el-button size="mini" type="text" icon="el-icon-check" @click="handleApprove(scope.row)" v-hasPermi="['fee:project:approve']">审批</el-button>
            <el-button size="mini" type="text" icon="el-icon-close" @click="handleReject(scope.row)" v-hasPermi="['fee:project:approve']">驳回</el-button>
          </template>
          <template v-if="scope.row.status === 'ACTIVE'">
            <el-button size="mini" type="text" icon="el-icon-user" @click="handleMembers(scope.row)" v-hasPermi="['fee:project:edit']">成员</el-button>
            <el-button size="mini" type="text" icon="el-icon-circle-close" @click="handleClose(scope.row)" v-hasPermi="['fee:project:close']">关闭</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { listProject, approveProject, rejectProject, closeProject, submitProject } from '@/api/system/fee/fundProject'

export default {
  name: 'FundProject',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      projectList: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        projectName: null,
        status: null
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询项目列表 */
    getList() {
      this.loading = true
      listProject(this.queryParams).then(response => {
        this.projectList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    statusText(status) {
      const map = { DRAFT: '草稿', PENDING_APPROVAL: '待审批', ACTIVE: '进行中', COMPLETED: '已完成', CLOSED: '已关闭', LOST: '烂账' }
      return map[status] || status
    },
    statusTag(status) {
      const map = { DRAFT: 'info', PENDING_APPROVAL: 'warning', ACTIVE: 'success', COMPLETED: 'primary', CLOSED: 'info', LOST: 'danger' }
      return map[status] || 'info'
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    /** 立项（跳转隐藏页） */
    handleCreate() {
      this.$router.push('/fee/project/create')
    },
    /** 详情（跳转隐藏页） */
    handleDetail(row) {
      this.$router.push('/fee/project/detail?id=' + row.id)
    },
    /** 成员维护（跳转隐藏页） */
    handleMembers(row) {
      this.$router.push('/fee/project/members?id=' + row.id)
    },
    /** 草稿（含驳回回退）提交审批 */
    handleSubmit(row) {
      this.$modal.confirm('确认提交项目【' + row.projectName + '】审批？').then(() => {
        return submitProject(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('已提交审批')
      }).catch(() => {})
    },
    /** 老板审批通过 */
    handleApprove(row) {
      this.$modal.confirm('确认审批通过项目【' + row.projectName + '】？').then(() => {
        return approveProject(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('审批通过')
      }).catch(() => {})
    },
    /** 审批驳回 */
    handleReject(row) {
      this.$prompt('请输入驳回原因', '驳回项目【' + row.projectName + '】', {
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }).then(({ value }) => {
        return rejectProject(row.id, value)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('已驳回')
      }).catch(() => {})
    },
    /** 关闭项目 */
    handleClose(row) {
      this.$modal.confirm('确认关闭项目【' + row.projectName + '】？关闭后不可再发生收支。').then(() => {
        return closeProject(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('已关闭')
      }).catch(() => {})
    }
  }
}
</script>
