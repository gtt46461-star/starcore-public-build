#!/usr/bin/env python3
"""Compare actual stopped-emulator SQLite snapshots without publishing application rows or model bytes."""
import argparse,collections,hashlib,json,re,sqlite3
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--before',type=Path,required=True);p.add_argument('--after',type=Path,required=True);p.add_argument('--out',type=Path,required=True);p.add_argument('--label',required=True);a=p.parse_args()
MAIN='binance_k3_nova_one_pro_v57.db'
def q(s):return '"'+s.replace('"','""')+'"'
def open_main(root):
 files=list(root.rglob(MAIN));assert len(files)==1,('ONE_MAIN_DB_REQUIRED',root,len(files));c=sqlite3.connect(files[0].resolve().as_uri()+'?mode=ro',uri=True);assert c.execute('PRAGMA integrity_check').fetchone()[0]=='ok';return c
def tables(c):return {r[0] for r in c.execute("SELECT name FROM sqlite_master WHERE type='table'")}
def draws(c):return {r[0]:tuple(r[1:]) for r in c.execute('SELECT issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10 FROM draws')}
def blobs(c,table,col):
 try:return collections.Counter(hashlib.sha256(r[0]).hexdigest() for r in c.execute(f'SELECT {q(col)} FROM {q(table)} WHERE typeof({q(col)})=\'blob\' AND length({q(col)})>0'))
 except sqlite3.OperationalError:return collections.Counter()
before=open_main(a.before);after=open_main(a.after);bt=tables(before);at=tables(after);missing=sorted(bt-at);old=draws(before);new=draws(after);lost=[issue for issue,value in old.items() if new.get(issue)!=value];proof=[]
assert old,"BASELINE_DRAW_DATA_EMPTY"
for table in sorted(bt):
 if not re.search(r'model|checkpoint|frozen|atomnet|forecast|optimizer|rng',table,re.I):continue
 for col in before.execute('PRAGMA table_info('+q(table)+')'):
  original=blobs(before,table,col[1]);
  if not original:continue
  retained=blobs(after,table,col[1])+blobs(after,'r30_keep_'+table,col[1]);unretained=original-retained
  proof.append({'table':table,'column':col[1],'originalNonemptyBlobRows':sum(original.values()),'unretainedBlobRows':sum(unretained.values()),'allOriginalValuesFound':not unretained})
# Durable evidence index never shrinks/reset silently. State changes themselves are not mistaken for byte loss.
budgets=[]
if 'r27_eprocess' in bt and 'r27_eprocess' in at:
 for policy,index,n in before.execute('SELECT policy_hash,global_index,n FROM r27_eprocess'):
  row=after.execute('SELECT global_index,n FROM r27_eprocess WHERE policy_hash=?',(policy,)).fetchone()
  budgets.append({'policyHashSha256':hashlib.sha256(str(policy).encode()).hexdigest(),'preservedIndexAndCumulativeN':row is not None and row[0]==index and row[1]>=n})
receipt={'scope':'ACTUAL_ANDROID_EMULATOR_UPGRADE_OR_ROLLBACK_NOT_PHONE','label':a.label,'schemaTablesLost':missing,'integrity':'ok','beforeDrawCount':len(old),'afterDrawCount':len(new),'originalDrawsLostOrChangedCount':len(lost),'modelBlobColumns':proof,'modelBlobEvidence':'CHECKED_ORIGINAL_NONEMPTY_ROWS' if proof else 'NO_ORIGINAL_MODEL_BLOB_ROWS_IN_BASELINE','qualificationBudgets':budgets,'sourceRowsUploaded':False,'modelBytesUploaded':False,'physicalDevice':'NOT_RUN','naturalQuality':'NOT_PROVEN'}
receipt['pass']=not missing and not lost and len(new)<=5000 and all(p['allOriginalValuesFound'] for p in proof) and all(p['preservedIndexAndCumulativeN'] for p in budgets)
a.out.parent.mkdir(parents=True,exist_ok=True);a.out.write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n');before.close();after.close();print(json.dumps(receipt,ensure_ascii=False));assert receipt['pass'],'ACTUAL_DATA_RETENTION_FAILED'
