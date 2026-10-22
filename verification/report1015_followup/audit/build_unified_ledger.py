from pathlib import Path
import json,csv,gzip,hashlib,re,collections,ast
csv.field_size_limit(100000000)
A=Path(__file__).resolve().parent; ROOT=A.parent; S=ROOT/'starcore'; M=A/'optimized_metadata/mapping'
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def write(name,obj): (A/name).write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n')
def jsonl(name,rows): (A/name).write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in rows))
def csvrows(p):
 with (gzip.open(p,'rt',encoding='utf-8-sig') if p.suffix=='.gz' else p.open(encoding='utf-8-sig')) as f:return list(csv.DictReader(f))
cs=json.loads((A/'checks_original.json').read_text()); cdict={c['id']:c for c in cs}
def R(*nums):return ['RSEC-%03d'%n for n in nums]
# Deliberately reviewed topic crosswalk; these links are evidence locations, never closure.
check_issues={
1:[4,5,6,7,8,9,10,11,12,13,14,15,16],2:[18,19,29],3:[5,6,7,8,9,13,15,16],4:[4,5,6,7,8,9,14,16,19],5:[13,14,27],6:[3],7:[12,14,15,17],9:[18,24],10:[1,2,5,6,7,8,9,15,16,24,25,27],11:[4,5,6,7,8,9,10,11,12,13,14,15,16],12:[5,6,7,8,9,13,14,16,28],13:[13,14,15],14:[14,15,17],15:[14,15,27],16:[12,20],17:[12,14],18:[1,2,3,18,19,24,27,29,30],19:[9,15,26],20:[9,15,16],21:[8,16,27],22:[1,2,4,5,6,7,30],23:[7,8,9,13,14,16,28],24:[4,5,6,7,8,9,10,11,12,13,14,15,16],25:[4,5,14,20],26:[23,24],27:[19,29],28:[18],29:[1,2,16,28,29],30:[1,2,16,24],31:[2,28],32:[24],33:[1],34:[9,15],35:[1,2,9,15,16],36:[29],37:[2,3,26],38:[28,29],39:[15,16],40:[2,15,16],41:[7,23,24],42:[24],43:[29],44:[18,27],48:[29],52:[1,2,25],53:[26,29],54:[26],55:[22,26],56:[26],57:[26,29],58:[26],59:[1,2,8,9,15],60:[16,29],61:[15,16,26,27],62:[15,16,26,27],63:[15,16,26,27],64:[15,16,26,27],65:[15,16,26,27],66:[15,17,26],67:[17],68:[17],69:[15,17,26,27],70:[19,26,29],71:[19,26,29],72:[15,17,26],73:[15,17,26],74:[1,2,15,16,17,24,26,29],75:[26,29],76:[29],77:[18,27,29],78:[1,2,19,25,27,28],79:[4,5,7,12,13,14,15,20,28],301:[27],302:[27],303:[27],304:[27],305:[1,2,7,24,28],306:[9,15,27],307:[4,5,6,7,8,9,13,14,15,16,28],308:[16,17],309:[4,5,6,7,8,9,13,14,15,16,28],310:[16,29],311:[27],312:[15,17,27],313:[15,17,27],314:[16],315:[28],316:[16],317:[13,14,15,27],318:[27],319:[27],320:[27],321:[13,14,15,27,28],322:[13,14,15,27,28],323:[13,14,15,28],324:[27]}
assert set(check_issues)=={int(c['id'].split('-')[1]) for c in cs}
# Engineer IDs are independent of report1015 work-item IDs.
engineer_map={1:[1,2,30],2:[1,3,30],3:[3],4:[2],5:[2,28],6:[24],7:[23],8:[4,5,6,7],9:[4,5,6,7],10:[8,9,28],11:[4,13,14,20],12:[7,13,14],13:[14,15,27],14:[8,9,15,16],15:[15,17],16:[15,16,26],17:[15,17],18:[17,26],19:[16],20:[29],21:[18,19,27],22:[25]}
engineer_titles=['本期请求至提交过慢','单库写入争用与审计遥测同库','数据库与大表存储','请求终态及漏期','延后证据收敛','后台存活与权限','watchdog可恢复锁异常','训练积压','预算反馈环及研究饥饿','盘前捕获及研究结算','深度学习与现代训练','创造变异融合调度','资源替换与代际评价','晋升漏斗','多重比较与选择偏差','Number/Rank/Calibration/Policy证明','生产基线与质量','度量审计消融','正式主链使用','Owner及冲突','审计版本与大文件','数据源与抓取写入']
engineer_checks={1:R(33,52,74),2:R(52,74),3:R(6),4:R(10,305),5:R(31,305),6:R(9,305),7:R(26),8:R(1,12),9:R(1),10:R(12,307),11:R(25,79,305),12:R(12,79),13:R(307,321),14:R(3,12,24),15:R(307,7),16:R(19,20,21,22,61,66),17:R(59,79),18:R(54,55,56,57,58,68,69),19:R(310,316),20:R(67,70,71),21:R(28,70),22:R(10,52)}
engineer=[]
for n in range(1,23):
 limitations='工程师只有2026-10-14旧快照和静态索引；此项非已证实当前源码根因。'
 if n==2:limitations+='105是写入类数，不是并发writer/线程数。'
 if n==5:limitations+='10/15 RSEC031当前10条formalActions=1200、deferred=COMPLETED，RSEC305 deferred pending=0；旧特定缺口不能沿用为当前未收敛。'
 if n==11:limitations+='10/15现代候选55、训练登记54、开发验证39；旧“现代只有1个候选”已过时。'
 if n==13:limitations+='新子代Future从0开始是合法隔离；问题应核查公平服务、保留父代、独立比较，不得继承Future。'
 if n==22:limitations+='10/15备源存在且落后一期间，providerCorroborated=0不直接证明单点、缺实现或主源错误。'
 engineer.append({'engineer_id':f'P-{n:02d}','title':engineer_titles[n-1],'original_snapshot':'2026-10-14 00:59 UTC, feee9344…','report_check_refs':engineer_checks[n],'work_item_ids':[f'R1015-{i:02d}' for i in engineer_map[n]],'detailed_proposal_present':n==1,'limitations':limitations,'current_closure':'OPEN_NO_BLANKET_PASS'})
