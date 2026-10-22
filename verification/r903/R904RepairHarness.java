package com.ethk3.novaonepro;

import java.util.HashSet;
import java.util.List;

/** Host-side invariant checks for the R904 runtime identity and load repair. */
public final class R904RepairHarness {
    public static void main(String[] args) {
        String hash = repeat('a', 64);
        List<ResearchStrategyDslEngine.Node> first =
                ResearchNovelFormulaSynthesizerR903.create(
                        "NUMBER_RISK", "disagreement and residual", hash, 20);
        List<ResearchStrategyDslEngine.Node> second =
                ResearchNovelFormulaSynthesizerR903.create(
                        "NUMBER_RISK", "disagreement and residual", hash, 20);
        require(first.size() >= 16, "NOVEL_COUNT=" + first.size());
        require(first.size() == second.size(), "NON_DETERMINISTIC_COUNT");
        HashSet<String> unique = new HashSet<String>();
        boolean disagreement = false, ratio = false, interaction = false;
        for (int i = 0; i < first.size(); i++) {
            String a = ResearchStrategyDslEngine.canonical(first.get(i));
            String b = ResearchStrategyDslEngine.canonical(second.get(i));
            require(a.equals(b), "NON_DETERMINISTIC_FORMULA_" + i);
            require(unique.add(a), "DUPLICATE_FORMULA_" + i);
            require(ResearchStrategyDslEngine.complexity(first.get(i)) > 0.0d, "ZERO_COMPLEXITY");
            disagreement |= a.indexOf("ABS(SUB(") >= 0;
            ratio |= a.indexOf("SAFE_DIV(") >= 0;
            interaction |= a.indexOf("INTERACTION(") >= 0;
        }
        require(disagreement && ratio && interaction, "STRUCTURAL_DIVERSITY_MISSING");
        List<ResearchMethodGraphSynthesizerR903.Blueprint> methods =
                ResearchMethodGraphSynthesizerR903.create(
                        "MODEL_ERROR", "create distinct method families", hash, 52);
        HashSet<String> families = new HashSet<String>();
        boolean markov = false, forest = false, neural = false, graph = false, moe = false, novel = false;
        for (int i = 0; i < methods.size(); i++) {
            ResearchMethodGraphSynthesizerR903.Blueprint method = methods.get(i);
            require(families.add(method.family), "DUPLICATE_METHOD_FAMILY");
            require(method.canonicalSpec.indexOf("PIT_PROTOCOL") >= 0, "METHOD_PIT_PROTOCOL");
            require(method.canonicalSpec.indexOf("FALSIFY") >= 0, "METHOD_FALSIFIER");
            require(method.canonicalSpec.indexOf("real_authority=false") >= 0, "METHOD_REAL_BOUNDARY");
            markov |= "MARKOV_REGIME".equals(method.family);
            forest |= "ONLINE_FOREST".equals(method.family);
            neural |= "TEMPORAL_NEURAL".equals(method.family);
            graph |= "PERMUTATION_GRAPH".equals(method.family);
            moe |= "DIVERSE_MOE".equals(method.family);
            novel |= method.family.startsWith("NOVA_");
        }
        require(methods.size() >= 10, "METHOD_FAMILY_COUNT=" + methods.size());
        require(markov && forest && neural && graph && moe && novel, "METHOD_FAMILY_DIVERSITY");
        require(ResearchMainlineGovernorR903.MIN_FUTURE_N >= 120, "MAINLINE_SAMPLE_GATE");
        require(ResearchMainlineGovernorR903.MIN_WILSON_LOWER >= 0.30d, "MAINLINE_WILSON_GATE");
        require(ResearchMainlineGovernorR903.MAX_BRIER <= 0.21d, "MAINLINE_BRIER_GATE");
        require(ResearchMainlineGovernorR903.SIMULTANEOUS_Z >= 3.4d, "MAINLINE_MULTIPLE_TEST_GATE");
        require(!ResearchMainlineGovernorR903.MAY_AUTHORIZE_REAL, "MAINLINE_REAL_AUTHORITY");
        require(V51BuildManifest.VERSION_CODE == 577200904, "VERSION_CODE");
        require(
                "V57.16.4-R4.0.4-904".equals(V51BuildManifest.VERSION_NAME),
                "VERSION_NAME");
        require(V51BuildManifest.SHADOW_ONLY, "RESEARCH_BOUNDARY_SHADOW_ONLY");
        require(V51BuildManifest.REAL_BET_HARD_DISABLED, "REAL_HARD_LOCK");
        require(ReleaseIdentityR817.isConsistent(), "INTERNAL_IDENTITY|" + ReleaseIdentityR817.mismatchDetail());
        require(
                R36AnalysisLeasePolicyR365.canUpgradeRearm(
                        AnalysisRequestStoreR36.FAILED_TERMINAL,
                        "V57.16.3-R4.0.3-903",
                        false,
                        false,
                        false),
                "R903_FAILED_REQUEST_NOT_REARMED");
        require(
                !R36AnalysisLeasePolicyR365.canUpgradeRearm(
                        AnalysisRequestStoreR36.FAILED_TERMINAL,
                        AppConfig.R36_RUNTIME_REPAIR_VERSION,
                        false,
                        false,
                        false),
                "R904_REQUEST_REARM_LOOP");
        System.out.println(
                "R904_REPAIR_HARNESS=PASS|novel="
                        + first.size()
                        + "|unique="
                        + unique.size()
                        + "|methodFamilies="
                        + families.size()
                        + "|structuralDiversity=PASS|realBoundary=PASS");
    }

    private static String repeat(char value, int count) {
        StringBuilder out = new StringBuilder(count);
        for (int i = 0; i < count; i++) out.append(value);
        return out.toString();
    }

    private static void require(boolean condition, String code) {
        if (!condition) throw new AssertionError("R904_" + code);
    }
}
