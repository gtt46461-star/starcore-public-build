# R76 training queue and report recovery

`ResearchTaskQueueR967.readyPredicate` used a predicate from which SQLite did not infer the active-task partial-index domain. On the supplied report's 3,537 task rows the maintenance query read completed history, and its predecessor check preferred the wider covering index. The fix declares the already-implied active status domain and selects the two existing predecessor partial indexes. Queue ordering, readiness, terminal-predecessor rules, lease ownership, budgets and evidence gates are unchanged.

The R75 predicate in `evidence/training_r75_ready_predicate.sql` was exported directly from the locked R75 release class before editing. `training_ReadyQueueR76` exports the current compiled predicate. `training_ready_profile.py` checks exact ordered result equality for four task kinds, exercises failed/expired/unresolved predecessor and process-owner cases, and appends 50,000 completed maintenance tasks. Warm SQLite VM work for maintenance was 72,342 → 1,137 on report rows and 1,222,198 → 1,029 after history growth. These are query-work measurements, not phone wall-clock latency. Actual product claim/yield owners are also invoked.

The claim prelude's same-process orphan scan also read all terminal history. It now uses the same existing active-task partial index. Its SQL work is 10,625 → 149 on report rows and 160,623 → 147 with 50,000 additional terminal rows. The baseline query in `evidence/training_r75_orphan.sql` is the exact prepatch source statement. The real Owner test protects a live local token, simulates loss of only that token, recovers the same task at generation 2 and rejects the previous generation's ownership and yield attempts. No process/lease identity semantics change.

`training_report_analysis.py` separates report evidence by the R75 release epoch at issue 202610130472. In the following report interval there are 5 new modern protocols, 1 new legacy numerical artifact, 4 new legacy outbox labels and 12 atomic FTRL learned events. Modern feature progress is 19 rows total, of which at least 16 are demonstrably new after installation. The installation-time REGIME cursor is absent, so its current three rows cannot all be attributed to this release. Modern optimizer checkpoints, development receipts and artifact identities remain zero in the original report. The three learning chains must not be conflated.

`training_prepare_report.py` builds a controlled host fixture from exported rows and the complete schema. Tables with omitted binary payloads are explicitly skipped; this is not the original phone DB. Modern feature cursors initially reset only in the fixture because their binary feature rows were not exported. `training_ReportOwnerR76` recreates each SCDEV file using its original stored header and original completed draws, and requires all six file hashes and byte counts to equal the report. The real training owner regenerates features; the runner requires REGIME's original prefix of 3 and TREE's prefix of 16 to hash exactly as reported.

The same fixture then continues in a fresh JVM through 7,920 authentic feature rows, six development receipts, six model artifacts and six completed freeze tasks (1,200 development and 120 validation samples each). A third JVM confirms no duplicate model/freeze publication. Tests rebuild `DbStore` between callbacks, preserve the normal 850 ms work scope and require Future, certificate and REAL counts to remain unchanged. This proves durable host-owner continuation from authenticated report inputs; it does not prove phone timing, calibration quality, profitable prediction, or performance of unavailable blob-backed lanes.

Final release bytecode mode uses no `--overlay`:

```sh
python verification/r76/training_run_report.py \
  --schema /workspace/scratch/7a6032548233/r72-final-schema \
  --report /workspace/scratch/e2544c9906f6/r76-input/report/DIAGNOSTIC_SCOPE.json \
  --classes app/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes \
  --jdk /workspace/scratch/cd9274eff1c6/jdk/jdk-17.0.20.1+1 \
  --out /workspace/scratch/e2544c9906f6/r76-final-training-report

python verification/r76/training_run_ready.py \
  --schema /workspace/scratch/7a6032548233/r72-final-schema \
  --database /workspace/scratch/e2544c9906f6/r76-training-report/report.db \
  --classes app/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes \
  --jdk /workspace/scratch/cd9274eff1c6/jdk/jdk-17.0.20.1+1 \
  --out /workspace/scratch/e2544c9906f6/r76-final-training-ready
```

The second command intentionally uses the untouched reconstruction's queue, not the queue after the first command completed modern tasks. Prebuild development runs explicitly record the single TaskQueue bytecode overlay. R75 budget, outbox recovery and protocol compatibility suites remain applicable to the shared task queue.
