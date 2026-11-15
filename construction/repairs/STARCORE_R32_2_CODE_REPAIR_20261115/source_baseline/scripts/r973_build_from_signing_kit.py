#!/usr/bin/env python3
"""Build reviewed source using an owner signing-kit ZIP, without exporting its key."""
import argparse
import io
import os
from pathlib import Path
import re
import subprocess
import tempfile
import zipfile


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('base-apk', 'base-source', 'signing-kit', 'toolchain', 'out'):
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    if not all(p.is_file() for p in (args.base_apk, args.signing_kit)):
        raise SystemExit('Missing base APK or authorized signing kit')
    if not (args.base_source / 'AutoFetchService.java').is_file():
        raise SystemExit('--base-source must identify the verified predecessor Java package directory')
    with zipfile.ZipFile(args.signing_kit) as outer:
        nested = [n for n in outer.namelist() if n.lower().endswith('release-signing-kit.zip')]
        if nested:
            if len(nested) != 1:
                raise SystemExit('Ambiguous signing-kit archive')
            kit_bytes = outer.read(nested[0])
        else:
            kit_bytes = args.signing_kit.read_bytes()
    with zipfile.ZipFile(io.BytesIO(kit_bytes)) as kit, tempfile.TemporaryDirectory(prefix='r973-owner-key-') as td:
        keys = [n for n in kit.namelist() if n.lower().endswith('.p12')]
        credentials = [n for n in kit.namelist() if Path(n).name == 'SIGNING-CREDENTIALS.txt']
        if len(keys) != 1 or len(credentials) != 1:
            raise SystemExit('Signing kit must contain one PKCS12 key and one credential file')
        key = Path(td) / 'owner.p12'
        key.write_bytes(kit.read(keys[0])); key.chmod(0o600)
        text = kit.read(credentials[0]).decode('utf-8-sig')
        def field(name: str) -> str:
            match = re.search(r'^\s*' + re.escape(name) + r'\s*[:=]\s*(.+?)\s*$', text, re.M | re.I)
            if match is None:
                raise SystemExit('Required signing field missing: ' + name)
            return match.group(1)
        env = dict(os.environ, STARCORE_TOOLCHAIN_ROOT=str(args.toolchain.resolve()),
                   STARCORE_BASE_JAVA_ROOT=str(args.base_source.resolve()),
                   STARCORE_SIGNING_P12=str(key), STARCORE_SIGNING_PASSWORD=field('Password'),
                   STARCORE_SIGNING_ALIAS=field('Alias'), STARCORE_SIGNING_TYPE=field('Keystore type'))
        args.out.mkdir(parents=True, exist_ok=True)
        subprocess.run(['bash', 'scripts/r973_chain_repair_build.sh', str(args.base_apk.resolve()),
                        str(args.out.resolve())], cwd=root, env=env, check=True)
    print('PRIVATE_KEY_TEMPORARY_DIRECTORY_REMOVED=YES')


if __name__ == '__main__':
    main()
