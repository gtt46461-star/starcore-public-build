# Release 64 regression scope

Compile these drivers and the typed SQLite adapter ahead of the current production classes, the existing O09 Android API transport adapters, and android-all.jar. Use the original user report as a private input; it is not committed.

Use a **fresh /tmp directory on local storage** for every SQLite database. The workspace-backed file transport produced stale rollback-journal artifacts when reopened; those failed runs are test-environment failures, not an accepted repair. The driver never changes the original input.

1. RepairReportR64 `<bridge> <original snapshot.db> <new /tmp job> <extracted report dir>` exercises preemption rollback, actual research migration, history preservation, 217 individually committed migration units, interleaved critical writes, all 96 handlers and explicit false allChecksPassed.
2. StartupConcurrentR64 `<bridge> <original snapshot.db> <new /tmp file.db>` exercises two actual coordinator callers, with first initializer lasting 16.5 seconds.
3. ReportCompactR64 `<new /tmp root> <APK> <repaired snapshot.db> <bridge> <extracted report dir>` uses actual packaged APK assets and product report capture, read-only checks, product JNI compression and verification, and single ZIP publication with a 64 MiB Java heap. Build host JNI with verification/batch01/build_host_report_native.py; this does not substitute for Android ABI verification.

The Context, AssetManager, ApplicationInfo, StatFs and AtomicFile transport sources are the existing verification/r994 and verification/canonical adapters. Numeric SQLite NULL reads match Android CursorWindow (zero); integer/float types remain distinct. The OS SQLite connection itself enforces read-only mode. HostProductR974 bypasses Android process construction only; all migrations and diagnostic methods are product bytecode.

No natural training candidate, Future observation, certificate or mainline authorization is fabricated by the migration. Passing these tests does not close all 1832 requirements or claim real-phone startup/SAF performance.

4. LiveResearchR64 runs the public IMPORT owner and training dispatcher on repaired real past data, using existing canonical file-descriptor asset adapters. It does not insert candidates, draws, outcomes or qualifications manually.
