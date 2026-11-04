package com.ethk3.novaonepro.strategyv2;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Map;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

/** Runs unchanged JUnit contracts against snapshotted Android HOST support. */
public final class OwnerConsumerRunnerR31Host {
    static void observed(String stage) {
        Runtime r = Runtime.getRuntime();
        System.out.println("VM_OBSERVATION " + stage + " max=" + r.maxMemory() + " total=" + r.totalMemory() + " used=" + (r.totalMemory() - r.freeMemory()));
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans())
            System.out.println("GC_OBSERVATION " + stage + " name=" + gc.getName() + " count=" + gc.getCollectionCount() + " millis=" + gc.getCollectionTime());
    }
    public static void main(String[] args) throws Exception {
        Class<?> identity = Class.forName("com.ethk3.novaonepro.BuildConfig");
        for (String field : new String[] {"SOURCE_COMMIT_R26", "SOURCE_DIRTY_R31", "SOURCE_TREE_R31"})
            System.out.println("ACTUAL_COMPILED_IDENTITY " + field + "=" + identity.getField(field).get(null));
        observed("initial");
        if (args.length == 2 && args[0].equals("golden")) {
            try (InputStream in = new FileInputStream(args[1]); FeatureCacheR27 cache = new FeatureCacheR27(SemanticAbiR27.readGolden(in, true))) {
                Map<String, String> fingerprints = OperatorGoldenR27.fingerprints(cache);
                for (Map.Entry<String, String> entry : fingerprints.entrySet()) System.out.println("GOLDEN " + entry.getKey() + "=" + entry.getValue());
                System.out.println("GOLDEN_COUNT " + fingerprints.size());
            }
            observed("final");
            return;
        }
        Class<?>[] tests = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) tests[i] = Class.forName(args[i]);
        JUnitCore junit = new JUnitCore(); junit.addListener(new TextListener(System.out));
        Result result = junit.run(tests);
        System.out.println("JUNIT_RECEIPT tests=" + result.getRunCount() + " failures=" + result.getFailureCount() + " ignored=" + result.getIgnoreCount() + " millis=" + result.getRunTime());
        observed("final");
        System.exit(result.wasSuccessful() ? 0 : 1);
    }
}
