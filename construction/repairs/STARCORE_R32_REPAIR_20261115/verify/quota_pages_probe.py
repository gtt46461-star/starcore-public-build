"""Exercise the exact SQLite admission and accounting DDL and keyset SELECTs emitted by product Java."""
import json
import sqlite3
import sys
from pathlib import Path


def check(condition, label):
    if not condition:
        raise AssertionError(label)
    print("PASS", label)


def binds(last):
    return [last[0]] + [v for length in range(1,len(last)+1) for v in last[:length]]


def main(ddl, queries):
    db = sqlite3.connect(":memory:")
    db.executescript(";\n".join(ddl) + ";")
    db.execute("INSERT INTO r31_hypothesis_quota VALUES(1,0)")
    # Populate through the original production triggers, then simulate an old DB with only its quota row missing.
    for i in range(131):
        campaign = "轮" + str(i // 44)
        child = "p" + str(i)
        payload = "原始凭据。" + str(i)
        db.execute("INSERT INTO r31_hypothesis_registry VALUES(?,?,?,?,?,?,?,?)", (child,"p","run","F","OPEN",payload,"digest",i))
        db.execute("INSERT INTO r31_hypothesis_binding VALUES(?,?,?,?,?,?,?,?,?)", (campaign,child,child,"ctrl","snap",0,131,"ord",130))
        db.execute("INSERT INTO r31_hypothesis_select VALUES(?,?,?,?,?,?,?,?,?,?,?)", (campaign,"p"+str(i//17),i,i,"snap",1,7,1,i%2,.4,"trace"))
        db.execute("INSERT INTO r31_hypothesis_outcome VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)", (campaign,child,child,"F","SELECT","DONE",i,.1,.01,.09,payload,"digest",i))
    original=db.execute("SELECT logical_bytes FROM r31_hypothesis_quota").fetchone()[0]
    db.execute("DELETE FROM r31_hypothesis_quota")
    db.commit()
    check(original>131*4*1024,"UTF-8 payload bytes included in original trigger charges")
    try:
        db.execute("INSERT INTO r31_hypothesis_registry VALUES('late','p','s','f','OPEN','{}','d',1)")
    except sqlite3.IntegrityError as exc:
        check("PROTECTED_STORAGE_WAIT" in str(exc),"business inserts refused throughout missing-quota scan")
    else:
        raise AssertionError("interleaved business insert admitted")
    db.rollback()
    all_costs=0
    for stage,q in enumerate(queries):
        last=None
        pages=[]
        keys=set()
        while True:
            args=binds(last) if last else []
            query=q["resume"] if last else q["first"]
            rows=db.execute(query,args).fetchall()
            if not rows:
                break
            replay=db.execute(query,args).fetchall()
            check(rows==replay,"same committed prefix resumes exactly: table %d, page %d"%(stage,len(pages)+1))
            pages.append(len(rows))
            for row in rows:
                key=row[:-1]
                if key in keys:
                    raise AssertionError("duplicate key on resume")
                keys.add(key)
                all_costs+=row[-1]
            last=[str(v) for v in rows[-1][:-1]]
        check(pages==[64,64,3] and len(keys)==131,"all 131 originals covered once in bounded pages: table %d"%stage)
    check(all_costs==original,"reconstructed four-table accounting equals trigger ledger byte-for-byte")
    with db:
        db.execute("INSERT INTO r31_hypothesis_quota VALUES(1,?)",(all_costs,))
        db.execute("INSERT INTO r31_hypothesis_registry VALUES('late','p','s','f','OPEN','{}','d',1)")
        db.execute("INSERT INTO r31_hypothesis_registry VALUES('late','p','s','f','OPEN','{}','d',1)")
    check(db.execute("SELECT logical_bytes FROM r31_hypothesis_quota").fetchone()[0]==original+1026,"normal admission resumes and retry is not charged twice")


if __name__=="__main__":
    main(*(json.loads(Path(arg).read_text()) for arg in sys.argv[1:]))
