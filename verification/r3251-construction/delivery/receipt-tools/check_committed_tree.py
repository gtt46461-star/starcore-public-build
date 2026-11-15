#!/usr/bin/env python3
import argparse,hashlib,json,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--package',type=Path,required=True);p.add_argument('--repo',type=Path,required=True);p.add_argument('--through',required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args();manifest=json.loads((a.package/'code_manifest.json').read_text());rows=[dict(x,batch=b) for b,v in manifest['batches'].items() if int(b[1:])<=int(a.through[1:]) for x in v['files']]
payload_count=len(rows)
rows.extend(dict(x,batch=a.through) for x in manifest['batches'][a.through]['derived_files'])
snapshot=json.loads((a.package/manifest['batches'][a.through]['stage_identity']['snapshot']).read_text())
rows.extend(dict(path=x['path'],owner='P1',batch=a.through,mode='644',after_sha256=x['sha256']) for x in snapshot['generated_outputs'])
proc=subprocess.run(['git','cat-file','--batch'],cwd=a.repo,input=b''.join(('HEAD:'+x['path']+'\n').encode() for x in rows),stdout=subprocess.PIPE,check=True);data=proc.stdout;offset=0;results=[]
mode_data=subprocess.check_output(['git','ls-tree','-z','-r','HEAD'],cwd=a.repo);modes={}
for record in mode_data.split(b'\0'):
 if not record:continue
 meta,path=record.split(b'\t',1);modes[path.decode()]=meta.split()[0].decode()
for x in rows:
 end=data.index(b'\n',offset);header=data[offset:end];offset=end+1
 if header.endswith(b' missing'):actual=None
 else:
  oid,kind,size=header.split();assert kind==b'blob';size=int(size);raw=data[offset:offset+size];offset+=size+1;actual=hashlib.sha256(raw).hexdigest()
 mode=modes.get(x['path']);expected_mode='100755' if int(x['mode'],8)&0o111 else '100644';results.append(dict(path=x['path'],owner=x['owner'],batch=x['batch'],expectedSha256=x['after_sha256'],committedSha256=actual,committedMode=mode,status='PASS' if actual==x['after_sha256'] and mode==expected_mode else 'MISSING_OR_DIFFERENT'))
assert offset==len(data)
value=dict(batch=a.through,originalPayloadPaths=payload_count,stageIdentityPaths=len(rows)-payload_count,sourceCommit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=a.repo,text=True).strip(),sourceTree=subprocess.check_output(['git','rev-parse','HEAD^{tree}'],cwd=a.repo,text=True).strip(),manifestSha256=hashlib.sha256((a.package/'code_manifest.json').read_bytes()).hexdigest(),files=results,passed=all(x['status']=='PASS' for x in results),scope='COMMITTED_PAYLOAD_BLOBS_AND_EXECUTABLE_MODES_NOT_BUSINESS_ACCEPTANCE')
a.out.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n');print(json.dumps(dict(batch=a.through,paths=len(rows),passed=value['passed'],missing=[x['path'] for x in results if x['status']!='PASS']),ensure_ascii=False));raise SystemExit(0 if value['passed'] else 1)
