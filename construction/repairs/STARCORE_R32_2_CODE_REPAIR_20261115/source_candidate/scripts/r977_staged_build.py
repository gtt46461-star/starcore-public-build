#!/usr/bin/env python3
"""Run the canonical R977 build in restartable foreground steps.

This is the same build script, split at checked markers, not an alternate product
implementation. Work files may contain credentials from the base APK. Keep --work
private, never package it, and run --step clean after sign. A successful step
records the exact input digest; resume after an input change is refused.
"""
import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import time
import zipfile

STEPS = ('prepare', 'resources', 'compile', 'dex', 'dexcheck', 'assemble', 'verify', 'sign')


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--step', choices=STEPS + ('all', 'clean'), required=True)
    for name in ('base-apk', 'base-source', 'toolchain', 'work', 'out'):
        p.add_argument('--' + name, type=Path, required=True)
    p.add_argument('--signing-kit', type=Path)
    a = p.parse_args()
    root = Path(__file__).resolve().parents[1]
    work, out = a.work.resolve(), a.out.resolve()
    if work == root or root.is_relative_to(work) or work == out or out.is_relative_to(work):
        raise SystemExit('Scratch must be separate from source and deliverable output')
    if a.step == 'clean':
        if not (work / 'r977-private-scratch.marker').is_file():
            raise SystemExit('Refusing to remove an unmarked directory')
        shutil.rmtree(work)
        print('PRIVATE_BUILD_SCRATCH_REMOVED=YES')
        return
    script = root / 'scripts/r977_chain_repair_build.sh'
    s = script.read_text()
    markers = [
        'mkdir -p "$OUT_DIR/evidence"',
        '# Stage one compiles',
        '# Decode the compiled skeleton',
        'java -Xmx2g -cp "$R8"',
        '# Prove the product Dex identity',
        '# Replace every predecessor Dex',
        '# Decode the exact assembled APK',
        'sha256sum "$UNSIGNED"',
    ]
    if any(s.count(m) != 1 for m in markers):
        raise SystemExit('Canonical build stage markers changed; review stage boundaries')
    cuts = [s.index(m) for m in markers] + [len(s)]
    if cuts != sorted(cuts):
        raise SystemExit('Canonical stage order changed')
    blocks = dict(zip(STEPS, (s[cuts[i]:cuts[i+1]] for i in range(len(STEPS)))))
    # Credentials are needed in compile rather than preparation. Preparation
    # still validates their format, and neither invocation prints them.
    blocks['compile'] = s[s.index('BASE_BUILD_CONFIG='):s.index('# Stage one compiles')] + blocks['compile']
    blocks['sign'] = ('class_count=$(find "$TMP/product-smali/com/ethk3/novaonepro" -name "*.smali" | wc -l)\n'
                      + blocks['sign'].replace('R977_PRODUCT_CLASSES=$class_count',
                                              'R977_DEX_SMALI_FILES=$class_count'))
    common = '''set -euo pipefail
ROOT=$R977_ROOT
BASE=$R977_BASE
OUT_DIR=$R977_OUT
TC=$STARCORE_TOOLCHAIN_ROOT
TMP=$R977_WORK
ANDROID="$TC/android-platform-36/android.jar"
R8="$TC/r8-9.2.23/r8-9.2.23.jar"
APKTOOL="$TC/apktool/apktool.jar"
LIB="$TC/jadx-node/package/resources/jadx-1.1.0/lib"
ZIPALIGN="$TC/android-signing/zipalign"
APKSIGNER="$TC/android-signing/apksigner"
AAPT2="$TC/aapt/bin/x64/linux/aapt2"
BASE_JAVA="$TMP/predecessor-source/java"
APP_ID=com.ethk3.novaonepro
VERSION_NAME=V57.21.2.0.20-R5.4.2.20-977
VERSION_CODE=577250977
VERSION_CODE_HEX=$(printf '0x%x' "$VERSION_CODE")
UNSIGNED="$OUT_DIR/Starcore-Tianshu-$VERSION_NAME-unsigned-aligned.apk"
fail() { echo "R977_STAGE_FAILURE=$1" >&2; exit 1; }
cd "$ROOT"
'''
    work.mkdir(parents=True, exist_ok=True); work.chmod(0o700)
    (work / 'r977-private-scratch.marker').touch()
    (out / 'evidence').mkdir(parents=True, exist_ok=True)
    inputs = []
    for folder in ('app/src', 'release'):
        inputs.extend(x for x in (root / folder).rglob('*') if x.is_file())
    inputs += [script, Path(__file__), root/'scripts/r967_semantic_identity.py',
               root/'scripts/r926_release_manifest.py', root/'scripts/r922_generate_r_java.py',
               root/'verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java']
    digest = hashlib.sha256()
    for f in sorted(set(inputs)):
        digest.update(str(f.relative_to(root)).encode()); digest.update(b'\0'); digest.update(f.read_bytes())
    digest.update(a.base_apk.read_bytes())
    for f in sorted(a.base_source.rglob('*.java')):
        digest.update(str(f.relative_to(a.base_source)).encode()); digest.update(f.read_bytes())
    input_sha = digest.hexdigest()
    meta = work / 'state.json'
    state = json.loads(meta.read_text()) if meta.exists() else {'inputs_sha256': input_sha, 'completed': {}}
    if state['inputs_sha256'] != input_sha:
        raise SystemExit('Build inputs changed after preparation; clean scratch and rebuild')
    env = dict(os.environ, R977_ROOT=str(root), R977_BASE=str(a.base_apk.resolve()),
               R977_OUT=str(out), R977_WORK=str(work), STARCORE_TOOLCHAIN_ROOT=str(a.toolchain.resolve()),
               STARCORE_BASE_JAVA_ROOT=str(a.base_source.resolve()))
    def execute(step: str, execution_env: dict) -> None:
        missing = [s for s in STEPS[:STEPS.index(step)] if s not in state['completed']]
        if missing:
            raise SystemExit('Prior build steps incomplete: ' + ', '.join(missing))
        if step in state['completed']:
            print(step.upper() + '=ALREADY_COMPLETED_SAME_INPUTS'); return
        start = time.time()
        log = out / 'evidence' / ('stage-' + step + '.log')
        with log.open('w') as fh:
            result = subprocess.run(['bash', '-c', common + blocks[step]], env=execution_env,
                                    stdout=fh, stderr=subprocess.STDOUT, cwd=root)
        # Copy diagnostics even on a real stage failure. No private scratch is copied.
        for src in (work / 'logs').glob('*'):
            if src.is_file(): shutil.copy2(src, out / 'evidence' / src.name)
        if result.returncode != 0:
            print(log.read_text()[-6000:]); raise SystemExit(result.returncode)
        state['completed'][step] = {'started_unix': start, 'finished_unix': time.time(),
                                    'command_sha256': hashlib.sha256(blocks[step].encode()).hexdigest(),
                                    'exit_code': 0}
        meta.write_text(json.dumps(state, indent=2));
        (out/'evidence'/'staged-build-state.json').write_text(json.dumps(state, indent=2))
        print(step.upper() + '=PASS', 'seconds=%.3f' % (time.time()-start), 'inputs_sha256=' + input_sha)
    for step in (STEPS if a.step == 'all' else (a.step,)):
        if step != 'sign': execute(step, env); continue
        if a.signing_kit is None:
            raise SystemExit('--signing-kit is required for sign; unsigned APK is not an overwrite APK')
        with zipfile.ZipFile(a.signing_kit) as outer:
            nested = [n for n in outer.namelist() if n.lower().endswith('release-signing-kit.zip')]
            if len(nested) > 1: raise SystemExit('Ambiguous owner signing kit')
            raw = outer.read(nested[0]) if nested else a.signing_kit.read_bytes()
        with zipfile.ZipFile(io.BytesIO(raw)) as kit, tempfile.TemporaryDirectory(prefix='r977-sign-') as td:
            keys = [n for n in kit.namelist() if n.lower().endswith('.p12')]
            creds = [n for n in kit.namelist() if Path(n).name == 'SIGNING-CREDENTIALS.txt']
            if len(keys) != 1 or len(creds) != 1: raise SystemExit('Invalid owner signing-kit contents')
            text = kit.read(creds[0]).decode('utf-8-sig')
            def field(name: str) -> str:
                m = re.search(r'^\s*'+re.escape(name)+r'\s*[:=]\s*(.+?)\s*$', text, re.M|re.I)
                if not m: raise SystemExit('Missing signing field ' + name)
                return m.group(1)
            key = Path(td)/'owner.p12'; key.write_bytes(kit.read(keys[0])); key.chmod(0o600)
            signing_env = dict(env, STARCORE_SIGNING_P12=str(key), STARCORE_SIGNING_PASSWORD=field('Password'),
                               STARCORE_SIGNING_ALIAS=field('Alias'), STARCORE_SIGNING_TYPE=field('Keystore type'))
            execute(step, signing_env)
        print('TEMPORARY_SIGNING_KEY_REMOVED=YES')

if __name__ == '__main__':
    main()
