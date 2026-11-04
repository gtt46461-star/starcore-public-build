from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,os,shlex,subprocess,time,signal
ROOT=Path('/workspace/r31-014-host-integration')
WORK=Path('/tmp/r31-014-host-current-workspace.txt').read_text().strip();WORK=Path(WORK)
setup=json.loads((WORK/'setup.json').read_text())
logs=WORK/'logs';logs.mkdir(exist_ok=True)
initial={str(p.relative_to(ROOT)):(p.stat().st_size,p.stat().st_mtime_ns) for p in (ROOT/'verification/r31').rglob('*') if p.is_file()}
(WORK/'verification-before-stat.json').write_text(json.dumps(initial,indent=2)+'\n')
env=os.environ.copy();env.update({'SQLITE_JDBC':setup['jdbcPath'],'R31_HOST_CLASSES':setup['frozenJar'],'R31_HOST_SUPPORT_CLASSES':setup['frozenJar'],'R31_MEMORY_CLASSES':setup['frozenClasses'],'R31_MEMORY_CONTROL_SUPPORT':setup['frozenJar'],'R31_HOST_RJAR':setup['frozenResourceJar'],'PYTHONUNBUFFERED':'1'})
cases=[]
for name in 'diagnosis writer retention timeline commit_failure progress neural qualification input_pin matrix research observatory snapshot'.split():cases.append((name,['python3',f'tools/r30-repair/run_{name}_host.py'],{}))
for name in 'memory pitguard_allocation runtime_memory_control resource_recovery'.split():cases.append(('r31-'+name,['python3',f'tools/r31/verify_{name}.py'],{}))
for name in 'task_runtime write_protocol service_protocol runtime_ddl research_schema'.split():cases.append(('r31-'+name,['python3',f'tools/r31/run_{name}_host.py'],{}))
for name in ['R31RuntimeHealthHost','R31ResourceRecoveryConsumerHost']:cases.append((name,['python3','tools/r31/run_task_runtime_host.py'],{'R31_RUNTIME_HOST_CLASS':name,'R31_RUNTIME_OUTPUT':f'verification/r31/ci-scoped-{name}'}))
assert len(cases)==24
base={'scope':'SEQUENTIAL_24_GROUPS_ACTUAL_PRODUCTION_BYTECODE_NATIVE_SQLITE_AND_LABELLED_HOST_ADAPTERS_NOT_ART_OR_NATURAL','sourceCommit':setup['sourceCommit'],'worktree':str(ROOT),'startedAt':datetime.now(timezone.utc).isoformat(),'dependencyEnvironment':{key:env[key] for key in ['SQLITE_JDBC','R31_HOST_CLASSES','R31_HOST_SUPPORT_CLASSES','R31_MEMORY_CLASSES','R31_MEMORY_CONTROL_SUPPORT','R31_HOST_RJAR','ANDROID_HOME','JAVA_HOME','GRADLE_USER_HOME']},'tests':[],'android':'NOT_RUN','physicalDevice':'NOT_RUN','continuous24h':'NOT_RUN','continuous72h':'NOT_RUN','naturalQuality':'NOT_PROVEN'}
def save():
 (WORK/'MATRIX.json').write_text(json.dumps(base,ensure_ascii=False,indent=2)+'\n')
save()
for index,(name,args,extra) in enumerate(cases,1):
 log=logs/f'{index:02d}-{name}.log'
 item={'index':index,'name':name,'command':args,'cwd':str(ROOT),'extraEnvironment':extra,'attempt':1,'sandbox':'require_escalated_for_actual_local_HTTP_SOCKET','startedAt':datetime.now(timezone.utc).isoformat(),'status':'RUNNING','log':str(log.relative_to(WORK))}
 base['tests'].append(item);save()
 print(f'HOST_START {index}/24 {name}',flush=True)
 start=time.monotonic();next_heartbeat=start+30
 with log.open('wb') as output:
  process=subprocess.Popen(args,cwd=ROOT,env=dict(env,**extra),stdout=output,stderr=subprocess.STDOUT,start_new_session=True)
  timed_out=False
  while process.poll() is None:
   now=time.monotonic()
   if now>=next_heartbeat:
    print(f'HOST_RUNNING {index}/24 {name} realElapsedSeconds={now-start:.1f}',flush=True);next_heartbeat=now+30
   if now-start>900:
    timed_out=True;os.killpg(process.pid,signal.SIGTERM)
    try:process.wait(timeout=10)
    except subprocess.TimeoutExpired:os.killpg(process.pid,signal.SIGKILL);process.wait()
    break
   time.sleep(1)
  status=process.returncode
 item.update({'status':'HOST_PASS' if status==0 and not timed_out else 'HOST_FAILED','exitCode':status,'timeout':timed_out,'elapsedRealSeconds':round(time.monotonic()-start,3),'finishedAt':datetime.now(timezone.utc).isoformat(),'logSha256':hashlib.sha256(log.read_bytes()).hexdigest(),'exactReproductionCommand':'source /workspace/starcore-environment/activate.sh\n'+ '\n'.join('export '+key+'='+shlex.quote(value) for key,value in {key:env[key] for key in ['SQLITE_JDBC','R31_HOST_CLASSES','R31_HOST_SUPPORT_CLASSES','R31_MEMORY_CLASSES','R31_MEMORY_CONTROL_SUPPORT','R31_HOST_RJAR']}.items())+'\ncd '+shlex.quote(str(ROOT))+'\n'+(' '.join(key+'='+shlex.quote(value) for key,value in extra.items())+' ' if extra else '')+shlex.join(args)})
 save();print(f'HOST_END {index}/24 {name} status={item["status"]} exit={status} realSeconds={item["elapsedRealSeconds"]}',flush=True)
 if status:
  tail=log.read_text(errors='replace').splitlines()[-12:]
  print('\n'.join(tail),flush=True)
base['finishedAt']=datetime.now(timezone.utc).isoformat();base['passed']=sum(t['status']=='HOST_PASS' for t in base['tests']);base['failed']=24-base['passed'];base['status']='HOST_PASS' if base['failed']==0 else 'HOST_MATRIX_HAS_REAL_FAILURES_ALL_24_ATTEMPTED'
save();print(f'HOST_MATRIX_DONE passed={base["passed"]} failed={base["failed"]} workspace={WORK}',flush=True)
