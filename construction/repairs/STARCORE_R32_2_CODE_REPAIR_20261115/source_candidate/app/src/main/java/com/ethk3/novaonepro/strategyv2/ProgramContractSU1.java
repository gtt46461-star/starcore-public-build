package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Static resource and semantic manifest for every executable grammar node. */
public final class ProgramContractSU1 {
    public static final long MAX_WORKING_BYTES = 64L << 20;
    public static final long MAX_OPERATIONS = 100000000;
    public final long workingBytes, operations, stateBytes, operationUnits;
    public final int historyWindow, nodes;
    public final String manifest, hash;

    private ProgramContractSU1(Analysis a) {
        workingBytes = a.bytes + (24L << 20) + 2048L * 864 + 5000L * 64;
        operations = a.ops; historyWindow = Math.min(5000, a.history); nodes = a.nodes;
        stateBytes = a.state;
        operationUnits = Math.max(1, (operations + 99) / 100);
        if (workingBytes > MAX_WORKING_BYTES || operations > MAX_OPERATIONS || stateBytes > 1048576 || a.relations.size() > 64)
            throw new IllegalArgumentException("SU1_COMPILED_RESOURCE_BUDGET");
        manifest = "SU1_PROGRAM_CONTRACT_V1|dtype=FLOAT32_NODE_FLOAT64_HEAD|axes=POSITION,NUMBER|output=SCORE"
                + "|normalizer=DECLARED_PER_LEAF|action=FIXED_K3_V2|workingBytes=" + workingBytes
                + "|operations=" + operations + "|costUnit=100_SCALAR_OPS|stateBytes=" + stateBytes + "|history=" + historyWindow + "|nodes=" + nodes
                + "|relations=" + a.relations.size() + "\n" + a.trace;
        hash = HashingSU1.text(manifest);
    }

