<template>
  <div class="app-container">
    <el-alert
      title="店铺管理（T15.24）：单店差异一律走 tenant_biz_config.module_overrides，不 fork 业态包"
      type="info"
      description="本页一次性维护营业时间、收款方式说明与通知方式设置；保存为「增量合并」，仅覆盖本次提交的字段，其余配置保留。也可用下方独立菜单单独维护。"
      show-icon
      :closable="false"
      class="mb8"
    />
    <el-alert
      v-if="!configAvailable"
      title="无法读取店铺配置：请以门店（租户）身份登录"
      type="error"
      description="平台超级管理员登录上下文无 tenantId，后端会拒绝读写店铺配置。请切换到具体门店账号后再维护本页。"
      show-icon
      :closable="false"
      class="mb8"
    />

    <el-card shadow="never" v-loading="loading">
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="营业时间" prop="businessHours">
          <el-input v-model="form.businessHours" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="如：周一至周日 09:00-21:00" />
          <div style="color:#909399;font-size:12px;">展示于顾客端「店铺信息」区块；单独维护入口见菜单「营业时间设置」</div>
        </el-form-item>
        <el-form-item label="收款方式说明" prop="payInfo">
          <el-input v-model="form.payInfo" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="如：服务完成后请向前台付款，或扫描老板收款码支付" />
          <div style="color:#e6a23c;font-size:12px;">
            ★V1 零支付合规：此处仅为「到店付 / 老板收款码」提示文案，禁止填写任何支付账户、商户号、密钥或在线收款链接
          </div>
        </el-form-item>
        <el-form-item label="通知方式设置" prop="notifyConfig">
          <el-input v-model="form.notifyConfig" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="文案或 JSON 串，如：结算提醒/预警经短信+小程序通知" />
          <div style="color:#909399;font-size:12px;">单独维护入口见菜单「通知方式设置」；★通知仅经营提醒，不含任何支付/扣款通知</div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" :disabled="!configAvailable" @click="submitForm" v-hasPermi="['micro:shop:edit']">保存店铺配置</el-button>
          <el-button :disabled="!configAvailable" @click="getConfig">重新加载</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { getShopConfig, saveShopConfig } from '@/api/micro/shop'

export default {
  name: 'MicroShop',
  data() {
    return {
      loading: false,
      saving: false,
      // 后端 requireTenantId() 在无租户上下文（超管）时抛异常，此处降级提示而非白屏
      configAvailable: true,
      form: { businessHours: null, payInfo: null, notifyConfig: null },
      rules: {}
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
        this.form = {
          businessHours: data.businessHours,
          payInfo: data.payInfo,
          notifyConfig: data.notifyConfig
        }
        this.configAvailable = true
        this.loading = false
      }).catch(() => {
        this.configAvailable = false
        this.loading = false
      })
    },
    /** 保存：后端 merge 仅覆盖非 null 字段，故三字段的空串会真实清空对应配置 */
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) return
        this.saving = true
        saveShopConfig({
          businessHours: this.form.businessHours || '',
          payInfo: this.form.payInfo || '',
          notifyConfig: this.form.notifyConfig || ''
        }).then(() => {
          this.saving = false
          this.$modal.msgSuccess('保存成功')
          this.getConfig()
        }).catch(() => { this.saving = false })
      })
    }
  }
}
</script>
