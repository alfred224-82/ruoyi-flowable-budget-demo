<template>
  <div class="app-container">
    <el-alert title="推广分销管理" type="info" description="管理业务员推广和顾客推广，各自独立配置奖励规则。" show-icon :closable="false" class="mb16" />

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover" class="entry-card" @click.native="$router.push('/microOps/distribution/salesmanRecords')">
          <div style="display:flex;align-items:center;">
            <div style="font-size:40px;margin-right:20px;color:#409eff;">
              <i class="el-icon-user"></i>
            </div>
            <div>
              <h3 style="margin:0 0 8px;">业务员推广</h3>
              <p style="color:#909399;margin:0;font-size:13px;">查看业务员拉新现金奖励、首单提成、消费返佣发放记录</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="entry-card" @click.native="$router.push('/microOps/distribution/customerRecords')">
          <div style="display:flex;align-items:center;">
            <div style="font-size:40px;margin-right:20px;color:#67c23a;">
              <i class="el-icon-s-custom"></i>
            </div>
            <div>
              <h3 style="margin:0 0 8px;">顾客推广</h3>
              <p style="color:#909399;margin:0;font-size:13px;">查看顾客分享码拉新优惠券、分享有礼、裂变奖励发放记录</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 推广统计概览 -->
    <el-card shadow="hover" class="mt16">
      <div slot="header"><span style="font-weight:600;">推广统计概览</span></div>
      <el-row :gutter="20">
        <el-col :span="8" style="text-align:center;">
          <div style="color:#909399;">总归因数</div>
          <div style="font-size:24px;font-weight:700;color:#409eff;margin-top:4px;">{{ stat.totalAttribution || 0 }}</div>
        </el-col>
        <el-col :span="8" style="text-align:center;">
          <div style="color:#909399;">奖励发放数</div>
          <div style="font-size:24px;font-weight:700;color:#e6a23c;margin-top:4px;">{{ stat.totalReward || 0 }}</div>
        </el-col>
        <el-col :span="8" style="text-align:center;">
          <div style="color:#909399;">统计周期</div>
          <div style="margin-top:4px;">
            <el-radio-group v-model="period" size="mini" @change="loadStat">
              <el-radio-button label="day">日</el-radio-button>
              <el-radio-button label="week">周</el-radio-button>
              <el-radio-button label="month">月</el-radio-button>
            </el-radio-group>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script>
import { getPerformanceStat } from '@/api/micro/distribution'

export default {
  name: 'Distribution',
  data() {
    return {
      period: 'month',
      stat: {}
    }
  },
  created() { this.loadStat() },
  methods: {
    loadStat() {
      getPerformanceStat(this.period).then(res => {
        this.stat = (res.data && res.data[0]) || {}
      })
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.mt16 { margin-top: 16px; }
.entry-card { cursor: pointer; transition: all 0.3s; }
.entry-card:hover { transform: translateY(-4px); border-color: #409eff; }
</style>
