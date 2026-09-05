<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="关键字" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="科目编码/名称" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="映射状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 140px">
          <el-option label="未映射" value="UNMAPPED" />
          <el-option label="已映射" value="MAPPED" />
          <el-option label="已忽略" value="IGNORED" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源系统" prop="sourceSystem">
        <el-select v-model="queryParams.sourceSystem" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="item in sourceSystems" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-upload" size="mini"
                   v-hasPermi="['fee:financeSubject:import']" @click="handleImport">导入科目</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-connection" size="mini" :loading="matchLoading"
                   v-hasPermi="['fee:financeSubject:match']" @click="handleAutoMatch">自动匹配</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple"
                   v-hasPermi="['fee:financeSubject:remove']" @click="handleDelete">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="subjectList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="科目编码" align="center" prop="subjectCode" width="140" />
      <el-table-column label="科目名称" align="center" prop="subjectName" min-width="180" show-overflow-tooltip />
      <el-table-column label="父级编码" align="center" prop="parentCode" width="120" />
      <el-table-column label="层级" align="center" prop="subjectLevel" width="60" />
      <el-table-column label="来源系统" align="center" prop="sourceSystem" width="100">
        <template slot-scope="scope">{{ getSourceLabel(scope.row.sourceSystem) }}</template>
      </el-table-column>
      <el-table-column label="导入批次" align="center" prop="importBatchNo" width="160" show-overflow-tooltip />
      <el-table-column label="映射A(预算科目)" align="center" min-width="200">
        <template slot-scope="scope">
          <span v-if="scope.row.mappedSubjectCode">
            {{ scope.row.mappedSubjectCode }}
            <span v-if="scope.row.mappedSubjectName">（{{ scope.row.mappedSubjectName }}）</span>
          </span>
          <span v-else style="color: #C0C4CC;">-</span>
        </template>
      </el-table-column>
      <el-table-column label="映射方式" align="center" prop="matchType" width="90">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.matchType === 'AUTO'" type="success" size="mini">自动</el-tag>
          <el-tag v-else-if="scope.row.matchType === 'MANUAL'" type="warning" size="mini">人工</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.status === 'MAPPED'" type="success" size="mini">已映射</el-tag>
          <el-tag v-else-if="scope.row.status === 'IGNORED'" type="info" size="mini">已忽略</el-tag>
          <el-tag v-else type="danger" size="mini">未映射</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-link"
                     v-hasPermi="['fee:financeSubject:map']" @click="handleMap(scope.row)">映射</el-button>
          <el-button size="mini" type="text" icon="el-icon-view-off" v-if="scope.row.status !== 'IGNORED'"
                     v-hasPermi="['fee:financeSubject:map']" @click="handleIgnore(scope.row)">忽略</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete"
                     v-hasPermi="['fee:financeSubject:remove']" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 导入弹窗 -->
    <el-dialog :title="upload.title" :visible.sync="upload.open" width="420px" append-to-body>
      <el-form label-width="90px" size="small" style="margin-bottom: 10px;">
        <el-form-item label="来源系统">
          <el-select v-model="upload.sourceSystem" style="width: 100%">
            <el-option v-for="item in sourceSystems" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-upload
        ref="upload"
        :limit="1"
        accept=".xlsx, .xls"
        :headers="upload.headers"
        :action="upload.url"
        :data="upload.data"
        :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress"
        :on-success="handleFileSuccess"
        :on-error="handleFileError"
        :auto-upload="false"
        drag
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip text-center" slot="tip">
          <span>支持金蝶/用友等财务软件导出的科目文件，仅允许xls、xlsx格式。</span>
          <el-link type="primary" :underline="false" style="font-size:12px;vertical-align: baseline;" @click="downloadTemplate">下载模板</el-link>
        </div>
      </el-upload>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitFileForm">确 定</el-button>
        <el-button @click="upload.open = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 人工映射弹窗 -->
    <el-dialog title="人工映射（映射A）" :visible.sync="mapOpen" width="500px" append-to-body>
      <el-form label-width="110px" size="small">
        <el-form-item label="财务科目">
          <span>{{ mapForm.subjectCode }}（{{ mapForm.subjectName }}）</span>
        </el-form-item>
        <el-form-item label="预算科目" required>
          <el-select v-model="mapForm.mappedSubjectCode" filterable placeholder="请选择预算科目" style="width: 100%">
            <el-option
              v-for="item in budgetSubjects"
              :key="item.subjectCode"
              :label="item.subjectCode + ' ' + item.subjectName"
              :value="item.subjectCode"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="mapLoading" @click="submitMap">确 定</el-button>
        <el-button @click="mapOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listFinanceSubject, autoMatchFinanceSubject, mapFinanceSubject, ignoreFinanceSubject, delFinanceSubject, importTemplate } from "@/api/system/fee/fundFinanceSubject";
