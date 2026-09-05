<template>
  <div class="app-container">
    <el-alert
      title="开放平台为专业版能力（FEAT_OPEN_API）：对外提供 AppKey + HMAC-SHA256 签名鉴权、配额限流、IP 白名单与全量调用审计。AppSecret / Webhook Secret 加密存储、列表脱敏，仅新建或重置时一次性明文返回，请立即复制保存。"
      type="info" :closable="false" style="margin-bottom: 10px;" />

    <el-tabs v-model="activeTab" @tab-click="onTabChange">
      <!-- ==================== API凭证 ==================== -->
      <el-tab-pane label="API凭证" name="credential" v-if="checkPermi(['fee:openApi:list'])">
        <el-form :inline="true" size="small">
          <el-form-item label="应用名称">
            <el-input v-model="credQuery.appName" placeholder="请输入应用名称" clearable style="width: 180px;" @keyup.enter.native="handleCredQuery" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="credQuery.status" placeholder="全部" clearable style="width: 130px;">
              <el-option label="启用" value="ACTIVE" />
              <el-option label="停用" value="DISABLED" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="handleCredQuery">查询</el-button>
            <el-button icon="el-icon-refresh" @click="resetCredQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleCredAdd" v-hasPermi="['fee:openApi:add']">新建凭证</el-button>
          </el-col>
        </el-row>

        <el-table v-loading="credLoading" :data="credList" border size="small">
          <el-table-column label="AppKey" align="center" prop="appKey" min-width="200" show-overflow-tooltip />
          <el-table-column label="应用名称" align="center" prop="appName" min-width="130" show-overflow-tooltip />
          <el-table-column label="AppSecret" align="center" prop="appSecret" width="130" />
          <el-table-column label="日配额" align="center" width="90">
            <template slot-scope="scope">{{ scope.row.quotaDaily > 0 ? scope.row.quotaDaily : '不限' }}</template>
          </el-table-column>
          <el-table-column label="月配额" align="center" width="90">
            <template slot-scope="scope">{{ scope.row.quotaMonthly > 0 ? scope.row.quotaMonthly : '不限' }}</template>
          </el-table-column>
          <el-table-column label="IP白名单" align="center" prop="ipWhitelist" min-width="140" show-overflow-tooltip>
            <template slot-scope="scope">{{ scope.row.ipWhitelist || '不限' }}</template>
          </el-table-column>
          <el-table-column label="状态" align="center" width="80">
            <template slot-scope="scope">
              <el-switch :value="scope.row.status === 'ACTIVE'" @change="v => toggleCredStatus(scope.row, v)" v-hasPermi="['fee:openApi:edit']" />
            </template>
          </el-table-column>
          <el-table-column label="过期时间" align="center" width="150">
            <template slot-scope="scope">{{ scope.row.expireTime ? parseTime(scope.row.expireTime, '{y}-{m}-{d} {h}:{i}') : '永久' }}</template>
          </el-table-column>
          <el-table-column label="最近调用" align="center" width="150">
            <template slot-scope="scope">{{ scope.row.lastCallTime ? parseTime(scope.row.lastCallTime, '{y}-{m}-{d} {h}:{i}') : '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="210">
            <template slot-scope="scope">
              <el-button size="mini" type="text" icon="el-icon-edit" @click="handleCredEdit(scope.row)" v-hasPermi="['fee:openApi:edit']">修改</el-button>
              <el-button size="mini" type="text" icon="el-icon-refresh-right" @click="handleResetSecret(scope.row)" v-hasPermi="['fee:openApi:resetSecret']">重置密钥</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete" @click="handleCredDelete(scope.row)" v-hasPermi="['fee:openApi:remove']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="credTotal > 0" :total="credTotal" :page.sync="credQuery.pageNum" :limit.sync="credQuery.pageSize" @pagination="getCredList" />
      </el-tab-pane>

      <!-- ==================== 调用日志 ==================== -->
      <el-tab-pane label="调用日志" name="callLog" v-if="checkPermi(['fee:openApi:callLog'])">
        <el-form :inline="true" size="small">
          <el-form-item label="AppKey">
            <el-input v-model="logQuery.appKey" placeholder="请输入 AppKey" clearable style="width: 240px;" @keyup.enter.native="handleLogQuery" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="handleLogQuery">查询</el-button>
            <el-button icon="el-icon-refresh" @click="resetLogQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="logLoading" :data="logList" border size="small">
          <el-table-column type="expand">
            <template slot-scope="props">
              <el-form label-position="left" size="mini" class="log-detail">
                <el-form-item label="请求参数"><span>{{ props.row.requestParam || '-' }}</span></el-form-item>
                <el-form-item label="响应体"><span>{{ props.row.responseBody || '-' }}</span></el-form-item>
                <el-form-item label="失败原因"><span style="color:#F56C6C;">{{ props.row.errorMsg || '-' }}</span></el-form-item>
              </el-form>
            </template>
          </el-table-column>
          <el-table-column label="AppKey" align="center" prop="appKey" min-width="180" show-overflow-tooltip />
          <el-table-column label="接口路径" align="center" prop="apiPath" min-width="180" show-overflow-tooltip />
          <el-table-column label="方法" align="center" prop="httpMethod" width="80" />
          <el-table-column label="响应码" align="center" prop="responseCode" width="90">
            <template slot-scope="scope">
              <el-tag :type="scope.row.responseCode === 200 ? 'success' : 'danger'" size="small">{{ scope.row.responseCode }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="耗时(ms)" align="center" prop="costMs" width="90" />
          <el-table-column label="来源IP" align="center" prop="clientIp" width="130" show-overflow-tooltip />
          <el-table-column label="结果" align="center" width="80">
            <template slot-scope="scope">
              <el-tag :type="scope.row.success === 1 ? 'success' : 'danger'" size="small">{{ scope.row.success === 1 ? '成功' : '失败' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="调用时间" align="center" width="150">
            <template slot-scope="scope">{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</template>
          </el-table-column>
        </el-table>
        <pagination v-show="logTotal > 0" :total="logTotal" :page.sync="logQuery.pageNum" :limit.sync="logQuery.pageSize" @pagination="getCallLogList" />
      </el-tab-pane>

      <!-- ==================== Webhook配置 ==================== -->
      <el-tab-pane label="Webhook配置" name="webhook" v-if="checkPermi(['fee:openApi:webhook'])">
        <el-form :inline="true" size="small">
          <el-form-item label="关联凭证">
            <el-select v-model="hookQuery.credentialId" placeholder="全部" clearable filterable style="width: 240px;">
              <el-option v-for="c in credOptions" :key="c.id" :label="c.appName + ' (' + c.appKey + ')'" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="事件类型">
            <el-select v-model="hookQuery.eventType" placeholder="全部" clearable style="width: 180px;">
              <el-option v-for="e in eventTypeOptions" :key="e.value" :label="e.label" :value="e.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="handleHookQuery">查询</el-button>
            <el-button icon="el-icon-refresh" @click="resetHookQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleHookAdd" v-hasPermi="['fee:openApi:webhook']">新建Webhook</el-button>
          </el-col>
        </el-row>

        <el-table v-loading="hookLoading" :data="hookList" border size="small">
          <el-table-column label="关联凭证" align="center" min-width="180" show-overflow-tooltip>
            <template slot-scope="scope">{{ scope.row.appName }} ({{ scope.row.appKey }})</template>
          </el-table-column>
          <el-table-column label="事件类型" align="center" width="140">
            <template slot-scope="scope"><el-tag size="small">{{ eventTypeText(scope.row.eventType) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="回调地址" align="center" prop="callbackUrl" min-width="220" show-overflow-tooltip />
          <el-table-column label="签名密钥" align="center" prop="secret" width="130" />
          <el-table-column label="启用" align="center" width="80">
            <template slot-scope="scope">
              <el-tag :type="scope.row.isEnabled === 1 ? 'success' : 'info'" size="small">{{ scope.row.isEnabled === 1 ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="重试次数" align="center" prop="retryTimes" width="90" />
          <el-table-column label="创建时间" align="center" width="150">
            <template slot-scope="scope">{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</template>
          </el-table-column>
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="210">
            <template slot-scope="scope">
              <el-button size="mini" type="text" icon="el-icon-edit" @click="handleHookEdit(scope.row)" v-hasPermi="['fee:openApi:webhook']">修改</el-button>
              <el-button size="mini" type="text" icon="el-icon-refresh-right" @click="handleHookResetSecret(scope.row)" v-hasPermi="['fee:openApi:webhook']">重置密钥</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete" @click="handleHookDelete(scope.row)" v-hasPermi="['fee:openApi:webhook']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="hookTotal > 0" :total="hookTotal" :page.sync="hookQuery.pageNum" :limit.sync="hookQuery.pageSize" @pagination="getHookList" />
      </el-tab-pane>
    </el-tabs>

    <!-- 新建/修改凭证 -->
    <el-dialog :title="credForm.id ? '修改凭证' : '新建凭证'" :visible.sync="credOpen" width="560px" append-to-body>
      <el-form ref="credForm" :model="credForm" :rules="credRules" label-width="110px">
        <el-form-item label="应用名称" prop="appName">
          <el-input v-model="credForm.appName" placeholder="如 某某对接系统" />
        </el-form-item>
        <el-form-item label="日调用配额" prop="quotaDaily">
          <el-input-number v-model="credForm.quotaDaily" :min="0" :precision="0" controls-position="right" style="width: 200px;" />
          <span class="form-tip">0 或留空表示不限</span>
        </el-form-item>
        <el-form-item label="月调用配额" prop="quotaMonthly">
          <el-input-number v-model="credForm.quotaMonthly" :min="0" :precision="0" controls-position="right" style="width: 200px;" />
          <span class="form-tip">0 或留空表示不限</span>
        </el-form-item>
        <el-form-item label="IP白名单" prop="ipWhitelist">
          <el-input v-model="credForm.ipWhitelist" type="textarea" :rows="2" placeholder="多个IP用英文逗号分隔，留空表示不限" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="credForm.status">
            <el-radio label="ACTIVE">启用</el-radio>
            <el-radio label="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="过期时间" prop="expireTime">
          <el-date-picker v-model="credForm.expireTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="留空为永久有效" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="credForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitCredForm">确 定</el-button>
        <el-button @click="credOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 新建/修改 Webhook -->
    <el-dialog :title="hookForm.id ? '修改Webhook' : '新建Webhook'" :visible.sync="hookOpen" width="560px" append-to-body>
      <el-form ref="hookForm" :model="hookForm" :rules="hookRules" label-width="110px">
        <el-form-item label="关联凭证" prop="credentialId">
          <el-select v-model="hookForm.credentialId" placeholder="请选择凭证" filterable :disabled="!!hookForm.id" style="width: 100%;">
            <el-option v-for="c in credOptions" :key="c.id" :label="c.appName + ' (' + c.appKey + ')'" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="事件类型" prop="eventType">
          <el-select v-model="hookForm.eventType" placeholder="请选择事件类型" style="width: 100%;">
            <el-option v-for="e in eventTypeOptions" :key="e.value" :label="e.label" :value="e.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="回调地址" prop="callbackUrl">
          <el-input v-model="hookForm.callbackUrl" placeholder="https://your-domain.com/callback" />
        </el-form-item>
        <el-form-item label="是否启用" prop="isEnabled">
          <el-switch :value="hookForm.isEnabled === 1" @change="v => (hookForm.isEnabled = v ? 1 : 0)" />
        </el-form-item>
        <el-form-item label="失败重试次数" prop="retryTimes">
          <el-input-number v-model="hookForm.retryTimes" :min="0" :max="10" :precision="0" controls-position="right" style="width: 200px;" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="hookForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitHookForm">确 定</el-button>
        <el-button @click="hookOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 密钥一次性明文展示 -->
    <el-dialog :title="secretTitle" :visible.sync="secretOpen" width="560px" append-to-body :close-on-click-modal="false">
      <el-alert title="以下密钥仅此一次明文展示，关闭后不再可见，请立即复制并妥善保管。" type="warning" :closable="false" style="margin-bottom: 12px;" />
      <el-form label-width="90px" size="small">
        <el-form-item v-if="secretAppKey" label="AppKey">
          <el-input :value="secretAppKey" readonly />
        </el-form-item>
        <el-form-item :label="secretLabel">
          <el-input :value="secretValue" readonly type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="secretOpen = false">我已保存，关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listCredential, getCredential, addCredential, updateCredential, resetSecret, changeStatus, delCredential, listCallLog,
  listWebhook, getWebhook, addWebhook, updateWebhook, resetWebhookSecret, delWebhook
} from '@/api/system/fee/openApi'
import { checkPermi } from '@/utils/permission'

export default {
  name: 'FeeOpenApi',
  data() {
    return {
      activeTab: 'credential',
      eventTypeOptions: [
        { value: 'APPROVAL_PASSED', label: '审批通过' },
        { value: 'APPROVAL_REJECTED', label: '审批驳回' },
        { value: 'BILL_CREATED', label: '单据创建' },
        { value: 'BUDGET_ALERT', label: '预算预警' }
      ],
      // 凭证
      credLoading: false,
      credList: [],
      credTotal: 0,
      credQuery: { pageNum: 1, pageSize: 10, appName: null, status: null },
      credOptions: [],
      credOpen: false,
      credForm: {},
      credRules: {
        appName: [{ required: true, message: '应用名称不能为空', trigger: 'blur' }]
      },
      // 调用日志
      logLoading: false,
      logList: [],
      logTotal: 0,
      logQuery: { pageNum: 1, pageSize: 10, appKey: null },
      logLoaded: false,
      // Webhook
      hookLoading: false,
      hookList: [],
      hookTotal: 0,
      hookQuery: { pageNum: 1, pageSize: 10, credentialId: null, eventType: null },
      hookOpen: false,
      hookForm: {},
      hookLoaded: false,
      hookRules: {
        credentialId: [{ required: true, message: '关联凭证不能为空', trigger: 'change' }],
        eventType: [{ required: true, message: '事件类型不能为空', trigger: 'change' }],
        callbackUrl: [{ required: true, message: '回调地址不能为空', trigger: 'blur' }]
      },
      // 密钥明文展示
      secretOpen: false,
      secretTitle: '',
      secretLabel: 'AppSecret',
      secretAppKey: null,
      secretValue: ''
    }
  },
  created() {
    if (checkPermi(['fee:openApi:list'])) {
      this.activeTab = 'credential'
      this.getCredList()
    } else if (checkPermi(['fee:openApi:callLog'])) {
      this.activeTab = 'callLog'
      this.getCallLogList()
      this.logLoaded = true
    } else if (checkPermi(['fee:openApi:webhook'])) {
      this.activeTab = 'webhook'
      this.openWebhookTab()
    }
  },
  methods: {
    checkPermi,
    eventTypeText(v) {
      const e = this.eventTypeOptions.find(o => o.value === v)
      return e ? e.label : (v || '-')
    },
    onTabChange(tab) {
      if (tab.name === 'credential') {
        this.getCredList()
      } else if (tab.name === 'callLog') {
        if (!this.logLoaded) { this.getCallLogList(); this.logLoaded = true }
      } else if (tab.name === 'webhook') {
        this.openWebhookTab()
      }
    },
    // ---------- 凭证 ----------
    getCredList() {
      this.credLoading = true
      listCredential(this.credQuery).then(res => {
        this.credList = res.rows || []
        this.credTotal = res.total || 0
        this.credLoading = false
      }).catch(() => { this.credLoading = false })
    },
    handleCredQuery() {
      this.credQuery.pageNum = 1
      this.getCredList()
    },
    resetCredQuery() {
      this.credQuery = { pageNum: 1, pageSize: 10, appName: null, status: null }
      this.getCredList()
    },
    handleCredAdd() {
      this.credForm = { id: null, appName: null, quotaDaily: 0, quotaMonthly: 0, ipWhitelist: null, status: 'ACTIVE', expireTime: null, remark: null }
      this.credOpen = true
      this.$nextTick(() => this.$refs.credForm && this.$refs.credForm.clearValidate())
    },
    handleCredEdit(row) {
      getCredential(row.id).then(res => {
        this.credForm = { ...(res.data || {}) }
        this.credOpen = true
        this.$nextTick(() => this.$refs.credForm && this.$refs.credForm.clearValidate())
      })
    },
    submitCredForm() {
      this.$refs.credForm.validate(valid => {
        if (!valid) return
        if (this.credForm.id) {
          updateCredential(this.credForm).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.credOpen = false
            this.getCredList()
          })
        } else {
          addCredential(this.credForm).then(res => {
            this.credOpen = false
            this.getCredList()
            const data = res.data || {}
            this.showSecret('凭证创建成功', 'AppSecret', data.appKey, data.appSecret)
          })
        }
      })
    },
    toggleCredStatus(row, val) {
      const status = val ? 'ACTIVE' : 'DISABLED'
      changeStatus(row.id, status).then(() => {
        this.$modal.msgSuccess(val ? '已启用' : '已停用')
        this.getCredList()
      }).catch(() => { this.getCredList() })
    },
    handleResetSecret(row) {
      this.$modal.confirm('确认重置凭证「' + row.appName + '」的 AppSecret？原密钥将立即失效。').then(() => {
        return resetSecret(row.id)
      }).then(res => {
        this.getCredList()
        this.showSecret('AppSecret 已重置', 'AppSecret', row.appKey, res.data)
      }).catch(() => {})
    },
    handleCredDelete(row) {
      this.$modal.confirm('确认删除凭证「' + row.appName + '」？其关联 Webhook 将一并失效。').then(() => {
        return delCredential(row.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.getCredList()
      }).catch(() => {})
    },
    // ---------- 调用日志 ----------
    getCallLogList() {
      this.logLoading = true
      listCallLog(this.logQuery).then(res => {
        this.logList = res.rows || []
        this.logTotal = res.total || 0
        this.logLoading = false
      }).catch(() => { this.logLoading = false })
    },
    handleLogQuery() {
      this.logQuery.pageNum = 1
      this.getCallLogList()
    },
    resetLogQuery() {
      this.logQuery = { pageNum: 1, pageSize: 10, appKey: null }
      this.getCallLogList()
    },
    // ---------- Webhook ----------
    openWebhookTab() {
      this.loadCredOptions()
      if (!this.hookLoaded) { this.getHookList(); this.hookLoaded = true }
    },
    loadCredOptions() {
      listCredential({ pageNum: 1, pageSize: 200 }).then(res => {
        this.credOptions = res.rows || []
      })
    },
    getHookList() {
      this.hookLoading = true
      listWebhook(this.hookQuery).then(res => {
        this.hookList = res.rows || []
        this.hookTotal = res.total || 0
        this.hookLoading = false
      }).catch(() => { this.hookLoading = false })
    },
    handleHookQuery() {
      this.hookQuery.pageNum = 1
      this.getHookList()
    },
    resetHookQuery() {
      this.hookQuery = { pageNum: 1, pageSize: 10, credentialId: null, eventType: null }
      this.getHookList()
    },
    handleHookAdd() {
      if (this.credOptions.length === 0) { this.loadCredOptions() }
      this.hookForm = { id: null, credentialId: null, eventType: null, callbackUrl: null, isEnabled: 1, retryTimes: 3, remark: null }
      this.hookOpen = true
      this.$nextTick(() => this.$refs.hookForm && this.$refs.hookForm.clearValidate())
    },
    handleHookEdit(row) {
      getWebhook(row.id).then(res => {
        this.hookForm = { ...(res.data || {}) }
        this.hookOpen = true
        this.$nextTick(() => this.$refs.hookForm && this.$refs.hookForm.clearValidate())
      })
    },
    submitHookForm() {
      this.$refs.hookForm.validate(valid => {
        if (!valid) return
        if (this.hookForm.id) {
          updateWebhook(this.hookForm).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.hookOpen = false
            this.getHookList()
          })
        } else {
          addWebhook(this.hookForm).then(res => {
            this.hookOpen = false
            this.getHookList()
            const data = res.data || {}
            this.showSecret('Webhook 创建成功', 'Secret', null, data.secret)
          })
        }
      })
    },
    handleHookResetSecret(row) {
      this.$modal.confirm('确认重置该 Webhook 的签名密钥？原密钥将立即失效。').then(() => {
        return resetWebhookSecret(row.id)
      }).then(res => {
        this.getHookList()
        this.showSecret('Webhook Secret 已重置', 'Secret', null, res.data)
      }).catch(() => {})
    },
    handleHookDelete(row) {
      this.$modal.confirm('确认删除该 Webhook 回调配置？').then(() => {
        return delWebhook(row.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.getHookList()
      }).catch(() => {})
    },
    // ---------- 密钥明文展示 ----------
    showSecret(title, label, appKey, secret) {
      this.secretTitle = title
      this.secretLabel = label
      this.secretAppKey = appKey || null
      this.secretValue = secret || ''
      this.secretOpen = true
    }
  }
}
</script>

<style scoped>
.form-tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}
.log-detail {
  padding: 0 20px;
}
.log-detail .el-form-item {
  margin-bottom: 4px;
}
</style>
