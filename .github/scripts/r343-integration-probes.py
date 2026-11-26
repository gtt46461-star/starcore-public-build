#!/usr/bin/env python3
"""Run existing cross-batch probes on the same original-signed B12 APK."""
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import uuid

ROOT = (Path(os.environ['GITHUB_WORKSPACE']) / 'app-source').resolve()
sys.path.insert(0, str(ROOT/'tools/r343'))
import run_native_field_case as native

CONTRACT = native.contract
BINDING = CONTRACT.binding
PACKAGE = 'com.ethk3.novaonepro.'
PROBES = (
    ('R343-004', 'R343RuntimeRepairInstrumentedTest', 'legitimateTempCacheFlushDoesNotBlockCheckpoint', False),
    ('R343-005', 'R343CircuitRotationInstrumentedTest', 'returningDependencyCannotExceedItsPreRotationBudget', True),
    ('R343-009', 'R343WriterRuntimeInstrumentedTest', 'fieldBackfillResumesAfterWriterFaultReadback', True),
    ('R343-014', 'Field173CoverageAndroidTest', 'actualSettlementAndScopedCoverageUseSeparateHashes', False),
    ('R343-015', 'r952.R343HistoryContinuityInstrumentedTest', 'verifiedBackfillAdvancesSourceWithoutChangingFrozenReplay', True),
    ('R343-015', 'r342.R342B04ContractTest', 'test_r342_012', False),
    ('R343-017', 'R343CheckpointAuditPagingInstrumentedTest', 'secondPinnedSnapshotVerifiesDeferredBodiesWithoutPromotingHistoricalBytes', True),
    ('R343-010', 'ShadowCycleAndroidTest', 'canonicalCommitReopensCaptureOnTheSameDraw', True),
    ('R343-043', 'ShadowCycleAndroidTest', 'eachPredrawSettlesOnceAndFeedsTheNextCycle', False),
    ('R343-021', 'r952.R343TrainingNativeTest', 'dependencyWaitKeepsOriginalTaskAndCursor', True),
    ('R343-023', 'r952.R343TrainingNativeTest', 'originalTerminalMissingCandidateStaysOnHold', True),
    ('R343-026', 'r952.R343RollingNativeTest', 'cleanupContinuesPastPinnedPageWithoutErasingHeadOrActiveInput', True),
    ('R343-026', 'r952.R343RetiredRollingOfferInstrumentedTest', 'retiredParentReleasesUnpublishedRawWithoutRetiringFormalOrQueuedOffers', True),
    ('R343-026', 'strategyv2.R26PublicationDeviceTest', 'tensorTrainingReceiptsFollowActualCandidateAndTerminalLiveCheckpointCas', False),
    ('R343-027', 'strategyv2.R26RuntimeDeviceTest', 'activeRuntimeOwnsModernContinuityAndCommitsItsLane', True),
    ('R343-027', 'r342.R342B07ContractTest', 'test_r342_025', False),
    ('R343-028', 'strategyv2.R343TypedRuleDonorAndroidTest', 'selectsOnlyEligiblePersistedDevelopmentDonors', False),
    ('R343-028', 'r342.R342B07ContractTest', 'test_r342_026', False),
    ('R343-030', 'r952.R343TeacherDependencyInstrumentedTest', 'r343_030_teacher_wait_preserves_foundation', True),
    ('R343-031', 'strategyv2.R343MatrixRetainedInstrumentedTest', 'r343_031_typed_matrix_readback', True),
    ('R343-034', 'R343RealAuthorityInstrumentedTest', 'r343_034_user_disabled_rejects_real', True),
    ('R343-040', 'Field173ChartGalleryCapture', 'galleryRefreshPreservesOriginalSourceRows', True),
    ('R343-042', 'R343LedgerAuditInstrumentedTest', 'preservesCurrentLedgerIdentityAndRejectsBrokenEvidence', True),
    ('R343-042', 'R343ReportCancellationInstrumentedTest', 'preservesCancellationTimeAtDurableRestartBoundary', True),
    ('R343-043', 'R343UniqueOwnerInstrumentedTest', 'existingCanonicalRealCannotBeIssuedByASecondGate', True),
)


