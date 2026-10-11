<template>
  <div class="app-container">
    <el-card shadow="never" v-loading="loading">
      <div slot="header">
        <span>项目成员维护：{{ projectName }}</span>
        <el-button style="float: right; margin-left: 10px;" type="primary" size="mini" @click="handleSave" v-hasPermi="['fee:project:edit']">保存</el-button>
        <el-button style="float: right;" size="mini" @click="back">返回</el-button>
      </div>
      <el-row :gutter="10" class="mb8">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="addMember">追加成员</el-button>
      </el-row>
      <!-- 成员跨项目负载提示（专业版 FEAT_MEMBER_LOAD，仅提示不拦截） -->
      <el-alert
        v-if="loadWarnings.length"
        :title="'负载提示：' + loadWarnings.map(w => w.userName + ' 已在 ' + w.projects.map(p => p.projectName + '(' + p.investRatio + '%)').join('、') + '，合计' + w.totalRatio + '%').join('；')"
        type="warning"
        :closable="false"
        style="margin-bottom: 10px;" />
      <el-table :data="members" border size="small">
        <el-table-column label="用户ID" width="160" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.userId || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="姓名" min-width="160">
          <template slot-scope="scope">
            <el-select v-model="scope.row.userId" placeholder="请选择报销人" size="small" filterable clearable style="width: 100%;" @change="val => onMemberChange(scope.row, val)">
              <el-option v-for="u in userOptions" :key="u.userId" :label="u.nickName" :value="u.userId" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="成员角色" width="160">
          <template slot-scope="scope">
            <el-select v-model="scope.row.role" size="small">
              <el-option label="项目经理" value="MANAGER" />
              <el-option label="成员" value="MEMBER" />
              <el-option label="只读" value="VIEWER" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="分成比例%" width="140">
          <template slot-scope="scope">
            <el-input-number v-model="scope.row.investRatio" :min="0" :max="100" size="small" style="width: 100%;" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template slot-scope="scope">
            <el-button size="mini" type="text" icon="el-icon-delete" @click="members.splice(scope.$index, 1)" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { getProject, listMembers, saveMembers, listReimbursers } from '@/api/system/fee/fundProject'
import { dashboardMemberLoad } from '@/api/system/fee/fundPro'
import { loadFeatures, featureVisible } from '@/utils/feature'

export default {
  name: 'FundProjectMembers',
  data() {
    return {
      loading: true,
      projectId: null,
      projectName: '',
      members: [],
      // 报销人(employee)角色用户下拉
      reimburserOptions: [],
      loadWarnings: []
    }
  },
  computed: {
    // 成员下拉：报销人角色人员 + 存量非报销人成员（仅保证回显，如项目经理）
    userOptions() {
      const extras = this.members
        .filter(m => m.userId && !this.reimburserOptions.some(u => u.userId === m.userId))
        .map(m => ({ userId: m.userId, nickName: m.userName }))
      return this.reimburserOptions.concat(extras)
    }
  },
  created() {
    this.projectId = this.$route.query.id
    this.loadData()
    // 成员仅限报销人角色人员
    listReimbursers().then(res => { this.reimburserOptions = res.data || [] }).catch(() => {})
    // 专业版开关：成员跨项目负载提示（展示成员已在哪些项目）
    loadFeatures().then(() => {
      if (featureVisible('FEAT_MEMBER_LOAD')) {
        dashboardMemberLoad().then(res => {
          this.loadWarnings = (res.data || []).filter(m => m.overLoad)
        }).catch(() => {})
      }
    })
  },
  methods: {
    loadData() {
      Promise.all([getProject(this.projectId), listMembers(this.projectId)]).then(([p, m]) => {
        this.projectName = p.data ? p.data.projectName : ''
        this.members = (m.data || []).map(item => ({ ...item }))
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    addMember() {
      this.members.push({ userId: null, userName: null, role: 'MEMBER', investRatio: 100 })
    },
    // 选中报销人后回填姓名
    onMemberChange(row, userId) {
      const user = this.userOptions.find(u => u.userId === userId)
      row.userName = user ? user.nickName : null
    },
    handleSave() {
      saveMembers(this.projectId, this.members).then(() => {
        this.$modal.msgSuccess('成员保存成功')
        this.loadData()
      })
    },
    back() {
      this.$router.push('/fee/fee/project')
    }
  }
}
</script>
