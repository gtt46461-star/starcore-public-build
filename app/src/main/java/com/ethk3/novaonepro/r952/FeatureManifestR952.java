package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.V51ChecksumEngine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.*;

public final class FeatureManifestR952 {
    public static final class Feature {
        public final String id,dtype,sourceTable,pitRule,missingPolicy;
        public final int lookback;
        Feature(String i,String d,String s,String p,int l,String m){id=i;dtype=d;sourceTable=s;pitRule=p;lookback=l;missingPolicy=m;}
        String canonical(){return id+'|'+dtype+'|'+sourceTable+'|'+pitRule+'|'+lookback+'|'+missingPolicy;}
    }
    public final List<Feature> features;
    public final String hash;
    public final boolean modern;
    public final String revision,graphHash,normalizerPolicy;
    public final List<String> orderedFeatureIds,featureTypes,atomSpecHashes,bindingHashes,missingPolicyPerFeature;
    public final List<MatrixRef> matrixSpecRefs;
    public final List<LongSummaryBindingsR80.Ref> summarySpecRefs;
    public final int requiredHistory,inputDimension;
    public final long estimatedStateBytes;
    private final String modernCanonical;
    private FeatureManifestR952(List<Feature> input){
        modern=false;revision="R952_FEATURES_V1";graphHash="";normalizerPolicy="";modernCanonical="";
        orderedFeatureIds=featureTypes=atomSpecHashes=bindingHashes=missingPolicyPerFeature=Collections.emptyList();
        matrixSpecRefs=Collections.emptyList();summarySpecRefs=Collections.emptyList();requiredHistory=120;inputDimension=10;estimatedStateBytes=0;
        features=Collections.unmodifiableList(input);StringBuilder b=new StringBuilder("R952_FEATURES_V1");for(Feature f:input)b.append('\n').append(f.canonical());hash=V51ChecksumEngine.sha256(b.toString());}
    public static FeatureManifestR952 production() {
        ArrayList<Feature> f=new ArrayList<Feature>();
        f.add(new Feature("position_frequency_120","float64","draws","source_issue<target_issue",120,"ZERO"));
        f.add(new Feature("position_recency_40","float64","draws","source_issue<target_issue",40,"ZERO"));
        f.add(new Feature("issue_count","int32","draws","source_issue<target_issue",120,"REJECT_EMPTY"));
        return new FeatureManifestR952(f);
    }
    public static final class MatrixRef {
        public final String type,partitionRule,manifest;
        MatrixRef(Map<String,Object> value) {
            CandidateAstR952.keys(value,"type","partitionRule","manifest");
            type=CandidateAstR952.str(value,"type");partitionRule=CandidateAstR952.str(value,"partitionRule");
            manifest=CandidateCompilerR952.Modern.hashText(CandidateAstR952.str(value,"manifest"));
            CandidateAstR952.need(type.matches("[A-Za-z][A-Za-z0-9_]{1,63}")&&!partitionRule.isEmpty()&&partitionRule.length()<=256,"MATRIX_REF");
            new MatrixBindingsCP12.Ref(this);
        }
        String key(){return type+'\u0000'+partitionRule+'\u0000'+manifest;}
    }
    private FeatureManifestR952(Map<String,Object> obj) {
        boolean longRevision=LongSummaryBindingsR80.FEATURE_REVISION.equals(obj.get("revision"));
        if(longRevision)CandidateAstR952.keys(obj,"revision","graphHash","orderedFeatureIds","featureTypes","requiredHistory","atomSpecHashes",
            "bindingHashes","matrixSpecRefs","normalizerPolicy","missingPolicyPerFeature","inputDimension","estimatedStateBytes","summarySpecRefs");
        else CandidateAstR952.keys(obj,"revision","graphHash","orderedFeatureIds","featureTypes","requiredHistory","atomSpecHashes",
            "bindingHashes","matrixSpecRefs","normalizerPolicy","missingPolicyPerFeature","inputDimension","estimatedStateBytes");
        // Reparse to ensure the entire immutable manifest is valid canonical JSON.
        modernCanonical=StrictCanonicalJson.write(obj);Map<String,Object> o=StrictCanonicalJson.readObject(modernCanonical);
        modern=true;features=Collections.emptyList();revision=CandidateAstR952.str(o,"revision");
        CandidateAstR952.need(revision.equals("CP4_FEATURE_1")||longRevision,"FEATURE_REVISION");
        graphHash=CandidateCompilerR952.Modern.hashText(CandidateAstR952.str(o,"graphHash"));
        normalizerPolicy=CandidateAstR952.str(o,"normalizerPolicy");
        CandidateAstR952.need(normalizerPolicy.equals("TRAIN_ONLY_ZSCORE_V1"),"FEATURE_NORMALIZER_POLICY");
        orderedFeatureIds=CandidateAstR952.strings(o.get("orderedFeatureIds"));featureTypes=CandidateAstR952.strings(o.get("featureTypes"));
        missingPolicyPerFeature=CandidateAstR952.strings(o.get("missingPolicyPerFeature"));
        CandidateAstR952.need(orderedFeatureIds.size()<=32&&new HashSet<>(orderedFeatureIds).size()==orderedFeatureIds.size()
            &&featureTypes.size()==orderedFeatureIds.size()&&missingPolicyPerFeature.size()==orderedFeatureIds.size(),"FEATURE_LIST_SHAPE");
        for(String id:orderedFeatureIds)CandidateAstR952.need(id.matches("n[0-9]{1,3}"),"FEATURE_ID");
        for(String type:featureTypes)CandidateAstR952.need(Arrays.asList("DOUBLE","INTEGER","BOOLEAN","PROBABILITY","REGIME").contains(type),"FEATURE_SCALAR_TYPE");
        for(String policy:missingPolicyPerFeature)CandidateAstR952.need(policy.equals("MASK")||policy.equals("REJECT"),"FEATURE_MISSING_POLICY");
        atomSpecHashes=sortedHashes(o,"atomSpecHashes",256);bindingHashes=sortedHashes(o,"bindingHashes",256);
        requiredHistory=CandidateAstR952.integer(o,"requiredHistory");inputDimension=CandidateAstR952.integer(o,"inputDimension");
        estimatedStateBytes=CandidateAstR952.integer(o,"estimatedStateBytes");
        CandidateAstR952.need(requiredHistory>=0&&requiredHistory<=5000&&inputDimension>=1&&inputDimension<=80
            &&estimatedStateBytes>=0&&estimatedStateBytes<=8L*1024*1024,"FEATURE_BOUNDS");
        ArrayList<MatrixRef> refs=new ArrayList<>();String previous=null;
        for(Object v:CandidateAstR952.array(o.get("matrixSpecRefs"))){MatrixRef ref=new MatrixRef(CandidateAstR952.object(v));
            CandidateAstR952.need(previous==null||previous.compareTo(ref.key())<0,"MATRIX_REF_ORDER");previous=ref.key();refs.add(ref);}
        CandidateAstR952.need(refs.size()<=20,"MATRIX_REF_COUNT");matrixSpecRefs=Collections.unmodifiableList(refs);
        ArrayList<LongSummaryBindingsR80.Ref> summaries=new ArrayList<>();String previousSummary=null;
        if(longRevision)for(Object value:CandidateAstR952.array(o.get("summarySpecRefs"))){LongSummaryBindingsR80.Ref ref=new LongSummaryBindingsR80.Ref(CandidateAstR952.object(value));CandidateAstR952.need(previousSummary==null||previousSummary.compareTo(ref.nodeId)<0,"LONG_REF_ORDER");previousSummary=ref.nodeId;summaries.add(ref);}
        CandidateAstR952.need(summaries.size()<=16&&(!longRevision||!summaries.isEmpty()),"LONG_REF_COUNT");summarySpecRefs=Collections.unmodifiableList(summaries);
        hash=V51ChecksumEngine.sha256(modernCanonical);
    }
    private static List<String> sortedHashes(Map<String,Object> o,String key,int max) {
        List<String> list=CandidateAstR952.strings(o.get(key));CandidateAstR952.need(list.size()<=max,"FEATURE_HASH_COUNT");String previous=null;
        for(String h:list){CandidateCompilerR952.Modern.hashText(h);CandidateAstR952.need(previous==null||previous.compareTo(h)<0,"FEATURE_HASH_ORDER");previous=h;}return list;
    }
    public static FeatureManifestR952 fromGraphV3(Object object){return new FeatureManifestR952(CandidateAstR952.object(object));}
    public static FeatureManifestR952 forAst(CandidateAstR952 ast) {
        if(ast==null)throw new IllegalArgumentException("MISSING_AST");return ast.formatVersion==3?ast.graphSpec.manifest:production();
    }
    public Map<String,Object> canonicalObject(){if(!modern)throw new IllegalStateException("LEGACY_MANIFEST");return StrictCanonicalJson.readObject(modernCanonical);}

}
