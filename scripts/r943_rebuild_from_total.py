#!/usr/bin/env python3
"""Rebuild the delivered source with its archived toolchain and the owner's signing kit."""
from pathlib import Path, PurePosixPath
import argparse, hashlib, json, os, shutil, subprocess, zipfile

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--package-root',type=Path,required=True)
    parser.add_argument('--signing-kit',type=Path,required=True)
    parser.add_argument('--out',type=Path,required=True)
    args=parser.parse_args(); package=args.package_root.resolve();source=package/'02_COMPLETE_SOURCE'
    args.out=args.out.resolve();args.out.mkdir(parents=True,exist_ok=True)
    parts=source/'delivery/r926-batch1/input-archive-parts'
    manifest=json.loads((parts/'parts_manifest.json').read_text())
    archive=args.out/'original-input.zip';whole=hashlib.sha256();total=0
    with archive.open('wb') as output:
        for item in manifest['parts']:
            path=parts/item['filename']
            if path.parent!=parts or path.stat().st_size!=item['bytes']:
                raise ValueError('INVALID_ARCHIVE_PART')
            data=path.read_bytes()
            if hashlib.sha256(data).hexdigest()!=item['sha256']:raise ValueError('PART_HASH')
            output.write(data);whole.update(data);total+=len(data)
    if whole.hexdigest()!=manifest['sha256'] or total!=manifest['bytes']:raise ValueError('ARCHIVE_HASH')
    toolchain=args.out/'toolchain';toolchain.mkdir(exist_ok=True)
    with zipfile.ZipFile(archive) as z:
        for entry in z.infolist():
            parts_of_name=PurePosixPath(entry.filename).parts
            if '08_TOOLCHAIN' not in parts_of_name or entry.is_dir():continue
            relative=PurePosixPath(*parts_of_name[parts_of_name.index('08_TOOLCHAIN')+1:])
            if relative.is_absolute() or '..' in relative.parts:raise ValueError('UNSAFE_TOOLCHAIN_PATH')
            target=toolchain.joinpath(*relative.parts);target.parent.mkdir(parents=True,exist_ok=True)
            with z.open(entry) as input_stream,target.open('wb') as output:shutil.copyfileobj(input_stream,output)
            if target.name in ('aapt','aapt2','apksigner','zipalign','java','javac') or target.suffix=='.sh':target.chmod(0o755)
    base=source/'delivery/r941/Starcore-Tianshu-V57.21.1.0.10-R5.4.1.0.10-941-release.apk'
    environment=dict(os.environ,STARCORE_BASE_JAVA_ROOT=str(package/'06_BUILD/predecessor_java'))
    subprocess.run(['python3',str(source/'scripts/r926_build_from_signing_kit.py'),
                    '--base-apk',str(base),'--signing-kit',str(args.signing_kit.resolve()),
                    '--toolchain',str(toolchain),'--out',str(args.out/'apk')],cwd=source,env=environment,check=True)

if __name__=='__main__':main()
