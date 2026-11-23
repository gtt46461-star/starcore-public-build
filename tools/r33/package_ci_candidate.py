#!/usr/bin/env python3
"""Package a signed, source-bound candidate. Device and natural outcomes stay unproved."""
from pathlib import Path, PurePosixPath
import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import tempfile
import zipfile
import verify_plan_binding as binding

CERT='d8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7'
BASE_APK='8f0cb6e6eadbce5787cffde310a6cbf204d43962e636fd9ebedf6e66556fb763'
PACKAGE='com.starcore.research.r13'
require=binding.require
sha=binding.sha


def source_tree_entries(repo, commit):
    entries = {}
    for row in binding.git(repo, 'ls-tree', '-r', '-z', '--full-tree', commit).split(b'\0'):
        if not row:
            continue
        metadata, raw_name = row.split(b'\t', 1)
        mode, kind, oid = metadata.decode('ascii').split()
        name = raw_name.decode('utf-8')
        path = PurePosixPath(name)
        require(name and not path.is_absolute() and path.as_posix() == name
                and '\\' not in name and not any(x in ('.', '..', '.git') for x in path.parts),
                'UNSAFE_SOURCE_ZIP_MEMBER')
        require(name not in entries and kind == 'blob' and mode in ('100644', '100755', '120000')
                and re.fullmatch('[0-9a-f]{40}', oid), 'UNSUPPORTED_SOURCE_ZIP_ENTRY')
        entries[name] = (mode, oid)
    require(entries, 'EMPTY_SOURCE_ARCHIVE')
    return entries


def source_zip_mode(mode):
    return int(mode, 8) | (0o777 if mode == '120000' else 0)


def verify_source_archive(path, entries):
    """Exact Git leaf set, Unix file type/mode, unfiltered original blob bytes."""
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        require(len(names) == len(set(names)) and set(names) == set(entries),
                'SOURCE_ZIP_MEMBER_SET_MISMATCH')
        for name, (mode, oid) in entries.items():
            info = archive.getinfo(name)
            require(info.create_system == 3 and info.external_attr >> 16 == source_zip_mode(mode),
                    'SOURCE_ZIP_MODE_MISMATCH:' + name)
            digest = hashlib.sha1(b'blob ' + str(info.file_size).encode('ascii') + b'\0')
            total = 0
            with archive.open(info) as stream:
                for block in iter(lambda: stream.read(1 << 20), b''):
                    total += len(block)
                    digest.update(block)
            require(total == info.file_size and digest.hexdigest() == oid,
                    'SOURCE_ZIP_BLOB_MISMATCH:' + name)
    return len(entries)


