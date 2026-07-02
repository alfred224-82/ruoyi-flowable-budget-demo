<template>
  <div class="budget-reject-timeline">
    <el-empty v-if="!historyList || historyList.length === 0" description="暂无驳回记录" />
    <el-timeline v-else>
      <el-timeline-item
        v-for="item in historyList"
        :key="item.id"
        :timestamp="formatTime(item.rejectTime)"
        placement="top"
        :type="getTimelineType(item.rejectFromLevel)"
        :icon="getTimelineIcon(item.rejectFromLevel)"
      >
        <el-card shadow="hover" class="timeline-card">
          <div class="timeline-header">
            <el-tag :type="getTimelineType(item.rejectFromLevel)" size="small">
              {{ item.rejectFromLevel === 'HQ' ? '总部驳回' : '分公司打回' }}
            </el-tag>
            <span class="timeline-arrow">
              <i class="el-icon-right"></i>
              {{ item.rejectToLevel === 'Branch' ? '分公司' : item.rejectToLevel === 'Dept' ? (item.rejectToDeptName || '部门') : '部门' }}
            </span>
            <el-tag v-if="item.isTimeout === 1" type="danger" size="small" style="margin-left: 8px;">
              已超时
            </el-tag>
          </div>
          <div class="timeline-body">
            <p class="reject-reason"><strong>驳回理由：</strong>{{ item.rejectReason }}</p>
            <p class="reject-info">
              <span>驳回人：{{ item.rejectFromName || item.rejectFromUser }}</span>
              <span v-if="item.deadlineTime" style="margin-left: 16px;">
                截止时间：{{ formatTime(item.deadlineTime) }}
              </span>
            </p>
            <p v-if="item.handleTime" class="handle-info">
              <span>处理时间：{{ formatTime(item.handleTime) }}</span>
              <span v-if="item.handleDurationHours" style="margin-left: 16px;">
                耗时：{{ item.handleDurationHours }}小时
              </span>
            </p>
          </div>
        </el-card>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<script>
import { getRejectHistory } from '@/api/system/preparation'

export default {
  name: 'BudgetRejectTimeline',
  props: {
    sheetId: {
      type: [Number, String],
      default: null
    }
  },
  data() {
    return {
      historyList: []
    }
  },
  watch: {
    sheetId: {
      immediate: true,
      handler(val) {
        if (val) {
          this.loadHistory()
        }
      }
    }
  },
  methods: {
    async loadHistory() {
      if (!this.sheetId) return
      try {
        const response = await getRejectHistory(this.sheetId)
        this.historyList = response.data || []
      } catch (error) {
        console.error('加载驳回历史失败', error)
      }
    },
    formatTime(time) {
      if (!time) return ''
      const d = new Date(time)
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      const h = String(d.getHours()).padStart(2, '0')
      const min = String(d.getMinutes()).padStart(2, '0')
      const s = String(d.getSeconds()).padStart(2, '0')
      return `${y}-${m}-${day} ${h}:${min}:${s}`
    },
    getTimelineType(level) {
      return level === 'HQ' ? 'danger' : 'warning'
    },
    getTimelineIcon(level) {
      return level === 'HQ' ? 'el-icon-close' : 'el-icon-warning'
    }
  }
}
</script>

<style scoped>
.budget-reject-timeline {
  padding: 10px 0;
}
.timeline-card {
  max-width: 500px;
}
.timeline-header {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}
.timeline-arrow {
  margin-left: 10px;
  color: #909399;
  font-size: 13px;
}
.timeline-body {
  font-size: 13px;
  color: #606266;
}
.reject-reason {
  margin: 0 0 8px 0;
  line-height: 1.6;
}
.reject-info,
.handle-info {
  margin: 4px 0;
  color: #909399;
  font-size: 12px;
}
</style>
