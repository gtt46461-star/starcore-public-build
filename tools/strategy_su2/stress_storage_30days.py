#!/usr/bin/env python3
"""Accelerated host check of production storage SQL; synthetic labels are not prediction evidence."""
from pathlib import Path
import hashlib
import json
import sqlite3
import sys
import time

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'scripts'))
from generate_strategy_su2_schema import statements
from test_plan1025_storage import fold,fold_daily


def main():
    out=ROOT/'build/plan1025-storage-stress';out.mkdir(parents=True,exist_ok=True)
    path=out/'synthetic.sqlite'
    if path.exists():raise SystemExit('Use a fresh build/plan1025-storage-stress directory')
    began=time.monotonic();keys=[hashlib.sha256(('synthetic-'+str(i)).encode()).hexdigest() for i in range(32)]
    totals=[0,0,0];max_db=max_wal=0;days=[]
    with sqlite3.connect(path) as db:
        db.execute('PRAGMA auto_vacuum=INCREMENTAL');db.execute('PRAGMA journal_mode=WAL')
        for sql in statements():db.execute(sql)
        for key in keys:db.execute("INSERT INTO su2_model_genome(genome_key,canonical_text,formula,tier,origin_op,born_campaign,born_sequence,created_at) VALUES(?,?,'synthetic storage test','F15','seed','STORAGE_ONLY',0,0)",(key,'SU2|SYNTHETIC_STORAGE_ONLY'))
        # Worst-case split checkpoints: 48 independent 1 MiB rows, below the per-row 16 MiB constraint.
        blob=bytes(range(256))*4096
        for i in range(48):db.execute('INSERT INTO su2_live_state VALUES(?,0,?,?,0)',('CAL:synthetic-'+str(i),blob,len(blob)))
        db.commit()
        insert='INSERT INTO su2_live_decision(genome_key,sequence_ordinal,position,number_mask,act,gscore,phat,frozen_at,target_open_time,hit,settled_at) VALUES(?,?,1,7,?,.5,.3,1,2,?,3)'
        for day in range(30):
            rows=[]
            for seq in range(day*1440,(day+1)*1440):
                for j,key in enumerate(keys):
                    act=int((seq+j)%20<3);hit=int((seq+3*j)%10<3)
                    rows.append((key,seq,act,hit));totals[0]+=1;totals[1]+=act;totals[2]+=act*hit
            db.executemany(insert,rows);seq=(day+1)*1440
            fold(db,seq);cut=seq-10080
            batches=0
            while db.execute('SELECT 1 FROM su2_live_decision WHERE hit IS NOT NULL AND sequence_ordinal<? LIMIT 1',(cut,)).fetchone():
                fold_daily(db,cut);batches+=1
                assert batches<=1440,'retention stopped making progress'
            db.commit();db.execute('PRAGMA wal_checkpoint(TRUNCATE)');db.execute('PRAGMA incremental_vacuum(2000)');db.commit()
            raw=db.execute('SELECT count(*),coalesce(sum(act),0),coalesce(sum(CASE WHEN act=1 THEN hit ELSE 0 END),0) FROM su2_live_decision').fetchone()
            daily=db.execute('SELECT coalesce(sum(periods),0),coalesce(sum(actions),0),coalesce(sum(hits),0) FROM su2_live_daily').fetchone()
            assert [raw[i]+daily[i] for i in range(3)]==totals,(day,raw,daily,totals)
            physical=path.stat().st_size;wal=Path(str(path)+'-wal');wal_bytes=wal.stat().st_size if wal.exists() else 0
            max_db=max(max_db,physical);max_wal=max(max_wal,wal_bytes)
            assert physical<=384*1024**2 and wal_bytes<=64*1024**2
            days.append({'day':day+1,'raw_rows':raw[0],'daily_rows':db.execute('SELECT count(*) FROM su2_live_daily').fetchone()[0],'database_bytes':physical,'wal_bytes_after_checkpoint':wal_bytes,'totals_exact':True})
            print('virtual day',day+1,'raw',raw[0],'database MiB',round(physical/1024**2,2),flush=True)
        assert db.execute('SELECT count(*) FROM su2_live_decision').fetchone()[0]<=10080*32
        assert db.execute('SELECT count(*) FROM su2_track_block').fetchone()[0]<=41*32
        assert db.execute('SELECT sum(periods) FROM su2_track_block').fetchone()[0]==(43200//500*500)*32
    report={'status':'PASS','scope':'Actual production DDL/retention SQL on host SQLite; excludes full Android database, physical-device endurance and report filesystem','synthetic_assigned_labels_not_prediction_evidence':True,'virtual_days':30,'strategies':32,'split_calibration_checkpoints':48,'checkpoint_bytes_each':1048576,'periods_actions_hits':totals,'maximum_database_bytes':max_db,'maximum_wal_bytes_after_checkpoint':max_wal,'seconds':round(time.monotonic()-began,2),'sqlite_version':sqlite3.sqlite_version,'days':days}
    (out/'verification.json').write_text(json.dumps(report,indent=2)+'\n')
    print('HOST_30_DAY_STORAGE_PASS',flush=True)


if __name__=='__main__':main()
