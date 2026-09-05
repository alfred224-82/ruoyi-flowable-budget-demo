# ruoyi-app · 资金管控微信小程序（单包多角色）

> 里程碑：M8（T8.1~T8.6 全量交付），单包多角色（ADR M-1）
> 依据：《各角色菜单权限文档》第五节（5.1 Tab 分流 / 5.2 页面级权限 / 5.3 版本增量）
> 技术栈：uni-app（Vue2），编译目标为微信小程序

## 一、目录结构

```
ruoyi-app/
├── App.vue                    # 应用入口：未登录跳登录页
├── main.js / manifest.json / pages.json / uni.scss
├── config/index.js            # baseUrl 与全量接口清单（联调时修改）
├── utils/
│   ├── auth.js                # token / 角色 / 用户信息 / 当前组织本地存储
│   ├── request.js             # 请求封装：JWT 头、401 回登录、错误透传、文件上传
│   ├── roles.js               # 角色识别 + Tab 分流表（5.1）+ applyRoleTabs()
│   ├── features.js            # 版本能力开关（/system/edition/features，防串③层）
│   ├── format.js              # 金额/日期格式化 + 六组单据状态映射
│   └── permission.js          # 页面级权限守卫（5.2 白名单 + guardPage()）
└── pages/
    ├── login/index.vue        # 登录：token→getInfo 取角色→能力开关→专业版选组织→应用 Tab 分流
    ├── tab-home/…             # Tab1：老板=看板摘要 / 项目经理=速览 / 报销人=首页待办 / 财务=通知列表（T8.6）
    ├── tab-work/…             # Tab2：老板=项目入口 / 项目经理=收入确认 / 报销人=发起报销 / 财务=预警列表（T8.6）
    ├── tab-approve/…          # Tab3：老板=审批入口+待办数 / 报销人(含项目经理兼任)=我的报销+补票 / 财务=不开放
    ├── tab-mine/…             # Tab4：我的（全角色；高级版信用分卡片、专业版组织切换按开关渲染）
    ├── dashboard/category.vue # 大类看板（老板）：总览/大类🟢🟡🔴/三层下钻/固定成本/风险预警/未归类池
    ├── project/…              # 项目列表/详情（利润拆解/科目/成员/立项审批）、收入确认列表/新建
    ├── approval/…             # 审批列表/详情：报销/付款/变更/立项四类（高级版证据等级标记）
    └── expense/…              # 新建报销（后补通道/硬软规则/打卡/人脸）、我的报销、补票页
```

## 二、角色分流机制（T8.1）

1. 登录成功后 `getInfo` 返回角色集合（`boss/finance/manager/employee`），写入本地；
2. 多角色按优先级取主角色（老板 > 项目经理 > 报销人 > 财务，权限文档 6.6 并集原则）；
3. `applyRoleTabs()` 用 `uni.setTabBarItem` 动态改 Tab 标题；各 Tab 页内部再按角色渲染内容；
4. **平台管理员不使用小程序**（权限文档 3.5/5.1）：仅持 `platform_admin` 角色登录时提示改用 PC 端；
5. 页面级权限：业务页面 `onLoad` 调 `guardPage(路径)`，越权提示并返回（验收：越权页面不可达）。

## 三、版本增量渲染（防串③层，T8.5）

- 登录后调 `/system/edition/features` 缓存当前租户开关集；
- 页面内用 `hasFeature('FEAT_XXX')` 控制增量能力显隐（信用分卡片、组织切换、打卡流程、人脸核身等）；
- **后端 `@RequireFeature` 为最终防线**：前端隐藏仅为体验，低版本直连接口仍被 403 拦截。

## 四、安全拦截联动（T3.4）

- 租户到期/停用后：登录被拒、已登录会话访问任意接口返回"租户授权已到期…"，`request.js` 统一提示；
- 401 自动清登录态回登录页。

## 五、各任务实现说明（T8.2~T8.6）

| 任务 | 实现 |
|------|------|
| T8.2 | 大类看板与 PC 3215/3288 同源（`/fee/dashboard`）；项目卡片含预算进度🟢🟡🔴；审批四类单据，报销审批为 POST、付款/变更/立项为 PUT；项目详情支持老板立项审批 |
| T8.3 | 我的项目/收入确认数据范围由后端按 `fee_project_member` 过滤；收入三态（待核验/已核验/已驳回）列表与新建（提交即待核验） |
| T8.4 | 报销表单：后补通道（勾选后自动创建补票记录，预计补票 +7 天）、项目关联硬/软规则（`/fee/linkRule/match`）、映射规则自动定位预算科目；我的报销含驳回原因展示；补票页支持发票图片上传补传（预占转实扣） |
| T8.5 | 高级版：打卡流程（FEAT_TIME_CLOCK，定位随单上报，≥500 预判必打卡）、审批证据等级标记（FEAT_EVIDENCE_CHAIN）、信用分卡片（FEAT_CREDIT_SCORE，后端未就绪优雅降级）；专业版：登录选组织（FEAT_MULTI_ORG）、大额≥2000 人脸核身提示（FEAT_FACE_RECOGNITION，采集流程部署中可继续提交） |
| T8.6 | 财务仅开放通知/预警查看，不开放业务 Tab（Tab1 通知列表 + Tab2 预警列表） |

### 关键交互约定

- 分页：`TableDataInfo`（rows/total）+ pageNum/pageSize；非分页接口为 `R`（data）；
- 补票上传依赖 `system:oss:upload` 权限，员工无权限时提示后降级（不阻断主流程）；
- 驳回报销单原因后端强校验 5-500 字符，前端同步预校验。

## 六、运行

1. 用 HBuilderX 导入本目录（uni-app 项目）；
2. 修改 `config/index.js` 的 `baseUrl` 指向后端网关；
3. 微信公众平台配置小程序 appid（`manifest.json`），配置服务器域名白名单。
