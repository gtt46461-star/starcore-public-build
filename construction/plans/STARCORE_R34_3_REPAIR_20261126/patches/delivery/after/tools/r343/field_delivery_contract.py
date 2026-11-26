"""Exact field-173 engineering evidence, shared by CI, packaging and publication.

This contract verifies original runner observations. It never creates an engineering
PASS from a plan entry and grants neither physical-device nor natural-quality credit.
"""
from pathlib import Path, PurePosixPath
import hashlib
import json
import re

import verify_plan_binding as binding

BASELINE = '4d1de9d83692635b5c5ce39144fece1c8a31c8ac'
BASELINE_CODE = 577311173
BASELINE_APK = '9304472262455c12b4600708fb23480d698acf9f3ba710ddee86cda214ae8750'
PLAN_PATH = 'construction/plans/STARCORE_R34_3_REPAIR_20261126'
RUNNERS = 'release/field173_test_runners.json'
RECEIPT = 'field173-engineering-receipt.json'
RUNNER_ORIGINAL = 'field173-test-runners.json'
MARKER = 'FIELD173_ENGINEERING_EVENT '
MAX_ORIGINAL_BYTES = 16 << 20
require = binding.require


def digest(data):
    return hashlib.sha256(data).hexdigest()


def canonical(value):
    return (json.dumps(value, ensure_ascii=False, sort_keys=True, indent=2)+'\n').encode()


def bound_field(repo, *, identity=None, build=None):
    repo = Path(repo).resolve()
    release = binding.read(repo/'release/release_manifest.json')
    advertised = (identity or {}).get('fieldRepair173') or (build or {}).get('fieldRepair173')
    active = (repo/'release/field173_plan_binding.json').exists() or release.get('versionCode', 0) > BASELINE_CODE
    if not active and advertised is None:
        return None
    require(active, 'FIELD173_UNBOUND_IDENTITY')
    source = binding.git(repo, 'rev-parse', 'HEAD').decode().strip()
    tree = binding.git(repo, 'rev-parse', 'HEAD^{tree}').decode().strip()
    field = binding.release_owner(repo, 'r343').identity(repo, source, tree, release)
    for record, source_key, tree_key in ((identity, 'source_commit', 'source_tree'),
                                         (build, 'sourceCommit', 'sourceTree')):
        if record is not None:
            require(record.get('fieldRepair173') == field
                    and record.get(source_key) == source and record.get(tree_key) == tree,
                    'FIELD173_BUILD_IDENTITY_MISMATCH')
    return field


def baseline(field):
    require(field['sourceBaseline'] == BASELINE
            and field['sourceApkEvidenceRef'] == {'versionCode':BASELINE_CODE, 'sha256':BASELINE_APK},
            'FIELD173_BASELINE_IDENTITY_MISMATCH')
    return {'versionCode':BASELINE_CODE, 'sha256':BASELINE_APK,
            'sourceCommit':BASELINE, 'file':'baseline173.apk'}


def catalog(repo, field):
    path = binding.safe(Path(repo)/PLAN_PATH, 'ACCEPTANCE_MATRIX.json')
    raw = path.read_bytes()
    rows = json.loads(raw)['cases']
    engineering = [r for r in rows if r['level'] == 'ENGINEERING']
    require(engineering and len({r['id'] for r in engineering}) == len(engineering),
            'FIELD173_CASE_CATALOG_IDENTITY')
    selected = {r['id']:r for r in engineering if r['batch'] == field['batch']}
    require(selected and all(re.fullmatch(r'R343-[0-9]{3}-(POS|NEG|KEEP)', k)
                            and r['issue_id'] == k.rsplit('-', 1)[0]
                            and isinstance(r.get('assertions'), list) and r['assertions']
                            and all(isinstance(a, str) and a for a in r['assertions'])
                            for k, r in selected.items()), 'FIELD173_CURRENT_CASE_SET_EMPTY_OR_INVALID')
    return selected, digest(raw)


