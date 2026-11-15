#!/usr/bin/env python3
"""Build-local identity, exact input verification, no tracked-source mutation.

The original sorted Java PATH/SHA256 contract excludes SourceManifestR815 itself.
The detached manifest also binds that facade, generated semantics, native source,
assets and build scripts. Runtime APK/DEX hashes remain separately observed.
"""
import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
FACADE = 'app/src/main/java/com/ethk3/novaonepro/SourceManifestR815.java'
SEMANTICS = 'app/build/generated/source/r967/com/ethk3/novaonepro/ResearchSemanticsGeneratedR967.java'
JAVA = 'app/build/generated/source/r815/com/ethk3/novaonepro/BuildSourceIdentityCP12.java'
MANIFEST = 'app/build/generated/r815/source-manifest.json'
ASSET = 'app/build/generated/assets/r815/diagnostics/source-provenance.json'
BUILD_CONTRACTS = (FACADE, 'app/build.gradle', 'build.gradle', 'settings.gradle',
                   'gradle.properties', 'gradle/wrapper/gradle-wrapper.properties',
                   'scripts/r815_generate_provenance.py', 'scripts/r815_generate_provenance.sh',
                   'scripts/r817_generate_provenance.sh', 'scripts/r967_semantic_identity.py',
                   'scripts/r926_release_manifest.py', 'scripts/cp4_compile_catalog.py',
                   'scripts/cp4_compile_molecule_seeds.py', 'scripts/verify_catalog_assets_cp12.py',
                   'scripts/r982_source_call_asset.py', 'scripts/r997_construction_assets.py',
                   'scripts/opt299_stage_build.py',
                   'release/research_dependencies_r967.json', 'release/research_gate_plan_r967.json')


def fail(reason):
    raise ValueError('R817_PROVENANCE=FAIL|' + reason)


def sha(path):
    if not path.is_file() or path.is_symlink():
        fail('MISSING_OR_SYMLINK_INPUT:' + path.name)
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        while block := stream.read(1024 * 1024):
            digest.update(block)
    return digest.hexdigest()


def canonical(value):
    return json.dumps(value, sort_keys=True, ensure_ascii=False, separators=(',', ':'))


def records(root, paths):
    result = []
    for path in sorted(paths, key=lambda p: p.relative_to(root).as_posix()):
        name = path.relative_to(root).as_posix()
        if '\n' in name or '\r' in name:
            fail('INPUT_PATH_CONTROL_CHARACTER')
        result.append({'path': name, 'sha256': sha(path), 'bytes': path.stat().st_size})
    return result


def files(root, directory):
    base = root / directory
    if not base.is_dir() or base.is_symlink():
        fail('MISSING_INPUT_DIRECTORY:' + directory)
    result = []
    for path in base.rglob('*'):
        if path.is_symlink():
            fail('SYMLINK_INPUT:' + path.relative_to(root).as_posix())
        if path.is_file():
            result.append(path)
    return result


def record_hash(rows):
    digest = hashlib.sha256()
    for row in rows:
        digest.update(('PATH=' + row['path'] + '\nSHA256=' + row['sha256'] + '\n').encode())
    return digest.hexdigest()


def timestamp(value):
    try:
        parsed = datetime.datetime.strptime(value, '%Y-%m-%dT%H:%M:%SZ')
    except (ValueError, TypeError):
        fail('GENERATED_AT_UTC_FORMAT')
    if parsed.strftime('%Y-%m-%dT%H:%M:%SZ') != value:
        fail('GENERATED_AT_UTC_FORMAT')
    return value


