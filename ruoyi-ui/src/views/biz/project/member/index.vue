<template>
  <div class="app-container">
    <el-form size="small" :inline="true" label-width="80px">
      <el-form-item label="所属容器">
        <el-select v-model="projectId" placeholder="请选择容器" filterable clearable style="width: 260px;" @change="loadMembers">
          <el-option v-for="p in containers" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['biz:member:edit']" :disabled="!projectId">追加成员</el-button>
        <el-button type="success" plain icon="el-icon-check" size="mini" @click="handleSave" v-hasPermi="['biz:member:edit']" :disabled="!projectId">保存成员</el-button>
      </el-form-item>
    </el-form>

    <el-alert v-if="!projectId" title="请先选择容器查看/维护成员（五枚举：owner/manager/member/participant/signup）" type="info" :closable="false" class="mb8" />

    <el-table v-loading="loading" :data="memberList">
      <el-table-column label="用户" align="center" min-width="140">
        <template slot-scope="scope">{{ userName(scope.row.userId) }}</template>
      </el-table-column>
      <el-table-column label="用户ID" align="center" prop="userId" width="120" />
      <el-table-column label="容器角色" align="center" width="160">
        <template slot-scope="scope">
          <el-select v-model="scope.row.role" size="small" :disabled="!canEdit()">
            <el-option label="负责人 owner" value="owner" />
            <el-option label="管理者 manager" value="manager" />
            <el-option label="成员 member" value="member" />
            <el-option label="参与者 participant" value="participant" />
            <el-option label="报名者 signup" value="signup" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="加入时间" align="center" prop="joinTime" width="160">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.joinTime) }}</span></template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="100">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleRemove(scope.row)" v-hasPermi="['biz:member:remove']">移除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 追加成员 -->
    <el-dialog title="追加成员" :visible.sync="open" width="460px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="成员" prop="userId">
          <el-select v-model="form.userId" placeholder="请选择本租户用户" filterable style="width: 100%;">
            <el-option v-for="u in availableUsers" :key="u.userId" :label="u.nickName" :value="u.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="容器角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%;">
            <el-option label="负责人 owner" value="owner" />
            <el-option label="管理者 manager" value="manager" />
            <el-option label="成员 member" value="member" />
            <el-option label="参与者 participant" value="participant" />
            <el-option label="报名者 signup" value="signup" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="open = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listProject, listMembers, saveMembers, removeMember, listUserOptions } from '@/api/biz/project'
import { checkPermi } from '@/utils/permission'

export default {
  name: 'BizProjectMember',
  data() {
    return {
      loading: false,
      projectId: null,
      containers: [],
      memberList: [],
      userOptions: [],
      open: false,
      form: {},
      rules: {
        userId: [{ required: true, message: '请选择成员', trigger: 'change' }],
        role: [{ required: true, message: '请选择容器角色', trigger: 'change' }]
      }
    }
  },
  computed: {
    // 下拉排除已是成员的用户（uk_project_user 一容器一用户仅一条）
    availableUsers() {
      const ids = this.memberList.map(m => m.userId)
      return this.userOptions.filter(u => !ids.includes(u.userId))
    }
  },
  created() {
    listProject({ pageNum: 1, pageSize: 200 }).then(res => { this.containers = res.rows || [] })
    listUserOptions().then(res => { this.userOptions = res.data || [] }).catch(() => {})
  },
  methods: {
    canEdit() {
      return checkPermi(['biz:member:edit'])
    },
    loadMembers() {
      if (!this.projectId) {
        this.memberList = []
        return
      }
      this.loading = true
      listMembers(this.projectId).then(res => {
        this.memberList = res.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    userName(id) {
      const u = this.userOptions.find(x => x.userId === id)
      return u ? u.nickName : ('#' + id)
    },
    handleAdd() {
      this.form = { userId: null, role: 'member' }
      this.resetForm('form')
      this.open = true
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          this.memberList.push({ userId: this.form.userId, userName: this.userName(this.form.userId), role: this.form.role })
          this.open = false
        }
      })
    },
    handleSave() {
      const members = this.memberList.map(m => ({ userId: m.userId, userName: m.userName, role: m.role }))
      saveMembers(this.projectId, members).then(() => {
        this.$modal.msgSuccess('保存成功')
        this.loadMembers()
      })
    },
    handleRemove(row) {
      this.$modal.confirm('确认将「' + this.userName(row.userId) + '」移出容器？').then(() => {
        return removeMember(this.projectId, row.userId)
      }).then(() => {
        this.loadMembers()
        this.$modal.msgSuccess('移除成功')
      }).catch(() => {})
    }
  }
}
</script>
