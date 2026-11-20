"""Exercise the same source-archive and clean-room bundle intake used on the APK path."""
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
from zipfile import ZipFile

import received_build

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


class ReceivedBuild(unittest.TestCase):
    def setUp(self):
        fixture = SourceDelivery()
        fixture.setUp()
        self.addCleanup(fixture.doCleanups)
        self.fixture = fixture
        self.root, self.repo, self.candidate = fixture.root, fixture.repo, fixture.candidate
        self.plan = self.root / 'plan'
        (self.plan / 'control').mkdir(parents=True)
        (self.plan / 'verification').mkdir()
        baseline = patch.object(packer.binding, 'BASE', fixture.base)
        baseline.start()
        self.addCleanup(baseline.stop)
        def put(path, value):
            path.write_text(json.dumps(value, sort_keys=True))
        self.put = put
        descriptor = dict(schema='STARCORE_PLAN_DESCRIPTOR_1', sourceBaseline=fixture.base, planRevision='R33.5',
                          gateRegistry='control/32_gate_contracts.json',scenarioRegistry='control/required_scenarios_R334.json')
        put(self.plan/'control/PLAN_IDENTITY.json',descriptor)
        for rel in (descriptor['gateRegistry'],descriptor['scenarioRegistry']):
            put(self.plan / rel, [])
        plan_manifest = self.plan/'verification/SHA256.json'
        put(plan_manifest,{p.relative_to(self.plan).as_posix(): receive.sha(p) for p in self.plan.rglob('*.json')})
        (self.repo/'release').mkdir()
        self.manifest = dict(applicationId='com.starcore.research.r13',versionCode=577311044,versionName='fixture')
        put(self.repo/'release/release_manifest.json',self.manifest)
        pin=self.repo/'release/r33_plan_binding.json'
        put(pin,dict(schema=packer.binding.PLAN_SCHEMA,sourceBaseline=fixture.base,planRevision='R33.5',
                     planManifestSha256=receive.sha(plan_manifest),gateRegistrySha256=receive.sha(self.plan/descriptor['gateRegistry']),
                     scenarioRegistrySha256=receive.sha(self.plan/descriptor['scenarioRegistry'])))
        git(self.repo,'add','release')
        git(self.repo,'commit','-qm','release and plan binding')
        self.head=git(self.repo,'rev-parse','HEAD').decode().strip()
        self.identity=packer.binding.identity(self.repo,self.plan,pin,True)
        self.apk=self.candidate/'candidate.apk'
        self.write_apk(self.identity)
        put(self.candidate/'build-identity-r334.json',self.identity)
        fixture.zip.unlink()
        fixture.bundle.unlink()
        archive=packer.write_source_archive(self.repo,self.head,fixture.zip)
        git(self.repo,'bundle','create',str(fixture.bundle),'HEAD')
        tree=self.identity['source_tree']
        baseline_apk=dict(package=self.manifest['applicationId'],certificateSha256=packer.CERT,versionCode=577311033)
        apk=dict(file=self.apk.name,sha256=receive.sha(self.apk),versionCode=self.manifest['versionCode'],
                 versionName=self.manifest['versionName'],certificateSha256=packer.CERT)
        log=self.candidate/'gradle-build.log'
        log.write_text('Fixture completed release task\n')
        self.build=dict(schema='STARCORE_R33_4_RELEASE_BUILD_1',gradleRelease='PASS',exitCode=0,wrapperExitCode=0,
                        sourceClean=True,sourceCleanBeforeBuild=True,sourceUnchangedDuringBuild=True,sourceCommit=self.head,
                        sourceTree=tree,apkSha256=apk['sha256'],planManifestSha256=receive.sha(plan_manifest),
                        gradleLogSha256=receive.sha(log),command=['gradlew',':app:assembleRelease'])
        self.delivery=dict(schema='STARCORE_R33_4_CANDIDATE_DELIVERY_1',sourceCommit=self.head,sourceTree=tree,
                           sourceArchive=dict(archive,file=fixture.zip.name,sha256=receive.sha(fixture.zip)),
                           gitBundle=dict(file=fixture.bundle.name,sha256=receive.sha(fixture.bundle),sourceCommit=self.head,sourceTree=tree),
                           apk=apk,baseline033=baseline_apk,planManifestSha256=receive.sha(plan_manifest),gradleLogSha256=receive.sha(log))
        for name in ('public-certificate.txt','badging.txt','baseline033-certificate.txt','baseline033-badging.txt'):
            (self.candidate/name).write_text('Fixture original '+name)
        self.outer=self.candidate/f"STARCORE-R33-577311044-{self.head[:12]}-APK-SOURCE-EVIDENCE.zip"
        self.repackage()

    def write_apk(self, identity):
        with ZipFile(self.apk,'w') as z:
            for name,body in [('classes.dex',b'fixture'),('AndroidManifest.xml',b'fixture'),
                              ('assets/diagnostics/build-identity-r31.json',json.dumps(self.identity)),
                              ('assets/diagnostics/build-identity-r334.json',json.dumps(identity))]:
                z.writestr(name,body)

    def repackage(self, override=None):
        self.outer.unlink(missing_ok=True)
        index=self.candidate/received_build.INDEX
        index.unlink(missing_ok=True)
        self.put(self.candidate/'CI_BUILD_RECEIPT.json',self.build)
        self.put(self.candidate/'DELIVERY.json',self.delivery)
        files=list(self.candidate.iterdir())
        self.put(index,{p.name:receive.sha(p) for p in files})
        with ZipFile(self.outer,'w') as z:
            for p in [*files,index]:
                z.writestr(p.name, (override or {}).get(p.name,p.read_bytes()))

    def verify(self):
        sources=receive.verify(packer,self.repo,self.candidate,self.fixture.base)
        return received_build.verify(packer,self.plan,self.repo,self.candidate,sources)

    def test_complete_delivery_and_adjacent_download(self):
        expected=receive.sha(self.outer)
        self.outer.rename(self.candidate.parent/self.outer.name)
        result=self.verify()
        self.assertEqual(result['downloadSha256'],expected)
        received_build.match_signature(dict(sourceCommit=self.head,apk=self.delivery['apk'],
            baseline033=dict(self.delivery['baseline033'],file='033.apk'),buildReceiptSha256=result['buildReceiptSha256']),
            self.delivery,result)

    def test_rehashed_apk_with_other_embedded_commit(self):
        self.write_apk(dict(self.identity,source_commit='e'*40))
        self.build['apkSha256']=self.delivery['apk']['sha256']=receive.sha(self.apk)
        self.repackage()
        # Source ZIP/bundle and the pre-existing candidate/version input route still accept this tuple.
        from verify_delivery import inputs
        dumpsys=self.root/'before.txt'
        dumpsys.write_text('  Package [com.starcore.research.r13] (observed):\n    versionCode=577311033\n')
        self.assertIn(str(self.apk),inputs(self.candidate,self.repo,dumpsys))
        with self.assertRaisesRegex(ValueError,'EMBEDDED_R334_PLAN_OR_SOURCE_MISMATCH'):
            self.verify()

    def test_gradle_log_missing_or_replaced(self):
        log=self.candidate/'gradle-build.log'
        log.unlink()
        with self.assertRaisesRegex(ValueError,'DELIVERY_ORIGINAL_REQUIRED'):
            self.verify()
        log.write_text('different run')
        self.repackage()
        with self.assertRaisesRegex(ValueError,'BUILD_ORIGINALS_MISMATCH'):
            self.verify()

    def test_source_changed_during_build(self):
        self.build['sourceUnchangedDuringBuild']=False
        self.repackage()
        with self.assertRaisesRegex(ValueError,'BUILD_ORIGINALS_MISMATCH'):
            self.verify()

    def test_download_disagrees_with_local_candidate(self):
        self.repackage({self.apk.name:b'other download bytes'})
        with self.assertRaisesRegex(ValueError,'DOWNLOADABLE_ZIP_ORIGINAL_DIFFERS'):
            self.verify()

    def test_missing_download_refuses_acceptance(self):
        self.outer.unlink()
        with self.assertRaisesRegex(ValueError,'DOWNLOADABLE_DELIVERY_ZIP_REQUIRED'):
            self.verify()


if __name__ == '__main__':
    unittest.main(verbosity=2)