    public static ProgramContractSU1 compile(ProgramTextSU1.Term term) {
        Analysis a = new Analysis(); analyze(term, "0", a); return new ProgramContractSU1(a);
    }
    private static int number(ProgramTextSU1.Term t, int i) { return Integer.parseInt(t.args.get(i).toString()); }
    private static final class Analysis {
        long bytes, ops, state;
        int history, nodes;
        final Set<String> relations = new HashSet<>();
        final StringBuilder trace = new StringBuilder();
    }
    private static void analyze(ProgramTextSU1.Term t, String path, Analysis a) {
        a.nodes++; a.bytes += 864; a.ops += 100;
        RelationTypeSU1.Knowledge knowledge = RelationTypeSU1.Knowledge.RESEARCH_HYPOTHESIS;
        String source = "TRANSFORMED_TYPED_CHILDREN";
        switch (t.op) {
            case "NUMIS": source = "FIXED_NUMBER_CATEGORY"; break;
            case "REL":
                knowledge = RelationTypeSU1.Knowledge.RESEARCH_HYPOTHESIS;
                source = "OBSERVED_NUMBER_TRANSFORM_NOT_CAUSAL_LAW";
                a.history = Math.max(a.history, number(t, 1)); a.relations.add(t.toString()); break;
            case "GT": case "ODD": case "BIG":
                knowledge = RelationTypeSU1.Knowledge.OBSERVED_FACT; source = "OBSERVED_LAG_COMPARISON";
                a.history = Math.max(a.history, number(t, 1));
                if (t.op.equals("GT")) a.history = Math.max(a.history, number(t, 3)); break;
            case "EXPERT":
                knowledge = RelationTypeSU1.Knowledge.STATISTICAL_ESTIMATE; source = "CAUSAL_HISTORY_ESTIMATOR";
                a.history = Math.max(a.history, number(t, 0) == 5 ? 5000 : 720); a.ops += 50000; break;
            case "XT": case "XI":
                knowledge = RelationTypeSU1.Knowledge.STATISTICAL_ESTIMATE; source = "ISSUE_CLUSTERED_CONDITIONAL_COUNTS";
                a.history = Math.max(a.history, 1001); a.relations.add(t.toString());
                a.bytes += 65536; a.state += 16384; a.ops += 20000; break;
            case "HEDGE":
                knowledge = RelationTypeSU1.Knowledge.STATISTICAL_ESTIMATE; source = "SETTLED_PREFIX_ONLINE_EXPERT_UPDATE";
                a.history = Math.max(a.history, number(t, 0) + 720); a.ops += number(t, 0) * 2000L; break;
            case "DEEP": {
                knowledge = RelationTypeSU1.Knowledge.STATISTICAL_ESTIMATE; source = "CAUSAL_RESET_BPTT_MODEL";
                int d = number(t, 0), w = number(t, 1);
                // Parameters, gradients, eight cached models, per-layer BPTT activations and adjoints.
                a.bytes += 8L * (12L * (3L*d*d + 31L*d + 110) + (w+1L)*10*d*8);
                a.state += 10L*d*8;
                a.ops += 32L * w * (40L*d*d + 200L*d);
                a.history = Math.max(a.history, 50 + 32 + w); break;
            }
            case "MODEL":
                knowledge = RelationTypeSU1.Knowledge.STATISTICAL_ESTIMATE; source = "HASH_BOUND_FROZEN_RELSTATE10_MANIFEST";
                a.bytes += 1048576; a.state += 1280; a.ops += number(t,1)*150000L;
                a.history = Math.max(a.history, number(t,1)); break;
            case "GDEEP": {
                knowledge = RelationTypeSU1.Knowledge.STATISTICAL_ESTIMATE; source = "TYPED_RELATION_FEATURE_HYPEREDGE_ADAM_BPTT_RELGRAPH10_V1";
                int d=number(t,0),w=number(t,1);a.bytes+=8L*w*(730L*d+700)+1048576;a.state+=65536;a.ops+=8L*w*(2400L*d*d+2000L*d);a.history=Math.max(a.history,1100);break;
            }
            case "FUSE":
                knowledge = RelationTypeSU1.Knowledge.STATISTICAL_ESTIMATE; source = "JOINT_CAUSAL_GATE_AND_TEMPERATURE_FIT_FROZEN_PARENTS";
                a.bytes += 4096; a.state += 320; a.ops += 500000; a.history = Math.max(a.history,170); break;
            case "GMODEL":
                knowledge=RelationTypeSU1.Knowledge.STATISTICAL_ESTIMATE;source="FROZEN_RELGRAPH18_GROUP_ADAM_MANIFEST";a.bytes+=8L*number(t,1)*(730L*16+700)+1048576;a.state+=262144;a.ops+=number(t,1)*500000L;a.history=Math.max(a.history,1100);break;
            case "ATOM": case "ATOMC":
                knowledge = RelationTypeSU1.Knowledge.STATISTICAL_ESTIMATE; source = "LIFTED_CP4_EXPLICIT_REGISTRY_PARAMETERS";
                a.history = 5000; a.ops += 50000; a.relations.add(t.toString()); break;
            case "HOT": case "HOTT":
                knowledge = RelationTypeSU1.Knowledge.STATISTICAL_ESTIMATE; source = "SETTLED_COMMON_POSITION_HIT_SHARE";
                a.history = Math.max(a.history, t.op.equals("HOT") ? 750 : number(t, 0) + 720); break;
            default: break;
        }
        a.trace.append(path).append('|').append(t.op).append('|').append(t.condition() ? "POSITION_BOOL" : "POSITION_NUMBER_SCORE")
                .append('|').append(knowledge).append('|').append(source).append("|direction=PAST_TO_TARGET|lagDomain=1..5000\n");
        long before = a.ops;
        for (int i = 0; i < t.args.size(); i++) if (t.args.get(i) instanceof ProgramTextSU1.Term)
            analyze((ProgramTextSU1.Term)t.args.get(i), path + "." + i, a);
        if (t.op.equals("HOTT")) a.ops += (a.ops - before) * (number(t, 0) * 10L - 1);
    }
}
