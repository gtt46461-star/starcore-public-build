package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import org.junit.Test;

/** R31-B07-C02#A1/#A2: oversized, too deep, too many nodes, unknown op, non-finite constant, bad integer and wrong
 * arity are refused as typed errors before recursion (never StackOverflowError/OOM); legal policies round-trip. */
public final class R31PolicyAstParseBudgetTest {
    private int r31LeasesBeforeTest;
    @org.junit.Before public void r31OutstandingLeaseGuardBefore(){FeatureCacheR27.trimRegisteredCachesR31();r31LeasesBeforeTest=MemoryBudgetR31.processBudget().explicitOwnerLeaseCountR31();}
    @org.junit.After public void r31OutstandingLeaseGuardAfter(){
        FeatureCacheR27.trimRegisteredCachesR31();MemoryBudgetR31 memory=MemoryBudgetR31.processBudget();int after=memory.explicitOwnerLeaseCountR31();
        org.junit.Assert.assertTrue("R31_OUTSTANDING_LEASES before="+r31LeasesBeforeTest+" after="+after+" owners="+memory.explicitOwnerLeasesR31(64),after<=r31LeasesBeforeTest);
    }
    private static String rejected(String text) {
        try { PolicyAstR27.parse(text); fail("accepted: " + text.substring(0, Math.min(80, text.length()))); return null; }
        catch (IllegalArgumentException expected) { return expected.getMessage(); }
    }
    private static String nested(int depth) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < depth; i++) b.append("(neg ");
        b.append("(freq 3 32)");
        for (int i = 0; i < depth; i++) b.append(')');
        return b.toString();
    }
    @Test public void deepHugeAndWideInputsAreTypedRefusalsBeforeRecursion() {
        assertEquals("a 100k-deep input (~700 KB) is refused by the byte limit before any recursion", "R31_AST_BYTES_EXCEEDED", rejected(nested(100_000)));
        assertEquals("deep but small inputs are refused by depth before recursion", "R31_AST_DEPTH_EXCEEDED", rejected(nested(1_000)));
        assertEquals("R31_AST_DEPTH_EXCEEDED", rejected(nested(PolicyAstR27.IMPORT_MAX_DEPTH)));
        assertNotNull(PolicyAstR27.parse(nested(PolicyAstR27.IMPORT_MAX_DEPTH - 1)));
        StringBuilder huge = new StringBuilder("(add (freq 3 32) (gap 3)");
        while (huge.length() <= PolicyAstR27.IMPORT_MAX_BYTES) huge.append("          ");
        assertEquals("R31_AST_BYTES_EXCEEDED", rejected(huge.append(')').toString()));
        StringBuilder wide = new StringBuilder();
        for (int i = 0; i < 3000; i++) wide.append("(add (freq 3 32) ");
        wide.append("(gap 3)");
        for (int i = 0; i < 3000; i++) wide.append(')');
        String wideRefusal = rejected(wide.toString());
        assertTrue(wideRefusal, wideRefusal.equals("R31_AST_DEPTH_EXCEEDED") || wideRefusal.equals("R31_AST_NODES_EXCEEDED") || wideRefusal.equals("R31_AST_BYTES_EXCEEDED"));
    }
    @Test public void unknownOpNonFiniteConstantBadIntegerAndArityAreControlled() {
        assertTrue(rejected("(launch_missiles 1)").startsWith("R27_AST_UNKNOWN_OP"));
        assertEquals("R27_NONFINITE_CONSTANT", rejected("(c NaN)"));
        assertEquals("R27_NONFINITE_CONSTANT", rejected("(c Infinity)"));
        assertTrue(rejected("(c 1e)").startsWith("R27_AST_PARAMETER"));
        assertTrue(rejected("(freq 3x 32)").startsWith("R27_AST_INT"));
        assertTrue(rejected("(add (freq 3 32)").startsWith("R27_AST_EOF"));
        PolicyAstR27.Node arity = PolicyAstR27.parse("(add (freq 3 32))");
        try { PolicyAstR27.type(arity); fail("wrong arity typed"); } catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().startsWith("TYPE:ARITY")); }
    }
    @Test public void legalPoliciesRoundTripExactlyAndBudgetsCannotBeWidened() {
        for (String text : new String[]{"(c 0.125)", "(zscore (freq 3 32))", "(softmax 2.0 (freq 3 32))", "(mix 0.25 (freq 3 32) (gap 3))", "(if (freq 3 32) (gap 3) (freq 3 32) (gap 3))"}) {
            PolicyAstR27.Node node = PolicyAstR27.parse(text);
            assertEquals("canonical text and identity are stable", node.toString(), PolicyAstR27.parse(node.toString()).toString());
        }
        try { new PolicyAstR27.ParseBudget(PolicyAstR27.IMPORT_MAX_BYTES + 1, 6, 40); fail("wider than import"); }
        catch (IllegalArgumentException expected) { assertEquals("R31_AST_BUDGET_WIDER_THAN_IMPORT", expected.getMessage()); }
        PolicyAstR27.ParseBudget birth = new PolicyAstR27.ParseBudget(4096, 6, 40);
        try { PolicyAstR27.parse(nested(10), birth); fail("birth-sized budget accepted depth 11"); }
        catch (IllegalArgumentException expected) { assertEquals("R31_AST_DEPTH_EXCEEDED", expected.getMessage()); }
    }
}
