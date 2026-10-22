#!/usr/bin/env python3
"""Real SQLite query-work reproduction; numbers are VM instructions, not phone latency."""
import json,sqlite3,time
c=sqlite3.connect(':memory:')
c.executescript('CREATE TABLE live(id INTEGER PRIMARY KEY); CREATE TABLE research_row_revision_v2(generation INTEGER PRIMARY KEY AUTOINCREMENT,source_table TEXT NOT NULL,source_rowid INTEGER NOT NULL,UNIQUE(source_table,source_rowid));')
c.executemany('INSERT INTO live VALUES(?)',((i,) for i in range(1,1000001)))
c.execute("INSERT INTO research_row_revision_v2(source_table,source_rowid) SELECT 'live',id FROM live")
def measure(sql,args):
 count=[0]
 def tick():count[0]+=1000;return 0
 c.set_progress_handler(tick,1000);t=time.monotonic();rows=c.execute(sql,args).fetchall();elapsed=time.monotonic()-t;c.set_progress_handler(None,0)
 return {'vm_instructions_lower_bound':count[0],'elapsed_ms':round(elapsed*1000,3),'rows':len(rows)}
old=measure('DELETE FROM research_row_revision_v2 WHERE generation IN(SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table=? AND NOT EXISTS(SELECT 1 FROM live WHERE live.rowid=r.source_rowid) ORDER BY r.generation LIMIT 32)',('live',))
new=measure('SELECT r.source_rowid,r.generation,EXISTS(SELECT 1 FROM live WHERE live.rowid=r.source_rowid) FROM research_row_revision_v2 r WHERE r.source_table=? AND r.source_rowid>? ORDER BY r.source_rowid LIMIT 128',('live',str(-(1<<63))))
assert old['vm_instructions_lower_bound']>5000000 and new['vm_instructions_lower_bound']<10000
assert c.execute('SELECT COUNT(*) FROM research_row_revision_v2').fetchone()[0]==1000000
print(json.dumps({'scope':'REAL_SQLITE_SYNTHETIC_MILLION_LIVE_ROWS_NOT_DEVICE','old_limit_does_not_bound_scan':old,'new_indexed_keyset_page':new,'live_rows_preserved':1000000},indent=2))
