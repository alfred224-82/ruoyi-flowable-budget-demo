<template>
  <div class="app-container">
    <el-alert
      title="价格调整记录（T15.4 / T15.11 ④）：改价历史只读查询"
      type="info"
      description="price_change_log 为租户级表，多租户插件自动隔离。记录由「服务项目编辑 / 改价确认(micro:service:editConfirm)」时后端自动写入，本模块不提供任何新增、修改、删除入口。列表按改价时间倒序（最近改价在前）。"
      show-icon
      :closable="false"
      class="mb8"
    />
    <el-alert
      title="本页不提供改价时间范围筛选"
      type="warning"
      description="后端 PriceChangeLogBo 的 beginTime / endTime 是顶层 java.util.Date 字段且未标注 @DateTimeFormat，而 spring.mvc.format.date-time 只作用于 java.time.* 类型，传字符串会触发类型转换失败（HTTP 400）。在补齐后端转换器之前，请先用「服务项目」缩小范围，再在表格内按时间倒序查看；需要完整轨迹可点「价格轨迹」一次性拉取该项目全部记录。"
      show-icon
      :closable="false"
      class="mb8"
    />

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="服务项目" prop="itemId">
        <el-select
          v-model="queryParams.itemId"
          placeholder="全部项目（可输入项目ID精确过滤）"
          clearable
          filterable
          allow-create
          default-first-option
          style="width:280px;"
        >
          <el-option v-for="item in itemOptions" :key="item.id" :label="item.name + '（ID ' + item.id + '）'" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="info" plain icon="el-icon-data-line" size="mini" :disabled="!queryParams.itemId" @click="handleTrack" v-hasPermi="['micro:priceLog:list']">价格轨迹</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="logList">
      <el-table-column label="服务项目" align="left" min-width="180" show-overflow-tooltip>
        <template slot-scope="scope">
          <span>{{ scope.row.itemName || '项目已删除' }}</span>
          <span class="sub-text">ID {{ scope.row.itemId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="调整前单价" align="right" width="120">
        <template slot-scope="scope"><span class="price-text">￥{{ scope.row.oldPrice }}</span></template>
      </el-table-column>
      <el-table-column label="调整后单价" align="right" width="120">
        <template slot-scope="scope"><span class="price-text">￥{{ scope.row.newPrice }}</span></template>
      </el-table-column>
      <el-table-column label="涨跌" align="center" width="150">
        <template slot-scope="scope">
          <el-tag :type="diffTagType(scope.row)" size="mini">{{ diffText(scope.row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作人" align="center" prop="operator" width="120" show-overflow-tooltip>
        <template slot-scope="scope"><span>{{ scope.row.operator || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="改价时间" align="center" prop="changeTime" width="160" />
      <el-table-column label="备注" align="left" prop="remark" min-width="180" show-overflow-tooltip>
        <template slot-scope="scope"><span>{{ scope.row.remark || '—' }}</span></template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 价格轨迹：指定服务项目的完整改价历史（不分页） -->
    <el-dialog :title="'价格轨迹 - ' + trackTitle" :visible.sync="trackOpen" width="820px" append-to-body>
      <el-table v-loading="trackLoading" :data="trackList" max-height="420">
        <el-table-column label="改价时间" align="center" prop="changeTime" width="160" />
        <el-table-column label="调整前" align="right" width="110">
          <template slot-scope="scope"><span class="price-text">￥{{ scope.row.oldPrice }}</span></template>
        </el-table-column>
        <el-table-column label="调整后" align="right" width="110">
          <template slot-scope="scope"><span class="price-text">￥{{ scope.row.newPrice }}</span></template>
        </el-table-column>
        <el-table-column label="涨跌" align="center" width="140">
          <template slot-scope="scope">
            <el-tag :type="diffTagType(scope.row)" size="mini">{{ diffText(scope.row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作人" align="center" prop="operator" width="110" show-overflow-tooltip />
        <el-table-column label="备注" align="left" prop="remark" min-width="150" show-overflow-tooltip>
          <template slot-scope="scope"><span>{{ scope.row.remark || '—' }}</span></template>
        </el-table-column>
      </el-table>
      <div class="tip">
        共 {{ trackList.length }} 条改价记录（change_time 倒序）。首条为最近一次改价，末条可追溯至该项目的初始定价调整。
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="trackOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listPriceLog, listPriceLogByItem } from '@/api/micro/priceLog'
import { onSaleService } from '@/api/micro/service'
import { checkPermi } from '@/utils/permission'

export default {
  name: 'MicroPriceLog',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      logList: [],
      itemOptions: [],
      queryParams: { pageNum: 1, pageSize: 10, itemId: null },
      // 价格轨迹
      trackOpen: false,
      trackLoading: false,
      trackTitle: '',
      trackList: []
    }
  },
  created() {
    this.getList()
    this.loadItemOptions()
  },
  methods: {
    getList() {
      this.loading = true
      listPriceLog(this.queryParams).then(response => {
        this.logList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    /**
     * 服务项目下拉候选：/micro/service/onSale 需 micro:service:list（本页 perms 仅 micro:priceLog:list），
     * 无权限时静默降级——下拉仍支持 allow-create 手工输入项目ID；且该接口只返回在售项目，
     * 已下架项目的改价历史同样需要手工输入ID才能过滤。
     */
    loadItemOptions() {
      if (!checkPermi(['micro:service:list'])) {
        return
      }
      onSaleService().then(response => {
        this.itemOptions = response.data || []
      }).catch(() => { this.itemOptions = [] })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.queryParams.itemId = null
      this.handleQuery()
    },
    /** 涨跌金额（newPrice - oldPrice），价格字段为 BigDecimal 序列化后的数值/字符串 */
    diffValue(row) {
      const oldPrice = Number(row.oldPrice || 0)
      const newPrice = Number(row.newPrice || 0)
      return newPrice - oldPrice
    },
    diffText(row) {
      const diff = this.diffValue(row)
      if (diff === 0) {
        return '持平'
      }
      const oldPrice = Number(row.oldPrice || 0)
      const sign = diff > 0 ? '+' : ''
      const percent = oldPrice > 0 ? '（' + sign + ((diff / oldPrice) * 100).toFixed(1) + '%）' : ''
      return sign + diff.toFixed(2) + percent
    },
    diffTagType(row) {
      const diff = this.diffValue(row)
      if (diff > 0) {
        return 'danger'
      }
      return diff < 0 ? 'success' : 'info'
    },
    handleTrack() {
      const itemId = this.queryParams.itemId
      if (!itemId) {
        this.$modal.msgWarning('请先选择服务项目')
        return
      }
      const hit = this.itemOptions.filter(item => item.id === itemId)[0]
      this.trackTitle = hit ? hit.name : ('项目 ID ' + itemId)
      this.trackList = []
      this.trackOpen = true
      this.trackLoading = true
      listPriceLogByItem(itemId).then(response => {
        this.trackList = response.data || []
        this.trackLoading = false
      }).catch(() => { this.trackLoading = false })
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
.price-text {
  font-family: Consolas, Monaco, monospace;
}
.tip {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
