<template>
  <div class="light-overview">
    <!-- 轻量收支概览三卡片（T5.19④：本月收入/支出/结余，不含大类下钻看板） -->
    <el-row :gutter="16" class="stat-row" v-loading="loading">
      <el-col :xs="24" :sm="8">
        <div class="stat-card">
          <div class="stat-title">
            <span class="stat-dot" style="background:#67C23A"></span>本月收入
          </div>
          <div class="stat-value income">{{ fmt(overview.income) }}</div>
          <div class="stat-sub">{{ monthLabel }}已核验收入合计</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="stat-card">
          <div class="stat-title">
            <span class="stat-dot" style="background:#E6A23C"></span>本月支出
          </div>
          <div class="stat-value expense">{{ fmt(overview.expense) }}</div>
          <div class="stat-sub">{{ monthLabel }}已审批报销 + 付款单合计</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="stat-card">
          <div class="stat-title">
            <span class="stat-dot" style="background:#2A7BFF"></span>本月结余
          </div>
          <div class="stat-value balance">{{ fmt(overview.balance) }}</div>
          <div class="stat-sub">本月收入 − 本月支出</div>
        </div>
      </el-col>
    </el-row>

    <!-- 底部说明 -->
    <div class="home-footer">
      基础版轻量记账：三卡片数据与项目资金流单据实时一致；大类下钻/项目利润拆解/风险预警/固定成本属高级版老板看板
    </div>
  </div>
</template>

<script>
import { getLightOverview } from '@/api/system/fee/lightOverview'

export default {
  name: "LightOverview",
  data() {
    return {
      loading: false,
      overview: {
        month: '',
        income: 0,
        expense: 0,
        balance: 0
      }
    };
  },
  computed: {
    monthLabel() {
      return this.overview.month || '--';
    }
  },
  created() {
    this.load();
  },
  methods: {
    load() {
      this.loading = true;
      getLightOverview().then(res => {
        this.overview = res.data || this.overview;
      }).finally(() => {
        this.loading = false;
      });
    },
    fmt(value) {
      const num = Number(value == null ? 0 : value);
      return '¥ ' + num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }
  }
};
</script>

<style scoped lang="scss">
.light-overview {
  padding: 20px;
  min-height: calc(100vh - 84px);
  background: linear-gradient(160deg, #f0f6ff 0%, #e9f1fc 50%, #f5f8fd 100%);
}

/* 收支结余三卡片 */
.stat-row {
  margin-bottom: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #eef2f8;
  box-shadow: 0 2px 12px rgba(42, 123, 255, 0.06);
  padding: 24px 20px;
  margin-bottom: 16px;
  transition: box-shadow 0.3s;

  &:hover {
    box-shadow: 0 4px 20px rgba(42, 123, 255, 0.12);
  }

  .stat-title {
    display: flex;
    align-items: center;
    font-size: 15px;
    font-weight: 600;
    color: #1a1a1a;
    margin-bottom: 16px;

    .stat-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      margin-right: 8px;
      flex-shrink: 0;
    }
  }

  .stat-value {
    font-size: 30px;
    font-weight: 700;
    color: #1a1a1a;
    margin-bottom: 12px;

    &.income { color: #67C23A; }
    &.expense { color: #E6A23C; }
    &.balance { color: #2A7BFF; }
  }

  .stat-sub {
    font-size: 12px;
    color: #999;
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
