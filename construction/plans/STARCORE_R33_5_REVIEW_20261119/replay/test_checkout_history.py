"""Run the reviewed handoff on real Git objects and check exact reuse."""
from pathlib import Path
import sys
import tempfile
import unittest

import stage_checked as checked

PLAN = Path(sys.argv.pop(1)).resolve()
tool = checked.pinned_module(PLAN, 'verification/contractor/stage.py', 'original_stage_history',
                             '2457b6f332ea478271dde5582edd36e8f09ae68c697c194339ab4d5981e9620c')
git = tool.git


class CheckoutHistory(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        self.origin = self.root / 'origin'
        self.origin.mkdir()
        git(self.origin, 'init', '-q')
        (self.origin / 'other').write_text('untouched')
        for i in range(3):
            (self.origin / 'source').write_text(str(i))
            git(self.origin, 'add', 'source', 'other')
            self.commit(self.origin, str(i))

    def commit(self, repo, message):
        git(repo, '-c', 'user.name=Fixture', '-c', 'user.email=fixture@example.invalid', 'commit', '-qm', message)

    def linked(self, shallow=False):
        source = self.root / 'main'
        extra = ['--depth', '2'] if shallow else []
        git(self.root, 'clone', '--quiet', *extra, self.origin.as_uri(), str(source))
        linked = self.root / 'linked'
        git(source, 'worktree', 'add', '--quiet', '--detach', str(linked), 'HEAD')
        return linked

    def install(self, source, file='source', body=b'2'):
        head = git(source, 'rev-parse', 'HEAD').decode().strip()
        return checked.create_verified_checkout(tool, source, head, self.root / 'out', b'',
                {file: (body, '100644')}, {'stage': 'P00', 'sourceHead': head})

    def run_case(self, shallow):
        source = self.linked(shallow)
        dest = self.root / 'out'
        self.assertEqual(self.install(source), 'CREATED_STAGED_REVIEW_CHECKOUT')
        self.assertEqual(git(dest, 'rev-list', '--count', 'HEAD').strip(), b'2' if shallow else b'3')
        self.assertEqual(git(dest, 'rev-parse', '--is-shallow-repository').strip(), b'true' if shallow else b'false')
        self.assertEqual(self.install(source), 'REUSED_VERIFIED_OUTPUT')
        (dest / 'source').write_bytes(b'user changes')
        with self.assertRaises(ValueError):
            self.install(source)
        self.assertEqual((dest / 'source').read_bytes(), b'user changes')

    def test_linked_shallow(self):
        self.run_case(True)

    def test_linked_full(self):
        self.run_case(False)

    def test_shallow_boundary_on_another_history(self):
        source = self.linked(shallow=True)
        git(source, 'checkout', '--orphan', 'independent')
        self.commit(source, 'independent full root')
        self.assertEqual(git(source, 'rev-parse', '--is-shallow-repository').strip(), b'true')
        self.assertEqual(self.install(source), 'CREATED_STAGED_REVIEW_CHECKOUT')
        self.assertEqual(git(self.root / 'out', 'rev-list', '--count', 'HEAD').strip(), b'1')
        self.assertEqual(self.install(source), 'REUSED_VERIFIED_OUTPUT')

    def test_staged_user_file_is_preserved_and_rejected_on_reuse(self):
        source = self.linked()
        self.install(source)
        dest = self.root / 'out'
        (dest / 'other').write_bytes(b'user stage')
        git(dest, 'add', 'other')
        with self.assertRaisesRegex(ValueError, 'OUTPUT_INDEX_OUTSIDE_STAGE'):
            self.install(source)
        self.assertEqual(git(dest, 'show', ':other'), b'user stage')

    def test_untracked_java_is_preserved_and_rejected_on_reuse(self):
        source = self.linked()
        self.install(source)
        dest = self.root / 'out'
        extra = dest / 'Extra.java'
        extra.write_bytes(b'class Extra {}')
        with self.assertRaisesRegex(ValueError, 'OUTPUT_UNTRACKED_FILES'):
            self.install(source)
        self.assertEqual(extra.read_bytes(), b'class Extra {}')

    def test_unstaged_user_changes_in_other_tracked_file(self):
        source = self.linked()
        self.install(source)
        dest = self.root / 'out'
        (dest / 'other').write_bytes(b'local work')
        with self.assertRaisesRegex(ValueError, 'OUTPUT_WORKTREE_DIFFERS'):
            self.install(source)
        self.assertEqual((dest / 'other').read_bytes(), b'local work')

    def test_assume_unchanged_hides_worktree_modification(self):
        source = self.linked()
        self.install(source)
        dest = self.root / 'out'
        git(dest, 'update-index', '--assume-unchanged', '--', 'other')
        (dest / 'other').write_bytes(b'local work')
        with self.assertRaisesRegex(ValueError, 'OUTPUT_WORKTREE_DIFFERS'):
            self.install(source)
        self.assertEqual((dest / 'other').read_bytes(), b'local work')


if __name__ == '__main__':
    unittest.main(verbosity=2)
