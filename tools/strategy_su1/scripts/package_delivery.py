#!/usr/bin/env python3
"""Package the audited design, excluding transient binaries and caches."""
from pathlib import Path
import hashlib
import json
import zipfile

ROOT=Path(__file__).resolve().parents[1]

def deliverable(p):
    return p.is_file() and not any(x in p.parts for x in ('__pycache__','java_classes','all_java_classes')) and p.suffix not in ('.pyc','.class','.db','.zip')

def main():
    audit=json.loads((ROOT/'verification/package_audit.json').read_text())
    if audit['status']!='PASS': raise RuntimeError('Package audit has not passed')
    files=sorted(p for p in ROOT.rglob('*') if deliverable(p) and p.name!='PACKAGE_SHA256SUMS.txt')
    lines=[hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.relative_to(ROOT).as_posix() for p in files]
    sums=ROOT/'PACKAGE_SHA256SUMS.txt'; sums.write_text('\n'.join(lines)+'\n'); files.append(sums)
    target=ROOT.parent/(ROOT.name+'.zip')
    with zipfile.ZipFile(target,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for p in files: z.write(p,arcname=ROOT.name+'/'+p.relative_to(ROOT).as_posix())
    with zipfile.ZipFile(target) as z:
        if z.testzip() is not None: raise RuntimeError('ZIP CRC failure')
        for p in files:
            name=ROOT.name+'/'+p.relative_to(ROOT).as_posix()
            if z.read(name)!=p.read_bytes(): raise RuntimeError('ZIP content mismatch: '+name)
    print(json.dumps({'path':str(target),'files':len(files),'bytes':target.stat().st_size,
        'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'zip_crc':'PASS'},ensure_ascii=False,indent=2))

if __name__=='__main__': main()
