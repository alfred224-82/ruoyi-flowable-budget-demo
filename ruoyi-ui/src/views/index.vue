<template>
  <div class="app-container home">
    <!-- 欢迎头部 -->
    <div class="welcome-banner">
      <div class="welcome-content">
        <h1><i class="el-icon-wallet"></i> 预算与报销管理系统</h1>
        <p class="subtitle">全流程预算编制 · 费用报销 · 预算调整 · 盈利分析 — 操作指南</p>
        <div class="dimension-tag">
          <el-tag effect="dark" type="info" size="medium">
            <i class="el-icon-key"></i> 统一数据维度：科目 + 部门 + 年月
          </el-tag>
        </div>
      </div>
    </div>

    <!-- 角色与工号速查 -->
    <div class="section">
      <h2><i class="el-icon-user"></i> 角色与工号对照</h2>
      <el-row :gutter="16">
        <el-col :xs="12" :sm="6" v-for="(role, idx) in roles" :key="idx">
          <div class="role-card" :style="{ borderTopColor: role.color }">
            <div class="role-icon" :style="{ background: role.color }">
              <i :class="role.icon"></i>
            </div>
            <div class="role-name">{{ role.name }}</div>
            <div class="role-code">工号: <strong>{{ role.code }}</strong></div>
            <div class="role-desc">{{ role.desc }}</div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 三大核心单据 -->
    <div class="section">
      <h2><i class="el-icon-document-checked"></i> 三大核心单据操作指南</h2>
      <el-collapse v-model="activeCollapse" accordion>
        <!-- 预算编制单 -->
        <el-collapse-item name="budget">
          <template slot="title">
            <div class="collapse-title">
              <span class="doc-badge" style="background:#409EFF"><i class="el-icon-edit"></i> 预算编制单</span>
              <span class="collapse-subtitle">发起当月预算，三级审批通过后生效</span>
            </div>
          </template>
          <div class="doc-detail">
            <el-row :gutter="20">
              <el-col :md="14">
                <h4><i class="el-icon-s-order"></i> 审批流程（逐级，不可跳过）</h4>
                <el-steps :active="4" finish-status="success" simple style="margin: 15px 0">
                  <el-step title="H00889" description="编制人员提交"></el-step>
                  <el-step title="H00998" description="部门经理审核"></el-step>
                  <el-step title="H00223" description="分公司本部审核"></el-step>
                  <el-step title="H00001" description="总公司本部审核"></el-step>
                </el-steps>
                <el-alert
                  title="全部通过后：生成当期「科目+部门+年月」初始预算余额"
                  type="success" :closable="false" show-icon
                  style="margin-bottom: 10px"
                ></el-alert>
                <el-alert
                  title="任意一级驳回：当期预算余额不生成，编制人员可修改后重新提交审核"
                  type="warning" :closable="false" show-icon
                ></el-alert>
              </el-col>
              <el-col :md="10">
                <div class="rule-box">
                  <h4><i class="el-icon-info"></i> 操作要点</h4>
                  <ul>
                    <li><strong>发起人：</strong>H00889（部门预算编制人员）</li>
                    <li><strong>前置条件：</strong>通过向导完成3步编制</li>
                    <li><strong>状态流转：</strong>Draft → Completed → Pending_Review → Approved</li>
                    <li><strong>驳回处理：</strong>状态变为 Rejected，可编辑后重新提交，从被驳回的级别继续审批</li>
                    <li><strong>余额效果：</strong>初始化当期预算余额，非加减操作</li>
                    <li><strong>资产类科目：</strong>编制时需填写使用时间（折旧年限），系统按月自动计算折旧</li>
                  </ul>
                </div>
              </el-col>
            </el-row>
          </div>
        </el-collapse-item>

        <!-- 费用报销单 -->
        <el-collapse-item name="expense">
          <template slot="title">
            <div class="collapse-title">
              <span class="doc-badge" style="background:#67C23A"><i class="el-icon-money"></i> 费用报销单</span>
              <span class="collapse-subtitle">报销费用，仅部门经理一级审核</span>
            </div>
          </template>
          <div class="doc-detail">
            <el-row :gutter="20">
              <el-col :md="14">
                <h4><i class="el-icon-s-order"></i> 审批流程（仅两级）</h4>
                <el-steps :active="2" finish-status="success" simple style="margin: 15px 0">
                  <el-step title="H00889" description="编制人员填写提交"></el-step>
                  <el-step title="H00998" description="部门经理审核"></el-step>
                </el-steps>
                <el-alert
                  title="部门经理审核通过：当期预算余额 = 原余额 - 报销金额（即时扣减）"
                  type="success" :closable="false" show-icon
                  style="margin-bottom: 10px"
                ></el-alert>
                <el-alert
                  title="驳回/不通过：预算余额不变，无任何变动"
                  type="warning" :closable="false" show-icon
                  style="margin-bottom: 10px"
                ></el-alert>
                <el-alert
                  title="草稿、待审核状态不改动预算余额"
                  type="info" :closable="false" show-icon
                ></el-alert>
              </el-col>
              <el-col :md="10">
                <div class="rule-box">
                  <h4><i class="el-icon-info"></i> 操作要点</h4>
                  <ul>
                    <li><strong>发起人：</strong>H00889（部门预算编制人员）</li>
                    <li><strong>审批层级：</strong>仅部门经理一级，无分公司/总公司节点</li>
                    <li><strong>扣减时机：</strong>仅在部门经理审批通过时扣减</li>
                    <li><strong>维度隔离：</strong>按科目+部门+年月定位余额</li>
                    <li><strong>余额不足：</strong>系统会提示超预算预警</li>
                  </ul>
                </div>
              </el-col>
            </el-row>
          </div>
        </el-collapse-item>

        <!-- 预算调整申请单 -->
        <el-collapse-item name="adjustment">
          <template slot="title">
            <div class="collapse-title">
              <span class="doc-badge" style="background:#E6A23C"><i class="el-icon-s-tools"></i> 预算调整申请单</span>
              <span class="collapse-subtitle">部门领导用资金池审批修正余额，上级仅分摊总额</span>
            </div>
          </template>
          <div class="doc-detail">
            <el-row :gutter="20">
              <el-col :md="14">
                <h4><i class="el-icon-s-order"></i> 审批流程（逐级，不可跳过）</h4>
                <el-steps :active="4" finish-status="success" simple style="margin: 15px 0">
                  <el-step title="H00889" description="编制人员提交调整"></el-step>
                  <el-step title="H00998" description="部门经理审核"></el-step>
                  <el-step title="H00223" description="分公司本部审核"></el-step>
                  <el-step title="H00001" description="总公司本部审核"></el-step>
                </el-steps>

                <!-- 分层级说明 -->
                <div class="level-section">
                  <div class="level-card level-dept">
                    <div class="level-badge"><i class="el-icon-user"></i> 部门经理（H00998）— 直接审批修正</div>
                    <div class="level-body">
                      <p>从 <strong>本部门资金池</strong> 扣减差额（申请金额 - 科目当前余额）</p>
                      <p>同时将目标科目余额 <strong>修正为申请金额</strong>（使科目余额 = 申请值）</p>
                      <p class="level-note"><i class="el-icon-info"></i> 部门领导是唯一直接操作科目余额和资金池的审批层级</p>
                    </div>
                  </div>
                  <div class="level-card level-branch">
                    <div class="level-badge"><i class="el-icon-office-building"></i> 分公司本部（H00223）— 分摊到部门资金池</div>
                    <div class="level-body">
                      <p>从 <strong>分公司资金池</strong> 扣减审批金额</p>
                      <p>将金额 <strong>增加到申请部门的部门资金池</strong> 总额中</p>
                      <p class="level-note"><i class="el-icon-info"></i> 不直接操作科目预算，由部门经理后续自行分配到科目</p>
                    </div>
                  </div>
                  <div class="level-card level-hq">
                    <div class="level-badge"><i class="el-icon-s-home"></i> 总公司本部（H00001）— 分摊到分公司资金池</div>
                    <div class="level-body">
                      <p>从 <strong>总公司资金池</strong> 扣减审批金额</p>
                      <p>将金额 <strong>增加到申请部门所在分公司的资金池</strong> 总额中</p>
                      <p class="level-note"><i class="el-icon-info"></i> 不直接操作部门或科目，由分公司后续向下分摊</p>
                    </div>
                  </div>
                </div>

                <el-alert
                  title="任意一级驳回：余额和资金池均保持原值，编制人员可修改后重新提交"
                  type="warning" :closable="false" show-icon
                  style="margin-top: 12px"
                ></el-alert>
              </el-col>
              <el-col :md="10">
                <div class="rule-box">
                  <h4><i class="el-icon-info"></i> 操作要点</h4>
                  <ul>
                    <li><strong>发起人：</strong>H00889（部门预算编制人员）</li>
                    <li><strong>部门审批：</strong>扣减本部门资金池差额，修正科目余额为申请值</li>
                    <li><strong>分公司审批：</strong>仅从分公司资金池分摊总额到部门资金池</li>
                    <li><strong>总公司审批：</strong>仅从总公司资金池分摊总额到分公司资金池</li>
                    <li><strong>驳回处理：</strong>余额和资金池不变，编制人员可修改后重新提交，从被驳回级别继续</li>
                    <li><strong>科目定位：</strong>通过物资小类+用途标签匹配科目</li>
                  </ul>
                </div>
                <div class="rule-box" style="margin-top:12px">
                  <h4><i class="el-icon-warning-outline"></i> 资金池流转方向</h4>
                  <div class="pool-flow">
                    <div class="pool-node pool-hq">总公司资金池</div>
                    <div class="pool-arrow"><i class="el-icon-bottom"></i> 分摊总额</div>
                    <div class="pool-node pool-branch">分公司资金池</div>
                    <div class="pool-arrow"><i class="el-icon-bottom"></i> 分摊总额</div>
                    <div class="pool-node pool-dept">部门资金池</div>
                    <div class="pool-arrow"><i class="el-icon-bottom"></i> 扣减差额</div>
                    <div class="pool-node pool-subject">科目预算余额</div>
                  </div>
                </div>
              </el-col>
            </el-row>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>

    <!-- 余额变动速查表 -->
    <div class="section">
      <h2><i class="el-icon-s-data"></i> 余额变动规则速查</h2>
      <el-table :data="balanceRules" stripe border style="width: 100%" :header-cell-style="{background:'#f5f7fa',color:'#303133'}">
        <el-table-column prop="doc" label="单据类型" width="150" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.tagType" effect="plain">{{ scope.row.doc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pass" label="全部审批通过" min-width="220">
          <template slot-scope="scope">
            <span class="rule-pass">{{ scope.row.pass }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reject" label="任意驳回" min-width="180">
          <template slot-scope="scope">
            <span class="rule-reject">{{ scope.row.reject }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="approver" label="审批层级" width="240" align="center"></el-table-column>
        <el-table-column prop="trigger" label="余额生效节点" width="180" align="center">
          <template slot-scope="scope">
            <el-tag size="small" :type="scope.row.triggerType">{{ scope.row.trigger }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 盈利分析表 -->
    <div class="section">
      <h2><i class="el-icon-s-marketing"></i> 盈利分析表</h2>
      <el-alert
        title="盈利分析表仅对部门经理、分公司本部、总公司本部三个角色开放，编制人员无权查看"
        type="info" :closable="false" show-icon
        style="margin-bottom: 16px"
      ></el-alert>

      <!-- 财务6大科目分析说明 -->
      <div class="subject-intro">
        <h4><i class="el-icon-s-grid"></i> 按财务6大科目分析</h4>
        <el-row :gutter="12">
          <el-col :xs="12" :sm="8" :md="4" v-for="(cat, idx) in subjectCategories" :key="idx">
            <div class="cat-card" :class="cat.cls">
              <div class="cat-icon"><i :class="cat.icon"></i></div>
              <div class="cat-name">{{ cat.name }}</div>
              <div class="cat-desc">{{ cat.desc }}</div>
            </div>
          </el-col>
        </el-row>
        <el-alert
          type="warning" :closable="false" show-icon
          style="margin-top: 12px"
        >
          <template slot="title">
            <strong>资产类</strong>属于固定资产，每月产生折旧费用。在预算编制时需填写<strong>使用时间（折旧年限）</strong>，系统按月自动计算折旧额并纳入盈利分析。
          </template>
        </el-alert>
      </div>

      <el-row :gutter="16">
        <el-col :md="8">
          <div class="perm-card perm-dept">
            <div class="perm-header">
              <i class="el-icon-user"></i> 部门经理（H00998）
            </div>
            <div class="perm-body">
              <div class="perm-scope">
                <el-tag size="small" effect="dark" type="success">仅看本部门</el-tag>
              </div>
              <ul>
                <li>只能查看 <strong>本部门</strong> 的盈利分析数据</li>
                <li>不可切换到其他部门或上级组织</li>
                <li>数据维度：科目 + 年月</li>
              </ul>
            </div>
          </div>
        </el-col>
        <el-col :md="8">
          <div class="perm-card perm-branch">
            <div class="perm-header">
              <i class="el-icon-office-building"></i> 分公司本部（H00223）
            </div>
            <div class="perm-body">
              <div class="perm-scope">
                <el-tag size="small" effect="dark" type="warning">分公司汇总 + 下钻部门</el-tag>
              </div>
              <ul>
                <li>默认查看 <strong>分公司整体</strong> 汇总数据</li>
                <li>可 <strong>下钻</strong> 查看下属各部门的明细数据</li>
                <li>数据维度：科目 + 部门 + 年月</li>
              </ul>
            </div>
          </div>
        </el-col>
        <el-col :md="8">
          <div class="perm-card perm-hq">
            <div class="perm-header">
              <i class="el-icon-s-home"></i> 总公司本部（H00001）
            </div>
            <div class="perm-body">
              <div class="perm-scope">
                <el-tag size="small" effect="dark" type="danger">全辖数据</el-tag>
              </div>
              <ul>
                <li>可查看 <strong>全部</strong> 盈利分析数据</li>
                <li>可按 <strong>分公司</strong> 维度筛选查看</li>
                <li>可 <strong>下钻</strong> 到具体部门查看明细</li>
                <li>数据维度：科目 + 分公司 + 部门 + 年月</li>
              </ul>
            </div>
          </div>
        </el-col>
      </el-row>
      <div class="perm-summary">
        <i class="el-icon-warning-outline"></i>
        权限规则：<strong>按组织层级隔离数据</strong>，部门只能看本部门，分公司看本公司及下属部门，总公司看全部
      </div>
    </div>

    <!-- 系统通用规范 -->
    <div class="section">
      <h2><i class="el-icon-setting"></i> 系统通用操作规范</h2>
      <el-row :gutter="20">
        <el-col :sm="8">
          <div class="norm-card">
            <div class="norm-header" style="color:#409EFF">
              <i class="el-icon-connection"></i> 维度绑定
            </div>
            <p>新增/提交任意单据时，必须选择 <strong>科目、部门、年月</strong> 三要素。</p>
            <p>系统根据三要素定位对应预算余额，<strong>跨组合不互通数据</strong>。</p>
          </div>
        </el-col>
        <el-col :sm="8">
          <div class="norm-card">
            <div class="norm-header" style="color:#67C23A">
              <i class="el-icon-s-check"></i> 审批流转
            </div>
            <p><strong>预算编制/调整：</strong>部门经理→分公司→总公司三级审核，中途驳回终止。</p>
            <p><strong>报销单：</strong>仅部门经理一级审核，无上级审核节点。</p>
          </div>
        </el-col>
        <el-col :sm="8">
          <div class="norm-card">
            <div class="norm-header" style="color:#E6A23C">
              <i class="el-icon-lock"></i> 权限限制
            </div>
            <p>仅工号 <strong>H00889</strong> 可发起预算编制、报销填报、预算调整申请。</p>
            <p>其他工号仅拥有对应节点的 <strong>审核权限</strong>，无法新增单据。</p>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 极简流程速记 -->
    <div class="section">
      <h2><i class="el-icon-magic-stick"></i> 极简流程速记</h2>
      <el-row :gutter="16">
        <el-col :md="8">
          <div class="summary-card summary-budget">
            <div class="summary-icon"><i class="el-icon-edit"></i></div>
            <h3>预算编制</h3>
            <div class="flow-line">
              <span class="flow-node">H00889 发起</span>
              <i class="el-icon-right"></i>
              <span class="flow-node">H00998</span>
              <i class="el-icon-right"></i>
              <span class="flow-node">H00223</span>
              <i class="el-icon-right"></i>
              <span class="flow-node">H00001</span>
            </div>
            <div class="summary-result">
              <el-tag type="success" effect="dark" size="small">通过 → 生成初始预算</el-tag>
            </div>
          </div>
        </el-col>
        <el-col :md="8">
          <div class="summary-card summary-expense">
            <div class="summary-icon"><i class="el-icon-money"></i></div>
            <h3>报销单</h3>
            <div class="flow-line">
              <span class="flow-node">H00889 填报</span>
              <i class="el-icon-right"></i>
              <span class="flow-node">H00998 审核</span>
            </div>
            <div class="summary-result">
              <el-tag type="success" effect="dark" size="small">通过 → 扣减余额</el-tag>
              <el-tag type="danger" effect="dark" size="small" style="margin-left:6px">驳回 → 不变</el-tag>
            </div>
          </div>
        </el-col>
        <el-col :md="8">
          <div class="summary-card summary-adjust">
            <div class="summary-icon"><i class="el-icon-s-tools"></i></div>
            <h3>预算调整</h3>
            <div class="flow-line">
              <span class="flow-node">H00889 发起</span>
              <i class="el-icon-right"></i>
              <span class="flow-node">H00998</span>
              <i class="el-icon-right"></i>
              <span class="flow-node">H00223</span>
              <i class="el-icon-right"></i>
              <span class="flow-node">H00001</span>
            </div>
            <div class="summary-result">
              <el-tag type="success" effect="dark" size="small">部门级 → 修正余额</el-tag>
              <el-tag type="warning" effect="dark" size="small" style="margin-left:6px">上级 → 分摊资金池</el-tag>
            </div>
          </div>
        </el-col>
      </el-row>
      <div class="dimension-footer">
        <i class="el-icon-warning-outline"></i>
        全部单据统一隔离维度：<strong>科目 + 部门 + 年月</strong>，不同组合余额互不干扰
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "Index",
  data() {
    return {
      activeCollapse: 'budget',
      roles: [
        { name: '部门预算编制人员', code: 'H00889', icon: 'el-icon-edit', color: '#409EFF', desc: '发起编制、报销、调整申请' },
        { name: '部门经理', code: 'H00998', icon: 'el-icon-user', color: '#67C23A', desc: '审核本部门所有单据' },
        { name: '分公司本部审核岗', code: 'H00223', icon: 'el-icon-office-building', color: '#E6A23C', desc: '审核编制与调整（第2级）' },
        { name: '总公司本部审核岗', code: 'H00001', icon: 'el-icon-s-home', color: '#F56C6C', desc: '最终审批（第3级）' }
      ],
      balanceRules: [
        {
          doc: '预算编制', tagType: '',
          pass: '生成当期初始预算余额（非加减，初始化赋值）',
          reject: '余额不生成，可修改后重新提交审核',
          approver: '部门经理 → 分公司 → 总公司',
          trigger: '三级全部通过后生效', triggerType: 'success'
        },
        {
          doc: '费用报销', tagType: 'success',
          pass: '当期余额 = 原余额 - 报销金额（即时扣减）',
          reject: '余额保持原值不变',
          approver: '仅部门经理',
          trigger: '部门经理通过即时扣减', triggerType: ''
        },
        {
          doc: '预算调整', tagType: 'warning',
          pass: '部门级：扣资金池差额+修正科目余额；上级：仅分摊总额到下级资金池',
          reject: '余额和资金池不变，可修改后重新提交',
          approver: '部门经理 → 分公司 → 总公司',
          trigger: '各级通过后分别生效', triggerType: 'success'
        }
      ],
      subjectCategories: [
        { name: '资产类', icon: 'el-icon-office-building', cls: 'cat-asset', desc: '固定资产，按月折旧' },
        { name: '负债类', icon: 'el-icon-document-delete', cls: 'cat-liability', desc: '应付账款/借款' },
        { name: '所有者权益', icon: 'el-icon-s-custom', cls: 'cat-equity', desc: '实收资本/留存收益' },
        { name: '收入类', icon: 'el-icon-top-right', cls: 'cat-income', desc: '营业收入/其他收入' },
        { name: '费用类', icon: 'el-icon-money', cls: 'cat-expense', desc: '营业成本/管理费用' },
        { name: '利润类', icon: 'el-icon-s-marketing', cls: 'cat-profit', desc: '净利润/分配利润' }
      ]
    };
  }
};
</script>

<style scoped lang="scss">
.home {
  font-family: "open sans", "Helvetica Neue", Helvetica, Arial, sans-serif;
  color: #676a6c;
  padding: 0 20px 30px;
}

/* 欢迎头部 */
.welcome-banner {
  margin: -20px -20px 20px -20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 30px;
  color: #fff;

  .welcome-content {
    text-align: center;
    h1 {
      font-size: 28px;
      font-weight: 600;
      margin: 0 0 8px 0;
      color: #fff;
      i { margin-right: 8px; }
    }
    .subtitle {
      font-size: 15px;
      opacity: 0.9;
      margin: 0 0 16px 0;
    }
    .dimension-tag {
      .el-tag {
        font-size: 14px;
        letter-spacing: 1px;
        i { margin-right: 4px; }
      }
    }
  }
}

/* 章节 */
.section {
  margin-bottom: 28px;
  h2 {
    font-size: 20px;
    font-weight: 600;
    color: #303133;
    margin: 0 0 16px 0;
    padding-bottom: 10px;
    border-bottom: 2px solid #ebeef5;
    i { margin-right: 6px; color: #409EFF; }
  }
}

/* 角色卡片 */
.role-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px 16px;
  text-align: center;
  border-top: 3px solid #409EFF;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  margin-bottom: 16px;
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 4px 16px rgba(0,0,0,0.1);
  }

  .role-icon {
    width: 48px; height: 48px;
    border-radius: 50%;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 10px;
    i { font-size: 22px; color: #fff; }
  }
  .role-name { font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 4px; }
  .role-code { font-size: 13px; color: #909399; margin-bottom: 6px; strong { color: #303133; } }
  .role-desc { font-size: 12px; color: #909399; }
}

/* 折叠面板 */
.collapse-title {
  display: flex;
  align-items: center;
  gap: 12px;
  .doc-badge {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    color: #fff;
    padding: 4px 12px;
    border-radius: 4px;
    font-size: 14px;
    font-weight: 500;
  }
  .collapse-subtitle {
    font-size: 13px;
    color: #909399;
  }
}

.doc-detail {
  padding: 10px 0;
  h4 {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
    margin: 0 0 6px 0;
    i { margin-right: 4px; color: #409EFF; }
  }
}

.rule-box {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 16px;
  h4 {
    margin-bottom: 8px;
    i { color: #E6A23C; }
  }
  ul {
    margin: 0;
    padding-left: 0;
    list-style: none;
    li {
      padding: 4px 0;
      font-size: 13px;
      color: #606266;
      &::before {
        content: "●";
        color: #409EFF;
        margin-right: 6px;
        font-size: 8px;
        vertical-align: middle;
      }
    }
  }
}

/* 余额规则表格 */
.rule-pass { color: #67C23A; font-weight: 500; }
.rule-reject { color: #F56C6C; font-weight: 500; }

/* 规范卡片 */
.norm-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);

  .norm-header {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 12px;
    i { margin-right: 6px; }
  }
  p {
    font-size: 13px;
    line-height: 1.8;
    margin: 0 0 4px 0;
    color: #606266;
  }
}

/* 速记卡片 */
.summary-card {
  background: #fff;
  border-radius: 10px;
  padding: 24px 16px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  margin-bottom: 16px;
  border: 1px solid #ebeef5;
  transition: transform 0.2s;

  &:hover { transform: translateY(-2px); }

  .summary-icon {
    width: 56px; height: 56px;
    border-radius: 50%;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 10px;
    i { font-size: 26px; color: #fff; }
  }
  h3 { font-size: 17px; margin: 0 0 12px 0; color: #303133; }

  .flow-line {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-wrap: wrap;
    gap: 4px;
    margin-bottom: 12px;
    .flow-node {
      background: #f0f2f5;
      padding: 3px 8px;
      border-radius: 4px;
      font-size: 12px;
      font-weight: 500;
      color: #303133;
      white-space: nowrap;
    }
    .el-icon-right { font-size: 12px; color: #c0c4cc; }
  }

  .summary-result {
    margin-top: 8px;
  }
}

.summary-budget .summary-icon { background: #409EFF; }
.summary-expense .summary-icon { background: #67C23A; }
.summary-adjust .summary-icon { background: #E6A23C; }

/* 盈利分析表权限卡片 */
.perm-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #ebeef5;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  margin-bottom: 16px;
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 14px rgba(0,0,0,0.1);
  }

  .perm-header {
    padding: 12px 16px;
    font-size: 14px;
    font-weight: 600;
    color: #fff;
    i { margin-right: 6px; }
  }
  .perm-body {
    padding: 14px 16px;
    .perm-scope { margin-bottom: 10px; }
    ul {
      margin: 0;
      padding-left: 0;
      list-style: none;
      li {
        padding: 3px 0;
        font-size: 13px;
        color: #606266;
        line-height: 1.7;
        &::before {
          content: "●";
          margin-right: 6px;
          font-size: 8px;
          vertical-align: middle;
        }
      }
    }
  }
}

.perm-dept .perm-header { background: #67C23A; }
.perm-dept .perm-body ul li::before { color: #67C23A; }
.perm-branch .perm-header { background: #E6A23C; }
.perm-branch .perm-body ul li::before { color: #E6A23C; }
.perm-hq .perm-header { background: #F56C6C; }
.perm-hq .perm-body ul li::before { color: #F56C6C; }

.perm-summary {
  text-align: center;
  margin-top: 4px;
  padding: 12px 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  border-radius: 8px;
  font-size: 14px;
  color: #606266;
  i { color: #E6A23C; margin-right: 6px; }
  strong { color: #303133; }
}

/* 财务科目分类卡片 */
.subject-intro {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;

  h4 {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
    margin: 0 0 12px 0;
    i { margin-right: 6px; color: #409EFF; }
  }
}

.cat-card {
  text-align: center;
  padding: 14px 8px;
  border-radius: 6px;
  margin-bottom: 12px;
  border: 1px solid #ebeef5;
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  }

  .cat-icon {
    font-size: 24px;
    margin-bottom: 6px;
  }
  .cat-name {
    font-size: 13px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 2px;
  }
  .cat-desc {
    font-size: 11px;
    color: #909399;
  }
}

.cat-asset { background: #fef0f0; .cat-icon { color: #F56C6C; } }
.cat-liability { background: #fdf6ec; .cat-icon { color: #E6A23C; } }
.cat-equity { background: #f0f9eb; .cat-icon { color: #67C23A; } }
.cat-income { background: #ecf5ff; .cat-icon { color: #409EFF; } }
.cat-expense { background: #fef0f0; .cat-icon { color: #F56C6C; } }
.cat-profit { background: #f4ecff; .cat-icon { color: #9B59B6; } }

/* 分层级审批说明 */
.level-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.level-card {
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #ebeef5;

  .level-badge {
    padding: 8px 14px;
    font-size: 13px;
    font-weight: 600;
    color: #fff;
    i { margin-right: 4px; }
  }
  .level-body {
    padding: 10px 14px;
    background: #fff;
    p {
      margin: 0 0 4px 0;
      font-size: 13px;
      color: #606266;
      line-height: 1.7;
    }
    .level-note {
      font-size: 12px;
      color: #909399;
      margin-top: 4px;
      margin-bottom: 0;
      i { margin-right: 3px; }
    }
  }
}

.level-dept .level-badge { background: #67C23A; }
.level-branch .level-badge { background: #E6A23C; }
.level-hq .level-badge { background: #F56C6C; }

/* 资金池流转图 */
.pool-flow {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
  padding: 8px 0;
}

.pool-node {
  padding: 6px 16px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  text-align: center;
  width: 140px;
}

.pool-hq { background: #F56C6C; }
.pool-branch { background: #E6A23C; }
.pool-dept { background: #67C23A; }
.pool-subject { background: #409EFF; }

.pool-arrow {
  font-size: 12px;
  color: #909399;
  padding: 2px 0;
  i { margin-right: 2px; }
}

.dimension-footer {
  text-align: center;
  margin-top: 8px;
  padding: 14px 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  border-radius: 8px;
  font-size: 15px;
  color: #606266;
  i { color: #E6A23C; margin-right: 6px; }
  strong { color: #303133; }
}
</style>