write('engineer_22_crosswalk.json',engineer)
# Preserve the full 22 handoff rows verbatim in separate fields.
aud=(S/'verification/report1015/AUDIT.md').read_text(); items=[]
for l in aud.splitlines():
 if re.match(r'^\|\d+\|',l):
  p=l.strip('|').split('|');n=int(p[0]); assert len(p)==5,(n,len(p))
  fns=re.findall(r'`([^`]+)`',p[3]);files=[]
  for f in fns:
   base=f.split('.')[0] if '/' not in f else f
   matches=list(S.glob('app/src/main/java/**/'+base.split('/')[-1]+'.java')) if not f.endswith(('.py','.gz','.csv')) else list(S.glob(f))
   files += [str(x.relative_to(S)) for x in matches]
  items.append({'id':f'R1015-{n:02d}','legacy_number':n,'title':p[1],'handoff_fact_and_rootcause_verbatim':p[2],'handoff_responsibility_verbatim':p[3],'handoff_validation_verbatim':p[4],'candidate_source_files':sorted(set(files)),'current_rootcause_certainty':'PARTIAL_CONFIRMED_R78_SEE_VERBATIM' if n in [1,3,6,8,14,19,20] else 'REQUIRES_SCOPED_SOURCE_AND_RUNTIME_VERIFICATION','code_status':'R78_CHANGE_REPORTED_NOT_NEW_R79_CHANGE' if n in [1,3,6,8,14,19,20] else 'INHERITED_SCOPE_OPEN','host_status':'R78_EVIDENCE_REPORTED_RECHECK_CURRENT_IDENTITY','device_status':'NOT_VERIFIED_BY_THIS_AUDIT','natural_status':'NOT_PROVEN_BY_THIS_AUDIT','whole_status':'OPEN'})
