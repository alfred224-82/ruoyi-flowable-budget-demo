<template>
  <div class="app-container">
    <el-alert title="固定支出标签管理（租户管理员）" type="info"
      description="管理固定支出标签（房租/水电/工资/物料/营销等），支持 CRUD + 排序。" show-icon :closable="false" class="mb16" />

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="80px">
      <el-form-item label="标签名称" prop="tagName">
        <el-input v-model="queryParams.tagName" placeholder="搜索标签" clearable :maxlength="50" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable>
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        <el-button type="success" icon="el-icon-plus" size="mini" @click="handleAdd">新增</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tagList">
      <el-table-column label="ID" align="center" prop="id" width="60" />
      <el-table-column label="排序" align="center" prop="sortOrder" width="60" />
      <el-table-column label="标签名称" align="left" prop="tagName" min-width="120" />
      <el-table-column label="编码" align="center" prop="tagCode" width="100" />
      <el-table-column label="颜色" align="center" width="80">
        <template slot-scope="scope">
          <span v-if="scope.row.color" :style="{ color: scope.row.color, fontWeight: 'bold' }">●</span>
          <span v-else>-</span>
        </template>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogOpen" width="500px">
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标签名称" prop="tagName">
          <el-input v-model="form.tagName" :maxlength="50" show-word-limit placeholder="如：房租/水电/工资" />
        </el-form-item>
        <el-form-item label="标签编码">
          <el-input v-model="form.tagCode" :maxlength="50" placeholder="可选，程序引用用" />
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="form.color" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
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
import { listTags, addTag, updateTag, removeTag } from '@/api/micro/tagPeriodRule'

export default {
  name: 'TagConfig',
  data() {
    return {
      loading: true, total: 0, tagList: [],
      queryParams: { pageNum: 1, pageSize: 10, tagName: null, status: null },
      dialogOpen: false, dialogTitle: '', form: {},
      rules: { tagName: [{ required: true, message: '标签名称不能为空', trigger: 'blur' }] }
    }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listTags(this.queryParams).then(res => { this.tagList = res.rows; this.total = res.total; this.loading = false }).catch(() => { this.loading = false }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm('queryForm'); this.handleQuery() },
    handleAdd() { this.form = { sortOrder: 0, status: 'ENABLED', color: '#409EFF' }; this.dialogTitle = '新增标签'; this.dialogOpen = true },
    handleEdit(row) { this.form = { ...row }; this.dialogTitle = '编辑标签'; this.dialogOpen = true },
    submitForm() {
      this.$refs['form'].validate(valid => { if (!valid) return
        if (this.form.id) { updateTag(this.form).then(() => { this.$modal.msgSuccess('修改成功'); this.dialogOpen = false; this.getList() }) }
        else { addTag(this.form).then(() => { this.$modal.msgSuccess('新增成功'); this.dialogOpen = false; this.getList() }) }
      })
    },
    handleDelete(row) { this.$modal.confirm('确认删除标签「' + row.tagName + '」？').then(() => removeTag(row.id)).then(() => { this.$modal.msgSuccess('删除成功'); this.getList() }) }
  }
}
</script>

<style scoped>.mb16 { margin-bottom: 16px; }</style>
