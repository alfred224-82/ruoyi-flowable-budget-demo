<template>
  <div class="app-container">
    <el-alert
      title="标准角色集全版本共用，本租户只能「挑选 → 改名 → 启停 → 排序」"
      type="info"
      description="角色编码（roleCode）与角色权限（菜单 / 页面区块）由平台统一定义，租户不可新建角色、不可改权限；改名后立即生效，无需重启。角色可见范围已按本租户产品线（tenant_type_scope）在数据源层硬隔离。"
      show-icon
      :closable="false"
      class="mb8"
    />

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="角色显示名" prop="displayName">
        <el-input v-model="queryParams.displayName" placeholder="请输入角色显示名" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="角色编码" prop="roleCode">
        <el-input v-model="queryParams.roleCode" placeholder="如 boss / salesman" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="启用" value="0" />
          <el-option label="禁用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleSelect" v-hasPermi="['micro:tenantRole:rename']">挑选标准角色</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="roleList">
      <el-table-column label="排序" align="center" prop="sortOrder" width="70" />
      <el-table-column label="角色显示名" align="left" prop="displayName" min-width="150" show-overflow-tooltip>
        <template slot-scope="scope">
          <span style="font-weight:600;">{{ scope.row.displayName }}</span>
          <span v-if="scope.row.roleName && scope.row.displayName !== scope.row.roleName" style="color:#909399;font-size:12px;">（原「{{ scope.row.roleName }}」）</span>
        </template>
      </el-table-column>
      <el-table-column label="标准角色编码" align="center" prop="roleCode" width="130" />
      <el-table-column label="标准角色名" align="center" prop="roleName" width="120" />
      <el-table-column label="适用产品线" align="center" prop="tenantTypeScope" width="110">
        <template slot-scope="scope">
          <el-tag :type="scopeTagType(scope.row.tenantTypeScope)" size="mini">{{ scopeText(scope.row.tenantTypeScope) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.status" active-value="0" inactive-value="1" @change="handleStatusChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="140">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['micro:tenantRole:rename']">改名 / 排序</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 改名 / 排序 -->
    <el-dialog title="角色改名 / 排序" :visible.sync="open" width="520px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="标准角色">
          <span>{{ form.roleName }}（{{ form.roleCode }}）</span>
        </el-form-item>
        <el-form-item label="角色显示名" prop="displayName">
          <el-input v-model="form.displayName" placeholder="如 salesman 改名为「助教」" maxlength="30" show-word-limit />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" />
          <span style="color:#909399;font-size:12px;margin-left:8px;">数值越小越靠前</span>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 挑选标准角色 -->
    <el-dialog title="挑选标准角色" :visible.sync="selectOpen" width="720px" append-to-body>
      <el-alert
        title="仅可选择与本租户产品线匹配且尚未挑选的标准角色"
        type="warning"
        :closable="false"
        show-icon
        class="mb8"
      />
      <el-table v-loading="selectLoading" :data="unselectedRoles" @selection-change="handleSelectionChange" max-height="380">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column label="标准角色名" align="left" prop="roleName" min-width="120" />
        <el-table-column label="角色编码" align="center" prop="roleCode" width="130" />
        <el-table-column label="适用产品线" align="center" prop="tenantTypeScope" width="110">
          <template slot-scope="scope">
            <el-tag :type="scopeTagType(scope.row.tenantTypeScope)" size="mini">{{ scopeText(scope.row.tenantTypeScope) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="挑选后显示名" align="center" prop="displayName" width="130">
          <template slot-scope="scope">
            <span style="color:#909399;">{{ scope.row.displayName }}</span>
          </template>
        </el-table-column>
      </el-table>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :disabled="selectedCodes.length === 0" @click="submitSelect">确认挑选（{{ selectedCodes.length }}）</el-button>
        <el-button @click="selectOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listTenantRole, listOptionalRoles, selectRoles, renameRole, toggleRole, sortRole } from '@/api/micro/tenantRole'

export default {
  name: 'MicroTenantRole',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      roleList: [],
      // 改名 / 排序
      open: false,
      form: {},
      // 挑选标准角色
      selectOpen: false,
      selectLoading: false,
      unselectedRoles: [],
      selectedCodes: [],
      scopeMap: { micro: '小微版', enterprise: '企业版', both: '全版本' },
      queryParams: { pageNum: 1, pageSize: 10, displayName: null, roleCode: null, status: null },
      rules: {
        displayName: [{ required: true, message: '角色显示名不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listTenantRole(this.queryParams).then(response => {
        this.roleList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    scopeText(val) {
      return this.scopeMap[val] || '全版本'
    },
    scopeTagType(val) {
      if (val === 'micro') return 'success'
      if (val === 'enterprise') return 'warning'
      return 'info'
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    reset() {
      this.form = { id: null, roleCode: null, roleName: null, displayName: null, sortOrder: 0 }
      this.resetForm('form')
    },
    cancel() {
      this.open = false
      this.reset()
    },
    handleUpdate(row) {
      this.reset()
      this.form = {
        id: row.id,
        roleCode: row.roleCode,
        roleName: row.roleName,
        displayName: row.displayName,
        sortOrder: row.sortOrder == null ? 0 : row.sortOrder
      }
      this.open = true
    },
    /**
     * 提交改名 + 排序。
     * ★后端拆成两个接口：PUT /rename 仅落 display_name，sort_order 必须走 PUT /sort；
     *   且 /rename 带 @Validated(AddGroup) 校验 roleCode 非空，故 payload 必须回传 roleCode。
     */
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) return
        const payload = { id: this.form.id, roleCode: this.form.roleCode, displayName: this.form.displayName }
        renameRole(payload)
          .then(() => sortRole({ id: this.form.id, sortOrder: this.form.sortOrder }))
          .then(() => {
            this.$modal.msgSuccess('保存成功')
            this.open = false
            this.getList()
          })
      })
    },
    handleStatusChange(row) {
      const text = row.status === '0' ? '启用' : '禁用'
      this.$modal.confirm('确认要' + text + '角色「' + row.displayName + '」吗？')
        .then(() => toggleRole(row.id, row.status))
        .then(() => { this.$modal.msgSuccess(text + '成功') })
        .catch(() => { row.status = row.status === '0' ? '1' : '0' })
    },
    handleSelect() {
      this.selectedCodes = []
      this.selectOpen = true
      this.selectLoading = true
      listOptionalRoles().then(response => {
        // optional-list 已在数据源层按 tenant_type_scope 硬隔离；id 为 null 者即本租户尚未挑选
        this.unselectedRoles = (response.data || []).filter(item => item.id == null)
        this.selectLoading = false
      }).catch(() => { this.selectLoading = false })
    },
    handleSelectionChange(selection) {
      this.selectedCodes = selection.map(item => item.roleCode)
    },
    submitSelect() {
      selectRoles(this.selectedCodes).then(() => {
        this.$modal.msgSuccess('挑选成功')
        this.selectOpen = false
        this.getList()
      })
    }
  }
}
</script>
