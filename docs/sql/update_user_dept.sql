-- User-Dept Association Update Script
-- Generated: 2026-07-25 10:02:09

-- admin (系统管理员) -> 预算智控集团总公司
UPDATE sys_user SET dept_id = 1000 WHERE user_id = 1;
-- zonghe_li (李总) -> 预算智控集团总公司
UPDATE sys_user SET dept_id = 1000 WHERE user_id = 3;
-- bj_leader (张伟) -> 北京分公司
UPDATE sys_user SET dept_id = 1001 WHERE user_id = 4;
-- bj_bb_leader (田宇) -> 北京分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '北京分公司' AND d.dept_name = '北京分公司本部') WHERE user_id = 5;
-- bj_bb_user1 (顾杰) -> 北京分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '北京分公司' AND d.dept_name = '北京分公司本部') WHERE user_id = 6;
-- bj_bb_user2 (罗娟) -> 北京分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '北京分公司' AND d.dept_name = '北京分公司本部') WHERE user_id = 7;
-- bj_js_leader (吴鹏) -> 北京分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '北京分公司' AND d.dept_name = '技术部') WHERE user_id = 8;
-- bj_js_user1 (龚浩) -> 北京分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '北京分公司' AND d.dept_name = '技术部') WHERE user_id = 9;
-- bj_js_user2 (丁杰) -> 北京分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '北京分公司' AND d.dept_name = '技术部') WHERE user_id = 10;
-- bj_js_user3 (石强) -> 北京分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '北京分公司' AND d.dept_name = '技术部') WHERE user_id = 11;
-- bj_xs_leader (杨明) -> 北京分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '北京分公司' AND d.dept_name = '销售部') WHERE user_id = 12;
-- bj_xs_user1 (侯健) -> 北京分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '北京分公司' AND d.dept_name = '销售部') WHERE user_id = 13;
-- bj_xs_user2 (梁健) -> 北京分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '北京分公司' AND d.dept_name = '销售部') WHERE user_id = 14;
-- bj_cw_leader (邓健) -> 北京分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '北京分公司' AND d.dept_name = '财务部') WHERE user_id = 15;
-- bj_cw_user1 (周杰) -> 北京分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '北京分公司' AND d.dept_name = '财务部') WHERE user_id = 16;
-- bj_cw_user2 (陆强) -> 北京分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '北京分公司' AND d.dept_name = '财务部') WHERE user_id = 17;
-- bj_cw_user3 (陈磊) -> 北京分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '北京分公司' AND d.dept_name = '财务部') WHERE user_id = 18;
-- bj_yf_leader (潘健) -> 北京分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '北京分公司' AND d.dept_name = '研发部') WHERE user_id = 19;
-- bj_yf_user1 (武强) -> 北京分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '北京分公司' AND d.dept_name = '研发部') WHERE user_id = 20;
-- bj_yf_user2 (韩雪) -> 北京分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '北京分公司' AND d.dept_name = '研发部') WHERE user_id = 21;
-- bj_xz_leader (蒋超) -> 北京分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '北京分公司' AND d.dept_name = '行政部') WHERE user_id = 22;
-- bj_xz_user1 (严鹏) -> 北京分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '北京分公司' AND d.dept_name = '行政部') WHERE user_id = 23;
-- bj_xz_user2 (史伟) -> 北京分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '北京分公司' AND d.dept_name = '行政部') WHERE user_id = 24;
-- bj_xz_user3 (熊浩) -> 北京分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '北京分公司' AND d.dept_name = '行政部') WHERE user_id = 25;
-- tj_leader (于洋) -> 天津分公司
UPDATE sys_user SET dept_id = 1002 WHERE user_id = 26;
-- tj_bb_leader (庞鹏) -> 天津分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '天津分公司' AND d.dept_name = '天津分公司本部') WHERE user_id = 27;
-- tj_bb_user1 (卢健) -> 天津分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '天津分公司' AND d.dept_name = '天津分公司本部') WHERE user_id = 28;
-- tj_bb_user2 (曹阳) -> 天津分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '天津分公司' AND d.dept_name = '天津分公司本部') WHERE user_id = 29;
-- tj_bb_user3 (殷婷) -> 天津分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '天津分公司' AND d.dept_name = '天津分公司本部') WHERE user_id = 30;
-- tj_js_leader (赵伟) -> 天津分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '天津分公司' AND d.dept_name = '技术部') WHERE user_id = 31;
-- tj_js_user1 (邓颖) -> 天津分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '天津分公司' AND d.dept_name = '技术部') WHERE user_id = 32;
-- tj_js_user2 (黄丽) -> 天津分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '天津分公司' AND d.dept_name = '技术部') WHERE user_id = 33;
-- tj_xs_leader (朱峰) -> 天津分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '天津分公司' AND d.dept_name = '销售部') WHERE user_id = 34;
-- tj_xs_user1 (崔丽) -> 天津分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '天津分公司' AND d.dept_name = '销售部') WHERE user_id = 35;
-- tj_xs_user2 (姜伟) -> 天津分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '天津分公司' AND d.dept_name = '销售部') WHERE user_id = 36;
-- tj_xs_user3 (孙浩) -> 天津分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '天津分公司' AND d.dept_name = '销售部') WHERE user_id = 37;
-- tj_cw_leader (雷强) -> 天津分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '天津分公司' AND d.dept_name = '财务部') WHERE user_id = 38;
-- tj_cw_user1 (姜伟) -> 天津分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '天津分公司' AND d.dept_name = '财务部') WHERE user_id = 39;
-- tj_cw_user2 (汪敏) -> 天津分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '天津分公司' AND d.dept_name = '财务部') WHERE user_id = 40;
-- tj_cw_user3 (唐磊) -> 天津分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '天津分公司' AND d.dept_name = '财务部') WHERE user_id = 41;
-- tj_yf_leader (吴鹏) -> 天津分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '天津分公司' AND d.dept_name = '研发部') WHERE user_id = 42;
-- tj_yf_user1 (付强) -> 天津分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '天津分公司' AND d.dept_name = '研发部') WHERE user_id = 43;
-- tj_yf_user2 (郭燕) -> 天津分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '天津分公司' AND d.dept_name = '研发部') WHERE user_id = 44;
-- tj_yf_user3 (董芳) -> 天津分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '天津分公司' AND d.dept_name = '研发部') WHERE user_id = 45;
-- tj_xz_leader (方鹏) -> 天津分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '天津分公司' AND d.dept_name = '行政部') WHERE user_id = 46;
-- tj_xz_user1 (金杰) -> 天津分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '天津分公司' AND d.dept_name = '行政部') WHERE user_id = 47;
-- tj_xz_user2 (严鹏) -> 天津分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '天津分公司' AND d.dept_name = '行政部') WHERE user_id = 48;
-- tj_xz_user3 (邓颖) -> 天津分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '天津分公司' AND d.dept_name = '行政部') WHERE user_id = 49;
-- sh_leader (贺鹏) -> 上海分公司
UPDATE sys_user SET dept_id = 1003 WHERE user_id = 50;
-- sh_bb_leader (田宇) -> 上海分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '上海分公司' AND d.dept_name = '上海分公司本部') WHERE user_id = 51;
-- sh_bb_user1 (闫鹏) -> 上海分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '上海分公司' AND d.dept_name = '上海分公司本部') WHERE user_id = 52;
-- sh_bb_user2 (高健) -> 上海分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '上海分公司' AND d.dept_name = '上海分公司本部') WHERE user_id = 53;
-- sh_js_leader (董强) -> 上海分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '上海分公司' AND d.dept_name = '技术部') WHERE user_id = 54;
-- sh_js_user1 (林鹏) -> 上海分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '上海分公司' AND d.dept_name = '技术部') WHERE user_id = 55;
-- sh_js_user2 (江涛) -> 上海分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '上海分公司' AND d.dept_name = '技术部') WHERE user_id = 56;
-- sh_xs_leader (赵伟) -> 上海分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '上海分公司' AND d.dept_name = '销售部') WHERE user_id = 57;
-- sh_xs_user1 (严鹏) -> 上海分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '上海分公司' AND d.dept_name = '销售部') WHERE user_id = 58;
-- sh_xs_user2 (沈浩) -> 上海分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '上海分公司' AND d.dept_name = '销售部') WHERE user_id = 59;
-- sh_xs_user3 (叶军) -> 上海分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '上海分公司' AND d.dept_name = '销售部') WHERE user_id = 60;
-- sh_cw_leader (龚浩) -> 上海分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '上海分公司' AND d.dept_name = '财务部') WHERE user_id = 61;
-- sh_cw_user1 (朱峰) -> 上海分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '上海分公司' AND d.dept_name = '财务部') WHERE user_id = 62;
-- sh_cw_user2 (樊丽) -> 上海分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '上海分公司' AND d.dept_name = '财务部') WHERE user_id = 63;
-- sh_cw_user3 (吴敏) -> 上海分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '上海分公司' AND d.dept_name = '财务部') WHERE user_id = 64;
-- sh_yf_leader (孟健) -> 上海分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '上海分公司' AND d.dept_name = '研发部') WHERE user_id = 65;
-- sh_yf_user1 (姚磊) -> 上海分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '上海分公司' AND d.dept_name = '研发部') WHERE user_id = 66;
-- sh_yf_user2 (沈浩) -> 上海分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '上海分公司' AND d.dept_name = '研发部') WHERE user_id = 67;
-- sh_xz_leader (郭涛) -> 上海分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '上海分公司' AND d.dept_name = '行政部') WHERE user_id = 68;
-- sh_xz_user1 (贾伟) -> 上海分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '上海分公司' AND d.dept_name = '行政部') WHERE user_id = 69;
-- sh_xz_user2 (黄涛) -> 上海分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '上海分公司' AND d.dept_name = '行政部') WHERE user_id = 70;
-- cq_leader (何强) -> 重庆分公司
UPDATE sys_user SET dept_id = 1004 WHERE user_id = 71;
-- cq_bb_leader (宋磊) -> 重庆分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '重庆分公司' AND d.dept_name = '重庆分公司本部') WHERE user_id = 72;
-- cq_bb_user1 (林鹏) -> 重庆分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '重庆分公司' AND d.dept_name = '重庆分公司本部') WHERE user_id = 73;
-- cq_bb_user2 (侯健) -> 重庆分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '重庆分公司' AND d.dept_name = '重庆分公司本部') WHERE user_id = 74;
-- cq_js_leader (丁杰) -> 重庆分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '重庆分公司' AND d.dept_name = '技术部') WHERE user_id = 75;
-- cq_js_user1 (万强) -> 重庆分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '重庆分公司' AND d.dept_name = '技术部') WHERE user_id = 76;
-- cq_js_user2 (沈浩) -> 重庆分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '重庆分公司' AND d.dept_name = '技术部') WHERE user_id = 77;
-- cq_js_user3 (夏明) -> 重庆分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '重庆分公司' AND d.dept_name = '技术部') WHERE user_id = 78;
-- cq_xs_leader (刘洋) -> 重庆分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '重庆分公司' AND d.dept_name = '销售部') WHERE user_id = 79;
-- cq_xs_user1 (孟芳) -> 重庆分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '重庆分公司' AND d.dept_name = '销售部') WHERE user_id = 80;
-- cq_xs_user2 (肖梅) -> 重庆分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '重庆分公司' AND d.dept_name = '销售部') WHERE user_id = 81;
-- cq_cw_leader (戴健) -> 重庆分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '重庆分公司' AND d.dept_name = '财务部') WHERE user_id = 82;
-- cq_cw_user1 (熊浩) -> 重庆分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '重庆分公司' AND d.dept_name = '财务部') WHERE user_id = 83;
-- cq_cw_user2 (伍芳) -> 重庆分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '重庆分公司' AND d.dept_name = '财务部') WHERE user_id = 84;
-- cq_cw_user3 (顾杰) -> 重庆分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '重庆分公司' AND d.dept_name = '财务部') WHERE user_id = 85;
-- cq_yf_leader (汪健) -> 重庆分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '重庆分公司' AND d.dept_name = '研发部') WHERE user_id = 86;
-- cq_yf_user1 (孔伟) -> 重庆分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '重庆分公司' AND d.dept_name = '研发部') WHERE user_id = 87;
-- cq_yf_user2 (陈磊) -> 重庆分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '重庆分公司' AND d.dept_name = '研发部') WHERE user_id = 88;
-- cq_yf_user3 (兰敏) -> 重庆分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '重庆分公司' AND d.dept_name = '研发部') WHERE user_id = 89;
-- cq_xz_leader (姜伟) -> 重庆分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '重庆分公司' AND d.dept_name = '行政部') WHERE user_id = 90;
-- cq_xz_user1 (陆强) -> 重庆分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '重庆分公司' AND d.dept_name = '行政部') WHERE user_id = 91;
-- cq_xz_user2 (王强) -> 重庆分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '重庆分公司' AND d.dept_name = '行政部') WHERE user_id = 92;
-- cq_xz_user3 (钟明) -> 重庆分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '重庆分公司' AND d.dept_name = '行政部') WHERE user_id = 93;
-- hebei_leader (武强) -> 河北分公司
UPDATE sys_user SET dept_id = 1005 WHERE user_id = 94;
-- hebei_bb_leader (沈浩) -> 河北分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河北分公司' AND d.dept_name = '河北分公司本部') WHERE user_id = 95;
-- hebei_bb_user1 (贾丽) -> 河北分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河北分公司' AND d.dept_name = '河北分公司本部') WHERE user_id = 96;
-- hebei_bb_user2 (齐婷) -> 河北分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河北分公司' AND d.dept_name = '河北分公司本部') WHERE user_id = 97;
-- hebei_bb_user3 (邹杰) -> 河北分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河北分公司' AND d.dept_name = '河北分公司本部') WHERE user_id = 98;
-- hebei_js_leader (程涛) -> 河北分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河北分公司' AND d.dept_name = '技术部') WHERE user_id = 99;
-- hebei_js_user1 (肖峰) -> 河北分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河北分公司' AND d.dept_name = '技术部') WHERE user_id = 100;
-- hebei_js_user2 (徐磊) -> 河北分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河北分公司' AND d.dept_name = '技术部') WHERE user_id = 101;
-- hebei_js_user3 (贺鹏) -> 河北分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河北分公司' AND d.dept_name = '技术部') WHERE user_id = 102;
-- hebei_xs_leader (闫鹏) -> 河北分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河北分公司' AND d.dept_name = '销售部') WHERE user_id = 103;
-- hebei_xs_user1 (韩雪) -> 河北分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河北分公司' AND d.dept_name = '销售部') WHERE user_id = 104;
-- hebei_xs_user2 (吴鹏) -> 河北分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河北分公司' AND d.dept_name = '销售部') WHERE user_id = 105;
-- hebei_cw_leader (邓健) -> 河北分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河北分公司' AND d.dept_name = '财务部') WHERE user_id = 106;
-- hebei_cw_user1 (顾杰) -> 河北分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河北分公司' AND d.dept_name = '财务部') WHERE user_id = 107;
-- hebei_cw_user2 (王芳) -> 河北分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河北分公司' AND d.dept_name = '财务部') WHERE user_id = 108;
-- hebei_yf_leader (陶强) -> 河北分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河北分公司' AND d.dept_name = '研发部') WHERE user_id = 109;
-- hebei_yf_user1 (龙浩) -> 河北分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河北分公司' AND d.dept_name = '研发部') WHERE user_id = 110;
-- hebei_yf_user2 (钱健) -> 河北分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河北分公司' AND d.dept_name = '研发部') WHERE user_id = 111;
-- hebei_yf_user3 (郭涛) -> 河北分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河北分公司' AND d.dept_name = '研发部') WHERE user_id = 112;
-- hebei_xz_leader (余明) -> 河北分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河北分公司' AND d.dept_name = '行政部') WHERE user_id = 113;
-- hebei_xz_user1 (曹阳) -> 河北分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河北分公司' AND d.dept_name = '行政部') WHERE user_id = 114;
-- hebei_xz_user2 (钱健) -> 河北分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河北分公司' AND d.dept_name = '行政部') WHERE user_id = 115;
-- shanxi_leader (白杰) -> 山西分公司
UPDATE sys_user SET dept_id = 1006 WHERE user_id = 116;
-- shanxi_bb_leader (孙浩) -> 山西分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山西分公司' AND d.dept_name = '山西分公司本部') WHERE user_id = 117;
-- shanxi_bb_user1 (陶强) -> 山西分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山西分公司' AND d.dept_name = '山西分公司本部') WHERE user_id = 118;
-- shanxi_bb_user2 (汤健) -> 山西分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山西分公司' AND d.dept_name = '山西分公司本部') WHERE user_id = 119;
-- shanxi_js_leader (汤健) -> 山西分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山西分公司' AND d.dept_name = '技术部') WHERE user_id = 120;
-- shanxi_js_user1 (汪敏) -> 山西分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山西分公司' AND d.dept_name = '技术部') WHERE user_id = 121;
-- shanxi_js_user2 (吕鹏) -> 山西分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山西分公司' AND d.dept_name = '技术部') WHERE user_id = 122;
-- shanxi_xs_leader (高健) -> 山西分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山西分公司' AND d.dept_name = '销售部') WHERE user_id = 123;
-- shanxi_xs_user1 (戴健) -> 山西分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山西分公司' AND d.dept_name = '销售部') WHERE user_id = 124;
-- shanxi_xs_user2 (尹强) -> 山西分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山西分公司' AND d.dept_name = '销售部') WHERE user_id = 125;
-- shanxi_cw_leader (莫浩) -> 山西分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山西分公司' AND d.dept_name = '财务部') WHERE user_id = 126;
-- shanxi_cw_user1 (余明) -> 山西分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山西分公司' AND d.dept_name = '财务部') WHERE user_id = 127;
-- shanxi_cw_user2 (刘洋) -> 山西分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山西分公司' AND d.dept_name = '财务部') WHERE user_id = 128;
-- shanxi_yf_leader (潘健) -> 山西分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山西分公司' AND d.dept_name = '研发部') WHERE user_id = 129;
-- shanxi_yf_user1 (陆强) -> 山西分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山西分公司' AND d.dept_name = '研发部') WHERE user_id = 130;
-- shanxi_yf_user2 (乐敏) -> 山西分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山西分公司' AND d.dept_name = '研发部') WHERE user_id = 131;
-- shanxi_yf_user3 (高敏) -> 山西分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山西分公司' AND d.dept_name = '研发部') WHERE user_id = 132;
-- shanxi_xz_leader (冯涛) -> 山西分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山西分公司' AND d.dept_name = '行政部') WHERE user_id = 133;
-- shanxi_xz_user1 (孙浩) -> 山西分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山西分公司' AND d.dept_name = '行政部') WHERE user_id = 134;
-- shanxi_xz_user2 (齐婷) -> 山西分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山西分公司' AND d.dept_name = '行政部') WHERE user_id = 135;
-- shanxi_xz_user3 (黄丽) -> 山西分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山西分公司' AND d.dept_name = '行政部') WHERE user_id = 136;
-- nm_leader (曾健) -> 内蒙古分公司
UPDATE sys_user SET dept_id = 1007 WHERE user_id = 137;
-- nm_bb_leader (谭浩) -> 内蒙古分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '内蒙古分公司' AND d.dept_name = '内蒙古分公司本部') WHERE user_id = 138;
-- nm_bb_user1 (史伟) -> 内蒙古分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '内蒙古分公司' AND d.dept_name = '内蒙古分公司本部') WHERE user_id = 139;
-- nm_bb_user2 (万强) -> 内蒙古分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '内蒙古分公司' AND d.dept_name = '内蒙古分公司本部') WHERE user_id = 140;
-- nm_js_leader (孙浩) -> 内蒙古分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '内蒙古分公司' AND d.dept_name = '技术部') WHERE user_id = 141;
-- nm_js_user1 (孙婷) -> 内蒙古分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '内蒙古分公司' AND d.dept_name = '技术部') WHERE user_id = 142;
-- nm_js_user2 (樊丽) -> 内蒙古分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '内蒙古分公司' AND d.dept_name = '技术部') WHERE user_id = 143;
-- nm_js_user3 (廖浩) -> 内蒙古分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '内蒙古分公司' AND d.dept_name = '技术部') WHERE user_id = 144;
-- nm_xs_leader (肖峰) -> 内蒙古分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '内蒙古分公司' AND d.dept_name = '销售部') WHERE user_id = 145;
-- nm_xs_user1 (薛健) -> 内蒙古分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '内蒙古分公司' AND d.dept_name = '销售部') WHERE user_id = 146;
-- nm_xs_user2 (范鹏) -> 内蒙古分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '内蒙古分公司' AND d.dept_name = '销售部') WHERE user_id = 147;
-- nm_cw_leader (彭涛) -> 内蒙古分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '内蒙古分公司' AND d.dept_name = '财务部') WHERE user_id = 148;
-- nm_cw_user1 (胡军) -> 内蒙古分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '内蒙古分公司' AND d.dept_name = '财务部') WHERE user_id = 149;
-- nm_cw_user2 (郑浩) -> 内蒙古分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '内蒙古分公司' AND d.dept_name = '财务部') WHERE user_id = 150;
-- nm_yf_leader (沈浩) -> 内蒙古分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '内蒙古分公司' AND d.dept_name = '研发部') WHERE user_id = 151;
-- nm_yf_user1 (樊丽) -> 内蒙古分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '内蒙古分公司' AND d.dept_name = '研发部') WHERE user_id = 152;
-- nm_yf_user2 (沈浩) -> 内蒙古分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '内蒙古分公司' AND d.dept_name = '研发部') WHERE user_id = 153;
-- nm_xz_leader (钟明) -> 内蒙古分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '内蒙古分公司' AND d.dept_name = '行政部') WHERE user_id = 154;
-- nm_xz_user1 (侯健) -> 内蒙古分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '内蒙古分公司' AND d.dept_name = '行政部') WHERE user_id = 155;
-- nm_xz_user2 (于洋) -> 内蒙古分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '内蒙古分公司' AND d.dept_name = '行政部') WHERE user_id = 156;
-- liaoning_leader (陆强) -> 辽宁分公司
UPDATE sys_user SET dept_id = 1008 WHERE user_id = 157;
-- liaoning_bb_leader (林鹏) -> 辽宁分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '辽宁分公司' AND d.dept_name = '辽宁分公司本部') WHERE user_id = 158;
-- liaoning_bb_user1 (汪健) -> 辽宁分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '辽宁分公司' AND d.dept_name = '辽宁分公司本部') WHERE user_id = 159;
-- liaoning_bb_user2 (毛强) -> 辽宁分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '辽宁分公司' AND d.dept_name = '辽宁分公司本部') WHERE user_id = 160;
-- liaoning_js_leader (向强) -> 辽宁分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '辽宁分公司' AND d.dept_name = '技术部') WHERE user_id = 161;
-- liaoning_js_user1 (董芳) -> 辽宁分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '辽宁分公司' AND d.dept_name = '技术部') WHERE user_id = 162;
-- liaoning_js_user2 (孔伟) -> 辽宁分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '辽宁分公司' AND d.dept_name = '技术部') WHERE user_id = 163;
-- liaoning_js_user3 (郭涛) -> 辽宁分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '辽宁分公司' AND d.dept_name = '技术部') WHERE user_id = 164;
-- liaoning_xs_leader (贾伟) -> 辽宁分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '辽宁分公司' AND d.dept_name = '销售部') WHERE user_id = 165;
-- liaoning_xs_user1 (董芳) -> 辽宁分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '辽宁分公司' AND d.dept_name = '销售部') WHERE user_id = 166;
-- liaoning_xs_user2 (元敏) -> 辽宁分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '辽宁分公司' AND d.dept_name = '销售部') WHERE user_id = 167;
-- liaoning_cw_leader (江涛) -> 辽宁分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '辽宁分公司' AND d.dept_name = '财务部') WHERE user_id = 168;
-- liaoning_cw_user1 (姜伟) -> 辽宁分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '辽宁分公司' AND d.dept_name = '财务部') WHERE user_id = 169;
-- liaoning_cw_user2 (杜峰) -> 辽宁分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '辽宁分公司' AND d.dept_name = '财务部') WHERE user_id = 170;
-- liaoning_cw_user3 (邹杰) -> 辽宁分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '辽宁分公司' AND d.dept_name = '财务部') WHERE user_id = 171;
-- liaoning_yf_leader (潘健) -> 辽宁分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '辽宁分公司' AND d.dept_name = '研发部') WHERE user_id = 172;
-- liaoning_yf_user1 (杜峰) -> 辽宁分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '辽宁分公司' AND d.dept_name = '研发部') WHERE user_id = 173;
-- liaoning_yf_user2 (韦强) -> 辽宁分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '辽宁分公司' AND d.dept_name = '研发部') WHERE user_id = 174;
-- liaoning_yf_user3 (万强) -> 辽宁分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '辽宁分公司' AND d.dept_name = '研发部') WHERE user_id = 175;
-- liaoning_xz_leader (叶军) -> 辽宁分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '辽宁分公司' AND d.dept_name = '行政部') WHERE user_id = 176;
-- liaoning_xz_user1 (沈浩) -> 辽宁分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '辽宁分公司' AND d.dept_name = '行政部') WHERE user_id = 177;
-- liaoning_xz_user2 (赵伟) -> 辽宁分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '辽宁分公司' AND d.dept_name = '行政部') WHERE user_id = 178;
-- jilin_leader (陈磊) -> 吉林分公司
UPDATE sys_user SET dept_id = 1009 WHERE user_id = 179;
-- jilin_bb_leader (魏强) -> 吉林分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '吉林分公司' AND d.dept_name = '吉林分公司本部') WHERE user_id = 180;
-- jilin_bb_user1 (顾杰) -> 吉林分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '吉林分公司' AND d.dept_name = '吉林分公司本部') WHERE user_id = 181;
-- jilin_bb_user2 (段杰) -> 吉林分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '吉林分公司' AND d.dept_name = '吉林分公司本部') WHERE user_id = 182;
-- jilin_js_leader (付健) -> 吉林分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '吉林分公司' AND d.dept_name = '技术部') WHERE user_id = 183;
-- jilin_js_user1 (殷婷) -> 吉林分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '吉林分公司' AND d.dept_name = '技术部') WHERE user_id = 184;
-- jilin_js_user2 (董芳) -> 吉林分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '吉林分公司' AND d.dept_name = '技术部') WHERE user_id = 185;
-- jilin_xs_leader (闫鹏) -> 吉林分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '吉林分公司' AND d.dept_name = '销售部') WHERE user_id = 186;
-- jilin_xs_user1 (毛强) -> 吉林分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '吉林分公司' AND d.dept_name = '销售部') WHERE user_id = 187;
-- jilin_xs_user2 (龙浩) -> 吉林分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '吉林分公司' AND d.dept_name = '销售部') WHERE user_id = 188;
-- jilin_xs_user3 (孙婷) -> 吉林分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '吉林分公司' AND d.dept_name = '销售部') WHERE user_id = 189;
-- jilin_cw_leader (林鹏) -> 吉林分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '吉林分公司' AND d.dept_name = '财务部') WHERE user_id = 190;
-- jilin_cw_user1 (夏明) -> 吉林分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '吉林分公司' AND d.dept_name = '财务部') WHERE user_id = 191;
-- jilin_cw_user2 (贺鹏) -> 吉林分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '吉林分公司' AND d.dept_name = '财务部') WHERE user_id = 192;
-- jilin_yf_leader (李军) -> 吉林分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '吉林分公司' AND d.dept_name = '研发部') WHERE user_id = 193;
-- jilin_yf_user1 (覃杰) -> 吉林分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '吉林分公司' AND d.dept_name = '研发部') WHERE user_id = 194;
-- jilin_yf_user2 (陆强) -> 吉林分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '吉林分公司' AND d.dept_name = '研发部') WHERE user_id = 195;
-- jilin_xz_leader (侯健) -> 吉林分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '吉林分公司' AND d.dept_name = '行政部') WHERE user_id = 196;
-- jilin_xz_user1 (付强) -> 吉林分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '吉林分公司' AND d.dept_name = '行政部') WHERE user_id = 197;
-- jilin_xz_user2 (贾丽) -> 吉林分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '吉林分公司' AND d.dept_name = '行政部') WHERE user_id = 198;
-- heilongjiang_leader (郭涛) -> 黑龙江分公司
UPDATE sys_user SET dept_id = 1010 WHERE user_id = 199;
-- heilongjiang_bb_leader (谢峰) -> 黑龙江分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '黑龙江分公司' AND d.dept_name = '黑龙江分公司本部') WHERE user_id = 200;
-- heilongjiang_bb_user1 (卢婷) -> 黑龙江分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '黑龙江分公司' AND d.dept_name = '黑龙江分公司本部') WHERE user_id = 201;
-- heilongjiang_bb_user2 (谭燕) -> 黑龙江分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '黑龙江分公司' AND d.dept_name = '黑龙江分公司本部') WHERE user_id = 202;
-- heilongjiang_bb_user3 (吕鹏) -> 黑龙江分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '黑龙江分公司' AND d.dept_name = '黑龙江分公司本部') WHERE user_id = 203;
-- heilongjiang_js_leader (庞鹏) -> 黑龙江分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '黑龙江分公司' AND d.dept_name = '技术部') WHERE user_id = 204;
-- heilongjiang_js_user1 (曾健) -> 黑龙江分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '黑龙江分公司' AND d.dept_name = '技术部') WHERE user_id = 205;
-- heilongjiang_js_user2 (田宇) -> 黑龙江分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '黑龙江分公司' AND d.dept_name = '技术部') WHERE user_id = 206;
-- heilongjiang_js_user3 (郭燕) -> 黑龙江分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '黑龙江分公司' AND d.dept_name = '技术部') WHERE user_id = 207;
-- heilongjiang_xs_leader (付健) -> 黑龙江分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '黑龙江分公司' AND d.dept_name = '销售部') WHERE user_id = 208;
-- heilongjiang_xs_user1 (于洋) -> 黑龙江分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '黑龙江分公司' AND d.dept_name = '销售部') WHERE user_id = 209;
-- heilongjiang_xs_user2 (闫鹏) -> 黑龙江分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '黑龙江分公司' AND d.dept_name = '销售部') WHERE user_id = 210;
-- heilongjiang_cw_leader (江涛) -> 黑龙江分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '黑龙江分公司' AND d.dept_name = '财务部') WHERE user_id = 211;
-- heilongjiang_cw_user1 (万强) -> 黑龙江分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '黑龙江分公司' AND d.dept_name = '财务部') WHERE user_id = 212;
-- heilongjiang_cw_user2 (邹强) -> 黑龙江分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '黑龙江分公司' AND d.dept_name = '财务部') WHERE user_id = 213;
-- heilongjiang_yf_leader (魏强) -> 黑龙江分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '黑龙江分公司' AND d.dept_name = '研发部') WHERE user_id = 214;
-- heilongjiang_yf_user1 (孔伟) -> 黑龙江分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '黑龙江分公司' AND d.dept_name = '研发部') WHERE user_id = 215;
-- heilongjiang_yf_user2 (方鹏) -> 黑龙江分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '黑龙江分公司' AND d.dept_name = '研发部') WHERE user_id = 216;
-- heilongjiang_yf_user3 (贾丽) -> 黑龙江分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '黑龙江分公司' AND d.dept_name = '研发部') WHERE user_id = 217;
-- heilongjiang_xz_leader (龚浩) -> 黑龙江分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '黑龙江分公司' AND d.dept_name = '行政部') WHERE user_id = 218;
-- heilongjiang_xz_user1 (韩雪) -> 黑龙江分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '黑龙江分公司' AND d.dept_name = '行政部') WHERE user_id = 219;
-- heilongjiang_xz_user2 (崔强) -> 黑龙江分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '黑龙江分公司' AND d.dept_name = '行政部') WHERE user_id = 220;
-- jiangsu_leader (陶强) -> 江苏分公司
UPDATE sys_user SET dept_id = 1011 WHERE user_id = 221;
-- jiangsu_bb_leader (徐磊) -> 江苏分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江苏分公司' AND d.dept_name = '江苏分公司本部') WHERE user_id = 222;
-- jiangsu_bb_user1 (杨明) -> 江苏分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江苏分公司' AND d.dept_name = '江苏分公司本部') WHERE user_id = 223;
-- jiangsu_bb_user2 (卢健) -> 江苏分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江苏分公司' AND d.dept_name = '江苏分公司本部') WHERE user_id = 224;
-- jiangsu_bb_user3 (宋磊) -> 江苏分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江苏分公司' AND d.dept_name = '江苏分公司本部') WHERE user_id = 225;
-- jiangsu_js_leader (彭涛) -> 江苏分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江苏分公司' AND d.dept_name = '技术部') WHERE user_id = 226;
-- jiangsu_js_user1 (严鹏) -> 江苏分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江苏分公司' AND d.dept_name = '技术部') WHERE user_id = 227;
-- jiangsu_js_user2 (朱峰) -> 江苏分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江苏分公司' AND d.dept_name = '技术部') WHERE user_id = 228;
-- jiangsu_js_user3 (万强) -> 江苏分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江苏分公司' AND d.dept_name = '技术部') WHERE user_id = 229;
-- jiangsu_xs_leader (闫鹏) -> 江苏分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江苏分公司' AND d.dept_name = '销售部') WHERE user_id = 230;
-- jiangsu_xs_user1 (徐磊) -> 江苏分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江苏分公司' AND d.dept_name = '销售部') WHERE user_id = 231;
-- jiangsu_xs_user2 (石强) -> 江苏分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江苏分公司' AND d.dept_name = '销售部') WHERE user_id = 232;
-- jiangsu_cw_leader (孙浩) -> 江苏分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江苏分公司' AND d.dept_name = '财务部') WHERE user_id = 233;
-- jiangsu_cw_user1 (莫浩) -> 江苏分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江苏分公司' AND d.dept_name = '财务部') WHERE user_id = 234;
-- jiangsu_cw_user2 (肖梅) -> 江苏分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江苏分公司' AND d.dept_name = '财务部') WHERE user_id = 235;
-- jiangsu_cw_user3 (钟明) -> 江苏分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江苏分公司' AND d.dept_name = '财务部') WHERE user_id = 236;
-- jiangsu_yf_leader (武强) -> 江苏分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江苏分公司' AND d.dept_name = '研发部') WHERE user_id = 237;
-- jiangsu_yf_user1 (夏明) -> 江苏分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江苏分公司' AND d.dept_name = '研发部') WHERE user_id = 238;
-- jiangsu_yf_user2 (郭燕) -> 江苏分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江苏分公司' AND d.dept_name = '研发部') WHERE user_id = 239;
-- jiangsu_yf_user3 (毛强) -> 江苏分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江苏分公司' AND d.dept_name = '研发部') WHERE user_id = 240;
-- jiangsu_xz_leader (向强) -> 江苏分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江苏分公司' AND d.dept_name = '行政部') WHERE user_id = 241;
-- jiangsu_xz_user1 (杨明) -> 江苏分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江苏分公司' AND d.dept_name = '行政部') WHERE user_id = 242;
-- jiangsu_xz_user2 (龚浩) -> 江苏分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江苏分公司' AND d.dept_name = '行政部') WHERE user_id = 243;
-- jiangsu_xz_user3 (龙浩) -> 江苏分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江苏分公司' AND d.dept_name = '行政部') WHERE user_id = 244;
-- zhejiang_leader (于洋) -> 浙江分公司
UPDATE sys_user SET dept_id = 1012 WHERE user_id = 245;
-- zhejiang_bb_leader (江涛) -> 浙江分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '浙江分公司' AND d.dept_name = '浙江分公司本部') WHERE user_id = 246;
-- zhejiang_bb_user1 (江涛) -> 浙江分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '浙江分公司' AND d.dept_name = '浙江分公司本部') WHERE user_id = 247;
-- zhejiang_bb_user2 (姚磊) -> 浙江分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '浙江分公司' AND d.dept_name = '浙江分公司本部') WHERE user_id = 248;
-- zhejiang_bb_user3 (李军) -> 浙江分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '浙江分公司' AND d.dept_name = '浙江分公司本部') WHERE user_id = 249;
-- zhejiang_js_leader (付健) -> 浙江分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '浙江分公司' AND d.dept_name = '技术部') WHERE user_id = 250;
-- zhejiang_js_user1 (钱健) -> 浙江分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '浙江分公司' AND d.dept_name = '技术部') WHERE user_id = 251;
-- zhejiang_js_user2 (王强) -> 浙江分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '浙江分公司' AND d.dept_name = '技术部') WHERE user_id = 252;
-- zhejiang_js_user3 (毕芳) -> 浙江分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '浙江分公司' AND d.dept_name = '技术部') WHERE user_id = 253;
-- zhejiang_xs_leader (方鹏) -> 浙江分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '浙江分公司' AND d.dept_name = '销售部') WHERE user_id = 254;
-- zhejiang_xs_user1 (江涛) -> 浙江分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '浙江分公司' AND d.dept_name = '销售部') WHERE user_id = 255;
-- zhejiang_xs_user2 (陈静) -> 浙江分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '浙江分公司' AND d.dept_name = '销售部') WHERE user_id = 256;
-- zhejiang_cw_leader (孙浩) -> 浙江分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '浙江分公司' AND d.dept_name = '财务部') WHERE user_id = 257;
-- zhejiang_cw_user1 (蔡静) -> 浙江分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '浙江分公司' AND d.dept_name = '财务部') WHERE user_id = 258;
-- zhejiang_cw_user2 (廖浩) -> 浙江分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '浙江分公司' AND d.dept_name = '财务部') WHERE user_id = 259;
-- zhejiang_cw_user3 (蔡磊) -> 浙江分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '浙江分公司' AND d.dept_name = '财务部') WHERE user_id = 260;
-- zhejiang_yf_leader (蔡磊) -> 浙江分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '浙江分公司' AND d.dept_name = '研发部') WHERE user_id = 261;
-- zhejiang_yf_user1 (兰敏) -> 浙江分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '浙江分公司' AND d.dept_name = '研发部') WHERE user_id = 262;
-- zhejiang_yf_user2 (邵伟) -> 浙江分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '浙江分公司' AND d.dept_name = '研发部') WHERE user_id = 263;
-- zhejiang_yf_user3 (夏明) -> 浙江分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '浙江分公司' AND d.dept_name = '研发部') WHERE user_id = 264;
-- zhejiang_xz_leader (孟健) -> 浙江分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '浙江分公司' AND d.dept_name = '行政部') WHERE user_id = 265;
-- zhejiang_xz_user1 (任芳) -> 浙江分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '浙江分公司' AND d.dept_name = '行政部') WHERE user_id = 266;
-- zhejiang_xz_user2 (付健) -> 浙江分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '浙江分公司' AND d.dept_name = '行政部') WHERE user_id = 267;
-- zhejiang_xz_user3 (钟明) -> 浙江分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '浙江分公司' AND d.dept_name = '行政部') WHERE user_id = 268;
-- anhui_leader (潘健) -> 安徽分公司
UPDATE sys_user SET dept_id = 1013 WHERE user_id = 269;
-- anhui_bb_leader (魏强) -> 安徽分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '安徽分公司' AND d.dept_name = '安徽分公司本部') WHERE user_id = 270;
-- anhui_bb_user1 (谢峰) -> 安徽分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '安徽分公司' AND d.dept_name = '安徽分公司本部') WHERE user_id = 271;
-- anhui_bb_user2 (余明) -> 安徽分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '安徽分公司' AND d.dept_name = '安徽分公司本部') WHERE user_id = 272;
-- anhui_bb_user3 (姜伟) -> 安徽分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '安徽分公司' AND d.dept_name = '安徽分公司本部') WHERE user_id = 273;
-- anhui_js_leader (潘健) -> 安徽分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '安徽分公司' AND d.dept_name = '技术部') WHERE user_id = 274;
-- anhui_js_user1 (汪健) -> 安徽分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '安徽分公司' AND d.dept_name = '技术部') WHERE user_id = 275;
-- anhui_js_user2 (汪健) -> 安徽分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '安徽分公司' AND d.dept_name = '技术部') WHERE user_id = 276;
-- anhui_xs_leader (钱健) -> 安徽分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '安徽分公司' AND d.dept_name = '销售部') WHERE user_id = 277;
-- anhui_xs_user1 (伍芳) -> 安徽分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '安徽分公司' AND d.dept_name = '销售部') WHERE user_id = 278;
-- anhui_xs_user2 (谭浩) -> 安徽分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '安徽分公司' AND d.dept_name = '销售部') WHERE user_id = 279;
-- anhui_cw_leader (姚磊) -> 安徽分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '安徽分公司' AND d.dept_name = '财务部') WHERE user_id = 280;
-- anhui_cw_user1 (叶军) -> 安徽分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '安徽分公司' AND d.dept_name = '财务部') WHERE user_id = 281;
-- anhui_cw_user2 (余明) -> 安徽分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '安徽分公司' AND d.dept_name = '财务部') WHERE user_id = 282;
-- anhui_cw_user3 (杜峰) -> 安徽分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '安徽分公司' AND d.dept_name = '财务部') WHERE user_id = 283;
-- anhui_yf_leader (闫鹏) -> 安徽分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '安徽分公司' AND d.dept_name = '研发部') WHERE user_id = 284;
-- anhui_yf_user1 (邵伟) -> 安徽分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '安徽分公司' AND d.dept_name = '研发部') WHERE user_id = 285;
-- anhui_yf_user2 (魏强) -> 安徽分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '安徽分公司' AND d.dept_name = '研发部') WHERE user_id = 286;
-- anhui_xz_leader (马超) -> 安徽分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '安徽分公司' AND d.dept_name = '行政部') WHERE user_id = 287;
-- anhui_xz_user1 (姚磊) -> 安徽分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '安徽分公司' AND d.dept_name = '行政部') WHERE user_id = 288;
-- anhui_xz_user2 (戴健) -> 安徽分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '安徽分公司' AND d.dept_name = '行政部') WHERE user_id = 289;
-- fujian_leader (尹强) -> 福建分公司
UPDATE sys_user SET dept_id = 1014 WHERE user_id = 290;
-- fujian_bb_leader (程涛) -> 福建分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '福建分公司' AND d.dept_name = '福建分公司本部') WHERE user_id = 291;
-- fujian_bb_user1 (黄丽) -> 福建分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '福建分公司' AND d.dept_name = '福建分公司本部') WHERE user_id = 292;
-- fujian_bb_user2 (苏敏) -> 福建分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '福建分公司' AND d.dept_name = '福建分公司本部') WHERE user_id = 293;
-- fujian_js_leader (金杰) -> 福建分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '福建分公司' AND d.dept_name = '技术部') WHERE user_id = 294;
-- fujian_js_user1 (卢婷) -> 福建分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '福建分公司' AND d.dept_name = '技术部') WHERE user_id = 295;
-- fujian_js_user2 (钱健) -> 福建分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '福建分公司' AND d.dept_name = '技术部') WHERE user_id = 296;
-- fujian_xs_leader (石强) -> 福建分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '福建分公司' AND d.dept_name = '销售部') WHERE user_id = 297;
-- fujian_xs_user1 (廖浩) -> 福建分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '福建分公司' AND d.dept_name = '销售部') WHERE user_id = 298;
-- fujian_xs_user2 (尹强) -> 福建分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '福建分公司' AND d.dept_name = '销售部') WHERE user_id = 299;
-- fujian_cw_leader (秦伟) -> 福建分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '福建分公司' AND d.dept_name = '财务部') WHERE user_id = 300;
-- fujian_cw_user1 (石强) -> 福建分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '福建分公司' AND d.dept_name = '财务部') WHERE user_id = 301;
-- fujian_cw_user2 (吴敏) -> 福建分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '福建分公司' AND d.dept_name = '财务部') WHERE user_id = 302;
-- fujian_yf_leader (庞鹏) -> 福建分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '福建分公司' AND d.dept_name = '研发部') WHERE user_id = 303;
-- fujian_yf_user1 (潘健) -> 福建分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '福建分公司' AND d.dept_name = '研发部') WHERE user_id = 304;
-- fujian_yf_user2 (莫浩) -> 福建分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '福建分公司' AND d.dept_name = '研发部') WHERE user_id = 305;
-- fujian_xz_leader (姜伟) -> 福建分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '福建分公司' AND d.dept_name = '行政部') WHERE user_id = 306;
-- fujian_xz_user1 (陈磊) -> 福建分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '福建分公司' AND d.dept_name = '行政部') WHERE user_id = 307;
-- fujian_xz_user2 (于洋) -> 福建分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '福建分公司' AND d.dept_name = '行政部') WHERE user_id = 308;
-- fujian_xz_user3 (邱明) -> 福建分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '福建分公司' AND d.dept_name = '行政部') WHERE user_id = 309;
-- jiangxi_leader (朱峰) -> 江西分公司
UPDATE sys_user SET dept_id = 1015 WHERE user_id = 310;
-- jiangxi_bb_leader (谢峰) -> 江西分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江西分公司' AND d.dept_name = '江西分公司本部') WHERE user_id = 311;
-- jiangxi_bb_user1 (黄涛) -> 江西分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江西分公司' AND d.dept_name = '江西分公司本部') WHERE user_id = 312;
-- jiangxi_bb_user2 (卢婷) -> 江西分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江西分公司' AND d.dept_name = '江西分公司本部') WHERE user_id = 313;
-- jiangxi_bb_user3 (史伟) -> 江西分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江西分公司' AND d.dept_name = '江西分公司本部') WHERE user_id = 314;
-- jiangxi_js_leader (赵伟) -> 江西分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江西分公司' AND d.dept_name = '技术部') WHERE user_id = 315;
-- jiangxi_js_user1 (闫鹏) -> 江西分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江西分公司' AND d.dept_name = '技术部') WHERE user_id = 316;
-- jiangxi_js_user2 (侯健) -> 江西分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江西分公司' AND d.dept_name = '技术部') WHERE user_id = 317;
-- jiangxi_xs_leader (龚浩) -> 江西分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江西分公司' AND d.dept_name = '销售部') WHERE user_id = 318;
-- jiangxi_xs_user1 (安婷) -> 江西分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江西分公司' AND d.dept_name = '销售部') WHERE user_id = 319;
-- jiangxi_xs_user2 (贾丽) -> 江西分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江西分公司' AND d.dept_name = '销售部') WHERE user_id = 320;
-- jiangxi_xs_user3 (郝健) -> 江西分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江西分公司' AND d.dept_name = '销售部') WHERE user_id = 321;
-- jiangxi_cw_leader (邓健) -> 江西分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江西分公司' AND d.dept_name = '财务部') WHERE user_id = 322;
-- jiangxi_cw_user1 (白杰) -> 江西分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江西分公司' AND d.dept_name = '财务部') WHERE user_id = 323;
-- jiangxi_cw_user2 (兰敏) -> 江西分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江西分公司' AND d.dept_name = '财务部') WHERE user_id = 324;
-- jiangxi_yf_leader (邓健) -> 江西分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江西分公司' AND d.dept_name = '研发部') WHERE user_id = 325;
-- jiangxi_yf_user1 (冯涛) -> 江西分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江西分公司' AND d.dept_name = '研发部') WHERE user_id = 326;
-- jiangxi_yf_user2 (秦伟) -> 江西分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江西分公司' AND d.dept_name = '研发部') WHERE user_id = 327;
-- jiangxi_yf_user3 (吴鹏) -> 江西分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江西分公司' AND d.dept_name = '研发部') WHERE user_id = 328;
-- jiangxi_xz_leader (顾杰) -> 江西分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江西分公司' AND d.dept_name = '行政部') WHERE user_id = 329;
-- jiangxi_xz_user1 (雷强) -> 江西分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江西分公司' AND d.dept_name = '行政部') WHERE user_id = 330;
-- jiangxi_xz_user2 (史伟) -> 江西分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江西分公司' AND d.dept_name = '行政部') WHERE user_id = 331;
-- jiangxi_xz_user3 (程丽) -> 江西分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '江西分公司' AND d.dept_name = '行政部') WHERE user_id = 332;
-- shandong_leader (江涛) -> 山东分公司
UPDATE sys_user SET dept_id = 1016 WHERE user_id = 333;
-- shandong_bb_leader (陆强) -> 山东分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山东分公司' AND d.dept_name = '山东分公司本部') WHERE user_id = 334;
-- shandong_bb_user1 (齐婷) -> 山东分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山东分公司' AND d.dept_name = '山东分公司本部') WHERE user_id = 335;
-- shandong_bb_user2 (孔伟) -> 山东分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山东分公司' AND d.dept_name = '山东分公司本部') WHERE user_id = 336;
-- shandong_bb_user3 (庞鹏) -> 山东分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山东分公司' AND d.dept_name = '山东分公司本部') WHERE user_id = 337;
-- shandong_js_leader (段杰) -> 山东分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山东分公司' AND d.dept_name = '技术部') WHERE user_id = 338;
-- shandong_js_user1 (姜伟) -> 山东分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山东分公司' AND d.dept_name = '技术部') WHERE user_id = 339;
-- shandong_js_user2 (段杰) -> 山东分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山东分公司' AND d.dept_name = '技术部') WHERE user_id = 340;
-- shandong_xs_leader (薛健) -> 山东分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山东分公司' AND d.dept_name = '销售部') WHERE user_id = 341;
-- shandong_xs_user1 (廖浩) -> 山东分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山东分公司' AND d.dept_name = '销售部') WHERE user_id = 342;
-- shandong_xs_user2 (韦强) -> 山东分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山东分公司' AND d.dept_name = '销售部') WHERE user_id = 343;
-- shandong_xs_user3 (薛健) -> 山东分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山东分公司' AND d.dept_name = '销售部') WHERE user_id = 344;
-- shandong_cw_leader (叶军) -> 山东分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山东分公司' AND d.dept_name = '财务部') WHERE user_id = 345;
-- shandong_cw_user1 (吕鹏) -> 山东分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山东分公司' AND d.dept_name = '财务部') WHERE user_id = 346;
-- shandong_cw_user2 (汪健) -> 山东分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山东分公司' AND d.dept_name = '财务部') WHERE user_id = 347;
-- shandong_yf_leader (严鹏) -> 山东分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山东分公司' AND d.dept_name = '研发部') WHERE user_id = 348;
-- shandong_yf_user1 (谭浩) -> 山东分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山东分公司' AND d.dept_name = '研发部') WHERE user_id = 349;
-- shandong_yf_user2 (韦强) -> 山东分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山东分公司' AND d.dept_name = '研发部') WHERE user_id = 350;
-- shandong_yf_user3 (兰敏) -> 山东分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山东分公司' AND d.dept_name = '研发部') WHERE user_id = 351;
-- shandong_xz_leader (程涛) -> 山东分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山东分公司' AND d.dept_name = '行政部') WHERE user_id = 352;
-- shandong_xz_user1 (彭丽) -> 山东分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山东分公司' AND d.dept_name = '行政部') WHERE user_id = 353;
-- shandong_xz_user2 (江涛) -> 山东分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '山东分公司' AND d.dept_name = '行政部') WHERE user_id = 354;
-- henan_leader (郑浩) -> 河南分公司
UPDATE sys_user SET dept_id = 1017 WHERE user_id = 355;
-- henan_bb_leader (熊浩) -> 河南分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河南分公司' AND d.dept_name = '河南分公司本部') WHERE user_id = 356;
-- henan_bb_user1 (沈浩) -> 河南分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河南分公司' AND d.dept_name = '河南分公司本部') WHERE user_id = 357;
-- henan_bb_user2 (孙浩) -> 河南分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河南分公司' AND d.dept_name = '河南分公司本部') WHERE user_id = 358;
-- henan_js_leader (邓健) -> 河南分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河南分公司' AND d.dept_name = '技术部') WHERE user_id = 359;
-- henan_js_user1 (郝健) -> 河南分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河南分公司' AND d.dept_name = '技术部') WHERE user_id = 360;
-- henan_js_user2 (叶军) -> 河南分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河南分公司' AND d.dept_name = '技术部') WHERE user_id = 361;
-- henan_js_user3 (付强) -> 河南分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河南分公司' AND d.dept_name = '技术部') WHERE user_id = 362;
-- henan_xs_leader (武强) -> 河南分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河南分公司' AND d.dept_name = '销售部') WHERE user_id = 363;
-- henan_xs_user1 (韦燕) -> 河南分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河南分公司' AND d.dept_name = '销售部') WHERE user_id = 364;
-- henan_xs_user2 (陆强) -> 河南分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河南分公司' AND d.dept_name = '销售部') WHERE user_id = 365;
-- henan_xs_user3 (韦强) -> 河南分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河南分公司' AND d.dept_name = '销售部') WHERE user_id = 366;
-- henan_cw_leader (林鹏) -> 河南分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河南分公司' AND d.dept_name = '财务部') WHERE user_id = 367;
-- henan_cw_user1 (贺鹏) -> 河南分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河南分公司' AND d.dept_name = '财务部') WHERE user_id = 368;
-- henan_cw_user2 (林鹏) -> 河南分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河南分公司' AND d.dept_name = '财务部') WHERE user_id = 369;
-- henan_yf_leader (范鹏) -> 河南分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河南分公司' AND d.dept_name = '研发部') WHERE user_id = 370;
-- henan_yf_user1 (万强) -> 河南分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河南分公司' AND d.dept_name = '研发部') WHERE user_id = 371;
-- henan_yf_user2 (伍芳) -> 河南分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河南分公司' AND d.dept_name = '研发部') WHERE user_id = 372;
-- henan_xz_leader (蒋超) -> 河南分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河南分公司' AND d.dept_name = '行政部') WHERE user_id = 373;
-- henan_xz_user1 (毛强) -> 河南分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河南分公司' AND d.dept_name = '行政部') WHERE user_id = 374;
-- henan_xz_user2 (元敏) -> 河南分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河南分公司' AND d.dept_name = '行政部') WHERE user_id = 375;
-- henan_xz_user3 (姚磊) -> 河南分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '河南分公司' AND d.dept_name = '行政部') WHERE user_id = 376;
-- hubei_leader (邵伟) -> 湖北分公司
UPDATE sys_user SET dept_id = 1018 WHERE user_id = 377;
-- hubei_bb_leader (曹阳) -> 湖北分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖北分公司' AND d.dept_name = '湖北分公司本部') WHERE user_id = 378;
-- hubei_bb_user1 (石强) -> 湖北分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖北分公司' AND d.dept_name = '湖北分公司本部') WHERE user_id = 379;
-- hubei_bb_user2 (贺鹏) -> 湖北分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖北分公司' AND d.dept_name = '湖北分公司本部') WHERE user_id = 380;
-- hubei_bb_user3 (孔伟) -> 湖北分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖北分公司' AND d.dept_name = '湖北分公司本部') WHERE user_id = 381;
-- hubei_js_leader (吕鹏) -> 湖北分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖北分公司' AND d.dept_name = '技术部') WHERE user_id = 382;
-- hubei_js_user1 (谭燕) -> 湖北分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖北分公司' AND d.dept_name = '技术部') WHERE user_id = 383;
-- hubei_js_user2 (向强) -> 湖北分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖北分公司' AND d.dept_name = '技术部') WHERE user_id = 384;
-- hubei_js_user3 (谢峰) -> 湖北分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖北分公司' AND d.dept_name = '技术部') WHERE user_id = 385;
-- hubei_xs_leader (彭涛) -> 湖北分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖北分公司' AND d.dept_name = '销售部') WHERE user_id = 386;
-- hubei_xs_user1 (肖峰) -> 湖北分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖北分公司' AND d.dept_name = '销售部') WHERE user_id = 387;
-- hubei_xs_user2 (覃杰) -> 湖北分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖北分公司' AND d.dept_name = '销售部') WHERE user_id = 388;
-- hubei_xs_user3 (毕芳) -> 湖北分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖北分公司' AND d.dept_name = '销售部') WHERE user_id = 389;
-- hubei_cw_leader (孟健) -> 湖北分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖北分公司' AND d.dept_name = '财务部') WHERE user_id = 390;
-- hubei_cw_user1 (邓颖) -> 湖北分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖北分公司' AND d.dept_name = '财务部') WHERE user_id = 391;
-- hubei_cw_user2 (于洋) -> 湖北分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖北分公司' AND d.dept_name = '财务部') WHERE user_id = 392;
-- hubei_yf_leader (薛健) -> 湖北分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖北分公司' AND d.dept_name = '研发部') WHERE user_id = 393;
-- hubei_yf_user1 (钱健) -> 湖北分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖北分公司' AND d.dept_name = '研发部') WHERE user_id = 394;
-- hubei_yf_user2 (薛健) -> 湖北分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖北分公司' AND d.dept_name = '研发部') WHERE user_id = 395;
-- hubei_yf_user3 (叶军) -> 湖北分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖北分公司' AND d.dept_name = '研发部') WHERE user_id = 396;
-- hubei_xz_leader (汪健) -> 湖北分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖北分公司' AND d.dept_name = '行政部') WHERE user_id = 397;
-- hubei_xz_user1 (罗浩) -> 湖北分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖北分公司' AND d.dept_name = '行政部') WHERE user_id = 398;
-- hubei_xz_user2 (孔伟) -> 湖北分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖北分公司' AND d.dept_name = '行政部') WHERE user_id = 399;
-- hunan_leader (姜伟) -> 湖南分公司
UPDATE sys_user SET dept_id = 1019 WHERE user_id = 400;
-- hunan_bb_leader (陈磊) -> 湖南分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖南分公司' AND d.dept_name = '湖南分公司本部') WHERE user_id = 401;
-- hunan_bb_user1 (樊丽) -> 湖南分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖南分公司' AND d.dept_name = '湖南分公司本部') WHERE user_id = 402;
-- hunan_bb_user2 (高敏) -> 湖南分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖南分公司' AND d.dept_name = '湖南分公司本部') WHERE user_id = 403;
-- hunan_js_leader (金杰) -> 湖南分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖南分公司' AND d.dept_name = '技术部') WHERE user_id = 404;
-- hunan_js_user1 (罗娟) -> 湖南分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖南分公司' AND d.dept_name = '技术部') WHERE user_id = 405;
-- hunan_js_user2 (郭燕) -> 湖南分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖南分公司' AND d.dept_name = '技术部') WHERE user_id = 406;
-- hunan_xs_leader (潘健) -> 湖南分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖南分公司' AND d.dept_name = '销售部') WHERE user_id = 407;
-- hunan_xs_user1 (雷强) -> 湖南分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖南分公司' AND d.dept_name = '销售部') WHERE user_id = 408;
-- hunan_xs_user2 (兰敏) -> 湖南分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖南分公司' AND d.dept_name = '销售部') WHERE user_id = 409;
-- hunan_cw_leader (顾杰) -> 湖南分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖南分公司' AND d.dept_name = '财务部') WHERE user_id = 410;
-- hunan_cw_user1 (戴健) -> 湖南分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖南分公司' AND d.dept_name = '财务部') WHERE user_id = 411;
-- hunan_cw_user2 (叶军) -> 湖南分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖南分公司' AND d.dept_name = '财务部') WHERE user_id = 412;
-- hunan_cw_user3 (崔丽) -> 湖南分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖南分公司' AND d.dept_name = '财务部') WHERE user_id = 413;
-- hunan_yf_leader (汤健) -> 湖南分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖南分公司' AND d.dept_name = '研发部') WHERE user_id = 414;
-- hunan_yf_user1 (韦燕) -> 湖南分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖南分公司' AND d.dept_name = '研发部') WHERE user_id = 415;
-- hunan_yf_user2 (董芳) -> 湖南分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖南分公司' AND d.dept_name = '研发部') WHERE user_id = 416;
-- hunan_yf_user3 (严鹏) -> 湖南分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖南分公司' AND d.dept_name = '研发部') WHERE user_id = 417;
-- hunan_xz_leader (钟明) -> 湖南分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖南分公司' AND d.dept_name = '行政部') WHERE user_id = 418;
-- hunan_xz_user1 (付强) -> 湖南分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖南分公司' AND d.dept_name = '行政部') WHERE user_id = 419;
-- hunan_xz_user2 (韩强) -> 湖南分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖南分公司' AND d.dept_name = '行政部') WHERE user_id = 420;
-- hunan_xz_user3 (常丽) -> 湖南分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '湖南分公司' AND d.dept_name = '行政部') WHERE user_id = 421;
-- gd_leader (孔伟) -> 广东分公司
UPDATE sys_user SET dept_id = 1020 WHERE user_id = 422;
-- gd_bb_leader (段杰) -> 广东分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广东分公司' AND d.dept_name = '广东分公司本部') WHERE user_id = 423;
-- gd_bb_user1 (闫鹏) -> 广东分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广东分公司' AND d.dept_name = '广东分公司本部') WHERE user_id = 424;
-- gd_bb_user2 (董芳) -> 广东分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广东分公司' AND d.dept_name = '广东分公司本部') WHERE user_id = 425;
-- gd_bb_user3 (袁伟) -> 广东分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广东分公司' AND d.dept_name = '广东分公司本部') WHERE user_id = 426;
-- gd_js_leader (龚浩) -> 广东分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广东分公司' AND d.dept_name = '技术部') WHERE user_id = 427;
-- gd_js_user1 (汪健) -> 广东分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广东分公司' AND d.dept_name = '技术部') WHERE user_id = 428;
-- gd_js_user2 (白杰) -> 广东分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广东分公司' AND d.dept_name = '技术部') WHERE user_id = 429;
-- gd_xs_leader (周杰) -> 广东分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广东分公司' AND d.dept_name = '销售部') WHERE user_id = 430;
-- gd_xs_user1 (顾杰) -> 广东分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广东分公司' AND d.dept_name = '销售部') WHERE user_id = 431;
-- gd_xs_user2 (夏明) -> 广东分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广东分公司' AND d.dept_name = '销售部') WHERE user_id = 432;
-- gd_xs_user3 (乐敏) -> 广东分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广东分公司' AND d.dept_name = '销售部') WHERE user_id = 433;
-- gd_cw_leader (林鹏) -> 广东分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广东分公司' AND d.dept_name = '财务部') WHERE user_id = 434;
-- gd_cw_user1 (段杰) -> 广东分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广东分公司' AND d.dept_name = '财务部') WHERE user_id = 435;
-- gd_cw_user2 (陈静) -> 广东分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广东分公司' AND d.dept_name = '财务部') WHERE user_id = 436;
-- gd_yf_leader (朱峰) -> 广东分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广东分公司' AND d.dept_name = '研发部') WHERE user_id = 437;
-- gd_yf_user1 (史伟) -> 广东分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广东分公司' AND d.dept_name = '研发部') WHERE user_id = 438;
-- gd_yf_user2 (任芳) -> 广东分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广东分公司' AND d.dept_name = '研发部') WHERE user_id = 439;
-- gd_xz_leader (宋磊) -> 广东分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广东分公司' AND d.dept_name = '行政部') WHERE user_id = 440;
-- gd_xz_user1 (邹强) -> 广东分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广东分公司' AND d.dept_name = '行政部') WHERE user_id = 441;
-- gd_xz_user2 (谭浩) -> 广东分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广东分公司' AND d.dept_name = '行政部') WHERE user_id = 442;
-- gx_leader (蒋超) -> 广西分公司
UPDATE sys_user SET dept_id = 1021 WHERE user_id = 443;
-- gx_bb_leader (李军) -> 广西分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广西分公司' AND d.dept_name = '广西分公司本部') WHERE user_id = 444;
-- gx_bb_user1 (于洋) -> 广西分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广西分公司' AND d.dept_name = '广西分公司本部') WHERE user_id = 445;
-- gx_bb_user2 (闫鹏) -> 广西分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广西分公司' AND d.dept_name = '广西分公司本部') WHERE user_id = 446;
-- gx_js_leader (高健) -> 广西分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广西分公司' AND d.dept_name = '技术部') WHERE user_id = 447;
-- gx_js_user1 (雷强) -> 广西分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广西分公司' AND d.dept_name = '技术部') WHERE user_id = 448;
-- gx_js_user2 (任强) -> 广西分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广西分公司' AND d.dept_name = '技术部') WHERE user_id = 449;
-- gx_xs_leader (于洋) -> 广西分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广西分公司' AND d.dept_name = '销售部') WHERE user_id = 450;
-- gx_xs_user1 (钱健) -> 广西分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广西分公司' AND d.dept_name = '销售部') WHERE user_id = 451;
-- gx_xs_user2 (叶军) -> 广西分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广西分公司' AND d.dept_name = '销售部') WHERE user_id = 452;
-- gx_cw_leader (付健) -> 广西分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广西分公司' AND d.dept_name = '财务部') WHERE user_id = 453;
-- gx_cw_user1 (谭浩) -> 广西分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广西分公司' AND d.dept_name = '财务部') WHERE user_id = 454;
-- gx_cw_user2 (吴鹏) -> 广西分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广西分公司' AND d.dept_name = '财务部') WHERE user_id = 455;
-- gx_cw_user3 (丁杰) -> 广西分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广西分公司' AND d.dept_name = '财务部') WHERE user_id = 456;
-- gx_yf_leader (李军) -> 广西分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广西分公司' AND d.dept_name = '研发部') WHERE user_id = 457;
-- gx_yf_user1 (夏明) -> 广西分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广西分公司' AND d.dept_name = '研发部') WHERE user_id = 458;
-- gx_yf_user2 (汤健) -> 广西分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广西分公司' AND d.dept_name = '研发部') WHERE user_id = 459;
-- gx_xz_leader (龚浩) -> 广西分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广西分公司' AND d.dept_name = '行政部') WHERE user_id = 460;
-- gx_xz_user1 (莫浩) -> 广西分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广西分公司' AND d.dept_name = '行政部') WHERE user_id = 461;
-- gx_xz_user2 (邵伟) -> 广西分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广西分公司' AND d.dept_name = '行政部') WHERE user_id = 462;
-- gx_xz_user3 (郝敏) -> 广西分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '广西分公司' AND d.dept_name = '行政部') WHERE user_id = 463;
-- hainan_leader (马超) -> 海南分公司
UPDATE sys_user SET dept_id = 1022 WHERE user_id = 464;
-- hainan_bb_leader (郑浩) -> 海南分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '海南分公司' AND d.dept_name = '海南分公司本部') WHERE user_id = 465;
-- hainan_bb_user1 (庞鹏) -> 海南分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '海南分公司' AND d.dept_name = '海南分公司本部') WHERE user_id = 466;
-- hainan_bb_user2 (庞鹏) -> 海南分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '海南分公司' AND d.dept_name = '海南分公司本部') WHERE user_id = 467;
-- hainan_bb_user3 (范鹏) -> 海南分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '海南分公司' AND d.dept_name = '海南分公司本部') WHERE user_id = 468;
-- hainan_js_leader (苏健) -> 海南分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '海南分公司' AND d.dept_name = '技术部') WHERE user_id = 469;
-- hainan_js_user1 (龙浩) -> 海南分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '海南分公司' AND d.dept_name = '技术部') WHERE user_id = 470;
-- hainan_js_user2 (段杰) -> 海南分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '海南分公司' AND d.dept_name = '技术部') WHERE user_id = 471;
-- hainan_xs_leader (梁健) -> 海南分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '海南分公司' AND d.dept_name = '销售部') WHERE user_id = 472;
-- hainan_xs_user1 (韦燕) -> 海南分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '海南分公司' AND d.dept_name = '销售部') WHERE user_id = 473;
-- hainan_xs_user2 (廖浩) -> 海南分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '海南分公司' AND d.dept_name = '销售部') WHERE user_id = 474;
-- hainan_xs_user3 (陈磊) -> 海南分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '海南分公司' AND d.dept_name = '销售部') WHERE user_id = 475;
-- hainan_cw_leader (金杰) -> 海南分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '海南分公司' AND d.dept_name = '财务部') WHERE user_id = 476;
-- hainan_cw_user1 (常丽) -> 海南分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '海南分公司' AND d.dept_name = '财务部') WHERE user_id = 477;
-- hainan_cw_user2 (周杰) -> 海南分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '海南分公司' AND d.dept_name = '财务部') WHERE user_id = 478;
-- hainan_cw_user3 (陈静) -> 海南分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '海南分公司' AND d.dept_name = '财务部') WHERE user_id = 479;
-- hainan_yf_leader (武强) -> 海南分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '海南分公司' AND d.dept_name = '研发部') WHERE user_id = 480;
-- hainan_yf_user1 (黎健) -> 海南分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '海南分公司' AND d.dept_name = '研发部') WHERE user_id = 481;
-- hainan_yf_user2 (方敏) -> 海南分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '海南分公司' AND d.dept_name = '研发部') WHERE user_id = 482;
-- hainan_yf_user3 (龚浩) -> 海南分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '海南分公司' AND d.dept_name = '研发部') WHERE user_id = 483;
-- hainan_xz_leader (汪健) -> 海南分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '海南分公司' AND d.dept_name = '行政部') WHERE user_id = 484;
-- hainan_xz_user1 (蔡磊) -> 海南分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '海南分公司' AND d.dept_name = '行政部') WHERE user_id = 485;
-- hainan_xz_user2 (罗娟) -> 海南分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '海南分公司' AND d.dept_name = '行政部') WHERE user_id = 486;
-- sichuan_leader (吕鹏) -> 四川分公司
UPDATE sys_user SET dept_id = 1023 WHERE user_id = 487;
-- sichuan_bb_leader (汪健) -> 四川分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '四川分公司' AND d.dept_name = '四川分公司本部') WHERE user_id = 488;
-- sichuan_bb_user1 (白鹏) -> 四川分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '四川分公司' AND d.dept_name = '四川分公司本部') WHERE user_id = 489;
-- sichuan_bb_user2 (李娜) -> 四川分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '四川分公司' AND d.dept_name = '四川分公司本部') WHERE user_id = 490;
-- sichuan_js_leader (李军) -> 四川分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '四川分公司' AND d.dept_name = '技术部') WHERE user_id = 491;
-- sichuan_js_user1 (樊丽) -> 四川分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '四川分公司' AND d.dept_name = '技术部') WHERE user_id = 492;
-- sichuan_js_user2 (秦伟) -> 四川分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '四川分公司' AND d.dept_name = '技术部') WHERE user_id = 493;
-- sichuan_xs_leader (潘健) -> 四川分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '四川分公司' AND d.dept_name = '销售部') WHERE user_id = 494;
-- sichuan_xs_user1 (高敏) -> 四川分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '四川分公司' AND d.dept_name = '销售部') WHERE user_id = 495;
-- sichuan_xs_user2 (侯健) -> 四川分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '四川分公司' AND d.dept_name = '销售部') WHERE user_id = 496;
-- sichuan_xs_user3 (乐敏) -> 四川分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '四川分公司' AND d.dept_name = '销售部') WHERE user_id = 497;
-- sichuan_cw_leader (罗浩) -> 四川分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '四川分公司' AND d.dept_name = '财务部') WHERE user_id = 498;
-- sichuan_cw_user1 (程丽) -> 四川分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '四川分公司' AND d.dept_name = '财务部') WHERE user_id = 499;
-- sichuan_cw_user2 (周杰) -> 四川分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '四川分公司' AND d.dept_name = '财务部') WHERE user_id = 500;
-- sichuan_cw_user3 (黎健) -> 四川分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '四川分公司' AND d.dept_name = '财务部') WHERE user_id = 501;
-- sichuan_yf_leader (杨明) -> 四川分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '四川分公司' AND d.dept_name = '研发部') WHERE user_id = 502;
-- sichuan_yf_user1 (梁健) -> 四川分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '四川分公司' AND d.dept_name = '研发部') WHERE user_id = 503;
-- sichuan_yf_user2 (莫浩) -> 四川分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '四川分公司' AND d.dept_name = '研发部') WHERE user_id = 504;
-- sichuan_yf_user3 (钟明) -> 四川分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '四川分公司' AND d.dept_name = '研发部') WHERE user_id = 505;
-- sichuan_xz_leader (周杰) -> 四川分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '四川分公司' AND d.dept_name = '行政部') WHERE user_id = 506;
-- sichuan_xz_user1 (马超) -> 四川分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '四川分公司' AND d.dept_name = '行政部') WHERE user_id = 507;
-- sichuan_xz_user2 (魏强) -> 四川分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '四川分公司' AND d.dept_name = '行政部') WHERE user_id = 508;
-- guizhou_leader (毛强) -> 贵州分公司
UPDATE sys_user SET dept_id = 1024 WHERE user_id = 509;
-- guizhou_bb_leader (龚浩) -> 贵州分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '贵州分公司' AND d.dept_name = '贵州分公司本部') WHERE user_id = 510;
-- guizhou_bb_user1 (万强) -> 贵州分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '贵州分公司' AND d.dept_name = '贵州分公司本部') WHERE user_id = 511;
-- guizhou_bb_user2 (潘婷) -> 贵州分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '贵州分公司' AND d.dept_name = '贵州分公司本部') WHERE user_id = 512;
-- guizhou_bb_user3 (莫浩) -> 贵州分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '贵州分公司' AND d.dept_name = '贵州分公司本部') WHERE user_id = 513;
-- guizhou_js_leader (夏明) -> 贵州分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '贵州分公司' AND d.dept_name = '技术部') WHERE user_id = 514;
-- guizhou_js_user1 (夏明) -> 贵州分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '贵州分公司' AND d.dept_name = '技术部') WHERE user_id = 515;
-- guizhou_js_user2 (汪敏) -> 贵州分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '贵州分公司' AND d.dept_name = '技术部') WHERE user_id = 516;
-- guizhou_js_user3 (余明) -> 贵州分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '贵州分公司' AND d.dept_name = '技术部') WHERE user_id = 517;
-- guizhou_xs_leader (邱明) -> 贵州分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '贵州分公司' AND d.dept_name = '销售部') WHERE user_id = 518;
-- guizhou_xs_user1 (谭浩) -> 贵州分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '贵州分公司' AND d.dept_name = '销售部') WHERE user_id = 519;
-- guizhou_xs_user2 (孙浩) -> 贵州分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '贵州分公司' AND d.dept_name = '销售部') WHERE user_id = 520;
-- guizhou_cw_leader (余明) -> 贵州分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '贵州分公司' AND d.dept_name = '财务部') WHERE user_id = 521;
-- guizhou_cw_user1 (贾丽) -> 贵州分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '贵州分公司' AND d.dept_name = '财务部') WHERE user_id = 522;
-- guizhou_cw_user2 (苏敏) -> 贵州分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '贵州分公司' AND d.dept_name = '财务部') WHERE user_id = 523;
-- guizhou_cw_user3 (严鹏) -> 贵州分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '贵州分公司' AND d.dept_name = '财务部') WHERE user_id = 524;
-- guizhou_yf_leader (戴健) -> 贵州分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '贵州分公司' AND d.dept_name = '研发部') WHERE user_id = 525;
-- guizhou_yf_user1 (袁伟) -> 贵州分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '贵州分公司' AND d.dept_name = '研发部') WHERE user_id = 526;
-- guizhou_yf_user2 (秦丽) -> 贵州分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '贵州分公司' AND d.dept_name = '研发部') WHERE user_id = 527;
-- guizhou_yf_user3 (刘洋) -> 贵州分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '贵州分公司' AND d.dept_name = '研发部') WHERE user_id = 528;
-- guizhou_xz_leader (莫浩) -> 贵州分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '贵州分公司' AND d.dept_name = '行政部') WHERE user_id = 529;
-- guizhou_xz_user1 (范鹏) -> 贵州分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '贵州分公司' AND d.dept_name = '行政部') WHERE user_id = 530;
-- guizhou_xz_user2 (贾伟) -> 贵州分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '贵州分公司' AND d.dept_name = '行政部') WHERE user_id = 531;
-- yunnan_leader (朱峰) -> 云南分公司
UPDATE sys_user SET dept_id = 1025 WHERE user_id = 532;
-- yunnan_bb_leader (钱健) -> 云南分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '云南分公司' AND d.dept_name = '云南分公司本部') WHERE user_id = 533;
-- yunnan_bb_user1 (余明) -> 云南分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '云南分公司' AND d.dept_name = '云南分公司本部') WHERE user_id = 534;
-- yunnan_bb_user2 (郭燕) -> 云南分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '云南分公司' AND d.dept_name = '云南分公司本部') WHERE user_id = 535;
-- yunnan_js_leader (龚浩) -> 云南分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '云南分公司' AND d.dept_name = '技术部') WHERE user_id = 536;
-- yunnan_js_user1 (吕鹏) -> 云南分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '云南分公司' AND d.dept_name = '技术部') WHERE user_id = 537;
-- yunnan_js_user2 (韦强) -> 云南分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '云南分公司' AND d.dept_name = '技术部') WHERE user_id = 538;
-- yunnan_js_user3 (钟明) -> 云南分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '云南分公司' AND d.dept_name = '技术部') WHERE user_id = 539;
-- yunnan_xs_leader (赵伟) -> 云南分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '云南分公司' AND d.dept_name = '销售部') WHERE user_id = 540;
-- yunnan_xs_user1 (程涛) -> 云南分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '云南分公司' AND d.dept_name = '销售部') WHERE user_id = 541;
-- yunnan_xs_user2 (丁杰) -> 云南分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '云南分公司' AND d.dept_name = '销售部') WHERE user_id = 542;
-- yunnan_cw_leader (邓健) -> 云南分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '云南分公司' AND d.dept_name = '财务部') WHERE user_id = 543;
-- yunnan_cw_user1 (姜伟) -> 云南分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '云南分公司' AND d.dept_name = '财务部') WHERE user_id = 544;
-- yunnan_cw_user2 (石芳) -> 云南分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '云南分公司' AND d.dept_name = '财务部') WHERE user_id = 545;
-- yunnan_cw_user3 (孙浩) -> 云南分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '云南分公司' AND d.dept_name = '财务部') WHERE user_id = 546;
-- yunnan_yf_leader (宋磊) -> 云南分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '云南分公司' AND d.dept_name = '研发部') WHERE user_id = 547;
-- yunnan_yf_user1 (王芳) -> 云南分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '云南分公司' AND d.dept_name = '研发部') WHERE user_id = 548;
-- yunnan_yf_user2 (高健) -> 云南分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '云南分公司' AND d.dept_name = '研发部') WHERE user_id = 549;
-- yunnan_xz_leader (谭浩) -> 云南分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '云南分公司' AND d.dept_name = '行政部') WHERE user_id = 550;
-- yunnan_xz_user1 (马超) -> 云南分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '云南分公司' AND d.dept_name = '行政部') WHERE user_id = 551;
-- yunnan_xz_user2 (武强) -> 云南分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '云南分公司' AND d.dept_name = '行政部') WHERE user_id = 552;
-- xizang_leader (黄涛) -> 西藏分公司
UPDATE sys_user SET dept_id = 1026 WHERE user_id = 553;
-- xizang_bb_leader (万强) -> 西藏分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '西藏分公司' AND d.dept_name = '西藏分公司本部') WHERE user_id = 554;
-- xizang_bb_user1 (雷强) -> 西藏分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '西藏分公司' AND d.dept_name = '西藏分公司本部') WHERE user_id = 555;
-- xizang_bb_user2 (孟健) -> 西藏分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '西藏分公司' AND d.dept_name = '西藏分公司本部') WHERE user_id = 556;
-- xizang_js_leader (侯健) -> 西藏分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '西藏分公司' AND d.dept_name = '技术部') WHERE user_id = 557;
-- xizang_js_user1 (汤健) -> 西藏分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '西藏分公司' AND d.dept_name = '技术部') WHERE user_id = 558;
-- xizang_js_user2 (熊浩) -> 西藏分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '西藏分公司' AND d.dept_name = '技术部') WHERE user_id = 559;
-- xizang_xs_leader (史伟) -> 西藏分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '西藏分公司' AND d.dept_name = '销售部') WHERE user_id = 560;
-- xizang_xs_user1 (江涛) -> 西藏分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '西藏分公司' AND d.dept_name = '销售部') WHERE user_id = 561;
-- xizang_xs_user2 (任芳) -> 西藏分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '西藏分公司' AND d.dept_name = '销售部') WHERE user_id = 562;
-- xizang_cw_leader (唐磊) -> 西藏分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '西藏分公司' AND d.dept_name = '财务部') WHERE user_id = 563;
-- xizang_cw_user1 (卢健) -> 西藏分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '西藏分公司' AND d.dept_name = '财务部') WHERE user_id = 564;
-- xizang_cw_user2 (陈静) -> 西藏分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '西藏分公司' AND d.dept_name = '财务部') WHERE user_id = 565;
-- xizang_yf_leader (陶强) -> 西藏分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '西藏分公司' AND d.dept_name = '研发部') WHERE user_id = 566;
-- xizang_yf_user1 (元敏) -> 西藏分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '西藏分公司' AND d.dept_name = '研发部') WHERE user_id = 567;
-- xizang_yf_user2 (尹强) -> 西藏分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '西藏分公司' AND d.dept_name = '研发部') WHERE user_id = 568;
-- xizang_yf_user3 (侯健) -> 西藏分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '西藏分公司' AND d.dept_name = '研发部') WHERE user_id = 569;
-- xizang_xz_leader (徐磊) -> 西藏分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '西藏分公司' AND d.dept_name = '行政部') WHERE user_id = 570;
-- xizang_xz_user1 (郝健) -> 西藏分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '西藏分公司' AND d.dept_name = '行政部') WHERE user_id = 571;
-- xizang_xz_user2 (余明) -> 西藏分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '西藏分公司' AND d.dept_name = '行政部') WHERE user_id = 572;
-- shaanxi_leader (丁杰) -> 陕西分公司
UPDATE sys_user SET dept_id = 1027 WHERE user_id = 573;
-- shaanxi_bb_leader (严鹏) -> 陕西分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '陕西分公司' AND d.dept_name = '陕西分公司本部') WHERE user_id = 574;
-- shaanxi_bb_user1 (庞鹏) -> 陕西分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '陕西分公司' AND d.dept_name = '陕西分公司本部') WHERE user_id = 575;
-- shaanxi_bb_user2 (尹强) -> 陕西分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '陕西分公司' AND d.dept_name = '陕西分公司本部') WHERE user_id = 576;
-- shaanxi_bb_user3 (石芳) -> 陕西分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '陕西分公司' AND d.dept_name = '陕西分公司本部') WHERE user_id = 577;
-- shaanxi_js_leader (汤健) -> 陕西分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '陕西分公司' AND d.dept_name = '技术部') WHERE user_id = 578;
-- shaanxi_js_user1 (郝敏) -> 陕西分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '陕西分公司' AND d.dept_name = '技术部') WHERE user_id = 579;
-- shaanxi_js_user2 (宋磊) -> 陕西分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '陕西分公司' AND d.dept_name = '技术部') WHERE user_id = 580;
-- shaanxi_js_user3 (邓健) -> 陕西分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '陕西分公司' AND d.dept_name = '技术部') WHERE user_id = 581;
-- shaanxi_xs_leader (郝健) -> 陕西分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '陕西分公司' AND d.dept_name = '销售部') WHERE user_id = 582;
-- shaanxi_xs_user1 (叶军) -> 陕西分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '陕西分公司' AND d.dept_name = '销售部') WHERE user_id = 583;
-- shaanxi_xs_user2 (段杰) -> 陕西分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '陕西分公司' AND d.dept_name = '销售部') WHERE user_id = 584;
-- shaanxi_cw_leader (钱健) -> 陕西分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '陕西分公司' AND d.dept_name = '财务部') WHERE user_id = 585;
-- shaanxi_cw_user1 (朱琳) -> 陕西分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '陕西分公司' AND d.dept_name = '财务部') WHERE user_id = 586;
-- shaanxi_cw_user2 (贾丽) -> 陕西分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '陕西分公司' AND d.dept_name = '财务部') WHERE user_id = 587;
-- shaanxi_yf_leader (闫鹏) -> 陕西分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '陕西分公司' AND d.dept_name = '研发部') WHERE user_id = 588;
-- shaanxi_yf_user1 (于洋) -> 陕西分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '陕西分公司' AND d.dept_name = '研发部') WHERE user_id = 589;
-- shaanxi_yf_user2 (黎健) -> 陕西分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '陕西分公司' AND d.dept_name = '研发部') WHERE user_id = 590;
-- shaanxi_yf_user3 (孟健) -> 陕西分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '陕西分公司' AND d.dept_name = '研发部') WHERE user_id = 591;
-- shaanxi_xz_leader (孟健) -> 陕西分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '陕西分公司' AND d.dept_name = '行政部') WHERE user_id = 592;
-- shaanxi_xz_user1 (彭涛) -> 陕西分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '陕西分公司' AND d.dept_name = '行政部') WHERE user_id = 593;
-- shaanxi_xz_user2 (韦强) -> 陕西分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '陕西分公司' AND d.dept_name = '行政部') WHERE user_id = 594;
-- gansu_leader (宋磊) -> 甘肃分公司
UPDATE sys_user SET dept_id = 1028 WHERE user_id = 595;
-- gansu_bb_leader (卢健) -> 甘肃分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '甘肃分公司' AND d.dept_name = '甘肃分公司本部') WHERE user_id = 596;
-- gansu_bb_user1 (朱峰) -> 甘肃分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '甘肃分公司' AND d.dept_name = '甘肃分公司本部') WHERE user_id = 597;
-- gansu_bb_user2 (范鹏) -> 甘肃分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '甘肃分公司' AND d.dept_name = '甘肃分公司本部') WHERE user_id = 598;
-- gansu_bb_user3 (熊浩) -> 甘肃分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '甘肃分公司' AND d.dept_name = '甘肃分公司本部') WHERE user_id = 599;
-- gansu_js_leader (钱健) -> 甘肃分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '甘肃分公司' AND d.dept_name = '技术部') WHERE user_id = 600;
-- gansu_js_user1 (龚浩) -> 甘肃分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '甘肃分公司' AND d.dept_name = '技术部') WHERE user_id = 601;
-- gansu_js_user2 (郭涛) -> 甘肃分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '甘肃分公司' AND d.dept_name = '技术部') WHERE user_id = 602;
-- gansu_js_user3 (薛健) -> 甘肃分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '甘肃分公司' AND d.dept_name = '技术部') WHERE user_id = 603;
-- gansu_xs_leader (梁健) -> 甘肃分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '甘肃分公司' AND d.dept_name = '销售部') WHERE user_id = 604;
-- gansu_xs_user1 (付健) -> 甘肃分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '甘肃分公司' AND d.dept_name = '销售部') WHERE user_id = 605;
-- gansu_xs_user2 (吴鹏) -> 甘肃分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '甘肃分公司' AND d.dept_name = '销售部') WHERE user_id = 606;
-- gansu_cw_leader (毛强) -> 甘肃分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '甘肃分公司' AND d.dept_name = '财务部') WHERE user_id = 607;
-- gansu_cw_user1 (郭燕) -> 甘肃分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '甘肃分公司' AND d.dept_name = '财务部') WHERE user_id = 608;
-- gansu_cw_user2 (孙浩) -> 甘肃分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '甘肃分公司' AND d.dept_name = '财务部') WHERE user_id = 609;
-- gansu_cw_user3 (谢峰) -> 甘肃分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '甘肃分公司' AND d.dept_name = '财务部') WHERE user_id = 610;
-- gansu_yf_leader (陶强) -> 甘肃分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '甘肃分公司' AND d.dept_name = '研发部') WHERE user_id = 611;
-- gansu_yf_user1 (马超) -> 甘肃分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '甘肃分公司' AND d.dept_name = '研发部') WHERE user_id = 612;
-- gansu_yf_user2 (高敏) -> 甘肃分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '甘肃分公司' AND d.dept_name = '研发部') WHERE user_id = 613;
-- gansu_yf_user3 (韦燕) -> 甘肃分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '甘肃分公司' AND d.dept_name = '研发部') WHERE user_id = 614;
-- gansu_xz_leader (邵伟) -> 甘肃分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '甘肃分公司' AND d.dept_name = '行政部') WHERE user_id = 615;
-- gansu_xz_user1 (付强) -> 甘肃分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '甘肃分公司' AND d.dept_name = '行政部') WHERE user_id = 616;
-- gansu_xz_user2 (陆强) -> 甘肃分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '甘肃分公司' AND d.dept_name = '行政部') WHERE user_id = 617;
-- qinghai_leader (方鹏) -> 青海分公司
UPDATE sys_user SET dept_id = 1029 WHERE user_id = 618;
-- qinghai_bb_leader (杜峰) -> 青海分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '青海分公司' AND d.dept_name = '青海分公司本部') WHERE user_id = 619;
-- qinghai_bb_user1 (韩雪) -> 青海分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '青海分公司' AND d.dept_name = '青海分公司本部') WHERE user_id = 620;
-- qinghai_bb_user2 (方鹏) -> 青海分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '青海分公司' AND d.dept_name = '青海分公司本部') WHERE user_id = 621;
-- qinghai_bb_user3 (范鹏) -> 青海分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '青海分公司' AND d.dept_name = '青海分公司本部') WHERE user_id = 622;
-- qinghai_js_leader (陈磊) -> 青海分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '青海分公司' AND d.dept_name = '技术部') WHERE user_id = 623;
-- qinghai_js_user1 (贾丽) -> 青海分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '青海分公司' AND d.dept_name = '技术部') WHERE user_id = 624;
-- qinghai_js_user2 (汤健) -> 青海分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '青海分公司' AND d.dept_name = '技术部') WHERE user_id = 625;
-- qinghai_js_user3 (付健) -> 青海分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '青海分公司' AND d.dept_name = '技术部') WHERE user_id = 626;
-- qinghai_xs_leader (陆强) -> 青海分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '青海分公司' AND d.dept_name = '销售部') WHERE user_id = 627;
-- qinghai_xs_user1 (龙浩) -> 青海分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '青海分公司' AND d.dept_name = '销售部') WHERE user_id = 628;
-- qinghai_xs_user2 (石强) -> 青海分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '青海分公司' AND d.dept_name = '销售部') WHERE user_id = 629;
-- qinghai_xs_user3 (孔伟) -> 青海分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '青海分公司' AND d.dept_name = '销售部') WHERE user_id = 630;
-- qinghai_cw_leader (袁伟) -> 青海分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '青海分公司' AND d.dept_name = '财务部') WHERE user_id = 631;
-- qinghai_cw_user1 (韦燕) -> 青海分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '青海分公司' AND d.dept_name = '财务部') WHERE user_id = 632;
-- qinghai_cw_user2 (孙婷) -> 青海分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '青海分公司' AND d.dept_name = '财务部') WHERE user_id = 633;
-- qinghai_yf_leader (宋磊) -> 青海分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '青海分公司' AND d.dept_name = '研发部') WHERE user_id = 634;
-- qinghai_yf_user1 (曹阳) -> 青海分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '青海分公司' AND d.dept_name = '研发部') WHERE user_id = 635;
-- qinghai_yf_user2 (方敏) -> 青海分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '青海分公司' AND d.dept_name = '研发部') WHERE user_id = 636;
-- qinghai_yf_user3 (方敏) -> 青海分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '青海分公司' AND d.dept_name = '研发部') WHERE user_id = 637;
-- qinghai_xz_leader (邓健) -> 青海分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '青海分公司' AND d.dept_name = '行政部') WHERE user_id = 638;
-- qinghai_xz_user1 (郝健) -> 青海分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '青海分公司' AND d.dept_name = '行政部') WHERE user_id = 639;
-- qinghai_xz_user2 (郭燕) -> 青海分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '青海分公司' AND d.dept_name = '行政部') WHERE user_id = 640;
-- ningxia_leader (覃杰) -> 宁夏分公司
UPDATE sys_user SET dept_id = 1030 WHERE user_id = 641;
-- ningxia_bb_leader (王强) -> 宁夏分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '宁夏分公司' AND d.dept_name = '宁夏分公司本部') WHERE user_id = 642;
-- ningxia_bb_user1 (郝健) -> 宁夏分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '宁夏分公司' AND d.dept_name = '宁夏分公司本部') WHERE user_id = 643;
-- ningxia_bb_user2 (沈浩) -> 宁夏分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '宁夏分公司' AND d.dept_name = '宁夏分公司本部') WHERE user_id = 644;
-- ningxia_js_leader (金杰) -> 宁夏分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '宁夏分公司' AND d.dept_name = '技术部') WHERE user_id = 645;
-- ningxia_js_user1 (熊浩) -> 宁夏分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '宁夏分公司' AND d.dept_name = '技术部') WHERE user_id = 646;
-- ningxia_js_user2 (熊浩) -> 宁夏分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '宁夏分公司' AND d.dept_name = '技术部') WHERE user_id = 647;
-- ningxia_xs_leader (宋磊) -> 宁夏分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '宁夏分公司' AND d.dept_name = '销售部') WHERE user_id = 648;
-- ningxia_xs_user1 (闫鹏) -> 宁夏分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '宁夏分公司' AND d.dept_name = '销售部') WHERE user_id = 649;
-- ningxia_xs_user2 (何强) -> 宁夏分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '宁夏分公司' AND d.dept_name = '销售部') WHERE user_id = 650;
-- ningxia_cw_leader (严鹏) -> 宁夏分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '宁夏分公司' AND d.dept_name = '财务部') WHERE user_id = 651;
-- ningxia_cw_user1 (邓健) -> 宁夏分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '宁夏分公司' AND d.dept_name = '财务部') WHERE user_id = 652;
-- ningxia_cw_user2 (向强) -> 宁夏分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '宁夏分公司' AND d.dept_name = '财务部') WHERE user_id = 653;
-- ningxia_cw_user3 (朱琳) -> 宁夏分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '宁夏分公司' AND d.dept_name = '财务部') WHERE user_id = 654;
-- ningxia_yf_leader (付健) -> 宁夏分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '宁夏分公司' AND d.dept_name = '研发部') WHERE user_id = 655;
-- ningxia_yf_user1 (肖梅) -> 宁夏分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '宁夏分公司' AND d.dept_name = '研发部') WHERE user_id = 656;
-- ningxia_yf_user2 (谭燕) -> 宁夏分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '宁夏分公司' AND d.dept_name = '研发部') WHERE user_id = 657;
-- ningxia_xz_leader (邹强) -> 宁夏分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '宁夏分公司' AND d.dept_name = '行政部') WHERE user_id = 658;
-- ningxia_xz_user1 (梁健) -> 宁夏分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '宁夏分公司' AND d.dept_name = '行政部') WHERE user_id = 659;
-- ningxia_xz_user2 (黄丽) -> 宁夏分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '宁夏分公司' AND d.dept_name = '行政部') WHERE user_id = 660;
-- ningxia_xz_user3 (范鹏) -> 宁夏分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '宁夏分公司' AND d.dept_name = '行政部') WHERE user_id = 661;
-- xinjiang_leader (段杰) -> 新疆分公司
UPDATE sys_user SET dept_id = 1031 WHERE user_id = 662;
-- xinjiang_bb_leader (尹强) -> 新疆分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '新疆分公司' AND d.dept_name = '新疆分公司本部') WHERE user_id = 663;
-- xinjiang_bb_user1 (金杰) -> 新疆分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '新疆分公司' AND d.dept_name = '新疆分公司本部') WHERE user_id = 664;
-- xinjiang_bb_user2 (陶强) -> 新疆分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '新疆分公司' AND d.dept_name = '新疆分公司本部') WHERE user_id = 665;
-- xinjiang_bb_user3 (肖梅) -> 新疆分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '新疆分公司' AND d.dept_name = '新疆分公司本部') WHERE user_id = 666;
-- xinjiang_js_leader (侯健) -> 新疆分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '新疆分公司' AND d.dept_name = '技术部') WHERE user_id = 667;
-- xinjiang_js_user1 (董强) -> 新疆分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '新疆分公司' AND d.dept_name = '技术部') WHERE user_id = 668;
-- xinjiang_js_user2 (姚磊) -> 新疆分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '新疆分公司' AND d.dept_name = '技术部') WHERE user_id = 669;
-- xinjiang_xs_leader (陆强) -> 新疆分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '新疆分公司' AND d.dept_name = '销售部') WHERE user_id = 670;
-- xinjiang_xs_user1 (万强) -> 新疆分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '新疆分公司' AND d.dept_name = '销售部') WHERE user_id = 671;
-- xinjiang_xs_user2 (邓健) -> 新疆分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '新疆分公司' AND d.dept_name = '销售部') WHERE user_id = 672;
-- xinjiang_cw_leader (万强) -> 新疆分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '新疆分公司' AND d.dept_name = '财务部') WHERE user_id = 673;
-- xinjiang_cw_user1 (贾丽) -> 新疆分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '新疆分公司' AND d.dept_name = '财务部') WHERE user_id = 674;
-- xinjiang_cw_user2 (孙浩) -> 新疆分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '新疆分公司' AND d.dept_name = '财务部') WHERE user_id = 675;
-- xinjiang_yf_leader (梁健) -> 新疆分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '新疆分公司' AND d.dept_name = '研发部') WHERE user_id = 676;
-- xinjiang_yf_user1 (周杰) -> 新疆分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '新疆分公司' AND d.dept_name = '研发部') WHERE user_id = 677;
-- xinjiang_yf_user2 (史伟) -> 新疆分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '新疆分公司' AND d.dept_name = '研发部') WHERE user_id = 678;
-- xinjiang_yf_user3 (姜伟) -> 新疆分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '新疆分公司' AND d.dept_name = '研发部') WHERE user_id = 679;
-- xinjiang_xz_leader (顾杰) -> 新疆分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '新疆分公司' AND d.dept_name = '行政部') WHERE user_id = 680;
-- xinjiang_xz_user1 (彭丽) -> 新疆分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '新疆分公司' AND d.dept_name = '行政部') WHERE user_id = 681;
-- xinjiang_xz_user2 (邹强) -> 新疆分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '新疆分公司' AND d.dept_name = '行政部') WHERE user_id = 682;
-- xinjiang_xz_user3 (林鹏) -> 新疆分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '新疆分公司' AND d.dept_name = '行政部') WHERE user_id = 683;
-- hk_leader (魏强) -> 香港分公司
UPDATE sys_user SET dept_id = 1032 WHERE user_id = 684;
-- hk_bb_leader (郭涛) -> 香港分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '香港分公司' AND d.dept_name = '香港分公司本部') WHERE user_id = 685;
-- hk_bb_user1 (夏明) -> 香港分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '香港分公司' AND d.dept_name = '香港分公司本部') WHERE user_id = 686;
-- hk_bb_user2 (薛健) -> 香港分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '香港分公司' AND d.dept_name = '香港分公司本部') WHERE user_id = 687;
-- hk_bb_user3 (吴敏) -> 香港分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '香港分公司' AND d.dept_name = '香港分公司本部') WHERE user_id = 688;
-- hk_js_leader (武强) -> 香港分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '香港分公司' AND d.dept_name = '技术部') WHERE user_id = 689;
-- hk_js_user1 (韩强) -> 香港分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '香港分公司' AND d.dept_name = '技术部') WHERE user_id = 690;
-- hk_js_user2 (秦伟) -> 香港分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '香港分公司' AND d.dept_name = '技术部') WHERE user_id = 691;
-- hk_js_user3 (龙浩) -> 香港分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '香港分公司' AND d.dept_name = '技术部') WHERE user_id = 692;
-- hk_xs_leader (贾伟) -> 香港分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '香港分公司' AND d.dept_name = '销售部') WHERE user_id = 693;
-- hk_xs_user1 (龚浩) -> 香港分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '香港分公司' AND d.dept_name = '销售部') WHERE user_id = 694;
-- hk_xs_user2 (毛强) -> 香港分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '香港分公司' AND d.dept_name = '销售部') WHERE user_id = 695;
-- hk_cw_leader (熊浩) -> 香港分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '香港分公司' AND d.dept_name = '财务部') WHERE user_id = 696;
-- hk_cw_user1 (元敏) -> 香港分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '香港分公司' AND d.dept_name = '财务部') WHERE user_id = 697;
-- hk_cw_user2 (陆强) -> 香港分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '香港分公司' AND d.dept_name = '财务部') WHERE user_id = 698;
-- hk_cw_user3 (高敏) -> 香港分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '香港分公司' AND d.dept_name = '财务部') WHERE user_id = 699;
-- hk_yf_leader (沈浩) -> 香港分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '香港分公司' AND d.dept_name = '研发部') WHERE user_id = 700;
-- hk_yf_user1 (邓健) -> 香港分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '香港分公司' AND d.dept_name = '研发部') WHERE user_id = 701;
-- hk_yf_user2 (叶军) -> 香港分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '香港分公司' AND d.dept_name = '研发部') WHERE user_id = 702;
-- hk_yf_user3 (田宇) -> 香港分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '香港分公司' AND d.dept_name = '研发部') WHERE user_id = 703;
-- hk_xz_leader (向强) -> 香港分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '香港分公司' AND d.dept_name = '行政部') WHERE user_id = 704;
-- hk_xz_user1 (龙浩) -> 香港分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '香港分公司' AND d.dept_name = '行政部') WHERE user_id = 705;
-- hk_xz_user2 (吕鹏) -> 香港分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '香港分公司' AND d.dept_name = '行政部') WHERE user_id = 706;
-- macau_leader (程涛) -> 澳门分公司
UPDATE sys_user SET dept_id = 1033 WHERE user_id = 707;
-- macau_bb_leader (曹阳) -> 澳门分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '澳门分公司' AND d.dept_name = '澳门分公司本部') WHERE user_id = 708;
-- macau_bb_user1 (陈磊) -> 澳门分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '澳门分公司' AND d.dept_name = '澳门分公司本部') WHERE user_id = 709;
-- macau_bb_user2 (夏明) -> 澳门分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '澳门分公司' AND d.dept_name = '澳门分公司本部') WHERE user_id = 710;
-- macau_js_leader (金杰) -> 澳门分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '澳门分公司' AND d.dept_name = '技术部') WHERE user_id = 711;
-- macau_js_user1 (孟芳) -> 澳门分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '澳门分公司' AND d.dept_name = '技术部') WHERE user_id = 712;
-- macau_js_user2 (潘健) -> 澳门分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '澳门分公司' AND d.dept_name = '技术部') WHERE user_id = 713;
-- macau_js_user3 (李娜) -> 澳门分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '澳门分公司' AND d.dept_name = '技术部') WHERE user_id = 714;
-- macau_xs_leader (武强) -> 澳门分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '澳门分公司' AND d.dept_name = '销售部') WHERE user_id = 715;
-- macau_xs_user1 (郝敏) -> 澳门分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '澳门分公司' AND d.dept_name = '销售部') WHERE user_id = 716;
-- macau_xs_user2 (方敏) -> 澳门分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '澳门分公司' AND d.dept_name = '销售部') WHERE user_id = 717;
-- macau_cw_leader (叶军) -> 澳门分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '澳门分公司' AND d.dept_name = '财务部') WHERE user_id = 718;
-- macau_cw_user1 (邹杰) -> 澳门分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '澳门分公司' AND d.dept_name = '财务部') WHERE user_id = 719;
-- macau_cw_user2 (孟芳) -> 澳门分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '澳门分公司' AND d.dept_name = '财务部') WHERE user_id = 720;
-- macau_yf_leader (黎健) -> 澳门分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '澳门分公司' AND d.dept_name = '研发部') WHERE user_id = 721;
-- macau_yf_user1 (叶军) -> 澳门分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '澳门分公司' AND d.dept_name = '研发部') WHERE user_id = 722;
-- macau_yf_user2 (苏敏) -> 澳门分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '澳门分公司' AND d.dept_name = '研发部') WHERE user_id = 723;
-- macau_yf_user3 (夏明) -> 澳门分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '澳门分公司' AND d.dept_name = '研发部') WHERE user_id = 724;
-- macau_xz_leader (尹强) -> 澳门分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '澳门分公司' AND d.dept_name = '行政部') WHERE user_id = 725;
-- macau_xz_user1 (莫浩) -> 澳门分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '澳门分公司' AND d.dept_name = '行政部') WHERE user_id = 726;
-- macau_xz_user2 (冯涛) -> 澳门分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '澳门分公司' AND d.dept_name = '行政部') WHERE user_id = 727;
-- taiwan_leader (廖浩) -> 台湾分公司
UPDATE sys_user SET dept_id = 1034 WHERE user_id = 728;
-- taiwan_bb_leader (罗浩) -> 台湾分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '台湾分公司' AND d.dept_name = '台湾分公司本部') WHERE user_id = 729;
-- taiwan_bb_user1 (邓健) -> 台湾分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '台湾分公司' AND d.dept_name = '台湾分公司本部') WHERE user_id = 730;
-- taiwan_bb_user2 (孔伟) -> 台湾分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '台湾分公司' AND d.dept_name = '台湾分公司本部') WHERE user_id = 731;
-- taiwan_bb_user3 (杜娟) -> 台湾分公司分公司本部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '台湾分公司' AND d.dept_name = '台湾分公司本部') WHERE user_id = 732;
-- taiwan_js_leader (彭涛) -> 台湾分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '台湾分公司' AND d.dept_name = '技术部') WHERE user_id = 733;
-- taiwan_js_user1 (程涛) -> 台湾分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '台湾分公司' AND d.dept_name = '技术部') WHERE user_id = 734;
-- taiwan_js_user2 (方敏) -> 台湾分公司技术部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '台湾分公司' AND d.dept_name = '技术部') WHERE user_id = 735;
-- taiwan_xs_leader (谢峰) -> 台湾分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '台湾分公司' AND d.dept_name = '销售部') WHERE user_id = 736;
-- taiwan_xs_user1 (杜峰) -> 台湾分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '台湾分公司' AND d.dept_name = '销售部') WHERE user_id = 737;
-- taiwan_xs_user2 (金杰) -> 台湾分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '台湾分公司' AND d.dept_name = '销售部') WHERE user_id = 738;
-- taiwan_xs_user3 (魏强) -> 台湾分公司销售部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '台湾分公司' AND d.dept_name = '销售部') WHERE user_id = 739;
-- taiwan_cw_leader (崔强) -> 台湾分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '台湾分公司' AND d.dept_name = '财务部') WHERE user_id = 740;
-- taiwan_cw_user1 (潘健) -> 台湾分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '台湾分公司' AND d.dept_name = '财务部') WHERE user_id = 741;
-- taiwan_cw_user2 (沈浩) -> 台湾分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '台湾分公司' AND d.dept_name = '财务部') WHERE user_id = 742;
-- taiwan_cw_user3 (武强) -> 台湾分公司财务部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '台湾分公司' AND d.dept_name = '财务部') WHERE user_id = 743;
-- taiwan_yf_leader (戴健) -> 台湾分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '台湾分公司' AND d.dept_name = '研发部') WHERE user_id = 744;
-- taiwan_yf_user1 (闫鹏) -> 台湾分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '台湾分公司' AND d.dept_name = '研发部') WHERE user_id = 745;
-- taiwan_yf_user2 (汤健) -> 台湾分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '台湾分公司' AND d.dept_name = '研发部') WHERE user_id = 746;
-- taiwan_yf_user3 (孟芳) -> 台湾分公司研发部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '台湾分公司' AND d.dept_name = '研发部') WHERE user_id = 747;
-- taiwan_xz_leader (张伟) -> 台湾分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '台湾分公司' AND d.dept_name = '行政部') WHERE user_id = 748;
-- taiwan_xz_user1 (尹强) -> 台湾分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '台湾分公司' AND d.dept_name = '行政部') WHERE user_id = 749;
-- taiwan_xz_user2 (顾杰) -> 台湾分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '台湾分公司' AND d.dept_name = '行政部') WHERE user_id = 750;
-- taiwan_xz_user3 (姚磊) -> 台湾分公司行政部
UPDATE sys_user SET dept_id = (SELECT d.dept_id FROM sys_dept d JOIN sys_dept p ON d.parent_id = p.dept_id WHERE p.dept_name = '台湾分公司' AND d.dept_name = '行政部') WHERE user_id = 751;

-- Total: 750 UPDATE statements generated
