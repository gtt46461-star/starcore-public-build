#!/usr/bin/env python3
"""Evidence-bound R31.4 source/acceptance audit. Never infers runtime PASS from files."""
import hashlib,html,json,subprocess
from collections import Counter
from datetime import datetime,timezone,timedelta
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
PLAN=ROOT/'construction/STARCORE_R31_FINAL_20261104'
OUT=ROOT/'construction/r31q/021'
OUT.mkdir(parents=True,exist_ok=True)
def read(name):return json.loads((PLAN/'ledgers'/name).read_text())
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def h(x):return html.escape(str(x))
def li(xs):return '<ul>'+''.join('<li>'+h(x)+'</li>' for x in xs)+'</ul>'
def table(head,rows):return '<div class="table"><table><thead><tr>'+''.join('<th>'+h(x)+'</th>' for x in head)+'</tr></thead><tbody>'+''.join('<tr>'+''.join('<td>'+str(x)+'</td>' for x in row)+'</tr>' for row in rows)+'</tbody></table></div>'
source=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
cards=read('cards.json');repairs=read('repair_cards.json');items=read('acceptance_items.json')
receipts={p.parent.name:json.loads(p.read_text()) for p in (ROOT/'construction/r31/batches').glob('*/RECEIPT.json')}
bindings={p.parent.name:json.loads(p.read_text()) for p in (ROOT/'construction/r31/batches').glob('*/BINDINGS.json')}
failed={'R31-B03-C01','R31-B03-C02','R31-B03-C03','R31-B04-C01','R31-B05-C02','R31-B11-C01','R31-B11-C02','R31-B11-C03','R31-B11-C04','R31-B12-C05'}
card_rows=[]
for c in cards+repairs:
    cid=c['id'];batch=c.get('construction_batch',c['batch']);receipt=receipts[batch]
    files=[f['path'] if isinstance(f,dict) else f for f in c['files']]
    local={i['item']:receipt.get('local_items',{}).get(i['item'],'NOT_IN_THIS_RECEIPT') for i in items if i['card']==cid and i['level']=='LOCAL'}
    # K01 carry items were discharged by K02; never erase the original carry history.
    for key,value in list(local.items()):
        if value!='PASS':
            for later in receipts.values():
                if later.get('local_items',{}).get(key)=='PASS':local[key]='PASS_LATER_RECEIPT:'+later['batch']
    runtime='DEVICE_COUNTEREXAMPLE_REOPENED' if cid in failed else 'CURRENT_DEVICE_NOT_MEASURED'
    card_rows.append({'id':cid,'batch':batch,'title':c['title'],'sourceFiles':[{'path':p,'exists':(ROOT/p).is_file(),'sha256':sha(ROOT/p) if (ROOT/p).is_file() else None} for p in files],
        'historicalReceiptSource':receipt.get('build_source_head'),'localReceiptItems':local,
        'bindings':{k:v for k,v in bindings.get(batch,{}).get('items',{}).items() if k.startswith(cid+'#')},
        'deferred':{i['item']:receipt.get('deferred_items',{}).get(i['item'],'NOT_RUN_CURRENT_DEVICE') for i in items if i['card']==cid and i['level']!='LOCAL'},
        'currentRuntimeState':runtime,'currentRepairAcceptance':'PENDING_CURRENT_CI_AND_DEVICE','scope':'source presence and historical exact receipt bindings; no implementation-completeness or runtime inference from file presence'})
paths=sorted({f['path'] for c in card_rows for f in c['sourceFiles']})
audit={'format':'R31Q21_SOURCE_AND_DELIVERY_REVIEW_V1','sourceCommit':source,'installedApkSource':'5e05e6689f64b1db11a524993681d49510648f8c','installedVersionCode':577311020,
       'plan':'R31.4-MERGED-11-20261107','originalPlanPreserved':True,'acceptanceConfigSha256':sha(PLAN/'ledgers/acceptance_config.json'),
       'counts':{'cards':len(card_rows),'originalCards':len(cards),'repairCards':len(repairs),'acceptanceLevels':dict(Counter(i['level'] for i in items)),'distinctPlanFiles':len(paths),'missingPlanFiles':sum(not (ROOT/p).is_file() for p in paths),'currentProductJavaFiles':len(list((ROOT/'app/src/main/java').rglob('*.java')))},
       'cards':card_rows,'metrics':read('metric_coverage_44.json'),'charts':read('charts.json'),'researchTables':read('research_tables_16.json'),'userRequirements':read('user_requirements_40.json'),
       'truthBoundary':'The user supplied a quick diagnostic, not a complete current DB snapshot. Historical plan observations and local receipt PASS do not become current runtime/model-quality PASS. Current build/device results are separate receipts.'}
