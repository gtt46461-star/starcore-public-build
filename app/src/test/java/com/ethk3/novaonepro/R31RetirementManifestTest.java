package com.ethk3.novaonepro;

import static org.junit.Assert.*;

import com.ethk3.novaonepro.strategyv2.JsonR27;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.*;
import org.junit.Test;

/**
 * R31-B12-C04#A1-#A3: controlled retirement. The manifest (tools/r31/retirement_manifest.json) is re-derived here from
 * the source tree, independently of the generator: no destructive DROP without its evidence, every preserved capability
 * still has an executable implementation and a test, and nothing retired breaks the Schema/Retention/Report registries
 * or the upgrade path. DbStore is mapped, not cut (DEFERRED_TARGET_SELECTION_AFTER_COVERAGE).
 */
public final class R31RetirementManifestTest {
    static final Path MAIN = Paths.get("src/main/java"), TEST = Paths.get("src/test/java");
    static final Pattern DROP = Pattern.compile("DROP\\s+(TABLE|VIEW|INDEX|TRIGGER)\\s+(?:IF\\s+EXISTS\\s+)?(\"?[\\w$]+\"?|\"\\s*\\+\\s*[\\w.()]+)");

    @SuppressWarnings("unchecked") static Map<String, Object> manifest() throws Exception {
        return (Map<String, Object>) JsonR27.parse(new String(Files.readAllBytes(Paths.get("../tools/r31/retirement_manifest.json")), StandardCharsets.UTF_8));
    }
    static String read(Path p) { try { return new String(Files.readAllBytes(p), StandardCharsets.UTF_8); } catch (Exception e) { throw new IllegalStateException(e); } }
    static List<Path> sources(Path root) throws Exception { List<Path> out = new ArrayList<>(); try (java.util.stream.Stream<Path> s = Files.walk(root)) { s.filter(p -> p.toString().endsWith(".java")).sorted().forEach(out::add); } return out; }
    /** {file|kind|target} for every DROP in production code, scanned here (not taken from the manifest). */
    static List<String> drops() throws Exception {
        List<String> out = new ArrayList<>();
        for (Path f : sources(MAIN)) {
            String[] lines = read(f).split("\n", -1);
            for (String line : lines) { Matcher m = DROP.matcher(line); while (m.find()) { String t = m.group(2).replace("\"", ""); if (t.contains("+")) t = "expr:" + t.replaceAll("[^\\w.]", ""); out.add(MAIN.relativize(f).toString().replace(File.separatorChar, '/') + "|" + m.group(1) + "|" + t); } }
        }
        return out;
    }
    @SuppressWarnings("unchecked") static List<Map<String, Object>> list(Map<String, Object> m, String key) { return (List<Map<String, Object>>) m.get(key); }

