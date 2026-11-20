<template>
    <view class="schema-renderer">
        <view v-if="loading" class="loading">加载中…</view>
        <view v-else-if="!blocks.length" class="empty">暂无配置</view>
        <view v-else>
            <view v-for="block in blocks" :key="block.code" class="block-card">
                <text class="block-title">{{ block.title || block.code }}</text>

                <!-- form 区块 -->
                <view v-if="block.type === 'form'" class="form-area">
                    <view v-for="field in (block.fields || [])" :key="field.key" class="form-item">
                        <text class="label">{{ field.label || field.key }}</text>
                        <input v-if="field.type === 'number'" class="input" type="number"
                            v-model="formData[field.key]" :placeholder="field.placeholder || ''" />
                        <input v-else class="input"
                            v-model="formData[field.key]" :placeholder="field.placeholder || ''" />
                    </view>
                </view>

                <!-- list 区块 -->
                <view v-else-if="block.type === 'list'">
                    <view v-for="(row, idx) in (block.data || [])" :key="idx" class="list-row">
                        <text v-for="col in (block.columns || [])" :key="col.key" class="list-cell">
                            {{ col.label }}: {{ row[col.key] || '-' }}
                        </text>
                    </view>
                </view>

                <!-- 其他区块：兜底展示 -->
                <view v-else class="fallback">
                    <text>区块 {{ block.type || 'unknown' }} 暂无渲染器</text>
                </view>
            </view>
        </view>
    </view>
</template>

<script>
/**
 * 小程序 Schema 渲染组件（T10.6，小微版）
 * <p>消费后端 /system/tenantBizConfig/render 下发的声明式区块，
 * 与 PC SchemaRenderer 共用同一份 Schema 契约。
 *
 * 用法：
 *   <schema-renderer auto-load @submit="onSubmit" />
 *   <schema-renderer :blocks="myBlocks" />
 */
import { get } from '../../utils/request'
import config from '../../config'

export default {
    name: 'SchemaRenderer',
    props: {
        blocks: { type: Array, default: () => [] },
        autoLoad: { type: Boolean, default: false }
    },
    data() {
        return { loading: false, innerBlocks: [], formData: {} }
    },
    computed: {
        renderBlocks() { return this.blocks.length ? this.blocks : this.innerBlocks }
    },
    created() {
        if (this.autoLoad) this.loadSchema()
    },
    methods: {
        async loadSchema() {
            this.loading = true
            try {
                const res = await get(config.api.microRenderSchema)
                const data = (res && res.data) || {}
                this.innerBlocks = data.blocks || []
                if (data.theme) this.$emit('theme', data.theme)
            } catch (e) { /* ignore */ }
            this.loading = false
        },
        getFormData() { return Object.assign({}, this.formData) }
    }
}
</script>

<style scoped>
.schema-renderer { padding: 0; }
.loading, .empty { text-align: center; color: #909399; padding: 60rpx 0; font-size: 26rpx; }
.block-card { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 20rpx; }
.block-title { font-size: 30rpx; font-weight: bold; display: block; margin-bottom: 20rpx; }
.form-item { display: flex; align-items: center; margin-bottom: 16rpx; }
.label { width: 180rpx; font-size: 26rpx; color: #606266; flex-shrink: 0; }
.input { flex: 1; height: 64rpx; border: 1rpx solid #dcdfe6; border-radius: 8rpx; padding: 0 16rpx; font-size: 26rpx; }
.list-row { display: flex; flex-wrap: wrap; padding: 12rpx 0; border-bottom: 1rpx solid #f0f2f5; }
.list-cell { font-size: 24rpx; color: #606266; margin-right: 24rpx; }
.fallback { text-align: center; color: #c0c4cc; padding: 40rpx 0; font-size: 24rpx; }
</style>
