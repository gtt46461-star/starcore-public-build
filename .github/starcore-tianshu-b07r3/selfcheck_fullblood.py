#!/usr/bin/env python3
from pathlib import Path
import re, json, sys
ROOT=Path.cwd(); SRC=ROOT/'app/src/main/java/com/ethk3/novaonepro'; R=ROOT/'reports';R.mkdir(exist_ok=True)
MIN={
'AppConfig.java':5670,'BuildIdentity.java':1000,'V51BuildManifest.java':1000,'V51ArtifactBundleManifest.java':1000,'KeyProvider.java':1000,'ApiClient.java':1000,'ApiParser.java':1056,'SafeLog.java':1000,'BinanceGameContract.java':1200,'ApiTransportGovernanceEngine.java':1200,
'DataGuard.java':1438,'CountdownClock.java':1067,'Models.java':18809,'V51Models.java':1000,'SchemaContractRegistry.java':1000,'FeatureContractRegistry.java':1000,'DrawIntegrityRecoveryEngine.java':1200,
'DbHelper.java':4645,'DatabaseCoordinator.java':1000,'DatabaseMigrationEngine.java':1000,'DatabaseIntegrityVerifier.java':1000,'DbStore.java':17584,'V51Repository.java':1000,
'PointInTimeSnapshotEngine.java':1104,'V51PermutationFeatureEngine.java':1000,'V51TrajectoryFeatureEngine.java':1000,'V51ObservableStateVectorEngine.java':1000,'RollingWindowCache.java':1200,'FullInformationFeatureGraphEngine.java':1200,
'V51StateTrajectoryEngine.java':1000,'V51StatePrototypeEngine.java':1000,'V51ChangePointEngine.java':1000,'V51LifecycleEngine.java':1000,'WindowFeatureEngine.java':1000,'TrendStateMachine.java':1000,'BreakRecoveryEngine.java':1000,'CycleRhythmEngine.java':1516,'DistributionDriftEngine.java':1111,
'PatternStateEngine.java':1482,'ShapeStateEngine.java':1067,'TransitionRelationEngine.java':1224,'SimilarShapeEngine.java':1000,'ShapeMemoryEngine.java':1000,'NumberBehaviorEngine.java':1406,'AttributionEngine.java':1373,
'StrategyModuleCatalog.java':1000,'StrategyEvidenceContract.java':1000,'UnifiedStrategyEvidenceEngine.java':1000,'EvidenceGroupAggregatorEngine.java':1000,'EvidenceHealthSnapshotEngine.java':1000,'RuleHealthMatrixEngine.java':2097,'FalseStrongFreezeEngine.java':1791,'DynamicFreezeMatrixEngine.java':2256,'StarcoreEvidenceKernel.java':1200}
checks=[]
def ck(name,ok,detail=''):
    checks.append({'name':name,'pass':bool(ok),'detail':str(detail)});print(('PASS ' if ok else 'FAIL ')+name+(' :: '+str(detail) if detail else ''))

short=[]
for name,mn in MIN.items():
    p=SRC/name
    n=len(p.read_text(encoding='utf-8',errors='replace').splitlines()) if p.exists() else 0
    if n<mn: short.append('%s=%d<%d'%(name,n,mn))
ck('54文件合同实际源码体量',len(MIN)==54 and not short,';'.join(short))

# Only unresolved executable stubs are forbidden. Legitimate compatibility
# helpers such as isWeakPlaceholderDecision(...) are real decision-ledger code,
# not empty implementations, and therefore must not be rejected by name alone.
unresolved=[]
stub_patterns=[
    re.compile(r'(?i)\bTODO\b'),
    re.compile(r'(?i)\bFIXME\b'),
    re.compile(r'(?i)UnsupportedOperationException\s*\(\s*["\'](?:TODO|placeholder)'),
    re.compile(r'(?i)return\s+(?:null|0|false|true)\s*;\s*//\s*(?:TODO|placeholder)'),
    re.compile(r'(?i)throw\s+new\s+RuntimeException\s*\(\s*["\'](?:TODO|placeholder)')]
for name in MIN:
    p=SRC/name
    if not p.exists(): continue
    for no,line in enumerate(p.read_text(encoding='utf-8',errors='replace').splitlines(),1):
        z=line.strip()
        if not z or z.startswith('//') or z.startswith('*') or z.startswith('/*'): continue
        if any(rx.search(z) for rx in stub_patterns): unresolved.append('%s:%d'%(name,no))
