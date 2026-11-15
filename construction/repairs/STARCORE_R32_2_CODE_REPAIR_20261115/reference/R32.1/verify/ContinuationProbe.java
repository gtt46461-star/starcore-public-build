package com.ethk3.novaonepro.strategyv2;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ContinuationProbe {
    private static int checks;

    private static void check(boolean value, String name) {
        if (!value) throw new AssertionError(name);
        System.out.println("PASS " + name);
        checks++;
    }

    private static FoundryCampaignR26.HistoryRankR32 rank(String key, int dk, int dn, int sk, int sn) {
        return new FoundryCampaignR26.HistoryRankR32(key, dk, dn, sk, sn);
    }

    private static void historicalParents() {
        check(rank("robust", 40, 100, 40, 100).compareTo(rank("lucky", 1, 1, 1, 1)) < 0, "1/1 cannot displace 40/100 on both development segments");
        check(rank("both", 40, 100, 36, 100).compareTo(rank("collapse", 60, 100, 24, 100)) < 0, "a weak SELECT segment reduces rank");
        check(rank("a", 40, 100, 40, 100).compareTo(rank("b", 40, 100, 40, 100)) < 0, "tied evidence has immutable-key order");
        for (int[] counts : new int[][]{{1,0,1,4}, {2,3,0,0}, {5,4,3,10}, {0,4,-1,10}}) {
            boolean rejected = false;
            try { rank("bad", counts[0], counts[1], counts[2], counts[3]); }
            catch (IllegalArgumentException ex) { rejected = true; }
            check(rejected, "out-of-domain history counts rejected " + Arrays.toString(counts));
        }
        check(rank("weak", 0, 10, 0, 20).lower == 0, "zero performance is reproducible");
    }

    private static void simultaneousCandidates() {
        MainlineBridgeSU2.Intent shadow = new MainlineBridgeSU2.Intent("R27:parent", "parent", 1, 7, .4, false, "WAIT");
        MainlineBridgeSU2.Intent typed = new MainlineBridgeSU2.Intent("R27:parent", "parent", 1, 7, .4, true, "R31_TYPED");
        MainlineBridgeSU2.Intent selective = new MainlineBridgeSU2.Intent("SU2:other", "other", 1, 14, .4, true, "CERTIFIED:cert");
        MainlineBridgeSU2.ProjectionR30 p = new MainlineBridgeSU2.ProjectionR30();
        p.candidate = CertificateSU2.PREFIX + "other"; p.certificate = "cert";
        p.certificateActive = p.freezeComplete = p.modelAvailable = p.selectorConfirmed = p.actRuleFired = true;
        check(MainlineBridgeSU2.preferVerifiedR32(shadow, selective, p) == selective, "only exact certified selective input supersedes a shadow-only proposal");
        check(MainlineBridgeSU2.preferVerifiedR32(typed, selective, p) == typed, "existing R27 certified priority survives");
        p.freezeComplete = false;
        check(MainlineBridgeSU2.preferVerifiedR32(shadow, selective, p) == shadow, "partial freeze rejected");
        p.freezeComplete = true; p.selectorConfirmed = null;
        check(MainlineBridgeSU2.preferVerifiedR32(shadow, selective, p) == shadow, "unknown selector rejected");
        p.selectorConfirmed = true; p.certificate = "rev2";
        check(MainlineBridgeSU2.preferVerifiedR32(shadow, selective, p) == shadow, "different certificate rejected");
        p.certificate = "cert"; p.candidate = "other-policy";
        check(MainlineBridgeSU2.preferVerifiedR32(shadow, selective, p) == shadow, "wrong policy rejected");
        check(MainlineBridgeSU2.preferVerifiedR32(shadow, null, p) == shadow, "missing alternative preserves diagnostic shadow");
    }

    @SuppressWarnings("unchecked")
    private static void reports(Path file) throws Exception {
        List<?> snapshots = (List<?>)JsonR27.parse(Files.readString(file));
        for (Object raw : snapshots) {
            Map<?,?> original = (Map<?,?>)raw;
            List<Map<String,Object>> chapters = (List<Map<String,Object>>)original.get("chapters");
            Map<String,Object> summary = RepairDiagnosisR32.operatingSummaryR32(chapters);
            Map<?,?> quality = (Map<?,?>)summary.get("independentFuture");
            Map<?,?> network = (Map<?,?>)summary.get("atomNet");
            Map<?,?> tasks = (Map<?,?>)summary.get("tasks");
            check("NO_SETTLED_FUTURE_ACT".equals(quality.get("rateStatus")) && quality.get("futureHitRate") == null, "empty independent future has no percent");
            check(((Number)network.get("materializedSamples")).longValue() == ((Number)original.get("expectedPrepared")).longValue()
                    && ((Number)network.get("trainingSamplesProcessed")).longValue() == 0 && Boolean.FALSE.equals(network.get("publicationReady")), "observed material is separated from training and publication");
            check("BLOCKED".equals(((Map<?,?>)tasks.get("foundry-su2")).get("status")) &&
                    summary.get("foundryRepair").toString().contains("LifecycleSchemaR31.advance"), "field quota block names its actual writer");
            Map<String,Object> detail = new LinkedHashMap<>(Map.of("rows",List.of(), "coverageGaps",List.of(), "operatingSummary",summary));
            String[] projection = RepairDiagnosisR32.attach(new String[]{"old-md",JsonR27.write(original.get("legacy"))}, detail);
            Map<?,?> json = (Map<?,?>)JsonR27.parse(projection[1]);
            Map<?,?> finding = (Map<?,?>)((List<?>)json.get("legacyFindingExplanationsR32")).get(0);
            check(!String.valueOf(finding.get("nextAction")).isEmpty() && projection[0].contains(String.valueOf(finding.get("verify"))), "S4 & verification present in both export formats");
            check("".equals(((Map<?,?>)((List<?>)json.get("findings")).get(0)).get("prescription")), "original empty machine prescription preserved");
        }
        List<Map<String,Object>> positive = List.of(Map.of("sectionId","R30_01","commonMetrics",Map.of("futureActs",40,"futureHits",16)));
        Map<?,?> quality = (Map<?,?>)RepairDiagnosisR32.operatingSummaryR32(positive).get("independentFuture");
        check(Double.valueOf(.4).equals(quality.get("futureHitRate")) && "OBSERVED_FUTURE_ONLY".equals(quality.get("rateStatus")), "valid independent future ratio uses its own denominator");
        List<Map<String,Object>> invalid = List.of(Map.of("sectionId","R30_01","commonMetrics",Map.of("futureActs",2,"futureHits",3)));
        check(((Map<?,?>)RepairDiagnosisR32.operatingSummaryR32(invalid).get("independentFuture")).get("futureHitRate")==null, "invalid counts stay unmeasured");
        Map<?,?> absent = RepairDiagnosisR32.operatingSummaryR32(List.of());
        check(((Map<?,?>)absent.get("tasks")).get("foundry-su2")==null &&
                ((Map<?,?>)absent.get("atomNet")).get("trainingSamplesProcessed")==null, "missing sources remain unknown");
    }

    public static void main(String[] args) throws Exception {
        historicalParents();
        simultaneousCandidates();
        reports(Path.of(args[0]));
        List<Map<String,Object>> queryPlans = new ArrayList<>();
        for (int stage = 0; stage < 4; stage++) {
            queryPlans.add(Map.of("first",HypothesisStoreR31.quotaPageR32(stage,false),
                    "resume",HypothesisStoreR31.quotaPageR32(stage,true)));
        }
        check(HypothesisStoreR31.quotaPageArgsR32(List.of()) == null, "empty migration page has no bind parameters");
        check(Arrays.equals(HypothesisStoreR31.quotaPageArgsR32(List.of("c","p","17")),
                new String[]{"c","c","c","p","c","p","17"}), "three-column cursor uses SQLite scalar bind order");
        List<Object> sample = new ArrayList<>(List.of(2, 234891L, List.of("c","p","17")));
        String seal = StatsR27.sha256(JsonR27.write(sample));
        sample.add(seal);
        List<?> restored=(List<?>)JsonR27.parse(JsonR27.write(sample));
        check(seal.equals(StatsR27.sha256(JsonR27.write(restored.subList(0,3)))), "checkpoint numeric & key types restore the same migration digest");
        Files.writeString(Path.of(args[1]),JsonR27.write(queryPlans));
        System.out.println("passed=" + checks);
    }
}
