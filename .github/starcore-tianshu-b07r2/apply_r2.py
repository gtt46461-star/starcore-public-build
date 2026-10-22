#!/usr/bin/env python3
from pathlib import Path
import hashlib, json, re
ROOT=Path(__file__).resolve().parents[0]
if (ROOT/'app').exists() is False:
    ROOT=Path.cwd()
SRC=ROOT/'app/src/main/java/com/ethk3/novaonepro'
changes=[]

def rw(rel, fn, label):
    p=ROOT/rel
    old=p.read_text(encoding='utf-8')
    new=fn(old)
    if new==old:
        raise SystemExit('PATCH_NO_CHANGE:'+str(rel)+':'+label)
    p.write_text(new,encoding='utf-8')
    changes.append({'file':str(rel),'change':label,'before_sha256':hashlib.sha256(old.encode()).hexdigest(),'after_sha256':hashlib.sha256(new.encode()).hexdigest(),'before_lines':old.count('\n')+1,'after_lines':new.count('\n')+1})

def once(s, old, new, tag):
    n=s.count(old)
    if n!=1: raise SystemExit('PATTERN_COUNT_'+tag+'='+str(n))
    return s.replace(old,new,1)

rw(Path('app/build.gradle'), lambda s: once(once(once(s,
    'versionCode 577110807','versionCode 577120807','versionCode'),
    "versionName 'V57.7.1-tianshu-b07-reinforced'","versionName 'V57.7.2-tianshu-b01-b07-reinforced-r2'",'versionName'),
    "Starcore-Tianshu-B07-Reinforced-Binance-K3","Starcore-Tianshu-B01-B07-Reinforced-R2-Binance-K3",'archive'),
    'R2 build identity')

app=Path('app/src/main/java/com/ethk3/novaonepro/AppConfig.java')
def patch_app(s):
    anchor='    public static final String B07R_VERSION = "STARCORE_TIANSHU_B01_B07_REINFORCED_20260807";\n'
    insert=anchor+'''    public static final String B07R2_VERSION = "STARCORE_TIANSHU_B01_B07_REINFORCED_R2_20260807";\n    public static final String B07R2_DIAG_SCHEMA = "B07R2_FULL_STATE_AUDIT_V2";\n    public static final boolean B07R2_PROCESS_BOOTSTRAP_REARM = true;\n    public static final boolean B07R2_TASK_REMOVED_REARM = true;\n    public static final boolean B07R2_LOW_MEMORY_SELF_HEAL = true;\n    public static final boolean B07R2_NO_FAKE_MODEL_PROOF = true;\n    public static final boolean B07R2_ACTIVE_TIANSHU_REAL_ACTION = true;\n    public static final String B07R2_REAL_ACTION_POLICY = "DECISION_GATE_STRICT_LIVE_ACTION_NO_PROOF_FORGERY";\n    public static final String B07R2_V50_PROOF_SCOPE = "INACTIVE_WHEN_ENABLE_V50_PRIME_FALSE";\n    public static final long B07R2_RUNTIME_GUARD_INTERVAL_MS = 3000L;\n    public static final long B07R2_BUSINESS_STALE_MS = 20000L;\n    public static final int B07R2_REPORT_MAX_CHARS = 360000;\n    public static final int B07R2_REPORT_RAW_STATE_CAP = 5000;\n    public static final int B07R2_MIN_SETTLED_FOR_RATE_CLAIM = 60;\n    public static final double B07R2_TARGET_HIT_40 = 0.400d;\n    public static final double B07R2_TARGET_HIT_50 = 0.500d;\n    public static final double B07R2_TARGET_HIT_60 = 0.600d;\n    public static final double B07R2_TARGET_COVERAGE = 0.150d;\n'''
    return once(s,anchor,insert,'appconfig_r2_anchor')
rw(app,patch_app,'R2 runtime/real-action/report/proof contract')

boot=Path('app/src/main/java/com/ethk3/novaonepro/BootReceiver.java')
def patch_boot(s):
    old='scheduleWatchdog(context, reason, Math.min(AppConfig.SERVICE_WATCHDOG_ALARM_DELAY_MS, AppConfig.B07R_FAST_RECOVERY_MS));'
    if old in s:
        s=once(s,old,'scheduleWatchdog(context, reason, Math.min(AppConfig.SERVICE_WATCHDOG_ALARM_DELAY_MS, AppConfig.B07R2_RUNTIME_GUARD_INTERVAL_MS));','boot_fast')
    return s
