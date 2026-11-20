"""Exercise the same source-archive and clean-room bundle intake used on the APK path."""
import json
from pathlib import Path
import sys
import tempfile
import unittest

import check_source_delivery as receive

PLAN = Path(sys.argv.pop(1)).resolve()
packer = receive.packager(PLAN)
git = packer.binding.git


class SourceDelivery(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.repo = self.root / 'repo'
        self.repo.mkdir()
        self.candidate = self.root / 'candidate'
        self.candidate.mkdir()
        git(self.repo, 'init', '-q')
        git(self.repo, 'config', 'user.name', 'Fixture')
        git(self.repo, 'config', 'user.email', 'fixture@example.invalid')
        (self.repo/'input').write_bytes(b'033 fixture\n')
        git(self.repo, 'add', '.')
        git(self.repo, 'commit', '-qm', 'fixture root')
        self.base = git(self.repo, 'rev-parse', 'HEAD').decode().strip()
        (self.repo/'input').write_bytes(b'new candidate bytes\n')
        git(self.repo, 'commit', '-qam', 'fixture candidate')
        self.commit = git(self.repo, 'rev-parse', 'HEAD').decode().strip()
        self.zip = self.candidate/'source.zip'
        self.bundle = self.candidate/'source.bundle'
        archive = packer.write_source_archive(self.repo, self.commit, self.zip)
        git(self.repo,'bundle','create',str(self.bundle),'HEAD')
        self.delivery = dict(sourceCommit=self.commit, sourceTree=archive['gitTree'],
            sourceArchive=dict(archive,file=self.zip.name,sha256=receive.sha(self.zip)),
            gitBundle=dict(file=self.bundle.name,sha256=receive.sha(self.bundle),
                           sourceCommit=self.commit,sourceTree=archive['gitTree']))
        self.save()

    def save(self):
        (self.candidate/'DELIVERY.json').write_text(json.dumps(self.delivery))

    def verify(self):
        return receive.verify(packer,self.repo,self.candidate,self.base)

    def test_actual_zip_and_empty_restore(self):
        self.assertEqual(self.verify()['restored033Ancestor'], self.base)

    def test_lost_source_after_packaging(self):
        self.zip.unlink()
        with self.assertRaisesRegex(ValueError, 'SOURCE_DELIVERY_MEMBER'):
            self.verify()

    def test_wrong_source_even_after_rehash(self):
        self.zip.unlink()
        import zipfile
        with zipfile.ZipFile(self.zip,'w') as archive:
            archive.writestr('substitute', b'wrong source')
        self.delivery['sourceArchive']['sha256']=receive.sha(self.zip)
        self.save()
        with self.assertRaisesRegex(ValueError,'SOURCE_ZIP_MEMBER_SET_MISMATCH'):
            self.verify()

    def test_prerequisite_bundle_refused_by_empty_restore(self):
        self.bundle.unlink()
        git(self.repo,'bundle','create',str(self.bundle),self.base+'..HEAD')
        self.delivery['gitBundle']['sha256']=receive.sha(self.bundle)
        self.save()
        with self.assertRaisesRegex(ValueError,'GIT_FAILED:bundle'):
            self.verify()

    def test_receipt_for_other_tree(self):
        self.delivery['gitBundle']['sourceTree']='0'*40
        self.save()
        with self.assertRaisesRegex(ValueError,'SOURCE_DELIVERY_COMMIT_OR_TREE'):
            self.verify()


if __name__ == '__main__':
    unittest.main(verbosity=2)
