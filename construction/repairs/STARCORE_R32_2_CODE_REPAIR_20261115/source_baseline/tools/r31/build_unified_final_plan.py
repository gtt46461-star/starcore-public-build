#!/usr/bin/env python3
"""Build one cumulative R31.1–R31.4 implementation/repair contract, never a completion claim."""
from pathlib import Path
import collections,hashlib,html,json,shutil,csv,io,zipfile,re
ROOT=Path(__file__).resolve().parents[2]
ORIG=ROOT/'construction/STARCORE_R31_FINAL_20261104'
OUT=ROOT/'construction/repairs/STARCORE_R31_UNIFIED_FINAL_20261112'
OUT.mkdir(parents=True,exist_ok=True)
def rd(p):return json.loads(p.read_text())
def digest(b):return hashlib.sha256(b).hexdigest()
def canon(x):return json.dumps(x,ensure_ascii=False,sort_keys=True,separators=(',',':')).encode()
base=rd(OUT/'HANDOFF_BASE.json')
cards=rd(ORIG/'ledgers/cards.json');eng=rd(ORIG/'ledgers/repair_cards.json');items=rd(ORIG/'ledgers/acceptance_items.json')
levels=dict(collections.Counter(i['level'] for i in items));assert levels=={'LOCAL':189,'DEVICE':24,'EMU':8,'NATURAL':1}
assert len(cards)==56 and len(eng)==2 and sum(len(c['acceptance']) for c in cards)==194
functional=['title','issue','files','steps','acceptance','rollback']
lineage=[]
for version,name in [('R31.1','STARCORE_R31_1_EXECUTION_20261105'),('R31.2','STARCORE_R31_2_SEQUENTIAL_20261105')]:
    prior=rd(ROOT/'construction/archives'/name/'ledgers/cards.json');current={c['id']:c for c in cards};assert len(prior)==56
    for c in prior:assert all(c.get(k)==current[c['id']].get(k) for k in functional),(version,c['id'])
    lineage.append({'revision':version,'original_cards':56,'functional_fields_exact_match':True,'original_acceptance_texts':194,'source':str((ROOT/'construction/archives'/name).relative_to(ROOT))})
original_ids={c['id']+'#A'+str(i+1) for c in cards for i in range(len(c['acceptance']))}
mapped={i['original_item'] for i in items};assert original_ids<=mapped
original_texts={c['id']+'#A'+str(i+1):s for c in cards for i,s in enumerate(c['acceptance'])}
assert all(digest(original_texts[i['original_item']].encode())==i['original_text_sha256'] for i in items if i['original_item'] in original_texts)
prior3=rd(ROOT/'construction/archives/STARCORE_R31_3_BATCHES_20261105/ledgers/acceptance_items.json')
current_items={i['item']:i for i in items}
assert {i['item'] for i in prior3}==set(current_items)
assert all(all(i.get(k)==current_items[i['item']].get(k) for k in ['text','original_text_sha256','original_item','level']) for i in prior3)
lineage.append({'revision':'R31.3','scope':'inherited original functional cards; 21-to-11 batch renumbering only','current_acceptance_items':222,'acceptance_text_identity_level_exact_match':True,'source':'construction/archives/STARCORE_R31_3_BATCHES_20261105'})
threshold_hash=digest((ORIG/'ledgers/acceptance_config.json').read_bytes())
preservation={'scope':'PLAN_PRESERVATION_ONLY_NOT_IMPLEMENTATION_PASS','lineage':lineage,'original_cards':56,'repair_cards':2,'original_acceptance_texts':194,'current_items':222,'levels':levels,'all_original_acceptance_ids_mapped':True,'acceptance_config_sha256':threshold_hash,'functional_digests':{c['id']:digest(canon({k:c.get(k) for k in functional})) for c in cards}}
legacy=list(csv.DictReader((ORIG/'ledgers/legacy_1832_preservation_map.csv').open(encoding='utf-8-sig')))
assert len(legacy)==1832
preservation['legacy_requirement_preservation']={'rows':1832,'file_sha256':digest((ORIG/'ledgers/legacy_1832_preservation_map.csv').read_bytes()),'mapping_semantics':'EXACT_PATH_OVERLAP_ONLY_NOT_SEMANTIC_ACCEPTANCE','semantic_implementation_closure':'NOT_COMPLETED','requirement_review_is_still_required':True}
(OUT/'PLAN_PRESERVATION_PROOF.json').write_text(json.dumps(preservation,ensure_ascii=False,indent=2)+'\n')
with (OUT/'ORIGINAL_TO_CURRENT_ACCEPTANCE.csv').open('w',newline='',encoding='utf-8-sig') as f:
    w=csv.writer(f);w.writerow(['original_item','current_item','card','K_batch','level','text','original_text_sha256'])
    for i in items:w.writerow([i['original_item'],i['item'],i['card'],i['batch'],i['level'],i['text'],i['original_text_sha256']])
for name in ['acceptance_config.json','acceptance_items.json','cards.json','repair_cards.json','batches.json','legacy_1832_preservation_map.csv']:
    shutil.copyfile(ORIG/'ledgers'/name,OUT/('ORIGINAL_'+name))
R=[]
def card(i,title,batch,old,paths,evidence,steps,tests):
    original=['R31-'+v if not v.startswith('R31-') else v for v in old]
    assert all(v in {c['id'] for c in cards+eng} for v in original),original
    R.append({'id':'UF%02d'%i,'title':title,'batch':batch,'original_cards':original,'files':paths,'evidence':evidence,'implementation':steps,'acceptance':tests,'status':'REOPENED_OR_REVERIFY_NOT_IMPLEMENTED_CLAIM'})
P='app/src/main/java/com/ethk3/novaonepro/'
S=P+'strategyv2/'
card(1,'唯一源码、修订合并、真实完成状态','K01',['B00-C01','B00-C02','B12-C05'],['CURRENT_FINAL_PLAN.md','construction/r31/NEXT_ACTION.json','tools/r31/verify_baseline.py'],
 ['旧入口仍写K02，旧批次收据只证明记录源码的LOCAL层；123路径存在不能证明58卡功能已部署。'],
 ['采用HANDOFF_BASE源码检查点；保留本地/远程未合入工作，不从5237或main覆盖重做。','建立56+2卡、194原验收→222现验收、1832旧要求/299旧卡的保全映射；旧1832账本原先仅路径重叠索引，未完成语义核查，必须继续逐项落实生产能力/验收归属及缺口，不能宣称1832项已实现；31.1/.2/.3/.4功能统一，不复活被替代的前置构建流程。','每项分列历史收据、当前生产入口、当前执行、设备/自然证据；一项辅助测试不能伪装整项运行。'],
 ['机器验证56卡六个功能字段跨修订完全一致；194原验收无孤项；阈值文件字节不变。','故意错源码、dirty、旧APK、旧结果或缺文件时必须拒绝当前PASS。'])
