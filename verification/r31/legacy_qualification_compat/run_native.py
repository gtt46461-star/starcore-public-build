#!/usr/bin/env python3
"""Actual append-only legacy qualification compatibility on native SQLite.
Private copied fixed app support, exact baseline/candidate/new producers, no Gradle.
"""
from pathlib import Path
import ast,datetime,gzip,hashlib,json,os,shutil,subprocess,tempfile

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
PRIOR=ROOT/'verification/r31/live_writer_budgets'
BASE='5d1da3156a7900816aa0f580e34c18fabf5ef322'
OUT=HERE/'runs'/datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
OUT.mkdir(parents=True,exist_ok=False)
TMP=Path(tempfile.mkdtemp(prefix='r31-legacy-qualification-native-'))
sha=lambda data:hashlib.sha256(data).hexdigest()
SUPPORT=TMP/'fixed-support.jar';SDK=TMP/'android.jar';JDBC=TMP/'sqlite-jdbc.jar'
for source,target in [('/tmp/r31-p02-012-actual-android-support/classes.jar',SUPPORT),('/workspace/starcore-environment/android-sdk/platforms/android-36/android.jar',SDK),('/tmp/r31-baseline-deps/sqlite-jdbc.jar',JDBC)]:
    shutil.copy2(source,target)
