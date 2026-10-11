<template>
  <!-- Admin（平台管理员）→ 平台驾驶舱（V3.7 铁律） -->
  <admin-dashboard v-if="isAdmin"></admin-dashboard>
  <!-- ★V3.7：boss（老板）→ 经营概览（micro/dashboard） -->
  <micro-dashboard v-else-if="isBoss"></micro-dashboard>
  <!-- ★v1.36 platform_admin（小微版租户管理员）→ 企业概览（T18.6） -->
  <tenant-overview v-else-if="isPlatformAdmin"></tenant-overview>
  <!-- 基础版首页 = 轻量收支概览三卡片（T5.19④）；高级版起保留综合看板 -->
  <light-overview v-else-if="isBasic"></light-overview>
  <!-- ★M21_07：小微版租户兜底——未命中 boss/platform_admin 时仍按产品线走小微经营概览，
       避免角色链路缺失时落到企业版综合看板（表现为「没有按租户区分开」） -->
  <micro-dashboard v-else-if="isMicro"></micro-dashboard>
  <div v-else class="home-dashboard">
    <!-- 顶部数据卡片 -->
    <el-row :gutter="16" class="stat-row">
      <!-- 本月预算总览 -->
      <el-col :xs="24" :sm="8">
        <div class="stat-card">
          <div class="stat-title">本月预算总览</div>
          <div class="stat-body">
            <div ref="budgetChart" class="chart-ring"></div>
            <div class="stat-legend">
              <div class="legend-item">
                <span class="legend-dot" style="background:#2A7BFF"></span>
                <span class="legend-label">总预算</span>
                <span class="legend-value">120万</span>
              </div>
              <div class="legend-item">
                <span class="legend-dot" style="background:#91caff"></span>
                <span class="legend-label">已使用</span>
                <span class="legend-value">78万</span>
              </div>
              <div class="legend-sub">剩余 42万</div>
            </div>
          </div>
        </div>
      </el-col>
      <!-- 报销单据统计 -->
      <el-col :xs="24" :sm="8">
        <div class="stat-card">
          <div class="stat-title">报销单据统计</div>
          <div class="stat-body">
            <div ref="billChart" class="chart-ring"></div>
            <div class="stat-legend">
              <div class="legend-item">
                <span class="legend-dot" style="background:#2A7BFF"></span>
                <span class="legend-label">待审批</span>
                <span class="legend-value">18</span>
              </div>
              <div class="legend-item">
                <span class="legend-dot" style="background:#b8d4fe"></span>
                <span class="legend-label">已通过</span>
                <span class="legend-value">326</span>
              </div>
              <div class="legend-sub">已驳回 42</div>
            </div>
          </div>
        </div>
      </el-col>
      <!-- 风控预警提醒（柱状图） -->
      <el-col :xs="24" :sm="8">
        <div class="stat-card">
          <div class="stat-title">
            风控预警提醒
            <i class="el-icon-arrow-down title-arrow"></i>
          </div>
          <div ref="warnBarChart" class="chart-bar"></div>
          <div class="bar-stats">
            <div class="bar-stat-item">
              <span class="bar-stat-label">待审批</span>
              <span class="bar-stat-value">18</span>
            </div>
            <div class="bar-stat-item">
              <span class="bar-stat-label">已驳回</span>
              <span class="bar-stat-value">36</span>
            </div>
            <div class="bar-stat-item">
              <span class="bar-stat-label">对应金额</span>
              <span class="bar-stat-value">78万</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 中部模块 -->
    <el-row :gutter="16" class="main-row">
      <!-- 风控预警提醒列表 -->
      <el-col :xs="24" :sm="8">
        <div class="panel-card warn-panel">
          <div class="panel-title">
            风控预警提醒
            <i class="el-icon-arrow-down title-arrow"></i>
          </div>
          <ul class="warn-list">
            <li v-for="(item, idx) in warnItems" :key="idx">
              <span class="warn-dot" :class="item.level"></span>
              <span class="warn-text">{{ item.text }}</span>
              <span class="warn-tag" v-if="item.tag">{{ item.tag }}</span>
            </li>
          </ul>
          <div class="warn-actions">
            <el-button type="primary" size="small" class="action-btn-primary" @click="$router.push('/system/preparation')">费用预算</el-button>
            <el-button size="small" class="action-btn-plain" @click="$router.push('/system/fee/expense')">我的单据</el-button>
          </div>
        </div>
      </el-col>

      <!-- 快捷操作 -->
      <el-col :xs="24" :sm="8">
        <div class="panel-card quick-panel">
          <div class="panel-title">快捷操作</div>
          <div class="quick-grid">
            <div class="quick-item quick-primary" @click="$router.push('/system/preparation/wizard')">
              <i class="el-icon-edit-outline"></i>
              <span>新建预算</span>
            </div>
            <div class="quick-item" @click="$router.push('/system/fee/expense/form')">
              <i class="el-icon-money"></i>
              <span>发起报销</span>
            </div>
            <div class="quick-item" @click="$router.push('/system/fee/expense-approval')">
              <i class="el-icon-s-check"></i>
              <span>批量审批</span>
            </div>
            <div class="quick-item" @click="$router.push('/system/fee/reserved-budget')">
              <i class="el-icon-coin"></i>
              <span>资金池配置</span>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 版本推广区 -->
      <el-col :xs="24" :sm="8">
        <div class="panel-card promo-panel">
          <div class="promo-title">解锁企业高管管控能力</div>
          <div class="promo-version">当前版本：<span>{{ editionLabel }}</span></div>
          <div class="promo-features">
            <div class="promo-feature-item" v-for="(feat, idx) in promoFeatures" :key="idx">
              <i class="el-icon-lock"></i>
              <span>{{ feat }}</span>
            </div>
          </div>
          <div class="promo-actions">
            <el-button type="primary" size="small" class="promo-btn-primary">查看版本对比</el-button>
            <el-button size="small" class="promo-btn-outline">联系商务开通</el-button>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 底部说明 -->
    <div class="home-footer">
      基础版免费使用，高级版与企业专业版为付费增值版本
    </div>
  </div>