if (ROOT/boot).exists():
    old=(ROOT/boot).read_text(encoding='utf-8')
    new=patch_boot(old)
    if new!=old:
        rw(boot,patch_boot,'R2 3s bounded recovery cadence')

svc=Path('app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java')
def patch_svc(s):
    s=once(s,
'''        try { heartbeatStateV8().markTaskRemoved(); } catch (Throwable ignore) { }\n        writeBusinessHeartbeatState("last_task_removed_at", "", "TASK_REMOVED_RECOVERY_REQUESTED");''',
'''        try { heartbeatStateV8().markTaskRemoved(); heartbeatStateV8().renewRuntimeLease("B07R2_TASK_REMOVED_REARM"); } catch (Throwable ignore) { }\n        writeBusinessHeartbeatState("last_task_removed_at", "", "B07R2_TASK_REMOVED_RECOVERY_REQUESTED");\n        try { if (store != null) { store.setState("b07r2_last_task_removed_rearm_at", String.valueOf(System.currentTimeMillis())); store.setState("b07r2_runtime_mode", AppConfig.B07R2_REAL_ACTION_POLICY); } } catch (Throwable ignore) { }''','svc_task_state')
    marker='''\t@Override\n\tpublic IBinder onBind(Intent intent) {\n\t\treturn null;\n\t}\n'''
    insert='''\t@Override\n    public void onLowMemory() {\n        try {\n            if (AppConfig.B07R2_LOW_MEMORY_SELF_HEAL) {\n                RollingWindowCache.shared().clear();\n                FullInformationFeatureGraphEngine.shared().clear();\n                if (store != null) {\n                    store.setState("b07r2_low_memory_at", String.valueOf(System.currentTimeMillis()));\n                    store.setState("b07r2_low_memory_action", "CLEAR_HEAVY_CACHE_KEEP_SERVICE_ALIVE");\n                }\n                heartbeatStateV8().markBusinessStage("LOW_MEMORY", "CACHE_CLEARED_SERVICE_KEPT_ALIVE");\n                ensureRuntimeLoopsAliveV17("b07r2_low_memory");\n                postFetchNow();\n            }\n        } catch (Throwable e) { CrashRecorder.recordCaught(getApplicationContext(), e, "B07R2_LOW_MEMORY"); }\n        super.onLowMemory();\n    }\n\n    @Override\n    public void onTrimMemory(int level) {\n        try {\n            if (AppConfig.B07R2_LOW_MEMORY_SELF_HEAL\n                    && level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {\n                RollingWindowCache.shared().clear();\n                FullInformationFeatureGraphEngine.shared().clear();\n                if (store != null) {\n                    store.setState("b07r2_trim_memory_at", String.valueOf(System.currentTimeMillis()));\n                    store.setState("b07r2_trim_memory_level", String.valueOf(level));\n                }\n                heartbeatStateV8().markBusinessStage("TRIM_MEMORY", "level=" + level + ";CACHE_CLEARED");\n                ensureRuntimeLoopsAliveV17("b07r2_trim_memory");\n            }\n        } catch (Throwable e) { CrashRecorder.recordCaught(getApplicationContext(), e, "B07R2_TRIM_MEMORY"); }\n        super.onTrimMemory(level);\n    }\n\n\t@Override\n\tpublic IBinder onBind(Intent intent) {\n\t\treturn null;\n\t}\n'''
    s=once(s,marker,insert,'svc_memory_hooks')
    s=s.replace('AppConfig.B07R_RUNTIME_GUARD_INTERVAL_MS);','AppConfig.B07R2_RUNTIME_GUARD_INTERVAL_MS);')
    return s
rw(svc,patch_svc,'task-remove lease + low-memory cache self-heal + faster guard')

crash=Path('app/src/main/java/com/ethk3/novaonepro/CrashRecorder.java')
def patch_crash(s):
    old='''        ed.putLong("at", now);\n        ed.putInt("crash_count", prefs.getInt("crash_count", 0) + 1);\n'''
    new='''        ed.putLong("at", now);\n        ed.putInt("crash_count", prefs.getInt("crash_count", 0) + 1);\n        long windowStart = prefs.getLong("crash_window_start", 0L);\n        int windowCount = prefs.getInt("crash_window_count", 0);\n        if (windowStart <= 0L || now - windowStart > 600000L) { windowStart = now; windowCount = 0; }\n        windowCount++;\n        ed.putLong("crash_window_start", windowStart);\n        ed.putInt("crash_window_count", windowCount);\n'''
    s=once(s,old,new,'crash_window')
    old2='''        ed.putString("version", VERSION);\n        ed.commit();\n'''
    new2='''        ed.putString("fingerprint", (error == null ? "" : error.getClass().getName()) + "|" + (stage == null ? "" : stage) + "|" + (thread == null ? "" : thread.getName()));\n        ed.putLong("max_memory", rt.maxMemory());\n        ed.putString("version", AppConfig.B07R2_VERSION);\n        ed.commit();\n'''
    return once(s,old2,new2,'crash_fingerprint')
