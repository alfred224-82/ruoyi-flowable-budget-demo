# -*- coding: utf-8 -*-
import os
from docx import Document
from docx.shared import Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn

BASE = r"D:\workspace\RuoYi-Flowable-Plus-0.8.X\docs\推广文案\20260919"

doc = Document()

# 页面设置 A4, 2.5cm 边距
sec = doc.sections[0]
sec.page_width = Cm(21)
sec.page_height = Cm(29.7)
sec.top_margin = Cm(2.5)
sec.bottom_margin = Cm(2.5)
sec.left_margin = Cm(2.5)
sec.right_margin = Cm(2.5)

def set_font(run, cn='宋体', en='Times New Roman', size=12, bold=False, color=None):
    run.font.name = en
    run.font.size = Pt(size)
    run.font.bold = bold
    r = run._element
    r.rPr.rFonts.set(qn('w:eastAsia'), cn)
    if color:
        run.font.color.rgb = RGBColor(*color)

def title(text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run(text)
    set_font(run, cn='黑体', size=22, bold=True, color=(0x1a, 0x2a, 0x4a))
    p.paragraph_format.space_after = Pt(12)

def h1(text):
    p = doc.add_paragraph()
    run = p.add_run(text)
    set_font(run, cn='黑体', size=16, bold=True, color=(0x1a, 0x2a, 0x4a))
    p.paragraph_format.space_before = Pt(14)
    p.paragraph_format.space_after = Pt(6)
    # 与下段同页
    p.paragraph_format.keep_with_next = True

def h2(text):
    p = doc.add_paragraph()
    run = p.add_run(text)
    set_font(run, cn='黑体', size=13, bold=True, color=(0x33, 0x33, 0x33))
    p.paragraph_format.space_before = Pt(8)
    p.paragraph_format.space_after = Pt(4)
    p.paragraph_format.keep_with_next = True

def body(text, indent=True):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    run = p.add_run(text)
    set_font(run, cn='宋体', size=12)
    pf = p.paragraph_format
    pf.line_spacing = 1.5
    if indent:
        pf.first_line_indent = Pt(24)
    return p

def bullet(text):
    p = doc.add_paragraph(style='List Bullet')
    run = p.add_run(text)
    set_font(run, cn='宋体', size=12)
    p.paragraph_format.line_spacing = 1.5

def add_image(fname, caption, width=Cm(13)):
    path = os.path.join(BASE, fname)
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run()
    run.add_picture(path, width=width)
    p.paragraph_format.space_before = Pt(8)
    p.paragraph_format.space_after = Pt(2)
    # 图题
    cap = doc.add_paragraph()
    cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    crun = cap.add_run(caption)
    set_font(crun, cn='楷体', size=10.5, color=(0x66, 0x66, 0x66))
    cap.paragraph_format.space_after = Pt(10)

# ===== 封面图 =====
add_image('封面_科技使您生活更加美好.png', '', width=Cm(15.5))

# ===== 标题 =====
title('科技使您生活更加美好')
sub = doc.add_paragraph()
sub.alignment = WD_ALIGN_PARAGRAPH.CENTER
srun = sub.add_run('我们使您赚钱更轻松')
set_font(srun, cn='楷体', size=14, bold=False, color=(0x8a, 0x6d, 0x2f))
sub.paragraph_format.space_after = Pt(6)
sub2 = doc.add_paragraph()
sub2.alignment = WD_ALIGN_PARAGRAPH.CENTER
s2run = sub2.add_run('你只需要提供灵感，我们负责给你实现落地')
set_font(s2run, cn='楷体', size=12, color=(0x88, 0x88, 0x88))
sub2.paragraph_format.space_after = Pt(14)

# ===== 开篇 =====
body('开店做生意，最怕三件事：')
body('周末爆单算不清、员工提成扯皮、月底一看流水热闹、利润没影。')
body('你不是缺客户，你是缺一套"老板也能用明白"的经营系统。')
body('我们做的店铺小程序系统，不碰支付、不让你搞复杂 ERP，只干一件事：把"预约—服务—记账—分成—固定支出—利润"这条线，替你跑顺。')

# ===== 一、桌游 =====
h1('一、桌游 / 剧本杀：房间、本子、DM 全靠脑子记？')
body('老张的周六上午：', indent=False)
body('"小王，下午两点那个《来电》的客人到了没？"')
body('"还没呢张哥，我发微信问问。"')
body('"别问了，昨晚他就说可能来不了……那两点空着，损失两百多。"')
add_image('桌游店美女店主烦恼图.png', '图1  周末爆单，房间、本子、DM 全靠脑子记')
h2('痛点')
bullet('周末一场接一场，房间冲突、DM 排重、拼场靠群聊')
bullet('按人收、按桌收、按小时收、加时补差，Excel 算到天亮')
bullet('顾客预约了不来，黄金时段空着，没人管"爽约"')
bullet('老客来了问"我上次玩啥本、跟哪个 DM"，你翻聊天记录')
h2('我们的解法')
bullet('房间 / 空间 / 项目统一建模：哪个房、哪小时、哪服务，系统里一目了然')
bullet('顾客小程序自助看房看本 → 预约 → 老板后台排期，不用来回微信')
bullet('防爽约规则（付费版）：频繁放鸽子自动进黑名单，老板手动放行')
bullet('一场一单、按时长计价，加时自动算钱')
bullet('员工（DM）有自己小程序：今日待办、我的场次、我的推广码')
body('桌游店老板终于不用当"人工排片表"。')
add_image('桌游店老板用系统松口气图.png', '图2  系统跑顺预约与排期，老板终于松口气')

# ===== 二、健身 =====
h1('二、健身 / 瑜伽 / 普拉提：流水好看，利润哭晕')
body('教练小陈的月底：', indent=False)
body('"老板，我这个月上了 60 节私教、卖了 3 万块钱课包，提成您算好了吗？"')
body('"我还在对……你上周三那节是体验课还是正式课？学员叫啥来着？"')
body('"……我忘了记了。"')
add_image('健身老板教练对账烦恼图.png', '图3  月底对账，课时与提成对不上')
h2('痛点')
bullet('私教课、团课、次卡、储值……收入类型比动作还多')
bullet('教练底薪+课时费+销售提成，老板算完发现：教练比自己赚得多')
bullet('房租、水电、器械损耗是刚性支出，会员不来也要付')
bullet('预收款≠利润，课没消完，钱别先花光——但没人提醒你')
h2('我们的解法')
bullet('固定支出标签：房租 / 水电 / 工资 / 器械维护，分类记账')
bullet('老板驾驶舱直接看：本月收入 − 流水支出 − 固定支出 = 真利润')
bullet('教练业绩、课时、评分、推广拉新，系统算，不靠嘴')
bullet('不接支付：现金/线下/老板自定收款都行，"标记已线下发放"记账，合规不踩线')
bullet('会员不是"办卡就结束"，是"消课才算收入"')
body('健身房最怕把负债当利润。我们帮你把账说人话。')
add_image('健身教练用系统松口气图.png', '图4  老板驾驶舱看清真利润')

# ===== 三、美业 =====
h1('三、美容 / 美甲 / 皮肤管理：技师提成算到吵架')
body('月底对账夜：', indent=False)
body('"芳姐，我这个月做了 28 个美甲、6 个睫毛，提成应该是 2100。"')
body('"不对啊小丽，你上周二那个顾客说是老客介绍的，只收了 8 折，提成要扣掉。"')
body('"那我怎么知道哪个是老客介绍的？你当时也没跟我说啊！"')
add_image('美业技师提成烦恼图.png', '图5  多技师协作，月底提成算到吵架')
h2('痛点')
bullet('项目多、耗材不同、提成规则乱')
bullet('多技师协作：谁主做、谁辅助、怎么分，月底全靠回忆')
bullet('顾客储值余额、剩余疗程、到店偏好，前台记不住')
bullet('老客被员工私加微信，老板看不见')
h2('我们的解法')
bullet('服务项 + 房间/工位 + 员工角色绑定，一次配置，长期复用')
bullet('每笔服务记"谁做、啥项目、多少时长、多少收入"')
bullet('员工结算只记账、不碰支付，老板确认后留痕')
bullet('顾客小程序：预约、看记录、看评价、我的推广码')
bullet('通用评价与点赞（付费版）：好技师不被埋没，差服务老板看得见')
body('美业不是缺项目，是缺"谁干了啥、该拿多少"的透明度。')
add_image('美业用系统松口气图.png', '图6  服务结算清晰，提成不再扯皮')

# ===== 四、助老 =====
h1('四、助老 / 社区养老服务站：老人不会用手机，活动通知全靠喊')
body('周二早上，独居的李大爷在家犯愁：', indent=False)
body('"闺女给我弄了个小程序，说上面能看到护工啥时候来……可我这老花眼，字跟蚂蚁似的，根本看不清。"')
body('他试着点了一下活动详情页下面的小喇叭图标——一个熟悉的声音传出来：')
body('"李爷爷您好，我是护工小刘。明天下午 1 点我准时到您家，帮您做康复训练和量血压。您什么都不用准备，我带着设备过去。到时候见！"')
body('李大爷笑了："是小刘的声音！这下放心了。"')
add_image('助老看不懂手机烦恼图.png', '图7  老人看不清手机，活动通知全靠喊')
h2('痛点')
bullet('日间照料、上门助浴、康复理疗、老年餐……项目杂，排班乱')
bullet('老人不会用小程序、不会看公众号，活动通知只能电话或上门喊')
bullet('家属想了解老人当天参加了什么、吃了什么，没有记录')
bullet('服务人员（护工/社工）流动性大，工时和服务记录全靠纸质本子')
h2('我们的解法')
bullet('语音播报功能：每个活动详情页都带"一键收听"按钮，点一下就用本地语音读出活动名称、时间、地点、费用，老人不用看字也能知道内容')
bullet('活动/服务项目在系统里配置好后，老人或家属在小程序上就能看到完整列表，支持语音播报')
bullet('护工有独立小程序：今日待办服务清单、服务打卡、老人签到记录')
bullet('家属端可查看老人当日参与记录，放心又省心')
bullet('固定支出标签：场地租金、护工工资、餐食采购，一笔笔算清楚')
body('助老服务最难的不是服务本身，是"老人看不到、家属不知道、护工记不清"。语音播报让信息听得见，系统让服务有记录。')
add_image('助老听语音播报松口气图.png', '图8  语音播报让老人听得见，服务有记录')

# ===== 五 =====
h1('五、为什么我们适合你？')
body('因为我们按小微商家设计：')
bullet('一个微信小程序，老板/员工/顾客都用')
bullet('手机号为身份锚点，不折腾账号体系')
bullet('V1 零支付：不接支付 SDK，钱不过平台，老板自己收')
bullet('开张选行业 → 自动套行业模板（项目 / 房间 / 角色）')
bullet('老板看"经营驾驶舱"，租户管理员管"配置对不对"，职责不混')
bullet('推广码永久有效、最后扫码归谁、拉新奖励只发一次')
bullet('语音播报：活动详情一键朗读，适合老年用户、视力不便人群')
bullet('防爽约、评价、合规记录、固定支出、异常待办，该有的都有')
body('你不用学财务，也不用请运营。')
body('你只要知道：今天赚没赚、钱花哪了、谁在帮你赚钱。')

# ===== 六 =====
h1('六、系统已覆盖的行业')
h2('休闲娱乐（6）')
body('桌游吧 / 剧本杀 / 密室逃脱 / 棋牌室 / 轰趴馆 / 电竞馆', indent=False)
h2('生活服务（7）')
body('美容院 / 美甲美睫 / 皮肤管理 / 美发沙龙 / 推拿艾灸 / 采耳护理 / 洗衣洗鞋', indent=False)
h2('运动健身（8）')
body('健身房 / 瑜伽馆 / 普拉提 / 搏击格斗 / 舞蹈室 / 羽毛球馆 / 游泳馆 / 攀岩馆', indent=False)
h2('专业工作室（3）')
body('摄影工作室 / 心理咨询室 / 宠物美容工作室', indent=False)
h2('助老服务（5）')
body('社区养老服务站 / 日间照料中心 / 居家养老服务商 / 老年大学 / 老年食堂', indent=False)
body('共 29 个细分行业模板。')
body('你选行业 → 系统给项目、房间、岗位预设 → 你改名字就行。')

# ===== 七 =====
h1('七、结尾一句话')
body('大系统教你怎么"管组织"，我们这套，教你怎么"生意别算糊涂账"。')
body('店铺小程序系统：免费档能推广，付费档管经营。')
body('不收你支付通道费，不绑你收款码，不把你变会计。')
body('老板的第一套经营系统，应该从"算得清"开始。')
body('助老机构的第一套数字化工具，应该从"听得见"开始。')
tail = doc.add_paragraph()
tail.alignment = WD_ALIGN_PARAGRAPH.CENTER
trun = tail.add_run('科技使您生活更加美好')
set_font(trun, cn='黑体', size=14, bold=True, color=(0x1a, 0x2a, 0x4a))
tail.paragraph_format.space_before = Pt(16)

def fix_quotes_in_doc(doc):
    """把所有 run 文本里的 ASCII 双引号转成中文弯引号"""
    for p in doc.paragraphs:
        for run in p.runs:
            t = run.text
            if '"' not in t:
                continue
            out = []
            open_q = True
            for ch in t:
                if ch == '"':
                    out.append('“' if open_q else '”')
                    open_q = not open_q
                else:
                    out.append(ch)
            run.text = ''.join(out)

out = os.path.join(BASE, '推广文案_科技使您生活更加美好.docx')
fix_quotes_in_doc(doc)
doc.save(out)
print('SAVED:', out)
