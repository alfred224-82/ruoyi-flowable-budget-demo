<template>
  <div class="message-bell right-menu-item hover-effect" @click="handleClick">
    <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="badge">
      <i class="el-icon-bell" style="font-size: 20px;" />
    </el-badge>
    <!-- 消息下拉面板 -->
    <el-popover v-model="visible" placement="bottom" width="360" trigger="manual">
      <div class="message-panel" slot="reference" />
      <div class="message-dropdown">
        <div class="message-header">
          <span>消息通知</span>
          <el-button type="text" size="mini" @click="handleMarkAllRead">全部已读</el-button>
        </div>
        <el-scrollbar style="max-height: 300px;">
          <div v-if="messageList.length === 0" class="message-empty">暂无消息</div>
          <div
            v-for="msg in messageList"
            :key="msg.id"
            :class="['message-item', { 'is-unread': msg.isRead === 0 }]"
            @click="handleMessageClick(msg)"
          >
            <div class="message-title">
              <el-tag :type="getMessageTypeTag(msg.messageType)" size="mini">{{ getMessageTypeLabel(msg.messageType) }}</el-tag>
              <span class="message-title-text">{{ msg.title }}</span>
            </div>
            <div class="message-time">{{ formatTime(msg.createTime) }}</div>
          </div>
        </el-scrollbar>
        <div class="message-footer" @click="goToMessageCenter">查看全部</div>
      </div>
    </el-popover>
  </div>
</template>

<script>
import { getUnreadCount, listMessage, markAsRead, markAllAsRead } from '@/api/system/message'

export default {
  name: 'MessageBell',
  data() {
    return {
      unreadCount: 0,
      messageList: [],
      visible: false,
      pollTimer: null
    }
  },
  mounted() {
    this.fetchUnreadCount()
    // 每30秒轮询未读数
    this.pollTimer = setInterval(() => {
      this.fetchUnreadCount()
    }, 30000)
  },
  beforeDestroy() {
    if (this.pollTimer) {
      clearInterval(this.pollTimer)
    }
  },
  methods: {
    async fetchUnreadCount() {
      try {
        const res = await getUnreadCount()
        this.unreadCount = res.data || 0
      } catch (e) {
        // 忽略网络错误
      }
    },
    async fetchRecentMessages() {
      try {
        const res = await listMessage({ pageNum: 1, pageSize: 10 })
        this.messageList = res.rows || []
      } catch (e) {
        // 忽略网络错误
      }
    },
    handleClick() {
      this.visible = !this.visible
      if (this.visible) {
        this.fetchRecentMessages()
      }
    },
    async handleMessageClick(msg) {
      if (msg.isRead === 0) {
        await markAsRead(msg.id)
        this.unreadCount = Math.max(0, this.unreadCount - 1)
        msg.isRead = 1
      }
      // 根据 bizType 跳转到对应页面
      if (msg.bizType === 'budget_preparation') {
        this.$router.push('/system/preparation/wizard?id=' + msg.bizId + '&viewOnly=true')
      }
      this.visible = false
    },
    async handleMarkAllRead() {
      await markAllAsRead()
      this.unreadCount = 0
      this.messageList.forEach(m => { m.isRead = 1 })
    },
    goToMessageCenter() {
      this.$router.push('/system/message/index')
      this.visible = false
    },
    getMessageTypeLabel(type) {
      const map = { APPROVAL: '审批', REJECT: '驳回', SYSTEM: '系统' }
      return map[type] || type
    },
    getMessageTypeTag(type) {
      const map = { APPROVAL: 'warning', REJECT: 'danger', SYSTEM: 'info' }
      return map[type] || 'info'
    },
    formatTime(time) {
      if (!time) return ''
      const d = new Date(time)
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      const hours = String(d.getHours()).padStart(2, '0')
      const minutes = String(d.getMinutes()).padStart(2, '0')
      return `${month}-${day} ${hours}:${minutes}`
    }
  }
}
</script>

<style lang="scss" scoped>
.message-bell {
  position: relative;
  display: inline-block;

  ::v-deep .el-badge__content {
      top: -2px;
    }
}

.message-dropdown {
  .message-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 12px;
    border-bottom: 1px solid #eee;
    font-weight: bold;
    font-size: 14px;
  }

  .message-empty {
    text-align: center;
    padding: 30px 0;
    color: #999;
    font-size: 13px;
  }

  .message-item {
    padding: 10px 12px;
    cursor: pointer;
    border-bottom: 1px solid #f5f5f5;
    transition: background 0.2s;

    &:hover {
      background: #f5f7fa;
    }

    &.is-unread {
      background: #ecf5ff;

      &:hover {
        background: #d9ecff;
      }
    }

    .message-title {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      line-height: 1.4;

      .message-title-text {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        flex: 1;
      }
    }

    .message-time {
      margin-top: 4px;
      font-size: 12px;
      color: #999;
    }
  }

  .message-footer {
    text-align: center;
    padding: 10px 0;
    color: #409eff;
    font-size: 13px;
    cursor: pointer;
    border-top: 1px solid #eee;

    &:hover {
      background: #f5f7fa;
    }
  }
}
</style>