rw(crash,patch_crash,'crash-loop window/fingerprint/max-memory evidence')

D=Path('app/src/main/java/com/ethk3/novaonepro/DecisionGateEngine.java')
def patch_decision(s):
    old='''\t\tif (bestBet != null) {\n\t\t\tbestBet.check.reason = appendReason(\n'''
    new='''\t\tif (bestBet != null) {\n            try {\n                if (store != null) {\n                    store.setState("b07r2_real_action_owner", "DecisionGateEngine");\n                    store.setState("b07r2_real_action_status", "BET_ALLOWED_BY_STRICT_GATE");\n                    store.setState("b07r2_real_action_target", safe(bestBet.candidate == null ? "" : bestBet.candidate.targetIssue));\n                    store.setState("b07r2_real_action_at", String.valueOf(System.currentTimeMillis()));\n                }\n            } catch (Throwable ignore) { }\n\t\t\tbestBet.check.reason = appendReason(\n'''
    return once(s,old,new,'decision_bet_audit')
rw(D,patch_decision,'real-action audit stamp; no gate bypass')

main=Path('app/src/main/java/com/ethk3/novaonepro/MainActivity.java')
def patch_main(s):
    s=once(s,'B07ReinforcedDiagnosticEngine.build(getApplicationContext(), db)','B07R2FullDiagnosticEngine.build(getApplicationContext(), db)','main_diag_engine')
    s=s.replace('AppConfig.B07R_REPORT_MAX_CHARS','AppConfig.B07R2_REPORT_MAX_CHARS')
    s=s.replace('星核天枢B01-B07加强版完整检测报告','星核天枢B01-B07加强优化R2完整检测报告')
    return s
rw(main,patch_main,'R2 intelligent diagnosis + one-tap safe complete copy')

PLAN={
1:['AppConfig.java','BuildIdentity.java','V51BuildManifest.java','V51ArtifactBundleManifest.java','KeyProvider.java','ApiClient.java','ApiParser.java','SafeLog.java','BinanceGameContract.java','ApiTransportGovernanceEngine.java'],
2:['DataGuard.java','CountdownClock.java','Models.java','V51Models.java','SchemaContractRegistry.java','FeatureContractRegistry.java','DrawIntegrityRecoveryEngine.java'],
3:['DbHelper.java','DatabaseCoordinator.java','DatabaseMigrationEngine.java','DatabaseIntegrityVerifier.java','DbStore.java','V51Repository.java'],
4:['PointInTimeSnapshotEngine.java','V51PermutationFeatureEngine.java','V51TrajectoryFeatureEngine.java','V51ObservableStateVectorEngine.java','RollingWindowCache.java','FullInformationFeatureGraphEngine.java'],
5:['V51StateTrajectoryEngine.java','V51StatePrototypeEngine.java','V51ChangePointEngine.java','V51LifecycleEngine.java','WindowFeatureEngine.java','TrendStateMachine.java','BreakRecoveryEngine.java','CycleRhythmEngine.java','DistributionDriftEngine.java'],
6:['PatternStateEngine.java','ShapeStateEngine.java','TransitionRelationEngine.java','SimilarShapeEngine.java','ShapeMemoryEngine.java','NumberBehaviorEngine.java','AttributionEngine.java'],
7:['StrategyModuleCatalog.java','StrategyEvidenceContract.java','UnifiedStrategyEvidenceEngine.java','EvidenceGroupAggregatorEngine.java','EvidenceHealthSnapshotEngine.java','RuleHealthMatrixEngine.java','FalseStrongFreezeEngine.java','DynamicFreezeMatrixEngine.java','StarcoreEvidenceKernel.java']}
entries=[]
for b,names in PLAN.items():
    for name in names:
        p=SRC/name
        if p.exists():
            txt=p.read_text(encoding='utf-8',errors='replace')
            lines=txt.count('\n')+1
            effective=sum(1 for x in txt.splitlines() if x.strip() and not x.strip().startswith('//'))
            todo=len(re.findall(r'(?i)\\bTODO\\b|placeholder|return\\s+null\\s*;\\s*//\\s*TODO',txt))
            sha=hashlib.sha256(txt.encode('utf-8')).hexdigest()
            entries.append((b,name,1,lines,effective,todo,sha))
        else:
            entries.append((b,name,0,0,0,0,''))

