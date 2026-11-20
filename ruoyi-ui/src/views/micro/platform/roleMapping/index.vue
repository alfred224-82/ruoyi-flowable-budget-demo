<template>
  <div class="app-container">
    <el-alert
      title="角色映射管理"
      type="info"
      description="查看和编辑本租户角色映射（显示名/启停），改名后立即生效，无需重启。本租户缺失行业角色映射时，进入页面将按租户所属行业自动从 admin（平台）配置的行业角色映射补录缺失角色（幂等，仅补缺失项，不覆盖已有映射）。"
      show-icon :closable="false" class="mb8" />

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="90px">
      <el-form-item label="角色显示名" prop="displayName">
        <el-input v-model="queryParams.displayName" placeholder="请输入" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable>
          <el-option label="启用" value="0" />
          <el-option label="禁用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="roleList">
      <el-table-column label="角色显示名" align="left" prop="displayName" min-width="150">
        <template slot-scope="scope">
          <span style="font-weight:600;">{{ scope.row.displayName }}</span>
          <span v-if="scope.row.roleName && scope.row.displayName !== scope.row.roleName" style="color:#909399;font-size:12px;">（{{ scope.row.roleName }}）</span>
        </template>
      </el-table-column>
      <el-table-column label="角色编码" align="center" prop="roleCode" width="130" />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.status" active-value="0" inactive-value="1" @change="handleStatusChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="120">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleRename(scope.row)">改名</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 改名弹窗 -->
    <el-dialog title="角色改名" :visible.sync="open" width="480px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="角色编码">
          <span>{{ form.roleCode }}</span>
        </el-form-item>
        <el-form-item label="显示名称" prop="displayName">
          <el-input v-model="form.displayName" placeholder="请输入新的显示名" maxlength="30" show-word-limit />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="open = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listPlatformRoleMapping, renamePlatformRole, togglePlatformRole } from '@/api/micro/platformRoleMapping'

export default {
  name: 'PlatformRoleMapping',
  data() {
    return {
      loading: true,
      total: 0,
      roleList: [],
      open: false,
      form: {},
      queryParams: { pageNum: 1, pageSize: 10, displayName: null, status: null },
      rules: {
        displayName: [{ required: true, message: '显示名称不能为空', trigger: 'blur' }]
      }
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      listPlatformRoleMapping(this.queryParams).then(res => {
        this.roleList = res.rows
        this.total = res.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm('queryForm'); this.handleQuery() },
    handleRename(row) {
      this.form = { id: row.id, roleCode: row.roleCode, displayName: row.displayName }
      this.open = true
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) return
        renamePlatformRole(this.form).then(() => {
          this.$modal.msgSuccess('改名成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleStatusChange(row) {
      const text = row.status === '0' ? '启用' : '禁用'
      this.$modal.confirm('确认' + text + '角色「' + row.displayName + '」吗？')
        .then(() => togglePlatformRole(row.id, row.status))
        .then(() => { this.$modal.msgSuccess(text + '成功') })
        .catch(() => { row.status = row.status === '0' ? '1' : '0' })
    }
  }
}
</script>
