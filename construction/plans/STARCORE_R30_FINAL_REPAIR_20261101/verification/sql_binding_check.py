"""Check actual new writer SQL against actual schema using SQLite EXPLAIN; no Android claims."""
from pathlib import Path
import re,sqlite3
root=Path(__file__).resolve().parents[1]; src=next(root.glob('batches/**/code/**/ResearchStoreR30R1.java')).read_text()
db=sqlite3.connect(':memory:')
for x in (root/'verification/schema_ddl.sql').read_text().splitlines():db.execute(x)
checked=0
for m in re.finditer(r'\b(?:b|batch)\.add\("([^"\n]+)"',src):
 pos=m.end();depth=0;quoted=False;escaped=False;args=0
 while pos<len(src):
  ch=src[pos];pos+=1
  if quoted:
   if escaped:escaped=False
   elif ch=='\\':escaped=True
   elif ch=='"':quoted=False
   continue
  if ch=='"':quoted=True
  elif ch in '([{':depth+=1
  elif ch in ')]}':
   if ch==')' and depth==0:break
   depth-=1
  elif ch==',' and depth==0:args+=1
 sql=m.group(1);assert sql.count('?')==args,(sql,args)
 db.execute('EXPLAIN '+sql,[None]*args).fetchall();checked+=1
assert checked==6,checked
(root/'verification/sql_binding_test.log').write_text('RESEARCH_WRITER_SQL_HOST_PASS statements='+str(checked)+' placeholder_arity=true schema_columns=true SQLite_EXPLAIN=true\n')
print((root/'verification/sql_binding_test.log').read_text())