assert len(items)==22
new=[
(23,'P1 watchdog异常传播与可恢复写失败','原crashSummary累计172，最后事件1791806924296、CP3_DB_BUSY_DEADLINE，ensurePending→reconcile→postAnalysisForLatest→runAnalysisWatchdogR36。最近10分钟字段说明无滚动日志，不能据0认定零崩溃。','AnalysisRequestStoreR36.java;BusinessReconcilerR36.java;AutoFetchService.java','只读当前异常边界；注入可恢复锁超时、逻辑错误和取消，验证串行重排/截止/终态且逻辑错误不吞掉。',R(26,41)),
(24,'P1 后台系统能力与设备恢复矩阵','RSEC305 capabilities=READY_DEVICE_SOAK_NOT_VERIFIED，权限8/9，taskRemoved未观察；RSEC042 T29–T34均待验。','AutoFetchService.java;BackgroundCapabilitySnapshotR927.java','核准缺失权限的实际语义；保持唯一权限出口；检查可用设备，执行亮屏/息屏/HOME/任务移除/杀进程/网络切换/24h，未发生的不记PASS。',R(9,32,41,42,305)),
(25,'P2 主备源相关证明与抓取入库拒绝分流','RSEC052 providerCorroborated=0；RSEC078主源202610150253 FRESH，备源202610150252 AWAITING_NEXT_ISSUE。备源存在；不能直接接受工程师“单点”推断。','ApiClient.java;ApiTransportGovernanceEngine.java;DrawCanonicalizerR811.java;DbStore.java','分类源迟到/同源镜像/冲突/写入拒绝；用精确开奖身份验证切换和故障恢复，不混游戏、不伪造佐证。',R(10,52,78)),
(26,'P2 PIT与号码面及K3分层运行证据','RSEC054 distribution/sinkhorn无样本；055 relation/permutation/transition无样本；056静态9→3但runtime无样本；058无样本；066六类诊断UNAVAILABLE。','FullInformationFeatureGraphEngine.java;V51SinkhornEngine.java;V51RelationGraphVerifier.java;K3FunnelOrchestrator.java;K3FailureDiagnosisEngine.java','定位运行写入/读取键与真实消费者；验证PIT、有限概率与parity、嵌套9/8/7/6/5/4/3、rank3/4边界及独立消融。仅诊断缺证不是数学错误。',R(54,55,56,58,66,70)),
(27,'P2 报告度量范围与旧诊断真实性','同报告RSEC061采用settled865/hit242，而RSEC034/306严格OOS3421/hit1043，未解释分母差异；RSEC317/321把PRUNED_RESOURCE_R966显示为质量或契约淘汰，reason明确NOT_QUALITY_OR_CERTIFICATE_VERDICT。RSEC324引用snapshot.db而归档未带原始页，DIAGNOSTIC_SCOPE rawDatabaseIncluded=false。','RuntimeTruthSnapshotR906.java;AlgorithmLifecycleReportR932.java;V57FailureDiagnosisEngine.java;CodeOwnershipCompletenessAuditEngine.java','追溯查询范围/历史快照/身份与资源淘汰标签；检查原库引用是否明确外部可用性。保持原证据，不以统一计数或改标签冒充功能修复。',R(18,61,306,317,321,324)),
(28,'P1 旧研究待结算逐条恢复与证据分类','交接截图旧R908冻结13490/待6539/结6951、旧R915冻结697/结262，不是AST3训练；RSEC315生产结算3421/3421不能替代研究结算；RSEC323研究方法亦有冻结/结算缺口。','RuntimeTruthSnapshotR906.java;ResearchLearningContinuityR912.java;SettlementEngine.java;ResearchSettlementEngineR903.java','按合法盘前执行收据+权威开奖+可取回归档分类并幂等结算；缺证明永久保留明确原因，禁止补造盘前/清空历史。',R(12,79,305,307,315,321,323)),
(29,'P2 Owner及来源治理独立验收','RSEC070/071 PASS11 WATCH5 NA1；POINT_IN_TIME无样本；NUMBER_Q/RANK/CALIBRATION/POLICY是证明WATCH，SOURCE_GOVERNANCE需离线审计。explicitConflict=false不能推断存在第二Owner，也不能证明全动态路径唯一。','CodeOwnershipCompletenessAuditEngine.java;ProductionOwnerCatalogR811.java;DecisionGateEngine.java;SettlementEngine.java','分离模型未证明与Owner冲突；核对动态/反射/XML/native实例及写入白名单；唯一Gate/Settlement保持。',R(36,43,70,71,75,76)),
(30,'P1 资源观测Span栈与可归因性','RSEC022 120行中OWNER_DISPATCH/DURABLE_CLAIM/REQUEST_TO_WORKER各115次STAGE_STACK_MISMATCH，INCOMPLETE_SPANS19，R36_COMPUTE mismatch7；GC字段是进程累计差。','RuntimeResourceAuditR927.java;V51RuntimeOrchestrator.java;AnalysisSingleFlightR36.java','先核Span模型是否把非嵌套队列事件误作为同步栈；为异步/取消/失败保留明确不可测状态，修正测量后复跑，不通过删除异常或填0获得完整。',R(18,22))]
for n,title,fact,files,next_,refs in new:
 items.append({'id':f'R1015-{n:02d}','legacy_number':None,'appended_number':n,'title':title,'report_fact':fact,'candidate_source_files':[str(p.relative_to(S)) for f in files.split(';') for p in S.glob('app/src/main/java/**/'+f)],'candidate_class_names':files.split(';'),'current_rootcause_certainty':'OBSERVED_SYMPTOM_OR_ACCEPTANCE_GAP_ROOT_CAUSE_UNCONFIRMED','remaining_action':next_,'original_evidence_refs':refs,'code_status':'NO_R79_CHANGE_CLAIMED_BY_AUDITOR','host_status':'NOT_RUN_BY_THIS_AUDIT','device_status':'NOT_VERIFIED_BY_THIS_AUDIT','natural_status':'NOT_PROVEN_BY_THIS_AUDIT','whole_status':'OPEN'})
