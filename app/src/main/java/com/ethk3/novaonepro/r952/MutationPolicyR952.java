package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.V51ChecksumEngine;

/** Retired AST1 repair entry points. CP12 MutationGraph owns actual graph changes;
 * Proposal remains a historical serialization DTO and grants no execution rights. */
public final class MutationPolicyR952 {
    private MutationPolicyR952() {}
    public static Proposal propose(String parent,CandidateAstR952 ast,String failureHash,int recentMisses,int earlierMisses) {
        throw new IllegalStateException("RETIRED_CP12_MUTATION_GRAPH_OWNER");
    }
    /** Search at most 240 explicit hypotheses; an ancestor cannot stall every later generation. */
    public static Proposal proposeNovel(String parent,CandidateAstR952 ast,String failureHash,
            int recentMisses,int earlierMisses,java.util.Set<String> existing) {
        throw new IllegalStateException("RETIRED_CP12_MUTATION_GRAPH_OWNER");
    }
    /** Unmeasured legacy calls cannot manufacture a repair evidence claim. */
    public static Proposal propose(String parent,CandidateAstR952 ast,String failureHash) {
        throw new IllegalStateException("RETIRED_CP12_MUTATION_GRAPH_OWNER");
    }
    public static final class Proposal {
        public final String parentId,repairTarget,evidenceHash,changedModules,expectedEffect,falsification,proposalHash;
        public final CandidateAstR952 ast;
        Proposal(String parent,CandidateAstR952 ast,String target,String evidence,String changed,String expected,String falsification) {
            this.parentId=parent;this.ast=ast;repairTarget=target;evidenceHash=evidence;changedModules=changed;expectedEffect=expected;this.falsification=falsification;
            proposalHash=V51ChecksumEngine.sha256("R958_PARAMETER_REPAIR|"+parent+'|'+ast.canonical+'|'+target+'|'+evidence+'|'+changed+'|'+expected+'|'+falsification);
        }
    }
}
