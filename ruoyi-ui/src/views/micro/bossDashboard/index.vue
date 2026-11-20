<template>
  <div class="app-container">
    <!-- ===== 免费版首页 ===== -->
    <template v-if="isLite">
      <div class="lite-welcome" :style="{ background: welcomeGradient }">
        <h2 class="lite-title">欢迎使用 {{ tenantName }}管理系统</h2>
        <p class="lite-subtitle">当前版本：免费版（推广档）</p>
      </div>

      <el-row :gutter="16" class="mb16">
        <el-col :span="12">
          <el-card shadow="hover">
            <div slot="header"><span style="font-weight:600;">当前版本能力</span></div>
            <div class="feature-list">
              <div class="feature-item">
                <i class="el-icon-check" style="color:#67c23a;"></i>
                <span>发布活动、生成分享海报</span>
              </div>
              <div class="feature-item">
                <i class="el-icon-check" style="color:#67c23a;"></i>
                <span>配置拉新奖励规则</span>
              </div>
              <div class="feature-item">
                <i class="el-icon-check" style="color:#67c23a;"></i>
                <span>查看推广数据与排行榜</span>
              </div>
              <div class="feature-item">
                <i class="el-icon-check" style="color:#67c23a;"></i>
                <span>查看奖励发放记录</span>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="hover">
            <div slot="header"><span style="font-weight:600;">快捷操作</span></div>
            <div class="quick-grid">
              <div class="quick-item">
                <i class="el-icon-megaphone" style="font-size:28px;color:#409EFF;"></i>
                <span>发布活动</span>
              </div>
              <div class="quick-item">
                <i class="el-icon-present" style="font-size:28px;color:#67c23a;"></i>
                <span>奖励配置</span>
              </div>
              <div class="quick-item">
                <i class="el-icon-data-line" style="font-size:28px;color:#e6a23c;"></i>
                <span>推广看板</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="hover" class="mb16 upgrade-card">
        <div slot="header"><span style="font-weight:600;">升级付费版解锁更多功能</span></div>
        <el-row :gutter="16">
          <el-col :span="8">
            <div class="upgrade-item">
              <i class="el-icon-lock" style="color:#909399;"></i>
              <span>经营驾驶舱（营收/利润/成本分析）</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="upgrade-item">
              <i class="el-icon-lock" style="color:#909399;"></i>
              <span>预约看板（可视化调度管理）</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="upgrade-item">
              <i class="el-icon-lock" style="color:#909399;"></i>
              <span>数据报表（固定支出/员工业绩/评价）</span>
            </div>
          </el-col>
        </el-row>
        <div style="text-align:center;margin-top:16px;">
          <el-button type="primary" size="small" disabled>立即升级</el-button>
        </div>
      </el-card>
    </template>

    <!-- ===== 付费版首页（完整经营驾驶舱） ===== -->
    <template v-else>
      <el-alert title="经营驾驶舱（老板专属）" type="info"
        description="核心经营数据 / 成本构成 / 业务待办与异常处理中心入口。" show-icon :closable="false" class="mb16" />

      <div v-loading="loading">
        <!-- ===== 核心经营数据 ===== -->
        <el-row :gutter="16" class="mb16">
          <el-col :span="12">
            <el-card shadow="hover">
              <div slot="header"><span style="font-weight:600;">今日概览</span></div>
              <el-row :gutter="12">
                <el-col :span="8">
                  <div class="metric">
                    <div class="metric-label">今日应收</div>
                    <div class="metric-value income">¥{{ fmtMoney(data.todayReceivable) }}</div>
                    <div class="metric-label" style="margin-top:6px;">今日实收</div>
                    <div class="metric-value" style="color:#409eff;">¥{{ fmtMoney(data.todayReceived) }}</div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="metric">
                    <div class="metric-label">支出</div>
                    <div class="metric-value expense">¥{{ fmtMoney(data.todayExpense) }}</div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="metric">
                    <div class="metric-label">净利</div>
                    <div class="metric-value" :class="netClass(data.todayNetProfit)">¥{{ fmtMoney(data.todayNetProfit) }}</div>
                  </div>
                </el-col>
              </el-row>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="hover">
              <div slot="header"><span style="font-weight:600;">本月汇总</span></div>
              <el-row :gutter="12">
                <el-col :span="8">
                  <div class="metric">
                    <div class="metric-label">本月应收</div>
                    <div class="metric-value income">¥{{ fmtMoney(data.monthReceivable) }}</div>
                    <div class="metric-label" style="margin-top:6px;">本月实收</div>
                    <div class="metric-value" style="color:#409eff;">¥{{ fmtMoney(data.monthReceived) }}</div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="metric">
                    <div class="metric-label">支出</div>
                    <div class="metric-value expense">¥{{ fmtMoney(data.monthExpense) }}</div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="metric">
                    <div class="metric-label">净利</div>
                    <div class="metric-value" :class="netClass(data.monthNetProfit)">¥{{ fmtMoney(data.monthNetProfit) }}</div>
                  </div>
                </el-col>
              </el-row>
              <div style="text-align:right;color:#909399;font-size:12px;margin-top:8px;">
                订单数：{{ data.monthOrderCount || 0 }}
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- ===== 成本构成 + 业务待办 ===== -->
        <el-row :gutter="16" class="mb16">
          <el-col :span="14">
            <el-card shadow="hover">
              <div slot="header"><span style="font-weight:600;">本月固定支出构成</span>
                <span style="float:right;color:#909399;">合计：¥{{ fmtMoney(data.monthFixedCostTotal) }}</span>
              </div>
              <div v-if="(data.costComposition || []).length === 0" style="color:#909399;text-align:center;padding:30px;">
                本月暂无固定支出配置
              </div>
              <div v-else>
                <div v-for="(item, idx) in data.costComposition" :key="idx" class="cost-bar">
                  <span class="cost-name">{{ item.name }}</span>
                  <div class="cost-bar-track">
                    <div class="cost-bar-fill" :style="{ width: item.percent + '%', backgroundColor: barColors[idx % barColors.length] }"></div>
                  </div>
                  <span class="cost-amount">¥{{ fmtMoney(item.amount) }} ({{ item.percent }}%)</span>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="10">
            <el-card shadow="hover">
              <div slot="header"><span style="font-weight:600;">业务待办</span>
                <el-button size="mini" type="primary" style="float:right;" @click="loadData">刷新</el-button>
              </div>
              <div class="todo-list">
                <div class="todo-item">
                  <i class="el-icon-date" style="color:#409EFF;"></i>
                  <span class="todo-label">待处理预约</span>
                  <el-badge :value="data.pendingBookingCount || 0" :type="(data.pendingBookingCount || 0) > 0 ? 'danger' : 'info'" class="todo-badge"></el-badge>
                </div>
                <div class="todo-item">
                  <i class="el-icon-service" style="color:#67c23a;"></i>
                  <span class="todo-label">今日待服务</span>
                  <el-badge :value="data.todayServiceCount || 0" :type="(data.todayServiceCount || 0) > 0 ? 'warning' : 'info'" class="todo-badge"></el-badge>
                </div>
                <div class="todo-item">
                  <i class="el-icon-warning" style="color:#f56c6c;"></i>
                  <span class="todo-label">合规待处理</span>
                  <el-badge :value="data.compliancePendingCount || 0" :type="(data.compliancePendingCount || 0) > 0 ? 'danger' : 'info'" class="todo-badge"></el-badge>
                </div>
                <div class="todo-item">
                  <i class="el-icon-money" style="color:#e6a23c;"></i>
                  <span class="todo-label">固定支出异常</span>
                  <el-badge :value="data.costAnomalyCount || 0" :type="(data.costAnomalyCount || 0) > 0 ? 'danger' : 'info'" class="todo-badge"></el-badge>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </template>
  </div>
