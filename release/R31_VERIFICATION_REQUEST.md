# R31 verification request

Changing this file starts .github/workflows/r31-verification.yml on construction/r31-final-20261104
(a new workflow cannot be dispatched from a non-default branch). It is ignored by the construction gate.

- 2026-11-08: final K11 run (gate source 64db91b6 + this request)
- 2026-11-08: rerun after the Gradle distribution timeout (run 42464955166 built nothing); workflow now fetches it with retries
- 2026-11-08: rerun with the Gradle wrapper distribution cached (run 42465197650: contracts PASS, android_build failed on the download)
- 2026-11-08: gate green at 28d5affc (622/622, lint, all hosts); run with the temporary-CI-key debug build
- 2026-11-08: refreeze the r982 source-call inventory (run 42471588575: SOURCE_CALL_INDEX_STALE)
- 2026-11-08: retrigger (179a97b0 carried a skip marker in its message body)
- 2026-11-08: build, emulator matrix and delivery on the committed r982 inventory (2101e674)
- 2026-11-08: overlay-install build signed with the installed device identity (R27 CI key d8d8cee7, cache starcore-r27-ci-temp-signing-v1)
- 2026-11-09: R31Q 017 device hotfix (phone: start shows no countdown, report stays queued): bounded report observers, visible export wait, quick diagnostic; refreeze r982 then build
- 2026-11-09: R31Q 017 rerun on the refrozen r982 inventory (3bd03e8)
- 2026-11-11: R31Q 018 device fix (quick diagnostic from V2353A: legacy schema migration held the writer lock >90 min in one transaction at MIN_PRIORITY; draw save failed CP3_DB_BUSY_DEADLINE): refreeze r982 then build
- 2026-11-11: R31Q 018 second request (DbStore left byte-identical so its owner pins hold; Gradle-update lint notice disabled): refreeze r982 then build
- 2026-11-12: R31Q 019 (V2353A 018 diagnostic: every thread BLOCKED in SQLiteConnectionPool.waitForConnection while the legacy migration ran DDL; report request left no receipt): refreeze r982 then build
- 2026-11-12: R31Q 020 (takes over 019, never built): the dedicated legacy-DDL handle gets a 60 s busy timeout so a same-process writer cannot make an ALTER fail silently; refreeze r982 then build
- 2026-11-12: R31Q 020 second request (019's cache-flush DROP TABLE failed R31RetirementManifestTest; replaced by a TEMP table): refreeze r982 then build

- 2026-11-13T05:36:04.808333+00:00: current cumulative R31 repair verification and exact existing-signer release build. Code/input base 9fece69685dd0ba6fc97a8950710f15634faf328; source is this request commit. Actual debug/release compile and lint passed; failed report recovery, cancel/requeue fencing, API/context ownership, conservative restore disk checks and HOST stdout/stderr transport repaired. Release call index frozen from actual release classes (198fbc42626d7d3b874accf0abdb11d3bacdcb03bc572a1ecdf57eaa4ae58ec9). Run the complete current contracts and Android matrix; require installed signer d8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7, package com.starcore.research.r13 and candidate versionCode 577311021. Local interrupted gate and HOST-only results are not final acceptance; no device/24h/72h/natural-quality PASS is claimed.

- 2026-11-13: current-source rerun after real API29/API34 failures in 43374763818. Product base 28d94453455723510f307ba1238202b1c9e8aaee; normalize the fixed app database path before diagnostic lease/native capability, preserve concrete failure feedback, and durably drain offline setup writer observations through the real Health consumer. No gate, threshold or queue clearing bypass. Fresh release/debugAndroidTest compilation and release lint passed. New compiled call archive a2698f8c179c1379d92db751602582f4a1f25fe6b10ab1db373f05e5f1d42fcb; full current source validation required. Prior APK and receipts are not substituted. Physical device/24h/72h/natural quality remain NOT_RUN/NOT_PROVEN.
