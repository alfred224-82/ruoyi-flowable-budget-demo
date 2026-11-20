<template>
  <div class="app-container">
    <!-- ===== micro_lite 免费版：简化经营概览 ===== -->
    <template v-if="isLite">
      <el-alert
        title="经营概览（免费版）"
        type="info"
        description="当前为小微版免费版，具备推广发布、活动浏览能力。升级至付费档可解锁预约管理、人员结算、收支报表等全经营功能。"
        show-icon
        :closable="false"
        class="mb20"
      />

      <!-- 核心数据卡片 -->
      <el-row :gutter="20" class="mb20">
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">已发布活动</div>
            <div class="stat-value income">{{ promoStats.published || 0 }}</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">分享海报</div>
            <div class="stat-value profit">{{ promoStats.posterCount || 0 }}</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">服务项目</div>
            <div class="stat-value balance">{{ promoStats.serviceCount || 0 }}</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 升级付费档提示 -->
      <el-card shadow="never" class="upgrade-card">
        <div slot="header"><span style="font-weight:600;font-size:15px;">升级付费档</span></div>
        <div style="color:#606266;line-height:2;">
          <p style="margin:0 0 8px;color:#909399;font-size:13px;">升级后可解锁：</p>
          <ul style="padding-left:20px;margin:0;color:#606266;">
            <li>预约 / 订单管理</li>
            <li>人员结算（线下发放）</li>
            <li>收支流水 + 报表中心</li>
            <li>防爽约 / 黑名单</li>
            <li>评价与点赞管理</li>
          </ul>
          <p style="margin-top:12px;color:#909399;font-size:12px;">
            <i class="el-icon-service"></i> 请联系平台管理员开通付费档
          </p>
        </div>
      </el-card>
    </template>

    <!-- ===== micro_pro 付费版：经营驾驶舱（V3.7 §6.5 重新设计） ===== -->
    <template v-else>
      <!-- 顶部欢迎栏 -->
      <div class="dashboard-header mb16">
        <div class="header-left">
          <h3 class="header-title">经营驾驶舱</h3>
          <span class="header-date">{{ currentDate }}</span>
        </div>
        <div class="header-right">
          <el-button size="small" icon="el-icon-date" @click="go('/microOps/operating/daily')">经营日报</el-button>
          <el-button size="small" icon="el-icon-data-analysis" @click="go('/microOps/operating/monthly')">经营月报</el-button>
        </div>
      </div>

      <!-- 第一行：今日核心 KPI（4 卡片） -->
      <el-row :gutter="16" class="mb16">
        <el-col :span="6">
          <div class="kpi-card kpi-income">
            <div class="kpi-icon"><i class="el-icon-wallet"></i></div>
            <div class="kpi-body">
              <div class="kpi-label">今日收入</div>
              <div class="kpi-value">&yen;{{ fmtNum(bossData.todayIncome) }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="kpi-card kpi-profit">
            <div class="kpi-icon"><i class="el-icon-trophy"></i></div>
            <div class="kpi-body">
              <div class="kpi-label">今日净利</div>
              <div class="kpi-value">&yen;{{ fmtNum(bossData.todayNetProfit) }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="kpi-card kpi-order">
            <div class="kpi-icon"><i class="el-icon-document-checked"></i></div>
            <div class="kpi-body">
              <div class="kpi-label">今日服务</div>
              <div class="kpi-value">{{ bossData.todayServiceCount || 0 }}<span class="kpi-unit">次</span></div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="kpi-card kpi-balance">
            <div class="kpi-icon"><i class="el-icon-money"></i></div>
            <div class="kpi-body">
              <div class="kpi-label">资金结余</div>
              <div class="kpi-value">&yen;{{ fmtNum(position.cashBalance) }}</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 第二行：业务待办 + 异常处理中心（V3.7 核心） -->
      <el-row :gutter="16" class="mb16">
        <el-col :span="16">
          <el-card shadow="hover" class="exception-card">
            <div slot="header" class="card-header">
              <span><i class="el-icon-warning-outline"></i> 业务待办与异常处理</span>
              <el-badge :value="exceptionTotal" :hidden="exceptionTotal === 0" :max="99" />
            </div>
            <el-row :gutter="12">
              <el-col :span="8">
                <div class="exception-item" @click="go('/microOps/bookingList')">
                  <div class="exception-count" :class="{'has-alert': bossData.pendingBookingCount > 0}">{{ bossData.pendingBookingCount || 0 }}</div>
                  <div class="exception-label">待处理预约</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="exception-item" @click="go('/microOps/report/voiceRecords')">
                  <div class="exception-count" :class="{'has-alert': bossData.compliancePendingCount > 0}">{{ bossData.compliancePendingCount || 0 }}</div>
                  <div class="exception-label">合规待处理</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="exception-item" @click="go('/microOps/exceptionCenter')">
                  <div class="exception-count exception-total" :class="{'has-alert': exceptionTotal > 0}">{{ exceptionTotal }}</div>
                  <div class="exception-label">异常总数</div>
                  <el-button type="primary" size="mini" plain style="margin-top:6px;" @click.stop="go('/microOps/exceptionCenter')">进入处理中心</el-button>
                </div>
              </el-col>
            </el-row>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="shortcut-card">
            <div slot="header"><span><i class="el-icon-s-operation"></i> 快捷入口</span></div>
            <div class="shortcut-grid">
              <div class="shortcut-btn" @click="go('/microOps/bookingList')"><i class="el-icon-date"></i>预约管理</div>
              <div class="shortcut-btn" @click="go('/microOps/report/cashflow/income')"><i class="el-icon-top"></i>收入流水</div>
              <div class="shortcut-btn" @click="go('/microOps/report/cashflow/expense')"><i class="el-icon-bottom"></i>支出流水</div>
              <div class="shortcut-btn" @click="go('/microOps/distribution/rewardConfig')"><i class="el-icon-present"></i>奖励配置</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 第三行：本月经营月报 + 成本构成 -->
      <el-row :gutter="16" class="mb16">
        <el-col :span="12">
          <el-card shadow="hover">
            <div slot="header"><span><i class="el-icon-data-line"></i> 本月经营</span></div>
            <div class="month-summary">
              <!-- REQ-02 收益拆应收/实收（活动应收首次报名锁定，实收活动结束回写） -->
              <div class="month-row">
                <div class="month-item">
                  <span class="month-label">本月应收</span>
                  <span class="month-val income">&yen;{{ fmtNum(bossData.monthReceivable) }}</span>
                </div>
                <div class="month-divider">｜</div>
                <div class="month-item">
                  <span class="month-label">本月实收</span>
                  <span class="month-val profit">&yen;{{ fmtNum(bossData.monthReceived) }}</span>
                </div>
              </div>
              <div class="month-row" style="margin-top:12px;">
                <div class="month-item">
                  <span class="month-label">支出</span>
                  <span class="month-val expense">&yen;{{ fmtNum(bossData.monthExpense) }}</span>
                </div>
                <div class="month-divider">=</div>
                <div class="month-item">
                  <span class="month-label">净利</span>
                  <span class="month-val profit">&yen;{{ fmtNum(bossData.monthNetProfit) }}</span>
                </div>
              </div>
              <div class="month-row" style="margin-top:12px;">
                <div class="month-item">
                  <span class="month-label">本月订单</span>
                  <span class="month-val">{{ bossData.monthOrderCount || 0 }}<small>单</small></span>
                </div>
                <div class="month-item">
                  <span class="month-label">固定支出（月分摊）</span>
                  <span class="month-val expense">&yen;{{ fmtNum(bossData.monthFixedCostTotal) }}</span>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="hover">
            <div slot="header">
              <span><i class="el-icon-pie-chart"></i> 本月成本构成</span>
              <span style="float:right;color:#909399;font-size:12px;">金额 = 总额 ÷ 周期月数（月分摊）</span>
            </div>
            <div v-if="costComposition.length === 0" class="empty-tip">暂无固定支出数据，请前往「数据与报表 → 收支记录」管理</div>
            <div v-else class="cost-list">
              <div v-for="(item, idx) in costComposition" :key="idx" class="cost-item">
                <div class="cost-item-header">
                  <span class="cost-name">{{ item.name || item.category }}</span>
                  <span class="cost-amount">&yen;{{ fmtNum(item.amount) }}</span>
                </div>
                <el-progress :percentage="Number(item.percent || 0)" :color="costColors[idx % costColors.length]" :stroke-width="10" />
              </div>
              <div v-if="bossData.monthFixedCostTotal > 0" class="cost-total">
                合计：<span>&yen;{{ fmtNum(bossData.monthFixedCostTotal) }}</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 第四行：近 7 天营收趋势 -->
      <el-card shadow="hover">
        <div slot="header"><span><i class="el-icon-s-data"></i> 近 7 天营收趋势</span></div>
        <revenue-trend :data="trend" />
      </el-card>
    </template>
  </div>
</template>

<script>
import { cashPosition, trendReport } from '@/api/micro/report'
import { getBossDashboard } from '@/api/micro/bossDashboard'
import RevenueTrend from '@/views/micro/components/RevenueTrend'
import { checkPermi } from '@/utils/permission'
import { listPromo } from '@/api/micro/promo'

export default {
  name: 'MicroDashboard',
  components: { RevenueTrend },
  data() {
    return {
      position: {},
      trend: [],
      promoStats: { published: 0, posterCount: 0, serviceCount: 0 },
      bossData: {},
      costColors: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399']
    }
  },
  computed: {
    /** micro_lite 免费版：无预约管理权限 → 显示简化首页 */
    isLite() {
      return !checkPermi(['micro:booking:list'])
    },
    /** 成本构成列表 */
    costComposition() {
      return this.bossData.costComposition || []
    },
    /** 异常待办合计 */
    exceptionTotal() {
      const b = this.bossData
      return (b.pendingBookingCount || 0) + (b.compliancePendingCount || 0) + (b.costAnomalyCount || 0)
    },
    /** 当前日期 */
    currentDate() {
      const d = new Date()
      const weekDays = ['日', '一', '二', '三', '四', '五', '六']
      return d.getFullYear() + '年' + (d.getMonth() + 1) + '月' + d.getDate() + '日 星期' + weekDays[d.getDay()]
    }
  },
  created() {
    if (this.isLite) {
      this.loadLiteStats()
    } else {
      this.loadData()
    }
  },
  methods: {
    /** 付费版：加载经营驾驶舱全部数据 */
    loadData() {
      // ★核心聚合接口：今日KPI + 月报 + 成本构成 + 异常待办
      getBossDashboard().then(res => {
        this.bossData = res.data || {}
      }).catch(() => {})
      // 资金结余
      cashPosition().then(res => {
        this.position = res.data || {}
      }).catch(() => {})
      // 近7天趋势
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 6)
      trendReport({
        beginDate: this.parseTime(start, '{y}-{m}-{d}'),
        endDate: this.parseTime(end, '{y}-{m}-{d}')
      }).then(res => {
        this.trend = res.data || []
      }).catch(() => {})
    },
    /** 免费版：加载推广统计 */
    loadLiteStats() {
      listPromo({ pageNum: 1, pageSize: 1 }).then(res => {
        this.promoStats.published = (res && res.total) || 0
      }).catch(() => {})
      this.promoStats.posterCount = 0
      this.promoStats.serviceCount = 0
    },
    go(path) {
      this.$router.push(path)
    },
    /** 数字格式化（千分位） */
    fmtNum(val) {
      if (val == null) return '0.00'
      return Number(val).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
    }
  }
}
</script>

<style scoped>
/* ── 通用间距 ── */
.mb12 { margin-bottom: 12px; }
.mb16 { margin-bottom: 16px; }
.mb20 { margin-bottom: 20px; }

/* ── 免费版 ── */
.stat-card { text-align: center; }
.stat-label { color: #909399; font-size: 13px; margin-bottom: 10px; }
.stat-value { font-size: 28px; font-weight: 600; color: #303133; }
.income { color: #67c23a; }
.expense { color: #f56c6c; }
.profit { color: #409eff; }
.balance { color: #e6a23c; }
.upgrade-card { border-left: 3px solid #409eff; }

/* ── 付费版：顶部欢迎栏 ── */
.dashboard-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; }
.header-title { margin: 0; font-size: 20px; font-weight: 600; color: #303133; }
.header-date { margin-left: 16px; color: #909399; font-size: 13px; }

/* ── KPI 卡片 ── */
.kpi-card { display: flex; align-items: center; padding: 20px; border-radius: 8px; background: #fff; border: 1px solid #ebeef5; transition: box-shadow .3s; }
.kpi-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,.08); }
.kpi-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 22px; color: #fff; margin-right: 16px; flex-shrink: 0; }
.kpi-income .kpi-icon { background: linear-gradient(135deg, #67c23a, #85ce61); }
.kpi-profit .kpi-icon { background: linear-gradient(135deg, #409eff, #66b1ff); }
.kpi-order .kpi-icon { background: linear-gradient(135deg, #e6a23c, #f0c78a); }
.kpi-balance .kpi-icon { background: linear-gradient(135deg, #f56c6c, #f89898); }
.kpi-label { font-size: 13px; color: #909399; margin-bottom: 4px; }
.kpi-value { font-size: 24px; font-weight: 700; color: #303133; }
.kpi-unit { font-size: 13px; font-weight: 400; color: #909399; margin-left: 2px; }

/* ── 异常处理卡片 ── */
.card-header { display: flex; justify-content: space-between; align-items: center; }
.exception-item { text-align: center; padding: 16px 8px; border-radius: 8px; background: #fafafa; cursor: pointer; transition: background .2s; }
.exception-item:hover { background: #f0f9eb; }
.exception-count { font-size: 28px; font-weight: 700; color: #303133; }
.exception-count.has-alert { color: #f56c6c; }
.exception-total.has-alert { color: #e6a23c; }
.exception-label { font-size: 13px; color: #909399; margin-top: 4px; }

/* ── 快捷入口 ── */
.shortcut-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.shortcut-btn { text-align: center; padding: 12px 8px; border-radius: 6px; background: #f5f7fa; cursor: pointer; font-size: 13px; color: #606266; transition: all .2s; }
.shortcut-btn:hover { background: #ecf5ff; color: #409eff; }
.shortcut-btn i { display: block; font-size: 20px; margin-bottom: 4px; }

/* ── 本月经营 ── */
.month-summary { padding: 8px 0; }
.month-row { display: flex; align-items: center; justify-content: center; gap: 16px; }
.month-item { text-align: center; }
.month-label { display: block; font-size: 12px; color: #909399; margin-bottom: 4px; }
.month-val { font-size: 20px; font-weight: 600; }
.month-val small { font-size: 12px; font-weight: 400; color: #909399; margin-left: 2px; }
.month-divider { font-size: 18px; color: #c0c4cc; font-weight: 300; }

/* ── 成本构成 ── */
.cost-list { padding: 4px 0; }
.cost-item { margin-bottom: 14px; }
.cost-item:last-child { margin-bottom: 0; }
.cost-item-header { display: flex; justify-content: space-between; margin-bottom: 4px; }
.cost-name { font-size: 13px; color: #606266; }
.cost-amount { font-size: 13px; font-weight: 600; color: #303133; }
.cost-total { margin-top: 14px; padding-top: 10px; border-top: 1px solid #ebeef5; font-size: 13px; color: #909399; text-align: right; }
.cost-total span { font-weight: 600; color: #303133; }
.empty-tip { color: #c0c4cc; font-size: 13px; text-align: center; padding: 20px 0; }
</style>
