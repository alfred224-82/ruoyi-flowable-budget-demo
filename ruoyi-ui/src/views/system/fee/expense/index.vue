<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="单据编号" prop="billNo">
        <el-input
          v-model="queryParams.billNo"
          placeholder="请输入单据编号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="已提交" value="SUBMITTED" />
          <el-option label="超预算待审" value="OVER_BUDGET_PENDING" />
          <el-option label="已批准" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['fee:expense:add']"
        >填写报销单</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="expenseList">
      <el-table-column label="单据编号" align="center" prop="billNo" width="260" :show-overflow-tooltip="true" />
      <el-table-column label="物资小类" align="center" prop="itemCategory">
        <template slot-scope="scope">
          {{ getDictLabel(itemCategoryOptions, scope.row.itemCategory) }}
        </template>
      </el-table-column>
      <el-table-column label="部门名称" align="center" prop="deptName" width="130" />
      <el-table-column label="用途标签" align="center" prop="purposeTagName" :show-overflow-tooltip="true" />
      <el-table-column label="金额" align="center" prop="amount">
        <template slot-scope="scope">
          ¥{{ Number(scope.row.amount).toFixed(2) }}
        </template>
      </el-table-column>
      <el-table-column label="费用日期" align="center" prop="expenseDate" width="120">
        <template slot-scope="scope">
          {{ parseTime(scope.row.expenseDate, '{y}-{m}-{d}') }}
        </template>
      </el-table-column>
      <el-table-column label="报销年月" align="center" prop="expensePeriod" width="100" />
      <el-table-column label="预算科目" align="center" prop="calcBudgetSubjectName" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" prop="status" width="120">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.status === 'SUBMITTED'" type="success">已提交</el-tag>
          <el-tag v-else-if="scope.row.status === 'OVER_BUDGET_PENDING'" type="warning">超预算待审</el-tag>
          <el-tag v-else-if="scope.row.status === 'APPROVED'" type="success">已批准</el-tag>
          <el-tag v-else-if="scope.row.status === 'REJECTED'" type="danger">已驳回</el-tag>
          <el-tag v-else>{{ scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="100">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleView(scope.row)"
          >查看</el-button>
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
import { listExpenseBill } from "@/api/system/fee/expense";

export default {
  name: "Expense",
  dicts: ['fee_item_category'],
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      expenseList: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        billNo: undefined,
        status: undefined,
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    // 获取字典标签
    getDictLabel(dictOptions, value) {
      if (!value || !dictOptions) return value;
      const dict = dictOptions.find(d => d.value === value);
      return dict ? dict.label : value;
    },
    getList() {
      this.loading = true;
      listExpenseBill(this.queryParams).then(response => {
        this.expenseList = response.rows;
        this.total = response.total;
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
    handleAdd() {
      this.$router.push('/system/fee/expense/form');
    },
    handleView(row) {
      this.$router.push({ path: '/system/fee/expense/form', query: { view: '1', id: row.id } });
    }
  }
};
</script>
