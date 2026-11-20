<template>
  <div class="app-container">
    <el-alert title="拉新奖励配置（老板专属）" type="info"
      description="开启/关闭拉新奖励、设置奖励形式（现金/抵扣券/积分）、触发条件（新客绑定/新客首单）、奖励上限。配置保存后奖励结算按新规则执行。"
      show-icon :closable="false" class="mb16" />

    <div v-loading="loading">
      <!-- ===== 全局开关 ===== -->
      <el-card shadow="hover" class="mb16">
        <div slot="header" class="card-header">
          <span>全局开关</span>
          <el-switch v-model="setting.enabled" active-text="启用" inactive-text="关闭"
            @change="handleGlobalSwitch" />
        </div>
        <el-alert v-if="!setting.enabled" title="拉新奖励已关闭，员工和顾客推广将不产生奖励" type="warning"
          show-icon :closable="false" />
        <el-alert v-else title="拉新奖励已启用，请分别配置员工和顾客奖励规则" type="success"
          show-icon :closable="false" />
      </el-card>

      <!-- ===== 员工奖励配置 ===== -->
      <el-card shadow="hover" class="mb16" v-if="setting.enabled">
        <div slot="header"><b>员工奖励配置</b>（推广人=员工/业务员）</div>
        <el-form :model="setting.staffConfig" label-width="130px" size="small" style="max-width:600px;">
          <el-form-item label="启用员工奖励">
            <el-switch v-model="setting.staffConfig.enabled" />
          </el-form-item>
          <template v-if="setting.staffConfig.enabled">
            <el-form-item label="奖励形式" required>
              <el-radio-group v-model="setting.staffConfig.rewardType">
                <el-radio label="cash">现金（线下发放）</el-radio>
                <el-radio label="points">积分</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item :label="setting.staffConfig.rewardType === 'points' ? '奖励积分' : '奖励金额(元)'" required>
              <el-input-number v-model="setting.staffConfig.rewardValue" :min="0.01" :precision="2"
                :max="99999" :step="1" placeholder="请输入金额" style="width:200px;" />
            </el-form-item>
            <el-form-item label="触发条件" required>
              <el-radio-group v-model="setting.staffConfig.triggerCondition">
                <el-radio label="NEW_CUSTOMER_BIND">新客绑定（绑定即奖励）</el-radio>
                <el-radio label="NEW_CUSTOMER_FIRST_ORDER">新客首单（首次消费才奖励）</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="奖励上限">
              <el-select v-model="setting.staffConfig.rewardLimitType" style="width:150px;">
                <el-option label="不限" value="NONE" />
                <el-option label="每日" value="DAILY" />
                <el-option label="每月" value="MONTHLY" />
              </el-select>
              <el-input-number v-if="setting.staffConfig.rewardLimitType !== 'NONE'"
                v-model="setting.staffConfig.rewardLimitValue" :min="1" :max="9999"
                placeholder="次数" style="width:150px;margin-left:12px;" />
              <span v-if="setting.staffConfig.rewardLimitType !== 'NONE'" style="margin-left:8px;color:#909399;">次</span>
            </el-form-item>
          </template>
        </el-form>
      </el-card>

      <!-- ===== 顾客奖励配置 ===== -->
      <el-card shadow="hover" class="mb16" v-if="setting.enabled">
        <div slot="header"><b>顾客奖励配置</b>（推广人=顾客）</div>
        <el-form :model="setting.customerConfig" label-width="130px" size="small" style="max-width:600px;">
          <el-form-item label="启用顾客奖励">
            <el-switch v-model="setting.customerConfig.enabled" />
          </el-form-item>
          <template v-if="setting.customerConfig.enabled">
            <el-form-item label="奖励形式" required>
              <el-radio-group v-model="setting.customerConfig.rewardType">
                <el-radio label="coupon">抵扣券</el-radio>
                <el-radio label="points">积分</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="setting.customerConfig.rewardType === 'points'" label="奖励积分" required>
              <el-input-number v-model="setting.customerConfig.rewardValue" :min="0.01" :precision="2"
                :max="99999" :step="1" placeholder="请输入积分数" style="width:200px;" />
            </el-form-item>
            <el-form-item v-if="setting.customerConfig.rewardType === 'coupon'" label="关联优惠券" required>
              <el-input-number v-model="setting.customerConfig.couponId" :min="1" :max="999999"
                placeholder="优惠券ID" style="width:200px;" />
              <span style="margin-left:8px;color:#909399;font-size:12px;">请填写已创建的优惠券ID</span>
            </el-form-item>
            <el-form-item label="触发条件" required>
              <el-radio-group v-model="setting.customerConfig.triggerCondition">
                <el-radio label="NEW_CUSTOMER_BIND">新客绑定（绑定即奖励）</el-radio>
                <el-radio label="NEW_CUSTOMER_FIRST_ORDER">新客首单（首次消费才奖励）</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="奖励上限">
              <el-select v-model="setting.customerConfig.rewardLimitType" style="width:150px;">
                <el-option label="不限" value="NONE" />
                <el-option label="每日" value="DAILY" />
                <el-option label="每月" value="MONTHLY" />
              </el-select>
              <el-input-number v-if="setting.customerConfig.rewardLimitType !== 'NONE'"
                v-model="setting.customerConfig.rewardLimitValue" :min="1" :max="9999"
                placeholder="次数" style="width:150px;margin-left:12px;" />
              <span v-if="setting.customerConfig.rewardLimitType !== 'NONE'" style="margin-left:8px;color:#909399;">次</span>
            </el-form-item>
          </template>
        </el-form>
      </el-card>

      <!-- ===== 保存按钮 ===== -->
      <div class="footer-btn" v-if="setting.enabled">
        <el-button type="primary" @click="handleSave" :loading="saving">保存配置</el-button>
        <el-button @click="loadConfig">重置</el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { getRewardConfig, saveRewardConfig } from '@/api/micro/rewardConfig'

