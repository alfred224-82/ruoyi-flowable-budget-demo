const getters = {
  sidebar: state => state.app.sidebar,
  size: state => state.app.size,
  device: state => state.app.device,
  dict: state => state.dict.dict,
  visitedViews: state => state.tagsView.visitedViews,
  cachedViews: state => state.tagsView.cachedViews,
  token: state => state.user.token,
  avatar: state => state.user.avatar,
  name: state => state.user.name,
  introduction: state => state.user.introduction,
  roles: state => state.user.roles,
  permissions: state => state.user.permissions,
  features: state => state.user.features,
  edition: state => state.user.edition,
  tenantName: state => state.user.tenantName,
  versionType: state => state.user.versionType,
  // 控制台标题：租户名称+「管理系统」；已登录但租户名称缺失时兜底「管理系统」；未登录保留产品默认标题
  systemTitle: state => {
    if (state.user.tenantName) {
      return state.user.tenantName + '管理系统'
    }
    return state.user.token ? '管理系统' : process.env.VUE_APP_TITLE
  },
  permission_routes: state => state.permission.routes,
  topbarRouters:state => state.permission.topbarRouters,
  defaultRoutes:state => state.permission.defaultRoutes,
  sidebarRouters:state => state.permission.sidebarRouters,
  // T10.5 v1.29 业态文案映射 i18n
  bizCode: state => state.bizCopy.bizCode,
  bizLang: state => state.bizCopy.lang,
  bizCopyMap: state => state.bizCopy.copyMap,
  bizCopyLoaded: state => state.bizCopy.loaded
}
export default getters