(OUT/'SOURCE_ACCEPTANCE_AUDIT.json').write_text(json.dumps(audit,ensure_ascii=False,indent=2)+'\n')
body=[]
def section(title,text):body.append('<section><h2>'+h(title)+'</h2>'+text+'</section>')
section('先纠正“全部完成”的含义','<p>R31.4 的 K01—K11 均有施工关门收据，K11记录622项单测零失败；原方案只把LOCAL层列为关门条件。原交付声明明确保留真机24/72小时NOT_RUN、自然质量NOT_PROVEN，并记录一组Android故障矩阵未通过。因此，存在施工收据，与手机上的抓取—冻结—结算—研究—报告完整闭环已经正常，是两个不同的事实。本次真机反例必须重新打开相关运行验收。</p>'+
      table(['对象','精确身份/范围'],[['已安装APK',h('577311020 / R31-CHECKPOINT-P01-2026.11.12-020')],['已安装APK源码',h(audit['installedApkSource'])],['本次复核源码',h(source)],['唯一施工分支',h('construction/r31-final-20261104')],['方案与原阈值',h(audit['plan'])+'；验收配置 SHA256 '+h(audit['acceptanceConfigSha256'])],['源码清点',h(str(len(paths))+'个方案路径均存在；产品Java '+str(audit['counts']['currentProductJavaFiles'])+'个。存在不等于实现与运行已通过。')]]))
section('2026-11-12 19:35快检：已证实、推断与未知',table(['观察','可支持的结论','不能据此宣称'],[
 ['心跳约1.1秒，init=READY，business=WAITING_CANONICAL','主进程/服务活着，但业务终端没有完成','不能把RUNNING或BUSINESS_READY视为出号、结算和研究正常'],
 ['lastRequestOutcome=FAILED；队列active=0/queued=0；COORDINATOR_BUSY；两次ACK也失败','本次请求已在入队前失败，界面无终态反馈','不是“只要再等队列就会完成”；也不是磁盘物理写满'],
 ['抓取/健康/研究线程waitForConnection；writer在旧表ALTER','该瞬间存在数据库连接/写者竞争；旧表迁移是明确阻塞来源','单次堆栈不能证明永久死锁或所有故障只有一个原因'],
 ['旧表迁移legacyRunningMs约9908ms','这一实例正在迁移；当前版的60秒busy_timeout可把单次冲突放大成长等待','不能用9.9秒样本断言已卡死数小时'],
 ['分析与结算记录停在11月3日/2日；研究progress=0、learning=UNKNOWN','旧持久状态陈旧，本次尚无新业务进展凭据','不能把UNKNOWN或hit/p/brier=-1改写成0或有效胜率'],
 ['开盘19:35:03，启动约19:35:17，快检19:35:39','这个进程在本期开奖后才启动；本期缺赛前冻结可以是合法结果','禁止补造过去赛前预测；后续未来期必须重新验证'],
 ['uptimeMs=492474178','设备单调运行时间约5.7天','不是该应用连续运行5.7天'],
 ['数据库670,515,200字节、空闲59,909,611,520字节','数据库约639.5MiB；没有本次物理空间耗尽证据','应用级软预算、锁竞争与手机物理空闲不能混为一谈'],
 ['系统退出记录含single-cleaner FORCE STOP及diagnostics低内存退出','存在系统/清理器终止记录，退出间隔实际工作未知','crashCount=0不证明从未退出；force-stop后不能保证自启'],
 ['主线程处于MessageQueue轮询，旧版本记录过4348ms停顿','本次快检没有直接给出Android ANR判定','不能把旧停顿或nativePollOnce单独判成当前ANR']]))