const defaultItem = () => ({
  enabled: false,
  rewardType: 'cash',
  rewardValue: 10,
  couponId: null,
  triggerCondition: 'NEW_CUSTOMER_FIRST_ORDER',
  rewardLimitType: 'NONE',
  rewardLimitValue: null
})

export default {
  name: 'RewardConfig',
  data() {
    return {
      loading: true,
      saving: false,
      setting: {
        enabled: false,
        staffConfig: defaultItem(),
        customerConfig: { ...defaultItem(), rewardType: 'coupon' }
      }
    }
  },
  created() { this.loadConfig() },
  methods: {
    loadConfig() {
      this.loading = true
      getRewardConfig().then(res => {
        const data = res.data || {}
        this.setting.enabled = !!data.enabled
        if (data.staffConfig) {
          Object.assign(this.setting.staffConfig, data.staffConfig)
        }
        if (data.customerConfig) {
          Object.assign(this.setting.customerConfig, data.customerConfig)
        }
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleGlobalSwitch(val) {
      if (!val) {
        this.$modal.confirm('关闭后所有奖励规则将停用，确定关闭？').then(() => {
          this.setting.enabled = false
        }).catch(() => {
          this.setting.enabled = true
        })
      }
    },
    handleSave() {
      // ★前端校验
      if (this.setting.enabled) {
        if (this.setting.staffConfig.enabled) {
          if (!this.setting.staffConfig.rewardType) {
            this.$modal.msgWarning('请选择员工奖励形式'); return
          }
          if (!this.setting.staffConfig.rewardValue || this.setting.staffConfig.rewardValue <= 0) {
            this.$modal.msgWarning('员工奖励金额必须大于0'); return
          }
          if (!this.setting.staffConfig.triggerCondition) {
            this.$modal.msgWarning('请选择员工触发条件'); return
          }
        }
        if (this.setting.customerConfig.enabled) {
          if (!this.setting.customerConfig.rewardType) {
            this.$modal.msgWarning('请选择顾客奖励形式'); return
          }
          if (this.setting.customerConfig.rewardType === 'coupon' && !this.setting.customerConfig.couponId) {
            this.$modal.msgWarning('请填写关联优惠券ID'); return
          }
          if (!this.setting.customerConfig.triggerCondition) {
            this.$modal.msgWarning('请选择顾客触发条件'); return
          }
        }
      }
      this.saving = true
      saveRewardConfig(this.setting).then(() => {
        this.$modal.msgSuccess('配置已保存')
        this.saving = false
      }).catch(() => { this.saving = false })
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.footer-btn { text-align: center; padding: 16px 0; }
</style>
