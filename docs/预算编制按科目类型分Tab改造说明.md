# 预算编制按科目类型分Tab显示改造说明

## 一、改造概述

将预算编制页面从**按一级科目分Tab**改为**按科目类型（budget_type）分Tab**显示，使预算管理更加符合财务分类习惯。

---

## 二、科目类型定义

参考 `budget_subject` 表的 `subject_type` 字段，定义以下6种科目类型：

| 类型代码 | 中文名称 | 说明 | 示例科目 |
|---------|---------|------|---------|
| INCOME | 收入类 | 企业主营业务收入 | 营业收入、投资收益 |
| COST | 成本类 | 企业主营业务成本 | 营业成本、直接材料 |
| EXPENSE | 费用类 | 企业管理活动费用 | 销售费用、管理费用 |
| ASSET | 资产类 | 企业拥有的资产 | 流动资产、固定资产 |
| LIABILITY | 负债类 | 企业承担的债务 | 流动负债、长期负债 |
| EQUITY | 权益类 | 所有者权益 | 实收资本、未分配利润 |

---

## 三、数据库表结构

### budget_preparation_detail 表

表中已有 `subject_type` 字段（VARCHAR(20)），用于存储科目类型：

```sql
CREATE TABLE `budget_preparation_detail` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `sheet_id` BIGINT(20) NOT NULL,
  `subject_code` VARCHAR(20) NOT NULL,
  `subject_name` VARCHAR(100),
  `subject_type` VARCHAR(20) DEFAULT NULL COMMENT '科目类型（INCOME/COST/EXPENSE/ASSET/LIABILITY/EQUITY）',
  `budget_amount` DECIMAL(18,2) DEFAULT 0.00,
  ...
);
```

**注意**: `subject_type` 字段从 `budget_subject` 表的对应科目继承而来。

---

## 四、前端改造详情

### 1. 预算编制向导页面（wizard.vue）

#### 改造前
- Tab按一级科目分组（如：营业收入、营业成本、销售费用等）
- 每个Tab显示该一级科目下的所有二级科目
- 数据结构：`firstLevelSubjects[]` + `secondLevelSubjects{parentId: []}`

#### 改造后
- Tab按科目类型分组（收入类、成本类、费用类、资产类、负债类、权益类）
- 每个Tab显示该类型下的所有科目（包括多级）
- 数据结构：`budgetTypes[]` + `subjectsByType{typeValue: []}`

#### 关键代码变更

**1.1 数据定义**
```javascript
// 预算类型列表
budgetTypes: [
  { value: 'INCOME', label: '收入类' },
  { value: 'COST', label: '成本类' },
  { value: 'EXPENSE', label: '费用类' },
  { value: 'ASSET', label: '资产类' },
  { value: 'LIABILITY', label: '负债类' },
  { value: 'EQUITY', label: '权益类' }
],
// 按类型分组的科目数据
subjectsByType: {}
```

**1.2 Tab渲染**
```vue
<el-tabs v-model="activeTab" type="border-card">
  <el-tab-pane
    v-for="(type, index) in budgetTypes"
    :key="type.value"
    :label="type.label"
    :name="type.value"
  >
    <el-table :data="subjectsByType[type.value] || []">
      <!-- 表格列 -->
    </el-table>
  </el-tab-pane>
</el-tabs>
```

**1.3 加载科目逻辑**
```javascript
async loadSubjectsByType() {
  // 1. 初始化每个类型的数组
  this.budgetTypes.forEach(type => {
    this.$set(this.subjectsByType, type.value, []);
  });
  
  // 2. 获取所有一级科目
  const firstLevelSubjects = await listFirstLevelSubject();
  
  // 3. 对每个一级科目，递归获取所有子科目
  for (const firstLevel of firstLevelSubjects) {
    const children = await this.getAllChildren(firstLevel.id);
    
    // 4. 将科目按类型分组
    children.forEach(subject => {
      const subjectType = subject.subjectType || firstLevel.subjectType;
      if (this.subjectsByType[subjectType]) {
        this.subjectsByType[subjectType].push({
          ...subject,
          budgetAmount: 0,
          remark: '',
          parentSubjectName: firstLevel.subjectName
        });
      }
    });
  }
}
```

**1.4 递归获取子科目**
```javascript
async getAllChildren(parentId) {
  const result = [];
  const children = await listSecondLevelSubject(parentId);
  result.push(...children);
  
  // 递归获取子科目的子科目
  for (const child of children) {
    const subChildren = await this.getAllChildren(child.id);
    result.push(...subChildren);
  }
  return result;
}
```

**1.5 完成编制时保存数据**
```javascript
const details = this.allDetailData.map(item => {
  // 找到对应的科目以获取subjectType
  let subjectType = '';
  for (const type of this.budgetTypes) {
    const subjects = this.subjectsByType[type.value];
    const subject = subjects?.find(s => s.subjectCode === item.subjectCode);
    if (subject) {
      subjectType = subject.subjectType || type.value;
      break;
    }
  }
  
  return {
    sheetId: this.preparationData.id,
    subjectCode: item.subjectCode,
    subjectName: item.subjectName,
    subjectType: subjectType,  // 保存科目类型
    budgetAmount: item.budgetAmount,
    remark: item.remark
  };
});
```