</template>
<script>
import * as echarts from 'echarts'
import LightOverview from '@/views/dashboard/LightOverview'
import AdminDashboard from '@/views/system/dashboard/index.vue'
import TenantOverview from '@/views/micro/tenantOverview/index.vue'
import MicroDashboard from '@/views/micro/dashboard/index.vue'

export default {
  name: "Index",
  components: { LightOverview, AdminDashboard, TenantOverview, MicroDashboard },
  data() {
    return {
      warnItems: [
        { text: '预算超支预警', level: 'danger', tag: '超支' },
        { text: '发票合规提醒', level: 'warning', tag: '' },
        { text: '长期借款提醒', level: 'warning', tag: '' },
        { text: '发票合规提醒', level: 'info', tag: '' },
        { text: '低额贷款提呈', level: 'info', tag: '' },
        { text: '据旁造单草签', level: 'info', tag: '' }
      ],
      promoFeatures: [
        '自定义多级审批', '部门资金池',
        '多币种报销', '资产自动计提',
        '跨部门分摊', '盈利分析报表'
      ]
    };
  },
  computed: {
    /** V3.7：Admin（roleKey=admin）首页固定为平台驾驶舱 */
    isAdmin() {
      const roles = this.$store.getters.roles || []
      return roles.includes('admin')
    },
    /** ★V3.7：boss（老板）首页固定为经营驾驶舱（T18.7），优先级高于 platform_admin */
    isBoss() {
      const roles = this.$store.getters.roles || []
      return roles.includes('boss')
    },
    /** ★v1.36：platform_admin（小微版租户管理员）首页固定为企业概览（T18.6） */
    isPlatformAdmin() {
      const roles = this.$store.getters.roles || []
      return roles.includes('platform_admin')
    },
    isBasic() {
      return String(this.$store.getters.edition || '').toUpperCase() === 'BASIC';
    },
    /** ★M21_07：小微版租户判定（edition=MICRO 或档位 micro_*），用于首页兜底按产品线分流 */
    isMicro() {
      const edition = String(this.$store.getters.edition || '').toUpperCase();
      const versionType = String(this.$store.getters.versionType || '');
      return edition === 'MICRO' || versionType.indexOf('micro_') === 0;
    },
    editionLabel() {
      const labels = { BASIC: '免费基础版', ADVANCED: '高级版', PROFESSIONAL: '企业专业版' };
      return labels[String(this.$store.getters.edition || '').toUpperCase()] || this.$store.getters.edition;
    }
  },
  mounted() {
    if (this.isAdmin || this.isBoss || this.isPlatformAdmin || this.isBasic || this.isMicro) {
      return;
    }
    this.initBudgetChart();
    this.initBillChart();
    this.initWarnBarChart();
    this._resizeHandler = () => {
      this.budgetChart && this.budgetChart.resize();
      this.billChart && this.billChart.resize();
      this.warnBarChart && this.warnBarChart.resize();
    };
    window.addEventListener('resize', this._resizeHandler);
  },
  beforeDestroy() {
    window.removeEventListener('resize', this._resizeHandler);
    this.budgetChart && this.budgetChart.dispose();
    this.billChart && this.billChart.dispose();
    this.warnBarChart && this.warnBarChart.dispose();
  },
  methods: {
    initBudgetChart() {
      this.budgetChart = echarts.init(this.$refs.budgetChart);
      this.budgetChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c}万' },
        series: [{
          type: 'pie',
          radius: ['62%', '82%'],
          center: ['50%', '50%'],
          avoidLabelOverlap: false,
          label: {
            show: true,
            position: 'center',
            formatter: '120万',
            fontSize: 20,
            fontWeight: 'bold',
            color: '#1a1a1a'
          },
          labelLine: { show: false },
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          data: [
            { value: 78, name: '已使用', itemStyle: { color: '#91caff' } },
            { value: 42, name: '剩余', itemStyle: { color: '#2A7BFF' } }
          ]
        }]
      });
    },
    initBillChart() {
      this.billChart = echarts.init(this.$refs.billChart);
      this.billChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c}' },
        series: [{
          type: 'pie',
          radius: ['62%', '82%'],
          center: ['50%', '50%'],
          avoidLabelOverlap: false,
          label: {
            show: true,
            position: 'center',
            formatter: '386单',
            fontSize: 20,
            fontWeight: 'bold',
            color: '#1a1a1a'
          },
          labelLine: { show: false },
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          data: [
            { value: 326, name: '已通过', itemStyle: { color: '#2A7BFF' } },
            { value: 18, name: '待审批', itemStyle: { color: '#91caff' } },
            { value: 42, name: '已驳回', itemStyle: { color: '#d6e4ff' } }
          ]
        }]
      });
    },
    initWarnBarChart() {
      this.warnBarChart = echarts.init(this.$refs.warnBarChart);
      this.warnBarChart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 8, right: 8, top: 10, bottom: 4, containLabel: true },
        xAxis: {
          type: 'category',
          data: ['1/1', '1/8', '1/15', '1/22', '1/29'],
          axisLine: { lineStyle: { color: '#e4e7ed' } },
          axisLabel: { color: '#999', fontSize: 11 },
          axisTick: { show: false }
        },
        yAxis: {
          type: 'value',
          splitLine: { lineStyle: { color: '#f0f2f5' } },
          axisLabel: { color: '#999', fontSize: 11 }
        },
        series: [{
          type: 'bar',
          barWidth: 18,
          itemStyle: {
            borderRadius: [4, 4, 0, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#5a9cfe' },
              { offset: 1, color: '#2A7BFF' }
            ])
          },
          data: [5, 12, 8, 16, 10]
        }]
      });
    }
  }
};
</script>

