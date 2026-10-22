#!/usr/bin/env python3
"""Materialize the lossless compiled call index and reject stale source bindings.

--freeze --classes DIR refreshes the archive from freshly compiled product classes.
Normal builds stream the frozen index into generated assets; no source file is overwritten.
"""
import argparse, gzip, hashlib, json, os, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ARCHIVE = ROOT / 'release/r982_source_calls.tsv.gz'
PIN = ROOT / 'release/r982_source_calls.manifest.json'
OUTPUT = ROOT / 'app/build/generated/assets/r982/research/r975_source_calls.tsv'

def source_binding(path):
    wanted = {str(f.relative_to(ROOT)) for f in (ROOT/'app/src/main/java').rglob('*.java')}
    seen = set()
    with path.open(encoding='utf-8') as lines:
        for line in lines:
            if not line.startswith('FILE\t'): continue
            fields=line.rstrip('\n').split('\t'); name, expected=fields[1:3]
            if name not in wanted or name in seen: raise SystemExit('SOURCE_CALL_INDEX_FILE_SET_DRIFT:'+name)
            seen.add(name)
            if hashlib.sha256((ROOT/name).read_bytes()).hexdigest()!=expected: raise SystemExit('SOURCE_CALL_INDEX_STALE:'+name)
    if seen!=wanted: raise SystemExit('SOURCE_CALL_INDEX_MISSING_FILES:'+str(sorted(wanted-seen)))

def main():
    p=argparse.ArgumentParser();p.add_argument('--freeze',action='store_true');p.add_argument('--classes',type=Path);a=p.parse_args()
    OUTPUT.parent.mkdir(parents=True,exist_ok=True)
    if a.freeze:
        if not a.classes: p.error('--freeze requires --classes')
        subprocess.run([sys.executable,str(ROOT/'scripts/r975_source_call_index.py'),'--classes',str(a.classes)],check=True)
        original=ROOT/'app/src/main/assets/research/r975_source_calls.tsv'
        source_binding(original)
        with original.open('rb') as src, ARCHIVE.open('wb') as raw, gzip.GzipFile(filename='',mode='wb',fileobj=raw,mtime=0) as out:
            while block:=src.read(65536):out.write(block)
        digest=hashlib.sha256();count=0
        with original.open('rb') as src:
            while block:=src.read(65536):digest.update(block);count+=len(block)
        PIN.write_text(json.dumps({'sha256':digest.hexdigest(),'bytes':count,'scope':'STATIC_COMPILED_CALLS_NOT_RUNTIME_COVERAGE'},indent=2)+'\n')
        original.unlink()
    pin=json.loads(PIN.read_text());digest=hashlib.sha256();count=0
    handle,temporary=tempfile.mkstemp(dir=OUTPUT.parent,prefix='source-calls-',suffix='.partial')
    try:
        with os.fdopen(handle,'wb') as out, gzip.open(ARCHIVE,'rb') as src:
            while block:=src.read(65536):out.write(block);digest.update(block);count+=len(block)
            out.flush();os.fsync(out.fileno())
        if digest.hexdigest()!=pin['sha256'] or count!=pin['bytes']:raise SystemExit('SOURCE_CALL_INDEX_ARCHIVE_DRIFT')
        source_binding(Path(temporary));os.replace(temporary,OUTPUT)
    finally:
        if os.path.exists(temporary):os.unlink(temporary)
    print('SOURCE_CALL_ASSET_VERIFIED|bytes='+str(count)+'|sha256='+pin['sha256'])

if __name__=='__main__':main()
