#!/usr/bin/env python3
"""Execute the production Java SQL to check lifetime folding and bounded report statistics."""
import json
from pathlib import Path
import re
import sqlite3
from generate_strategy_su2_schema import statements

ROOT = Path(__file__).resolve().parents[1]


def fold(db, sequence):
    source = (ROOT / 'app/src/main/java/com/ethk3/novaonepro/strategyv2/DiagnosisStoreSU2.java').read_text()
    body = source.split('static void foldTrackBlocks(', 1)[1].split('static void retention(', 1)[0]
    values = {'hi': sequence // 500 * 500, 'cut': sequence // 500 - 40, 'cut-1': sequence // 500 - 41}
    for match in re.finditer(r'db\.execSQL\((.*?)\);', body, re.S):
        call = match[1]
        sql_part = call.split(', new Object[]{', 1)[0]
        sql = ''.join(json.loads(s) for s in re.findall(r'"(?:\\.|[^"\\])*"', sql_part))
        args = re.search(r'new Object\[\]\{([^}]+)\}', call)
        params = [values[a.strip()] for a in args[1].split(',')] if args else []
        db.execute(sql, params)


def main():
    with sqlite3.connect(':memory:') as db:
        for statement in statements(): db.execute(statement)
        key='a'*64
        db.execute("INSERT INTO su2_model_genome(genome_key,canonical_text,formula,tier,origin_op,born_campaign,born_sequence,created_at) VALUES(?,?,'test','F15','seed','test',0,0)",(key,'SU2|12345678901234567890'))
        db.execute("INSERT INTO su2_league(member_id,kind,name,genome_key,joined_sequence) VALUES('SU2:test','SU2','test',?,0)",(key,))
        db.execute("INSERT INTO su2_mainline_choice VALUES(0,'SU2:test',0,0,'test',0,0)")
        insert='INSERT INTO su2_live_decision(genome_key,sequence_ordinal,position,number_mask,act,gscore,phat,frozen_at,target_open_time,hit,settled_at) VALUES(?,?,1,7,?,.5,.4,1,2,?,3)'
        db.executemany(insert,[(key,1,1,1),(key,2,0,0),(key,501,1,0)])
        fold(db,21000);fold(db,21000)
        expected=[(key,3,2,1),('MAINLINE',3,2,1)]
        assert db.execute('SELECT member_id,sum(periods),sum(actions),sum(hits) FROM su2_track_block GROUP BY member_id ORDER BY member_id DESC').fetchall()==expected
        db.execute('DELETE FROM su2_live_decision')
        fold(db,22000)
        assert db.execute('SELECT member_id,sum(periods),sum(actions),sum(hits) FROM su2_track_block GROUP BY member_id ORDER BY member_id DESC').fetchall()==expected
        # A later closed block adds once after old raw rows have been removed.
        db.execute(insert,(key,22001,1,1));fold(db,22500);fold(db,22500)
        assert db.execute('SELECT sum(periods),sum(actions),sum(hits) FROM su2_track_block WHERE member_id=?',(key,)).fetchone()==(4,3,2)
    print('P50 production SQL: exact lifetime totals, idempotent folding and raw-retention independence PASS')


if __name__=='__main__': main()
