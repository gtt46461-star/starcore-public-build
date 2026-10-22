#!/usr/bin/env python3
"""Executable model of the R909 invariant: derivative failures cannot roll back core settlement."""
import sqlite3, tempfile, os
fd,path=tempfile.mkstemp(prefix='r909-settle-',suffix='.db'); os.close(fd)
try:
    db=sqlite3.connect(path)
    db.executescript('''
    create table task(issue text primary key,status text,last_error text default '');
    create table shadow(issue text primary key,settled int,hit int);
    create table oos(issue text primary key,strict int,hit int);
    create table closure(issue text primary key,status text);
    create table outbox(id integer primary key autoincrement,issue text,event text,unique(issue,event));
    ''')
    # Production core commit.
    db.execute("insert into task values('100','RUNNING','')")
    db.execute("insert into shadow values('100',0,-1)")
    with db:
        db.execute("insert into oos values('100',1,1)")
        db.execute("update shadow set settled=1,hit=1 where issue='100'")
        db.execute("update task set status='COMPLETED' where issue='100'")
    # Intentionally destroy derivative schema and prove sidecar failure is isolated.
    db.execute('drop table closure'); db.commit()
    failed=False
    try:
        with db:
            db.execute("insert into closure values('100','COMPLETED')")
            db.execute("insert into outbox(issue,event) values('100','RESEARCH')")
    except sqlite3.Error:
        failed=True
    core=db.execute("select status from task where issue='100'").fetchone()[0]
    settled=db.execute("select settled from shadow where issue='100'").fetchone()[0]
    oos=db.execute("select strict from oos where issue='100'").fetchone()[0]
    assert failed and core=='COMPLETED' and settled==1 and oos==1
    print('SIDECAR_FAILURE_INJECTED=PASS')
    print('CORE_SURVIVES_SIDECAR_FAILURE=PASS')
    # Repair derivative schema and reconcile idempotently.
    db.execute('create table closure(issue text primary key,status text)')
    with db:
        db.execute("insert or replace into closure values('100','COMPLETED')")
        db.execute("insert or ignore into outbox(issue,event) values('100','RESEARCH')")
    with db:
        db.execute("insert or replace into closure values('100','COMPLETED')")
        db.execute("insert or ignore into outbox(issue,event) values('100','RESEARCH')")
    assert db.execute("select count(*) from closure where issue='100'").fetchone()[0]==1
    assert db.execute("select count(*) from outbox where issue='100'").fetchone()[0]==1
    print('SIDECAR_EVENTUAL_RECONCILE=PASS')
    print('SIDECAR_IDEMPOTENT=PASS')
finally:
    try: db.close()
    except Exception: pass
    try: os.unlink(path)
    except OSError: pass