def j(s): return s.replace('\\','\\\\').replace('"','\\"')
rows=[]
for e in entries:
    rows.append('        new Entry(%d, "%s", %s, %d, %d, %d, "%s")' % (e[0],j(e[1]),'true' if e[2] else 'false',e[3],e[4],e[5],e[6]))
manifest='''package com.ethk3.novaonepro;\n\n/** Generated from the exact B01-B07 construction contract at build time. */\npublic final class B07R2BuildAuditManifest {\n    private B07R2BuildAuditManifest() { }\n    public static final String VERSION = AppConfig.B07R2_VERSION;\n    public static final int EXPECTED_FILES = 54;\n    public static final Entry[] ENTRIES = new Entry[]{\n%s\n    };\n    public static String render() {\n        StringBuilder s=new StringBuilder(24000); int present=0,todo=0;\n        int[] exp=new int[]{0,10,7,6,6,9,7,9}; int[] got=new int[8];\n        for(int i=0;i<ENTRIES.length;i++){Entry e=ENTRIES[i];if(e.present){present++;got[e.batch]++;}todo+=e.todoHits;\n            s.append("B").append(e.batch<10?"0":"").append(e.batch).append('|').append(e.file)\n             .append("|present=").append(e.present).append("|lines=").append(e.lines).append("|effective=").append(e.effectiveLines)\n             .append("|todoHits=").append(e.todoHits).append("|sha256=").append(e.sha256).append('\\n');}\n        s.append("TOTAL=").append(present).append('/').append(EXPECTED_FILES).append(";todoHits=").append(todo).append('\\n');\n        for(int b=1;b<=7;b++)s.append("B0").append(b).append("_SOURCE_FILES=").append(got[b]).append('/').append(exp[b]).append(got[b]==exp[b]?"|PRESENT":"|MISSING").append('\\n');\n        return s.toString();\n    }\n    public static final class Entry {\n        public final int batch; public final String file; public final boolean present; public final int lines; public final int effectiveLines; public final int todoHits; public final String sha256;\n        Entry(int b,String f,boolean p,int l,int e,int t,String h){batch=b;file=f;present=p;lines=l;effectiveLines=e;todoHits=t;sha256=h;}\n    }\n}\n''' % (',\n'.join(rows))
mp=SRC/'B07R2BuildAuditManifest.java'
old=mp.read_text(encoding='utf-8') if mp.exists() else ''
mp.write_text(manifest,encoding='utf-8')
changes.append({'file':str(mp.relative_to(ROOT)),'change':'generated exact 54-file B01-B07 source audit manifest','before_sha256':hashlib.sha256(old.encode()).hexdigest() if old else 'NEW','after_sha256':hashlib.sha256(manifest.encode()).hexdigest(),'before_lines':old.count('\n')+1 if old else 0,'after_lines':manifest.count('\n')+1})

