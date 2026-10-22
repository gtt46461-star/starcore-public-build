# R77 input audit (R76 phone report)

Input: `检测报告10.13.zip`, SHA-256 `584f674314e5715b2c3a0cc08add8cae6f33ec773c1adfd2bb06feda4bdea50a` (20,883,953 bytes). Runtime sampled 2026-10-13 19:33:24 +08; inventory sampled 19:43:07–19:43:08 +08. File inventory is later/live, not the immutable SQL snapshot.

## Evidence identity and completeness

- Installed version is V57.21.2.0.76-R5.4.2.76-1013-O09; APK SHA-256 `757ff09c339358398146f7bebb8c5ed894356a02d22a2e1b6c67525802480e8e` (25,727,289 bytes); release manifest SHA-256 `f4c549365491c5f20e0a0e1370361dbc9442d94aae98a4e4be654df85baf92a3`.
- All 162 integrity assertions passed: 45 raw manifest members, member size/hash, manifest body/full hash, delivery ZIP size/hash/CRC, 9 attachments, 30 reference sites (14 distinct referenced members), source assets and two full runtime observations, 62 complete metadata table exports.
- 418 tables; 2,803 schema objects; 8,157,173 table rows; 96 unique contracts/results, no unevaluated checks and no query errors. Result counts by `health`: 4 FAIL, 8 WARN, 21 NOT_PROVEN, 43 OBSERVED, 16 NOT_APPLICABLE, 4 PASS.
- Declared exclusions remain raw database, APK, binary models/training/retention artifacts. `rawDatabaseIncluded=false` is intentional and must not be relabeled a complete device byte backup. Raw member integrity is verifiable; the archived compressed SCREPORT part files are not supplied inside the final single ZIP.
- Every nonpassing check is listed in `report_audit.json` with source method, member and excerpt. NOT_PROVEN is not automatically a code defect.

## Confirmed observations relevant to repair

1. Runtime is alive but analysis remains slow: 71 current-release events, request-to-commit P95 41,346 ms, maximum analysis concurrency 1, OOM 0. This is distinct from display rendering: 47,441 sampled frames, 25 above 33 ms, 8 above 50 ms, maximum 233.696 ms; truth-bind average 1.046 ms / max 27.922 ms. Good average frame statistics do not prove every countdown update on time.
2. Last 30 draws have 28 frozen predictions / 28 SHADOW, missing issues 202610130669 and 202610130693; last 120 have 97 predictions / 23 missing. Missing frozen predictions are real coverage failures. They cannot be reconstructed after the draw as valid predraw observations.
3. Issue 202610130693 has an explicit settlement terminal `NO_PREDRAW_CANONICAL_TERMINAL`, failure class `EXCLUDED_NO_PREDRAW`, code `FROZEN_OBSERVATION_MISSING`; there is no shadow row for that issue. All 1,288 existing predraw obligations are settled, no pending settlement tasks or data-blocked tasks in RSEC-315. Thus `SETTLEMENT_OOS` based solely on latest drawn 693 vs latest settled prediction 692 describes the wrong fault: the lost obligation is predraw analysis/coverage, not a missing settlement for an existing frozen prediction.
4. Latest target 202610130694 has canonical committed, deferred evidence PENDING, 360/1200 action rows persisted and one pending deferred job. RSEC-057 and finding B14-F070 treat this transitional state as action-universe failure. Source repair should classify pending separately and still FAIL on completed/inconsistent evidence. This does not prove that the deferred task will eventually complete.
5. Stored app data now 8,976,923,126 bytes; live database 5,225,709,568 bytes, +747,073,536 since prior report. Old backup files total 3,717,898,240 bytes and are unchanged. This is disk storage, not 8–9 GB RAM. Immutable SQL snapshot size is 5,192,216,576 bytes at its earlier sampling time.
6. Largest row-count growth since previous input: `research_row_revision_v2` +768,289 (now 4,385,698); action-label +306,000; action-space +300,000. Causal disk-I/O blame needs profiling; row counts alone are not timing evidence. Revision retention must preserve row-generation/CAS protections when compacting.
7. Retention registries have exactly two PRUNED archives, containing 128 draw-canonical rows and 1 issue-projection row. No whole-device cleanup or capped secondary evidence storage is established by those two archives. Last 5,000 raw draws are present.
8. Crash count remains 172 and latest recorded crash timestamp is 1791806924296 (October 12, before R76). OOM/StackOverflow remain 0. System exit entries include installPackageLI stops, not new observed crashes. `windowCrashCount10m=1` next to this old lastAt may be an unrefreshed diagnostic counter; source inspection is needed before classifying its cause.
9. `v57_runtime_event_outbox_r926`: all 5,721 rows COMPLETED; schema migration journal 229 rows all COMPLETED. No incomplete schema-migration or event-outbox queue is visible at this snapshot. Modern DEV receipts/artifact identities remain zero (separate training agent analysis required), while legacy online numeric update records exist; do not say all learning is zero.

## Delta from preceding phone input

- RSEC-053 PASS → WARN / RETEST_REQUIRED (current deferred evidence pending).
- RSEC-057 PASS → FAIL (current partial action evidence).
- RSEC-061 NOT_PROVEN → WARN.
- RSEC-066 stays FAIL; state BLOCKED → BLOCKED_UPSTREAM.
- Actual slow analyses/coverage gaps remain repair obligations irrespective of these diagnostic classifications.
