<template>
  <div class="app-container">
    <el-alert title="语音与合规配置（租户管理员）" type="info"
      description="语音开关 / 敏感词管理（平台底库+自定义）/ 录音留存设置（≤ 全局上限）/ 转写留痕开关。活动类型自动取当前租户行业。" show-icon :closable="false" class="mb16" />

    <el-tabs v-model="activeTab">
      <!-- ===== 合规规则管理 ===== -->
      <el-tab-pane label="合规规则" name="rules">
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="80px">
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="全部" clearable>
              <el-option label="启用" value="NORMAL" />
              <el-option label="停用" value="DISABLED" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
            <el-button type="success" icon="el-icon-plus" size="mini" @click="handleAdd">新增</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="loading" :data="ruleList">
          <el-table-column label="ID" align="center" prop="id" width="60" />
          <el-table-column label="语音开关" align="center" width="80">
            <template slot-scope="scope">
              <el-tag :type="scope.row.enabled ? 'success' : 'info'" size="mini">
                {{ scope.row.enabled ? '开启' : '关闭' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="敏感词策略" align="center" width="100" prop="sensitiveWordOverride" />
          <el-table-column label="转写留痕" align="center" width="80">
            <template slot-scope="scope">
              <el-tag :type="scope.row.transcriptionEnabled ? 'success' : 'info'" size="mini">
                {{ scope.row.transcriptionEnabled ? '开启' : '关闭' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="最大语音时长" align="center" width="110">
            <template slot-scope="scope">
              {{ scope.row.maxVoiceDuration ? Math.ceil(scope.row.maxVoiceDuration / 60) + ' 分钟' : '不限' }}
            </template>
          </el-table-column>
          <el-table-column label="状态" align="center" width="80">
            <template slot-scope="scope">
              <el-tag :type="scope.row.status === 'NORMAL' ? 'success' : 'danger'" size="mini">
                {{ scope.row.status === 'NORMAL' ? '启用' : '停用' }}
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
      </el-tab-pane>

      <!-- ===== 录音留存策略 ===== -->
      <el-tab-pane label="录音留存" name="policy">
        <el-card shadow="hover">
          <el-form ref="policyForm" :model="policyForm" label-width="120px" size="small">
            <el-form-item label="留存天数">
              <el-input-number v-model="policyForm.maxRetentionDays" :min="1" :max="3650" />
            </el-form-item>
            <el-form-item label="存储路径">
              <el-input v-model="policyForm.storagePath" placeholder="OSS 路径前缀" :maxlength="500" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="savePolicy">保存</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- ===== 敏感词管理 ===== -->
      <el-tab-pane label="敏感词管理" name="words">
        <el-form :model="wordQuery" ref="wordQueryForm" size="small" :inline="true" label-width="80px">
          <el-form-item label="敏感词" prop="word">
            <el-input v-model="wordQuery.word" placeholder="搜索" clearable :maxlength="100" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="handleWordQuery">搜索</el-button>
            <el-button type="success" icon="el-icon-plus" size="mini" @click="handleAddWord">新增</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="wordLoading" :data="wordList">
          <el-table-column label="ID" align="center" prop="id" width="60" />
          <el-table-column label="敏感词" align="left" prop="word" min-width="120" show-overflow-tooltip />
          <el-table-column label="分类" align="center" width="80">
            <template slot-scope="scope">
              <span>{{ categoryText(scope.row.category) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="匹配模式" align="center" width="90">
            <template slot-scope="scope">
              <el-tag :type="matchModeTagType(scope.row.matchMode)" size="mini">
                {{ matchModeText(scope.row.matchMode) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" align="center" width="70">
            <template slot-scope="scope">
              <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'" size="mini">
                {{ scope.row.status === '0' ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="来源" align="center" width="80">
            <template slot-scope="scope">
              <span>{{ sourceText(scope.row.source) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="120" fixed="right">
            <template slot-scope="scope">
              <el-button size="mini" type="text" @click="handleEditWord(scope.row)">编辑</el-button>
              <el-button v-if="scope.row.source !== 'global'" size="mini" type="text" style="color:#f56c6c;" @click="handleDeleteWord(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="wordTotal > 0" :total="wordTotal" :page.sync="wordQuery.pageNum" :limit.sync="wordQuery.pageSize" @pagination="getWordList" />
      </el-tab-pane>
    </el-tabs>

    <!-- 规则弹窗 -->
    <el-dialog :title="ruleDialogTitle" :visible.sync="ruleDialogOpen" width="550px">
      <el-form ref="ruleForm" :model="ruleForm" :rules="ruleRules" label-width="100px">
        <el-form-item label="语音开关">
          <el-switch v-model="ruleForm.enabled" />
        </el-form-item>
        <el-form-item label="敏感词策略">
          <el-select v-model="ruleForm.sensitiveWordOverride">
            <el-option label="继承平台底库" value="INHERIT" />
            <el-option label="仅自定义" value="OVERRIDE" />
            <el-option label="平台+自定义合并" value="MERGE" />
          </el-select>
        </el-form-item>
        <el-form-item label="转写留痕">
          <el-switch v-model="ruleForm.transcriptionEnabled" />
        </el-form-item>
        <el-form-item label="最大语音时长">
          <el-input-number v-model="ruleForm.maxVoiceDurationMinutes" :min="0" :max="60" :step="1" style="width:160px;" />
          <span style="margin-left:8px;color:#909399;font-size:12px;">分钟（0 = 不限时长）</span>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="ruleDialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitRule">确定</el-button>
      </span>
    </el-dialog>

    <!-- 敏感词弹窗 -->
    <el-dialog :title="wordDialogTitle" :visible.sync="wordDialogOpen" width="500px">
      <el-form ref="wordForm" :model="wordForm" :rules="wordRules" label-width="80px">
        <el-form-item label="敏感词" prop="word">
          <el-input v-model="wordForm.word" :maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="wordForm.category">
            <el-option label="自定义" value="CUSTOM" />
            <el-option label="政治" value="POLITICAL" />
            <el-option label="色情" value="PORN" />
            <el-option label="暴力" value="VIOLENCE" />
            <el-option label="辱骂" value="ABUSE" />
          </el-select>
        </el-form-item>
        <el-form-item label="匹配模式" prop="matchMode">
          <el-select v-model="wordForm.matchMode">
            <el-option label="精确匹配" value="exact" />
            <el-option label="模糊匹配（编辑距离≤2）" value="fuzzy" />
            <el-option label="正则表达式" value="regex" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="wordForm.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="wordDialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitWord">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { listComplianceRules, addComplianceRule, updateComplianceRule, removeComplianceRule, getRecordingPolicy, saveRecordingPolicy, listTenantWords, addTenantWord, updateTenantWord, removeTenantWord } from '@/api/micro/compliance'

export default {
  name: 'ComplianceConfig',
  data() {
    return {
      activeTab: 'rules',
      // 规则
      loading: false, total: 0, ruleList: [],
      queryParams: { pageNum: 1, pageSize: 10, status: null },
      ruleDialogOpen: false, ruleDialogTitle: '', ruleForm: {},
      ruleRules: {},
      // 录音策略
      policyForm: { maxRetentionDays: 30, storagePath: '' },
      // 敏感词
      wordLoading: false, wordTotal: 0, wordList: [],
      wordQuery: { pageNum: 1, pageSize: 10, word: null },
      wordDialogOpen: false, wordDialogTitle: '', wordForm: {},
      wordRules: { word: [{ required: true, message: '敏感词不能为空', trigger: 'blur' }] }
    }
  },
  created() { this.getList(); this.loadPolicy(); this.getWordList() },
  methods: {
    // ===== 规则 =====
    getList() { this.loading = true; listComplianceRules(this.queryParams).then(res => { this.ruleList = res.rows; this.total = res.total; this.loading = false }).catch(() => { this.loading = false }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm('queryForm'); this.handleQuery() },
    handleAdd() { this.ruleForm = { enabled: false, sensitiveWordOverride: 'INHERIT', transcriptionEnabled: false, maxVoiceDurationMinutes: 3 }; this.ruleDialogTitle = '新增合规规则'; this.ruleDialogOpen = true },
    handleEdit(row) { this.ruleForm = { ...row, maxVoiceDurationMinutes: row.maxVoiceDuration ? Math.round(row.maxVoiceDuration / 60) : 3 }; this.ruleDialogTitle = '编辑合规规则'; this.ruleDialogOpen = true },
    submitRule() {
      this.$refs['ruleForm'].validate(valid => { if (!valid) return
        // 分钟 → 秒
        const payload = { ...this.ruleForm, maxVoiceDuration: (this.ruleForm.maxVoiceDurationMinutes || 0) * 60 }
        delete payload.maxVoiceDurationMinutes
        if (payload.id) { updateComplianceRule(payload).then(() => { this.$modal.msgSuccess('修改成功'); this.ruleDialogOpen = false; this.getList() }) }
        else { addComplianceRule(payload).then(() => { this.$modal.msgSuccess('新增成功'); this.ruleDialogOpen = false; this.getList() }) }
      })
    },
    handleDelete(row) { this.$modal.confirm('确认删除该规则？').then(() => removeComplianceRule(row.id)).then(() => { this.$modal.msgSuccess('删除成功'); this.getList() }) },
    // ===== 录音策略 =====
    loadPolicy() { getRecordingPolicy().then(res => { if (res.data) this.policyForm = { ...res.data } }) },
    savePolicy() { saveRecordingPolicy(this.policyForm).then(() => { this.$modal.msgSuccess('保存成功') }) },
    // ===== 敏感词 =====
    getWordList() { this.wordLoading = true; listTenantWords(this.wordQuery).then(res => { this.wordList = res.rows; this.wordTotal = res.total; this.wordLoading = false }).catch(() => { this.wordLoading = false }) },
    handleWordQuery() { this.wordQuery.pageNum = 1; this.getWordList() },
    handleAddWord() { this.wordForm = { category: 'CUSTOM', matchMode: 'exact', status: '0' }; this.wordDialogTitle = '新增自定义敏感词'; this.wordDialogOpen = true },
    handleEditWord(row) { this.wordForm = { ...row }; this.wordDialogTitle = '编辑敏感词'; this.wordDialogOpen = true },
    submitWord() {
      this.$refs['wordForm'].validate(valid => { if (!valid) return
        if (this.wordForm.id) { updateTenantWord(this.wordForm).then(() => { this.$modal.msgSuccess('修改成功'); this.wordDialogOpen = false; this.getWordList() }) }
        else { addTenantWord(this.wordForm).then(() => { this.$modal.msgSuccess('新增成功'); this.wordDialogOpen = false; this.getWordList() }) }
      })
    },
    handleDeleteWord(row) { this.$modal.confirm('确认删除敏感词「' + row.word + '」？').then(() => removeTenantWord(row.id)).then(() => { this.$modal.msgSuccess('删除成功'); this.getWordList() }) },
    categoryText(val) {
      const map = { CUSTOM: '自定义', POLITICAL: '政治', PORN: '色情', VIOLENCE: '暴力', ABUSE: '辱骂' }
      return map[val] || val || '-'
    },
    sourceText(val) {
      const map = { global: '平台底库', tenant: '自定义' }
      return map[val] || val || '-'
    },
    matchModeText(val) {
      const map = { exact: '精确', fuzzy: '模糊', regex: '正则' }
      return map[val] || val || '-'
    },
    matchModeTagType(val) {
      const map = { exact: 'success', fuzzy: 'warning', regex: 'info' }
      return map[val] || ''
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
</style>
