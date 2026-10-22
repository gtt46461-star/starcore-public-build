from pathlib import Path
import sys
root=Path(sys.argv[1]) if len(sys.argv)>1 else Path('.')
auto=(root/'app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java').read_text()
cont=(root/'app/src/main/java/com/ethk3/novaonepro/ResearchLearningContinuityR912.java').read_text()
report=(root/'app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java').read_text()
gate=(root/'app/src/main/java/com/ethk3/novaonepro/BackgroundResearchCompletionGateR956.java').read_text()
checks={
 'COMPLETION_GATE_CALLED':'BackgroundResearchCompletionGateR956.evaluate(' in auto,
 'INCOMPLETE_IS_ATTEMPT':'V51_OWNER_INCOMPLETE_R956' in auto and 'markLearningAttempt(' in auto,
 'PROGRESS_STILL_SINGLE_CALL':auto.count('markLearningPump(')==1,
 'CONTINUITY_TRACKS_LANE_ERRORS':'errorLanes' in cont and 'completedLanes' in cont and 'yieldedLanes' in cont,
 'CONTINUITY_FAILS_ON_LANE_ERROR':'boolean useful = completedLanes > 0 && errorLanes == 0;' in cont,
 'NO_POST_DEADLINE_EVOLUTION':'SKIPPED_R956_CONTINUITY_BUDGET_OR_P0' in cont and 'TimeSourceR811.monoNowMs() < deadline' in cont,
 'PHASE0_NO_EMBEDDED_CONTINUITY_YIELD':'phaseResult = "RETENTION_QUANTUM:OK";' in auto,
 'SIDECAR_INCLUDED_IN_COMPLETION':'sidecarStatus=compact.status;' in auto,
 'DIAGNOSTIC_EXPOSES_R956':'R956ResearchOwnerCompletion=' in report and 'R956ContinuityLaneSummary=' in report,
 'NO_REAL_AUTH': 'MAY_AUTHORIZE_REAL = false' in gate,
}
failed=[k for k,v in checks.items() if not v]
for k,v in checks.items(): print(f'{k}={"PASS" if v else "FAIL"}')
if failed:
    print('R956_SOURCE_CONTRACT=FAIL|' + ','.join(failed)); raise SystemExit(1)
print('R956_SOURCE_CONTRACT=PASS|owner_progress_fail_closed=TRUE|continuity_lane_truth=VISIBLE')
