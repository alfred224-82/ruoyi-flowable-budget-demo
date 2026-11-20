<template>
  <div class="app-container">
    <!-- 发起结算面板 -->
    <el-card shadow="never" style="margin-bottom:12px;">
      <div slot="header">
        <span style="font-weight:600;">发起结算</span>
        <span style="color:#909399;font-size:12px;margin-left:8px;">选择员工与结算周期 → 预览应发金额 → 生成「待发放」结算单（★只记账不打款）</span>
      </div>
      <el-form :model="genForm" ref="genForm" :rules="genRules" :inline="true" size="small" label-width="80px">
        <el-form-item label="结算员工" prop="staffUserId">
          <el-select v-model="genForm.staffUserId" placeholder="请选择员工" clearable filterable style="width:200px;">
            <el-option v-for="s in staffOptions" :key="s.userId" :label="staffLabel(s)" :value="s.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="周期起" prop="periodStart">
          <el-date-picker v-model="genForm.periodStart" type="date" value-format="yyyy-MM-dd" placeholder="开始日期" style="width:160px;" />
        </el-form-item>
        <el-form-item label="周期止" prop="periodEnd">
          <el-date-picker v-model="genForm.periodEnd" type="date" value-format="yyyy-MM-dd" placeholder="结束日期" style="width:160px;" />
        </el-form-item>
        <el-form-item>
          <el-button icon="el-icon-view" size="mini" @click="handlePreview" v-hasPermi="['micro:settle:create']">预览应发</el-button>
          <el-button type="primary" icon="el-icon-plus" size="mini" @click="handleGenerate" v-hasPermi="['micro:settle:generate']">生成结算单</el-button>
        </el-form-item>
      </el-form>
      <el-alert v-if="preview" type="success" :closable="false" show-icon :title="'预览：' + (preview.staffName || '该员工') + ' 周期内完成 ' + (preview.totalOrders || 0) + ' 单，应发金额 ￥' + (preview.totalAmount || 0)" />
    </el-card>

    <!-- 结算单列表 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="发放状态" prop="payStatus">
        <el-select v-model="queryParams.payStatus" placeholder="请选择发放状态" clearable>
          <el-option label="待发放" value="PENDING" />
          <el-option label="已线下发放" value="PAID_OFFLINE" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['micro:settle:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="settleList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="结算单号" align="center" prop="settleNo" width="170" show-overflow-tooltip />
      <el-table-column label="员工" align="center" prop="staffName" width="100" />
      <el-table-column label="结算周期" align="center" width="200">
        <template slot-scope="scope">{{ scope.row.periodStart }} ~ {{ scope.row.periodEnd }}</template>
      </el-table-column>
      <el-table-column label="完成单量" align="center" prop="totalOrders" width="90" />
      <el-table-column label="应发金额" align="center" prop="totalAmount" width="110">
        <template slot-scope="scope"><span style="color:#f56c6c;font-weight:600;">￥{{ scope.row.totalAmount }}</span></template>
      </el-table-column>
      <el-table-column label="发放状态" align="center" prop="payStatus" width="110">
        <template slot-scope="scope">
          <el-tag :type="scope.row.payStatus === 'PAID_OFFLINE' ? 'success' : 'warning'" size="small">{{ scope.row.payStatus === 'PAID_OFFLINE' ? '已线下发放' : '待发放' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标记人" align="center" prop="markedBy" width="90">
        <template slot-scope="scope"><span>{{ scope.row.markedBy || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="标记时间" align="center" prop="markedTime" width="150">
        <template slot-scope="scope"><span>{{ scope.row.markedTime ? parseTime(scope.row.markedTime) : '—' }}</span></template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150" fixed="right">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-money" @click="handleMarkPaid(scope.row)" v-if="scope.row.payStatus === 'PENDING'" v-hasPermi="['micro:settle:markPaid']">标记已线下发放</el-button>
          <span v-else style="color:#909399;">已发放</span>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { listSettle, previewSettle, generateSettle, markPaid, delSettle } from '@/api/micro/settle'
import { listStaff } from '@/api/micro/userRole'

export default {
  name: 'MicroSettle',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      settleList: [],
      ids: [],
      multiple: true,
      staffOptions: [],
      preview: null,
      genForm: { staffUserId: null, periodStart: null, periodEnd: null },
      genRules: {
        staffUserId: [{ required: true, message: '结算员工不能为空', trigger: 'change' }],
        periodStart: [{ required: true, message: '结算周期起不能为空', trigger: 'change' }],
        periodEnd: [{ required: true, message: '结算周期止不能为空', trigger: 'change' }]
      },
      queryParams: { pageNum: 1, pageSize: 10, payStatus: null }
    }
  },
  created() {
    this.getList()
    this.getStaffOptions()
  },
  methods: {
    getList() {
      this.loading = true
      listSettle(this.queryParams).then(response => {
        this.settleList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    getStaffOptions() {
      listStaff({ roleCode: 'salesman', status: '0', pageNum: 1, pageSize: 200 }).then(response => {
        this.staffOptions = response.rows || []
      })
    },
    staffLabel(s) {
      const name = s.nickName || s.userName || '员工'
      return s.phonenumber ? name + '（' + s.phonenumber + '）' : name
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    // 预览：选择员工+周期→自动汇总单量→算应发金额（不落库）
    handlePreview() {
      this.$refs['genForm'].validate(valid => {
        if (valid) {
          previewSettle(this.genForm.staffUserId, this.genForm.periodStart, this.genForm.periodEnd).then(response => {
            this.preview = response.data
          })
        }
      })
    },
    // 生成「待发放」结算单
    handleGenerate() {
      this.$refs['genForm'].validate(valid => {
        if (valid) {
          this.$modal.confirm('确认为该员工生成结算单？生成后状态为「待发放」，线下转账后再点「标记已线下发放」。').then(() => {
            return generateSettle(this.genForm)
          }).then(() => {
            this.$modal.msgSuccess('结算单已生成（待发放）')
            this.preview = null
            this.getList()
          }).catch(() => {})
        }
      })
    },
    // ★标记已线下发放：线下转账后点此，后端改状态 + 记 cash_flow 支出流水（不调支付接口）
    handleMarkPaid(row) {
      this.$modal.confirm('确认已通过线下转账发放结算单「' + row.settleNo + '」￥' + row.totalAmount + '？将自动记一笔支出流水。').then(() => {
        return markPaid(row.id)
      }).then(() => {
        this.$modal.msgSuccess('已标记线下发放并记入支出流水')
        this.getList()
      }).catch(() => {})
    },
    // 多选
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.multiple = !selection.length
    },
    // 删除结算单（仅允许删除「待发放」状态）
    handleDelete() {
      this.$modal.confirm('确认删除选中的 ' + this.ids.length + ' 条结算单？仅「待发放」状态可删除。').then(() => {
        return delSettle(this.ids)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.getList()
      }).catch(() => {})
    }
  }
}
</script>
