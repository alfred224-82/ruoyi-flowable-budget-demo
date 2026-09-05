<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="项目" prop="projectId">
        <el-select v-model="queryParams.projectId" placeholder="全部项目" clearable filterable>
          <el-option v-for="p in activeProjects" :key="p.id" :label="p.projectName" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="核验状态" prop="verifyStatus">
        <el-select v-model="queryParams.verifyStatus" placeholder="全部状态" clearable>
          <el-option label="待核验" value="PENDING" />
          <el-option label="已核验" value="VERIFIED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['fee:income:add']">创建收入确认</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

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
      <el-table-column label="创建时间" align="center" prop="createTime" width="110">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 创建收入确认（待核验） -->
    <el-dialog title="创建收入确认（提交后待财务核验）" :visible.sync="open" width="520px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="所属项目" prop="projectId">
          <el-select v-model="form.projectId" placeholder="请选择项目" filterable style="width: 100%;">
            <el-option v-for="p in activeProjects" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="确认金额" prop="confirmedAmount">
          <el-input-number v-model="form.confirmedAmount" :min="0.01" :precision="2" style="width: 100%;" placeholder="元" />
        </el-form-item>
        <el-form-item label="确认日期" prop="confirmDate">
          <el-date-picker v-model="form.confirmDate" type="date" value-format="yyyy-MM-dd" placeholder="选择确认日期" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="依据类型" prop="basisType">
          <el-select v-model="form.basisType" placeholder="请选择依据类型" style="width: 100%;">
            <el-option label="合同" value="CONTRACT" />
            <el-option label="发票" value="INVOICE" />
            <el-option label="回单" value="RECEIPT" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="依据编号" prop="basisNo">
          <el-input v-model="form.basisNo" placeholder="请输入依据编号" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="open = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listIncome, addIncome } from '@/api/system/fee/fundIncome'
import { listActiveProject } from '@/api/system/fee/fundProject'

export default {
  name: 'FundIncome',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      incomeList: [],
      activeProjects: [],
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        projectId: null,
        verifyStatus: null
      },
      form: {},
      rules: {
        projectId: [{ required: true, message: '请选择项目', trigger: 'change' }],
        confirmedAmount: [{ required: true, message: '确认金额不能为空', trigger: 'blur' }]
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
      this.handleQuery()
    },
    handleAdd() {
      this.form = { projectId: null, confirmedAmount: null, confirmDate: null, basisType: 'CONTRACT', basisNo: null }
      this.open = true
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          addIncome(this.form).then(() => {
            this.$modal.msgSuccess('已创建，等待财务核验')
            this.open = false
            this.getList()
          })
        }
      })
    }
  }
}
</script>
