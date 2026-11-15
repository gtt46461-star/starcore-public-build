package com.ethk3.novaonepro;

import static org.junit.Assert.*;

import java.nio.file.*;
import java.util.*;
import org.junit.Test;

/** R31-B09-C01#A1-#A3, B09-C02#A1a/#A2a, B09-C04#A1-#A4, B09-C05#A1a/#A3, B05-C08#A2/#A3, B09-C03#A2. */
public final class R31ChartRegistryTest {
    @Test public void fortySevenUniqueIdsAndOnlyTwelveR83PanelsExecute() throws Exception {
        assertEquals(47, ChartRegistryR31.all().size()); assertEquals(47, new HashSet<>(ChartRegistryR31.ids()).size());
        assertEquals(29, ChartRegistryR31.visibleLegacyIds().size()); assertEquals(12, ChartRegistryR31.R83_VISIBLE.size());
        for (int g = 0; g < 3; g++) assertEquals("six research views per workbench group", 6, ChartRegistryR31.researchGroup(g).size());
        String r83 = new String(Files.readAllBytes(Paths.get("src/main/java/com/ethk3/novaonepro/ChartsRepositoryR83.java")), "UTF-8");
        String load = r83.substring(r83.indexOf("static List<Panel> load("), r83.indexOf("static Panel loadPanel("));
        for (String retired : new String[]{"mainlineWeight(", "evolutionEvents(", "lineage("}) {
            assertFalse("retired builder " + retired + " is not executed", load.contains(retired));
            String dispatch = r83.substring(r83.indexOf("static Panel loadPanel("), r83.indexOf("interface Builder"));
            assertFalse(dispatch.contains(retired));
        }
        String r27 = new String(Files.readAllBytes(Paths.get("src/main/java/com/ethk3/novaonepro/ChartsRepositoryR27.java")), "UTF-8");
        String page = r27.substring(r27.indexOf("static List<Panel> load("), r27.indexOf("static Panel headline("));
        assertFalse("titles never filter or route a panel", page.contains("title.startsWith") || page.contains("title.equals"));
    }
    @Test public void aRenamedTitleChangesNoSelectionAndUiReportAndMachineIdSetsAreEqual() {
        List<String> registry = ChartRegistryR31.ids();
        assertEquals("report index", registry, com.ethk3.novaonepro.strategyv2.ReportObjectIndexR30.chartIdsR31());
        assertTrue(com.ethk3.novaonepro.strategyv2.ReportObjectIndexR30.chartRegistryConsistentR31());
        assertEquals("charts page order", ChartRegistryR31.visibleLegacyIds(), ChartsRepositoryR27.panelIdsR31());
        List<String> machine = new ArrayList<>(); for (ChartRegistryR31.Entry e : ChartRegistryR31.all()) machine.add(String.valueOf(e.json().get("id")));
        assertEquals("machine table", registry, machine);
        for (com.ethk3.novaonepro.strategyv2.ResearchGraphSpecsR30R1.Spec s : com.ethk3.novaonepro.strategyv2.ResearchGraphSpecsR30R1.ALL)
            assertEquals("a research view is found by id, its title is display text only", s.id, ChartRegistryR31.find(s.id).id);
    }
    @Test public void oneBlockedOrMalformedPanelLeavesTheOthersInBudget() throws Exception {
        try (ChartRegistryR31.PanelLoader loader = new ChartRegistryR31.PanelLoader(2)) {
            long epoch = loader.begin(); long started = System.nanoTime();
            List<ChartRegistryR31.PanelLoader.Outcome> out = loader.load(epoch, Arrays.asList("p01_three_scores", "r83_calibration", "R03", "p08_bss"), id -> {
                if (id.equals("r83_calibration")) { Thread.sleep(10_000); return "never"; }
                if (id.equals("R03")) throw new IllegalArgumentException("MALFORMED_FRAME");
                return "frame:" + id;
            }, 300);
            long ms = (System.nanoTime() - started) / 1_000_000;
            Map<String, ChartRegistryR31.PanelLoader.Status> s = new HashMap<>(); for (ChartRegistryR31.PanelLoader.Outcome o : out) s.put(o.id, o.status);
            assertEquals(ChartRegistryR31.PanelLoader.Status.READY, s.get("p01_three_scores")); assertEquals(ChartRegistryR31.PanelLoader.Status.READY, s.get("p08_bss"));
            assertEquals(ChartRegistryR31.PanelLoader.Status.TIMEOUT, s.get("r83_calibration")); assertEquals(ChartRegistryR31.PanelLoader.Status.FAILED, s.get("R03"));
            assertTrue("inside the page budget: " + ms, ms < 3000);
        }
        try { new ChartRegistryR31.PanelLoader(3); fail("more than two UI reads"); } catch (IllegalArgumentException e) { assertEquals("R31_PANEL_LOADER_CONCURRENCY", e.getMessage()); }
    }
    @Test public void oneHundredSwitchesNeverDeliverAnOldEpochAndThreadsReturnToBaseline() throws Exception {
        Set<Thread> before = new HashSet<>(Thread.getAllStackTraces().keySet());
        ChartRegistryR31.PanelLoader loader = new ChartRegistryR31.PanelLoader(2); int staleDelivered = 0;
        for (int i = 0; i < 100; i++) {
            long mine = loader.begin(); final boolean switchDuring = i % 2 == 0;
            List<ChartRegistryR31.PanelLoader.Outcome> out = loader.load(mine, Collections.singletonList("p01_three_scores"), id -> { if (switchDuring) loader.begin(); return "frame"; }, 1000);
            for (ChartRegistryR31.PanelLoader.Outcome o : out) {
                if (switchDuring) assertEquals("a superseded page never shows its frame", ChartRegistryR31.PanelLoader.Status.STALE_EPOCH, o.status);
                if (o.status == ChartRegistryR31.PanelLoader.Status.READY && o.epoch != loader.epoch()) staleDelivered++;
            }
        }
        assertEquals(0, staleDelivered);
        loader.close(); assertTrue(loader.awaitClosed(2000));
        Thread.sleep(50); Set<Thread> after = new HashSet<>(Thread.getAllStackTraces().keySet()); after.removeAll(before); after.removeIf(t -> !t.isAlive());
        assertEquals("loader threads are gone", Collections.emptySet(), after);
    }
    @Test public void evidenceAccountingNeverDoubleCountsMixesAbisOrUsesTheDisplayLimit() {
        assertEquals("multiple testing uses every candidate, not the LIMIT", 140, ChartsRepositoryR83.EvidenceAccountingR31.multipleTestingCount(140, 60));
        assertEquals(140, ChartsRepositoryR83.EvidenceAccountingR31.multipleTestingCount(140, 20));
        try { ChartsRepositoryR83.EvidenceAccountingR31.multipleTestingCount(10, 20); fail(); } catch (IllegalStateException e) { assertEquals("R31_FULL_CANDIDATES_BELOW_DISPLAYED", e.getMessage()); }
        List<String[]> rows = Arrays.asList(new String[]{"SHADOW", "202611010001"}, new String[]{"REAL", "202611010001"}, new String[]{"RESEARCH", "202611010001"}, new String[]{"SHADOW", "202611010002"});
        assertEquals("switching role does not add samples", 2, ChartsRepositoryR83.EvidenceAccountingR31.distinctIssues(rows));
        assertNull("old and new ABI are never averaged into one value", ChartsRepositoryR83.EvidenceAccountingR31.averageWithinOneAbi(Arrays.asList(new Object[]{"ABI-1", .3}, new Object[]{"ABI-2", .5})));
        assertEquals(.4, ChartsRepositoryR83.EvidenceAccountingR31.averageWithinOneAbi(Arrays.asList(new Object[]{"ABI-1", .3}, new Object[]{"ABI-1", .5})), 1e-12);
        Map<String, Object> none = ChartsRepositoryR83.EvidenceAccountingR31.validAssetSummary(0, 0, 37);
        assertEquals("NO_VALID_ASSET_RESEARCH_SHOWN_IN_FULL", none.get("status")); assertEquals(0L, none.get("realActionsShown")); assertEquals(37L, none.get("researchOutputs"));
    }
    @Test public void everyIdHasBudgetsAnEmptyReasonAndCachesAreClassifiedBeforeAnyGc() {
        for (ChartRegistryR31.Entry e : ChartRegistryR31.all()) {
            assertFalse(e.id, e.emptyState.isEmpty()); assertTrue(e.id, e.byteBudget > 0 && e.pointBudget > 0); assertFalse(e.id, e.sourceOwner.isEmpty());
            assertFalse("no chart claims natural advantage from fixtures", e.role.contains("NATURAL"));
        }
        assertEquals(com.ethk3.novaonepro.strategyv2.ChartCacheR27.CacheClassR31.BUSINESS_STATE, com.ethk3.novaonepro.strategyv2.ChartCacheR27.classR31("mainline"));
        assertEquals("an unknown panel is protected", com.ethk3.novaonepro.strategyv2.ChartCacheR27.CacheClassR31.BUSINESS_STATE, com.ethk3.novaonepro.strategyv2.ChartCacheR27.classR31("some-new-panel"));
        for (String checkpoint : new String[]{"research-study-r1", "matrix-r30-r1", "r30-qualification-gap"}) assertEquals(com.ethk3.novaonepro.strategyv2.ChartCacheR27.CacheClassR31.CHECKPOINT, com.ethk3.novaonepro.strategyv2.ChartCacheR27.classR31(checkpoint));
        String sql = com.ethk3.novaonepro.strategyv2.ResearchRetentionStoreR30.visualCacheDeleteSql();
        for (String p : com.ethk3.novaonepro.strategyv2.ChartCacheR27.displayPanelsR31()) assertTrue(sql.contains("'" + p + "'"));
        for (String protectedPanel : new String[]{"mainline", "asset", "report", "research-study-r1", "matrix-r30-r1", "r30-charts"}) assertFalse("GC never scans " + protectedPanel, sql.contains("'" + protectedPanel + "'"));
    }
}
