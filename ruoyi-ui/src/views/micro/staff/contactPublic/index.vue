<template>
  <div class="app-container">
    <el-alert
      title="员工联系方式公开开关（M34 T34.5，§八之二）"
      type="info"
      description="按员工单独设置「公开手机号」/「公开微信号」。新入职员工默认全关，由老板或员工本人开启。开启后，顾客在 H5 端点击该员工头像弹名片时，才会显示对应的「打电话」/「加微信」入口；两开关全关时名片仅显示头像姓名并提示「请通过门店联系」。"
      show-icon
      :closable="false"
      class="mb8"
    />

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-refresh" size="mini" @click="getList">刷新</el-button>
      </el-col>
      <right-toolbar :search="false" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="员工" align="left" min-width="180">
        <template slot-scope="scope">
          <div class="staff-cell">
            <el-avatar :size="36" :src="scope.row.avatar">{{ (scope.row.nickName || '?').charAt(0) }}</el-avatar>
            <span class="staff-name">{{ scope.row.nickName || '-' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="公开手机号" align="center" width="140">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.showPhone"
            :active-value="1"
            :inactive-value="0"
            @change="onToggle(scope.row, 'showPhone', $event)"
          />
        </template>
      </el-table-column>
      <el-table-column label="公开微信号" align="center" width="140">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.showWechat"
            :active-value="1"
            :inactive-value="0"
            @change="onToggle(scope.row, 'showWechat', $event)"
          />
        </template>
      </el-table-column>
      <el-table-column label="微信二维码" align="center" width="120">
        <template slot-scope="scope">
          <el-image
            v-if="scope.row.wechatQrcodeUrl"
            :src="scope.row.wechatQrcodeUrl"
            :preview-src-list="[scope.row.wechatQrcodeUrl]"
            style="width: 48px; height: 48px; border-radius: 4px;"
            fit="cover"
          />
          <span v-else style="color:#c0c4cc;">未上传</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160" fixed="right">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-picture" @click="openQrcode(scope.row)" v-hasPermi="['micro:staff:manage']">设置二维码</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 微信二维码设置弹窗 -->
    <el-dialog title="设置员工微信二维码" :visible.sync="qrcodeOpen" width="440px" append-to-body>
      <div class="qr-tip">顾客在名片中点击「加微信」将展示此二维码，长按识别添加。建议上传清晰的方形二维码图片。</div>
      <image-upload v-model="qrcodeForm.wechatQrcodeUrl" :limit="1" />
      <div slot="footer">
        <el-button type="primary" @click="submitQrcode">确 定</el-button>
        <el-button @click="qrcodeOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listContactPublic, updateContactPublic } from '@/api/micro/staffContactPublic'

export default {
  name: 'StaffContactPublic',
  data() {
    return {
      loading: false,
      dataList: [],
      qrcodeOpen: false,
      qrcodeForm: { staffId: null, showPhone: 0, showWechat: 0, wechatQrcodeUrl: '' }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listContactPublic().then(res => {
        this.dataList = res.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    /** 切换开关即时保存（失败回滚） */
    onToggle(row, field, val) {
      const payload = {
        staffId: row.userId,
        showPhone: row.showPhone,
        showWechat: row.showWechat,
        wechatQrcodeUrl: row.wechatQrcodeUrl
      }
      updateContactPublic(payload).then(() => {
        this.$modal.msgSuccess((field === 'showPhone' ? '手机号' : '微信号') + (val === 1 ? '已公开' : '已隐藏'))
      }).catch(() => {
        // 保存失败回滚开关
        row[field] = val === 1 ? 0 : 1
      })
    },
    openQrcode(row) {
      this.qrcodeForm = {
        staffId: row.userId,
        showPhone: row.showPhone,
        showWechat: row.showWechat,
        wechatQrcodeUrl: row.wechatQrcodeUrl || ''
      }
      this.qrcodeOpen = true
    },
    submitQrcode() {
      updateContactPublic(this.qrcodeForm).then(() => {
        this.$modal.msgSuccess('二维码已保存')
        this.qrcodeOpen = false
        this.getList()
      })
    }
  }
}
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.staff-cell { display: flex; align-items: center; gap: 10px; }
.staff-name { font-size: 14px; color: #303133; }
.qr-tip { font-size: 13px; color: #909399; margin-bottom: 12px; line-height: 1.6; }
</style>
