#!/usr/bin/env python3
"""R31-B12-C01#A1b/#A2 on a disposable Android emulator (EMU layer; never a physical-device claim).

Overlay-installs the built debug APK, kills the app process on a rooted disposable emulator at several
offsets during startup, relaunches, and records what is actually observed: the UI resumes, no crash, the database
passes integrity_check, the Critical stamp is READY and - only if the runtime owner ran - the R31 upgrade journal
phases are COMPLETED with attempt <= kills+1. A journal that never started (service not enabled by a user) is
reported NOT_REACHED, never PASS.
"""
import argparse, datetime as dt, hashlib, json, shutil, sqlite3, subprocess, sys, time
from pathlib import Path

PKG = 'com.starcore.research.r13'
ACTIVITY = PKG + '/com.ethk3.novaonepro.MainActivity'
DB = 'binance_k3_nova_one_pro_v57.db'
PHASES = ['PREPARE', 'VERIFY_SOURCE', 'TRANSFORM', 'VERIFY_TARGET', 'PUBLISH', 'CLEANUP']


def adb(*args, timeout=120, check=False):
    r = subprocess.run(['adb', *args], capture_output=True, text=True, timeout=timeout)
    if check and r.returncode:
        raise SystemExit('ADB_FAILED %s: %s' % (args, r.stdout + r.stderr))
    return r.stdout.strip()


def pid():
    return adb('shell', 'pidof', PKG)


def package_pids():
    text = adb('shell', 'ps', '-A', '-o', 'PID,ARGS')
    return [int(row.split()[0]) for row in text.splitlines()
            if len(row.split()) >= 2 and row.split()[0].isdigit()
            and (row.split()[1] == PKG or row.split()[1].startswith(PKG + ':'))]


def stop_before_copy():
    # Raw DB/WAL/SHM files must describe the same stopped writer generation.
    # A successful adb copy of three live files is not a SQLite snapshot.
    adb('shell', 'am', 'force-stop', PKG, check=True)
    deadline = time.monotonic() + 5
    while package_pids() and time.monotonic() < deadline:
        time.sleep(.05)
    remaining = package_pids()
    if remaining:
        raise RuntimeError('UPGRADE_CAPTURE_PACKAGE_PROCESS_STILL_ALIVE:' + str(remaining))
    return {'method': 'SYSTEM_FORCE_STOP_THEN_ALL_PACKAGE_PIDS_GONE',
            'remainingPids': remaining, 'rawFilesCopiedWhileWriterActive': False}


def pull_db(target):
    target.mkdir(parents=True, exist_ok=True)
    for suffix in ('', '-wal', '-shm'):
        raw = subprocess.run(['adb', 'exec-out', 'run-as', PKG, 'cat', 'databases/' + DB + suffix], capture_output=True, timeout=300)
        if raw.returncode == 0 and raw.stdout and not raw.stdout.startswith(b'cat:'):
            (target / (DB + suffix)).write_bytes(raw.stdout)
    return target / DB


