# -*- coding: utf-8 -*-
import os

f = r"D:\workspace\RuoYi-Flowable-Plus-0.8.X\docs\最新整理好的需求\系统管理\00_管理员操作手册.md"
with open(f, 'r', encoding='utf-8') as fh:
    c = fh.read()

# 1. Remove duplicate v1.27 change notice (keep first, remove second + its following > line)
lines = c.split('\n')
new_lines = []
v127_count = 0
skip_next_blank = False
for line in lines:
    if 'v1.27' in line and 'FEAT_RATING' in line:
        v127_count += 1
        if v127_count >= 2:
            skip_next_blank = True
            continue
    if skip_next_blank and line.strip() == '>':
        skip_next_blank = False
        continue
    skip_next_blank = False
    new_lines.append(line)
c = '\n'.join(new_lines)

# 2. Update execution order version
c = c.replace('（v1.16，严格按序号', '（v1.27，严格按序号')

# 3. Update script counts
c = c.replace('★小微版 32 脚本', '★小微版 41 脚本')
c = c.replace('基础版 29 脚本', '基础版 33 脚本')

# 4. Update M15 script range
c = c.replace('M15_01~M15_14 + M16_01~M16_03', 'M15_01~M15_21 + M16_01~M16_03')

# 5. Update §5.3 feature flag heading
c = c.replace('功能开关管理（feature_flag 33 项 / 完整部署 34 项）', '功能开关管理（feature_flag 34 项 / 完整部署 35 项）')

# 6. Update footer date
c = c.replace('2026-09-13（v2.12', '2026-09-16（v2.12')

# 7. Update §9.3 title
c = c.replace('9.3 SQL 脚本清单（v1.26 系统管理 30 个脚本）', '9.3 SQL 脚本清单（v1.27 系统管理 30 个脚本）')

# 8. Update §9.3 script counts for each version
c = c.replace('11 阶段 34 脚本', '11 阶段 33 脚本')
c = c.replace('14 阶段 45 脚本', '14 阶段 44 脚本')
c = c.replace('15 阶段 36 脚本', '15 阶段 47 脚本')
c = c.replace('7 阶段 32 脚本', '7 阶段 41 脚本')

# 9. Update §7.1 and §7.3 headings
c = c.replace('v1.16 用例扩充至 34 条', 'v1.27 用例扩充至 35 条')
c = c.replace('v1.16 扩充至 34 条', 'v1.27 扩充至 35 条')

# 10. Update §7.3 description
c = c.replace('防串用例集扩展至 **34** 条', '防串用例集扩展至 **35** 条')

# 11. Update §5.3 description
c = c.replace('**33 项分组**（系统管理范围，组 A~D）', '**34 项分组**（系统管理范围，组 A~D + FEAT_RATING）')
c = c.replace('合计 34 项', '合计 35 项')

# 12. Update §9.2 matrix title
c = c.replace('v1.16，含小微版分档 + 推广分销）', 'v1.27，含小微版分档 + 推广分销 + 通用评价）')

# 13. Update §5.3 "查看 33 项开关列表"
c = c.replace('查看 33 项开关列表', '查看 34 项开关列表')

# 14. Update §9.3 小微版 description line at bottom
c = c.replace('M15_09~M15_13 为 v1.14 新增，M16_01~M16_03 为 v1.16 新增', 'M15_09~M15_13 为 v1.14 新增，M15_17~M15_21 为 v1.27 新增，M16_01~M16_03 为 v1.16 新增')

with open(f, 'w', encoding='utf-8') as fh:
    fh.write(c)

print("Done - all replacements applied successfully")
