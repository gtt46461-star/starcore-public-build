"""Use real linked shallow and full checkouts and exercise no-replace reuse."""
from pathlib import Path
import sys
import tempfile
import unittest

import stage_checked as checked

PLAN=Path(sys.argv.pop(1)).resolve()
tool=checked.pinned_module(PLAN,'verification/contractor/stage.py','original_stage_history',
        '2457b6f332ea478271dde5582edd36e8f09ae68c697c194339ab4d5981e9620c')


class CheckoutHistory(unittest.TestCase):
    def run_case(self, shallow):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp)
            origin=root/'origin'
            origin.mkdir()
            git=tool.git
            git(origin,'init','-q')
            for i in range(3):
                (origin/'source').write_text(str(i))
                git(origin,'add','source')
                git(origin,'-c','user.name=Fixture','-c','user.email=fixture@example.invalid','commit','-qm',str(i))
            source=root/'main'
            if shallow:
                git(root,'clone','--quiet','--depth','2',origin.as_uri(),str(source))
            else:
                git(root,'clone','--quiet',origin.as_uri(),str(source))
            linked=root/'linked'
            git(source,'worktree','add','--quiet','--detach',str(linked),'HEAD')
            head=git(linked,'rev-parse','HEAD').decode().strip()
            stamp={'stage':'P00','sourceHead':head}
            dest=root/'out'
            args=(tool, linked, head, dest, b'', {'source':(b'2','100644')},stamp)
            self.assertEqual(checked.create_verified_checkout(*args),'CREATED_STAGED_REVIEW_CHECKOUT')
            self.assertEqual(git(dest,'rev-list','--count','HEAD').strip(),b'2' if shallow else b'3')
            self.assertEqual(git(dest,'rev-parse','--is-shallow-repository').strip(),b'true' if shallow else b'false')
            self.assertEqual(checked.create_verified_checkout(*args),'REUSED_VERIFIED_OUTPUT')
            (dest/'source').write_bytes(b'user changes')
            with self.assertRaises(ValueError):
                checked.create_verified_checkout(*args)
            self.assertEqual((dest/'source').read_bytes(),b'user changes')

    def test_linked_shallow(self):self.run_case(True)
    def test_linked_full(self):self.run_case(False)


if __name__=='__main__':
    unittest.main(verbosity=2)
