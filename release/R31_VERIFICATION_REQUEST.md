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

- 2026-11-13T06:35:38.023572+00:00: cumulative verification after the four actually failing HOST fixtures were repaired and targeted replay passed. Source base bf7afe42; this request commit is the exact build/verification source. Prior source 0124f5da passed all three Android API29/34/35 jobs in 43377916545, including actual canonical diagnostic WAL and real independent restore. Those receipts are prior-source diagnosis only. Execute the current full gate, 26 named Android tests, original 47-object gallery and real UI report readback, original 14 runtime +12 research fault cells and upgrade observations. Keep all 58 cards, 194 original/222 current acceptance and original thresholds. Physical device/24h/72h/natural quality remain NOT_RUN/NOT_PROVEN.

- 2026-11-13T08:46:32.985511+00:00: execute the current cumulative gate after actual API35 UI recovery isolated missing optional observation at ReportAttachmentClosureCP12.capture. Preserve consumable durable intent, bounded outside-lock observation preparation, exact request/release refresh and explicit UNKNOWN recovery after observer death. Version 577311022; frozen actual release call archive 028515e44f92607f05cd0992467fd1b71d7e7bfe1e7548cf697a07c4e4881626. New exact APK must execute all API29/34 seven groups/27 tests, API35 eight groups/28 tests including real 100 UI lifecycle cycles, original 47-object/188-screen gallery, production clear/re-export SHA/CRC readback, 14 runtime +12 research fault cells and overlay recovery. Prior-source greens remain diagnosis only. Physical device/24h/72h NOT_RUN; natural quality NOT_PROVEN.

- 2026-11-13: unified 027 stable K04 integration on 81e49006c935f0341d622c2471d34d56eb9e0df7: preserve accepted 026 report repairs; verify explicit live routing/current settlement admission, independent durable fault drain, typed dependency retry, original signer, actual full report and API29/34/35. Targeted 16 tests and native writer/service protocols passed; physical handset and natural quality remain NOT_RUN/NOT_PROVEN.
