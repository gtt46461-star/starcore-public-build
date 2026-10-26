import sys,unittest,tempfile,hashlib
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'code'))
from durable_learning_r23 import DurableLearning
class DurableTests(unittest.TestCase):
    def setUp(self):self.x=DurableLearning();self.draw=list(range(1,11))
    def tearDown(self):self.x.close()
    def test_no_prediction_still_trains(self):
        self.x.register('a');self.x.record_label(1,self.draw)
        self.assertEqual(self.x.db.execute('select count(*) from learn_queue').fetchone()[0],1)
        self.assertEqual(self.x.db.execute('select count(*) from settlements').fetchone()[0],0)
    def test_new_model_bootstraps_existing_history(self):
        self.x.record_label(1,self.draw);self.x.register('new')
        self.assertEqual(self.x.db.execute('select count(*) from learn_queue').fetchone()[0],1)
    def test_label_settlement_queue_idempotency(self):
        self.x.register('a');self.x.freeze(1,'a','LIVE_SHADOW',0,[1,2,3],0)
        self.x.record_label(1,self.draw);self.x.record_label(1,self.draw)
        self.assertEqual(self.x.db.execute('select count(*),sum(hit) from settlements').fetchone(),(1,1))
        self.assertEqual(self.x.db.execute('select count(*) from learn_queue').fetchone()[0],1)
    def test_live_backfill_forbidden(self):
        self.x.record_label(1,self.draw)
        with self.assertRaises(ValueError):self.x.freeze(1,'a','LIVE_SHADOW',0,[1,2,3],0)
        self.x.freeze(1,'a','HISTORY_REPLAY',0,[1,2,3],0)
    def test_immutable_prediction_and_label(self):
        self.x.freeze(1,'a','LIVE_SHADOW',0,[1,2,3],0)
        with self.assertRaises(ValueError):self.x.freeze(1,'a','LIVE_SHADOW',0,[4,5,6],0)
        self.x.record_label(1,self.draw)
        with self.assertRaises(ValueError):self.x.record_label(1,list(reversed(self.draw)))
    def test_atomic_settlement_failure(self):
        self.x.register('a')
        with self.assertRaises(RuntimeError):self.x.record_label(1,self.draw,True)
        self.assertEqual(self.x.db.execute('select count(*) from labels').fetchone()[0],0)
        self.assertEqual(self.x.db.execute('select count(*) from learn_queue').fetchone()[0],0)
    def test_publish_failure_cannot_advance_cursor(self):
        self.x.register('a');self.x.record_label(1,self.draw)
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/'state';p.write_bytes(b'trained');h=hashlib.sha256(p.read_bytes()).hexdigest()
            with self.assertRaises(RuntimeError):self.x.publish('a',0,1,p,h,True)
            self.assertEqual(self.x.db.execute('select through_issue from models').fetchone()[0],0)
            self.assertEqual(self.x.db.execute('select count(*) from publications').fetchone()[0],0)
            self.x.publish('a',0,1,p,h)
            self.assertEqual(self.x.db.execute('select through_issue from models').fetchone()[0],1)
    def test_missing_artifact_and_label_gap_rejected(self):
        self.x.register('a');self.x.record_label(2,self.draw)
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/'state';p.write_bytes(b'trained');h=hashlib.sha256(p.read_bytes()).hexdigest()
            with self.assertRaises(ValueError):self.x.publish('a',0,2,p,h)
            with self.assertRaises(ValueError):self.x.publish('a',0,1,p,'bad')
if __name__=='__main__':unittest.main()
