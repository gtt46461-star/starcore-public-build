#!/usr/bin/env python3
"""Prepare/apply a recoverable four-file version update using the original generator.

A new external journal preserves exact before/after hashes. Resume accepts only
those bytes, so an interrupted update can finish without discarding other edits.
No commit, signing, install, authorization switch or APK claim is performed.
"""
from pathlib import Path
import argparse
import contextlib
import datetime
import hashlib
import io
import importlib.util
import json
import os
import tempfile
import subprocess
from zoneinfo import ZoneInfo

FILES=('release/release_manifest.json','release/release_identity.properties',
       'app/src/main/java/com/ethk3/novaonepro/ReleaseManifestR926.java',
       'verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java')
STAGES=('P00',*(f'B{i:02d}' for i in range(1,9)))


def require(ok,message):
    if not ok:raise ValueError(message)


def sha(data):return hashlib.sha256(data).hexdigest()


def generator(repo):
    path=repo/'scripts/r926_release_manifest.py'
    require(path.is_file() and not path.is_symlink(),'ORIGINAL_MANIFEST_GENERATOR_REQUIRED')
    spec=importlib.util.spec_from_file_location('r335_original_release_generator',path)
    result=importlib.util.module_from_spec(spec);spec.loader.exec_module(result)
    return result


def regular(repo,relative):
    path=repo/relative
    require(path.is_file() and not path.is_symlink()
            and not any(p.is_symlink() for p in path.parents if p!=repo and p.is_relative_to(repo)),
            'REGULAR_CONTROLLED_IDENTITY_REQUIRED:'+relative)
    return path


def atomic(path,data):
    mode=path.stat().st_mode & 0o777 if path.exists() else 0o644
    handle,name=tempfile.mkstemp(dir=path.parent,prefix='.r335-version-')
    temp=Path(name)
    try:
        with os.fdopen(handle,'wb') as stream:
            stream.write(data);stream.flush();os.fsync(stream.fileno())
        os.chmod(temp,mode)
        os.replace(temp,path)
        if os.name!='nt':
            descriptor=os.open(path.parent,os.O_RDONLY)
            try:os.fsync(descriptor)
            finally:os.close(descriptor)
    finally:temp.unlink(missing_ok=True)


