/**
 * v-hasFeature License功能开关指令
 * 用法: v-hasFeature="['FEE_MULTI_CURRENCY']"
 * 当License不包含指定功能时，元素将被移除
 */
import store from '@/store'

export default {
  inserted(el, binding, vnode) {
    const { value } = binding
    const features = store.getters && store.getters.features

    if (value && value instanceof Array && value.length > 0) {
      const hasFeature = features && features.some(feature => {
        return value.includes(feature)
      })

      if (!hasFeature) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    }
  }
}
