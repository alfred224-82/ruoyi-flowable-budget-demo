<template>
  <div class="app-container">
    <el-alert title="平台合规管理（Admin 专属）" type="info"
      description="语音合规总开关 / 录音留存全局上限 / 平台敏感词库 CRUD / 合规告警看板。" show-icon :closable="false" class="mb16" />

    <el-tabs v-model="activeTab">
      <!-- ===== 合规告警看板 ===== -->
      <el-tab-pane label="合规告警看板" name="dashboard">
        <el-card shadow="hover">
          <div slot="header"><span style="font-weight:600;">近 7 天命中趋势</span>
            <el-button size="mini" type="primary" style="float:right;" @click="loadDashboard">刷新</el-button>
          </div>
          <el-descriptions :column="3" border v-if="dashboard">
            <el-descriptions-item label="7 天总命中">{{ dashboard.totalHits7d }}</el-descriptions-item>
            <el-descriptions-item label="待处理">{{ dashboard.pendingCount }}</el-descriptions-item>
            <el-descriptions-item label="全局留存上限">{{ globalRetention }} 天</el-descriptions-item>
          </el-descriptions>
          <div v-if="dashboard && dashboard.byType" class="mt16">
            <span style="font-weight:600;">按类型分布：</span>
            <el-tag v-for="(count, type) in dashboard.byType" :key="type" class="mr8" size="small">
              {{ hitTypeMap[type] || type }}：{{ count }}
            </el-tag>
          </div>
        </el-card>
        <!-- 全局留存上限设置 -->
        <el-card shadow="hover" class="mt16">
          <div slot="header"><span style="font-weight:600;">全局录音留存上限</span></div>
          <el-form :inline="true" size="small">
            <el-form-item label="上限天数">
              <el-input-number v-model="globalRetention" :min="1" :max="3650" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveGlobalRetention">保存</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- ===== 平台敏感词库 ===== -->
      <el-tab-pane label="平台敏感词库" name="words">
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="80px">
          <el-form-item label="敏感词" prop="word">
            <el-input v-model="queryParams.word" placeholder="搜索关键词" clearable :maxlength="100" />
          </el-form-item>
          <el-form-item label="分类" prop="category">
            <el-select v-model="queryParams.category" placeholder="全部" clearable>
              <el-option label="政治" value="POLITICAL" />
              <el-option label="色情" value="PORN" />
              <el-option label="暴力" value="VIOLENCE" />
              <el-option label="辱骂" value="ABUSE" />
              <el-option label="自定义" value="CUSTOM" />
            </el-select>
          </el-form-item>
          <el-form-item label="级别" prop="level">
            <el-select v-model="queryParams.level" placeholder="全部" clearable>
              <el-option label="警告" value="WARN" />
              <el-option label="拦截" value="BLOCK" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
            <el-button type="success" icon="el-icon-plus" size="mini" @click="handleAdd">新增</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="loading" :data="wordList">
          <el-table-column label="ID" align="center" prop="id" width="60" />
          <el-table-column label="敏感词" align="left" prop="word" min-width="120" show-overflow-tooltip />
          <el-table-column label="分类" align="center" prop="category" width="80">
            <template slot-scope="scope">
              {{ categoryMap[scope.row.category] || scope.row.category }}
            </template>
          </el-table-column>
          <el-table-column label="级别" align="center" width="80">
            <template slot-scope="scope">
              <el-tag :type="scope.row.level === 'BLOCK' ? 'danger' : 'warning'" size="mini">
                {{ scope.row.level === 'BLOCK' ? '拦截' : '警告' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="来源" align="center" prop="source" width="80" />
          <el-table-column label="状态" align="center" width="80">
            <template slot-scope="scope">
              <el-tag :type="scope.row.status === 'ENABLED' ? 'success' : 'info'" size="mini">
                {{ scope.row.status === 'ENABLED' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" width="150" />
          <el-table-column label="操作" align="center" width="120" fixed="right">
            <template slot-scope="scope">
              <el-button size="mini" type="text" @click="handleEdit(scope.row)">编辑</el-button>
              <el-button v-if="scope.row.source !== 'SYSTEM'" size="mini" type="text" style="color:#f56c6c;" @click="handleDelete(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
      </el-tab-pane>
    </el-tabs>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogOpen" width="500px">
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="敏感词" prop="word">
          <el-input v-model="form.word" :maxlength="100" show-word-limit placeholder="请输入敏感词" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择">
            <el-option label="政治" value="POLITICAL" />
            <el-option label="色情" value="PORN" />
            <el-option label="暴力" value="VIOLENCE" />
            <el-option label="辱骂" value="ABUSE" />
            <el-option label="自定义" value="CUSTOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="级别" prop="level">
          <el-select v-model="form.level" placeholder="请选择">
            <el-option label="警告" value="WARN" />
            <el-option label="拦截" value="BLOCK" />
          </el-select>
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
import { getGlobalRetention, setGlobalRetention, getAlertDashboard, listPlatformWords, addPlatformWord, updatePlatformWord, removePlatformWord } from '@/api/micro/compliance'

export default {
  name: 'Compliance',
  data() {
    return {
      activeTab: 'dashboard',
      loading: false,
      total: 0,
      wordList: [],
      queryParams: { pageNum: 1, pageSize: 10, word: null, category: null, level: null },
      categoryMap: { POLITICAL: '政治', PORN: '色情', VIOLENCE: '暴力', ABUSE: '辱骂', CUSTOM: '自定义' },
      hitTypeMap: { SENSITIVE_WORD: '敏感词', NO_TRANSCRIPTION: '未转写', POLICY_VIOLATION: '策略违规' },
      globalRetention: 365,
      dashboard: null,
      // 弹窗
      dialogOpen: false,
      dialogTitle: '',
      form: {},
      rules: {
        word: [{ required: true, message: '敏感词不能为空', trigger: 'blur' }],
        category: [{ required: true, message: '请选择分类', trigger: 'change' }],
        level: [{ required: true, message: '请选择级别', trigger: 'change' }]
      }
    }
  },
  created() { this.loadDashboard(); this.getList() },
  methods: {
    loadDashboard() {
      getAlertDashboard().then(res => { this.dashboard = res.data && res.data[0] ? res.data[0] : null })
      getGlobalRetention().then(res => { this.globalRetention = res.data || 365 })
    },
    saveGlobalRetention() {
      setGlobalRetention(this.globalRetention).then(() => { this.$modal.msgSuccess('保存成功') })
    },
    getList() {
      this.loading = true
      listPlatformWords(this.queryParams).then(res => {
        this.wordList = res.rows; this.total = res.total; this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm('queryForm'); this.handleQuery() },
    handleAdd() { this.form = { category: 'CUSTOM', level: 'WARN' }; this.dialogTitle = '新增平台敏感词'; this.dialogOpen = true },
    handleEdit(row) { this.form = { ...row }; this.dialogTitle = '编辑平台敏感词'; this.dialogOpen = true },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) return
        if (this.form.id) {
          updatePlatformWord(this.form).then(() => { this.$modal.msgSuccess('修改成功'); this.dialogOpen = false; this.getList() })
        } else {
          addPlatformWord(this.form).then(() => { this.$modal.msgSuccess('新增成功'); this.dialogOpen = false; this.getList() })
        }
      })
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除敏感词「' + row.word + '」？')
        .then(() => removePlatformWord(row.id))
        .then(() => { this.$modal.msgSuccess('删除成功'); this.getList() })
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.mt16 { margin-top: 16px; }
.mr8 { margin-right: 8px; }
</style>
