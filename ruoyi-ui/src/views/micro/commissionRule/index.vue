<template>
  <div class="app-container">
    <el-alert
      title="员工分成规则（M35 T35.2）：单场活动结算分账口径"
      type="info"
      description="三种类型：固定金额（元/场）/ 按比例（0~100%）/ 阶梯（预留）。规则变更后历史活动仍用快照，历史账不可变。★与「员工提成」（月度薪酬口径）独立。"
      show-icon
      :closable="false"
      class="mb8"
    />

    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="员工姓名" prop="staffName">
        <el-input v-model="queryParams.staffName" placeholder="请输入员工姓名" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="分成类型" prop="commissionType">
        <el-select v-model="queryParams.commissionType" placeholder="全部类型" clearable style="width: 160px;">
          <el-option label="固定金额" value="FIXED_AMOUNT" />
          <el-option label="按比例" value="PERCENTAGE" />
          <el-option label="阶梯(预留)" value="TIERED" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="isActive">
        <el-select v-model="queryParams.isActive" placeholder="全部状态" clearable style="width: 120px;">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['micro:commissionRule:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['micro:commissionRule:export']">导出</el-button>
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
      <el-table-column label="分成类型" align="center" width="110">
        <template slot-scope="scope">
          <el-tag size="mini" :type="typeTag(scope.row.commissionType)">{{ typeLabel(scope.row.commissionType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="固定金额(元/场)" align="right" width="130">
        <template slot-scope="scope">
          <span v-if="scope.row.commissionType === 'FIXED_AMOUNT'">¥ {{ fmt(scope.row.fixedAmount) }}</span>
          <span v-else style="color:#c0c4cc;">-</span>
        </template>
      </el-table-column>
      <el-table-column label="百分比(%)" align="center" width="100">
        <template slot-scope="scope">
          <span v-if="scope.row.commissionType === 'PERCENTAGE'">{{ fmt(scope.row.percentageRate) }}%</span>
          <span v-else style="color:#c0c4cc;">-</span>
        </template>
      </el-table-column>
      <el-table-column label="追加消费计提成" align="center" width="120">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.includeAddonInCommission === 1 ? 'success' : 'info'">
            {{ scope.row.includeAddonInCommission === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="月底薪(元)" align="right" width="110">
        <template slot-scope="scope">¥ {{ fmt(scope.row.baseSalary) }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.isActive === 1 ? 'success' : 'info'" size="mini">{{ scope.row.isActive === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="left" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" align="center" width="140" fixed="right">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(scope.row)" v-hasPermi="['micro:commissionRule:edit']">编辑</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['micro:commissionRule:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogOpen" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="130px">
        <el-form-item label="员工" prop="staffId">
          <el-select v-model="form.staffId" placeholder="请选择员工" filterable style="width:100%;" :disabled="!!form.id">
            <el-option v-for="s in staffOptions" :key="s.userId" :label="s.nickName" :value="s.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="分成类型" prop="commissionType">
          <el-radio-group v-model="form.commissionType" @change="onTypeChange">
            <el-radio label="FIXED_AMOUNT">固定金额</el-radio>
            <el-radio label="PERCENTAGE">按比例</el-radio>
            <el-radio label="TIERED" disabled>阶梯(预留)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.commissionType === 'FIXED_AMOUNT'" label="固定金额(元/场)" prop="fixedAmount">
          <el-input-number v-model="form.fixedAmount" :min="0" :precision="2" :step="50" controls-position="right" style="width:100%;" />
          <div style="font-size:12px;color:#909399;">每场活动固定发放该金额，与人数/收入无关。</div>
        </el-form-item>
        <el-form-item v-if="form.commissionType === 'PERCENTAGE'" label="百分比(%)" prop="percentageRate">
          <el-input-number v-model="form.percentageRate" :min="0" :max="100" :precision="2" :step="1" controls-position="right" style="width:100%;" />
          <div style="font-size:12px;color:#909399;">分成 = 活动收入 × 百分比；如填 10 表示 10%。</div>
        </el-form-item>
        <el-form-item v-if="form.commissionType === 'TIERED'" label="阶梯规则 JSON" prop="tieredConfig">
          <el-input v-model="form.tieredConfig" type="textarea" :rows="3" placeholder='本期预留，如 [{"min":0,"max":10,"amount":100}]' />
        </el-form-item>
        <el-form-item label="追加消费计提成" prop="includeAddonInCommission">
          <el-switch v-model="form.includeAddonInCommission" :active-value="1" :inactive-value="0" active-text="是" inactive-text="否" />
          <div style="font-size:12px;color:#909399;">开启后饮料零食等追加消费也计入分成基数（默认关）。</div>
        </el-form-item>
        <el-form-item label="月底薪(元)" prop="baseSalary">
          <el-input-number v-model="form.baseSalary" :min="0" :precision="2" :step="500" controls-position="right" style="width:100%;" />
          <div style="font-size:12px;color:#909399;">进 M38 月分摊固定成本，不进单场活动成本。</div>
        </el-form-item>
        <el-form-item label="状态" prop="isActive">
          <el-radio-group v-model="form.isActive">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
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
  listCommissionRule, getCommissionRule, addCommissionRule, updateCommissionRule,
  delCommissionRule, listRuleStaffOptions
} from '@/api/micro/commissionRule'

export default {
  name: 'CommissionRule',
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
      queryParams: { pageNum: 1, pageSize: 10, staffName: null, commissionType: null, isActive: null },
      rules: {
        staffId: [{ required: true, message: '请选择员工', trigger: 'change' }],
        commissionType: [{ required: true, message: '请选择分成类型', trigger: 'change' }]
      }
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
    typeLabel(t) {
      return { FIXED_AMOUNT: '固定金额', PERCENTAGE: '按比例', TIERED: '阶梯' }[t] || t || '-'
    },
    typeTag(t) {
      return { FIXED_AMOUNT: 'warning', PERCENTAGE: 'success', TIERED: 'info' }[t] || 'info'
    },
    getList() {
      this.loading = true
      listCommissionRule(this.queryParams).then(res => {
        this.dataList = res.rows || []
        this.total = res.total || 0
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    loadStaffOptions() {
      listRuleStaffOptions().then(res => { this.staffOptions = res.data || [] }).catch(() => { this.staffOptions = [] })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm('queryForm'); this.handleQuery() },
    reset() {
      this.form = {
        id: null, staffId: null, commissionType: 'FIXED_AMOUNT',
        fixedAmount: 0, percentageRate: 0, tieredConfig: null,
        includeAddonInCommission: 0, baseSalary: 0, isActive: 1, remark: ''
      }
    },
    onTypeChange() {
      // 切换类型时清空另一类型的值，避免脏数据
      if (this.form.commissionType === 'FIXED_AMOUNT') {
        this.form.percentageRate = 0
        this.form.tieredConfig = null
      } else if (this.form.commissionType === 'PERCENTAGE') {
        this.form.fixedAmount = 0
        this.form.tieredConfig = null
      }
    },
    handleAdd() { this.reset(); this.dialogTitle = '新增分成规则'; this.dialogOpen = true },
    handleEdit(row) {
      getCommissionRule(row.id).then(res => {
        const d = res.data || row
        this.form = {
          ...d,
          fixedAmount: Number(d.fixedAmount || 0),
          percentageRate: Number(d.percentageRate || 0),
          baseSalary: Number(d.baseSalary || 0),
          includeAddonInCommission: d.includeAddonInCommission == null ? 0 : d.includeAddonInCommission,
          isActive: d.isActive == null ? 1 : d.isActive
        }
        this.dialogTitle = '编辑分成规则'
        this.dialogOpen = true
      })
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) return
        const payload = { ...this.form }
        // 按类型清理无关字段
        if (payload.commissionType === 'FIXED_AMOUNT') {
          payload.percentageRate = null
          payload.tieredConfig = null
        } else if (payload.commissionType === 'PERCENTAGE') {
          payload.fixedAmount = null
          payload.tieredConfig = null
        }
        delete payload.staffName
        delete payload.deptName
        delete payload.createTime
        delete payload.updateTime
        const action = payload.id ? updateCommissionRule(payload) : addCommissionRule(payload)
        action.then(() => {
          this.$modal.msgSuccess(payload.id ? '修改成功' : '新增成功')
          this.dialogOpen = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除员工「' + (row.staffName || '') + '」的分成规则吗？删除后新建活动将不再生成该员工快照。')
        .then(() => delCommissionRule(row.id))
        .then(() => { this.$modal.msgSuccess('删除成功'); this.getList() })
        .catch(() => {})
    },
    handleExport() {
      this.download('micro/commissionRule/export', { ...this.queryParams }, `员工分成规则_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
</style>
