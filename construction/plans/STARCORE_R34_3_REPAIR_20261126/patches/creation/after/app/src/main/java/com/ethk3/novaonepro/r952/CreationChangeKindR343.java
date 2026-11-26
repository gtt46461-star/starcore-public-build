package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.V51ChecksumEngine;
import java.util.*;

/** Audit taxonomy only. This class never grants admission, novelty or quality. */
public final class CreationChangeKindR343 {
    private CreationChangeKindR343() {}
    private static final Set<String> COMMUTATIVE = new HashSet<>(Arrays.asList(
        "ADD","MULTIPLY","MINIMUM","MAXIMUM","ABS_DIFFERENCE",
        "MEAN","SUM","INTERACTION3"));

    public static String receipt(CandidateAstR952 parent,CandidateAstR952 child,
            CandidateAstR952 secondary,String action,String operator,String evidence,
            List<AtomSpecCP4> registry) {
        Map<String,Object> r=new TreeMap<>();
        r.put("schema","R343_CREATION_CLASSIFICATION_1");r.put("action_id",action);
        r.put("operator",operator);r.put("evidence_hash",evidence);
        r.put("parent_ast_hash",parent.structuralHash);r.put("child_ast_hash",child.structuralHash);
        r.put("secondary_ast_hash",secondary==null?"":secondary.structuralHash);
        r.put("parent_family",parent.family.name());r.put("child_family",child.family.name());
        // The registry may contain 4096 atoms. This audit needs only atoms
        // referenced by these two bounded (<=128-node) graphs, not every spec JSON.
        Set<String> required=new HashSet<>();usedAtoms(parent,required);usedAtoms(child,required);
        Map<String,Map<String,Object>> specs=new HashMap<>();
        for(AtomSpecCP4 s:registry){
            if(!required.contains(s.atomId))continue;
            Map<String,Object> k=new TreeMap<>();k.put("family",s.family);k.put("version",s.version);
            k.put("context",s.context);k.put("transform",s.transform);k.put("regime",s.regime);
            k.put("outputType",s.outputType);k.put("missingPolicy",s.missingPolicy);
            Map<String,Object> raw=StrictCanonicalJson.readAtomObject(s.canonicalSpec);
            Map<String,Object> p=map(raw.get("params"));k.put("parameterNames",new ArrayList<>(new TreeSet<>(p.keySet())));
            // Markov order changes the conditioning mechanism. Windows, lags,
            // digit labels, strengths and epoch/teacher identities do not establish novelty.
            if(p.containsKey("order"))k.put("markovOrder",p.get("order"));
            specs.put(s.atomId,k);
        }
        String ps=shape(parent,specs),cs=shape(child,specs);
        r.put("parent_shape_hash",ps);r.put("child_shape_hash",cs);
        r.put("shape_scope","TYPED_OPERATOR_TOPOLOGY_AND_PRIMITIVE_FAMILY_V1");
        String kind;
        if(child.structuralHash.equals(parent.structuralHash))kind="NO_OP";
        else if(ps.isEmpty()||cs.isEmpty())kind="UNCLASSIFIED_LEGACY_OR_MISSING_SPEC";
        else if(ps.equals(cs))kind="PARAMETER_OR_SOURCE_ITERATION";
        else if("FIXED_TWO_EXPERT_MIX".equals(operator))kind="FUSION_STRUCTURE_UNVALIDATED";
        else if("CROSSOVER_SAME_TYPE".equals(operator)&&secondary!=null)kind="CROSSOVER_STRUCTURE_UNVALIDATED";
        else kind="STRUCTURE_CHANGE_UNVALIDATED";
        r.put("variation_class",kind);
        r.put("novel_mechanism_status","NOT_EVALUATED");
        r.put("quality_status","NOT_EVALUATED");
        r.put("generation_count_authority","EXISTING_DIRECTOR_ONLY");
        return StrictCanonicalJson.writeContext(r);
    }
    private static void usedAtoms(CandidateAstR952 ast,Set<String> ids){
        if(ast.formatVersion!=3)return;
        for(com.ethk3.novaonepro.TypedAtomMoleculeGraphR930.NodeSpec node:ast.graphSpec.nodes)
            if(node.kind==com.ethk3.novaonepro.TypedAtomMoleculeGraphR930.NodeKind.ATOM)ids.add(node.source);
    }
    private static String shape(CandidateAstR952 ast,Map<String,Map<String,Object>> specs){
        if(ast.formatVersion!=3)return "";
        return shapeObject(ast.graphSpec.canonicalObject(),specs);
    }
    /** Public for deterministic topology regression tests; input must be compiled AST3. */
    public static String shapeObject(Map<String,Object> root,Map<String,Map<String,Object>> specs){
        Map<String,String> digests=new HashMap<>();
        for(Object raw:list(root.get("nodes"))){
            Map<String,Object> n=map(raw),k=new TreeMap<>();
            for(String key:Arrays.asList("operator","outputType","stage","unit"))k.put(key,n.get(key));
            String op=String.valueOf(n.get("operator"));
            List<String> parents=new ArrayList<>();
            for(Object id:list(n.get("parents"))){String h=digests.get(String.valueOf(id));if(h==null)return "";parents.add(h);}
            if(COMMUTATIVE.contains(op))Collections.sort(parents);
            k.put("parents",parents);k.put("parameterArity",list(n.get("parameters")).size());
            if("ATOM".equals(op)){Map<String,Object> s=specs.get(String.valueOf(n.get("atomId")));if(s==null)return "";k.put("primitive",s);}
            digests.put(String.valueOf(n.get("id")),hash(k));
        }
        Map<String,Object> result=new TreeMap<>();
        result.put("modelKind",root.get("modelKind"));result.put("normalizerPolicy",root.get("normalizerPolicy"));
        result.put("inputDimension",root.get("inputDimension"));result.put("expertCount",list(root.get("experts")).size());
        List<String> features=new ArrayList<>();
        for(Object id:list(root.get("featureOutputIds"))){String h=digests.get(String.valueOf(id));if(h==null)return "";features.add(h);}
        result.put("features",features);
        String out=String.valueOf(root.get("outputId"));
        if(!"@MODEL".equals(out)&&!digests.containsKey(out))return "";
        result.put("output","@MODEL".equals(out)?out:digests.get(out));
        Map<String,Object> manifest=map(root.get("featureManifest"));
        result.put("missingPolicies",manifest.get("missingPolicyPerFeature"));
        // Binding/epoch/model hashes are provenance, not new mathematics. They
        // remain fully verified by the compiler/Director; this digest is NOT an ABI.
        return hash(result);
    }
    private static String hash(Map<String,Object> v){return V51ChecksumEngine.sha256(StrictCanonicalJson.writeContext(v));}
    @SuppressWarnings("unchecked") private static Map<String,Object> map(Object o){return (Map<String,Object>)o;}
    @SuppressWarnings("unchecked") private static List<Object> list(Object o){return (List<Object>)o;}
}