BOOTSTRAP_REGRESSIONS = {
    'canonicalCommitReopensCaptureOnTheSameDraw',
    'eachPredrawSettlesOnceAndFeedsTheNextCycle',
    'activeRuntimeOwnsModernContinuityAndCommitsItsLane',
}


def original(directory, name, data):
    return native.write_original(directory/name, data)


def start_attachment(directory, field, apk_sha, run_id):
    receipt_path = directory/'batch-receipt.json'
    draft = receipt_path.read_bytes()
    receipt = json.loads(draft)
    original_files = receipt['originals']
    CONTRACT.require(receipt['status']=='ENGINEERING_CHECKS_PASSED' and receipt['fieldRepair173']==field
                     and receipt['apkSha256']==apk_sha and receipt['buildRun']==run_id,
                     'R343_INTEGRATED_BATCH_RECEIPT_IDENTITY')
    CONTRACT.require(set(p.name for p in directory.iterdir()) == set(original_files) | {receipt_path.name},
                     'R343_INTEGRATED_DRAFT_ATTACHMENT_FILE_SET')
    CONTRACT.require(all(BINDING.sha(directory/name)==digest for name,digest in original_files.items()),
                     'R343_INTEGRATED_DRAFT_ATTACHMENT_SHA')
    return draft, receipt


def finish_attachment(directory, draft, receipt):
    receipt_path = directory/'batch-receipt.json'
    CONTRACT.require(receipt_path.read_bytes()==draft and all(
        BINDING.sha(directory/name)==digest for name,digest in receipt['originals'].items()),
        'R343_INTEGRATED_BASE_ORIGINAL_CHANGED')
    original(directory, 'r343-integrated-batch-receipt-before.json', draft)
    additions = sorted(p for p in directory.iterdir() if p.name not in receipt['originals'] and p!=receipt_path)
    CONTRACT.require(all(p.is_file() and not p.is_symlink() and re.fullmatch(
        r'(?:field173-R343-INTEGRATION-[0-9]{2}-[a-zA-Z0-9_.-]+|r343-integrated-[a-z-]+\.json)',p.name)
        for p in additions), 'R343_INTEGRATED_UNEXPECTED_ATTACHMENT_MEMBER')
    updated = dict(receipt, originals={**receipt['originals'], **{p.name:BINDING.sha(p) for p in additions}})
    staging = directory/'batch-receipt.json.next'
    original(directory, staging.name, CONTRACT.canonical(updated))
    os.replace(staging, receipt_path)
    print(f'INTEGRATED_ORIGINALS_BOUND {len(additions)}',flush=True)


