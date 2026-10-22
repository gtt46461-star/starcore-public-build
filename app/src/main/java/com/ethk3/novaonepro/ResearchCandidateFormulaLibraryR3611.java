package com.ethk3.novaonepro;

/** R3.6.11 planned Research seed library. Every DSL string was parsed by the 813 parser.
 *  RESEARCH ONLY. None of these formulas is claimed to be effective before strict OOS. */
public final class ResearchCandidateFormulaLibraryR3611 {
    private ResearchCandidateFormulaLibraryR3611() {}
    public static final String[][] SEEDS = new String[][] {
        new String[] {"F001", "RANK_BASE", "SIGMOID(FEATURE(rank_score))"},
        new String[] {"F002", "RANK_STABILITY", "SIGMOID(ADD(FEATURE(rank_score),MUL(CONST(0.5),FEATURE(stability))))"},
        new String[] {"F003", "RANK_THIRD", "SIGMOID(ADD(FEATURE(rank_score),MUL(CONST(0.45),FEATURE(third_margin))))"},
        new String[] {"F004", "RANK_ENTROPY_PENALTY", "SIGMOID(SUB(FEATURE(rank_score),MUL(CONST(0.35),FEATURE(entropy))))"},
        new String[] {"F005", "RAW_BASE", "CLIP(FEATURE(raw_action_p),CONST(0),CONST(1))"},
        new String[] {"F006", "RAW_THIRD", "CLIP(ADD(FEATURE(raw_action_p),MUL(CONST(0.25),FEATURE(third_margin))),CONST(0),CONST(1))"},
        new String[] {"F007", "RAW_STABILITY", "CLIP(ADD(FEATURE(raw_action_p),MUL(CONST(0.20),FEATURE(stability))),CONST(0),CONST(1))"},
        new String[] {"F008", "RAW_ENTROPY_PENALTY", "CLIP(SUB(FEATURE(raw_action_p),MUL(CONST(0.20),FEATURE(entropy))),CONST(0),CONST(1))"},
        new String[] {"F009", "WINDOW_ASSIGNMENT", "SIGMOID(ADD(FEATURE(window_consensus),FEATURE(assignment_support)))"},
        new String[] {"F010", "WINDOW_ASSIGNMENT_STABILITY", "SIGMOID(ADD(ADD(FEATURE(window_consensus),FEATURE(assignment_support)),MUL(CONST(0.4),FEATURE(stability))))"},
        new String[] {"F011", "RANK_WINDOW", "SIGMOID(ADD(FEATURE(rank_score),MUL(CONST(0.4),FEATURE(window_consensus))))"},
        new String[] {"F012", "RANK_ASSIGNMENT", "SIGMOID(ADD(FEATURE(rank_score),MUL(CONST(0.4),FEATURE(assignment_support))))"},
        new String[] {"F013", "RANK_WINDOW_ASSIGNMENT", "SIGMOID(ADD(FEATURE(rank_score),MUL(CONST(0.35),ADD(FEATURE(window_consensus),FEATURE(assignment_support)))))"},
        new String[] {"F014", "RAW_WINDOW_ASSIGNMENT", "CLIP(ADD(FEATURE(raw_action_p),MUL(CONST(0.20),ADD(FEATURE(window_consensus),FEATURE(assignment_support)))),CONST(0),CONST(1))"},
        new String[] {"F015", "RANK_STABLE_LOWENT", "SIGMOID(ADD(SUB(FEATURE(rank_score),MUL(CONST(0.25),FEATURE(entropy))),MUL(CONST(0.35),FEATURE(stability))))"},
        new String[] {"F016", "RAW_STABLE_LOWENT", "CLIP(ADD(SUB(FEATURE(raw_action_p),MUL(CONST(0.15),FEATURE(entropy))),MUL(CONST(0.20),FEATURE(stability))),CONST(0),CONST(1))"},
        new String[] {"F017", "THIRD_STABLE", "SIGMOID(ADD(FEATURE(third_margin),MUL(CONST(0.6),FEATURE(stability))))"},
        new String[] {"F018", "THIRD_LOWENT", "SIGMOID(SUB(FEATURE(third_margin),MUL(CONST(0.45),FEATURE(entropy))))"},
        new String[] {"F019", "INTERACT_RANK_STABLE", "SIGMOID(ADD(FEATURE(rank_score),INTERACTION(FEATURE(rank_score),FEATURE(stability))))"},
        new String[] {"F020", "INTERACT_RAW_STABLE", "CLIP(ADD(FEATURE(raw_action_p),MUL(CONST(0.3),INTERACTION(FEATURE(raw_action_p),FEATURE(stability)))),CONST(0),CONST(1))"},
        new String[] {"F021", "INTERACT_WINDOW_ASSIGN", "SIGMOID(ADD(FEATURE(rank_score),INTERACTION(FEATURE(window_consensus),FEATURE(assignment_support))))"},
        new String[] {"F022", "INTERACT_THIRD_STABLE", "SIGMOID(ADD(FEATURE(rank_score),INTERACTION(FEATURE(third_margin),FEATURE(stability))))"},
        new String[] {"F023", "ENTROPY_GATED_RANK", "SIGMOID(MUL(FEATURE(rank_score),ONE_MINUS(CLIP(FEATURE(entropy),CONST(0),CONST(1)))))"},
        new String[] {"F024", "ENTROPY_GATED_RAW", "CLIP(MUL(FEATURE(raw_action_p),ONE_MINUS(CLIP(FEATURE(entropy),CONST(0),CONST(1)))),CONST(0),CONST(1))"},
        new String[] {"F025", "STABILITY_GATED_RANK", "SIGMOID(MUL(FEATURE(rank_score),ADD(CONST(0.5),FEATURE(stability))))"},
        new String[] {"F026", "STABILITY_GATED_RAW", "CLIP(MUL(FEATURE(raw_action_p),ADD(CONST(0.75),MUL(CONST(0.25),FEATURE(stability)))),CONST(0),CONST(1))"},
        new String[] {"F027", "RATIO_THIRD_ENTROPY", "SIGMOID(ADD(FEATURE(rank_score),MUL(CONST(0.2),SAFE_DIV(FEATURE(third_margin),ADD(CONST(0.1),FEATURE(entropy)),CONST(0.001)))))"},
        new String[] {"F028", "RATIO_STABLE_ENTROPY", "SIGMOID(ADD(FEATURE(rank_score),MUL(CONST(0.2),SAFE_DIV(FEATURE(stability),ADD(CONST(0.1),FEATURE(entropy)),CONST(0.001)))))"},
        new String[] {"F029", "MIN_WINDOW_ASSIGN", "SIGMOID(ADD(FEATURE(rank_score),MUL(CONST(0.5),MIN(FEATURE(window_consensus),FEATURE(assignment_support)))))"},
        new String[] {"F030", "MAX_WINDOW_ASSIGN", "SIGMOID(ADD(FEATURE(rank_score),MUL(CONST(0.25),MAX(FEATURE(window_consensus),FEATURE(assignment_support)))))"},
        new String[] {"F031", "CONSENSUS_PLUS_THIRD", "SIGMOID(ADD(FEATURE(window_consensus),MUL(CONST(0.5),FEATURE(third_margin))))"},
        new String[] {"F032", "ASSIGN_PLUS_THIRD", "SIGMOID(ADD(FEATURE(assignment_support),MUL(CONST(0.5),FEATURE(third_margin))))"},
        new String[] {"F033", "RAW_RANK_BLEND", "CLIP(ADD(MUL(CONST(0.6),FEATURE(raw_action_p)),MUL(CONST(0.4),SIGMOID(FEATURE(rank_score)))),CONST(0),CONST(1))"},
        new String[] {"F034", "RAW_RANK_STABILITY_BLEND", "CLIP(ADD(ADD(MUL(CONST(0.5),FEATURE(raw_action_p)),MUL(CONST(0.3),SIGMOID(FEATURE(rank_score)))),MUL(CONST(0.2),FEATURE(stability))),CONST(0),CONST(1))"},
        new String[] {"F035", "RAW_RANK_THIRD_BLEND", "CLIP(ADD(ADD(MUL(CONST(0.55),FEATURE(raw_action_p)),MUL(CONST(0.30),SIGMOID(FEATURE(rank_score)))),MUL(CONST(0.15),FEATURE(third_margin))),CONST(0),CONST(1))"},
        new String[] {"F036", "RAW_RANK_WINDOW_BLEND", "CLIP(ADD(ADD(MUL(CONST(0.55),FEATURE(raw_action_p)),MUL(CONST(0.25),SIGMOID(FEATURE(rank_score)))),MUL(CONST(0.20),FEATURE(window_consensus))),CONST(0),CONST(1))"},
        new String[] {"F037", "RAW_RANK_ASSIGN_BLEND", "CLIP(ADD(ADD(MUL(CONST(0.55),FEATURE(raw_action_p)),MUL(CONST(0.25),SIGMOID(FEATURE(rank_score)))),MUL(CONST(0.20),FEATURE(assignment_support))),CONST(0),CONST(1))"},
        new String[] {"F038", "RAW_RANK_LOWENT_BLEND", "CLIP(SUB(ADD(MUL(CONST(0.6),FEATURE(raw_action_p)),MUL(CONST(0.4),SIGMOID(FEATURE(rank_score)))),MUL(CONST(0.15),FEATURE(entropy))),CONST(0),CONST(1))"},
        new String[] {"F039", "FOUR_SIGNAL", "SIGMOID(ADD(ADD(FEATURE(rank_score),MUL(CONST(0.35),FEATURE(third_margin))),MUL(CONST(0.25),ADD(FEATURE(window_consensus),FEATURE(assignment_support)))))"},
        new String[] {"F040", "FOUR_SIGNAL_LOWENT", "SIGMOID(SUB(ADD(ADD(FEATURE(rank_score),MUL(CONST(0.35),FEATURE(third_margin))),MUL(CONST(0.25),ADD(FEATURE(window_consensus),FEATURE(assignment_support)))),MUL(CONST(0.25),FEATURE(entropy))))"},
        new String[] {"F041", "FOUR_SIGNAL_STABLE", "SIGMOID(ADD(ADD(ADD(FEATURE(rank_score),MUL(CONST(0.30),FEATURE(third_margin))),MUL(CONST(0.20),ADD(FEATURE(window_consensus),FEATURE(assignment_support)))),MUL(CONST(0.30),FEATURE(stability))))"},
        new String[] {"F042", "RISK_BALANCED", "SIGMOID(SUB(ADD(FEATURE(rank_score),MUL(CONST(0.3),ADD(FEATURE(third_margin),FEATURE(stability)))),MUL(CONST(0.35),FEATURE(entropy))))"},
        new String[] {"F043", "CONSENSUS_PRODUCT", "SIGMOID(ADD(FEATURE(rank_score),MUL(CONST(0.6),INTERACTION(FEATURE(window_consensus),FEATURE(assignment_support)))))"},
        new String[] {"F044", "THIRD_CONSENSUS_PRODUCT", "SIGMOID(ADD(FEATURE(rank_score),MUL(CONST(0.5),INTERACTION(FEATURE(third_margin),MIN(FEATURE(window_consensus),FEATURE(assignment_support))))))"},
        new String[] {"F045", "STABILITY_CONSENSUS_PRODUCT", "SIGMOID(ADD(FEATURE(rank_score),MUL(CONST(0.5),INTERACTION(FEATURE(stability),MIN(FEATURE(window_consensus),FEATURE(assignment_support))))))"},
        new String[] {"F046", "RAW_CONSENSUS_PRODUCT", "CLIP(ADD(FEATURE(raw_action_p),MUL(CONST(0.25),INTERACTION(FEATURE(window_consensus),FEATURE(assignment_support)))),CONST(0),CONST(1))"},
        new String[] {"F047", "RAW_THIRD_STABLE", "CLIP(ADD(FEATURE(raw_action_p),MUL(CONST(0.20),INTERACTION(FEATURE(third_margin),FEATURE(stability)))),CONST(0),CONST(1))"},
        new String[] {"F048", "RAW_ALL_RISK", "CLIP(SUB(ADD(ADD(FEATURE(raw_action_p),MUL(CONST(0.15),FEATURE(third_margin))),MUL(CONST(0.15),ADD(FEATURE(window_consensus),FEATURE(assignment_support)))),MUL(CONST(0.20),FEATURE(entropy))),CONST(0),CONST(1))"},
        new String[] {"F049", "LOG_RANK", "SIGMOID(LOG1P_POS(ADD(CONST(1),ABS(FEATURE(rank_score)))))"},
        new String[] {"F050", "SQRT_THIRD", "SIGMOID(ADD(FEATURE(rank_score),MUL(CONST(0.35),SQRT_POS(ABS(FEATURE(third_margin))))))"},
        new String[] {"F051", "MIN_STABLE_CONSENSUS", "SIGMOID(ADD(FEATURE(rank_score),MUL(CONST(0.5),MIN(FEATURE(stability),FEATURE(window_consensus)))))"},
        new String[] {"F052", "MIN_STABLE_ASSIGN", "SIGMOID(ADD(FEATURE(rank_score),MUL(CONST(0.5),MIN(FEATURE(stability),FEATURE(assignment_support)))))"},
        new String[] {"F053", "TRIPLE_MIN", "SIGMOID(ADD(FEATURE(rank_score),MUL(CONST(0.6),MIN(FEATURE(stability),MIN(FEATURE(window_consensus),FEATURE(assignment_support))))))"},
        new String[] {"F054", "TRIPLE_AVG", "SIGMOID(ADD(FEATURE(rank_score),MUL(CONST(0.3333333333),ADD(FEATURE(stability),ADD(FEATURE(window_consensus),FEATURE(assignment_support))))))"},
        new String[] {"F055", "RAW_TRIPLE_AVG", "CLIP(ADD(FEATURE(raw_action_p),MUL(CONST(0.15),ADD(FEATURE(stability),ADD(FEATURE(window_consensus),FEATURE(assignment_support))))),CONST(0),CONST(1))"},
        new String[] {"F056", "LOWENT_TRIPLE_AVG", "SIGMOID(SUB(ADD(FEATURE(rank_score),MUL(CONST(0.3333333333),ADD(FEATURE(stability),ADD(FEATURE(window_consensus),FEATURE(assignment_support))))),MUL(CONST(0.3),FEATURE(entropy))))"},
        new String[] {"F057", "RAW_RANK_INTERACTION", "CLIP(ADD(FEATURE(raw_action_p),MUL(CONST(0.2),INTERACTION(FEATURE(raw_action_p),SIGMOID(FEATURE(rank_score))))),CONST(0),CONST(1))"},
        new String[] {"F058", "RANK_MARGIN_CONSENSUS", "SIGMOID(ADD(ADD(FEATURE(rank_score),MUL(CONST(0.4),FEATURE(third_margin))),MUL(CONST(0.3),MIN(FEATURE(window_consensus),FEATURE(assignment_support)))))"},
        new String[] {"F059", "RANK_MARGIN_RISK", "SIGMOID(SUB(ADD(FEATURE(rank_score),MUL(CONST(0.45),FEATURE(third_margin))),MUL(CONST(0.25),ADD(FEATURE(entropy),ONE_MINUS(CLIP(FEATURE(stability),CONST(0),CONST(1)))))))"},
        new String[] {"F060", "ENSEMBLE_BALANCED", "CLIP(ADD(MUL(CONST(0.5),FEATURE(raw_action_p)),MUL(CONST(0.5),SIGMOID(ADD(FEATURE(rank_score),MIN(FEATURE(window_consensus),FEATURE(assignment_support)))))),CONST(0),CONST(1))"},
    };

    public static void validateAll() {
        for (int i = 0; i < SEEDS.length; i++) {
            ResearchStrategyDslEngine.parse(SEEDS[i][2]);
        }
    }
}
