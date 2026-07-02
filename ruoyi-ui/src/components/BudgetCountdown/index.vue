<template>
  <div class="budget-countdown" :class="{ 'is-warning': isWarning, 'is-timeout': isTimeout }">
    <template v-if="isTimeout">
      <el-tag type="danger" effect="dark" class="countdown-tag">
        <i class="el-icon-warning"></i> 已超时
      </el-tag>
    </template>
    <template v-else-if="showCountdown">
      <span class="countdown-text">
        <i class="el-icon-time"></i>
        剩余 <span class="countdown-value" :class="{ 'blink': isWarning }">{{ remainingText }}</span>
      </span>
    </template>
    <template v-else>
      <el-tag type="info" class="countdown-tag">无截止时间</el-tag>
    </template>
  </div>
</template>

<script>
export default {
  name: 'BudgetCountdown',
  props: {
    deadline: {
      type: [String, Date],
      default: null
    },
    status: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      timer: null,
      remainingMs: 0
    }
  },
  computed: {
    showCountdown() {
      return this.deadline && this.remainingMs > 0
    },
    isWarning() {
      // 最后2小时变红
      return this.remainingMs > 0 && this.remainingMs <= 2 * 60 * 60 * 1000
    },
    isTimeout() {
      return this.deadline && this.remainingMs <= 0
    },
    remainingText() {
      if (this.remainingMs <= 0) return ''
      const hours = Math.floor(this.remainingMs / (1000 * 60 * 60))
      const minutes = Math.floor((this.remainingMs % (1000 * 60 * 60)) / (1000 * 60))
      const seconds = Math.floor((this.remainingMs % (1000 * 60)) / 1000)
      if (hours > 24) {
        const days = Math.floor(hours / 24)
        return `${days}天 ${hours % 24}小时`
      }
      return `${hours}h ${minutes}m ${seconds}s`
    }
  },
  watch: {
    deadline: {
      immediate: true,
      handler(val) {
        this.startTimer()
      }
    }
  },
  mounted() {
    this.startTimer()
  },
  beforeDestroy() {
    this.clearTimer()
  },
  methods: {
    startTimer() {
      this.clearTimer()
      this.updateRemaining()
      this.timer = setInterval(() => {
        this.updateRemaining()
      }, 1000)
    },
    updateRemaining() {
      if (!this.deadline) {
        this.remainingMs = 0
        return
      }
      const deadlineTime = new Date(this.deadline).getTime()
      const now = Date.now()
      this.remainingMs = deadlineTime - now
      if (this.remainingMs <= 0) {
        this.clearTimer()
        this.$emit('timeout')
      }
    },
    clearTimer() {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
    }
  }
}
</script>

<style scoped>
.budget-countdown {
  display: inline-block;
  font-size: 14px;
}
.countdown-text {
  color: #606266;
}
.countdown-value {
  font-weight: bold;
  color: #409EFF;
}
.is-warning .countdown-value {
  color: #F56C6C;
}
.blink {
  animation: blink-animation 1s ease-in-out infinite;
}
@keyframes blink-animation {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}
.countdown-tag {
  font-size: 13px;
}
</style>
