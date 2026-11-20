<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <!-- ① 通知方式设置（T15.24，perms micro:notify:config） -->
      <el-tab-pane label="通知方式设置" name="config">
        <el-alert
          title="通知方式设置存储在 tenant_biz_config.module_overrides.notifyConfig"
          type="info"
          description="可为纯文案，也可为 JSON 串（由小程序端自行解析）。保存仅覆盖 notifyConfig 键，营业时间与收款说明不受影响。★V1 零支付合规：通知仅限经营提醒（结算提醒 / 支出预警 / 每日简报），不含任何支付或扣款通知。"
          show-icon
          :closable="false"
          class="mb8"
        />
        <el-card shadow="never" v-loading="loading">
          <el-form ref="form" :model="form" :rules="rules" label-width="120px">
            <el-form-item label="快捷模板">
              <el-radio-group v-model="template" size="small" @change="applyTemplate">
                <el-radio-button v-for="t in templates" :key="t.label" :label="t.label">{{ t.label }}</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="通知方式" prop="notifyConfig">
              <el-input
                v-model="form.notifyConfig"
                type="textarea"
                :rows="8"
                maxlength="1000"
                show-word-limit
                placeholder="如：结算提醒 / 支出预警经短信 + 小程序通知推送给老板"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="submitForm" v-hasPermi="['micro:notify:config']">保存通知方式</el-button>
              <el-button @click="getConfig">重新加载</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- ② 消息提醒（T15.14 / T15.20，perms micro:notify:list） -->
      <el-tab-pane v-if="canList" name="list">
        <span slot="label">
          消息提醒
          <el-badge v-if="unread > 0" :value="unread" :max="99" class="unread-badge" />
        </span>
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="80px">
          <el-form-item label="消息类型" prop="messageType">
            <el-select v-model="queryParams.messageType" placeholder="请选择类型" clearable style="width:150px;">
              <el-option v-for="(label, val) in typeMap" :key="val" :label="label" :value="val" />
            </el-select>
          </el-form-item>
          <el-form-item label="阅读状态" prop="isRead">
            <el-select v-model="queryParams.isRead" placeholder="请选择状态" clearable style="width:130px;">
              <el-option label="未读" :value="0" />
              <el-option label="已读" :value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="el-icon-finished" size="mini" @click="handleReadAll">全部标记已读</el-button>
          </el-col>
        </el-row>

        <el-table v-loading="listLoading" :data="messageList">
          <el-table-column label="状态" align="center" width="70">
            <template slot-scope="scope">
              <el-tag v-if="scope.row.isRead !== 1" type="danger" size="mini">未读</el-tag>
              <el-tag v-else type="info" size="mini">已读</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="标题" align="left" prop="title" min-width="160" show-overflow-tooltip />
          <el-table-column label="内容" align="left" prop="content" min-width="240" show-overflow-tooltip />
          <el-table-column label="类型" align="center" prop="messageType" width="100">
            <template slot-scope="scope">
              <el-tag size="mini">{{ typeMap[scope.row.messageType] || scope.row.messageType || '—' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="业务类型" align="center" prop="bizType" width="140" show-overflow-tooltip />
          <el-table-column label="时间" align="center" prop="createTime" width="160">
            <template slot-scope="scope">
              <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="110">
            <template slot-scope="scope">
              <el-button
                v-if="scope.row.isRead !== 1"
                size="mini"
                type="text"
                icon="el-icon-check"
                @click="handleRead(scope.row)"
              >标记已读</el-button>
              <span v-else style="color:#c0c4cc;">—</span>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { getNotifyConfig, saveNotifyConfig, listNotify, getUnreadCount, readNotify, readAllNotify } from '@/api/micro/notify'
import { checkPermi } from '@/utils/permission'

export default {
  name: 'MicroNotify',
  data() {
    return {
      activeTab: 'config',
      loading: false,
      saving: false,
      template: '',
      templates: [
        { label: '短信+小程序', text: '结算提醒、支出预警、每日经营简报：短信 + 小程序服务通知双通道推送给老板。' },
        { label: '仅小程序', text: '结算提醒、支出预警、每日经营简报：仅通过小程序服务通知推送给老板。' },
        { label: 'JSON 结构', text: '{"channels":["sms","mini"],"events":["settle","alert","briefing"],"receivers":["boss"]}' }
      ],
      form: { notifyConfig: null },
      rules: {
        notifyConfig: [{ required: true, message: '通知方式设置不能为空', trigger: 'blur' }]
      },
      // 消息提醒
      canList: checkPermi(['micro:notify:list']),
      listLoading: false,
      messageList: [],
      total: 0,
      unread: 0,
      typeMap: { SYSTEM: '系统通知', APPROVAL: '审批提醒', REJECT: '驳回提醒' },
      queryParams: { pageNum: 1, pageSize: 10, messageType: null, isRead: null }
    }
  },
  created() {
    this.getConfig()
    if (this.canList) {
      this.getList()
      this.getUnread()
    }
  },
  methods: {
    getConfig() {
      this.loading = true
      getNotifyConfig().then(response => {
        this.form.notifyConfig = response.data
        this.loading = false
        this.matchTemplate()
      }).catch(() => { this.loading = false })
    },
    matchTemplate() {
      const hit = this.templates.filter(t => t.text === this.form.notifyConfig)
      this.template = hit.length ? hit[0].label : ''
    },
    applyTemplate(label) {
      const hit = this.templates.filter(t => t.label === label)
      if (hit.length) {
        this.form.notifyConfig = hit[0].text
      }
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) return
        this.saving = true
        saveNotifyConfig(this.form.notifyConfig).then(() => {
          this.saving = false
          this.$modal.msgSuccess('保存成功')
          this.getConfig()
        }).catch(() => { this.saving = false })
      })
    },
    getList() {
      this.listLoading = true
      listNotify(this.queryParams).then(response => {
        this.messageList = response.rows
        this.total = response.total
        this.listLoading = false
      }).catch(() => { this.listLoading = false })
    },
    getUnread() {
      getUnreadCount().then(response => {
        this.unread = response.data || 0
      }).catch(() => {})
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleRead(row) {
      readNotify(row.id).then(() => {
        this.$modal.msgSuccess('已标记为已读')
        this.getList()
        this.getUnread()
      })
    },
    handleReadAll() {
      this.$modal.confirm('确认将全部消息标记为已读吗？').then(() => readAllNotify()).then(() => {
        this.$modal.msgSuccess('已全部标记为已读')
        this.getList()
        this.getUnread()
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.unread-badge {
  margin-left: 4px;
  vertical-align: baseline;
}
</style>
