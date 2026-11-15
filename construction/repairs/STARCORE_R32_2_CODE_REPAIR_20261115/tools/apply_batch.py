#!/usr/bin/env python3
"""Apply a reviewed source batch using exact pre/post hashes. No arbitrary merges."""
import argparse, hashlib, json, os, pathlib, tempfile, shutil, datetime, sys

def digest(data): return hashlib.sha256(data).hexdigest()
def safe(root, name):
    rel=pathlib.PurePosixPath(name)
    if rel.is_absolute() or '..' in rel.parts or not rel.parts: raise ValueError('UNSAFE_PATH '+name)
    target=root.joinpath(*rel.parts)
    if not target.resolve().is_relative_to(root.resolve()): raise ValueError('SYMLINK_ESCAPE '+name)
    return target

def execute(package, repo, batch, apply=False):
    manifest=json.loads((package/'code_manifest.json').read_text())
    if batch not in manifest['batches']: raise ValueError('UNKNOWN_BATCH')
    report={'batch':batch,'mode':'APPLY' if apply else 'CHECK','files':[],
            'scope':'SOURCE_BYTES_ONLY_NOT_BUSINESS_ACCEPTANCE','plan':manifest['revision'],
            'manifest_sha256':digest((package/'code_manifest.json').read_bytes())}
    row=manifest['batches'][batch]
    # Earlier producers must have the prescribed bytes, including preserved dependencies.
    for previous in row['prerequisites']:
        for entry in manifest['batches'][previous]['files']:
            path=safe(repo,entry['path']); current=digest(path.read_bytes()) if path.exists() else None
            if current!=entry['after_sha256']: raise ValueError('PREREQUISITE_DRIFT '+previous+' '+entry['path'])
    pending=[]
    for entry in row['files']:
        target=safe(repo,entry['path']); payload=safe(package,entry['source'])
        data=payload.read_bytes()
        if digest(data)!=entry['after_sha256']: raise ValueError('PACKAGE_CORRUPT '+entry['path'])
        if target.exists() and not target.is_file(): raise ValueError('NOT_FILE '+entry['path'])
        current=digest(target.read_bytes()) if target.exists() else None
        if current==entry['after_sha256']: status='PRESERVED' if current==entry['before_sha256'] else 'ALREADY_APPLIED'
        elif current==entry['before_sha256']: status='READY';pending.append((target,data,entry))
        else: raise ValueError('SOURCE_DRIFT '+entry['path']+' expected='+str(entry['before_sha256'])+' actual='+str(current))
        report['files'].append({'path':entry['path'],'status':status,'code_action':entry['action']})
    # Validate ALL files before touching any; roll back this invocation on exceptions.
    if apply and pending:
        transaction=pathlib.Path(tempfile.mkdtemp(prefix='starcore-batch-'))
        backups=[]
        try:
            for index,(target,data,entry) in enumerate(pending):
                backup=transaction/str(index)
                if target.exists():shutil.copy2(target,backup)
                backups.append((target,backup if backup.exists() else None))
                target.parent.mkdir(parents=True,exist_ok=True)
                fd,temporary=tempfile.mkstemp(dir=target.parent,prefix='.starcore-write-')
                try:
                    with os.fdopen(fd,'wb') as output:output.write(data);output.flush();os.fsync(output.fileno())
                    if backup.exists():shutil.copymode(backup,temporary)
                    else:os.chmod(temporary,int(entry.get('mode','100644')[-3:],8))
                    os.replace(temporary,target)
                finally:
                    if os.path.exists(temporary):os.unlink(temporary)
            for target,data,entry in pending:
                if digest(target.read_bytes())!=entry['after_sha256']:raise ValueError('WRITE_VERIFY '+entry['path'])
        except BaseException:
            for target,backup in reversed(backups):
                if backup is None:target.unlink(missing_ok=True)
                else:shutil.copy2(backup,target)
            raise
        finally:shutil.rmtree(transaction)
    report['changed_files']=len(pending) if apply else 0
    report['pending_files']=len(pending)
    report['at']=datetime.datetime.now(datetime.timezone.utc).isoformat()
    if apply:
        receipts=repo/'.starcore-plan-receipts';receipts.mkdir(exist_ok=True)
        (receipts/(batch+'.json')).write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    return report

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--package',type=pathlib.Path,default=pathlib.Path(__file__).resolve().parents[1])
    parser.add_argument('--repo',type=pathlib.Path,required=True)
    parser.add_argument('--batch',required=True,choices=['P1','P2','P3','P4','P5'])
    parser.add_argument('--apply',action='store_true')
    args=parser.parse_args()
    try:print(json.dumps(execute(args.package.resolve(),args.repo.resolve(),args.batch,args.apply),ensure_ascii=False,indent=2))
    except Exception as error:print(json.dumps({'status':'REFUSED','reason':str(error)},ensure_ascii=False),file=sys.stderr);sys.exit(2)