for it in items:
 n=int(it['id'].split('-')[1]);it['report_check_refs']=[f'RSEC-{k:03d}' for k,v in check_issues.items() if n in v];it['engineer_refs']=[f'P-{k:02d}' for k,v in engineer_map.items() if n in v]
 it['source_file_existence']={f:(S/f).exists() for f in it['candidate_source_files']}
 if n==2:it['audit_correction']='157 in located raw evidence is over_ms=157 for issue202610150246, recent30 distribution count1; handoff wording 157次 lacks a verified count source and must not be repeated as a fact.'
write('work_items_30.json',items)
checks=[]
for c in cs:
 x=dict(c);x['work_item_ids']=[f'R1015-{n:02d}' for n in check_issues[int(c['id'].split('-')[1])]];x['engineer_refs']=[e['engineer_id'] for e in engineer if c['id'] in e['report_check_refs']];x['evidence_sha256']=sha(ROOT/'r79_inputs/report'/c['evidence_member']);x['audit_disposition']='ORIGINAL_REPORT_RESULT_PRESERVED_NOT_CURRENT_R79_ACCEPTANCE';checks.append(x)
jsonl('report_checks_96.jsonl',checks)
# Original per-row identity is the join; sequence and business-ID namespaces are both preserved.
reqs=csvrows(S/'construction/r997/REQUIREMENTS_LEDGER.csv.gz'); gates=[json.loads(l) for l in (M/'REQUIREMENTS_1832_CURRENT_GATE_MAP.jsonl').read_text().splitlines()]; gm={x['original_business_id']:x for x in gates}
assert len(reqs)==len(gates)==1832 and len(gm)==1832
errors=[];unified=[]
domain_map={'DOC':[18],'ISSUE':[2,29],'CAL':[8,9,15,16],'ROOT':[18],'HISTORY':[2,3,4,25],'TARGET':[2,29],'PIT':[4,8,26],'ATOM':[12,20],'AST':[13,14,20],'MATRIX':[10,11,12,17],'ATOM_MATH':[12,20],'RESEARCH':[7,13,14,21,22],'CREATOR':[13,14,20],'TRAIN':[4,5,20],'FREEZE':[4,9,19],'PREDRAW':[2,8,28],'FUTURE':[9,14,15,16],'CERT':[15,16],'MAINLINE':[16],'GATE':[16,29],'SETTLE':[28,29],'EVOLUTION':[13,14,20],'CALL':[16,29],'BACKGROUND':[7,23,24],'UI':[27],'OWNER':[29],'INTERFACE':[18,29],'SEQUENCE':[7,18],'CARD':[18],'PATCH':[18],'SCHEMA':[3,18],'STREAK':[15,17],'CLOCK':[2,8],'PERF':[1,30],'RETENTION':[3,28],'STATE':[7,14],'THREAD':[1,7,23,30],'RECOVERY':[3,4,5,7,23,24,28],'REASON':[14,27],'RETIRE':[14,27],'TEST':[18],'BUILD':[18,19],'MEMORY':[1,3],'REPORT':[18,27],'ADMISSION':[12,15],'QUEUE':[4,5,6,7],'POST':[6,14]}
for r in reqs:
 g=gm.get(r['requirement_id']);h=hashlib.sha256(r['exact_requirement'].encode()).hexdigest()
 if not g or g['exact_requirement']!=r['exact_requirement'] or g['exact_requirement_sha256']!=h or r['exact_requirement_sha256']!=h:errors.append(r['requirement_id'])
 product_files=set(g.get('product_files',[]));direct=[it['id'] for it in items if product_files.intersection(it['candidate_source_files'])]
 unified.append({'requirement_sequence':g['requirement_sequence'],'original_business_id':r['requirement_id'],'original_row':r,'optimized_current_gate_contract':g,'exact_text_crosswalk_verified':r['requirement_id'] not in errors,'direct_product_file_issue_links':direct,'domain_candidate_issue_links':[f'R1015-{n:02d}' for n in domain_map[r['module']]],'link_semantics':'FILE_OVERLAP_OR_TOPIC_ROUTING_ONLY_NOT_REQUIREMENT_SATISFACTION','audit_current_status':'PRESERVE_ORIGINAL_NO_NEW_ACCEPTANCE','new_acceptance_receipts':[]})