card(2,'业务路径零DDL，迁移只由唯一owner执行','K03',['B02-C01','B02-C02'],[P+'DbStore.java',P+'DatabaseCoordinator.java',P+'LegacySchemaCoordinatorR926.java'],
 ['saveDecisionInternal、saveTeacherMatrixSample、saveLearningCase等仍调用ensureRuntimeExtraTablesOnce，能在业务池同步执行迁移；独立连接只包住显式后台操作。'],
 ['拆分requireReadyReadOnly与advanceMigrationOwned；所有业务/报表/健康读取只能检查，不得建表、加列、补默认值或推进迁移日志。','业务遇缺表/缺列返回有类型的SCHEMA_PENDING并向唯一owner投递幂等请求；选定DDL连接贯穿schema、日志、验证。','扫描所有DDL入口及调用图，登记允许的owner操作；保留唯一写者和提交结果合同。'],
 ['schema缺失/迁移中的每个业务入口执行零DDL、零迁移日志推进、零业务误写。','并发100次请求只保留一个迁移owner；旧owner/fence不能写。'])
card(3,'严格DDL、单元后置条件与失败自动重放','K03',['B02-C02','B12-C01'],[P+'DbStore.java',P+'DbHelper.java',P+'LegacySchemaCoordinatorR926.java',P+'SchemaDomainCoordinatorR926.java'],
 ['ensureLedgerIndexes与execQuiet吞CREATE/INDEX错误；当前列错误审计不能覆盖它们。','MAIN_INDEXES可被写COMPLETED而缺索引，最终验证失败；incompleteRows>0又使completed单元无法重开，形成恢复死结。'],
 ['列出所有生产必需表、列、索引定义、默认值、约束与域归属，逐单元严格验证实际产物后才能COMPLETED。','迁移禁止使用吞异常的execQuiet；失败保留第一条实际异常、SQL单元ID及完整cause chain。','最终校验发现漂移时仅使负责单元失效并重放，不能只无限重试最终验证；已正确完成单元保持进度。','旧READY必须经过当前审查，不能仅凭旧stamp跳过；第一条列/DDL失败即停止本单元后续SQL。'],
 ['逐个注入CREATE/ALTER/INDEX失败：失败必需操作绝不能COMPLETED或READY。','撤销故障后，无人工删journal/清数据，自动恢复READY；原行/列值和保护记录逐行不变。','旧READY且真实缺列/缺索引的旧库可修复；不仅是删除测试收据的空壳fixture。'])
card(4,'真正短迁移切片与关键写者进展','K03',['B02-C01','B03-C01','B05-C09'],[P+'DatabaseCoordinator.java',P+'DbHelper.java',P+'DbStore.java'],
 ['当前RUNTIME_COLUMNS、FIELD、DYNAMIC_WINDOW、BASE、INDEXES仍含多表/多语句扫描；写者deadline只约束获取锁，不约束task.call。'],
 ['持久游标细分至有限DDL单元，避免用“一个表/一个功能组”冒充耗时有界；每单元记录语句数、耗时、锁持有和取消状态。','切片之间释放写者并按原优先级允许当期draw/freeze/settle；大索引不能伪装短单元，须沿原B05-C09安全重建/校验/切换合同设计，保护活动pin、原件和新写入，单独可观测、可中断与可恢复，不把后台工作转到主线程。','维持原P99≤50ms与>200ms缺陷披露要求；不可宣称任何不可分索引天然满足该目标。'],
 ['代表性约670MB有数据旧库下，迁移未结束期间必须观测多次关键写提交，during不得允许为0。','在单元前、部分autocommit DDL后、完成journal前杀进程，重开续做且不重复业务效果。','记录全分布、最坏20次及超标原因；不只展示一秒调度目标或锁获取时间。'])
card(5,'存储普查与准入临界区分离','K04',['B05-C01','B05-C02','B05-C11'],[P+'StorageBudgetR31.java',P+'StorageCensusSU2.java',P+'ReportIoR31.java'],
 ['5秒只限制获取锁；reserve/ensureAdditional/snapshot/recover仍在全局OS锁内遍历目录，无耗时上限。'],
 ['昂贵普查移出临界区，使用generation/fence/作用域核对后提交保守会计；过期、不完整、取消的普查明确重试或拒绝。','控制元数据、bulk workspace、已发布报告分清预算，给必要ACK/取消/恢复元数据预留有界且预先计费的空间，不能无预算旁路。','保留OS租约、跨PID总预算、原子写、有效pin和原文件保护；不要只增timeout。'],
 ['两个真实进程、慢/大目录、并发文件增长、崩溃owner、取消、磁盘不足下会计正确。','报告锁竞争不再使实时写入或报告请求不可恢复；未持锁不能改账本。'])
card(6,'最小请求先持久化，再采可选证据','K04→K10',['B05-C02','B11-C03'],[P+'ReportExportServiceCP4.java',P+'ReportRequestQueueCP12.java',P+'ReportJobJournalCP12.java'],
 ['requestExport仍先采集并写长观察，再写请求/FIFO；超出5秒占锁可再次导致从未入队。','enqueue成功但kick失败仍进入统一requestFailure，造成后台有任务、界面FAILED。'],
 ['先原子持久化有界requestId/模式/身份/时间及可恢复提交意图，再补充可选观察；每个边界可幂等reconcile。','区分未提交、已提交待服务、采集、生成、等待资源、失败、取消；瞬时竞争变成持久等待/重试，服务start失败不等于请求从未提交。','可选观察失败记UNKNOWN/DEP/TIMEOUT，不虚构值；永久I/O故障有真实终态和可导出的恢复原因。'],
 ['在请求意图、请求文件、FIFO、kick前后逐点杀进程，重启不丢请求、不重复job。','存储锁被占超过5秒、FGS启动暂拒、观察器异常仍能恢复生成或明确失败，不停在假排队。'])
card(7,'ACK outbox、READY重连与同一报告恢复','K04→K10',['B11-C03','B12-C01'],[P+'ReportJobJournalCP12.java',P+'ReportExportServiceCP4.java',P+'CompleteReportFileR927.java'],
 ['transition先存READY再写ACK；两次ACK失败后FIFO已出队，服务终态+空队列直接返回，UI又不kick READY，只等ACK。'],
 ['为ACK建立持久待发状态/单调revision，并在启动与drain中独立于FIFO修复；不依赖仅16条的进程内失败缓存。','取回已READY任务直接验证权威job、manifest、ZIP后恢复分享/保存，缺ACK不应永久遮蔽有效产物。','UI依据requestId/jobId/revision显示真实阶段、阻塞原因、最后进展和可执行恢复，不错误触发重做快照。'],
 ['故意让READY ACK及重试都失败，杀进程重开后恢复同一jobId、同一ZIP hash，备份次数不增加。','旧ACK不能覆盖更新状态；进程内缓存清空后恢复仍成立。'])
