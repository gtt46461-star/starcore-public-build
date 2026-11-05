# B00 C02: actual observation producer, native fault and report consumer

The passing run is `runs/20261105T023753Z/receipt.json`: **98 named assertions**,
using candidate commit `810818d5a8f73e886dbeb0ad593a6fc6bddd3665`. No production
file or old shared test tool was modified by this module.

The runner copies the actual frozen f607 Android development bootstrap jar
(`/workspace/r31-f607-frozen/classes.jar`, 4,493 classes, SHA-256
`111dfd9f2591906036d031a32adfcc999ab58e644ebda7e24548fceb1d37e30f`), then compiles
exactly five captured candidate sources: ResearchPoolR27, RuntimeFaultEvidenceR31,
ReportSnapshotR30, ReportR27 and RuntimeDiagnosticLinksR31. All other production
dependencies remain that fixed bytecode. The snapshot did not drift during the run.
The development bootstrap's identity metadata exclusions are not an APK build or
release receipt. No Gradle, Android framework runtime, device or natural-quality
trial is claimed here.

There is **no DbStore facade**. Unsafe only constructs and wires the real final
DbStore, DbHelper, DatabaseCoordinator and ResearchPool objects, avoiding automatic
Android service/thread startup. Actual coordinator locks/transactions, actual
pool `record` and `observeAttemptR31`, and actual `ReportSnapshotR30.capture` execute.
Startup uses the real RuntimeSchema base helper and the actual R31 DDL/declarative
SchemaR27 columns. This is the required schema subset, not a full-app startup test.
The API-34 host boundary selects the production single SELECT report path; the
native report connection itself is opened read-only. Filesystem, monotonic clock,
thread CPU, Java memory observations and SQLite run on the host. Unsupported native
Android memory stays UNKNOWN through the production JVM detection.

The business payload is an explicitly controlled 8-byte fixture. It is committed
by `runIdempotentAtomicWriteR31`, with real task/session/boot/lease fences and a real
`r31_commit_receipt`. It is not a model, settlement or quality trial.

- Actual frozen f607 ResearchPool bytecode reproduces the normal CLAIM_WAIT bug:
  no task receipt was expected or attempted, but the failure counter changes 0→1.
  The same real entry with the candidate pool keeps both process and durable receipt
  failure counts at zero. `before-claim-wait` intentionally exits 1; it is not a test
  harness assertion weakened for the candidate.
- Two separate cases install a **native SQLite trigger fault**, once on fault
  INSERT and once on observation UPDATE. The actual private pool `record` first
  commits the task receipt and then calls `observeAttemptR31`; the diagnostic
  transaction fails and rolls back. The already committed business row, business
  receipt and task receipt all survive. No recursive fault write is attempted.
- Actual ReportSnapshot capture after that failure shows `telemetry_incomplete=true`
  and `observationWritesFailed=1`. The next successful actual observation durably
  flushes the count. Another actual capture reads the persisted fault and counter,
  retains COMMITTED/AFTER_COMMIT, and links event_id with matching owner/fence.
  Both captures retain all 44 metric definitions and perform no data writes. The
  report also keeps unavailable artifact progress explicitly unbound; a task receipt
  or diagnostic event is not model progress.
- Through the real private persist entry **and real coordinator transactions**, the
  row case accepts exactly 2,000 samples / 2,511,560 logical bytes and drops the next
  sample. The byte case accepts 233 samples / 8,384,272 bytes, then rejects the sample
  that would exceed 8,388,608 bytes. The drop count is one and is flushed to the real
  telemetry table. Existing samples are never deleted; first/last aggregation of an
  existing sample still increments at capacity.

The run records 2,249 NEW-labelled native physical transactions, including the one
intentional frozen-baseline transaction. Maximum SQL count including BEGIN/END is
14; maximum bind bytes is 35,540. Maximum observed host transaction is 24.786433 ms.
These are the complete named denominator, not an Android writer-SLO acceptance.

Earlier compile/setup failures are retained: the nested ResourceBudget lookup and
missing declared startup columns. They led to executing the actual base startup
helper and declared column additions. No production guard, statistical value,
evidence row or quality gate was changed to pass these tests.

Run `python3 verification/r31/b00_observation_pipeline/run_native.py --repo <candidate>`.
`--only` can rerun one named case. The fixed jar is copied before compilation. All
JDBC extraction, Java temporary files, SQLite files and new output live under this
module's own `/workspace` paths; the full `/tmp` volume is not used or cleaned.
Raw SQL frames and logs are losslessly archived with hashes; `verify_receipt.py`
checks their original bytes and results. Cross-process loss of unflushed counters,
Android execution and unified final-APK proof are not established by this same-JVM
host test. The production process-only scope remains visible in captured reports.