assert not errors,errors
jsonl('requirements_unified_1832.jsonl',unified)
cards=csvrows(S/'construction/r997/CARD_LEDGER.csv.gz'); cm={r['card_id']:r for r in csvrows(M/'ORIGINAL_299_FILE_PHASE_RESPONSIBILITIES.csv')};assert len(cards)==len(cm)==299
uc=[]
for c in cards:
 assert cm[c['card_id']]['path']==c['path']
 uc.append({'card_id':c['card_id'],'original_row':c,'optimized_phase_responsibility':cm[c['card_id']],'related_work_items':[it['id'] for it in items if c['path'] in it['candidate_source_files']],'current_source_exists':(S/c['path']).exists(),'current_source_sha256':sha(S/c['path']) if (S/c['path']).is_file() else None,'audit_current_status':'ORIGINAL_STATUS_PRESERVED_CURRENT_SHA_NOT_ACCEPTANCE','new_acceptance_receipts':[]})
jsonl('cards_unified_299.jsonl',uc);write('legacy_subitems_5.json',csvrows(S/'construction/r997/SUBITEMS.csv'))
write('ledger_contract.json',{'version':'R79_AUDIT_APPEND_ONLY_V1','namespaces':['RSEC report IDs','P-xx engineer IDs','R1015-01..22 inherited work IDs','R1015-23..30 appended review scopes','REQ-xxxx optimized sequence','original_business_id','FILE-CAN-xxx cards','R997-Sxx partial subitems'],'join_rule':'Business ID plus exact requirement UTF8 SHA256; never numeric index alone','whole_pass':'All original applicable conjuncts with same source/input/variant identity; document, engineering, host, device, natural, release kept separate. No inherited/source-presence/quantity closure.','status_options':['未定位','已定位未修改','已修改待测试','主机测试通过','设备验收通过','明确阻塞'],'proof_scopes':['STATIC_CONTRACT','HOST_SYNTHETIC','HOST_REAL_INPUT_REPLAY','DEVICE_RUNTIME','NATURAL_FUTURE','RELEASE_IDENTITY'],'requirements_count':len(reqs),'cards_count':len(cards),'checks_count':len(checks),'engineer_count':len(engineer),'work_items_count':len(items),'r79_new_whole_pass_count':0,'current_req_statuses':dict(collections.Counter(r['current_status'] for r in reqs)),'current_card_statuses':dict(collections.Counter(r['current_status'] for r in cards)),'checks_health':dict(collections.Counter(c['health'] for c in checks)),'note':'Device/natural blockers never block independent engineering work. This audit neither initializes nor modifies product continuity state.'})
write('crosswalk_integrity.json',{'exact_requirement_sha_and_text_matches':len(unified),'card_id_path_matches':len(uc),'all96_checks_linked':len(checks)==96 and all(c['work_item_ids'] for c in checks),'all22_engineer_items_linked':len(engineer)==22 and all(e['work_item_ids'] for e in engineer),'all22_handoff_rows_preserved':len([i for i in items if i['legacy_number']])==22,'appended_scope_count':8,'duplicates':{'requirements':len(reqs)-len(set(r['requirement_id'] for r in reqs)),'cards':len(cards)-len(set(c['card_id'] for c in cards)),'checks':len(cs)-len(set(c['id'] for c in cs))},'errors':errors,'does_not_establish_runtime_completion':True})
print(json.dumps(json.loads((A/'crosswalk_integrity.json').read_text()),ensure_ascii=False,indent=2))
# Appended independently verified continuity-tool defect, after all inherited IDs.
continuity_item={'id':'R1015-31','legacy_number':None,'appended_number':31,'title':'P1 施工事件schema兼容与旧交接状态','report_fact':'optimized events211条hash链有效，HANDOFF停在197。第206/207的CHECKPOINT是带kind的部分验收收据，没有旧replay必取的lane/status/source_sha256。原包与仓库construct299_base.py均使用旧schema；独立提取原replay在205条可读、206起KeyError(status)。','candidate_source_files':['construction_state/optimized299/continuity/construct299_base.py','construction_state/optimized299/continuity/construct299_cached.py'],'current_rootcause_certainty':'CONFIRMED_READ_ONLY_REPRO_LEGACY_REPLAY_SCHEMA_ASSUMPTION','remaining_action':'新增审计兼容读取器或明确版本适配，校验完整SHA链并分别解释标准checkpoint和kind范围收据；不重写历史hash、不升级partial为whole、不重新init。新状态必须绑定当前源码和真实验收。','report_check_refs':R(18,28),'engineer_refs':['P-21'],'code_status':'AUDIT_REPRO_ONLY_NO_PRODUCT_OR_CONTINUITY_MUTATION','host_status':'READ_ONLY_REPRO_205_PARSE_206_KEYERROR','device_status':'NOT_APPLICABLE_TO_TOOL_PARSER','natural_status':'NO_PROOF_GRANTED','whole_status':'OPEN','evidence':['optimized_continuity_review.json','continuity_legacy_replay_repro.json']}
items.append(continuity_item);write('work_items.json',items)
# Preserve all original diagnostics/test rows and their independent denominators.
det=json.load(open(A/'construction-DETECTIONS_166.json'));tests=json.load(open(A/'construction-TEST_CASES_328.json'));tm={t['test_id']:t for t in csvrows(M/'ORIGINAL_328_TEST_CURRENT_EXPECTATIONS.csv')}
assert len(det)==166 and len(tests)==len(tm)==328
jsonl('detections_unified_166.jsonl',[{'original_issue_id':r['issue_id'],'original_row':r,'related_work_items':[it['id'] for it in items if set(r['target_file'].split(';')).intersection(it['candidate_source_files'])],'new_acceptance_receipts':[],'whole_status':'ORIGINAL_ACCEPTANCE_PRESERVED_NO_NEW_PASS'} for r in det])
jsonl('tests_unified_328.jsonl',[{'test_id':r['test_id'],'original_row':r,'optimized_current_expectation':tm[r['test_id']],'related_work_items':[it['id'] for it in items if set(r['file'].split(';')).intersection(it['candidate_source_files'])],'new_acceptance_receipts':[],'whole_status':'ORIGINAL_ACCEPTANCE_PRESERVED_NO_NEW_PASS'} for r in tests])
contract=json.load(open(A/'ledger_contract.json'));contract.update({'work_items_count':31,'appended_scopes':9,'detection_count':166,'original_tests_count':328,'detection_execution_statuses':dict(collections.Counter(r['current_execution_status'] for r in det)),'original_test_execution_statuses':dict(collections.Counter(r['current_execution_status'] for r in tests))});write('ledger_contract.json',contract)
integ=json.load(open(A/'crosswalk_integrity.json'));integ.update({'appended_scope_count':9,'original_166_detections_preserved':True,'original_328_test_ids_joined':True});write('crosswalk_integrity.json',integ)