def run_probe(directory, serial, apk, field, run_id, index, probe):
    issue, name, method, instrumented = probe
    cls = PACKAGE + name
    source_path = 'app/src/androidTest/java/' + cls.replace('.', '/') + '.java'
    case_id = f'R343-INTEGRATION-{index:02d}'
    expected = CONTRACT.expected_scope(field, case_id, BINDING.sha(apk), run_id)
    expected['executionId'] = uuid.uuid4().hex
    stem = f'field173-{case_id}'
    source = CONTRACT.committed_bytes(ROOT, field['sourceCommit'], source_path)
    CONTRACT.require(method.encode() in source and b'@Test' in source, 'R343_PROBE_SOURCE_METHOD:' + method)
    source_ref = original(directory, stem+'-source.java', source)
    context_path = directory / (stem+'-context.json')
    context = dict(expected, originalIssue=issue, sourcePath=source_path, inputSha256=source_ref['sha256'])
    context_ref = original(directory, context_path.name, CONTRACT.canonical(context))
    output = directory / (stem+'-output.json')
    environment = dict(os.environ, STARCORE_FIELD173_CASE_CONTEXT=str(context_path),
                       STARCORE_FIELD173_CANDIDATE_APK=str(apk), STARCORE_FIELD173_CASE_OUTPUT=str(output))
    selector = cls + '#' + method
    if instrumented:
        argv = [sys.executable, '-B', str(ROOT/'tools/r343/run_native_field_case.py'), selector]
    else:
        argv = native.adb(serial, 'shell','am','instrument','-w','-r','-e','class',selector,
                          '-e','r30Sandbox','R343_'+expected['executionId'],native.RUNNER)
    command = original(directory, stem+'-argv.json', CONTRACT.canonical(argv))
    try:
        result = subprocess.run(argv, cwd=ROOT, env=environment, capture_output=True, timeout=600, check=False)
        exit_code, stdout, stderr = result.returncode, result.stdout, result.stderr
    except subprocess.TimeoutExpired as fault:
        exit_code, stdout, stderr = 124, fault.stdout or b'', fault.stderr or b''
        subprocess.run(native.adb(serial, 'shell','am','force-stop',native.PACKAGE),timeout=15,check=False)
    stdout_ref = original(directory, stem+'-stdout.log', stdout)
    stderr_ref = original(directory, stem+'-stderr.log', stderr)
    failure = None
    assertions = None
    readback = None
    ok = exit_code == 0 and re.search(rb'OK \(1 test\)',stdout) and not re.search(rb'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed',stdout)
    if not ok:
        failure = 'ANDROID_TEST_DID_NOT_COMPLETE:' + str(exit_code)
    elif instrumented:
        try:
            assertions = CONTRACT.events(stdout, expected, 1)
            readback = {'file': output.name, 'sha256': BINDING.sha(output)}
            CONTRACT.verify_case_output(case_id, expected, readback, CONTRACT.directory_reader(directory))
        except (KeyError, OSError, ValueError) as fault:
            failure = str(fault)
    disposition = 'FAIL' if failure else 'PASS'
    print(f'INTEGRATED_NATIVE {issue} {selector} {disposition} assertions={assertions}',flush=True)
    if failure:
        excerpt = (stderr + b'\n' + stdout).decode('utf-8',errors='replace').splitlines()
        for line in [line for line in excerpt if re.search(r'Error|Exception|Expected|actual|FAILURES|R343_STARTUP_',line)][-24:]:
            limit = 2400 if "R343_STARTUP_" in line else 500
            print(line[:limit],flush=True)
    return dict(expected, issue=issue, selector=selector, status=disposition, failure=failure,
                assertionCount=assertions, junitTests=1 if ok else None, exitCode=exit_code,
                source=source_ref,context=context_ref,command=command,stdout=stdout_ref,stderr=stderr_ref,readback=readback)


