#!/usr/bin/env python3
"""Future result gate. A valid plan and synthetic tests never grant production proof."""
import json,sys,hashlib
from pathlib import Path
root=Path(__file__).resolve().parents[1]
if len(sys.argv)!=2:raise SystemExit('Usage: python release_gate.py <result.json>')
p=Path(sys.argv[1]).resolve();r=json.loads(p.read_text());tasks=json.loads((root/'02_施工台账/tasks.json').read_text());errors=[]
ids=[x['task_id'] for x in r.get('results',[])];expected={t['id'] for t in tasks}
if len(ids)!=len(set(ids)) or set(ids)!=expected:errors.append('TASK_COVERAGE_OR_DUPLICATE')
for x in r.get('results',[]):
 if x.get('implementation')!='PASS' or x.get('tests')!='PASS':errors.append(x['task_id']+':NOT_COMPLETE')
 paths=x.get('evidence_paths',[])
 if not paths:errors.append(x['task_id']+':NO_EVIDENCE')
 for rel in paths:
  file=(p.parent/rel).resolve()
  if p.parent not in file.parents or not file.is_file():errors.append(x['task_id']+':MISSING_EVIDENCE');continue
  if hashlib.sha256(file.read_bytes()).hexdigest()!=x.get('evidence_sha256',{}).get(rel):errors.append(x['task_id']+':EVIDENCE_HASH')
for key in ['source_commit','apk_sha256']:
 if not r.get(key):errors.append('MISSING_'+key.upper())
d=r.get('device',{});prod=r.get('production',{});e=r.get('effect',{})
if d.get('status')!='PASS' or not d.get('evidence_paths') or d.get('normal_missing')!=0 or d.get('duplicate')!=0 or not isinstance(d.get('warm_p95_ms'),(float,int)) or d.get('warm_p95_ms',1e99)>5000 or not isinstance(d.get('soak_hours'),(float,int)) or d.get('soak_hours',0)<24:errors.append('DEVICE_GATE')
if prod.get('status')!='PROVEN' or not prod.get('evidence_paths') or not all(prod.get(k) is True for k in ['real_data','sealed_once','android_parity','future_windows_pass']):errors.append('PRODUCTION_GATE')
if e.get('status')!='PASS' or not isinstance(e.get('selected_hr'),(int,float)) or not 0.40<=e['selected_hr']<=1 or not isinstance(e.get('real_frequency'),(int,float)) or not 0.15<=e['real_frequency']<=1 or not isinstance(e.get('ece'),(int,float)) or not 0<=e['ece']<=.04:errors.append('EFFECT_GATE')
print(json.dumps({'release_gate':'FAIL' if errors else 'PASS_EVIDENCE_INDEX_ONLY','errors':errors,'note':'Even a valid evidence index requires review of raw device/data evidence; this tool cannot authenticate physical reality.'},ensure_ascii=False,indent=2))
raise SystemExit(1 if errors else 0)
