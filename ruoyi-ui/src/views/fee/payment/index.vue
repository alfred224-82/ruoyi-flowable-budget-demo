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
          <el-option label="待审批" value="PENDING" />
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['fee:payment:add']">登记付款单</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="paymentList">
      <el-table-column label="付款单号" align="center" prop="paymentNo" width="150" show-overflow-tooltip />
      <el-table-column label="项目" align="left" prop="projectName" min-width="150" show-overflow-tooltip />
      <el-table-column label="收款方" align="left" prop="payee" min-width="130" show-overflow-tooltip />
      <el-table-column label="金额" align="right" prop="amount" width="110" />
      <el-table-column label="付款类型" align="center" prop="payType" width="100">
        <template slot-scope="scope">{{ payTypeText(scope.row.payType) }}</template>
      </el-table-column>
      <el-table-column label="付款日期" align="center" prop="paymentDate" width="110">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.paymentDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="发票" align="center" prop="invoiceStatus" width="80">
        <template slot-scope="scope">
          {{ invoiceText(scope.row.invoiceStatus) }}
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
            <el-button size="mini" type="text" icon="el-icon-check" @click="handleApprove(scope.row)" v-hasPermi="['fee:payment:approve']">审批</el-button>
            <el-button size="mini" type="text" icon="el-icon-close" @click="handleReject(scope.row)" v-hasPermi="['fee:payment:reject']">驳回</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 登记付款单 -->
    <el-dialog title="登记付款单（必选项目，提交后待老板审批）" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="所属项目" prop="projectId">
          <el-select v-model="form.projectId" placeholder="请选择项目" filterable style="width: 100%;">
            <el-option v-for="p in activeProjects" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="收款方" prop="payee">
          <el-input v-model="form.payee" placeholder="对方单位名称" />
        </el-form-item>
        <el-form-item label="付款方" prop="payer">
          <el-input v-model="form.payer" placeholder="我方单位名称" />
        </el-form-item>
        <el-form-item label="付款金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" style="width: 100%;" placeholder="元" />
        </el-form-item>
        <el-form-item label="付款类型" prop="payType">
          <el-select v-model="form.payType" placeholder="请选择付款类型" style="width: 100%;">
            <el-option label="工程款" value="ENGINEERING" />
            <el-option label="材料款" value="MATERIAL" />
            <el-option label="服务费" value="SERVICE" />
            <el-option label="租金" value="RENT" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="付款日期" prop="paymentDate">
          <el-date-picker v-model="form.paymentDate" type="date" value-format="yyyy-MM-dd" placeholder="选择付款日期" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="发票状态" prop="invoiceStatus">
          <el-radio-group v-model="form.invoiceStatus">
            <el-radio label="HAS">已有发票</el-radio>
            <el-radio label="NONE">无发票</el-radio>
            <el-radio label="PENDING">待补票</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="付款说明" />
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
import { listPayment, addPayment, approvePayment, rejectPayment } from '@/api/system/fee/fundPayment'
import { listActiveProject } from '@/api/system/fee/fundProject'

export default {
  name: 'FundPayment',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      paymentList: [],
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
        projectId: [{ required: true, message: '付款单必须关联项目', trigger: 'change' }],
        payee: [{ required: true, message: '收款方不能为空', trigger: 'blur' }],
        amount: [{ required: true, message: '付款金额不能为空', trigger: 'blur' }]
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
      listPayment(this.queryParams).then(response => {
        this.paymentList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    payTypeText(type) {
      const map = { ENGINEERING: '工程款', MATERIAL: '材料款', SERVICE: '服务费', RENT: '租金', OTHER: '其他' }
      return map[type] || type
    },
    invoiceText(status) {
      const map = { HAS: '已有', NONE: '无', PENDING: '待补' }
      return map[status] || status
    },
    statusText(status) {
      const map = { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回' }
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
      this.form = { projectId: null, payee: null, payer: null, amount: null, payType: 'OTHER', paymentDate: null, invoiceStatus: 'NONE', description: null }
      this.open = true
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          addPayment(this.form).then(() => {
            this.$modal.msgSuccess('登记成功，等待老板审批')
            this.open = false
            this.getList()
          })
        }
      })
    },
    handleApprove(row) {
      this.$modal.confirm('审批通过将扣减项目预算并记入总支出，确认？').then(() => {
        return approvePayment(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('审批通过')
      }).catch(() => {})
    },
    handleReject(row) {
      this.$prompt('请输入驳回原因', '驳回付款单', {
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }).then(({ value }) => {
        return rejectPayment(row.id, value)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('已驳回')
      }).catch(() => {})
    }
  }
}
</script>
