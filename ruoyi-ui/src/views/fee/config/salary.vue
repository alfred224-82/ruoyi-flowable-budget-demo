<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="薪资基数" name="salary">
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['fee:staffSalary:edit']">维护薪资基数</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="warning" plain icon="el-icon-time" size="mini" @click="handleAccrue" v-hasPermi="['fee:staffSalary:edit']">手动计提</el-button>
          </el-col>
        </el-row>

        <el-table v-loading="loading" :data="salaryList">
          <el-table-column label="员工姓名" align="center" prop="userName" width="120" />
          <el-table-column label="月薪基数" align="right" prop="monthlyBase" width="110" />
          <el-table-column label="社保比例(%)" align="right" prop="socialRate" width="100" />
          <el-table-column label="社保金额" align="right" prop="socialAmount" width="110" />
          <el-table-column label="公积金比例(%)" align="right" prop="providentRate" width="110" />
          <el-table-column label="公积金金额" align="right" prop="providentAmount" width="110" />
          <el-table-column label="其他补贴" align="right" prop="otherAllowance" width="110" />
          <el-table-column label="生效日期" align="center" prop="effectiveDate" width="110">
            <template slot-scope="scope">
              <span>{{ parseTime(scope.row.effectiveDate, '{y}-{m}-{d}') }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" align="center" prop="status" width="80">
            <template slot-scope="scope">
              <el-tag :type="scope.row.status === 'ACTIVE' ? 'success' : 'info'" size="small">{{ scope.row.status === 'ACTIVE' ? '在职' : '离职' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
            <template slot-scope="scope">
              <el-button v-if="scope.row.status === 'ACTIVE'" size="mini" type="text" icon="el-icon-edit" @click="handleEdit(scope.row)" v-hasPermi="['fee:staffSalary:edit']">修改</el-button>
              <el-button v-if="scope.row.status === 'ACTIVE'" size="mini" type="text" icon="el-icon-user" @click="handleResign(scope.row)" v-hasPermi="['fee:staffSalary:edit']">离职</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
      </el-tab-pane>

      <el-tab-pane label="计提记录" name="record">
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
          <el-table-column label="员工姓名" align="center" prop="userName" width="120" />
          <el-table-column label="月薪基数" align="right" prop="baseAmount" width="110" />
          <el-table-column label="社保(公司)" align="right" prop="socialAmount" width="110" />
          <el-table-column label="公积金(公司)" align="right" prop="providentAmount" width="120" />
          <el-table-column label="其他补贴" align="right" prop="allowanceAmount" width="110" />
          <el-table-column label="计提总额" align="right" prop="totalAmount" width="110" />
          <el-table-column label="计提时间" align="center" prop="createTime" width="160">
            <template slot-scope="scope">
              <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="recordTotal > 0" :total="recordTotal" :page.sync="recordQuery.pageNum" :limit.sync="recordQuery.pageSize" @pagination="getRecordList" />
      </el-tab-pane>
    </el-tabs>

    <!-- 维护薪资基数 -->
    <el-dialog :title="form.id ? '修改薪资基数' : '新增薪资基数'" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="员工姓名" prop="userName">
          <el-input v-model="form.userName" placeholder="请输入员工姓名" />
        </el-form-item>
        <el-form-item label="月薪基数" prop="monthlyBase">
          <el-input-number v-model="form.monthlyBase" :min="0" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="社保比例(%)" prop="socialRate">
          <el-input-number v-model="form.socialRate" :min="0" :max="100" :precision="2" style="width: 100%;" placeholder="公司承担部分" />
        </el-form-item>
        <el-form-item label="公积金比例(%)" prop="providentRate">
          <el-input-number v-model="form.providentRate" :min="0" :max="100" :precision="2" style="width: 100%;" placeholder="公司承担部分" />
        </el-form-item>
        <el-form-item label="其他补贴" prop="otherAllowance">
          <el-input-number v-model="form.otherAllowance" :min="0" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="生效日期" prop="effectiveDate">
          <el-date-picker v-model="form.effectiveDate" type="date" value-format="yyyy-MM-dd" placeholder="选择生效日期" style="width: 100%;" />
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
import { listSalary, saveSalary, resignSalary, listSalaryRecord, accrueSalaries } from '@/api/system/fee/fundSalary'

export default {
  name: 'FundSalary',
  data() {
    return {
      loading: true,
      recordLoading: false,
      activeTab: 'salary',
      total: 0,
      recordTotal: 0,
      salaryList: [],
      recordList: [],
      open: false,
      queryParams: { pageNum: 1, pageSize: 10 },
      recordQuery: { pageNum: 1, pageSize: 10, accrualMonth: null },
      form: {},
      rules: {
        userName: [{ required: true, message: '员工姓名不能为空', trigger: 'blur' }],
        monthlyBase: [{ required: true, message: '月薪基数不能为空', trigger: 'blur' }]
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
      listSalary(this.queryParams).then(response => {
        this.salaryList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    getRecordList() {
      this.recordLoading = true
      listSalaryRecord(this.recordQuery).then(response => {
        this.recordList = response.rows
        this.recordTotal = response.total
        this.recordLoading = false
      }).catch(() => { this.recordLoading = false })
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
      this.form = { userName: null, monthlyBase: null, socialRate: 0, providentRate: 0, otherAllowance: 0, effectiveDate: null }
      this.open = true
    },
    handleEdit(row) {
      this.form = { ...row }
      this.open = true
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          saveSalary(this.form).then(() => {
            this.$modal.msgSuccess('保存成功')
            this.open = false
            this.getList()
          })
        }
      })
    },
    handleResign(row) {
      this.$modal.confirm('离职后停止计提，确认【' + row.userName + '】离职？').then(() => {
        return resignSalary(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('已标记离职')
      }).catch(() => {})
    },
    handleAccrue() {
      this.$prompt('请输入计提月份', '手动计提薪资社保', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /^\d{4}-\d{2}$/,
        inputErrorMessage: '格式为 yyyy-MM，如 2026-08'
      }).then(({ value }) => {
        return accrueSalaries(value)
      }).then(() => {
        this.getRecordList()
        this.$modal.msgSuccess('计提完成')
      }).catch(() => {})
    }
  }
}
</script>
