# R77 retention: avoid scanning a wholly protected current range

The new report records 8,976,923,126 bytes of app storage, including a 5,225,709,568-byte live database and unchanged 3,717,898,240-byte old backups. This is disk occupancy, not measured RAM. The live database grew 747,073,536 bytes since the preceding report. Most action/label evidence remains inside the protected 5,000-draw tail; its retention cannot be bypassed to make a storage counter smaller. The report alone does not prove a retention failure from the 129 published archive rows.

## Actual change

`DataRetentionGovernorR900.prepareArchivePageCP12` previously ran the complete eligibility query in rowid order even when the table had no data older than the cutoff. On the installed action-table schema, SQLite chooses a table scan despite an existing target-issue index.

The new preliminary `SELECT 1 ... WHERE issue < cutoff LIMIT 1` checks only the necessary old range and uses the existing covering index. An empty result skips that table. A positive result still runs the complete original eligibility query. Generated special predicates that cannot be factored safely are left on the original path. The time-based equivalent is supported. The cutoff comparison remains strict `<`, so an equal cutoff stays protected.

This does not change page size, row ordering, retention window, current-period protection, certificate/training dependency pins, archive verification, publication permits, or generation-CAS deletion. It does not make a positive range query an authorization to delete.

## Development verification

`evidence/retention_range/result.json` records source overlay execution with actual product selection code, real SQLite, exact report DDL/indexes and the report's 5,000 draw rows. Synthetic action payloads vary table size independently of eligibility.

| Case | Original selector work | New range-probe work | Result |
| --- | ---: | ---: | --- |
| 50,000 current protected actions | 300,300–300,399 SQLite VM operations | 0–99 | No archive page; source unchanged |
| 1,500,000 current protected actions | 9,000,300–9,000,399 | 0–99 | No archive page; source unchanged |
| 50,000 current + 180 eligible old actions | Original complete selector retained | 0–99 | First 128 eligible old rows selected |
| 50,000 current + 180 actively pinned old actions | Original complete selector retained | 0–99 | No archive page; no deletion |

The four cases have 153 assertions, plus a second-JVM replay of the 131-assertion mixed case that produces the same durable page identity. Checks include equal cutoff, NULL/empty/malformed issue values, zero/negative time values, custom OR predicate fallback, missing optional index, real missing-table errors, and unchanged source hashes/counts. A separate process rechecked all four fixture hashes and counts after the runner ended.

`evidence/retention_cas/result.json` replays the existing R76 native tests against a private product-class copy with the changed governor compiled into it: 49 native/SQLite checks, 52 archive/publication/restart checks, and 8 concurrency/large-cell edge checks pass. These preserve independently verified archival before deleting 383 eligible rows; changed generations survive stale proof; duplicate proof cannot delete twice; short publication slices resume; the 5,000-draw tail stays intact; and failed/cancelled snapshots never authorize deletion.

Both runners expose `--final-bytecode` to disable product overlays. These development receipts are not final APK, Android SQLite-pool, phone latency, or long-duration device evidence.

## Explicit remaining limits

- If older rows exist but all are pinned, the original complete selector still runs. This change does **not** establish a hard execution deadline for that query or solve all-pinned scan cost.
- Tables without a suitable range index preserve correctness but do not receive a guaranteed speedup. Read errors propagate; they are never converted to an empty-range success.
- Storage reclamation depends on actual eligible old data, verified archive receipts and the existing reclaim process. This test does not prove the user's 8.97 GB will immediately shrink, nor permit clearing protected evidence or backups.
- Earlier scratch fixtures showed an unexpected hot rollback journal and a later large-file count/hash change. Those runs are excluded; `retention5/result.json` is explicitly invalidated. The origin of that host fixture mutation remains unproven. The accepted run uses separate unique `/tmp` fixture directories, explicit journal completion checks, whole-run terminal count/hash checks and an additional process recheck. No preliminary anomalous run is treated as product acceptance.

## Reproduction entry points

`verification/r77/retention_range_run.py` accepts `--jdk`, `--classes`, `--schema`, `--android-runtime`, `--report`, `--out`, optional `--fixture-root`, and `--final-bytecode`.

`verification/r77/retention_cas_run.py` accepts `--jdk`, `--classes`, `--schema`, `--android-runtime`, `--native`, `--out`, and `--final-bytecode`. It reuses the unchanged R76 test assertions and the restored typed/read-only R64 SQLite transport, with host filesystem adapters for Android-only APIs. It does not replace retention implementations in final-bytecode mode.
