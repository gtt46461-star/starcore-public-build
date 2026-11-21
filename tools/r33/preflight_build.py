#!/usr/bin/env python3
"""Read-only build prerequisites. No dependency download, secret read, signing or Git mutation."""
from pathlib import Path
import argparse
import json
import os
import platform
import re
import shutil
import subprocess
import sys


def collect(repo, sdk):
    repo=Path(repo).resolve()
    checks=[]
    def record(name,okay,observed):
        checks.append({'id':name,'status':'PASS' if okay else 'BLOCKED','observed':observed})
    for name in ('settings.gradle','build.gradle','app/build.gradle','gradlew',
                 'gradle/wrapper/gradle-wrapper.jar','gradle/wrapper/gradle-wrapper.properties',
                 'app/src/main/AndroidManifest.xml','release/release_manifest.json',
                 'release/release_identity.properties','tools/r31/verify_baseline.py',
                 'scripts/r967_semantic_identity.py','scripts/r815_generate_provenance.py',
                 'scripts/r982_source_call_asset.py','scripts/r975_source_call_index.py',
                 'gradle/r335_source_calls.gradle'):
        record('SOURCE:'+name,(repo/name).is_file(),'present' if (repo/name).is_file() else 'missing')
    java_home=os.environ.get('JAVA_HOME')
    command=str(Path(java_home)/'bin/java') if java_home else shutil.which('java')
    if command and not Path(command).is_file():command=None
    version='MISSING'
    if command:
        result=subprocess.run([command,'-version'],capture_output=True,text=True,timeout=20)
        match=re.search(r'(?:openjdk|java) version "([0-9]+)',result.stderr+'\n'+result.stdout)
        version=match[1] if result.returncode==0 and match else 'UNKNOWN'
    record('JDK17',version=='17',version)
    compiler=False
    if command:
        modules=subprocess.run([command,'--list-modules'],capture_output=True,text=True,timeout=20)
        compiler=modules.returncode==0 and any(line.split('@',1)[0]=='jdk.compiler' for line in modules.stdout.splitlines())
    record('JDK_COMPILER_MODULE',compiler,'present' if compiler else 'missing')
    git=shutil.which('git')
    if git:
        result=subprocess.run([git,'-C',str(repo),'rev-parse','--is-inside-work-tree'],capture_output=True,text=True,timeout=20)
        history=result.returncode==0 and result.stdout.strip()=='true'
        if history:
            top=subprocess.run([git,'-C',str(repo),'rev-parse','--show-toplevel'],capture_output=True,text=True,timeout=20)
            history=top.returncode==0 and Path(top.stdout.strip()).resolve()==repo
        if history:
            fixed=subprocess.run([git,'-C',str(repo),'merge-base','--is-ancestor',
                                  'bb7b49bbcc9266bad548ec37e71403256fc06a4d','HEAD'],capture_output=True,timeout=20)
            history=fixed.returncode==0
        record('EXACT_GIT_HISTORY',history,'REQUIRED_033_ANCESTOR_PRESENT' if history else 'ARCHIVE_OR_REQUIRED_HISTORY_MISSING')
    else:record('EXACT_GIT_HISTORY',False,'GIT_MISSING')
    record('PYTHON_3_12',sys.version_info>=(3,12),'.'.join(map(str,sys.version_info[:3])))
    record('POSIX_BUILD_HOST',os.name=='posix',platform.system())
    tag='darwin-x86_64' if platform.system()=='Darwin' else 'linux-x86_64'
    for relative in ('platforms/android-36/android.jar','build-tools/36.0.0/aapt',
                     'build-tools/36.0.0/aapt2','build-tools/36.0.0/lib/d8.jar',
                     'build-tools/36.0.0/apksigner','ndk/28.2.13676358/source.properties',
                     'ndk/28.2.13676358/toolchains/llvm/prebuilt/'+tag+'/bin/clang',
                     'cmake/3.22.1/bin/cmake'):
        present=sdk is not None and (Path(sdk)/relative).is_file()
        record('SDK:'+relative,present,'present' if present else 'missing')
    return {'schema':'STARCORE_R335_BUILD_PREFLIGHT_1','checks':checks,
            'status':'READY_TO_ATTEMPT_BUILD' if all(x['status']=='PASS' for x in checks) else 'BLOCKED_DEPENDENCIES',
            'scope':'PREREQUISITES_ONLY_NOT_COMPILATION_OR_RUNTIME',
            'secrets':'NOT_READ','dependencies':'NOT_DOWNLOADED'}


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo',type=Path,default=Path(__file__).resolve().parents[2])
    parser.add_argument('--android-sdk',type=Path,default=os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT'))
    parser.add_argument('--out',type=Path)
    args=parser.parse_args();result=collect(args.repo,args.android_sdk)
    text=json.dumps(result,ensure_ascii=False,indent=2)+'\n'
    if args.out:
        args.out.parent.mkdir(parents=True,exist_ok=True)
        with args.out.open('x',encoding='utf-8') as out:out.write(text)
    print(text,end='')
    raise SystemExit(0 if result['status']=='READY_TO_ATTEMPT_BUILD' else 2)


if __name__=='__main__':main()