card(8,'发布可分享的原子边界与租约竞争','K04→K10',['B05-C02','B11-C03'],[P+'ReportExportServiceCP4.java',P+'ReportPublicationCP12.java',P+'ReportIoR31.java'],
 ['READY ACK发生在workspace目录reservation释放前；UI grant独立预约job/share-until.json会与目录scope冲突。'],
 ['定义验证完成、stream/reader关闭、workspace owner释放、发布READY/ACK和分享控制元数据的明确顺序。','或采用经授权的独立发布后控制作用域；保留预算、文件hash、发布不可变性与reader pin，不靠跳过准入解决。'],
 ['在READY ACK后刻意暂停owner，立即执行UI grant及Provider读取/保存，必须成功或持久等待后自动成功。','并发取消、删除、分享、重启不损坏已验证ZIP与真实数据库。'])
card(9,'证据生命周期生产接线、孤儿恢复与有界清理','K04/K09',['B05-C03','B05-C04','B05-C05','B05-C06','B05-C07','B05-C08','B05-C10','B05-C11','B12-C01'],[P+'ReportJobJournalCP12.java',P+'ReportBudgetSU2.java',P+'ReportPublicationCP12.java',P+'CompleteReportFileR927.java'],
 ['无关缺失/损坏job.json可使latestFullReport整次失败；先删ACK后遇读者租约会让遗留目录以后永远无法回收。','旧可选观察/快检使用cached线程池；当前有界原型未做忽略中断的超时长期测试。'],
 ['保留可疑原件，以类型化警告隔离孤儿/坏job，继续选择有效报告。','逐项把keep迁移、引用闭包GC、CAS陈旧页恢复、事件有界聚合及旧库重建接入原合同允许的实际owner/显式维护入口；静态未见调用的工具类不能算完成。必须先完成P05权威迁移再做P06清理，保留原件、活跃pin、迁移收据和新写入。','保留可恢复cleanup tombstone，直到所有生成目录真实退役；读者关闭后续清理必须能发现它。','报告观察器/快检工作者及队列有上界，超时不退出的读者继续占槽，容量不足记未知而非无限开线程；取消不得吞掉。'],
 ['有效READY旁放空目录、坏retention job和不可读无关journal，仍能取回有效报告。','持有读者跨过过期时间，清理不删原件；读者释放后下一轮能完整清理。','读者忽略中断并超过timeout，重复请求后线程/队列仍有界，完整报告不会被可选观察永久阻断。'])
card(10,'R31指标、对象状态和真实跨源冲突进入生产报告','K10',['B11-C01','B11-C02','B11-C04'],[S+'ReportR27.java',S+'ReportR29.java',S+'ReportSnapshotR30.java',S+'MetricEvaluatorR29.java',S+'ReportObjectIndexR30.java',S+'CheckMetricCrosswalkR31.java'],
 ['生产R30Input仍调用evaluateR30；evaluateR31/crossSourceR31主要为测试调用，M32固定0/44。','ReportSnapshot仍用旧build，新的六层对象状态辅助入口未接真实生产采集。'],
 ['生产快照内接R31定义、collector、crosswalk及对象原六状态：DEFINED/IMPLEMENTED/RAN/PRODUCED/CONSUMED/EFFECTIVE（定义/实现/实际运行提交/产出/消费/有效）；输入与部署另列附加字段；每项绑定身份、role、scope、水位、时间、原件。','M32比较真实同scope/同水位事实；不同role/epoch区分不可比与矛盾；缺源为UNKNOWN，不能固定0。','单一事实快照派生JSON、HTML/Markdown和图表；44指标、96检查、47图、20矩阵、16表及能力集合逐项完整解释。'],
 ['通过ReportR29.fromSnapshotR30到document到真实ZIP验证，不只调用helper。','注入真实跨源矛盾、合法不同scope、零样本、缺源和有效样本；最终打包报告正确反映状态/分母/证据，不宣称全部健康。'])
card(11,'完整报告语义、附件闭包及保存回读','K10',['B11-C03','B09-C04'],[P+'ReportExportServiceCP4.java',P+'ReportArchiveVerifierCP12.java',P+'ReportPublicationCP12.java',P+'DiagnosticReportProviderR923.java',P+'CompleteReportFileR927.java'],
 ['现有新增联测主要验证READY、Provider字节hash；没有验证ZIP内完整章节、对象、数据契约与可复算原件闭包。'],
 ['生成同快照JSON与人读报告，逐项检查12章、44/96及全部对象、附件、失败/未知原因、修复卡；含运行、训练、家族、进化、模型资格与实盘状态。','验证ZIP解压/CRC、分卷重建hash、大小、路径、索引和必须成员；预算不足列出精确缺失，不伪装完整。','将生成、发布、分享Provider、SAF文件/目录保存、保存后回读分别验收；恢复诊断包明确不是完整报告。'],
 ['缺关键章/损坏附件/错水位/错jobId/同名旧报告必须被最终验收抓到。','真实Service生成后，经Provider与SAF落文件回读，hash和报告语义一致；证据完整、检查执行完整、业务健康三项独立。'])
card(12,'抓取—下一期冻结—结算—训练事件闭环','K04/K05',['B03-C01','B03-C02','B03-C03','B06-C01','B06-C02','B06-C03'],[P+'AutoFetchService.java',P+'AnalysisRequestStoreR36.java',P+'CanonicalObservationWriterR36.java',P+'SettlementTaskStoreR36.java',P+'OosAfterDrawWriterR36.java',S+'RuntimeCoordinatorR26.java'],
 ['快检有抓取成功样本但业务等待canonical，分析/结算显示旧时间；当前进程在本期开奖后启动，该期缺冻结不能反向伪造。'],
 ['每期跟踪HTTP/解析/draw_commit/next_freeze_commit/settle_commit/label/feedback/research_commit各独立时间和水位，旧回补不挡当期。','唯一赛前冻结、同owner deadline不刷新、迟到结果只入研究；合法NO_BET说明原因但不能冒充有K3预测。','结算outbox幂等，训练标签与研究反馈不因无REAL/无冻结永久饥饿；历史回放与模型状态共同提交，5000期滚动及pin保护。','倒计时依据单调时钟投影与合法开奖时间；数据陈旧与倒计时单独显示。'],
 ['真实生产服务观测新抓取、新未来冻结、随后对应真实开奖结算、标签反馈及研究提交，不能只看心跳。','错序/重复/修正/缺号/网络超时/重启/回补下无重复结算、无未来泄漏。','原P95≤1秒结算、≤5秒冻结按真实子阶段分布验收，源晚到单列。'])
