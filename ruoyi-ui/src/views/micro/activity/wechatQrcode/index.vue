<template>
  <div class="app-container">
    <el-alert
      title="H5推广码"
      type="info"
      description="展示 H5 二维码，顾客手机浏览器扫码直接进入登录页（带 ref_t/ref_u 参数），业绩自动归属。"
      show-icon
      :closable="false"
      class="mb8"
    />

    <!-- 二维码展示 -->
    <el-card shadow="never">
      <div slot="header"><span>H5 推广二维码</span></div>
      <div class="qrcode-container">
        <!-- 加载中 -->
        <div v-if="loading" class="qrcode-loading">
          <i class="el-icon-loading" style="font-size:24px;color:#409EFF;"></i>
          <p style="color:#909399;margin-top:8px;">正在生成 H5 推广码…</p>
        </div>
        <!-- 生成失败 -->
        <div v-else-if="error" class="qrcode-empty">
          <el-empty :description="error" />
          <el-button type="primary" size="small" @click="loadQrcode">重新生成</el-button>
        </div>
        <!-- 展示二维码 -->
        <div v-else-if="qrcodeUrl" class="qrcode-wrapper">
          <div class="qr-image-box">
            <el-image :src="qrcodeUrl" fit="contain" style="width: 280px; height: 280px;" />
          </div>
          <div class="qrcode-tip">
            <i class="el-icon-mobile-phone" style="font-size:16px;color:#409EFF;"></i>
            <span>顾客手机浏览器扫码直接进入登录页，业绩自动归属您</span>
          </div>
          <div class="qrcode-info">
            <span style="color:#909399;font-size:12px;">H5链接：{{ h5Url }}</span>
          </div>
          <!-- 归属信息 -->
          <div class="owner-info">
            <el-tag size="mini" type="info">租户：{{ tenantName || '-' }}</el-tag>
            <el-tag size="mini" type="info">人员：{{ userName || '-' }}</el-tag>
            <el-tag size="mini" type="success">角色：{{ roleText }}</el-tag>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { getMyQrcode } from '@/api/micro/promo'

const ROLE_MAP = {
  boss: '老板',
  salesman: '业务员',
  staff: '员工',
  customer: '顾客'
}

export default {
  name: 'MicroWechatQrcode',
  data() {
    return {
      loading: true,
      qrcodeUrl: null,
      h5Url: null,
      scene: null,
      userName: null,
      roleKey: null,
      tenantName: null,
      error: null
    }
  },
  computed: {
    roleText() { return ROLE_MAP[this.roleKey] || this.roleKey || '-' }
  },
  created() {
    this.loadQrcode()
  },
  methods: {
    /** 加载 H5 推广码（与 app 端 boss 推广码共用同一后端方法） */
    loadQrcode() {
      this.loading = true
      this.error = null
      getMyQrcode().then(res => {
        const data = res.data || {}
        this.qrcodeUrl = data.qrcodeUrl || null
        this.h5Url = data.h5Url || null
        this.scene = data.scene || null
        this.userName = data.userName || null
        this.roleKey = data.roleKey || null
        this.tenantName = data.tenantName || null
        this.loading = false
        if (!this.qrcodeUrl) {
          this.error = 'H5 推广码生成失败，请稍后重试'
        }
      }).catch(err => {
        this.loading = false
        this.error = (err && err.msg) || 'H5 推广码生成失败，请稍后重试'
      })
    }
  }
}
</script>

<style scoped>
.qrcode-container {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}
.qrcode-loading {
  text-align: center;
}
.qrcode-wrapper {
  text-align: center;
}
.qr-image-box {
  display: inline-block;
  padding: 12px;
  background: #fff;
  border: 2px dashed #DCDFE6;
  border-radius: 8px;
}
.qrcode-tip {
  margin-top: 16px;
  color: #606266;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.qrcode-info {
  margin-top: 8px;
  word-break: break-all;
}
.owner-info {
  margin-top: 16px;
  display: flex;
  justify-content: center;
  gap: 12px;
}
.qrcode-empty {
  width: 100%;
}
</style>
