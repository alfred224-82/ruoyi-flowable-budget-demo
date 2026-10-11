import { login, logout, getInfo } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'

const user = {
  state: {
    token: getToken(),
    name: '',
    avatar: '',
    roles: [],
    permissions: [],
    features: [],
    edition: 'BASIC',
    tenantName: '',
    versionType: ''
  },

  mutations: {
    SET_TOKEN: (state, token) => {
      state.token = token
    },
    SET_NAME: (state, name) => {
      state.name = name
    },
    SET_AVATAR: (state, avatar) => {
      state.avatar = avatar
    },
    SET_ROLES: (state, roles) => {
      state.roles = roles
    },
    SET_PERMISSIONS: (state, permissions) => {
      state.permissions = permissions
    },
    SET_FEATURES: (state, features) => {
      state.features = features || []
    },
    SET_EDITION: (state, edition) => {
      state.edition = edition || 'BASIC'
    },
    SET_TENANT_NAME: (state, tenantName) => {
      state.tenantName = tenantName || ''
    },
    SET_VERSION_TYPE: (state, versionType) => {
      state.versionType = versionType || ''
    }
  },

  actions: {
    // 登录
    Login({ commit }, userInfo) {
      const username = userInfo.username.trim()
      const password = userInfo.password
      const code = userInfo.code
      const uuid = userInfo.uuid
      const tenantId = userInfo.tenantId
      return new Promise((resolve, reject) => {
        login(username, password, code, uuid, tenantId).then(res => {
          setToken(res.data.token)
          commit('SET_TOKEN', res.data.token)
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 获取用户信息
    GetInfo({ commit, state, dispatch }) {
      return new Promise((resolve, reject) => {
        getInfo().then(res => {
          const user = res.data.user
          const avatar = (user.avatar == "" || user.avatar == null) ? require("@/assets/images/profile.jpg") : user.avatar;
          if (res.data.roles && res.data.roles.length > 0) { // 验证返回的roles是否是一个非空数组
            commit('SET_ROLES', res.data.roles)
            commit('SET_PERMISSIONS', res.data.permissions)
          } else {
            commit('SET_ROLES', ['ROLE_DEFAULT'])
          }
          commit('SET_NAME', user.userName)
          commit('SET_AVATAR', avatar)
          // License功能列表
          commit('SET_FEATURES', res.data.features)
          commit('SET_EDITION', res.data.edition)
          // 租户名称：控制台标题按「租户名称+管理系统」拼接（缺失时前端兜底「管理系统」）
          commit('SET_TENANT_NAME', res.data.tenantName)
          // 版本档位：前端据此区分 micro_lite / micro_pro 首页展示
          commit('SET_VERSION_TYPE', res.data.versionType)
          // T10.5：注入当前租户业态包编码，触发加载 biz_copy 文案映射（失败不阻断登录）
          dispatch('bizCopy/switchBizCode', res.data.bizCode, { root: true }).catch(() => {})
          resolve(res)
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 退出系统
    LogOut({ commit, state }) {
      return new Promise((resolve, reject) => {
        logout(state.token).then(() => {
          commit('SET_TOKEN', '')
          commit('SET_ROLES', [])
          commit('SET_PERMISSIONS', [])
          commit('SET_FEATURES', [])
          commit('SET_EDITION', 'BASIC')
          commit('SET_TENANT_NAME', '')
          commit('SET_VERSION_TYPE', '')
          removeToken()
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 前端 登出
    FedLogOut({ commit }) {
      return new Promise(resolve => {
        commit('SET_TOKEN', '')
        removeToken()
        resolve()
      })
    }
  }
}

export default user
