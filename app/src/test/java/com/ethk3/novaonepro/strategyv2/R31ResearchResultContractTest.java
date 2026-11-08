package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.lang.reflect.*;
import java.util.*;
import org.junit.Test;

/** R31-B08-C02#A2/#A4, R31-B08-C04#A1-#A3: research products reach consumers through a typed, verifiable contract. */
public final class R31ResearchResultContractTest {
    static ResearchContextR30R1 context(boolean signal, String run) throws Exception {
        Constructor<ResearchContextR30R1> c = ResearchContextR30R1.class.getDeclaredConstructor(String.class, long.class); c.setAccessible(true);
        ResearchContextR30R1 ctx = c.newInstance(StatsR27.sha256("dataset"), 3L);
        List<Object> values = new ArrayList<>(), support = new ArrayList<>();
        for (int k = 0; k < 10; k++) { List<Object> v = new ArrayList<>(), n = new ArrayList<>(); for (int p = 0; p < 10; p++) { v.add(signal && p == 6 ? (k == 0 ? .28 : .08) : .1); n.add(400L); } values.add(v); support.add(n); }
        Map<String, Object> frame = new LinkedHashMap<>(); frame.put("values", values); frame.put("support", support);
        put(ctx, "frames", "MATRIX:NUMBER_POSITION_COMPATIBILITY", frame); put(ctx, "runs", "MATRIX:NUMBER_POSITION_COMPATIBILITY", run);
        return ctx;
    }
    @SuppressWarnings("unchecked") static void put(Object target, String field, String key, Object value) throws Exception { Field f = ResearchContextR30R1.class.getDeclaredField(field); f.setAccessible(true); ((Map<String, Object>) f.get(target)).put(key, value); }
    static PolicySpecR27 parent() { return new PolicySpecR27(PolicyAstR27.op("zscore", PolicyAstR27.leaf("freq", 1, 32)), 2, PolicySpecR27.Rule.ALWAYS, 0, 1, 1, false, .01, 0, IdeaFamilyR27.meta("I04", "TEST", 0, 1, "fixture")); }

    @Test public void aControlledSignalChangesTheConsumerProductAndAnEmptySignalProducesNothing() throws Exception {
        String run = StatsR27.sha256("run-A"); int changed = 0;
        for (long seed = 1; seed <= 16; seed++) {
            ResearchContextR30R1 withSignal = context(true, run), empty = context(false, run);
            int chosen = withSignal.position(new StatsR27.SplitMix(seed), 2), none = empty.position(new StatsR27.SplitMix(seed), 2);
            assertEquals("a flat (no-signal) frame proposes nothing new", 2, none);
            if (chosen != 2) {
                changed++; assertEquals("the signalled position is consumed", 7, chosen);
                PolicySpecR27 p = parent(), child = withSignal.annotate(p.withSelector(chosen, p.rule, p.threshold, p.rate, IdeaFamilyR27.meta("I04", "RESEARCH_POSITION", 1, seed, "position from matrix", p.policyHash())));
                assertNotEquals("producer -> consumer -> new product hash", p.policyHash(), child.policyHash());
                assertEquals(Collections.singletonList(run), ResearchContextR30R1.references(child));
                Map<String, Object> usage = withSignal.usagesR31().get(0);
                assertEquals(child.policyHash(), usage.get("childHash")); assertEquals("STRUCTURE_OR_PARAMETER_CHANGED", usage.get("effect")); assertEquals(Collections.singletonList(run), usage.get("sourceRuns"));
                // ablation: the same consumer without that source run returns to the fallback
                assertEquals(2, withSignal.withoutSourceRunR31(run).position(new StatsR27.SplitMix(seed), 2));
                assertTrue(withSignal.withoutSourceRunR31(run).sourceRunsR31().isEmpty());
            }
        }
        assertTrue("the signal is consumed on most draws (25% exploration keeps the fallback): " + changed, changed >= 8);
    }

    @Test public void effectivenessRequiresTheSameBudgetAblationToBeWorse() {
        ResearchGainR30R1 better = new ResearchGainR30R1(1, 512, 64, 4, 200), noise = new ResearchGainR30R1(1, 512, 64, 4, 200);
        StatsR27.SplitMix r = new StatsR27.SplitMix(5);
        for (long t = 1; t <= 512; t++) { better.add(t, .45 + .01 * r.nextDouble(), .40 + .01 * r.nextDouble()); double x = .4 + .1 * r.nextDouble(); noise.add(t, x, .4 + .1 * r.nextDouble()); }
        assertEquals("EFFECTIVE", ResearchResultContractR31.effectiveness(better.estimate(), 100, 100));
        assertEquals("NOT_SHOWN_EFFECTIVE", ResearchResultContractR31.effectiveness(noise.estimate(), 100, 100));
        assertEquals("NOT_COMPARABLE_DIFFERENT_BUDGET", ResearchResultContractR31.effectiveness(better.estimate(), 100, 120));
        assertEquals("INSUFFICIENT_SUPPORT", ResearchResultContractR31.effectiveness(new ResearchGainR30R1(1, 512, 64, 4, 200).estimate(), 100, 100));
        assertEquals("a development interval stays descriptive", "DESCRIPTIVE_RESEARCH_ONLY", better.estimate().status);
    }

