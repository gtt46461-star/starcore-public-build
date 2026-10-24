# R13 5.4.3.03 startup recovery

The user reported repeated startup/initialization failure on their phone. The precise phone error and device model were not supplied, so the reproduced defect below must not be described as a confirmed diagnosis of every phone-side initialization failure.

## Reproduced defect

The delivered 5.4.3.02 APK started successfully with an empty application database on Android API 29 and 35. After force-stop and an immediate launcher restart, it retained the previous process's `service_ready_at` and `process_id`, without starting a new AutoFetchService. The final API 29 screenshot displayed `后台心跳异常·等待恢复`.

MainActivity called `BootReceiver.startAutoFetchService` after its first frame. This path could return `SKIP_HEALTHY_SERVICE` based on persisted heartbeat/runtime-lease timestamps produced by the process that Android had just killed. No service start was then delivered by this UI launch. A recovery lease can also remain valid for 120 seconds after its owning process dies, suppressing the new process's start request. The first candidate that only changed the UI entry still failed the fresh-process restart test on all three Android versions; it was withheld.

The original 24 instrumentation tests explicitly disabled automatic service startup in their test runner. The initial follow-up shell probe also accepted persisted READY state after restart; that acceptance was invalid. The revised probe requires a current process ID matching the heartbeat owner, a new service-ready timestamp, and a moving heartbeat that remains fresh for 60 seconds.

## Change

MainActivity now calls the existing idempotent `reconcileAutoFetchService` entry after the first frame. It retains the user-enabled check, cold database initialization ordering and startup retry policy. Heartbeat health and recovery leases additionally verify the main process identity. A previous process's recovery lease is reclaimable immediately; a live current-process lease still coalesces duplicate requests. Pre-fix leases without an explicit owner use the recorded heartbeat PID. Secondary diagnostic processes do not compare their PID with the service PID. Reconciliation delivers START even if historical persisted state looks healthy; AutoFetchService remains the sole owner of runtime initialization and running workers.

The R13 package remains `com.starcore.research.r13`, signed by the existing fixed R13 certificate. Version code increases from 577251061 to 577251062. The fix does not delete application data or change the private/SU1 packages. REAL authorization policy remains unchanged.

## Acceptance

- Baseline reproduction uses the exact delivered 5.4.3.02 APK from workflow 40066618076, not a rebuild of it.
- Candidate probes run directly through the launcher with automatic service enabled, on single-core Android API 29, 35 and 36 emulators.
- Cold launch and process restart each require a fresh service-ready record from the actual current PID, followed by 60 seconds of moving heartbeat.
- A recorded startup error, startup retry, caught exception, or stale historical READY cannot satisfy acceptance.
- Two real-Android preference tests cover legacy/current owner leases, duplicate coalescing and explicit user-stop preservation. The API 29 instrumentation suite continues to verify the research UI, charts, schema, live settlement and installed API client separately.
- Baseline run 40079212096 reproduced a 90-second restart failure: actual PID 3782, persisted service PID 3323, unchanged ready time and heartbeat. Detailed baseline run 40080151501 also captured `SKIP_RECOVERY_LEASE_ACTIVE` with a lease left by the previous PID, followed by `WATCHDOG_RECOVERY_REJECTED`. See the final delivery receipt for candidate run IDs and results. These emulator checks do not establish physical-phone compatibility or natural prediction performance.

The initial combined API 29 instrumentation run and its retry were terminated during AtomKernelDeviceSU1Test. Logcat explicitly records lowmemorykiller killing the app (about 1.2 GB resident in the first attempt), after killing multiple system apps. These failed attempts are retained; they are not startup passes and do not establish acceptable memory usage for that combined workload. The exact same candidate APK is separately tested through the launcher before instrumentation, then each instrumentation class is run in a fresh application process without reducing assertions or increasing emulator RAM. Run 40081604183 records this additional verification.

Run 40081604183 verified both API 29 launcher phases with fresh PID/READY and 60 seconds of moving heartbeat. It then reproduced the kernel-test low-memory kill even with isolated test processes. Thus isolation alone did not resolve the pressure; the failed run is not counted as a 26-test pass. A subsequent diagnostic uses two emulator CPU cores with the same 2 GB RAM and unchanged APK/assertions.

Final instrumentation verification: run 40082044526, job 120837424222 passed all 26 tests in isolated application processes with two CPU cores and 2 GB RAM, including both StartupLeaseR13Test cases. The APK bytes are unchanged from build 40080151500. Single-core startup/restart acceptance remains the separate six-phase evidence. The single-core kernel workload memory limitation is unresolved and is not represented as a pass.
