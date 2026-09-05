<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="贷款台账" name="loan">
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['fee:loan:edit']">登记贷款</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="warning" plain icon="el-icon-time" size="mini" @click="handleAccrue" v-hasPermi="['fee:loan:edit']">手动计提利息</el-button>
          </el-col>
        </el-row>

        <el-table v-loading="loading" :data="loanList">
          <el-table-column label="贷款名称" align="left" prop="loanName" min-width="140" show-overflow-tooltip />
          <el-table-column label="贷款金额" align="right" prop="amount" width="120" />
          <el-table-column label="年利率(%)" align="right" prop="annualRate" width="100" />
          <el-table-column label="还款方式" align="center" prop="method" width="120">
            <template slot-scope="scope">{{ methodText(scope.row.method) }}</template>
          </el-table-column>
          <el-table-column label="开始日期" align="center" prop="startDate" width="110">
            <template slot-scope="scope">
              <span>{{ parseTime(scope.row.startDate, '{y}-{m}-{d}') }}</span>
            </template>
          </el-table-column>
          <el-table-column label="结束日期" align="center" prop="endDate" width="110">
            <template slot-scope="scope">
              <span>{{ parseTime(scope.row.endDate, '{y}-{m}-{d}') }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" align="center" prop="status" width="90">
            <template slot-scope="scope">
              <el-tag :type="scope.row.status === 'ACTIVE' ? 'success' : 'info'" size="small">{{ scope.row.status === 'ACTIVE' ? '还款中' : '已结清' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
            <template slot-scope="scope">
              <el-button v-if="scope.row.status === 'ACTIVE'" size="mini" type="text" icon="el-icon-edit" @click="handleEdit(scope.row)" v-hasPermi="['fee:loan:edit']">修改</el-button>
              <el-button v-if="scope.row.status === 'ACTIVE'" size="mini" type="text" icon="el-icon-circle-check" @click="handleSettle(scope.row)" v-hasPermi="['fee:loan:edit']">结清</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
      </el-tab-pane>

      <el-tab-pane label="利息计提记录" name="record">
        <el-form :model="recordQuery" ref="recordQueryForm" size="small" :inline="true" label-width="80px">
          <el-form-item label="计提月份" prop="accrualMonth">
            <el-date-picker v-model="recordQuery.accrualMonth" type="month" value-format="yyyy-MM" placeholder="选择月份" clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="handleRecordQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="resetRecordQuery">重置</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="recordLoading" :data="recordList">
          <el-table-column label="计提月份" align="center" prop="accrualMonth" width="110" />
          <el-table-column label="贷款名称" align="left" prop="loanName" min-width="150" show-overflow-tooltip />
          <el-table-column label="月利息金额" align="right" prop="amount" width="120" />
          <el-table-column label="计提时间" align="center" prop="createTime" width="160">
            <template slot-scope="scope">
              <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="recordTotal > 0" :total="recordTotal" :page.sync="recordQuery.pageNum" :limit.sync="recordQuery.pageSize" @pagination="getRecordList" />
      </el-tab-pane>
    </el-tabs>

    <!-- 登记/修改贷款 -->
    <el-dialog :title="form.id ? '修改贷款' : '登记贷款'" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="贷款名称" prop="loanName">
          <el-input v-model="form.loanName" placeholder="如：XX银行经营贷" />
        </el-form-item>
        <el-form-item label="贷款金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="年利率(%)" prop="annualRate">
          <el-input-number v-model="form.annualRate" :min="0" :max="100" :precision="4" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="还款方式" prop="method">
          <el-select v-model="form.method" placeholder="请选择还款方式" style="width: 100%;">
            <el-option label="等额本息" value="EQUAL_INSTALLMENT" />
            <el-option label="先息后本" value="INTEREST_FIRST" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker v-model="form.startDate" type="date" value-format="yyyy-MM-dd" placeholder="选择开始日期" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker v-model="form.endDate" type="date" value-format="yyyy-MM-dd" placeholder="选择结束日期" style="width: 100%;" />
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
import { listLoan, saveLoan, settleLoan, listLoanInterest, accrueInterests } from '@/api/system/fee/fundLoanInterest'

export default {
  name: 'FundLoan',
  data() {
    return {
      loading: true,
      recordLoading: false,
      activeTab: 'loan',
      total: 0,
      recordTotal: 0,
      loanList: [],
      recordList: [],
      open: false,
      queryParams: { pageNum: 1, pageSize: 10 },
      recordQuery: { pageNum: 1, pageSize: 10, accrualMonth: null },
      form: {},
      rules: {
        loanName: [{ required: true, message: '贷款名称不能为空', trigger: 'blur' }],
        amount: [{ required: true, message: '贷款金额不能为空', trigger: 'blur' }],
        annualRate: [{ required: true, message: '年利率不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
    this.getRecordList()
  },
  methods: {
    getList() {
      this.loading = true
      listLoan(this.queryParams).then(response => {
        this.loanList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    getRecordList() {
      this.recordLoading = true
      listLoanInterest(this.recordQuery).then(response => {
        this.recordList = response.rows
        this.recordTotal = response.total
        this.recordLoading = false
      }).catch(() => { this.recordLoading = false })
    },
    methodText(method) {
      const map = { EQUAL_INSTALLMENT: '等额本息', INTEREST_FIRST: '先息后本' }
      return map[method] || method
    },
    handleRecordQuery() {
      this.recordQuery.pageNum = 1
      this.getRecordList()
    },
    resetRecordQuery() {
      this.resetForm('recordQueryForm')
      this.handleRecordQuery()
    },
    handleAdd() {
      this.form = { loanName: null, amount: null, annualRate: null, method: 'EQUAL_INSTALLMENT', startDate: null, endDate: null }
      this.open = true
    },
    handleEdit(row) {
      this.form = { ...row }
      this.open = true
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          saveLoan(this.form).then(() => {
            this.$modal.msgSuccess('保存成功')
            this.open = false
            this.getList()
          })
        }
      })
    },
    handleSettle(row) {
      this.$modal.confirm('结清后停止计息，确认结清【' + row.loanName + '】？').then(() => {
        return settleLoan(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('已结清')
      }).catch(() => {})
    },
    handleAccrue() {
      this.$prompt('请输入计提月份', '手动计提利息', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /^\d{4}-\d{2}$/,
        inputErrorMessage: '格式为 yyyy-MM，如 2026-08'
      }).then(({ value }) => {
        return accrueInterests(value)
      }).then(() => {
        this.getRecordList()
        this.$modal.msgSuccess('计提完成')
      }).catch(() => {})
    }
  }
}
</script>