    @Test public void everyDeletedObjectHasInactivityNoReferenceReplacementAndRecovery() throws Exception {
        Map<String, Object> m = manifest();
        // 1) approved drops: none in R31; any entry must carry all four kinds of evidence and be unreferenced in production code
        for (Map<String, Object> d : list(m, "approved_drops")) {
            for (String k : new String[] {"inactive_evidence", "no_reference_evidence", "replacement", "recovery"}) assertFalse(d + " missing " + k, String.valueOf(d.get(k)).trim().isEmpty() || "null".equals(String.valueOf(d.get(k))));
            String name = String.valueOf(d.get("object"));
            for (Path f : sources(MAIN)) { String text = read(f).replaceAll("DROP\\s+(?:TABLE|VIEW)\\s+(?:IF\\s+EXISTS\\s+)?\"?" + Pattern.quote(name) + "\"?", ""); assertFalse(name + " still referenced in " + f, Pattern.compile("\\b" + Pattern.quote(name) + "\\b").matcher(text).find()); }
        }
        // 2) every DROP TABLE/VIEW in the tree is a listed same-transaction rebuild: the same file copies every row first and recreates the name
        List<String> tableDrops = new ArrayList<>(); for (String d : drops()) if (d.contains("|TABLE|") || d.contains("|VIEW|")) tableDrops.add(d);
        List<String> listed = new ArrayList<>(); for (Map<String, Object> s : list(m, "drop_table_sites")) listed.add(s.get("file") + "|" + s.get("kind") + "|" + s.get("target"));
        assertEquals("the manifest lists exactly the DROP TABLE sites of the current tree", tableDrops, listed);
        Set<String> rebuildFiles = new HashSet<>(); for (Map<String, Object> r : list(m, "rebuild_in_place")) rebuildFiles.add(String.valueOf(r.get("file")));
        for (String d : tableDrops) {
            String file = d.substring(0, d.indexOf('|')); String text = read(MAIN.resolve(file));
            assertTrue("DROP TABLE outside a declared rebuild: " + d, rebuildFiles.contains(file));
            assertTrue("rebuild copies rows before dropping: " + d, Pattern.compile("INSERT INTO [^;]*SELECT", Pattern.DOTALL).matcher(text).find());
            assertTrue("rebuild recreates the original name: " + d, text.contains("RENAME TO") || text.contains("CREATE TABLE \"") || text.contains("\"CREATE TABLE \""));
        }
        for (Map<String, Object> r : list(m, "rebuild_in_place")) for (String k : new String[] {"copy", "recreated_as", "transaction", "rows", "reason"}) assertFalse(r + " " + k, String.valueOf(r.get(k)).isEmpty());
        // 3) candidates are never dropped without evidence
        for (Map<String, Object> c : list(m, "candidates")) {
            for (String k : new String[] {"owner", "readers", "writers", "dynamic_sql_or_reflection", "persistent_references", "replacement", "observation", "equivalence", "decision"}) assertNotNull(c.get("object") + " " + k, c.get(k));
            assertFalse("a candidate without equivalence evidence is not approved for DROP: " + c.get("object"), String.valueOf(c.get("decision")).startsWith("DROP"));
        }
        // 4) derived drops (triggers/indexes) are recreated by the same owner file
        for (String d : drops()) {
            if (!(d.contains("|TRIGGER|") || d.contains("|INDEX|"))) continue;
            String file = d.substring(0, d.indexOf('|')), target = d.substring(d.lastIndexOf('|') + 1); String text = read(MAIN.resolve(file));
            assertTrue("dropped derived object is recreated by its owner: " + d, target.startsWith("expr:") || text.contains("CREATE TRIGGER") || text.contains("CREATE INDEX") || text.contains("CREATE UNIQUE INDEX") || text.contains("REBUILD"));
        }
    }

    @Test public void everyPreservedCapabilityStillHasAnExecutableImplementationAndATest() throws Exception {
        Map<String, Object> m = manifest(); Set<String> hosts = new HashSet<>();
        Matcher h = Pattern.compile("\"id\": \"([^\"]+)\"").matcher(read(Paths.get("../construction/r31/HOST_SUITE.json"))); while (h.find()) hosts.add(h.group(1));
        Map<String, String> testSources = new HashMap<>(); for (Path f : sources(TEST)) testSources.put(f.getFileName().toString().replace(".java", ""), read(f));
        assertTrue("the capability list is not trivially small", list(m, "preserved_capabilities").size() >= 15);
        for (Map<String, Object> c : list(m, "preserved_capabilities")) {
            String impl = String.valueOf(c.get("implementation")); String cls = impl.contains("#") ? impl.substring(0, impl.indexOf('#')) : impl;
            Class<?> k = Class.forName(cls);
            if (impl.contains("#")) { String name = impl.substring(impl.indexOf('#') + 1); boolean found = false; for (Method x : k.getDeclaredMethods()) found |= x.getName().equals(name); assertTrue(impl + " has no executable implementation", found); }
            @SuppressWarnings("unchecked") List<String> tests = (List<String>) c.get("tests"); assertFalse(impl + " has no test", tests.isEmpty());
            for (String t : tests) {
                if (t.startsWith("HOST:")) { assertTrue(t + " not in HOST_SUITE", hosts.contains(t.substring(5))); continue; }
                String testClass = t.substring(t.lastIndexOf('.') + 1, t.indexOf('#')), method = t.substring(t.indexOf('#') + 1);
                String text = testSources.get(testClass); assertNotNull(t + " test class missing", text);
                assertTrue(t + " is not a @Test method", Pattern.compile("@(?:org\\.junit\\.)?Test\\b[^{;]*?void\\s+" + Pattern.quote(method) + "\\s*\\(", Pattern.DOTALL).matcher(text).find());
                Class.forName(t.substring(0, t.indexOf('#')));
            }
        }
    }

