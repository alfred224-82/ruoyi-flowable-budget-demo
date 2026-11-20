<template>
  <div class="app-container">
    <el-alert
      title="游客信息管理（M31 T31.6/T31.8）：临时游客列表 + 补全信息 + 批量转正"
      type="info"
      description="扫码进入小程序的游客（手机号后4位标识），30天有效期。Boss 可补全游客真实姓名/身份证等信息，或批量将临时游客转为正式顾客。"
      show-icon
      :closable="false"
      class="mb8"
    />

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-check" size="mini" :disabled="!selectedIds.length" @click="handleBatchUpgrade" v-hasPermi="['micro:customer:profile:upgrade']">批量转正</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="visitorList" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="游客昵称" align="left" min-width="120" show-overflow-tooltip>
        <template slot-scope="scope">
          <span>{{ scope.row.nickName || '游客' + scope.row.phoneLast4 }}</span>
          <span class="sub-text">UID {{ scope.row.userId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="手机号后4位" align="center" prop="phoneLast4" width="110" />
      <el-table-column label="手机号" align="center" prop="phone" width="130">
        <template slot-scope="scope"><span>{{ scope.row.phone || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="真实姓名" align="center" prop="realName" width="100">
        <template slot-scope="scope"><span>{{ scope.row.realName || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="身份证" align="center" prop="idCard" width="160">
        <template slot-scope="scope"><span>{{ scope.row.idCard || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="信息补全" align="center" width="90">
        <template slot-scope="scope">
          <el-tag :type="scope.row.isComplete === 1 ? 'success' : 'warning'" size="mini">{{ scope.row.isComplete === 1 ? '已补全' : '未补全' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="账号状态" align="center" width="90">
        <template slot-scope="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="mini">{{ statusMap[scope.row.status] || '未知' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="到期时间" align="center" prop="expireTime" width="160">
        <template slot-scope="scope"><span>{{ scope.row.expireTime || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button type="text" size="mini" icon="el-icon-edit" @click="handleEdit(scope.row)" v-hasPermi="['micro:customer:profile:edit']">补全信息</el-button>
          <el-button type="text" size="mini" icon="el-icon-check" @click="handleSingleUpgrade(scope.row)" v-hasPermi="['micro:customer:profile:upgrade']">转正</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 补全游客信息弹窗 -->
    <el-dialog title="补全游客信息" :visible.sync="editOpen" width="520px" append-to-body>
      <el-form ref="editForm" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item label="游客昵称">
          <span>{{ editForm.nickName || '—' }}</span>
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="editForm.realName" placeholder="请输入真实姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="editForm.idCard" placeholder="请输入身份证号" maxlength="18" />
        </el-form-item>
        <el-form-item label="生日">
          <el-date-picker v-model="editForm.birthday" type="date" placeholder="选择生日" value-format="yyyy-MM-dd" style="width:100%;" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="editForm.gender">
            <el-radio :label="1">男</el-radio>
            <el-radio :label="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="editForm.address" placeholder="请输入地址" maxlength="200" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="editLoading" @click="submitEdit">确 定</el-button>
        <el-button @click="editOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listVisitorProfile, updateVisitorProfile, batchUpgradeVisitors } from '@/api/micro/customerProfile'

export default {
  name: 'MicroCustomerProfile',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      visitorList: [],
      selectedIds: [],
      queryParams: { pageNum: 1, pageSize: 10 },
      statusMap: { 0: '有效', 1: '已补全', 2: '已过期' },
      // 编辑弹窗
      editOpen: false,
      editLoading: false,
      editForm: {},
      editRules: {
        realName: [{ required: true, message: '真实姓名不能为空', trigger: 'blur' }],
        idCard: [
          { required: true, message: '身份证号不能为空', trigger: 'blur' },
          { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '身份证号格式不正确', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listVisitorProfile(this.queryParams).then(res => {
        this.visitorList = res.rows || []
        this.total = res.total || 0
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    statusTagType(status) {
      if (status === 0) return 'warning'
      if (status === 1) return 'success'
      if (status === 2) return 'info'
      return 'info'
    },
    onSelectionChange(selection) {
      this.selectedIds = selection.map(item => item.userId)
    },
    handleEdit(row) {
      this.editForm = { ...row }
      this.editOpen = true
    },
    submitEdit() {
      this.$refs['editForm'].validate(valid => {
        if (!valid) return
        this.editLoading = true
        updateVisitorProfile(this.editForm.userId, this.editForm).then(() => {
          this.$modal.msgSuccess('补全成功')
          this.editOpen = false
          this.getList()
        }).catch(() => {}).finally(() => { this.editLoading = false })
      })
    },
    handleSingleUpgrade(row) {
      this.$modal.confirm('确认将游客「' + (row.realName || row.nickName || 'UID ' + row.userId) + '」转为正式顾客吗？需要已填写身份证号。')
        .then(() => batchUpgradeVisitors([row.userId]))
        .then(res => {
          const data = res.data || {}
          if (data.failedCount > 0) {
            this.$modal.msgWarning('转正失败：' + (data.failures || []).map(f => f.reason).join('; '))
          } else {
            this.$modal.msgSuccess('转正成功')
          }
          this.getList()
        })
        .catch(() => {})
    },
    handleBatchUpgrade() {
      if (!this.selectedIds.length) return
      this.$modal.confirm('确认将选中的 ' + this.selectedIds.length + ' 位游客批量转为正式顾客吗？')
        .then(() => batchUpgradeVisitors(this.selectedIds))
        .then(res => {
          const data = res.data || {}
          const successCount = data.successCount || 0
          const failedCount = data.failedCount || 0
          if (failedCount > 0) {
            this.$modal.msgWarning('成功 ' + successCount + ' 人，失败 ' + failedCount + ' 人：' + (data.failures || []).map(f => f.reason).join('; '))
          } else {
            this.$modal.msgSuccess('批量转正成功 ' + successCount + ' 人')
          }
          this.getList()
        })
        .catch(() => {})
    }
  }
}
</script>

<style scoped>
.sub-text {
  display: block;
  color: #909399;
  font-size: 12px;
}
</style>
