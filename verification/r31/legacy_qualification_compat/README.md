# Append-only legacy qualification compatibility

P01/P02 only. The sole production file changed is `QualificationInboxR29.java`. `LiveShadowsR27`, the original host tools, shared schema owners, training/model implementations, app identity/version and construction ledgers are unchanged here. Root integrates the startup owner as the separate `RESEARCH_R31_QUALIFICATION_RECEIPT` revision 2 unit; this source and that owner must ship together.

[CURRENT_EVIDENCE.json](CURRENT_EVIDENCE.json) identifies the current exact-source run and its [receipt, full logs and SQL frames](runs/). Earlier runs and candidate failures are retained with their own source hashes and are superseded where explicitly noted.

## Original failures actually reproduced

- [Original rebuild](baseline_reproduction/receipt.json): actual producer → owned runtime/queue/coordinator → native SQLite changed `eprocess.n` from 256 to 0 and deleted all 96 remaining receipts. The original assertion and SQL are retained; no repaired result is substituted for this failure.
- [Candidate receipt CAS failure](receipt_race_counterexample/receipt.json): count/min/max checks failed to detect an existing receipt digest changed after preparation. The last missing prefix could appear complete. The failed candidate source and original error are retained.
- [Strict-guard foldback failure](foldback_duplicate_counterexample/receipt.json): the actual `TableCopierR30.copy` uses `INSERT OR REPLACE`; the first strict insert guard incorrectly rejected an entirely identical old receipt. Original source, native stored v1 SQL and failure are preserved.
- [Writer-count candidate](writer_count_counterexample/QualificationInboxR29.count_guard_candidate.java): the former callback performed a full count/min/max of the experiment's receipts. The runner executes this exact candidate on 100,096 real rows under a native SQLite VM progress handler and retains its deliberate failing assertion. It compares the repaired writer on the same input; no latency claim is inferred from instrumented VM timing.

## Compatibility behavior

The old destructive `DELETE receipt → zero e-process → replay` path is removed. Compatibility never updates an existing receipt, e-process row, global index, alpha, model, trial identity or exposure ledger.

An already registered original continuous prefix is recomputed outside the writer, in cancellable pages with bounded retained memory. Recovery is permitted only when original opportunity identity, registration boundary, ACT timing/probabilities/results, every existing receipt and the entire numerical e-process match. Numerical state includes alpha, rate, log values, sufficient statistics, counts, last ordinal and terminal state; equality preserves the stored double values.

Proven missing receipts are appended eight at a time under the original experiment/ordinal keys. Each physical transaction checks the complete stored state, calendar, global receipt append watermark, cursor, certificate metadata, audit predecessor, source revision, actual guard definitions and prepared missing rows. It commits its audit/receipt/cursor effects and the existing durable receipt atomically. No numerical replay or full receipt count is performed inside the writer. Existing cursor exposure is retained while restoring; only the final verified page may monotonically reconcile it to the original `n`.

The watermark is **global** `max(rowid)` with no experiment filter, verified by actual SQLite `EXPLAIN` and native VM steps. Readiness rejects `WITHOUT ROWID` and a user column shadowing `rowid`. With the immutable guards, existing rows cannot be replaced/deleted; a new append changes the watermark. Another experiment's append conservatively retries the prepared legacy page. The real interleaving test proves retry, normal owner liveness and eventual completion without fault quarantine. Full count/range and numerical verification remain outside the writer.

During recovery, the old e-process remains preserved evidence and the experiment has an explicit `LEGACY_RECEIPT_EVIDENCE_RESTORING` dependency. Partial restored receipt count is not presented as a new complete mathematical state. `Pass.rebuilt` records committed compatibility artifacts/pages, not new observations; sample counts remain the original e-process/receipt identities.

Unprovable boundary, noncontiguous exposure, conflicting original receipt, missing original input, inconsistent mathematics, excess cursor or invalid original timing remain explicit per-experiment dependencies. Existing receipts and old numerical state remain intact. An unreliable ACTIVE certificate changes only its status to `REVOKED_LEGACY_EXPOSURE`; original metadata and gates hash are audited, and original gate JSON bytes remain in their row. Other healthy experiments continue through the normal consumer. Unexpected storage/program errors propagate; they are not converted into a successful repair.

This owner only appends a versioned audit to the existing `r29_qual_rebuild` ledger, recording original state/calendar/cursor/receipt summary/watermark and certificate metadata. Existing audit rows of all formats are preserved. No cache becomes migration authority and no new business schema is created.

## Startup-only protection contract

Stable public APIs in the owned class:

- `legacyReceiptGuardDdlR31()` and `legacyReceiptGuardObjectsR31()` describe the three guards.
- `ensureLegacyReceiptGuardsR31(db)` requires an active startup migration transaction. It validates all existing same-name definitions before any DDL, upgrades only the exact known old strict insert guard, rejects unknown definitions/types, and validates the final contract. Only that known trigger may be dropped; no data row is changed.
- `requireLegacyReceiptGuardsR31(db)` performs readonly checks of the real composite primary key and canonical stored trigger SQL. Business paths call only the readonly API.

UPDATE/DELETE are refused. An INSERT for an existing primary key becomes an ignored no-op only when every stored non-key value and SQLite storage type match. Any different value is refused. This preserves legitimate identical foldback/replay while preventing `OR REPLACE` from overwriting original evidence.

Owner journal/manifest integration belongs to root. The host test executes the exact owned startup APIs through actual coordinator MIGRATION transactions; it does not claim to have validated root's separate startup journal implementation.

## Actual verification

The final run executes the fixed actual Android support jar on a private native SQLite host adapter. Its hash, both production source hashes, harness/adapter hashes, commands, original output and every physical frame are recorded. No mutable main-worktree classes or Gradle are used.

- Original 256 observations with 160 missing receipts, and with zero receipts/no cursor: full original state and old receipt values retained; all evidence is restored in eight-row pages. Old alpha/index and independent exposure, REPLAY and REAL funds sentinels are unchanged.
- Forty rollback positions: before receipt writes and before the durable commit receipt, at each of twenty pages. Rollback and reentry retain the original state and exactly the committed evidence prefix.
- Cancellation before/during commit, old owner, source/state CAS, and wrong same-name guard: no stale publication, audit or certificate modification.
- Submitted commits with failing diagnostics remain committed once. An actual JVM `halt(73)` after a page is followed by a fresh JVM that proves the old state/receipt values and resumes once.
- Seven incompatible legacy cases remain blocked while an independent healthy experiment consumes all sixteen of its original opportunities.
- Missing guard causes dependency with business DDL zero. Actual schema migration failure rolls back; repeated startup migration preserves data. The exact old v1 insert guard upgrades, rollback preserves it, reopening preserves the new contract, and unknown definitions are not dropped.
- Normal sixteen-row prefix, all 256 normal opportunities, one hundred identical durable commit receipt replays and one hundred business reentries retain their original counts and mathematics.
- Actual `TableCopierR30` performs one hundred identical receipt foldbacks without replacing old values. A changed old-source receipt is rejected; both root and source evidence remain.
- On 100,096 retained receipts the old writer count performs at least 903,500 actual native VM steps. The global-watermark writer performs about 2,700 steps on the same data, retaining every original row and the old `n`. The exact final run's values and plans are in its machine-readable evidence. A different experiment appending between preparation and commit triggers a fair retry and eventual recovery.
- Actual existing retention archive setup and phase-17 cleanup coexist with qualification protection. The separate original full `ResearchRetentionR30HostTest` also passes after only installing the startup guards in its temporary fixture; its existing archive/model/matrix/file assertions are unchanged.

Physical cost includes traced API reads/DML/fences/audit/cursor/durable receipt/BEGIN/END plus the installed guard's indexed EXISTS reads. The guard program is validated against actual stored SQL and native SQLite `EXPLAIN`. It requires one lookup for a new receipt and a second for an existing key; duplicate outcomes add that second read. This is explicit conservative accounting, not an SQLite VM trace. The current evidence reports measured bounds and host timings. Host SQL duration is not Android writer acquisition/hold acceptance.

## Existing regression contracts

[existing_contract_regression_v2/receipt.json](existing_contract_regression_v2/receipt.json) preserves actual outcomes:

- Original normal/random arrival/conflict/model preservation checks pass with the previously approved physical-prefix test adaptation.
- Original 640-row settlement/reference/rollback tests and original pending-draw tests pass.
- The full original retention test passes; its only temporary fixture change installs the actual startup guard owner APIs.
- Old R29 Q3 and Q11 still require deleting/resetting an inconsistent legacy experiment and counting the replay as a new continuous trial. Their original failures remain recorded as **expected contract failures**, not waived PASS. Root must update those two expectations to preservation/dependency or exact verified append-only compatibility; the implementation does not restore the forbidden reset behavior to satisfy them.

## Remaining scope

Only legacy original inputs with the required proof can be recovered. Missing/ambiguous original evidence is retained as unresolved dependency; no forecast, new alpha, alternate identity, role relabel or retrospective choice is invented. Source changes during proof defer it.

Freeze/certify large payloads, other producer budgets and the broader retention/storage work remain OPEN. `TableCopierR30` is exercised here specifically for qualification receipt copies; other tables' replacement/budget semantics are not accepted by this module. P03 training capability work is not opened. Android/Gradle/full unified tests, actual writer-hold SLO, device, 24/72-hour and natural predictive quality are NOT_RUN / NOT_PROVEN here.

Reproduce from this worktree:

```sh
python3 verification/r31/legacy_qualification_compat/run_native.py
```
