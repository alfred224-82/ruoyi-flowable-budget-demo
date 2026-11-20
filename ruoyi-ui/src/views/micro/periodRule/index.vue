<template>
  <div class="app-container">
    <el-alert title="周期规则管理（租户管理员）" type="info"
      description="管理周期规则（数字+时间单位，如「1 个月」「15 天」），配置后老板端可选。" show-icon :closable="false" class="mb16" />

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="80px">
      <el-form-item label="规则名称" prop="ruleName">
        <el-input v-model="queryParams.ruleName" placeholder="搜索" clearable :maxlength="100" />
      </el-form-item>
      <el-form-item label="时间单位" prop="periodUnit">
        <el-select v-model="queryParams.periodUnit" placeholder="全部" clearable>
          <el-option label="天" value="DAY" />
          <el-option label="周" value="WEEK" />
          <el-option label="月" value="MONTH" />
          <el-option label="季" value="QUARTER" />
          <el-option label="年" value="YEAR" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        <el-button type="success" icon="el-icon-plus" size="mini" @click="handleAdd">新增</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="ruleList">
      <el-table-column label="ID" align="center" prop="id" width="60" />
      <el-table-column label="规则名称" align="left" prop="ruleName" min-width="140" />
      <el-table-column label="周期" align="center" width="120">
        <template slot-scope="scope">
          {{ scope.row.periodValue }} {{ unitMap[scope.row.periodUnit] || scope.row.periodUnit }}
        </template>
      </el-table-column>
      <el-table-column label="换算天数" align="center" prop="totalDays" width="80" />
      <el-table-column label="关联标签" align="center" prop="tagName" width="100">
        <template slot-scope="scope">{{ scope.row.tagName || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 'ENABLED' ? 'success' : 'info'" size="mini">
            {{ scope.row.status === 'ENABLED' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" fixed="right">
        <template slot-scope="scope">
          <el-button size="mini" type="text" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="mini" type="text" style="color:#f56c6c;" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogOpen" width="550px">
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="form.ruleName" :maxlength="100" show-word-limit placeholder="如：月付房租/季度水电" />
        </el-form-item>
        <el-form-item label="周期数值" prop="periodValue">
          <el-input-number v-model="form.periodValue" :min="0.01" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="时间单位" prop="periodUnit">
          <el-select v-model="form.periodUnit" placeholder="请选择">
            <el-option label="天" value="DAY" />
            <el-option label="周" value="WEEK" />
            <el-option label="月" value="MONTH" />
            <el-option label="季" value="QUARTER" />
            <el-option label="年" value="YEAR" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联标签">
          <el-select v-model="form.tagId" placeholder="可选" clearable>
            <el-option v-for="t in tagOptions" :key="t.id" :label="t.tagName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio label="ENABLED">启用</el-radio>
            <el-radio label="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { listPeriodRules, addPeriodRule, updatePeriodRule, removePeriodRule } from '@/api/micro/tagPeriodRule'
import { listAllTags } from '@/api/micro/tagPeriodRule'

export default {
  name: 'PeriodRule',
  data() {
    return {
      loading: true, total: 0, ruleList: [], tagOptions: [],
      queryParams: { pageNum: 1, pageSize: 10, ruleName: null, periodUnit: null },
      unitMap: { DAY: '天', WEEK: '周', MONTH: '月', QUARTER: '季', YEAR: '年' },
      dialogOpen: false, dialogTitle: '', form: {},
      rules: {
        ruleName: [{ required: true, message: '规则名称不能为空', trigger: 'blur' }],
        periodValue: [{ required: true, message: '周期数值不能为空', trigger: 'blur' }],
        periodUnit: [{ required: true, message: '请选择时间单位', trigger: 'change' }]
      }
    }
  },
  created() { this.getList(); this.loadTags() },
  methods: {
    getList() { this.loading = true; listPeriodRules(this.queryParams).then(res => { this.ruleList = res.rows; this.total = res.total; this.loading = false }).catch(() => { this.loading = false }) },
    loadTags() { listAllTags({ status: 'ENABLED' }).then(res => { this.tagOptions = res.data || [] }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm('queryForm'); this.handleQuery() },
    handleAdd() { this.form = { periodValue: 1, periodUnit: 'MONTH', status: 'ENABLED' }; this.dialogTitle = '新增周期规则'; this.dialogOpen = true },
    handleEdit(row) { this.form = { ...row }; this.dialogTitle = '编辑周期规则'; this.dialogOpen = true },
    submitForm() {
      this.$refs['form'].validate(valid => { if (!valid) return
        if (this.form.id) { updatePeriodRule(this.form).then(() => { this.$modal.msgSuccess('修改成功'); this.dialogOpen = false; this.getList() }) }
        else { addPeriodRule(this.form).then(() => { this.$modal.msgSuccess('新增成功'); this.dialogOpen = false; this.getList() }) }
      })
    },
    handleDelete(row) { this.$modal.confirm('确认删除规则「' + row.ruleName + '」？').then(() => removePeriodRule(row.id)).then(() => { this.$modal.msgSuccess('删除成功'); this.getList() }) }
  }
}
</script>

<style scoped>.mb16 { margin-bottom: 16px; }</style>
