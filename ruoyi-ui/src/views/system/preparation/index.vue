<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="年度" prop="budgetYear">
        <el-date-picker
          v-model="queryParams.budgetYear"
          type="year"
          placeholder="选择年度"
          value-format="yyyy"
          clearable
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item label="月份" prop="budgetMonth">
        <el-select v-model="queryParams.budgetMonth" placeholder="请选择月份" clearable style="width: 150px">
          <el-option label="1月" value="1" />
          <el-option label="2月" value="2" />
          <el-option label="3月" value="3" />
          <el-option label="4月" value="4" />
          <el-option label="5月" value="5" />
          <el-option label="6月" value="6" />
          <el-option label="7月" value="7" />
          <el-option label="8月" value="8" />
          <el-option label="9月" value="9" />
          <el-option label="10月" value="10" />
          <el-option label="11月" value="11" />
          <el-option label="12月" value="12" />
        </el-select>
      </el-form-item>
      <el-form-item label="编制状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 150px">
          <el-option label="草稿" value="Draft" />
          <el-option label="审批中" value="Pending_Review" />
          <el-option label="已驳回" value="Rejected" />
          <el-option label="已通过" value="Approved" />
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
          v-hasPermi="['system:preparation:add']"
        >新建编制</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-check"
          size="mini"
          :disabled="multiple || !canBatchSubmit"
          @click="handleBatchSubmit"
          v-hasPermi="['system:preparation:submit']"
        >批量提交审核</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="preparationList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="年月" align="center" width="120">
        <template slot-scope="scope">
          {{ scope.row.budgetYear }}年{{ scope.row.budgetMonth ? String(scope.row.budgetMonth).padStart(2, '0') : '' }}月
        </template>
      </el-table-column>
      <el-table-column label="编制状态" align="center" prop="status" width="120">
        <template slot-scope="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusLabel(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="预算总额" align="center" prop="totalBudget" width="150">
        <template slot-scope="scope">
          {{ scope.row.totalBudget ? Number(scope.row.totalBudget).toFixed(2) : '0.00' }}
        </template>
      </el-table-column>
      <el-table-column label="预算单号" align="center" prop="sheetNo" width="180" />
      <el-table-column label="组织名称" align="center" prop="orgName" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="250">
        <template slot-scope="scope">
          <el-button
            v-if="scope.row.status === 'Draft' || scope.row.status === 'Rejected'"
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:preparation:edit']"
          >修改</el-button>
          <el-button
            v-if="scope.row.status === 'Draft' || scope.row.status === 'Rejected'"
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:preparation:remove']"
          >删除</el-button>
          <el-button
            v-if="scope.row.status === 'Draft' || scope.row.status === 'Rejected'"
            size="mini"
            type="text"
            icon="el-icon-check"
            @click="handleSubmit(scope.row)"
            v-hasPermi="['system:preparation:submit']"
          >提交审核</el-button>
          <el-button
            v-if="scope.row.status === 'Pending_Review'"
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleView(scope.row)"
            v-hasPermi="['system:preparation:query']"
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
import { listPreparation, delPreparation, submitPreparation, batchSubmitPreparation } from "@/api/system/preparation";

export default {
  name: "Preparation",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 选中的行数据
      selectedRows: [],
      // 非多个禁用
      multiple: true,
      // 是否可以批量提交
      canBatchSubmit: false,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 预算编制表格数据
      preparationList: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        budgetYear: undefined,
        budgetMonth: undefined,
        status: undefined
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询预算编制列表 */
    getList() {
      this.loading = true;
      listPreparation(this.queryParams).then(response => {
        this.preparationList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id);
      this.selectedRows = selection;
      this.multiple = !selection.length;
      
      // 检查是否所有选中的记录都可以批量提交（只有草稿和驳回状态可以）
      this.canBatchSubmit = selection.length > 0 && 
        selection.every(item => item.status === 'Draft' || item.status === 'Rejected');
    },
    /** 新增按钮操作 - 进入向导页面 */
    handleAdd() {
      this.$router.push({ path: '/system/preparation/wizard' });
    },
    /** 修改按钮操作 - 进入向导页面 */
    handleUpdate(row) {
      const id = row.id || this.ids[0];
      this.$router.push({ path: '/system/preparation/wizard', query: { id: id } });
    },
    /** 查看按钮操作 */
    handleView(row) {
      this.$router.push({ path: '/system/preparation/wizard', query: { id: row.id, viewOnly: true } });
    },
    /** 提交审核 */
    handleSubmit(row) {
      const id = row.id;
      this.$modal.confirm('是否确认提交预算单号为"' + row.sheetNo + '"的编制进行审核？').then(() => {
        return submitPreparation(id);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("提交成功");
      }).catch(() => {});
    },
    /** 批量提交审核 */
    handleBatchSubmit() {
      if (this.ids.length === 0) {
        this.$modal.msgWarning("请选择要提交的编制");
        return;
      }
      this.$modal.confirm('是否确认批量提交选中的 ' + this.ids.length + ' 条编制进行审核？').then(() => {
        return batchSubmitPreparation(this.ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("批量提交成功");
      }).catch(() => {});
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$modal.confirm('是否确认删除选中的预算编制数据项？').then(() => {
        return delPreparation(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 获取状态标签类型 */
    getStatusType(status) {
      const statusMap = {
        'Draft': 'info',
        'Pending_Review': 'warning',
        'Rejected': 'danger',
        'Approved': 'success'
      };
      return statusMap[status] || 'info';
    },
    /** 获取状态标签文本 */
    getStatusLabel(status) {
      const statusMap = {
        'Draft': '草稿',
        'Pending_Review': '审批中',
        'Rejected': '已驳回',
        'Approved': '已通过'
      };
      return statusMap[status] || status;
    }
  }
};
</script>
