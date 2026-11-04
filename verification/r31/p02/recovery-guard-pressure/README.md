# Recovery guard pressure comparison

This is a verification-only engineering fixture. It changes no production source, tools source or original JUnit test. It does not rerun or clear the seven remaining capacity failures from the whole 472-test gate and is not Android/device, continuous-run or natural-quality evidence.

The runner copies the old guard source directly from commit `3246ccf8eb6f7b2f7b9d0fe77fc18a70ab958056` into a temporary directory. It copies the current root-repaired test separately. Both complete four-method test classes compile against the exact current `MemoryBudgetR31` (`494541...`) and `JsonR27` production sources. A byte comparison requires the current test to differ solely in the intended fixture arguments: `Kind.CHECKPOINT` and `Math.max(4096L, available - 4MiB)`. Every assertion, method and existing age definition is preserved. The unchanged guard that deliberately verifies rejection of a custom Probe remains present; the surrounding pressure injection and all runtime heap observations use the actual final RuntimeProbe.

Each case runs in its own default `-Xmx512m` JVM, with no `-Xms`, enlarged maximum heap, explicit GC, time offset or pressure-Probe substitution. The pressure cases hold a protected, pre-admitted 343,597,383-byte primitive payload (about 0.64 of maximum) in a static strong root, record its completed allocation, and retain its lease through all JUnit methods. Before/after SHA256 verifies actual payload contents; the exact declared bytes and open lease are checked before releasing the engineering owner only after the result is recorded.

| Case | Measured heap before JUnit | Original four guards | Explicit collector requests |
|---|---:|---|---:|
| Old 3246 under pressure | 346,278,048 bytes | 3 passed, 1 expected reproduced failure | 0 |
| Root repair under same pressure | 346,278,072 bytes | 4 passed | 0 |
| Root repair in healthy JVM | 2,312,968 bytes | 4 passed | 0 |

The old failure is exactly `pendingPromisesAndOrdinaryAdmissionNeverRequestCollector`, `IllegalArgumentException: R31_MEMORY_OWNER_BYTES`: zero optional headroom made `available - 4MiB` negative before any assertion in that test could validate admission. Full original traces remain in `old-pressure.json` and its raw log. The root fixture uses protected admission and nonnegative descriptor bytes; it preserves the intended ordinary optional refusal, no-GC, age and live-owner assertions.

The two pressure cases each observed one VM collection during JUnit. The API's explicit collection request count stayed zero, the array stayed strongly held and byte-for-byte unchanged, and pressure was not removed to make assertions pass. Observed collection counters, separate pre-allocation/pre-JUnit/post-JUnit readings, method events, explicit API counters and all heap/declaration snapshots are retained in the JSON. This is a VM activity observation, not an explicit collector request from this fixture.

Run with a JDK and existing JUnit/Hamcrest dependencies: `python3 verification/r31/p02/recovery-guard-pressure/run.py`. The receipt records exact commands, jars, source hashes, old Git identity and original/current test identities. All source inputs were unchanged during execution and were checked again afterward. No original files are written by the runner. `FROZEN_SOURCES.json` freezes this fixture and its inputs for root integration.
