# HOST initial committed heap experiment

This one experiment used copies of the current actual app/test classes and their existing runtime dependencies. Each dependency was hashed before copying, after copying, and in the fixed snapshot; all three matched. The compiled BuildConfig identifies E056 and truthfully reports its debug support build as source_dirty=true. This is neither a signed APK receipt nor a clean release claim.

The unmodified complete R27EvolutionTest and R30NullPipelineTest classes ran together in one JVM, all six methods and original assertions intact. Only the initial heap flag changed: `-Xms32m -Xmx512m`. No fake budget probe, threshold change, System.gc, GC tuning flag, omitted trial, or production/test/build file edit was used. GC counters and collection time came from the real JVM management beans.

The same runner's separate VM-only default observation (`-Xmx512m`, no math tests repeated) measured initial Runtime.totalMemory at 297,795,584 bytes, maxMemory 536,870,912. With `-Xms32m`, the first observation measured totalMemory 35,651,584, the same maxMemory 536,870,912. These are observed committed-heap samples, not an assertion that maxMemory equals initial committed memory.

The experiment failed: six tests, four MEMORY_DECLARED_CAPACITY failures, zero skipped, exit 1, 11.026 seconds. The planted synthetic signal and draw-permutation contracts passed; H12 restore, H07 negative control and both full null-pipeline scenarios encountered actual waits near 322–327MB used heap. H07 entered but did not finish all 100 trials x 3 generations; its exact completed trial count is not emitted by the unchanged aborted test and remains UNKNOWN.

The JVM performed 160 natural young collections taking 259ms and zero old collections. Earlier natural collection therefore occurred with the small initial heap, but it still did not produce a passing original mathematical run. This result cannot justify reporting a HOST initial-heap adjustment as Android continuity or completed resource recovery. The original default-budget failures remain preserved in the earlier evidence directories.

`SUMMARY.json` contains the measured comparison and explicit outcome; `RECEIPT.json` contains commands, exact source/dependency hashes and events. All sampled source hashes remained unchanged during the run. The experiment did not alter app/build.gradle or any production or test source.
