"""Compare the actual 202 and candidate catalog statements on the signed field catalog."""
import hashlib
import io
import json
import re
import sqlite3
import subprocess
import time
import zipfile
from pathlib import Path

field=Path("construction/plans/STARCORE_R34_3_REPAIR_20261126/report/ORIGINAL_DIAGNOSTIC_173.zip")
baseline="36f075222c8a8f7970a9dd04aefdda9379b11f1a"
catalog="app/src/main/java/com/ethk3/novaonepro/ReportCatalogCP4.java"
assert hashlib.file_digest(field.open("rb"),"sha256").hexdigest()=="3fb241b3181dccd4d149cf91471d86532919f77d943847ff8ce348cce5ec4d86"
with zipfile.ZipFile(field) as outer,zipfile.ZipFile(io.BytesIO(outer.read(outer.namelist()[0]))) as report:
    rows={}
    for name in sorted(x for x in report.namelist() if re.fullmatch(r"catalog-\d+.jsonl",x)):
        for line in report.open(name):
            r=json.loads(line)
            if r["kind"]=="schema_object":
                rows[r["type"],r["name"]]=[r["table"],r["rootpage"],None if r["sql_null"] else ""]
            if r["kind"]=="schema_sql_chunk":
                rows[r["type"],r["name"]][2]+=r["text"]
    faults=sum(1 for line in report.open("r335-fault-originals.jsonl"))
db=sqlite3.connect(":memory:")
db.execute("CREATE TABLE field_catalog(type,name,tbl_name,rootpage,sql)")
db.executemany("INSERT INTO field_catalog VALUES(?,?,?,?,?)",[(t,k,*value) for (t,k),value in rows.items()])
expected=[(t,k,*value) for (t,k),value in sorted(rows.items())]

def run(src):
    scan=re.search(r'query\(snapshot,"(SELECT (?:rowid,)?type,name,tbl_name,rootpage,length\(sql\) FROM sqlite_master[^"]+)"',src)[1].replace("sqlite_master","field_catalog")
    ddl=re.search(r'query\(snapshot,"(SELECT substr\(sql,\?,\?\) FROM sqlite_master[^"]+)"',src)[1].replace("sqlite_master","field_catalog")
    last=("","");seen=[];calls=0;start=time.monotonic()
    while True:
        page=db.execute(scan,(last[0],*last)).fetchall();calls+=1
        if not page:break
        for row in page:
            t,name,table,root,length=row[-5:];last=(t,name);sql=None if length is None else ""
            for pos in range(1,(length or 0)+1,8192):
                args=(row[0],) if len(row)==6 else last
                sql+=db.execute(ddl,(pos,8192,*args)).fetchone()[0]
            seen.append((t,name,table,root,sql))
    assert seen==expected
    return {"catalogPages":calls,"seconds":round(time.monotonic()-start,4),
            "catalogDigest":hashlib.sha256(json.dumps(seen,ensure_ascii=False).encode()).hexdigest()}
old=run(subprocess.check_output(["git","show",baseline+":"+catalog],text=True))
new=run(Path(catalog).read_text())
print(json.dumps({"fieldFaultLocators":faults,"schemaObjects":len(rows),"before":old,"after":new,"sameContents":True}))
assert new["catalogPages"]*30<old["catalogPages"] and new["catalogDigest"]==old["catalogDigest"]
