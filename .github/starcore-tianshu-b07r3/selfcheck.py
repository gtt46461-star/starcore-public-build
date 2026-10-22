#!/usr/bin/env python3
from pathlib import Path
import hashlib,json,re,sys
ROOT=Path.cwd(); SRC=ROOT/'app/src/main/java/com/ethk3/novaonepro'; R=ROOT/'reports';R.mkdir(exist_ok=True)
checks=[]
def ck(name,ok,detail=''):
    checks.append({'name':name,'pass':bool(ok),'detail':str(detail)})
    print(('PASS ' if ok else 'FAIL ')+name+(' :: '+str(detail) if detail else ''))
def txt(p): return (ROOT/p).read_text(encoding='utf-8',errors='replace')
PLAN={1:['AppConfig.java','BuildIdentity.java','V51BuildManifest.java','V51ArtifactBundleManifest.java','KeyProvider.java','ApiClient.java','ApiParser.java','SafeLog.java','BinanceGameContract.java','ApiTransportGovernanceEngine.java'],2:['DataGuard.java','CountdownClock.java','Models.java','V51Models.java','SchemaContractRegistry.java','FeatureContractRegistry.java','DrawIntegrityRecoveryEngine.java'],3:['DbHelper.java','DatabaseCoordinator.java','DatabaseMigrationEngine.java','DatabaseIntegrityVerifier.java','DbStore.java','V51Repository.java'],4:['PointInTimeSnapshotEngine.java','V51PermutationFeatureEngine.java','V51TrajectoryFeatureEngine.java','V51ObservableStateVectorEngine.java','RollingWindowCache.java','FullInformationFeatureGraphEngine.java'],5:['V51StateTrajectoryEngine.java','V51StatePrototypeEngine.java','V51ChangePointEngine.java','V51LifecycleEngine.java','WindowFeatureEngine.java','TrendStateMachine.java','BreakRecoveryEngine.java','CycleRhythmEngine.java','DistributionDriftEngine.java'],6:['PatternStateEngine.java','ShapeStateEngine.java','TransitionRelationEngine.java','SimilarShapeEngine.java','ShapeMemoryEngine.java','NumberBehaviorEngine.java','AttributionEngine.java'],7:['StrategyModuleCatalog.java','StrategyEvidenceContract.java','UnifiedStrategyEvidenceEngine.java','EvidenceGroupAggregatorEngine.java','EvidenceHealthSnapshotEngine.java','RuleHealthMatrixEngine.java','FalseStrongFreezeEngine.java','DynamicFreezeMatrixEngine.java','StarcoreEvidenceKernel.java']}
missing=[]
for b,names in PLAN.items():
    for n in names:
        if not (SRC/n).exists(): missing.append('B%02d/%s'%(b,n))
ck('54计划文件完整',sum(len(v) for v in PLAN.values())==54 and not missing,'missing='+','.join(missing))

k=SRC/'StarcoreEvidenceKernel.java'; ks=k.read_text(encoding='utf-8') if k.exists() else ''
lines=ks.splitlines(); effective=[]; in_block=False
for line in lines:
    z=line.strip()
    if in_block:
        if '*/' in z: in_block=False
        continue
    if z.startswith('/*'):
        if '*/' not in z: in_block=True
        continue
    if not z or z.startswith('//') or z.startswith('*'): continue
    effective.append(line)
ck('StarcoreEvidenceKernel存在',k.exists())
ck('Kernel满血行数',len(lines)>=1200,'lines=%d'%len(lines))
ck('Kernel有效代码比例',len(lines)>0 and len(effective)/float(len(lines))>=0.70,'effective=%d/%d'%(len(effective),len(lines)))
ck('Kernel无TODO空壳',not re.search(r'(?i)\bTODO\b|placeholder|return\s+null\s*;\s*//',ks))
ck('Kernel职责边界',all(x not in ks for x in ['DECISION_BET','new Models.Decision','new Models.Trade','finalP =','dynamicGate =','lane =']))
ck('Kernel插件规模',ks.count('implements Plugin')>=60,'plugins=%d'%ks.count('implements Plugin'))
ck('Kernel固定健康状态',all(x in ks for x in ['"WATCH"','"READY"','"STRONG"','"FREEZE"','observeReplay','groupSupport','timedOutPlugins']))

