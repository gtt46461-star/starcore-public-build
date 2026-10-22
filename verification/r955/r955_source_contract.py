from pathlib import Path
import sys
root=Path(sys.argv[1]) if len(sys.argv)>1 else Path('.')
auto=(root/'app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java').read_text()
hb=(root/'app/src/main/java/com/ethk3/novaonepro/ServiceHeartbeatStore.java').read_text()
report=(root/'app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java').read_text()
ui=(root/'app/src/main/java/com/ethk3/novaonepro/MainActivity.java').read_text()
contract=(root/'app/src/main/java/com/ethk3/novaonepro/BackgroundResearchProgressContractR955.java').read_text()
checks={
 'POST_USES_ATTEMPT':'markLearningAttempt("POSTED_R955"' in auto,
 'P0_YIELD_USES_ATTEMPT':'markLearningAttempt(\n                                "V51_OWNER_YIELD", "P0_PRESSURE|' in auto,
 'SUCCESS_ONLY_PROGRESS':auto.count('markLearningPump(')==1 and 'markLearningPump(\n                            "V51_OWNER_OK"' in auto,
 'LEGACY_POST_IS_ATTEMPT':'markLearningAttempt("LEGACY_POSTED", reason);' in auto and 'markLearningPump("POSTED", reason);' not in auto,
 'ERROR_NOT_PROGRESS':'markLearningPump("V51_OWNER_ERROR"' not in auto and 'markLearningAttempt("V51_OWNER_ERROR"' in auto,
 'DIRECT_R908_REPAIR':'postAutonomousResearchTickR908("R955_RESEARCH_PROGRESS_REPAIR:' in auto,
 'SEPARATE_ATTEMPT_CLOCK':'markLoopPulse("learning_attempt"' in hb,
 'STALE_USES_PROGRESS_CONTRACT':'return researchProgressTruth().stale();' in hb,
 'BUSINESS_TRUTH_INCLUDES_RESEARCH':'getResearchStaleReason()' in hb and 'RESEARCH_STALE' in hb,
 'V57_HAS_RESEARCH':'researchProgressAge' in hb and '+ "|research="' in hb,
 'REPORT_EXPOSES_HEARTBEAT':'R955ResearchProgressTruth=' in report and 'peekLastDetailedSummary()' in report,
 'UI_EXPOSES_ATTEMPT_PROGRESS':'ResearchAttempt=' in ui and 'ResearchProgressAge=' in ui,
 'FRESH_ATTEMPT_CANNOT_MASK':'effectiveProgressAt' in contract and 'attemptAt' in contract,
}
failed=[k for k,v in checks.items() if not v]
for k,v in checks.items(): print(f'{k}={"PASS" if v else "FAIL"}')
if failed:
    print('R955_SOURCE_CONTRACT=FAIL|' + ','.join(failed))
    raise SystemExit(1)
print('R955_SOURCE_CONTRACT=PASS|attempt_clock!=progress_clock|watchdog_reposts_R908=TRUE|report_truth=VISIBLE')
