#!/usr/bin/env python3
"""S00 channel/inventory checks (R312-S00-C01/C02). Exit 0 only when every check passes."""
import json,subprocess,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];G=[sys.executable,str(ROOT/'construction/STARCORE_R31_FINAL_20261104/tools/r312_gate.py')]
def git(*a):return subprocess.check_output(['git',*a],cwd=ROOT,text=True).strip()
fail=[]
suite=json.loads((ROOT/'construction/r31/r312/HOST_SUITE.json').read_text())
empty=[h['id'] for h in suite['host'] if not h.get('retired') and not h.get('command')]
bad_retired=[h['id'] for h in suite['host'] if h.get('retired') and not h.get('replacement')]
if empty or bad_retired:fail.append('C01#A1 suite empty=%s retired_without_replacement=%s'%(empty,bad_retired))
for cmd in ('check-plan','status'):
    if subprocess.run(G+[cmd],cwd=ROOT,capture_output=True).returncode:fail.append('C01#A1 gate '+cmd+' failed')
wf=ROOT/'.github/workflows/r312-gate.yml';ref=ROOT/'construction/STARCORE_R31_FINAL_20261104/reference_code/r312/r312-gate.yml'
if not wf.is_file() or wf.read_bytes()!=ref.read_bytes():fail.append('C01#A3 r312-gate.yml missing or differs from reference')
inv=ROOT/'construction/r31/r312/WIP_INVENTORY.json'
if not inv.is_file():fail.append('C02 WIP_INVENTORY.json missing')
else:
    w=json.loads(inv.read_text());ledger=json.loads((ROOT/'construction/r31/EXECUTION_LEDGER.json').read_text())
    started={c['id'] for c in ledger['cards'] if c['status']!='NOT_STARTED'};listed={c['card'] for c in w.get('cards',[])}
    if started-listed:fail.append('C02#A1 cards missing: %s'%sorted(started-listed))
    missing=[f for c in w.get('cards',[]) for f in c.get('files',[]) if not (ROOT/f).exists()]
    if missing:fail.append('C02#A1 paths absent at HEAD: %s'%missing[:10])
    if subprocess.run(['git','merge-base','--is-ancestor',w['resume_commit'],'HEAD'],cwd=ROOT).returncode:fail.append('C02#A2 resume commit not ancestor of HEAD')
    if w.get('other_branches_requiring_merge'):fail.append('C02#A2 other branches still require merge: %s'%w['other_branches_requiring_merge'])
    prev=ROOT/'verification/r31/r312/S00/BUILD_SUMMARY.json'
    if not prev.is_file():fail.append('C02#A3 no S00 BUILD_SUMMARY to compare (first gate run records failures; rerun after WIP_INVENTORY)')
    else:
        s=json.loads(prev.read_text())
        want=sorted([k for k,v in s.get('tests',{}).items() if v['status'] in ('FAIL','ERROR')]+['HOST:'+h for h in s.get('host_failed',[]) if h!='verify_s00']+['GRADLE:'+t for t in s.get('gradle',{}).get('failed_tasks',[])])
        have=sorted(w.get('failures',[]))
        if want!=have:fail.append('C02#A3 WIP failures differ from S00 BUILD_SUMMARY: missing=%s extra=%s'%(sorted(set(want)-set(have))[:10],sorted(set(have)-set(want))[:10]))
print(json.dumps({'state':'FAIL' if fail else 'PASS','problems':fail},ensure_ascii=False,indent=1))
sys.exit(1 if fail else 0)
