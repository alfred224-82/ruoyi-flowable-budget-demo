<template>
  <div class="app-container">
    <el-alert title="小微版套餐管理" type="info" :closable="false" class="mb16">
      <template slot="default">
        展示小微版各档位（lite/pro）的计费套餐配置。V1 版本为零支付模式，套餐价格仅供展示，实际续费请线下联系平台管理员。
      </template>
    </el-alert>

    <!-- 套餐概览卡片 -->
    <el-row :gutter="20" class="mb16">
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="text-align:center;">
            <div style="color:#909399;font-size:14px;">套餐总数</div>
            <div style="font-size:28px;font-weight:700;color:#409eff;margin-top:8px;">{{ overview.total || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="text-align:center;">
            <div style="color:#909399;font-size:14px;">免费档</div>
            <div style="font-size:28px;font-weight:700;color:#67c23a;margin-top:8px;">{{ (overview.plans || []).filter(p => p.price === 0 && p.needPay === 0).length }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="text-align:center;">
            <div style="color:#909399;font-size:14px;">试用档</div>
            <div style="font-size:28px;font-weight:700;color:#e6a23c;margin-top:8px;">{{ (overview.plans || []).filter(p => p.licenseType === 'TRIAL').length }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="text-align:center;">
            <div style="color:#909399;font-size:14px;">付费档</div>
            <div style="font-size:28px;font-weight:700;color:#f56c6c;margin-top:8px;">{{ (overview.plans || []).filter(p => p.needPay === 1).length }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 分档套餐列表 -->
    <el-row :gutter="20">
      <!-- micro_lite 档 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <div slot="header">
            <span style="font-weight:600;">小微版·免费档 (micro_lite)</span>
            <el-tag type="success" size="mini" style="margin-left:8px;">永久免费</el-tag>
          </div>
          <el-table :data="grouped.micro_lite || []" size="small">
            <el-table-column label="套餐名称" prop="name" min-width="140" show-overflow-tooltip />
            <el-table-column label="价格" prop="price" width="80" align="right">
              <template slot-scope="scope">¥{{ scope.row.price }}</template>
            </el-table-column>
            <el-table-column label="天数" prop="days" width="60" align="center">
              <template slot-scope="scope">{{ scope.row.days || '永久' }}</template>
            </el-table-column>
            <el-table-column label="状态" prop="status" width="70" align="center">
              <template slot-scope="scope">
                <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="mini">{{ scope.row.status === '0' ? '上架' : '下架' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- micro_pro 档 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <div slot="header">
            <span style="font-weight:600;">小微版·专业档 (micro_pro)</span>
            <el-tag type="warning" size="mini" style="margin-left:8px;">收费</el-tag>
          </div>
          <el-table :data="grouped.micro_pro || []" size="small">
            <el-table-column label="套餐名称" prop="name" min-width="140" show-overflow-tooltip />
            <el-table-column label="价格" prop="price" width="80" align="right">
              <template slot-scope="scope">¥{{ scope.row.price }}</template>
            </el-table-column>
            <el-table-column label="天数" prop="days" width="60" align="center" />
            <el-table-column label="首月" prop="firstOnly" width="60" align="center">
              <template slot-scope="scope">
                <el-tag v-if="scope.row.firstOnly === 1" type="danger" size="mini">限首次</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" prop="status" width="70" align="center">
              <template slot-scope="scope">
                <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="mini">{{ scope.row.status === '0' ? '上架' : '下架' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 备注说明 -->
    <el-card shadow="hover" class="mt16">
      <div slot="header"><span style="font-weight:600;">套餐说明</span></div>
      <div style="color:#606266;font-size:13px;line-height:2;">
        <p>1. <b>micro_lite（免费档）</b>：永久免费，need_pay=0 不激活计费周期；仅推广发布/店铺管理/活动浏览页。</p>
        <p>2. <b>micro_pro_trial（试用档）</b>：7 天免费试用，不绑卡、不自动扣款、无隐藏收费。</p>
        <p>3. <b>micro_pro_first_month（首月特惠）</b>：首月 299 元，仅首次可用（first_only=1），同租户仅一次。</p>
        <p>4. <b>micro_pro_monthly（月付）</b>：月付 499 元/30 天。</p>
        <p>5. <b>micro_pro_yearly（包年）</b>：包年 4790 元（约 8 折）。</p>
        <p style="color:#e6a23c;">★ V1 零支付合规：所有套餐价格仅供展示，实际续费请线下联系平台管理员。</p>
      </div>
    </el-card>
  </div>
</template>

<script>
import { listMicroPackage, getMicroPackageOverview } from '@/api/micro/package'

export default {
  name: 'MicroPackage',
  data() {
    return {
      loading: true,
      grouped: {},
      overview: {}
    }
  },
  created() { this.loadData() },
  methods: {
    loadData() {
      this.loading = true
      Promise.all([
        listMicroPackage(),
        getMicroPackageOverview()
      ]).then(([listRes, overviewRes]) => {
        this.grouped = listRes.data || {}
        this.overview = overviewRes.data || {}
        this.loading = false
      }).catch(() => { this.loading = false })
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.mt16 { margin-top: 16px; }
</style>
