<template>
  <div class="app-container">
    <el-alert
      title="活动设置（M19 T19.1~T19.5）：老板端活动创建流程增强"
      type="info"
      description="选择项目/房间/员工 → 自动生成标题 → 上传背景图（★仅对当前活动生效，不跨活动复用）。"
      show-icon
      :closable="false"
      class="mb8"
    />

    <!-- 活动选择 -->
    <el-card class="mb8" shadow="never">
      <div slot="header"><span>选择活动</span></div>
      <el-form :inline="true" size="small">
        <el-form-item label="活动">
          <el-select v-model="activityId" placeholder="请选择要设置的活动" filterable style="width: 360px;" @change="loadSetting">
            <el-option v-for="item in activityList" :key="item.id" :label="item.title || '(无标题)'" :value="item.id">
              <span>{{ item.title || '(无标题)' }}</span>
              <el-tag size="mini" :type="item.status === 'PUBLISHED' ? 'success' : 'info'" style="margin-left:8px;">{{ statusMap[item.status] || item.status }}</el-tag>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 活动设置表单 -->
    <el-card v-if="activityId" shadow="never">
      <div slot="header"><span>活动设置</span></div>
      <el-form ref="settingForm" :model="form" label-width="120px" size="small">
        <!-- T19.2 项目选择器（单选） -->
        <el-form-item label="选择项目">
          <el-select v-model="form.selectedProjectId" placeholder="请选择项目（单选）" filterable clearable style="width: 100%;" @change="onSelectionChange">
            <el-option v-for="item in projectList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>

        <!-- T19.2 房间选择器（单/多选） -->
        <el-form-item label="选择房间">
          <el-select v-model="form.selectedRoomIdList" placeholder="请选择房间" filterable clearable :multiple="roomMultiple" style="width: 100%;" @change="onSelectionChange">
            <el-option v-for="item in roomList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <div style="color:#909399;font-size:12px;">{{ roomMultiple ? '多选模式（租户管理员可配置为单选）' : '单选模式' }}</div>
        </el-form-item>

        <!-- T19.2 员工选择器（单/多选） -->
        <el-form-item label="选择员工">
          <el-select v-model="form.selectedStaffIdList" placeholder="请选择员工" filterable clearable :multiple="staffMultiple" style="width: 100%;" @change="onSelectionChange">
            <el-option v-for="item in staffList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <div style="color:#909399;font-size:12px;">{{ staffMultiple ? '多选模式（租户管理员可配置为单选）' : '单选模式' }}</div>
        </el-form-item>

        <!-- T19.3 标题预览/手动修改 -->
        <el-form-item label="活动标题">
          <el-input v-model="form.manualTitle" placeholder="留空则自动生成，也可手动输入覆盖" maxlength="200" show-word-limit>
            <el-button slot="append" @click="handlePreviewTitle" :loading="previewLoading">自动预览</el-button>
          </el-input>
          <div v-if="autoTitlePreview" style="color:#67C23A;font-size:12px;margin-top:4px;">自动生成预览：{{ autoTitlePreview }}</div>
        </el-form-item>

        <!-- T19.4 背景图上传（★仅对当前活动生效） -->
        <el-form-item label="背景图">
          <image-upload v-model="form.bgImageUrl" :limit="1" />
          <div style="color:#E6A23C;font-size:12px;">★背景图仅对当前活动生效，不跨活动复用。每次创建活动需独立上传。</div>
        </el-form-item>

        <!-- ★M35 T35.4 定价页自动算账（其他成本 + 实时试算面板） -->
        <el-form-item label="其他成本(元)">
          <el-input-number v-model="form.otherCost" :min="0" :precision="2" :step="100" controls-position="right" style="width:240px;" @change="calcPricingDebounced" />
          <div style="color:#909399;font-size:12px;">场地/物料等成本；员工成本走分成规则快照自动计算，不在此列。</div>
        </el-form-item>
        <el-form-item label="定价试算">
          <el-card shadow="never" class="pricing-card" v-loading="pricingLoading">
            <div v-if="pricing" class="pricing-grid">
              <div class="pricing-item">
                <span class="pricing-label">预计收入</span>
                <span class="pricing-value" style="color:#409eff;">¥ {{ fmtMoney(pricing.expectedRevenue) }}</span>
                <span class="pricing-sub">{{ fmtMoney(pricing.unitPrice) }} × {{ pricing.expectedHeadcount || 0 }} 人</span>
              </div>
              <div class="pricing-item">
                <span class="pricing-label">员工成本</span>
                <span class="pricing-value" style="color:#e6a23c;">¥ {{ fmtMoney(pricing.employeeCost) }}</span>
                <span class="pricing-sub">按分成规则快照求和</span>
              </div>
              <div class="pricing-item">
                <span class="pricing-label">其他成本</span>
                <span class="pricing-value" style="color:#e6a23c;">¥ {{ fmtMoney(pricing.otherCost) }}</span>
                <span class="pricing-sub">场地/物料等</span>
              </div>
              <div class="pricing-item">
                <span class="pricing-label">预估利润</span>
                <span class="pricing-value" :style="{ color: Number(pricing.estimatedProfit) >= 0 ? '#67c23a' : '#f56c6c' }">¥ {{ fmtMoney(pricing.estimatedProfit) }}</span>
                <span class="pricing-sub">预计收入 - 成本合计</span>
              </div>
              <div class="pricing-item">
                <span class="pricing-label">保本人数</span>
                <span class="pricing-value" style="color:#303133;">{{ pricing.breakevenHeadcount == null ? '无法保本' : pricing.breakevenHeadcount + ' 人' }}</span>
                <span class="pricing-sub">ceil(成本合计 ÷ 单位边际贡献 {{ fmtMoney(pricing.unitContribution) }})</span>
              </div>
            </div>
            <div v-else class="pricing-empty">选择员工并填写售价/预计人数后自动试算</div>
            <!-- 员工成本明细 -->
            <div v-if="pricing && pricing.staffCostItems && pricing.staffCostItems.length" class="pricing-detail">
              <div class="pricing-detail-title">本场员工成本明细（按当前分成规则试算，保存后生成快照）</div>
              <el-table :data="pricing.staffCostItems" size="mini" border>
                <el-table-column label="员工" prop="staffName" width="120" />
                <el-table-column label="分成规则" prop="ruleDesc" min-width="160" />
                <el-table-column label="试算金额" align="right" width="120">
                  <template slot-scope="scope">¥ {{ fmtMoney(scope.row.amount) }}</template>
                </el-table-column>
              </el-table>
            </div>
            <div v-if="pricing && pricing.footnote" class="pricing-footnote">{{ pricing.footnote }}</div>
          </el-card>
        </el-form-item>

        <!-- 语音上传（仅租户管理员启用语音时显示） -->
        <el-form-item label="活动介绍语音" v-if="voiceEnabled">
          <el-upload
            :action="uploadUrl"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleVoiceSuccess"
            :before-upload="beforeVoiceUpload"
            accept="audio/*"
          >
            <el-button size="small" type="primary" icon="el-icon-upload2">上传语音</el-button>
          </el-upload>
          <div v-if="form.voiceUrl" style="margin-top:8px;">
            <audio :src="form.voiceUrl" controls preload="none" style="width:100%;max-width:480px;height:36px;"></audio>
            <el-button type="text" size="mini" @click="form.voiceUrl = null" style="margin-left:8px;color:#F56C6C;">移除</el-button>
          </div>
          <div style="color:#909399;font-size:12px;">{{ maxVoiceDuration > 0 ? '最长 ' + Math.ceil(maxVoiceDuration / 60) + ' 分钟' : '不限时长' }}，单向播放，不做语音互动</div>
        </el-form-item>

        <!-- 保存按钮 -->
        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saveLoading" v-hasPermi="['micro:activity:setting:edit']">保存设置</el-button>
          <el-button @click="loadSetting(activityId)">重置</el-button>
          <!-- ★M35 T35.6 本场结算入口（跳转 M36 活动结算单页面，M36 未交付前提示敬请期待） -->
          <el-button type="success" plain icon="el-icon-money" @click="goSettlement" v-hasPermi="['micro:commissionRule:list']">本场结算</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { listPromo, checkVoiceEnabled } from '@/api/micro/promo'