card(13,'研究进展、可执行失败假设与regime接入','K06',['B04-C01','B07-C06'],[S+'RuntimeCoordinatorR26.java',S+'FailureDirectedResearchR26.java',S+'ResearchHypothesisR31.java',S+'RegimeHypothesisR31.java',S+'EvolutionLoopR27.java'],
 ['FailureDirectedResearch仍先用F08/F17选子代，再追加假设metadata，component为空；dispatch/recordOutcome及Regime辅助逻辑未发现生产调用。'],
 ['按真实失败证据在birth之前路由typed可执行adapter；有component证据才使用component假设；不支持的机制明确UNSUPPORTED。','连接实际Regime检测/窗口/路由/结构构建并持久化状态，保留原父代对照、fence、游标事务、家族与专家能力。','研究健康用已提交cursor/产物/反馈衡量，attempt、RUNNING、owner代数不当成进展。','子代独立训练/选择/确认/未来证据；outcome credit幂等写入，不继承父证书。'],
 ['六种受控失败经真实select→birth→recordWithin产生对应AST/谱系或UNSUPPORTED；不能只比dispatch字符串。','kill/restart后cursor、credit、产物一致；空转线程不能显示研究推进。'])
card(14,'完整创造工厂实验而非四程序演示','K06',['B07-C01','B07-C02','B07-C03','B07-C04','B07-C05'],[S+'ResearchCapabilityExperimentR31.java',S+'EvolutionLoopR27.java',S+'BirthGateR27.java',S+'FamilyRegistryR26.java'],
 ['当前candidates固定4程序，PRODUCTION_STAGES没有驱动实际family/expert/proposal/进化编排，不能证明完整工厂能力。'],
 ['保留四程序测试为内核测试；另建驱动真实工厂的可复现adapter，使用隔离SYNTHETIC命名空间、预算、seed与真实搜索路径。','记录所有候选/淘汰/失败、搜索暴露、训练—选择—确认—资格门及成本；失败试验不能标COMPLETE。','保留所有有效家族、typed AST、变异/融合/淘汰和父子比较；历史快速训练筛劣继续可用。'],
 ['null、注入信号/禁用信号、漂移、缺口实验经过完整工厂，给出trial级不确定性及完整搜索分母。','合成检出力不计自然胜率，不保证每代或每次变异必然更强。'])
card(15,'图表新鲜度、ABI、取消和健康口径','K09',['B09-C01','B09-C02','B09-C03','B09-C04','B09-C05'],[P+'ChartsRepositoryR27.java',S+'ChartCacheR27.java',P+'ChartRegistryR31.java'],
 ['缓存READY直接透传；updated_at仅展示，points列表被当ABI；全快照缺普通panel字节上限；取消被RuntimeException吞掉。','每次缓存刷新又完整capture报告，健康文案仍用旧CPU/代数指标。'],
 ['读取前检查字节、schema/ABI、hash、source watermark；根据时效独立判STALE/UNKNOWN，不能信缓存READY。','各panel错误有类型、相互隔离；取消向上传播；缓存由已有事实快照投影，避免图刷新触发完整报告。','分别显示服务活着、研究提交推进、资格/自然质量，47稳定ID与全部功能保留。'],
 ['陈旧合法READY、错ABI/hash、超大快照、取消、单图损坏均经真实repository/load/render验证。','线程RUNNING但无commit不得显示研究健康；一个坏图不拖垮其他面板。'])
card(16,'16逻辑表实际读者与T09对象数值投影','K07',['B08-C01','B08-C02','B08-C03','B08-C04','B08-C05'],[S+'ResearchTableAdaptersR30R1.java',S+'ResearchMeasureProjectionR31.java',S+'ReportSnapshotR30.java'],
 ['T09仍读顶层mean_gain，产品原件可能为null；正确frame.points投影及page接口主要是测试调用；导出SQL合同不等于表格能用。'],
 ['接入有界、只读的真实产品读者/渲染及报告路径，执行16个逻辑查询、筛选与分页。','T09按对象展示gain、区间、样本、node cost和payload hash；CPU未知保持未知，role/dataset/source过滤严格。','矩阵产物的生产、消费与实验效果各有回执，不把统计表行数当学习。'],
 ['顶层mean_gain=null但frame.points有值时，真实界面/报告呈现正确对象值。','16视图全过当前schema、分页、缺源、缺产品、错role/source和只读不改业务库测试。'])
card(17,'主链资格、真实选择与撤销闭环','K08',['B10-C01','B10-C02','B10-C03'],[S+'MainlineChoiceStoreR31.java',S+'QualificationWindowR26.java',S+'CertificateRevocationR30.java',P+'DecisionGateEngine.java'],
 ['新快检没有足够新冻结/结算/未来样本，不能据此判定当前资格与高胜率；不能用工程修复强行主链非零。'],
 ['原数值门槛字节不变，typed权威选择、缓存投影、唯一DecisionGate及撤销/过期生效期闭环。','达标策略真实进入主链且可追溯；不达标如实显示原因和缺证据，SHADOW/合成/回放不计REAL。','保留全部有效历史训练和高质量策略资产，GC按引用闭包，不为清理删除活跃证据。'],
 ['合法资格正例可走真实Decision/Trade/结算路径；过期/撤销/篡改/旧build/旧source反例全拒绝。','生产实际执行typed切换与撤销，默认LEGACY_READ不能靠测试单独调用switch算迁移完成；新资格风险合同须接真实注册/校验/激活编排。','K3单位置、无马丁/追期/加倍、PIT、原收益与主链红线负例持续通过。'])
card(18,'统一CI、同次运行结果与低消耗施工','K01基础/K11总验',['B00-C01','B12-C02','B12-C04','B12-C05'],['.github/workflows/r31-gate.yml','.github/workflows/r31-verification.yml','.github/workflows/r31-build-r27.yml','tools/r31/verification_receipt.py','construction/r31/HOST_SUITE.json'],
 ['post-K11 ||true及复用摘要仍可能把同SHA旧绿结果算新运行；只比较source_head不足。','新锁/Android34测试已加入部分签名路径，但未统一注册到完整HOST/Android验收集合。'],
 ['每次run创建唯一invocationId、清理/归档旧摘要，验证进程exit、source/tree/控制输入、runId、开始时间、实跑任务与完整项集合。','复用原close的编译/测试数/跳过/覆盖/已知失败清偿验证器，post-K11不能仅检查0失败。','新锁测试和真实Android34报告/实时链测试进入统一注册集合；API29/35原矩阵继续；过期重复历史门单独标历史，不拿其错误代替当前结论。','检查工具链一次；缺SDK则使用已能运行的CI，无代理盲重试。先改业务代码、针对性测试，批末一次全量；同源码同失败不重复烧额度。','签名构建工作树必须干净，测试输出放临时目录；最后用真实编译classes重冻调用索引，再正式构建。'],
 ['同SHA上次绿、本次前置失败或无JUnit/跳过/漏suite时，必须红且不能消费旧摘要。','原整文件保护和窄schema块复核负例保持；不能批量改期望hash掩盖业务变化。','每份APK签名与源码身份和同次CI记录互证，旧APK改名/重压缩不算新交付。'])
