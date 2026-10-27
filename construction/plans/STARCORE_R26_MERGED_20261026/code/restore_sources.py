"""Restore archived input A and verify B's 142 identical file contents.
Does not execute or extract archival code. Output is ignored by git.
"""
from pathlib import Path
import json,hashlib,zipfile,os
ROOT=Path(__file__).resolve().parents[1]
parts=ROOT/'archive/source_a_parts'
manifest=json.loads((parts/'manifest.json').read_text())
def sha(p):
    h=hashlib.sha256()
    with p.open('rb') as f:
        for data in iter(lambda:f.read(1024*1024),b''):h.update(data)
    return h.hexdigest()
def main():
    out=ROOT/'.restored';out.mkdir(exist_ok=True);dest=out/'SOURCE_A_FULL.zip';temp=out/'SOURCE_A_FULL.tmp'
    original=manifest.get('original',manifest)
    expected=original.get('sha256') or manifest.get('original_sha256')
    size=original.get('bytes') or original.get('size_bytes') or manifest.get('original_bytes')
    if not expected:raise ValueError('MANIFEST_SCHEMA')
    if dest.exists():
        if sha(dest)!=expected:raise ValueError('EXISTING_OUTPUT_MISMATCH')
    else:
        with temp.open('wb') as w:
            for part in manifest['parts']:
                name=part.get('name') or part.get('file') or part.get('filename')
                p=parts/name
                if sha(p)!=part['sha256']:raise ValueError('PART_HASH '+name)
                with p.open('rb') as f:
                    for data in iter(lambda:f.read(1024*1024),b''):w.write(data)
            w.flush();os.fsync(w.fileno())
        if sha(temp)!=expected or (size and temp.stat().st_size!=size):raise ValueError('ARCHIVE_HASH')
        temp.replace(dest)
    mapping=json.loads((ROOT/'archive/source_b_nested_zip_member_map.json').read_text())['members']
    with zipfile.ZipFile(dest) as z:
        bad=z.testzip()
        if bad:raise ValueError('CRC '+bad)
        for item in mapping:
            data=z.read(item['a_archive_path'])
            if len(data)!=item['bytes'] or hashlib.sha256(data).hexdigest()!=item['sha256']:raise ValueError('B_MEMBER '+item['b_path'])
    print(json.dumps({'archive':str(dest),'sha256':expected,'b_members_verified':len(mapping),'container_b_reconstruction':False}))
if __name__=='__main__':main()
