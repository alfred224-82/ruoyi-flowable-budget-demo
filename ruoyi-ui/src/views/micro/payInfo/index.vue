<template>
  <div class="app-container">
    <el-alert
      title="★V1 零支付合规：本页只写「提示文案」，不绑定任何支付账户"
      type="warning"
      description="全系统无微信支付 / 转账 / 代付调用，资金不经平台。收款方式说明仅告知顾客「到店付 / 扫描老板收款码」，禁止填写商户号、支付密钥、结算账户或任何在线收款链接。"
      show-icon
      :closable="false"
      class="mb8"
    />
    <el-alert
      v-if="!configAvailable"
      title="无法读取当前收款方式说明"
      type="warning"
      description="读取配置需要 micro:shop:edit 权限且需以门店（租户）身份登录；当前身份仅能保存，无法回显既有内容，请谨慎覆盖。"
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
        <el-form-item label="收款方式说明" prop="payInfo">
          <el-input
            v-model="form.payInfo"
            type="textarea"
            :rows="6"
            maxlength="500"
            show-word-limit
            placeholder="如：服务完成后请向前台/老板付款，或扫描老板收款码支付。本小程序不提供在线支付。"
          />
          <div style="color:#909399;font-size:12px;">展示于顾客端「我的预约 → 查看收款方式」，由后端 micro:customer:payInfo 返回</div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="submitForm" v-hasPermi="['micro:shop:payInfo']">保存收款方式说明</el-button>
          <el-button @click="getConfig">重新加载</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { getShopConfig, saveShopPayInfo } from '@/api/micro/shop'

export default {
  name: 'MicroPayInfo',
  data() {
    return {
      loading: false,
      saving: false,
      configAvailable: true,
      template: '',
      templates: [
        { label: '到店付', text: '本店支持到店付款：服务完成后请向前台/老板付款。本小程序不提供在线支付。' },
        { label: '老板收款码', text: '服务完成后请扫描店内老板收款码支付。本小程序不代收任何款项，资金直达门店。' },
        { label: '会员卡/储值', text: '支持到店付款、老板收款码，以及本店会员卡/储值余额抵扣。本小程序不提供在线支付与充值。' }
      ],
      form: { payInfo: null },
      rules: {
        payInfo: [{ required: true, message: '收款方式说明不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getConfig()
  },
  methods: {
    getConfig() {
      this.loading = true
      getShopConfig().then(response => {
        const data = response.data || {}
        this.form.payInfo = data.payInfo
        this.configAvailable = true
        this.loading = false
        this.matchTemplate()
      }).catch(() => {
        this.configAvailable = false
        this.loading = false
      })
    },
    matchTemplate() {
      const hit = this.templates.filter(t => t.text === this.form.payInfo)
      this.template = hit.length ? hit[0].label : ''
    },
    applyTemplate(label) {
      const hit = this.templates.filter(t => t.label === label)
      if (hit.length) {
        this.form.payInfo = hit[0].text
      }
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) return
        this.saving = true
        saveShopPayInfo(this.form.payInfo).then(() => {
          this.saving = false
          this.$modal.msgSuccess('保存成功')
          this.getConfig()
        }).catch(() => { this.saving = false })
      })
    }
  }
}
</script>
