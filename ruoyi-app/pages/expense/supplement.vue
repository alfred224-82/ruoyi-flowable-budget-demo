<template>
    <view class="page">
        <view class="intro card">
            <text class="desc">📄 发票后补通道（T5.8）：勾选后补的报销单需在预计日期前补传发票，补传后额度由预占转实扣；超期将释放预占并标记异常。</text>
        </view>

        <!-- ========== 状态筛选 ========== -->
        <view class="filter">
            <view v-for="f in filters" :key="f.value" class="filter-item"
                :class="{ active: query.status === f.value }" @click="changeStatus(f.value)">
                <text>{{ f.label }}</text>
            </view>
        </view>

        <!-- ========== 补票记录 ========== -->
        <view v-if="!list.length && !loading" class="card"><text class="desc">暂无补票记录</text></view>
        <view v-for="item in list" :key="item.id" class="card"
            :class="{ overdue: item.status === 'OVERDUE' }">
            <view class="item-head">
                <text class="item-title">{{ item.billNo || ('报销单#' + item.billId) }}</text>
                <text class="amount">¥{{ formatAmount(item.billAmount) }}</text>
            </view>
            <view class="item-row">
                <text class="badge" :style="{ color: statusColor(SUPPLEMENT_STATUS, item.status), borderColor: statusColor(SUPPLEMENT_STATUS, item.status) }">
                    {{ statusText(SUPPLEMENT_STATUS, item.status) }}
                </text>
                <text class="desc">预计补票：{{ formatDate(item.estimatedDate) }}</text>
            </view>
            <view class="kv"><text>预占额度</text><text>¥{{ formatAmount(item.reservedAmount) }}</text></view>
            <view v-if="item.status === 'COMPLETED'" class="kv"><text>补票时间</text><text>{{ formatDate(item.supplementDate) }}</text></view>

            <!-- 待补票：上传发票 -->
            <view v-if="item.status === 'PENDING'" class="upload-area">
                <view class="img-row">
                    <view v-for="(img, idx) in uploadMap[item.id] || []" :key="idx" class="img-item">
                        <image :src="img" mode="aspectFill" class="img" @click="previewImage(item.id, idx)" />
                        <text class="img-del" @click="removeImage(item.id, idx)">✕</text>
                    </view>
                    <view class="img-add" @click="chooseImage(item)">
                        <text class="img-add-text">＋</text>
                        <text class="img-add-label">发票</text>
                    </view>
                </view>
                <button class="upload-btn" size="mini" :loading="item._uploading"
                    :disabled="!(uploadMap[item.id] || []).length" @click="submitInvoice(item)">
                    提交补票
                </button>
            </view>
        </view>

        <view v-if="list.length" class="load-tip">
            <text class="desc">{{ finished ? '— 已加载全部 —' : (loading ? '加载中…' : '上拉加载更多') }}</text>
        </view>
    </view>
</template>

<script>
import { guardPage } from '../../utils/permission'
import { get, put, upload } from '../../utils/request'
import config from '../../config'
import { formatAmount, formatDate, SUPPLEMENT_STATUS, statusText, statusColor } from '../../utils/format'

