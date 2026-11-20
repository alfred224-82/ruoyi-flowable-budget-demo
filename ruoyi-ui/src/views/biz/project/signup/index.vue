<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="所属活动" prop="projectId">
        <el-select v-model="queryParams.projectId" placeholder="请选择活动/团购" filterable clearable>
          <el-option v-for="p in containers" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="签到状态" prop="signInStatus">
        <el-select v-model="queryParams.signInStatus" placeholder="请选择签到状态" clearable>
          <el-option label="未签到" value="NOT_SIGNED" />
          <el-option label="已签到" value="SIGNED" />
          <el-option label="缺席" value="ABSENT" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['biz:signup:add']">报名登记</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="signupList">
      <el-table-column label="报名编号" align="center" prop="signupNo" width="160" show-overflow-tooltip />
      <el-table-column label="所属活动" align="center" min-width="140" show-overflow-tooltip>
        <template slot-scope="scope">{{ containerName(scope.row.projectId) }}</template>
      </el-table-column>
      <el-table-column label="报名人" align="center" width="110">
        <template slot-scope="scope">{{ signupUser(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="人数" align="center" prop="peopleCount" width="70" />
      <el-table-column label="候补" align="center" width="70">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.isWaitlist === 1" type="warning" size="small">候补</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="签到状态" align="center" width="90">
        <template slot-scope="scope">
          <el-tag :type="signTag(scope.row.signInStatus)" size="small">{{ signText(scope.row.signInStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="报名时间" align="center" prop="signupTime" width="150">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.signupTime) }}</span></template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="230">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-check" @click="handleCheckin(scope.row, 'SIGNED')" v-hasPermi="['biz:signup:checkin']" v-if="scope.row.signInStatus === 'NOT_SIGNED'">签到</el-button>
          <el-button size="mini" type="text" icon="el-icon-close" @click="handleCheckin(scope.row, 'ABSENT')" v-hasPermi="['biz:signup:checkin']" v-if="scope.row.signInStatus === 'NOT_SIGNED'">缺席</el-button>
          <el-button size="mini" type="text" icon="el-icon-s-data" @click="handleTeam(scope.row)" v-hasPermi="['biz:signup:query']">成团</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleCancel(scope.row)" v-hasPermi="['biz:signup:remove']">取消</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 报名登记 -->
    <el-dialog title="报名登记（满员进候补）" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="所属活动" prop="projectId">
          <el-select v-model="form.projectId" placeholder="请选择活动/团购" filterable style="width: 100%;">
            <el-option v-for="p in containers" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="报名用户" prop="userId">
          <el-select v-model="form.userId" placeholder="留空为游客报名（记 openid）" filterable clearable style="width: 100%;">
            <el-option v-for="u in userOptions" :key="u.userId" :label="u.nickName" :value="u.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="游客openid" prop="openid">
          <el-input v-model="form.openid" placeholder="游客报名时填写微信 openid" />
        </el-form-item>
        <el-form-item label="报名人数" prop="peopleCount">
          <el-input-number v-model="form.peopleCount" :min="1" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="open = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 成团状态 -->
    <el-dialog title="容器成团状态（策略实时判定）" :visible.sync="teamOpen" width="420px" append-to-body>
      <el-descriptions :column="1" border size="small" v-if="teamResult">
        <el-descriptions-item label="是否成团">
          <el-tag :type="teamResult.formed ? 'success' : 'warning'" size="small">{{ teamResult.formed ? '已成团' : '未成团' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="当前/目标">{{ teamResult.current }} / {{ teamResult.target < 0 ? '不限' : teamResult.target }}</el-descriptions-item>
        <el-descriptions-item label="判定说明">{{ teamResult.message }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
import { listSignup, addSignup, cancelSignup, checkinSignup, teamStatus } from '@/api/biz/signup'
import { listProject, listUserOptions } from '@/api/biz/project'

export default {
  name: 'BizProjectSignup',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      signupList: [],
      containers: [],
      userOptions: [],
      open: false,
      teamOpen: false,
      teamResult: null,
      queryParams: { pageNum: 1, pageSize: 10, projectId: null, signInStatus: null },
      form: {},
      rules: {
        projectId: [{ required: true, message: '请选择所属活动', trigger: 'change' }]
      }
    }
  },
  created() {
    this.getList()
    // 报名仅活动/团购容器可用
    Promise.all([
      listProject({ pageNum: 1, pageSize: 200, type: 'activity' }),
      listProject({ pageNum: 1, pageSize: 200, type: 'team_buy' })
    ]).then(([a, b]) => { this.containers = [...(a.rows || []), ...(b.rows || [])] })
    listUserOptions().then(res => { this.userOptions = res.data || [] }).catch(() => {})
  },
  methods: {
    getList() {
      this.loading = true
      listSignup(this.queryParams).then(response => {
        this.signupList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    containerName(id) {
      const p = this.containers.find(c => c.id === id)
      return p ? p.name : ('#' + id)
    },
    signupUser(row) {
      if (row.userId == null) { return '游客' + (row.openid ? '(' + row.openid.slice(-6) + ')' : '') }
      const u = this.userOptions.find(x => x.userId === row.userId)
      return u ? u.nickName : ('#' + row.userId)
    },
    signText(status) {
      const map = { NOT_SIGNED: '未签到', SIGNED: '已签到', ABSENT: '缺席' }
      return map[status] || status
    },
    signTag(status) {
      const map = { NOT_SIGNED: 'info', SIGNED: 'success', ABSENT: 'danger' }
      return map[status] || 'info'
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleAdd() {
      this.form = { projectId: null, userId: null, openid: null, peopleCount: 1 }
      this.resetForm('form')
      this.open = true
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          addSignup(this.form).then(res => {
            const t = res.data
            this.$modal.msgSuccess('报名成功' + (t ? '：' + (t.formed ? '已成团' : '未成团') + '（' + t.current + '/' + (t.target < 0 ? '不限' : t.target) + '）' : ''))
            this.open = false
            this.getList()
          })
        }
      })
    },
    handleCheckin(row, signInStatus) {
      checkinSignup(row.id, signInStatus).then(() => {
        this.$modal.msgSuccess(signInStatus === 'SIGNED' ? '签到成功' : '已标记缺席')
        this.getList()
      })
    },
    handleTeam(row) {
      teamStatus(row.projectId).then(res => {
        this.teamResult = res.data
        this.teamOpen = true
      })
    },
    handleCancel(row) {
      this.$modal.confirm('确认取消报名「' + row.signupNo + '」？').then(() => {
        return cancelSignup(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('取消成功')
      }).catch(() => {})
    }
  }
}
</script>
