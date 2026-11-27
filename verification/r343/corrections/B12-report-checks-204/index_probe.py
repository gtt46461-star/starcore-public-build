"""Check digest identity over all DDL in the actual field catalog, including UTF-16 length framing."""
import hashlib,io,json,re,sqlite3,zipfile
from pathlib import Path
field=Path('construction/plans/STARCORE_R34_3_REPAIR_20261126/report/ORIGINAL_DIAGNOSTIC_173.zip')
with zipfile.ZipFile(field) as outer, zipfile.ZipFile(io.BytesIO(outer.read(outer.namelist()[0]))) as z:
    ddl={}
    for name in sorted(f for f in z.namelist() if re.fullmatch(r'catalog-\d+.jsonl',f)):
        for line in z.open(name):
            row=json.loads(line)
            if row['kind']=='schema_object':ddl[row['type'],row['name']]=[row['table'],'']
            if row['kind']=='schema_sql_chunk':ddl[row['type'],row['name']][1]+=row['text']
db=sqlite3.connect(':memory:')
db.execute('CREATE TABLE objects(type,name,tbl_name,sql)')
db.executemany('INSERT INTO objects VALUES(?,?,?,?)',[(t,n,*v) for (t,n),v in ddl.items()])
condition=" WHERE name NOT LIKE 'sqlite_%'"
rows=lambda cursor: ['|'.join(field.strip() for field in r) for r in cursor]
original=sorted(rows(db.execute('SELECT * FROM objects'+condition+' ORDER BY type,name')))
prefix='FIELD|1|hash'
frame=lambda r:'\n'+str(len(r.encode('utf-16-le'))//2)+':'+r
expected=hashlib.sha256((prefix+''.join(map(frame,original))).encode()).hexdigest()
cursor=0; retained=[];pages=0
while True:
    page=db.execute('SELECT rowid,* FROM objects'+condition+' AND rowid>? ORDER BY rowid LIMIT 64',(cursor,)).fetchall()
    if not page:break
    retained+=rows(r[1:] for r in page);cursor=page[-1][0];pages+=1
digest=hashlib.sha256(prefix.encode())
for row in sorted(retained):digest.update(frame(row).encode())
assert digest.hexdigest()==expected and sorted(retained)==original
print(json.dumps(dict(schemaObjects=len(ddl),included=len(original),pages=pages,canonicalHash=expected,identical=True)))
