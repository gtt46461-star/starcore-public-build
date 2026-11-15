#!/usr/bin/env python3
"""Record real commands for the authorized R32.5.1 construction worktree."""
import datetime, hashlib, json, os, subprocess, sys
from pathlib import Path

ROOT = Path(os.environ.get('STARCORE_EVIDENCE_ROOT',str(Path(__file__).resolve().parent))).resolve()
PACKAGE = Path(os.environ.get('STARCORE_PACKAGE','/workspace/starcore-plan-r3251/construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/R32_5_1_restored/STARCORE_R32_5_FINAL_REPAIR_20261115')).resolve()
REPO = Path(os.environ.get('STARCORE_BUILD_REPO','/workspace/starcore-r3251-build')).resolve()
TC = Path('/workspace/starcore-r3251-toolchain')
PY = sys.executable
SDK = TC/'android.jar'
JDBC = TC/'sqlite-jdbc-3.46.1.3.jar'
SLF = TC/'slf4j-api-2.0.13.jar'
JSON = TC/'json-20240303.jar'

def sha(p):
    h = hashlib.sha256()
    with Path(p).open('rb') as f:
        for b in iter(lambda: f.read(1048576), b''): h.update(b)
    return h.hexdigest()

def now(): return datetime.datetime.now(datetime.timezone.utc).isoformat()

def run(label, argv, cwd=REPO, env=None):
    log = ROOT/(label+'.log')
    receipt = ROOT/(label+'.command.json')
    if log.exists() or receipt.exists(): raise ValueError('REFUSE_OVERWRITE '+label)
    data = dict(id=label, argv=list(map(str,argv)), cwd=str(cwd), startedAt=now(),
                packageManifestSha256=sha(PACKAGE/'code_manifest.json'),
                sourceCommit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=REPO,text=True).strip(),
                sourceTree=subprocess.check_output(['git','rev-parse','HEAD^{tree}'],cwd=REPO,text=True).strip(),
                status='RUNNING', log=log.name)
    receipt.write_text(json.dumps(data,indent=2)+'\n')
    print('START '+label,flush=True)
    with log.open('xb') as f:
        proc = subprocess.run(data['argv'],cwd=cwd,env=env,stdout=f,stderr=subprocess.STDOUT)
    data.update(exitCode=proc.returncode,finishedAt=now(),logSha256=sha(log),status='PASS_COMMAND' if proc.returncode==0 else 'FAILED_COMMAND')
    receipt.write_text(json.dumps(data,indent=2)+'\n')
    print('END '+label+' exit='+str(proc.returncode),flush=True)
    if proc.returncode:
        print(log.read_text(errors='replace')[-10000:],flush=True)
        raise SystemExit(proc.returncode)

