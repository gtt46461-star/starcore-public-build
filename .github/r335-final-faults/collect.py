"""Package only allow-listed results for a controlled, disposable Android guest."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess

scratch=Path(os.environ['R335_FAULT_SCRATCH'])
out=Path(os.environ['R335_PROBE_EVIDENCE'])
def sha(p):
    with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def read(p):return json.loads(p.read_bytes())

doc={'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'apkSha256':os.environ['R335_INSTALLED_SHA'],'api':int(os.environ['R335_DEVICE_API']),'scope':'Disposable guest and synthetic controlled test data','suites':{}}
for name in ['preparation','runtime','research','gallery','upgrade']:
    console=scratch/(name+'-console.txt')
    doc['suites'][name]={'exit':int((scratch/(name+'.exit')).read_text()),'consoleSha256':sha(console),'receipts':[]}
for group in ['runtime','research','gallery','upgrade']:
    p=scratch/group
    for f in p.rglob('*'):
        if not f.is_file():continue
        relative=f.relative_to(p)
        allowed_files={'cells.tsv','results.tsv','matrix.json','research-fault-matrix.json','EMU-UPGRADE-RECEIPT.json'}
        if f.name in allowed_files:
            target=out/'original-controlled-results'/group/relative
            target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(f.read_bytes())
            doc['suites'][group]['receipts'].append({'path':str(target.relative_to(out)),'sha256':sha(target)})
        elif group=='gallery' and f.suffix.lower()=='.png' and re.fullmatch(r'(READY|EMPTY|INSUFFICIENT|READ_ERROR)-[A-Za-z0-9_-]+\.png',f.name):
            target=out/'controlled-chart-images'/relative
            target.parent.mkdir(exist_ok=True,parents=True);target.write_bytes(f.read_bytes())
            doc['suites'][group].setdefault('screenshots',[]).append({'path':str(target.relative_to(out)),'sha256':sha(target)})
        elif (f.suffix=='.txt' and ('-result' in f.name or '-recover' in f.name or '-run' in f.name or f.name in ['decisions.txt','authority.txt','workbench.txt','schema-stamp-reason.txt'])):
            contents=f.read_text(errors='replace')
            ok=re.search(r'OK \((\d+) tests?\)',contents)
            slugs=sorted(set(re.findall(r'\b(?:R30_[A-Z0-9_]+|R31_[A-Z0-9_]+|java[.][\w.]+Exception|android[.][\w.]+Exception)\b',contents)))
            doc['suites'][group].setdefault('controlledTestResults',[]).append({'path':str(relative),'sha256':sha(f),'okTests':int(ok.group(1)) if ok else None,'resultCodes':slugs})
(out/'fault-suite-receipt.json').write_text(json.dumps(doc,ensure_ascii=False,indent=2)+'\n')