card(19,'旧库、后台、故障与真机时长分层','K11',['B04-C02','B05-C09','B12-C01','B12-C02','B12-C03'],['tools/r31/collect_device_receipts.py','tools/r31/accept_device_receipts.py','tools/r31/run_r31q_android_delivery.sh'],
 ['用户670MB旧库未取得，快检只是一瞬间；当前测试允许during=0且没有真正进程杀重启，不能证明旧库恢复。'],
 ['代表性有数据旧库、真实迁移并发业务、报告生成/取消/分享/保存同时测试，原数据逐行对照。','杀主/诊断进程、磁盘满、错序开奖、超时、缓存坏、时钟回拨、系统回收逐点注入；保留所有失败，不让前置失败遮住后续场景。','正常系统条件下前后台/熄屏实测；force-stop未知区间如实记，不保证平台不允许的自动复活。','DEVICE24项和24/72小时无设备则NOT_RUN并给明确采集协议，继续源码工作，不能借缺设备停工或伪记PASS。'],
 ['API29/34/35各自输出当前源码真实结果；原机验证另列。','完整报告导出后能回答真实进展、性能分布、研究状态和剩余失败；无新样本不显示虚假胜率/P95。'])
card(20,'同签名成品APK与可核对交付','K11',['B12-C05'],['release/release_manifest.json','tools/r31/package_release.py','release/R31_DELIVERY.json'],
 ['当前只有未完成检查点和历史APK声明，没有可以认定本轮全部验收通过的成品。'],
 ['完成本轮所有工程闭环后，在单一干净提交冻结身份和实际编译调用清单，再构建release APK。','保持包com.starcore.research.r13、既有d8d8cee7…证书，核对现机版本后递增versionCode，覆盖安装保留数据。','交付release.apk、可直接解压ZIP、完整源码归档或精确仓库SHA、方案、diff、资产、公开签名指纹、SHA256及完整验收结果；不公开密钥或凭证。','提供用户能下载的文件，不只给Actions工件页；同时准确声明设备/自然证据边界。'],
 ['安装/覆盖/启动/核心闭环/完整报告保存实跑；包、签名、ABI、版本、嵌入source和外部receipt一致。','未完成或失败项逐项列出；不能把“方案写完/源码提交/测试helper通过”写成“软件全部完美完成”。'])
