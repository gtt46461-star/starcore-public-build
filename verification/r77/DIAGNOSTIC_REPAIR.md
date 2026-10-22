# R77 diagnostic classification repair

Product scope: `LiveResearchDiagnosisOrchestrator`, `RuntimeHealthLedgerR36`, `CrashRecorder`. `LiveDiagnosticReportEngine` does not need a direct edit because its live/snapshot sections consume these owners.

- Action-space live/snapshot collectors read actual request/deferred states. A canonical commit with a pending/running/retry deferred job and fewer than 1200 persisted rows is `RETEST_REQUIRED`; it explicitly remains unverified. Completed/orphaned missing rows and excess rows remain `FAIL`; errors remain read errors. No action count, worker, gate or evidence row is changed.
- Settlement health now looks up the newest immutable prediction at/before the draw, using the indexed target key, and compares settlement against that real obligation. Latest drawn issue without predraw evidence becomes `PREDRAW_COVERAGE_MISSING` / `FAIL` and appears in the derived runtime finding. Existing-predraw unsettled/OOS failures, dead business, 5000 ms SLA, concurrency and OOM checks remain failures. Live and immutable-report paths use the same pure classifier.
- Crash summary no longer presents a persisted fixed-window count from a previous day as the current rolling ten-minute count. Old last-event timestamps safely establish zero recent events. When the last event is recent, exact rolling count is `UNKNOWN` because the legacy store has no timestamp log. Clock rollback remains explicit. The raw window count/start, observation time, historical crash total and last crash remain visible. No crash record, persisted counter or recovery rule is modified.

## Reproduction

```
python verification/r77/diagnostic_classification_run.py \
  --jdk <JDK17> --classes <compiled-product-classes> \
  --android-runtime <android-all.jar> --report <extracted-report> --out <fresh-output-dir>
```

Use `--baseline` to compile HEAD's pre-fix sources and reproduce the old false action-space failure (expected nonzero exit). Use `--final-bytecode` after the final R77 build to run without product source overlays.

Development results: 33 assertions pass, including the actual snapshot action collector over read-only SQLite with the report's production DDL, actual `businessTruthSnapshotCP4` SQL for missing predraw versus existing unsettled predraw, derived finding preservation of both coverage and 41346 ms latency, and old/recent/missing/rollback crash timestamps. Every fixture database hash is unchanged after reads. The retained product class files are also unchanged. R76's 23 runtime-finding regression assertions pass. Baseline fails at the first partial/PENDING action-space scenario with `FAIL`, confirming regression sensitivity.

These are controlled host regressions with actual changed classes and retained dependency bytecode, not final R77 APK or physical-phone runtime evidence. The final build must rerun `--final-bytecode`. The diagnostic change does not repair the original missing prediction or latency itself; separate runtime work is required.
