"""Usage: python3 verification/run_host_checks.py /absolute/path/to/b225_checkout
Only HOST logic/SQL checks; no Android/full runtime acceptance. Uses a temporary overlay, never edits checkout.
"""
from pathlib import Path
import tempfile,subprocess,sys,json,shutil,sqlite3,re
pkg=Path(__file__).resolve().parents[1];base=Path(sys.argv[1]).resolve()
assert subprocess.check_output(['git','rev-parse','HEAD'],cwd=base,text=True).strip()=='b225ab204db043b347f9d76d6c5c1376fd9a2590','Wrong baseline'
def run(args,cwd=None):subprocess.run(list(map(str,args)),cwd=cwd,check=True)
run(['git','apply','--check',pkg/'patches/ALL_KEY_CODE.patch'],base)
with tempfile.TemporaryDirectory(prefix='r30_host_') as tmp:
 tree=Path(tmp)/'tree';tree.mkdir();run(['git','archive','--format=tar','--output='+str(Path(tmp)/'src.tar'),'HEAD'],base)
 import tarfile
 with tarfile.open(Path(tmp)/'src.tar')as tf:tf.extractall(tree,filter='data')
 run(['git','apply',pkg/'patches/ALL_KEY_CODE.patch'],tree)
 s=tree/'app/src/main/java/com/ethk3/novaonepro/strategyv2';v=pkg/'verification';classes=Path(tmp)/'classes';classes.mkdir()
 pure=['ResearchContractR30R1','ResearchGainR30R1','ResearchAdviceR30R1','TaskProgressR30R1','CommitEnvelopeR30R1','ConfirmationViewR30R1','ResearchSchemaR30R1','ResearchGraphSpecsR30R1','WriterHoldLedgerR30']
 javac=['java','com.sun.tools.javac.Main','-encoding','UTF-8','-d',classes]
 run(javac+[s/(x+'.java')for x in pure]+[v/x for x in ['ResearchCoreTest.java','WriterRotateTest.java','SchemaDump.java']])
 for name in ['ResearchCoreTest','WriterRotateTest']:run(['java','-cp',classes,'com.ethk3.novaonepro.strategyv2.'+name])
 run(javac+list((v/'adapters').rglob('*.java'))+[s/'DiagnosisTextSU2.java',s/'UpgradeDiagnosisSU2.java',v/'DiagnosisRenderRegression.java'])
 run(['java','-cp',classes,'com.ethk3.novaonepro.strategyv2.DiagnosisRenderRegression',tree/'tools/strategy_su2/fixtures/r16_diag'])
 fc=Path(tmp)/'flush';fc.mkdir();run(['java','com.sun.tools.javac.Main','-encoding','UTF-8','-d',fc]+list((v/'flush_adapters').rglob('*.java'))+[s/'WriterHoldLedgerR30.java',s/'HoldLedgerFlushR30.java',v/'HoldFlushRetryTest.java'])
 run(['java','-cp',fc,'com.ethk3.novaonepro.strategyv2.HoldFlushRetryTest'])
 ddl=subprocess.check_output(['java','-cp',str(classes),'SchemaDump'],text=True);db=sqlite3.connect(':memory:');db.execute('PRAGMA foreign_keys=ON')
 for sql in ddl.splitlines():db.execute(sql)
 for sql in ddl.splitlines():db.execute(sql)
 db.execute("INSERT INTO r30r1_analysis_run VALUES('r','d','p','SELECT','b225',1,1,10,'c','DONE','{}',1)")
 db.execute("INSERT INTO r30r1_proposal VALUES('p','r','parent','PRUNE','{}','h','READY','reason',1)")
 db.execute("INSERT INTO r30r1_consume VALUES('p','child',1,'v','receipt',1)");db.commit()
 for sql in ["INSERT INTO r30r1_consume VALUES('p','other',1,'v','receipt',1)","UPDATE r30r1_consume SET child_policy_hash='other'","INSERT INTO r30r1_proposal VALUES('orphan','missing','parent','PRUNE','{}','h','READY','reason',1)"]:
  try:db.execute(sql)
  except sqlite3.IntegrityError:pass
  else:raise AssertionError(sql)
 db.rollback();db.execute('BEGIN');db.execute("INSERT INTO r30r1_proposal VALUES('rollback','r','parent','PRUNE','{}','h','READY','reason',1)");db.rollback();assert db.execute("SELECT count(*)FROM r30r1_proposal WHERE proposal_id='rollback'").fetchone()[0]==0
 src=(s/'ResearchStoreR30R1.java').read_text();checked=0
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
    if ch==')'and depth==0:break
    depth-=1
   elif ch==','and depth==0:args+=1
  sql=m.group(1);assert sql.count('?')==args;db.execute('EXPLAIN '+sql,[None]*args).fetchall();checked+=1
 assert checked==6
 print('HOST_AND_SQL_CHECKS_PASS; NOT_ANDROID_OR_DEVICE_OR_NATURAL_QUALITY')