    @Test public void retirementKeepsSchemaRetentionAndReportRegistriesAndTheUpgradePath() throws Exception {
        Map<String, Object> m = manifest();
        // Retention: every rebuilt table and every protected upgrade root keeps a non-age retention classification
        Set<String> research = new HashSet<>(com.ethk3.novaonepro.strategyv2.ResearchRetentionR30.PROTECTED_TABLES);
        for (String t : new String[] {"b03_selected_event", "research_development_receipt_v2", "su2_model_genome"})
            assertTrue(t + " lost its retention registration", research.contains(t) || registered(t));
        for (String root : SchemaDomainCoordinatorR926.PROTECTED_ROOTS_R31) {
            assertTrue(root + " lost its retention registration", research.contains(root) || registered(root));
            assertFalse(root + " deletable by age", com.ethk3.novaonepro.strategyv2.ResearchRetentionR30.deletableByAgeR31(root));
        }
        // Schema: no Critical/Research/Legacy contract object and no protected root is a DROP TABLE target that is not rebuilt
        Set<String> dropped = new HashSet<>(); for (String d : drops()) if (d.contains("|TABLE|") || d.contains("|VIEW|")) dropped.add(d.substring(d.lastIndexOf('|') + 1));
        Set<String> recreated = new HashSet<>(Arrays.asList("su2_model_genome", "research_development_receipt_v2", "expr:legacy"));
        for (SchemaDomainCoordinatorR926.Contract c : Arrays.asList(SchemaDomainCoordinatorR926.criticalContract(), SchemaDomainCoordinatorR926.researchContract(), SchemaDomainCoordinatorR926.legacyContract()))
            for (String o : c.objects) assertTrue(c.domain + " object dropped without rebuild: " + o, !dropped.contains(o) || recreated.contains(o));
        for (String root : SchemaDomainCoordinatorR926.PROTECTED_ROOTS_R31) assertFalse("protected root dropped: " + root, dropped.contains(root));
        assertTrue("every dropped table is rebuilt", recreated.containsAll(dropped));
        // Upgrade path: the upgrade journal shares no domain with a schema contract and changes no user_version
        assertFalse(SchemaDomainCoordinatorR926.UPGRADE_PREFIX_R31.startsWith(SchemaDomainCoordinatorR926.DOMAIN_CRITICAL));
        String coordinator = read(MAIN.resolve("com/ethk3/novaonepro/SchemaDomainCoordinatorR926.java"));
        int journal = coordinator.indexOf("public static UpgradeOutcomeR31 runUpgradeR31"), end = coordinator.indexOf("private static void markUpgradeR31");
        assertTrue(journal > 0 && end > journal); String body = coordinator.substring(journal, end);
        for (String forbidden : new String[] {"user_version=", "DROP ", "RENAME", "DELETE FROM"}) assertFalse("upgrade journal performs " + forbidden, body.toUpperCase(Locale.ROOT).contains(forbidden.toUpperCase(Locale.ROOT)));
        // Report: the chart/report registry is unchanged and consistent
        assertTrue(com.ethk3.novaonepro.strategyv2.ReportObjectIndexR30.chartRegistryConsistentR31());
        assertEquals(47, ChartRegistryR31.ids().size());
        // DbStore: mapped, not cut; the coverage map is the current source
        @SuppressWarnings("unchecked") Map<String, Object> db = (Map<String, Object>) m.get("dbstore");
        assertEquals("DEFERRED_TARGET_SELECTION_AFTER_COVERAGE", db.get("status")); assertEquals(Collections.emptyList(), db.get("removed_by_this_card"));
        byte[] source = Files.readAllBytes(MAIN.resolve("com/ethk3/novaonepro/DbStore.java"));
        StringBuilder hex = new StringBuilder(); for (byte b : MessageDigest.getInstance("SHA-256").digest(source)) hex.append(String.format(Locale.ROOT, "%02x", b & 0xff));
        assertEquals("DbStore changed after its coverage map was built (regenerate and re-review)", db.get("source_sha256"), hex.toString());
        Pattern method = Pattern.compile("^    (?:(?:public|private|protected|static|synchronized|final)\\s+)*[\\w<>\\[\\],.?]+(?:<[^>]*>)?\\s+(\\w+)\\s*\\(");
        int declarations = 0; for (String line : new String(source, StandardCharsets.UTF_8).split("\n", -1)) { Matcher x = method.matcher(line); if (x.find() && !Arrays.asList("if", "for", "while", "switch", "catch", "return", "new", "else", "synchronized").contains(x.group(1))) declarations++; }
        assertEquals("every DbStore method is in the coverage map", ((Number) db.get("method_declarations")).intValue(), declarations);
        assertEquals(declarations, list(db, "methods").size());
    }

    static boolean registered(String table) { try { RetentionTableRegistryCP12.require(table); return true; } catch (IllegalArgumentException missing) { return false; } }
}
