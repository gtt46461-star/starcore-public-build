"""Executable SQLite design contracts. Not production/Android/72h acceptance."""
import json,sqlite3,unittest
from pathlib import Path

HERE=Path(__file__).resolve().parent
def database():
 c=sqlite3.connect(':memory:',isolation_level=None)
 c.executescript((HERE/'storage_contract.sql').read_text())
 c.execute('INSERT INTO budget VALUES(1,20,100)')
 return c

def claim_gc(c):
 # Single production writer must own this complete transaction. No file IO inside.
 c.execute('BEGIN IMMEDIATE')
 try:
  candidates=c.execute("""WITH RECURSIVE reached(hash) AS (
    SELECT hash FROM root WHERE state='ACTIVE'
    UNION SELECT e.child FROM edge e JOIN reached r ON e.parent=r.hash
  ) SELECT hash,generation FROM artifact WHERE state='LIVE' AND hash NOT IN(SELECT hash FROM reached) ORDER BY hash""").fetchall()
  for h,g in candidates:
   c.execute("UPDATE artifact SET state='DELETING',generation=generation+1 WHERE hash=? AND generation=? AND state='LIVE'",(h,g))
   c.execute("INSERT INTO gc_action VALUES(?,?,?,'CLAIMED')",(f'{h}:{g+1}',h,g+1))
  c.execute('COMMIT');return candidates
 except BaseException:
  c.execute('ROLLBACK');raise

def finish_reservation(c,id):
 c.execute('BEGIN IMMEDIATE')
 try:
  row=c.execute('SELECT used FROM reservation WHERE id=?',(id,)).fetchone()
  if row is None:raise ValueError('UNKNOWN_RESERVATION')
  c.execute('UPDATE budget SET base_bytes=base_bytes+? WHERE singleton=1',(row[0],))
  c.execute('DELETE FROM reservation WHERE id=?',(id,))
  c.execute('COMMIT')
 except BaseException:
  c.execute('ROLLBACK');raise

class Contracts(unittest.TestCase):
 def setUp(self):self.c=database()
 def tearDown(self):self.c.close()
 def obj(self,h,state='LIVE'):self.c.execute('INSERT INTO artifact VALUES(?,1,?,1)',(h,state))
 def test_budget_includes_all_unfinished_reservations(self):
  self.c.execute("INSERT INTO reservation VALUES('report','r',60,0)")
  with self.assertRaises(sqlite3.IntegrityError):self.c.execute("INSERT INTO reservation VALUES('train','t',21,0)")
  self.assertEqual(self.c.execute('SELECT count(*) FROM reservation').fetchone()[0],1)
 def test_chunk_cannot_exceed_reserved_bytes(self):
  self.c.execute("INSERT INTO reservation VALUES('r','o',60,0)")
  with self.assertRaises(sqlite3.IntegrityError):self.c.execute("UPDATE reservation SET used=61 WHERE id='r'")
  self.assertEqual(self.c.execute('SELECT used FROM reservation').fetchone()[0],0)
 def test_published_bytes_and_reservation_move_atomically(self):
  self.c.execute("INSERT INTO reservation VALUES('r','o',60,40)");finish_reservation(self.c,'r')
  self.assertEqual(self.c.execute('SELECT base_bytes FROM budget').fetchone()[0],60)
  self.assertEqual(self.c.execute('SELECT count(*) FROM reservation').fetchone()[0],0)
  self.c.execute("INSERT INTO reservation VALUES('t','o',40,0)")
 def test_finish_is_not_double_counted(self):
  self.c.execute("INSERT INTO reservation VALUES('r','o',60,40)");finish_reservation(self.c,'r')
  with self.assertRaises(ValueError):finish_reservation(self.c,'r')
  self.assertEqual(self.c.execute('SELECT base_bytes FROM budget').fetchone()[0],60)
 def test_reachable_cycle_is_preserved(self):
  self.obj('a');self.obj('b');self.c.execute("INSERT INTO edge VALUES('a','b')");self.c.execute("INSERT INTO edge VALUES('b','a')");self.c.execute("INSERT INTO root VALUES('champion','a','ACTIVE')")
  self.assertEqual(claim_gc(self.c),[])
 def test_unreachable_cycle_is_claimed(self):
  self.obj('a');self.obj('b');self.c.execute("INSERT INTO edge VALUES('a','b')");self.c.execute("INSERT INTO edge VALUES('b','a')")
  self.assertEqual([x[0] for x in claim_gc(self.c)],['a','b'])
 def test_pin_before_claim_wins(self):
  self.obj('a');self.c.execute("INSERT INTO root VALUES('pending-training','a','ACTIVE')")
  self.assertEqual(claim_gc(self.c),[])
 def test_claim_before_pin_wins(self):
  self.obj('a');claim_gc(self.c)
  with self.assertRaises(sqlite3.IntegrityError):self.c.execute("INSERT INTO root VALUES('late-training','a','ACTIVE')")
 def test_claim_blocks_new_reference_edges(self):
  self.obj('a');self.obj('b');self.c.execute("INSERT INTO root VALUES('model','a','ACTIVE')");claim_gc(self.c)
  with self.assertRaises(sqlite3.IntegrityError):self.c.execute("INSERT INTO edge VALUES('a','b')")
 def test_shared_input_waits_for_all_roots(self):
  self.obj('a');self.c.execute("INSERT INTO root VALUES('t1','a','ACTIVE')");self.c.execute("INSERT INTO root VALUES('t2','a','ACTIVE')");self.c.execute("UPDATE root SET state='CLOSED' WHERE id='t1'")
  self.assertEqual(claim_gc(self.c),[])
  self.c.execute("UPDATE root SET state='CLOSED' WHERE id='t2'");self.assertEqual(len(claim_gc(self.c)),1)
 def test_incomplete_staged_object_not_gc_claimable(self):
  self.obj('a','STAGED');self.assertEqual(claim_gc(self.c),[])
 def test_crash_after_claim_not_reported_as_unlinked(self):
  self.obj('a');claim_gc(self.c)
  self.assertEqual(self.c.execute('SELECT phase FROM gc_action').fetchone()[0],'CLAIMED')
  self.assertEqual(claim_gc(self.c),[])

if __name__=='__main__':
 suite=unittest.defaultTestLoader.loadTestsFromTestCase(Contracts)
 result=unittest.TextTestRunner(verbosity=2).run(suite)
 receipt={'scope':'SQLite standalone protocol design model only; Android integration and physical device NOT_RUN','tests':result.testsRun,'failures':len(result.failures),'errors':len(result.errors),'passed':result.wasSuccessful(),'sqlite_version':sqlite3.sqlite_version}
 (HERE/'storage_contract_receipt.json').write_text(json.dumps(receipt,indent=2))
 raise SystemExit(0 if result.wasSuccessful() else 1)