    static Map<String, Object> receipts() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("source_run", StatsR27.sha256("run")); m.put("input_hash", StatsR27.sha256("in")); m.put("role", "SELECT"); m.put("abi", "ABI-1"); m.put("source_revision", 3L);
        m.put("consumer_version", "ResearchContextR30R1.position@R31"); m.put("before_hash", StatsR27.sha256("before")); m.put("after_hash", StatsR27.sha256("after")); m.put("budget", 256L); m.put("product_hash", StatsR27.sha256("product"));
        return m;
    }
    static ResearchResultContractR31.Result result(String view, String role, long revision, String abi, String parent, long label, Map<String, Object> receipts) {
        return new ResearchResultContractR31.Result(view, ResearchResultContractR31.ProductState.PRODUCED, "D", revision, abi, role, "POSITION", parent, label, receipts);
    }
    static String reject(ResearchResultContractR31.Result r, Set<String> consumed) {
        try { ResearchResultContractR31.validateForConsumption(r, new ResearchResultContractR31.Expect("D", 3, "ABI-1", "POSITION", "P", 1000), "consumer", "child", consumed); return "ACCEPTED"; }
        catch (ResearchResultContractR31.Rejected e) { return e.code; }
    }
    @Test public void lateLabelsWrongAbiRevisionParentAndRepeatedConsumptionAreRefused() {
        Set<String> consumed = new HashSet<>();
        ResearchResultContractR31.Result ok = result("R03", "SELECT", 3, "ABI-1", "P", 999, receipts());
        String key = ResearchResultContractR31.validateForConsumption(ok, new ResearchResultContractR31.Expect("D", 3, "ABI-1", "POSITION", "P", 1000), "consumer", "child", consumed);
        consumed.add(key);
        assertEquals("R31_RESULT_DUPLICATE_CONSUME", reject(ok, consumed));
        assertEquals("R31_RESULT_LATE_LABEL:1000>=1000", reject(result("R03", "SELECT", 3, "ABI-1", "P", 1000, receipts()), consumed));
        assertEquals("R31_RESULT_ABI", reject(result("R03", "SELECT", 3, "ABI-2", "P", 999, receipts()), consumed));
        assertEquals("R31_RESULT_SOURCE_REVISION", reject(result("R03", "SELECT", 4, "ABI-1", "P", 999, receipts()), consumed));
        assertEquals("R31_RESULT_PARENT", reject(result("R03", "SELECT", 3, "ABI-1", "Q", 999, receipts()), consumed));
        Map<String, Object> confirm = receipts(); confirm.put("role", "CONFIRM");
        assertEquals("R31_RESULT_FORMAL_ROLE:CONFIRM", reject(result("R03", "CONFIRM", 3, "ABI-1", "P", 999, confirm), consumed));
        Map<String, Object> unchanged = receipts(); unchanged.put("after_hash", unchanged.get("before_hash"));
        assertEquals("R31_RESULT_NO_EFFECTIVE_CHANGE", reject(result("R03", "SELECT", 3, "ABI-1", "P", 999, unchanged), consumed));
        for (String k : ResearchResultContractR31.REQUIRED_ACTION_RECEIPTS) { Map<String, Object> missing = receipts(); missing.remove(k); assertEquals("R31_RESULT_RECEIPT_MISSING:" + k, reject(result("R03", "SELECT", 3, "ABI-1", "P", 999, missing), consumed)); }
        for (String formal : new String[]{"R11", "R14", "R18"}) {
            assertEquals(ResearchResultContractR31.ConsumptionKind.FORMAL_READ_ONLY, ResearchResultContractR31.consumptionKindOf(formal));
            assertEquals("R31_RESULT_NOT_APPLICABLE_OPTIMIZATION:" + formal, reject(result(formal, "SELECT", 3, "ABI-1", "P", 999, receipts()), consumed));
        }
        ResearchResultContractR31.Result waiting = new ResearchResultContractR31.Result("R03", ResearchResultContractR31.ProductState.WAITING_STUDENT, "D", 3, "ABI-1", "SELECT", "POSITION", "P", 999, receipts());
        assertEquals("R31_RESULT_STATE:WAITING_STUDENT", reject(waiting, consumed));
        for (ResearchGraphSpecsR30R1.Spec s : ResearchGraphSpecsR30R1.ALL)
            assertEquals(s.id, s.consumerKind == ResearchResultContractR31.ConsumptionKind.RESEARCH_ACTION ? ResearchResultContractR31.REQUIRED_ACTION_RECEIPTS : Collections.emptyList(), s.requiredReceipts);
    }
}
