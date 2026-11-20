import { getCopyMap, getCopyMapAllLangs } from '@/api/system/bizCopy'

/**
 * 业态文案映射 store（T10.5 v1.29 文案映射 i18n）
 *
 * 三维合并优先级（调用方按序尝试）：
 *   1. 租户 module_overrides.copy（由 TenantBizConfigServiceImpl#getMergedOverrides 下发）
 *   2. biz_copy(biz_code, lang)（本 store）
 *   3. biz_bundle.copy（单语言 zh-CN 兜底）
 *   4. 前端硬编码兜底
 *
 * state：
 *   bizCode   当前业态包编码（登录后从 /system/edition/features 或用户信息接口注入）
 *   lang      当前语言（默认 zh-CN，可通过 settings 切换）
 *   copyMap   { i18n_key: copy_value } 当前业态包 + 当前语言的文案映射
 *   allLangs  { lang: { key: value } } 当前业态包的全部语言文案（切换语言时零延迟）
 *   loaded    是否已加载
 */
const state = {
  bizCode: 'fund_control',
  lang: 'zh-CN',
  copyMap: {},
  allLangs: {},
  loaded: false
}

const mutations = {
  SET_BIZ_CODE: (state, bizCode) => {
    state.bizCode = bizCode || 'fund_control'
    state.loaded = false
    state.copyMap = {}
    state.allLangs = {}
  },
  SET_LANG: (state, lang) => {
    state.lang = lang || 'zh-CN'
    // 切换语言时若已加载 allLangs 则直接从 allLangs 取，零延迟
    if (state.allLangs && state.allLangs[state.lang]) {
      state.copyMap = state.allLangs[state.lang]
    } else {
      state.loaded = false
    }
  },
  SET_COPY_MAP: (state, map) => {
    state.copyMap = map || {}
    state.loaded = true
  },
  SET_ALL_LANGS: (state, allLangs) => {
    state.allLangs = allLangs || {}
    if (state.allLangs[state.lang]) {
      state.copyMap = state.allLangs[state.lang]
      state.loaded = true
    }
  }
}

const actions = {
  /**
   * 加载当前业态包 + 当前语言的文案映射
   */
  loadCopyMap({ commit, state }) {
    return getCopyMap(state.bizCode, state.lang).then(res => {
      commit('SET_COPY_MAP', res.data || {})
      return res.data || {}
    })
  },
  /**
   * 加载当前业态包的全部语言文案（供多语言切换零延迟）
   */
  loadAllLangs({ commit, state }) {
    return getCopyMapAllLangs(state.bizCode).then(res => {
      commit('SET_ALL_LANGS', res.data || {})
      return res.data || {}
    })
  },
  /**
   * 切换业态包（登录后或租户切换时调用）
   */
  switchBizCode({ commit, dispatch }, bizCode) {
    commit('SET_BIZ_CODE', bizCode)
    return dispatch('loadAllLangs')
  },
  /**
   * 切换语言（settings 面板调用）
   */
  switchLang({ commit, state, dispatch }, lang) {
    commit('SET_LANG', lang)
    if (!state.allLangs || !state.allLangs[lang]) {
      return dispatch('loadAllLangs')
    }
    return Promise.resolve(state.copyMap)
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
