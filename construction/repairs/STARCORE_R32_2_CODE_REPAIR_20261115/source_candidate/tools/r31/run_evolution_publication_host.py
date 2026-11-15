#!/usr/bin/env python3
"""Exact owner protocol against native SQLite/JDBC. Engineering HOST evidence, never Android/quality."""
from pathlib import Path
from datetime import datetime, timezone
import argparse, hashlib, json, os, shutil, subprocess, sys, tempfile, time
from host_runtime_fixture import compile_owned_platform
from host_tool_support import dependencies
ROOT=Path(__file__).resolve().parents[2]
MAIN=ROOT/'app/src/main/java/com/ethk3/novaonepro/strategyv2'
HOST=ROOT/'tools/r31/evolution_publication_host'
BASELINE='de9a30834025c18ce291c241a722abe437717539'
NAMES=['StoreR27','EvolutionLoopR27','EvolutionPublicationSqlR31','EvolutionPublicationStoreR31']
CASES={'baseline':'EvolutionPublicationR31Host','protocol':'EvolutionPublicationProtocolR31Host','phases':'EvolutionPublicationPhasesR31Host','fault':'EvolutionPublicationFaultR31Host','capacity':'EvolutionPublicationCapacityR31Host'}
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--case',choices=['all',*CASES],default='all');parser.add_argument('--output');args=parser.parse_args()
 stamp=datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ');out=Path(args.output) if args.output else ROOT/'verification/r31/evolution-publication'/('host-'+stamp)
 out.mkdir(parents=True,exist_ok=False);work=Path(tempfile.mkdtemp(prefix='r31-evolution-final-'))
 classes,sdk,rjar,jdbc=dependencies(ROOT)
 sources=[*[MAIN/(name+'.java') for name in NAMES],*sorted(HOST.glob('*.java')),ROOT/'tools/r31/host/R31OwnedRuntimeFixture.java',Path(__file__).resolve()]
 before={str(p.relative_to(ROOT)):sha(p) for p in sources};commands=[]
 def run(command,name,timeout=300):
  command=list(map(str,command));log=out/name;started=time.monotonic()
  with log.open('wb') as target:
   process=subprocess.Popen(command,cwd=ROOT,stdout=target,stderr=subprocess.STDOUT)
   next_notice=time.monotonic()+30
   while process.poll() is None:
    now=time.monotonic()
    if now>=next_notice:print('HOST_RUNNING',name,'realSeconds='+str(round(now-started,1)),flush=True);next_notice=now+30
    if now-started>timeout:process.kill();process.wait();break
    time.sleep(.1)
  result={'command':command,'log':name,'exitCode':process.returncode,'realElapsedSeconds':round(time.monotonic()-started,3),'sha256':sha(log)};commands.append(result);(out/'commands.json').write_text(json.dumps(commands,indent=2)+'\n')
  if process.returncode:print(log.read_text(errors='replace')[-8000:]);raise RuntimeError(name)
  return log.read_text()
 try:
  platform=compile_owned_platform(ROOT,work/'owned-platform',classes,sdk);production=work/'production';test=work/'test';baseline=work/'baseline'
  for folder in [production,test,baseline]:folder.mkdir()
  run(['javac','-encoding','UTF-8','-sourcepath','','-cp',os.pathsep.join(map(str,[sdk,classes])),'-d',production,*[MAIN/(name+'.java') for name in NAMES]],'production-javac.log')
  cp=os.pathsep.join(map(str,[platform,production,test,classes,jdbc,sdk]))
  run(['javac','-encoding','UTF-8','-sourcepath','','-cp',cp,'-d',test,ROOT/'tools/r31/host/R31OwnedRuntimeFixture.java',*sorted(HOST.glob('*.java'))],'fixtures-javac.log')
  old=[]
  for name in ['StoreR27','EvolutionLoopR27']:
   file=work/'baseline-source'/(name+'.java');file.parent.mkdir(exist_ok=True);file.write_bytes(subprocess.check_output(['git','show',BASELINE+':'+str((MAIN/(name+'.java')).relative_to(ROOT))],cwd=ROOT));old.append(file)
  run(['javac','-encoding','UTF-8','-sourcepath','','-cp',os.pathsep.join(map(str,[sdk,classes])),'-d',baseline,*old],'original-source-javac.log')
  statuses={}
  for name,klass in CASES.items():
   if args.case not in ['all',name]:continue
   current=cp if name!='baseline' else os.pathsep.join(map(str,[platform,baseline,test,classes,jdbc,sdk]))
   text=run(['java','-Xmx512m','-cp',current,'com.ethk3.novaonepro.strategyv2.'+klass],name+'-native.log',600)
   statuses[name]='PASS';print('HOST_PASS',name,flush=True)
  after={str(p.relative_to(ROOT)):sha(p) for p in sources}
  if before!=after:raise RuntimeError('SOURCE_CHANGED_DURING_VERIFICATION')
  receipt={'status':'TARGETED_HOST_PASS','scope':'ACTUAL_OWNER_128_SOURCE_NATIVE_SQLITE_WITH_LABELLED_HOST_PLATFORM_ADAPTERS_NOT_ANDROID_OR_NATURAL','head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'dirtyAtRun':bool(subprocess.check_output(['git','status','--porcelain'],cwd=ROOT,text=True).strip()),'baselineSourceCommit':BASELINE,'capturedAt':datetime.now(timezone.utc).isoformat(),'cases':statuses,'sources':before,'sourceStable':True,'support':str(classes),'supportSha256':sha(classes) if classes.is_file() else None,'sqliteJdbcSha256':sha(jdbc),'work':str(work),'commands':commands,'android':'NOT_RUN','physicalDevice':'NOT_RUN','continuous24h':'NOT_RUN','continuous72h':'NOT_RUN','naturalQuality':'NOT_EVALUATED','performanceBoundary':'HOST samples/actual WriterHold/WAL only; no Android P99 certificate','globalResourceBoundary':'Local private encoded admission and default per-PID memory budget; all-app physical database/index/WAL/shared-model quota remains separately unverified'}
  (out/'RECEIPT.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n');print(json.dumps({'status':receipt['status'],'cases':statuses,'evidence':str(out)}))
 except BaseException as failure:
  (out/'FAILED.json').write_text(json.dumps({'status':'FAILED','error':repr(failure),'sources':before,'commands':commands,'work':str(work),'android':'NOT_RUN','naturalQuality':'NOT_PROVEN'},indent=2)+'\n');raise
if __name__=='__main__':main()
