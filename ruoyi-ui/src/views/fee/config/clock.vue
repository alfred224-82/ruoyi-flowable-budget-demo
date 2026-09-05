<template>
  <div class="app-container">
    <el-alert title="打卡触发规则（高级版）：命中规则的报销单提交时必须携带定位；定位存疑仅标记不拦截。内置模板不可修改，可新增租户级规则覆盖。" type="info" :closable="false" style="margin-bottom: 15px;" />

    <el-form :inline="true" size="small">
      <el-form-item>
        <el-button type="primary" plain icon="el-icon-plus" @click="handleAdd" v-hasPermi="['fee:clock:edit']">新增规则</el-button>
        <el-button icon="el-icon-refresh" @click="loadList">刷新</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="ruleList" border size="small" v-loading="loading">
      <el-table-column label="规则编码" width="200" align="center" prop="ruleCode" />
      <el-table-column label="规则名称" min-width="180" prop="ruleName" />
      <el-table-column label="阈值" width="120" align="center" prop="threshold">
        <template slot-scope="scope">{{ scope.row.threshold == null ? '-' : scope.row.threshold }}</template>
      </el-table-column>
      <el-table-column label="大类范围" min-width="160" prop="categoryScope">
        <template slot-scope="scope">{{ scope.row.categoryScope || '全部' }}</template>
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
          <el-button size="mini" type="text" icon="el-icon-edit" :disabled="scope.row.tenantId === 0" @click="handleEdit(scope.row)" v-hasPermi="['fee:clock:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" style="color:#f56c6c;" :disabled="scope.row.tenantId === 0" @click="handleDel(scope.row)" v-hasPermi="['fee:clock:edit']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="form.id ? '修改打卡触发规则' : '新增打卡触发规则'" :visible.sync="dialogVisible" width="520px" append-to-body>
      <el-form :model="form" :rules="rules" ref="ruleForm" label-width="110px">
        <el-form-item label="规则编码" prop="ruleCode">
          <el-select v-model="form.ruleCode" placeholder="请选择规则类型" style="width: 100%;">
            <el-option label="差旅招待大类" value="TRAVEL_CATEGORY" />
            <el-option label="金额阈值" value="AMOUNT_THRESHOLD" />
            <el-option label="历史异常次数" value="HISTORY_ABNORMAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="form.ruleName" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="阈值">
          <el-input-number v-model="form.threshold" :min="0" :precision="2" controls-position="right" style="width: 100%;" placeholder="金额阈值/异常次数" />
        </el-form-item>
        <el-form-item label="大类范围">
          <el-input v-model="form.categoryScope" placeholder="逗号分隔大类编码，留空=全部" />
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
import { listClockConfig, addClockConfig, updateClockConfig, delClockConfig } from '@/api/system/fee/fundClock'

export default {
  name: 'FeeClockConfig',
  data() {
    return {
      loading: false,
      ruleList: [],
      dialogVisible: false,
      submitLoading: false,
      form: {},
      rules: {
        ruleCode: [{ required: true, message: '请选择规则编码', trigger: 'change' }],
        ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.loadList()
  },
  methods: {
    loadList() {
      this.loading = true
      listClockConfig().then(res => {
        this.ruleList = res.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleAdd() {
      this.form = { ruleCode: null, ruleName: '', threshold: null, categoryScope: '', isEnabled: 1 }
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
        const action = this.form.id ? updateClockConfig : addClockConfig
        action(this.form).then(() => {
          this.$modal.msgSuccess('保存成功')
          this.dialogVisible = false
          this.loadList()
        }).finally(() => { this.submitLoading = false })
      })
    },
    handleDel(row) {
      this.$modal.confirm('确认删除打卡触发规则「' + row.ruleName + '」？').then(() => {
        return delClockConfig(row.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.loadList()
      }).catch(() => {})
    }
  }
}
</script>
