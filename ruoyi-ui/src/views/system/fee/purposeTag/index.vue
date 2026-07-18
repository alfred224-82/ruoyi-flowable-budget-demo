<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="标签编码" prop="tagCode">
        <el-input
          v-model="queryParams.tagCode"
          placeholder="请输入标签编码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="标签名称" prop="tagName">
        <el-input
          v-model="queryParams.tagName"
          placeholder="请输入标签名称"
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
          v-hasPermi="['fee:purposeTag:add']"
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
          v-hasPermi="['fee:purposeTag:edit']"
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
          v-hasPermi="['fee:purposeTag:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="purposeTagList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="标签编码" align="center" prop="tagCode" />
      <el-table-column label="标签名称" align="center" prop="tagName" />
      <el-table-column label="图标" align="center" prop="tagIcon" width="80">
        <template slot-scope="scope">
          <span style="font-size: 20px">{{ scope.row.tagIcon }}</span>
        </template>
      </el-table-column>
      <el-table-column label="默认会计大类" align="center" prop="defaultAccountingClass">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.defaultAccountingClass === 'EXPENSE'" type="warning">费用类</el-tag>
          <el-tag v-else-if="scope.row.defaultAccountingClass === 'COST'" type="success">成本类</el-tag>
          <el-tag v-else>{{ scope.row.defaultAccountingClass }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="要求项目编号" align="center" prop="requireProject" width="120">
        <template slot-scope="scope">
          <el-tag :type="scope.row.requireProject === 1 ? 'danger' : 'info'">
            {{ scope.row.requireProject === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sortOrder" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 'ENABLED' ? 'success' : 'danger'">
            {{ scope.row.status === 'ENABLED' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['fee:purposeTag:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['fee:purposeTag:remove']"
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

    <!-- 添加或修改用途标签对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="标签编码" prop="tagCode">
          <el-input v-model="form.tagCode" placeholder="请输入标签编码" maxlength="50" />
        </el-form-item>
        <el-form-item label="标签名称" prop="tagName">
          <el-input v-model="form.tagName" placeholder="请输入标签名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="图标" prop="tagIcon">
          <el-input v-model="form.tagIcon" placeholder="请输入图标（如：🏢）" maxlength="10" />
        </el-form-item>
        <el-form-item label="默认会计大类" prop="defaultAccountingClass">
          <el-select v-model="form.defaultAccountingClass" placeholder="请选择会计大类" style="width: 100%">
            <el-option label="费用类(EXPENSE)" value="EXPENSE" />
            <el-option label="成本类(COST)" value="COST" />
            <el-option label="资产类(ASSET)" value="ASSET" />
          </el-select>
        </el-form-item>
        <el-form-item label="要求项目编号" prop="requireProject">
          <el-radio-group v-model="form.requireProject">
            <el-radio :label="1">是</el-radio>
            <el-radio :label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
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
  </div>
</template>

<script>
import { listPurposeTag, getPurposeTag, delPurposeTag, addPurposeTag, updatePurposeTag } from "@/api/system/fee/purposeTag";

export default {
  name: "PurposeTag",
  data() {
    return {
      buttonLoading: false,
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      purposeTagList: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        tagCode: undefined,
        tagName: undefined,
        status: undefined,
      },
      form: {},
      rules: {
        tagCode: [
          { required: true, message: "标签编码不能为空", trigger: "blur" }
        ],
        tagName: [
          { required: true, message: "标签名称不能为空", trigger: "blur" }
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
      listPurposeTag(this.queryParams).then(response => {
        this.purposeTagList = response.rows;
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
        tagCode: undefined,
        tagName: undefined,
        tagIcon: undefined,
        defaultAccountingClass: "EXPENSE",
        requireProject: 0,
        sortOrder: 0,
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
      this.title = "添加用途标签";
    },
    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids[0];
      getPurposeTag(id).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改用途标签";
      });
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.buttonLoading = true;
          if (this.form.id != null) {
            updatePurposeTag(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
              this.buttonLoading = false;
            });
          } else {
            addPurposeTag(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除用途标签编号为"' + ids + '"的数据项？').then(function() {
        return delPurposeTag(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    }
  }
};
</script>
