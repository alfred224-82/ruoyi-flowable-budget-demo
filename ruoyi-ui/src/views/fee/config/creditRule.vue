<template>
  <div class="app-container">
    <el-alert title="信用分规则（高级版）：内置默认按时+2/逾期-5/连续3次按时+10，财务可维护租户级规则覆盖。每周一 creditScoreCalcJob 自动重算。" type="info" :closable="false" style="margin-bottom: 15px;" />

    <el-form :inline="true" size="small">
      <el-form-item>
        <el-button type="primary" plain icon="el-icon-plus" @click="handleAdd" v-hasPermi="['fee:creditRule:edit']">新增规则</el-button>
        <el-button icon="el-icon-refresh" @click="loadList">刷新</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="ruleList" border size="small" v-loading="loading">
      <el-table-column label="事件类型" width="200" align="center">
        <template slot-scope="scope">{{ eventText(scope.row.eventType) }}</template>
      </el-table-column>
      <el-table-column label="加减分值" width="120" align="center">
        <template slot-scope="scope">
          <span :style="Number(scope.row.scoreDelta) >= 0 ? 'color:#67c23a;' : 'color:#f56c6c;'">
            {{ Number(scope.row.scoreDelta) >= 0 ? '+' : '' }}{{ scope.row.scoreDelta }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="触发阈值" width="120" align="center" prop="threshold">
        <template slot-scope="scope">{{ scope.row.threshold == null ? '-' : scope.row.threshold }}</template>
      </el-table-column>
      <el-table-column label="来源" width="90" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.tenantId === 0 ? 'info' : 'success'" size="small">{{ scope.row.tenantId === 0 ? '内置' : '自定义' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="是否启用" width="100" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.isEnabled === 1 ? 'success' : 'info'" size="small">{{ scope.row.isEnabled === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" :disabled="scope.row.tenantId === 0" @click="handleEdit(scope.row)" v-hasPermi="['fee:creditRule:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" style="color:#f56c6c;" :disabled="scope.row.tenantId === 0" @click="handleDel(scope.row)" v-hasPermi="['fee:creditRule:edit']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="form.id ? '修改信用规则' : '新增信用规则'" :visible.sync="dialogVisible" width="520px" append-to-body>
      <el-form :model="form" :rules="rules" ref="ruleForm" label-width="110px">
        <el-form-item label="事件类型" prop="eventType">
          <el-select v-model="form.eventType" placeholder="请选择事件类型" style="width: 100%;">
            <el-option label="补票按时" value="ON_TIME" />
            <el-option label="补票逾期" value="OVERDUE" />
            <el-option label="连续按时奖励" value="CONSECUTIVE_BONUS" />
          </el-select>
        </el-form-item>
        <el-form-item label="加减分值" prop="scoreDelta">
          <el-input-number v-model="form.scoreDelta" :min="-100" :max="100" controls-position="right" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="触发阈值">
          <el-input-number v-model="form.threshold" :min="1" controls-position="right" style="width: 100%;" placeholder="连续次数等" />
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="form.isEnabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listCreditRule, addCreditRule, updateCreditRule, delCreditRule } from '@/api/system/fee/fundCredit'

export default {
  name: 'FeeCreditRule',
  data() {
    return {
      loading: false,
      ruleList: [],
      dialogVisible: false,
      submitLoading: false,
      form: {},
      rules: {
        eventType: [{ required: true, message: '请选择事件类型', trigger: 'change' }],
        scoreDelta: [{ required: true, message: '请填写加减分值', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.loadList()
  },
  methods: {
    eventText(type) {
      const map = { ON_TIME: '补票按时', OVERDUE: '补票逾期', CONSECUTIVE_BONUS: '连续按时奖励' }
      return map[type] || type
    },
    loadList() {
      this.loading = true
      listCreditRule().then(res => {
        this.ruleList = res.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleAdd() {
      this.form = { eventType: null, scoreDelta: null, threshold: null, isEnabled: 1 }
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.form = { ...row }
      this.dialogVisible = true
    },
    submitForm() {
      this.$refs.ruleForm.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        const action = this.form.id ? updateCreditRule : addCreditRule
        action(this.form).then(() => {
          this.$modal.msgSuccess('保存成功')
          this.dialogVisible = false
          this.loadList()
        }).finally(() => { this.submitLoading = false })
      })
    },
    handleDel(row) {
      this.$modal.confirm('确认删除该信用规则？').then(() => {
        return delCreditRule(row.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.loadList()
      }).catch(() => {})
    }
  }
}
</script>
