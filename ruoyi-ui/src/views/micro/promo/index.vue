<template>
  <div class="app-container">
    <el-alert
      title="推广发布（T15.24 / T15.28）：lite 档老板的唯一经营能力"
      type="info"
      description="发布的活动在小程序「活动浏览页」（micro:activity:browse）只读展示，顾客/业务员无需登录即可浏览已发布活动。★V1 零支付：活动正文与海报不得出现任何在线支付入口或收款链接。"
      show-icon
      :closable="false"
      class="mb8"
    />

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="70px">
          <el-form-item label="活动标题" prop="title">
            <el-input v-model="queryParams.title" placeholder="请输入活动标题" clearable @keyup.enter.native="handleQuery" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width:130px;">
              <el-option v-for="(label, val) in statusMap" :key="val" :label="label" :value="val" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['micro:promo:add']">发布活动</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['micro:promo:remove']">删除</el-button>
          </el-col>
          <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="promoList" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column label="封面" align="center" width="80">
            <template slot-scope="scope">
              <image-preview v-if="scope.row.coverUrl" :src="scope.row.coverUrl" :width="50" :height="38" />
              <span v-else style="color:#c0c4cc;">—</span>
            </template>
          </el-table-column>
          <el-table-column label="活动标题" align="left" prop="title" min-width="160" show-overflow-tooltip />
          <el-table-column label="状态" align="center" prop="status" width="90">
            <template slot-scope="scope">
              <el-tag v-if="scope.row.isLocked" type="danger" size="mini">已锁定</el-tag>
              <el-tag v-else :type="statusTagType(scope.row.status)" size="mini">{{ statusMap[scope.row.status] || scope.row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="活动时间" align="center" width="300">
            <template slot-scope="scope">
              <span>{{ parseTime(scope.row.startTime) || '—' }} ~ {{ parseTime(scope.row.endTime) || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="发布时间" align="center" prop="publishTime" width="160">
            <template slot-scope="scope">
              <span>{{ scope.row.publishTime ? parseTime(scope.row.publishTime) : '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="300">
            <template slot-scope="scope">
              <el-button v-if="scope.row.status === 'DRAFT' && !scope.row.isLocked" size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['micro:promo:edit']">编辑</el-button>
              <el-button v-if="scope.row.status !== 'DRAFT' || scope.row.isLocked" size="mini" type="text" icon="el-icon-view" @click="handleUpdate(scope.row)">查看</el-button>
              <el-button
                v-if="scope.row.status !== 'PUBLISHED'"
                size="mini"
                type="text"
                icon="el-icon-upload2"
                @click="handleStatus(scope.row, 'PUBLISHED')"
                v-hasPermi="['micro:promo:edit']"
              >发布</el-button>
              <el-button
                v-else
                size="mini"
                type="text"
                icon="el-icon-download"
                @click="handleStatus(scope.row, 'DRAFT')"
                v-hasPermi="['micro:promo:edit']"
              >下架</el-button>
              <el-button
                v-if="scope.row.status !== 'ENDED'"
                size="mini"
                type="text"
                icon="el-icon-circle-close"
                @click="handleStatus(scope.row, 'ENDED')"
                v-hasPermi="['micro:promo:edit']"
              >结束</el-button>
              <el-button
                v-if="!scope.row.isLocked"
                size="mini"
                type="text"
                icon="el-icon-lock"
                @click="handleLock(scope.row, true)"
                v-hasPermi="['micro:activity:lock']"
              >锁定</el-button>
              <el-button
                v-else
                size="mini"
                type="text"
                icon="el-icon-unlock"
                @click="handleLock(scope.row, false)"
                v-hasPermi="['micro:activity:unlock']"
                style="color:#E6A23C;"
              >解锁</el-button>
              <el-button size="mini" type="text" icon="el-icon-picture-outline" @click="handleGenPoster(scope.row)" v-hasPermi="['micro:promo:edit']">生成海报</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['micro:promo:remove']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 发布 / 编辑活动（★重新设计：选项目→选房间→选人员→自动标题→图片→语音→介绍） -->
    <el-dialog :title="title" :visible.sync="open" width="720px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" :disabled="readonly">
        <!-- ① 选项目 + 选房间 -->
        <el-row>
          <el-col :span="12">
            <el-form-item label="选择项目">
              <el-select v-model="form.selectedProjectId" placeholder="请选择项目" filterable clearable style="width:100%;" @change="onSelectionChange">
                <el-option v-for="item in projectList" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="选择房间">
              <el-select v-model="selectedRoomIds" placeholder="请选择房间" filterable clearable multiple style="width:100%;" @change="onRoomChange">
                <el-option v-for="item in roomList" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <!-- ② 选人员 -->
        <el-form-item label="选择员工">
          <el-select v-model="selectedStaffIds" placeholder="请选择员工" filterable clearable multiple style="width:100%;" @change="onSelectionChange">
            <el-option v-for="item in staffList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <!-- ★T26.10 背景画布加人：点座位「＋」号选人（员工=导游/司机 或 注册游客），头像上座 -->
        <el-form-item label="画布加人" v-if="canArrangeSeats">
          <div class="canvas-toolbar">
            <el-select v-model="canvasRoomId" placeholder="选择编排房间" size="mini" style="width:180px;" @change="onCanvasRoomChange">
              <el-option v-for="rid in selectedRoomIds" :key="rid" :label="roomName(rid)" :value="rid" />
            </el-select>
            <span class="canvas-tip">点座位「＋」号加人；已加人座位显示头像，点击可换人/移除</span>
          </div>
          <div v-if="canvasLoading" class="canvas-loading">布局加载中…</div>
          <div v-else-if="canvasLayout" ref="seatCanvas" class="seat-canvas" :style="canvasWrapStyle">
            <!-- ★背景图流内等比渲染：容器高度=图片高度，座位百分比坐标与图片/编辑器对齐（cover+paddingBottom 在布局尺寸过期时会裁切偏移） -->
            <img v-if="canvasLayout.bgImageUrl" :src="canvasLayout.bgImageUrl" class="canvas-bg" draggable="false" />
            <div
              v-for="seat in canvasLayout.seats"
              :key="seat.id"
              class="canvas-seat"
              :class="{ 'is-assigned': !!seatAssignMap[seat.id], 'is-static-preset': seat.isPreset === 1 }"
              :style="canvasSeatStyle(seat)"
              @click="!readonly && onCanvasSeatClick(seat)"
            >
              <img v-if="seatAssignMap[seat.id] && seatAssignMap[seat.id].avatar" class="canvas-avatar" :src="seatAssignMap[seat.id].avatar" />
              <span v-else-if="seatAssignMap[seat.id]" class="canvas-icon assigned">👤</span>
              <span v-else class="canvas-icon plus">＋</span>
              <span v-if="canvasSeatSize >= 32" class="canvas-seat-no">{{ seat.seatNo || '-' }}</span>
            </div>
          </div>
          <div v-else class="canvas-empty">该房间未配置座位布局，无法画布加人（请先在「房间布局配置」中设置）</div>
        </el-form-item>
        <!-- ★M22 费用与人数 -->
        <el-divider content-position="left">费用与人数</el-divider>
        <el-row>
          <el-col :span="12">
            <el-form-item label="参与人数">
              <el-input-number v-model="form.participantCount" :min="1" :max="999" placeholder="设定参与人数" style="width:100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="自定义价格">
              <el-checkbox v-model="useCustomPrice">使用自定义价格</el-checkbox>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="自定义总价" v-if="useCustomPrice">
          <el-input-number v-model="form.customPrice" :min="0" :precision="2" :step="10" placeholder="输入自定义总价" style="width:240px;" />
          <span style="margin-left:8px;color:#909399;">元（覆盖自动计算价格）</span>
        </el-form-item>
        <el-form-item v-if="form.participantCount && form.participantCount > 0">
          <span style="color:#909399;">人均费用将在保存后由系统自动计算（总价÷参与人数）</span>
        </el-form-item>
        <!-- ③ 活动标题（自动生成 + 可手动覆盖） -->
        <el-form-item label="活动标题" prop="title">
          <el-input v-model="form.title" placeholder="留空则自动生成，也可手动输入" maxlength="200" show-word-limit>
            <el-button slot="append" @click="handlePreviewTitle" :loading="previewLoading">自动生成</el-button>
          </el-input>
          <div v-if="autoTitlePreview" style="color:#67C23A;font-size:12px;margin-top:4px;">预览：{{ autoTitlePreview }}</div>
        </el-form-item>
        <!-- ④ 活动起止时间 -->
        <el-row>
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" value-format="yyyy-MM-dd HH:mm:ss" style="width:100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择结束时间" value-format="yyyy-MM-dd HH:mm:ss" style="width:100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- ⑤ 活动海报上传（不限制张数，小程序详情横滑展示；后端 cover_url 已扩为 TEXT） -->
        <el-form-item label="活动图片">
          <image-upload v-model="form.coverUrl" :limit="999" />
          <div style="color:#909399;font-size:12px;">可上传多张海报（不限张数），小程序活动详情横滑展示</div>
        </el-form-item>
        <!-- ⑥ 语音上传（仅管理员启用语音时显示） -->
        <el-form-item label="活动语音" v-if="voiceEnabled">
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
        <!-- ⑦ 活动介绍 -->
        <el-form-item label="活动介绍" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" maxlength="2000" show-word-limit placeholder="活动详情、规则说明。★请勿包含任何在线支付入口或收款链接" />
        </el-form-item>
        <!-- ⑧ 状态 -->
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="DRAFT">草稿</el-radio>
            <el-radio label="PUBLISHED">立即发布</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button v-if="!readonly" type="primary" :loading="submitting" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">{{ readonly ? '关 闭' : '取 消' }}</el-button>
      </div>
    </el-dialog>

    <!-- ★M31 T31.1 生成活动海报弹窗 -->
    <el-dialog title="生成活动海报" :visible.sync="posterOpen" width="520px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="活动">
          <span>{{ posterActivity.title || '—' }}</span>
        </el-form-item>
        <el-form-item label="海报底图">
          <image-upload v-model="posterBgUrl" :limit="1" />
          <div style="color:#909399;font-size:12px;">上传一张底图，系统将自动在右下角叠加活动二维码</div>
        </el-form-item>
        <el-form-item v-if="posterResult" label="生成结果">
          <image-preview :src="posterResult" :width="200" :height="150" />
          <div style="margin-top:8px;">
            <el-link type="primary" :href="posterResult" target="_blank">下载海报</el-link>
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="posterLoading" @click="doGeneratePoster">生成海报</el-button>
        <el-button @click="posterOpen = false">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- ★T26.10 画布加人——选人弹窗（按角色分组、头像列表） -->
    <el-dialog title="选择人员" :visible.sync="pickerVisible" width="480px" append-to-body>
      <div v-if="candidatesLoading" style="text-align:center;color:#909399;padding:24px;">加载中…</div>
      <div v-else class="picker-groups">
        <div v-for="group in candidateGroups" :key="group.roleCode" class="picker-group">
          <div class="picker-group-title">{{ group.roleLabel }}（{{ (group.persons || []).length }}）</div>
          <div class="picker-persons">
            <div
              v-for="p in group.persons"
              :key="p.userId"
              class="picker-person"
              :class="{ disabled: isPersonUsed(p.userId) }"
              @click="choosePerson(group, p)"
            >
              <img v-if="p.avatar" class="picker-avatar" :src="p.avatar" />
              <span v-else class="picker-avatar placeholder">👤</span>
              <span class="picker-name">{{ p.name || '未命名' }}</span>
            </div>
            <div v-if="!(group.persons || []).length" class="picker-empty">暂无{{ group.roleLabel }}</div>
          </div>
        </div>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button v-if="pickerSeat && seatAssignMap[pickerSeat.id]" type="danger" plain size="small" @click="removePerson">移除该座人员</el-button>
        <el-button size="small" @click="pickerVisible = false">取 消</el-button>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import { listByIds } from '@/api/system/oss'
import { listPromo, getPromo, addPromo, updatePromo, changePromoStatus, delPromo, checkVoiceEnabled, lockActivity, unlockActivity } from '@/api/micro/promo'
import { generatePoster } from '@/api/micro/poster'
import { listActivityProjects, listActivityRooms, listActivityStaff, previewAutoTitle } from '@/api/micro/activitySetting'
import { getRoomLayout } from '@/api/micro/roomLayout'
import { listSeatCandidates, saveCanvasPersons, listSeatAssignments } from '@/api/micro/seatAssignment'
import { checkPermi } from '@/utils/permission'
import { getToken } from '@/utils/auth'

export default {
  name: 'MicroPromo',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      promoList: [],
      ids: [],
      multiple: true,
      open: false,
      title: '',
      statusMap: { DRAFT: '草稿', PUBLISHED: '已发布', ENDED: '已结束' },
      queryParams: { pageNum: 1, pageSize: 10, title: null, status: null },
      form: {},
      rules: {
        title: [{ required: true, message: '活动标题不能为空', trigger: 'blur' }],
        startTime: [{ required: true, message: '开始时间不能为空', trigger: 'change' }],
        endTime: [{ required: true, message: '结束时间不能为空', trigger: 'change' }]
      },
      // ★活动发布增强：项目/房间/员工选择 + 自动标题 + 语音 + LLM
      projectList: [],
      roomList: [],
      staffList: [],
      selectedRoomIds: [],
      selectedStaffIds: [],
      autoTitlePreview: null,
      previewLoading: false,
      voiceEnabled: false,
      maxVoiceDuration: 180,
      useCustomPrice: false,
      uploadHeaders: { Authorization: 'Bearer ' + getToken() },
      uploadUrl: process.env.VUE_APP_BASE_API + '/system/oss/upload',
      // ★T26.10 背景画布加人
      canArrangeSeats: false,   // 是否有编排权限（micro:seat:assign）
      readonly: false,          // ★查看模式：非草稿活动只读（PUBLISHED/ENDED 仅查看，下架 DRAFT 后可编辑）
      canvasRoomId: null,       // 当前编排房间
      canvasLayout: null,       // 房间布局（bgImageUrl/layoutWidth/layoutHeight/seats）
      canvasLoading: false,
      // ★防重复提交：保存期间（含同步语音合规 ASR，可达数十秒）确定按钮 loading 并拒绝重复点击
      submitting: false,
      // ★座位圆点直径（px）：按座位点最小邻距随画布宽度等比换算，与背景图座位同大小
      canvasSeatSize: 0,
      seatAssignMap: {},        // seatId -> { userId, name, avatar, roleCode }
      pickerVisible: false,
      pickerSeat: null,
      candidateGroups: [],
      candidatesLoading: false,
      candidatesLoaded: false,
      // ★M31 T31.1 生成活动海报
      posterOpen: false,
      posterActivity: {},
      posterBgUrl: null,
      posterResult: null,
      posterLoading: false
    }
  },
  created() {
    this.canArrangeSeats = checkPermi(['micro:seat:assign'])
    this.getList()
    this.loadSelectorData()
    this.checkFeatures()
  },
  mounted() {
    // ★窗口缩放时对话框/画布宽度变化，圆点直径需重新换算
    window.addEventListener('resize', this.measureCanvasSeat)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.measureCanvasSeat)
  },
  watch: {
    // ★对话框打开后画布才挂载，校准圆点直径（编辑回显时布局加载可能早于对话框渲染）
    open(val) {
      if (val) this.$nextTick(() => this.measureCanvasSeat())
    }
  },
  computed: {
    /** ★注入 --ss 变量：座位圆点直径随画布宽度等比缩放（与背景图座位同大小） */
    canvasWrapStyle() {
      return { '--ss': (this.canvasSeatSize || 40) + 'px' }
    }
  },
  methods: {
    getList() {
      this.loading = true
      listPromo(this.queryParams).then(response => {
        this.promoList = response.rows
        this.total = response.total
        this.loading = false
        // ★coverUrl/posterUrl 存的是 ossId，需解析为完整 URL 才能被 image-preview 渲染
        this.resolveOssIds()
      }).catch(() => { this.loading = false })
    },
    /** 批量解析 ossId → 完整 URL（coverUrl / posterUrl / logoUrl）
     * ★服务端（PromotionActivityServiceImpl）已统一翻译，此处仅作兼容兜底：
     *   仅纯数字 token 视为 ossId；已是 URL 的 token 原样保留；单 token 解析失败回退自身而非整串 */
    resolveOssIds() {
      const isOssId = v => /^\d+$/.test(v)
      const ossIdSet = new Set()
      this.promoList.forEach(row => {
        ['coverUrl', 'posterUrl', 'logoUrl'].forEach(key => {
          if (row[key]) row[key].split(',').forEach(id => { if (isOssId(id.trim())) ossIdSet.add(id.trim()) })
        })
      })
      if (ossIdSet.size === 0) return
      const ossIds = Array.from(ossIdSet).join(',')
      listByIds(ossIds).then(res => {
        const urlMap = {}
        ;(res.data || []).forEach(item => { urlMap[item.ossId] = item.url })
        this.promoList.forEach(row => {
          ['coverUrl', 'posterUrl', 'logoUrl'].forEach(key => {
            if (row[key]) row[key] = row[key].split(',').map(id => urlMap[id.trim()] || id).join(',')
          })
        })
      }).catch(() => {})
    },
    statusTagType(status) {
      if (status === 'PUBLISHED') return 'success'
      if (status === 'ENDED') return 'info'
      return 'warning'
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.multiple = !selection.length
    },
    reset() {
      this.form = { id: null, title: null, coverUrl: null, content: null, status: 'DRAFT', selectedProjectId: null, voiceUrl: null, startTime: null, endTime: null, customPrice: null, participantCount: null }
      this.selectedRoomIds = []
      this.selectedStaffIds = []
      this.autoTitlePreview = null
      this.useCustomPrice = false
      // ★T26.10 清空画布状态
      this.canvasRoomId = null
      this.canvasLayout = null
      this.seatAssignMap = {}
      this.pickerSeat = null
      this.pickerVisible = false
      this.readonly = false
      this.resetForm('form')
    },
    cancel() {
      this.open = false
      this.reset()
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '发布活动'
    },
    handleUpdate(row) {
      this.reset()
      // ★非草稿或已锁定活动仅查看（全部只读），下架(DRAFT)且未锁定后可编辑
      this.readonly = row.status !== 'DRAFT' || row.isLocked
      getPromo(row.id).then(response => {
        const data = response.data
        this.form = { id: data.id, title: data.title, coverUrl: data.coverUrl, content: data.content, status: data.status, selectedProjectId: data.selectedProjectId, voiceUrl: data.voiceUrl, startTime: data.startTime, endTime: data.endTime, customPrice: data.customPrice, participantCount: data.participantCount }
        this.useCustomPrice = data.customPrice != null
        // 解析 JSON 列表
        try { this.selectedRoomIds = data.selectedRoomIds ? JSON.parse(data.selectedRoomIds) : [] } catch (e) { this.selectedRoomIds = [] }
        try { this.selectedStaffIds = data.selectedStaffIds ? JSON.parse(data.selectedStaffIds) : [] } catch (e) { this.selectedStaffIds = [] }
        // ★T26.10 编辑回显：加载主房间布局 + 已加人座位
        if (this.canArrangeSeats && this.selectedRoomIds.length) {
          this.canvasRoomId = this.selectedRoomIds[0]
          this.loadCanvasLayout()
          this.loadExistingAssignments(data.id)
        }
        this.open = true
        this.title = this.readonly ? '查看活动' : '编辑活动'
      })
    },
    submitForm() {
      if (this.submitting) return
      this.$refs['form'].validate(valid => {
        if (!valid) return
        const payload = Object.assign({}, this.form)
        // 附加项目/房间/员工选择
        payload.selectedProjectId = this.form.selectedProjectId
        payload.selectedRoomIds = this.selectedRoomIds.length > 0 ? JSON.stringify(this.selectedRoomIds) : null
        payload.selectedStaffIds = this.selectedStaffIds.length > 0 ? JSON.stringify(this.selectedStaffIds) : null
        // ★M22 费用字段
        payload.participantCount = this.form.participantCount || null
        payload.customPrice = this.useCustomPrice ? this.form.customPrice : null
        // 如果标题为空且选择了项目/房间/员工，自动生成
        if (!payload.title && (this.form.selectedProjectId || this.selectedRoomIds.length || this.selectedStaffIds.length)) {
          previewAutoTitle({ projectId: this.form.selectedProjectId, roomIdList: [].concat(...this.selectedRoomIds), staffIdList: [].concat(...this.selectedStaffIds) }).then(res => {
            payload.title = res.data || '未命名活动'
            this.doSubmit(payload)
          }).catch(() => { payload.title = '未命名活动'; this.doSubmit(payload) })
        } else {
          this.doSubmit(payload)
        }
      })
    },
    doSubmit(payload) {
      if (this.submitting) return
      this.submitting = true
      const isEdit = payload.id != null
      const req = isEdit ? updatePromo(payload) : addPromo(payload)
      req.then(res => {
        // ★T26.10 新建时 addPromo 返回 R<Long>.data = 新活动ID；链式保存画布加人
        const activityId = isEdit ? payload.id : (res && res.data)
        return this.saveCanvasIfNeeded(activityId)
      }).then(() => {
        this.$modal.msgSuccess(isEdit ? '修改成功' : '发布成功')
        this.open = false
        this.getList()
      }).catch(() => {}).finally(() => { this.submitting = false })
    },
    handleStatus(row, status) {
      const actionMap = { PUBLISHED: '发布', DRAFT: '下架', ENDED: '结束' }
      this.$modal.confirm('确认' + actionMap[status] + '活动「' + row.title + '」吗？')
        .then(() => changePromoStatus(row.id, status))
        .then(() => {
          this.$modal.msgSuccess(actionMap[status] + '成功')
          this.getList()
        })
        .catch(() => {})
    },
    handleDelete(row) {
      const delIds = row.id ? [row.id] : this.ids
      if (!delIds.length) return
      this.$modal.confirm('确认删除选中的 ' + delIds.length + ' 个活动吗？')
        .then(() => delPromo(delIds.join(',')))
        .then(() => {
          this.$modal.msgSuccess('删除成功')
          this.getList()
        })
        .catch(() => {})
    },
    // ===== 活动发布增强方法 =====
    /** 加载项目/房间/员工选择器数据 */
    loadSelectorData() {
      listActivityProjects().then(res => { this.projectList = res.data || [] })
      listActivityRooms().then(res => { this.roomList = res.data || [] })
      listActivityStaff().then(res => { this.staffList = res.data || [] })
    },
    /** 检查语音开关 + 最大语音时长 */
    checkFeatures() {
      checkVoiceEnabled().then(res => {
        const data = res.data || {}
        this.voiceEnabled = data.enabled === true
        // maxVoiceDuration 单位秒，0 表示不限
        this.maxVoiceDuration = typeof data.maxVoiceDuration === 'number' ? data.maxVoiceDuration : 180
      }).catch(() => { this.voiceEnabled = false; this.maxVoiceDuration = 180 })
    },
    /** 选择变化时更新标题预览 */
    onSelectionChange() {
      this.autoTitlePreview = null
    },
    // ===== ★T26.10 背景画布加人 =====
    /** 房间多选变化：同步编排房间（默认首个），房间变更时清空已加人 */
    onRoomChange() {
      this.onSelectionChange()
      if (!this.canArrangeSeats) return
      const rooms = this.selectedRoomIds || []
      if (!rooms.length) {
        this.canvasRoomId = null
        this.canvasLayout = null
        this.seatAssignMap = {}
        return
      }
      if (!this.canvasRoomId || rooms.indexOf(this.canvasRoomId) === -1) {
        this.canvasRoomId = rooms[0]
        this.seatAssignMap = {}
        this.loadCanvasLayout()
      }
    },
    /** 画布内切换编排房间 */
    onCanvasRoomChange() {
      this.seatAssignMap = {}
      this.loadCanvasLayout()
    },
    /** 房间显示名 */
    roomName(rid) {
      const r = (this.roomList || []).find(x => String(x.id) === String(rid))
      return r ? r.name : ('房间' + rid)
    },
    /** 加载当前编排房间的座位布局 */
    loadCanvasLayout() {
      if (!this.canvasRoomId) { this.canvasLayout = null; return }
      this.canvasLoading = true
      getRoomLayout(this.canvasRoomId).then(res => {
        this.canvasLayout = res.data || null
        this.canvasLoading = false
        this.$nextTick(() => this.measureCanvasSeat())
      }).catch(() => { this.canvasLayout = null; this.canvasLoading = false })
    },
    /** ★校准圆点直径：= 座位点最小邻距（百分比坐标换算为画布宽比例）× 0.85 ≈ 背景图座位方块大小；
     * 座位点由 Boss 沿图中座位落位，最小邻距即座位间距，随画布宽度等比缩放保证圆点与座位同大小 */
    measureCanvasSeat() {
      const el = this.$refs.seatCanvas
      if (!el || !el.clientWidth || !this.canvasLayout) return
      const seats = this.canvasLayout.seats || []
      // 百分比坐标各向异性：Y 方向按布局高宽比折算为宽度比例
      const aspect = (this.canvasLayout.layoutHeight || 1080) / (this.canvasLayout.layoutWidth || 1920)
      let minDist = 0
      for (let i = 0; i < seats.length; i++) {
        for (let j = i + 1; j < seats.length; j++) {
          const dx = parseFloat(seats[i].posXPercent || 0) - parseFloat(seats[j].posXPercent || 0)
          const dy = (parseFloat(seats[i].posYPercent || 0) - parseFloat(seats[j].posYPercent || 0)) * aspect
          const dist = Math.sqrt(dx * dx + dy * dy)
          if (dist > 0 && (minDist === 0 || dist < minDist)) minDist = dist
        }
      }
      // 单点回落 4%；上限 6% 防个别异常间距撑大圆点
      const ratio = minDist > 0 ? minDist * 0.85 : 0.04
      this.canvasSeatSize = Math.max(12, Math.round(el.clientWidth * Math.min(ratio, 0.06)))
    },
    /** 座位绝对定位样式（百分比坐标 0~1 → %） */
    canvasSeatStyle(seat) {
      const x = parseFloat(seat.posXPercent || 0) * 100
      const y = parseFloat(seat.posYPercent || 0) * 100
      return { left: x + '%', top: y + '%' }
    },
    /** 编辑回显：加载本活动已加人座位（仅 PRESET/BOSS_ASSIGN，排除顾客自选） */
    loadExistingAssignments(activityId) {
      if (!activityId) return
      listSeatAssignments(activityId).then(res => {
        const map = {}
        ;(res.data || []).forEach(a => {
          if (a.assignmentType === 'CUSTOMER_PICK') return
          if (a.seatId == null || a.userId == null) return
          map[a.seatId] = { userId: a.userId, name: a.userNickName, avatar: a.userAvatar, roleCode: a.roleCode }
        })
        this.seatAssignMap = map
      }).catch(() => {})
    },
    /** 点击画布座位 → 打开选人弹窗 */
    onCanvasSeatClick(seat) {
      this.pickerSeat = seat
      this.pickerVisible = true
      this.ensureCandidates()
    },
    /** 懒加载候选人（首次打开选人弹窗时拉取） */
    ensureCandidates() {
      if (this.candidatesLoaded) return
      this.candidatesLoading = true
      listSeatCandidates().then(res => {
        this.candidateGroups = res.data || []
        this.candidatesLoaded = true
        this.candidatesLoading = false
      }).catch(() => { this.candidatesLoading = false })
    },
    /** 选中某人到当前座位 */
    choosePerson(group, p) {
      if (this.isPersonUsed(p.userId)) {
        this.$modal.msgWarning('该人员已在本活动其他座位')
        return
      }
      this.$set(this.seatAssignMap, this.pickerSeat.id, {
        userId: p.userId, name: p.name, avatar: p.avatar, roleCode: group.roleCode
      })
      this.pickerVisible = false
    },
    /** 人员是否已被本活动其他座位占用 */
    isPersonUsed(userId) {
      const cur = this.pickerSeat ? this.seatAssignMap[this.pickerSeat.id] : null
      return Object.keys(this.seatAssignMap).some(k => {
        const a = this.seatAssignMap[k]
        return String(a.userId) === String(userId) && (!cur || a !== cur)
      })
    },
    /** 移除当前座位人员 */
    removePerson() {
      if (this.pickerSeat) this.$delete(this.seatAssignMap, this.pickerSeat.id)
      this.pickerVisible = false
    },
    /** 保存画布加人（活动保存成功后链式调用；空列表=清空预置/指派） */
    saveCanvasIfNeeded(activityId) {
      if (!this.canArrangeSeats || !this.canvasLayout || !activityId) return Promise.resolve()
      const items = []
      ;(this.canvasLayout.seats || []).forEach(seat => {
        const a = this.seatAssignMap[seat.id]
        if (a) items.push({ seatId: seat.id, seatNo: seat.seatNo, userId: a.userId, remark: a.remark || null })
      })
      return saveCanvasPersons({ activityId: activityId, items: items })
    },
    /** 预览自动生成的标题 */
    handlePreviewTitle() {
      // ★业务规则：项目/房间/员工均为可选，至少选择一项即可自动生成标题
      if (!this.form.selectedProjectId && !this.selectedRoomIds.length && !this.selectedStaffIds.length) {
        this.$modal.msgWarning('请至少选择一个项目、房间或员工')
        return
      }
      
      this.previewLoading = true
      // ★防御性扁平化：确保传给 API 的列表始终为一维数组，避免 tansParams 序列化异常
      const flatRoomIds = [].concat(...this.selectedRoomIds)
      const flatStaffIds = [].concat(...this.selectedStaffIds)
      previewAutoTitle({
        projectId: this.form.selectedProjectId,
        roomIdList: flatRoomIds,
        staffIdList: flatStaffIds
      }).then(res => {
        this.previewLoading = false
        if (res.data) {
          this.autoTitlePreview = res.data
          this.form.title = res.data
          this.$modal.msgSuccess('标题已自动生成')
        } else {
          // API 返回 null，说明所选项在数据库中不存在或已被删除
          const missingItems = []
          if (this.form.selectedProjectId) missingItems.push('项目')
          if (this.selectedRoomIds.length > 0) missingItems.push('房间')
          if (this.selectedStaffIds.length > 0) missingItems.push('员工')
          this.$modal.msgWarning('无法生成标题：所选' + (missingItems.join('/') || '资源') + '在系统中不存在或已被删除，请重新选择')
        }
      }).catch(err => {
        this.previewLoading = false
        console.error('自动生成标题失败:', err)
        this.$modal.msgError('自动生成标题失败：' + (err.message || '未知错误'))
      })
    },
    /** 语音上传前校验（类型 + 大小 + Web Audio API 时长） */
    beforeVoiceUpload(file) {
      const isAudio = file.type.startsWith('audio/')
      if (!isAudio) { this.$modal.msgError('只能上传音频文件'); return false }
      const isLt10M = file.size / 1024 / 1024 < 10
      if (!isLt10M) { this.$modal.msgError('音频文件大小不能超过 10MB'); return false }
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
    },
    /** ★M28 T28.1/T28.5 锁定/解锁活动（解锁需二次确认：解锁后座位可调整） */
    /** ★M31 T31.1 生成活动海报 */
    handleGenPoster(row) {
      this.posterActivity = row
      this.posterBgUrl = row.posterBgUrl || null
      this.posterResult = null
      this.posterOpen = true
    },
    doGeneratePoster() {
      if (!this.posterBgUrl) {
        this.$modal.msgWarning('请先上传海报底图')
        return
      }
      this.posterLoading = true
      generatePoster(this.posterActivity.id, this.posterBgUrl).then(res => {
        this.posterResult = (res.data && res.data.posterUrl) || res.data
        this.$modal.msgSuccess('海报生成成功')
      }).catch(() => {}).finally(() => { this.posterLoading = false })
    },
    handleLock(row, locked) {
      if (locked) {
        // 锁定：告知锁定后不可报名/取消，座位变只读
        this.$modal.confirm('确认锁定活动「' + row.title + '」吗？锁定后顾客不可报名/选座，座位变只读。')
          .then(() => lockActivity(row.id))
          .then(() => {
            this.$modal.msgSuccess('锁定成功')
            this.getList()
          })
          .catch(() => {})
      } else {
        // 解锁：★二次确认弹窗（T28.5）——解锁后座位可调整
        this.$modal.confirm('解锁后座位可调整，确定解锁吗？')
          .then(() => unlockActivity(row.id))
          .then(() => {
            this.$modal.msgSuccess('解锁成功')
            this.getList()
          })
          .catch(() => {})
      }
    }
  }
}
</script>

<style scoped>
/* ===== ★T26.10 背景画布加人 ===== */
.canvas-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.canvas-tip { color: #909399; font-size: 12px; }
.canvas-loading, .canvas-empty { color: #909399; font-size: 13px; padding: 24px; text-align: center; background: #fafafa; border: 1px dashed #dcdfe6; border-radius: 6px; }
.seat-canvas {
  position: relative;
  width: 100%;
  background-color: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
}
/* ★背景图流内等比缩放（宽撑满、高按比例）：容器高度自适应图片比例，座位百分比坐标精准落在图片上 */
.canvas-bg { display: block; width: 100%; }
.canvas-seat {
  position: absolute;
  /* ★直径取容器 --ss 变量随画布宽度等比缩放，与背景图座位同大小；--ss 未注入时回落 40px */
  width: var(--ss, 40px);
  height: var(--ss, 40px);
  margin-left: calc(var(--ss, 40px) / -2);
  margin-top: calc(var(--ss, 40px) / -2);
  border-radius: 50%;
  background: #fff;
  border: 2px solid #dcdfe6;
  box-shadow: 0 1px 4px rgba(0,0,0,0.15);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.15s;
}
.canvas-seat:hover { transform: scale(1.12); z-index: 5; }
.canvas-seat.is-assigned { border-color: #ff9800; background: #fff8e7; }
.canvas-seat.is-static-preset { border-color: #ff9800; border-style: dashed; }
.canvas-avatar { width: calc(var(--ss, 40px) * 0.7); height: calc(var(--ss, 40px) * 0.7); border-radius: 50%; object-fit: cover; }
.canvas-icon { font-size: calc(var(--ss, 40px) * 0.45); line-height: 1; }
.canvas-icon.plus { color: #409eff; }
.canvas-icon.assigned { color: #ff9800; }
.canvas-seat-no { font-size: calc(var(--ss, 40px) * 0.24); color: #909399; line-height: 1; margin-top: 1px; }

/* ===== ★T26.10 选人弹窗 ===== */
.picker-groups { max-height: 420px; overflow-y: auto; }
.picker-group { margin-bottom: 16px; }
.picker-group-title { font-size: 14px; font-weight: bold; color: #606266; margin-bottom: 8px; border-left: 3px solid #409eff; padding-left: 8px; }
.picker-persons { display: flex; flex-wrap: wrap; gap: 12px; }
.picker-person {
  width: 64px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 6px 2px;
  border-radius: 6px;
}
.picker-person:hover { background: #f0f7ff; }
.picker-person.disabled { opacity: 0.4; cursor: not-allowed; }
.picker-person.disabled:hover { background: transparent; }
.picker-avatar { width: 44px; height: 44px; border-radius: 50%; object-fit: cover; border: 1px solid #ebeef5; }
.picker-avatar.placeholder { display: flex; align-items: center; justify-content: center; background: #f5f7fa; font-size: 22px; }
.picker-name { font-size: 12px; color: #606266; text-align: center; max-width: 64px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.picker-empty { color: #c0c4cc; font-size: 12px; padding: 8px 0; }
</style>
