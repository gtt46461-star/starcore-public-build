# R952 migration-step root-cause review

- Symptom: the existing fresh-install harness rejected the sixth completed Research migration.
- Root cause: R952 deliberately adds one authoritative, journaled `R952_EXTENSION` step after `R932_UNIFIED`; the harness encoded the old cardinality `5` as a literal.
- Correct owner: `ResearchSchemaCoordinatorR926` remains the only bounded scheduler and `ResearchMigrationRegistryR952` is the only R952 DDL owner.
- Fix: update the expected cardinality to `6` and assert the two critical R952 runtime tables exist. Production code was not rolled back to satisfy a stale test.
- Negative guarantee: no user-version bump, destructive migration, duplicate DDL owner, certificate, challenger, or REAL row is created.
- Verification: `scripts/r951_verify_runtime.py`, `scripts/r952_batch_acceptance.sh`, migration journal replay and fresh-install repair.
