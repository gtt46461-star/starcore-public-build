# Checkpoint startup test compatibility with strict archive ownership

Scope: adapt the original `CheckpointStartupR30HostTest` without changing any application production source, shared platform shim, schema owner, or business acceptance condition. This is a synthetic HOST fixture running the actual retention/domain/revocation source against native SQLite. Android/ART, device runtime, whole-card completion, 24/72-hour operation, and natural strategy quality are not verified here.

Base commit: `2f91a41c2904df4d465158254c090f85be7f16ad`. The original test is preserved byte-for-byte in `tools/r31/checkpoint_startup_host/baseline/CheckpointStartupR30HostTest.java`, SHA256 `b0d2c9d7c565268db90b5b0ede2def51808fabaa4ada131c40f3fb10e369d09a`.

The execution used three actual, frozen owner sources ahead of the 4475-class Android bootstrap jar. These sources were copied before compilation; each receipt records that their origins remained unchanged throughout execution:

| Actual source | SHA256 |
| --- | --- |
| SchemaDomainCoordinatorR926.java | `71722a8582e6eeeb2daf8d16446de145501bf54793819695bc4eaeea74a7a913` |
| ResearchRetentionStoreR30.java | `e26556fdde2b055e51e8f8c7ad748023a4ad7b69a84b412df50ee7e64cc48680` |
| CertificateRevocationR30.java | `f46c6b02aa86041846c7a91780ac32387d46377d58a9fd254420681b78285756` |
| `/workspace/r31-shared-bootstrap-2f91-frozen/classes.jar` | `e46f65ea8e53b8982e1077a6908600cfab66d690f9c7bc11f501285fc85405c9` |

Retention had acquired the owner's public read-only `sourceProtectionTriggersR31` helper before the first actual source capture. The earlier read-only inspection saw `e588...`; no execution in this evidence folder used that earlier source. Do not describe these runs as e588 tests.

The unmodified original test was first run and actually failed, exit 1. The failure log in `original-negative-20261104/` preserves the uncaught `DatabaseCoordinator.SchemaPendingR31` at `UNKNOWN_ARCHIVE_ATTACHMENT:r30_keep_r29_model_checkpoint:host_startup_archive_failure` and the parent's `REAL_REOPEN_FAILED`. The original parent baseline behavior passed before the child reached this schema rejection. Read-only inspection shows seed 1 still archived, seed 3 still current, both 131072-byte BLOBs, startup marker count 1, and zero Critical stamps. This original failure is retained and reproduced in both subsequent runs; it is not relabeled as a production PASS.

The adapted test preserves the original retention, normal training replacement, same-process no-churn, and actual new-JVM reopen assertions. It adds two separate checks:

1. The unknown synthetic archive trigger yields typed `DEPENDENCY`/`SCHEMA_READINESS` when enabled and still yields that dependency after its fixture flag is set to zero. Startup markers and archived bytes remain unchanged. Only `DROP TRIGGER host_startup_archive_failure` removes this synthetic attachment; no owner guard is dropped or bypassed.
2. Once the schema is exact, a test-only standard JDBC proxy observes the actual production archive INSERT. Immediately before its real `PreparedStatement.execute`, it enables native SQLite `PRAGMA query_only=ON` on that same native connection. The real archive INSERT then raises `[SQLITE_READONLY] Attempt to write a readonly database`. A test probe INSERT performed earlier in the same real JDBC transaction rolls back; startup markers and archive count do not advance. Query-only mode is restored in `finally`. Retrying the actual producer succeeds and preserves the complete seed 1 and seed 3 bytes before ordinary training advances to seed 4. Repeating ensure in the same process adds neither snapshots nor markers.

The fault adapter changes no business SQL, schema declaration, production class, or archive trigger. It neither throws a fake database success nor substitutes a business implementation. Native SQLite performs the actual failed INSERT and rollback.

Final execution: `final-native-20261104/RECEIPT.json`, `sourceCapturedAt=2026-11-04T21:33:15.124597+00:00`. The adapted JVM exited 0, with 43 counted child assertions and 11 counted parent assertions, in addition to retained original assertions. The child has finite 45-second execution and 10-second forced-exit bounds. The runner bounds each whole process group at 90 seconds. The real child, statements, and connections exited/closed before completion. The earlier adapted run is also preserved; the final run added independent read-only artifact SHA256 evidence rather than choosing a faster timing result.

The final independent read-only observation shows two archive rows, one current row, each an exact 131072-byte BLOB; all actual artifact SHA256s equal their stored state hash. Startup markers are 2, the synthetic trigger is absent, and the rollback probe contains 0 rows. `matchReason(criticalContract())` remains `STAMP_MISSING` throughout the adapted checks, and Critical domain stamp rows remain empty. The minimal checkpoint fixture does not authenticate or repair the complete Critical/Core schema.

Reproduction from an activated Android/JDK environment:

```sh
python3 tools/r31/run_checkpoint_startup_host.py \
  --out /tmp/r31-checkpoint-startup-reproduce \
  --classes /workspace/r31-shared-bootstrap-2f91-frozen/classes.jar \
  --production-root /workspace/r31-shared-migrations
```

The production-root option must point at the intended owner source bytes; the exact inputs are always recorded. On an integrated checkout, omit that option to compile the checkout's actual three sources. The existing `tools/r30/ci_build.sh` javac/java entry can execute the adapted original test unchanged: it only uses existing JDBC and Cursor platform APIs. No new platform hook is required.

A focused read-only search of legacy runners found the checkpoint fixture as the only explicit unknown `BEFORE INSERT ON r30_keep_*` trigger in the owned test paths. `ResearchRetentionR30HostTest` also injects a trigger on `r30_copy_log` for its pre-unlink reference fault; that is a different object and was not edited or executed here. The other HOST suites were not rerun, skipped, or reported passing by this targeted result.
