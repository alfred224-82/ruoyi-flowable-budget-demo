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
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="el-icon-upload2"
          size="mini"
          @click="handleImport"
          v-hasPermi="['fee:expense:import']"
        >导入</el-button>
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
      <!-- V5.4: 币种 -->
      <el-table-column label="币种" align="center" prop="currencyCode" width="80">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.currencyCode !== 'CNY'" type="warning" size="mini">{{ scope.row.currencyCode }}</el-tag>
          <span v-else>CNY</span>
        </template>
      </el-table-column>
      <!-- V5.4: 补助金额 -->
      <el-table-column label="补助" align="center" prop="subsidyAmount" width="100">
        <template slot-scope="scope">
          <span v-if="scope.row.subsidyAmount">¥{{ Number(scope.row.subsidyAmount).toFixed(2) }}</span>
          <span v-else>-</span>
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
      <!-- V5.4: 合规状态 -->
      <el-table-column label="合规" align="center" prop="complianceStatus" width="100">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.complianceStatus === 'PASS'" type="success" size="mini">通过</el-tag>
          <el-tag v-else-if="scope.row.complianceStatus === 'FAIL'" type="danger" size="mini">不通过</el-tag>
          <el-tag v-else-if="scope.row.complianceStatus === 'PENDING_SEAL'" type="warning" size="mini">待盖章</el-tag>
          <span v-else>-</span>
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

    <!-- 导入弹窗 -->
    <el-dialog :title="upload.title" :visible.sync="upload.open" width="400px" append-to-body>
      <el-upload
        ref="upload"
        :limit="1"
        accept=".xlsx, .xls"
        :headers="upload.headers"
        :action="upload.url"
        :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress"
        :on-success="handleFileSuccess"
        :auto-upload="false"
        drag
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip text-center" slot="tip">
          <div class="el-upload__tip" slot="tip">
            <el-checkbox v-model="upload.updateSupport" /> 是否更新已经存在的数据
          </div>
          <span>仅允许导入xls、xlsx格式文件。</span>
          <el-link type="primary" :underline="false" style="font-size:12px;vertical-align: baseline;" @click="importTemplate">下载模板</el-link>
        </div>
      </el-upload>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitFileForm">确 定</el-button>
        <el-button @click="upload.open = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listExpenseBill, importTemplate } from "@/api/system/fee/expense";
import { getToken } from "@/utils/auth";

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
      },
      // 导入参数
      upload: {
        open: false,
        title: "报销单导入",
        isUploading: false,
        updateSupport: false,
        headers: { Authorization: "Bearer " + getToken() },
        url: process.env.VUE_APP_BASE_API + "/fee/expense/importData"
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
    },
    /** 导入按钮 */
    handleImport() {
      this.upload.open = true;
    },
    /** 下载模板 */
    importTemplate() {
      importTemplate().then(response => {
        this.$download.saveAs(response, "报销单导入模板.xlsx");
      });
    },
    /** 文件上传中 */
    handleFileUploadProgress() {
      this.upload.isUploading = true;
    },
    /** 文件上传成功 */
    handleFileSuccess(response) {
      this.upload.isUploading = false;
      this.$refs.upload.clearFiles();
      this.$alert("<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" + response.msg + "</div>", "导入结果", { dangerouslyUseHTMLString: true });
      this.getList();
    },
    /** 提交上传文件 */
    submitFileForm() {
      this.$refs.upload.submit();
    }
  }
};
</script>
