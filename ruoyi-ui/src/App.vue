<template>
  <div id="app">
    <router-view />
    <theme-picker />
  </div>
</template>

<script>
import ThemePicker from "@/components/ThemePicker";

export default {
  name: "App",
  components: { ThemePicker },
    metaInfo() {
        // 浏览器标签标题：与侧边栏 Logo 一致，按「租户名称+管理系统」动态渲染
        // 注意：systemTitle 必须在 metaInfo() 内读取（vue-meta 仅将 metaInfo 函数体转为 computed 并监听），
        // 若放在 titleTemplate 回调里读取则不被响应式追踪，租户名称加载后标签标题不会刷新
        const systemTitle = this.$store.getters.systemTitle
        return {
            title: this.$store.state.settings.dynamicTitle && this.$store.state.settings.title,
            titleTemplate: title => {
                return title ? `${title} - ${systemTitle}` : systemTitle
            }
        }
    }
};
</script>
<style scoped>
#app .theme-picker {
  display: none;
}
</style>
