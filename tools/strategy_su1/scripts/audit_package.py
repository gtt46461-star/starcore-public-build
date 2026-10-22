#!/usr/bin/env python3
"""Check deliverable structure and traceability; not a production-code verifier."""
from pathlib import Path
from html.parser import HTMLParser
import csv
import hashlib
import json
import re
import sys

ROOT=Path(__file__).resolve().parents[1]
errors=[]
def require(ok,msg):
    if not ok: errors.append(msg)
def load(p): return json.loads((ROOT/p).read_text())

def main():
    docs=sorted((ROOT/'docs').glob('*.md'))
    require(len(docs)>=9,'expected at least nine complete chapters')
    require([p.name[:2] for p in docs]==['%02d'%i for i in range(len(docs))],'chapters must be numbered 00..N without gaps')
    for p in ROOT.rglob('*.json'):
        if '__pycache__' not in p.parts:
            try: json.loads(p.read_text())
            except Exception as e: errors.append(str(p.relative_to(ROOT))+': '+str(e))
    batches=load('manifests/batches.json')['batches']; ids={b['batch_id'] for b in batches}
    require(len(batches)==12 and len(ids)==12,'batch ids must be 12 unique entries (new work goes into U01-U12)')
    byid={b['batch_id']:b for b in batches}; visited=set(); active=set()
    def visit(k):
        if k in active: errors.append('batch cycle: '+k); return
        if k in visited: return
        if k not in byid: errors.append('unknown dependency: '+k); return
        active.add(k)
        for d in byid[k]['depends_on']: visit(d)
        active.remove(k); visited.add(k)
    for k in ids: visit(k)
    with (ROOT/'manifests/requirements.csv').open(encoding='utf-8-sig',newline='') as f: req=list(csv.DictReader(f))
    require(len(req)>=77 and len({r['requirement_id'] for r in req})==len(req),'requirements must have unique ids and keep the original 77')
    require(all(r['requirement_id']=='SU1-R%03d'%(i+1) for i,r in enumerate(req)),'requirement ids must be contiguous')
    for r in req:
        require((ROOT/r['design_chapter']).is_file(),r['requirement_id']+' chapter missing')
        b=byid.get(r['implementation_batch'])
        require(b is not None,r['requirement_id']+' batch missing')
        if b: require(any(x.startswith(r['acceptance_id']+'：') for x in b['acceptance']),r['requirement_id']+' acceptance mismatch')
    result=load('verification/reference_run.json')
    require(result['status']=='PASS' and all(c['returncode']==0 for c in result['checks']),'reference run not PASS')
    golden=load('reference/fixtures/relstate10_golden.json')
    digest=hashlib.sha256((ROOT/'reference/fixtures/relstate10_model.json').read_bytes()).hexdigest()
    require(digest==golden['modelSha256'],'golden model SHA mismatch')
    schema=load('manifests/runtime_changes.json')['schema']
    ddl=(ROOT/schema['path']).read_bytes()
    require(hashlib.sha256(ddl).hexdigest()==schema['sha256'],'runtime schema SHA mismatch')
    require(len(re.findall(rb'CREATE TABLE IF NOT EXISTS',ddl))==schema['table_count'],'runtime schema table count mismatch')
    page=(ROOT/'README.html').read_text(); HTMLParser().feed(page)
    require(len(re.findall(r'<article id="chapter-',page))==len(docs),'HTML chapter count')
    require('<script' not in page and 'href="#chapter-%d"'%(len(docs)-1) in page,'HTML should be self-contained with complete navigation')
    # R10: every SU1 public type named in the design must have an owning batch.
    owned={f.rsplit('/',1)[-1][:-5] for b in batches for f in b['affected_files'] if f.endswith('.java')}
    named=set()
    for p in docs: named|=set(re.findall(r'\b([A-Z][A-Za-z0-9]+SU1)\b',p.read_text()))
    retired={'PreparedTrainingStepSU1','PreparedStateUpdateSU1'}  # listed only in 09 §8 as replaced names
    unowned=sorted(named-owned-retired)
    require(not unowned,'SU1 types without batch owner: '+','.join(unowned))
    for alias in sorted(retired):
        require(not any(alias in p.read_text() and p.name[:2]!='09' for p in docs),'retired interface alias still used: '+alias)
    # R10: promotion gates present in the executable DDL.
    for token in (b'su1_alpha_root',b'SU1_CERTIFICATE_NULL_BASELINE_REQUIRED',b'SU1_NULL_CONTROL_ALARM',b'expires_at',b'bound_radius'):
        require(token in ddl,'R10 gate missing from DDL: '+token.decode())
    for b in batches:
        for f in b['affected_files']:
            require(f.count('/')>=1 and not f.startswith('/'),'bad affected path '+f)
    full=(ROOT/'FULL_DESIGN.md').read_text()
    require(all(p.read_text() in full for p in docs),'compiled Markdown not up to date')
    forbidden=['*.jks','*.keystore','*.p12','*.pem','*.apk','*.db']
    for pattern in forbidden: require(not list(ROOT.rglob(pattern)),'unexpected file type: '+pattern)
    require(not any(p.name.endswith('.zip') for p in ROOT.rglob('*')),'nested old package not expected')
    bycheck={c['name']:c for c in result['checks']}
    pycount=re.search(r'Ran (\d+) tests',bycheck['python_reference_suites']['stderr'])
    java_counts={name:int(re.search(r'checks=(\d+)',bycheck[name]['stdout'])[1]) for name in ['TypedProgramTest','PairedEvidenceTest','NullBaselineTest','ProgramTreeSU1Test']}
    report={'status':'FAIL' if errors else 'PASS','scope':'Deliverable structure, JSON, batch DAG, requirement mapping, fixture identity and recorded host checks only',
      'chapter_count':len(docs),'batch_count':len(batches),'requirement_count':len(req),
      'su1_types_named':len(named),'su1_types_unowned':unowned,
      'python_unit_tests':int(pycount[1]) if pycount else None,'java_assertions':java_counts,
      'errors':errors}
    (ROOT/'verification/package_audit.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(report,ensure_ascii=False,indent=2))
    return bool(errors)

if __name__=='__main__': sys.exit(main())
