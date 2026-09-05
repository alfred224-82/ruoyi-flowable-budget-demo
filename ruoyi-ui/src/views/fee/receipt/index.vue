<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="项目" prop="projectId">
        <el-select v-model="queryParams.projectId" placeholder="全部项目" clearable filterable>
          <el-option v-for="p in activeProjects" :key="p.id" :label="p.projectName" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable>
          <el-option label="待审核" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['fee:receipt:add']">创建收款单</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="receiptList">
      <el-table-column label="收款单号" align="center" prop="receiptNo" width="150" show-overflow-tooltip />
      <el-table-column label="项目" align="left" prop="projectName" min-width="150" show-overflow-tooltip />
      <el-table-column label="付款方" align="left" prop="payer" min-width="130" show-overflow-tooltip />
      <el-table-column label="金额" align="right" prop="amount" width="110" />
      <el-table-column label="收款类型" align="center" prop="receiptType" width="110">
        <template slot-scope="scope">{{ receiptTypeText(scope.row.receiptType) }}</template>
      </el-table-column>
      <el-table-column label="收款日期" align="center" prop="receivedDate" width="110">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.receivedDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template slot-scope="scope">
          <el-tag :type="statusTag(scope.row.status)" size="small">{{ statusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope">
          <template v-if="scope.row.status === 'PENDING'">
            <el-button size="mini" type="text" icon="el-icon-check" @click="handleApprove(scope.row)" v-hasPermi="['fee:receipt:approve']">审核通过</el-button>
            <el-button size="mini" type="text" icon="el-icon-close" @click="handleReject(scope.row)" v-hasPermi="['fee:receipt:approve']">驳回</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 创建收款单 -->
    <el-dialog title="创建收款单（审核通过联动生成已核验收入确认）" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="所属项目" prop="projectId">
          <el-select v-model="form.projectId" placeholder="请选择项目" filterable style="width: 100%;">
            <el-option v-for="p in activeProjects" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="付款方" prop="payer">
          <el-input v-model="form.payer" placeholder="对方单位名称" />
        </el-form-item>
        <el-form-item label="收款金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" style="width: 100%;" placeholder="元" />
        </el-form-item>
        <el-form-item label="收款类型" prop="receiptType">
          <el-select v-model="form.receiptType" placeholder="请选择收款类型" style="width: 100%;">
            <el-option label="项目回款" value="PROJECT_PAYMENT" />
            <el-option label="退款" value="REFUND" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="收款日期" prop="receivedDate">
          <el-date-picker v-model="form.receivedDate" type="date" value-format="yyyy-MM-dd" placeholder="选择收款日期" style="width: 100%;" />
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
import { listReceipt, addReceipt, approveReceipt, rejectReceipt } from '@/api/system/fee/fundReceipt'
import { listActiveProject } from '@/api/system/fee/fundProject'

export default {
  name: 'FundReceipt',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      receiptList: [],
      activeProjects: [],
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        projectId: null,
        status: null
      },
      form: {},
      rules: {
        projectId: [{ required: true, message: '收款单必须关联项目', trigger: 'change' }],
        payer: [{ required: true, message: '付款方不能为空', trigger: 'blur' }],
        amount: [{ required: true, message: '收款金额不能为空', trigger: 'blur' }]
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
      listReceipt(this.queryParams).then(response => {
        this.receiptList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    receiptTypeText(type) {
      const map = { PROJECT_PAYMENT: '项目回款', REFUND: '退款', OTHER: '其他' }
      return map[type] || type
    },
    statusText(status) {
      const map = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }
      return map[status] || status
    },
    statusTag(status) {
      const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
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
    handleAdd() {
      this.form = { projectId: null, payer: null, amount: null, receiptType: 'PROJECT_PAYMENT', receivedDate: null }
      this.open = true
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          addReceipt(this.form).then(() => {
            this.$modal.msgSuccess('创建成功，请审核')
            this.open = false
            this.getList()
          })
        }
      })
    },
    handleApprove(row) {
      this.$modal.confirm('审核通过将更新项目收入并联动生成已核验收入确认，确认？').then(() => {
        return approveReceipt(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('审核通过')
      }).catch(() => {})
    },
    handleReject(row) {
      this.$prompt('请输入驳回原因', '驳回收款单', {
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }).then(({ value }) => {
        return rejectReceipt(row.id, value)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('已驳回')
      }).catch(() => {})
    }
  }
}
</script>
