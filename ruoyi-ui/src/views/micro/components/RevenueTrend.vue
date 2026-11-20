<template>
  <div ref="chart" :style="{ height: height, width: '100%' }"></div>
</template>

<script>
import * as echarts from 'echarts'
import resize from '@/views/dashboard/mixins/resize'

/**
 * 小微版营收趋势折线图（收入/支出/净利，按日快照 micro_daily_revenue）
 * 复用 dashboard 的 resize mixin（this.chart.resize）；data 变化自动重绘。
 */
export default {
  name: 'RevenueTrend',
  mixins: [resize],
  props: {
    data: {
      type: Array,
      default: () => []
    },
    height: {
      type: String,
      default: '360px'
    }
  },
  data() {
    return {
      chart: null
    }
  },
  watch: {
    data: {
      deep: true,
      handler() {
        this.renderChart()
      }
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.renderChart()
    })
  },
  beforeDestroy() {
    if (this.chart) {
      this.chart.dispose()
      this.chart = null
    }
  },
  methods: {
    renderChart() {
      if (!this.$refs.chart) {
        return
      }
      if (!this.chart) {
        this.chart = echarts.init(this.$refs.chart)
      }
      const list = this.data || []
      this.chart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['收入', '支出', '净利'] },
        grid: { left: 10, right: 20, bottom: 10, top: 40, containLabel: true },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: list.map(i => i.revenueDate)
        },
        yAxis: { type: 'value' },
        series: [
          { name: '收入', type: 'line', smooth: true, itemStyle: { color: '#67c23a' }, areaStyle: { opacity: 0.08 }, data: list.map(i => i.grossIncome) },
          { name: '支出', type: 'line', smooth: true, itemStyle: { color: '#f56c6c' }, data: list.map(i => i.totalExpense) },
          { name: '净利', type: 'line', smooth: true, itemStyle: { color: '#409eff' }, data: list.map(i => i.netProfit) }
        ]
      }, true)
    }
  }
}
</script>
