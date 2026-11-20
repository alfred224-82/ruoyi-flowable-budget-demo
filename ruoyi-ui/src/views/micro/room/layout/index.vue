<template>
  <div class="app-container room-layout-page">
    <el-alert
      title="房间座位布局配置（拖拽定位）"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom:12px;">
      <template slot="title">
        上传房间平面图作为背景，在图上拖拽落位座位点，保存后顾客小程序可按位置选座（T26.7）。坐标以背景图百分比存储，PC/手机/平板三端渲染一致。
      </template>
    </el-alert>

    <!-- 顶部：房间选择 + 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="8">
        <el-select
          v-model="currentRoomId"
          placeholder="请选择房间"
          filterable
          style="width:100%;"
          @change="handleRoomChange">
          <el-option
            v-for="room in roomList"
            :key="room.id"
            :label="room.name + (room.hasSeatLayout === 1 ? ' ✓已配置' : '')"
            :value="room.id" />
        </el-select>
      </el-col>
      <el-col :span="16" style="text-align:right;">
        <el-button
          type="primary"
          plain
          icon="el-icon-picture-outline"
          size="mini"
          :disabled="!currentRoomId"
          @click="triggerUpload"
          v-hasPermi="['micro:room:uploadBg']">上传背景图</el-button>
        <el-button
          type="success"
          plain
          icon="el-icon-document-copy"
          size="mini"
          :disabled="!currentRoomId"
          @click="handleCopyLayout"
          v-hasPermi="['micro:room:copyLayout']">复制布局</el-button>
        <el-button
          type="warning"
          plain
          icon="el-icon-collection"
          size="mini"
          :disabled="!currentRoomId || !layout.bgImageUrl"
          @click="handleSaveAsTemplate"
          v-hasPermi="['micro:room:saveTemplate']">存为模板</el-button>
        <el-button
          type="info"
          plain
          icon="el-icon-magic-stick"
          size="mini"
          :disabled="!currentRoomId"
          @click="handleApplyTemplate">应用模板</el-button>
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="!layout.id"
          @click="handleDeleteLayout"
          v-hasPermi="['micro:room:layout']">删除布局</el-button>
        <input
          ref="bgFileInput"
          type="file"
          accept="image/jpeg,image/png,image/webp"
          style="display:none;"
          @change="handleBgFileChange" />
      </el-col>
    </el-row>

    <el-row :gutter="12">
      <!-- 左侧：画布 -->
      <el-col :span="17">
        <el-card shadow="never" class="canvas-card">
          <div slot="header" class="canvas-header">
            <span>座位布局画布</span>
            <span class="header-hint">
              <i class="el-icon-info"></i>
              点击画布添加座位 · 拖拽调整位置 · 双击座位编辑 · 右键座位删除
            </span>
          </div>
          <div
            v-if="!layout.bgImageUrl"
            class="empty-canvas">
            <i class="el-icon-picture-outline" style="font-size:64px;color:#c0c4cc;"></i>
            <p style="color:#909399;margin-top:12px;">请先选择房间并上传背景图</p>
          </div>
          <div
            v-else
            ref="canvasWrap"
            class="canvas-wrap"
            :style="canvasStyle"
            @click.self="handleCanvasClick"
            @contextmenu.prevent>
            <img :src="layout.bgImageUrl" class="canvas-bg" draggable="false" @load="handleBgLoaded" />
            <div
              v-for="(seat, idx) in layout.seats"
              :key="seat._uid || idx"
              class="seat-point"
              :class="{ 'seat-preset': seat.isPreset === 1, 'seat-selected': selectedSeatIdx === idx }"
              :style="seatStyle(seat)"
              @mousedown.stop="startDrag(idx, $event)"
              @dblclick.stop="handleEditSeat(idx)"
              @contextmenu.prevent.stop="handleRemoveSeat(idx)">
              <span class="seat-label">{{ seat.seatNo }}</span>
            </div>
          </div>
          <div class="canvas-footer">
            <span>座位总数：<b>{{ layout.seats.length }}</b></span>
            <span style="margin-left:16px;">预置人员：<b>{{ presetCount }}</b></span>
            <span style="margin-left:16px;">背景来源：
              <el-tag size="mini" :type="layout.bgImageSource === 'PRESET' ? 'warning' : 'success'">
                {{ layout.bgImageSource === 'PRESET' ? '平台默认模板' : 'Boss 自传' }}
              </el-tag>
            </span>
            <span v-if="layout.templateName" style="margin-left:16px;">模板：{{ layout.templateName }}</span>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：座位列表 + 保存 -->
      <el-col :span="7">
        <el-card shadow="never" class="seat-list-card">
          <div slot="header">
            <span>座位列表</span>
            <el-button
              type="text"
              size="mini"
              icon="el-icon-plus"
              style="float:right;padding:0;"
              :disabled="!layout.bgImageUrl"
              @click="handleAddSeatCenter">添加座位</el-button>
          </div>
          <el-table
            :data="layout.seats"
            size="mini"
            height="420"
            highlight-current-row
            @current-change="handleSeatRowSelect">
            <el-table-column label="座位号" prop="seatNo" width="90" />
            <el-table-column label="X%" width="70" align="center">
              <template slot-scope="scope">{{ (scope.row.posXPercent * 100).toFixed(1) }}</template>
            </el-table-column>
            <el-table-column label="Y%" width="70" align="center">
              <template slot-scope="scope">{{ (scope.row.posYPercent * 100).toFixed(1) }}</template>
            </el-table-column>
            <el-table-column label="预置" width="70" align="center">
              <template slot-scope="scope">
                <el-tag v-if="scope.row.isPreset === 1" size="mini" type="warning">{{ scope.row.presetRole }}</el-tag>
                <span v-else>—</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template slot-scope="scope">
                <el-button type="text" size="mini" icon="el-icon-edit" @click="handleEditSeat(scope.$index)" />
                <el-button type="text" size="mini" icon="el-icon-delete" style="color:#f56c6c;" @click="handleRemoveSeat(scope.$index)" />
              </template>
            </el-table-column>
          </el-table>
          <div style="margin-top:12px;text-align:center;">
            <el-button
              type="primary"
              icon="el-icon-check"
              :loading="saving"
              :disabled="!currentRoomId || !layout.bgImageUrl"
              @click="handleSave"
              v-hasPermi="['micro:room:layout']">保存布局</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 座位编辑弹窗 -->
    <el-dialog title="编辑座位" :visible.sync="seatDialogVisible" width="480px" append-to-body>
      <el-form ref="seatForm" :model="seatForm" :rules="seatRules" label-width="100px">
        <el-form-item label="座位号" prop="seatNo">
          <el-input v-model="seatForm.seatNo" placeholder="如：A1 / 1 / 前排-左1" />
        </el-form-item>
        <el-form-item label="预置人员">
          <el-switch v-model="seatForm.isPreset" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item v-if="seatForm.isPreset === 1" label="预置角色" prop="presetRole">
          <el-select v-model="seatForm.presetRole" placeholder="请选择" style="width:100%;">
            <el-option label="导游" value="guide" />
            <el-option label="司机" value="driver" />
            <el-option label="助理" value="assistant" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="seatForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="seatDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitSeatForm">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 复制布局弹窗 -->
    <el-dialog title="从其他房间复制布局" :visible.sync="copyDialogVisible" width="480px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="源房间">
          <el-select v-model="copySourceRoomId" placeholder="请选择已配置布局的房间" filterable style="width:100%;">
            <el-option
              v-for="room in roomList.filter(r => r.hasSeatLayout === 1 && r.id !== currentRoomId)"
              :key="room.id"
              :label="room.name"
              :value="room.id" />
          </el-select>
        </el-form-item>
        <el-alert
          title="复制后目标房间将拥有独立的布局记录；源房间后续修改不影响目标（脱钩原则）。"
          type="warning"
          :closable="false"
          show-icon />
      </el-form>
      <div slot="footer">
        <el-button @click="copyDialogVisible = false">取 消</el-button>
        <el-button type="primary" :disabled="!copySourceRoomId" @click="submitCopyLayout">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 存为模板弹窗 -->
    <el-dialog title="存为布局模板" :visible.sync="tplDialogVisible" width="420px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="模板名称" required>
          <el-input v-model="templateName" placeholder="如：标准包厢 8 人位" maxlength="100" show-word-limit />
        </el-form-item>
        <el-alert
          title="模板仅本租户可见（Boss 私有）；平台默认模板由平台 Admin 在「原子资源管理 → 默认房间模板」维护。"
          type="info"
          :closable="false"
          show-icon />
      </el-form>
      <div slot="footer">
        <el-button @click="tplDialogVisible = false">取 消</el-button>
        <el-button type="primary" :disabled="!templateName" @click="submitSaveAsTemplate">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 应用模板弹窗 -->
    <el-dialog title="应用布局模板" :visible.sync="applyDialogVisible" width="600px" append-to-body>
      <el-table :data="visibleTemplates" size="mini" height="360" highlight-current-row @current-change="row => selectedTemplate = row">
        <el-table-column label="模板名称" prop="name" min-width="140" />
        <el-table-column label="类型" width="120" align="center">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.isIndustryDefault === 1 ? 'warning' : 'success'">
              {{ scope.row.isIndustryDefault === 1 ? '平台默认' : 'Boss 私有' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="行业" prop="industryName" width="100" align="center" />
        <el-table-column label="预览" width="100" align="center">
          <template slot-scope="scope">
            <el-image
              v-if="scope.row.bgImageUrl"
              :src="scope.row.bgImageUrl"
              :preview-src-list="[scope.row.bgImageUrl]"
              style="width:60px;height:40px;"
              fit="cover" />
            <span v-else>—</span>
          </template>
        </el-table-column>
      </el-table>
      <div slot="footer">
        <el-button @click="applyDialogVisible = false">取 消</el-button>
        <el-button type="primary" :disabled="!selectedTemplate" @click="submitApplyTemplate">应 用</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  uploadBgImage, getRoomLayout, saveRoomLayout, deleteRoomLayout,
  copyLayoutFromRoom, saveLayoutAsTemplate, listVisibleTemplates, applyRoomTemplate
} from '@/api/micro/roomLayout'
import { listPlatformRoom } from '@/api/micro/platformRoom'

let uidSeed = 0
const nextUid = () => `_s${Date.now()}_${++uidSeed}`

export default {
  name: 'MicroRoomLayout',
  data() {
    return {
      roomList: [],
      currentRoomId: null,
      layout: this.emptyLayout(),
      selectedSeatIdx: -1,
      saving: false,
      // 座位编辑
      seatDialogVisible: false,
      seatEditIdx: -1,
      seatForm: { seatNo: '', isPreset: 0, presetRole: null, remark: '' },
      seatRules: {
        seatNo: [{ required: true, message: '座位号不能为空', trigger: 'blur' }]
      },
      // 复制布局
      copyDialogVisible: false,
      copySourceRoomId: null,
      // 存为模板
      tplDialogVisible: false,
      templateName: '',
      // 应用模板
      applyDialogVisible: false,
      visibleTemplates: [],
      selectedTemplate: null,
      // 拖拽状态
      dragState: null,
      // ★座位点圆点直径（px）：按座位点最小邻距随画布宽度等比换算，与背景图座位同大小
      seatPointSize: 0
    }
  },
  computed: {
    canvasStyle() {
      const w = this.layout.layoutWidth || 1920
      const h = this.layout.layoutHeight || 1080
      // 按 16:9 自适应，最大宽 100%
      const style = {
        aspectRatio: `${w} / ${h}`,
        maxWidth: '100%',
        backgroundImage: `url(${this.layout.bgImageUrl})`,
        // ★注入 --ss 变量：座位点圆点直径随画布宽度等比缩放，与背景图座位同大小
        '--ss': (this.seatPointSize || 28) + 'px'
      }
      // M27 T27.3：PRESET 默认图（透明底线条 SVG）按主题主色 20% 铺底 + 内描边；UPLOAD 自传图不叠加滤镜
      if (this.layout.bgImageSource === 'PRESET') {
        style.backgroundColor = 'var(--micro-theme-primary-soft, rgba(74,144,226,0.2))'
        style.boxShadow = 'inset 0 0 0 2px var(--micro-theme-primary-soft, rgba(74,144,226,0.2))'
      }
      return style
    },
    presetCount() {
      return this.layout.seats.filter(s => s.isPreset === 1).length
    }
  },
  created() {
    this.loadRooms()
    // M27 T27.3 / T27.4：加载租户主题到 store（写入 :root CSS 变量，供 PRESET 默认图铺底消费）
    this.$store.dispatch('theme/loadCurrentTheme')
  },
  mounted() {
    // ★窗口缩放时画布宽度变化，圆点直径需重新换算
    window.addEventListener('resize', this.measureSeatPoint)
  },
  beforeDestroy() {
    this.removeDragListeners()
    window.removeEventListener('resize', this.measureSeatPoint)
  },
  methods: {
    emptyLayout() {
      return {
        id: null,
        roomId: null,
        bgImageUrl: '',
        bgImageSource: 'UPLOAD',
        layoutWidth: 1920,
        layoutHeight: 1080,
        templateId: null,
        templateName: null,
        seats: []
      }
    },
    loadRooms() {
      // 复用 platform_admin 房间列表 API（Boss 亦可见本租户房间）
      listPlatformRoom({ pageNum: 1, pageSize: 200, status: '0' }).then(res => {
        this.roomList = res.rows || []
      }).catch(() => { this.roomList = [] })
    },
    handleRoomChange(roomId) {
      this.selectedSeatIdx = -1
      if (!roomId) {
        this.layout = this.emptyLayout()
        return
      }
      getRoomLayout(roomId).then(res => {
        if (res.data) {
          const data = res.data
          data.seats = (data.seats || []).map(s => ({ ...s, _uid: nextUid() }))
          this.layout = data
        } else {
          this.layout = this.emptyLayout()
          this.layout.roomId = roomId
        }
        this.$nextTick(() => this.measureSeatPoint())
      }).catch(() => {
        this.layout = this.emptyLayout()
        this.layout.roomId = roomId
      })
    },
    // ========== 背景图上传 ==========
    triggerUpload() {
      this.$refs.bgFileInput.click()
    },
    handleBgFileChange(e) {
      const file = e.target.files[0]
      if (!file) return
      // 前置校验：大小 ≤2MB
      if (file.size > 2 * 1024 * 1024) {
        this.$modal.msgError('图片大小不能超过 2MB')
        e.target.value = ''
        return
      }
      const loading = this.$loading({ lock: true, text: '上传中...' })
      uploadBgImage(file).then(res => {
        this.layout.bgImageUrl = res.data.url
        this.layout.bgImageSource = 'UPLOAD'
        this.$modal.msgSuccess('上传成功')
      }).catch(err => {
        this.$modal.msgError(err.message || '上传失败')
      }).finally(() => {
        loading.close()
        e.target.value = ''
      })
    },
    handleBgLoaded(e) {
      // 记录自然尺寸作为坐标换算基准（首次上传时）
      const img = e.target
      if (!this.layout.id && img.naturalWidth > 0) {
        this.layout.layoutWidth = img.naturalWidth
        this.layout.layoutHeight = img.naturalHeight
      }
      this.$nextTick(() => this.measureSeatPoint())
    },
    /** ★校准圆点直径：= 座位点最小邻距（百分比坐标换算为画布宽比例）× 0.85 ≈ 背景图座位方块大小；
     * 座位点由 Boss 沿图中座位落位，最小邻距即座位间距，随画布宽度等比缩放保证圆点与座位同大小 */
    measureSeatPoint() {
      const el = this.$refs.canvasWrap
      if (!el || !el.clientWidth) return
      const seats = this.layout.seats || []
      // 百分比坐标各向异性：Y 方向按布局高宽比折算为宽度比例
      const aspect = (this.layout.layoutHeight || 1080) / (this.layout.layoutWidth || 1920)
      let minDist = 0
      for (let i = 0; i < seats.length; i++) {
        for (let j = i + 1; j < seats.length; j++) {
          const dx = parseFloat(seats[i].posXPercent || 0) - parseFloat(seats[j].posXPercent || 0)
          const dy = (parseFloat(seats[i].posYPercent || 0) - parseFloat(seats[j].posYPercent || 0)) * aspect
          const dist = Math.sqrt(dx * dx + dy * dy)
          if (dist > 0 && (minDist === 0 || dist < minDist)) minDist = dist
        }
      }
      // 单点回落 4%；上限 6% 防个别异常间距撑大圆点；下限 12px 保可拖拽
      const ratio = minDist > 0 ? minDist * 0.85 : 0.04
      this.seatPointSize = Math.max(12, Math.round(el.clientWidth * Math.min(ratio, 0.06)))
    },
    // ========== 座位增删改 ==========
    handleCanvasClick(e) {
      if (!this.layout.bgImageUrl) return
      const rect = this.$refs.canvasWrap.getBoundingClientRect()
      const x = (e.clientX - rect.left) / rect.width
      const y = (e.clientY - rect.top) / rect.height
      this.addSeatAt(x, y)
    },
    handleAddSeatCenter() {
      this.addSeatAt(0.5, 0.5)
    },
    addSeatAt(x, y) {
      const idx = this.layout.seats.length + 1
      const seatNo = this.generateSeatNo(idx)
      this.layout.seats.push({
        _uid: nextUid(),
        id: null,
        seatNo,
        posXPercent: Number(x.toFixed(4)),
        posYPercent: Number(y.toFixed(4)),
        isPreset: 0,
        presetRole: null,
        presetUserId: null,
        remark: ''
      })
      this.selectedSeatIdx = this.layout.seats.length - 1
      this.$nextTick(() => this.measureSeatPoint())
    },
    generateSeatNo(idx) {
      // 简单递增：A1, A2, ..., A26, B1, ...
      const letter = String.fromCharCode(64 + Math.ceil(idx / 26))
      const num = ((idx - 1) % 26) + 1
      let candidate = `${letter}${num}`
      let suffix = 1
      while (this.layout.seats.some(s => s.seatNo === candidate)) {
        candidate = `${letter}${num}_${suffix++}`
      }
      return candidate
    },
    handleEditSeat(idx) {
      this.seatEditIdx = idx
      const s = this.layout.seats[idx]
      this.seatForm = {
        seatNo: s.seatNo,
        isPreset: s.isPreset || 0,
        presetRole: s.presetRole,
        remark: s.remark || ''
      }
      this.seatDialogVisible = true
      this.$nextTick(() => this.$refs.seatForm && this.$refs.seatForm.clearValidate())
    },
    submitSeatForm() {
      this.$refs.seatForm.validate(valid => {
        if (!valid) return
        // 座位号唯一性预校验（Service 层 + DB UNIQUE 兜底）
        const dup = this.layout.seats.some((s, i) => i !== this.seatEditIdx && s.seatNo === this.seatForm.seatNo)
        if (dup) {
          this.$modal.msgError('座位号重复：' + this.seatForm.seatNo)
          return
        }
        const s = this.layout.seats[this.seatEditIdx]
        s.seatNo = this.seatForm.seatNo
        s.isPreset = this.seatForm.isPreset
        s.presetRole = this.seatForm.isPreset === 1 ? this.seatForm.presetRole : null
        s.remark = this.seatForm.remark
        this.seatDialogVisible = false
      })
    },
    handleRemoveSeat(idx) {
      this.$modal.confirm(`确认删除座位「${this.layout.seats[idx].seatNo}」？`).then(() => {
        this.layout.seats.splice(idx, 1)
        if (this.selectedSeatIdx === idx) this.selectedSeatIdx = -1
        else if (this.selectedSeatIdx > idx) this.selectedSeatIdx--
        this.$nextTick(() => this.measureSeatPoint())
      }).catch(() => {})
    },
    handleSeatRowSelect(row) {
      if (!row) { this.selectedSeatIdx = -1; return }
      this.selectedSeatIdx = this.layout.seats.findIndex(s => s._uid === row._uid)
    },
    // ========== 拖拽 ==========
    seatStyle(seat) {
      return {
        left: `${seat.posXPercent * 100}%`,
        top: `${seat.posYPercent * 100}%`
      }
    },
    startDrag(idx, e) {
      this.selectedSeatIdx = idx
      const wrap = this.$refs.canvasWrap
      const rect = wrap.getBoundingClientRect()
      this.dragState = {
        idx,
        startX: e.clientX,
        startY: e.clientY,
        rect,
        origX: this.layout.seats[idx].posXPercent,
        origY: this.layout.seats[idx].posYPercent
      }
      document.addEventListener('mousemove', this.onDragMove)
      document.addEventListener('mouseup', this.onDragEnd)
    },
    onDragMove(e) {
      if (!this.dragState) return
      const { idx, rect, origX, origY, startX, startY } = this.dragState
      const dx = (e.clientX - startX) / rect.width
      const dy = (e.clientY - startY) / rect.height
      let nx = origX + dx
      let ny = origY + dy
      // 边界约束 0~1
      nx = Math.max(0, Math.min(1, nx))
      ny = Math.max(0, Math.min(1, ny))
      this.$set(this.layout.seats[idx], 'posXPercent', Number(nx.toFixed(4)))
      this.$set(this.layout.seats[idx], 'posYPercent', Number(ny.toFixed(4)))
    },
    onDragEnd() {
      this.dragState = null
      this.removeDragListeners()
      // ★拖拽改变座位间距后重新校准圆点直径
      this.measureSeatPoint()
    },
    removeDragListeners() {
      document.removeEventListener('mousemove', this.onDragMove)
      document.removeEventListener('mouseup', this.onDragEnd)
    },
    // ========== 保存 ==========
    handleSave() {
      if (!this.currentRoomId) { this.$modal.msgError('请先选择房间'); return }
      if (!this.layout.bgImageUrl) { this.$modal.msgError('请先上传背景图'); return }
      if (this.layout.seats.length === 0) {
        this.$modal.msgWarning('尚未添加任何座位点')
      }
      // 座位号唯一性预校验
      const seatNos = this.layout.seats.map(s => s.seatNo)
      const dup = seatNos.find((n, i) => seatNos.indexOf(n) !== i)
      if (dup) { this.$modal.msgError('座位号重复：' + dup); return }

      const payload = {
        id: this.layout.id,
        roomId: this.currentRoomId,
        bgImageUrl: this.layout.bgImageUrl,
        bgImageSource: this.layout.bgImageSource || 'UPLOAD',
        layoutWidth: this.layout.layoutWidth || 1920,
        layoutHeight: this.layout.layoutHeight || 1080,
        templateId: this.layout.templateId,
        templateName: this.layout.templateName,
        seats: this.layout.seats.map(s => ({
          id: s.id,
          seatNo: s.seatNo,
          posXPercent: s.posXPercent,
          posYPercent: s.posYPercent,
          isPreset: s.isPreset || 0,
          presetRole: s.presetRole,
          presetUserId: s.presetUserId,
          remark: s.remark
        }))
      }
      this.saving = true
      saveRoomLayout(payload).then(res => {
        this.layout.id = res.data
        this.$modal.msgSuccess(`保存成功，共 ${payload.seats.length} 个座位`)
        this.loadRooms() // 刷新 has_seat_layout 标记
      }).catch(err => {
        this.$modal.msgError(err.message || '保存失败')
      }).finally(() => { this.saving = false })
    },
    handleDeleteLayout() {
      if (!this.layout.id) return
      this.$modal.confirm('删除布局将同时清除所有座位点，房间回退为「无位置」状态。确认删除？').then(() => {
        return deleteRoomLayout(this.layout.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.layout = this.emptyLayout()
        this.layout.roomId = this.currentRoomId
        this.loadRooms()
      }).catch(() => {})
    },
    // ========== 复制布局 ==========
    handleCopyLayout() {
      this.copySourceRoomId = null
      this.copyDialogVisible = true
    },
    submitCopyLayout() {
      copyLayoutFromRoom({
        sourceRoomId: this.copySourceRoomId,
        targetRoomId: this.currentRoomId
      }).then(() => {
        this.$modal.msgSuccess('复制成功')
        this.copyDialogVisible = false
        this.handleRoomChange(this.currentRoomId)
        this.loadRooms()
      }).catch(err => this.$modal.msgError(err.message || '复制失败'))
    },
    // ========== 存为模板 ==========
    handleSaveAsTemplate() {
      if (!this.layout.id) {
        this.$modal.msgWarning('请先保存布局后再存为模板')
        return
      }
      this.templateName = ''
      this.tplDialogVisible = true
    },
    submitSaveAsTemplate() {
      saveLayoutAsTemplate({
        layoutId: this.layout.id,
        templateName: this.templateName
      }).then(() => {
        this.$modal.msgSuccess('已存为模板')
        this.tplDialogVisible = false
      }).catch(err => this.$modal.msgError(err.message || '存为模板失败'))
    },
    // ========== 应用模板 ==========
    handleApplyTemplate() {
      this.selectedTemplate = null
      listVisibleTemplates().then(res => {
        this.visibleTemplates = res.data || []
        if (this.visibleTemplates.length === 0) {
          this.$modal.msgWarning('暂无可用模板，请先存为模板或联系平台 Admin 配置默认模板')
          return
        }
        this.applyDialogVisible = true
      })
    },
    submitApplyTemplate() {
      if (!this.selectedTemplate) return
      this.$modal.confirm(`应用模板「${this.selectedTemplate.name}」将覆盖当前房间的布局（若已有）。确认？`).then(() => {
        return applyRoomTemplate(this.currentRoomId, this.selectedTemplate.id)
      }).then(() => {
        this.$modal.msgSuccess('应用成功，请拖拽落位座位点后保存')
        this.applyDialogVisible = false
        this.handleRoomChange(this.currentRoomId)
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.room-layout-page {
  .canvas-card, .seat-list-card {
    min-height: 560px;
  }
  .canvas-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    .header-hint {
      font-size: 12px;
      color: #909399;
      font-weight: normal;
    }
  }
  .empty-canvas {
    height: 480px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    background: #f5f7fa;
    border: 1px dashed #dcdfe6;
    border-radius: 4px;
  }
  .canvas-wrap {
    position: relative;
    width: 100%;
    background-size: contain;
    background-repeat: no-repeat;
    background-position: center;
    background-color: #fafafa;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    cursor: crosshair;
    user-select: none;
    overflow: hidden;
    .canvas-bg {
      display: block;
      width: 100%;
      height: auto;
      pointer-events: none;
    }
  }
  .seat-point {
    position: absolute;
    /* ★直径取容器 --ss 变量随画布宽度等比缩放，与背景图座位同大小；--ss 未注入时回落 28px */
    width: var(--ss, 28px);
    height: var(--ss, 28px);
    margin-left: calc(var(--ss, 28px) / -2);
    margin-top: calc(var(--ss, 28px) / -2);
    border-radius: 50%;
    background: #409eff;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: calc(var(--ss, 28px) * 0.38);
    cursor: move;
    box-shadow: 0 2px 6px rgba(0,0,0,0.25);
    border: 2px solid #fff;
    transition: transform 0.1s;
    &:hover { transform: scale(1.15); z-index: 10; }
    &.seat-preset { background: #e6a23c; }
    &.seat-selected { background: #f56c6c; transform: scale(1.2); z-index: 11; }
    .seat-label { pointer-events: none; }
  }
  .canvas-footer {
    margin-top: 10px;
    padding: 8px 12px;
    background: #f5f7fa;
    border-radius: 4px;
    font-size: 13px;
    color: #606266;
  }
}
</style>
