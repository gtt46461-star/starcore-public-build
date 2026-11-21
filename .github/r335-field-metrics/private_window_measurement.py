"""Read the post-window guest database on this runner only. Publishable projections are calculated separately."""
import hashlib
import importlib.util
import json
import os
import re
import shutil
import sqlite3
import subprocess
from pathlib import Path

root = Path(os.environ['RUNNER_TEMP'])
raw = root/'report-originals/window'
destination = root/'r335-private-post-window'
destination.mkdir(exist_ok=True)
source = Path('app-source').resolve()
def read(path):return json.loads(path.read_bytes())
def sha(path):
    with path.open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()
def adb(*args):
    return subprocess.check_output(['adb',*map(str,args)],timeout=75).strip()
def state(code,**fields):
    (destination/'measurement.json').write_text(json.dumps(dict(status=code,**fields),indent=2)+'\n')
def main():
    calendar=raw/'calendar.json';events=root/'upgrade-evidence/observations.jsonl'
    if not calendar.is_file() or not events.is_file():
        state('NO_REGISTERED_WINDOW');return
    if not any(json.loads(line).get('kind')=='official-end' for line in events.read_text().splitlines()):
        state('WINDOW_END_NOT_OBSERVED');return
    cfg=read(calendar);commit=cfg['sourceCommit'];apk=cfg['apkSha256']
    assert re.fullmatch('[a-f0-9]{40}',commit) and re.fullmatch('[a-f0-9]{64}',apk)
    assert subprocess.check_output(['git','-C',source,'rev-parse','HEAD']).decode().strip()==commit
    if (raw/'source-manifest.json').is_file() and (raw/'snapshot.sqlite').is_file():
        manifest=read(raw/'source-manifest.json')
        if manifest['sourceCommit']==commit and manifest['apkSha256']==apk and sha(raw/'snapshot.sqlite')==manifest['snapshotSha256']:
            state('NATIVE_MANIFEST_BOUND_SNAPSHOT_PRESENT');return
    assert adb('shell','getprop','ro.kernel.qemu')==b'1' and adb('get-serialno').startswith(b'emulator-')
    adb('root');adb('wait-for-device')
    assert adb('shell','id','-u')==b'0'
    pkg='com.starcore.research.r13'
    base=next(v[8:] for v in adb('shell','pm','path',pkg).decode().splitlines() if v.startswith('package:') and v.endswith('/base.apk'))
    assert adb('shell','sha256sum',base).decode().split()[0]==apk
    tool=source/'tools/r3251/verify_release_overlay.py'
    spec=importlib.util.spec_from_file_location('post_window_app_snapshot',tool)
    owner=importlib.util.module_from_spec(spec);spec.loader.exec_module(owner)
    acquired=destination/'unmodified-guest-originals'
    assert not acquired.exists()
    original=owner.snapshot(acquired)
    assert original[owner.DB]['present'] and (acquired/owner.DB).is_file()
    work=destination/'sqlite-read';shutil.copytree(acquired,work)
    snapshot=destination/'measurements.sqlite'
    with sqlite3.connect((work/owner.DB).as_uri()+'?mode=ro',uri=True) as db,sqlite3.connect(snapshot) as copy:
        assert db.execute('PRAGMA integrity_check').fetchone()[0]=='ok'
        db.backup(copy)
    with sqlite3.connect(snapshot.as_uri()+'?mode=ro&immutable=1',uri=True) as copy:
        assert copy.execute('PRAGMA integrity_check').fetchone()[0]=='ok'
        assert copy.execute("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='app_state'").fetchone()[0]==1
    assert adb('shell','sha256sum',base).decode().split()[0]==apk
    state('CONSISTENT_POST_WINDOW_DB',sourceCommit=commit,apkSha256=apk,calendarSha256=sha(calendar),snapshotSha256=sha(snapshot),bytes=snapshot.stat().st_size,
          acquisition='POST_END_FORCE_STOP_STABLE_COPIES_THEN_SQLITE_BACKUP',nativeReport=False,exportedDatabase=False)
if __name__=='__main__':main()
