# >200ms writer observation → independent durable fault receipt

`DatabaseCoordinator.recordUnlockedWriterR30` previously updated only the in-memory hold histogram. The observed 401/322ms transactions in the Atom readiness run therefore lacked the required fault receipts. Their original failure evidence remains unchanged; this implementation cannot retroactively produce those receipts.

The production observer now queues bounded immutable facts after unlocking. `RuntimeR27.HealthTask` drains at most one event in a separate idempotent `TELEMETRY` transaction before optional cleanup or report throttling. The receipt records actual operation, task/owner/fence when present, wait/hold times, actual commit outcome and source identity. Unmeasured SQL/END/GC/scheduling attribution and absent exception stacks remain explicitly unknown. Delivery does not update task isolation or quality/exposure state.

The queue has 32 slots with 16KiB declared ownership per event. At 24 outstanding entries, new `RESEARCH_DATA` writes receive a recoverable dependency before their body runs; Critical, control and telemetry remain admitted. A real durable drain below 24 resumes research. Delivery rollback, missing schema and ambiguous failure retain the same event and lease. Queue or memory-admission loss remains an explicit incomplete-coverage counter. An in-memory event lost before drain during process death is not durable evidence and is not claimed as such. The existing 2,000-row/8MiB fault-store ceilings cause capacity debt rather than eviction of evidence.

The three native SQLite cases in `initial-real-protocol/RECEIPT.json` all passed on frozen actual writer/helper/Runtime sources, with no source changes during execution. They exercise real 230ms delays inside actual coordinator transactions, business replay, diagnostic rollback and post-commit exception, startup-owned schema repair, actual backlog backpressure, Critical admission and subsequent resumption. `injected_counter` is an engineering fixture; these tests do not prove a learner improvement, Android timing or a natural prediction result.

Run with a complete current Android class tree in `--classes` and a frozen current API closure in `--joint-classes`:

```sh
SQLITE_JDBC=/path/to/verified/sqlite-jdbc.jar python3 tools/r31/run_writer_hold_fault_host.py \
  --classes /path/to/current/classes.jar \
  --joint-classes /path/to/current/compiled/classes \
  --out /path/to/unique/new/evidence
```

This remains a P01/P02 integration candidate. Whole-project Android compilation, unchanged API29/35 full fault matrices, device timing, 24h/72h and natural quality are not established by this receipt.
