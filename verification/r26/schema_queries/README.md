These regressions exercise the actual `SchemaDomainCoordinatorR926` with the real SQLite bridge at `verification/r64/sqlite_bridge.py`. They are host contract checks, not Android timing evidence.

`Presence.java` compares required-object membership against the previous independent per-name query, including exact missing-object diagnostics, 0/1/499/500-object boundaries, unusual identifier fallback, fresh reads and same-version index drift. Compile the production coordinator and this test with the existing Android API adapter and production dependencies, then run `com.ethk3.novaonepro.Presence BRIDGE FIXTURE_DB`. The fixture is a disposable copy of the product schema with `draws(value)`.

The independent complete-catalogue fingerprint reference remains `verification/report1015_r80/combined/schema/fingerprint_filter/FingerprintFilterR80.java`; its existing config-driven runner records all commands. The source hashes and results for this change are in `construction/r26/continuation/schema-query-host-evidence.json` and the adjacent logs.

The source-bound Android suite additionally runs `R26SchemaFingerprintDeviceTest` and `R26SchemaMigrationDeviceTest` against Android SQLite. Only the latter measures cold migration and repair writer calls; host timing is not promoted to device acceptance.
