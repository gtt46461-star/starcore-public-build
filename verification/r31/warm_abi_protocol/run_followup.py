#!/usr/bin/env python3
"""Only affected final-source capacity and retention edges, after saved full regressions.
The original receipts remain authoritative for their prior source hashes.
"""
from pathlib import Path
import datetime,hashlib,json,os,shutil,subprocess,tempfile,sys
ROOT=Path(__file__).resolve().parents[3]
HERE=Path(__file__).resolve().parent
PRIOR=Path(sys.argv[2]).resolve() if len(sys.argv)==3 and sys.argv[1]=='--prior' else HERE/'runs/20261105T015733Z/receipt.json'
r=json.loads(PRIOR.read_text())
old=next(c['argv'] for c in r['commands'] if c['name']=='harness-compile')
oldcp=old[old.index('-cp')+1];oldtest=old[old.index('-d')+1]
OUT=HERE/'runs'/datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ');OUT.mkdir()
TMP=Path(tempfile.mkdtemp(prefix='.r31-warm-followup-',dir='/workspace'))
sha=lambda b:hashlib.sha256(b).hexdigest()
sources=[ROOT/'app/src/main/java/com/ethk3/novaonepro/strategyv2'/(n+'.java') for n in ['ModelPublicationR31','WarmCandidateR26','WarmModelSeedR26','WarmPublicationR31']]
receipt={'schema':'R31_WARM_FINAL_AFFECTED_FOLLOWUP','priorProductionReceipt':str(PRIOR.relative_to(ROOT)),'sourceSha256':{str(p.relative_to(ROOT)):sha(p.read_bytes()) for p in sources},'android':'NOT_RUN','commands':[]}
def run(name,argv):
    result=subprocess.run(list(map(str,argv)),cwd=TMP,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=180)
    (OUT/(name+'.log')).write_text(result.stdout)
    frames=[json.loads(s[6:]) for s in result.stdout.splitlines() if s.startswith('FRAME ')]
    (OUT/(name+'.frames.json')).write_text(json.dumps(frames,indent=2)+'\n')
    receipt['commands'].append({'name':name,'argv':list(map(str,argv)),'exit':result.returncode,'logSha256':sha(result.stdout.encode()),'frames':len(frames)})
    (OUT/'receipt.json').write_text(json.dumps(receipt,indent=2)+'\n')
    print(name,result.returncode,result.stdout[-1400:],flush=True)
    if result.returncode:raise RuntimeError(name)
compiler=['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-g']
platform=TMP/'statfs-platform';platform.mkdir()
run('actual-linux-statfs-transport-compile',[*compiler,'-d',platform,HERE/'adapters/android/os/StatFs.java'])
classes=TMP/'classes';classes.mkdir();snapshots=[]
for source in sources:
    target=OUT/'source_snapshot'/source.relative_to(ROOT);target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,target);snapshots.append(target)
sdk='/workspace/starcore-environment/android-sdk/platforms/android-36/android.jar'
run('final-four-source-android-api-compile',[*compiler,'-cp',sdk+os.pathsep+oldcp,'-sourcepath',TMP/'empty','-d',classes,*snapshots])
test=TMP/'test';test.mkdir();cp=os.pathsep.join(map(str,[platform,classes,oldtest,oldcp]))
run('followup-harness-compile',[*compiler,'-cp',cp,'-d',test,HERE/'WarmRetentionEdgesHostR31.java',HERE/'WarmCapacityHostR31.java'])
cp=str(test)+os.pathsep+cp
run('actual-runtime-retention-edges',['java','-Xmx512m','-cp',cp,'com.ethk3.novaonepro.strategyv2.WarmRetentionEdgesHostR31',TMP/'edges.db'])
run('statfs-capacity-and-warm-publish',['java','-Xmx512m','-cp',cp,'com.ethk3.novaonepro.strategyv2.WarmCapacityHostR31',TMP/'capacity.db'])
receipt['status']='NAMED_AFFECTED_FINAL_SOURCE_HOST_PASS_NOT_ANDROID'
receipt['changedDuringRun']=[str(p.relative_to(ROOT)) for p in sources if sha(p.read_bytes())!=receipt['sourceSha256'][str(p.relative_to(ROOT))]]
assert not receipt['changedDuringRun']
(OUT/'receipt.json').write_text(json.dumps(receipt,indent=2)+'\n')
print('EVIDENCE',OUT)
