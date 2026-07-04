<template>
  <div class="app-container ai-chat-container">
    <!-- 顶部标题栏 -->
    <div class="chat-header">
      <div class="header-left">
        <svg-icon icon-class="robot" />
        <span class="header-title">AI 智能报表助手</span>
        <el-tag :type="aiEnabled ? 'success' : 'danger'" size="mini" class="status-tag">
          {{ aiEnabled ? '在线' : '离线' }}
        </el-tag>
      </div>
      <div class="header-right">
        <el-tooltip content="清除当前会话" placement="top">
          <el-button icon="el-icon-delete" circle size="mini" @click="handleClearSession" />
        </el-tooltip>
        <el-tooltip content="新建会话" placement="top">
          <el-button icon="el-icon-plus" circle size="mini" @click="handleNewSession" />
        </el-tooltip>
      </div>
    </div>

    <!-- 快捷问题 -->
    <div class="quick-questions" v-if="messages.length === 0">
      <div class="welcome-text">
        <h3>你好，我是 AI 报表助手</h3>
        <p>我可以帮助你分析预算数据、生成报表摘要、解答预算管理问题</p>
      </div>
      <div class="question-cards">
        <div
          v-for="(q, idx) in quickQuestions"
          :key="idx"
          class="question-card"
          @click="handleQuickQuestion(q)"
        >
          <svg-icon :icon-class="q.icon" />
          <span>{{ q.text }}</span>
        </div>
      </div>
    </div>

    <!-- 消息列表 -->
    <div class="chat-messages" ref="messagesContainer" v-show="messages.length > 0">
      <div
        v-for="(msg, index) in messages"
        :key="index"
        :class="['message-item', msg.role === 'user' ? 'message-user' : 'message-ai']"
      >
        <div class="message-avatar">
          <el-avatar v-if="msg.role === 'user'" :size="36" icon="el-icon-user-solid" />
          <el-avatar v-else :size="36" icon="el-icon-s-opportunity" class="ai-avatar" />
        </div>
        <div class="message-content">
          <div class="message-role">{{ msg.role === 'user' ? '我' : 'AI 助手' }}</div>
          <div class="message-text" v-html="renderMarkdown(msg.content)"></div>
          <div class="message-time">{{ msg.time }}</div>
        </div>
      </div>
      <!-- 加载指示器 -->
      <div v-if="loading" class="message-item message-ai">
        <div class="message-avatar">
          <el-avatar :size="36" icon="el-icon-s-opportunity" class="ai-avatar" />
        </div>
        <div class="message-content">
          <div class="message-role">AI 助手</div>
          <div class="message-text typing-indicator">
            <span class="dot"></span>
            <span class="dot"></span>
            <span class="dot"></span>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input-area">
      <div class="input-wrapper">
        <el-input
          ref="messageInput"
          v-model="inputMessage"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 4 }"
          placeholder="输入你的问题，如：帮我分析本月预算数据..."
          @keydown.enter.native.exact="handleSend"
          :disabled="loading || !aiEnabled"
        />
        <el-button
          type="primary"
          icon="el-icon-s-promotion"
          :loading="loading"
          :disabled="!inputMessage.trim() || !aiEnabled"
          @click="handleSend"
          class="send-btn"
        >
          发送
        </el-button>
      </div>
      <div class="input-tips">
        <span>按 Enter 发送，Shift + Enter 换行</span>
      </div>
    </div>
  </div>
</template>

<script>
import { aiChat, aiChatStream, clearAiSession, getAiStatus } from '@/api/system/ai'

