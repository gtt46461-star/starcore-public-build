# R31 P01 task protocol implementation

Source baseline: `5237b265a27fe5ad27fd842901c445d5f8c27749`. Parent integrates, commits, builds and records the final APK identity. This receipt does not close device or natural-quality acceptance.

- B01-C01: FailureDispositionR31 and WaitKindR27 distinguish busy, cancellation, dependency, stale ownership, faults and fatal storage/process failures. UNKNOWN commit waits for durable resolution; committed diagnostics cannot be retried as rollback. Nested VM/thread/linkage fatal errors retain the original Error.
- B01-C02: RuntimeCoordinatorR26.claimWithinR31 / requireFence use owner, fence, current boot and elapsed lease. R26/R27 success and failure receipts compare the same owner/fence. Prepared claims retain logical commit chunk sequence through renewal. Revoked drafts reload through RuntimeR27.invalidateTaskDraftR31 only after the parent and its actual helper group exit.
- B01-C03: inherited quarantines receive finite control records without clearing errors. One shared recovery control admits at most one half-open task per second. Only a new same-owner/fence durable artifact resets recovery; six repeated same-root rounds become NEEDS_FIX.
- B01-C04: bounded helper queue and one global continuation/helper permit controller; helpers carry task/owner/fence. Cancelling a Future does not free active helper ownership. PreparedTask combines parent and helper termination. Pool.close is immediate and idempotent, with a bounded ten-second controller and continued explicit DRAINING_NOT_TERMINATED ownership. R26 refuses replacement until actual termination and retains the shared database meanwhile.
- B02 integration: affected writes use explicit control/data/retention/telemetry/migration contexts. RuntimeCoordinator retention slices no longer run ensureProtected DDL. Existing schema tasks remain explicit migration work. r30_cache_rebuild creation moves to startup migration.

Schema: SchemaR27.R31_WRITE_REVISION=1; journal domain RESEARCH_R31_WRITE via SchemaDomainCoordinatorR926.migrateR31WriteProtocol. R31_WRITE_DDL creates r31_commit_receipt and task index, r31_task_lease, r31_recovery_clock, and the original r30_cache_rebuild schema. Guarded task-control columns and required-column checks run only in ensureR31WriteProtocol. No DROP, evidence reset or live-database destructive experiment was added.

Run: `source /workspace/starcore-environment/activate.sh; python3 tools/r31/run_task_runtime_host.py`. The machine receipt hashes the actual production sources compiled before the run and records scope explicitly. Native SQLite/JDBC uses host platform adapters; it is not Android SQLite, physical-device, 24h or 72h evidence. Six direct JUnit disposition tests are in R31TaskFailureDispositionTest. Observation tests retain original report 60/14 and R27 13/13/11+1+1 facts.

A close-idempotence regression discovered during implementation is retained as native-runtime-close-idempotence-FAIL.log; native-runtime.log records the subsequent corrected run.

Remaining: Android instrumentation execution, actual wall-clock/reboot changes, all model kill-point recoveries, device resource and timing distributions, 24h/72h continuous run. Broad runtime-DDL tracing outside the repaired retention/cache paths is not claimed by these tests. Natural quality remains NOT_PROVEN.
