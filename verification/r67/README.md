# R67 runtime repair

Baseline: R66 source/evidence commit `17ba3d2c5d227f69a878714c85c75d514ab61f4c`.
The new user report identifies version 5.4.2.66, an incomplete atom import (23,824/25,350), zero modern development protocols, 103 pending online training events, and a persisted history-write timeout. Its last recorded analysis exception predates the R66 installation and is not counted as a new R66 crash.

Changes:
- Broadcast the clock immediately after durable current-draw validation, before diagnostic database writes. Keep the original issue/time anchor and freshness contract. Expired periods show waiting for the draw rather than an inert zero.
- Always reschedule the existing serial fetch callback from finally, including failures during error-state persistence.
- Identical confirmed polls avoid duplicate draw/history writes. Conflicting, new, dirty and corrected-time inputs still use the sole canonical writer. Poll receipts and reconciliation continue.
- Do not claim critical analysis pressure just to read an existing canonical result. Research writes with CP12 names now respect their existing cooperative budget and critical-writer priority.
- Import pinned atoms in pages up to 96 rows / 64 KiB instead of 32; preserve full SHA, immutable row verification and cursor/generation CAS. TRAIN uses the rest of its bounded quantum while retaining the durable online/development/freeze rotation.
- Default new report requests include the full immutable SQLite snapshot, installed APK and frozen model/training/retention attachments. Archive completeness and check success remain separate facts.
- Carry only the explicitly reviewed R65/R66 numeric training streams forward; do not migrate certificates, retrospective future labels or policy qualifications.

`run.py` executes the current Gradle-compiled product against real SQLite. The R67 cases cover duplicate/conflict admission, countdown expiry/new-period projection, 96-row checkpoint/replay/stale CAS, CP12 research preemption and R66 numerical continuation. It also retains the R66 26-check recovery suite and 234-check numeric suite. Synthetic fixtures do not prove phone endurance, natural-future performance, mainline qualification or profitability. Quality thresholds and unique decision/settlement owners remain unchanged.

Run:
```
python3 verification/r67/run.py --classes app/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes --android-runtime /absolute/android-all.jar --jdk /absolute/jdk17 --out /absolute/new-evidence-directory
```