engine=r'''package com.ethk3.novaonepro;

import android.content.Context;
import android.content.SharedPreferences;
import java.lang.reflect.Method;
import java.util.Locale;

/** B01-B07 R2 full-state diagnostic doctor. Read-only. */
public final class B07R2FullDiagnosticEngine {
    private B07R2FullDiagnosticEngine() { }
    public static String build(Context context, DbStore store) {
        StringBuilder s=new StringBuilder(96000); long now=System.currentTimeMillis();
        s.append("【星核天枢 B01-B07 加强优化R2 · 全状态智能检测报告】\n");
        s.append("版本=").append(AppConfig.B07R2_VERSION).append("；schema=").append(AppConfig.B07R2_DIAG_SCHEMA).append('\n');
        s.append("范围=B01-B07返工；B08=NOT_STARTED_BY_CONTRACT；报告只读，不改策略，不造胜率。\n");
        s.append("游戏=").append(AppConfig.LOTTERY_CODE).append("；固定K=").append(AppConfig.FIXED_K).append("；单位置=").append(!AppConfig.ALLOW_MULTI_POSITION_BET).append("；DecisionGate唯一真实动作Owner=").append(AppConfig.STRICT_DECISION_GATE_ONLY_CAN_BET).append('\n');
        s.append("真实动作策略=").append(AppConfig.B07R2_REAL_ACTION_POLICY).append("；伪造MODEL_PROVEN=").append(!AppConfig.B07R2_NO_FAKE_MODEL_PROOF).append('\n');
        section(s,"1. B01-B07 54文件构建期审计"); s.append(B07R2BuildAuditManifest.render());
        s.append("说明：文件存在/行数/SHA只证明源码交付，不等于策略效果已证明；效果必须看PIT/OOS和实时结算。\n");
        section(s,"2. 唯一主链/Owner/旁路冲突");
        s.append("合同主链=ApiClient/ApiParser -> DataGuard -> PIT/FeatureGraph -> State/Trend/Pattern/Relation/Memory -> EvidencePacket -> Evidence聚合 -> Probability/Calibration -> DynamicGate -> DecisionGateEngine -> Ledger/Settlement\n");
        s.append("ACTIVE_V51_TIANSHU=").append(AppConfig.ENABLE_V51_TIANSHU).append("；ENABLE_V50_PRIME=").append(AppConfig.ENABLE_V50_PRIME).append("；V50证明门作用域=").append(AppConfig.B07R2_V50_PROOF_SCOPE).append('\n');
        states(s,store,new String[]{"v44_pipeline_invariant_detail","v44_decision_funnel_detail","strategy_module_catalog_summary","strategy_module_catalog_validation","module_effect_summary","dynamic_freeze_summary","b07r2_real_action_owner","b07r2_real_action_status","b07r2_real_action_target"});
        String chain=joinStates(store,new String[]{"v44_pipeline_invariant_detail","v44_decision_funnel_detail","strategy_module_catalog_validation","module_effect_summary"}).toUpperCase(Locale.US);
        if (containsAny(chain,new String[]{"DUPLICATE_OWNER","BYPASS","CONFLICT","FUTURE_LEAK","SHADOW_POLLUTION"})) s.append("主链判定=CONFLICT_DETECTED；先解决Owner/未来泄漏/重复计权，再谈胜率。\n"); else s.append("主链判定=未从现有状态字段发现显式Owner冲突；仍以运行回放Hash和OOS证据为最终验收。\n");
        section(s,"3. 后台常驻/退出软件/息屏/恢复链"); runtime(s,context,store,now);
        section(s,"4. 闪退/OOM/线程/SQLite健康"); crash(s,context,store,now); states(s,store,new String[]{"database_health_summary","db_queue_state","last_error","analysis_runtime_state","b07r2_low_memory_at","b07r2_low_memory_action","b07r2_trim_memory_level"});
        section(s,"5. API/开奖/目标期/数据完整性"); states(s,store,new String[]{"last_api_success_at","last_api_error","last_fetch_issue","draw_integrity_summary","history_integrity_summary","target_issue_status","last_prediction_issue","last_prediction_success_at","last_analysis_success_at","last_settlement_at"});
        section(s,"6. 真实出手/SHADOW/结算口径");
        s.append("真实动作=允许严格DecisionGate产生BET；不会因为B07R2把质量门直接拆掉；不会把SHADOW/HOLD伪装成真实出手。\n");
        s.append("V50 typed proof-lock若MODEL_PROVEN=false仍保持SHADOW；因为当前V50_PRIME=false，该锁不应支配V51天枢活动主链。\n");
        states(s,store,new String[]{"real_bet_diagnosis_summary","real_bet_diagnosis_detail","shadow_promotion_summary","shadow_promotion_detail","decision_authorization_summary","settlement_diagnosis_summary","b07r2_real_action_status"});
        s.append("真实胜率口径=REAL_TRADE + BET + 已结算WIN/MISS；SHADOW只做反事实，HOLD不计真实盈亏/ECE。\n");
        section(s,"7. 胜率/频率/ECE/OOS是否达标");
        String evidence=joinStates(store,new String[]{"diagnosis_closed_loop_summary","real_bet_diagnosis_summary","adaptive_judge_summary","calibration_summary","coverage_frontier_summary","micro_walk_forward_summary","strict_oos_summary"});
        states(s,store,new String[]{"diagnosis_closed_loop_summary","adaptive_judge_summary","calibration_summary","coverage_frontier_summary","micro_walk_forward_summary","strict_oos_summary"});
        Metrics m=Metrics.parse(evidence); renderMetrics(s,m); diagnoseTargets(s,m,evidence);
        section(s,"8. 为什么不达标：动态根因优先级"); rootCause(s,context,store,now,m,evidence);
        section(s,"9. 策略方法算法公式是否满血/是否冲突");
        s.append("检查维度=信息增益、独立性、校准、漂移鲁棒、第三号贡献、窗口稳定、位置稳定、运行时成本、OOS跨块一致。\n");
        states(s,store,new String[]{"module_effect_summary","module_score_summary","ablation_summary","rule_health_summary","dynamic_freeze_summary","teacher_matrix_summary","counterfactual_summary","third_guard_summary"});
        s.append("核心公式A 模块可靠度：w_i=max(0,LCB_i-0.30)*stability_i*freshness_i*(1-corrPenalty_i)。\n");
        s.append("核心公式B 去相关融合：z=sum(w_i*logit(p_i))/max(eps,sum(abs(w_i)))；p=sigmoid(z)，同源模块相关越高权重越低。\n");
        s.append("核心公式C 校准目标：min Brier + lambda*ECE + gamma*跨时间块方差；小样本用层级收缩，不允许抬高概率。\n");
        s.append("核心公式D 漂移门：risk=max(JS,PSI,CUSUM,BOCPD_changeP)；漂移高时缩权/转SHADOW，而不是频率债务补单。\n");
        section(s,"10. 40/50/60%与频率15%+的具体施工建议"); strategyPlan(s,m,evidence);
        section(s,"11. 本版修改后的验收结论");
        s.append("B08=未开始。B01-B07源码完整度以第1节54文件审计为准；运行完美度以第3-8节实时证据为准。\n");
        s.append("本报告不会声称50/60%已达标。达到50/60只能由新的可重复信息增益 + 严格PIT/OOS + Wilson下界 + 多块一致性证明，不能靠多写几个算法名。\n");
        s.append("如果运行层STALE/CRASH/OOM，优先P0修运行；如果运行健康但OOS低，才进入策略/特征/Ranker/校准的模型层升级。\n");
        return clip(s.toString(),AppConfig.B07R2_REPORT_MAX_CHARS);
    }
    private static void runtime(StringBuilder s,Context c,DbStore d,long now){try{ServiceHeartbeatStore h=new ServiceHeartbeatStore(c.getApplicationContext());long age=h.getHeartbeatAgeMs();s.append("heartbeatAgeMs=").append(age).append(";expired=").append(h.isHeartbeatExpired()).append(";businessStale=").append(h.isBusinessStale()).append(";runtimeLeaseFresh=").append(h.isRuntimeLeaseFresh()).append(";userEnabled=").append(h.isUserEnabled()).append('\n');boolean healthy=!h.isHeartbeatExpired()&&h.isRuntimeLeaseFresh()&&!h.isBusinessStale();s.append("后台判定=").append(healthy?"HEALTHY":"STALE_OR_RECOVERING").append("；UI退出后服务应继续；只有显式STOP才允许永久停止。\n");}catch(Throwable e){s.append("runtimeSnapshot=ERROR:").append(e.getClass().getSimpleName()).append('\n');}states(s,d,new String[]{"last_service_started_at","last_service_command_at","last_task_removed_at","b07r2_last_task_removed_rearm_at","service_foreground_main_loop","last_prediction_success_at","last_analysis_success_at","last_api_success_at"});}
    private static void crash(StringBuilder s,Context c,DbStore d,long now){try{SharedPreferences p=c.getApplicationContext().getSharedPreferences("v20_crash_snapshot",Context.MODE_PRIVATE);long at=p.getLong("at",0L);s.append("crashCount=").append(p.getInt("crash_count",0)).append(";window10m=").append(p.getInt("crash_window_count",0)).append(";oomCount=").append(p.getInt("oom_count",0)).append(";soeCount=").append(p.getInt("soe_count",0)).append('\n');s.append("lastCrashAgeMs=").append(at<=0?-1:Math.max(0L,now-at)).append(";type=").append(p.getString("type","")).append(";stage=").append(p.getString("stage","")).append(";fingerprint=").append(p.getString("fingerprint","")).append('\n');s.append("memory free/total/max=").append(Runtime.getRuntime().freeMemory()).append('/').append(Runtime.getRuntime().totalMemory()).append('/').append(Runtime.getRuntime().maxMemory()).append('\n');}catch(Throwable e){s.append("crashSnapshot=ERROR:").append(e.getClass().getSimpleName()).append('\n');}}
    private static void renderMetrics(StringBuilder s,Metrics m){s.append("解析指标 hit=").append(fmt(m.hit)).append(";coverage=").append(fmt(m.coverage)).append(";ece=").append(fmt(m.ece)).append(";settled=").append(m.settled).append(";wilsonLower=").append(fmt(m.wilsonLower)).append('\n');s.append("40%目标=").append(rateVerdict(m,0.40d)).append("；50%目标=").append(rateVerdict(m,0.50d)).append("；60%目标=").append(rateVerdict(m,0.60d)).append("；频率15%=").append(m.coverage<0?"NO_EVIDENCE":(m.coverage>=0.15d?"OBSERVED_PASS":"OBSERVED_LOW")).append('\n');}
    private static void diagnoseTargets(StringBuilder s,Metrics m,String raw){String u=safe(raw).toUpperCase(Locale.US);if(u.contains("NOT_PROVEN")||u.contains("NO_SAMPLE")||m.settled>=0&&m.settled<AppConfig.B07R2_MIN_SETTLED_FOR_RATE_CLAIM){s.append("正式结论=NOT_PROVEN：样本/证明门不足，不得宣称40/50/60。\n");return;}if(m.ece>=0&&m.ece>0.04d)s.append("校准=FAIL(ECE>4%)；先降虚高概率再扩大频率。\n");if(m.hit>=0&&m.hit<0.40d)s.append("命中=低于40%工程目标；需要找负贡献模块/第三号拖累/状态漂移/Ranker弱项，不允许靠降门槛硬出手。\n");if(m.coverage>=0&&m.coverage<0.15d)s.append("频率=低于15%；只能扩大已证明状态原型/互补专家覆盖，不能用频率债务补单。\n");}
    private static void rootCause(StringBuilder s,Context c,DbStore d,long now,Metrics m,String raw){boolean runtimeBad=false;try{ServiceHeartbeatStore h=new ServiceHeartbeatStore(c.getApplicationContext());runtimeBad=h.isHeartbeatExpired()||!h.isRuntimeLeaseFresh()||h.isBusinessStale();}catch(Throwable e){runtimeBad=true;}String all=(safe(raw)+' '+joinStates(d,new String[]{"last_error","module_effect_summary","dynamic_freeze_summary","rule_health_summary"})).toUpperCase(Locale.US);if(runtimeBad||all.contains("OOM")||all.contains("CRASH"))s.append("P0=运行层：后台中断/假活/OOM/崩溃会直接造成不抓取、不分析、不预测；先修运行，策略胜率暂不评价。\n");if(all.contains("FUTURE")||all.contains("LEAK")||all.contains("OUT_OF_ORDER")||all.contains("DATA_ERROR"))s.append("P0=数据/PIT：未来泄漏、乱序或目标期错位会让回测虚高或实时失真。\n");if(all.contains("DUPLICATE")||all.contains("CONFLICT")||all.contains("BYPASS"))s.append("P0=Owner冲突：重复概率Owner/旁路BET/重复Evidence会使主链不可信。\n");if(m.hit>=0&&m.hit<0.40d)s.append("P1=模型效果：严格OOS命中不足；优先做逐模块消融、第三号条件排名、状态分层校准、低相关融合。\n");if(m.coverage>=0&&m.coverage<0.15d)s.append("P1=覆盖不足：扩高质量状态域和互补专家，不降低真实动作门。\n");if(m.ece>=0&&m.ece>0.04d)s.append("P1=校准：概率虚高/虚低；使用层级校准并限制小样本。\n");if(!runtimeBad&&m.hit<0&&!all.contains("NOT_PROVEN"))s.append("P2=证据解析不足：状态字段没有稳定的结构化指标；先补结构化诊断账本。\n");}
    private static void strategyPlan(StringBuilder s,Metrics m,String raw){s.append("A. ExpertCorrelationAuditEngine：EvidenceGroup相关矩阵/互信息/重复证据审计，高相关模块降权。\n");s.append("B. AblationLedgerEngine：逐模块严格时间块开启/关闭，对比Hit/Coverage/Brier/ECE/WilsonLCB，负贡献自动WATCH/FREEZE。\n");s.append("C. ThirdNumberConditionalRankEngine：建模第三号边际贡献P(hit|n1,n2,state)-P(hit|n1,n2)，负贡献则重排。\n");s.append("D. HierarchicalCalibrationEngine：position×regime×evidence-domain分层Beta/分桶收缩，解决小样本概率虚高。\n");s.append("E. RegimeDriftCertificateEngine：JS/PSI/CUSUM/BOCPD联合漂移证书，只缩权失效专家。\n");s.append("F. CertifiedCoverageExpansionEngine：从SHADOW中寻找跨4时间块稳定、WilsonLCB过线的状态原型逐级晋级，Coverage先15-20%，再20-30%。\n");s.append("G. RankerResidualMiningEngine：统计真实号码落在rank4-6/7-10的条件残差，训练pairwise/listwise残差修正。\n");s.append("40%路线=先把运行/PIT/Owner清零，再让OOS Hit>=40%、Coverage>=15%、ECE<=4%、多块一致；50/60路线=必须发现额外、互补、可重复的信息源，否则算法复杂度本身不会创造预测信息。\n");s.append("频率路线=扩大已证明域，不动硬风险门；稳定性路线=相关惩罚+漂移缩权+层级校准+消融冻结。\n");}
    private static String rateVerdict(Metrics m,double target){if(m.hit<0)return "NO_EVIDENCE";if(m.settled>=0&&m.settled<AppConfig.B07R2_MIN_SETTLED_FOR_RATE_CLAIM)return "SAMPLE_TOO_SMALL";if(m.wilsonLower>=0)return m.wilsonLower>=target?"PROVEN_BY_PARSED_LCB":"NOT_PROVEN_LCB";return m.hit>=target?"OBSERVED_ONLY_NOT_PROVEN":"OBSERVED_LOW";}
    private static String fmt(double x){return x<0?"NA":String.format(Locale.US,"%.2f%%",x*100.0d);}
    private static void section(StringBuilder s,String x){s.append("\n【").append(x).append("】\n");}
    private static void states(StringBuilder s,DbStore d,String[] keys){for(int i=0;i<keys.length;i++){String v=state(d,keys[i]);s.append(keys[i]).append('=').append(v.length()==0?"NO_DATA":clip(v,AppConfig.B07R2_REPORT_RAW_STATE_CAP)).append('\n');}}
    private static String joinStates(DbStore d,String[] keys){StringBuilder s=new StringBuilder();for(int i=0;i<keys.length;i++)s.append(state(d,keys[i])).append(' ');return s.toString();}
    private static String state(DbStore d,String key){if(d==null)return "";try{Method m=d.getClass().getMethod("getState",String.class);Object v=m.invoke(d,key);return v==null?"":String.valueOf(v);}catch(Throwable e){return "";}}
    private static String clip(String x,int n){if(x==null)return "";return x.length()<=n?x:x.substring(0,n)+"...[RAW_TRIMMED]";}
    private static String safe(String x){return x==null?"":x;}
    private static boolean containsAny(String x,String[] a){for(int i=0;i<a.length;i++)if(x.contains(a[i]))return true;return false;}
    static final class Metrics{double hit=-1,coverage=-1,ece=-1,wilsonLower=-1;int settled=-1;static Metrics parse(String raw){Metrics m=new Metrics();String x=safe(raw);m.hit=percentAny(x,new String[]{"Hit=","命中率=","hitRate=","realHitRate="});m.coverage=percentAny(x,new String[]{"Coverage=","覆盖率=","coverage="});m.ece=percentAny(x,new String[]{"ECE=","ece="});m.wilsonLower=percentAny(x,new String[]{"WilsonLower=","Wilson95%下界=","LCB="});m.settled=intAny(x,new String[]{"settled=","已结算=","结算="});return m;}static double percentAny(String x,String[] keys){for(int i=0;i<keys.length;i++){int p=x.indexOf(keys[i]);if(p>=0){p+=keys[i].length();StringBuilder n=new StringBuilder();while(p<x.length()){char c=x.charAt(p++);if((c>='0'&&c<='9')||c=='.'||c=='-')n.append(c);else if(n.length()>0)break;}try{double v=Double.parseDouble(n.toString());return v>1.0d?v/100.0d:v;}catch(Throwable e){}}}return -1;}static int intAny(String x,String[] keys){for(int i=0;i<keys.length;i++){int p=x.indexOf(keys[i]);if(p>=0){p+=keys[i].length();StringBuilder n=new StringBuilder();while(p<x.length()){char c=x.charAt(p++);if(c>='0'&&c<='9')n.append(c);else if(n.length()>0)break;}try{return Integer.parseInt(n.toString());}catch(Throwable e){}}}return -1;}}
}
'''
ep=SRC/'B07R2FullDiagnosticEngine.java'
old=ep.read_text(encoding='utf-8') if ep.exists() else ''
ep.write_text(engine,encoding='utf-8')
changes.append({'file':str(ep.relative_to(ROOT)),'change':'new full-state dynamic diagnosis doctor','before_sha256':hashlib.sha256(old.encode()).hexdigest() if old else 'NEW','after_sha256':hashlib.sha256(engine.encode()).hexdigest(),'before_lines':old.count('\n')+1 if old else 0,'after_lines':engine.count('\n')+1})

(ROOT/'reports').mkdir(exist_ok=True)
(ROOT/'reports/B07R2_CODE_CHANGE_MANIFEST.json').write_text(json.dumps({'version':'B07R2','files':changes},ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({'changed_files':len(changes),'files':[x['file'] for x in changes]},ensure_ascii=False,indent=2))
