<template>
  <div class="app-container">
    <el-alert title="系统设置（租户管理员）" type="info"
      description="企业信息编辑（公司名称/行业/联系方式）/ 安全设置提示。" show-icon :closable="false" class="mb16" />

    <el-tabs v-model="activeTab">
      <!-- ===== 企业信息 ===== -->
      <el-tab-pane label="企业信息" name="company">
        <el-card v-loading="loading" shadow="hover" style="max-width:700px;">
          <el-form ref="form" :model="form" label-width="100px" size="small">
            <el-form-item label="公司名称" required>
              <el-input v-model="form.companyName" :maxlength="100" show-word-limit placeholder="请输入公司名称" />
            </el-form-item>
            <el-form-item label="行业">
              <el-input v-model="form.industry" :maxlength="50" placeholder="如：桌游/健身/餐饮" />
            </el-form-item>
            <el-form-item label="联系人">
              <el-input v-model="form.contactName" :maxlength="50" placeholder="联系人姓名" />
            </el-form-item>
            <el-form-item label="联系电话">
              <el-input v-model="form.contactPhone" :maxlength="20" placeholder="联系电话" />
            </el-form-item>
            <el-form-item label="城市">
              <el-input v-model="form.city" :maxlength="50" placeholder="所在城市" />
            </el-form-item>
            <el-form-item label="门店地址">
              <el-input v-model="form.address" :maxlength="200" placeholder="门店详细地址" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSave">保存</el-button>
              <el-button @click="loadSetting">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- ===== 安全设置 ===== -->
      <el-tab-pane label="安全设置" name="security">
        <el-card shadow="hover" style="max-width:700px;">
          <el-alert title="安全提示" type="warning" :closable="false" show-icon class="mb16">
            <template slot="title">
              <b>密码策略与登录限制</b>
            </template>
            <ul style="padding-left:16px;line-height:2;">
              <li>建议定期修改管理员密码，密码长度不少于 8 位</li>
              <li>密码应包含大小写字母、数字和特殊字符</li>
              <li>连续 5 次密码错误将锁定账号 30 分钟</li>
              <li>登录会话默认 24 小时过期，过期需重新登录</li>
              <li>如需调整安全策略，请联系平台管理员</li>
            </ul>
          </el-alert>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="密码最小长度">8 位</el-descriptions-item>
            <el-descriptions-item label="登录失败锁定">5 次后锁定 30 分钟</el-descriptions-item>
            <el-descriptions-item label="会话过期时间">24 小时</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { getSystemSetting, saveCompanySetting } from '@/api/micro/systemSetting'

export default {
  name: 'SystemSetting',
  data() {
    return {
      activeTab: 'company',
      loading: true,
      form: {
        companyName: '',
        industry: '',
        contactName: '',
        contactPhone: '',
        address: '',
        city: ''
      }
    }
  },
  created() { this.loadSetting() },
  methods: {
    loadSetting() {
      this.loading = true
      getSystemSetting().then(res => {
        const data = res.data || {}
        Object.keys(this.form).forEach(k => {
          if (data[k] != null) this.form[k] = data[k]
        })
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleSave() {
      if (!this.form.companyName || !this.form.companyName.trim()) {
        this.$modal.msgWarning('公司名称不能为空')
        return
      }
      saveCompanySetting(this.form).then(() => {
        this.$modal.msgSuccess('企业信息已保存')
      })
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
</style>