sources=[Path('app/src/main/java/com/ethk3/novaonepro/strategyv2')/(name+'.java') for name in ['LiveShadowsR27','QualificationInboxR29']]
source_hashes={str(p):sha((ROOT/p).read_bytes()) for p in sources}
receipt={'schema':'R31_APPEND_ONLY_LEGACY_QUALIFICATION_NATIVE_1','started_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'base_commit':BASE,'support_jar_sha256':sha(SUPPORT.read_bytes()),'sdk_sha256':sha(SDK.read_bytes()),'jdbc_sha256':sha(JDBC.read_bytes()),'private_workspace':str(TMP),'production_source_sha256':source_hashes,'runner_sha256':sha(Path(__file__).read_bytes()),'harness_sha256':sha((HERE/'LegacyCompatibilityHostR31.java').read_bytes()),'adapter_sha256':sha((HERE/'adapters/android/database/sqlite/SQLiteDatabase.java').read_bytes()),'bootstrap_harness_sha256':sha((PRIOR/'LiveBudgetHostR31.java').read_bytes()),'device':'NOT_RUN','gradle':'NOT_RUN','natural_quality':'NOT_PROVEN','commands':[],'results':[]}
def run(name,argv,expected=0,marker=None):
    result=subprocess.run(list(map(str,argv)),cwd=TMP,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=240)
    data=result.stdout.encode();log=OUT/(name+'.log.gz')
    with gzip.open(log,'wb') as f:f.write(data)
    frames=[json.loads(l[6:]) for l in result.stdout.splitlines() if l.startswith('FRAME ')]
    with gzip.open(OUT/(name+'.frames.json.gz'),'wt') as f:json.dump(frames,f)
    record={'name':name,'argv':list(map(str,argv)),'exit_code':result.returncode,'expected_exit_code':expected,'original_log':log.name,'log_sha256':sha(data),'frames':len(frames)};receipt['commands'].append(record)
    facts=[json.loads(l[7:]) for l in result.stdout.splitlines() if l.startswith('RESULT ')]
    checks=[l for l in result.stdout.splitlines() if l.startswith(('CHECK ','LEGACY_COMPAT_NATIVE_HOST_PASS','EXPLAIN_GUARD '))]
    if facts or checks:receipt['results'].append({'name':name,'facts':facts,'checks':checks})
    (OUT/'receipt.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({'name':name,'exit':result.returncode,'expected':expected,'frames':len(frames),'tail':result.stdout[-1200:] if result.returncode!=expected else result.stdout[-300:]}),flush=True)
    if result.returncode!=expected or marker and marker not in result.stdout:raise RuntimeError('Unexpected original result retained: '+name)
    return result.stdout

compiler=['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-g']
try:
    platform=TMP/'platform';platform.mkdir();src=TMP/'platform-source';src.mkdir();shims=[]
    for name in ['android/database/Cursor.java','android/content/ContentValues.java','android/database/sqlite/SQLiteOpenHelper.java']:
        p=src/name;p.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(ROOT/'tools/r31/ddl_host'/name,p);shims.append(p)
    p=src/'android/database/sqlite/SQLiteDatabase.java';shutil.copy2(HERE/'adapters/android/database/sqlite/SQLiteDatabase.java',p);shims.append(p)
    for name in ['SQLiteException','SQLiteConstraintException','SQLiteDatabaseLockedException']:
        original=ROOT/'tools/r29/qual_host/shim/android/database/sqlite'/(name+'.java')
        if original.exists():p=src/'android/database/sqlite'/(name+'.java');shutil.copy2(original,p);shims.append(p)
    parsed=ast.parse((PRIOR/'run_native.py').read_text())
    adapters=ast.literal_eval(next(node.value for node in parsed.body if isinstance(node,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='adapters' for t in node.targets)))
    for name,content in adapters.items():p=src/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content);shims.append(p)
    run('platform-compile',[*compiler,'-cp',os.pathsep.join(map(str,[SDK,SUPPORT,JDBC])),'-d',platform,*shims])
    builds={}
    for version in ['baseline','race-candidate','count-candidate','new']:
        source=TMP/(version+'-source');classes=TMP/(version+'-classes');tests=TMP/(version+'-tests');classes.mkdir();tests.mkdir();files=[]
        for relative in sources:
            if relative.stem=='LiveShadowsR27':data=subprocess.check_output(['git','show',BASE+':'+str(relative)],cwd=ROOT)
            elif version=='baseline':data=subprocess.check_output(['git','show',BASE+':'+str(relative)],cwd=ROOT)
            elif version=='race-candidate':data=(HERE/'receipt_race_counterexample/QualificationInboxR29.pre_guard_candidate.java').read_bytes()
            elif version=='count-candidate':data=(HERE/'writer_count_counterexample/QualificationInboxR29.count_guard_candidate.java').read_bytes()
            else:data=(ROOT/relative).read_bytes()
            p=source/relative;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data);files.append(p)
            archive=OUT/'source_snapshot'/version/relative;archive.parent.mkdir(parents=True,exist_ok=True);archive.write_bytes(data)
        run(version+'-producer-compile',[*compiler,'-cp',str(SDK)+os.pathsep+str(SUPPORT),'-sourcepath',TMP/'empty-sourcepath','-d',classes,*files])
        cp=os.pathsep.join(map(str,[platform,classes,tests,SUPPORT,JDBC,SDK]));builds[version]=cp
        testsources=[PRIOR/'LiveBudgetHostR31.java',HERE/'LegacyCompatibilityHostR31.java']
        if version=='race-candidate':testsources.append(HERE/'receipt_race_counterexample/LegacyReceiptRaceProofR31.java')
        run(version+'-harness-compile',[*compiler,'-cp',cp,'-d',tests,*testsources])
    main='com.ethk3.novaonepro.strategyv2.LegacyCompatibilityHostR31'
    run('baseline-original-erasure',['java','-Xmx512m','-cp',builds['baseline'],main,'baseline',TMP/'baseline-fixture'],1,'BASELINE_FAIL_REPRODUCED_LEGACY_EXPOSURE_RESET_AND_RECEIPT_ERASURE')
    run('candidate-original-receipt-race',['java','-Xmx512m','-cp',builds['race-candidate'],'com.ethk3.novaonepro.strategyv2.LegacyReceiptRaceProofR31',TMP/'race-fixture'],1,'BASELINE_FAIL_REPRODUCED_LEGACY_RECEIPT_MUTATION_AFTER_PROOF')
    run('candidate-unbounded-writer-vm-scan',['java','-Xmx512m','-cp',builds['count-candidate'],main,'vm-counterexample',TMP/'vm-counterexample-fixture'],1,'CANDIDATE_FAIL_REPRODUCED_UNBOUNDED_LEGACY_WRITER_COUNT_SCAN')
    (TMP/'new-fixtures').mkdir();shutil.copy2(HERE/'foldback_duplicate_counterexample/actual_v1_no_replace.sql',TMP/'new-fixtures/actual-v1-guard.sql')
    for mode in ['vm-backlog','concurrent','normal','positive','migration','negative','owner','diagnostic','retention','foldback','upgrade','rollback']:
        run('new-'+mode,['java','-Xmx512m','-cp',builds['new'],main,mode,TMP/'new-fixtures'])
    run('new-actual-jvm-halt',['java','-Xmx512m','-cp',builds['new'],main,'halt',TMP/'halt-fixture'],73)
    run('new-fresh-jvm-resume',['java','-Xmx512m','-cp',builds['new'],main,'resume',TMP/'halt-fixture'])
    for mode in ['settle','pending','consume','resolve']:
        run('unchanged-normal-'+mode,['java','-Xmx512m','-cp',builds['new'],'com.ethk3.novaonepro.strategyv2.LiveBudgetHostR31',mode,TMP/'normal-regression-fixtures'])
    changed=[str(p) for p in sources if sha((ROOT/p).read_bytes())!=source_hashes[str(p)]]
    if changed:raise RuntimeError('Production source changed during execution: '+str(changed))
    receipt.update(status='APPEND_ONLY_LEGACY_COMPATIBILITY_NATIVE_HOST_PASS_NOT_ANDROID',source_changed_during_run=changed,limitations=['Root startup migration journal/SchemaR27 integration is owned/tested by root; this run uses actual coordinator MIGRATION transactions and the exact owned DDL/checker APIs.','Host timings and synthetic original rows are not Android/device latency or natural qualification evidence.','Guard indexed-read cost is a conservative per-INSERT addition to traced API SQL, with actual SQLite trigger program EXPLAIN retained.','Existing Retention phase17 compatibility is exercised separately; its prior row budget is not accepted by this producer repair.','Unprovable legacy experiments remain blocked with original evidence retained; no alpha, trial identity or sample state is reset.'])
except Exception as error:
    receipt.update(status='EXECUTION_FAILURE_NOT_PASS',failure=str(error));raise
finally:
    receipt['finished_utc']=datetime.datetime.now(datetime.timezone.utc).isoformat();(OUT/'receipt.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n');print('RECEIPT '+str(OUT/'receipt.json'),flush=True)
