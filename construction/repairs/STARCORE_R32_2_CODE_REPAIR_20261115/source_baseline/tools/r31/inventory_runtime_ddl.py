#!/usr/bin/env python3
"""Conservative production DDL inventory. Static literals do not prove runtime ownership."""
import hashlib
import json
from pathlib import Path
import re
import subprocess

ROOT=Path(__file__).resolve().parents[2]
DEST=ROOT/'verification/r31/p01/ddl-inventory.json'
LITERAL=re.compile(r'"(?:\\.|[^"\\])*"')
DDL=re.compile(r'^\s*(?:CREATE\s+(?:UNIQUE\s+)?(?:TABLE|INDEX|TRIGGER|VIEW)|ALTER\s+TABLE|DROP\s+(?:TABLE|INDEX|TRIGGER|VIEW))\b',re.I)
METHOD=re.compile(r'^\s*(?:(?:public|private|protected|static|final|synchronized)\s+)+[\w<>,?\[\]. ]+\s+(\w+)\s*\(',re.M)


if __name__=='__main__':
    rows=[];files=0
    for path in sorted((ROOT/'app/src/main/java').rglob('*.java')):
        files+=1;text=path.read_text();methods=list(METHOD.finditer(text));sha=hashlib.sha256(path.read_bytes()).hexdigest()
        for found in LITERAL.finditer(text):
            raw=found.group()[1:-1]
            if not DDL.match(raw):continue
            prior=[m for m in methods if m.start()<found.start()]
            symbol=prior[-1].group(1) if prior else 'STATIC_INITIALIZER_OR_UNRESOLVED'
            rows.append({'path':path.relative_to(ROOT).as_posix(),'line':text.count('\n',0,found.start())+1,
                         'approximate_enclosing_method':symbol,'literal_prefix':raw[:240],
                         'source_sha256':sha,'ownership':'REQUIRES_CALLGRAPH_AND_RUNTIME_TRACE'})
    result={'scope':'STATIC_DDL_LITERAL_INVENTORY_NOT_RUNTIME_DDL_ZERO_PROOF',
            'source_head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
            'java_files_scanned':files,'ddl_literals':len(rows),'files_with_ddl':len({r['path'] for r in rows}),
            'known_r31_repairs':['RuntimeCoordinatorR26.retentionStepR30 checks migration state without ensureProtected',
                                 'RuntimeR27.recordCacheRebuildsR30 no longer submits CREATE; SchemaR27 startup unit owns it',
                                 'ResearchWriteQueueR27.ImmutableBatch rejects DDL with SCHEMA_PENDING'],
            'runtime_trace':'NOT_RUN_ON_ANDROID','all_runtime_ddl_removed':'NOT_ESTABLISHED',
            'limitations':['Literal concatenations and dynamic SQL require call graph review.',
                           'Method association is a navigation heuristic, never an execution-owner exemption.',
                           'Schema/Registry filename does not authorize DDL in a running business slice.'],
            'objects':rows}
    DEST.parent.mkdir(parents=True,exist_ok=True);DEST.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({k:v for k,v in result.items() if k!='objects'},ensure_ascii=False,indent=2))