assert len(R)==20
R[3]['files'] += [P+'StorageCheckpointR31.java']
R[3]['implementation'] += ['审计实际所有WAL checkpoint入口；StorageCheckpointR31静态未见生产调用，不能以策略helper替代真实入口的mode、deadline、结果与业务延迟验收。']
R[8]['files'] += [P+'EvidenceReachabilityR31.java',P+'StorageGcR31.java',S+'KeepMigrationR31.java',S+'ResearchRetentionStoreR30.java']
R[8]['evidence'] += ['KeepMigration/引用闭包GC/事件有界等新接口有实现，但静态未见完整生产Store、root/edge/pin与append接线；CAS缩页和部分revision/cache保护已有真实调用，须保留而非整体重写。']
R[12]['files'] += [S+'ResearchPoolR27.java']
R[12]['implementation'] += ['recoverOneR31的恢复控制异常必须分类并独立记录原因/次数，不得吞RuntimeException后显示健康；保留已有生产owner/fence、隔离、历史训练及AtomNet消费保护。']
R[13]['implementation'] += ['家族kernel、Foundry遍历、AST出生门、CreationBudget和FrozenConfirmation已有生产实现，先核对新接口与这些实现的等价合同；FamilyRegistry新接口或VerifiedImprovement helper未调用既不能算新合同完成，也不能误判原创造能力全不存在；以同机会父子、冻结确认、未来证据的生产回执关门。']
R[15]['files'] += [S+'MatrixResearchR30R1.java',S+'ResearchContextR30R1.java',S+'ResearchStudyTaskR30R1.java',S+'ResearchResultContractR31.java',S+'ResearchObservatoryR30R1.java']
R[15]['evidence'] += ['矩阵按kind状态/依赖及source→proposal→BORN同事务消费已有真实实现；新usages/withoutSourceRun及统一validateForConsumption/数值效果合同缺生产调用；部分family/selector/calibrator为UNSUPPORTED，未见fusion adapter，不能仅以通用helper通过关门。']
R[15]['implementation'] += ['将统一结果校验、真实消费效果/去源对照、完整source身份贯穿实际矩阵/研究/提案/出生/报告；不能仅输出Spec元数据。','按原卡落实family、selector、calibrator、fusion等应支持的真实适配；原要求未实现时UNSUPPORTED必须记未完成。缺学生/rt.evo按实际依赖kind隔离等待，已有可运行kind继续真实提交。']
R[16]['files'] += [S+'EvidenceContractR31.java',S+'LiveShadowsR27.java']
R[16]['evidence'] += ['typed双写及授权读取确已接入，但switchToTypedRead/revoke未见生产调用，默认LEGACY_READ；model_state_hash当前传semantic ABI hash，不是实际模型状态发布回执；新风险合同及机会分母仍缺生产编排。']
R[16]['implementation'] += ['落实原子双写→逐行对账→受控typed切换→启动恢复→真实事件撤销和资格过期的完整链；冻结绑定实际model publication receipt/state hash、choice、contract，ABI身份与模型状态不可混用。','实现新风险合同真实预注册/持久化/工厂验证/激活及eligible/ACT/ABSTAIN/MISSED_INPUT/MISSED_DEADLINE分母；未经验证的新合同不授权。']
R[18]['implementation'] += ['旧库恢复证明必须有真实采集/重建/验证/recordRestoreProofR31调用与回执，不以证明API存在替代旧库数据保全、恢复与备用文件删除验收。']
R[17]['source_file_branches']={'.github/workflows/r31-build-r27.yml':'construction/r27-20261028'}
(OUT/'REPAIR_CARDS_20.json').write_text(json.dumps(R,ensure_ascii=False,indent=2)+'\n')
summary='''# STARCORE R31.1—R31.4 统一最终施工方案

日期：2026-11-12（北京时间）。性质：同一R31方案的全量保全、真实源码返修及生产调用闭环实施合同。不是新起一套缩水方案，不宣称施工或APK已经完成。

## 1. 核对结论与统一规则

已逐项机器比较R31.1、R31.2和当前R31.4的56张原卡：title、issue、files、steps、acceptance、rollback完全相同；194条原验收文本全部保留。当前另有ENG-01/02两张真实修复卡，验收分层后为222项：LOCAL189、EMU8、DEVICE24、NATURAL1。PLAN_PRESERVATION_PROOF.json及ORIGINAL_TO_CURRENT_ACCEPTANCE.csv给出完整证据。文本保全通过不是源码实现通过。

31.1/.2/.3/.4主要是施工控制的连续修订：31.1纠偏接续，31.2顺序批与分层，31.3改为先施工后批末棘轮，31.4把21批合并为11批。31.2的三张临时工作流卡不再独立成卡，其CI/HOST、在制品身份和验收目的并入UF01/UF18；不能把失效的“开工前先全绿”再叠回流程。

优先级：最新用户要求 → 全量功能/数据/阈值保护合同 → 17分册的批次编号覆盖 → 16分册执行规则 → 14/15分册历史与未被替代目的。历史收据只是带源码身份的证据，不能取消需求。原acceptance_config.json不得改字节；功能冲突先列事实、采用依据、兼容方案与验证，不静默删功能。

统一底本为original_R31_4完整原包（打包时原样附带），保留全部56+2卡、47图、20矩阵、16逻辑表、44指标/96检查、40合并要求、1832旧要求/299旧卡、190个精确源码锚点与专题10—13。新20返修卡只作为原功能卡的实施补缺覆盖层，不替代这些原文。

## 2. 当前到底完成了什么

已做：快检与对应源码定位、跨版本功能保全核对、历史收据核对、关键生产链路审阅、部分运行修复原型、一次真实主机OS锁回归。未做成：当前版本全套工程通过、完整生产报告语义通过、原机670MB旧库恢复、24/72小时、自然优势证明。应用Java共1278文件，本轮聚焦123个原卡路径及实际关键调用链；不声称逐行穷尽审完1278文件或所有缺陷。

源码审阅已确认部分真实能力存在：ResearchPool的故障分类、owner/fence提交、隔离与半开恢复，进程内health的生产查询，以及历史训练与AtomNet部分发布/消费保护。它们要按原卡保留并在当前整链重验，不能因其他模块缺陷统称全空壳。相反，类和helper虽存在但生产入口仍旧或缺调用的项目，要明确作为未完成接线返修。

HANDOFF_BASE.json固定已保存源码检查点。检查点包含未验收改动，旧库切片和新报告辅助方法仍有明确阻塞；不能直接编包称成品。原APK577311020为本轮输入，577311021只是未交付候选身份。Codex必须检查最新分支差异，保留工作后继续，不从旧main或5237重做。

## 3. 快检的正确解释

- RUNNING/READY只证明进程与部分初始化，不证明抓取、预测、结算、训练正常。
- 本次报告request已FAILED且active/queued=0，失败发生在入队前；不是一直有后台队列等着。两次ACK也被同一锁拒绝。
- 同时有迁移ALTER及多线程等待SQLite连接，证明该瞬间数据库竞争；单次堆栈不能独自判永久死锁。
- 本次启动在目标期开奖之后，不能补造该期赛前预测。后续未来期必须证明恢复。
- uptime是设备单调时间，不是应用连续运行时长；crashCount=0不能抹去系统退出记录。job.lock存在/很旧不证明OS锁被占，禁止按mtime删锁。
- 剩余磁盘充足不等于预算协调锁可用。UNKNOWN、-1和无样本不写成0、胜率或满足P95。

## 4. 按31.4的11批落实，重开的是验收，保留的是功能

K01：全量保全、身份、真实状态与CI基础（UF01、UF18基础）。K02：原owner/fence、故障分类、内存与队列能力全保留并复验。K03：业务零DDL、严格迁移与短写者（UF02—04）。K04：真实抓取/冻结/结算、研究健康、预算；报告持久请求/ACK/发布阻塞在本批先修通（UF05—08、UF12；属必需前置依赖，登记scope_extensions，不等K10才恢复可诊断性）。K05：原历史回放/模型状态共同提交及PIT复验。K06：原全部家族、typed AST、创造、父子比较、完整工厂与真实失败假设接线（UF13—14）。K07：原矩阵依赖/融合和16表真实读者（UF16）。K08：原主链权威、资格和撤销（UF17）。K09：原引用闭包GC/CAS/WAL/缓存迁移及47图（UF09、UF15），必须保留原前置主链迁移顺序。K10：完整R31生产报告、全部对象与保存回读（UF10—11；UF06—09重新联合验收）。K11：原故障矩阵、完整当前CI、旧库/设备层与成品交付（UF18最终、UF19—20）。

旧K01—K11收据不覆盖、不改成新源码PASS。新执行收据放execution/Kxx/，引用原卡/222项和本轮UF项。每批报告真实源码diff、通过/失败、未完成与下一动作。不存在“先把所有功能都修好才准改第一批代码”的前置门。

## 5. 每张原卡的真正完成定义

每张卡必须有需求→实际生产入口→生产者→持久产物→消费者→反馈/撤销→检测字段→测试原件链。新增类、辅助函数、表/图存在、单个helper测试、线程RUNNING、代数增加均不能独自关闭应用功能。未部署、缺输入、缺产物、未消费、无效果与证据不足必须分别表示。

新helper静态未见调用时，先检查生产中是否已有等价实现；完整等价需逐条证明并绑定实际集成验收，避免重复造第三套状态。已有实现继续使用和完善，确实缺少的新合同或功能必须接线实施，不能只加说明、计数器或UNSUPPORTED文案代替。

原189项LOCAL仍逐项绑定；需要真实生产路径的验收不得绑定helper-only测试冒充。每卡保留实现、静态接线、集成执行、Android、原机及自然质量不同状态。新失败归属明确；本批新增失败必须修，旧失败先登记责任但不得无限结转；最终工程发布前原LOCAL及本轮工程检查清零，测试数不减、skip不增、编译失败阻断。

不因增加类别、数量或复杂度就认定算法更强。保留有效的历史训练快速筛劣、完整家族/专家、每期标签/反馈、动态进化/变异/融合、严格子代独立证据；合成实验与历史分数不转成未来/REAL胜率。达标策略可进入唯一主链，不达标保持理由，不强行生成号码/放低资格凑非0。阈值以原文件为准。

## 6. 完整检测报告必须能回答的问题

同一水位事实快照生成全部机器/人读产物。逐项回答：当前版本与签名；服务活着及业务推进；各阶段最近真实commit/时延分布；每期缺号/缺冻结/结算尾部；训练/选择/确认/未来各自样本；各家族/专家/策略/资产/模型状态；创造与失败反馈的真实路由；父子比较和资格/撤销；47图/20矩阵/16表每项生产、消费、效用与缺失；44指标与96检查的定义、输入、分母、scope、状态、证据；当前胜率是否有充分独立证据；每项不足对应源码/修复卡与可执行下一步。

文件生成成功、证据结构完整、检查都执行、功能健康、自然质量达标分开报告。M32不得固定0；没有输入就UNKNOWN。恢复诊断包不是完整报告。报告链要覆盖请求持久化、服务启动、快照、生成、打包、发布、ACK、UI取回、Provider、SAF、读回、取消与重启；每个断点有持久恢复策略。

## 7. 验证与交付

先局部真实故障复现和代码修复，再跑所属整类；每批末在稳定源码跑全量。无新修改/新失败不反复跑相同CI。当前工具链检查一次；本地缺SDK不冒称无法施工，使用既有签名/构建lane；不盲目重试代理。正式构建不能跳过身份门，调用索引须来自最终真实编译classes。

必须包含：严格DDL故障/重放、迁移期间关键写入且during>0、真实kill/restart、>5秒跨进程锁、提交与kick边界、ACK双失败重启、READY时grant竞争、孤儿job、读者跨清理期、R31生产报告语义、真实typed子代与完整工厂实验、实际47图/16表读者、真实API新抓取—赛前冻结—其后结算—训练反馈、原故障矩阵及原主链红线。

最终单一干净提交构建同包名同证书的递增版本release APK，并交付APK+ZIP+源码与资产+公开签名信息+SHA256+逐项验收回执。没有原机时继续工程工作，可以交付明确标注的工程候选，但不能宣称V2353A旧库、24/72小时或自然优势全部通过。平台force-stop限制及未知运行间隔如实呈现。任何“完美完成”须由原项目项和本轮故障验收共同支持，不能靠文档承诺。
'''
details=[summary,'\n## 8. 二十张真实返修项目卡\n']
for r in R:
    details += ['\n### '+r['id']+' '+r['title']+'\n','原批次：'+r['batch']+'；回开原卡：'+', '.join(r['original_cards'])+'。\n',
                '\n**源码文件**\n\n'+'\n'.join('- '+p for p in r['files'])+'\n',
                '\n**已定位事实/缺口**\n\n'+'\n'.join('- '+v for v in r['evidence'])+'\n',
                '\n**必须真实改代码**\n\n'+'\n'.join(str(i+1)+'. '+v for i,v in enumerate(r['implementation']))+'\n',
                '\n**关门验收**\n\n'+'\n'.join('- '+v for v in r['acceptance'])+'\n']
