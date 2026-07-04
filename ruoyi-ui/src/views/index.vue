<template>
  <div class="app-container home">
    <!-- 项目标题 -->
    <el-row>
      <el-col :span="24">
        <div class="project-header">
          <h1>企业全面预算管理系统</h1>
          <p class="subtitle">基于 RuoYi-Flowable-Plus 框架的企业级预算管理解决方案</p>
          <el-tag type="info">版本 v1.4</el-tag>
          <el-tag type="success">2026-07-03</el-tag>
        </div>
      </el-col>
    </el-row>

    <!-- 核心价值 -->
    <el-row :gutter="20" class="section">
      <el-col :span="24">
        <h2>核心价值</h2>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" v-for="(item, index) in coreValues" :key="index">
        <el-card shadow="hover" class="value-card">
          <div slot="header" class="card-header">
            <i :class="item.icon" :style="{ color: item.color }"></i>
            <span>{{ item.title }}</span>
          </div>
          <p>{{ item.desc }}</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- 业务周期 -->
    <el-row :gutter="20" class="section">
      <el-col :span="24">
        <h2>业务周期与时间节点</h2>
      </el-col>
      <el-col :span="24">
        <el-table :data="businessCycle" stripe border style="width: 100%">
          <el-table-column prop="stage" label="阶段" width="120"></el-table-column>
          <el-table-column prop="time" label="时间范围" width="180"></el-table-column>
          <el-table-column prop="action" label="核心动作"></el-table-column>
          <el-table-column prop="owner" label="责任主体" width="140"></el-table-column>
        </el-table>
      </el-col>
    </el-row>

    <!-- 状态流转 -->
    <el-row :gutter="20" class="section">
      <el-col :span="24">
        <h2>预算编制状态流转</h2>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" v-for="(item, index) in statusList" :key="index">
        <el-card shadow="hover" class="status-card">
          <div slot="header">
            <el-tag :type="item.tagType" size="small">{{ item.code }}</el-tag>
            <span style="margin-left: 8px">{{ item.name }}</span>
          </div>
          <p class="status-desc">{{ item.desc }}</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- 审批流程 -->
    <el-row :gutter="20" class="section">
      <el-col :span="24">
        <h2>三级审批流程</h2>
      </el-col>
      <el-col :span="24">
        <el-steps :active="3" finish-status="success" align-center>
          <el-step title="部门经理审批" description="第1级"></el-step>
          <el-step title="分公司本部审批" description="第2级"></el-step>
          <el-step title="总公司本部审批" description="第3级"></el-step>
        </el-steps>
      </el-col>
    </el-row>

    <!-- 角色权限 -->
    <el-row :gutter="20" class="section">
      <el-col :span="24">
        <h2>角色权限矩阵</h2>
      </el-col>
      <el-col :span="24">
        <el-table :data="roleMatrix" stripe border style="width: 100%">
          <el-table-column prop="role" label="角色" width="140"></el-table-column>
          <el-table-column prop="budget" label="编制权限" width="120"></el-table-column>
          <el-table-column prop="approval" label="审核权限" width="120"></el-table-column>
          <el-table-column prop="scope" label="数据可见范围"></el-table-column>
        </el-table>
      </el-col>
    </el-row>

    <!-- 功能模块 -->
    <el-row :gutter="20" class="section">
      <el-col :span="24">
        <h2>功能模块</h2>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" v-for="(item, index) in functionModules" :key="index">
        <el-card shadow="hover" class="module-card">
          <div slot="header" class="card-header">
            <i :class="item.icon" :style="{ color: item.color }"></i>
            <span>{{ item.title }}</span>
            <el-tag v-if="item.status" :type="item.statusType" size="mini" style="margin-left: auto">{{ item.status }}</el-tag>
          </div>
          <ul>
            <li v-for="(feat, fi) in item.features" :key="fi">{{ feat }}</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>

    <!-- 技术栈 -->
    <el-row :gutter="20" class="section">
      <el-col :span="24">
        <h2>技术选型</h2>
      </el-col>
      <el-col :sm="12">
        <h4>后端技术</h4>
        <el-tag v-for="tech in backendTech" :key="tech" class="tech-tag">{{ tech }}</el-tag>
      </el-col>
      <el-col :sm="12">
        <h4>前端技术</h4>
        <el-tag v-for="tech in frontendTech" :key="tech" class="tech-tag" type="success">{{ tech }}</el-tag>
      </el-col>
    </el-row>

    <!-- 项目信息 -->
    <el-row class="section footer-info">
      <el-col :span="24">
        <el-divider></el-divider>
        <p><b>项目作者：</b>alfred224-82</p>
        <p><b>框架基础：</b>RuoYi-Flowable-Plus v0.8.X</p>
        <p><b>仓库地址：</b>
          <el-button type="text" icon="el-icon-link" @click="goTarget('https://github.com/alfred224-82/origin')">
            https://github.com/alfred224-82/origin
          </el-button>
        </p>
      </el-col>
    </el-row>
  </div>
