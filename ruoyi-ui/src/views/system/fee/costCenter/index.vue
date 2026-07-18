<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="责任中心编码" prop="centerCode">
        <el-input
          v-model="queryParams.centerCode"
          placeholder="请输入责任中心编码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="责任中心名称" prop="centerName">
        <el-input
          v-model="queryParams.centerName"
          placeholder="请输入责任中心名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="启用" value="ENABLED" />
          <el-option label="禁用" value="DISABLED" />
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
          v-hasPermi="['fee:costCenter:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['fee:costCenter:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['fee:costCenter:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="costCenterList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="责任中心编码" align="center" prop="centerCode" />
      <el-table-column label="责任中心名称" align="center" prop="centerName" />
      <el-table-column label="默认会计大类" align="center" prop="defaultAccountingClass">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.defaultAccountingClass === 'EXPENSE'" type="warning">费用类</el-tag>
          <el-tag v-else-if="scope.row.defaultAccountingClass === 'COST'" type="success">成本类</el-tag>
          <el-tag v-else>{{ scope.row.defaultAccountingClass }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="是否待分摊中心" align="center" prop="isPending" width="120">
        <template slot-scope="scope">
          <el-tag :type="scope.row.isPending === 1 ? 'danger' : 'info'">
            {{ scope.row.isPending === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 'ENABLED' ? 'success' : 'danger'">
            {{ scope.row.status === 'ENABLED' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['fee:costCenter:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-setting"
            @click="handleDeptMapping(scope.row)"
            v-hasPermi="['fee:costCenter:edit']"
          >部门关联</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['fee:costCenter:remove']"
          >删除</el-button>
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

    <!-- 添加或修改责任中心对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="责任中心编码" prop="centerCode">
          <el-input v-model="form.centerCode" placeholder="请输入责任中心编码" maxlength="50" />
        </el-form-item>
        <el-form-item label="责任中心名称" prop="centerName">
          <el-input v-model="form.centerName" placeholder="请输入责任中心名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="默认会计大类" prop="defaultAccountingClass">
          <el-select v-model="form.defaultAccountingClass" placeholder="请选择会计大类" style="width: 100%">
            <el-option label="费用类(EXPENSE)" value="EXPENSE" />
            <el-option label="成本类(COST)" value="COST" />
            <el-option label="资产类(ASSET)" value="ASSET" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否待分摊中心" prop="isPending">
          <el-radio-group v-model="form.isPending">
            <el-radio :label="1">是</el-radio>
            <el-radio :label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="ENABLED">启用</el-radio>
            <el-radio label="DISABLED">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button :loading="buttonLoading" type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 部门关联对话框 -->
    <el-dialog title="部门关联设置" :visible.sync="deptOpen" width="600px" append-to-body>
      <el-tree
        :data="deptTree"
        show-checkbox
        node-key="id"
        :default-checked-keys="checkedDeptIds"
        ref="deptTree"
        :props="{ label: 'label', children: 'children' }"
      ></el-tree>
      <div slot="footer" class="dialog-footer">
        <el-button :loading="deptLoading" type="primary" @click="submitDeptMapping">确 定</el-button>
        <el-button @click="deptOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listCostCenter, getCostCenter, delCostCenter, addCostCenter, updateCostCenter, saveDeptMapping, queryDeptList } from "@/api/system/fee/costCenter";
import { treeselect } from "@/api/system/dept";

export default {
  name: "CostCenter",
  data() {
    return {
      buttonLoading: false,
      deptLoading: false,
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      costCenterList: [],
      title: "",
      open: false,
      deptOpen: false,
      currentCenterCode: "",
      deptTree: [],
      checkedDeptIds: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        centerCode: undefined,
        centerName: undefined,
        status: undefined,
      },
      form: {},
      rules: {
        centerCode: [
          { required: true, message: "责任中心编码不能为空", trigger: "blur" }
        ],
        centerName: [
          { required: true, message: "责任中心名称不能为空", trigger: "blur" }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      listCostCenter(this.queryParams).then(response => {
        this.costCenterList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    cancel() {
      this.open = false;
      this.reset();
    },
    reset() {
      this.form = {
        id: undefined,
        centerCode: undefined,
        centerName: undefined,
        defaultAccountingClass: "EXPENSE",
        isPending: 0,
        status: "ENABLED"
      };
      this.resetForm("form");
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加责任中心";
    },
    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids[0];
      getCostCenter(id).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改责任中心";
      });
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.buttonLoading = true;
          if (this.form.id != null) {
            updateCostCenter(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
              this.buttonLoading = false;
            });
          } else {
            addCostCenter(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
              this.buttonLoading = false;
            });
          }
        }
      });
    },
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$modal.confirm('是否确认删除责任中心编号为"' + ids + '"的数据项？').then(function() {
        return delCostCenter(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleDeptMapping(row) {
      this.currentCenterCode = row.centerCode;
      this.deptOpen = true;
      // 加载部门树
      treeselect().then(response => {
        this.deptTree = response.data;
      });
      // 加载已关联部门
      queryDeptList(row.centerCode).then(response => {
        this.checkedDeptIds = response.data.map(dept => dept.id);
      });
    },
    submitDeptMapping() {
      const checkedKeys = this.$refs.deptTree.getCheckedKeys();
      this.deptLoading = true;
      saveDeptMapping(this.currentCenterCode, checkedKeys).then(response => {
        this.$modal.msgSuccess("保存成功");
        this.deptOpen = false;
        this.deptLoading = false;
      });
    }
  }
};
</script>
