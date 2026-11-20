-- =====================================================================
-- M18_11 全局敏感词底库初始化 + 旧表迁移脚本（T18.7，v1.30 新增）
-- 版本：v1.0  日期：2026-09-19
-- 依据：《资金管控系统开发任务文档》v1.27 M18 图片合规与全局敏感词
--       《V3.8_当日草稿_图片合规与敏感词.md》附录 A 八大类底库
--       《广告法》第九条（极限词）/《网络信息内容生态治理规定》第六·七条（谣言/违法信息）
-- 前置依赖：M18_10（sensitive_word 表已建立）
-- 规范（合规硬约束）：
--   1. 全部 INSERT 走 INSERT IGNORE 幂等，重复执行 0 影响（uk_tenant_word_category 保护）
--   2. 不 DROP、不 TRUNCATE、不 DELETE 全表
--   3. 全局底库 tenant_id=0 / source=global / status=0（启用）/ match_mode=exact
--      方言·谐音变体 match_mode=fuzzy（编辑距离/拼音相似匹配）
--   4. industry_scope=NULL（全行业通用）
--   5. 旧表 sensitive_word_library → 新表 sensitive_word 幂等迁移（不动旧表原数据）
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. A.2 政治/涉密（POLITICAL）
-- ----------------------------
INSERT IGNORE INTO `sensitive_word` (`tenant_id`,`word`,`category`,`match_mode`,`industry_scope`,`status`,`source`,`create_by`,`create_time`,`remark`) VALUES
(0,'颠覆国家政权','POLITICAL','exact',NULL,'0','global','admin',NOW(),'V3.8 A.2 政治涉密'),
(0,'分裂国家','POLITICAL','exact',NULL,'0','global','admin',NOW(),'V3.8 A.2 政治涉密'),
(0,'恐怖主义','POLITICAL','exact',NULL,'0','global','admin',NOW(),'V3.8 A.2 政治涉密'),
(0,'邪教','POLITICAL','exact',NULL,'0','global','admin',NOW(),'V3.8 A.2 政治涉密'),
(0,'反政府','POLITICAL','exact',NULL,'0','global','admin',NOW(),'V3.8 A.2 政治涉密'),
(0,'国家机密','POLITICAL','exact',NULL,'0','global','admin',NOW(),'V3.8 A.2 涉密'),
(0,'军事机密','POLITICAL','exact',NULL,'0','global','admin',NOW(),'V3.8 A.2 涉密'),
(0,'泄密','POLITICAL','exact',NULL,'0','global','admin',NOW(),'V3.8 A.2 涉密'),
(0,'煽动分裂','POLITICAL','exact',NULL,'0','global','admin',NOW(),'V3.8 A.2 政治涉密'),
(0,'非法集会','POLITICAL','exact',NULL,'0','global','admin',NOW(),'V3.8 A.2 政治涉密'),
(0,'游行示威','POLITICAL','exact',NULL,'0','global','admin',NOW(),'V3.8 A.2 政治涉密'),
(0,'暴乱','POLITICAL','exact',NULL,'0','global','admin',NOW(),'V3.8 A.2 政治涉密'),
(0,'颜色革命','POLITICAL','exact',NULL,'0','global','admin',NOW(),'V3.8 A.2 政治涉密'),
(0,'政变','POLITICAL','exact',NULL,'0','global','admin',NOW(),'V3.8 A.2 政治涉密'),
(0,'间谍','POLITICAL','exact',NULL,'0','global','admin',NOW(),'V3.8 A.2 涉密'),
(0,'卖国','POLITICAL','exact',NULL,'0','global','admin',NOW(),'V3.8 A.2 政治涉密'),
(0,'叛国','POLITICAL','exact',NULL,'0','global','admin',NOW(),'V3.8 A.2 政治涉密'),
(0,'台独','POLITICAL','exact',NULL,'0','global','admin',NOW(),'V3.8 A.2 政治涉密'),
(0,'藏独','POLITICAL','exact',NULL,'0','global','admin',NOW(),'V3.8 A.2 政治涉密'),
(0,'疆独','POLITICAL','exact',NULL,'0','global','admin',NOW(),'V3.8 A.2 政治涉密'),
(0,'港独','POLITICAL','exact',NULL,'0','global','admin',NOW(),'V3.8 A.2 政治涉密'),
(0,'法轮','POLITICAL','exact',NULL,'0','global','admin',NOW(),'V3.8 A.2 邪教'),
(0,'圣战','POLITICAL','exact',NULL,'0','global','admin',NOW(),'V3.8 A.2 政治涉密');

