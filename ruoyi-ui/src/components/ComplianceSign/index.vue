<template>
  <el-dialog
    title="合规协议确认（T25.5）"
    :visible.sync="visible"
    width="760px"
    top="6vh"
    append-to-body
    :show-close="false"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    custom-class="compliance-sign-dialog">
    <el-alert
      title="平台已发布新版《合规协议》，依据 T25.5 租户管理员须阅读并签署后方可继续使用系统。"
      type="warning" show-icon :closable="false" class="mb16" />

    <el-descriptions :column="2" border size="small" class="mb16">
      <el-descriptions-item label="协议版本">
        <el-tag size="small" type="primary">{{ info.currentVersion }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="签署状态">
        <el-tag size="small" type="danger">未签署</el-tag>
      </el-descriptions-item>
    </el-descriptions>

    <div class="agreement-frame">
      <iframe v-if="frameUrl" :src="frameUrl" frameborder="0" title="合规协议内容" />
      <el-empty v-else description="协议内容未上传，请联系平台管理员" :image-size="80" />
    </div>

    <div class="mt16">
      <el-checkbox v-model="agreed">我已阅读并同意上述《合规协议》全部条款</el-checkbox>
      <el-link v-if="frameUrl" type="primary" :href="frameUrl" target="_blank" class="ml16">新窗口打开</el-link>
    </div>

    <div slot="footer">
      <el-button type="primary" :loading="signing" :disabled="!agreed" @click="handleSign">确认签署</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getAgreementCurrent, signAgreement } from '@/api/micro/supervision'
import { mapGetters } from 'vuex'

/** 会话内已确认标记：needSign=false 或签署成功后写入，避免重复请求；未签署时不写入，刷新页面仍强制弹窗 */
const CHECKED_FLAG = 'complianceSignChecked'

export default {
  name: 'ComplianceSign',
  data() {
    return {
      visible: false,
      signing: false,
      agreed: false,
      info: {}
    }
  },
  computed: {
    ...mapGetters(['roles']),
    /** 协议正文 URL：OSS 绝对地址直接用，相对路径补当前域名前缀 */
    frameUrl() {
      const url = this.info.agreementContentUrl
      if (!url) {
        return ''
      }
      return /^https?:\/\//.test(url) ? url : window.location.origin + url
    }
  },
  mounted() {
    this.check()
  },
  methods: {
    /** T25.5 登录守卫：仅租户管理员（platform_admin）强制签署；接口异常不阻断登录 */
    check() {
      if (!this.roles || this.roles.indexOf('platform_admin') === -1) {
        return
      }
      if (sessionStorage.getItem(CHECKED_FLAG)) {
        return
      }
      getAgreementCurrent().then(res => {
        const data = res.data || {}
        if (data.needSign && data.currentVersion) {
          this.info = data
          this.agreed = false
          this.visible = true
        } else {
          sessionStorage.setItem(CHECKED_FLAG, '1')
        }
      }).catch(() => {})
    },
    handleSign() {
      if (!this.agreed) {
        this.$modal.msgWarning('请先阅读并勾选同意协议')
        return
      }
      this.signing = true
      signAgreement({ agreementVersion: this.info.currentVersion }).then(() => {
        this.signing = false
        this.visible = false
        sessionStorage.setItem(CHECKED_FLAG, '1')
        this.$modal.msgSuccess('合规协议签署成功')
      }).catch(() => {
        this.signing = false
      })
    }
  }
}
</script>

<style scoped>
.mb16 {
  margin-bottom: 16px;
}
.mt16 {
  margin-top: 16px;
}
.ml16 {
  margin-left: 16px;
}
.agreement-frame {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}
.agreement-frame iframe {
  width: 100%;
  height: 380px;
  background: #fff;
  display: block;
}
</style>
