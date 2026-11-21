package com.ethk3.novaonepro;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Closed, versioned and bounded codec for all 304 fields reachable from V51RuntimeResult.
 * No Java serialization, arbitrary class lookup, field mutation, inference or authority creation.
 * Object handles preserve aliasing; arrays are bounded before allocation; cycles are rejected.
 * Raw IEEE-754 bits and nulls are preserved. The original canonical owner revalidates authority.
 */
public final class PreparedCanonicalCodecR335 {
    public static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final int MAX_ELEMENTS = 1_000_000, MAX_HANDLES = 32_768, MAX_DEPTH = 48;
    private static final int MAGIC = 0x52333335;
    public static final String SCHEMA_HASH = "2f2b7ee5d3de35cfbb58d03aaf8eab33ace7c6159b20ea49f43c928e73c4b638";
    private PreparedCanonicalCodecR335() {}
    static final class Decoded {
        private final Object[] values;
        private Decoded(Object[] values) { this.values = values; }
        Object value(int n) { return values[n]; }
    }
    private static final class Schema {
        final Class<?> type; final Field[] fields;
        Schema(Class<?> type, String[] names, Class<?>[] types) {
            this.type=type; fields=new Field[names.length];
            try {
                int n=0; for(Field f:type.getDeclaredFields()) if(!Modifier.isStatic(f.getModifiers())&&!f.isSynthetic())n++;
                if(n!=names.length||names.length!=types.length)throw new IllegalStateException("R335_CODEC_FIELD_COUNT:"+type.getName());
                for(int i=0;i<names.length;i++) {
                    fields[i]=type.getDeclaredField(names[i]);
                    if(fields[i].getType()!=types[i]||!Modifier.isFinal(fields[i].getModifiers())||!Modifier.isPublic(fields[i].getModifiers()))throw new IllegalStateException("R335_CODEC_FIELD_SCHEMA:"+names[i]);
                }
            } catch(ReflectiveOperationException error) { throw new IllegalStateException("R335_CODEC_SCHEMA",error); }
        }
    }
    private static final Schema[] SCHEMAS = new Schema[] {
        new Schema(CertifiedPredictionR974.Proof.class,new String[]{"candidate","structure","certificate","evidence","modelHash","base","target","bundle","history","recipe","actionHash","hash","capturedAt","deadlineAt","deadlineMono","calibrationHash","selectionHash","scopePolicyHash","selectedHitP"},new Class<?>[]{String.class,String.class,String.class,String.class,String.class,String.class,String.class,String.class,String.class,String.class,String.class,String.class,long.class,long.class,long.class,String.class,String.class,String.class,double.class}),
        new Schema(ControlledCandidateOpportunityAuditR922.Opportunity.class,new String[]{"baseIssue","targetIssue","runtimeVersion","gateCode","gateDetail","governanceLane","governanceCandidate","governanceStatus","authorizedAction","authorizedReject","recordHash","gateEligible","governanceApplied","controlledInfluenceEligible","position","n1","n2","n3","governanceWeight","rawActionP","thirdMargin","actionStability","entropy","assignmentSupport","falsifier","rankRetention","thirdRobustness","marginRobustness","createdAt"},new Class<?>[]{String.class,String.class,String.class,String.class,String.class,String.class,String.class,String.class,String.class,String.class,String.class,boolean.class,boolean.class,boolean.class,int.class,int.class,int.class,int.class,double.class,double.class,double.class,double.class,double.class,double.class,double.class,double.class,double.class,double.class,long.class}),
        new Schema(K3FunnelOrchestrator.Audit.class,new String[]{"retained","paths","formalActionCount","neverDeleteFormalAction","allPathsNested","matrixChecksum","checksum"},new Class<?>[]{int[][][].class,K3FunnelOrchestrator.PositionPath[].class,int.class,boolean.class,boolean.class,String.class,String.class}),
        new Schema(K3FunnelOrchestrator.PositionPath.class,new String[]{"position","sourceOrder","stages","finalThree","nested","trace","checksum"},new Class<?>[]{int.class,int[].class,K3FunnelOrchestrator.StageEvidence[].class,int[].class,boolean.class,String.class,String.class}),
        new Schema(K3FunnelOrchestrator.StageEvidence.class,new String[]{"position","stageIndex","retainedCount","retained","removedNumber","weakestRetainedP","strongestRemovedP","boundaryMargin","reason","checksum"},new Class<?>[]{int.class,int.class,int.class,int[].class,int.class,double.class,double.class,double.class,String.class,String.class}),
        new Schema(NumberSurfaceTensorEngine.TensorResult.class,new String[]{"tensor","exclusive","assignmentMargin","conflict","effectiveExperts","positionPredictability","expertJaccard","expertMutualInformation","checksum","diagnostics"},new Class<?>[]{double[][][][].class,double[][].class,double[][].class,double[][].class,double[].class,double[].class,double[][].class,double[][].class,String.class,String.class}),
        new Schema(ResearchMainlineContributionAuditR922.Contribution.class,new String[]{"targetIssue","runtimeVersion","lane","candidateId","candidateName","status","evidenceHash","rankOneHash","recordHash","applied","controlledInfluenceEligible","influenceWeight","hitRate","wilsonLower","brier","ece","coverage","futureN","pairedN","pairedDelta","fdrQ","createdAt"},new Class<?>[]{String.class,String.class,String.class,String.class,String.class,String.class,String.class,String.class,String.class,boolean.class,boolean.class,double.class,double.class,double.class,double.class,double.class,double.class,int.class,int.class,double.class,double.class,long.class}),
        new Schema(V51CompiledActionContract.class,new String[]{"selected","policy","stability","certifiedPrediction","featureHash","stateHash","expertHash","probabilityHash","actionHash","checksum"},new Class<?>[]{V51Models.SelectedActionEnvelope.class,V51SelectiveRiskController.Result.class,V51RankStabilityEngine.Result.class,CertifiedPredictionR974.Proof.class,String.class,String.class,String.class,String.class,String.class,String.class}),
        new Schema(V51EffectiveConsensusEngine.Consensus.class,new String[]{"distributionAgreement","top3Overlap","rankAgreement","directionalAgreement","signalStrength","historicalReliability","recentReliability","stateReliability","effective"},new Class<?>[]{double.class,double.class,double.class,double.class,double.class,double.class,double.class,double.class,double.class}),
        new Schema(V51ExpertGateEngine.Result.class,new String[]{"weight","health","status","artifactProven","effectiveExperts","fallbackUsed","fallbackExpertIndex","sumBeforeFallback","checksum"},new Class<?>[]{double[][].class,V51ExpertHealthRegistry.Snapshot[].class,V51ExpertGateEngine.GateStatus.class,boolean.class,double[].class,boolean[].class,int[].class,double[].class,String.class}),
        new Schema(V51ExpertHealthRegistry.Snapshot.class,new String[]{"expertId","state","sample","consecutiveBad","recoveryGood","reliability","recentReliability","calibrationError","rankRegret","weightMultiplier","reason","updatedAt"},new Class<?>[]{String.class,V51ExpertHealthRegistry.HealthState.class,int.class,int.class,int.class,double.class,double.class,double.class,double.class,double.class,String.class,long.class}),
        new Schema(V51Models.AuthorizedDecision.class,new String[]{"action","rejectCode","selected","decisionId","authorizationHash","artifactBundleHash"},new Class<?>[]{V51Enums.DecisionAction.class,V51Enums.RejectCode.class,V51Models.SelectedActionEnvelope.class,String.class,String.class,String.class}),
        new Schema(V51Models.DomainCertificate.class,new String[]{"domainId","checksum","status","sample","hitRate","credibleLow","ece","blockMinimum","driftRisk"},new Class<?>[]{String.class,String.class,V51Enums.DomainStatus.class,int.class,double.class,double.class,double.class,double.class,double.class}),
        new Schema(V51Models.ExpertEvidence.class,new String[]{"expertId","artifactId","checksum","rawScore","trust","uncertainty","signalStrength","evidence","counterEvidence"},new Class<?>[]{String.class,String.class,String.class,double[][].class,double[].class,double[].class,double[].class,String[].class,String[].class}),
        new Schema(V51Models.NumberProbabilityMatrix.class,new String[]{"probability","maxRowError","maxColumnError","iterations","converged","usedFallback","mapPermutation","checksum"},new Class<?>[]{double[][].class,double.class,double.class,int.class,boolean.class,boolean.class,int[].class,String.class}),
        new Schema(V51Models.PermutationFeatureSnapshot.class,new String[]{"context","globalFeatures","numberFeatures","checksum"},new Class<?>[]{V51Models.PredictionContext.class,double[].class,double[][][].class,String.class}),
        new Schema(V51Models.PositionAction.class,new String[]{"position","n1","n2","n3","n4","rank","rawActionP","rankScore","rankMargin","thirdMargin","entropy","stability","metaFeatures","fingerprint","checksum"},new Class<?>[]{int.class,int.class,int.class,int.class,int.class,int.class,double.class,double.class,double.class,double.class,double.class,double.class,double[].class,String.class,String.class}),
        new Schema(V51Models.PredictabilityEvidence.class,new String[]{"probability","stability","changePointRisk","falsifierRisk","qualified","checksum","missingReason","inputFeatures"},new Class<?>[]{double.class,double.class,double.class,double.class,boolean.class,String.class,String.class,double[].class}),
        new Schema(V51Models.PredictionContext.class,new String[]{"baseIssue","targetIssue","historyMaxIssue","historyHash","cohortId","artifactBundleHash","createdAt","historyRows"},new Class<?>[]{String.class,String.class,String.class,String.class,String.class,String.class,long.class,int.class}),
        new Schema(V51Models.SelectedActionEnvelope.class,new String[]{"context","action","selectedHitP","predictability","domain","lane","uncertainty","falsifierScore","contractHash","selectionPolicyHash"},new Class<?>[]{V51Models.PredictionContext.class,V51Models.PositionAction.class,double.class,V51Models.PredictabilityEvidence.class,V51Models.DomainCertificate.class,V51Enums.Lane.class,double.class,double.class,String.class,String.class}),
        new Schema(V51Models.StateTrajectorySnapshot.class,new String[]{"targetIssue","prototypeId","domainId","checksum","position","runLength","lifecycle","posterior","remainingLife","changePointRisk","confidence"},new Class<?>[]{String.class,String.class,String.class,String.class,int.class,int.class,V51Enums.Lifecycle.class,double.class,double.class,double.class,double.class}),
        new Schema(V51PermutationNumberModelEngine.GateTelemetryR811.class,new String[]{"expertIds","healthState","gateWeight","effectiveExperts","sumBeforeFallback","fallbackUsed","fallbackExpertIndex","gateStatus","diagnosticHash"},new Class<?>[]{String[].class,String[].class,double[][].class,double[].class,double[].class,boolean[].class,int[].class,String.class,String.class}),
        new Schema(V51PermutationNumberModelEngine.Result.class,new String[]{"experts","gateWeight","rawScore","matrix","gate","projection","tensor","status","artifactProven","telemetry","checksum"},new Class<?>[]{V51Models.ExpertEvidence[].class,double[][].class,double[][].class,V51Models.NumberProbabilityMatrix.class,V51ExpertGateEngine.Result.class,V51SinkhornEngine.ProjectionResult.class,NumberSurfaceTensorEngine.TensorResult.class,V51PermutationNumberModelEngine.ModelStatus.class,boolean.class,V51PermutationNumberModelEngine.GateTelemetryR811.class,String.class}),
        new Schema(V51PolicyCell.class,new String[]{"id","domainPrefix","lifecycle","position","sample","selectedPMin","predictabilityMin","rankMarginMin","thirdMarginMin","consensusMin","uncertaintyMax","falsifierMax","hitRate","credibleLow","ece","blockMinimum","maxDrift","active"},new Class<?>[]{String.class,String.class,V51Enums.Lifecycle.class,int.class,int.class,double.class,double.class,double.class,double.class,double.class,double.class,double.class,double.class,double.class,double.class,double.class,double.class,boolean.class}),
        new Schema(V51RankStabilityEngine.Result.class,new String[]{"retention","thirdRobustness","rankMarginRobustness","stable"},new Class<?>[]{double.class,double.class,double.class,boolean.class}),
        new Schema(V51RuntimeResult.class,new String[]{"success","context","features","states","numberModel","consensus","funnel","actions","selected","authorized","contract","opportunityAuditR922","contributionAuditR922","status","checksum","frozenConsumerRecipe","elapsedMs"},new Class<?>[]{boolean.class,V51Models.PredictionContext.class,V51Models.PermutationFeatureSnapshot.class,V51Models.StateTrajectorySnapshot[].class,V51PermutationNumberModelEngine.Result.class,V51EffectiveConsensusEngine.Consensus[].class,K3FunnelOrchestrator.Audit.class,V51Models.PositionAction[].class,V51Models.SelectedActionEnvelope.class,V51Models.AuthorizedDecision.class,V51CompiledActionContract.class,ControlledCandidateOpportunityAuditR922.Opportunity.class,ResearchMainlineContributionAuditR922.Contribution.class,String.class,String.class,String.class,long.class}),
        new Schema(V51SelectiveRiskController.Result.class,new String[]{"lane","rejectCode","cell","realEligible","proofReason"},new Class<?>[]{V51Enums.Lane.class,V51Enums.RejectCode.class,V51PolicyCell.class,boolean.class,String.class}),
        new Schema(V51SinkhornEngine.ProjectionResult.class,new String[]{"matrix","fallbackReason","maxLogKernel","minLogKernel","iterations","rowError","columnError","diagnostics"},new Class<?>[]{V51Models.NumberProbabilityMatrix.class,V51SinkhornEngine.FallbackReason.class,double.class,double.class,int.class,double.class,double.class,String.class}),
    };
    private static int schemaIndex(Class<?> type) throws IOException {
        for(int i=0;i<SCHEMAS.length;i++)if(SCHEMAS[i].type==type)return i;
        throw new IOException("R335_CODEC_TYPE:"+type.getName());
    }
    private static Object construct(int index,Object[] values) throws IOException {
        Decoded d = new Decoded(values);
        switch(index) {
            case 0: return new CertifiedPredictionR974.Proof(d);
            case 1: return new ControlledCandidateOpportunityAuditR922.Opportunity(d);
            case 2: return new K3FunnelOrchestrator.Audit(d);
            case 3: return new K3FunnelOrchestrator.PositionPath(d);
            case 4: return new K3FunnelOrchestrator.StageEvidence(d);
            case 5: return new NumberSurfaceTensorEngine.TensorResult(d);
            case 6: return new ResearchMainlineContributionAuditR922.Contribution(d);
            case 7: return new V51CompiledActionContract(d);
            case 8: return new V51EffectiveConsensusEngine.Consensus(d);
            case 9: return new V51ExpertGateEngine.Result(d);
            case 10: return new V51ExpertHealthRegistry.Snapshot(d);
            case 11: return new V51Models.AuthorizedDecision(d);
            case 12: return new V51Models.DomainCertificate(d);
            case 13: return new V51Models.ExpertEvidence(d);
            case 14: return new V51Models.NumberProbabilityMatrix(d);
            case 15: return new V51Models.PermutationFeatureSnapshot(d);
            case 16: return new V51Models.PositionAction(d);
            case 17: return new V51Models.PredictabilityEvidence(d);
            case 18: return new V51Models.PredictionContext(d);
            case 19: return new V51Models.SelectedActionEnvelope(d);
            case 20: return new V51Models.StateTrajectorySnapshot(d);
            case 21: return new V51PermutationNumberModelEngine.GateTelemetryR811(d);
            case 22: return new V51PermutationNumberModelEngine.Result(d);
            case 23: return new V51PolicyCell(d);
            case 24: return new V51RankStabilityEngine.Result(d);
            case 25: return new V51RuntimeResult(d);
            case 26: return new V51SelectiveRiskController.Result(d);
            case 27: return new V51SinkhornEngine.ProjectionResult(d);
            default: throw new IOException("R335_CODEC_CLASS_ID");
        }
    }
    private static final class Limits {
        int elements;
        void use(int count,int depth)throws IOException {
            if(count<0||depth>MAX_DEPTH||count>MAX_ELEMENTS-elements)throw new IOException("R335_CODEC_ALLOCATION_BOUND");
            elements+=count;
            if((elements&255)==0&&Thread.currentThread().isInterrupted())throw new InterruptedIOException("R335_CODEC_CANCELLED");
        }
    }
    private static final class BoundedOutput extends OutputStream {
        final ByteArrayOutputStream bytes=new ByteArrayOutputStream(16384);
        public void write(int b)throws IOException{check(1);bytes.write(b);}
        public void write(byte[] b,int off,int len)throws IOException{check(len);bytes.write(b,off,len);}
        void check(int len)throws IOException{if(len<0||len>MAX_BYTES-bytes.size())throw new IOException("R335_CODEC_BYTE_BOUND");}
    }
    public static byte[] encode(V51RuntimeResult result)throws IOException {
        if(result==null)throw new IOException("R335_CODEC_NULL_ROOT");
        BoundedOutput raw=new BoundedOutput();DataOutputStream out=new DataOutputStream(raw);
        out.writeInt(MAGIC);out.writeUTF(SCHEMA_HASH);
        write(out,result,V51RuntimeResult.class,new IdentityHashMap<>(),new IdentityHashMap<>(),new Limits(),0);
        out.flush();return raw.bytes.toByteArray();
    }
    public static V51RuntimeResult decode(byte[] bytes)throws IOException {
        if(bytes==null||bytes.length==0||bytes.length>MAX_BYTES)throw new IOException("R335_CODEC_BYTE_BOUND");
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));
        if(in.readInt()!=MAGIC||!SCHEMA_HASH.equals(in.readUTF()))throw new IOException("R335_CODEC_SCHEMA_VERSION");
        Object value=read(in,V51RuntimeResult.class,new ArrayList<>(),new Limits(),0);
        if(value==null||in.read()!=-1)throw new IOException("R335_CODEC_TRAILING_OR_NULL");
        return (V51RuntimeResult)value;
    }
    private static void write(DataOutputStream out,Object value,Class<?> type,IdentityHashMap<Object,Integer> handles,IdentityHashMap<Object,Boolean> active,Limits limits,int depth)throws IOException {
        limits.use(1,depth);
        if(type==boolean.class){out.writeBoolean((Boolean)value);return;}
        if(type==int.class){out.writeInt((Integer)value);return;}
        if(type==long.class){out.writeLong((Long)value);return;}
        if(type==double.class){out.writeLong(Double.doubleToRawLongBits((Double)value));return;}
        if(value==null){out.writeInt(-1);return;}
        if(value.getClass()!=type)throw new IOException("R335_CODEC_EXACT_TYPE");
        Integer old=handles.get(value);
        if(old!=null){if(active.containsKey(value))throw new IOException("R335_CODEC_CYCLE");out.writeInt(old);return;}
        if(handles.size()>=MAX_HANDLES)throw new IOException("R335_CODEC_HANDLE_BOUND");
        out.writeInt(-2);handles.put(value,handles.size());active.put(value,Boolean.TRUE);
        if(type==String.class) {
            String text=(String)value;if(text.length()>MAX_BYTES/2)throw new IOException("R335_CODEC_STRING_BOUND");
            byte[] utf=text.getBytes(StandardCharsets.UTF_8);if(!text.equals(new String(utf,StandardCharsets.UTF_8)))throw new IOException("R335_CODEC_UTF16");out.writeInt(utf.length);out.write(utf);
        } else if(type.isEnum())out.writeUTF(((Enum<?>)value).name());
        else if(type.isArray()) {
            int n=Array.getLength(value);if(n>MAX_ELEMENTS)throw new IOException("R335_CODEC_ARRAY_BOUND");out.writeInt(n);
            for(int i=0;i<n;i++)write(out,Array.get(value,i),type.getComponentType(),handles,active,limits,depth+1);
        } else {
            int index=schemaIndex(type);out.writeInt(index);Schema schema=SCHEMAS[index];
            try{for(Field field:schema.fields)write(out,field.get(value),field.getType(),handles,active,limits,depth+1);}
            catch(IllegalAccessException denied){throw new IOException("R335_CODEC_FIELD_ACCESS",denied);}
        }
        active.remove(value);
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    private static Object read(DataInputStream in,Class<?> type,ArrayList<Object> handles,Limits limits,int depth)throws IOException {
        limits.use(1,depth);
        if(type==boolean.class){int b=in.readUnsignedByte();if(b>1)throw new IOException("R335_CODEC_BOOLEAN");return b==1;}
        if(type==int.class)return in.readInt();
        if(type==long.class)return in.readLong();
        if(type==double.class)return Double.longBitsToDouble(in.readLong());
        int tag=in.readInt();if(tag==-1)return null;
        if(tag>=0){if(tag>=handles.size()||handles.get(tag)==null||handles.get(tag).getClass()!=type)throw new IOException("R335_CODEC_REFERENCE");return handles.get(tag);}
        if(tag!=-2||handles.size()>=MAX_HANDLES)throw new IOException("R335_CODEC_NEW_REFERENCE");
        int id=handles.size();handles.add(null);Object result;
        if(type==String.class){int n=in.readInt();if(n<0||n>MAX_BYTES||n>in.available())throw new IOException("R335_CODEC_STRING_BOUND");byte[] utf=new byte[n];in.readFully(utf);String text=new String(utf,StandardCharsets.UTF_8);if(!Arrays.equals(utf,text.getBytes(StandardCharsets.UTF_8)))throw new IOException("R335_CODEC_UTF8");result=text;}
        else if(type.isEnum()){try{result=Enum.valueOf((Class)type,in.readUTF());}catch(IllegalArgumentException bad){throw new IOException("R335_CODEC_ENUM",bad);}}
        else if(type.isArray()) {
            int n=in.readInt();if(n<0||n>MAX_ELEMENTS-limits.elements||n>in.available())throw new IOException("R335_CODEC_ARRAY_BOUND");
            result=Array.newInstance(type.getComponentType(),n);
            for(int i=0;i<n;i++)Array.set(result,i,read(in,type.getComponentType(),handles,limits,depth+1));
        } else {
            int index=in.readInt();if(index<0||index>=SCHEMAS.length||SCHEMAS[index].type!=type)throw new IOException("R335_CODEC_CLASS_ID");
            Schema schema=SCHEMAS[index];Object[] fields=new Object[schema.fields.length];
            for(int i=0;i<fields.length;i++)fields[i]=read(in,schema.fields[i].getType(),handles,limits,depth+1);
            result=construct(index,fields);
        }
        handles.set(id,result);return result;
    }
}
