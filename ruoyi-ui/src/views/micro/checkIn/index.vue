<template>
  <div class="app-container">
    <el-alert title="打卡记录管理（T24.5，★FEAT_CHECK_IN 功能开关）" type="info"
      description="查看员工打卡记录、补录打卡、导出 Excel、配置打卡参数（拍照要求/迟到阈值）。三级开关：行业→租户→Boss。"
      show-icon :closable="false" class="mb16" />

    <!-- ===== 顶部筛选 ===== -->
    <el-card shadow="never" class="mb16">
      <el-form :inline="true" size="small">
        <el-form-item label="活动ID">
          <el-input v-model="query.activityId" placeholder="请输入活动ID" clearable style="width:160px;" />
        </el-form-item>
        <el-form-item label="员工ID">
          <el-input v-model="query.staffUserId" placeholder="请输入员工ID" clearable style="width:160px;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="loadRecords">查询</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
          <el-button type="success" icon="el-icon-plus" v-hasPermi="['micro:checkIn:supplement']"
            @click="openSupplementDialog">补录打卡</el-button>
          <el-button type="warning" icon="el-icon-download" v-hasPermi="['micro:checkIn:export']"
            @click="handleExport">导出 Excel</el-button>
          <el-button icon="el-icon-setting" v-hasPermi="['micro:checkIn:view']"
            @click="openConfigDialog">打卡配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ===== 打卡记录表格 ===== -->
    <el-card shadow="never" v-loading="loading">
      <el-table :data="recordList" border stripe style="width:100%;">
        <el-table-column label="ID" prop="id" width="70" align="center" />
        <el-table-column label="员工" prop="staffName" width="120" align="center" />
        <el-table-column label="活动" prop="activityTitle" min-width="150" show-overflow-tooltip />
        <el-table-column label="打卡时间" prop="checkInTime" width="160" align="center" />
        <el-table-column label="打卡地点" prop="locationName" min-width="160" show-overflow-tooltip />
        <el-table-column label="是否迟到" prop="isLateStr" width="90" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.isLateStr === '是' ? 'danger' : 'success'" size="mini">
              {{ scope.row.isLateStr }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源" prop="source" width="80" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.source === '补录' ? 'warning' : ''" size="mini">
              {{ scope.row.source }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="补录原因" prop="supplementReason" min-width="120" show-overflow-tooltip />
        <el-table-column label="打卡照片" width="90" align="center">
          <template slot-scope="scope">
            <el-image v-if="scope.row.photoUrl" :src="scope.row.photoUrl" :preview-src-list="[scope.row.photoUrl]"
              style="width:40px;height:40px;" fit="cover" />
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && recordList.length === 0" description="暂无打卡记录" />
    </el-card>

    <!-- ===== 补录打卡对话框 ===== -->
    <el-dialog title="补录打卡" :visible.sync="supplementDialog.visible" width="500px" append-to-body>
      <el-form :model="supplementDialog.form" :rules="supplementRules" ref="supplementForm" label-width="100px" size="small">
        <el-form-item label="员工ID" prop="staffUserId">
          <el-input-number v-model="supplementDialog.form.staffUserId" :min="1" style="width:100%;" placeholder="请输入员工 user_id" />
        </el-form-item>
        <el-form-item label="活动ID" prop="activityId">
          <el-input-number v-model="supplementDialog.form.activityId" :min="1" style="width:100%;" placeholder="请输入活动ID" />
        </el-form-item>
        <el-form-item label="打卡时间" prop="checkInTime">
          <el-date-picker v-model="supplementDialog.form.checkInTime" type="datetime" format="yyyy-MM-dd HH:mm:ss"
            value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择打卡时间" style="width:100%;" />
        </el-form-item>
        <el-form-item label="打卡地点">
          <el-input v-model="supplementDialog.form.locationName" placeholder="选填" />
        </el-form-item>
        <el-form-item label="补录原因">
          <el-input v-model="supplementDialog.form.supplementReason" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="supplementDialog.visible = false">取消</el-button>
        <el-button size="small" type="primary" :loading="supplementDialog.submitting" @click="submitSupplement">确认补录</el-button>
      </div>
    </el-dialog>

    <!-- ===== 打卡配置对话框 ===== -->
    <el-dialog title="打卡配置" :visible.sync="configDialog.visible" width="500px" append-to-body>
      <el-form :model="configDialog.form" label-width="120px" size="small">
        <el-form-item label="启用打卡">
          <el-switch v-model="configDialog.form.enabled" :active-value="1" :inactive-value="0" />
          <span style="margin-left:8px;color:#999;font-size:12px;">关闭后员工无法打卡</span>
        </el-form-item>
        <el-form-item label="必须拍照">
          <el-switch v-model="configDialog.form.photoRequired" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="迟到阈值(分钟)">
          <el-input-number v-model="configDialog.form.lateThresholdMinutes" :min="1" :max="120" style="width:160px;" />
          <span style="margin-left:8px;color:#999;font-size:12px;">超过活动开始时间+N分钟视为迟到</span>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="configDialog.visible = false">取消</el-button>
        <el-button size="small" type="primary" :loading="configDialog.submitting" @click="submitConfig">保存配置</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listActivityRecords, supplementCheckIn, exportRecords, getCheckInConfig, updateCheckInConfig } from '@/api/micro/checkIn'

export default {
  name: 'CheckIn',
  data() {
    return {
      loading: false,
      recordList: [],
      query: {
        activityId: null,
        staffUserId: null
      },
      supplementDialog: {
        visible: false,
        submitting: false,
        form: {
          staffUserId: null,
          activityId: null,
          checkInTime: null,
          locationName: '',
          supplementReason: ''
        }
      },
      supplementRules: {
        staffUserId: [{ required: true, message: '员工ID不能为空', trigger: 'blur' }],
        activityId: [{ required: true, message: '活动ID不能为空', trigger: 'blur' }],
        checkInTime: [{ required: true, message: '打卡时间不能为空', trigger: 'change' }]
      },
      configDialog: {
        visible: false,
        submitting: false,
        form: {
          enabled: 0,
          photoRequired: 1,
          lateThresholdMinutes: 15
        }
      }
    }
  },
  created() {
    this.loadRecords()
  },
  methods: {
    loadRecords() {
      if (!this.query.activityId) {
        this.recordList = []
        return
      }
      this.loading = true
      const params = {}
      if (this.query.staffUserId) params.staffUserId = this.query.staffUserId
      listActivityRecords(this.query.activityId, params).then(res => {
        this.recordList = res.data || []
      }).finally(() => {
        this.loading = false
      })
    },
    resetQuery() {
      this.query.activityId = null
      this.query.staffUserId = null
      this.recordList = []
    },
    openSupplementDialog() {
      this.supplementDialog.form = { staffUserId: null, activityId: null, checkInTime: null, locationName: '', supplementReason: '' }
      this.supplementDialog.visible = true
    },
    submitSupplement() {
      this.$refs.supplementForm.validate(valid => {
        if (!valid) return
        this.supplementDialog.submitting = true
        supplementCheckIn(this.supplementDialog.form).then(() => {
          this.$modal.msgSuccess('补录成功')
          this.supplementDialog.visible = false
          this.loadRecords()
        }).finally(() => {
          this.supplementDialog.submitting = false
        })
      })
    },
    handleExport() {
      const params = {}
      if (this.query.activityId) params.activityId = this.query.activityId
      if (this.query.staffUserId) params.staffUserId = this.query.staffUserId
      exportRecords(params).then(res => {
        this.$download.saveAs(res, '打卡记录.xlsx')
      })
    },
    openConfigDialog() {
      getCheckInConfig().then(res => {
        const data = res.data || {}
        this.configDialog.form.enabled = data.enabled || 0
        this.configDialog.form.photoRequired = data.photoRequired != null ? data.photoRequired : 1
        this.configDialog.form.lateThresholdMinutes = data.lateThresholdMinutes || 15
        this.configDialog.visible = true
      })
    },
    submitConfig() {
      this.configDialog.submitting = true
      updateCheckInConfig(this.configDialog.form).then(() => {
        this.$modal.msgSuccess('配置已保存')
        this.configDialog.visible = false
      }).finally(() => {
        this.configDialog.submitting = false
      })
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
</style>