</template>

<script>
import { getBossDashboard } from '@/api/micro/bossDashboard'

export default {
  name: 'BossDashboard',
  data() {
    return {
      loading: true,
      data: {},
      barColors: ['#409EFF', '#67c23a', '#e6a23c', '#f56c6c', '#909399'],
      welcomeGradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
    }
  },
  computed: {
    isLite() {
      return this.$store.getters.versionType === 'micro_lite'
    },
    tenantName() {
      return this.$store.getters.tenantName || ''
    }
  },
  created() {
    // 免费版不调经营驾驶舱接口（无数据）
    if (!this.isLite) {
      this.loadData()
    }
  },
  methods: {
    loadData() {
      this.loading = true
      getBossDashboard().then(res => {
        this.data = res.data || {}
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    fmtMoney(val) {
      if (val == null) return '0.00'
      return Number(val).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
    },
    netClass(val) {
      if (val == null) return ''
      return Number(val) >= 0 ? 'income' : 'expense'
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.metric { text-align: center; }
.metric-label { font-size: 13px; color: #909399; margin-bottom: 6px; }
.metric-value { font-size: 22px; font-weight: bold; }
.income { color: #67c23a; }
.expense { color: #f56c6c; }
.cost-bar { display: flex; align-items: center; margin-bottom: 12px; }
.cost-name { width: 80px; font-size: 13px; color: #606266; }
.cost-bar-track { flex: 1; height: 16px; background: #f0f0f0; border-radius: 8px; margin: 0 12px; overflow: hidden; }
.cost-bar-fill { height: 100%; border-radius: 8px; transition: width 0.3s; }
.cost-amount { width: 140px; text-align: right; font-size: 12px; color: #909399; }
.todo-list { }
.todo-item { display: flex; align-items: center; padding: 12px 0; border-bottom: 1px solid #f5f5f5; }
.todo-item:last-child { border-bottom: none; }
.todo-label { flex: 1; margin-left: 10px; font-size: 14px; }
.todo-badge { margin-right: 8px; }
/* 免费版首页样式 */
.lite-welcome { padding: 32px 24px; border-radius: 8px; color: #fff; text-align: center; margin-bottom: 16px; }
.lite-title { font-size: 24px; font-weight: 700; margin: 0 0 8px; }
.lite-subtitle { font-size: 14px; opacity: 0.85; margin: 0; }
.feature-list { }
.feature-item { display: flex; align-items: center; padding: 8px 0; font-size: 14px; }
.feature-item i { margin-right: 8px; }
.quick-grid { display: flex; gap: 16px; }
.quick-item { display: flex; flex-direction: column; align-items: center; padding: 16px; border-radius: 8px; flex: 1; }
.quick-item span { margin-top: 8px; font-size: 13px; }
.upgrade-card { }
.upgrade-item { display: flex; align-items: center; padding: 8px 0; font-size: 13px; color: #606266; }
.upgrade-item i { margin-right: 8px; }
</style>