u=txt(Path('app/src/main/java/com/ethk3/novaonepro/UnifiedStrategyEvidenceEngine.java'))
rb=txt(Path('app/src/main/java/com/ethk3/novaonepro/RollingCausalBacktestEngine.java'))
ck('Kernel实时生产调用',u.count('starcoreKernel.evaluate(store, c,')>=2 and u.count('applyEvidenceOnly(c, kb)')>=2)
ck('Kernel回放调用','StarcoreEvidenceKernel.observeReplay(store, replay, actualNumber' in rb)

lane=txt(Path('app/src/main/java/com/ethk3/novaonepro/LanePolicyCompiler.java'))
ck('全局Champion不再一票锁死','return publish(store, c, reject(out, "MODEL_NOT_PROVEN")' not in lane and 'DIAGNOSTIC_NOT_ABSOLUTE_V51_LIVE_VETO' in lane)
ck('候选级严格门仍存在',all(x in lane for x in ['PredictionContractEngine.verifyCandidate','RuntimeFreshnessGuard.evaluate','NORMAL_ACTION_LCB_LOW','NORMAL_COVERAGE_LOW','NORMAL_THIRD_LOW','NORMAL_COUNTER_HIGH','NORMAL_UNCERTAINTY_HIGH']))
ck('活动Lane无频率债务',not re.search(r'(?i)debt|frequencyFloorMandatory',lane))

dec=txt(Path('app/src/main/java/com/ethk3/novaonepro/DecisionGateEngine.java'))
ck('DecisionGate唯一正式出口语义','V46_SINGLE_LANE_AUTHORIZED' in dec and '旧pickDecisionCandidate/finalCheck真实路径=DISABLED' in dec)
ck('V50模型锁仅限typed V50',dec.count('V50BuildManifest.MODEL_PROVEN')==1)

manifest=txt(Path('app/src/main/AndroidManifest.xml'))
svc=txt(Path('app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java'))
boot=txt(Path('app/src/main/java/com/ethk3/novaonepro/BootReceiver.java'))
main=txt(Path('app/src/main/java/com/ethk3/novaonepro/MainActivity.java'))
ck('后台Manifest不随任务停止','android:stopWithTask="false"' in manifest)
ck('前台服务及时建立','startForeground(AppConfig.NOTIFICATION_ID' in svc)
ck('服务START_STICKY','return START_STICKY;' in svc)
ck('任务移除强制重挂','forceRearmAutoFetchService(getApplicationContext(), "B07R3_TASK_REMOVED")' in svc and 'public static boolean forceRearmAutoFetchService' in boot)
ck('Alarm+Job+Boot恢复',all(x in boot for x in ['setExactAndAllowWhileIdle','scheduleJobWatchdog','ACTION_BOOT_COMPLETED','ACTION_MY_PACKAGE_REPLACED']))
ck('UI退出不stopService','stopService(' not in main)

report=txt(Path('app/src/main/java/com/ethk3/novaonepro/B07R2FullDiagnosticEngine.java'))
ck('报告含批次/后台/出手/Kernel/50-60',all(x in report for x in ['B07R3 1-7批次加强复核','后台能力=','真实出手修复','Kernel插件健康','50%/60%目标分析']))
ck('报告禁止伪造50/60','禁止简单降低门槛凑50/60' in report and '严格时间顺序OOS' in report)
ck('一次复制R3完整性','未做二次截断' in main and 'B07R31_REPORT_COMPLETE_MAX_CHARS' in main)

