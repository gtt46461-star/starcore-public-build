package com.ethk3.novaonepro.strategyv2;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Map;

/** Actual ownership checks; resource returns are not claims that heap bytes were collected. */
public final class OwnerConsumerResourceR31Host {
    static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static boolean closed(Object owner) throws Exception {
        Field field = owner.getClass().getDeclaredField("closedR31"); field.setAccessible(true); return field.getBoolean(owner);
    }
    static long number(Map<String, Object> observation, String key) { return ((Number) observation.get(key)).longValue(); }
    public static void main(String[] args) throws Exception {
        R30FailureRepairTest.OwnedFailureContextR31 fixture = R30FailureRepairTest.failedPositionContext(R30FailureRepairTest.rich());
        require(!closed(fixture.cache) && !closed(fixture.evaluation), "fixture prematurely closed");
        fixture.close();
        require(closed(fixture.cache) && closed(fixture.evaluation), "private fixture cache/context still open");
        System.out.println("RESOURCE_EVIDENCE privateFailureFixtureBothOwnersClosed=true");

        try (InputStream input = new FileInputStream(args[0])) {
            ReplayDatasetR27 data = SemanticAbiR27.readGolden(input, true);
            MemoryBudgetR31 memory = MemoryBudgetR31.processBudget();
            try (SharedFeatureStoreR31 store = new SharedFeatureStoreR31(memory, 32L << 20);
                 FeatureCacheR27 cache = new FeatureCacheR27(data, 48, memory, store)) {
                Map<String, String> result = OperatorGoldenR27.fingerprints(cache);
                Map<String, Object> after = store.snapshot();
                require(number(after, "readerLeases") == 0, "golden retained immutable readers");
                require(number(after, "workingLoans") == 0, "golden retained private working loans");
                require(number(after, "activeProductions") == 0, "golden retained an unfinished production");
                Map<String, Object> scratch = cache.scratchMemorySnapshotR31();
                require(scratch.isEmpty() || number(scratch, "busyBlocks") == 0, "golden retained a busy AST scope");
                require(!closed(cache), "internal golden consumer closed its caller's cache");
                require(cache.leaf("freq", 3, 3, 32).length == data.size() * 10, "caller cache unusable after golden");
                System.out.println("RESOURCE_EVIDENCE goldenEntries=" + result.size() + " readerLeases=0 workingLoans=0 activeProductions=0 busyScopes=0 callerCacheStillUsable=true actualHeapReleasedBytes=UNKNOWN");
                try (EvalContextR27 context = new EvalContextR27(cache, SplitR27.of(data, null), null)) {
                    context.close();
                    require(!closed(cache), "non-owning context closed its shared cache");
                    require(cache.leaf("gap", 3, 3, 0).length == data.size() * 10, "shared cache unusable after context close");
                }
                System.out.println("RESOURCE_EVIDENCE nonOwningContextLeavesSharedCacheUsable=true");
            }
        }
    }
}