status_path=OUT/'CURRENT_SOURCE_STATUS_58.md'
if status_path.exists():details+=['\n## 9. 最新源码逐卡落实情况与未闭环清单\n',status_path.read_text()]
text='\n'.join(details)
(OUT/'FINAL_CONSTRUCTION_PLAN.md').write_text(text)
prompt='''请直接施工 STARCORE R31 统一最终方案，完成真实Android Java/SQL及资源代码修改、编写、替换、生产调用集成、故障修复、验证和同签名成品APK交付。不要只分析、写文档、做helper示例、登记完成或改版本号。

仓库：https://github.com/gtt46461-star/starcore-public-build
唯一应用源码分支：construction/r31-final-20261104。main只作索引，不得拿旧main覆盖最新应用。
统一入口：construction/repairs/STARCORE_R31_UNIFIED_FINAL_20261112/FINAL_CONSTRUCTION_PLAN.md
同时读取该目录 HANDOFF_BASE.json、CURRENT_SOURCE_STATUS_58.md、CURRENT_SOURCE_STATUS_58.json、REPAIR_CARDS_20.json、PLAN_PRESERVATION_PROOF.json、ORIGINAL_TO_CURRENT_ACCEPTANCE.csv 和完整原31.4目录 construction/STARCORE_R31_FINAL_20261104/（尤其专题10—13、docs16/17及原56+2卡）。逐卡现状表中的未核实项必须继续读实际生产调用并执行，不能默认为完成；表内已定位缺陷须修复后联测。

R31.1、31.2、31.3、31.4是同一累计功能方案。56原卡、194原验收文本、ENG-01/02、222现验收（LOCAL189/EMU8/DEVICE24/NATURAL1）、全部数值门槛和资产保护要求完整保留；不能只做20张新增返修卡而丢原功能。执行采用31.4的K01—K11布局和31.3“先改代码、批末棘轮”，不再恢复31.2开工前先全绿的死循环。

先核对HEAD、git status、原机APK身份和检查点差异，保留所有已做工作；检查点含未验收原型，不能当成品。不得硬reset旧基线，不盲目套旧行号，不照搬源码快照覆盖现工程。按函数/调用链定位，以当前生产路径为准。先把已有失败和实际阻塞登记责任，立即修真实代码，不先反复全量构建、重写新方案或反复审计整包。

必须优先解决这些已定位缺口：
1. 业务仍能同步跑DDL；迁移组不是真正有界；CREATE/INDEX吞异常可假COMPLETED/READY，或让最终校验永久重试。改为唯一迁移owner、业务只读就绪检查、严格DDL+每单元实际后置条件、负责单元自动失效重放，旧READY缺列/索引要能修，切片之间真实关键写提交。
2. 报告仍先采长观察后入队；enqueue与kick失败状态混淆；ACK丢失后READY报告不可取回；READY曝光与workspace reservation冲突。实现最小请求先持久化、资源等待/重试、独立ACK outbox和启动reconcile、正确发布/租约顺序、孤儿目录隔离及清理墓碑。锁忙不能永远假排队，也不能仅改失败文案。
3. 生产报告仍走旧evaluateR30，M32固定0，新R31指标/对象状态未接真实采集。必须接到ReportR29实际输出与最终ZIP，完整逐项展示44指标、96检查、47图、20矩阵、16表及能力状态、分母、水位、原件、缺口和修复卡；未知不能填0，导出成功不能当全部健康。
4. 失败假设只贴metadata、仍按固定F08/F17生子；Regime/dispatch/credit未进生产；工厂实验只测四个固定程序。把真实失败路由、机制适配、谱系、反馈credit接入实际工厂与事务；不支持的机制明确UNSUPPORTED；完整工厂实验通过真实家族/专家/提案/训练/选择/确认/资格编排，保留原有有效能力。
5. 图表缓存READY缺新鲜度/ABI/hash检查，取消被吞，刷新又capture全报告；16表分页/T09正确对象投影未进真实读者。接好真实生产/消费与渲染，保持有界、只读、缺源/陈旧/错误分开。
6. 真实启用生产服务，验证新抓取→下一期赛前冻结→对应开奖结算→标签/反馈→研究提交。不得用心跳、attempt、代数或NO_BET终端覆盖率代替真实预测/研究进展，不补造过去预测。

每张原卡必须记录：实际生产入口、产物、消费者、持久状态、反馈/撤销、最终报告字段和同源码集成测试。helper-only测试不能关闭生产功能。K01—K11历史收据保持原身份，新返修证据独立记录；不把旧绿结果当当前PASS。

保持PIT、唯一canonical、单位置固定K3、唯一DecisionGate、typed资格/撤销、SHADOW不计REAL、原收益及主链字节红线、5000期滚动及活跃pin。不得降低门槛、删家族/有效功能、随机凑号码、强行主链非0、用合成/回放胜率冒充自然质量。schema变化只按明确窄块复核，保持原整文件红线与篡改负例，不能批量改断言/hash掩盖业务变化。

先针对性修红，再在稳定源码批末全量，测试数不减、skip不增、编译失败阻断、最终已知工程失败清零。CI统一注册新测试，清旧摘要并绑定source/tree/输入hash/runId/invocationId/exit与实跑集合；同SHA旧PASS不得给本次早期失败兜底。测试输出放临时目录，正式构建工作树干净。无SDK使用既有CI；检查环境一次，不盲重试网络。同源码同失败不要重复烧额度。

本次适用工程验收通过后（DEVICE/NATURAL保持独立真实状态，缺真机仅可标注为工程候选，不得宣称整机验收全部完成），重冻最终实际编译调用索引、发布身份和签名，核对现机版本后提高versionCode，保持com.starcore.research.r13和现机同证书（检查点指纹在HANDOFF_BASE），生成可覆盖安装、保留数据库的release APK。交付可直接下载的APK与ZIP、完整源码/资产或精确仓库SHA、公开签名指纹、SHA256、逐项验收与失败清单。不要只给Actions页面，不得旧APK改名重打包。

必须执行迁移真kill/restart、期间关键写提交、报告>5秒锁/ACK双失败/READY授权竞争/孤儿与清理租约、实际报告ZIP语义与SAF回读、真实子代与完整工厂、47图/16表实际读者及原故障矩阵。DEVICE与24/72小时无证据写NOT_RUN，NATURAL写NOT_PROVEN；继续工程施工，但不能伪称原机、后台连续运行或高胜率已证明。

按批持续完成，阶段汇报只说明真实源码差异、验证结果、剩余阻塞和下一动作。不要停在“可以继续/建议这样改”，不要把方案保全、源码存在或测试辅助方法通过写成整机全部完成。
'''
(OUT/'CODEX_CONSTRUCTION_PROMPT.txt').write_text(prompt)
intro='''# 唯一阅读入口

本包将R31.1/.2/.3/.4统合为同一完整功能方案，沿用31.4的11批次并补齐真实运行/报告/研究/图表接线缺口。

1. FINAL_CONSTRUCTION_PLAN.md：完整统一施工合同和20张代码返修卡。
2. CODEX_CONSTRUCTION_PROMPT.txt：可直接复制给Codex。
3. PLAN_PRESERVATION_PROOF.json与ORIGINAL_TO_CURRENT_ACCEPTANCE.csv：56卡/194原验收及222现验收无遗漏的机器证据。
4. HANDOFF_BASE.json：已保存源码检查点、版本、签名与未完成边界。
5. 原方案全文在ZIP的original_R31_4/；修订控制原文在revision_history/。仓库中它们保持原路径，不重复覆盖封存包。
6. CURRENT_SOURCE_STATUS_58.md、CURRENT_SOURCE_STATUS_58.json：最新版58卡逐项历史声明、源码实现事实、未闭环缺口与返修去向。未深入核实项明确保留，不能假记完成。

方案已完成统合，不代表软件施工已完成。当前检查点不是合格APK；必须按原所有功能卡及本轮返修卡真实实施与验证。
'''
(OUT/'README.md').write_text(intro)
# Include the source-bound broad inventory as a secondary audit, never as completion evidence.
for name in ['SCHEMA_BLOCK_REVIEW.json']:
    p=ROOT/'construction/r31q/021'/name
    if p.exists():shutil.copyfile(p,OUT/('PRIOR_AUDIT_'+name))
