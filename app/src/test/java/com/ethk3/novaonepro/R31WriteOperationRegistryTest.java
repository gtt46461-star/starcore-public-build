package com.ethk3.novaonepro;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/** R31-B02-C01#A1: every literal write operation in the production tree resolves to a registered WriteClass;
 * an unregistered operation is observable as unknown and never promoted to a critical writer. */
public final class R31WriteOperationRegistryTest {
    private static final Pattern LITERAL_ENTRY = Pattern.compile("\\.(?:runWrite[A-Za-z0-9]*|runAtomicWrite[A-Za-z0-9]*)\\(\"([^\"]+)\"");
    private static File root() {
        File d = new File(System.getProperty("user.dir")).getAbsoluteFile();
        for (int i = 0; i < 5 && d != null; i++, d = d.getParentFile()) if (new File(d, "app/src/main/java").isDirectory()) return d;
        throw new IllegalStateException("R31_TEST_ROOT_NOT_FOUND");
    }
    private static void collect(File dir, Map<String, String> out) throws Exception {
        File[] children = dir.listFiles(); if (children == null) return;
        Arrays.sort(children);
        for (File child : children) {
            if (child.isDirectory()) collect(child, out);
            else if (child.getName().endsWith(".java")) {
                Matcher m = LITERAL_ENTRY.matcher(new String(Files.readAllBytes(child.toPath()), StandardCharsets.UTF_8));
                while (m.find()) out.putIfAbsent(m.group(1), child.getName());
            }
        }
    }
    @Test public void everyLiteralProductionOperationIsRegistered() throws Exception {
        Map<String, String> operations = new TreeMap<>();
        collect(new File(root(), "app/src/main/java"), operations);
        assertTrue("scan found the production write entries: " + operations.size(), operations.size() >= 250);
        List<String> unregistered = new ArrayList<>();
        for (Map.Entry<String, String> e : operations.entrySet())
            if (!DatabaseCoordinator.WriteContext.legacy(e.getKey()).registered) unregistered.add(e.getKey() + "@" + e.getValue());
        assertEquals("unregistered production write operations", Collections.emptyList(), unregistered);
    }
    /** Pre-R31 isCriticalBusinessOperationR914 (5237b265), minus R914_DRAW_HISTORY_BATCH (B02-C01 step 3:
     * catch-up history is a background slice), plus the explicit R26 LIVE_FREEZE writers. */
    private static final Set<String> CRITICAL_EXACT = new HashSet<>(Arrays.asList("R932_BUNDLE_CAPTURE", "R932_FREEZE_ONE_RESEARCH_CONSUMER",
            "R925_FETCH_BEGIN", "R925_FETCH_PAYLOAD", "R925_FETCH_COMPLETE", "R26_INCUMBENT_FREEZE", "R26_LIVE_FREEZE"));
    private static final String[] CRITICAL_PREFIX = {"R914_DRAW_SAVE_ATOMIC", "R914_SETTLEMENT_HANDOFF_CURRENT", "R36_CANONICAL", "R36_ANALYSIS",
            "R366_ANALYSIS", "R36_SETTLEMENT", "R811_SETTLEMENT_PROMOTE_CURRENT", "R909_SETTLEMENT_REOPEN_CURRENT", "R905_REOPEN_FALSE_NO_PREDRAW",
            "R820_SETTLEMENT", "R900_SETTLEMENT"};
    static boolean expectedCritical(String op) {
        String upper = op.toUpperCase(Locale.ROOT);
        if (CRITICAL_EXACT.contains(upper)) return true;
        for (String prefix : CRITICAL_PREFIX) if (upper.startsWith(prefix)) return true;
        return false;
    }
    @Test public void criticalWritersAreExactlyThePreR31BusinessSet() throws Exception {
        Map<String, String> operations = new TreeMap<>();
        collect(new File(root(), "app/src/main/java"), operations);
        List<String> mismatch = new ArrayList<>();
        for (String op : operations.keySet())
            if (expectedCritical(op) != DatabaseCoordinator.WriteContext.legacy(op).critical()) mismatch.add(op);
        assertEquals("registration neither promotes nor demotes business-critical writers", Collections.emptyList(), mismatch);
        assertFalse("catch-up history is background", DatabaseCoordinator.WriteContext.legacy("R914_DRAW_HISTORY_BATCH").critical());
        assertTrue("current draw ingest stays critical", DatabaseCoordinator.WriteContext.legacy("R914_DRAW_SAVE_ATOMIC").critical());
    }
    @Test public void unregisteredOperationIsUnknownAndBackground() {
        DatabaseCoordinator.WriteContext unknown = DatabaseCoordinator.WriteContext.legacy("R31_TEST_NEVER_REGISTERED_OPERATION");
        assertFalse(unknown.registered);
        assertFalse("unknown priority is never treated as a critical writer", unknown.critical());
        assertEquals(DatabaseCoordinator.WriteClass.RESEARCH_DATA, unknown.writeClass);
    }
}
