#!/usr/bin/env python3
"""Verify one exact Android invocation across instrumentation and its cleared logcat."""
import hashlib
import re

CLASS = 'com.ethk3.novaonepro.R31QDeliveryInstrumentedTest'
TESTS = {
    'committedIntentSurvivesObserverDeathAndExportsExplicitUnknown': 'DURABLE_INTENT_OBSERVER_DEATH',
    'fullReportReachesPublishedZipAndProviderReadback': 'FULL_REPORT_PUBLISHED|PROVIDER_READBACK_HASH_PASS',
    'blockedOptionalObserversStayBoundedAndDoNotBlockTheNextRequest': 'OPTIONAL_OBSERVERS_BOUNDED_AT_TWO',
    'legacyMigrationResumesAndAllowsCriticalCommitsBetweenUnits': 'LEGACY_READY|CRITICAL_COMMITS=',
    'firstSchemaProbeFailureStopsRemainingColumns': 'FAILED_PROBE_NOT_SWALLOWED',
    'oldReadyStampCannotSkipTheNewAuditedMigrationPass': 'OLD_READY_REAUDITED',
    'liveServiceFetchesFreezesAndSettlesNewFutureObservations': 'REAL_API_FETCH_AND_NEW_FREEZE_SETTLEMENT',
}


def verify(protocol, logcat):
    problems = []
    records, current = [], {}
    for line in protocol.splitlines():
        if line.startswith('INSTRUMENTATION_STATUS: '):
            key, sep, value = line[len('INSTRUMENTATION_STATUS: '):].partition('=')
            if sep:
                current[key] = value
        elif line.startswith('INSTRUMENTATION_STATUS_CODE: '):
            code = int(line.split(':', 1)[1])
            records.append((current.get('class'), current.get('test'), code))
            current = {}
    starts = [test for cls, test, code in records if cls == CLASS and code == 1]
    done = [test for cls, test, code in records if cls == CLASS and code == 0]
    if len(starts) != len(TESTS) or set(starts) != set(TESTS) or starts != done:
        problems.append('EXACT_SEVEN_STARTED_AND_COMPLETED_TESTS_REQUIRED')
    if any(cls != CLASS or code not in (0, 1) for cls, _, code in records):
        problems.append('FAILED_SKIPPED_OR_FOREIGN_TEST_STATUS')
    if not re.search(r'^OK \(7 tests\)\s*$', protocol, re.M) or 'FAILURES!!!' in protocol:
        problems.append('INSTRUMENTATION_DID_NOT_SUCCEED')
    active, markers, runner_starts, runner_finishes = {}, {}, [], []
    pattern = re.compile(r'^\d\d-\d\d\s+\S+\s+(\d+)\s+\d+\s+\S\s+(TestRunner|System\.out):\s*(.*)$')
    for line in logcat.splitlines():
        match = pattern.match(line)
        if not match:
            continue
        pid, tag, message = match.groups()
        if tag == 'TestRunner':
            if message == 'run started: 7 tests':
                runner_starts.append(pid)
            if message == 'run finished: 7 tests, 0 failed, 0 ignored':
                runner_finishes.append(pid)
            for event in ('started', 'finished'):
                test = re.fullmatch(event + r': (\w+)\(' + re.escape(CLASS) + r'\)', message)
                if test:
                    if event == 'started':
                        active[pid] = test.group(1)
                    elif active.get(pid) == test.group(1):
                        del active[pid]
        else:
            test = active.get(pid)
            if test in TESTS and message.startswith('R31Q_ANDROID|') and TESTS[test] in message and message.endswith('|PASS'):
                markers[test] = {'channel': 'LOGCAT_SAME_TEST_PID_AND_LIFECYCLE', 'pid': int(pid), 'record': message}
    if len(runner_starts) != 1 or runner_starts != runner_finishes:
        problems.append('ONE_COMPLETE_LOGCAT_RUN_REQUIRED')
    for test, marker in TESTS.items():
        if test not in markers:
            problems.append('MISSING_CURRENT_TEST_MARKER:' + test)
        elif len(runner_starts) != 1 or str(markers[test]['pid']) != runner_starts[0]:
            problems.append('FOREIGN_MARKER_PID:' + test)
    return {'passed': not problems, 'problems': problems, 'completedTests': done, 'markers': markers,
            'protocolSha256': hashlib.sha256(protocol.encode()).hexdigest(),
            'logcatSha256': hashlib.sha256(logcat.encode()).hexdigest(),
            'scope': 'EXACT_INVOCATION_TEST_COMPLETION_AND_ORIGINAL_LOGCAT_MARKERS; NOT_SAF_OR_HANDSET_OR_NATURAL_QUALITY'}
