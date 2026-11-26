#!/usr/bin/env python3
"""Host-only synthetic fixtures for the field receipt gate; never device acceptance."""
import copy
import importlib.util
import io
import json
from pathlib import Path
import subprocess
import sys
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch
import zipfile

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(1,str(ROOT/'tools/r33'))
import verify_plan_binding as binding
import field_delivery_contract as contract
import run_field_engineering as runner


SCRIPT=r'''import hashlib,json,os
from pathlib import Path
c=json.loads(Path(os.environ['STARCORE_FIELD173_CASE_CONTEXT']).read_text())
print('FIELD173_ENGINEERING_EVENT '+json.dumps(dict(c,event='CASE_ENTER')))
x=Path('fixture.json').read_bytes()
assert json.loads(x)['value']==7
assert hashlib.sha256(x).hexdigest()==c['inputHashes']['fixture.json']
assert sorted([5,2,3])==[2,3,5]
manifest=Path(os.environ['STARCORE_FIELD173_CASE_OUTPUT'])
output=manifest.with_name('field173-'+c['caseId']+'-observed.json')
output.write_text(json.dumps({'fixtureOnly':True,'observed':7}))
scope={'role':'ENGINEERING_FIXTURE','target':None,'cutoff':None,'databaseIdentity':None,
       'notApplicableReason':'Host verifier fixture; no application or database is executed.'}
payload={k:c[k] for k in ('caseId','sourceCommit','sourceTree','planCommit','planSha256','batch','apkSha256','buildRun','executionId')}
payload.update(schema='STARCORE_FIELD173_CASE_OUTPUT_1',dataScope=scope,
               artifacts=[{'file':output.name,'kind':'OUTPUT','sha256':hashlib.sha256(output.read_bytes()).hexdigest()}])
manifest.write_text(json.dumps(payload))
print('FIELD173_ENGINEERING_EVENT '+json.dumps(dict(c,event='CASE_ASSERTIONS_COMPLETE',assertionCount=3,status='PASS')))
'''


class FieldGateTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.scratch=tempfile.TemporaryDirectory(prefix='field173-host-contract-')
        cls.root=Path(cls.scratch.name); cls.repo=cls.root/'source';cls.repo.mkdir()
        def put(name,data):
            p=cls.repo/name;p.parent.mkdir(parents=True,exist_ok=True)
            p.write_bytes(data if isinstance(data,bytes) else data.encode())
        cases=[{'id':'R343-001-'+suffix,'issue_id':'R343-001','batch':'B01',
                'level':'ENGINEERING','assertions':['fixture value','fixture input hash','fixture sort']}
               for suffix in ('POS','NEG','KEEP')]
        put(contract.PLAN_PATH+'/ACCEPTANCE_MATRIX.json',contract.canonical({'cases':cases}))
        put('fixture.json',b'{"value":7}\n');put('fixture_runner.py',SCRIPT)
        put('release/release_manifest.json',contract.canonical({'versionCode':577311174}))
        spec={'argv':[sys.executable,'fixture_runner.py'],'inputFiles':['fixture.json'],'timeoutSeconds':10}
        put(contract.RUNNERS,contract.canonical({'schema':'STARCORE_FIELD173_TEST_RUNNERS_1',
            'batch':'B01','cases':{c['id']:spec for c in cases}}))
        for args in [('init','-q'),('config','user.email','fixture@example.invalid'),
                     ('config','user.name','Host Fixture'),('add','.'),('commit','-qm','synthetic receipt gate fixture')]:
            subprocess.run(['git',*args],cwd=cls.repo,check=True,capture_output=True)
        source=binding.git(cls.repo,'rev-parse','HEAD').decode().strip()
        tree=binding.git(cls.repo,'rev-parse','HEAD^{tree}').decode().strip()
        cls.field={'schema':'STARCORE_FIELD173_BUILD_IDENTITY_1','sourceCommit':source,'sourceTree':tree,
                   'planCommit':'b'*40,'planSha256':'c'*64,'sourceBaseline':contract.BASELINE,
                   'batch':'B01','versionCode':577311174,'versionName':'R34.2-B01-2026-11-26-577311174',
                   'apkSeries':'R34.2','sourceApkEvidenceRef':{'versionCode':contract.BASELINE_CODE,'sha256':contract.BASELINE_APK}}
        cls.apk=cls.root/'candidate-bytes.fixture';cls.apk.write_bytes(b'HOST FIXTURE; NOT AN APK')
        cls.build={'sourceCommit':source,'sourceTree':tree,'fieldRepair173':cls.field,
                   'apkSha256':binding.sha(cls.apk),'ciRun':'123456','gradleRelease':'PASS'}
        cls.originals=cls.root/'originals'
        with patch.object(contract,'bound_field',return_value=cls.field):
            cls.receipt=runner.execute(cls.repo,cls.originals,cls.build,cls.apk)
        cls.files={p.name:p.read_bytes() for p in cls.originals.iterdir() if p.is_file()}

    @classmethod
    def tearDownClass(cls):cls.scratch.cleanup()

    def verify(self,receipt=None,files=None):
        return contract.verify_receipt(self.repo,self.field,receipt or self.receipt,
                (files or self.files).__getitem__,apk_sha=self.build['apkSha256'],run_id='123456')

    def reject(self,receipt=None,files=None):
        with self.assertRaises((ValueError,KeyError)):self.verify(receipt,files)

    def test_actual_host_driver_runs_and_retains_three_synthetic_cases(self):
        self.assertEqual(self.verify()['cases'],sorted(self.receipt['cases']))
        self.assertEqual([r['assertionCount'] for r in self.receipt['cases'].values()],[3,3,3])

    def test_wrong_source_plan_apk_run_or_batch_rejected(self):
        for key,value in [('sourceCommit','0'*40),('sourceTree','0'*40),('planSha256','0'*64),
                          ('planCommit','0'*40),('apkSha256','0'*64),('buildRun','999'),('batch','B02')]:
            with self.subTest(key=key):
                r=copy.deepcopy(self.receipt);r[key]=value;self.reject(r)

    def test_missing_and_old_regression_cases_rejected(self):
        r=copy.deepcopy(self.receipt);r['cases'].pop('R343-001-NEG');self.reject(r)
        r=copy.deepcopy(self.receipt);r['cases']['R342-001']=r['cases'].pop('R343-001-POS');self.reject(r)

    def test_nonzero_runner_exit_rejected(self):
        r=copy.deepcopy(self.receipt);r['cases']['R343-001-POS']['exitCode']=1;self.reject(r)

    def test_stale_command_or_missing_input_rejected(self):
        r=copy.deepcopy(self.receipt);r['cases']['R343-001-POS']['argv']=['true'];self.reject(r)
        r=copy.deepcopy(self.receipt);r['cases']['R343-001-POS']['inputOriginals']={};self.reject(r)

    def test_tampered_input_original_rejected_even_if_local_hash_changes(self):
        r=copy.deepcopy(self.receipt);files=dict(self.files)
        ref=r['cases']['R343-001-POS']['inputOriginals']['fixture.json']
        files[ref['file']]=b'{"value":8}\n';ref['sha256']=contract.digest(files[ref['file']]);self.reject(r,files)

    def test_self_written_pass_without_runner_markers_rejected(self):
        r=copy.deepcopy(self.receipt);files=dict(self.files);ref=r['cases']['R343-001-POS']['stdoutOriginal']
        files[ref['file']]=b'PASS\n';ref['sha256']=contract.digest(files[ref['file']]);self.reject(r,files)

    def test_zero_fractional_or_boolean_assertion_count_rejected(self):
        context=json.loads(self.files[self.receipt['cases']['R343-001-POS']['contextOriginal']['file']])
        for count in (0,1.5,True):
            data='\n'.join(contract.MARKER+json.dumps(dict(context,event=event,assertionCount=count,status='PASS'))
                           for event in ('CASE_ENTER','CASE_ASSERTIONS_COMPLETE')).encode()
            with self.subTest(count=count),self.assertRaises(ValueError):contract.events(data,context,3)

    def test_actual_event_wrong_execution_identity_rejected(self):
        context=json.loads(self.files[self.receipt['cases']['R343-001-POS']['contextOriginal']['file']])
        other=dict(context,executionId='f'*32)
        data='\n'.join(contract.MARKER+json.dumps(dict(other,event=e,assertionCount=3,status='PASS'))
                       for e in ('CASE_ENTER','CASE_ASSERTIONS_COMPLETE')).encode()
        with self.assertRaises(ValueError):contract.events(data,context,3)

    def test_missing_output_manifest_or_tampered_output_rejected(self):
        r=copy.deepcopy(self.receipt);r['cases']['R343-001-POS'].pop('outputManifestOriginal');self.reject(r)
        r=copy.deepcopy(self.receipt);files=dict(self.files)
        ref=r['cases']['R343-001-POS']['outputManifestOriginal'];output=json.loads(files[ref['file']])
        files[output['artifacts'][0]['file']]=b'changed';self.reject(r,files)

    def test_database_scope_requires_readback(self):
        row=self.receipt['cases']['R343-001-POS'];ctx=json.loads(self.files[row['contextOriginal']['file']])
        ref=copy.deepcopy(row['outputManifestOriginal']);files=dict(self.files);output=json.loads(files[ref['file']])
        output['dataScope']['databaseIdentity']='actual-fixture-db-id'
        files[ref['file']]=contract.canonical(output);ref['sha256']=contract.digest(files[ref['file']])
        with self.assertRaises(ValueError):contract.verify_case_output('R343-001-POS',ctx,ref,files.__getitem__)

    def test_catalog_and_runner_manifest_hashes_are_required(self):
        for key in ('catalogSha256','runnerManifestSha256'):
            r=copy.deepcopy(self.receipt);r[key]='0'*64;self.reject(r)

    def test_natural_or_device_credit_cannot_be_inferred(self):
        for key in ('physicalDevice','naturalQuality'):
            r=copy.deepcopy(self.receipt);r[key]='PASS';self.reject(r)

    def test_archive_requires_exact_173_cover_and_uid(self):
        reference=self.verify();reference['sha256']=contract.digest(self.files[contract.RECEIPT])
        cover={'status':'PASS','baselineSourceCommit':contract.BASELINE,
               'scope':'DISPOSABLE_ANDROID_API34_ORIGINAL_SIGNATURE',
               'before':{'uid':10001,'versionCode':contract.BASELINE_CODE,'apkSha256':contract.BASELINE_APK},
               'after':{'uid':10001,'versionCode':self.field['versionCode'],'apkSha256':self.build['apkSha256']}}
        def check(value):
            files=dict(self.files);files['cover-install.json']=contract.canonical(value)
            batch={'fieldRepair173':self.field,'fieldEngineeringEvidence':reference,
                   'originals':{name:contract.digest(raw) for name,raw in files.items()}}
            memory=io.BytesIO()
            with zipfile.ZipFile(memory,'w') as z:
                for name,raw in files.items():z.writestr(name,raw)
            with zipfile.ZipFile(io.BytesIO(memory.getvalue())) as z:
                return contract.verify_archive(self.repo,self.field,batch,z,apk_sha=self.build['apkSha256'],run_id='123456')
        self.assertEqual(check(cover)['status'],'ENGINEERING_CHECKS_PASSED')
        for category in ('oldBaseline','wrongUid','wrongBaselineSource'):
            bad=copy.deepcopy(cover)
            if category=='oldBaseline':bad['before']['versionCode']=577311152
            elif category=='wrongUid':bad['after']['uid']=10002
            else:bad['baselineSourceCommit']='0'*40
            with self.subTest(category=category),self.assertRaises(ValueError):check(bad)

    def test_bound_build_cannot_drop_nested_identity(self):
        owner=SimpleNamespace(identity=lambda *args:self.field)
        with patch.object(binding,'release_owner',return_value=owner):
            self.assertEqual(contract.bound_field(self.repo,build=self.build),self.field)
            broken=copy.deepcopy(self.build);broken.pop('fieldRepair173')
            with self.assertRaises(ValueError):contract.bound_field(self.repo,build=broken)

    def test_old_unpinned_source_does_not_require_field_owner(self):
        with tempfile.TemporaryDirectory() as scratch:
            repo=Path(scratch);(repo/'release').mkdir()
            (repo/'release/release_manifest.json').write_text('{"versionCode":577311173}')
            with patch.object(binding,'release_owner',side_effect=AssertionError('Old path must not load new owner')):
                self.assertIsNone(contract.bound_field(repo,identity={'r342':{}},build={}))

    def test_package_checks_exact173_and_preserves_legacy152(self):
        import package_ci_candidate as package
        common={'package':package.PACKAGE,'certificateSha256':package.CERT,'signatureVerified':True}
        formal=dict(common,versionCode=577311033,versionName='old033',sha256=package.BASE_APK)
        new=dict(common,versionCode=577311174,versionName=self.field['versionName'],sha256='d'*64)
        release={'versionCode':new['versionCode'],'versionName':new['versionName'],'applicationId':package.PACKAGE}
        installed=dict(common,versionCode=contract.BASELINE_CODE,sha256=contract.BASELINE_APK)
        expected=self.field['sourceApkEvidenceRef']
        self.assertEqual(package.validate_pair(release,formal,new,current_installed=installed,
                        expected_installed=expected),'APKS_COMPATIBLE_COVER_INSTALL_NOT_MEASURED')
        old=dict(common,versionCode=577311152,sha256='2afc220e8942f7eda678b7ddd09ca16c48225c2f4f99167a9233a5e9bf1a8c38')
        with self.assertRaisesRegex(ValueError,'EXACT_173_BASELINE_REQUIRED'):
            package.validate_pair(release,formal,new,current_installed=old,expected_installed=expected)
        self.assertEqual(package.validate_pair(release,formal,new,current_installed=old,
                        expected_installed={'versionCode':old['versionCode'],'sha256':old['sha256']}),
                         'APKS_COMPATIBLE_COVER_INSTALL_NOT_MEASURED')

    def test_embedded_full_identity_cannot_lose_field(self):
        import package_ci_candidate as package
        identity={'source_commit':self.field['sourceCommit'],'source_tree':self.field['sourceTree'],
                  'r342':{'sourceCommit':self.field['sourceCommit']},'fieldRepair173':self.field}
        old={'source_commit':identity['source_commit'],'source_tree':identity['source_tree'],'source_dirty':False}
        def make(path,current):
            with zipfile.ZipFile(path,'w') as z:
                z.writestr('assets/diagnostics/build-identity-r31.json',json.dumps(old))
                z.writestr('assets/diagnostics/build-identity-r334.json',json.dumps(current))
                z.writestr('assets/diagnostics/build-identity-r342.json',json.dumps(identity['r342']))
        with tempfile.TemporaryDirectory() as scratch:
            path=Path(scratch)/'synthetic-identity-container.zip';make(path,identity)
            package.embedded(path,identity)
            bad=copy.deepcopy(identity);bad.pop('fieldRepair173');make(path,bad)
            with self.assertRaisesRegex(ValueError,'EMBEDDED_R334_PLAN_OR_SOURCE_MISMATCH'):
                package.embedded(path,identity)

    def test_public_byte_collector_retains_field_identity(self):
        spec=importlib.util.spec_from_file_location('field_test_collect',ROOT/'tools/r341/publish_delivery_manifest.py')
        collect=importlib.util.module_from_spec(spec);spec.loader.exec_module(collect)
        body=b'HOST READBACK FIXTURE; NO PUBLICATION'
        candidate={'series':'R34.2','sourceCommit':self.field['sourceCommit'],'batch':'B01',
                   'planCommit':self.field['planCommit'],'planSha256':self.field['planSha256'],'buildRun':'123456',
                   'fieldRepair173':self.field,'fieldEngineeringEvidence':{'status':'ENGINEERING_CHECKS_PASSED'},
                   'legacyR342Plan':{'planCommit':'1'*40,'planSha256':'2'*64},
                   'artifacts':[{'filename':'fixture.txt','bytes':len(body),'sha256':contract.digest(body),
                    'url':'https://github.com/gtt46461-star/starcore-public-build/releases/download/r34.2-B01-'+self.field['sourceCommit'][:12]+'/fixture.txt'}]}
        with tempfile.TemporaryDirectory() as scratch,patch.object(collect.urllib.request,'urlopen',return_value=io.BytesIO(body)):
            result=collect.collect(candidate,Path(scratch)/'received','R34.2')
        self.assertEqual(result['fieldRepair173'],self.field)
        self.assertEqual(result['planSha256'],self.field['planSha256'])


if __name__=='__main__':unittest.main(verbosity=2)