-- ----------------------------
-- 2. A.3 广告极限词（AD，依《广告法》第九条）
-- ----------------------------
INSERT IGNORE INTO `sensitive_word` (`tenant_id`,`word`,`category`,`match_mode`,`industry_scope`,`status`,`source`,`create_by`,`create_time`,`remark`) VALUES
(0,'国家级','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'最高级','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'最佳','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'第一','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'顶级','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'极品','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'绝对','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'独一无二','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'首选','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'独家','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'首个','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'唯一','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'王牌','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'销量第一','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'全国第一','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'世界领先','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'国际品质','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'史上最','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'全网最低','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'史上最低','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'百分百','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'100%','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'永久','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'万能','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'驰名商标','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'免检','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'质量免检','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'中国名牌','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'纯天然','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'祖传秘方','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'特效','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'根治','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'药到病除','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'包治百病','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'无效退款','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'稳赚不赔','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'保本保息','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'零风险','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'一夜暴富','AD','exact',NULL,'0','global','admin',NOW(),'广告法第九条极限词'),
(0,'点击领红包','AD','exact',NULL,'0','global','admin',NOW(),'诱导性广告');

-- ----------------------------
-- 3. A.4 色情/低俗（PORN）
-- ----------------------------
INSERT IGNORE INTO `sensitive_word` (`tenant_id`,`word`,`category`,`match_mode`,`industry_scope`,`status`,`source`,`create_by`,`create_time`,`remark`) VALUES
(0,'色情','PORN','exact',NULL,'0','global','admin',NOW(),'V3.8 A.4 色情低俗'),
(0,'黄色视频','PORN','exact',NULL,'0','global','admin',NOW(),'V3.8 A.4 色情低俗'),
(0,'裸聊','PORN','exact',NULL,'0','global','admin',NOW(),'V3.8 A.4 色情低俗'),
(0,'援交','PORN','exact',NULL,'0','global','admin',NOW(),'V3.8 A.4 色情低俗'),
(0,'一夜情','PORN','exact',NULL,'0','global','admin',NOW(),'V3.8 A.4 色情低俗'),
(0,'约炮','PORN','exact',NULL,'0','global','admin',NOW(),'V3.8 A.4 色情低俗'),
(0,'卖淫','PORN','exact',NULL,'0','global','admin',NOW(),'V3.8 A.4 色情低俗'),
(0,'嫖娼','PORN','exact',NULL,'0','global','admin',NOW(),'V3.8 A.4 色情低俗'),
(0,'成人用品','PORN','exact',NULL,'0','global','admin',NOW(),'V3.8 A.4 色情低俗'),
(0,'情色','PORN','exact',NULL,'0','global','admin',NOW(),'V3.8 A.4 色情低俗'),
(0,'AV女优','PORN','exact',NULL,'0','global','admin',NOW(),'V3.8 A.4 色情低俗'),
(0,'福利姬','PORN','exact',NULL,'0','global','admin',NOW(),'V3.8 A.4 色情低俗'),
(0,'裸照','PORN','exact',NULL,'0','global','admin',NOW(),'V3.8 A.4 色情低俗'),
(0,'不雅照','PORN','exact',NULL,'0','global','admin',NOW(),'V3.8 A.4 色情低俗'),
(0,'性服务','PORN','exact',NULL,'0','global','admin',NOW(),'V3.8 A.4 色情低俗'),
(0,'特殊服务','PORN','exact',NULL,'0','global','admin',NOW(),'V3.8 A.4 色情低俗'),
(0,'包养','PORN','exact',NULL,'0','global','admin',NOW(),'V3.8 A.4 色情低俗'),
(0,'小三','PORN','exact',NULL,'0','global','admin',NOW(),'V3.8 A.4 色情低俗'),
(0,'迷奸','PORN','exact',NULL,'0','global','admin',NOW(),'V3.8 A.4 色情低俗'),
(0,'春药','PORN','exact',NULL,'0','global','admin',NOW(),'V3.8 A.4 色情低俗'),
(0,'催情','PORN','exact',NULL,'0','global','admin',NOW(),'V3.8 A.4 色情低俗'),
(0,'露点','PORN','exact',NULL,'0','global','admin',NOW(),'V3.8 A.4 色情低俗'),
(0,'三级片','PORN','exact',NULL,'0','global','admin',NOW(),'V3.8 A.4 色情低俗'),
(0,'成人直播','PORN','exact',NULL,'0','global','admin',NOW(),'V3.8 A.4 色情低俗'),
(0,'性交易','PORN','exact',NULL,'0','global','admin',NOW(),'V3.8 A.4 色情低俗');

