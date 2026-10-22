"""Run actual production SQL with Android-compatible string parameter binding."""
import json
import pathlib
import re
import sqlite3

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = ROOT / 'app/src/main/java/com/ethk3/novaonepro'
NAMES = {'DbHelper.TABLE_R36_SETTLEMENT_TASK': 'tasks',
         'DbHelper.TABLE_R35_SHADOW_OBSERVATION': 'obs',
         'DrawDependencyInputCP12.SOURCE': 'draws',
         'DbHelper.TABLE_R906_RESEARCH_EVAL_COVERAGE': 'coverage',
         'classes': "task_class='BACKLOG'"}

def expression(fragment):
    tokens = re.findall(r'"((?:[^"\\]|\\.)*)"|\b(DbHelper\.\w+|DrawDependencyInputCP12\.SOURCE|classes)\b', fragment)
    return ''.join(NAMES[b] if b else json.loads('"' + a + '"') for a, b in tokens)

def method(source, name):
    start = re.search(r'(?:public|private) static \w+ ' + name + r'\(', source).start()
    start = source.index('{', start)
    # All selected methods end before the next declaration at this indentation.
    return source[start:source.index('\n    }', start)]

db = sqlite3.connect(':memory:')
db.executescript('''CREATE TABLE tasks(target_issue TEXT,status TEXT,lease_until INTEGER,
 next_attempt_at INTEGER,priority INTEGER,created_at INTEGER,task_class TEXT,draw_hash TEXT);
 CREATE TABLE obs(target_issue TEXT,pre_draw_frozen INTEGER,settled INTEGER);
 CREATE TABLE draws(issue TEXT,dirty INTEGER);
 INSERT INTO obs VALUES('202610110001',1,0);
 INSERT INTO draws VALUES('202610110001',0);
 CREATE TABLE coverage(asset_id TEXT,drift_score REAL,ece REAL,brier REAL);
 CREATE TABLE cert(asset_id TEXT,status TEXT);
''')
checks = []
source = (JAVA / 'SettlementTaskStoreR36.java').read_text()
coord = (JAVA / 'SettlementCoordinatorR815.java').read_text()
for src, name in [(source, 'nextRunnable'), (coord, 'nextRunnablePredrawBacklogR901'),
                  (coord, 'nextRunnableReadyPredrawR903'), (coord, 'nextRunnable')]:
    body = method(src, name)
    fragment = body[body.index('"SELECT'):body.index('new String[]')]
    sql = expression(fragment)
    for status in ('PENDING', 'RETRY_WAIT', 'RUNNING'):
        for instant in (999, 1000, 1001, None):
            db.execute('DELETE FROM tasks')
            db.execute('INSERT INTO tasks VALUES(?,?,?,?,1,1,?,?)',
                       ('202610110001', status, instant, instant, 'BACKLOG', 'hash'))
            expected = instant is None or (instant < 1000 if status == 'RUNNING' else instant <= 1000)
            actual = bool(db.execute(sql, ('1000', '1000')).fetchone())
            assert actual == expected, (name, status, instant, actual, expected)
            checks.append([name, status, instant, actual])

body = method(source, 'terminalizeAuditedNoPredrawBacklog')
sql = expression(body[body.index('"SELECT'):body.index('new String[]')])
db.execute('DELETE FROM obs')
for status in ('PENDING', 'RETRY_WAIT', 'RUNNING'):
    for instant in (999, 1000, 1001, None):
        db.execute('DELETE FROM tasks')
        db.execute('INSERT INTO tasks VALUES(?,?,?,?,1,1,?,?)',
                   ('202610110001', status, instant, instant, 'BACKLOG', 'hash'))
        actual = bool(db.execute(sql, ('1000', '500')).fetchone())
        assert actual == (status != 'RUNNING' or instant is None or instant < 1000)
        checks.append(['terminalize', status, instant, actual])

cert = method((JAVA / 'ResearchCertificateR906.java').read_text(), 'revokeDriftedWithin')
where = expression(cert[cert.index('"status='):cert.index('new String[]')])
for values, expected in [((0, 0, 0), False), ((.12, .10, .30), False),
                         ((.121, 0, 0), True), ((0, .101, 0), True),
                         ((0, 0, .301), True), ((None, None, None), True)]:
    db.execute('DELETE FROM coverage'); db.execute('DELETE FROM cert')
    db.execute('INSERT INTO coverage VALUES(?,?,?,?)', ('a', *values))
    db.execute("INSERT INTO cert VALUES('a','ACTIVE_SHADOW')")
    actual = bool(db.execute('SELECT asset_id FROM cert WHERE ' + where, ('.12', '.10', '.30')).fetchone())
    assert actual == expected, (values, actual)
    checks.append(['certificate', values, actual])

payload = (JAVA / 'DrawRawPayloadCompactorR817.java').read_text()
predicate = re.search(r'WHERE (length\(COALESCE\(raw_json,\'\'\)\)>[^"\n]+)', payload).group(1)
db.execute('CREATE TABLE payload(raw_json TEXT)')
for size in (0, 8191, 8192, 8193):
    db.execute('DELETE FROM payload');db.execute('INSERT INTO payload VALUES(?)', ('x' * size,))
    actual = bool(db.execute('SELECT 1 FROM payload WHERE ' + predicate, ('8192',)).fetchone())
    assert actual == (size > 8192)
    checks.append(['payload', size, actual])
print(json.dumps({'result': 'PASS', 'count': len(checks), 'scope': 'PRODUCTION_SQL_HOST_SQLITE_NOT_ANDROID',
                  'checks': checks}, indent=2))