import { listAllSubjects } from "@/api/system/preparation";
import { getToken } from "@/utils/auth";

export default {
  name: "FinanceSubject",
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      subjectList: [],
      ids: [],
      multiple: true,
      matchLoading: false,
      sourceSystems: [
        { value: 'KINGDEE', label: '金蝶' },
        { value: 'YONYOU', label: '用友' },
        { value: 'CHANJET', label: '畅捷通' },
        { value: 'SUPERDATA', label: '速达' },
        { value: 'GRASP', label: '管家婆' },
        { value: 'LEMONCLOUD', label: '柠檬云' },
        { value: 'GENERAL', label: '通用格式' }
      ],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        keyword: undefined,
        status: undefined,
        sourceSystem: undefined
      },
      // 导入参数
      upload: {
        open: false,
        title: "财务科目导入",
        isUploading: false,
        sourceSystem: 'GENERAL',
        headers: { Authorization: "Bearer " + getToken() },
        url: process.env.VUE_APP_BASE_API + "/fee/financeSubject/importData",
        data: {}
      },
      // 人工映射弹窗
      mapOpen: false,
      mapLoading: false,
      mapForm: { id: null, subjectCode: '', subjectName: '', mappedSubjectCode: '' },
      budgetSubjects: []
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getSourceLabel(value) {
      const item = this.sourceSystems.find(s => s.value === value);
      return item ? item.label : value;
    },
    getList() {
      this.loading = true;
      listFinanceSubject(this.queryParams).then(response => {
        this.subjectList = response.rows;
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
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id);
      this.multiple = !selection.length;
    },
    /** 导入按钮 */
    handleImport() {
      this.upload.open = true;
    },
    /** 下载模板 */
    downloadTemplate() {
      importTemplate().then(response => {
        this.$download.saveAs(response, "财务科目导入模板.xlsx");
      });
    },
    /** 文件上传中 */
    handleFileUploadProgress() {
      this.upload.isUploading = true;
    },
    /** 文件上传成功 */
    handleFileSuccess(response) {
      this.upload.isUploading = false;
      this.upload.open = false;
      this.$refs.upload.clearFiles();
      this.$alert("<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" + response.msg + "</div>", "导入结果", { dangerouslyUseHTMLString: true });
      this.getList();
    },
    handleFileError() {
      this.upload.isUploading = false;
      this.$modal.msgError("导入失败，请检查文件格式");
    },
    /** 提交上传文件 */
    submitFileForm() {
      this.upload.data = { sourceSystem: this.upload.sourceSystem };
      this.$refs.upload.submit();
    },
    /** 关键字自动匹配 */
    handleAutoMatch() {
      this.matchLoading = true;
      autoMatchFinanceSubject().then(response => {
        this.$modal.msgSuccess("自动匹配完成，成功建立映射 " + (response.data || 0) + " 条");
        this.getList();
      }).finally(() => {
        this.matchLoading = false;
      });
    },
    /** 人工映射 */
    handleMap(row) {
      this.mapForm = {
        id: row.id,
        subjectCode: row.subjectCode,
        subjectName: row.subjectName,
        mappedSubjectCode: row.mappedSubjectCode || ''
      };
      if (this.budgetSubjects.length === 0) {
        listAllSubjects().then(response => {
          this.budgetSubjects = (response.data || []).filter(s => s.isLeaf === 1);
        });
      }
      this.mapOpen = true;
    },
    submitMap() {
      if (!this.mapForm.mappedSubjectCode) {
        this.$modal.msgWarning("请选择预算科目");
        return;
      }
      this.mapLoading = true;
      mapFinanceSubject(this.mapForm.id, this.mapForm.mappedSubjectCode).then(() => {
        this.$modal.msgSuccess("映射成功");
        this.mapOpen = false;
        this.getList();
      }).finally(() => {
        this.mapLoading = false;
      });
    },
    /** 忽略科目 */
    handleIgnore(row) {
      this.$modal.confirm('确认忽略科目"' + row.subjectName + '"？忽略后不参与映射').then(() => {
        return ignoreFinanceSubject(row.id);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("已忽略");
      }).catch(() => {});
    },
    /** 删除 */
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$modal.confirm('是否确认删除选中的财务科目？').then(() => {
        return delFinanceSubject(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    }
  }
};
</script>
