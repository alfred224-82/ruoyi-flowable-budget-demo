<template>
  <div class="app-container">
    <el-alert title="告警规则配置（Admin 专属）" type="info"
      description="配置系统告警规则（License 到期/合规命中/租户禁用/服务宕机），定义触发阈值、严重程度和通知渠道。"
      show-icon :closable="false" class="mb16" />

    <!-- ===== 筛选 ===== -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="90px">
      <el-form-item label="规则类型" prop="ruleType">
        <el-select v-model="queryParams.ruleType" placeholder="全部" clearable>
          <el-option label="License 到期" value="LICENSE_EXPIRE" />
          <el-option label="合规命中" value="COMPLIANCE_HIT" />
          <el-option label="租户禁用" value="TENANT_DISABLED" />
          <el-option label="服务宕机" value="SERVICE_DOWN" />
        </el-select>
      </el-form-item>
      <el-form-item label="严重程度" prop="severity">
        <el-select v-model="queryParams.severity" placeholder="全部" clearable>
          <el-option label="信息" value="INFO" />
          <el-option label="警告" value="WARNING" />
          <el-option label="严重" value="CRITICAL" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="enabled">
        <el-select v-model="queryParams.enabled" placeholder="全部" clearable>
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- ===== 操作栏 ===== -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" icon="el-icon-plus" size="mini" @click="handleAdd">新增规则</el-button>
      </el-col>
    </el-row>

    <!-- ===== 列表 ===== -->
    <el-table v-loading="loading" :data="ruleList" border>
      <el-table-column label="ID" align="center" prop="id" width="60" />
      <el-table-column label="规则名称" align="left" prop="ruleName" min-width="160" show-overflow-tooltip />
      <el-table-column label="规则类型" align="center" width="120">
        <template slot-scope="scope">
          <el-tag :type="ruleTypeTag(scope.row.ruleType)" size="small">{{ ruleTypeLabel(scope.row.ruleType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="触发阈值" align="center" prop="thresholdValue" width="90" />
      <el-table-column label="严重程度" align="center" width="90">
        <template slot-scope="scope">
          <el-tag :type="severityTag(scope.row.severity)" size="small">{{ severityLabel(scope.row.severity) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="通知渠道" align="center" width="100">
        <template slot-scope="scope">
          {{ channelLabel(scope.row.notifyChannel) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="80">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.enabled" :active-value="1" :inactive-value="0"
            @change="handleToggle(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="备注" align="left" prop="remark" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" align="center" width="160">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" style="color:#f56c6c;"
            @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- ===== 新增/编辑弹窗 ===== -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogOpen" width="600px">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="form.ruleName" :maxlength="100" placeholder="如：License 30天到期预警" />
        </el-form-item>
        <el-form-item label="规则类型" prop="ruleType">
          <el-select v-model="form.ruleType" placeholder="请选择" style="width:100%;">
            <el-option label="License 到期" value="LICENSE_EXPIRE" />
            <el-option label="合规命中" value="COMPLIANCE_HIT" />
            <el-option label="租户禁用" value="TENANT_DISABLED" />
            <el-option label="服务宕机" value="SERVICE_DOWN" />
          </el-select>
        </el-form-item>
        <el-form-item label="触发阈值" prop="thresholdValue">
          <el-input-number v-model="form.thresholdValue" :min="1" :max="99999" style="width:200px;" />
          <span style="margin-left:8px;color:#909399;">次/天</span>
        </el-form-item>
        <el-form-item label="严重程度" prop="severity">
          <el-radio-group v-model="form.severity">
            <el-radio label="INFO">信息</el-radio>
            <el-radio label="WARNING">警告</el-radio>
            <el-radio label="CRITICAL">严重</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="通知渠道" prop="notifyChannel">
          <el-checkbox-group v-model="form.channels">
            <el-checkbox label="SYSTEM">站内消息</el-checkbox>
            <el-checkbox label="EMAIL">邮件</el-checkbox>
            <el-checkbox label="SMS">短信</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" :maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogOpen = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { listAlertRule, getAlertRule, addAlertRule, updateAlertRule, deleteAlertRule, toggleAlertRule } from '@/api/system/alertRule'

export default {
  name: 'AlertRule',
  data() {
    return {
      loading: true,
      total: 0,
      ruleList: [],
      queryParams: { pageNum: 1, pageSize: 10, ruleType: null, severity: null, enabled: null },
      dialogOpen: false,
      dialogTitle: '',
      form: {},
      rules: {
        ruleName: [{ required: true, message: '规则名称不能为空', trigger: 'blur' }],
        ruleType: [{ required: true, message: '规则类型不能为空', trigger: 'change' }],
        thresholdValue: [{ required: true, message: '触发阈值不能为空', trigger: 'blur' }],
        severity: [{ required: true, message: '严重程度不能为空', trigger: 'change' }],
        notifyChannel: [{ required: true, message: '通知渠道不能为空', trigger: 'change' }]
      }
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      listAlertRule(this.queryParams).then(res => {
        this.ruleList = res.rows
        this.total = res.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm('queryForm'); this.handleQuery() },
    handleAdd() {
      this.form = { ruleType: 'LICENSE_EXPIRE', thresholdValue: 1, severity: 'WARNING', channels: ['SYSTEM'], enabled: 1 }
      this.dialogTitle = '新增告警规则'
      this.dialogOpen = true
    },
    handleEdit(row) {
      getAlertRule(row.id).then(res => {
        const data = res.data
        // 将字符串转为数组供 checkbox-group 使用
        if (data.notifyChannel) {
          data.channels = data.notifyChannel.split(',')
        } else {
          data.channels = []
        }
        this.form = data
        this.dialogTitle = '编辑告警规则'
        this.dialogOpen = true
      })
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) return
        // 将 channels 数组转回字符串
        const submitData = { ...this.form }
        submitData.notifyChannel = submitData.channels.join(',')
        delete submitData.channels
        if (this.form.id) {
          updateAlertRule(submitData).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.dialogOpen = false
            this.getList()
          })
        } else {
          addAlertRule(submitData).then(() => {
            this.$modal.msgSuccess('新增成功')
            this.dialogOpen = false
            this.getList()
          })
        }
      })
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除规则"' + row.ruleName + '"？').then(() => {
        return deleteAlertRule(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleToggle(row) {
      toggleAlertRule(row.id, row.enabled).then(() => {
        this.$modal.msgSuccess(row.enabled ? '已启用' : '已停用')
      }).catch(() => {
        row.enabled = row.enabled === 1 ? 0 : 1 // 回滚
      })
    },
    ruleTypeLabel(t) {
      const map = { LICENSE_EXPIRE: 'License 到期', COMPLIANCE_HIT: '合规命中', TENANT_DISABLED: '租户禁用', SERVICE_DOWN: '服务宕机' }
      return map[t] || t
    },
    ruleTypeTag(t) {
      const map = { LICENSE_EXPIRE: 'warning', COMPLIANCE_HIT: 'danger', TENANT_DISABLED: 'info', SERVICE_DOWN: 'danger' }
      return map[t] || ''
    },
    severityLabel(s) {
      const map = { INFO: '信息', WARNING: '警告', CRITICAL: '严重' }
      return map[s] || s
    },
    severityTag(s) {
      const map = { INFO: 'info', WARNING: 'warning', CRITICAL: 'danger' }
      return map[s] || ''
    },
    channelLabel(c) {
      const parts = (c || '').split(',').map(ch => ({ SYSTEM: '站内', EMAIL: '邮件', SMS: '短信' }[ch] || ch))
      return parts.join(' / ')
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
</style>
