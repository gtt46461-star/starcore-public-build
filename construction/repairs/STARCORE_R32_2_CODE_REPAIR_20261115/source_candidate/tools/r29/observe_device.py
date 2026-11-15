#!/usr/bin/env python3
"""Read-only, bounded ADB observations for a chosen phone/emulator. Never installs, clears data, roots or signs.
This is an observation recorder, not automatic acceptance of business continuity or prediction quality.
Use the app's own complete diagnostic export to join draw/freeze/settlement/learning cursors afterwards.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time
import xml.etree.ElementTree as ET

PKG = 'com.starcore.research.r13'
ALLOWED = {'init_status', 'init_last_error', 'init_retry_count', 'heartbeat_at', 'service_ready_at', 'process_id', 'user_enabled'}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--serial', required=True)
    p.add_argument('--duration-seconds', type=int, required=True)
    p.add_argument('--interval-seconds', type=int, default=30)
    p.add_argument('--out', type=Path, required=True)
    a = p.parse_args()
    if not 1 <= a.duration_seconds <= 604800 or not 1 <= a.interval_seconds <= 300:
        p.error('duration must be 1..604800 seconds, interval 1..300 seconds')
    a.out.mkdir(parents=True, exist_ok=True)
    lock = a.out / 'observer.lock'
    fd = os.open(lock, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
    os.close(fd)

    def adb(*args):
        try:
            r = subprocess.run(['adb', '-s', a.serial, *args], capture_output=True, text=True, timeout=12)
            return r.stdout.strip() if r.returncode == 0 else None
        except (subprocess.TimeoutExpired, OSError):
            return None

    started = time.monotonic()
    counts = {'observations': 0, 'adbUnavailable': 0, 'processMissing': 0, 'heartbeatUnavailable': 0, 'heartbeatStale': 0}
    session = {'package': PKG, 'deviceIdHash': hashlib.sha256(a.serial.encode()).hexdigest()[:16],
               'scope': 'EMULATOR' if adb('shell', 'getprop', 'ro.kernel.qemu') == '1' else 'PHYSICAL_DEVICE',
               'model': adb('shell', 'getprop', 'ro.product.model'), 'requestedSeconds': a.duration_seconds,
               'startedAtUtcMs': int(time.time() * 1000), 'acceptance': 'OBSERVATION_ONLY_NOT_FULL_ACCEPTANCE'}
    interrupted = False
    try:
        with (a.out / 'observations.jsonl').open('a', encoding='utf-8') as out:
            out.write(json.dumps({'session': session}) + '\n')
            while time.monotonic() - started < a.duration_seconds:
                tick = time.monotonic()
                online = adb('get-state') == 'device'
                pid = adb('shell', 'pidof', PKG) if online else None
                raw = adb('exec-out', 'run-as', PKG, 'cat', 'shared_prefs/v20_service_runtime_light.xml') if online else None
                prefs = {}
                if raw:
                    try: prefs = {e.attrib['name']: e.text or e.attrib.get('value', '') for e in ET.fromstring(raw) if e.attrib.get('name') in ALLOWED}
                    except ET.ParseError: pass
                row = {'wallTimeUtcMs': int(time.time() * 1000), 'elapsedSeconds': time.monotonic() - started,
                       'adbOnline': online, 'pid': pid, 'runtime': prefs or None}
                counts['observations'] += 1
                if not online: counts['adbUnavailable'] += 1
                elif not pid: counts['processMissing'] += 1
                heartbeat = prefs.get('heartbeat_at', '')
                device_time = adb('shell', 'date', '+%s') if online else None
                if not heartbeat.isdigit() or not device_time or not device_time.isdigit(): counts['heartbeatUnavailable'] += 1
                elif int(device_time) * 1000 - int(heartbeat) > 15000: counts['heartbeatStale'] += 1
                # Only service-health fields are exported; API/signing configuration is never read.
                out.write(json.dumps(row, ensure_ascii=False) + '\n'); out.flush(); os.fsync(out.fileno())
                left = a.duration_seconds - (time.monotonic() - started)
                if left > 0: time.sleep(min(left, max(0, a.interval_seconds - (time.monotonic() - tick))))
    except KeyboardInterrupt:
        interrupted = True
    finally:
        summary = dict(session, elapsedSeconds=time.monotonic() - started, counts=counts, interrupted=interrupted,
                       businessCursorValidation='REQUIRES_APP_DIAGNOSTIC_EXPORT', naturalQuality='NOT_EVALUATED')
        (a.out / 'summary.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n')
        lock.unlink(missing_ok=True)
        print(json.dumps(summary, ensure_ascii=False))


if __name__ == '__main__':
    main()
