# R68 report size and startup repair

The R67 phone video shows a 2.15 GB `snapshot.db` in the exported archive. The user reports a roughly 650 MB ZIP and a 20 minute export. The later message confirms the export eventually finished; this is not recorded as a permanent export failure. The previous video also shows a crash after opening the model learning page while database initialization is incomplete.

Changes:

- Default export is one standard ZIP containing the complete diagnostic evaluation, rather than a raw database backup. The same 96 registered checks still use one immutable full database snapshot. All schema objects/table counts/state groups, existing runtime observations, exact compiled call index and current construction ledgers remain. Full raw backup remains an explicit separate menu option.
- Expand the diagnostic metadata to runtime events/projections/outbox, fetch ledger, draws/provenance, model/training/archive registries, atom specifications, matrix records and existing research/certificate/mainline records. Each selected table exports every row and every text/numeric column, including newly added columns. Binary cells have explicit byte-length markers, with existing hash/reference columns retained. Missing schemas are explicit; they are not represented as healthy empty tables. Unrelated app-state credentials are excluded.
- Add a live file-state inventory with paths, sizes, times, readability and enumeration failures. It does not copy file contents or claim atomicity/hash validation of changing live files. The current export directory is excluded to avoid self inclusion; links are not followed.
- Use lossless ZIP deflate level 6, retain per-member SHA/length checks and complete ZIP CRC readback. Reuse the owner's already verified immutable snapshot SHA instead of hashing the same GB-scale file twice more.
- Show snapshot hashing and final ZIP creation as separate active steps. Display actual delivered ZIP size. The final report still requires `READY_TO_SHARE`; completed backup page count is not report completion.
- After publication, verify the sealed ZIP, index binding and manifest before removing private duplicate compressed parts and raw temporary files. Resume verifies the ZIP directly. Existing multipart deliveries retain their parts. An invalid old delivery retains recovery inputs and does not block a new report. Existing old-report/shared-file expiry protections remain.
- Open model-learning/database-health page databases in background workers, with visible read failures. Startup replay only rebuilds missing or mismatched projection watermarks using the original immutable events. No algorithm quality thresholds, certificate authority, decision or settlement rules are relaxed.
- Carry only explicitly reviewed R65/R66/R67 numeric streams forward. No migration of certificates or retrospective future evidence.

`run.py` executes the current Gradle-compiled product against real SQLite and the actual host native archive codec. Coverage includes 5000 draws, 1025 long formulas, new metadata fields, missing schemas, credentials exclusion, source immutability, single-ZIP byte equality, integrity-gated cleanup/recovery, legacy multipart compatibility and startup replay/rollback. A separate 96 MiB incompressible fixture exercises bounded archive streaming. The existing R66/R67 recovery and numerical tests are retained.

The repeated-text synthetic fixture is deliberately compressible and is not an estimate of the user's final report size. Size reduction comes chiefly from excluding the raw backup and repeated private copies; distinct useful algorithm metadata is not silently capped. App data retention remains owned by the existing verified 5000-period retention pipeline. No user database, learned model or saved external report is deleted by this change.

Device installation, cold-start navigation, background endurance and the user's actual export size remain to be verified on the phone. Static compiled call coverage is not runtime execution proof. Complete evidence collection and all checks passing remain separate facts.

Run from the repository root:

```
python3 verification/r68/run.py --classes app/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes --android-runtime /absolute/android-all.jar --jdk /absolute/jdk17 --out /absolute/fresh-evidence-directory --native /absolute/host-native-library-directory
```
