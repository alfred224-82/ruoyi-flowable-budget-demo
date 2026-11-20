<template>
  <div class="app-container">
    <el-alert title="小程序主题选择" type="info" description="选择小程序全局主题，选定后写入租户配置，小程序端下次冷启动读取最新主题配置。" show-icon :closable="false" class="mb16" />

    <div v-loading="loading">
      <el-row :gutter="20">
        <el-col :span="6" v-for="theme in themes" :key="theme.key" class="mb16">
          <el-card :class="['theme-card', { active: currentThemeKey === theme.key }]" shadow="hover" @click.native="selectTheme(theme.key)">
            <div class="theme-preview" :style="{ backgroundColor: theme.bgColor }">
              <div class="theme-header" :style="{ background: gradientOf(theme) }">
                <span style="color:#fff;font-size:14px;">{{ theme.name }}</span>
              </div>
              <div class="theme-body">
                <div class="theme-btn" :style="{ backgroundColor: theme.primaryColor }">按钮</div>
                <div class="theme-text" :style="{ color: theme.primaryColor }">链接文字</div>
              </div>
            </div>
            <div class="theme-info">
              <span style="font-weight:600;">{{ theme.name }}</span>
              <el-tag v-if="currentThemeKey === theme.key" type="success" size="mini" style="margin-left:8px;">当前</el-tag>
            </div>
            <div style="color:#909399;font-size:12px;margin-top:4px;">{{ theme.key }}</div>
          </el-card>
        </el-col>
      </el-row>

      <div style="text-align:center;margin-top:20px;">
        <el-button type="primary" :loading="saving" :disabled="!selectedKey || selectedKey === currentThemeKey" @click="handleSave">保存主题配置</el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { getThemeConfig, saveTheme } from '@/api/micro/platformTheme'

export default {
  name: 'PlatformTheme',
  data() {
    return {
      loading: true,
      saving: false,
      currentThemeKey: 'theme_fresh',
      selectedKey: null,
      themes: []
    }
  },
  created() { this.loadConfig() },
  mounted() {
    // M27 T27.4：PC Boss 端进入主题页时加载租户主题到 store（写入 :root CSS 变量）
    this.$store.dispatch('theme/loadCurrentTheme', true)
  },
  methods: {
    /** v1.43 美化版色板：预览头部按渐变起止色渲染 */
    gradientOf(theme) {
      if (theme.gradientStart && theme.gradientEnd) {
        return `linear-gradient(135deg, ${theme.gradientStart} 0%, ${theme.gradientEnd} 100%)`
      }
      return theme.primaryColor
    },
    loadConfig() {
      this.loading = true
      getThemeConfig().then(res => {
        this.currentThemeKey = res.data.currentThemeKey || 'theme_fresh'
        this.themes = res.data.themes || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    selectTheme(key) {
      this.selectedKey = key
    },
    handleSave() {
      const targetKey = this.selectedKey || this.currentThemeKey
      this.saving = true
      saveTheme(targetKey).then(() => {
        this.$modal.msgSuccess('主题保存成功，小程序端下次冷启动生效')
        this.currentThemeKey = targetKey
        this.selectedKey = null
        this.saving = false
        // M27 T27.4：保存后强制刷新 store 主题，PC 端各小微页面立即联动新主题色
        this.$store.dispatch('theme/refreshTheme')
      }).catch(() => { this.saving = false })
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.theme-card { cursor: pointer; transition: all 0.3s; }
.theme-card:hover { transform: translateY(-4px); }
.theme-card.active { border: 2px solid #409eff; }
.theme-preview { border-radius: 8px; overflow: hidden; margin-bottom: 12px; }
.theme-header { padding: 12px; text-align: center; }
.theme-body { padding: 16px; text-align: center; }
.theme-btn { display: inline-block; padding: 6px 20px; border-radius: 4px; color: #fff; font-size: 12px; margin-bottom: 8px; }
.theme-text { font-size: 12px; }
.theme-info { text-align: center; }
</style>
