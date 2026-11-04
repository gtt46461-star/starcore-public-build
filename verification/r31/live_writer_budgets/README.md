# R31 named live producer writer budgets

Scope: P01/P02 only. `LiveShadowsR27.java` and `QualificationInboxR29.java` are the only changed production files. Base: `3246ccf8eb6f7b2f7b9d0fe77fc18a70ab958056`. No P03 source, tools, shared schema/coordinator/queue/runtime, app identity, version, or construction ledger was edited.

Current evidence is [CURRENT_EVIDENCE.json](CURRENT_EVIDENCE.json), bound to the [fresh run receipt](runs/20261104T174330Z/receipt.json). Earlier timestamped runs retain development evidence and the recorded producer/harness hashes. The pre-174330 JDBC adapter used JDBC commit/rollback, which implicitly opened an empty transaction. Those physical-frame measurements are superseded by the current audited BEGIN/END run.

## Actual results

Two exact baseline/new producers are compiled separately against a private copy of the supplied actual Android app support jar, SHA-256 `892e5be7feb3609755c6746ea080644fd19427a90f8d9ab8493c964af1f01fa8`. The new sources call the actual `RuntimeR27.submit`, immutable queue and `DatabaseCoordinator` implementation. A JDBC platform adapter executes real native SQLite transactions, task/session/boot/elapsed fences, compare-and-set reads, typed progress and durable commit receipts. No user database is used. The audited adapter ends each outer native transaction once, avoids Xerial’s automatic empty BEGIN after JDBC commit/rollback, and counts internal last-insert-rowid reads when used.

Each maximum below is for a **physical transaction**, including BEGIN/END, read API calls, business DML, source/CAS checks, fence checks, progress and durable receipt. Direct changed-row totals include protocol DML and attempted changes subsequently rolled back. The binding limit in this 512 MiB host run is 262,144 bytes.

| Complete logical input | Baseline SQL / direct changed rows | New SQL / direct changed rows | New max bound bytes |
| --- | ---: | ---: | ---: |
| 160 unsettled original frozen rows | 332 / 323 | 37 / 27 | 3,722 |
| 128 missing draw periods | 268 / 259 | 31 / 17 | 1,694 |
| 256 contiguous opportunities | 267 / 299 | 28 / 20 | 4,281 |
| 128 existing pending periods whose draws arrived | 13 / 131 | 23 / 9 | 961 |

The baseline processes exit with the retained original `AssertionError: BASELINE_FAIL_REPRODUCED_R31_WRITER_BUDGET`. The resolution counterexample catches row fanout even though its API SQL count is small. Full SQL, read/DML/fence/receipt counts, bind bytes, outcome and transaction duration are retained in `*.frames.json.gz`; full process output is in `*.log.gz`.

The final controlled run covers 4,500 physical native frames. The fault harness passes 27,766 assertions, including:

- Every qualification prefix (16), settlement slice (14 for 160 rows), missing-draw slice (16) and resolution slice (16), at both the first business DML and the durable receipt insertion: 124 rollback locations. Each verifies the committed prefix, atomic summaries/state, and complete reentry.
- Cancellation before submission and during the transaction, stale persisted owner/fence, and successful current-owner continuation for all four paths.
- `AFTER_COMMIT` diagnostic exceptions for all paths, followed by duplicate reentry without another sample or settlement.
- Four actual JVM exits via `Runtime.halt(73)` after a durable commit. A fresh JVM proves the one committed page, resumes all original input, and reenters again without duplication.
- Source changes between preparation and commit reject settlement/ACT/pending plans. A late non-ACT draw preserves its preregistered zero score. Missing business tables or the existing settlement index yield classified `DEPENDENCY`, enter no writer transaction, and issue no business DDL.
- After each tested prefix, receipt count equals `eprocess.n` and cursor total. Receipts remain unique and contiguous; numerical e-process log values and sufficient statistics match the original `State.observe` double bits. Global index/alpha, the independent exposure ledger, the REPLAY sentinel and the REAL trade count/6.9 funds sentinel retain their original values.

The host SQL transaction p99 was 4.640 ms and maximum 36.565 ms in this run. These are **native host SQL transaction timings**, not Android writer acquisition/hold, scheduling, filesystem, or latency acceptance.

## Producer changes

`LiveShadowsR27.settle` keeps the original 640-row selection window and all original outcome mathematics. It submits independent, source-guarded outcome slices of at most 12 rows; each slice also updates exactly its affected opportunity summaries in the same transaction. Pending bookkeeping is drained separately before settlement. The reported count advances after a committed slice. Cancellation/precondition failure leaves the remaining original rows available for reentry.