section('源码根因与本次真实修改',table(['链路','原代码问题','实现变化','仍须验证'],[
 ['跨进程存储准入','StorageBudgetR31单次tryLock，瞬时竞争立即抛COORDINATOR_BUSY；进程内synchronized等待也无时限','公平ReentrantLock与OS文件锁共享5秒单调期限；可中断，未持有OS锁不进入账本逻辑；容量/租约/原子写规则保留','Android并发普查、长目录、报告服务与研究并存'],
 ['报告排队与失败反馈','长诊断文本写入被拒后请求未持久入队；两级失败ACK仍走同一预算锁','请求失败先发布有界、按requestId隔离的进程内终态；界面无ACK时读取；持续失败不再伪装排队','ACK仍须落盘；进程内反馈不是可恢复的持久ACK或成功报告'],
 ['独立迁移连接','Legacy.ensure内部重新选择owner.writable，日志与主链步骤回到业务连接池','选定连接贯穿运行表、迁移日志、主链步骤与最终校验；保留同一个应用写者','跨Android版本连接池行为、旧库升级与原始数据保全'],
 ['旧库升级长期占写者','单个迁移回调执行全部旧表和大量ALTER；写锁获取期限不能限制回调体总时长','运行迁移按组/表分成持久日志单元；主链按表分单元；每次回调一个单元，队列续跑时先释放原写者','单个大索引仍可能耗时；以实际锁持有分布验收，不能声称已保证设备P95'],
 ['加列与列缓存','ALTER/PRAGMA异常被吞；失败空集合进入列缓存；表存在检查可能掩盖缺列','迁移期收集列操作异常并拒绝READY；失败或空列清单不缓存；忙等待从每语句60秒改为2秒后进入可观测重试','所有生产必需列、索引、默认值的最终合同必须实际通过'],
 ['迁移续跑与保活','原控制节拍5秒才能再次申请整段迁移，恢复进展与线程活着混淆','完成单元后排到原有后台队列继续；队列满才回退已有控制节拍；不并发开第二写者','取消、进程退出、升级后续做、服务停止条件'],
 ['全部关门后的CI','status返回ALL_BATCHES_CLOSED且无open_batch，CI取字段直接退出；打印旧摘要可能误导','已关闭时继续跑K11完整回归；检查摘要源码等于本次提交且Gradle/单测/HOST均通过','历史收据不改写为新源码验收']]))
section('必须同时跑通的运行合同',li([
 '身份：同包名com.starcore.research.r13、既有证书d8d8cee7…、递增versionCode、APK内源码身份一致。保留数据库，不靠清空数据过关。',
 '启动：关键域READY与旧库/研究迁移状态分别显示；每个迁移步骤有持久进展、失败原因和可恢复游标；不能用心跳代替业务进展。',
 '实时：正常网络与系统允许运行时目标1秒轮询，单飞；区分HTTP、解析、入库时间。当前期写入优先，历史回补有界，5000期滚动并保留活跃输入pin。',
 '预测：只基于当时已知数据；单位置固定K=3；唯一冻结提交，晚到FULL不覆盖已冻结记录。没有合格策略可以合法NO_BET，必须显示原因；不能随机生成K3伪装修复。',
 '结算：已开奖只消费真实赛前冻结；缺冻结记缺失，不补未来预测；结算outbox幂等，标签/反馈/训练事件不因NO_BET或缺冻结永久饥饿。',
 '研究：历史训练、样本外、未来冻结证据分开；每次推进有cursor/产物/提交回执，只有attempt或owner存活不算学习。进化/变异/融合继承结构，不能继承父代证书或伪造子代独立证据。',
 '主链：typed资格、撤销、过期与唯一DecisionGate继续生效；SHADOW不计REAL；不降低统计门槛换取主链非零。',
 '报告：44指标、96检查、47图、20矩阵、16逻辑表逐项列明定义、输入、生产、消费、有效性、分母、水位、失败/未知原因和修复卡；不能只展示总数。',
 '交付：完整报告生成、ZIP/分卷校验、分享Provider读取、SAF保存与回读各自验收；取消/杀进程/锁忙/磁盘不足不得破坏实时库。恢复诊断包必须标明不是完整报告。',
 '性能：draw入库至结算P95≤1秒、base draw提交至下一期冻结P95≤5秒按原合同实测；源数据晚到单列；未知或零样本不填虚假P95。',
 '后台：正常系统约束下验证熄屏/后台；系统force-stop后的未知间隔如实记录。24/72小时未跑不得记PASS。',
 '质量：工程恢复不等于存在预测优势；胜率、置信区间、覆盖频率、窗口稳定性和独立未来样本分别报告。自然优势无法由架构或测试承诺。']))
