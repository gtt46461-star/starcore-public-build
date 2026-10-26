#!/usr/bin/env python3
"""Restore and verify the exact original R25 ZIP using only Python's standard library."""
from pathlib import Path
import hashlib,json,os,tempfile,zipfile

ROOT=Path(__file__).resolve().parent
PARTS=ROOT/'original_parts'

def digest(path):
    value=hashlib.sha256()
    with path.open('rb') as source:
        for block in iter(lambda:source.read(1024*1024),b''):value.update(block)
    return value.hexdigest()

def main():
    manifest=json.loads((PARTS/'manifest.json').read_text(encoding='utf-8'))
    output_name=manifest['output_name']
    if Path(output_name).name!=output_name:raise ValueError('Unsafe output name')
    directory=ROOT/'restored';directory.mkdir(exist_ok=True)
    target=directory/output_name
    for row in manifest['parts']:
        if Path(row['name']).name!=row['name']:raise ValueError('Unsafe part name')
        p=PARTS/row['name']
        if p.stat().st_size!=row['bytes'] or digest(p)!=row['sha256']:
            raise ValueError('Part verification failed: '+row['name'])
    if target.exists():
        if target.stat().st_size!=manifest['bytes'] or digest(target)!=manifest['sha256']:
            raise FileExistsError('Existing output differs; move it before retrying: '+str(target))
    else:
        handle,temp=tempfile.mkstemp(prefix='.restore-',suffix='.partial',dir=directory)
        temporary=Path(temp)
        try:
            with os.fdopen(handle,'wb') as sink:
                for row in manifest['parts']:
                    with (PARTS/row['name']).open('rb') as source:
                        for block in iter(lambda:source.read(1024*1024),b''):sink.write(block)
                sink.flush();os.fsync(sink.fileno())
            if temporary.stat().st_size!=manifest['bytes'] or digest(temporary)!=manifest['sha256']:
                raise ValueError('Reconstructed ZIP hash mismatch')
            with zipfile.ZipFile(temporary) as archive:
                if archive.testzip() is not None:raise ValueError('ZIP CRC mismatch')
            temporary.replace(target)
        finally:
            if temporary.exists():temporary.unlink()
    with zipfile.ZipFile(target) as archive:
        if archive.testzip() is not None:raise ValueError('ZIP CRC mismatch')
    print(json.dumps({'status':'PASS','path':str(target),'bytes':target.stat().st_size,'sha256':digest(target),'parts':len(manifest['parts'])},ensure_ascii=False,indent=2))

if __name__=='__main__':main()