<style scoped lang="scss">
.home-dashboard {
  padding: 20px;
  min-height: calc(100vh - 84px);
  background: linear-gradient(160deg, #f0f6ff 0%, #e9f1fc 50%, #f5f8fd 100%);
}

/* 顶部统计卡片 */
.stat-row {
  margin-bottom: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #eef2f8;
  box-shadow: 0 2px 12px rgba(42, 123, 255, 0.06);
  padding: 20px;
  margin-bottom: 16px;
  transition: box-shadow 0.3s;

  &:hover {
    box-shadow: 0 4px 20px rgba(42, 123, 255, 0.12);
  }

  .stat-title {
    font-size: 15px;
    font-weight: 600;
    color: #1a1a1a;
    margin-bottom: 12px;

    .title-arrow {
      margin-left: 6px;
      color: #c0c4cc;
      font-size: 13px;
    }
  }

  .stat-body {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .chart-ring {
    width: 120px;
    height: 120px;
    flex-shrink: 0;
  }

  .stat-legend {
    flex: 1;

    .legend-item {
      display: flex;
      align-items: center;
      margin-bottom: 10px;

      .legend-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        margin-right: 8px;
        flex-shrink: 0;
      }

      .legend-label {
        font-size: 13px;
        color: #666;
        margin-right: 12px;
      }

      .legend-value {
        font-size: 18px;
        font-weight: 700;
        color: #1a1a1a;
      }
    }

    .legend-sub {
      font-size: 12px;
      color: #999;
      padding-left: 16px;
    }
  }

  .chart-bar {
    width: 100%;
    height: 110px;
  }

  .bar-stats {
    display: flex;
    justify-content: space-around;
    margin-top: 10px;
    padding-top: 10px;
    border-top: 1px solid #f5f7fa;

    .bar-stat-item {
      text-align: center;

      .bar-stat-label {
        display: block;
        font-size: 12px;
        color: #999;
        margin-bottom: 2px;
      }

      .bar-stat-value {
        font-size: 16px;
        font-weight: 700;
        color: #1a1a1a;
      }
    }
  }
}

/* 中部面板 */
.main-row {
  margin-bottom: 16px;
}

.panel-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #eef2f8;
  box-shadow: 0 2px 12px rgba(42, 123, 255, 0.06);
  padding: 20px;
  margin-bottom: 16px;
  min-height: 320px;
  display: flex;
  flex-direction: column;
  transition: box-shadow 0.3s;

  &:hover {
    box-shadow: 0 4px 20px rgba(42, 123, 255, 0.12);
  }

  .panel-title {
    font-size: 15px;
    font-weight: 600;
    color: #1a1a1a;
    margin-bottom: 16px;

    .title-arrow {
      margin-left: 6px;
      color: #c0c4cc;
      font-size: 13px;
    }
  }
}