def pending_device_and_natural(repo):
    rows = binding.read(binding.safe(Path(repo)/PLAN_PATH,'ACCEPTANCE_MATRIX.json'))['cases']
    selected = [r for r in rows if r['level'] != 'ENGINEERING']
    require(all(r['level'] in ('DEVICE','NATURAL') for r in selected), 'FIELD173_ACCEPTANCE_LEVEL')
    return {r['id']:{'batch':r['batch'],'level':r['level'],
                     'status':'NOT_RUN_BY_ENGINEERING_DRIVER'} for r in selected}


def committed_bytes(repo, source, name):
    require(isinstance(name, str) and name and not name.startswith('/') and '\\' not in name
            and PurePosixPath(name).as_posix() == name and '..' not in PurePosixPath(name).parts,
            'FIELD173_SOURCE_INPUT_PATH')
    size = binding.git(repo, 'cat-file', '-s', source+':'+name).decode().strip()
    require(size.isdecimal() and int(size) <= MAX_ORIGINAL_BYTES, 'FIELD173_INPUT_CAPACITY')
    return binding.git(repo, 'show', source+':'+name)


def runner_catalog(repo, field, cases):
    raw = committed_bytes(repo, field['sourceCommit'], RUNNERS)
    data = json.loads(raw)
    require(set(data) == {'schema', 'batch', 'cases'}
            and data['schema'] == 'STARCORE_FIELD173_TEST_RUNNERS_1'
            and data['batch'] == field['batch'] and set(data['cases']) == set(cases),
            'FIELD173_EXACT_CURRENT_RUNNER_SET_REQUIRED')
    for case_id, spec in data['cases'].items():
        require(set(spec) == {'argv', 'inputFiles', 'timeoutSeconds'}
                and isinstance(spec['argv'], list) and 1 <= len(spec['argv']) <= 128
                and all(isinstance(a,str) and a and '\x00' not in a and len(a) <= 4096 for a in spec['argv'])
                and isinstance(spec['inputFiles'], list) and 1 <= len(spec['inputFiles']) <= 64
                and len(set(spec['inputFiles'])) == len(spec['inputFiles'])
                and type(spec['timeoutSeconds']) is int and 1 <= spec['timeoutSeconds'] <= 1200,
                'FIELD173_RUNNER_SPEC_INVALID:'+case_id)
        for name in spec['inputFiles']:
            committed_bytes(repo, field['sourceCommit'], name)
    return data['cases'], raw


def original(read_bytes, name, expected=None):
    require(isinstance(name,str) and re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]{0,240}', name),
            'FIELD173_ORIGINAL_NAME_INVALID')
    data = read_bytes(name)
    require(len(data) <= MAX_ORIGINAL_BYTES, 'FIELD173_ORIGINAL_CAPACITY:'+name)
    if expected is not None:
        require(re.fullmatch('[0-9a-f]{64}', str(expected)) and digest(data) == expected,
                'FIELD173_ORIGINAL_DIGEST_MISMATCH:'+name)
    return data


def directory_reader(root):
    root=Path(root).resolve()
    def read(name):
        path=binding.safe(root,name)
        require(path.is_file() and path.stat().st_size <= MAX_ORIGINAL_BYTES,
                'FIELD173_ORIGINAL_CAPACITY:'+name)
        return path.read_bytes()
    return read


def archive_reader(archive):
    def read(name):
        require(archive.getinfo(name).file_size <= MAX_ORIGINAL_BYTES,
                'FIELD173_ORIGINAL_CAPACITY:'+name)
        return archive.read(name)
    return read


def expected_scope(field, case_id, apk_sha, run_id):
    return {'caseId':case_id, 'sourceCommit':field['sourceCommit'], 'sourceTree':field['sourceTree'],
            'planCommit':field['planCommit'], 'planSha256':field['planSha256'],
            'batch':field['batch'], 'apkSha256':apk_sha, 'buildRun':run_id}


def events(stdout, context, minimum):
    observed=[]
    for line in stdout.decode('utf-8', errors='strict').splitlines():
        _, found, value = line.partition(MARKER)
        if not found:
            continue
        row=json.loads(value)
        require(all(row.get(k) == context[k] for k in
                    ('caseId','sourceCommit','sourceTree','planCommit','planSha256','batch',
                     'apkSha256','buildRun','executionId')), 'FIELD173_RUNNER_EVENT_IDENTITY')
        observed.append(row)
    require([r.get('event') for r in observed] == ['CASE_ENTER','CASE_ASSERTIONS_COMPLETE'],
            'FIELD173_ACTUAL_ASSERTIONS_NOT_OBSERVED')
    count=observed[-1].get('assertionCount')
    require(type(count) is int and count >= max(1,minimum)
            and observed[-1].get('status') == 'PASS', 'FIELD173_ASSERTIONS_FAILED_OR_EMPTY')
    return count


