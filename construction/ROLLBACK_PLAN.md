# R957 rollback plan

1. Before overlay install, copy the production database, WAL/SHM files, installed APK fingerprint, and report export to device-controlled backup storage.
2. R952 schema changes are additive and idempotent; they do not change SQLite `user_version=36` or rewrite historical predictions/settlements.
3. If R957 fails before any new live issue is accepted, reinstall the same-signer R954 APK and retain the database.
4. If R957 has accepted new issues, first export R957 evidence and database hashes. Reinstalling R954 is permitted only after confirming that R954 tolerates the additive `r952_*` tables; never delete those tables to force downgrade.
5. After rollback, verify application ID, certificate, historical row counts, latest settled issue, unique REAL count per issue, and absence of duplicate training deliveries.
6. A physical rollback remains `NOT_RUN` until executed on the target device; host schema repair is not a substitute.
