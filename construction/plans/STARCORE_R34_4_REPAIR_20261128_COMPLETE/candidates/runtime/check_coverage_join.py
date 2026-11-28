#!/usr/bin/env python3
"""SQL-equivalence fixture only: not Android or device performance acceptance."""
import json
import random
import sqlite3

db = sqlite3.connect(':memory:')
db.executescript('''
CREATE TABLE event(event_id TEXT PRIMARY KEY, target_issue TEXT, game_code TEXT);
CREATE TABLE scope(event_id TEXT PRIMARY KEY, release_id TEXT, source_commit TEXT,
 database_identity TEXT, sample_wall_at INTEGER);
CREATE INDEX event_target ON event(target_issue, game_code);
CREATE INDEX r341_observation_release ON scope(release_id, source_commit, database_identity, sample_wall_at);
''')
rng = random.Random(343)
events, scopes = [], []
for i in range(50000):
    event_id = f'e{i:06}'
    issue = f'202611{rng.randrange(100000, 999999):06}'
    if i % 101 == 0: issue = 'malformed'
    if i % 103 == 0: issue = '20261100000X'
    if i % 107 == 0: issue = None
    events.append((event_id, issue, 'g' + str(i % 3)))
    if i % 97:
        scopes.append((event_id, 'r' + str(i % 11), 's' + str(i % 7), 'd' + str(i % 5), i))
db.executemany('INSERT INTO event VALUES(?,?,?)', events)
db.executemany('INSERT INTO scope VALUES(?,?,?,?,?)', scopes)
db.execute('INSERT INTO scope VALUES(?,?,?,?,?)', ('orphan', 'r0', 's0', 'd0', 1))
db.execute('ANALYZE')
suffix = '''WHERE s.release_id=? AND s.source_commit=? AND s.database_identity=? AND e.game_code=?
AND length(e.target_issue)=12 AND e.target_issue NOT GLOB '*[^0-9]*' '''
old = 'SELECT min(e.target_issue) FROM event e JOIN scope s ON s.event_id=e.event_id ' + suffix
new = 'SELECT min(e.target_issue) FROM scope s CROSS JOIN event e ON e.event_id=s.event_id ' + suffix
cases = []
for i in range(100):
    args = ('r' + str(rng.randrange(12)), 's' + str(rng.randrange(8)),
            'd' + str(rng.randrange(6)), 'g' + str(rng.randrange(4)))
    left, right = db.execute(old, args).fetchone(), db.execute(new, args).fetchone()
    assert left == right, (args, left, right)
    cases.append(args)
plan = [r[3] for r in db.execute('EXPLAIN QUERY PLAN ' + new, ('r0', 's0', 'd0', 'g0'))]
assert 'r341_observation_release' in plan[0], plan
assert 'event_id' in plan[1], plan
print(json.dumps({'scope': 'SYNTHETIC_SQL_EQUIVALENCE_ONLY', 'sqlite': sqlite3.sqlite_version,
    'cases': len(cases), 'rows': len(events), 'equivalent': True, 'candidateQueryPlan': plan,
    'androidCompiled': False, 'deviceMeasured': False}, ensure_ascii=False, indent=2))
