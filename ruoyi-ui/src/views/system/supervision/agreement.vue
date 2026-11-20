<template>
  <div class="app-container">
    <el-alert
      title="合规协议管理（T25.5，platform_admin 管理协议版本号 + OSS 内容地址）"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom:12px;"
    />

    <el-card shadow="hover">
      <template slot="header">
        <div style="display:flex;align-items:center;justify-content:space-between;">
          <span style="font-size:16px;font-weight:bold;">合规协议配置</span>
          <el-button type="primary" icon="el-icon-edit" size="mini" @click="handleEdit"
            v-hasPermi="['system:supervision:agreement']">编辑配置</el-button>
        </div>
      </template>

      <el-descriptions :column="1" border v-loading="loading">
        <el-descriptions-item label="协议版本号">
          <el-tag type="success" size="small">{{ config.version || '未配置' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="协议内容 URL">
          <el-link v-if="config.contentUrl" :href="config.contentUrl" target="_blank" type="primary">
            {{ config.contentUrl }}
          </el-link>
          <span v-else style="color:#999;">未配置</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog title="编辑合规协议配置" :visible.sync="editOpen" width="550px" append-to-body>
      <el-form ref="editForm" :model="editForm" :rules="editRules" label-width="110px">
        <el-form-item label="协议版本号" prop="version">
          <el-input v-model="editForm.version" placeholder="例如 v1.1" />
        </el-form-item>
        <el-form-item label="协议内容URL" prop="contentUrl">
          <el-input v-model="editForm.contentUrl" type="textarea" :rows="3" placeholder="OSS 协议文件地址" />
          <div style="color:#909399;font-size:12px;margin-top:4px;">
            请先通过 OSS 上传协议 HTML 文件，再将 URL 填入此处
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="submitEdit" :loading="submitLoading">保 存</el-button>
        <el-button @click="editOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'Agreement',
  data() {
    return {
      loading: false,
      config: {},
      editOpen: false,
      editForm: { version: '', contentUrl: '' },
      editRules: {
        version: [{ required: true, message: '请输入协议版本号', trigger: 'blur' }],
        contentUrl: [{ required: true, message: '请输入协议内容URL', trigger: 'blur' }]
      },
      submitLoading: false
    }
  },
  created() {
    this.getConfig()
  },
  methods: {
    /** 获取协议配置 */
    getConfig() {
      this.loading = true
      request({ url: '/system/supervision/agreement/config', method: 'get' }).then(res => {
        this.config = res.data || {}
      }).finally(() => {
        this.loading = false
      })
    },
    /** 打开编辑 */
    handleEdit() {
      this.editForm.version = this.config.version || ''
      this.editForm.contentUrl = this.config.contentUrl || ''
      this.editOpen = true
    },
    /** 提交编辑 */
    submitEdit() {
      this.$refs.editForm.validate(valid => {
        if (!valid) return
        this.$modal.confirm('确认更新合规协议配置？').then(() => {
          this.submitLoading = true
          request({
            url: '/system/supervision/agreement/config',
            method: 'put',
            data: { version: this.editForm.version, contentUrl: this.editForm.contentUrl }
          }).then(() => {
            this.$modal.msgSuccess('配置更新成功')
            this.editOpen = false
            this.getConfig()
          }).finally(() => {
            this.submitLoading = false
          })
        }).catch(() => {})
      })
    }
  }
}
</script>
