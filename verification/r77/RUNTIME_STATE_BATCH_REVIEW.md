# R77 runtime-state batch review

The tested product change replaces 9 / 31 separately admitted state writes with one
`DbStore.setStates` call. The public setter now uses the existing coordinator's
atomic throwing API. A negative `insertWithOnConflict` result is treated as failure;
it cannot silently commit the rest of a batch.

`runtime_state_batch.java` executes the actual product DbStore and coordinator.
`HostProductR974` bypasses Android process startup; the Android database adapter
executes SQL against an actual SQLite connection. A real `BEFORE INSERT` trigger
rejects the ninth runtime-state field. The test adapter either preserves the SQL
exception or translates that exception into Android's `-1` insert return. It does
not replace product transaction, writer, or state-update logic.

Current development results: 135 checks pass with the real JVM monotonic clock.
The earlier 89-check receipt predates optional telemetry coverage and is retained
as development history, not timing evidence. The current checks cover both update overloads, all 31 / 9
field values, one writer admission and one SQL transaction, a common observation
timestamp, null / empty-key behavior, conditional summary retention, and all eight
combinations of SQL error transport, standalone / enclosing transaction, and
update overload. Each fault leaves all pre-existing values and timestamps intact;
the enclosing transaction's sentinel is rolled back too. Removing the trigger
allows the complete retry to commit.

Two exact-base negative controls are retained. The previous DbStore fails the
single-transaction check. Its previous `setStates` also demonstrably commits a
partial batch after the Android-style `-1` result (`propagated=false`,
`partial_change=true`). The new setter produces `propagated=true`,
`partial_change=false` for that same real SQL failure.

The optional `trySetTelemetryStatesR77` entry point is separately checked for
unchanged successful field values, one atomic write, complete rollback and `false`
for either SQL exception transport, and successful retry. Nested calls are rejected
before the first batch insert; an outer owner can safely catch this rejection and
commit its own unrelated sentinel with all pre-existing state fields intact. The
strict `setStates` failure-propagation tests still run independently.

The actual `saveBackgroundSliceState` helper is also covered. A SQLite trigger
counts real inserts and rejects the second field, independent of HashMap order.
For both thrown SQL errors and the Android-style `-1` result, the helper keeps the
dashboard failure within its optional boundary, rolls back all six fields and
timestamps, releases the transaction, then commits all six correct fields on
retry. Null strings, negative cost normalization, empty tasks, and rejection of a
nested call before its first write are checked. An intermediate-code negative
control, retaining strict `setStates` at this callsite, fails the no-escape
assertion. This is additional evidence for the final callsite change, separate
from the two exact-base negative controls.

The current receipts explicitly identify development overlays. Final verification
must use the sealed build's full product class directory and `--final-bytecode`,
without `--development-dependencies`. The runner rejects product-class collisions
in test adapters and checks product bytecode hashes before and after execution.
The runner recompiles the checked-in `System.nanoTime` clock adapter and verifies
that clock at runtime; no fake clock or accelerated deadlines are used in current
receipts. These SQLite host contracts do not prove Android pool behavior or physical-phone
latency; `SqliteBusyPoolR77Test` provides a separate real Android pool contract.

Compatibility note: `setStates` now propagates database failures instead of
silently returning. Callers must preserve the original error boundary. An enclosing
transaction which lets the error propagate rolls back fully; this test does not
claim atomicity when a caller deliberately catches the error and commits anyway.