</template>

<script>
export default {
  name: "Index",
  data() {
    return {
      coreValues: [
        { title: "流程自动化", desc: "通过 Flowable 工作流引擎实现预算审批流程自动化", icon: "el-icon-s-operation", color: "#409EFF" },
        { title: "多级管控", desc: "总公司→分公司→部门的多层级预算管控体系", icon: "el-icon-s-custom", color: "#67C23A" },
        { title: "智能风控", desc: "内置风控规则引擎，自动识别异常数据", icon: "el-icon-warning-outline", color: "#E6A23C" },
        { title: "数据可视化", desc: "报表与图表展示能力，预算数据一目了然", icon: "el-icon-data-line", color: "#F56C6C" },
        { title: "安全合规", desc: "敏感数据脱敏、操作日志追溯、水印保护", icon: "el-icon-lock", color: "#909399" },
        { title: "AI 助手", desc: "基于 LangChain4j 的 AI 对话式报表生成与分析", icon: "el-icon-chat-dot-round", color: "#409EFF" }
      ],
      businessCycle: [
        { stage: "编制期", time: "本月26日 - 次月3日", action: "部门编制→提交审核", owner: "部门编制人员" },
        { stage: "三级审批期", time: "编制提交后即时", action: "部门经理→分公司本部→总公司本部逐级审批", owner: "各级审批人" },
        { stage: "驳回修改期", time: "驳回后即时", action: "编制人员接收驳回并修改重提", owner: "部门编制人员" },
        { stage: "执行期", time: "总公司本部通过后", action: "预算锁定，正式执行", owner: "系统自动" }
      ],
      statusList: [
        { code: "Draft", name: "草稿", tagType: "info", desc: "可编辑、可删除，编辑后仍为 Draft" },
        { code: "Completed", name: "完成编制", tagType: "", desc: "编制已完成，可提交审核；不可编辑、不可删除" },
        { code: "Pending_Review", name: "待审核", tagType: "warning", desc: "三级审批流程中，具体阶段由 approval_stage 决定" },
        { code: "Pending_Revision", name: "待修订", tagType: "warning", desc: "分公司打回至部门，需修改后重新提交" },
        { code: "Approved", name: "审核通过", tagType: "success", desc: "三级审批全部通过，预算锁定，不可修改" },
        { code: "Rejected", name: "已驳回", tagType: "danger", desc: "任一级别驳回，回退到编制人员；可编辑后重置为 Draft" }
      ],
      roleMatrix: [
        { role: "总公司本部", budget: "全辖编制", approval: "全辖审批", scope: "全辖数据" },
        { role: "分公司本部", budget: "分公司汇总编制", approval: "分公司审批", scope: "分公司及下属部门" },
        { role: "部门编制人员", budget: "本部门编制", approval: "无", scope: "本部门（实际金额脱敏）" },
        { role: "部门经理", budget: "无", approval: "本部门审批", scope: "本部门（实际金额脱敏）" }
      ],
      functionModules: [
        {
          title: "预算编制", icon: "el-icon-edit-outline", color: "#409EFF",
          features: ["向导式3步编制流程", "6种科目类型分Tab录入", "上月实绩自动填充", "部门+月份重复校验", "预算总额实时汇总"],
          status: "已完成", statusType: "success"
        },
        {
          title: "预算审核", icon: "el-icon-check", color: "#67C23A",
          features: ["三级审批流程", "批量审批/驳回", "角色-阶段匹配校验", "驳回原因追溯", "Flowable 流程集成"],
          status: "已完成", statusType: "success"
        },
        {
          title: "智能风控", icon: "el-icon-warning", color: "#E6A23C",
          features: ["11条初始化校验规则", "ERROR/WARNING/INFO 三级", "必填/金额/比例/合计校验", "规则动态配置"],
          status: "已完成", statusType: "success"
        },
        {
          title: "消息通知", icon: "el-icon-bell", color: "#F56C6C",
          features: ["站内消息实时通知", "未读角标展示", "审批/驳回/系统消息", "邮件通知互补"],
          status: "已完成", statusType: "success"
        },
        {
          title: "AI 对话", icon: "el-icon-chat-line-round", color: "#409EFF",
          features: ["多会话记忆", "预算上下文注入", "SSE 流式响应", "预算数据分析"],
          status: "开发中", statusType: "warning"
        },
        {
          title: "报表系统", icon: "el-icon-pie-chart", color: "#909399",
          features: ["预算数据汇总报表", "差异分析", "科目类型统计"],
          status: "待开发", statusType: "info"
        }
      ],
      backendTech: ["Spring Boot", "Sa-Token", "MyBatis-Plus", "Flowable", "LangChain4j", "Redisson", "Undertow", "Jackson", "p6spy", "Hutool", "Lombok", "Docker"],
      frontendTech: ["Vue 2", "Vuex", "Element UI", "Axios", "SCSS", "Vue Router", "Monaco Editor"]
    };
  },
  methods: {
    goTarget(href) {
      window.open(href, "_blank");
    }
  }
};
</script>

