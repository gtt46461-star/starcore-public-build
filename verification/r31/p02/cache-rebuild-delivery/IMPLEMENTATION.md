# Cache rebuild diagnostic delivery

Baseline: `241a93848851440fdfd21155ae400ff2354eceb2`. Branch: `construction/r31-cache-rebuild-20261104`. Only `RuntimeR27.java` changes production behavior. The main working tree and its uncommitted repairs were not edited. This is a P01/P02 contribution to B02-C02/C03; it does not reopen P03 or modify model/training mathematics.

The baseline production consumer polled up to 64 producer events before cancellation/admission/transaction completion. The native SQLite negative control reproduces both cancellation and real trigger-induced transaction rollback leaving the producer queue empty and the event absent from SQLite; the next retry finds no event. This is executed evidence from the original complete production class, not a static interpretation of its loop.

The replacement snapshots at most 16 producer objects and 8 KiB of immutable bind payload. It leaves the exact original objects in the queue. A single process-local prepared chunk holds frozen primitive/String inputs, a nonce-based event-group identity and a 64 KiB declared Java working-set lease covering retained row/object copies, bounded UTF8 hashing, hashes and immutable batch preparation. It does not represent measured heap or SQLite native memory. Hashing streams bounded inputs rather than building an unbounded JSON copy. Oversized input leaves its original queue object in place and returns a typed dependency; no silent truncation or task-fault budget is used. Producer-queue allocation policy itself is inherited, not claimed as a new global hard byte bound.

One finite submit attempt runs per ABI slice. A busy/freeze gate returns control after the existing wait budget. Cancellation, rollback, missing schema, stale ownership and unresolved COMMIT keep the same prepared events and receipt identity. Owner/fence change only audit fields and fence guards. An already committed receipt is replayed without another append. Successful receipt observation removes original objects by identity, so an equal-valued event appended during the wait is preserved for its own later chunk. The shared prepared lease is retired after successful consumption; cancelling an old pool alone cannot erase unconsumed evidence. The controller lock is not held over SQLite writes.

Schema readiness reuses the existing coordinator's readonly object/column checks. Missing cache/receipt schema is DEPENDENCY, not a runtime CREATE. Existing startup schema and `r31_commit_receipt` are reused; no migration or schema-owner edits are required. Idle empty queues avoid schema/database reads.

`run.py baseline` and `run.py fixed` compile complete actual production RuntimeR27/queue/coordinator/fence/schema/memory producers ahead of a frozen prior Android support artifact, then execute native SQLite JDBC with explicitly documented HOST Android clock, connection, DbStore/DbHelper and logging adapters. Only orchestration/construction boundaries are adapted; event delivery, queue admission, fencing, commit/receipt and schema checks execute actual production methods. No shared Gradle/build directory is used. The source/adapter support and exact compiler/runtime commands are recorded.

The repaired native suite covers:

- Cancellation before preparation and later valid consumption.
- Real SQL rollback retaining frozen events, receipt identity and declared memory, then retry.
- Bounded backpressure, immutable inputs despite mutation of the producer's outer array, new-owner retry and equal-valued appended events.
- Actual native COMMIT followed by injected END-return loss and unavailable independent readback: UNKNOWN retains the chunk. A different valid owner replays the same committed receipt with no duplicate row.
- An actual post-commit diagnostic callback throwing while business commit/ack remain successful exactly once.
- Missing schema reporting DEPENDENCY with input retained and no runtime DDL.
- Oversized input reporting typed DEPENDENCY, retaining the source and releasing unretained preparation memory.
- A full 256-event queue drained fairly in 16 finite chunks without starvation or duplicate appends.
- Real in-flight cancellation, failed competing-owner preparation, actual old dispatcher/pool exit, then a new pool consuming the retained chunk without removing a newly appended equal-valued event.

The final suite records 31 named runtime stages and their actual SQL API traces. Each steady full 16-event chunk uses 32 SQL API calls, including schema/fence reads, transaction boundaries and the receipt insert; these traces establish the measured HOST statement bound for this consumer. All named business stages execute zero CREATE/ALTER/DROP/etc. Setup migration, deliberate test trigger/rename fault injection and other app paths are outside that business DDL count. These results do not establish Android writer hold-time P99, arbitrary external triggers, full-app DDL=0, physical I/O or native-memory budgets.

Durability limit: events and the prepared chunk remain process-local until the SQLite commit receipt. Uncommitted diagnostic events are not preserved after process death. This deliberately bounded retry protocol is not the training/feedback outbox, and it must not be counted as their crash-durable guarantee. A durable diagnostic inbox would require a separately owned schema/producers migration. No DB clearing, model deletion, mainline-cache cleanup, statistics relaxation, APK substitution or natural-quality claim is involved.

Android build/device, install/upgrade, 24/72-hour operation and natural future quality are NOT_RUN/NOT_EVALUATED here. Whole-app integration/rebind/release remains the root gate after cherry-picking this branch's commit.