export default {
  name: 'AiChat',
  data() {
    return {
      aiEnabled: false,
      sessionId: null,
      inputMessage: '',
      messages: [],
      loading: false,
      abortStream: null,
      quickQuestions: [
        { text: '帮我分析本月预算编制情况', icon: 'chart' },
        { text: '各部门预算执行情况如何？', icon: 'money' },
        { text: '预算审批流程是怎样的？', icon: 'guide' },
        { text: '哪些科目预算超过预警线？', icon: 'warning' }
      ]
    }
  },
  mounted() {
    this.checkAiStatus()
    this.$refs.messageInput && this.$refs.messageInput.focus()
  },
  methods: {
    async checkAiStatus() {
      try {
        const res = await getAiStatus()
        this.aiEnabled = res.data === true
      } catch (e) {
        this.aiEnabled = false
      }
    },
    handleSend() {
      if (!this.inputMessage.trim() || this.loading) return
      if (this.inputMessage.includes('\n') && !window.event.shiftKey) return

      const userMsg = this.inputMessage.trim()
      this.inputMessage = ''

      // 添加用户消息
      this.messages.push({
        role: 'user',
        content: userMsg,
        time: this.formatTime(new Date())
      })

      // 添加AI占位消息
      const aiMsgIndex = this.messages.length
      this.messages.push({
        role: 'assistant',
        content: '',
        time: this.formatTime(new Date())
      })

      this.loading = true
      this.scrollToBottom()

      // 使用流式对话
      this.abortStream = aiChatStream(
        {
          sessionId: this.sessionId,
          message: userMsg,
          stream: true
        },
        // onMessage
        (token) => {
          if (this.messages[aiMsgIndex]) {
            this.messages[aiMsgIndex].content += token
            this.scrollToBottom()
          }
        },
        // onDone
        () => {
          this.loading = false
          this.abortStream = null
          this.scrollToBottom()
        },
        // onError
        (error) => {
          this.loading = false
          this.abortStream = null
          if (this.messages[aiMsgIndex]) {
            this.messages[aiMsgIndex].content = '抱歉，AI服务出现异常：' + (error.message || '未知错误')
          }
          this.scrollToBottom()
        }
      )
    },
    handleQuickQuestion(q) {
      this.inputMessage = q.text
      this.$nextTick(() => {
        this.handleSend()
      })
    },
    handleClearSession() {
      if (!this.sessionId) return
      this.$confirm('确定清除当前会话记录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        clearAiSession(this.sessionId).catch(() => {})
        this.messages = []
        this.sessionId = null
        this.$message.success('会话已清除')
      }).catch(() => {})
    },
    handleNewSession() {
      this.messages = []
      this.sessionId = null
      this.$message.success('已新建会话')
    },
    scrollToBottom() {
      this.$nextTick(() => {
        const container = this.$refs.messagesContainer
        if (container) {
          container.scrollTop = container.scrollHeight
        }
      })
    },
    formatTime(date) {
      const h = String(date.getHours()).padStart(2, '0')
      const m = String(date.getMinutes()).padStart(2, '0')
      return h + ':' + m
    },
    renderMarkdown(text) {
      if (!text) return ''
      // 简单markdown渲染
      return text
        .replace(/\n/g, '<br>')
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/`(.*?)`/g, '<code>$1</code>')
    }
  },
  beforeDestroy() {
    if (this.abortStream) {
      this.abortStream()
    }
  }
}
</script>

<style lang="scss" scoped>
.ai-chat-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 84px);
  padding: 0;
  background: #f5f7fa;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;

  .header-left {
    display: flex;
    align-items: center;
    gap: 8px;

    .header-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }

    .status-tag {
      margin-left: 4px;
    }
  }

  .header-right {
    display: flex;
    gap: 8px;
  }
}

.quick-questions {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;

  .welcome-text {
    text-align: center;
    margin-bottom: 32px;

    h3 {
      font-size: 22px;
      color: #303133;
      margin-bottom: 8px;
    }

    p {
      color: #909399;
      font-size: 14px;
    }
  }

  .question-cards {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    max-width: 600px;
    width: 100%;

    .question-card {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 14px 16px;
      background: #fff;
      border-radius: 8px;
      border: 1px solid #e8e8e8;
      cursor: pointer;
      transition: all 0.2s;
      font-size: 14px;
      color: #606266;

      &:hover {
        border-color: #409eff;
        box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
        transform: translateY(-1px);
      }

      .svg-icon {
        color: #409eff;
        font-size: 18px;
        flex-shrink: 0;
      }
    }
  }
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;

  .message-item {
    display: flex;
    margin-bottom: 20px;
    gap: 12px;

    &.message-user {
      flex-direction: row-reverse;

      .message-content {
        align-items: flex-end;

        .message-text {
          background: #409eff;
          color: #fff;
          border-radius: 12px 2px 12px 12px;
        }

        .message-time {
          text-align: right;
        }
      }
    }

    &.message-ai {
      .message-content {
        .message-text {
          background: #fff;
          color: #303133;
          border-radius: 2px 12px 12px 12px;
          border: 1px solid #e8e8e8;
        }
      }
    }

    .message-avatar {
      flex-shrink: 0;

      .ai-avatar {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      }
    }

    .message-content {
      display: flex;
      flex-direction: column;
      max-width: 70%;

      .message-role {
        font-size: 12px;
        color: #909399;
        margin-bottom: 4px;
      }

      .message-text {
        padding: 12px 16px;
        font-size: 14px;
        line-height: 1.6;
        word-break: break-word;

        ::v-deep code {
          background: rgba(0, 0, 0, 0.06);
          padding: 2px 4px;
          border-radius: 3px;
          font-size: 13px;
        }
      }

      .message-time {
        font-size: 11px;
        color: #c0c4cc;
        margin-top: 4px;
      }
    }
  }

  .typing-indicator {
    display: flex;
    gap: 4px;
    padding: 16px;

    .dot {
      width: 8px;
      height: 8px;
      background: #909399;
      border-radius: 50%;
      animation: typing 1.4s infinite;

      &:nth-child(2) { animation-delay: 0.2s; }
      &:nth-child(3) { animation-delay: 0.4s; }
    }
  }
}

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-6px); opacity: 1; }
}

.chat-input-area {
  padding: 16px 20px;
  background: #fff;
  border-top: 1px solid #e8e8e8;
  flex-shrink: 0;

  .input-wrapper {
    display: flex;
    gap: 12px;
    align-items: flex-end;

    ::v-deep .el-textarea__inner {
      border-radius: 8px;
      resize: none;
      padding: 10px 12px;
      font-size: 14px;
    }

    .send-btn {
      height: 40px;
      border-radius: 8px;
      flex-shrink: 0;
    }
  }

  .input-tips {
    margin-top: 6px;
    font-size: 12px;
    color: #c0c4cc;
    text-align: right;
  }
}
</style>