def write_source_archive(repo, commit, target):
    """Write raw Git blobs, unaffected by export-ignore/export-subst or replace refs.

    Checkout verification also validates committed symlink bytes and constrained
    repository-internal tracked targets. No extraction or link traversal is used
    to read archive contents. One cat-file process serves all blobs.
    """
    repo, target = Path(repo).resolve(), Path(target)
    require(not target.exists() and not target.is_symlink(), 'FRESH_SOURCE_ARCHIVE_REQUIRED')
    binding.verify_checkout_bytes(repo, commit)
    entries = source_tree_entries(repo, commit)
    # Same isolation policy as binding.git; the streaming process cannot inherit
    # a different worktree/object store, use replace refs, or lazy-fetch objects.
    env = dict(os.environ, GIT_NO_REPLACE_OBJECTS='1', GIT_OPTIONAL_LOCKS='0',
               GIT_TERMINAL_PROMPT='0', GIT_NO_LAZY_FETCH='1')
    for key in ('GIT_DIR', 'GIT_WORK_TREE', 'GIT_COMMON_DIR', 'GIT_INDEX_FILE',
                'GIT_OBJECT_DIRECTORY', 'GIT_ALTERNATE_OBJECT_DIRECTORIES', 'GIT_PREFIX'):
        env.pop(key, None)
    handle, temp_name = tempfile.mkstemp(dir=target.parent, prefix='.r334-source-', suffix='.part')
    os.close(handle)
    temporary = Path(temp_name)
    process = None
    try:
        process = subprocess.Popen(['git', '-c', 'core.hooksPath=/dev/null', '-C', str(repo),
                                    'cat-file', '--batch'], stdin=subprocess.PIPE,
                                   stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, env=env)
        with zipfile.ZipFile(temporary, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
            for name, (mode, oid) in sorted(entries.items()):
                process.stdin.write(oid.encode('ascii') + b'\n')
                process.stdin.flush()
                header = process.stdout.readline(256).strip().split()
                require(len(header) == 3 and header[0] == oid.encode('ascii') and header[1] == b'blob'
                        and header[2].isdigit(), 'SOURCE_BLOB_BATCH_HEADER')
                size = int(header[2])
                info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
                info.create_system = 3
                info.external_attr = source_zip_mode(mode) << 16
                info.compress_type = zipfile.ZIP_DEFLATED
                info.file_size = size
                digest = hashlib.sha1(b'blob ' + str(size).encode('ascii') + b'\0')
                remaining = size
                with archive.open(info, 'w', force_zip64=size >= zipfile.ZIP64_LIMIT) as output:
                    while remaining:
                        data = process.stdout.read(min(1 << 20, remaining))
                        require(data, 'SOURCE_BLOB_TRUNCATED')
                        remaining -= len(data)
                        digest.update(data)
                        output.write(data)
                require(process.stdout.read(1) == b'\n' and digest.hexdigest() == oid,
                        'SOURCE_BLOB_IDENTITY_MISMATCH')
        process.stdin.close()
        require(process.wait(timeout=30) == 0, 'SOURCE_BLOB_BATCH_FAILED')
        verify_source_archive(temporary, entries)
        with temporary.open('rb') as stream:
            os.fsync(stream.fileno())
        os.link(temporary, target)  # Atomic no-replace publication.
        return {'format': 'R334_EXACT_GIT_BLOB_SOURCE_ZIP_1', 'members': len(entries),
                'gitTree': binding.git(repo, 'rev-parse', commit + '^{tree}').decode().strip()}
    finally:
        if process is not None:
            if process.poll() is None:
                process.kill()
                process.wait()
            if process.stdin is not None and not process.stdin.closed:
                process.stdin.close()
            if process.stdout is not None:
                process.stdout.close()
        temporary.unlink(missing_ok=True)


def write_source_bundle(repo, commit, target):
    """Publish a standalone Git history bundle, including only ancestors of HEAD.

    A tracked-files ZIP cannot satisfy the existing R31/R33 commit ancestry
    gates. Verify the bundle in an EMPTY repository so missing prerequisites
    cannot be concealed by objects already installed in the builder checkout.
    """
    repo, target = Path(repo).resolve(), Path(target)
    require(not target.exists() and not target.is_symlink(), 'FRESH_SOURCE_BUNDLE_REQUIRED')
    require(binding.git(repo, 'rev-parse', 'HEAD').decode().strip() == commit,
            'SOURCE_BUNDLE_HEAD_MISMATCH')
    require(binding.git(repo, 'rev-parse', '--is-shallow-repository').decode().strip() == 'false',
            'SOURCE_BUNDLE_SHALLOW_HISTORY')
    handle, name = tempfile.mkstemp(dir=target.parent, prefix='.r335-bundle-', suffix='.part')
    os.close(handle)
    temporary = Path(name)
    temporary.unlink()
    try:
        binding.git(repo, 'bundle', 'create', str(temporary), 'HEAD')
        with tempfile.TemporaryDirectory(prefix='r335-verify-bundle-') as root:
            empty = Path(root)
            binding.git(empty, 'init', '--quiet')
            binding.git(empty, 'bundle', 'verify', str(temporary.resolve()))
            binding.git(empty, 'fetch', '--quiet', '--no-tags', str(temporary.resolve()), 'HEAD')
            got = binding.git(empty, 'rev-parse', 'FETCH_HEAD').decode().strip()
            tree = binding.git(empty, 'rev-parse', 'FETCH_HEAD^{tree}').decode().strip()
            require(got == commit and tree == binding.git(repo, 'rev-parse', commit + '^{tree}').decode().strip(),
                    'SOURCE_BUNDLE_RESTORED_IDENTITY_MISMATCH')
            binding.git(empty, 'merge-base', '--is-ancestor', binding.BASE, got)
        with temporary.open('rb') as stream: os.fsync(stream.fileno())
        os.link(temporary, target)
        return {'format': 'R335_STANDALONE_GIT_BUNDLE_1', 'sourceCommit': commit,
                'sourceTree': tree, 'emptyRepositoryVerified': True,
                'historyScope': 'HEAD_REACHABLE_ANCESTORS_ONLY'}
    finally:
        temporary.unlink(missing_ok=True)


def command(args):
    result=subprocess.run([str(x) for x in args],capture_output=True,text=True,timeout=180)
    require(result.returncode==0,'PUBLIC_BUILD_TOOL_FAILED:'+Path(args[0]).name)
    return result.stdout+'\n'+result.stderr


def inspect(apk,aapt,apksigner):
    apk=Path(apk);require(apk.is_file() and not apk.is_symlink(),'ACTUAL_APK_REQUIRED')
    signature=command([apksigner,'verify','--verbose','--print-certs',apk])
    certs={v.lower().replace(':','') for v in re.findall(
        r'^(?:V[0-9]+(?:\.[0-9]+)? )?Signer(?: #[0-9]+)?(?: \([^\r\n]*\))?:? certificate SHA-256 digest:\s*([a-fA-F0-9:]+)\s*$',signature,re.M)}
    require(re.search(r'^Verifies\s*$',signature,re.M) and certs=={CERT},'ORIGINAL_SIGNER_REQUIRED')
    badging=command([aapt,'dump','badging',apk]);m=re.search(
        r"^package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']*)'",badging,re.M)
    require(m is not None,'ACTUAL_APK_BADGING_REQUIRED')
    with zipfile.ZipFile(apk) as z:
        names=z.namelist();require(len(names)==len(set(names)) and z.testzip() is None,'APK_DUPLICATE_MEMBER_OR_CRC')
        require('classes.dex' in names and 'AndroidManifest.xml' in names,'APK_NOT_EXECUTABLE')
        require(not any(re.search(r'\.(p12|jks|keystore|pfx|key)$',n,re.I) for n in names),'PRIVATE_SIGNING_FILE_IN_APK')
    return dict(package=m[1],versionCode=int(m[2]),versionName=m[3],certificateSha256=CERT,
                signatureVerified=True,sha256=sha(apk),bytes=apk.stat().st_size),signature,badging


def validate_pair(manifest,old,new):
    require(type(manifest.get('versionCode')) is int,'RELEASE_VERSION_INTEGER_REQUIRED')
    require(manifest.get('applicationId')==old['package']==new['package']==PACKAGE,'RELEASE_PACKAGE_MISMATCH')
    require(old['sha256']==BASE_APK and old['versionCode']==577311033,'EXACT_033_BASELINE_REQUIRED')
    require(new['versionCode']==manifest['versionCode']>=577311034 and old['versionCode']<new['versionCode'],'RELEASE_VERSION_NOT_HIGHER')
    require(new['versionName']==manifest.get('versionName') and new['versionName']!=old['versionName'],'RELEASE_NAME_MISMATCH')
    require(new['certificateSha256']==old['certificateSha256']==CERT,'SIGNER_CHANGED')


def embedded(apk,identity):
    with zipfile.ZipFile(apk) as z:
        old=json.loads(z.read('assets/diagnostics/build-identity-r31.json'))
        current=json.loads(z.read('assets/diagnostics/build-identity-r334.json'))
        if 'r34' in identity:
            require(json.loads(z.read('assets/diagnostics/build-identity-r34.json')) == identity['r34'],
                    'R34_EMBEDDED_OWNER_OR_PLAN_MISMATCH')
    require(old.get('source_commit')==identity['source_commit'] and old.get('source_tree')==identity['source_tree']
            and old.get('source_dirty') is False,'EMBEDDED_R31_SOURCE_IDENTITY_MISMATCH')
    require(current==identity,'EMBEDDED_R334_PLAN_OR_SOURCE_MISMATCH')


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo',type=Path,default=Path(__file__).resolve().parents[2])
    parser.add_argument('--out',type=Path,required=True)
    parser.add_argument('--plan',type=Path,default=os.environ.get('STARCORE_R33_PLAN_ROOT'))
    parser.add_argument('--baseline-apk',type=Path,required=True)
    parser.add_argument('--apk',type=Path)
    parser.add_argument('--aapt',type=Path,required=True);parser.add_argument('--apksigner',type=Path,required=True)
    parser.add_argument('--build-log',type=Path,required=True)
    parser.add_argument('--build-receipt',type=Path,required=True)
    args=parser.parse_args();repo=args.repo.resolve();out=args.out.resolve()
    require(args.plan is not None,'STARCORE_R33_PLAN_ROOT_REQUIRED')
    require(not out.is_relative_to(repo),'OUTPUT_MUST_BE_OUTSIDE_SOURCE_CHECKOUT')
    require(not out.exists(),'FRESH_OUTPUT_DIRECTORY_REQUIRED')
    require(args.build_log.is_file() and args.build_log.stat().st_size>0,'ORIGINAL_GRADLE_LOG_REQUIRED')
    identity=binding.identity(repo,args.plan,repo/'release/r33_plan_binding.json',True)
    manifest=binding.read(repo/'release/release_manifest.json')
    choices=sorted((repo/'app/build/outputs/apk/release').glob('*.apk')) if args.apk is None else [args.apk]
    require(len(choices)==1,'EXACTLY_ONE_RELEASE_APK_REQUIRED');apk=choices[0]
    new,signature,badging=inspect(apk,args.aapt,args.apksigner)
    old,old_signature,old_badging=inspect(args.baseline_apk,args.aapt,args.apksigner)
    validate_pair(manifest,old,new);embedded(apk,identity)
    build=binding.read(args.build_receipt)
    require(build.get('schema')=='STARCORE_R33_4_RELEASE_BUILD_1' and build.get('gradleRelease')=='PASS'
            and type(build.get('exitCode')) is int and build['exitCode']==0
            and build.get('sourceClean') is True and build.get('sourceUnchangedDuringBuild') is True
            and build.get('sourceCommit')==identity['source_commit'] and build.get('sourceTree')==identity['source_tree']
            and build.get('apkSha256')==new['sha256'] and build.get('planManifestSha256')==identity['plan_manifest_sha256']
            and build.get('gradleLogSha256')==sha(args.build_log)
            and ':app:assembleRelease' in build.get('command',[]),'ACTUAL_BUILD_RECEIPT_MISMATCH')
    out.mkdir(parents=True,exist_ok=False)
    series = 'R34' if 'r34' in identity else 'R33'
    stem=f"STARCORE-{series}-{new['versionCode']}-{identity['source_commit'][:12]}"
    copied=out/(stem+'-release.apk');shutil.copy2(apk,copied)
    require(sha(copied)==new['sha256'],'COPIED_APK_MISMATCH')
    (out/'public-certificate.txt').write_text(signature);(out/'badging.txt').write_text(badging)
    (out/'baseline033-certificate.txt').write_text(old_signature);(out/'baseline033-badging.txt').write_text(old_badging)
    shutil.copy2(args.build_log,out/'gradle-build.log')
    shutil.copy2(args.build_receipt,out/'CI_BUILD_RECEIPT.json')
    binding.write_json(out/'build-identity-r334.json',identity)
    source=out/(stem+'-source.zip')
    source_verified=write_source_archive(repo,identity['source_commit'],source)
    require(source_verified['gitTree']==identity['source_tree'],'SOURCE_ARCHIVE_TREE_MISMATCH')
    git_bundle=out/(stem+'-source.bundle')
    bundle_verified=write_source_bundle(repo,identity['source_commit'],git_bundle)
    require(sha(out/'gradle-build.log')==build['gradleLogSha256']
            and binding.read(out/'CI_BUILD_RECEIPT.json')==build,'COPIED_BUILD_ORIGINALS_MISMATCH')
    with zipfile.ZipFile(source) as z:
        require(z.testzip() is None,'SOURCE_ARCHIVE_CRC')
        require(not any(re.search(r'\.(p12|jks|keystore|pfx|key)$',n,re.I) for n in z.namelist()),'PRIVATE_SIGNING_FILE_IN_SOURCE')
        for n in z.namelist():
            if not n.endswith('/') and z.getinfo(n).file_size<2*1024*1024:
                require(not re.search(rb'-----BEGIN (?:RSA |EC |OPENSSH |DSA |ENCRYPTED )?PRIVATE KEY-----',z.read(n)),
                        'PRIVATE_KEY_MATERIAL_IN_SOURCE')
    # No archive hash is embedded inside that archive: the outer checksum is computed last.
    receipt={'schema':'STARCORE_R33_4_CANDIDATE_DELIVERY_1','sourceCommit':identity['source_commit'],
             'sourceTree':identity['source_tree'],'apk':dict(new,file=copied.name),'baseline033':old,
             'planManifestSha256':identity['plan_manifest_sha256'],'sourceArchive':dict(source_verified,file=source.name,sha256=sha(source)),
             'gitBundle':dict(bundle_verified,file=git_bundle.name,sha256=sha(git_bundle)),
             'gradleLogSha256':sha(out/'gradle-build.log'),'samePackageSameSignerHigherVersion':True,
             'engineeringAcceptance':'SEPARATE_32_GROUP_LEDGER_REQUIRED','physical24h':'NOT_RUN_BY_PACKAGER',
             'physical72h':'NOT_RUN_BY_PACKAGER','naturalQuality':'NOT_PROVEN_BY_PACKAGING',
             'realSwitchChangedByPackager':False,'ciRun':os.environ.get('GITHUB_RUN_ID')}
    binding.write_json(out/'DELIVERY.json',receipt)
    members=sorted(p for p in out.iterdir() if p.is_file())
    inner={p.name:sha(p) for p in members}
    binding.write_json(out/'ZIP_MEMBERS_SHA256.json',inner);members.append(out/'ZIP_MEMBERS_SHA256.json')
    bundle=out/(stem+'-APK-SOURCE-EVIDENCE.zip')
    with zipfile.ZipFile(bundle,'x',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for p in members:z.write(p,p.name)
    with zipfile.ZipFile(bundle) as z:
        require(z.testzip() is None,'DELIVERY_ZIP_CRC')
        for n,digest in inner.items():require(__import__('hashlib').sha256(z.read(n)).hexdigest()==digest,'DELIVERY_MEMBER_HASH')
    (out/'SHA256SUMS.txt').write_text(''.join(sha(p)+'  '+p.name+'\n' for p in (copied,source,git_bundle,bundle,out/'DELIVERY.json')))
    print(json.dumps({'apk':str(copied),'zip':str(bundle),'apkSha256':sha(copied),'zipSha256':sha(bundle),
                      'scope':'SIGNED_CANDIDATE_DEVICE_AND_NATURAL_ACCEPTANCE_SEPARATE'},ensure_ascii=False))


if __name__=='__main__':main()
