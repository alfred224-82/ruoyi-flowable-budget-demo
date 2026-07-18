<template>
  <div class="app-container">
    <!-- 资金池概览 -->
    <el-card v-if="fundPool" shadow="never" style="margin-bottom: 15px;">
      <el-row :gutter="20" align="middle" type="flex">
        <el-col :span="6">
          <span style="color: #909399; font-size: 13px;">我的资金池（{{ fundPoolLevelLabel }}）</span>
        </el-col>
        <el-col :span="6">
          <span style="color: #606266;">总额度：</span>
          <strong style="color: #409EFF;">¥{{ fmtAmount(fundPool.totalAmount) }}</strong>
        </el-col>
        <el-col :span="6">
          <span style="color: #606266;">已分摊：</span>
          <strong style="color: #E6A23C;">¥{{ fmtAmount(fundPool.allocatedAmount) }}</strong>
        </el-col>
        <el-col :span="6">
          <span style="color: #606266;">剩余可用：</span>
          <strong style="color: #67C23A;">¥{{ fundPoolAvailable }}</strong>
        </el-col>
      </el-row>
    </el-card>

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="申请单号" prop="appNo">
        <el-input
          v-model="queryParams.appNo"
          placeholder="请输入申请单号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="审批状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="部门审批中" value="DEPT_APPROVING" />
          <el-option label="分公司审批中" value="BRANCH_APPROVING" />
          <el-option label="总公司审批中" value="HQ_APPROVING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
          <el-option label="已撤回" value="CANCELLED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="approvalList" border>
      <el-table-column label="申请单号" prop="appNo" width="160" :show-overflow-tooltip="true" />
      <el-table-column label="申请人" prop="applicantName" width="100" />
      <el-table-column label="目标科目" prop="targetSubjectName" min-width="130" :show-overflow-tooltip="true">
        <template slot-scope="scope">
          {{ scope.row.targetSubjectName || scope.row.targetSubject }}
        </template>
      </el-table-column>
      <el-table-column label="申请金额" prop="requestAmount" width="110" align="right">
        <template slot-scope="scope">
          <span v-if="scope.row.requestAmount != null">¥{{ Number(scope.row.requestAmount).toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="预算可用余额" prop="currentBalance" width="120" align="right">
        <template slot-scope="scope">
          <span v-if="scope.row.currentBalance != null">¥{{ Number(scope.row.currentBalance).toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="当前层级" prop="currentLevel" width="90" align="center">
        <template slot-scope="scope">
          {{ levelMap[scope.row.currentLevel] || scope.row.currentLevel }}
        </template>
      </el-table-column>
      <el-table-column label="部门名称" prop="deptName" width="130" />
      <el-table-column label="状态" width="120" align="center">
        <template slot-scope="scope">
          <el-tag :type="statusTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" prop="createTime" width="150" align="center">
        <template slot-scope="scope">
          {{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right" align="center">
        <template slot-scope="scope">
          <el-button
            v-if="isPending(scope.row.status)"
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleView(scope.row)"
            style="color: #409EFF; font-weight: bold;"
          >审批</el-button>
          <el-button
            v-else
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleView(scope.row)"
          >详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script>
import { listBudgetAdjustment, getMyFundPool } from "@/api/system/fee/budgetAdjustment";

export default {
  name: "OverBudgetApproval",
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      approvalList: [],
      fundPool: null,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        appNo: undefined,
        status: undefined,
      },
      levelMap: { 1: '部门', 2: '分公司', 3: '总公司' }
    };
  },
  computed: {
    fundPoolAvailable() {
      if (!this.fundPool) return '0.00';
      return Number((this.fundPool.totalAmount || 0) - (this.fundPool.allocatedAmount || 0)).toFixed(2);
    },
    fundPoolLevelLabel() {
      if (!this.fundPool) return '';
      const map = { 'DEPT': '部门', 'BRANCH': '分公司', 'HQ': '总公司' };
      return map[this.fundPool.levelType] || this.fundPool.levelType || '';
    }
  },
  created() {
    this.getList();
    this.loadFundPool();
  },
  methods: {
    fmtAmount(val) {
      return Number(val || 0).toFixed(2);
    },
    isPending(status) {
      return ['DEPT_APPROVING', 'BRANCH_APPROVING', 'HQ_APPROVING'].includes(status);
    },
    async loadFundPool() {
      try {
        const res = await getMyFundPool();
        this.fundPool = res.data;
      } catch (e) {
        console.error('加载资金池失败', e);
      }
    },
    getList() {
      this.loading = true;
      listBudgetAdjustment(this.queryParams).then(response => {
        this.approvalList = response.rows || [];
        this.total = response.total || 0;
        this.loading = false;
      }).catch((error) => {
        console.error('查询申请列表失败:', error);
        this.approvalList = [];
        this.total = 0;
        this.loading = false;
      });
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    handleView(row) {
      this.$router.push('/system/fee/budget-adjustment/approvalDetail/' + row.id);
    },
    statusTagType(status) {
      const map = {
        'DEPT_APPROVING': 'warning', 'BRANCH_APPROVING': 'warning', 'HQ_APPROVING': 'warning',
        'APPROVED': 'success', 'REJECTED': 'danger', 'ESCALATED': 'info', 'CANCELLED': 'info'
      };
      return map[status] || 'info';
    },
    statusLabel(status) {
      const map = {
        'DEPT_APPROVING': '部门审批中', 'BRANCH_APPROVING': '分公司审批中', 'HQ_APPROVING': '总公司审批中',
        'APPROVED': '已通过', 'REJECTED': '已驳回', 'ESCALATED': '已转审', 'CANCELLED': '已撤回'
      };
      return map[status] || status;
    }
  }
};
</script>
