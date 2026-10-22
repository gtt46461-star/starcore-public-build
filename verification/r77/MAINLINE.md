# R77 prospective calibration and capture verification

The phone report contains four R76 frozen predictions with `captured_at >= draw.open_time` (issues 617, 651 twice, 668). Frozen prediction settlement rejected them, but the calibration settlement owner lacked that check. The R76 source at `2e82e9834a081be2a0e0f4ec326572df634e0543` reproduces this counting error in the boundary fixture.

Changed product owners:

- `CandidateCalibrationR932`: strictly earlier capture is required for settlement and fitting; invalid pending observations become terminal `hit=-2` without incrementing a denominator. Original observations remain. Invalid READY inputs are durably blocked and no longer monopolize fitting; only missing/invalid input failures allow a new prospective study. Quality failures cannot restart to retry their statistics.
- `FrozenCandidateAdapterR958`: check the existing 60-second game clock before scoring and before/after publication. A publication crossing the deadline rolls back all candidate, calibration and proof writes.
- `ResearchPredrawCaptureR923`: reject stale tasks before spending a research slice. Due reservations retain their existing first-priority order.
- `DrawDependencyInputCP12`: authenticate retained timestamps with the original full input hash, valid issue/permutation and draw hash. Retain settled binary CAL input while its study is COLLECTING/READY; release after FROZEN/BLOCKED. A dirty hot row never falls back to retained data.

No evidence threshold, training denominator, certificate, selection authority or REAL decision gate was weakened. The report has no current qualified model/certificate; historical display hit rate does not establish a qualified prospective candidate.

Run the four scripts in this directory against the supplied product bytecode and installed schema. `--overlay` is development-only; omit it for final product replay. `run_mainline_maintenance.py` instead accepts `--product-classes` to disable all product overlays. Each output directory must be fresh.

```sh
python -B verification/r77/cal_strict_time_run.py --jdk "$JDK" --classes "$PRODUCT" --adapters "$ADAPTERS" --android-runtime "$ANDROID" --out "$OUT/cal"
python -B verification/r77/cal_input_retention_run.py --jdk "$JDK" --classes "$PRODUCT" --adapters "$ADAPTERS" --android-runtime "$ANDROID" --schema "$SCHEMA/schema-complete.db" --out "$OUT/retention"
python -B verification/r77/capture_deadline_run.py --jdk "$JDK" --classes "$PRODUCT" --adapters "$ADAPTERS" --android-runtime "$ANDROID" --schema "$SCHEMA/schema-complete.db" --out "$OUT/deadline"
python -B verification/r77/run_mainline_maintenance.py --jdk "$JDK" --schema "$SCHEMA" --product-classes "$PRODUCT" --adapters "$ADAPTERS" --out "$OUT/maintenance"
```

The four suites contain 66 assertions: CAL boundary/retained/READY recovery 23; input ownership and release 8; timely/expired/transaction-crossing capture 16; continuous 241 captures with a fresh JVM after 120 and public maintenance 19. Initial runs used a shared host clock that advanced per call; their elapsed-time/budget behavior is superseded. Every runner now explicitly compiles the canonical `System.nanoTime` clock ahead of shared adapters. The continuous suite has also passed with this real monotonic clock; The sealed-bytecode final rerun also passes all 66 assertions; its receipts are in `evidence/mainline-final`. Each continuous period creates a real 1200-action proof through product code and settles exactly once. The fitted fixture has ECE 0.1211758977, exceeding the unchanged 0.04 limit, so it truthfully receives FITTED_BUT_NOT_QUALIFIED and creates no certificate or REAL.

These are controlled host SQLite fixtures with synthetic inputs and actual product owners. They do not establish phone timing, upstream publication latency, future hit rate, profitable operation, or future results on the user phone. Final replay uses the sealed Gradle release classes from source commit `5823c4b3f198c37d9d1a4db3152d2ec0d296bf29`, classset SHA-256 `cacfd80efeb17c3ad48a6ae9b61f1d0a6c94ff45c57429c9338467ad1f1e6266`, without product overlays. Fixture clocks are relative to their run; the earlier R75/76 fixture's 1970 source times cannot satisfy the prospective deadline.

Reader audit: calibration fitting and CertifiedPrediction accept only 0/1; ResearchFailureEvolution's outer query requires 0/1; pending maintenance and old input pins require -1. The new -2 label cannot become a hit, miss or pending label through these paths. Frozen observation triggers still preserve immutable fitted evidence. The report's preexisting interrupted studies are not rewritten or relabeled.

`SETSTATES_CALLSITE_AUDIT.json` records the independent caller audit after batch writes became strict. Four research postcommit projections use the independent optional telemetry API: FTRL, settlement sidecar, legacy shadow capture, continuity summary. Cursors, atom quality, retry controls and authoritative receipts keep strict writes. `research_telemetry_run.py` exercises an actual FTRL commit followed by a second-key display fault, whole-projection rollback, idempotent replay, a successful next-period update, sidecar display failure and nested API rejection (12 assertions).

`certified_mainline_run.py` builds a qualification fixture using the supplied product bytecode, actual CAL fitter, quality evaluator and certificate publisher. Its 241 CAL and 360 formal observations are explicitly synthetic test evidence. An earlier build's certificate must not be reused because dependency rejection is correct. The suite retains revoked/postdraw/duplicate/negative-score guards and adds three consecutive valid-score periods through the actual choice, certified prediction, sole decision gate, authorized trade writer and settlement owner. Test-created REAL rows exist only in isolated fixture databases.

Final acceptance also passes 12 telemetry assertions and the current-version synthetic qualified suite: 19 actual publisher assertions, 26 repeat/revocation/postdraw guards, 28 positive/negative exact-trade closure assertions and 35 assertions across three consecutive eligible periods. The separate exact-selection probe also passes. The 5% certified research ranking weight does not block those consecutive REAL authorizations. Test evidence is supplied explicitly; no phone certificate, sample, gate or label is edited.

The qualified closure supplies a CONTROLLED_PROBE risk result and a synthetic frozen shadow observation, then invokes the actual product selector, exact-model scoring, sole decision gate, authorized trade writer and settlement owner. It proves that eligible consecutive decisions can traverse those owners and retain duplicate/revocation/negative-score protections. It does not prove that the full runtime orchestrator naturally triggers that path on the phone. The qualification fixture now includes all intervening draws because modern temporal atoms correctly reject history gaps.

```sh
python -B verification/r77/research_telemetry_run.py --jdk "$JDK" --classes "$PRODUCT" --adapters "$ADAPTERS" --android-runtime "$ANDROID" --schema "$SCHEMA/schema-complete.db" --out "$OUT/telemetry"
python -B verification/r77/certified_mainline_run.py --jdk "$JDK" --schema "$SCHEMA" --product-classes "$PRODUCT" --out "$OUT/certified"
```

After independent review, the paired fixture also aligns task creation/completion to draw time + 1, matrix input to + 2 and projection to + 3. All six qualified modes passed again; `final-certified-clock-consistent` replaces the earlier qualification receipt. The broader qualification rows remain explicit synthetic seeds, not naturally collected phone evidence.