app=txt(Path('app/src/main/java/com/ethk3/novaonepro/AppConfig.java'))
ck('玩法固定K3',all(x in app for x in ['public static final int FIXED_K = 3','ALLOW_K4 = false','ALLOW_DYNAMIC_K = false','ALLOW_MULTI_POSITION_BET = false','ALLOW_MARTINGALE = false','ALLOW_CHASE_BET = false']))
ck('V51活动V50关闭','ENABLE_V50_PRIME = false' in app and 'ENABLE_V51_TIANSHU = true' in app)

# Secret scan: only credential-specific identifiers and URL key literals count.
# Generic variables named `key` are used throughout state/database code and are
# not credentials. Never print a matched secret value; only the file path.
secret_hits=[]
credential_assignment=re.compile(r'(?i)\b(api[_-]?key|duoqu[_-]?api[_-]?key)\b\s*[=:]\s*["\']([A-Za-z0-9_-]{12,})["\']')
url_key_literal=re.compile(r'(?i)https?://[^"\'\s]+[?&]key=([A-Za-z0-9_-]{12,})')
# Exact synthetic values used by SafeLog.selfTest are not credentials. Keeping
# this list exact means any other 12+ character credential-looking literal fails.
allowed_literals={'DUOQU_API_KEY','BuildConfig','NOT_CONFIGURED','sample-secret'}
for p in list(SRC.rglob('*.java'))+list((ROOT/'.github/starcore-tianshu-b07r3').glob('*')):
    if not p.is_file(): continue
    s=p.read_text(encoding='utf-8',errors='replace')
    bad=False
    for m in credential_assignment.finditer(s):
        if m.group(2) not in allowed_literals:
            bad=True; break
    if not bad:
        for m in url_key_literal.finditer(s):
            if m.group(1) not in allowed_literals:
                bad=True; break
    if bad: secret_hits.append(str(p.relative_to(ROOT)))
ck('无明显硬编码接口密钥',not secret_hits,'files='+','.join(secret_hits))

# Lexical Java brace scanner. It ignores braces inside strings, chars and
# comments instead of using regex substitutions that can corrupt large files.
def java_brace_balance(text):
    NORMAL,STRING,CHAR,LINE,BLOCK=0,1,2,3,4
    state=NORMAL; depth=0; i=0; n=len(text); escape=False
    while i<n:
        c=text[i]; nxt=text[i+1] if i+1<n else ''
        if state==NORMAL:
            if c=='"': state=STRING; escape=False
            elif c=="'": state=CHAR; escape=False
            elif c=='/' and nxt=='/': state=LINE; i+=1
            elif c=='/' and nxt=='*': state=BLOCK; i+=1
            elif c=='{': depth+=1
            elif c=='}':
                depth-=1
                if depth<0: return depth
        elif state==STRING:
            if escape: escape=False
            elif c=='\\': escape=True
            elif c=='"': state=NORMAL
        elif state==CHAR:
            if escape: escape=False
            elif c=='\\': escape=True
            elif c=="'": state=NORMAL
        elif state==LINE:
            if c=='\n': state=NORMAL
        elif state==BLOCK:
            if c=='*' and nxt=='/': state=NORMAL; i+=1
        i+=1
    if state in (STRING,CHAR,BLOCK): return 10**9
    return depth
brace_bad=[]
for p in SRC.rglob('*.java'):
    bal=java_brace_balance(p.read_text(encoding='utf-8',errors='replace'))
    if bal!=0: brace_bad.append('%s:%s'%(str(p.relative_to(ROOT)),bal))
ck('Java词法括号门',not brace_bad,'bad='+','.join(brace_bad[:10]))

passes=sum(1 for x in checks if x['pass']); fails=len(checks)-passes
out={'version':'B07R3','pass':passes,'fail':fails,'checks':checks,'kernel_lines':len(lines),'kernel_effective':len(effective),'missing_plan_entries':missing}
(R/'B07R3_BOUNDED_SELFCHECK.json').write_text(json.dumps(out,ensure_ascii=False,indent=2),encoding='utf-8')
print('SUMMARY PASS=%d FAIL=%d'%(passes,fails))
sys.exit(0 if fails==0 else 1)
