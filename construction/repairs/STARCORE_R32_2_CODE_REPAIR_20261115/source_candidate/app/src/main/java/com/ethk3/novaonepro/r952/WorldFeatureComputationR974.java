package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.Models;
import com.ethk3.novaonepro.V51ChecksumEngine;
import java.util.*;
import com.ethk3.novaonepro.TypedAtomMoleculeGraphR930;
import com.ethk3.novaonepro.TypedAtomMoleculeGraphR930.*;
import com.ethk3.novaonepro.r952.CandidateCompilerR952.Program;
import com.ethk3.novaonepro.r952.FrozenGraphContextCP4.ExpertPrediction;

/** Pure PIT feature graph: observed atoms -> temporal matrices -> normalized molecules.
 * Lagged conditional associations are not intervention-based causal-effect estimates.
 * The exact computed vector is consumed by the persisted neural model, not just displayed.
 */
public final class WorldFeatureComputationR974 {
    public static final String REVISION="R974_OBSERVED_ATOMS_TEMPORAL_MATRICES_V1";
    private WorldFeatureComputationR974(){}
    public static final class Result {
        private final double[] values;
        public final String inputHash,outputHash,trace;
        Result(double[] vector,String input,String trace){
            values=vector.clone();inputHash=input;this.trace=trace;
            outputHash=V51ChecksumEngine.sha256(REVISION+'|'+input+'|'+CandidateCompilerR952.encode(values)+'|'+trace);
        }
        public double[] values(){return values.clone();}
    }
    public static Result compute(CandidateAstR952 ast,List<Models.Draw> newest){
        if(ast==null||newest==null||newest.size()<20)throw new IllegalArgumentException("R974_WORLD_HISTORY_SUPPORT");
        if(ast.operator==CandidateAstR952.Operator.FUSION){
            Result a=compute(ast.left,newest),b=compute(ast.right,newest);double[] x=a.values(),y=b.values();
            double w=ast.weightPercent/100.0;for(int i=0;i<10;i++)x[i]=w*x[i]+(1-w)*y[i];
            return new Result(x,V51ChecksumEngine.sha256(a.inputHash+'|'+b.inputHash),
                    "FUSION|ast="+ast.structuralHash+"|left="+a.outputHash+"|right="+b.outputHash+"|weight="+ast.weightPercent+"|L{"+a.trace+"}|R{"+b.trace+"}");
        }
        int used=Math.min(ast.lookback,newest.size()),source=ast.position%10+1;
        double[] frequency=new double[10],recency=new double[10],markov=new double[100],cross=new double[100];
        java.util.Arrays.fill(frequency,1);java.util.Arrays.fill(recency,1);
        java.util.Arrays.fill(markov,.5);java.util.Arrays.fill(cross,.5);
        StringBuilder input=new StringBuilder(REVISION).append('|').append(ast.canonical);String previous="";int lagPairs=0,skippedGaps=0;
        for(int i=0;i<used;i++){
            Models.Draw d=newest.get(i);
            if(d==null||!d.isValid()||d.dirty!=0||i>0&&d.issue.compareTo(previous)>=0)
                throw new IllegalArgumentException("R958_HISTORY_NOT_NEWEST_FIRST");
            previous=d.issue;int n=d.getNumberAtPosition(ast.position)-1;
            frequency[n]++;recency[n]+=1.0/(1+i/20.0);
            input.append('|').append(d.issue).append(':');for(int v:d.numbers)input.append(v).append(',');
            if(i>0){
                // Older observation is the source, the immediately newer observation the label.
                Models.Draw newer=newest.get(i-1);
                if(!com.ethk3.novaonepro.TargetIssueResolver.exact(d.issue,newer.issue).valid){skippedGaps++;continue;}
                lagPairs++;
                int y=newer.getNumberAtPosition(ast.position)-1;
                markov[n*10+y]++;cross[(d.getNumberAtPosition(source)-1)*10+y]++;
            }
        }
        normalize(frequency);normalize(recency);
        double[] transition=row(markov,newest.get(0).getNumberAtPosition(ast.position)-1);
        double[] lag=row(cross,newest.get(0).getNumberAtPosition(source)-1),output;
        switch(ast.operator){
            case POSITION_FREQUENCY:output=frequency;break;
            case RECENCY_BLEND:output=recency;break;
            case MARKOV_TRANSITION:output=transition;break;
            case CROSS_POSITION_LAG:output=lag;break;
            case MOLECULAR_BLEND:
                output=new double[10];for(int i=0;i<10;i++)output[i]=(frequency[i]+recency[i]+transition[i]+lag[i])*.25;break;
            default:throw new IllegalArgumentException("R974_WORLD_OPERATOR");
        }
        String trace="operator="+ast.operator+"|position="+ast.position+"|sourcePosition="+source+"|observations="+used+
                "|lagPairs="+lagPairs+"|skippedGaps="+skippedGaps+"|frequency="+CandidateCompilerR952.encode(frequency)+"|recency="+CandidateCompilerR952.encode(recency)+
                "|transitionCounts="+CandidateCompilerR952.encode(markov)+"|crossLagCounts="+CandidateCompilerR952.encode(cross)+
                "|causalStatus=LAGGED_ASSOCIATION_NOT_CAUSAL_PROOF";
        return new Result(output,V51ChecksumEngine.sha256(input.toString()),trace);
    }
    /** Historical development rows consume raw graph values. They must not use a
     * prediction context whose model already knows validationLast, nor inference z-scores.
     * The caller has separately verified immutable historical Atom/teacher receipts. */
    public static DevelopmentTrainerCP9.Row rawTrainingRowCP11(Program program,
            DevelopmentInputCP9.Input input,int targetIndex,String protocolHash,
            Map<String,Value> frozenAtoms,List<ExpertPrediction> experts,String boundReceiptHash,
            MemoryBudgetCP4 budget) {
        if(program==null||program.ast.formatVersion!=3||input==null||frozenAtoms==null||experts==null
                ||budget==null||protocolHash==null||!protocolHash.matches("[0-9a-f]{64}")
                ||boundReceiptHash==null||!boundReceiptHash.matches("[0-9a-f]{64}")
                ||targetIndex<Math.max(20,program.manifest.requiredHistory)||targetIndex>=input.rowCount())
            throw new IllegalArgumentException("TRAIN_RAW_INPUT");
        String target=input.issue(targetIndex);long cutoff=input.openTime(targetIndex)-1;
        if(cutoff<=0)throw new IllegalArgumentException("TRAIN_RAW_CUTOFF");
        Set<String> keys=new HashSet<>();long bytes=4096;
        for(NodeSpec node:program.ast.graphSpec.typedGraph.orderedNodes){
            if(node.kind==NodeKind.ATOM){keys.add(node.qualifiedId());Value v=frozenAtoms.get(node.qualifiedId());
                if(v==null||!com.ethk3.novaonepro.TargetIssueResolver.isValid(v.maxSourceIssue)
                        ||v.maxSourceIssue.compareTo(target)>=0||v.availableAtMs>cutoff)
                    throw new IllegalArgumentException("TRAIN_RAW_ATOM_NOT_PIT");}
            bytes+=512L+32L*node.shape.elements();
        }
        if(!frozenAtoms.keySet().equals(keys))throw new IllegalArgumentException("TRAIN_RAW_ATOM_CLOSURE");
        try(MemoryBudgetCP4.Lease reserved=budget.acquire(bytes)){
            String source=V51ChecksumEngine.sha256("TRAIN_RAW_CP11|"+protocolHash+'|'+input.inputHash
                    +'|'+target+'|'+program.codeHash+'|'+boundReceiptHash);
            Evaluation ev=TypedAtomMoleculeGraphR930.evaluate(program.ast.graphSpec.typedGraph,
                    new FrozenInputs(target,cutoff,source,frozenAtoms));
            double[] raw;boolean[] present;
            switch(program.ast.graphSpec.modelKind){
            case FIXED_GRAPH:{
                if(!experts.isEmpty())throw new IllegalArgumentException("TRAIN_RAW_EXPERT_CLOSURE");
                Value v=ev.require("CP4:"+program.ast.graphSpec.outputId);raw=v.valuesCopy();present=v.maskCopy();
                if(raw.length!=10||v.presentCount()!=10)throw new IllegalArgumentException("TRAIN_RAW_FIXED_SHAPE");break;
            }
            case EXPERT_HEDGE:{
                if(experts.size()!=program.ast.graphSpec.experts.size())throw new IllegalArgumentException("TRAIN_RAW_EXPERT_CLOSURE");
                raw=new double[experts.size()*10];present=new boolean[raw.length];Arrays.fill(present,true);
                for(int i=0;i<experts.size();i++){
                    ExpertPrediction e=experts.get(i);
                    if(!e.matchesFrozenRef(program.ast.graphSpec.experts.get(i))||!e.target.equals(target)
                            ||e.maxSourceIssue.compareTo(target)>=0||e.availableAtMs>cutoff)
                        throw new IllegalArgumentException("TRAIN_RAW_TEACHER_NOT_PIT");
                    System.arraycopy(e.probabilities(),0,raw,i*10,10);
                    source=V51ChecksumEngine.sha256(source+'|'+e.sourceHash);
                }break;
            }
            default:{
                if(!experts.isEmpty())throw new IllegalArgumentException("TRAIN_RAW_EXPERT_CLOSURE");
                int width=program.ast.graphSpec.featureOutputIds.size();raw=new double[width];present=new boolean[width];
                for(int i=0;i<width;i++){
                    Value v=ev.require("CP4:"+program.ast.graphSpec.featureOutputIds.get(i));
                    if(v.size()!=1)throw new IllegalArgumentException("TRAIN_RAW_SCALAR_REQUIRED");
                    present[i]=v.present(0);raw[i]=present[i]?v.get(0):0;
                    if(!present[i]&&!"MASK".equals(program.manifest.missingPolicyPerFeature.get(i)))
                        throw new IllegalArgumentException("TRAIN_REQUIRED_FEATURE_MISSING");
                }break;
            }}
            String rowHash=V51ChecksumEngine.sha256(source+'|'+ev.evaluationHash);
            // The first access to the target label occurs after all raw feature evaluation.
            return new DevelopmentTrainerCP9.Row(target,rowHash,raw,present,input.actual(targetIndex,program.ast.position));
        }
    }
    private static double[] row(double[] matrix,int source){double[] out=new double[10];System.arraycopy(matrix,source*10,out,0,10);normalize(out);return out;}
    private static void normalize(double[] v){double total=0;for(double n:v)total+=n;for(int i=0;i<v.length;i++)v[i]/=total;}
public static Result compute(Program program, PITSnapshotContractR952 pit,
                             FrozenGraphContextCP4 ctx) {
    if (program == null || pit == null || ctx == null || program.ast.formatVersion != 3)
        throw new IllegalArgumentException("AST3_CONTEXT_REQUIRED");
    ctx.requireOpen();
    if (!pit.targetIssue.equals(ctx.target) || !pit.snapshotHash.equals(ctx.pitHash)
            || !program.manifest.hash.equals(ctx.manifest.hash))
        throw new IllegalStateException("FROZEN_CONTEXT_MISMATCH");
    if (pit.history.size() < program.manifest.requiredHistory)
        throw new IllegalStateException("INSUFFICIENT_HISTORY");
    pit.requireConsecutive();
    Set<String> keys = new HashSet<>();
    long bytes = 4096;
    for (NodeSpec node : program.ast.graphSpec.typedGraph.orderedNodes) {
        if (node.kind == NodeKind.ATOM) keys.add(node.qualifiedId());
        bytes += 512L + 32L * node.shape.elements();
    }
    if (!ctx.replayFrozenAtoms.keySet().equals(keys))
        throw new IllegalStateException("FROZEN_ATOM_CLOSURE");
    try (MemoryBudgetCP4.Lease reservation = ctx.atomMatrixBudget.acquire(bytes)) {
        FrozenInputs input = new FrozenInputs(pit.targetIssue, ctx.frozenAt,
                ctx.contextHash, ctx.replayFrozenAtoms);
        Evaluation evaluation = TypedAtomMoleculeGraphR930.evaluate(
                program.ast.graphSpec.typedGraph, input);
        double[] mean=new double[ctx.normalizer.size()],std=new double[mean.length];
        for(int i=0;i<mean.length;i++){mean[i]=ctx.normalizer.mean(i);std[i]=ctx.normalizer.std(i);}
        return frozenFeaturesCP12(program,evaluation,ctx.experts,mean,std,pit.targetIssue,pit.snapshotHash,ctx.contextHash);
    }
}
    /** One pure consumption implementation shared by live frozen inference and immutable
     * historical teacher replay. It reads no database, current Matrix or outcome label. */
    public static Result frozenFeaturesCP12(Program program,Evaluation evaluation,List<ExpertPrediction> experts,
            double[] mean,double[] std,String target,String pitHash,String contextHash){
        if(program==null||evaluation==null||experts==null||mean==null||std==null||mean.length!=std.length
                ||target==null||pitHash==null||contextHash==null
                ||!target.equals(evaluation.targetIssue)||!pitHash.matches("[0-9a-f]{64}")||!contextHash.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("FROZEN_FEATURE_REPLAY_IDENTITY");
        for(int i=0;i<mean.length;i++)
            if(!Double.isFinite(mean[i])||!Double.isFinite(std[i])||std[i]<=0)
                throw new IllegalArgumentException("FROZEN_NORMALIZER_NONFINITE_OR_ZERO");
        double[] consumed;
        switch (program.ast.graphSpec.modelKind) {
            case FIXED_GRAPH: {
                Value root = evaluation.require("CP4:" + program.ast.graphSpec.outputId);
                if (root.size() != 10 || root.presentCount() != 10)
                    throw new IllegalStateException("MISSING_FEATURE");
                consumed = root.valuesCopy();
                break;
            }
            case EXPERT_HEDGE: {
                if (experts.size() != program.ast.graphSpec.experts.size())
                    throw new IllegalStateException("EXPERT_CLOSURE");
                consumed = new double[10 * experts.size()];
                for (int i = 0; i < experts.size(); i++) {
                    ExpertPrediction expert = experts.get(i);
                    if (!expert.target.equals(target)
                            || expert.maxSourceIssue.compareTo(target) >= 0
                            || !expert.matchesFrozenRef(program.ast.graphSpec.experts.get(i)))
                        throw new IllegalStateException("EXPERT_MODEL_OR_PIT_MISMATCH");
                    double[] probabilities = expert.probabilities();
                    if (probabilities.length != 10) throw new IllegalStateException("EXPERT_DIMENSION");
                    System.arraycopy(probabilities, 0, consumed, i * 10, 10);
                }
                break;
            }
            default: {
                int f = program.ast.graphSpec.featureOutputIds.size();
                if (mean.length != f) throw new IllegalStateException("NORMALIZER_DIMENSION");
                consumed = new double[2 * f];int present = 0;
                for (int i = 0; i < f; i++) {
                    Value value = evaluation.require("CP4:" + program.ast.graphSpec.featureOutputIds.get(i));
                    if (value.size() != 1) throw new IllegalStateException("FEATURE_SCALAR_REQUIRED");
                    if (value.present(0)) {
                        consumed[i] = (value.get(0) - mean[i]) / std[i];
                        consumed[f+i] = 1;present++;
                    } else if (!"MASK".equals(program.manifest.missingPolicyPerFeature.get(i))) {
                        throw new IllegalStateException("MISSING_REQUIRED_FEATURE");
                    }
                }
                if (present == 0) throw new IllegalStateException("ALL_FEATURES_MISSING");
            }
        }
        if (consumed.length != program.ast.graphSpec.inputDimension)
            throw new IllegalStateException("MODEL_FEATURE_DIMENSION_MISMATCH");
        for (double value : consumed) if (!Double.isFinite(value))
            throw new IllegalStateException("NONFINITE_FEATURE");
        String inputHash = V51ChecksumEngine.sha256("CP7_WORLD|" + pitHash
                + '|' + program.codeHash + '|' + contextHash);
        // The complete Atom/Expert evidence is in the separately persisted context.
        // This trace is a bounded identity index; no optional chunk-store fallback exists.
        String trace = "CP7_WORLD|context=" + contextHash + "|program=" + program.codeHash
                + "|manifest=" + program.manifest.hash + "|evaluation=" + evaluation.evaluationHash;
        return new Result(consumed, inputHash, trace);
    }
}