<style scoped lang="scss">
.home {
  font-family: "open sans", "Helvetica Neue", Helvetica, Arial, sans-serif;
  color: #676a6c;
  overflow-x: hidden;
  padding: 20px;

  .project-header {
    text-align: center;
    padding: 30px 0;

    h1 {
      font-size: 32px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 10px;
    }

    .subtitle {
      font-size: 16px;
      color: #909399;
      margin-bottom: 15px;
    }

    .el-tag {
      margin: 0 5px;
    }
  }

  .section {
    margin-top: 20px;

    h2 {
      font-size: 22px;
      font-weight: 500;
      color: #303133;
      margin-bottom: 15px;
      padding-left: 10px;
      border-left: 4px solid #409EFF;
    }

    h4 {
      font-size: 16px;
      font-weight: 500;
      color: #606266;
      margin-bottom: 10px;
    }
  }

  .value-card, .status-card, .module-card {
    margin-bottom: 15px;

    .card-header {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 500;
    }
  }

  .status-desc {
    font-size: 13px;
    color: #909399;
    margin: 0;
  }

  .module-card {
    ul {
      padding-left: 0;
      list-style: none;
      margin: 0;

      li {
        padding: 4px 0;
        font-size: 13px;
        color: #606266;

        &::before {
          content: "✓ ";
          color: #67C23A;
          font-weight: bold;
        }
      }
    }
  }

  .tech-tag {
    margin: 4px 6px;
  }

  .footer-info {
    p {
      margin: 6px 0;
      font-size: 14px;

      b {
        color: #303133;
      }
    }
  }
}
</style>
