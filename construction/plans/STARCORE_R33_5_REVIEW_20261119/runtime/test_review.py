"""Usage: python -B test_review.py /path/to/prepared-tools"""
import hashlib
import json
import sqlite3
import sys
import tempfile
import unittest
from pathlib import Path
import check_window

tools=Path(sys.argv.pop(1)).resolve()
sys.path[:0]=[str(tools/'B02/enabled_window_r335'),str(tools/'verification/mainline'),str(tools/'B01/recovery')]
import test_enabled_window as windows
import test_trace as traces
import prepare_exact_intake as intake

class Corrections(unittest.TestCase):
    def test_changed_volume_is_bounded(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp)
            chunks=[b'AA',b'BB']
            rows=[]
            for i,chunk in enumerate(chunks,1):
                name='STARCORE_FULL_REPORT.zip.part%02d'%i
                (root/name).write_bytes(chunk)
                rows.append(dict(name=name,path='delivery/'+name,partNumber=i,offset=2*i-2,
                                 bytes=2,sha256=hashlib.sha256(chunk).hexdigest()))
            index=root/'index.json'
            index.write_text(json.dumps(dict(format='SCREPORT_ZIP_PARTS_R30_1',files=rows,
                       zipBytes=4,totalVolumeBytes=4,zipSha256=hashlib.sha256(b'AABB').hexdigest(),
                       expectedVolumes=2,exportedVolumes=2,omittedVolumes=0)))
            with intake.Parts(index) as stream:
                (root/rows[0]['name']).write_bytes(b'')
                with self.assertRaisesRegex(OSError,'PART_CHANGED'): stream.read(1)

    def test_bet_cutoffs_independently(self):
        fixture=traces.TraceTests('test_real_with_receipt_and_tampering')
        self.addCleanup(fixture.doCleanups)
        fixture.setUp()
        fixture.test_real_with_receipt_and_tampering()
        issue=fixture.ids[0]
        with sqlite3.connect(fixture.db) as db:
            db.row_factory=sqlite3.Row
            db.execute('UPDATE r974_exact_real_receipt SET trade_id=7')
            trace=traces.c
            f=trace.single(db,trace.FREEZE,'target_issue',issue)
            draw=dict(trace.single(db,trace.DRAW,'issue',issue))
            cutoff=f['deadline_at']
            draw.update(open_time=cutoff+1000,first_seen_at=cutoff+1100)
            action=trace.single(db,'v57_k3_champion_freeze','target_issue',issue)['action_hash']
            def check():
                trace.real_hashes(db,issue,'BET',f,draw,1,1,(1,2,3),action)
            for table in ('r974_exact_real_receipt','research_exact_probability_f05'):
                db.execute('UPDATE '+table+' SET committed_at=?',(cutoff-1,))
                check()
                for late in (cutoff,cutoff+1):
                    db.execute('UPDATE '+table+' SET committed_at=?',(late,))
                    with self.assertRaises(ValueError):check()
                db.execute('UPDATE '+table+' SET committed_at=?',(f['commit_at'],))

    def test_full_enabled_window_keeps_missing_and_timing(self):
        f=windows.EnabledCoverageTests()
        self.addCleanup(f.doCleanups)
        f.setUp()
        result=check_window.evaluate(tools,f.bundle,f.root,f.start,f.end,windows.C,windows.A,'FIXTURE')
        self.assertEqual((result['outcome'],len(result['missingOrInvalid'])),('INCOMPLETE',20))
        self.assertEqual(result['timing']['previousOpenToFreeze']['p95Ms'],4000)
        self.assertEqual(result['timing']['targetOpenToSettlement']['maxMs'],450)
        f.test_real_explicit_stop_only_excludes_wholly_disabled_intervals()
        short=check_window.evaluate(tools,f.bundle,f.root,f.start,f.end,windows.C,windows.A,'FIXTURE')
        self.assertEqual((short['outcome'],short['counts']['explicitlyDisabled']),('INCOMPLETE',20))
        late=check_window.summarize([{'target':'a','ms':5000},{'target':'b','ms':5001}],5000)
        self.assertEqual(late['violations'],[{'target':'b','ms':5001}])

if __name__ == '__main__':
    unittest.main(verbosity=2)
