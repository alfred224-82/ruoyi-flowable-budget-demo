<template>
  <div class="app-container">
    <el-alert
      title="投诉管理（T25.4，双渠道投诉：Boss 门店 / 平台 Admin）"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom:12px;">
      <template slot="title">
        查看并处理顾客提交的双渠道投诉。渠道一（Boss 门店）为门店内部问题反馈，渠道二（平台 Admin）为违法违规举报。处理后可标记为「已解决」或「已关闭」。
      </template>
    </el-alert>

    <!-- 搜索栏 -->
    <el-form :inline="true" size="small" class="mb16">
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width:130px;" @change="handleQuery">
          <el-option label="待处理" value="PENDING" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="已解决" value="RESOLVED" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 投诉列表 -->
    <el-table v-loading="loading" :data="complaintList" border stripe style="width:100%;">
      <el-table-column label="ID" prop="id" width="60" align="center" />
      <el-table-column label="类别" prop="category" width="90" align="center">
        <template slot-scope="scope">
          {{ scope.row.category || '—' }}
        </template>
      </el-table-column>
      <el-table-column label="投诉内容" prop="contentText" min-width="200" show-overflow-tooltip />
      <el-table-column label="投诉人" width="100" align="center">
        <template slot-scope="scope">
          <span>{{ scope.row.complainantName || '匿名' }}</span>
          <div v-if="scope.row.complainantPhone" style="font-size:11px;color:#999;">{{ scope.row.complainantPhone }}</div>
        </template>
      </el-table-column>
      <el-table-column label="关联活动" prop="activityTitle" width="130" show-overflow-tooltip>
        <template slot-scope="scope">
          {{ scope.row.activityTitle || '—' }}
        </template>
      </el-table-column>
      <el-table-column label="被投诉员工" prop="staffName" width="100" align="center">
        <template slot-scope="scope">
          {{ scope.row.staffName || '—' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="90" align="center">
        <template slot-scope="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="mini">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" prop="submitTime" width="155" align="center" />
      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template slot-scope="scope">
          <el-button
            v-if="scope.row.status === 'PENDING' || scope.row.status === 'PROCESSING'"
            type="text" size="mini"
            v-hasPermi="['micro:complaint:handle']"
            @click="handleReply(scope.row)">处理</el-button>
          <el-button
            v-else
            type="text" size="mini"
            @click="handleDetail(scope.row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 详情弹窗（已解决，只读） -->
    <el-dialog title="投诉详情" :visible.sync="detailOpen" width="560px" append-to-body>
      <el-descriptions :column="2" border size="small" style="margin-bottom:16px;">
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detailRow.status)" size="mini">{{ statusLabel(detailRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ detailRow.submitTime || '—' }}</el-descriptions-item>
        <el-descriptions-item label="投诉人" :span="2">
          {{ detailRow.complainantName || '匿名' }}
          <span v-if="detailRow.complainantPhone" style="color:#999;margin-left:8px;">{{ detailRow.complainantPhone }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="投诉内容" :span="2">{{ detailRow.contentText || '—' }}</el-descriptions-item>
        <el-descriptions-item label="关联活动" v-if="detailRow.activityTitle">{{ detailRow.activityTitle }}</el-descriptions-item>
        <el-descriptions-item label="被投诉员工" v-if="detailRow.staffName">{{ detailRow.staffName }}</el-descriptions-item>
        <el-descriptions-item label="附加图片" :span="2" v-if="detailPhotoList.length">
          <div class="photo-preview">
            <el-image v-for="(url, idx) in detailPhotoList" :key="idx" :src="url"
              :preview-src-list="detailPhotoList" fit="cover"
              style="width:80px;height:80px;margin-right:8px;border-radius:4px;cursor:pointer;" />
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="取证照片" :span="2" v-if="detailEvidenceList.length">
          <div class="photo-preview">
            <el-image v-for="(url, idx) in detailEvidenceList" :key="idx" :src="url"
              :preview-src-list="detailEvidenceList" fit="cover"
              style="width:80px;height:80px;margin-right:8px;border-radius:4px;cursor:pointer;" />
          </div>
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="detailRow.handlerReply">
        <el-divider>解决方案</el-divider>
        <div style="background:#f5f7fa;padding:10px;border-radius:4px;">
          {{ detailRow.handlerReply }}
        </div>
      </div>

      <div slot="footer">
        <el-button @click="detailOpen = false">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- 处理弹窗 -->
    <el-dialog title="处理投诉" :visible.sync="replyOpen" width="560px" append-to-body>
      <el-descriptions :column="2" border size="small" style="margin-bottom:16px;">
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(currentRow.status)" size="mini">{{ statusLabel(currentRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="投诉人" :span="2">
          {{ currentRow.complainantName || '匿名' }}
          <span v-if="currentRow.complainantPhone" style="color:#999;margin-left:8px;">{{ currentRow.complainantPhone }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="投诉内容" :span="2">{{ currentRow.contentText }}</el-descriptions-item>
        <el-descriptions-item label="关联活动" v-if="currentRow.activityTitle">{{ currentRow.activityTitle }}</el-descriptions-item>
        <el-descriptions-item label="被投诉员工" v-if="currentRow.staffName">{{ currentRow.staffName }}</el-descriptions-item>
        <!-- ★附加图片（BOSS 渠道） -->
        <el-descriptions-item label="附加图片" :span="2" v-if="photoList.length">
          <div class="photo-preview">
            <el-image v-for="(url, idx) in photoList" :key="idx" :src="url"
              :preview-src-list="photoList" fit="cover"
              style="width:80px;height:80px;margin-right:8px;border-radius:4px;cursor:pointer;" />
          </div>
        </el-descriptions-item>
        <!-- ★取证照片（PLATFORM 渠道） -->
        <el-descriptions-item label="取证照片" :span="2" v-if="evidenceList.length">
          <div class="photo-preview">
            <el-image v-for="(url, idx) in evidenceList" :key="idx" :src="url"
              :preview-src-list="evidenceList" fit="cover"
              style="width:80px;height:80px;margin-right:8px;border-radius:4px;cursor:pointer;" />
          </div>
        </el-descriptions-item>
      </el-descriptions>

      <el-form ref="replyForm" :model="replyForm" :rules="replyRules" label-width="80px">
        <el-form-item label="处理结果" prop="status">
          <el-radio-group v-model="replyForm.status">
            <el-radio label="RESOLVED">已解决</el-radio>
            <el-radio label="CLOSED">已关闭</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="回复内容" prop="handlerReply">
          <el-input v-model="replyForm.handlerReply" type="textarea" :rows="4" placeholder="请输入处理回复..." maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>

      <div slot="footer">
        <el-button type="primary" @click="submitReply" :loading="submitting">提 交</el-button>
        <el-button @click="replyOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listComplaints, replyComplaint } from '@/api/micro/complaint'

export default {
  name: 'MicroComplaint',
  data() {
    return {
      loading: false,
      complaintList: [],
      queryParams: { channel: 'BOSS', status: null },
      // 处理弹窗
      replyOpen: false,
      currentRow: {},
      replyForm: { handlerReply: '', status: 'RESOLVED' },
      replyRules: {
        handlerReply: [{ required: true, message: '请输入处理回复', trigger: 'blur' }]
      },
      submitting: false,
      photoList: [],
      evidenceList: [],
      // 详情弹窗（已解决，只读）
      detailOpen: false,
      detailRow: {},
      detailPhotoList: [],
      detailEvidenceList: []
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 解析 JSON 图片 URL 列表 */
    parsePhotoUrls(jsonStr) {
      if (!jsonStr) return []
      try {
        const arr = JSON.parse(jsonStr)
        return Array.isArray(arr) ? arr : []
      } catch (e) { return [] }
    },
    /** 查询投诉列表 */
    getList() {
      this.loading = true
      listComplaints(this.queryParams).then(res => {
        this.complaintList = res.data || []
      }).finally(() => {
        this.loading = false
      })
    },
    /** 搜索 */
    handleQuery() {
      this.getList()
    },
    /** 重置 */
    resetQuery() {
      this.queryParams = { channel: 'BOSS', status: null }
      this.getList()
    },
    /** 打开详情弹窗（已解决，只读） */
    handleDetail(row) {
      this.detailRow = row
      this.detailPhotoList = this.parsePhotoUrls(row.photoUrls)
      this.detailEvidenceList = this.parsePhotoUrls(row.evidencePhotos)
      this.detailOpen = true
    },
    /** 打开处理弹窗 */
    handleReply(row) {
      this.currentRow = row
      this.photoList = this.parsePhotoUrls(row.photoUrls)
      this.evidenceList = this.parsePhotoUrls(row.evidencePhotos)
      this.replyForm = { handlerReply: '', status: 'RESOLVED' }
      this.replyOpen = true
    },
    /** 提交处理 */
    submitReply() {
      this.$refs.replyForm.validate(valid => {
        if (!valid) return
        this.submitting = true
        replyComplaint(this.currentRow.id, this.replyForm).then(res => {
          this.$modal.msgSuccess('处理成功')
          this.replyOpen = false
          this.getList()
        }).finally(() => {
          this.submitting = false
        })
      })
    },
    /** 状态标签文本 */
    statusLabel(status) {
      const map = { PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已解决', CLOSED: '已关闭' }
      return map[status] || status
    },
    /** 状态标签颜色 */
    statusTagType(status) {
      const map = { PENDING: 'info', PROCESSING: 'warning', RESOLVED: 'success', CLOSED: '' }
      return map[status] || 'info'
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
</style>