-- ----------------------------
-- 4. A.5 暴恐/违禁（VIOLENCE）
-- ----------------------------
INSERT IGNORE INTO `sensitive_word` (`tenant_id`,`word`,`category`,`match_mode`,`industry_scope`,`status`,`source`,`create_by`,`create_time`,`remark`) VALUES
(0,'枪支','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 暴恐违禁'),
(0,'弹药','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 暴恐违禁'),
(0,'炸药','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 暴恐违禁'),
(0,'炸弹','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 暴恐违禁'),
(0,'雷管','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 暴恐违禁'),
(0,'管制刀具','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 暴恐违禁'),
(0,'砍刀','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 暴恐违禁'),
(0,'匕首','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 暴恐违禁'),
(0,'毒品','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 违禁'),
(0,'冰毒','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 违禁'),
(0,'海洛因','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 违禁'),
(0,'大麻','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 违禁'),
(0,'摇头丸','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 违禁'),
(0,'K粉','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 违禁'),
(0,'迷药','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 违禁'),
(0,'麻醉枪','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 违禁'),
(0,'暗杀','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 暴恐'),
(0,'绑架','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 暴恐'),
(0,'爆炸袭击','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 暴恐'),
(0,'自杀式袭击','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 暴恐'),
(0,'制造炸弹','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 暴恐'),
(0,'赌博','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 违禁'),
(0,'地下赌场','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 违禁'),
(0,'六合彩','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 违禁'),
(0,'私彩','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 违禁'),
(0,'高利贷','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 违禁'),
(0,'套路贷','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 违禁'),
(0,'代孕','VIOLENCE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.5 违禁');

-- ----------------------------
-- 5. A.6 迷信/封建（SUPERSTITION）
-- ----------------------------
INSERT IGNORE INTO `sensitive_word` (`tenant_id`,`word`,`category`,`match_mode`,`industry_scope`,`status`,`source`,`create_by`,`create_time`,`remark`) VALUES
(0,'算命','SUPERSTITION','exact',NULL,'0','global','admin',NOW(),'V3.8 A.6 迷信封建'),
(0,'看相','SUPERSTITION','exact',NULL,'0','global','admin',NOW(),'V3.8 A.6 迷信封建'),
(0,'占卜','SUPERSTITION','exact',NULL,'0','global','admin',NOW(),'V3.8 A.6 迷信封建'),
(0,'风水改运','SUPERSTITION','exact',NULL,'0','global','admin',NOW(),'V3.8 A.6 迷信封建'),
(0,'转运','SUPERSTITION','exact',NULL,'0','global','admin',NOW(),'V3.8 A.6 迷信封建'),
(0,'驱邪','SUPERSTITION','exact',NULL,'0','global','admin',NOW(),'V3.8 A.6 迷信封建'),
(0,'做法事','SUPERSTITION','exact',NULL,'0','global','admin',NOW(),'V3.8 A.6 迷信封建'),
(0,'请神','SUPERSTITION','exact',NULL,'0','global','admin',NOW(),'V3.8 A.6 迷信封建'),
(0,'降头','SUPERSTITION','exact',NULL,'0','global','admin',NOW(),'V3.8 A.6 迷信封建'),
(0,'蛊毒','SUPERSTITION','exact',NULL,'0','global','admin',NOW(),'V3.8 A.6 迷信封建'),
(0,'符咒','SUPERSTITION','exact',NULL,'0','global','admin',NOW(),'V3.8 A.6 迷信封建'),
(0,'开光改命','SUPERSTITION','exact',NULL,'0','global','admin',NOW(),'V3.8 A.6 迷信封建'),
(0,'改命','SUPERSTITION','exact',NULL,'0','global','admin',NOW(),'V3.8 A.6 迷信封建'),
(0,'招财转运','SUPERSTITION','exact',NULL,'0','global','admin',NOW(),'V3.8 A.6 迷信封建'),
(0,'消灾','SUPERSTITION','exact',NULL,'0','global','admin',NOW(),'V3.8 A.6 迷信封建'),
(0,'通灵','SUPERSTITION','exact',NULL,'0','global','admin',NOW(),'V3.8 A.6 迷信封建'),
(0,'跳大神','SUPERSTITION','exact',NULL,'0','global','admin',NOW(),'V3.8 A.6 迷信封建'),
(0,'冥币','SUPERSTITION','exact',NULL,'0','global','admin',NOW(),'V3.8 A.6 迷信封建'),
(0,'阴婚','SUPERSTITION','exact',NULL,'0','global','admin',NOW(),'V3.8 A.6 迷信封建'),
(0,'养小鬼','SUPERSTITION','exact',NULL,'0','global','admin',NOW(),'V3.8 A.6 迷信封建');

