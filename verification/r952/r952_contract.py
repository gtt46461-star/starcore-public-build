#!/usr/bin/env python3
from __future__ import annotations
import hashlib,importlib.util,json,re,sqlite3,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def require(v:bool,code:str)->None:
    if not v:raise AssertionError(code)
def source_contract()->None:
    settlement=(ROOT/"app/src/main/java/com/ethk3/novaonepro/SettlementCoordinatorR815.java").read_text()
    predraw=(ROOT/"app/src/main/java/com/ethk3/novaonepro/ResearchPredrawCaptureR923.java").read_text()
    director=(ROOT/"app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java").read_text()
    require(settlement.count("ResearchSettlementHookRegistryR952.dispatchCompleted")==1,"UNIQUE_SETTLEMENT_HOOK")
    require(predraw.count("ResearchDirectorR952.capturePredraw")==1,"REAL_PREDRAW_PATH")
    require("new SettlementEngine" not in "\n".join(p.read_text() for p in (ROOT/"app/src/main/java/com/ethk3/novaonepro/r952").glob("*.java")),"SECOND_SETTLEMENT_OWNER")
    require("may_create_real integer not null check(may_create_real=0)" in (ROOT/"app/src/main/java/com/ethk3/novaonepro/r952/ResearchMigrationRegistryR952.java").read_text().lower(),"REAL_DB_GUARD")
    require("future_n=future_n+1" in director and "settled=0" in director,"FUTURE_EXACTLY_ONCE")
    diagnostic=(ROOT/"app/src/main/java/com/ethk3/novaonepro/r952/FullbloodDiagnosticsProviderR952.java").read_text()
    require('"UNKNOWN"' in diagnostic and "SCHEMA_OR_QUERY_UNAVAILABLE" in diagnostic,"DIAGNOSTIC_UNKNOWN_NOT_ZERO")
    continuity=(ROOT/"app/src/main/java/com/ethk3/novaonepro/ResearchLearningContinuityR912.java").read_text()
    require(continuity.count("CertificateLifecycleProjectionR957.maintain(store)")==1,"R957_CERTIFICATE_PROJECTION_REACHABLE")
def sqlite_contract()->None:
    db=sqlite3.connect(":memory:");db.executescript("""
    CREATE TABLE predraw(issue TEXT,candidate TEXT,settled INTEGER DEFAULT 0,future_n INTEGER DEFAULT 0,PRIMARY KEY(issue,candidate));
    CREATE TABLE outbox(delivery TEXT PRIMARY KEY,state TEXT NOT NULL,attempt INTEGER DEFAULT 0);
    INSERT INTO predraw VALUES('2','c',0,0);
    """)
    delivery=hashlib.sha256(b"TRAIN|2|c|settlement|label").hexdigest()
    def settle():
        changed=db.execute("UPDATE predraw SET settled=1,future_n=future_n+1 WHERE issue='2' AND candidate='c' AND settled=0").rowcount
        if changed:db.execute("INSERT OR IGNORE INTO outbox VALUES(?, 'PENDING',0)",(delivery,))
    settle();settle();require(db.execute("SELECT future_n FROM predraw").fetchone()[0]==1,"DUPLICATE_FUTURE")
    require(db.execute("SELECT COUNT(*) FROM outbox").fetchone()[0]==1,"DUPLICATE_OUTBOX")
    require(db.execute("UPDATE outbox SET state='RUNNING',attempt=attempt+1 WHERE delivery=? AND state='PENDING'",(delivery,)).rowcount==1,"CLAIM")
    require(db.execute("UPDATE outbox SET state='COMPLETED' WHERE delivery=? AND state='RUNNING'",(delivery,)).rowcount==1,"COMPLETE")
    require(db.execute("UPDATE outbox SET state='RUNNING' WHERE delivery=? AND state='PENDING'",(delivery,)).rowcount==0,"DUPLICATE_TRAINING")
def negative_contract()->None:
    text="\n".join(p.read_text(encoding="utf-8",errors="ignore") for p in (ROOT/"app/src/main/java/com/ethk3/novaonepro/r952").glob("*.java"))
    for forbidden in ("dynamicK","martingale","doubling","frequency-debt"):
        require(forbidden.lower() not in text.lower(),"FORBIDDEN_"+forbidden)
def load_module(name:str,path:Path):
    spec=importlib.util.spec_from_file_location(name,path);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module
def training_contract()->None:
    pipeline=load_module("r952_pipeline",ROOT/"training/r925/pipeline.py")
    rows=[{"max_source_issue":f"{i:04d}","target_issue":f"{i+1:04d}","ordinal":"0"} for i in range(1,26)]
    pipeline.validate(rows);parts=pipeline.split(rows);require(tuple(parts)==pipeline.STAGES,"SPLIT_STAGES")
    bad=[dict(rows[0])];bad[0]["max_source_issue"]=bad[0]["target_issue"]
    try:pipeline.validate(bad);raise AssertionError("PIT_NEGATIVE_NOT_REJECTED")
    except ValueError:pass
    ftrl=load_module("r952_ftrl",ROOT/"training/v51_training_core.py")
    one=ftrl.update([0.0,0.0],[1.0,.5],1);two=ftrl.update([0.0,0.0],[1.0,.5],1)
    require(one["artifact_hash"]==two["artifact_hash"],"TRAINING_NONDETERMINISTIC")
def main()->None:
    source_contract();sqlite_contract();negative_contract();training_contract()
    subprocess.run(["python3",str(ROOT/"scripts/r952_frozen_file_guard.py")],check=True)
    print(json.dumps({"status":"PASS","suite":"R952_CONTRACT","tests":20}))
if __name__=="__main__":main()
