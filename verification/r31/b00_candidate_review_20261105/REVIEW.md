# Read-only Warm integration and B00 card review

Scope: `/workspace/r31-integration-20261105`. The retained initial observation
counterexample is tied to its exact pre-fix five source hashes, not to later source.
Root implemented the phase/fingerprint, URL redaction, diagnostic links and normal
CLAIM_WAIT correction. The complete native after-test binds five sources at
`810818d5a8f73e886dbeb0ad593a6fc6bddd3665`; see the sibling observation pipeline.

## Warm shared integration

Source review found no API or dependency-order mismatch in the submitted hunks:

1. Legacy and the two Foundry callers use the three-argument API. The four-argument
   compatibility overload still delegates to the closed dispatcher and never calls
   arbitrary callbacks; the original complete publication regression was preserved.
2. Both runtime migration hooks run after Runtime/SU2 construction and
   `ensureProtected`. The required runtime columns and complete candidate archive
   therefore precede Warm's exact index/archive readiness checks. The startup-only
   method requires an active transaction.
3. The Warm domain owns the actual single index at REV1. Its journal path is
   `prepareRepairIfDrift → runStep → requireSchemaR31 → verifyAndStamp`, so unknown
   definitions cannot gain READY through object-name existence alone. The current
   hardcoded one-object contract equals the helper API; no extra DDL was added.
4. Foundry and retention prepare outside the writer. Retention 4/5 remain on their
   durable phase while `more` is true. Both writer fence edges remain. Business
   methods verify readiness; the new migration DDL is only in the startup hooks.

This is source/API review and the previously bound host evidence, not a new claim
of complete cold-start Android migration or a reopened Warm timing audit.

## B00 acceptance against implementation and real evidence

The exact source of the obligations is `ledgers/cards.json`, B00 C01/C02, and
`docs/10_运行并发后台逐文件设计.md` lines 40–63. Neither old card status nor document
existence is used as implementation acceptance.

| Obligation | Assessment | Actual source/evidence and remaining work |
| --- | --- | --- |
| C01 fixed report/package/blobs; old-line mismatch rejection | Satisfied in the checked snapshot | Actual verifier read-only execution returned sealed package PASS and 190 anchors PASS. Current divergence blocks old-line automatic patching. Wrong commit/missing/mismatch/dirty/signature rejection tests are implemented; signature adapter tests are explicitly mocks. |
| C01 preserve 1,832 requirements and 299 historical cards without inventing acceptance | Satisfied for preservation | Actual verifier checks original byte hashes, row counts and preservation map. All semantic acceptance remains NOT_PERFORMED. This is not implementation proof for 1,832 features. |
| C01 exact same-build source/tree/dirty/plan/migration identity and all changed-file owner mapping | Partial / final evidence pending | Source and build hooks exist, dirty candidate correctly reports dirty=true; release is blocked. The initial read-only identity is at f607 and is not current final-APK identity. C01's assigned owner is completing current change ownership and identity validation. Re-freeze and check final sources; do not refresh expected hashes to hide a mismatch. |
| C01 actual signed APK/applicationId/version/ABI and installed identity comparison | Missing final release evidence | No final APK was built by this module. Retain this as a release-stage proof item, not an invented device pass or a blocker for preceding source work. |
| C02 exact original task counts | Satisfied for historical recount | Actual preserved report gives total60/quarantined14, R27 total13/quarantined13/claim11/cancel1/student1. The 11 wrapper roots remain unobserved; SQLite busy is not inferred. |
| C02 bounded chain, UTF-8 stack, safe diagnostics and declared outcome/phase | Satisfied for named corrected cases | Actual source limits16 causes/16KiB stack and detects cycles. Root repaired declared phase/outcome and operation/phase fingerprint separation without changing old rows. This review's real synthetic long-URL negative exposed truncation-before-@ leakage; the retained rerun on corrected source passes. Native persistence/report after-tests retain COMMITTED/AFTER_COMMIT. |
| C02 aggregation/quota/drop limits | Satisfied for actual tested entry | Actual persist+coordinator exercises 2,000 rows and the 8MiB boundary; original samples stay, repeat aggregation works, drop count flushes. Other error producers sharing the table retain their own finite contracts. |
| C02 telemetry failure cannot undo business; next report reveals incompleteness | Satisfied for same-JVM native tested path | Real coordinator business+receipt, real pool task receipt→observe, two native SQL failures, actual report capture and subsequent counter flush all pass. Process-only unflushed counts can be lost across death and a separate diagnostic PID cannot observe another PID's in-memory counters; that scope must remain explicit, never read as global zero missing errors. |
| C02 raw scheduler vs derived liveness; event_id linkage | Satisfied for named corrected paths | Existing status precedence regression preserves all14 quarantines. Root's read-only link projection now includes task owner/fence and reports UNKNOWN_OR_OUTSIDE_SAMPLE for no displayed match. Native actual capture links the one stored event with exact owner/fence and does not change raw task state. |
| C02 busy/owner/syntax/full/cancel distinct and policy correct | Helper coverage satisfied; platform scope limited | Existing 34 helper assertions use explicit Android-exception doubles and distinguish them; native task/telemetry persistence additionally executes actual SQLite failures. These are not Android framework exception-injection or device proofs. |

No further production change was made by this reviewer. The smallest corrections
identified were the precise declared phase/fingerprint fields, safe truncated URL
authority redaction, read-only event linkage, and receiptExpected distinction for
claim-null attempts. Root implemented those. Remaining evidence is bounded to its
source and platform scope; final batch/release closure belongs to Root's unified
checks. The P04 cut was cancelled as instructed.

`run_probe.py --repo <candidate>` recompiles the original synthetic URL probe on a
copied current pure-Java source snapshot. Its printed chain/stack phase boolean
only describes those two fields; corrected declared phase is a separate property
and is validated at the real persist/report boundary by the native pipeline.
