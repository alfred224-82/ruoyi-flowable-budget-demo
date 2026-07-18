<template>
  <div class="app-container">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>预算归属异常监控表</span>
      </div>

      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button
            type="warning"
            plain
            icon="el-icon-edit"
            size="mini"
            :disabled="multiple"
            @click="handleBatchAdjust"
            v-hasPermi="['fee:report:adjust']"
          >批量调整预算归属</el-button>
        </el-col>
        <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table
        v-loading="loading"
        :data="anomalyList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="单据编号" align="center" prop="billNo" width="180" />
        <el-table-column label="物资小类" align="center" prop="itemCategory">
          <template slot-scope="scope">
            {{ getDictLabel(itemCategoryOptions, scope.row.itemCategory) }}
          </template>
        </el-table-column>
        <el-table-column label="用途标签" align="center" prop="purposeTag" />
        <el-table-column label="金额" align="right" prop="amount">
          <template slot-scope="scope">
            ¥{{ Number(scope.row.amount || 0).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="费用日期" align="center" prop="expenseDate" width="120" />
        <el-table-column label="责任中心" align="center" prop="costCenterName" />
        <el-table-column label="分摊状态" align="center" prop="allocationStatus" width="120">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.allocationStatus === 'PENDING'" type="warning">待分摊</el-tag>
            <el-tag v-else-if="scope.row.allocationStatus === 'ALLOCATED'" type="success">已分摊</el-tag>
            <el-tag v-else type="info">{{ scope.row.allocationStatus || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="异常原因" align="center" width="200">
          <template slot-scope="scope">
            <span v-if="scope.row.costCenterCode === 'PENDING'" style="color: #e6a23c;">
              待分摊责任中心
            </span>
            <span v-else-if="scope.row.allocationStatus === 'PENDING'" style="color: #f56c6c;">
              未执行分摊
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="120">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleAdjust(scope.row)"
              v-hasPermi="['fee:report:adjust']"
            >调整</el-button>
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
    </el-card>

    <!-- 调整预算归属对话框 -->
    <el-dialog title="调整预算归属" :visible.sync="adjustOpen" width="600px" append-to-body>
      <el-form ref="adjustForm" :model="adjustForm" :rules="adjustRules" label-width="120px">
        <el-form-item label="选择单据">
          <el-tag v-for="(bill, index) in selectedBills" :key="index" style="margin-right: 5px;">
            {{ bill.billNo }}
          </el-tag>
        </el-form-item>
        <el-form-item label="目标责任中心" prop="targetCostCenter">
          <el-select v-model="adjustForm.targetCostCenter" placeholder="请选择目标责任中心" style="width: 100%" filterable>
            <el-option
              v-for="center in costCenters"
              :key="center.centerCode"
              :label="center.centerName + ' (' + center.centerCode + ')'"
              :value="center.centerCode"
              :disabled="center.isPending === 1"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标预算科目" prop="targetBudgetSubject">
          <el-input v-model="adjustForm.targetBudgetSubject" placeholder="请输入目标预算科目编码" />
        </el-form-item>
        <el-form-item label="调整说明" prop="remark">
          <el-input v-model="adjustForm.remark" type="textarea" :rows="3" placeholder="请输入调整说明" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button :loading="adjustLoading" type="primary" @click="submitAdjust">确 定</el-button>
        <el-button @click="adjustOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { anomalyMonitorList } from "@/api/system/fee/report";
import { listCostCenter } from "@/api/system/fee/costCenter";

export default {
  name: "AnomalyMonitor",
  dicts: ['fee_item_category'],
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      anomalyList: [],
      selectedBills: [],
      multiple: true,
      adjustOpen: false,
      adjustLoading: false,
      costCenters: [],
      // 物资小类字典选项
      itemCategoryOptions: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10
      },
      adjustForm: {
        targetCostCenter: '',
        targetBudgetSubject: '',
        remark: ''
      },
      adjustRules: {
        targetCostCenter: [
          { required: true, message: "请选择目标责任中心", trigger: "change" }
        ],
        targetBudgetSubject: [
          { required: true, message: "请输入目标预算科目", trigger: "blur" }
        ]
      }
    };
  },
  created() {
    this.getList();
    this.loadCostCenters();
    // 加载字典数据
    this.itemCategoryOptions = this.dict.type.fee_item_category || [];
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
      anomalyMonitorList(this.queryParams).then(response => {
        this.anomalyList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    loadCostCenters() {
      listCostCenter({ status: 'ENABLED', pageNum: 1, pageSize: 100 }).then(response => {
        this.costCenters = response.rows || [];
      });
    },
    handleSelectionChange(selection) {
      this.selectedBills = selection;
      this.multiple = !selection.length;
    },
    handleAdjust(row) {
      this.selectedBills = [row];
      this.openAdjustDialog();
    },
    handleBatchAdjust() {
      if (this.selectedBills.length === 0) {
        this.$message.warning('请先选择要调整的单据');
        return;
      }
      this.openAdjustDialog();
    },
    openAdjustDialog() {
      this.adjustForm = {
        targetCostCenter: '',
        targetBudgetSubject: '',
        remark: ''
      };
      this.adjustOpen = true;
    },
    submitAdjust() {
      this.$refs["adjustForm"].validate(valid => {
        if (valid) {
          this.adjustLoading = true;
          // TODO: 调用后端接口批量调整预算归属
          const billIds = this.selectedBills.map(bill => bill.id);
          console.log('调整预算归属:', {
            billIds,
            ...this.adjustForm
          });

          // 模拟成功
          setTimeout(() => {
            this.$message.success('调整成功');
            this.adjustOpen = false;
            this.adjustLoading = false;
            this.getList();
          }, 1000);
        }
      });
    }
  }
};
</script>

<style scoped>
.box-card {
  margin: 20px;
}
.mb8 {
  margin-bottom: 8px;
}
</style>
