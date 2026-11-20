#!/usr/bin/env python3
"""Apply the reviewed B07 after-image overlay to a staged source checkout.

All input states are checked before the first write. A retry accepts an exact
already-applied after-image; any other drift is refused. Git commits, identity
generation, signing and device evidence remain the enclosing stage's owners.
"""
from pathlib import Path
import argparse,hashlib,json,os,sys,tempfile

ROOT=Path(__file__).resolve().parent
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest() if path.is_file() else 'NEW'
def targets(source):
    manifest=json.loads((ROOT/'REPORT_OVERLAY_MANIFEST.json').read_text(encoding='utf-8'))
    if manifest['schema']!='R335_B07_REPORT_OVERLAY_1':raise ValueError('MANIFEST_SCHEMA')
    result=[]
    for row in manifest['files']:
        path=Path(row['path']);payload=ROOT/row['afterFile']
        if path.is_absolute() or '..' in path.parts:raise ValueError('SOURCE_PATH')
        target=source/path
        if target.is_symlink() or not target.resolve().is_relative_to(source):raise ValueError('SYMLINK_OR_ESCAPE:'+str(path))
        if not payload.is_file() or sha(payload)!=row['afterSha256']:raise ValueError('PAYLOAD_HASH:'+str(path))
        result.append((row,target,payload))
    if len({r['path'] for r,_,_ in result})!=len(result):raise ValueError('DUPLICATE_PATH')
    return result

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source',type=Path,required=True);p.add_argument('--check',action='store_true');a=p.parse_args()
    source=a.source.resolve();entries=targets(source)
    for row,target,payload in entries:
        actual=sha(target);allowed={row['afterSha256']} if a.check else {row['beforeSha256'],row['afterSha256']}
        if actual not in allowed:raise ValueError('SOURCE_DRIFT:'+row['path']+':'+actual)
    changed=[]
    if not a.check:
        for row,target,payload in entries:
            if sha(target)==row['afterSha256']:continue
            target.parent.mkdir(parents=True,exist_ok=True)
            fd,tmp=tempfile.mkstemp(prefix='.'+target.name+'.',dir=target.parent)
            try:
                with os.fdopen(fd,'wb') as stream:stream.write(payload.read_bytes());stream.flush();os.fsync(stream.fileno())
                os.chmod(tmp,0o644);os.replace(tmp,target);changed.append(row['path'])
            finally:
                if os.path.exists(tmp):os.unlink(tmp)
    for row,target,payload in entries:
        if sha(target)!=row['afterSha256']:raise ValueError('AFTER_VERIFY:'+row['path'])
    print(json.dumps({'status':'PASS','stage':'B07','verifiedFiles':len(entries),'changedFiles':changed,'scope':'EXACT_SOURCE_BYTES_ONLY; NO_RUNTIME_ACCEPTANCE'},ensure_ascii=False))

if __name__=='__main__':
    try:main()
    except (ValueError,KeyError,OSError) as e:print('REPORT_OVERLAY_REFUSED:'+str(e),file=sys.stderr);sys.exit(2)
