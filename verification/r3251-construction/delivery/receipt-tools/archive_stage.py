#!/usr/bin/env python3
import hashlib,json,shutil,subprocess,sys,stat,os
from pathlib import Path
from run_step import ROOT,PACKAGE,REPO,sha,now

b=sys.argv[1]
variant=os.environ.get('STARCORE_EVIDENCE_VARIANT','')
dest=REPO/'verification/r3251-construction'
if variant:dest=dest/variant
dest=dest/b
dest.mkdir(parents=True,exist_ok=True)
for p in sorted(ROOT.rglob('*')):
    rel=p.relative_to(ROOT)
    if not p.is_file() or not rel.parts[0].startswith(b):continue
    if p.suffix not in ('.log','.json','.xml','.md','.txt','.jsonl'):continue
    if any(x in rel.parts for x in ('classes','platform','production','tests','host-stubs')):continue
    q=dest/rel;q.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,q)
execution=json.loads((PACKAGE/'contracts/ACCEPTANCE_EXECUTION_MAP_222.json').read_text())
host_ids={entry[5:] for item in execution['items'] if item['runAt']==b for entry in item.get('entries',[]) if entry.startswith('HOST:')}
for host_id in sorted(host_ids):
    for base in (REPO/'app/build/r312-host'/host_id,REPO/'app/build/r312-host'/(host_id+'-receipt')):
        if not base.exists():continue
        inventory=[]
        for p in sorted(base.rglob('*')):
            if not p.is_file():continue
            rel=p.relative_to(base)
            inventory.append(dict(path=rel.as_posix(),bytes=p.stat().st_size,sha256=sha(p)))
            if p.suffix not in ('.log','.json','.xml','.md','.txt','.jsonl','.properties','.csv'):continue
            q=dest/'bound-host-artifacts'/base.name/rel;q.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,q)
        q=dest/'bound-host-artifacts'/base.name/'ARTIFACT_INVENTORY.json';q.parent.mkdir(parents=True,exist_ok=True)
        q.write_text(json.dumps(inventory,indent=2)+'\n')
manifest=json.loads((PACKAGE/'code_manifest.json').read_text())
rows=[]
for f in manifest['batches'][b]['files']:
    p=REPO/f['path'];digest=sha(p);mode=stat.S_IMODE(p.stat().st_mode)
    assert digest==f['after_sha256'] and mode==int(f['mode'],8),f['path']
    rows.append(dict(path=f['path'],action=f['action'],sha256=digest,mode=oct(mode),owner=f['owner'],cards=f['cards']))
(dest/'applied-files.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2)+'\n')
patch=subprocess.check_output(['git','diff','--binary'],cwd=REPO)
review=ROOT/(b+'-review.patch')
if not review.exists():review.write_bytes(patch)
patch=review.read_bytes()
receipt=dict(batch=b,at=now(),baseline='7424eeae6354eb6599dd8498e123c059bd410c90',planCommit='0e932cc14fa0d72dd67546488915fa038f201501',packageSha256='ceb5e3516452524de2519781bff04ba9f844bb95f95052bd4c2778928b856966',manifestSha256=sha(PACKAGE/'code_manifest.json'),files=len(rows),changed=sum(x['action']!='PRESERVE_REFERENCE' for x in rows),sourceBytes='PASS_EXACT_DECLARED_BYTES_AND_MODES',host='PASS_DECLARED_CURRENT_STAGE_COMMANDS',android='SEE_CURRENT_GRADLE_COMMAND_RECEIPTS',device='BLOCKED_NO_CONNECTED_DEVICE',original032Database='NOT_PROVIDED_IN_ORIGINAL_REPORTS',missing556Checkpoints='NOT_RECOVERED_NO_ORIGINAL_BYTES',naturalQuality='NOT_PROVEN',full222BusinessAcceptance=False,reviewPatchSha256=hashlib.sha256(patch).hexdigest())
if (PACKAGE/'owner_revision/REVIEW.json').exists():
    receipt['parentPackageSha256']=receipt.pop('packageSha256')
    receipt.update(packageKind='EXPLICIT_REVIEWED_OWNER_CORRECTION_CHILD',ownerReviewSha256=sha(PACKAGE/'owner_revision/REVIEW.json'),revision=manifest.get('revision'))
    receipt['ownerReviewChain']=[dict(path=p.relative_to(PACKAGE).as_posix(),sha256=sha(p)) for p in sorted((PACKAGE/'owner_revision').rglob('*REVIEW*.json')) if p.is_file()]
(dest/'STAGE.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
state=REPO/'.starcore-plan-receipts'
if state.exists():
    for p in state.glob(b+'.json'):
        q=dest/'source-receipts'/p.name;q.parent.mkdir(exist_ok=True);shutil.copy2(p,q)
print(json.dumps({'batch':b,'evidence':str(dest),'files':len(rows),'changed':receipt['changed']}))