def inspect(path):
    facts = {'pulled': path.is_file()}
    if not path.is_file():
        return facts
    with sqlite3.connect(path) as c:
        facts['integrity'] = c.execute('PRAGMA integrity_check').fetchone()[0]
        tables = {r[0] for r in c.execute("SELECT name FROM sqlite_master WHERE type='table'")}
        if 'v57_schema_domain_r926' in tables:
            facts['stamps'] = {d: s for d, s in c.execute('SELECT domain,state FROM v57_schema_domain_r926')}
        if 'v57_schema_migration_journal_r926' in tables:
            rows = c.execute("SELECT domain,step_id,status,attempt FROM v57_schema_migration_journal_r926 WHERE domain LIKE 'R31_UPGRADE:%'").fetchall()
            facts['upgradeJournal'] = {step: {'status': status, 'attempt': attempt} for _, step, status, attempt in rows}
        for t in ('draws', 'r27_certificate', 'r27_policy', 'r27_live_frozen'):
            if t in tables:
                facts.setdefault('rows', {})[t] = c.execute('SELECT count(*) FROM ' + t).fetchone()[0]
    return facts


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--apk', required=True); ap.add_argument('--out', required=True); ap.add_argument('--api', required=True)
    a = ap.parse_args()
    out = Path(a.out); out.mkdir(parents=True, exist_ok=True)
    if adb('shell', 'getprop', 'ro.kernel.qemu') != '1':
        raise SystemExit('DISPOSABLE_ANDROID_EMULATOR_REQUIRED_NO_PHYSICAL_DEVICE_CLAIM')
    if adb('shell', 'id', '-u') != '0':
        raise SystemExit('ROOTED_DISPOSABLE_EMULATOR_REQUIRED_FOR_CONFIRMED_PROCESS_KILLS')
    receipt = {'scope': 'EMU_OVERLAY_INSTALL_KILL_RECOVERY_NOT_DEVICE_NOT_NATURAL', 'api': a.api, 'startedAt': dt.datetime.now(dt.timezone.utc).isoformat(),
               'apkSha256': hashlib.sha256(Path(a.apk).read_bytes()).hexdigest(), 'kills': [], 'physicalDevice': 'NOT_RUN'}
    adb('logcat', '-c')
    receipt['firstInstall'] = adb('install', '-r', '-g', a.apk, timeout=600)
    adb('shell', 'am', 'start', '-W', '-n', ACTIVITY, timeout=120); time.sleep(8)
    receipt['overlayInstall'] = adb('install', '-r', '-g', a.apk, timeout=600)  # overlay install of the same build over its own data
    for delay in (0.3, 0.8, 1.5, 3.0, 6.0):
        adb('shell', 'am', 'start', '-n', ACTIVITY); time.sleep(delay); p = pid()
        if p and not all(value.isdigit() for value in p.split()):
            raise RuntimeError('UPGRADE_PID_NOT_NUMERIC')
        signal = subprocess.run(['adb','shell','kill','-9',*p.split()],capture_output=True,text=True,timeout=120) if p else None
        time.sleep(1.0)
        still = bool(set(p.split()) & set(pid().split()))
        receipt['kills'].append({'afterSeconds': delay, 'pid': p,
                                 'method': 'ROOTED_DISPOSABLE_EMULATOR_SIGKILL',
                                 'signalExitCode': None if signal is None else signal.returncode,
                                 'oldPidExitConfirmed': signal is not None and signal.returncode == 0 and not still,
                                 'stillAlive': still})
    adb('shell', 'am', 'start', '-W', '-n', ACTIVITY, timeout=120); time.sleep(20)
    resumed = adb('shell', 'dumpsys', 'activity', 'activities')
    receipt['uiResumed'] = PKG in '\n'.join(l for l in resumed.splitlines() if 'ResumedActivity' in l)
    receipt['processAlive'] = bool(pid())
    crash = adb('logcat', '-d', '-b', 'crash', '-v', 'threadtime')
    (out / 'crash.txt').write_text(crash); receipt['crashForPackage'] = PKG in crash
    receipt['capture'] = stop_before_copy()
    facts = inspect(pull_db(out / 'db')); receipt['database'] = facts
    journal = facts.get('upgradeJournal') or {}
    if not journal:
        receipt['upgradeJournal'] = 'NOT_REACHED: the runtime owner did not start (service not enabled on a fresh emulator); no journal claim'
    else:
        ok = all(journal.get(p, {}).get('status') == 'COMPLETED' for p in PHASES) and all(v['attempt'] <= len(receipt['kills']) + 1 for v in journal.values())
        receipt['upgradeJournal'] = 'COMPLETED_AFTER_KILLS' if ok else 'INCOMPLETE:' + json.dumps(journal)
    critical = (facts.get('stamps') or {}).get('CRITICAL')
    receipt['criticalStamp'] = critical or 'MISSING'
    observed = receipt['uiResumed'] and receipt['processAlive'] and not receipt['crashForPackage'] and facts.get('integrity') == 'ok'
    observed = observed and critical == 'READY' and all(row['oldPidExitConfirmed'] for row in receipt['kills'])
    receipt['status'] = 'OBSERVED_PASS' if observed and receipt['upgradeJournal'] == 'COMPLETED_AFTER_KILLS' else 'OBSERVED_FAIL'
    receipt['finishedAt'] = dt.datetime.now(dt.timezone.utc).isoformat()
    shutil.rmtree(out / 'db', ignore_errors=True)
    (out / 'EMU-UPGRADE-RECEIPT.json').write_text(json.dumps(receipt, indent=1, ensure_ascii=False) + '\n')
    print('R31_EMU_UPGRADE|' + json.dumps({k: receipt[k] for k in ('api', 'status', 'uiResumed', 'processAlive', 'crashForPackage', 'upgradeJournal', 'criticalStamp')}, ensure_ascii=False))
    sys.exit(0 if receipt['status'] == 'OBSERVED_PASS' else 1)


if __name__ == '__main__':
    main()
