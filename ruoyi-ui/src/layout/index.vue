<template>
  <div :class="classObj" class="app-wrapper" :style="{'--current-color': theme}">
    <div v-if="device==='mobile'&&sidebar.opened" class="drawer-bg" @click="handleClickOutside"/>
    <sidebar v-if="!sidebar.hide" class="sidebar-container"/>
    <div :class="{hasTagsView:needTagsView,sidebarHide:sidebar.hide}" class="main-container">
      <div :class="{'fixed-header':fixedHeader}">
        <navbar/>
        <tags-view v-if="needTagsView"/>
      </div>
      <app-main/>
      <right-panel>
        <settings/>
      </right-panel>
    </div>
    <!-- T25.5 合规协议登录签署守卫：租户管理员未签署时强制弹窗 -->
    <compliance-sign/>
    <!-- ★ 备案信息（ICP + 公安备案徽章，管理后台底部居中展示） -->
    <div v-if="beian.icpNumber || beian.policeNumber" class="icp-footer">
      <a v-if="beian.icpNumber" :href="beian.icpLink" target="_blank" rel="noopener noreferrer">{{ beian.icpNumber }}</a>
      <template v-if="beian.icpNumber && beian.policeNumber"> | </template>
      <a v-if="beian.policeNumber" class="police-link" :href="beian.policeLink" target="_blank" rel="noopener noreferrer">
        <img v-if="beian.policeIcon" class="police-badge" :src="beian.policeIcon" alt="公安备案徽章">
        <span>{{ beian.policeNumber }}</span>
      </a>
    </div>
  </div>
</template>

<script>
import RightPanel from '@/components/RightPanel'
import ComplianceSign from '@/components/ComplianceSign'
import { AppMain, Navbar, Settings, Sidebar, TagsView } from './components'
import ResizeMixin from './mixin/ResizeHandler'
import { getBeianInfo } from '@/utils/beian'
import { mapState } from 'vuex'
import variables from '@/assets/styles/variables.scss'

export default {
  name: 'Layout',
  components: {
    AppMain,
    ComplianceSign,
    Navbar,
    RightPanel,
    Settings,
    Sidebar,
    TagsView
  },
  mixins: [ResizeMixin],
  computed: {
    ...mapState({
      theme: state => state.settings.theme,
      sideTheme: state => state.settings.sideTheme,
      sidebar: state => state.app.sidebar,
      device: state => state.app.device,
      needTagsView: state => state.settings.tagsView,
      fixedHeader: state => state.settings.fixedHeader
    }),
    classObj() {
      return {
        hideSidebar: !this.sidebar.opened,
        openSidebar: this.sidebar.opened,
        withoutAnimation: this.sidebar.withoutAnimation,
        mobile: this.device === 'mobile'
      }
    },
    variables() {
      return variables;
    },
    // ★ 备案信息（ICP + 公安备案，统一读 .env 配置，见 @/utils/beian）
    beian() { return getBeianInfo() }
  },
  methods: {
    handleClickOutside() {
      this.$store.dispatch('app/closeSideBar', { withoutAnimation: false })
    }
  }
}
</script>

<style lang="scss" scoped>
  @import "~@/assets/styles/mixin.scss";
  @import "~@/assets/styles/variables.scss";

  .app-wrapper {
    @include clearfix;
    position: relative;
    height: 100%;
    width: 100%;

    &.mobile.openSidebar {
      position: fixed;
      top: 0;
    }
  }

  .drawer-bg {
    background: #000;
    opacity: 0.3;
    width: 100%;
    top: 0;
    height: 100%;
    position: absolute;
    z-index: 999;
  }

  .fixed-header {
    position: fixed;
    top: 0;
    right: 0;
    z-index: 9;
    width: calc(100% - #{$base-sidebar-width});
    transition: width 0.28s;
  }

  .hideSidebar .fixed-header {
    width: calc(100% - 54px);
  }

  .sidebarHide .fixed-header {
    width: 100%;
  }

  .mobile .fixed-header {
    width: 100%;
  }

  /* ★ 备案信息底部展示 */
  .icp-footer {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    height: 28px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    color: #909399;
    background: #f5f7fa;
    border-top: 1px solid #ebeef5;
    z-index: 99;
  }
  .icp-footer a {
    color: #909399;
    text-decoration: none;
    transition: color 0.2s;
  }
  .icp-footer a:hover {
    color: #409eff;
    text-decoration: underline;
  }
  /* 公安备案：官方徽章 + 备案号同行居中 */
  .icp-footer .police-link {
    display: inline-flex;
    align-items: center;
  }
  .icp-footer .police-badge {
    width: 16px;
    height: 17px;
    margin-right: 4px;
  }
</style>