export default {
    data() {
        return {
            SUPPLEMENT_STATUS,
            filters: [
                { label: '待补票', value: 'PENDING' },
                { label: '已完成', value: 'COMPLETED' },
                { label: '已超期', value: 'OVERDUE' },
                { label: '全部', value: '' }
            ],
            query: { status: 'PENDING', pageNum: 1, pageSize: 10 },
            list: [],
            loading: false,
            finished: false,
            // 本地已上传成功的发票地址（按补票记录 id 分组）
            uploadMap: {}
        }
    },
    onLoad() {
        if (!guardPage('/pages/expense/supplement')) return
        this.loadList(true)
    },
    onPullDownRefresh() {
        this.loadList(true).finally(() => uni.stopPullDownRefresh())
    },
    onReachBottom() {
        if (!this.finished && !this.loading) {
            this.query.pageNum += 1
            this.loadList()
        }
    },
    methods: {
        formatAmount, formatDate, statusText, statusColor,
        /** 补票记录列表 */
        async loadList(reset = false) {
            if (reset) {
                this.query.pageNum = 1
                this.list = []
                this.finished = false
            }
            this.loading = true
            try {
                const params = { pageNum: this.query.pageNum, pageSize: this.query.pageSize }
                if (this.query.status) params.status = this.query.status
                const res = await get(config.api.supplementList, params)
                const rows = res.rows || []
                this.list = reset ? rows : this.list.concat(rows)
                this.finished = this.list.length >= (res.total || 0)
            } catch (e) { /* 已统一提示 */ }
            this.loading = false
        },
        changeStatus(status) {
            if (this.query.status === status) return
            this.query.status = status
            this.loadList(true)
        },
        /** 选择并上传发票图片（/system/oss/upload 需 system:oss:upload 权限，403 时提示联系财务） */
        chooseImage(item) {
            uni.chooseImage({
                count: 3,
                sizeType: ['compressed'],
                success: async (res) => {
                    for (const path of res.tempFilePaths) {
                        try {
                            const r = await upload(config.api.upload, path)
                            const url = (r.data || {}).url
                            if (url) {
                                const arr = this.uploadMap[item.id] || []
                                this.$set(this.uploadMap, item.id, arr.concat(url))
                            }
                        } catch (e) {
                            // 上传失败（多为无 oss 上传权限）已提示，继续下一张
                        }
                    }
                }
            })
        },
        previewImage(id, idx) {
            uni.previewImage({ urls: this.uploadMap[id] || [], current: idx })
        },
        removeImage(id, idx) {
            const arr = (this.uploadMap[id] || []).slice()
            arr.splice(idx, 1)
            this.$set(this.uploadMap, id, arr)
        },
        /** 提交补票（预占转实扣，后端必填 invoiceUrls 校验） */
        submitInvoice(item) {
            const urls = this.uploadMap[item.id] || []
            if (!urls.length) {
                uni.showToast({ title: '请先上传发票图片', icon: 'none' })
                return
            }
            uni.showModal({
                title: '提交补票',
                content: '确认提交 ' + urls.length + ' 张发票？提交后预占额度转为实扣。',
                success: async (res) => {
                    if (!res.confirm) return
                    this.$set(item, '_uploading', true)
                    try {
                        await put(config.api.supplementSubmit + '/' + item.id
                            + '?invoiceUrls=' + encodeURIComponent(urls.join(',')))
                        uni.showToast({ title: '补票成功', icon: 'success' })
                        this.loadList(true)
                    } catch (e) { /* 已统一提示 */ }
                    this.$set(item, '_uploading', false)
                }
            })
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 60rpx; }
.filter { display: flex; margin-bottom: 20rpx; }
.filter-item { flex: 1; text-align: center; font-size: 26rpx; color: #606266; padding: 12rpx 0; background: #fff; border-radius: 8rpx; margin-right: 12rpx; }
.filter-item:last-child { margin-right: 0; }
.filter-item.active { color: #fff; background: #409eff; }
.card { background: #fff; border-radius: 16rpx; padding: 28rpx; margin-bottom: 20rpx; }
.card.overdue { border-left: 8rpx solid #f56c6c; }
.desc { font-size: 24rpx; color: #909399; display: block; }
.item-head { display: flex; align-items: center; margin-bottom: 12rpx; }
.item-title { flex: 1; font-size: 30rpx; font-weight: bold; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.amount { font-size: 30rpx; font-weight: bold; color: #303133; margin-left: 16rpx; flex-shrink: 0; }
.item-row { display: flex; align-items: center; margin-bottom: 8rpx; }
.badge { font-size: 22rpx; border: 1rpx solid; border-radius: 8rpx; padding: 2rpx 12rpx; margin-right: 16rpx; flex-shrink: 0; }
.kv { display: flex; justify-content: space-between; padding: 8rpx 0; font-size: 26rpx; color: #303133; }
.kv text:first-child { color: #909399; }
.upload-area { margin-top: 16rpx; padding-top: 16rpx; border-top: 1rpx solid #f0f2f5; }
.img-row { display: flex; flex-wrap: wrap; }
.img-item { position: relative; width: 140rpx; height: 140rpx; margin-right: 16rpx; margin-bottom: 16rpx; }
.img { width: 140rpx; height: 140rpx; border-radius: 8rpx; }
.img-del { position: absolute; right: -8rpx; top: -8rpx; width: 36rpx; height: 36rpx; line-height: 32rpx; text-align: center; background: #f56c6c; color: #fff; font-size: 22rpx; border-radius: 50%; }
.img-add { width: 140rpx; height: 140rpx; border: 2rpx dashed #c0c4cc; border-radius: 8rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.img-add-text { font-size: 48rpx; color: #c0c4cc; line-height: 1; }
.img-add-label { font-size: 20rpx; color: #909399; margin-top: 6rpx; }
.upload-btn { margin-top: 8rpx; background: #409eff; color: #fff; border-radius: 8rpx; }
.load-tip { text-align: center; padding: 16rpx 0; }
</style>