/* 风控预警列表 */
.warn-panel {
  .warn-list {
    list-style: none;
    margin: 0;
    padding: 0;
    flex: 1;

    li {
      display: flex;
      align-items: center;
      padding: 10px 0;
      border-bottom: 1px solid #f8f9fb;

      &:last-child {
        border-bottom: none;
      }

      .warn-dot {
        width: 7px;
        height: 7px;
        border-radius: 50%;
        margin-right: 10px;
        flex-shrink: 0;

        &.danger { background: #F56C6C; }
        &.warning { background: #E6A23C; }
        &.info { background: #91caff; }
      }

      .warn-text {
        font-size: 13px;
        color: #333;
        flex: 1;
      }

      .warn-tag {
        font-size: 11px;
        color: #F56C6C;
        background: #fef0f0;
        padding: 1px 6px;
        border-radius: 3px;
      }
    }
  }

  .warn-actions {
    display: flex;
    gap: 12px;
    margin-top: 16px;

    .action-btn-primary {
      flex: 1;
      border-radius: 8px;
      background: linear-gradient(90deg, #3d8bfd 0%, #1E69F2 100%);
      border: none;
      font-weight: 500;
    }

    .action-btn-plain {
      flex: 1;
      border-radius: 8px;
      background: #1E69F2;
      border: none;
      color: #fff;
      font-weight: 500;
    }
  }
}

/* 快捷操作 */
.quick-panel {
  .quick-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 14px;
    flex: 1;
    align-content: center;

    .quick-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 8px;
      padding: 24px 12px;
      border-radius: 10px;
      background: #f5f7fa;
      cursor: pointer;
      transition: all 0.25s;

      i {
        font-size: 24px;
        color: #666;
      }

      span {
        font-size: 13px;
        font-weight: 500;
        color: #333;
      }

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
      }

      &.quick-primary {
        background: linear-gradient(135deg, #2A7BFF 0%, #1E69F2 100%);
        box-shadow: 0 4px 14px rgba(30, 105, 242, 0.3);

        i { color: #fff; }
        span { color: #fff; }

        &:hover {
          box-shadow: 0 6px 18px rgba(30, 105, 242, 0.4);
        }
      }
    }
  }
}

/* 版本推广区 */
.promo-panel {
  .promo-title {
    font-size: 16px;
    font-weight: 700;
    color: #1a1a1a;
    margin-bottom: 10px;
  }

  .promo-version {
    font-size: 13px;
    color: #666;
    margin-bottom: 16px;

    span {
      color: #2A7BFF;
      font-weight: 600;
    }
  }

  .promo-features {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
    flex: 1;
    align-content: start;

    .promo-feature-item {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 10px 12px;
      background: #f8fafd;
      border: 1px solid #eef2f8;
      border-radius: 8px;
      font-size: 12px;
      color: #555;

      i {
        color: #c0c4cc;
        font-size: 13px;
      }
    }
  }

  .promo-actions {
    display: flex;
    gap: 12px;
    margin-top: 16px;

    .promo-btn-primary {
      flex: 1;
      border-radius: 8px;
      background: linear-gradient(90deg, #3d8bfd 0%, #1E69F2 100%);
      border: none;
      font-weight: 500;
    }

    .promo-btn-outline {
      flex: 1;
      border-radius: 8px;
      border: 1px solid #2A7BFF;
      color: #2A7BFF;
      background: #fff;
      font-weight: 500;

      &:hover {
        background: #f0f6ff;
      }
    }
  }
}

/* 底部说明 */
.home-footer {
  text-align: center;
  padding: 18px 0 8px;
  font-size: 12px;
  color: #aaa;
  letter-spacing: 1px;
}
</style>
