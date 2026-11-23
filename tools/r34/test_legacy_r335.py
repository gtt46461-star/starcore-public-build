"""Resolve all R33.5 issue IDs against the current R34 owner and assertion registries."""
import argparse
import json
import os
import re
import shutil
import tempfile
import fcntl
from contextlib import contextmanager
import subprocess
import sys
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path


def check(plan, legacy):
    old = json.loads((legacy / 'WORK_ORDERS.json').read_bytes())
    owners = json.loads((plan / 'WORK_ORDERS.json').read_bytes())['issues']
    new_ids = {r['id'] for r in owners}
    entries = json.loads((plan / 'R335_CROSSWALK.json').read_bytes())['requirements']
    old_ids = {r['id'] for r in old['issues']}
    if len(entries) != 66 or len(old_ids) != 66 or {r['legacy_id'] for r in entries} != old_ids:
        raise ValueError('R34_LEGACY_ISSUE_SET')
    for r in entries:
        if not r['new_issue_ids'] or not set(r['new_issue_ids']) <= new_ids:
            raise ValueError('R34_LEGACY_OWNER:' + r['legacy_id'])
        if not r['mandatory_assertion_ids'] or not r['test_contracts'] or not r['retained_contract']:
            raise ValueError('R34_LEGACY_TEST_CONTRACT:' + r['legacy_id'])
    return {'retainedIssueMappings':len(entries),'newOwnerIssues':len(new_ids),
            'scope':'CHECKED_SOURCE_CONTRACT_LINKS'}


from legacy_release_cases import BY_ID, CASES
from legacy_behavior_cases import PROBES_BY_CASE, digest, run_logged, run_original


# These complement the named original assertions. Their results retain their
# actual test names and never certify an entire multifaceted field requirement.
ADDITIONAL_SUITES = {
    'C01': ['ResearchRegistrationR34Test', 'ReportShareLeaseR34Test'],
    'C02': ['R31ReplayCheckpointEnvelopeTest', 'R31ReplayInputIdentityTest'],
    'C03': ['AtomNetTrainingEvidenceR34Test', 'R31AtomNetCompleteIssueConsumerTest'],
    'C04': ['R27BirthGateTest', 'R30CapabilityRegistryTest'],
    'C06': ['R31StorageCheckpointRebuildTest', 'ReportShareLeaseR34Test'],
    'RT01': ['ReportShareLeaseR34Test', 'R31StorageGovernanceTest'],
    'RT02': ['R31FaultObservationContractTest'], 'RT03': ['R32CheckpointRawStrictTest'],
    'RT04': ['R32CheckpointRawStrictTest', 'R31ReplayCheckpointEnvelopeTest'],
    'RT05': ['AtomNetTrainingEvidenceR34Test', 'RepairModelReplayR30Test'],
    'RT06': ['R31SettlementCoverageColumnsTest', 'RepairIssueTimelineR30Test'],
    'RT07': ['RepairCommitGuardsR30Test', 'R31WriterOutcomeCountsTest'],
    'RT08': ['HotHistoryWindowR335Test', 'R31StorageGrowthBoundTest'],
    'RT09': ['R32CheckpointRawStrictTest', 'R31StorageCheckpointRebuildTest'],
    'RT10': ['R31KeepMigrationTest', 'R32CheckpointRawStrictTest'],
    'P01': ['R31PreparedWriteContractTest', 'R31ReplayCheckpointEnvelopeTest'],
    'P02': ['R31PreparedWriteContractTest', 'RepairCommitGuardsR30Test'],
    'P03': ['R31InjectedCauseChainContractTest', 'ResearchRegistrationR34Test'],
    'RE01': ['R335BirthSchedulingCostTest', 'RepairTaskProgressR30Test'],
    'RE02': ['RepairMatrixEvidenceR30Test', 'RepairResearchFrameBoundR30Test'],
    'RE03': ['RepairModelReplayR30Test', 'RepairCalibrationRoleR30Test'],
    'RE04': ['RepairStructureStudyR30Test', 'RepairSetIntersectionR30Test'],
    'RE05': ['RepairDirectedResearchR30Test', 'R30FailureRepairTest'],
    'RE06': ['FusionCoverageR34Test', 'RepairFusionStudyR30Test'],
    'RE07': ['R31RuntimeMemoryControlTest', 'RepairTaskProgressR30Test'],
    'RE08': ['ResearchRegistrationR34Test'], 'RE09': ['RepairPairedResearchR30Test'],
    'RE10': ['R30DiagnosisRulesTest'], 'RE11': ['RepairTaskProgressR30Test'],
    'RE12': ['R30BrierContributionTest'], 'RE13': ['MatrixReportReferenceR335Test'],
    'RE14': ['R30ChartSnapshotTest', 'RepairMatrixEvidenceR30Test'],
    'RE15': ['ResearchWorkbenchProjectionR335Test'], 'RE16': ['AtomNetTrainingEvidenceR34Test'],
    'RE18': ['R30CapabilityRegistryTest', 'R27BirthGateTest'],
    'RE19': ['FutureCoverageR34Test', 'R27QualityTest', 'R30CertificateVersionTest'],
    'RE20': ['R30CapabilityRegistryTest', 'R31ResearchCapabilityExperimentTest'],
    'RE21': ['EnabledWindowPreferenceR335Test', 'FutureCoverageR34Test'],
    'RE23': ['R335BirthSchedulingCostTest'], 'RE24': ['RepairReportIndexR30Test', 'R31ProductionReportTest'],
    'D06': ['EnabledWindowPreferenceR335Test', 'FutureCoverageR34Test'],
    'D10': ['RepairPairedResearchR30Test'], 'D11': ['R31VerifiedImprovementTest'],
    'D12': ['Batch4ContractsR930Test', 'RepairPairedResearchR30Test'],
}


