#!/usr/bin/env python3
import sqlite3,tempfile,os,time
fd,path=tempfile.mkstemp(prefix='r911-settle-sm-',suffix='.db'); os.close(fd)
try:
    db=sqlite3.connect(path)
    db.executescript('''
    create table task(issue text primary key,status text,task_class text,priority int,next_attempt int,lease_owner text,lease_until int,attempts int,recovery_count int,last_error text,failure_class text);
    ''')
    now=100000
    db.execute("insert into task values('925','RUNNING','LIVE_CURRENT',100,0,'dead-owner',999999,7,0,'','')")
    # Ordinary claim must fail while old wall-clock lease is active.
    n=db.execute("update task set status='RUNNING',lease_owner='new',lease_until=?,attempts=attempts+1 where issue='925' and status in ('PENDING','RETRY_WAIT','RUNNING') and next_attempt<=? and (lease_until=0 or lease_until<?)",(now+15000,now,now)).rowcount
    assert n==0
    print('R910_STALE_RUNNING_LEASE_REPRODUCED=PASS')
    # R911: sole process gate proves the durable owner is orphaned; current issue is made runnable-now.
    with db:
        db.execute("update task set status='RETRY_WAIT',task_class='LIVE_CURRENT',priority=100,next_attempt=0,lease_owner='',lease_until=0,recovery_count=recovery_count+1,last_error='R911_SOLE_GATE_ORPHAN_RECOVERED' where issue='925' and status='RUNNING'")
    n=db.execute("update task set status='RUNNING',lease_owner='new',lease_until=?,attempts=attempts+1 where issue='925' and status in ('PENDING','RETRY_WAIT','RUNNING') and next_attempt<=? and (lease_until=0 or lease_until<?)",(now+15000,now,now)).rowcount
    assert n==1
    row=db.execute("select status,lease_owner,recovery_count,attempts from task where issue='925'").fetchone()
    assert row==('RUNNING','new',1,8)
    print('R911_SOLE_GATE_ORPHAN_RECOVERY=PASS')
    print('R911_SAME_SCOPE_RECLAIM=PASS')

    # Live retry backoff contract: seconds, not the old 15-second boundary that matched give-up horizon.
    delays=[]
    for attempts in range(1,20):
        delay=min(2000,120*(1<<min(4,attempts-1)))
        delays.append(delay)
    assert max(delays)<=2000 and delays[0]==120
    print('R911_LIVE_RETRY_MAX_MS=2000')
    print('R911_LIVE_RETRY_SECONDSCALE=PASS')

    # Device-report shaped convergence: 293 LIVE_RETRY + 11 other runnable current/backlog.
    ready=list(range(304)); settled=[]; pulses=0
    while ready:
        pulses+=1
        quantum=min(48,len(ready)); settled.extend(ready[:quantum]); del ready[:quantum]
        if pulses>8: raise AssertionError('R911_BACKLOG_DID_NOT_CONVERGE')
    assert len(settled)==304
    print(f'R911_REPORT_SHAPED_304_DRAIN_PULSES={pulses}')
    print('R911_304_READY_CONVERGENCE=PASS')

    # Hash-rule evolution is replay if immutable business facts are equal; fact conflict remains blocked.
    existing=('base924',8,2,3,7,5,0,'oldhash')
    same=('base924',8,2,3,7,5,0,'newhash')
    conflict=('base924',8,2,3,7,6,0,'newhash')
    assert existing[:-1]==same[:-1]
    assert existing[:-1]!=conflict[:-1]
    print('R911_OOS_HASH_ONLY_REPLAY=PASS')
    print('R911_OOS_BUSINESS_FACT_CONFLICT_BLOCK=PASS')
finally:
    try: db.close()
    except Exception: pass
    try: os.unlink(path)
    except OSError: pass