section('新增联测：验证整条导出路径','<p>R31QDeliveryInstrumentedTest在显式启用的全新模拟器私有目录中运行，主进程和diagnostics服务共用真实应用数据根。测试不替换存储、报告服务或分享Provider：</p>'+li([
 '旧表迁移续跑：在迁移单元之间尝试真实关键写入，最终验证主链表结构及探针原值；现成READY库与实际迁移分开记录。',
 '真实报告：由ReportExportServiceCP4.requestExport提交，等待真实ACK进入READY_TO_SHARE，验证发布ZIP，再逐文件通过ContentResolver打开分享URI并复算字节数与SHA256。',
 '有报告不等于检查全部健康；空样本/缺模型必须如实报告。测试输出checksComplete与allChecksPassed，不能用文件可读替代领域结果。',
 '主机锁回归使用真实独立JVM及OS文件锁：瞬时占锁后继续、长期占锁5秒退出且账本不变、取消保留中断标志、容量超额仍拒绝、请求失败不串到别的requestId。',
 '真实服务联网联测：启用AutoFetchService后必须取得新API抓取成功、新赛前冻结和其后真实开奖结算；commit_at必须早于deadline_at；不以启动成功或心跳代替这三个进展。继续执行原622项单测、lint、注册HOST套件、API29/35运行协议与故障矩阵；新增Android34真实报告导出与联网闭环，匹配用户Android14平台版本。任何失败继续修，不删断言。']))
