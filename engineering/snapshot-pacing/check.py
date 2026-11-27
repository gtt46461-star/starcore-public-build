import hashlib
import json
from pathlib import Path
import sqlite3
import subprocess
import sys
import time
from contextlib import closing

work = Path(sys.argv[1])
before, after = map(Path, sys.argv[2:])
source = work / 'source.db'
with closing(sqlite3.connect(source)) as db:
    db.executescript("PRAGMA page_size=4096; PRAGMA journal_mode=OFF; PRAGMA synchronous=OFF; "
                     "CREATE TABLE archived(id INTEGER PRIMARY KEY, raw BLOB NOT NULL);")
    db.executemany("INSERT INTO archived(id,raw) VALUES(?,zeroblob(?))",
                   ((index, 4_996_000 + index % 512) for index in range(1000)))
    db.commit()
    pages = db.execute('PRAGMA page_count').fetchone()[0]
    assert pages > 1_200_000, pages

def digest(file):
    with file.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()

def run_copy(binary, dest):
    start = time.monotonic()
    result = subprocess.run([str(binary), 'copy', str(source), str(dest)],
                            capture_output=True, text=True, check=True, timeout=135)
    elapsed = time.monotonic() - start
    _, rc, checks, admissions, chunks = result.stdout.split()
    assert int(rc) == 0 and int(chunks) == (pages+99)//100
    assert int(checks) >= int(chunks) and int(admissions) == int(chunks)+1
    with closing(sqlite3.connect(f'{dest.as_uri()}?mode=ro', uri=True)) as db:
        assert db.execute('PRAGMA quick_check').fetchone() == ('ok',)
        assert db.execute('SELECT count(*),sum(length(raw)) FROM archived').fetchone() == (1000, 4996249644)
    sha = digest(dest)
    dest.unlink()
    return dict(seconds=round(elapsed, 3), sha256=sha, callbacks=int(chunks))

baseline = run_copy(before, work / 'original.db')
candidate = run_copy(after, work / 'repaired.db')
assert baseline['sha256'] == candidate['sha256'], (baseline, candidate)
print('NATIVE_AB', json.dumps(dict(pages=pages, original=baseline, repaired=candidate)), flush=True)

out = work / 'stopped.db'
cancel = subprocess.run([str(after), 'cancel', str(source), str(out)],
                        capture_output=True, text=True, check=True, timeout=20)
assert '-1001' in cancel.stdout and not out.exists() and not Path(str(out)+'-journal').exists(), cancel.stdout
print('CANCEL_AFTER_20_CHUNKS', cancel.stdout.strip(), flush=True)

live = work / 'live.db'
snapshot = work / 'wal_snapshot.db'
with closing(sqlite3.connect(live, isolation_level=None)) as writer:
    assert writer.execute('PRAGMA journal_mode=WAL').fetchone() == ('wal',)
    writer.execute('PRAGMA wal_autocheckpoint=0')
    writer.execute('CREATE TABLE live_commits(id INTEGER PRIMARY KEY, data BLOB NOT NULL)')
    writer.execute('BEGIN')
    writer.executemany('INSERT INTO live_commits VALUES(?,zeroblob(8192))', ((i,) for i in range(2400)))
    writer.execute('COMMIT')
    proc = subprocess.Popen([str(after), 'wal', str(live), str(snapshot)],
                            stdin=subprocess.PIPE, stdout=subprocess.PIPE, text=True)
    try:
        assert proc.stdout.readline().strip() == 'PINNED'
        writer.execute("INSERT INTO live_commits VALUES(9999,x'AB')")
        output, _ = proc.communicate('\n', timeout=35)
        assert proc.returncode == 0, output
        with closing(sqlite3.connect(f'{snapshot.as_uri()}?mode=ro', uri=True)) as db:
            assert db.execute('PRAGMA quick_check').fetchone() == ('ok',)
            assert db.execute('SELECT count(*) FROM live_commits').fetchone() == (2400,)
            assert db.execute('SELECT count(*) FROM live_commits WHERE id=9999').fetchone() == (0,)
        assert writer.execute('SELECT count(*) FROM live_commits').fetchone() == (2401,)
    finally:
        if proc.poll() is None:
            proc.kill()
            proc.wait()
print('CONCURRENT_WAL_COMMIT_PRESERVED_PINNED_READ', output.strip(), flush=True)