def verify_case_output(case_id, context, reference, read_bytes):
    data=original(read_bytes,reference['file'],reference['sha256'])
    report=json.loads(data)
    require(report.get('schema') == 'STARCORE_FIELD173_CASE_OUTPUT_1'
            and all(report.get(k) == context[k] for k in
                    ('caseId','sourceCommit','sourceTree','planCommit','planSha256','batch',
                     'apkSha256','buildRun','executionId')),
            'FIELD173_OUTPUT_IDENTITY:'+case_id)
    scope=report.get('dataScope',{})
    require(set(scope) == {'role','target','cutoff','databaseIdentity','notApplicableReason'}
            and isinstance(scope['role'],str) and 1 <= len(scope['role']) <= 64
            and (scope['target'] is None or isinstance(scope['target'],str) and 1 <= len(scope['target']) <= 64)
            and (scope['cutoff'] is None or type(scope['cutoff']) is int and scope['cutoff'] >= 0)
            and (scope['databaseIdentity'] is None or isinstance(scope['databaseIdentity'],str)
                 and 1 <= len(scope['databaseIdentity']) <= 512)
            and isinstance(scope['notApplicableReason'],str)
            and (all(scope[k] is not None for k in ('target','cutoff','databaseIdentity'))
                 or 1 <= len(scope['notApplicableReason']) <= 512), 'FIELD173_DATA_SCOPE_REQUIRED:'+case_id)
    artifacts=report.get('artifacts')
    require(isinstance(artifacts,list) and 1 <= len(artifacts) <= 64
            and len({r['file'] for r in artifacts}) == len(artifacts)
            and all(r.get('kind') in ('OUTPUT','READBACK') for r in artifacts),
            'FIELD173_OUTPUT_ARTIFACTS_REQUIRED:'+case_id)
    if scope['databaseIdentity'] is not None:
        require(any(r['kind'] == 'READBACK' for r in artifacts), 'FIELD173_DATABASE_READBACK_REQUIRED:'+case_id)
    for artifact in artifacts:
        require(artifact['file'].startswith('field173-'+case_id+'-'),
                'FIELD173_OUTPUT_BELONGS_TO_OTHER_CASE:'+case_id)
        require(original(read_bytes,artifact['file'],artifact['sha256']),
                'FIELD173_EMPTY_OUTPUT:'+case_id)
    return report


