<script>
import { getToken } from './utils/auth'
import { getSelectedStore } from './utils/store'

export default {
    onLaunch() {
        // 引导态双阶段租户识别（T8.10/T13.1）：入口页为门店选择页
        // 已登录 → 首页；未登录但已选定门店 → 登录页；未选定门店 → 停留门店选择页
        if (getToken()) {
            uni.switchTab({ url: '/pages/tab-home/index' })
        } else if (getSelectedStore()) {
            uni.reLaunch({ url: '/pages/login/index' })
        }
    }
}
</script>

<style>
page {
    background-color: #f5f6f8;
    font-size: 28rpx;
    color: #303133;
}
</style>
