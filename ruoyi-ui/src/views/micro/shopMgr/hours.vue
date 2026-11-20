<template>
  <div class="app-container">
    <el-alert
      title="营业时间设置（T15.24，perms micro:shop:hours）"
      type="info"
      description="营业时间以文案形式存储在 tenant_biz_config.module_overrides.businessHours，展示于顾客端「店铺信息」区块。仅覆盖该键，收款说明与通知设置不受影响。"
      show-icon
      :closable="false"
      class="mb8"
    />
    <el-alert
      v-if="!configAvailable"
      title="无法读取当前营业时间"
      type="warning"
      description="读取配置需要 micro:shop:edit 权限且需以门店（租户）身份登录；当前身份仅能保存，无法回显既有内容。请谨慎覆盖，或改用「店铺信息设置」页维护。"
      show-icon
      :closable="false"
      class="mb8"
    />

    <el-card shadow="never" v-loading="loading">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="快捷模板">
          <el-radio-group v-model="template" size="small" @change="applyTemplate">
            <el-radio-button v-for="t in templates" :key="t.label" :label="t.label">{{ t.label }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="营业时间" prop="businessHours">
          <el-input
            v-model="form.businessHours"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="如：周一至周日 09:00-21:00"
          />
          <div style="color:#909399;font-size:12px;line-height:1.6;">
            可直接编辑文案；分段营业建议写成「周一至周五 09:00-12:00、14:00-21:00；周六至周日 09:00-22:00」。
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="submitForm" v-hasPermi="['micro:shop:hours']">保存营业时间</el-button>
          <el-button @click="getConfig">重新加载</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { getShopConfig, saveShopHours } from '@/api/micro/shop'

export default {
  name: 'MicroShopHours',
  data() {
    return {
      loading: false,
      saving: false,
      configAvailable: true,
      template: '',
      templates: [
        { label: '全天营业', text: '周一至周日 00:00-24:00' },
        { label: '白天营业', text: '周一至周日 09:00-18:00' },
        { label: '晚间营业', text: '周一至周日 14:00-次日02:00' },
        { label: '工作日+周末', text: '周一至周五 09:00-21:00；周六至周日 09:00-22:00' }
      ],
      form: { businessHours: null },
      rules: {
        businessHours: [{ required: true, message: '营业时间不能为空', trigger: 'blur' }]
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
        this.form.businessHours = data.businessHours
        this.configAvailable = true
        this.loading = false
        this.matchTemplate()
      }).catch(() => {
        // 读取需 micro:shop:edit，无权限或无租户上下文时降级：保留可保存能力
        this.configAvailable = false
        this.loading = false
      })
    },
    matchTemplate() {
      const hit = this.templates.filter(t => t.text === this.form.businessHours)
      this.template = hit.length ? hit[0].label : ''
    },
    applyTemplate(label) {
      const hit = this.templates.filter(t => t.label === label)
      if (hit.length) {
        this.form.businessHours = hit[0].text
      }
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) return
        this.saving = true
        saveShopHours(this.form.businessHours).then(() => {
          this.saving = false
          this.$modal.msgSuccess('保存成功')
          this.getConfig()
        }).catch(() => { this.saving = false })
      })
    }
  }
}
</script>
