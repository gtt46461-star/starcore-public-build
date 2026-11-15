package com.ethk3.novaonepro;

/** Finite build-reviewed numeric replay compatibility; NOT evidence or certificate migration.
 * Old runs retain their exact immutable identity, label roles and model provenance.
 * Neither a new child nor a new frozen evaluation inherits the parent's future samples.
 */
public final class TrainingContinuationR973 {
    private static final String[] PREDECESSORS = reviewedRules();
    private TrainingContinuationR973() {}
    private static String[] reviewedRules() {
        String csv=ResearchSemanticsGeneratedR967.COMPATIBLE_PREDECESSOR_RULES_SHA256;
        String[] rules=csv.isEmpty()?new String[0]:csv.split(",");
        // R81 retains sixteen exact reviewed R65-R80 development rules.
        // This is still a finite exact hash allowlist, never transitive trust.
        if(rules.length>16)throw new IllegalStateException("R976_TOO_MANY_PREDECESSORS");
        java.util.HashSet<String> unique=new java.util.HashSet<String>();
        for(String rule:rules)if(!rule.matches("[0-9a-f]{64}")||!unique.add(rule))
            throw new IllegalStateException("R976_INVALID_GENERATED_PREDECESSOR");
        return rules;
    }
    public static boolean compatible(String rule) {
        if(rule==null)return false;
        if(rule.equals(TrainingCodeIdentityR964.SHA256))return true;
        for(String prior:PREDECESSORS)if(rule.equals(prior))return true;
        return false;
    }
    /** Identical admission domain for enrollment, pending checks and actual predraw selection. */
    public static String predicate(String column) {
        if(column==null||!column.matches("[a-zA-Z_][a-zA-Z0-9_]*\\.[a-zA-Z_][a-zA-Z0-9_]*"))
            throw new IllegalArgumentException("R973_SQL_IDENTIFIER_REQUIRED");
        StringBuilder sql=new StringBuilder("(").append(column).append("='").append(TrainingCodeIdentityR964.SHA256).append("'");
        for(String prior:PREDECESSORS)sql.append(" OR ").append(column).append("='").append(prior).append("'");
        return sql.append(')').toString();
    }
    public static String scope() {
        return "EXACT_SOURCE_REVIEW_NUMERIC_CONTINUATION_ONLY|certificateMigration=0|"
                + "futureSamplesInherited=0|proof=" + ResearchSemanticsGeneratedR967.CONTINUATION_PROOF_SHA256;
    }
}