# Human-readable offline document includes the complete repair contract and machine crosswalk.
def render(md):
    result=[];paragraph=[];code=False;table=False
    def inline(s):return re.sub(r'\*\*(.*?)\*\*',r'<strong>\1</strong>',html.escape(s))
    def flush():
        if paragraph:result.append('<p>'+html.escape(' '.join(paragraph))+'</p>');paragraph.clear()
    for line in md.splitlines():
        if table and not line.startswith('|'):
            result.append('</table></div>');table=False
        if line.startswith('```'):flush();result.append('</pre>' if code else '<pre>');code=not code;continue
        if code:result.append(html.escape(line)+'\n');continue
        if line.startswith('|'):
            flush()
            if re.fullmatch(r'[| :\-]+',line):continue
            first=not table
            if first:result.append('<div class="tablewrap"><table>');table=True
            tag='th' if first else 'td'
            result.append('<tr>'+''.join('<'+tag+'>'+inline(c.strip())+'</'+tag+'>' for c in line.strip('|').split('|'))+'</tr>')
            continue
        if not line.strip():flush();continue
        if line.startswith('#'):
            flush();n=min(4,len(line)-len(line.lstrip('#')));result.append('<h'+str(n)+'>'+html.escape(line[n:].strip())+'</h'+str(n)+'>')
        elif line.startswith('- ') or (line[:1].isdigit() and '. ' in line[:5]):flush();result.append('<p class="item">'+inline(line)+'</p>')
        else:paragraph.append(line)
    if table:result.append('</table></div>')
    flush();return ''.join(result)
body=render(text)+'<h2>直接交给Codex的完整提示词</h2><pre>'+html.escape(prompt)+'</pre><h2>跨修订功能保全核验</h2><pre>'+html.escape(json.dumps(preservation,ensure_ascii=False,indent=2))+'</pre>'
page='<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>STARCORE R31统一最终施工方案</title><style>body{margin:0;background:#f2f5f9;color:#18283a;font:16px/1.7 system-ui,sans-serif}main{max-width:1050px;margin:auto;padding:30px;background:white}h1{font-size:30px;color:#164e63}h2{border-top:1px solid #dce4ed;padding-top:24px;color:#155e75}h3{margin-top:30px}.tablewrap{overflow:auto}table{width:100%;border-collapse:collapse;font-size:14px}td,th{border:1px solid #dce4ed;padding:9px;text-align:left;vertical-align:top}th{background:#edf2f7}p{overflow-wrap:anywhere}.item{padding-left:16px;margin:7px 0}pre{background:#edf2f7;border-radius:8px;padding:18px;white-space:pre-wrap;overflow-wrap:anywhere;font:14px/1.7 ui-monospace,monospace}@media print{main{padding:0}h2,h3{break-after:avoid}pre{font-size:11px}}</style><main>'+body+'</main></html>'
(OUT/'STARCORE_R31_UNIFIED_FINAL_20261112.html').write_text(page)
print(json.dumps({'directory':str(OUT),'repair_cards':len(R),'original_cards':len(cards),'original_acceptances':194,'current_acceptance_items':len(items),'preservation':'PASS_PLAN_ONLY','plan_bytes':len(text.encode()),'html_bytes':len(page.encode())},ensure_ascii=False))
