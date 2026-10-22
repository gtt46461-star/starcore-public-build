# STARCORE_TIANSHU V57.15.3-R3.6.11-814 Runtime Stability Repair Report

## Scope

This repair is intentionally restricted to runtime stability, UI rendering semantics, startup scheduling, memory/OOM containment, and settlement backlog scheduling. Strategy/model/formula semantics are not changed.

Identity:
- package: `com.ethk3.novaonepro`
- version: `V57.15.3-R3.6.11-814`
- versionCode: `577190823`
- game: `Binance1-PK10`
- play: single-position, fixed K=3, max one REAL per issue
- REAL owner: `DecisionGateEngine_ONLY`
- settlement owner: `SettlementEngine_ONLY`
- model proof: `NOT_PROVEN_UNTRAINED`
- release posture: `SHADOW_ONLY`

## User-observed failures addressed

1. Cold start could freeze for several seconds and the process could exit.
2. Starting the background service could immediately kill the process under memory pressure.
3. Live UI cards changed height as text length changed, producing visible flicker/expansion.
4. An unavailable probability could be rendered as `0%` while a configured threshold remained visible.
5. The runtime could leave SHADOW observations pending because predraw canonical production and settlement draining were starved by runtime failure/OOM.
6. Repeated initialization after OOM could retain a partial object graph and increase the chance of another OOM.

## Runtime-only corrections

### Cold-start / Service sequencing
- `MainActivity` now attaches the B15 diagnostic panel only after `setContentView()` and view binding.
- Automatic background-service recovery waits for the Activity startup database initialization to finish instead of racing a second heavy SQLite/open graph against first-frame UI startup.
- `AutoFetchService.startWork()` is guarded against execution on the Android main looper and is dispatched to the existing serialized runtime/startup handler when necessary.
- Startup settlement seeding and diagnosis-baseline maintenance are deferred to the existing background maintenance lane instead of running synchronously in the Service start path.

### OOM containment
- Runtime initialization failure now releases the partially constructed engine/store graph before retry.
- Analysis OOM enters a bounded backoff instead of immediately rebuilding another heavy analysis graph.
- Deterministically recomputable caches are released after completed analysis and after failure recovery.
- Low-allocation OOM crash recording avoids constructing a large stack trace while the heap is exhausted.

### Settlement closure scheduling
- The exact current-draw settlement remains first priority.
- A small bounded backlog drain is scheduled after the exact current settlement and in deferred startup maintenance.
- No settlement rule, hit rule, target issue rule, REAL/SHADOW accounting rule, or SettlementEngine algorithm was changed.

### Stable UI / probability truth
- High-frequency dashboard text areas use fixed line counts and ellipsis instead of changing card height every refresh.
- Probability broadcast defaults are `NaN`/unavailable rather than numeric zero.
- The UI renders `未生成/未证明` when no valid probability exists and `未校准｜原始P=...` when only an upstream probability is available; it no longer claims a real `0%` merely because a Java numeric field was absent.
- Gate threshold display remains separate from probability availability.

## Strategy immutability evidence

The following decision/strategy core files are byte-identical to the R3.6.9-812 Git baseline:
- `V51PermutationFeatureEngine.java`
- `FullInformationFeatureGraphEngine.java`
- `WindowFeatureEngine.java`
- `TrendStateMachine.java`
- `BreakRecoveryEngine.java`
- `CycleRhythmEngine.java`
- `SequenceDistributionEngine.java`
- `DistributionDriftEngine.java`
- `K3ActionSpaceEngine.java`
- `K3FunnelOrchestrator.java`
- `V51ActionRankerEngine.java`
- `V51SinkhornEngine.java`
- `DecisionGateEngine.java`
- `SettlementEngine.java`

100-case golden comparison: decision-critical mismatches = 0. The only 5/100 differences are field 2 feature-contract hashes associated with the new build/runtime identity; action-universe hash, rank order, Champion checksum, Selected contract, authorization action and rejection code are unchanged.

## Build verification

- Full Java source compile: PASS, Java 17, classfile major 61.
- Compiled application classes: 1851 in the production-key build.
- R36 host contracts: PASS.
- R811 host contracts: PASS, 208 assertions.
- Neural deterministic/finite/gradient/Sinkhorn checks: PASS.
- Final APK zip integrity: PASS.
- zipalign: PASS.
- APK signature verification: PASS.
- signing certificate continuity against R3.6.9-812: PASS.
- Android ABI gate: PASS; reviewed new framework call is only `Looper.myLooper()` for main-thread dispatch protection.
- package/version identity: PASS.

## Truth boundary

This build fixes the identified code-level startup/runtime/UI/settlement scheduling defects and passes host/build/ABI/signature verification. A container cannot prove behavior on the user's exact physical Android device, vendor process manager, battery policy, or live network. Therefore `PHYSICAL_DEVICE_RUNTIME` remains `RETEST_REQUIRED` until the supplied device protocol is completed.

The repair does **not** claim model effectiveness, 45% hit rate, or REAL authorization. `MODEL_PROVEN=false`, `SHADOW_ONLY=true`, and REAL remains gated by the existing DecisionGate contract.