def compute(root, toolchain):
    source = [p for p in files(root, 'app/src/main/java') if p.suffix == '.java' and p.relative_to(root).as_posix() != FACADE]
    if not source:
        fail('EMPTY_JAVA_SOURCE_SET')
    resources = [root / 'app/src/main/AndroidManifest.xml'] + files(root, 'app/src/main/res')
    contracts = [root / p for p in BUILD_CONTRACTS]
    inputs = {'java': records(root, source), 'resources': records(root, resources),
              'native': records(root, files(root, 'app/src/main/cpp')),
              'assets': records(root, files(root, 'app/src/main/assets')),
              'generatedJava': records(root, [root / SEMANTICS]),
              'buildContracts': records(root, contracts)}
    if not toolchain:
        fail('TOOLCHAIN_FILES_REQUIRED')
    names, tools = set(), []
    for item in toolchain:
        name, separator, path = item.partition('=')
        if not separator:
            path, name = item, Path(item).name
        if not name or name in names or any(c in name for c in '\n\r|'):
            fail('TOOLCHAIN_LOGICAL_NAME_INVALID_OR_DUPLICATE')
        names.add(name)
        p = Path(path)
        # SDK/JDK executable links are legitimate; bind their actual target bytes.
        # Source and resource symlinks remain forbidden by files()/records().
        tools.append({'path': name, 'sha256': sha(p.resolve(strict=True)), 'bytes': p.stat().st_size})
    version = subprocess.run(['java', '-version'], capture_output=True, check=True)
    java_version = (version.stdout + version.stderr).decode('utf-8')
    tools.sort(key=lambda row: row['path'])
    inputs['toolchain'] = {'javaRuntime': java_version, 'files': tools,
                           'scope': 'EXACT_DECLARED_BUILD_TOOL_BYTES; APK_RUNTIME_FINGERPRINTS_SEPARATE'}
    tool_bytes = 'JAVA_RUNTIME\n' + java_version + 'TOOLCHAIN_FILES\n'
    tool_bytes += ''.join('PATH=' + r['path'] + '\nSHA256=' + r['sha256'] + '\n' for r in tools)
    return {'format': 'CP12_BUILD_SOURCE_PROVENANCE_V1', 'sourceTreeSha256': record_hash(inputs['java']),
            'resourceManifestSha256': record_hash(inputs['resources']),
            'toolchainSha256': hashlib.sha256(tool_bytes.encode()).hexdigest(),
            'buildInputsSha256': hashlib.sha256(canonical(inputs).encode()).hexdigest(),
            'inputs': inputs, 'acceptanceBoundary': 'BUILD_INPUT_IDENTITY_ONLY_NOT_RUNTIME_COVERAGE_DEVICE_OR_MODEL_QUALIFICATION'}


def render(manifest):
    return ('package com.ethk3.novaonepro;\n\n'
            '/** Generated by r815_generate_provenance; verified against actual build inputs. */\n'
            'final class BuildSourceIdentityCP12 {\n'
            # Generated inputs change without editing the facade. They must not
            # be javac constants: an incrementally retained facade would inline
            # its previous identity even though this generated class is rebuilt.
            + ''.join('    static final String ' + key + ' = value("' + manifest[value] + '");\n' for key, value in
                      [('SOURCE_TREE_SHA256', 'sourceTreeSha256'), ('RESOURCE_MANIFEST_SHA256', 'resourceManifestSha256'),
                       ('TOOLCHAIN_SHA256', 'toolchainSha256'), ('GENERATED_AT_UTC', 'generatedAtUtc')])
            + '    private static String value(String text) { return text; }\n'
            + '    private BuildSourceIdentityCP12() {}\n}\n').encode()


def atomic_write(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.is_file() and path.read_bytes() == content:
        return
    descriptor, name = tempfile.mkstemp(dir=path.parent, prefix=path.name + '.', suffix='.partial')
    try:
        with os.fdopen(descriptor, 'wb') as stream:
            stream.write(content)
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(name, path)
    finally:
        if os.path.exists(name):
            os.unlink(name)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--root', type=Path, default=ROOT)
    parser.add_argument('--toolchain-file', action='append', default=[])
    args = parser.parse_args()
    root = args.root.resolve()
    declared = os.environ.get('R817_TOOLCHAIN_FILES', os.environ.get('R816_TOOLCHAIN_FILES', os.environ.get('R815_TOOLCHAIN_FILES', '')))
    manifest = compute(root, args.toolchain_file or [p for p in declared.split('|') if p])
    prior = json.loads((root / MANIFEST).read_text()) if (root / MANIFEST).is_file() else None
    supplied = os.environ.get('R817_GENERATED_AT_UTC', os.environ.get('R816_GENERATED_AT_UTC', os.environ.get('R815_GENERATED_AT_UTC', '')))
    if args.check:
        if prior is None:
            fail('GENERATED_MANIFEST_MISSING')
        generated = supplied or prior.get('generatedAtUtc')
    elif supplied:
        generated = supplied
    elif prior and prior.get('buildInputsSha256') == manifest['buildInputsSha256']:
        generated = prior.get('generatedAtUtc')
    else:
        generated = datetime.datetime.now(datetime.timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')
    manifest['generatedAtUtc'] = timestamp(generated)
    payload = (json.dumps(manifest, ensure_ascii=False, indent=2) + '\n').encode()
    artifacts = {JAVA: render(manifest), MANIFEST: payload, ASSET: payload}
    if args.check:
        for name, expected in artifacts.items():
            path = root / name
            if not path.is_file() or path.read_bytes() != expected:
                fail('PROVENANCE_MISMATCH:' + name)
    else:
        for name, content in artifacts.items():
            atomic_write(root / name, content)
    print('R817_PROVENANCE=PASS|MODE=' + ('CHECK' if args.check else 'GENERATE')
          + '|SOURCE=' + manifest['sourceTreeSha256'] + '|RESOURCE=' + manifest['resourceManifestSha256']
          + '|TOOLCHAIN=' + manifest['toolchainSha256'] + '|INPUTS=' + manifest['buildInputsSha256']
          + '|GENERATED_AT=' + manifest['generatedAtUtc'])


if __name__ == '__main__':
    try:
        main()
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        raise SystemExit(str(error)) from error
