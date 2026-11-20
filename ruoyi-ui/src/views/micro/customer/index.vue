<template>
  <div class="app-container">
    <el-alert
      title="顾客管理（T15.32）：防爽约信誉 + 黑名单，仅小微版·经营档(micro_pro)生效"
      type="info"
      description="customer_credit / no_show_record 均为租户级表，多租户插件自动隔离——A 店拉黑不影响该顾客在 B 店预约。推广档(micro_lite)无预约能力，因此不产生信誉数据。"
      show-icon
      :closable="false"
      class="mb8"
    />
    <el-alert
      title="黑名单唯一解除出口 = 老板手动「移出黑名单」"
      type="warning"
      description="累计爽约满 3 次由后端自动拉黑（noShowCheckJob 扫描 expire_time 已过仍「待服务」的预约 → 标记 NO_SHOW → DB 层原子 +1）。移出后 is_blacklisted=0，但 no_show_count 保留不清零以便历史追溯。被拉黑顾客受双重限制：不能提交预约 + 不接收店铺推送。主动取消(CANCELLED)不计爽约；游客预约（无顾客用户ID）仅关单不计信誉。"
      show-icon
      :closable="false"
      class="mb8"
    />

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="顾客用户ID" prop="userId">
        <el-input v-model="queryParams.userId" placeholder="请输入顾客用户ID" clearable style="width:180px;" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="黑名单状态" prop="isBlacklisted">
        <el-select v-model="queryParams.isBlacklisted" placeholder="请选择状态" clearable style="width:140px;">
          <el-option label="已拉黑" :value="1" />
          <el-option label="正常" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      title="检索能力说明"
      type="info"
      description="后端 /micro/customer/list 仅支持 userId（精确）与 isBlacklisted 两个过滤条件；顾客昵称与手机号是服务层从 sys_user 回填的展示字段，不参与检索。列表默认按「已拉黑优先 → 爽约次数倒序」排列。"
      show-icon
      :closable="false"
      class="mb8"
    />

    <el-row :gutter="10" class="mb8">
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="customerList">
      <el-table-column label="顾客" align="left" min-width="150" show-overflow-tooltip>
        <template slot-scope="scope">
          <span>{{ scope.row.customerName || '未回填昵称' }}</span>
          <span class="sub-text">UID {{ scope.row.userId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="手机号" align="center" prop="customerPhone" width="130">
        <template slot-scope="scope"><span>{{ scope.row.customerPhone || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="累计爽约" align="center" width="100">
        <template slot-scope="scope">
          <span :class="scope.row.noShowCount >= threshold ? 'danger-text' : ''">{{ scope.row.noShowCount || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="信誉状态" align="center" width="150">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.isBlacklisted === 1" type="danger" size="mini">已拉黑</el-tag>
          <el-tag v-else type="success" size="mini">正常</el-tag>
          <span v-if="scope.row.isBlacklisted !== 1" class="sub-text">再爽约 {{ remainCount(scope.row.noShowCount) }} 次自动拉黑</span>
        </template>
      </el-table-column>
      <el-table-column label="最近拉黑时间" align="center" prop="blacklistedAt" width="160">
        <template slot-scope="scope"><span>{{ scope.row.blacklistedAt || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="最近移出时间" align="center" prop="unblacklistedAt" width="160">
        <template slot-scope="scope"><span>{{ scope.row.unblacklistedAt || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="备注" align="left" prop="remark" min-width="140" show-overflow-tooltip>
        <template slot-scope="scope"><span>{{ scope.row.remark || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button type="text" size="mini" icon="el-icon-tickets" @click="handleDetail(scope.row)" v-hasPermi="['micro:customer:creditDetail']">爽约明细</el-button>
          <el-button
            v-if="scope.row.isBlacklisted === 1"
            type="text"
            size="mini"
            icon="el-icon-unlock"
            @click="handleUnblacklist(scope.row)"
            v-hasPermi="['micro:customer:unblacklist']"
          >移出黑名单</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 爽约明细（no_show_record 完整历史，按标记时间倒序） -->
    <el-dialog :title="'爽约明细 - ' + detailTitle" :visible.sync="detailOpen" width="860px" append-to-body>
      <el-descriptions :column="3" border size="small" class="mb8">
        <el-descriptions-item label="顾客">{{ detailRow.customerName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detailRow.customerPhone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="累计爽约">{{ detailRow.noShowCount || 0 }} 次</el-descriptions-item>
      </el-descriptions>
      <el-table v-loading="detailLoading" :data="noShowList" max-height="400">
        <el-table-column label="爽约订单编号" align="left" prop="orderNo" min-width="180" show-overflow-tooltip />
        <el-table-column label="原预约到店时间" align="center" prop="bookTime" width="160">
          <template slot-scope="scope"><span>{{ scope.row.bookTime || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="标记爽约时间" align="center" prop="markedAt" width="160">
          <template slot-scope="scope"><span>{{ scope.row.markedAt || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="标记来源" align="center" width="120">
          <template slot-scope="scope">
            <el-tag :type="scope.row.markedBy === 'system' ? 'info' : 'warning'" size="mini">{{ markedByLabel(scope.row.markedBy) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" align="left" prop="remark" min-width="140" show-overflow-tooltip>
          <template slot-scope="scope"><span>{{ scope.row.remark || '—' }}</span></template>
        </el-table-column>
      </el-table>
      <div class="tip">
        共 {{ noShowList.length }} 条爽约记录。明细只做追溯，不提供删除入口——移出黑名单也不会清空本列表与累计次数。
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button
          v-if="detailRow.isBlacklisted === 1"
          type="primary"
          @click="handleUnblacklist(detailRow)"
          v-hasPermi="['micro:customer:unblacklist']"
        >移出黑名单</el-button>
        <el-button @click="detailOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listCustomer, unblacklistCustomer, getCreditDetail } from '@/api/micro/customer'

export default {
  name: 'MicroCustomer',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      customerList: [],
      // 与后端 CustomerCreditServiceImpl.BLACKLIST_THRESHOLD 保持一致
      threshold: 3,
      queryParams: { pageNum: 1, pageSize: 10, userId: null, isBlacklisted: null },
      // 爽约明细
      detailOpen: false,
      detailLoading: false,
      detailTitle: '',
      detailRow: {},
      noShowList: []
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      const params = {
        pageNum: this.queryParams.pageNum,
        pageSize: this.queryParams.pageSize,
        isBlacklisted: this.queryParams.isBlacklisted
      }
      // userId 为 Long，非数字入参会导致后端类型转换失败，此处先行拦截
      const userId = (this.queryParams.userId || '').toString().trim()
      if (userId) {
        if (!/^\d+$/.test(userId)) {
          this.$modal.msgWarning('顾客用户ID必须为数字')
          this.loading = false
          return
        }
        params.userId = userId
      }
      listCustomer(params).then(response => {
        this.customerList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    /** 距自动拉黑还剩几次（未满阈值时展示，便于老板提前干预） */
    remainCount(noShowCount) {
      const remain = this.threshold - (noShowCount || 0)
      return remain > 0 ? remain : 0
    },
    markedByLabel(markedBy) {
      const map = { system: '定时任务自动', boss: '老板手动' }
      return map[markedBy] || markedBy || '—'
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.queryParams.userId = null
      this.queryParams.isBlacklisted = null
      this.handleQuery()
    },
    handleDetail(row) {
      this.detailRow = row
      this.detailTitle = (row.customerName || '顾客') + '（UID ' + row.userId + '）'
      this.noShowList = []
      this.detailOpen = true
      this.detailLoading = true
      getCreditDetail(row.userId).then(response => {
        this.noShowList = response.data || []
        this.detailLoading = false
      }).catch(() => { this.detailLoading = false })
    },
    handleUnblacklist(row) {
      this.$modal.confirm('确认将顾客「' + (row.customerName || 'UID ' + row.userId) + '」移出黑名单吗？移出后立即恢复预约与推送能力，累计爽约次数（' + (row.noShowCount || 0) + ' 次）保留不清零。')
        .then(() => unblacklistCustomer(row.userId))
        .then(() => {
          this.$modal.msgSuccess('已移出黑名单')
          this.detailOpen = false
          this.getList()
        })
        .catch(() => {})
    }
  }
}
</script>

<style scoped>
.sub-text {
  display: block;
  color: #909399;
  font-size: 12px;
}
.danger-text {
  color: #f56c6c;
  font-weight: 600;
}
.tip {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
