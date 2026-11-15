#!/usr/bin/env python3
"""Run the existing production mainline HOST fixture with explicit dependencies.

This creates only disposable compiler/SDK/cache aliases. It does not install
anything, sign an APK, create production certificates, or claim natural quality.
"""
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo', type=Path, required=True)
    parser.add_argument('--classes', type=Path, required=True)
    parser.add_argument('--android-jar', type=Path, required=True)
    parser.add_argument('--sqlite-jdbc', type=Path, required=True)
    parser.add_argument('--slf4j', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    repo, classes, sdk, jdbc, slf = [path.resolve() for path in
                                  (args.repo, args.classes, args.android_jar,
                                   args.sqlite_jdbc, args.slf4j)]
    runner = repo / 'tools/r31/run_typed_mainline_host.py'
    for path in (runner, sdk, jdbc, slf):
        if not path.is_file():
            parser.error('Required file missing: ' + str(path))
    if not classes.exists():
        parser.error('Compiled production classes missing: ' + str(classes))
    if not shutil.which('java'):
        parser.error('A Java 17+ JDK runtime is required')
    with tempfile.TemporaryDirectory(prefix='r32-mainline-env-') as temporary:
        work = Path(temporary)
        platform = work / 'sdk/platforms/android-36/android.jar'
        api = work / 'gradle/caches/modules-2/files-2.1/org.slf4j/slf4j-api/local/explicit/slf4j-api.jar'
        for target, source in ((platform, sdk), (api, slf)):
            target.parent.mkdir(parents=True, exist_ok=True)
            target.symlink_to(source)
        compiler = work / 'bin/javac'
        compiler.parent.mkdir()
        compiler.write_text('#!' + sys.executable + '\n'
            'import os,sys\n'
            'vm=[x[2:] for x in sys.argv[1:] if x.startswith("-J")]\n'
            'args=[x for x in sys.argv[1:] if not x.startswith("-J")]\n'
            'os.execvp("java",["java",*vm,"-m","jdk.compiler/com.sun.tools.javac.Main",*args])\n')
        compiler.chmod(0o700)
        env = os.environ.copy()
        env.update(ANDROID_HOME=str(work / 'sdk'), SQLITE_JDBC=str(jdbc),
                   GRADLE_USER_HOME=str(work / 'gradle'),
                   PATH=str(compiler.parent) + os.pathsep + env.get('PATH', ''))
        subprocess.run([sys.executable, str(runner), '--classes', str(classes),
                        '--out', str(args.out.resolve())], cwd=repo, env=env, check=True)
    receipt_path = args.out.resolve() / 'RECEIPT.json'
    receipt = json.loads(receipt_path.read_text())
    if receipt.get('status') != 'PASS_HOST_ACTUAL_PATH_NOT_ANDROID_OR_NATURAL':
        raise RuntimeError('Mainline fixture did not produce the exact HOST pass state')
    print('HOST_MAINLINE_ACTUAL_PATH_PASS receipt=' + str(receipt_path))


if __name__ == '__main__':
    main()