-- ----------------------------
-- 6. A.7 谩骂/歧视（ABUSE）
-- ----------------------------
INSERT IGNORE INTO `sensitive_word` (`tenant_id`,`word`,`category`,`match_mode`,`industry_scope`,`status`,`source`,`create_by`,`create_time`,`remark`) VALUES
(0,'傻逼','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 谩骂歧视'),
(0,'煞笔','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 谩骂歧视'),
(0,'fuck','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 谩骂歧视'),
(0,'shit','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 谩骂歧视'),
(0,'贱人','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 谩骂歧视'),
(0,'婊子','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 谩骂歧视'),
(0,'狗屎','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 谩骂歧视'),
(0,'王八蛋','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 谩骂歧视'),
(0,'畜生','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 谩骂歧视'),
(0,'去死','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 谩骂歧视'),
(0,'废物','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 谩骂歧视'),
(0,'垃圾人','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 谩骂歧视'),
(0,'智障','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 歧视'),
(0,'脑残','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 歧视'),
(0,'低能儿','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 歧视'),
(0,'死瘸子','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 歧视残障'),
(0,'死瞎子','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 歧视残障'),
(0,'黑鬼','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 种族歧视'),
(0,'支那','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 种族歧视'),
(0,'棒子','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 种族歧视'),
(0,'阿三','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 种族歧视'),
(0,'死外地人','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 地域歧视'),
(0,'乡巴佬','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 地域歧视'),
(0,'娘炮','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 性别歧视'),
(0,'死gay','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 性向歧视'),
(0,'人妖','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 性向歧视'),
(0,'绿茶婊','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 谩骂歧视'),
(0,'滚蛋','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 谩骂'),
(0,'狗东西','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 谩骂'),
(0,'王八','ABUSE','exact',NULL,'0','global','admin',NOW(),'V3.8 A.7 谩骂');

