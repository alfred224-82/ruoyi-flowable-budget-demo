<template>
  <div class="app-container">
    <el-alert title="自定义凭证模板（高级版）：内置模板只读，可新增/修改/删除自定义模板（列头/分隔符/借贷方向），导出凭证时选择模板。" type="info" :closable="false" style="margin-bottom: 15px;" />

    <el-form :inline="true" size="small">
      <el-form-item label="模板名称">
        <el-input v-model="templateName" placeholder="请输入模板名称" clearable style="width: 200px;" @keyup.enter.native="loadList" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="loadList">查询</el-button>
        <el-button type="primary" plain icon="el-icon-plus" @click="handleAdd" v-hasPermi="['fee:exportTemplate:edit']">新增模板</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="templateList" border size="small" v-loading="loading">
      <el-table-column label="模板编码" width="160" align="center" prop="templateCode" />
      <el-table-column label="模板名称" min-width="160" prop="templateName" />
      <el-table-column label="适用财务系统" width="140" align="center" prop="targetSystem" />
      <el-table-column label="分隔符" width="90" align="center" prop="delimiter" />
      <el-table-column label="排序" width="80" align="center" prop="sortOrder" />
      <el-table-column label="来源" width="90" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.isBuiltin === 1 ? 'info' : 'success'" size="small">{{ scope.row.isBuiltin === 1 ? '内置' : '自定义' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" align="center">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleView(scope.row)" v-hasPermi="['fee:exportTemplate:query']">查看</el-button>
          <el-button size="mini" type="text" icon="el-icon-document-copy" @click="handleCopy(scope.row)" v-hasPermi="['fee:exportTemplate:edit']">复制</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" :disabled="scope.row.isBuiltin === 1" @click="handleEdit(scope.row)" v-hasPermi="['fee:exportTemplate:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" style="color:#f56c6c;" :disabled="scope.row.isBuiltin === 1" @click="handleDel(scope.row)" v-hasPermi="['fee:exportTemplate:edit']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="680px" append-to-body>
      <el-form :model="form" :rules="rules" ref="templateForm" label-width="110px" :disabled="readonly">
        <el-form-item label="模板编码" prop="templateCode">
          <el-input v-model="form.templateCode" placeholder="唯一编码，如 CUSTOM_KINGDEE" />
        </el-form-item>
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="适用财务系统">
          <el-input v-model="form.targetSystem" placeholder="如 金蝶/用友/通用" />
        </el-form-item>
        <el-form-item label="分隔符">
          <el-input v-model="form.delimiter" placeholder="如 , 或 ;" style="width: 120px;" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="列头定义">
          <el-input v-model="form.columnConfig" type="textarea" :rows="8" placeholder='列头/借贷方向 JSON，如 [{"field":"summary","label":"摘要","direction":"DEBIT"}]' />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">{{ readonly ? '关闭' : '取消' }}</el-button>
        <el-button v-if="!readonly" type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listTemplateConfig, addTemplateConfig, updateTemplateConfig, delTemplateConfig } from '@/api/system/fee/fundExportTemplate'

export default {
  name: 'FeeExportTemplate',
  data() {
    return {
      loading: false,
      templateName: null,
      templateList: [],
      dialogVisible: false,
      dialogTitle: '',
      readonly: false,
      submitLoading: false,
      form: {},
      rules: {
        templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
        templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.loadList()
  },
  methods: {
    loadList() {
      this.loading = true
      listTemplateConfig({ templateName: this.templateName }).then(res => {
        this.templateList = res.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    emptyForm() {
      return { id: null, templateCode: '', templateName: '', targetSystem: '', delimiter: ',', sortOrder: 100, columnConfig: '' }
    },
    handleAdd() {
      this.form = this.emptyForm()
      this.dialogTitle = '新增自定义模板'
      this.readonly = false
      this.dialogVisible = true
    },
    handleView(row) {
      this.form = { ...row }
      this.dialogTitle = '模板详情 - ' + row.templateName
      this.readonly = true
      this.dialogVisible = true
    },
    handleCopy(row) {
      this.form = { ...row, id: null, templateCode: row.templateCode + '_COPY', templateName: row.templateName + '（自定义）', isBuiltin: 0 }
      this.dialogTitle = '复制为自定义模板'
      this.readonly = false
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.form = { ...row }
      this.dialogTitle = '修改自定义模板'
      this.readonly = false
      this.dialogVisible = true
    },
    submitForm() {
      this.$refs.templateForm.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        const action = this.form.id ? updateTemplateConfig : addTemplateConfig
        action(this.form).then(() => {
          this.$modal.msgSuccess('保存成功')
          this.dialogVisible = false
          this.loadList()
        }).finally(() => { this.submitLoading = false })
      })
    },
    handleDel(row) {
      this.$modal.confirm('确认删除自定义模板「' + row.templateName + '」？').then(() => {
        return delTemplateConfig(row.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.loadList()
      }).catch(() => {})
    }
  }
}
</script>
