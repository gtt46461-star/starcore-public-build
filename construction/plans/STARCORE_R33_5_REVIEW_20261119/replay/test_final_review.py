import contextlib
import io
import json
from pathlib import Path
import tempfile
import unittest
import final_review as final


class Intake(unittest.TestCase):
    def test_old_score_alone_does_not_grant_review(self):
        with tempfile.TemporaryDirectory() as temp:
            old = Path(temp) / 'historical.json'
            old.write_text('{"engineeringGroups":{"verified":32,"denominator":32}}')
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                code = final.main(['--execution', str(old)])
            row = json.loads(output.getvalue())
            self.assertNotEqual(code, 0)
            self.assertIn('stage-receipt', row['missingInputs'])
            self.assertEqual(row['checks'], {})
            self.assertEqual(row['status'], 'INCOMPLETE')

    def test_unrun_or_absent_trace_cannot_bind(self):
        with tempfile.TemporaryDirectory() as temp:
            for rows in ([], [{'id':'B08.2.TRACE','status':'NOT_RUN','proofs':[]}],
                          [{'id':'B08.2.TRACE','status':'PASS','proofs':[]}]):
                with self.assertRaises(ValueError):
                    final.proof_paths({'groups':[{'assertions':rows}]}, Path(temp), 'B08.2.TRACE')


if __name__ == '__main__':
    unittest.main(verbosity=2)