def source_id(root):
    def git(*args):
        return subprocess.check_output(['git', '--no-replace-objects', '-C', str(root), *args], text=True).strip()
    if git('status', '--porcelain', '--untracked-files=no'):
        raise ValueError('DIRTY_TRACKED_SOURCE')
    return {'commit': git('rev-parse', 'HEAD'), 'tree': git('rev-parse', 'HEAD^{tree}')}


def read_jobs(plan, source):
    binding = json.loads((source / 'release/r34_plan_binding.json').read_bytes())
    seal = plan / 'MANIFEST_SHA256.json'
    if digest(seal) != binding['planSha256']:
        raise ValueError('PLAN_SEAL_MISMATCH')
    listing = json.loads(seal.read_bytes())['files']
    entry, = [x for x in listing if x['path'] == 'R335_CROSSWALK.json']
    file = plan / entry['path']
    if digest(file) != entry['sha256'] or file.stat().st_size != entry['bytes']:
        raise ValueError('CROSSWALK_CONTENT_CHANGED')
    rows = json.loads(file.read_bytes())['requirements']
    if len(rows) != len({row['legacy_id'] for row in rows}) or len(rows) != 66:
        raise ValueError('CROSSWALK_ID_CARDINALITY')
    return {row['legacy_id']: row for row in rows}


def suite_names(source, jobs):
    named = {}
    for file in (source / 'app/src/test/java').rglob('*.java'):
        fqcn = '.'.join(file.relative_to(source / 'app/src/test/java').with_suffix('').parts)
        if file.stem in named:
            named[file.stem] = None
        else:
            named[file.stem] = fqcn
    result = {}
    for job in jobs:
        original = []
        android = []
        for item in job['test_contracts']:
            if item['state'] == 'EXISTING_ENTRY_REQUIRES_REVALIDATION':
                if ':app:connectedDebugAndroidTest' in item['command']:
                    method = item['command'].rsplit('=', 1)[1]
                    if not re.fullmatch(r'[\w.]+#[\w]+', method):
                        raise ValueError('INVALID_INSTRUMENTATION_METHOD')
                    android.append(method)
                    continue
                full = item['command'].split('--tests ', 1)[1]
                if not re.fullmatch(r'[\w.]+', full) or full.rsplit('.',1)[-1] != item['method']:
                    raise ValueError('INVALID_ORIGINAL_METHOD:' + job['legacy_id'])
                original.append(full)
        short = job['legacy_id'].removeprefix('R335-')
        more = [named[n] for n in ADDITIONAL_SUITES.get(short, ())]
        if None in more:
            raise ValueError('AMBIGUOUS_SUITE_NAME')
        result[job['legacy_id']] = original, more, android
    return result


