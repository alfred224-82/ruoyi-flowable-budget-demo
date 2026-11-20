<template>
  <div class="app-container">
    <el-alert title="预约策略配置（租户管理员）" type="info"
      description="配置顾客自助预约开关 / 提前预约天数 / 取消政策 / 自动确认 / 每日上限。" show-icon :closable="false" class="mb16" />
    <el-alert :title="`当前行业：${industryLabel}`" :type="isProjectLocked ? 'warning' : 'info'"
      :description="isProjectLocked ? '旅游 / 按摩 / 足浴行业项目为必选资源，下方「项目」两项开关已锁定为开启 + 必填，不可修改。' : '桌游等行业可自由搭配三类资源的开放与必填。'"
      show-icon :closable="false" class="mb16" />

    <el-card v-loading="loading" shadow="hover">
      <el-form ref="form" :model="form" label-width="160px" size="small" style="max-width:600px;">
        <el-form-item label="顾客自助预约">
          <el-switch v-model="form.customerSelfBooking" :active-value="1" :inactive-value="0" />
          <span style="margin-left:12px;color:#909399;font-size:12px;">开启后顾客可通过小程序自行预约</span>
        </el-form-item>
        <el-form-item label="提前预约天数">
          <el-input-number v-model="form.advanceBookingDays" :min="0" :max="365" />
          <span style="margin-left:12px;color:#909399;font-size:12px;">0 = 当天可约</span>
        </el-form-item>
        <el-form-item label="取消政策">
          <el-select v-model="form.cancelPolicy" style="width:200px;">
            <el-option label="免费取消" value="FREE" />
            <el-option label="收费取消" value="FEE" />
            <el-option label="禁止取消" value="FORBID" />
          </el-select>
        </el-form-item>
        <el-form-item label="免费取消截止（小时）" v-if="form.cancelPolicy !== 'FORBID'">
          <el-input-number v-model="form.cancelFreeHours" :min="0" :max="168" />
          <span style="margin-left:12px;color:#909399;font-size:12px;">距预约时间前 N 小时可免费取消</span>
        </el-form-item>
        <el-form-item label="爽约次数阈值">
          <el-input-number v-model="form.noShowThreshold" :min="1" :max="20" />
          <span style="margin-left:12px;color:#909399;font-size:12px;">达到后自动限制顾客预约权限</span>
        </el-form-item>
        <el-form-item label="自动确认预约">
          <el-switch v-model="form.autoConfirm" :active-value="1" :inactive-value="0" />
          <span style="margin-left:12px;color:#909399;font-size:12px;">开启后顾客预约自动确认，无需老板手动确认</span>
        </el-form-item>
        <el-form-item label="每日最大预约数">
          <el-input-number v-model="form.maxDailyBookings" :min="1" :max="999" />
          <span style="margin-left:12px;color:#909399;font-size:12px;">留空 = 不限</span>
        </el-form-item>

        <el-divider content-position="left">三类资源控制</el-divider>
        <el-form-item label="项目 · 对客户开放">
          <el-switch v-model="form.projectBookingEnabled" :active-value="1" :inactive-value="0" :disabled="isProjectLocked" />
          <el-tooltip v-if="isProjectLocked" content="该行业项目必须开启且必填" placement="top">
            <i class="el-icon-question" style="margin-left:8px;color:#e6a23c;"></i>
          </el-tooltip>
          <span v-else style="margin-left:12px;color:#909399;font-size:12px;">关闭后顾客端不展示项目预约入口</span>
        </el-form-item>
        <el-form-item label="项目 · 是否必填">
          <el-switch v-model="form.projectBookingRequired" :active-value="1" :inactive-value="0" :disabled="isProjectLocked" />
          <span style="margin-left:12px;color:#909399;font-size:12px;">开启后顾客提交预约必须选择项目</span>
        </el-form-item>
        <el-form-item label="场地 · 对客户开放">
          <el-switch v-model="form.roomBookingEnabled" :active-value="1" :inactive-value="0" />
          <span style="margin-left:12px;color:#909399;font-size:12px;">关闭后顾客端不展示场地预约入口</span>
        </el-form-item>
        <el-form-item label="场地 · 是否必填">
          <el-switch v-model="form.roomBookingRequired" :active-value="1" :inactive-value="0" />
          <span style="margin-left:12px;color:#909399;font-size:12px;">开启后顾客提交预约必须选择场地</span>
        </el-form-item>
        <el-form-item label="员工 · 对客户开放">
          <el-switch v-model="form.staffBookingEnabled" :active-value="1" :inactive-value="0" />
          <span style="margin-left:12px;color:#909399;font-size:12px;">关闭后顾客端不展示员工预约入口</span>
        </el-form-item>
        <el-form-item label="员工 · 是否必填">
          <el-switch v-model="form.staffBookingRequired" :active-value="1" :inactive-value="0" />
          <span style="margin-left:12px;color:#909399;font-size:12px;">开启后顾客提交预约必须选择员工</span>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" :maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave">保存策略</el-button>
          <el-button @click="loadPolicy">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { getBookingPolicy, saveBookingPolicy } from '@/api/micro/bookingPolicy'

export default {
  name: 'BookingPolicy',
  data() {
    return {
      loading: true,
      // REQ-01 当前租户行业编码（customerView/queryPolicy 回传）
      industryCode: null,
      form: {
        customerSelfBooking: 1,
        advanceBookingDays: 7,
        cancelPolicy: 'FREE',
        cancelFreeHours: 2,
        noShowThreshold: 3,
        autoConfirm: 0,
        maxDailyBookings: null,
        projectBookingEnabled: 1,
        projectBookingRequired: 0,
        roomBookingEnabled: 1,
        roomBookingRequired: 0,
        staffBookingEnabled: 1,
        staffBookingRequired: 0,
        remark: ''
      }
    }
  },
  computed: {
    // REQ-01 锁死行业（旅游/按摩/足浴）：项目开关置灰强制 1
    isProjectLocked() {
      return ['travel', 'massage', 'foot_bath'].includes(this.industryCode)
    },
    industryLabel() {
      const map = {
        board_game: '桌游',
        travel: '旅游',
        massage: '按摩',
        foot_bath: '足浴'
      }
      return map[this.industryCode] || (this.industryCode ? this.industryCode : '未设置')
    }
  },
  created() { this.loadPolicy() },
  methods: {
    loadPolicy() {
      this.loading = true
      getBookingPolicy().then(res => {
        const data = res.data || {}
        this.industryCode = data.industryCode || null
        Object.keys(this.form).forEach(k => {
          if (data[k] !== undefined && data[k] !== null) {
            this.form[k] = data[k]
          }
        })
        // 锁死行业：项目两项强制回写为 1（与后端一致）
        if (this.isProjectLocked) {
          this.form.projectBookingEnabled = 1
          this.form.projectBookingRequired = 1
        }
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleSave() {
      saveBookingPolicy(this.form).then(() => {
        this.$modal.msgSuccess('预约策略已保存')
      })
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
</style>
