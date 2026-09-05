<template>
  <div class="app-container">
    <el-alert title="凭证导出：选择月份与导出模板，生成借贷两行式记账凭证 Excel，可交给财务软件/代账会计。" type="info" :closable="false" style="margin-bottom: 15px;" />

    <el-form ref="form" :model="form" :rules="rules" label-width="100px" style="max-width: 560px;">
      <el-form-item label="导出月份" prop="month">
        <el-date-picker v-model="form.month" type="month" value-format="yyyy-MM" placeholder="选择月份" style="width: 100%;" />
      </el-form-item>
      <el-form-item label="导出模板" prop="templateId">
        <el-select v-model="form.templateId" placeholder="请选择模板" style="width: 100%;">
          <el-option v-for="t in templates" :key="t.id" :label="t.templateName + (t.targetSystem ? '（' + t.targetSystem + '）' : '')" :value="t.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="导出范围" prop="projectId">
        <el-select v-model="form.projectId" placeholder="全部（含公司级）" clearable filterable style="width: 100%;">
          <el-option label="全部项目（含公司级）" :value="null" />
          <el-option label="仅公司级（未归类）" :value="0" />
          <el-option v-for="p in activeProjects" :key="p.id" :label="p.projectName" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-download" :loading="exporting" @click="handleExport" v-hasPermi="['fee:export:run']">导出凭证</el-button>
        <el-button v-if="can2d" type="success" icon="el-icon-s-grid" :loading="exporting2d" @click="handleExport2d" v-hasPermi="['fee:export:run2d']">二维导出（项目×科目）</el-button>
      </el-form-item>
    </el-form>

    <el-divider content-position="left">模板说明</el-divider>
    <el-table :data="templates" size="small" border>
      <el-table-column label="模板编码" align="center" prop="templateCode" width="160" />
      <el-table-column label="模板名称" align="left" prop="templateName" min-width="180" />
      <el-table-column label="目标系统" align="center" prop="targetSystem" width="160" />
      <el-table-column label="类型" align="center" prop="isBuiltin" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.isBuiltin === 1 ? 'success' : 'warning'" size="small">{{ scope.row.isBuiltin === 1 ? '内置' : '自定义' }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import { listExportTemplate, runExport } from '@/api/system/fee/fundExport'
import { listActiveProject } from '@/api/system/fee/fundProject'
import { runExport2d } from '@/api/system/fee/fundPro'
import { loadFeatures, featureVisible } from '@/utils/feature'

export default {
  name: 'FundExport',
  data() {
    return {
      templates: [],
      activeProjects: [],
      exporting: false,
      exporting2d: false,
      can2d: false,
      form: {
        month: null,
        templateId: null,
        projectId: null
      },
      rules: {
        month: [{ required: true, message: '请选择导出月份', trigger: 'change' }],
        templateId: [{ required: true, message: '请选择导出模板', trigger: 'change' }]
      }
    }
  },
  created() {
    listExportTemplate().then(res => { this.templates = res.data || [] })
    listActiveProject().then(res => { this.activeProjects = res.data || [] })
    // 专业版开关：二维导出入口按 License 下发
    loadFeatures().then(() => { this.can2d = featureVisible('FEAT_EXPORT_2D') })
  },
  methods: {
    handleExport() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          this.exporting = true
          runExport(this.form.month, this.form.templateId, this.form.projectId).then(res => {
            const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
            const url = window.URL.createObjectURL(blob)
            const link = document.createElement('a')
            link.href = url
            link.download = '凭证导出_' + this.form.month + '.xlsx'
            link.click()
            window.URL.revokeObjectURL(url)
            this.$modal.msgSuccess('导出成功')
          }).finally(() => { this.exporting = false })
        }
      })
    },
    handleExport2d() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          this.exporting2d = true
          runExport2d(this.form.month, this.form.templateId, this.form.projectId).then(res => {
            const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
            const url = window.URL.createObjectURL(blob)
            const link = document.createElement('a')
            link.href = url
            link.download = '凭证二维导出_' + this.form.month + '.xlsx'
            link.click()
            window.URL.revokeObjectURL(url)
            this.$modal.msgSuccess('二维导出成功')
          }).finally(() => { this.exporting2d = false })
        }
      })
    }
  }
}
</script>
