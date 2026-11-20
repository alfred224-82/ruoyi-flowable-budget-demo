<template>
  <div class="app-container">
    <el-alert title="License 版本模板管理（Admin 专属）" type="info"
      description="创建/编辑版本模板（BASIC/ADVANCED/PROFESSIONAL × TRIAL/FORMAL），定义功能开关、用户上限、角色上限。"
      show-icon :closable="false" class="mb16" />

    <!-- ===== 筛选 ===== -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="90px">
      <el-form-item label="版本等级" prop="edition">
        <el-select v-model="queryParams.edition" placeholder="全部" clearable>
          <el-option label="基础版" value="BASIC" />
          <el-option label="高级版" value="ADVANCED" />
          <el-option label="专业版" value="PROFESSIONAL" />
        </el-select>
      </el-form-item>
      <el-form-item label="授权类型" prop="licenseType">
        <el-select v-model="queryParams.licenseType" placeholder="全部" clearable>
          <el-option label="试用" value="TRIAL" />
          <el-option label="正式" value="FORMAL" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable>
          <el-option label="启用" value="0" />
          <el-option label="停用" value="1" />
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
        <el-button type="primary" icon="el-icon-plus" size="mini" @click="handleAdd">新增模板</el-button>
      </el-col>
    </el-row>

    <!-- ===== 列表 ===== -->
    <el-table v-loading="loading" :data="templateList" border>
      <el-table-column label="ID" align="center" prop="id" width="60" />
      <el-table-column label="模板名称" align="center" prop="templateName" min-width="140" />
      <el-table-column label="版本等级" align="center" prop="edition" width="110">
        <template slot-scope="scope">
          <el-tag :type="editionTagType(scope.row.edition)" size="small">{{ editionLabel(scope.row.edition) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="授权类型" align="center" prop="licenseType" width="90">
        <template slot-scope="scope">
          {{ scope.row.licenseType === 'TRIAL' ? '试用' : '正式' }}
        </template>
      </el-table-column>
      <el-table-column label="试用天数" align="center" prop="trialDays" width="80">
        <template slot-scope="scope">{{ scope.row.trialDays || '-' }}</template>
      </el-table-column>
      <el-table-column label="最大用户" align="center" prop="maxUsers" width="80" />
      <el-table-column label="最大角色" align="center" prop="maxRoles" width="80">
        <template slot-scope="scope">{{ scope.row.maxRoles || '不限' }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.status" active-value="0" inactive-value="1"
            @change="handleStatusChange(scope.row)" />
        </template>
      </el-table-column>
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
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" :maxlength="100" placeholder="如：基础版-正式" />
        </el-form-item>
        <el-form-item label="版本等级" prop="edition">
          <el-select v-model="form.edition" placeholder="请选择" style="width:100%;">
            <el-option label="基础版" value="BASIC" />
            <el-option label="高级版" value="ADVANCED" />
            <el-option label="专业版" value="PROFESSIONAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="授权类型" prop="licenseType">
          <el-radio-group v-model="form.licenseType">
            <el-radio label="TRIAL">试用</el-radio>
            <el-radio label="FORMAL">正式</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="试用天数" prop="trialDays" v-if="form.licenseType === 'TRIAL'">
          <el-input-number v-model="form.trialDays" :min="1" :max="365" style="width:200px;" />
          <span style="margin-left:8px;color:#909399;">天</span>
        </el-form-item>
        <el-form-item label="最大用户数" prop="maxUsers">
          <el-input-number v-model="form.maxUsers" :min="1" :max="99999" style="width:200px;" />
        </el-form-item>
        <el-form-item label="最大角色数">
          <el-input-number v-model="form.maxRoles" :min="1" :max="999" style="width:200px;" />
          <span style="margin-left:8px;color:#909399;">（留空=不限）</span>
        </el-form-item>
        <el-form-item label="功能开关">
          <el-input v-model="form.features" type="textarea" :rows="3" :maxlength="2000"
            show-word-limit placeholder='JSON数组，如["FEAT_BOOKING","FEAT_CASH_FLOW"]' />
        </el-form-item>
        <el-form-item label="适用行业">
          <el-input v-model="form.industries" type="textarea" :rows="2" :maxlength="500"
            show-word-limit placeholder='JSON数组，如["桌游","健身","餐饮"]' />
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
import { listLicenseTemplate, getLicenseTemplate, addLicenseTemplate, updateLicenseTemplate, deleteLicenseTemplate, changeLicenseTemplateStatus } from '@/api/system/licenseTemplate'

export default {
  name: 'LicenseTemplate',
  data() {
    return {
      loading: true,
      total: 0,
      templateList: [],
      queryParams: { pageNum: 1, pageSize: 10, edition: null, licenseType: null, status: null },
      dialogOpen: false,
      dialogTitle: '',
      form: {},
      rules: {
        templateName: [{ required: true, message: '模板名称不能为空', trigger: 'blur' }],
        edition: [{ required: true, message: '版本等级不能为空', trigger: 'change' }],
        licenseType: [{ required: true, message: '授权类型不能为空', trigger: 'change' }],
        maxUsers: [{ required: true, message: '最大用户数不能为空', trigger: 'blur' }]
      }
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      listLicenseTemplate(this.queryParams).then(res => {
        this.templateList = res.rows
        this.total = res.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm('queryForm'); this.handleQuery() },
    handleAdd() {
      this.form = { edition: 'BASIC', licenseType: 'FORMAL', maxUsers: 10, status: '0' }
      this.dialogTitle = '新增 License 模板'
      this.dialogOpen = true
    },
    handleEdit(row) {
      getLicenseTemplate(row.id).then(res => {
        this.form = res.data
        this.dialogTitle = '编辑 License 模板'
        this.dialogOpen = true
      })
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) return
        if (this.form.id) {
          updateLicenseTemplate(this.form).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.dialogOpen = false
            this.getList()
          })
        } else {
          addLicenseTemplate(this.form).then(() => {
            this.$modal.msgSuccess('新增成功')
            this.dialogOpen = false
            this.getList()
          })
        }
      })
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除模板"' + row.templateName + '"？').then(() => {
        return deleteLicenseTemplate(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleStatusChange(row) {
      const text = row.status === '0' ? '启用' : '停用'
      this.$modal.confirm('确认' + text + '模板"' + row.templateName + '"？').then(() => {
        return changeLicenseTemplateStatus(row.id, row.status)
      }).then(() => {
        this.$modal.msgSuccess(text + '成功')
      }).catch(() => {
        row.status = row.status === '0' ? '1' : '0'
      })
    },
    editionLabel(edition) {
      return { BASIC: '基础版', ADVANCED: '高级版', PROFESSIONAL: '专业版' }[edition] || edition
    },
    editionTagType(edition) {
      return { BASIC: 'info', ADVANCED: 'warning', PROFESSIONAL: 'danger' }[edition] || ''
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
</style>
