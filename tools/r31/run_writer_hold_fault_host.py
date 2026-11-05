#!/usr/bin/env python3
"""Freeze production writer/Health inputs and run real delayed native SQLite transactions."""
import argparse
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools/r31'))
from host_runtime_fixture import compile_owned_platform


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--classes', type=Path, required=True)
    parser.add_argument('--joint-classes', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    out = args.out.resolve(); out.mkdir(parents=True, exist_ok=False)
    work = Path(tempfile.mkdtemp(prefix='r31-writer-hold-fault-'))
    sdk = Path(os.environ['ANDROID_HOME']) / 'platforms/android-36/android.jar'
    jdbc = Path(os.environ['SQLITE_JDBC'])
    receipt = {'scope': 'FROZEN_PRODUCTION_NATIVE_SQLITE_ENGINEERING_FAULT_INJECTION_NOT_ANDROID_OR_NATURAL',
               'work': str(work), 'sourceHead': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
               'startedAt': dt.datetime.now(dt.timezone.utc).isoformat(), 'sources': [], 'commands': [],
               'support': {'path': str(args.classes), 'sha256': digest(args.classes)},
               'state': 'IMPLEMENTING', 'physicalDevice': 'NOT_RUN', '24h': 'NOT_RUN', '72h': 'NOT_RUN', 'naturalQuality': 'NOT_EVALUATED',
               'boundaries': ['Injected counter measures actual coordinator commit/replay semantics, not a scientific learner or quality.',
                              'HOST adapters provide platform connections and clocks; durations are actual HOST monotonic intervals.',
                              'An unflushed in-memory observation is not durable across process death; snapshot reports this limitation.',
                              'The frozen joint API closure supplies unrelated new producer APIs; this is not whole-project Android compilation.']}

    def save():
        (out / 'RECEIPT.json').write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + '\n')

    def run(name, command):
        command = list(map(str, command)); started = dt.datetime.now(dt.timezone.utc)
        result = subprocess.run(command, cwd=ROOT, capture_output=True, text=True, timeout=240)
        log = out / (name + '.log'); log.write_text(result.stdout + result.stderr)
        receipt['commands'].append({'name': name, 'argv': command, 'exitCode': result.returncode,
                                    'elapsedSeconds': (dt.datetime.now(dt.timezone.utc) - started).total_seconds(), 'log': str(log), 'sha256': digest(log)})
        save(); print(name, result.returncode, flush=True)
        if result.returncode:
            raise RuntimeError(name + ' failed; original log retained')
        return result

    def snapshot(origin, target):
        target.parent.mkdir(parents=True, exist_ok=True); target.write_bytes(origin.read_bytes())
        receipt['sources'].append({'origin': str(origin), 'snapshot': str(target), 'sha256': digest(origin)})
        return target

    try:
        api = work / 'joint-api'; api.mkdir(); inventory = {}
        for origin in sorted(args.joint_classes.rglob('*.class')):
            relative = origin.relative_to(args.joint_classes); target = api / relative
            target.parent.mkdir(parents=True, exist_ok=True); target.write_bytes(origin.read_bytes()); inventory[str(relative)] = digest(origin)
        if not inventory: raise RuntimeError('Joint API closure is empty')
        receipt['jointApiInventory'] = inventory
        platform = compile_owned_platform(ROOT, work / 'platform-inputs', args.classes, sdk)
        # The original HOST adapter lacked the public close API used by production readback/reapers.
        # Closing here closes the actual JDBC connection; no production cancellation method is replaced.
        sqlite = work / 'platform-inputs/sources/android/database/sqlite/SQLiteDatabase.java'
        text = sqlite.read_text()
        text = text.replace('public class SQLiteDatabase {', 'public class SQLiteDatabase implements java.lang.AutoCloseable {', 1)
        at = text.index('    static final class Rows')
        text = text[:at] + '    public void close(){try{jdbc.close();}catch(java.sql.SQLException failure){throw new SQLiteException(failure.getMessage());}}\n' + text[at:]
        sqlite.write_text(text)
        receipt['hostSqliteCloseAdapterSha256'] = digest(sqlite)
        run('compile-real-close-adapter', ['javac', '-encoding', 'UTF-8', '-cp', str(platform), '-d', platform, sqlite])
        production = work / 'production'; production.mkdir()
        base = ROOT / 'app/src/main/java/com/ethk3/novaonepro'
        names = ['DatabaseCoordinator.java', 'strategyv2/WriterHoldFaultsR31.java', 'strategyv2/RuntimeR27.java']
        sources = [snapshot(base / name, work / 'source/production' / name) for name in names]
        cp = os.pathsep.join(map(str, (sdk, api, args.classes)))
        run('compile-actual-production', ['javac', '-encoding', 'UTF-8', '-sourcepath', '', '-implicit:none', '-cp', cp, '-d', production, *sources])
        test = snapshot(ROOT / 'tools/r31/host/R31WriterHoldFaultHost.java', work / 'source/host/R31WriterHoldFaultHost.java')
        hosts = work / 'hosts'; hosts.mkdir()
        runtime = os.pathsep.join(map(str, (platform, production, api, args.classes, jdbc, sdk)))
        run('compile-host', ['javac', '-encoding', 'UTF-8', '-sourcepath', '', '-implicit:none', '-cp', runtime, '-d', hosts, test])
        for mode in ['basic', 'schema', 'backpressure']:
            run(mode, ['java', '-Xmx512m', '-cp', str(hosts) + os.pathsep + runtime,
                       'com.ethk3.novaonepro.R31WriterHoldFaultHost', mode, work / (mode + '.db')])
        changed = [row['origin'] for row in receipt['sources'] if digest(Path(row['origin'])) != row['sha256']]
        receipt['sourceChangesDuringRun'] = changed
        if changed: raise RuntimeError('Source drift: receipt applies only to frozen snapshot')
        receipt['state'] = 'NATIVE_NAMED_PROTOCOLS_PASS_NOT_ANDROID_OR_DEVICE'
    except BaseException as failure:
        receipt['state'] = 'FAILED'; receipt['failure'] = repr(failure); raise
    finally:
        receipt['finishedAt'] = dt.datetime.now(dt.timezone.utc).isoformat(); save()


if __name__ == '__main__':
    main()
