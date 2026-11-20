<template>
  <div class="app-container">
    <el-alert title="人员管理" type="info" description="新增人员并分配角色，手机号为唯一标识，按租户隔离。设置登录账号和密码后，用户可通过 PC 端登录。" show-icon :closable="false" class="mb8" />

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="80px">
      <el-form-item label="手机号" prop="phonenumber">
        <el-input v-model="queryParams.phonenumber" placeholder="请输入手机号" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="用户昵称" prop="nickName">
        <el-input v-model="queryParams.nickName" placeholder="请输入用户昵称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['micro:platform:userManage']">新增人员</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="userList">
      <el-table-column label="头像" align="center" width="70">
        <template slot-scope="scope">
          <el-avatar v-if="scope.row.avatar" :size="36" :src="scope.row.avatar" />
          <el-avatar v-else :size="36" icon="el-icon-user-solid" />
        </template>
      </el-table-column>
      <el-table-column label="用户昵称" align="left" prop="nickName" min-width="120" show-overflow-tooltip />
      <el-table-column label="登录账号" align="center" prop="userName" min-width="120" show-overflow-tooltip />
      <el-table-column label="手机号" align="center" prop="phonenumber" width="130" />
      <el-table-column label="角色" align="center" prop="roleName" min-width="120" />
      <el-table-column label="角色编码" align="center" prop="roleCode" width="120" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="mini">{{ scope.row.status === '0' ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="140">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(scope.row)" v-hasPermi="['micro:platform:userManage']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleRemove(scope.row)" v-hasPermi="['micro:platform:userManage']">移除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 新增人员弹窗（参照系统用户管理，增加租户ID隔离） -->
    <el-dialog title="新增人员" :visible.sync="addOpen" width="600px" append-to-body>
      <el-form ref="addForm" :model="addForm" :rules="addRules" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="登录账号" prop="userName">
              <el-input v-model="addForm.userName" placeholder="用于 PC 端登录" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="登录密码" prop="password">
              <el-input v-model="addForm.password" placeholder="默认 123456" maxlength="20" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户昵称" prop="nickName">
              <el-input v-model="addForm.nickName" placeholder="请输入用户昵称" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号码" prop="phone">
              <el-input v-model="addForm.phone" placeholder="请输入手机号码" maxlength="11" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="addForm.email" placeholder="请输入邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户性别">
              <el-select v-model="addForm.sex" placeholder="请选择性别">
                <el-option label="男" value="0" />
                <el-option label="女" value="1" />
                <el-option label="未知" value="2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="addForm.status">
                <el-radio label="0">正常</el-radio>
                <el-radio label="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色编码" prop="roleMappingId">
              <el-select v-model="addForm.roleMappingId" placeholder="请选择角色" style="width:100%;">
                <el-option v-for="r in roleOptions" :key="r.id" :label="roleLabel(r)" :value="r.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="头像">
              <el-upload
                class="avatar-uploader"
                :action="uploadUrl"
                :headers="uploadHeaders"
                :show-file-list="false"
                :on-success="handleAvatarSuccess"
                :before-upload="beforeAvatarUpload"
                accept="image/*"
              >
                <img v-if="addForm.avatar" :src="addForm.avatar" class="avatar-image" />
                <i v-else class="el-icon-plus avatar-uploader-icon"></i>
              </el-upload>
              <span v-if="addForm.avatar" class="avatar-remove-btn" @click="addForm.avatar = ''">移除头像</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="描述">
              <el-input v-model="addForm.description" type="textarea" :rows="3" placeholder="请输入描述（可选）" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="submitAdd">确 定</el-button>
        <el-button @click="addOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 修改人员弹窗（密码/头像/昵称/邮箱/性别/角色/状态/描述；登录账号与手机号不可修改） -->
    <el-dialog title="修改人员" :visible.sync="editOpen" width="600px" append-to-body>
      <el-form ref="editForm" :model="editForm" :rules="editRules" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="登录账号">
              <el-input v-model="editForm.userName" placeholder="用于 PC 端登录，租户内唯一" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号码">
              <el-input v-model="editForm.phone" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="登录密码" prop="password">
              <el-input v-model="editForm.password" placeholder="留空则保持原密码" maxlength="20" show-password />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户昵称" prop="nickName">
              <el-input v-model="editForm.nickName" placeholder="请输入用户昵称" maxlength="30" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="editForm.email" placeholder="请输入邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户性别">
              <el-select v-model="editForm.sex" placeholder="请选择性别">
                <el-option label="男" value="0" />
                <el-option label="女" value="1" />
                <el-option label="未知" value="2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="editForm.status">
                <el-radio label="0">正常</el-radio>
                <el-radio label="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色编码" prop="roleMappingId">
              <el-select v-model="editForm.roleMappingId" placeholder="请选择角色" style="width:100%;">
                <el-option v-for="r in roleOptions" :key="r.id" :label="roleLabel(r)" :value="r.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="头像">
              <el-upload
                class="avatar-uploader"
                :action="uploadUrl"
                :headers="uploadHeaders"
                :show-file-list="false"
                :on-success="handleEditAvatarSuccess"
                :before-upload="beforeAvatarUpload"
                accept="image/*"
              >
                <img v-if="editForm.avatar" :src="editForm.avatar" class="avatar-image" />
                <i v-else class="el-icon-plus avatar-uploader-icon"></i>
              </el-upload>
              <span v-if="editForm.avatar" class="avatar-remove-btn" @click="editForm.avatar = ''">移除头像</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="描述">
              <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="请输入描述（可选）" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="submitEdit">确 定</el-button>
        <el-button @click="editOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listPlatformUser, addPlatformUser, editPlatformUser, delPlatformUser } from '@/api/micro/platformUserManage'
import { listPlatformRoleMapping } from '@/api/micro/platformRoleMapping'
import { getToken } from '@/utils/auth'

export default {
  name: 'PlatformUserManage',
  data() {
    return {
      loading: true,
      total: 0,
      userList: [],
      // 本租户角色映射启用行（下拉数据源：映射名+编码，同 role_code 多业务名并存）
      roleOptions: [],
      addOpen: false,
      editOpen: false,
      uploadUrl: process.env.VUE_APP_BASE_API + '/micro/platform/userManage/upload',
      uploadHeaders: { Authorization: 'Bearer ' + getToken() },
      addForm: {
        userName: '',
        password: '123456',
        phone: '',
        nickName: '',
        email: '',
        sex: '0',
        status: '0',
        avatar: '',
        description: '',
        roleMappingId: null
      },
      addRules: {
        userName: [
          { required: true, message: '登录账号不能为空', trigger: 'blur' },
          { min: 2, max: 30, message: '长度在 2 到 30 个字符', trigger: 'blur' }
        ],
        phone: [
          { required: true, message: '手机号不能为空', trigger: 'blur' },
          { pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: '请输入正确的手机号码', trigger: 'blur' }
        ],
        nickName: [{ required: true, message: '用户昵称不能为空', trigger: 'blur' }],
        roleMappingId: [{ required: true, message: '请选择角色', trigger: 'change' }],
        email: [
          { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
        ]
      },
      editForm: {
        id: null,
        userId: null,
        userName: '',
        phone: '',
        password: '',
        nickName: '',
        email: '',
        sex: '0',
        status: '0',
        roleMappingId: null,
        avatar: '',
        description: ''
      },
      editRules: {
        nickName: [{ required: true, message: '用户昵称不能为空', trigger: 'blur' }],
        roleMappingId: [{ required: true, message: '请选择角色', trigger: 'change' }],
        email: [
          { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
        ]
      },
      queryParams: { pageNum: 1, pageSize: 10, phonenumber: null, nickName: null }
    }
  },
  created() { this.getList(); this.getRoleOptions() },
  methods: {
    getList() {
      this.loading = true
      listPlatformUser(this.queryParams).then(res => {
        this.userList = res.rows
        this.total = res.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    // 角色下拉数据源：本租户角色映射启用行（按 sort_order 排序，含同 code 多业务名）
    getRoleOptions() {
      listPlatformRoleMapping({ pageNum: 1, pageSize: 100, status: '0' }).then(res => {
        this.roleOptions = res.rows || []
      }).catch(() => { this.roleOptions = [] })
    },
    roleLabel(r) {
      return r.displayName + '(' + r.roleCode + ')'
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm('queryForm'); this.handleQuery() },
    handleAdd() {
      this.addForm = {
        userName: '',
        password: '123456',
        phone: '',
        nickName: '',
        email: '',
        sex: '0',
        status: '0',
        avatar: '',
        description: '',
        roleMappingId: null
      }
      this.resetForm('addForm')
      this.addOpen = true
    },
    submitAdd() {
      this.$refs['addForm'].validate(valid => {
        if (!valid) return
        const opt = this.roleOptions.find(r => r.id === this.addForm.roleMappingId)
        addPlatformUser({
          userName: this.addForm.userName,
          password: this.addForm.password,
          phonenumber: this.addForm.phone,
          nickName: this.addForm.nickName,
          email: this.addForm.email,
          sex: this.addForm.sex,
          status: this.addForm.status,
          avatar: this.addForm.avatar,
          description: this.addForm.description,
          roleCode: opt.roleCode,
          roleMappingId: opt.id
        }).then(() => {
          this.$modal.msgSuccess('新增成功')
          this.addOpen = false
          this.getList()
        })
      })
    },
    handleRemove(row) {
      this.$modal.confirm('确认移除用户「' + row.nickName + '」的角色分配吗？')
        .then(() => delPlatformUser(row.id))
        .then(() => { this.$modal.msgSuccess('移除成功'); this.getList() })
    },
    handleEdit(row) {
      // 回填：优先按已记录映射行；存量/映射变更后回退同 role_code 首行
      const mapped = this.roleOptions.find(r => r.id === row.roleMappingId)
      const fallback = this.roleOptions.find(r => r.roleCode === row.roleCode)
      this.editForm = {
        id: row.id,
        userId: row.userId,
        userName: row.userName,
        phone: row.phonenumber,
        password: '',
        nickName: row.nickName,
        email: row.email || '',
        sex: row.sex || '0',
        status: row.status,
        roleMappingId: (mapped || fallback || {}).id || null,
        avatar: row.avatar || '',
        description: row.description || ''
      }
      this.editOpen = true
    },
    submitEdit() {
      this.$refs['editForm'].validate(valid => {
        if (!valid) return
        const opt = this.roleOptions.find(r => r.id === this.editForm.roleMappingId)
        editPlatformUser({
          id: this.editForm.id,
          userId: this.editForm.userId,
          userName: this.editForm.userName,
          password: this.editForm.password,
          nickName: this.editForm.nickName,
          email: this.editForm.email,
          sex: this.editForm.sex,
          status: this.editForm.status,
          roleCode: opt.roleCode,
          roleMappingId: opt.id,
          avatar: this.editForm.avatar,
          description: this.editForm.description
        }).then(() => {
          this.$modal.msgSuccess('修改成功')
          this.editOpen = false
          this.getList()
        })
      })
    },
    handleAvatarSuccess(res) {
      if (res.code === 200) {
        this.addForm.avatar = res.data.url
        this.$modal.msgSuccess('头像上传成功')
      } else {
        this.$modal.msgError(res.msg || '头像上传失败')
      }
    },
    handleEditAvatarSuccess(res) {
      if (res.code === 200) {
        this.editForm.avatar = res.data.url
        this.$modal.msgSuccess('头像上传成功')
      } else {
        this.$modal.msgError(res.msg || '头像上传失败')
      }
    },
    beforeAvatarUpload(file) {
      const isImage = file.type.indexOf('image') > -1
      const isLt5M = file.size / 1024 / 1024 < 5
      if (!isImage) {
        this.$modal.msgError('只能上传图片文件')
        return false
      }
      if (!isLt5M) {
        this.$modal.msgError('图片大小不能超过 5MB')
        return false
      }
      return true
    }
  }
}
</script>

<style scoped>
.avatar-uploader {
  display: inline-block;
}
.avatar-uploader >>> .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-uploader >>> .el-upload:hover {
  border-color: #409EFF;
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
}
.avatar-image {
  width: 100px;
  height: 100px;
  object-fit: cover;
}
.avatar-remove-btn {
  display: block;
  margin-top: 4px;
  color: #F56C6C;
  cursor: pointer;
  font-size: 12px;
}
</style>