`QualificationInboxR29.consume` keeps `MAX_STEP=256` as the logical window and preserves the original maximum of 64 such windows per pass. It commits immutable 16-opportunity prefixes through the existing owned durable submit path. Each prefix visibly contains all qualification receipts, the complete numerical e-process update with expected `(last_ordinal,n)`, and cursor advancement in the same CAS transaction. Its callback performs bounded read-only validation of the prepared scoring facts. JSON replay markers are prepared before the writer. The legacy evolution qualified flag is echoed in guarded 16-primary-key pages after the e-process commit; all matching attempts are eventually updated.

Pending discovery still considers the original 128 missing periods, preserves backoff/STALE rules, and submits every selected row in pages of eight. Each row retains its original identity and expected pending state; draw arrival is revalidated under the writer. Resolution walks existing pending keys in pages of eight to a captured upper key, retaining all original rows and removing the global fanout UPDATE. No schema or storage migration is introduced.

Prepared budget checks include visible operations, the actual bounded guard reads and guard bind values, plus a conservative allowance of 32 protocol SQL calls and 8,192 protocol bind bytes. Resource rejection is typed `DEPENDENCY/NOT_SUBMITTED`. The actual physical trace verifies the allowance against the fixed supplied protocol implementation; this is not a new universal shared-queue enforcement mechanism.

## Existing tests and exact contract adaptations

[existing_test_contract_changes.json](runs/20261104T174330Z/existing_test_contract_changes.json) records original/adapted SHA-256 values and exact diffs. The checked-in `tools` tests were never edited. [legacy_host_contract.patch](legacy_host_contract.patch) is the exact two-file adaptation, verified with `git apply --check`; root can apply it to the unified tree without changing any full-input or numerical assertion.

The unchanged R29 qualification test first passes its original ordered, randomized/late, killed transaction, legacy rebuild, conflict and separate-pending-page checks. Its Q6 then intentionally fails because it requests a 30-row `step` single batch, exceeding the new physical prefix contract. Its temporary copy plans 16, proves the same stale-step rejection, then consumes the original remaining 14 through production `consume` before evaluating the original full 30-row numerical/reference assertions.

The unchanged R30 settlement test intentionally rejects the old fixed `4 × 160` expectation. Its temporary copy derives physical size/count from the new bounded constant and retains every original 640-row, full row-for-row pre-R30 reference, rollback and complete reentry comparison across all three original random seeds. The unchanged R30 pending-draw test passes. All three adapted/unchanged appropriate tests exit zero; their original and adapted sources and complete outputs are retained.

## Explicit remaining work

This is a named-producer host mechanics result, **not full B02 completion**:

- `LiveShadowsR27.freeze` and `certify` still prepare potentially large policy/model payloads and broad batches. They were preserved and are not covered by this budget pass.
- Existing `needsRebuild`/`rebuild` compatibility repair remains unchanged: it archives old e-process state/certificate, deletes existing qualification receipts, resets the legacy state and replays original frozen rows. Its fanout and reset/rebuild continuation need a separate, non-destructive compatibility design. It was not disabled or represented as bounded here.
- The package-private legacy `settleBatch` test adapter combines pages into one batch. Production has no call to it; it is not a bounded production submission API. `pendingDraws` returns a bounded compatibility page; actual production drains the complete `reconcilePendingDrawsR31` continuation.
- Shared callbacks, universal writer-side budget enforcement, protocol key/revision semantics and shared automatic progress computations remain shared-protocol work. No claim is made for arbitrary other producers, custom database triggers, or future protocol growth.
- Controlled bootstrap, host platform adapters and synthetic rows prove SQL/state mechanics. Android scheduling/WAL/lock hold, full Gradle/unit/lint gates, signed APK, device, 24/72 h and natural predictive quality are **NOT_RUN / NOT_PROVEN**. No certificate, freeze coverage or natural REAL/FUTURE quality pass is inferred from these fixtures.

## Reproduce

From this worktree, with the supplied stable support jar, Android SDK and SQLite JDBC paths available:

```sh
python3 verification/r31/live_writer_budgets/run_native.py
```

The runner copies dependencies into a private `/tmp` workspace before compilation, never invokes Gradle, never reads mutable main-worktree classes, retains baseline errors, and checks that both owned production sources stayed unchanged during the run.