-- ----------------------------
-- 7. A.8 谣言/虚假（RUMOR，依《网络信息内容生态治理规定》第六·七条）
-- ----------------------------
INSERT IGNORE INTO `sensitive_word` (`tenant_id`,`word`,`category`,`match_mode`,`industry_scope`,`status`,`source`,`create_by`,`create_time`,`remark`) VALUES
(0,'内部消息','RUMOR','exact',NULL,'0','global','admin',NOW(),'生态治理规定第六七条'),
(0,'据可靠消息','RUMOR','exact',NULL,'0','global','admin',NOW(),'生态治理规定第六七条'),
(0,'官方未证实','RUMOR','exact',NULL,'0','global','admin',NOW(),'生态治理规定第六七条'),
(0,'震惊体','RUMOR','exact',NULL,'0','global','admin',NOW(),'标题党谣言'),
(0,'不转不是中国人','RUMOR','exact',NULL,'0','global','admin',NOW(),'诱导传播谣言'),
(0,'速看速删','RUMOR','exact',NULL,'0','global','admin',NOW(),'诱导传播谣言'),
(0,'央视曝光','RUMOR','exact',NULL,'0','global','admin',NOW(),'冒用权威谣言'),
(0,'紧急通知','RUMOR','exact',NULL,'0','global','admin',NOW(),'诱导传播谣言'),
(0,'某地发生','RUMOR','exact',NULL,'0','global','admin',NOW(),'未经核实谣言'),
(0,'吃死人','RUMOR','exact',NULL,'0','global','admin',NOW(),'食品安全谣言'),
(0,'致癌','RUMOR','exact',NULL,'0','global','admin',NOW(),'健康谣言'),
(0,'塑料做的','RUMOR','exact',NULL,'0','global','admin',NOW(),'食品安全谣言'),
(0,'注水肉','RUMOR','exact',NULL,'0','global','admin',NOW(),'食品安全谣言'),
(0,'转基因有毒','RUMOR','exact',NULL,'0','global','admin',NOW(),'食品安全谣言'),
(0,'疫苗致死','RUMOR','exact',NULL,'0','global','admin',NOW(),'医疗谣言'),
(0,'地震预报','RUMOR','exact',NULL,'0','global','admin',NOW(),'灾害谣言'),
(0,'银行倒闭','RUMOR','exact',NULL,'0','global','admin',NOW(),'金融谣言'),
(0,'货币贬值内幕','RUMOR','exact',NULL,'0','global','admin',NOW(),'金融谣言'),
(0,'政府隐瞒','RUMOR','exact',NULL,'0','global','admin',NOW(),'政治谣言'),
(0,'某明星去世','RUMOR','exact',NULL,'0','global','admin',NOW(),'未经核实谣言');

-- ----------------------------
-- 8. A.9~A.11 方言/谐音变体（DIALECT，match_mode=fuzzy，编辑距离/拼音相似匹配）
-- ----------------------------
INSERT IGNORE INTO `sensitive_word` (`tenant_id`,`word`,`category`,`match_mode`,`industry_scope`,`status`,`source`,`create_by`,`create_time`,`remark`) VALUES
(0,'sb','DIALECT','fuzzy',NULL,'0','global','admin',NOW(),'V3.8 A.9 谐音变体'),
(0,'傻b','DIALECT','fuzzy',NULL,'0','global','admin',NOW(),'V3.8 A.9 谐音变体'),
(0,'沙比','DIALECT','fuzzy',NULL,'0','global','admin',NOW(),'V3.8 A.9 谐音变体'),
(0,'煞逼','DIALECT','fuzzy',NULL,'0','global','admin',NOW(),'V3.8 A.9 谐音变体'),
(0,'尼玛','DIALECT','fuzzy',NULL,'0','global','admin',NOW(),'V3.8 A.9 谐音变体'),
(0,'草泥马','DIALECT','fuzzy',NULL,'0','global','admin',NOW(),'V3.8 A.9 谐音变体'),
(0,'特么','DIALECT','fuzzy',NULL,'0','global','admin',NOW(),'V3.8 A.9 谐音变体'),
(0,'tmd','DIALECT','fuzzy',NULL,'0','global','admin',NOW(),'V3.8 A.10 首字母缩写'),
(0,'nmsl','DIALECT','fuzzy',NULL,'0','global','admin',NOW(),'V3.8 A.10 首字母缩写'),
(0,'wsm','DIALECT','fuzzy',NULL,'0','global','admin',NOW(),'V3.8 A.10 首字母缩写'),
(0,'卧槽','DIALECT','fuzzy',NULL,'0','global','admin',NOW(),'V3.8 A.9 谐音变体'),
(0,'我擦','DIALECT','fuzzy',NULL,'0','global','admin',NOW(),'V3.8 A.9 谐音变体'),
(0,'靠北','DIALECT','fuzzy',NULL,'0','global','admin',NOW(),'V3.8 A.11 方言'),
(0,'干你娘','DIALECT','fuzzy',NULL,'0','global','admin',NOW(),'V3.8 A.11 方言'),
(0,'叼毛','DIALECT','fuzzy',NULL,'0','global','admin',NOW(),'V3.8 A.11 方言'),
(0,'扑街','DIALECT','fuzzy',NULL,'0','global','admin',NOW(),'V3.8 A.11 粤语方言'),
(0,'冚家铲','DIALECT','fuzzy',NULL,'0','global','admin',NOW(),'V3.8 A.11 粤语方言'),
(0,'瓜娃子','DIALECT','fuzzy',NULL,'0','global','admin',NOW(),'V3.8 A.11 川渝方言'),
(0,'哈批','DIALECT','fuzzy',NULL,'0','global','admin',NOW(),'V3.8 A.11 川渝方言'),
(0,'日批','DIALECT','fuzzy',NULL,'0','global','admin',NOW(),'V3.8 A.11 川渝方言'),
(0,'瘪三','DIALECT','fuzzy',NULL,'0','global','admin',NOW(),'V3.8 A.11 吴语方言'),
(0,'戆大','DIALECT','fuzzy',NULL,'0','global','admin',NOW(),'V3.8 A.11 吴语方言'),
(0,'棒槌','DIALECT','fuzzy',NULL,'0','global','admin',NOW(),'V3.8 A.11 北方方言'),
(0,'二球','DIALECT','fuzzy',NULL,'0','global','admin',NOW(),'V3.8 A.11 西北方言'),
(0,'怂样','DIALECT','fuzzy',NULL,'0','global','admin',NOW(),'V3.8 A.11 方言');