**1.6 汇总展示表格**
```vue
<el-table :data="allDetailData">
  <el-table-column label="科目类型" prop="budgetType" />
  <el-table-column label="一级科目" prop="parentSubjectName" />
  <el-table-column label="科目编码" prop="subjectCode" />
  <el-table-column label="科目名称" prop="subjectName" />
  <el-table-column label="预算金额" prop="budgetAmount" />
  <el-table-column label="说明" prop="remark" />
</el-table>
```

---

### 2. 预算审核详细页面（approvalDetail.vue）

#### 改造内容
与向导页面相同，将Tab从按一级科目分组改为按科目类型分组。

#### 关键变更
- 添加 `budgetTypes` 和 `subjectsByType` 数据
- 修改 `loadSubjectsByType()` 方法
- 修改 `loadDetailData()` 方法
- 修改Tab渲染逻辑

---

## 五、优势分析

### 改造前的缺点
1. **Tab数量不固定** - 取决于一级科目的数量，可能很多
2. **分类不清晰** - 不同性质的科目混在一起
3. **不符合财务习惯** - 财务人员习惯按科目性质分类

### 改造后的优点
1. **Tab数量固定** - 始终6个Tab，界面简洁
2. **分类清晰** - 按科目性质（收入/成本/费用等）分组
3. **符合财务规范** - 与会计准则的科目分类一致
4. **便于审核** - 审批人可以按类型快速定位问题
5. **易于扩展** - 新增科目自动归入对应类型

---

## 六、数据处理流程

### 1. 加载流程
```
用户进入编制页面
    ↓
调用 loadSubjectsByType()
    ↓
获取所有一级科目（level=1）
    ↓
对每个一级科目递归获取所有子科目
    ↓
根据 subject_type 将科目分组到6个类型中
    ↓
渲染6个Tab，每个Tab显示对应类型的科目列表
```

### 2. 保存流程
```
用户填写预算金额
    ↓
点击"完成编制"
    ↓
遍历 subjectsByType 收集所有填写了金额的科目
    ↓
为每个科目查找对应的 subjectType
    ↓
构建明细数据数组（包含 subjectType 字段）
    ↓
调用 batchSavePreparationDetail() 批量保存
    ↓
更新主表的 total_budget
```

### 3. 编辑加载流程
```
用户点击"修改"进入编辑模式
    ↓
调用 loadSubjectsByType() 加载所有科目
    ↓
调用 loadDetailData(sheetId) 加载已保存的明细
    ↓
将明细数据映射回 subjectsByType 中的对应科目
    ↓
填充 budgetAmount 和 remark 字段
    ↓
用户可以看到之前填写的数据
```

---

## 七、注意事项

### ⚠️ 重要提醒

1. **科目类型来源**
   - `subject_type` 字段来自 `budget_subject` 表
   - 如果科目没有设置 `subject_type`，则继承其一级科目的类型
   - 确保所有科目都正确设置了类型

2. **递归深度**
   - `getAllChildren()` 方法会递归获取所有层级的子科目
   - 如果科目层级很深（超过5层），可能需要优化性能
   - 建议科目层级控制在3-4层以内

3. **数据一致性**
   - 保存时必须确保 `subjectType` 字段正确填充
   - 后端保存时应验证 `subjectType` 的有效性
   - 只允许6种预定义的类型值

4. **空Tab处理**
   - 如果某个类型下没有科目，Tab仍然显示但表格为空
   - 默认激活第一个有数据的Tab
   - 避免用户看到空白页面

5. **性能优化**
   - 所有科目在页面加载时一次性获取
   - Tab切换时无需再次请求数据
   - 如果科目数量超过1000，考虑虚拟滚动或分页

---

## 八、测试要点

### 功能测试
- [ ] 6个Tab正确显示
- [ ] 每个Tab显示对应类型的科目
- [ ] 科目可以正确填写金额
- [ ] 预算总额计算正确
- [ ] 保存时 subjectType 字段正确
- [ ] 编辑时可以正确加载历史数据
- [ ] 审核页面也能正确显示

### 边界测试
- [ ] 某个类型下没有科目时的显示
- [ ] 科目层级超过3层时的递归获取
- [ ] 科目没有设置 subject_type 时的继承逻辑
- [ ] 大量科目（500+）时的页面性能

### 数据一致性测试
- [ ] 保存后查询数据库，subject_type 字段正确
- [ ] 重新加载后数据不丢失
- [ ] 修改金额后保存正确

---

## 九、后续优化建议

1. **科目搜索**
   - 在每个Tab内添加搜索框
   - 支持按科目编码或名称快速定位

2. **批量操作**
   - 支持批量设置相同金额
   - 支持从一个期间复制到另一个期间

3. **数据校验**
   - 实时校验预算金额的合理性
   - 与历史数据对比提示异常

4. **可视化**
   - 添加饼图显示各类型占比
   - 添加趋势图显示月度变化

5. **权限控制**
   - 某些类型只对特定角色可见
   - 敏感科目金额脱敏显示

---

**改造完成时间**: 2026-06-19  
**涉及文件**: 
- `ruoyi-ui/src/views/system/preparation/wizard.vue`
- `ruoyi-ui/src/views/system/preparation/approvalDetail.vue`

**状态**: ✅ 已完成