import {
  listActivityProjects,
  listActivityRooms,
  listActivityStaff,
  getActivitySetting,
  saveActivitySetting,
  previewAutoTitle
} from '@/api/micro/activitySetting'
import { calcPricing } from '@/api/micro/commissionRule'
import { getToken } from '@/utils/auth'

export default {
  name: 'MicroActivitySetting',
  data() {
    return {
      activityId: null,
      activityList: [],
      statusMap: { DRAFT: '草稿', PUBLISHED: '已发布', ENDED: '已结束' },
      // 选择器数据
      projectList: [],
      roomList: [],
      staffList: [],
      // 单/多选模式（默认多选，可由租户管理员配置）
      roomMultiple: true,
      staffMultiple: true,
      // 表单
      form: {
        selectedProjectId: null,
        selectedRoomIdList: [],
        selectedStaffIdList: [],
        manualTitle: null,
        bgImageUrl: null,
        voiceUrl: null,
        otherCost: 0
      },
      settingData: null,
      autoTitlePreview: null,
      previewLoading: false,
      saveLoading: false,
      // ★M35 T35.4 定价试算
      pricing: null,
      pricingLoading: false,
      pricingTimer: null,
      // 语音配置
      voiceEnabled: false,
      maxVoiceDuration: 180,
      // 上传 headers
      uploadHeaders: { Authorization: 'Bearer ' + getToken() },
      uploadUrl: process.env.VUE_APP_BASE_API + '/system/oss/upload'
    }
  },
  created() {
    this.loadActivityList()
    this.loadSelectorData()
    this.loadVoiceConfig()
  },
  methods: {
    /** 加载活动列表 */
    loadActivityList() {
      listPromo({ pageNum: 1, pageSize: 100 }).then(res => {
        this.activityList = res.rows || []
      })
    },
    /** 加载选择器数据源 */
    loadSelectorData() {
      listActivityProjects().then(res => { this.projectList = res.data || [] })
      listActivityRooms().then(res => { this.roomList = res.data || [] })
      listActivityStaff().then(res => { this.staffList = res.data || [] })
    },
    /** 加载语音配置（开关 + 最大时长） */
    loadVoiceConfig() {
      checkVoiceEnabled().then(res => {
        const data = res.data || {}
        this.voiceEnabled = data.enabled === true
        this.maxVoiceDuration = typeof data.maxVoiceDuration === 'number' ? data.maxVoiceDuration : 180
      }).catch(() => { this.voiceEnabled = false; this.maxVoiceDuration = 180 })
    },
    /** 加载活动设置 */
    loadSetting(activityId) {
      if (!activityId) return
      this.activityId = activityId
      getActivitySetting(activityId).then(res => {
        const data = res.data || {}
        this.settingData = data
        this.form = {
          selectedProjectId: data.selectedProjectId || null,
          selectedRoomIdList: data.selectedRoomIdList || [],
          selectedStaffIdList: data.selectedStaffIdList || [],
          manualTitle: data.autoTitle || data.title || null,
          bgImageUrl: data.bgImageUrl || null,
          voiceUrl: data.voiceUrl || null,
          otherCost: data.otherCost == null ? 0 : Number(data.otherCost)
        }
        this.autoTitlePreview = data.autoTitle || null
        // ★M35 T35.4 加载后自动试算一次
        this.calcPricingDebounced()
      })
    },
    /** 选择变化时更新标题预览 */
    onSelectionChange() {
      this.autoTitlePreview = null
      // ★M35 T35.4 员工选择变化时重新试算
      this.calcPricingDebounced()
    },
    /** ★M35 T35.4 定价试算（防抖 400ms） */
    calcPricingDebounced() {
      if (this.pricingTimer) clearTimeout(this.pricingTimer)
      this.pricingTimer = setTimeout(() => { this.doCalcPricing() }, 400)
    },
    doCalcPricing() {
      const staffIds = [].concat(...(this.form.selectedStaffIdList || []))
      // 单人售价：优先取活动 customPrice，其次 autoCalculatedPrice÷participantCount
      const data = this.settingData || {}
      let unitPrice = data.customPrice != null ? Number(data.customPrice) : 0
      const headcount = data.participantCount != null ? Number(data.participantCount) : 0
      if (!unitPrice && data.autoCalculatedPrice != null && headcount > 0) {
        unitPrice = Number(data.autoCalculatedPrice) / headcount
      }
      this.pricingLoading = true
      calcPricing({
        staffIds: staffIds.join(','),
        unitPrice: unitPrice,
        expectedHeadcount: headcount,
        otherCost: this.form.otherCost || 0
      }).then(res => {
        this.pricing = res.data || null
        this.pricingLoading = false
      }).catch(() => { this.pricing = null; this.pricingLoading = false })
    },
    fmtMoney(v) {
      return (v == null ? 0 : Number(v)).toFixed(2)
    },
    /** ★M35 T35.6 本场结算入口（跳转 M36 活动结算单页面） */
    goSettlement() {
      this.$router.push({ path: '/micro/settlement', query: { activityId: this.activityId } })
    },
    /** 预览自动标题 */
    handlePreviewTitle() {
      this.previewLoading = true
      // ★防御性扁平化：确保传给 API 的列表始终为一维数组
      const params = {
        projectId: this.form.selectedProjectId,
        roomIdList: [].concat(...(this.form.selectedRoomIdList || [])),
        staffIdList: [].concat(...(this.form.selectedStaffIdList || []))
      }
      previewAutoTitle(params).then(res => {
        this.autoTitlePreview = res.data || ''
        this.previewLoading = false
      }).catch(() => { this.previewLoading = false })
    },
    /** 保存设置 */
    handleSave() {
      this.saveLoading = true
      const payload = {
        activityId: this.activityId,
        selectedProjectId: this.form.selectedProjectId,
        selectedRoomIdList: this.form.selectedRoomIdList,
        selectedStaffIdList: this.form.selectedStaffIdList,
        manualTitle: this.form.manualTitle,
        bgImageUrl: this.form.bgImageUrl,
        voiceUrl: this.form.voiceUrl,
        otherCost: this.form.otherCost
      }
      saveActivitySetting(payload).then(res => {
        this.settingData = res.data || null
        this.$modal.msgSuccess('保存成功')
        this.saveLoading = false
        this.loadActivityList()
      }).catch(() => { this.saveLoading = false })
    },
    /** 语音上传前校验（类型 + 大小 + Web Audio API 时长） */
    beforeVoiceUpload(file) {
      const isAudio = file.type.startsWith('audio/')
      if (!isAudio) {
        this.$modal.msgError('只能上传音频文件')
        return false
      }
      const isLt10M = file.size / 1024 / 1024 < 10
      if (!isLt10M) {
        this.$modal.msgError('音频文件大小不能超过 10MB')
        return false
      }
      // Web Audio API 校验语音时长
      if (this.maxVoiceDuration > 0) {
        return new Promise((resolve, reject) => {
          const reader = new FileReader()
          reader.onload = (e) => {
            const AudioContext = window.AudioContext || window.webkitAudioContext
            if (!AudioContext) { resolve(true); return }
            const ctx = new AudioContext()
            ctx.decodeAudioData(e.target.result, (buffer) => {
              const duration = buffer.duration
              ctx.close()
              if (duration > this.maxVoiceDuration) {
                const maxMin = Math.floor(this.maxVoiceDuration / 60)
                const maxSec = this.maxVoiceDuration % 60
                const tip = maxSec > 0 ? maxMin + '分' + maxSec + '秒' : maxMin + '分钟'
                this.$modal.msgError('语音时长不能超过 ' + tip + '，当前音频 ' + Math.ceil(duration) + ' 秒')
                reject(new Error('语音时长超限'))
              } else {
                resolve(true)
              }
            }, () => {
              ctx.close()
              this.$modal.msgError('无法解析音频文件，请更换后重试')
              reject(new Error('音频解析失败'))
            })
          }
          reader.onerror = () => { reject(new Error('文件读取失败')) }
          reader.readAsArrayBuffer(file)
        })
      }
      return true
    },
    /** 语音上传成功回调 */
    handleVoiceSuccess(res, file) {
      console.log('[voice upload] response:', res)
      // 兼容多种响应结构：el-upload on-success 收到的是后端原始响应体
      // 结构1: { code:200, data:{ url:"..." } }  结构2: { url:"..." }（被拦截器解包）
      let url = null
      if (res && res.data && res.data.url) {
        url = res.data.url
      } else if (res && res.url) {
        url = res.url
      } else if (typeof res === 'string') {
        url = res
      }
      if (url) {
        this.$set(this.form, 'voiceUrl', url)
        console.log('[voice upload] voiceUrl set to:', url)
        this.$modal.msgSuccess('语音上传成功')
      } else {
        console.error('[voice upload] 无法从响应中提取 URL:', res)
        this.$modal.msgError('上传成功但无法获取音频地址')
      }
    }
  }
}
</script>

<style scoped>
/* ★M35 T35.4 定价试算面板 */
.pricing-card { background: #fafcff; border: 1px solid #ebeef5; }
.pricing-grid { display: flex; flex-wrap: wrap; gap: 16px; }
.pricing-item { flex: 1; min-width: 140px; display: flex; flex-direction: column; gap: 4px; padding: 8px 12px; background: #fff; border-radius: 6px; border: 1px solid #f0f2f5; }
.pricing-label { font-size: 12px; color: #909399; }
.pricing-value { font-size: 20px; font-weight: bold; }
.pricing-sub { font-size: 11px; color: #c0c4cc; }
.pricing-empty { text-align: center; color: #909399; padding: 20px 0; font-size: 13px; }
.pricing-detail { margin-top: 16px; }
.pricing-detail-title { font-size: 13px; color: #606266; font-weight: 600; margin-bottom: 8px; }
.pricing-footnote { margin-top: 12px; font-size: 11px; color: #909399; line-height: 1.6; border-top: 1px dashed #ebeef5; padding-top: 8px; }
</style>