def verify_receipt(repo, field, receipt, read_bytes, *, apk_sha, run_id):
    cases, catalog_sha = catalog(repo, field)
    specs, runner_raw = runner_catalog(repo, field, cases)
    require(receipt.get('schema') == 'STARCORE_FIELD173_BATCH_ENGINEERING_1'
            and receipt.get('fieldRepair173') == field
            and receipt.get('sourceCommit') == field['sourceCommit']
            and receipt.get('sourceTree') == field['sourceTree']
            and receipt.get('planCommit') == field['planCommit']
            and receipt.get('planSha256') == field['planSha256']
            and receipt.get('batch') == field['batch']
            and receipt.get('apkSha256') == apk_sha and receipt.get('buildRun') == run_id
            and receipt.get('catalogSha256') == catalog_sha
            and receipt.get('runnerManifestSha256') == digest(runner_raw)
            and receipt.get('status') == 'ENGINEERING_CHECKS_PASSED'
            and receipt.get('physicalDevice') == 'NOT_INFERRED'
            and receipt.get('naturalQuality') == 'NOT_PROVEN'
            and receipt.get('pendingDeviceAndNatural') == pending_device_and_natural(repo)
            and isinstance(receipt.get('cases'),dict) and set(receipt['cases']) == set(cases),
            'FIELD173_ENGINEERING_RECEIPT_IDENTITY_OR_CASE_SET')
    require(original(read_bytes,RUNNER_ORIGINAL,digest(runner_raw)) == runner_raw,
            'FIELD173_RUNNER_ORIGINAL_SOURCE_MISMATCH')
    for case_id, row in receipt['cases'].items():
        spec=specs[case_id]
        require(row.get('argv') == spec['argv'] and type(row.get('exitCode')) is int
                and row['exitCode'] == 0 and row.get('status') == 'PASS',
                'FIELD173_RUNNER_EXIT_OR_COMMAND:'+case_id)
        require(set(row.get('inputOriginals',{})) == set(spec['inputFiles']),
                'FIELD173_INPUT_SET:'+case_id)
        inputs={}
        for name, ref in row['inputOriginals'].items():
            expected=committed_bytes(repo,field['sourceCommit'],name)
            require(original(read_bytes,ref['file'],ref['sha256']) == expected,
                    'FIELD173_INPUT_SOURCE_MISMATCH:'+case_id)
            inputs[name]=digest(expected)
        ctx=row['contextOriginal']; context=json.loads(original(read_bytes,ctx['file'],ctx['sha256']))
        expected=expected_scope(field,case_id,apk_sha,run_id)
        require(context.get('schema') == 'STARCORE_FIELD173_CASE_CONTEXT_1'
                and all(context.get(k) == v for k,v in expected.items())
                and re.fullmatch('[0-9a-f]{32}',str(context.get('executionId')))
                and context.get('inputHashes') == inputs,
                'FIELD173_INPUT_CONTEXT_IDENTITY:'+case_id)
        argv_ref=row['argvOriginal']
        require(json.loads(original(read_bytes,argv_ref['file'],argv_ref['sha256'])) == spec['argv'],
                'FIELD173_ARGV_ORIGINAL_MISMATCH:'+case_id)
        stdout=row['stdoutOriginal']; stderr=row['stderrOriginal']
        observed=events(original(read_bytes,stdout['file'],stdout['sha256']),context,len(cases[case_id]['assertions']))
        original(read_bytes,stderr['file'],stderr['sha256'])
        verify_case_output(case_id,context,row['outputManifestOriginal'],read_bytes)
        require(type(row.get('assertionCount')) is int and row['assertionCount'] == observed,
                'FIELD173_ASSERTION_COUNT_MISMATCH:'+case_id)
    return {'file':RECEIPT,'sha256':digest(canonical(receipt)),
            'cases':sorted(cases),'status':'ENGINEERING_CHECKS_PASSED',
            'physicalDevice':'NOT_INFERRED','naturalQuality':'NOT_PROVEN'}


def verify_archive(repo, field, batch, archive, *, apk_sha, run_id):
    require(batch.get('fieldRepair173') == field, 'FIELD173_BATCH_IDENTITY_REQUIRED')
    require(RECEIPT in archive.namelist(), 'FIELD173_ENGINEERING_ORIGINALS_REQUIRED')
    read=archive_reader(archive)
    raw=original(read,RECEIPT); receipt=json.loads(raw)
    result=verify_receipt(repo,field,receipt,read,apk_sha=apk_sha,run_id=run_id)
    result['sha256']=digest(raw)
    require(batch.get('fieldEngineeringEvidence') == result
            and batch.get('originals',{}).get(RECEIPT) == result['sha256'],
            'FIELD173_BATCH_EVIDENCE_REFERENCE_MISMATCH')
    cover=json.loads(original(read,'cover-install.json',batch['originals'].get('cover-install.json')))
    before,after=cover.get('before',{}),cover.get('after',{})
    require(cover.get('status') == 'PASS' and cover.get('baselineSourceCommit') == BASELINE
            and cover.get('scope') == 'DISPOSABLE_ANDROID_API34_ORIGINAL_SIGNATURE'
            and before.get('versionCode') == BASELINE_CODE and before.get('apkSha256') == BASELINE_APK
            and after.get('versionCode') == field['versionCode'] and after.get('apkSha256') == apk_sha
            and type(before.get('uid')) is int and before['uid'] == after.get('uid'),
            'FIELD173_EXACT_COVER_INSTALL_RECEIPT_REQUIRED')
    return result