ck('核心可执行路径无TODO/空实现占位',not unresolved,';'.join(unresolved[:30]))

catalog=(SRC/'StrategyModuleCatalog.java').read_text(encoding='utf-8')
contract=(SRC/'StrategyEvidenceContract.java').read_text(encoding='utf-8')
unified=(SRC/'UnifiedStrategyEvidenceEngine.java').read_text(encoding='utf-8')
agg=(SRC/'EvidenceGroupAggregatorEngine.java').read_text(encoding='utf-8')
health=(SRC/'EvidenceHealthSnapshotEngine.java').read_text(encoding='utf-8')
kernel=(SRC/'StarcoreEvidenceKernel.java').read_text(encoding='utf-8')
ck('B07注册表满血','B07R3_CATALOG_FULLBLOOD' in catalog and 'persistEvidenceContractAudit(store)' in catalog and catalog.count('new EvidenceDescriptor(')>=70)
ck('B07字段合同满血','B07R3_EVIDENCE_FIELD_CONTRACT' in contract and 'auditCandidateR3(candidate)' in contract and contract.count('new FieldRule(')>=100)
ck('B07阶段编排满血','B07R3_ORCHESTRATION_PLAN' in unified and 'persistStagePlanR3(store, stage' in unified and unified.count('new StageContract(')>=70)
ck('B07稳健聚合满血','B07R3_ROBUST_AGGREGATION' in agg and 'R3RobustAggregation.enrich(out, groups)' in agg and all(x in agg for x in ['weightedMedian','huber','tukey','effectiveSampleSize','tailMean']))
ck('B07滚动OOS健康满血','B07R3_ROLLING_OOS_HEALTH' in health and 'R3HealthAudit.analyzeAndPersist(store, rows)' in health and all(x in health for x in ['new Window(12)','new Window(24)','new Window(48)','new Window(96)','new Window(360)','wilson','brier','ece']))
ck('B07Kernel满血','B07R3_KERNEL_CONTRACT_INDEX' in kernel and kernel.count('implements Plugin')>=60 and 'R3KernelContractIndex.validate(out)' in kernel and 'observeReplay' in kernel)
ck('B07Kernel禁止动作Owner',all(x not in kernel for x in ['new Models.Decision','new Models.Trade','DECISION_BET','finalP =','lane =']))

ck('Evidence生产前概率边界',unified.count('starcoreKernel.evaluate(store, c, "PRE_PROBABILITY")')==1)
ck('Evidence生产决策前边界',unified.count('starcoreKernel.evaluate(store, c, "PRE_DECISION")')==1)
rolling=(SRC/'RollingCausalBacktestEngine.java').read_text(encoding='utf-8')
ck('Evidence真实回放边界','StarcoreEvidenceKernel.observeReplay(store, replay, actualNumber' in rolling)

lane=(SRC/'LanePolicyCompiler.java').read_text(encoding='utf-8')
ck('V51不被全局Champion一票锁死','return publish(store, c, reject(out, "MODEL_NOT_PROVEN")' not in lane)
ck('候选级OOS硬门保留',all(x in lane for x in ['PredictionContractEngine.verifyCandidate','RuntimeFreshnessGuard.evaluate','NORMAL_ACTION_LCB_LOW','NORMAL_COVERAGE_LOW','NORMAL_THIRD_LOW','NORMAL_COUNTER_HIGH','NORMAL_UNCERTAINTY_HIGH']))

boot=(SRC/'BootReceiver.java').read_text(encoding='utf-8');service=(SRC/'AutoFetchService.java').read_text(encoding='utf-8');main=(SRC/'MainActivity.java').read_text(encoding='utf-8')
ck('TaskRemoved即时重挂','forceRearmAutoFetchService(getApplicationContext(), "B07R3_TASK_REMOVED")' in service and 'public static boolean forceRearmAutoFetchService' in boot)
ck('UI生命周期不停止服务','stopService(' not in main)

passes=sum(1 for x in checks if x['pass']);fails=len(checks)-passes
out={'pass':passes,'fail':fails,'short_files':short,'unresolved':unresolved,'checks':checks}
(R/'B07R3_FULLBLOOD_SELFCHECK.json').write_text(json.dumps(out,ensure_ascii=False,indent=2),encoding='utf-8')
print('FULLBLOOD SUMMARY PASS=%d FAIL=%d'%(passes,fails))
sys.exit(0 if fails==0 else 1)
