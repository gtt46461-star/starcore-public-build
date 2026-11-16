#!/usr/bin/env python3
"""Archive current own-stage evidence without importing a predecessor PASS."""
import collections, datetime, hashlib, json, shutil, subprocess, sys
import xml.etree.ElementTree as ET
from pathlib import Path

BASE = Path('/workspace/starcore-r3251-evidence/complete-chain-final')
PKG = Path('/workspace/starcore-r3251-owner5-revision')
STAGES = {
 'P2': ('/workspace/starcore-r3251-P2-attachment-owner', 'ab23d83301026937ab2225bc64fdebfc96761b68'),
 'P3': ('/workspace/starcore-r3251-P3-complete-chain', '20b51dd9401bb11e0f5fc1473f57c1bccd9da518'),
 'P4': ('/workspace/starcore-r3251-P4-complete-chain', '8894692374ad971cd80120f7aeee2e46c16afa86'),
 'P5': ('/workspace/starcore-r3251-attachment-final', 'bb7b49bbcc9266bad548ec37e71403256fc06a4d'),
}

def sha(p): return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def write(p, d): p.parent.mkdir(parents=True, exist_ok=True); p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
def cp(p,q): q.parent.mkdir(parents=True,exist_ok=True); shutil.copy2(p,q)

def main(b):
 repo,commit=STAGES[b];repo=Path(repo);root=BASE/b
 dest=repo/'verification/r3251-construction/complete-research-chain'/b
 out=dest/'current-validation';out.mkdir(exist_ok=True)
 head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=repo,text=True).strip()
 assert head==commit,(b,head)
 # Original HOST runners intentionally refresh verification/r31 receipts. They
 # are generated evidence, and must not be confused with changed build inputs.
 dirty=subprocess.check_output(['git','status','--porcelain','-z'],cwd=repo).split(b'\0')
 generated=[]
 for row in dirty:
  if not row:continue
  path=row[3:].decode();assert path.startswith('verification/r31/'),(b,path)
  generated.append(path)
  source=repo/path
  if source.is_dir():
   for f in source.rglob('*'):
    if f.is_file():cp(f,out/'generated-host-evidence/after'/f.relative_to(repo))
  else:
   cp(source,out/'generated-host-evidence/after'/path)
   old=subprocess.run(['git','show',commit+':'+path],cwd=repo,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL)
   if old.returncode==0:
    q=out/'generated-host-evidence/before'/path;q.parent.mkdir(parents=True,exist_ok=True);q.write_bytes(old.stdout)
 gradle=root/(b+'-current-complete-chain-gradle.command.json');g=json.loads(gradle.read_text())
 required={':app:compileDebugJavaWithJavac',':app:compileDebugAndroidTestJavaWithJavac',':app:testDebugUnitTest',':app:lintDebug'}
 assert g['exitCode']==0 and g['sourceCommit']==commit and required.issubset(g['argv'])
 assert sha(root/g['log'])==g['logSha256']
 counts=collections.Counter();tests={}
 for f in sorted((repo/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml')):
  cp(f,out/'junit'/f.name)
  for c in ET.parse(f).getroot().iter('testcase'):
   result='FAIL' if c.find('failure') is not None else 'ERROR' if c.find('error') is not None else 'SKIP' if c.find('skipped') is not None else 'PASS'
   counts[result]+=1;tests[c.get('classname')+'#'+c.get('name')]={'status':result,'xml':'junit/'+f.name,'sha256':sha(f)}
 assert counts=={'PASS':659},dict(counts)
 lint=repo/'app/build/reports/lint-results-debug.xml';issues=list(ET.parse(lint).getroot().iter('issue'))
 assert not [i for i in issues if i.get('severity') in ('Error','Fatal')]
 for f in (repo/'app/build/reports').glob('lint-results-debug.*'):cp(f,out/'lint'/f.name)
 # Preserve every failed attempt and its original digest, not just the successful retry.
 for f in root.rglob('*'):
  if f.is_file() and f.suffix in ('.log','.json','.xml','.txt','.md','.jsonl') and 'classes' not in f.relative_to(root).parts:
   cp(f,out/'commands'/f.relative_to(root))
 mapping=json.loads((PKG/'contracts/ACCEPTANCE_EXECUTION_MAP_222.json').read_text())
 host_ids=sorted({e[5:] for x in mapping['items'] if x['runAt']==b for e in x.get('entries',[]) if e.startswith('HOST:')})
 hosts={}
 for host in host_ids:
  candidates=[]
  for f in root.glob(b+'-bound-'+host+'*.command.json'):
   d=json.loads(f.read_text())
   if d.get('sourceCommit')==commit:
    assert d.get('status')!='RUNNING',f
    assert sha(root/d['log'])==d['logSha256'],f
    candidates.append(dict(receipt=f.name,**d))
  good=[d for d in candidates if d.get('exitCode')==0]
  assert good,(b,host)
  hosts[host]={'status':'PASS','selected':sorted(good,key=lambda d:d['finishedAt'])[-1]['receipt'],'attempts':candidates}
  for folder in [repo/'app/build/r312-host'/host,repo/'app/build/r312-host'/(host+'-receipt')]:
   if not folder.exists():continue
   inv=[]
   for f in sorted(folder.rglob('*')):
    if not f.is_file():continue
    inv.append({'path':f.relative_to(folder).as_posix(),'bytes':f.stat().st_size,'sha256':sha(f)})
    if f.suffix in ('.json','.log','.xml','.txt','.md','.csv','.properties'):
     cp(f,out/'bound-host-artifacts'/folder.name/f.relative_to(folder))
   write(out/'bound-host-artifacts'/folder.name/'ARTIFACT_INVENTORY.json',inv)
 write(out/'JUNIT_METHOD_RESULTS.json',tests)
 status={'stage':b,'at':datetime.datetime.now(datetime.timezone.utc).isoformat(),'state':'PASS_CURRENT_OWN_STAGE_DECLARED_ENGINEERING_GATES',
  'sourceCommit':commit,'sourceTree':g['sourceTree'],'gradle':g,'junit':dict(counts),'lintErrors':0,'lintWarnings':len(issues),
  'hostGeneratedEvidencePaths':generated,'buildInputsChangedByArchiving':False,
  'boundHostCommands':hosts,'boundHostCount':len(hosts),'ownerProductionCorrection':'P2','dependentOwnerCorrectionCodeReviewed':True,
  'original312PayloadContractRetained':True,'reviewedPayloadAudit':'ORIGINAL_PAYLOAD_DIFFERENCE_AUDIT.json',
  'additionalProductionCorrectionPathOutsideOriginal312':'app/src/main/java/com/ethk3/novaonepro/ResearchSchemaCoordinatorR926.java',
  'full222BusinessAcceptance':False,'physical24h':'NOT_RUN','physical72h':'NOT_RUN','naturalQuality':'NOT_PROVEN',
  'apkBuildSource':'bb7b49bbcc9266bad548ec37e71403256fc06a4d','privateSigningMaterialArchived':False}
 write(out/'STAGE_ENGINEERING_ACCEPTANCE.json',status)
 original=json.loads((dest/'STATUS.json').read_text());write(dest/'PREVALIDATION_STATUS_RETAINED.json',original)
 write(dest/'STATUS.json',status)
 for f in (dest/'reviewed-identity/files/app/build').rglob('*'):
  if f.is_file():
   assert f.name in ('semantic_manifest.json','ResearchSemanticsGeneratedR967.java')
   subprocess.run(['git','add','-f','--',str(f.relative_to(repo))],cwd=repo,check=True)
 subprocess.run(['git','add','--',str(dest.relative_to(repo))],cwd=repo,check=True)
 if generated:subprocess.run(['git','add','--',*generated],cwd=repo,check=True)
 diff=subprocess.check_output(['git','diff','--cached','--name-only'],cwd=repo,text=True).splitlines()
 assert diff and all(p.startswith(str(dest.relative_to(repo))+'/') or p.startswith('verification/r31/') for p in diff)
 subprocess.run(['git','commit','-m',f'verify({b}): accept own-stage current Gradle, 659 tests, lint and bound host gates; retain failed attempts [skip ci]'],cwd=repo,check=True)
 print(json.dumps({'stage':b,'sourceCommit':commit,'evidenceCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=repo,text=True).strip(),'tests':659,'hosts':len(hosts)}))

if __name__=='__main__':main(sys.argv[1])
