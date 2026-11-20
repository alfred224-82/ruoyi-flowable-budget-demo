<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="员工姓名" prop="staffName">
        <el-input v-model="queryParams.staffName" placeholder="请输入员工姓名" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 140px;">
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['micro:staffCommission:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['micro:staffCommission:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 列表 -->
    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="员工姓名" align="left" prop="staffName" min-width="100" show-overflow-tooltip />
      <el-table-column label="部门" align="center" prop="deptName" width="120" show-overflow-tooltip>
        <template slot-scope="scope">
          <span v-if="scope.row.deptName">{{ scope.row.deptName }}</span>
          <span v-else style="color:#c0c4cc;">-</span>
        </template>
      </el-table-column>
      <el-table-column label="基础工资" align="right" width="110">
        <template slot-scope="scope">¥ {{ fmt(scope.row.baseSalary) }}</template>
      </el-table-column>
      <el-table-column label="提成比例" align="center" width="100">
        <template slot-scope="scope">{{ rateToPercent(scope.row.commissionRate) }}</template>
      </el-table-column>
      <el-table-column label="当月已完成订单金额" align="right" width="150">
        <template slot-scope="scope">¥ {{ fmt(scope.row.monthlyCompletedAmount) }}</template>
      </el-table-column>
      <el-table-column label="当月提成" align="right" width="110">
        <template slot-scope="scope"><span style="color:#e6a23c;">¥ {{ fmt(scope.row.monthlyCommission) }}</span></template>
      </el-table-column>
      <el-table-column label="当月人力成本" align="right" width="120">
        <template slot-scope="scope"><span style="color:#f56c6c;font-weight:600;">¥ {{ fmt(scope.row.monthlyLaborCost) }}</span></template>
      </el-table-column>
      <el-table-column label="生效日期" align="center" prop="effectiveDate" width="110">
        <template slot-scope="scope">
          <span v-if="scope.row.effectiveDate">{{ scope.row.effectiveDate }}</span>
          <span v-else style="color:#c0c4cc;">-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="mini">{{ scope.row.status === '0' ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="left" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" align="center" width="140" fixed="right">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(scope.row)" v-hasPermi="['micro:staffCommission:edit']">编辑</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['micro:staffCommission:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogOpen" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="员工" prop="staffUserId">
          <el-select v-model="form.staffUserId" placeholder="请选择员工" filterable style="width:100%;" :disabled="!!form.id">
            <el-option v-for="s in staffOptions" :key="s.userId" :label="s.nickName" :value="s.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="基础工资(元)" prop="baseSalary">
          <el-input-number v-model="form.baseSalary" :min="0" :precision="2" :step="100" controls-position="right" style="width:100%;" />
        </el-form-item>
        <el-form-item label="提成比例(%)" prop="ratePercent">
          <el-input-number v-model="form.ratePercent" :min="0" :max="100" :precision="2" :step="0.5" controls-position="right" style="width:100%;" />
          <div style="font-size:12px;color:#909399;line-height:1.4;">提成 = 当月已完成订单金额 × 比例；如填 10 表示 10%。</div>
        </el-form-item>
        <el-form-item v-if="form.id && form.monthlyCompletedAmount != null" label="当月预估">
          <span style="color:#606266;">
            基数 ¥{{ fmt(form.monthlyCompletedAmount) }} × {{ rateToPercent(form.ratePercent != null ? form.ratePercent / 100 : 0) }}
            = <b style="color:#e6a23c;">¥ {{ fmt(estimateCommission) }}</b>
          </span>
        </el-form-item>
        <el-form-item label="生效日期" prop="effectiveDate">
          <el-date-picker v-model="form.effectiveDate" type="date" placeholder="选择生效日期" value-format="yyyy-MM-dd" style="width:100%;" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="dialogOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listStaffCommission, getStaffCommission, addStaffCommission, updateStaffCommission,
  delStaffCommission, listStaffOptions
} from '@/api/micro/staffCommission'

export default {
  name: 'StaffCommission',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      dataList: [],
      staffOptions: [],
      dialogOpen: false,
      dialogTitle: '',
      form: {},
      queryParams: { pageNum: 1, pageSize: 10, staffName: null, status: null },
      rules: {
        staffUserId: [{ required: true, message: '请选择员工', trigger: 'change' }],
        ratePercent: [{ required: true, message: '提成比例不能为空', trigger: 'blur' }]
      }
    }
  },
  computed: {
    // 编辑态：按当前弹窗比例实时预估当月提成（基数取列表回填值）
    estimateCommission() {
      const base = Number(this.form.monthlyCompletedAmount || 0)
      const rate = Number(this.form.ratePercent || 0) / 100
      return (base * rate).toFixed(2)
    }
  },
  created() {
    this.getList()
    this.loadStaffOptions()
  },
  methods: {
    fmt(v) {
      return (v == null ? 0 : Number(v)).toFixed(2)
    },
    // 0~1 小数 → 百分比展示（去掉多余小数）
    rateToPercent(rate) {
      const r = Number(rate || 0) * 100
      return (Math.round(r * 100) / 100) + '%'
    },
    getList() {
      this.loading = true
      listStaffCommission(this.queryParams).then(res => {
        this.dataList = res.rows || []
        this.total = res.total || 0
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    loadStaffOptions() {
      listStaffOptions().then(res => { this.staffOptions = res.data || [] }).catch(() => { this.staffOptions = [] })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm('queryForm'); this.handleQuery() },
    reset() {
      this.form = {
        id: null, staffUserId: null, staffName: null, commissionRate: 0,
        ratePercent: 0, baseSalary: 0, effectiveDate: null, status: '0', remark: '',
        monthlyCompletedAmount: null
      }
    },
    handleAdd() { this.reset(); this.dialogTitle = '新增员工提成'; this.dialogOpen = true },
    handleEdit(row) {
      getStaffCommission(row.id).then(res => {
        const d = res.data || row
        this.form = {
          ...d,
          ratePercent: Math.round(Number(d.commissionRate || 0) * 10000) / 100,
          baseSalary: Number(d.baseSalary || 0),
          monthlyCompletedAmount: d.monthlyCompletedAmount
        }
        this.dialogTitle = '编辑员工提成'
        this.dialogOpen = true
      })
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) return
        const payload = { ...this.form }
        // 百分比 → 0~1 小数（后端存储口径）
        payload.commissionRate = Number((Number(this.form.ratePercent || 0) / 100).toFixed(4))
        delete payload.ratePercent
        delete payload.monthlyCompletedAmount
        delete payload.monthlyCommission
        delete payload.monthlyLaborCost
        delete payload.deptName
        const action = payload.id ? updateStaffCommission(payload) : addStaffCommission(payload)
        action.then(() => {
          this.$modal.msgSuccess(payload.id ? '修改成功' : '新增成功')
          this.dialogOpen = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除员工「' + (row.staffName || '') + '」的提成配置吗？')
        .then(() => delStaffCommission(row.id))
        .then(() => { this.$modal.msgSuccess('删除成功'); this.getList() })
        .catch(() => {})
    },
    handleExport() {
      this.download('micro/staffCommission/export', { ...this.queryParams }, `员工提成_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
</style>