-- ----------------------------
-- 9. 旧表迁移：sensitive_word_library（v1.29）→ sensitive_word（v1.27 M18）
--    ★INSERT IGNORE 幂等，不动旧表原数据；category 直传 POLITICAL/PORN/VIOLENCE/ABUSE，
--      其余统一映射为 CUSTOM；tenant_id NULL → 0（平台级归入全局底库），非 NULL 保留租户级
--    ★仅迁移 status='ENABLED' 的有效词；match_mode 统一 exact；source 依 tenant_id 判定
--    ★前置：旧表 sensitive_word_library 存在（M18_01）；不存在则本段 no-op（IF EXISTS 守卫）
-- ----------------------------
SET @has_old_table = (SELECT COUNT(*) FROM information_schema.TABLES
                      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sensitive_word_library');

SET @sql_migrate = IF(@has_old_table > 0,
'INSERT IGNORE INTO `sensitive_word` (`tenant_id`,`word`,`category`,`match_mode`,`industry_scope`,`status`,`source`,`create_by`,`create_time`,`remark`)
 SELECT IFNULL(l.tenant_id, 0),
        l.word,
        CASE WHEN UPPER(IFNULL(l.category,''CUSTOM'')) IN (''POLITICAL'',''PORN'',''VIOLENCE'',''ABUSE'')
             THEN UPPER(l.category) ELSE ''CUSTOM'' END,
        ''exact'',
        NULL,
        ''0'',
        IF(l.tenant_id IS NULL, ''global'', ''tenant''),
        ''migration'',
        NOW(),
        CONCAT(''M18_11 迁移自 sensitive_word_library#'', l.id)
   FROM `sensitive_word_library` l
  WHERE l.status = ''ENABLED'' AND l.word IS NOT NULL AND l.word <> ''''',
'SELECT ''sensitive_word_library not present, skip migration'' AS msg');
PREPARE stmt FROM @sql_migrate;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================================
-- 10. 验收查询（执行后人工核对）
-- =====================================================================
-- 10.1 全局底库总量 ≥200
-- SELECT COUNT(*) FROM sensitive_word WHERE tenant_id=0 AND source='global';
-- 预期：≥200
--
-- 10.2 八大类分布
-- SELECT category, COUNT(*) AS cnt FROM sensitive_word WHERE tenant_id=0 AND source='global'
--   GROUP BY category ORDER BY category;
-- 预期：POLITICAL/AD/PORN/VIOLENCE/SUPERSTITION/ABUSE/RUMOR/DIALECT 八类均有分布
--
-- 10.3 方言类为 fuzzy 匹配
-- SELECT match_mode, COUNT(*) FROM sensitive_word WHERE category='DIALECT' GROUP BY match_mode;
-- 预期：fuzzy
--
-- 10.4 幂等：重复执行本脚本，全局底库数量不变
-- （再次 source M18_11 后重复 10.1，count 应保持一致）
--
-- 10.5 旧表迁移（若 sensitive_word_library 存在且有 ENABLED 词）
-- SELECT COUNT(*) FROM sensitive_word WHERE source='tenant' OR create_by='migration';
-- 预期：> 0（存在旧租户自定义词时）
-- =====================================================================
