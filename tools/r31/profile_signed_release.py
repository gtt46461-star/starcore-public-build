#!/usr/bin/env python3
"""Observe one exact signed release through its real service; publish no raw logs/DB."""
import argparse
import hashlib
import json
import re
import sqlite3
import subprocess
import tempfile
import time
from pathlib import Path

PACKAGE = 'com.starcore.research.r13'
SOURCE = '971c376b317dba3acb6eb10ddee49b41a91593b2'
TEST_SOURCE = 'e6d9a99782cdf7fe7f69efef67d673259dd71c1d'
APK_SHA = '71439a6bb6b07aab12c39c101d0a4dec0d11db8a3573a62667fae88d030e6740'


def run(*args, check=True, timeout=30):
    return subprocess.run(args, capture_output=True, check=check, timeout=timeout)


def single(root, glob):
    found = list(root.rglob(glob))
    assert len(found) == 1, 'EXACT_ARTIFACT_FILE_REQUIRED:' + glob
    return found[0]


def observe(built, support, out):
    out.mkdir(parents=True, exist_ok=False)
    assert run('adb', 'shell', 'getprop', 'ro.kernel.qemu').stdout.strip() == b'1'
    # The old test APK is transport only: the invoked test and runner are byte-identical.
    paths = ['app/src/androidTest/java/com/ethk3/novaonepro/' + x + '.java'
             for x in ['R31QDeliveryInstrumentedTest', 'R30FaultAndroidRunner']]
    run('git', 'diff', '--exit-code', TEST_SOURCE, SOURCE, '--', *paths)
    apk = single(built, '*-release.apk')
    test = single(support, '*-androidTest.apk')
    assert hashlib.sha256(apk.read_bytes()).hexdigest() == APK_SHA
    assert single(built, 'source-commit.txt').read_text().strip() == SOURCE
    run('adb', 'install', '-r', '-g', str(apk), timeout=90)
    run('adb', 'install', '-r', '-t', str(test), timeout=90)
    installed = run('adb', 'shell', 'pm', 'path', PACKAGE).stdout.decode().strip().split(':', 1)[1]
    assert hashlib.sha256(run('adb', 'exec-out', 'cat', installed, timeout=90).stdout).hexdigest() == APK_SHA
    run('adb', 'shell', 'pm', 'grant', PACKAGE, 'android.permission.POST_NOTIFICATIONS', check=False)
    run('adb', 'shell', 'am', 'force-stop', PACKAGE)
    result = {'sourceCommit': SOURCE, 'apkSha256': APK_SHA, 'supportTestSource': TEST_SOURCE,
              'supportTestAndRunnerSourceUnchanged': True, 'scope': 'EXACT_RELEASE_SERVICE_PHASE_AND_PERSISTENT_ERROR_DIAGNOSIS_NOT_ACCEPTANCE',
              'physicalDevice': 'NOT_RUN', '24h': 'NOT_RUN', '72h': 'NOT_RUN', 'naturalQuality': 'NOT_PROVEN'}
    with tempfile.TemporaryDirectory(prefix='r31-private-live-') as private:
        private = Path(private)
        log = private / 'instrumentation.txt'
        command = ['adb', 'shell', 'am', 'instrument', '-w', '-r', '-e', 'r30Sandbox', 'r31-current-live',
                   '-e', 'r31qRealService', 'true', '-e', 'class',
                   'com.ethk3.novaonepro.R31QDeliveryInstrumentedTest#liveServiceFetchesFreezesAndSettlesNewFutureObservations',
                   PACKAGE + '.test/com.ethk3.novaonepro.R30FaultAndroidRunner']
        with log.open('wb') as stream:
            process = subprocess.Popen(command, stdout=stream, stderr=subprocess.STDOUT)
            start = time.monotonic(); samples = 0
            while process.poll() is None and time.monotonic() - start < 300:
                # First two minutes are uninstrumented; later stacks only diagnose failures.
                elapsed = time.monotonic() - start
                if elapsed >= 120 + samples * 7 and samples < 24:
                    pid = run('adb', 'shell', 'pidof', PACKAGE, check=False).stdout.decode().strip()
                    if re.fullmatch(r'[0-9]+', pid):
                        run('adb', 'shell', 'kill', '-3', pid, check=False)
                    samples += 1
                time.sleep(1)
            if process.poll() is None:
                process.kill();process.wait();result['diagnosticObservationWindowClosed'] = True
        text = log.read_text(errors='replace')
        result['processExit'] = process.returncode
        result['testPassed'] = bool(re.search(r'OK \(1 tests?\)', text) and 'FAILURES!!!' not in text)
        result['protocolOutputSha256'] = hashlib.sha256(log.read_bytes()).hexdigest()
        result['publicFailures'] = sorted(set(re.findall(r'\b(?:NO_FRESH_API_FETCH|NO_NEW_FUTURE_FREEZE|NO_NEW_FROZEN_SETTLEMENT|LEGACY_NOT_READY|R31_PHASE_BUDGET_[A-Z]+)\b', text)))
        result['phaseSamples'] = [list(x) for x in re.findall(r'R31Q_PHASE_COST\|([A-Z0-9_]+)\|elapsedMs=([0-9]+)\|budgetMs=([0-9]+)\|(OK|OVER_BUDGET|FAILED)', text)]
        run('adb', 'shell', 'am', 'force-stop', PACKAGE)
        remote = '/data/user/0/' + PACKAGE + '/databases/binance_k3_nova_one_pro_v57.db'
        local = private / 'live.db'
        for suffix in ['', '-wal', '-shm']:
            run('adb', 'pull', remote + suffix, str(local) + suffix, check=False)
        if local.is_file():
            with sqlite3.connect(local) as db:
                result['databaseIntegrity'] = db.execute('PRAGMA integrity_check').fetchone()[0]
                queries = {
                    'requests': ('v57_analysis_request_r36', ['target_issue', 'base_issue', 'status', 'attempts', 'last_stage', 'error_code', 'first_error_code', 'request_at', 'deadline_at', 'last_transition_at']),
                    'frozenAndSettled': ('v57_shadow_observation_r35', ['target_issue', 'pre_draw_frozen', 'commit_at', 'deadline_at', 'settled', 'settled_at', 'history_hash', 'snapshot_hash']),
                    'events': ('v57_runtime_event_r926', ['target_issue', 'event_type', 'stage', 'source', 'created_at']),
                    'resources': ('v57_attempt_resource_r927', ['target_issue', 'status', 'elapsed_ms', 'lock_wait_max_ms', 'db_body_max_ms', 'gc_count', 'gc_time_ms', 'stage_samples', 'reason']),
                    'draws': ('draws', ['issue', 'open_time', 'fetch_time', 'dirty']),
                    'settlementTasks': ('v57_settlement_task_r36', ['target_issue', 'status', 'attempts', 'error_code', 'first_error_code', 'last_stage', 'updated_at']),
                }
                for name, (table, allowed) in queries.items():
                    columns = {row[1] for row in db.execute('PRAGMA table_info(' + table + ')')}
                    selected = [c for c in allowed if c in columns]
                    if selected:
                        rows = db.execute('SELECT ' + ','.join(selected) + ' FROM ' + table + ' ORDER BY rowid DESC LIMIT 12').fetchall()
                        result[name] = [dict(zip(selected, row)) for row in rows]
                    else:
                        result[name] = {'state': 'SCHEMA_UNAVAILABLE'}
        run('adb', 'pull', '/data/anr', str(private / 'anr'), check=False, timeout=90)
        frames = {}
        for path in (private / 'anr').rglob('*'):
            if path.is_file():
                stack = path.read_text(errors='replace')
                selected = re.findall(r'at (com\.ethk3\.novaonepro\.[A-Za-z0-9_.$]+\([^\r\n]*\))', stack)
                for frame in selected:
                    if any(k in frame for k in ['V51', 'SelectedHit', 'Tianshu', 'Action', 'Canonical', 'Analysis', 'DatabaseCoordinator', 'LegacyDdl']):
                        frames[frame] = frames.get(frame, 0) + 1
        result['sampledSourceFrames'] = sorted(frames.items(), key=lambda row: (-row[1], row[0]))[:100]
        result['stackSamplesRequested'] = samples
    serialized = json.dumps(result, ensure_ascii=False, indent=2)
    serialized = re.sub(r'(?i)((?:api[_-]?key|authorization|password|token|secret)[\\" ]*[:=][\\" ]*)[^\\" ,;&]+', r'\1[REDACTED]', serialized)
    serialized = re.sub(r'(?i)Bearer\s+[^\\" ,;]+', 'Bearer [REDACTED]', serialized)
    (out / 'LIVE_PROFILE.json').write_text(serialized + '\n')
    print(json.dumps(json.loads(serialized), ensure_ascii=False), flush=True)
    # A completed diagnostic is not converted to a healthy service verdict.
    return 0 if result['testPassed'] else 1


if __name__ == '__main__':
    p = argparse.ArgumentParser();p.add_argument('--built', type=Path, required=True)
    p.add_argument('--support', type=Path, required=True);p.add_argument('--out', type=Path, required=True)
    a = p.parse_args();raise SystemExit(observe(a.built, a.support, a.out))
