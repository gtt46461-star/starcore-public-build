package com.ethk3.novaonepro.r952;

import java.util.*;
import com.ethk3.novaonepro.V51ChecksumEngine;

/** Tests the actual classifier and strict JSON against metadata fixtures only. */
public final class CreationClassificationHarness {
    static int assertions;
    static void check(boolean b,String m){assertions++;if(!b)throw new AssertionError(m);}
    static Map<String,Object> m(Object... fields){Map<String,Object> o=new TreeMap<>();for(int i=0;i<fields.length;i+=2)o.put((String)fields[i],fields[i+1]);return o;}
    static Map<String,Object> atom(String id,String spec){return m("id",id,"operator","ATOM","atomId",spec,"parents",Collections.emptyList(),"parameters",Collections.emptyList(),"outputType","DOUBLE","stage","FROZEN_PREDRAW","unit","dimensionless");}
    static Map<String,Object> node(String id,String op,String a,String b){return m("id",id,"operator",op,"atomId","","parents",Arrays.asList(a,b),"parameters",Collections.emptyList(),"outputType","DOUBLE","stage","FROZEN_PREDRAW","unit","dimensionless");}
    static Map<String,Object> root(String op,boolean swap,String first,String second){
        return m("nodes",Arrays.asList(atom("n0",first),atom("n1",second),node("n2",op,swap?"n1":"n0",swap?"n0":"n1")),"modelKind","SOFTMAX_LINEAR","normalizerPolicy","TRAIN_ONLY_MEAN_STD","inputDimension",2,"experts",Collections.emptyList(),"featureOutputIds",Arrays.asList("n2"),"outputId","@MODEL","featureManifest",m("missingPolicyPerFeature",Arrays.asList("MASK"),"bindingHashes",Arrays.asList("bindingA")));
    }
    static AtomSpecCP4 spec(String family,Map<String,Object> parameters){
        Map<String,Object> s=m("family",family,"version","CP2","context","PIT_DRAW","transform","IDENTITY","regime","ALL","position",1,"params",parameters);
        String json=StrictCanonicalJson.writeAtom(s),hash=V51ChecksumEngine.sha256(json);
        return AtomSpecCP4.fromRegistry("ATOM2-"+hash.substring(0,24),hash,json,family.equals("MARKOV_SCORE")?"PROBABILITY":"SCALAR",family.equals("ENTROPY")?20:family.equals("MARKOV_SCORE")?20+((Number)parameters.get("order")).intValue():((Number)parameters.get("window")).intValue(),"MASK","ACTIVE");
    }
    static CandidateAstR952 ast(Map<String,Object> root){return new CandidateAstR952(root);}
    static Map<String,Object> receipt(CandidateAstR952 a,CandidateAstR952 b,CandidateAstR952 other,String op,List<AtomSpecCP4> registry){
        String json=CreationChangeKindR343.receipt(a,b,other,"action",""+op,"evidence",registry);
        Map<String,Object> r=StrictCanonicalJson.readContextObject(json);
        check("NOT_EVALUATED".equals(r.get("novel_mechanism_status")),"no novelty grant");
        check("NOT_EVALUATED".equals(r.get("quality_status")),"no quality grant");
        check("EXISTING_DIRECTOR_ONLY".equals(r.get("generation_count_authority")),"count owner unchanged");return r;
    }
    public static void main(String[] args)throws Exception {
        AtomSpecCP4 frequency=spec("FREQUENCY",m("digit",1,"window",20)),entropy=spec("ENTROPY",m("window",20));
        List<AtomSpecCP4> registry=new ArrayList<>(Arrays.asList(frequency,entropy));
        CandidateAstR952 add=ast(root("ADD",false,frequency.atomId,entropy.atomId)),swappedAdd=ast(root("ADD",true,frequency.atomId,entropy.atomId));
        Map<String,Object> addReceipt=receipt(add,swappedAdd,null,"OPERATOR_REPLACE",registry);
        check(addReceipt.get("parent_shape_hash").equals(addReceipt.get("child_shape_hash")),"ADD parents commute");
        check("PARAMETER_OR_SOURCE_ITERATION".equals(addReceipt.get("variation_class")),"commuted ADD is conservative iteration");
        CandidateAstR952 signed=ast(root("SIGNED_PRODUCT",false,frequency.atomId,entropy.atomId)),swappedSigned=ast(root("SIGNED_PRODUCT",true,frequency.atomId,entropy.atomId));
        Map<String,Object> signedReceipt=receipt(signed,swappedSigned,null,"OPERATOR_REPLACE",registry);
        check(!signedReceipt.get("parent_shape_hash").equals(signedReceipt.get("child_shape_hash")),"SIGNED_PRODUCT order retained");
        check(SignedProductActualKernel.evaluate(-2,3)!=SignedProductActualKernel.evaluate(3,-2),"actual production signed kernel not commutative");
        check("STRUCTURE_CHANGE_UNVALIDATED".equals(signedReceipt.get("variation_class")),"no novelty from signed shape difference");
        Map<String,Object> unchanged=receipt(add,add,null,"WINDOW_MUTATION",registry);check("NO_OP".equals(unchanged.get("variation_class")),"same actual AST NO_OP");
        AtomSpecCP4 longer=spec("FREQUENCY",m("digit",1,"window",40));registry.add(longer);
        CandidateAstR952 param=ast(root("ADD",false,longer.atomId,entropy.atomId));
        Map<String,Object> paramReceipt=receipt(add,param,null,"WINDOW_MUTATION",registry);check("PARAMETER_OR_SOURCE_ITERATION".equals(paramReceipt.get("variation_class")),"window is parameter iteration");
        Map<String,Object> fusion=receipt(add,signed,null,"FIXED_TWO_EXPERT_MIX",registry);check("FUSION_STRUCTURE_UNVALIDATED".equals(fusion.get("variation_class")),"fusion not quality");
        Map<String,Object> crossover=receipt(add,signed,swappedAdd,"CROSSOVER_SAME_TYPE",registry);check("CROSSOVER_STRUCTURE_UNVALIDATED".equals(crossover.get("variation_class")),"crossover not quality");
        Map<String,Object> missing=receipt(add,param,null,"WINDOW_MUTATION",Arrays.asList(entropy));check("UNCLASSIFIED_LEGACY_OR_MISSING_SPEC".equals(missing.get("variation_class")),"missing spec cannot imply novelty");
        CandidateAstR952 legacy=new CandidateAstR952();Map<String,Object> old=receipt(legacy,add,null,"NEW_FAMILY",registry);check("UNCLASSIFIED_LEGACY_OR_MISSING_SPEC".equals(old.get("variation_class")),"legacy stays unclassified");
        // An unused registry row is neither parsed nor an extra failure dependency.
        AtomSpecCP4 unused=spec("FREQUENCY",m("digit",2,"window",20));
        java.lang.reflect.Field value=AtomSpecCP4.class.getDeclaredField("canonicalSpec");value.setAccessible(true);value.set(unused,"not valid JSON");
        List<AtomSpecCP4> many=new ArrayList<>(registry);for(int i=0;i<4092;i++)many.add(unused);
        String before=CreationChangeKindR343.receipt(add,param,null,"action","WINDOW_MUTATION","evidence",registry);
        String after=CreationChangeKindR343.receipt(add,param,null,"action","WINDOW_MUTATION","evidence",many);
        check(before.equals(after),"unused 4092 specs excluded from parse/result");
        Map<String,Object> binding=root("ADD",false,frequency.atomId,entropy.atomId);
        @SuppressWarnings("unchecked") Map<String,Object> manifest=(Map<String,Object>)binding.get("featureManifest");manifest.put("bindingHashes",Arrays.asList("bindingB"));
        Map<String,Object> source=receipt(add,ast(binding),null,"REPLACE_SOURCE",registry);check("PARAMETER_OR_SOURCE_ITERATION".equals(source.get("variation_class")),"provenance changes cannot imply new mechanism");
        System.out.println("{\"status\":\"PASS\",\"assertions\":"+assertions+",\"scope\":\"actual classifier, StrictCanonicalJson, AtomSpecCP4, SHA and extracted signed kernel; graph metadata fixture adapters; not Android compiler acceptance or DB transaction\"}");
    }
}