def batch(b):
    run(b+'-inspect',[PY,PACKAGE/'tools/inspect_resume.py','--package',PACKAGE,'--repo',REPO])
    run(b+'-check',[PY,PACKAGE/'tools/apply_batch.py','--package',PACKAGE,'--repo',REPO,'--batch',b])
    run(b+'-apply',[PY,PACKAGE/'tools/apply_batch.py','--package',PACKAGE,'--repo',REPO,'--batch',b,'--apply'])
    run(b+'-identity',[PY,PACKAGE/'tools/verify_stage_identity.py','--package',PACKAGE,'--repo',REPO,'--batch',b,'--generate'])
    cl = ROOT/(b+'-classes')
    run(b+'-compile',[PY,PACKAGE/'tools/compile_rehearsal.py','--repo',REPO,'--android-jar',SDK,'--expected-sdk-sha256','d9eb9da824d9e247a352f570f01e1169e725b2954bca9e283a71786c59b59f9a','--out',cl])
    entries = {p.relative_to(cl).as_posix():sha(p) for p in sorted(cl.rglob('*.class'))}
    (ROOT/(b+'-classes.manifest.json')).write_text(json.dumps(entries,indent=2)+'\n')
    if b=='P1':
        run(b+'-host',[PY,REPO/'tools/r32/p1_current/run_p1_current_host.py','--repo',REPO,'--classes',cl,'--sdk',SDK,'--jdbc',JDBC,'--slf4j',SLF,'--out',ROOT/(b+'-host')])
    elif b=='P2':
        run(b+'-recovery',[PY,REPO/'tools/r32/run_atomnet_recovery_host.py','--classes',cl,'--android-jar',SDK,'--sqlite-jdbc',JDBC,'--slf4j-api',SLF,'--out',ROOT/(b+'-recovery')])
        for label,script in [('archive-liveness','tools/r325/archive_liveness/run_archive_liveness.py'),('retention','tools/r324/retention/run_retention_host.py'),('gc','tools/r324/gc/run_gc_closure.py'),('closure','tools/r324/retention/run_closure_host.py')]:
            a=[PY,REPO/script,'--repo',REPO,'--classes',cl,'--android-jar',SDK,'--jdbc-jar',JDBC,'--slf4j-jar',SLF,'--out',ROOT/(b+'-'+label)]
            if label=='closure':a+=['--json-jar',JSON]
            run(b+'-'+label,a)
    elif b=='P3':
        run(b+'-host',[PY,REPO/'tools/r32/run_mainline_host.py','--repo',REPO,'--classes',cl,'--android-jar',SDK,'--sqlite-jdbc',JDBC,'--slf4j',SLF,'--out',ROOT/(b+'-host')])
    elif b=='P4':
        run(b+'-diagnostic',[PY,REPO/'tools/r324/diagnostic/run_diagnostic.py','--repo',REPO,'--out',ROOT/(b+'-diagnostic')])
        run(b+'-q08-originals',[PY,REPO/'tools/r325/diagnostic/run_q08_diagnostic.py','--repo',REPO,'--dependency-classes',cl,'--android-jar',SDK,'--sqlite-jdbc',JDBC,'--slf4j-jar',SLF,'--out',ROOT/(b+'-q08-originals')])
        env=os.environ.copy();env.update(R33_DEPENDENCY_CLASSES=str(cl),R33_ANDROID_JAR=str(SDK),R33_SQLITE_JDBC=str(JDBC),R33_SLF4J_JAR=str(SLF),R33_INPUT_DIR=str(PACKAGE/'evidence/report_inputs'),R33_REPORT_OUT=str(ROOT/(b+'-report')))
        run(b+'-report',['bash',REPO/'tools/r33/report_audit/run_report_reaudit.sh'],env=env)
    elif b=='P5':
        run(b+'-device-tools',[PY,REPO/'tools/r31/test_device_receipts.py'])
        for label,script in [('apply-contract','test_apply_batch.py'),('resume-contract','test_inspect_resume.py'),('file-card-contract','test_verify_package.py')]:
            run(b+'-'+label,[PY,PACKAGE/'tools'/script],cwd=PACKAGE)
        run(b+'-release-contract',[PY,PACKAGE/'tools/test_release_contract.py','--baseline',ROOT/'exact-baseline'],cwd=PACKAGE)
        run(b+'-r926-check',[PY,REPO/'scripts/r926_release_manifest.py','--check'])
        run(b+'-r926-drift',[PY,REPO/'scripts/r926_release_manifest.py','--drift-test'])
        run(b+'-r982-check',[PY,REPO/'scripts/r982_source_call_asset.py'])
    after = {p.relative_to(cl).as_posix():sha(p) for p in sorted(cl.rglob('*.class'))}
    if entries!=after:raise ValueError('CLASSES_MUTATED '+b)
    print('CURRENT_STAGE_HOST_COMMANDS_PASSED '+b,flush=True)

def bound_hosts(b):
    mapping=json.loads((PACKAGE/'contracts/ACCEPTANCE_EXECUTION_MAP_222.json').read_text())
    ids=sorted({e[5:] for x in mapping['items'] if x['runAt']==b for e in x.get('entries',[]) if e.startswith('HOST:')})
    suite=json.loads((REPO/'construction/r31/HOST_SUITE.json').read_text())
    commands={x['id']:x.get('command') for x in suite['host'] if not x.get('retired')}
    failures=[]
    for entry in ids:
        label=b+'-bound-'+entry
        previous=ROOT/(label+'.command.json')
        if previous.exists():
            state=json.loads(previous.read_text())
            if state.get('exitCode')==0:continue
            retries=[json.loads(p.read_text()) for p in ROOT.glob(label+'-*.command.json')]
            if any(x.get('exitCode')==0 for x in retries):continue
            if state.get('status')=='RUNNING':
                label+='-after-restart'
                if (ROOT/(label+'.command.json')).exists():failures.append(entry);continue
            else:
                failures.append(entry);continue
        try:run(label,['bash','-c',commands[entry]])
        except SystemExit:failures.append(entry)
    if failures:
        print('BOUND_HOST_FAILURES '+b+' '+json.dumps(failures),flush=True)
        raise SystemExit(1)
    print('BOUND_HOST_COMMANDS_PASSED '+b+' count='+str(len(ids)),flush=True)

if __name__=='__main__':
    os.umask(0o022)
    if sys.argv[1]=='batch':batch(sys.argv[2])
    elif sys.argv[1]=='bound-hosts':bound_hosts(sys.argv[2])
    elif sys.argv[1]=='run':run(sys.argv[2],sys.argv[3:])
