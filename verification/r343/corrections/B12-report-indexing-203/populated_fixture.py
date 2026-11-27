"""Populate a disposable baseline SQLite with synthetic cold faults sized from the field manifest."""
import hashlib
import io
import json
import sqlite3
import struct
import tarfile
import zipfile
from pathlib import Path

CODEC="R33_FAULT_ROW_V1"
COLUMNS="event_id,task_id,owner_epoch,fence,operation,phase,category,root_fingerprint,minute_bucket,cause_chain_json,stack,stack_hash,last_cause_chain_json,last_stack,last_stack_hash,commit_outcome,boot_id,first_wall_ms,last_wall_ms,first_elapsed_ms,last_elapsed_ms,occurrences,byte_count".split(",")

def encoded(event,padding):
    out=io.BytesIO()
    out.write(CODEC.encode()+b"\0")
    for name in COLUMNS:
        key=name.encode()
        value=event.encode() if name=="event_id" else b"x"*padding if name=="stack" else None
        out.write(struct.pack(">H",len(key))+key)
        out.write((b"N" if value is None else b"T")+struct.pack(">Q",len(value or b""))+(value or b""))
    return out.getvalue()

def populate(database,field,tarpath):
    with zipfile.ZipFile(field) as outer,zipfile.ZipFile(io.BytesIO(outer.read(outer.namelist()[0]))) as original:
        lengths=[json.loads(line)["bytes"] for line in original.open("r335-fault-originals.jsonl")]
    assert len(lengths)==11396
    with sqlite3.connect(database) as db,tarfile.open(tarpath,"w:gz") as archive:
        for i,length in enumerate(lengths):
            event="field-index-fixture-%05d"%i
            body=encoded(event,max(0,length-len(encoded(event,0))))
            digest=hashlib.sha256(body).hexdigest()
            rel="r335-fault-originals/"+digest+".row"
            key="FIELD-INDEX:"+event
            db.execute("INSERT INTO r335_fault_original(event_id,original_hash,original_bytes,codec,relative_path,owner_epoch,task_id,category,last_wall_ms,receipt_key,release_hash,archived_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                       (event,digest,len(body),CODEC,rel,"fixture","fixture","RESOURCE_QUEUE",0,key,"fixture",0))
            db.execute("INSERT INTO r31_commit_receipt(commit_key,payload_hash,operation,task_id,owner_epoch,fence,input_snapshot,chunk_seq,operator_version,committed_at) VALUES (?,?,?,?,?,?,?,?,?,?)",
                       (key,digest if i!=1 else "0"*64,"fixture","fixture","fixture",0,digest,0,CODEC,0))
            if i!=2:
                item=tarfile.TarInfo(rel)
                item.size=len(body)
                archive.addfile(item,io.BytesIO(body))
        db.commit()
    return len(lengths)

def check_archive(archive):
    seen=0
    states={}
    for line in archive.open("r335-fault-originals.jsonl"):
        row=json.loads(line)
        if not row["eventId"].startswith("field-index-fixture-"):continue
        n=int(row["eventId"].rsplit("-",1)[1])
        assert n==seen
        seen+=1
        state=row["availability"]
        if n==1:assert state=="UNAVAILABLE:R335_FAULT_LOCATOR_RECEIPT_INTEGRITY",row
        elif n==2:assert state=="UNAVAILABLE:REPORT_FAULT_BODY_MISSING_OR_BYTES",row
        else:assert state in {"EXACT_ORIGINAL_HASH_AND_RECEIPT_VERIFIED","PRESENT_NOT_HASHED_COMPACT_BYTE_BUDGET"},row
        states[state]=states.get(state,0)+1
    assert seen==11396 and states["EXACT_ORIGINAL_HASH_AND_RECEIPT_VERIFIED"]>0 and states["PRESENT_NOT_HASHED_COMPACT_BYTE_BUDGET"]>0,states
    return states