def prepare(repo,journal,installed_version,stage,day,series='R33.5'):
    repo=Path(repo).resolve();journal=Path(journal).absolute()
    require(type(installed_version) is int and installed_version>0,'POSITIVE_INSTALLED_VERSION_REQUIRED')
    require(series in ('R33.5','R34'),'KNOWN_RELEASE_SERIES_REQUIRED')
    stages = STAGES if series == 'R33.5' else tuple(f'B{i:02d}' for i in range(13))
    require(stage in stages,'KNOWN_STAGE_REQUIRED')
    datetime.date.fromisoformat(day)
    require(not journal.exists() and not journal.is_symlink() and not journal.resolve().is_relative_to(repo),
            'FRESH_EXTERNAL_VERSION_JOURNAL_REQUIRED')
    gen=generator(repo);current=gen.load()
    require(type(current['versionCode']) is int,'INTEGER_CURRENT_VERSION_REQUIRED')
    version=max(current['versionCode'],installed_version,577311033)+1
    require(version<=2100000000,'ANDROID_VERSION_CODE_LIMIT')
    new=json.loads(json.dumps(current));new['versionCode']=version
    new['versionName']=f'{series}-{stage}-{day}-{version}'
    manifest=(json.dumps(new,ensure_ascii=False,sort_keys=True,indent=2)+'\n').encode()
    digest=sha(manifest)
    after=(manifest,gen.prop_text(new,digest).encode(),gen.java_text(new,digest).encode(),
           gen.host_build_config_text(new,digest).encode())
    before={name:regular(repo,name).read_bytes() for name in FILES}
    journal.mkdir(parents=True,exist_ok=False)
    rows=[]
    for name,data in zip(FILES,after):
        leaf=journal/'after'/name;leaf.parent.mkdir(parents=True,exist_ok=True);leaf.write_bytes(data)
        old=journal/'before'/name;old.parent.mkdir(parents=True,exist_ok=True);old.write_bytes(before[name])
        rows.append({'path':name,'beforeSha256':sha(before[name]),'afterSha256':sha(data)})
    receipt={'schema':'STARCORE_R335_VERSION_JOURNAL_1','repo':str(repo),'stage':stage,
             'installedVersionObserved':installed_version,'previousVersionCode':current['versionCode'],
             'versionCode':version,'versionName':new['versionName'],'files':rows,
             'scope':'VERSION_IDENTITY_ONLY_NOT_BUILD_OR_ACCEPTANCE'}
    atomic(journal/'JOURNAL.json',(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n').encode())
    return receipt


@contextlib.contextmanager
def repo_version_lock(repo):
    # The lock is in the existing Git administration directory, never a
    # production input or untracked release file that would dirty the build.
    import fcntl
    result=subprocess.run(['git','-C',str(repo),'rev-parse','--git-path','r335-version.lock'],
                          capture_output=True,text=True,timeout=20)
    require(result.returncode==0,'VERSION_UPDATE_REQUIRES_ACTUAL_GIT_CHECKOUT')
    lock=Path(result.stdout.strip())
    if not lock.is_absolute():lock=repo/lock
    with lock.open('ab') as handle:
        try:fcntl.flock(handle.fileno(),fcntl.LOCK_EX|fcntl.LOCK_NB)
        except BlockingIOError:raise ValueError('VERSION_TRANSACTION_BUSY_RETRY_SAME_JOURNAL')
        try:yield
        finally:fcntl.flock(handle.fileno(),fcntl.LOCK_UN)


def apply(repo,journal):
    repo=Path(repo).resolve()
    with repo_version_lock(repo):
        return apply_locked(repo,journal)


def apply_locked(repo,journal):
    repo=Path(repo).resolve();journal=Path(journal).resolve()
    require(not journal.is_relative_to(repo),'EXTERNAL_JOURNAL_REQUIRED')
    receipt=json.loads((journal/'JOURNAL.json').read_text())
    require(receipt.get('schema')=='STARCORE_R335_VERSION_JOURNAL_1' and receipt.get('repo')==str(repo),
            'VERSION_JOURNAL_IDENTITY')
    rows=receipt.get('files',[])
    require(len(rows)==4 and tuple(r['path'] for r in rows)==FILES,'EXACT_FOUR_VERSION_FILES_REQUIRED')
    updates=[]
    # Inspect the entire transaction before changing any file.
    for row in rows:
        name=row['path'];target=regular(repo,name)
        data=(journal/'after'/name).read_bytes()
        before=(journal/'before'/name).read_bytes()
        require(sha(data)==row['afterSha256'] and sha(before)==row['beforeSha256'],'JOURNAL_BYTES_MISMATCH')
        actual=sha(target.read_bytes())
        require(actual in (row['beforeSha256'],row['afterSha256']),'IDENTITY_DIVERGED_RETAIN_USER_EDIT:'+name)
        updates.append((target,data))
    for target,data in updates:atomic(target,data)
    gen=generator(repo)
    with contextlib.redirect_stdout(io.StringIO()):gen.check()
    result={'schema':'STARCORE_R335_VERSION_APPLIED_1','journalSha256':sha((journal/'JOURNAL.json').read_bytes()),
            'versionCode':receipt['versionCode'],'versionName':receipt['versionName'],
            'stage':receipt['stage'],'status':'EXACT_FOUR_IDENTITIES_APPLIED',
            'scope':'VERSION_IDENTITY_ONLY_NOT_BUILD_OR_ACCEPTANCE'}
    atomic(journal/'APPLIED.json',(json.dumps(result,ensure_ascii=False,indent=2)+'\n').encode())
    return result


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo',type=Path,default=Path(__file__).resolve().parents[2])
    parser.add_argument('--journal',type=Path,required=True)
    parser.add_argument('--stage')
    parser.add_argument('--series',choices=['R33.5','R34'],default='R33.5')
    parser.add_argument('--installed-version',type=int)
    parser.add_argument('--date',default=datetime.datetime.now(ZoneInfo('Asia/Shanghai')).date().isoformat())
    parser.add_argument('--apply',action='store_true')
    parser.add_argument('--resume',action='store_true')
    args=parser.parse_args()
    if args.resume:
        require(args.apply,'RESUME_REQUIRES_EXPLICIT_APPLY')
        result=apply(args.repo,args.journal)
    else:
        require(args.stage is not None and args.installed_version is not None,'STAGE_AND_INSTALLED_VERSION_REQUIRED')
        result=prepare(args.repo,args.journal,args.installed_version,args.stage,args.date,args.series)
        if args.apply:result=apply(args.repo,args.journal)
    print(json.dumps(result,ensure_ascii=False,indent=2))


if __name__=='__main__':main()
