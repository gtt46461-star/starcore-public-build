#!/usr/bin/env python3
"""Run actual provenance generator against isolated changed/missing build inputs."""
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import time

parser = argparse.ArgumentParser()
parser.add_argument('--out', type=Path, required=True)
args = parser.parse_args()
root = Path(__file__).resolve().parents[2]
script = root / 'scripts/r815_generate_provenance.py'
spec = importlib.util.spec_from_file_location('provenance', script)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
out = args.out.resolve()
out.mkdir(parents=True, exist_ok=False)
receipts = []
with tempfile.TemporaryDirectory(prefix='starcore-provenance-current-') as temporary:
    fixture = Path(temporary) / 'source'
    fixture.mkdir()
    originals = {
        module.FACADE: (root / module.FACADE).read_bytes(),
        'app/src/main/java/com/ethk3/novaonepro/ActualInput.java': b'package com.ethk3.novaonepro; final class ActualInput {}\n',
        'app/src/main/AndroidManifest.xml': (root / 'app/src/main/AndroidManifest.xml').read_bytes(),
        'app/src/main/res/values/strings.xml': b'<resources><string name="test">actual</string></resources>\n',
        'app/src/main/cpp/actual.c': b'int actual(void) { return 17; }\n',
        'app/src/main/assets/catalog.jsonl': b'{"identity":"synthetic-input-only"}\n',
        module.SEMANTICS: (root / module.SEMANTICS).read_bytes(),
    }
    for name in module.BUILD_CONTRACTS:
        originals[name] = (root / name).read_bytes()
    for name, content in originals.items():
        path = fixture / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(content)
    tool = Path(temporary) / 'compiler-input.bin'
    tool.write_bytes(b'current isolated toolchain byte fixture')
    tool_bytes = tool.read_bytes()
    environment = {k: v for k, v in os.environ.items() if not k.startswith(('R815_', 'R816_', 'R817_'))}
    base = ['python3', str(script), '--root', str(fixture), '--toolchain-file', 'compiler=' + str(tool)]

    def run(name, expected, extra=None, env=None):
        command = base + (extra or [])
        started = time.monotonic()
        result = subprocess.run(command, capture_output=True, text=True, env=env or environment, timeout=120)
        log = out / (name + '.log')
        log.write_text(result.stdout + result.stderr)
        passed = result.returncode == 0 if expected == 'PASS' else result.returncode != 0
        receipts.append({'assertion': name, 'expected': expected, 'status': 'PASS' if passed else 'FAIL',
                         'command': command, 'exit_code': result.returncode, 'seconds': time.monotonic() - started,
                         'log_sha256': hashlib.sha256(log.read_bytes()).hexdigest()})
        if not passed:
            raise AssertionError(name + ':' + result.stdout + result.stderr)

    try:
        run('initial_generation', 'PASS')
        run('exact_input_check', 'PASS', ['--check'])
        frozen = {name: (fixture / name).read_bytes() for name in (module.JAVA, module.MANIFEST, module.ASSET)}
        run('repeat_generation', 'PASS')
        assert frozen == {name: (fixture / name).read_bytes() for name in frozen}, 'IDEMPOTENT_BYTES'
        assert all((fixture / name).read_bytes() == value for name, value in originals.items()), 'TRACKED_INPUT_IMMUTABILITY'
        for index, name in enumerate(originals):
            path = fixture / name
            path.write_bytes(originals[name] + b'\n')
            run('input_mutation_' + str(index), 'FAIL', ['--check'])
            path.write_bytes(originals[name])
        added = fixture / 'app/src/main/java/com/ethk3/novaonepro/AddedInput.java'
        added.write_text('package com.ethk3.novaonepro; class AddedInput {}\n')
        run('added_java_rejected', 'FAIL', ['--check'])
        added.unlink()
        missing = fixture / module.SEMANTICS
        missing.unlink()
        run('missing_generated_semantics', 'FAIL', ['--check'])
        missing.write_bytes(originals[module.SEMANTICS])
        tool.write_bytes(tool_bytes + b'changed')
        run('changed_toolchain_bytes', 'FAIL', ['--check'])
        tool.unlink()
        run('missing_toolchain_rejected', 'FAIL')
        tool.write_bytes(tool_bytes)
        run('duplicate_logical_toolchain_name', 'FAIL', ['--toolchain-file', 'compiler=' + str(tool)])
        bad_time = dict(environment, R817_GENERATED_AT_UTC='2026-02-30T00:00:00Z')
        run('invalid_calendar_timestamp', 'FAIL', env=bad_time)
        for index, name in enumerate(frozen):
            (fixture / name).write_bytes(frozen[name] + b'changed')
            run('generated_output_corruption_' + str(index), 'FAIL', ['--check'])
            (fixture / name).write_bytes(frozen[name])
        symlink = fixture / 'app/src/main/res/values/linked.xml'
        symlink.symlink_to(fixture / 'app/src/main/res/values/strings.xml')
        run('symlink_input_rejected', 'FAIL')
        symlink.unlink()
        changed = fixture / 'app/src/main/java/com/ethk3/novaonepro/ActualInput.java'
        changed.write_bytes(originals[changed.relative_to(fixture).as_posix()] + b'\n')
        run('changed_source_requires_regeneration', 'FAIL', ['--check'])
        run('regenerate_changed_source', 'PASS')
        run('recovered_exact_input_check', 'PASS', ['--check'])
        old = json.loads(frozen[module.MANIFEST])
        new = json.loads((fixture / module.MANIFEST).read_bytes())
        assert old['sourceTreeSha256'] != new['sourceTreeSha256'], 'SOURCE_IDENTITY_CHANGES'
        assert old['resourceManifestSha256'] == new['resourceManifestSha256'], 'UNCHANGED_RESOURCE_IDENTITY'
        (out / 'REGENERATED_MANIFEST.json').write_text(json.dumps(new, indent=2) + '\n')
    finally:
        (out / 'CURRENT_EXECUTION.json').write_text(json.dumps({'executions': receipts,
            'source_binding': {'path': str(script.relative_to(root)), 'sha256': hashlib.sha256(script.read_bytes()).hexdigest()},
            'scope': 'ACTUAL_PRODUCTION_GENERATOR_ISOLATED_INPUT_MUTATIONS_NOT_ANDROID_RUNTIME_DEVICE_OR_MODEL_EVIDENCE',
            'implicit_assertions': ['IDEMPOTENT_BYTES', 'TRACKED_INPUT_IMMUTABILITY', 'SOURCE_IDENTITY_CHANGES', 'UNCHANGED_RESOURCE_IDENTITY']}, indent=2) + '\n')
print('CURRENT_PROVENANCE_MUTATIONS=PASS|executions=' + str(len(receipts)))
