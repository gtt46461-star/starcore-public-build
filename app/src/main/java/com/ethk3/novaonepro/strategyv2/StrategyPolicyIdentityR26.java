package com.ethk3.novaonepro.strategyv2;
import com.ethk3.novaonepro.ResearchSemanticsGeneratedR967;
import java.util.*;
/** Closed identities for composite and fitted strategies; the exact fitted bundle is part of graph/initial state. */
public final class StrategyPolicyIdentityR26 {
    private StrategyPolicyIdentityR26() {}
    public static PolicyIdentityR26.Policy policy(String graph,Object initial,String abi){Map<String,Object> rules=new HashMap<>();rules.put("graph",graph);rules.put("features","R26_PAST_ONLY_LEGAL_MARGINALS_V1");rules.put("updater","COMPLETE_PRIVATE_LEGACY_STATE_V3");rules.put("update_schedule","ORDERED_REVEALED_LABELS_EXACTLY_ONCE");rules.put("initial_state",initial);rules.put("rng","PER_MEMBER_SEEDED_AND_CHECKPOINTED");rules.put("gate","SPECIFIED_IN_GRAPH; CALENDAR_COMPLETE");rules.put("router","SPECIFIED_IN_GRAPH");rules.put("calibration","ONLINE_TEMPERATURE_THEN_HELD_LATER_SELECTED_ACT_CALIBRATION");rules.put("missing_policy","MASK_CONTEXT_GAP; NO_BACKFILLED_LIVE_RECORDS");rules.put("resource_schedule","R26_CAPABILITY_WEIGHTED_CPU_SERVICE_200MS_POLL20MS_V2");rules.put("evidence_protocol",QualificationWindowR26.PROTOCOL);rules.put("runtime_abi",abi);return new PolicyIdentityR26.Policy(rules);}
    public static String hash(String graph,Object initial,String abi){return policy(graph,initial,abi).hash;}
    public static String current(String graph,Object initial){return hash(graph,initial,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256);}
}