def selected_results(source, output, names):
    # --rerun applies only to the test task. A cache hit or an old XML cannot
    # satisfy this run. Gradle uses its normal compile and build identity path.
    wanted = sorted({method for pair in names.values() for filters in pair[:2] for method in filters})
    if not wanted:
        return None, {}
    gradle = [source / 'gradlew', '--no-daemon', '--max-workers=2', '--no-build-cache',
              ':app:testDebugUnitTest', '--rerun']
    for target in wanted:
        gradle.extend(['--tests', target])
    log = output / 'selected-gradle.log'
    code = run_logged(gradle, source, log, 1800)
    invocation = {'exit': code, 'command': list(map(str, gradle)), 'logSha256': digest(log)}
    archive = output / 'junit'; archive.mkdir()
    cases = {}
    if code or 'BUILD SUCCESSFUL' not in log.read_text(errors='replace'):
        return invocation, cases
    for xml in sorted((source / 'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml')):
        suite = ET.parse(xml).getroot()
        for test in suite.findall('testcase'):
            name = test.attrib['classname'] + '.' + test.attrib['name']
            if name in cases:
                raise ValueError('DUPLICATE_TEST_CASE:' + name)
            cases[name] = {'name': name, 'passed': all(test.find(t) is None for t in ('failure','error','skipped')),
                           'xml': 'junit/' + xml.name, 'xmlSha256': digest(xml)}
        shutil.copyfile(xml, archive / xml.name)
    return invocation, cases


@contextmanager
def exclusive_test_input(root):
    # Gradle writes into the checkout's build directories even for read-only
    # tests. Independent source worktrees have independent locks.
    import hashlib
    name = hashlib.sha256(os.fsencode(str(root))).hexdigest()
    with (Path(tempfile.gettempdir()) / ('starcore-r34-verification-' + name + '.lock')).open('a+b') as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError as exc:
            raise ValueError('TEST_CHECKOUT_ALREADY_IN_USE') from exc
        yield


def instrumentation_results(args, source, jobs, before):
    if not args.android_receipt:
        return {}
    if (not args.android_receipt_sha256 or digest(args.android_receipt) != args.android_receipt_sha256
            or not args.apk):
        raise ValueError('PIN_THE_RECEIVED_ANDROID_RECEIPT_AND_SIGNED_TARGET_APK')
    raw = json.loads(args.android_receipt.read_bytes())
    if (raw['sourceCommit'] != before['commit'] or raw['sourceTree'] != before['tree']
            or raw['apkSha256'] != digest(args.apk) or raw['scope'] != 'ORIGINAL_ANDROID_JUNIT_ON_ISOLATED_API_34'
            or raw['api'] != 34 or raw['physical'] is not False):
        raise ValueError('ANDROID_RECEIPT_IDENTITY')
    expected = {c['command'].rsplit('=',1)[1]: c['path'] for j in jobs.values() for c in j['test_contracts']
                if c['state']=='EXISTING_ENTRY_REQUIRES_REVALIDATION' and ':app:connectedDebugAndroidTest' in c['command']}
    if set(expected) != set(raw['results']):
        raise ValueError('ANDROID_RECEIPT_METHOD_SET')
    result = {}
    for method,path in expected.items():
        item=raw['results'][method]
        if (item['method']!=method or item['fixtureSha256']!=digest(source/path)
                or not re.fullmatch('[0-9a-f]{64}',item['outputSha256'])):
            raise ValueError('ANDROID_METHOD_INPUT_MISMATCH:' + method)
        success=item['exit']==0 and 1 in item['statusCodes'] and 0 in item['statusCodes'] and min(item['statusCodes'])>=0
        result[method]=dict(item, passed=success and item['passed'] is True, runId=raw['runId'], receiptSha256=args.android_receipt_sha256)
    return result


