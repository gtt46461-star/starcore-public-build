#!/usr/bin/env python3
"""Run retained asset consumer tests on current release bytecode and real SQLite.

Run through run_logged_command.py to retain the command, raw output, exits and
start/end source identities. Host adapters provide transport only, never device
timing, natural Future results, qualified models or real betting authority.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import sqlite3
import subprocess

ROOT = Path(__file__).resolve().parents[2]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--evidence-dir', type=Path, required=True)
    parser.add_argument('--atom-db', type=Path, required=True)
    parser.add_argument('--toolchain-root', type=Path, required=True)
    parser.add_argument('--gapped-negative-dir', type=Path, required=True)
    parser.add_argument('--native-library-dir', type=Path, required=True)
    args = parser.parse_args()
    output = args.evidence_dir.resolve()
    output.mkdir(parents=True, exist_ok=False)
    tools = args.toolchain_root.resolve()
    native = args.native_library_dir.resolve()
    native_receipt = json.loads((native / 'receipt.json').read_text())
    library = native / 'libstarcore_report_cp4.so'
    if hashlib.sha256(library.read_bytes()).hexdigest() != native_receipt['library_sha256']:
        raise RuntimeError('HOST_NATIVE_LIBRARY_IDENTITY_DRIFT')
    for source in native_receipt['source_files']:
        if hashlib.sha256((ROOT/source['path']).read_bytes()).hexdigest() != source['sha256']:
            raise RuntimeError('HOST_NATIVE_PRODUCT_SOURCE_IDENTITY_DRIFT:' + source['path'])
    release = ROOT / 'app/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes'
    platform = tools / 'sdk/platforms/android-36/android.jar'
    json_jar = tools / 'android-json.jar'
    sources = [
        'verification/canonical/schema/SQLiteDatabase.java',
        'verification/canonical/schema/Looper.java',
        'verification/canonical/schema/SchemaCaptureCP4.java',
        'verification/r974/HostProductR974.java',
        'verification/r974/android/database/sqlite/SQLiteOpenHelper.java',
        'verification/r925/stubs/android/content/ContentValues.java',
        'verification/r925/stubs/android/os/CancellationSignal.java',
        'verification/r36-stubs/android/database/sqlite/SQLiteException.java',
        'verification/canonical/tests/audit_cp12/matrix/tests/AtomAssetFdCP12Test.java',
        'verification/canonical/tests/audit_cp12/coverage/tests/com/ethk3/novaonepro/ColdCreatorPublicContractCP12.java',
        'verification/canonical/tests/audit_cp12/coverage/tests/com/ethk3/novaonepro/ModernSeedImportContractCP12.java',
        'verification/batch01/HistoricalCreatorInputSetup.java',
        'verification/canonical/tests/audit_cp12/native/cas_java/com/ethk3/novaonepro/CasDirectoryActualCP12.java',
        *[str(x.relative_to(ROOT)) for x in sorted((ROOT/'verification/canonical/adapters').rglob('*.java'))],
    ]
    compiled = output / 'classes'
    compiled.mkdir()
    classpath = os.pathsep.join(map(str, [release, json_jar, platform]))

    def run(command):
        command = list(map(str, command))
        print('COMMAND ' + json.dumps(command), flush=True)
        subprocess.run(command, cwd=ROOT, check=True)

    run([tools/'jdk17/bin/javac', '-cp', classpath, '-d', compiled,
         *[ROOT/x for x in sources]])
    classpath = str(compiled) + os.pathsep + classpath
    bridge = ROOT / 'verification/canonical/schema/sqlite_bridge.py'
    # Run original CAS JNI contract, including refusal and diagnostics isolation.
    run([tools/'jdk17/bin/java', '-Xmx64m', '-Djava.library.path=' + str(native),
         '-cp', classpath, 'com.ethk3.novaonepro.CasDirectoryActualCP12', output/'cas-native-contract'])

    # Preserve the earlier actual 5000-row failure, then verify the refusal boundary.
    # Never turn that original failing positive receipt into PASS.
    negative=args.gapped_negative_dir.resolve()
    negative_log=negative.parent/'actual-asset-consumers-replay-07.log'
    if 'java.lang.IllegalArgumentException: SCDEV_SEQUENCE_GAP' not in negative_log.read_text():
        raise RuntimeError('ORIGINAL_FULL_SCALE_NEGATIVE_LOG_MISSING')
    with sqlite3.connect(negative/'cold.sqlite') as db:
        if db.execute('PRAGMA integrity_check').fetchall()!=[('ok',)]:
            raise RuntimeError('NEGATIVE_DURABILITY_FAILED')
        for table,expected in [('draws',5000),('research_molecule_seed_v2',4200),('r952_candidate',0),
                               ('research_training_protocol_v2',0),('v57_research_task_r967',0),('trades',0)]:
            if db.execute('SELECT COUNT(*) FROM '+table).fetchone()!=(expected,):
                raise RuntimeError('FULL_SCALE_GAPPED_INPUT_REFUSAL_FAILED:'+table)
    print('PASS|5000_REAL_GAPPED_HISTORY_REFUSED|original_failure_preserved=true|candidate=0|task=0|real=0',flush=True)

    def java(name, *arguments):
        # Positive Creator setup needs the original 16MiB shared algorithm parent.
        # At 64MiB JVM heap, the unchanged maxMemory/8 guard correctly limits it to
        # 8MiB and holds MEMORY_PRESSURE. Other numerical/import cases stay at 64MiB.
        heap = '-Xmx128m' if name.endswith('ColdCreatorPublicContractCP12') else '-Xmx64m'
        run([tools/'jdk17/bin/java', heap, '-Djava.library.path='+str(args.native_library_dir.resolve()),
             '-cp', classpath, name, bridge, *arguments])

    identities = []

    def snapshot(source, target):
        if target.exists():
            raise RuntimeError('REPLAY_DESTINATION_EXISTS')
        with sqlite3.connect(source) as db, sqlite3.connect(target) as backup:
            if db.execute('PRAGMA integrity_check').fetchall() != [('ok',)]:
                raise RuntimeError('SOURCE_SQLITE_INTEGRITY_FAILED')
            if db.execute('SELECT COUNT(*) FROM research_atom_spec_v2').fetchone() != (25350,):
                raise RuntimeError('SOURCE_REAL_ATOM_IMPORT_INCOMPLETE')
            if db.execute('SELECT next_row,state FROM research_atom_catalog_import_cp12').fetchall() != [(25350, 'COMPLETE')]:
                raise RuntimeError('SOURCE_DURABLE_ATOM_CURSOR_INCOMPLETE')
            db.backup(backup)
        identities.append({'source_db': str(source), 'snapshot_db': str(target),
                           'sqlite_backup_api': True, 'atom_rows': 25350,
                           'sha256': hashlib.sha256(target.read_bytes()).hexdigest()})

    def integrity(database):
        with sqlite3.connect(database) as db:
            if db.execute('PRAGMA integrity_check').fetchall() != [('ok',)]:
                raise RuntimeError('REOPEN_SQLITE_INTEGRITY_FAILED')
            if db.execute('PRAGMA foreign_key_check').fetchall():
                raise RuntimeError('REOPEN_SQLITE_FOREIGN_KEYS_FAILED')
        print('PASS|REOPEN_SQLITE_INTEGRITY_AND_FOREIGN_KEYS|' + str(database), flush=True)

    cold = output/'cold.sqlite'
    snapshot(args.atom_db.resolve(), cold)
    inputs = output/'training-inputs'
    inputs.mkdir()
    java('com.ethk3.novaonepro.HistoricalCreatorInputSetup', cold,
         ROOT/'app/src/main/assets/history_seed.csv.gz', 'CONTIGUOUS_REAL_INPUT')
    integrity(cold)
    snapshot(cold,output/'prepared-cold.sqlite')
    java('com.ethk3.novaonepro.ColdCreatorPublicContractCP12', cold,
         ROOT/'app/src/main/assets/cp4_atom_registry.jsonl',
         ROOT/'app/src/main/assets/research/cp4_molecule_seeds.jsonl',
         inputs, output/'cold-creator-result.json')
    integrity(cold)
    seeds = output/'typed-seeds.sqlite'
    snapshot(args.atom_db.resolve(), seeds)
    java('com.ethk3.novaonepro.ModernSeedImportContractCP12', seeds,
         ROOT/'app/src/main/assets/research/cp4_molecule_seeds.jsonl',
         output/'typed-seed-result.json')
    integrity(seeds)
    fd = output/'fd.sqlite'
    java('com.ethk3.novaonepro.SchemaCaptureCP4', fd)
    java('com.ethk3.novaonepro.r952.AtomAssetFdCP12Test', fd,
         ROOT/'app/src/main/assets/cp4_atom_registry.jsonl', output/'fd-container.bin')
    integrity(fd)
    receipt = {'status': 'PASS', 'scope': 'ACTUAL_RELEASE_BYTECODE_HOST_REPLAY_NOT_DEVICE_OR_NATURAL_FUTURE',
               'snapshot_identities': identities,
               'transport_and_test_source_identities': [
                   {'path': x, 'sha256': hashlib.sha256((ROOT/x).read_bytes()).hexdigest()} for x in sources],
               'release_class_identities': [
                   {'path': str(x.relative_to(release)), 'sha256': hashlib.sha256(x.read_bytes()).hexdigest()}
                   for x in sorted(release.rglob('*.class'))],
               'json_jar_sha256': hashlib.sha256(json_jar.read_bytes()).hexdigest(),
               'cold_creator': json.loads((output/'cold-creator-result.json').read_text()),
               'typed_seeds': json.loads((output/'typed-seed-result.json').read_text()),
               'native_heap_and_android_timing': 'NOT_MEASURED_HOST_ONLY',
               'creator_host_heap_mb': 128,
               'host_native_library_sha256': native_receipt['library_sha256'],
               'algorithm_parent_policy_unchanged': 'min(16MiB,maxMemory/8); no budget or policy override'}
    (output/'receipt.json').write_text(json.dumps(receipt, indent=2)+'\n')
    print(json.dumps({k:receipt[k] for k in ['status','scope','cold_creator','typed_seeds']}, indent=2))


if __name__ == '__main__':
    main()
