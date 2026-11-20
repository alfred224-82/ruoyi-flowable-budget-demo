<template>
  <div class="app-container">
    <el-alert
      title="一个手机号可在多个租户拥有多个角色，同一租户内主角色唯一"
      type="info"
      description="分配前角色必须已在「业务角色管理」中挑选并启用；用户的首个角色会自动置为主角色，主角色决定其登录本租户后的默认视图。"
      show-icon
      :closable="false"
      class="mb8"
    />

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="用户" prop="userId">
        <el-select
          v-model="queryParams.userId"
          placeholder="输入账号或手机号搜索"
          filterable
          remote
          clearable
          reserve-keyword
          :remote-method="remoteUser"
          :loading="userLoading"
          style="width:220px;"
        >
          <el-option v-for="u in userOptions" :key="u.userId" :label="userLabel(u)" :value="u.userId" />
        </el-select>
      </el-form-item>
      <el-form-item label="角色" prop="roleCode">
        <el-select v-model="queryParams.roleCode" placeholder="请选择角色" clearable style="width:180px;">
          <el-option v-for="r in roleOptions" :key="r.roleCode" :label="r.displayName" :value="r.roleCode" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width:120px;">
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['micro:userRole:assign']">分配角色</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="assignList">
      <el-table-column label="用户账号" align="left" prop="userName" min-width="120" show-overflow-tooltip />
      <el-table-column label="昵称" align="left" prop="nickName" min-width="110" show-overflow-tooltip />
      <el-table-column label="手机号" align="center" prop="phonenumber" width="130" />
      <el-table-column label="角色" align="left" prop="roleName" min-width="130">
        <template slot-scope="scope">
          <span style="font-weight:600;">{{ scope.row.roleName }}</span>
          <span style="color:#909399;font-size:12px;margin-left:6px;">{{ scope.row.roleCode }}</span>
        </template>
      </el-table-column>
      <el-table-column label="主角色" align="center" prop="isPrimary" width="90">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.isPrimary === 1" type="success" size="mini">主角色</el-tag>
          <span v-else style="color:#c0c4cc;">—</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="mini">{{ scope.row.status === '0' ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="分配时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="190">
        <template slot-scope="scope">
          <el-button
            v-if="scope.row.isPrimary !== 1"
            size="mini"
            type="text"
            icon="el-icon-star-off"
            @click="handlePrimary(scope.row)"
            v-hasPermi="['micro:userRole:assign']"
          >设为主角色</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['micro:userRole:remove']">取消分配</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 分配角色 -->
    <el-dialog title="分配用户角色" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户" prop="userId">
          <el-select
            v-model="form.userId"
            placeholder="输入账号或手机号搜索"
            filterable
            remote
            reserve-keyword
            :remote-method="remoteUser"
            :loading="userLoading"
            style="width:100%;"
          >
            <el-option v-for="u in userOptions" :key="u.userId" :label="userLabel(u)" :value="u.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色" prop="roleCodes">
          <el-select v-model="form.roleCodes" multiple placeholder="可一次分配多个角色" style="width:100%;">
            <el-option v-for="r in roleOptions" :key="r.roleCode" :label="r.displayName" :value="r.roleCode" />
          </el-select>
          <div style="color:#909399;font-size:12px;line-height:1.6;">
            仅列出本租户已挑选且启用的角色；若目标角色不在此列，请先到「业务角色管理」挑选。
          </div>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" show-word-limit placeholder="备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listUserRole, assignUserRole, setPrimaryRole, delUserRole } from '@/api/micro/userRole'
import { listTenantRole } from '@/api/micro/tenantRole'
import { listUser } from '@/api/system/user'

export default {
  name: 'MicroUserRole',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      assignList: [],
      open: false,
      // 用户远程搜索下拉（查询区与表单区共用）
      userOptions: [],
      userLoading: false,
      // 本租户已挑选且启用的角色下拉
      roleOptions: [],
      queryParams: { pageNum: 1, pageSize: 10, userId: null, roleCode: null, status: null },
      form: {},
      rules: {
        userId: [{ required: true, message: '请选择用户', trigger: 'change' }],
        roleCodes: [{ required: true, type: 'array', min: 1, message: '请至少选择一个角色', trigger: 'change' }]
      }
    }
  },
  created() {
    this.getList()
    this.getRoleOptions()
    this.remoteUser('')
  },
  methods: {
    getList() {
      this.loading = true
      listUserRole(this.queryParams).then(response => {
        this.assignList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    // 角色下拉只取启用状态，禁用的角色后端会拒绝分配
    getRoleOptions() {
      listTenantRole({ pageNum: 1, pageSize: 100, status: '0' }).then(response => {
        this.roleOptions = response.rows || []
      }).catch(() => {})
    },
    userLabel(u) {
      const phone = u.phonenumber ? ' / ' + u.phonenumber : ''
      return (u.nickName || u.userName) + '（' + u.userName + phone + '）'
    },
    /**
     * 用户远程搜索：纯数字按手机号查，否则按账号查（★手机号为小微版身份锚点）。
     * 关键字为空时预载前 20 条，避免下拉初始为空。
     */
    remoteUser(keyword) {
      this.userLoading = true
      const query = { pageNum: 1, pageSize: 20, status: '0' }
      const kw = (keyword || '').trim()
      if (kw) {
        if (/^\d+$/.test(kw)) {
          query.phonenumber = kw
        } else {
          query.userName = kw
        }
      }
      listUser(query).then(response => {
        this.userOptions = response.rows || []
        this.userLoading = false
      }).catch(() => { this.userLoading = false })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.queryParams.userId = null
      this.handleQuery()
    },
    reset() {
      this.form = { userId: null, roleCodes: [], remark: null }
      this.resetForm('form')
    },
    cancel() {
      this.open = false
      this.reset()
    },
    handleAdd() {
      this.reset()
      this.remoteUser('')
      this.open = true
    },
    /**
     * 提交分配。
     * ★不传 isPrimary：批量分配时后端对每个角色套用同一个 isPrimary 会造成主角色归属不确定，
     *   交由后端「首个角色自动置主」，需要调整时用行内「设为主角色」。
     */
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) return
        assignUserRole({ userId: this.form.userId, roleCodes: this.form.roleCodes, remark: this.form.remark }).then(() => {
          this.$modal.msgSuccess('分配成功')
          this.open = false
          this.getList()
        })
      })
    },
    handlePrimary(row) {
      this.$modal.confirm('确认将「' + (row.nickName || row.userName) + '」的主角色设为「' + row.roleName + '」吗？原主角色将自动取消。')
        .then(() => setPrimaryRole(row.id))
        .then(() => {
          this.$modal.msgSuccess('设置成功')
          this.getList()
        })
        .catch(() => {})
    },
    handleDelete(row) {
      this.$modal.confirm('确认取消「' + (row.nickName || row.userName) + '」的「' + row.roleName + '」角色吗？')
        .then(() => delUserRole(row.id))
        .then(() => {
          this.$modal.msgSuccess('取消分配成功')
          this.getList()
        })
        .catch(() => {})
    }
  }
}
</script>