def execute(args, parser):
    if not args.source_root or not args.output:
        parser.error('--case or --all requires --source-root and --output')
    source = args.source_root.resolve(strict=True)
    plan = args.plan or os.environ.get('STARCORE_R34_PLAN_ROOT')
    if not plan:
        parser.error('set STARCORE_R34_PLAN_ROOT or pass --plan')
    plan = Path(plan).resolve(strict=True)
    rows = read_jobs(plan, source)
    if args.case and args.case not in rows:
        parser.error('unknown R33.5 case')
    selected = list(rows.values()) if args.all else [rows[args.case]]
    out = args.output.resolve()
    if out.is_relative_to(source):
        parser.error('the receipt directory must be outside the input checkout')
    out.mkdir(parents=True, exist_ok=False)
    before = source_id(source)
    tools = {p.name: digest(p) for p in (Path(__file__), Path(__file__).with_name('legacy_behavior_cases.py'), Path(__file__).with_name('legacy_release_cases.py'))}
    started = datetime.now(timezone.utc).isoformat()
    android = instrumentation_results(args, source, rows, before)
    names = suite_names(source, selected)
    gradle, cases = selected_results(source, out, names)
    volume = args.original_volume or os.environ.get('STARCORE_R33_PLAN_ROOT')
    volume = Path(volume).resolve(strict=True) if volume else None
    groups = sorted({group for job in selected for group in PROBES_BY_CASE.get(job['legacy_id'][5:], ())})
    original = {name: run_original(name, volume, source, out) for name in groups}
    checked = {}
    for key in sorted({BY_ID[j['legacy_id'][5:]] for j in selected if j['legacy_id'][5:] in BY_ID}):
        folder=out/key;folder.mkdir()
        try:
            checked[key]=CASES[key](source,folder)
        except (ValueError, AssertionError, OSError, subprocess.SubprocessError) as exc:
            checked[key]={'status':'FAIL','reason':str(exc)}
    summary = []
    for job in selected:
        ident = job['legacy_id']; old, suites, device = names[ident]
        contract, = [c for c in job['test_contracts'] if c['state']=='NEW_TEST_REQUIRED']
        original_results = [cases.get(key, {'name': key, 'passed': False, 'reason': 'NO_CURRENT_JUNIT_RESULT'}) for key in old]
        additional = [test for key, test in cases.items() if key.rsplit('.',1)[0] in suites]
        missing_suites = [name for name in suites if not any(key.rsplit('.',1)[0] == name for key in cases)]
        probes = {k: original[k] for k in PROBES_BY_CASE.get(ident[5:], ())}
        failed = bool(missing_suites or any(not x['passed'] for x in original_results + additional)
                      or any(probe['status'] == 'FAIL' for probe in probes.values()))
        build_check=checked.get(BY_ID.get(ident[5:]))
        if build_check and build_check['status']=='FAIL': failed=True
        mobile={key:android[key] for key in device if key in android}
        failed=failed or any(not item['passed'] for item in mobile.values())
        # The four-file case is fully exercised, including an isolated corruption of
        # each of the four inputs. Other checks expose the unverified protocol parts.
        complete = ident in ('R335-D02','R335-D03') and build_check and build_check['status']=='PASS' and not failed
        record = {'case': ident, 'title': job['title'], 'source': before, 'toolSha256': tools,
                  'startedUtc': started, 'newIssueIds': job['new_issue_ids'], 'originalJUnit': original_results,
                  'additionalJUnit': additional, 'missingSuites': missing_suites, 'originalOwnerProbes': probes,
                  'instrumentationRequired': device, 'instrumentationReadbacks': mobile,
                  'requiredInputs': contract['inputs'], 'requiredAssertions': contract['assertions'],
                  'buildOwnerCase': build_check, 'caseStatus': 'FAIL' if failed else ('PASS' if complete else 'OPEN'),
                  'scope': 'MEASURED_COMPONENT_RESULTS_ONLY; END_TO_END_RETAINED_CASE_REQUIRES_BOTH_SPECIFIED_INPUTS'}
        summary.append({'case': ident, 'status': record['caseStatus'], 'originalJUnitPassed': sum(x['passed'] for x in original_results),
                        'originalJUnitCount': len(old), 'additionalJUnitInvocations': len(additional)})
        (out / (ident + '.json')).write_text(json.dumps(record, ensure_ascii=False, indent=2) + '\n')
    if source_id(source) != before:
        raise ValueError('SOURCE_CHANGED_DURING_EXECUTION')
    (out / 'INDEX.json').write_text(json.dumps({'source': before, 'toolSha256': tools, 'gradle': gradle,
                'cases': summary, 'physicalAcceptance': 'NOT_MEASURED', 'naturalQuality': 'NOT_MEASURED'}, indent=2) + '\n')
    print(json.dumps({'cases': len(summary), 'originalJUnitPassed': sum(row['originalJUnitPassed'] for row in summary),
                      'source': before, 'index': str(out / 'INDEX.json')}))
    return 0 if all(row['status'] == 'PASS' for row in summary) else 1


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument('--case'); mode.add_argument('--all', action='store_true')
    parser.add_argument('--source-root', type=Path); parser.add_argument('--output', type=Path)
    parser.add_argument('--plan', type=Path); parser.add_argument('--legacy', type=Path)
    parser.add_argument('--original-volume', type=Path)
    parser.add_argument('--android-receipt', type=Path); parser.add_argument('--android-receipt-sha256')
    parser.add_argument('--apk', type=Path)
    args = parser.parse_args()
    if args.case or args.all:
        if not args.source_root: parser.error('--source-root is required')
        with exclusive_test_input(args.source_root.resolve(strict=True)):
            sys.exit(execute(args, parser))
    if not args.plan or not args.legacy:
        parser.error('registry validation requires --plan and --legacy')
    print(json.dumps(check(args.plan, args.legacy)))
