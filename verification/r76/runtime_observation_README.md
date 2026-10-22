# R76 runtime observation I/O regression

This controlled host test invokes the real `ServiceHeartbeatStore` and `RealtimePressureGateR811` entry points, plus the real `ReportExportServiceCP4.ActiveControl` pressure reader. It does not replace those owners with test implementations. The development runner compiles only the two changed owners into a separate directory. Final mode compiles no production source, prohibits class collisions and records unchanged release class identities and runtime origins.

The Android preferences adapter separates in-memory values from its ordered disk queue. Every synchronous commit sleeps 20 ms; `apply()` immediately publishes memory and enqueues persistence. The AtomicFile adapter uses host files and actual fsync, injecting 20 ms per finish, and implements old `.bak` recovery. These are explicit boundary models, not measurements of phone storage latency.

On unchanged source `22e9a2ee`, identical assertions fail at the first observational patch (two synchronous commits, 40.906 ms) and first pressure publication (one AtomicFile durable finish, 20.715 ms). Current source passes 81 checks: twelve changed observation categories plus attempt/progress separation; immediate visibility in both preference stores; control/permission/stop/start/recovery/runtime leases still committed; ordered flush; durable runtime-session; two-second advisory pressure without AtomicFile fsync; legacy backup recovery; write/rename failures preserve the previous record and retry clock; real report consumer identity/expiry/rejection behavior.

The rename fault is injected by JDK 17 SecurityManager at the destination write-permission check; this tests the real publication cleanup and exception path. It is not an Android filesystem fault experiment. No source or release classes are modified by the fixture.

Example (use a new output directory each time):

```sh
python3 verification/r76/runtime_observation_run.py \
  --jdk /workspace/scratch/cd9274eff1c6/jdk/jdk-17.0.20.1+1 \
  --classes app/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes \
  --android-runtime /workspace/scratch/cd9274eff1c6/android-all.jar \
  --out /workspace/scratch/e2544c9906f6/r76-observation-final \
  --final-bytecode
```

Omit `--final-bytecode` for isolated development source. Add `--source-revision 22e9a2ee --case heartbeat` or `--case pressure` for the two old-source negative controls. Final release bytecode passed after the R76 strict build; see `evidence/final/observation/` and `evidence/final/suite.json`. The small logs, hashes and manifest are in `verification/r76/evidence/runtime_observation_*`.

These regressions establish the controlled mechanisms only. They do not establish phone lifecycle persistence, cross-process freshness, a 55-second capture guarantee, natural training/Future quality, real trades, or complete 1–9 business acceptance.