section('11批逐批落实情况与当前重开范围',table(['批次','历史收据','原本地项目','本次复核结论'],[[h(b),'存在；源码 '+h(r.get('build_source_head','')[:12]),h(str(len(r.get('local_items',{})))),'历史LOCAL关门；'+('运行反例相关，必须重新联测' if b in {'K03','K04','K10','K11'} else '当前版本全量回归；不继承当前真机通过')] for b,r in sorted(receipts.items())]))
section('58张项目卡：源码、测试绑定、运行状态逐项对照','<p>以下逐卡列出的“源码存在”只证明路径完整；每个文件的当前SHA256、精确LOCAL item与绑定测试名、延后项目、历史收据源码，完整保存在同包SOURCE_ACCEPTANCE_AUDIT.json。禁止以路径存在自动判断功能完整。</p>'+table(['卡 / 批','用途','源码路径','历史LOCAL / 延后','当前运行判断'],[[h(c['id']+' / '+c['batch']),h(c['title']),'<br>'.join(h(f['path']) for f in c['sourceFiles']),h(str(len(c['localReceiptItems']))+'项；延后'+str(len(c['deferred']))),h(c['currentRuntimeState'])] for c in card_rows]))
section('44指标必须逐项给出结果','<p>这里保留指标定义与数据来源；原方案中的current_status/current_value属于历史观测，不自动移植为新版本结果。本机当前完整报告尚不可取得，以下设备值一律待新报告采集。</p>'+table(['ID / 名称','取证入口','目标','当前设备证据'],[[h(m['id']+' / '+m['name']),h(m.get('source','')),h(m.get('target','')),'待新完整报告；未知不补0'] for m in audit['metrics']]))
section('47图逐图：生产、消费、诊断与验收','<p>展示帧、可用数值、研究消费与策略提升是四层证据。图表缓存存在不证明研究已消费；消费成功不证明存在统计优势。</p>'+table(['图 / 用途','生产与输入','实际消费对象','当前缺口与验收'],[[h(c['id']+' / '+c.get('title','')),h(c.get('producer',''))+'<br>'+h(c.get('source_method',''))+'<br>'+h(c.get('source_tables_or_sql','')),h(c.get('consumer','')),h(c.get('repair',''))+'<br>'+h('; '.join(c.get('acceptance',[])))] for c in audit['charts']]))
section('16逻辑研究表逐表合同','<p>每张表要能解释“什么输入、什么角色、谁生产、谁消费、与哪个模型或资产对应、哪条独立证据说明有用”。物理表行数只是存量，不能替代研究推进。</p>'+table(['表ID','查询合同','修改和缺口','验收'],[[h(t['id']),'<code>'+h(t.get('baseline_sql',''))+'</code>',h(t.get('operation',''))+'<br>'+h(t.get('defect','')),h(t.get('acceptance',''))] for t in audit['researchTables']]))
section('用户40项要求的保全映射',table(['要求','关联卡','应用验收'],[[h(r['id']+' / '+r['requirement']),h(', '.join(r.get('cards',[]))),h(r.get('app_acceptance',''))] for r in audit['userRequirements']]))
section('发布验收：不再把源码施工完成写成手机全部正常',li([
 '先冻结实际编译的调用索引和源文件哈希，再从干净提交构建；签名、包名、版本、ABI、APK内源码身份必须互证。',
 '所有本地确定性检查必须通过；模拟器的真实报告生成、发布和分享读取必须实跑；旧故障矩阵的失败不得隐藏。',
 '安装包、源码提交、原方案、此次代码差异、完整验收日志和问题清单绑定一个交付版本。历史收据保持原源码身份，不改成当前PASS。',
 '实机V2353A的原670MB数据库不在云端，不能把模拟器新库成功冒充该旧库恢复成功。交付时写清已验证条件，安装后用新完整报告复核旧库及未来期链路。',
 '无论何时，尚未运行的24/72小时、未知后台缺口和自然质量都保持NOT_RUN/NOT_PROVEN；不承诺“所有将来情况100%无故障”。']))
doc='<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>STARCORE R31.4 源码复核与R31Q21交付验收</title><style>body{font:16px/1.65 system-ui,sans-serif;background:#f4f6fa;color:#1e293b;margin:0}main{max-width:1280px;margin:auto;padding:28px}header,section{background:white;border:1px solid #dce3ec;border-radius:12px;padding:24px;margin:16px 0}h1{font-size:30px;line-height:1.3}h2{font-size:23px;color:#164e63}p,li{max-width:1100px}.tag{color:#0e7490;font-weight:650}.table{overflow:auto}table{border-collapse:collapse;width:100%;font-size:14px}td,th{border:1px solid #dce3ec;padding:10px;vertical-align:top;min-width:130px;overflow-wrap:anywhere}th{background:#eaf1f8;text-align:left}code{font:12px/1.5 ui-monospace,monospace}footer{color:#64748b;font-size:13px}@media print{main{padding:0}header,section{border:0;padding:12px}tr{break-inside:avoid}h2{break-after:avoid}} </style><main><header><div class="tag">源码审计 · 真实运行反例 · 完整交付验收</div><h1>STARCORE R31.4 源码复核<br>R31Q21 运行恢复与报告闭环</h1><p>依据：用户2026-11-12 19:35:39 +0800快检、唯一施工分支实际源码、K01—K11精确收据与原31.4封存方案。此文是源码复核与修订验收说明，不冒充手机生成的完整检测报告。</p></header>'+''.join(body)+'<footer>生成于 '+datetime.now(timezone(timedelta(hours=8))).isoformat(timespec='seconds')+'。当前验收状态以独立CI/设备回执为准。自然质量不可由施工承诺。</footer></main></html>'
(OUT/'STARCORE_R31Q21_SOURCE_AUDIT.html').write_text(doc)
print(json.dumps({'source':source,'cards':len(card_rows),'paths':len(paths),'missing':audit['counts']['missingPlanFiles'],'levels':audit['counts']['acceptanceLevels'],'htmlBytes':len(doc.encode())},ensure_ascii=False))
