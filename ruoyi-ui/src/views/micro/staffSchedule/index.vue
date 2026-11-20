<template>
  <div class="app-container">
    <el-alert title="员工排班管理（T22.3，★FEAT_STAFF_SCHEDULE 功能开关）" type="info"
      description="按周/月批量编排员工排班，班次分早班/晚班，起止时间自定义。草稿发布后员工/顾客可见；发布后仍可修正（写留痕日志）。"
      show-icon :closable="false" class="mb16" />

    <!-- ===== 顶部工具栏 ===== -->
    <el-card shadow="never" class="mb16">
      <el-form :inline="true" size="small">
        <el-form-item label="周期类型">
          <el-radio-group v-model="query.periodType" @change="onPeriodTypeChange">
            <el-radio-button label="week">按周</el-radio-button>
            <el-radio-button label="month">按月</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="周期">
          <el-select v-model="query.periodKey" placeholder="请选择周期" style="width:180px;" @change="loadSchedules">
            <el-option v-for="p in periodOptions" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="员工">
          <el-select v-model="query.staffId" placeholder="全部员工" clearable style="width:160px;" @change="loadSchedules">
            <el-option v-for="s in staffOptions" :key="s.userId" :label="s.nickName" :value="s.userId">
              <el-avatar v-if="s.avatar" :size="20" :src="s.avatar" style="margin-right:6px;vertical-align:middle;" />
              <span>{{ s.nickName }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:120px;" @change="loadSchedules">
            <el-option label="草稿" value="draft" />
            <el-option label="已发布" value="published" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-refresh" @click="loadSchedules">刷新</el-button>
          <el-button type="success" icon="el-icon-plus" v-hasPermi="['micro:staffSchedule:create']"
            @click="openBatchDialog">批量创建</el-button>
          <el-button type="warning" icon="el-icon-upload2" v-hasPermi="['micro:staffSchedule:publish']"
            :disabled="!hasDraft" @click="handlePublish">发布本期</el-button>
          <el-button icon="el-icon-time" v-hasPermi="['micro:staffSchedule:edit']"
            @click="openShiftTimeDialog">班次时间设定</el-button>
          <el-button icon="el-icon-document" @click="openLogDialog">修正日志</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ===== 排班日历（行=员工，列=日期） ===== -->
    <el-card shadow="never" v-loading="loading">
      <div slot="header">
        <span>排班日历 - {{ currentPeriodLabel }}</span>
        <el-tag v-if="hasDraft" type="warning" size="mini" style="margin-left:8px;">含草稿</el-tag>
      </div>
      <el-table :data="calendarRows" border stripe style="width:100%;" :span-method="spanMethod">
        <el-table-column label="员工" align="center" width="140" fixed>
          <template slot-scope="scope">
            <div style="display:flex;align-items:center;justify-content:center;">
              <el-avatar v-if="scope.row.avatar" :size="28" :src="scope.row.avatar" style="margin-right:6px;" />
              <el-avatar v-else :size="28" icon="el-icon-user-solid" style="margin-right:6px;" />
              <span>{{ scope.row.nickName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column v-for="d in dateColumns" :key="d.date" :label="d.label" align="center" min-width="110">
          <template slot-scope="scope">
            <div class="cell-schedules">
              <div v-for="sch in scope.row.schedules[d.date]" :key="sch.id"
                class="schedule-chip" :class="'chip-' + sch.status">
                <el-tag :type="sch.shiftType === 'morning' ? '' : 'warning'" size="mini" effect="plain">
                  {{ sch.shiftType === 'morning' ? '早' : '晚' }}
                </el-tag>
                <span class="chip-time">{{ formatTime(sch.startTime) }}-{{ formatTime(sch.endTime) }}</span>
                <el-button v-if="sch.status === 'draft'" type="text" size="mini" icon="el-icon-delete"
                  v-hasPermi="['micro:staffSchedule:edit']" @click="handleDeleteDraft(sch)" />
                <el-button v-else type="text" size="mini" icon="el-icon-edit"
                  v-hasPermi="['micro:staffSchedule:edit']" @click="openModifyDialog(sch)" />
              </div>
              <el-button v-if="!scope.row.schedules[d.date] || scope.row.schedules[d.date].length === 0"
                type="text" size="mini" icon="el-icon-plus" class="add-btn"
                v-hasPermi="['micro:staffSchedule:create']"
                @click="openQuickAdd(scope.row, d.date)">添加</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && calendarRows.length === 0" description="暂无员工，请先在「用户管理」中创建 salesman 角色用户" />
    </el-card>

    <!-- ===== 批量创建对话框 ===== -->
    <el-dialog title="批量创建排班" :visible.sync="batchDialog.visible" width="900px" append-to-body>
      <el-form :model="batchDialog.form" label-width="100px" size="small">
        <el-form-item label="周期">
          <el-tag>{{ batchDialog.form.periodType === 'week' ? '周' : '月' }} - {{ batchDialog.form.periodKey }}</el-tag>
        </el-form-item>
        <el-form-item label="选择员工">
          <el-select v-model="batchDialog.form.staffIds" multiple placeholder="请选择员工" style="width:100%;">
            <el-option v-for="s in staffOptions" :key="s.userId" :label="s.nickName" :value="s.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择日期">
          <el-checkbox-group v-model="batchDialog.form.dates">
            <el-checkbox v-for="d in batchDialog.dateOptions" :key="d" :label="d">{{ d }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="班次">
          <el-checkbox-group v-model="batchDialog.form.shifts">
            <el-checkbox label="morning">早班</el-checkbox>
            <el-checkbox label="evening">晚班</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="早班时间">
          <el-time-picker v-model="batchDialog.form.morningStart" format="HH:mm" value-format="HH:mm:ss"
            placeholder="开始" style="width:120px;" />
          <span style="margin:0 8px;">至</span>
          <el-time-picker v-model="batchDialog.form.morningEnd" format="HH:mm" value-format="HH:mm:ss"
            placeholder="结束" style="width:120px;" />
        </el-form-item>
        <el-form-item label="晚班时间">
          <el-time-picker v-model="batchDialog.form.eveningStart" format="HH:mm" value-format="HH:mm:ss"
            placeholder="开始" style="width:120px;" />
          <span style="margin:0 8px;">至</span>
          <el-time-picker v-model="batchDialog.form.eveningEnd" format="HH:mm" value-format="HH:mm:ss"
            placeholder="结束" style="width:120px;" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="batchDialog.visible = false">取消</el-button>
        <el-button size="small" type="primary" :loading="batchDialog.submitting" @click="submitBatchCreate">
          生成 {{ batchPreviewCount }} 条草稿
        </el-button>
      </div>
    </el-dialog>

    <!-- ===== 修正对话框 ===== -->
    <el-dialog title="修正排班" :visible.sync="modifyDialog.visible" width="500px" append-to-body>
      <el-form :model="modifyDialog.form" label-width="100px" size="small">
        <el-form-item label="员工">
          <span>{{ modifyDialog.staffName }}</span>
        </el-form-item>
        <el-form-item label="日期">
          <span>{{ modifyDialog.form.scheduleDate }}</span>
        </el-form-item>
        <el-form-item label="班次">
          <el-radio-group v-model="modifyDialog.form.shiftType">
            <el-radio label="morning">早班</el-radio>
            <el-radio label="evening">晚班</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="起止时间">
          <el-time-picker v-model="modifyDialog.form.startTime" format="HH:mm" value-format="HH:mm:ss"
            placeholder="开始" style="width:120px;" />
          <span style="margin:0 8px;">至</span>
          <el-time-picker v-model="modifyDialog.form.endTime" format="HH:mm" value-format="HH:mm:ss"
            placeholder="结束" style="width:120px;" />
        </el-form-item>
        <el-form-item label="修正原因" required>
          <el-input v-model="modifyDialog.form.changeReason" type="textarea" :rows="2" placeholder="必填，将写入留痕日志" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="modifyDialog.form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="modifyDialog.visible = false">取消</el-button>
        <el-button size="small" type="primary" :loading="modifyDialog.submitting" @click="submitModify">保存修正</el-button>
      </div>
    </el-dialog>

    <!-- ===== 班次时间设定对话框 ===== -->
    <el-dialog title="班次时间设定" :visible.sync="shiftTimeDialog.visible" width="500px" append-to-body>
      <el-alert type="warning" :closable="false" show-icon class="mb16"
        title="批量更新本期所有对应班次记录的起止时间（不写留痕日志，仅调整时间）" />
      <el-form :model="shiftTimeDialog.form" label-width="100px" size="small">
        <el-form-item label="周期">
          <el-tag>{{ shiftTimeDialog.form.periodType === 'week' ? '周' : '月' }} - {{ shiftTimeDialog.form.periodKey }}</el-tag>
        </el-form-item>
        <el-form-item label="班次类型">
          <el-radio-group v-model="shiftTimeDialog.form.shiftType">
            <el-radio label="morning">早班</el-radio>
            <el-radio label="evening">晚班</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-time-picker v-model="shiftTimeDialog.form.startTime" format="HH:mm" value-format="HH:mm:ss" style="width:160px;" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-time-picker v-model="shiftTimeDialog.form.endTime" format="HH:mm" value-format="HH:mm:ss" style="width:160px;" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="shiftTimeDialog.visible = false">取消</el-button>
        <el-button size="small" type="primary" :loading="shiftTimeDialog.submitting" @click="submitShiftTime">应用</el-button>
      </div>
    </el-dialog>

    <!-- ===== 修正日志对话框 ===== -->
    <el-dialog title="排班修正日志" :visible.sync="logDialog.visible" width="900px" append-to-body>
      <el-table :data="logDialog.list" v-loading="logDialog.loading" border stripe max-height="500">
        <el-table-column label="时间" prop="operateTime" width="160" />
        <el-table-column label="员工" prop="staffName" width="100" />
        <el-table-column label="日期" prop="scheduleDate" width="110" />
        <el-table-column label="班次" width="70">
          <template slot-scope="scope">
            <el-tag :type="scope.row.shiftType === 'morning' ? '' : 'warning'" size="mini">
              {{ scope.row.shiftType === 'morning' ? '早' : '晚' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变更类型" width="90">
          <template slot-scope="scope">
            <el-tag size="mini" :type="logTypeColor(scope.row.changeType)">{{ logTypeLabel(scope.row.changeType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变更内容" min-width="180">
          <template slot-scope="scope">
            <span v-if="scope.row.changeType === 'TIME'">{{ scope.row.beforeTime }} → {{ scope.row.afterTime }}</span>
            <span v-else-if="scope.row.changeType === 'STATUS'">{{ scope.row.beforeStatus }} → {{ scope.row.afterStatus }}</span>
            <span v-else-if="scope.row.changeType === 'SHIFT'">班次调整</span>
            <span v-else>{{ scope.row.beforeTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="原因" prop="changeReason" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作人" prop="operatorName" width="100" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import {
  listStaffOptions, listSchedules, batchCreate, publishSchedule,
  modifySchedule, setShiftTime, deleteDraft, listChangeLogs
} from '@/api/micro/staffSchedule'

export default {
  name: 'StaffSchedule',
  data() {
    return {
      loading: false,
      query: {
        periodType: 'week',
        periodKey: '',
        status: '',
        staffId: null
      },
      staffOptions: [],
      schedules: [],
      periodOptions: [],
      // 批量创建对话框
      batchDialog: {
        visible: false,
        submitting: false,
        form: {
          periodType: 'week',
          periodKey: '',
          staffIds: [],
          dates: [],
          shifts: ['morning'],
          morningStart: '09:00:00',
          morningEnd: '18:00:00',
          eveningStart: '18:00:00',
          eveningEnd: '23:00:00'
        },
        dateOptions: []
      },
      // 修正对话框
      modifyDialog: {
        visible: false,
        submitting: false,
        staffName: '',
        form: {
          id: null,
          scheduleDate: '',
          shiftType: 'morning',
          startTime: '09:00:00',
          endTime: '18:00:00',
          changeReason: '',
          remark: ''
        }
      },
      // 班次时间设定对话框
      shiftTimeDialog: {
        visible: false,
        submitting: false,
        form: {
          periodType: 'week',
          periodKey: '',
          shiftType: 'morning',
          startTime: '09:00:00',
          endTime: '18:00:00'
        }
      },
      // 修正日志对话框
      logDialog: {
        visible: false,
        loading: false,
        list: []
      }
    }
  },
  computed: {
    // 当前周期标签
    currentPeriodLabel() {
      return this.query.periodKey || '未选择'
    },
    // 日期列（周=7 天，月=当月天数）
    dateColumns() {
      if (!this.query.periodKey) return []
      if (this.query.periodType === 'week') {
        return this.getWeekDates(this.query.periodKey)
      } else {
        return this.getMonthDates(this.query.periodKey)
      }
    },
    // 日历行（按员工分组）
    calendarRows() {
      const rows = []
      const staffMap = {}
      this.staffOptions.forEach(s => {
        staffMap[s.userId] = {
          userId: s.userId,
          nickName: s.nickName,
          avatar: s.avatar,
          schedules: {}
        }
        rows.push(staffMap[s.userId])
      })
      // 初始化每个日期的空数组
      const dates = this.dateColumns.map(d => d.date)
      rows.forEach(r => {
        dates.forEach(d => { r.schedules[d] = [] })
      })
      // 填充排班
      this.schedules.forEach(sch => {
        const row = staffMap[sch.staffId]
        if (row && row.schedules[sch.scheduleDate]) {
          row.schedules[sch.scheduleDate].push(sch)
        }
      })
      return rows
    },
    // 是否含草稿
    hasDraft() {
      return this.schedules.some(s => s.status === 'draft')
    },
    // 批量创建预览数量
    batchPreviewCount() {
      return this.batchDialog.form.staffIds.length
        * this.batchDialog.form.dates.length
        * this.batchDialog.form.shifts.length
    }
  },
  created() {
    this.initPeriodOptions()
    this.loadStaffOptions()
  },
  methods: {
    // ===== 周期工具 =====
    initPeriodOptions() {
      const opts = []
      const now = new Date()
      if (this.query.periodType === 'week') {
        // 生成本周 + 前后各 4 周
        for (let i = -4; i <= 4; i++) {
          const d = new Date(now.getTime() + i * 7 * 86400000)
          const iso = this.getIsoWeek(d)
          opts.push({ label: `${iso.year} 第 ${iso.week} 周`, value: `${iso.year}-W${String(iso.week).padStart(2, '0')}` })
        }
      } else {
        // 生成本月 + 前后各 3 月
        for (let i = -3; i <= 3; i++) {
          const d = new Date(now.getFullYear(), now.getMonth() + i, 1)
          const y = d.getFullYear()
          const m = String(d.getMonth() + 1).padStart(2, '0')
          opts.push({ label: `${y} 年 ${m} 月`, value: `${y}-${m}` })
        }
      }
      this.periodOptions = opts
      // 默认选当前周期
      const current = opts.find(o => {
        if (this.query.periodType === 'week') {
          const iso = this.getIsoWeek(now)
          return o.value === `${iso.year}-W${String(iso.week).padStart(2, '0')}`
        } else {
          const y = now.getFullYear()
          const m = String(now.getMonth() + 1).padStart(2, '0')
          return o.value === `${y}-${m}`
        }
      })
      this.query.periodKey = current ? current.value : opts[4].value
    },
    onPeriodTypeChange() {
      this.query.periodKey = ''
      this.initPeriodOptions()
      this.loadSchedules()
    },
    getIsoWeek(date) {
      const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()))
      const dayNum = d.getUTCDay() || 7
      d.setUTCDate(d.getUTCDate() + 4 - dayNum)
      const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1))
      const weekNo = Math.ceil((((d - yearStart) / 86400000) + 1) / 7)
      return { year: d.getUTCFullYear(), week: weekNo }
    },
    getWeekDates(periodKey) {
      // periodKey = 2026-W41
      const match = periodKey.match(/^(\d{4})-W(\d{2})$/)
      if (!match) return []
      const year = parseInt(match[1])
      const week = parseInt(match[2])
      // ISO 周：该周周一
      const jan4 = new Date(Date.UTC(year, 0, 4))
      const dayOfWeek = jan4.getUTCDay() || 7
      const monday = new Date(jan4.getTime() + (1 - dayOfWeek + (week - 1) * 7) * 86400000)
      const dates = []
      const weekNames = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
      for (let i = 0; i < 7; i++) {
        const d = new Date(monday.getTime() + i * 86400000)
        const dateStr = d.toISOString().substring(0, 10)
        dates.push({
          date: dateStr,
          label: `${weekNames[i]} ${d.getUTCMonth() + 1}/${d.getUTCDate()}`
        })
      }
      return dates
    },
    getMonthDates(periodKey) {
      // periodKey = 2026-10
      const match = periodKey.match(/^(\d{4})-(\d{2})$/)
      if (!match) return []
      const year = parseInt(match[1])
      const month = parseInt(match[2])
      const daysInMonth = new Date(Date.UTC(year, month, 0)).getUTCDate()
      const dates = []
      for (let i = 1; i <= daysInMonth; i++) {
        const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(i).padStart(2, '0')}`
        dates.push({ date: dateStr, label: `${month}/${i}` })
      }
      return dates
    },
    // ===== 数据加载 =====
    loadStaffOptions() {
      listStaffOptions().then(res => {
        this.staffOptions = res.data || []
        this.loadSchedules()
      })
    },
    loadSchedules() {
      if (!this.query.periodKey) return
      this.loading = true
      listSchedules({
        periodType: this.query.periodType,
        periodKey: this.query.periodKey,
        status: this.query.status || undefined,
        staffId: this.query.staffId || undefined
      }).then(res => {
        this.schedules = res.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    // ===== 批量创建 =====
    openBatchDialog() {
      this.batchDialog.form.periodType = this.query.periodType
      this.batchDialog.form.periodKey = this.query.periodKey
      this.batchDialog.form.staffIds = []
      this.batchDialog.form.dates = this.dateColumns.map(d => d.date)
      this.batchDialog.form.shifts = ['morning']
      this.batchDialog.dateOptions = this.dateColumns.map(d => d.date)
      this.batchDialog.visible = true
    },
    submitBatchCreate() {
      const f = this.batchDialog.form
      if (f.staffIds.length === 0) return this.$modal.msgWarning('请选择员工')
      if (f.dates.length === 0) return this.$modal.msgWarning('请选择日期')
      if (f.shifts.length === 0) return this.$modal.msgWarning('请选择班次')
      const items = []
      f.staffIds.forEach(staffId => {
        f.dates.forEach(date => {
          f.shifts.forEach(shift => {
            const startTime = shift === 'morning' ? f.morningStart : f.eveningStart
            const endTime = shift === 'morning' ? f.morningEnd : f.eveningEnd
            items.push({ staffId, scheduleDate: date, shiftType: shift, startTime, endTime })
          })
        })
      })
      this.batchDialog.submitting = true
      batchCreate({
        periodType: f.periodType,
        periodKey: f.periodKey,
        items
      }).then(res => {
        this.$modal.msgSuccess(`成功创建 ${res.data} 条草稿排班`)
        this.batchDialog.visible = false
        this.batchDialog.submitting = false
        this.loadSchedules()
      }).catch(() => { this.batchDialog.submitting = false })
    },
    // ===== 快捷添加（单元格 + 号） =====
    openQuickAdd(row, date) {
      this.batchDialog.form.periodType = this.query.periodType
      this.batchDialog.form.periodKey = this.query.periodKey
      this.batchDialog.form.staffIds = [row.userId]
      this.batchDialog.form.dates = [date]
      this.batchDialog.form.shifts = ['morning']
      this.batchDialog.dateOptions = [date]
      this.batchDialog.visible = true
    },
    // ===== 发布 =====
    handlePublish() {
      this.$modal.confirm(`确认发布本期（${this.query.periodKey}）全部草稿排班？发布后员工/顾客可见，仍可修正。`).then(() => {
        return publishSchedule({ periodType: this.query.periodType, periodKey: this.query.periodKey })
      }).then(res => {
        this.$modal.msgSuccess(`成功发布 ${res.data} 条排班`)
        this.loadSchedules()
      }).catch(() => {})
    },
    // ===== 修正 =====
    openModifyDialog(sch) {
      const staff = this.staffOptions.find(s => s.userId === sch.staffId)
      this.modifyDialog.staffName = staff ? staff.nickName : '未知'
      this.modifyDialog.form = {
        id: sch.id,
        scheduleDate: sch.scheduleDate,
        shiftType: sch.shiftType,
        startTime: sch.startTime,
        endTime: sch.endTime,
        changeReason: '',
        remark: sch.remark || ''
      }
      this.modifyDialog.visible = true
    },
    submitModify() {
      const f = this.modifyDialog.form
      if (!f.changeReason || !f.changeReason.trim()) {
        return this.$modal.msgWarning('修正原因必填')
      }
      this.modifyDialog.submitting = true
      modifySchedule(f).then(() => {
        this.$modal.msgSuccess('修正成功（已写入留痕日志）')
        this.modifyDialog.visible = false
        this.modifyDialog.submitting = false
        this.loadSchedules()
      }).catch(() => { this.modifyDialog.submitting = false })
    },
    // ===== 删除草稿 =====
    handleDeleteDraft(sch) {
      this.$modal.confirm('确认删除该草稿排班？').then(() => {
        return deleteDraft(sch.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.loadSchedules()
      }).catch(() => {})
    },
    // ===== 班次时间设定 =====
    openShiftTimeDialog() {
      this.shiftTimeDialog.form.periodType = this.query.periodType
      this.shiftTimeDialog.form.periodKey = this.query.periodKey
      this.shiftTimeDialog.form.shiftType = 'morning'
      this.shiftTimeDialog.visible = true
    },
    submitShiftTime() {
      const f = this.shiftTimeDialog.form
      this.shiftTimeDialog.submitting = true
      setShiftTime({
        periodType: f.periodType,
        periodKey: f.periodKey,
        shiftType: f.shiftType,
        startTime: f.startTime,
        endTime: f.endTime
      }).then(res => {
        this.$modal.msgSuccess(`成功更新 ${res.data} 条班次时间`)
        this.shiftTimeDialog.visible = false
        this.shiftTimeDialog.submitting = false
        this.loadSchedules()
      }).catch(() => { this.shiftTimeDialog.submitting = false })
    },
    // ===== 修正日志 =====
    openLogDialog() {
      this.logDialog.visible = true
      this.logDialog.loading = true
      listChangeLogs({
        periodType: this.query.periodType,
        periodKey: this.query.periodKey
      }).then(res => {
        this.logDialog.list = res.data || []
        this.logDialog.loading = false
      }).catch(() => { this.logDialog.loading = false })
    },
    logTypeLabel(type) {
      return { TIME: '时间', SHIFT: '班次', STATUS: '状态', DELETE: '删除' }[type] || type
    },
    logTypeColor(type) {
      return { TIME: 'primary', SHIFT: 'warning', STATUS: 'success', DELETE: 'danger' }[type] || 'info'
    },
    // ===== 格式化 =====
    formatTime(t) {
      if (!t) return ''
      return t.substring(0, 5)
    },
    spanMethod() {
      return { rowspan: 1, colspan: 1 }
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.cell-schedules { min-height: 40px; display: flex; flex-direction: column; gap: 4px; }
.schedule-chip {
  display: flex; align-items: center; justify-content: space-between;
  padding: 2px 6px; border-radius: 4px; font-size: 12px; gap: 4px;
}
.chip-draft { background: #fdf6ec; border: 1px dashed #e6a23c; }
.chip-published { background: #f0f9eb; border: 1px solid #67c23a; }
.chip-time { font-weight: 500; }
.add-btn { color: #c0c4cc; padding: 0; }
.add-btn:hover { color: #409eff; }
</style>