def main():
    run_temp = Path(os.environ['RUNNER_TEMP']).resolve()
    output = run_temp/'engineering'
    build = BINDING.read(run_temp/'build/CI_BUILD_RECEIPT.json')
    field = CONTRACT.bound_field(ROOT, build=build)
    CONTRACT.require(os.environ['STAGE']=='B12' and field['batch']=='B12'
                     and build['gradleRelease']=='PASS' and build['sourceClean'] and build['sourceUnchangedDuringBuild'],
                     'R343_INTEGRATED_NATIVE_REQUIRES_EXACT_B12_BUILD')
    apk, = sorted((ROOT/'app/build/outputs/apk/release').glob('*.apk'))
    apk_sha = BINDING.sha(apk)
    run_id = os.environ['GITHUB_RUN_ID']
    CONTRACT.require(build['apkSha256']==apk_sha and build['ciRun']==run_id, 'R343_INTEGRATED_NATIVE_BUILD_CHANGED')
    serial = subprocess.check_output(['adb','get-serialno'], text=True, timeout=20).strip()
    CONTRACT.require(re.fullmatch(r'emulator-[0-9]+', serial), 'R343_DISPOSABLE_ANDROID_REQUIRED')
    early = run_temp/'engineering-preflight'
    selectors = dict(enumerate(PROBES, 1))
    early_indexes = {i for i, p in selectors.items() if p[2] in BOOTSTRAP_REGRESSIONS}
    if sys.argv[1:] == ['--startup-first']:
        early.mkdir()
        native.ensure_candidate_installed(serial, apk, apk_sha)
        try:
            initial = []
            order = sorted(early_indexes, key=lambda i: (
                selectors[i][2] != 'activeRuntimeOwnsModernContinuityAndCommitsItsLane', i))
            for i in order:
                case = run_probe(early, serial, apk, field, run_id, i, selectors[i])
                initial.append(case)
                if case['status'] != 'PASS':
                    break
            checks = {p.name:BINDING.sha(p) for p in early.iterdir() if p.is_file()}
            original(early, 'bootstrap-originals.json', CONTRACT.canonical({
                'field': field, 'apkSha256': apk_sha, 'run': run_id,
                'cases': initial, 'originals': checks,
            }))
            CONTRACT.require(all(r['status']=='PASS' for r in initial), 'R343_BOOTSTRAP_REGRESSION_FAILED')
        finally:
            # The original cover-upgrade enters from a clean emulator and installs 173 itself.
            for package in (native.PACKAGE+'.test', native.PACKAGE):
                subprocess.run(native.adb(serial, 'uninstall', package), check=True, timeout=45)
        return

    draft, batch_receipt = start_attachment(output, field, apk_sha, run_id)
    native.ensure_candidate_installed(serial, apk, apk_sha)
    replayed = {}
    if early.is_dir():
        record = BINDING.read(early/'bootstrap-originals.json')
        CONTRACT.require(record['field']==field and record['apkSha256']==apk_sha and record['run']==run_id,
                         'R343_BOOTSTRAP_IDENTITY')
        replayed = {int(r['caseId'].rsplit('-',1)[1]):r for r in record['cases']}
        CONTRACT.require(set(replayed)==early_indexes and all(r['status']=='PASS' for r in replayed.values()),
                         'R343_BOOTSTRAP_CASE_SET')
        CONTRACT.require(set(p.name for p in early.iterdir())==set(record['originals'])|{'bootstrap-originals.json'},
                         'R343_BOOTSTRAP_ORIGINAL_SET')
        for name, digest in record['originals'].items():
            CONTRACT.require(re.fullmatch(r'field173-R343-INTEGRATION-[0-9]{2}-[a-zA-Z0-9_.-]+',name)
                             and BINDING.sha(early/name)==digest and not (output/name).exists(),
                             'R343_BOOTSTRAP_ORIGINAL_SHA')
            shutil.copyfile(early/name, output/name)
    result = [replayed[i] if i in replayed else run_probe(output, serial, apk, field, run_id, i, probe)
              for i,probe in selectors.items()]
    assert native.installed_apk_sha(serial) == apk_sha
    passed = sum(row['status']=='PASS' for row in result)
    receipt = {'schema':'STARCORE_FIELD173_INTEGRATED_NATIVE_PROBES_1','fieldRepair173':field,
               'apkSha256':apk_sha,'buildRun':run_id,'deviceScope':'DISPOSABLE_ANDROID_API_34',
               'originalCaseGateCoverage':'SEPARATE','passed':passed,'total':len(result),'probes':result}
    original(output, 'r343-integrated-native-probes.json', CONTRACT.canonical(receipt))
    CONTRACT.require(passed==len(result), 'R343_INTEGRATED_NATIVE_PROBES_FAILED:' + str(len(result)-passed))
    finish_attachment(output, draft, batch_receipt)


if __name__ == '__main__':
    main()
