package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.V51ChecksumEngine;
import com.ethk3.novaonepro.ReleaseManifestR926;
import com.ethk3.novaonepro.TypedAtomMoleculeGraphR930;
import com.ethk3.novaonepro.TypedAtomMoleculeGraphR930.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Typed bounded grammar. No generated Java, reflection or executable text. */
public final class CandidateAstR952 {
    public enum Operator { POSITION_FREQUENCY, RECENCY_BLEND, MARKOV_TRANSITION, CROSS_POSITION_LAG, MOLECULAR_BLEND, FUSION, GRAPH_V3 }
    /** Ordinals are part of CP4MODEL binary v1. */
    public enum Family { FREQUENCY, OMISSION, MARKOV, TRANSITION, STATISTICAL, REGIME,
        LINEAR, LOGISTIC, TREE, RULE, BANDIT, EXPERT, NEURAL, ENSEMBLE, HYBRID }
    public final int formatVersion;
    public final Family family;
    public final GraphSpec graphSpec;
    public final Operator operator;
    public final int position,lookback,depth,weightPercent;
    public final CandidateAstR952 left,right;
    public final String canonical,structuralHash,semanticHashCP12;
    public CandidateAstR952(Operator op,int pos,int window){
        if(op==null||op==Operator.FUSION||op==Operator.GRAPH_V3)throw new IllegalArgumentException("R974_AST_LEAF_OPERATOR");
        if(pos<1||pos>10||window<20||window>5000)throw new IllegalArgumentException("R952_AST_POSITION_OR_LOOKBACK");
        formatVersion=1;family=legacyFamily(op);graphSpec=null;
        operator=op;position=pos;lookback=window;depth=0;weightPercent=0;left=right=null;
        canonical="AST1("+op.name()+",POSITION="+position+",LOOKBACK="+lookback+",K=3)";
        structuralHash=V51ChecksumEngine.sha256(canonical);semanticHashCP12=SemanticGenomeCP12.hash(formatVersion,canonical);
    }
    private CandidateAstR952(CandidateAstR952 a,CandidateAstR952 b,int weight){
        if(a==null||b==null||a.formatVersion==3||b.formatVersion==3||a.position!=b.position||a.structuralHash.equals(b.structuralHash)
                ||(weight!=25&&weight!=50&&weight!=75)||Math.max(a.depth,b.depth)>=3)
            throw new IllegalArgumentException("R974_AST_FUSION_CONTRACT");
        formatVersion=2;family=Family.ENSEMBLE;graphSpec=null;
        operator=Operator.FUSION;position=a.position;lookback=Math.max(a.lookback,b.lookback);
        left=a;right=b;depth=Math.max(a.depth,b.depth)+1;weightPercent=weight;
        canonical="AST2(FUSE,W="+weight+",L="+a.canonical+",R="+b.canonical+")";
        if(canonical.length()>4096)throw new IllegalArgumentException("R974_AST_SIZE");
        structuralHash=V51ChecksumEngine.sha256(canonical);semanticHashCP12=SemanticGenomeCP12.hash(formatVersion,canonical);
    }
    public static CandidateAstR952 fuse(CandidateAstR952 a,CandidateAstR952 b,int weight){return new CandidateAstR952(a,b,weight);}
    public CandidateAstR952 withLookback(int window){
        if(formatVersion==3)throw new IllegalArgumentException("AST3_MUTATION_REQUIRES_REGISTRY");
        if(operator!=Operator.FUSION)return new CandidateAstR952(operator,position,window);
        // Mutate one source program; retain the other parent's distinct formula and semantics.
        CandidateAstR952 changed=left.withLookback(window);
        if(changed.structuralHash.equals(right.structuralHash))return fuse(left,right,weightPercent==50?25:50);
        return fuse(changed,right,weightPercent);
    }
    public static CandidateAstR952 parse(String value){
        if(value!=null&&value.startsWith("AST3:"))return parseGraphV3(value);
        return parse(value,0);
    }
    private static Family legacyFamily(Operator op){
        switch(op){case MARKOV_TRANSITION:return Family.MARKOV;case CROSS_POSITION_LAG:return Family.TRANSITION;
            case MOLECULAR_BLEND:return Family.HYBRID;default:return Family.FREQUENCY;}
    }
    public static boolean familyAccepts(Family family,CandidateCompilerR952.ModelKind kind) {
        if(family==null||kind==null)return false;
        switch(family){
            case FREQUENCY:case OMISSION:case MARKOV:case TRANSITION:case STATISTICAL:case REGIME:case RULE:
                return kind==CandidateCompilerR952.ModelKind.FIXED_GRAPH;
            case LINEAR:return kind==CandidateCompilerR952.ModelKind.SOFTMAX_LINEAR;
            case LOGISTIC:return kind==CandidateCompilerR952.ModelKind.OVR_LOGISTIC;
            case TREE:return kind==CandidateCompilerR952.ModelKind.HISTOGRAM_TREE;
            case BANDIT:case EXPERT:case ENSEMBLE:return kind==CandidateCompilerR952.ModelKind.EXPERT_HEDGE;
            case NEURAL:return kind==CandidateCompilerR952.ModelKind.MLP_D32_16_10;
            case HYBRID:return true;default:return false;
        }
    }
    public static final class GraphSpec {
        public final String game,compilerRevision,featureManifestHash,normalizerPolicy,outputId,canonical;
        public final int position,inputDimension,requiredHistory,depth;
        public final CandidateCompilerR952.ModelKind modelKind;
        public final List<NodeSpec> nodes;
        public final List<String> featureOutputIds;
        public final List<CandidateCompilerR952.ExpertRef> experts;
        public final TypedAtomMoleculeGraphR930.Graph typedGraph;
        public final FeatureManifestR952 manifest;
        private GraphSpec(Map<String,Object> root,TypedAtomMoleculeGraphR930.Graph graph,
                List<String> features,List<CandidateCompilerR952.ExpertRef> experts,
                int depth,FeatureManifestR952 manifest) {
            canonical=StrictCanonicalJson.write(root);this.depth=depth;typedGraph=graph;
            nodes=graph.orderedNodes;game=str(root,"game");position=integer(root,"position");
            compilerRevision=str(root,"compilerRevision");normalizerPolicy=str(root,"normalizerPolicy");
            outputId=str(root,"outputId");inputDimension=integer(root,"inputDimension");
            modelKind=CandidateCompilerR952.ModelKind.valueOf(str(root,"modelKind"));
            this.manifest=manifest;featureManifestHash=manifest.hash;requiredHistory=manifest.requiredHistory;
            featureOutputIds=Collections.unmodifiableList(new ArrayList<>(features));
            this.experts=Collections.unmodifiableList(new ArrayList<>(experts));
        }
        /** Returns a newly parsed immutable object, never a mutable alias into the AST. */
        public Map<String,Object> canonicalObject(){return StrictCanonicalJson.readObject(canonical);}
    }
    private CandidateAstR952(Family family,GraphSpec spec,String canonical) {
        formatVersion=3;operator=Operator.GRAPH_V3;position=spec.position;lookback=spec.requiredHistory;
        depth=spec.depth;weightPercent=0;left=right=null;this.family=family;graphSpec=spec;
        this.canonical=canonical;structuralHash=V51ChecksumEngine.sha256(canonical);semanticHashCP12=SemanticGenomeCP12.hash(formatVersion,canonical);
    }
    public static String hashGraphKernelWithoutManifest(Map<String,Object> root) {
        Map<String,Object> kernel=new HashMap<>(root);kernel.remove("featureManifest");
        return V51ChecksumEngine.sha256("CP5_GRAPH_KERNEL_V1|"+StrictCanonicalJson.write(kernel));
    }
    private static CandidateAstR952 parseGraphV3(String text) {
        need(text.length()<=32768&&text.getBytes(StandardCharsets.UTF_8).length<=32768,"AST3_SIZE");
        Map<String,Object> root=StrictCanonicalJson.readObject(text.substring(5));
        keys(root,"compilerRevision","experts","family","featureManifest","featureOutputIds","game",
            "inputDimension","modelKind","nodes","normalizerPolicy","outputId","position","version");
        need(integer(root,"version")==3,"AST3_VERSION");need(str(root,"compilerRevision").equals("AST3_CP4_1"),"AST3_COMPILER");
        String game=str(root,"game");need(game.equals(ReleaseManifestR926.GAME_CODE),"AST3_GAME");
        int position=integer(root,"position");need(position>=1&&position<=10,"AST3_POSITION");
        Family family=Family.valueOf(str(root,"family"));
        CandidateCompilerR952.ModelKind kind=CandidateCompilerR952.ModelKind.valueOf(str(root,"modelKind"));
        need(familyAccepts(family,kind),"AST3_FAMILY_MODEL_MISMATCH");
        List<?> raw=array(root.get("nodes"));need(raw.size()>=1&&raw.size()<=128,"AST3_NODE_COUNT");
        ArrayList<NodeSpec> nodes=new ArrayList<>();Map<String,NodeSpec> prior=new LinkedHashMap<>();
        Map<String,Integer> depths=new HashMap<>();int maximumDepth=0;
        for(int i=0;i<raw.size();i++) {
            Map<String,Object> n=object(raw.get(i));keys(n,"atomId","id","operator","outputType","parameters","parents","stage","unit");
            String id=str(n,"id");need(id.equals("n"+i),"AST3_NODE_ORDER");
            String op=str(n,"operator"),atom=str(n,"atomId"),unit=str(n,"unit");
            need(!unit.isEmpty()&&unit.length()<=64&&unit.equals(unit.trim()),"AST3_UNIT");
            ValueType type=ValueType.valueOf(str(n,"outputType"));Stage stage=Stage.valueOf(str(n,"stage"));
            need(stage.productionPredraw(),"AST3_STAGE");List<String> parents=strings(n.get("parents"));int depth=0;
            List<String> qualified=new ArrayList<>();for(String p:parents){need(prior.containsKey(p),"AST3_PARENT_NOT_EARLIER");
                depth=Math.max(depth,depths.get(p)+1);qualified.add("CP4:"+p);}
            need(depth<=12,"AST3_DEPTH");maximumDepth=Math.max(maximumDepth,depth);
            List<String> ps=strings(n.get("parameters"));need(ps.size()<=64,"AST3_PARAMETER_COUNT");double[] parameters=new double[ps.size()];
            for(int j=0;j<ps.size();j++){double v=Double.parseDouble(ps.get(j));need(Double.isFinite(v),"AST3_PARAMETER_FINITE");if(v==0)v=0;
                need(Double.toHexString(v).equals(ps.get(j)),"AST3_PARAMETER_CANONICAL");parameters[j]=v;}
            boolean isAtom=op.equals("ATOM");need(isAtom?atom.matches("ATOM2-[0-9a-f]{24}")&&parents.isEmpty()&&parameters.length==0:atom.isEmpty(),"AST3_ATOM_CONTRACT");
            need(type!=ValueType.VECTOR&&type!=ValueType.MATRIX&&type!=ValueType.CATEGORY,"AST3_USE_EXPLICIT_TYPED_SHAPE");
            int width=type==ValueType.SCORE_VECTOR10||type==ValueType.RANK_VECTOR10?10:type==ValueType.K3_SET?3:1;
            NodeSpec node=new NodeSpec("CP4",id,op,isAtom?NodeKind.ATOM:NodeKind.MOLECULE,type,new Shape(1,width),stage,unit,0,0,
                MissingPolicy.PROPAGATE,isAtom?atom:op.equals("CONSTANT")?"CONSTANT":"AST3","SOURCE_STRICTLY_BEFORE_TARGET",0L,qualified,
                isAtom?null:TypedAtomMoleculeGraphR930.Operator.valueOf(op),parameters,false,"AST3_CP4_1");
            nodes.add(node);prior.put(id,node);depths.put(id,depth);
        }
        TypedAtomMoleculeGraphR930.Graph graph=new TypedAtomMoleculeGraphR930.Graph(game,"FROZEN_PREDRAW","AST3_CP4_1",nodes);
        FeatureManifestR952 manifest=FeatureManifestR952.fromGraphV3(root.get("featureManifest"));
        List<String> features=strings(root.get("featureOutputIds"));String output=str(root,"outputId");int d=integer(root,"inputDimension");
        ArrayList<CandidateCompilerR952.ExpertRef> experts=new ArrayList<>();HashSet<String> expertIds=new HashSet<>();
        for(Object item:array(root.get("experts"))){Map<String,Object> ref=object(item);keys(ref,"freezeHash","modelHash","featureManifestHash","position");
            CandidateCompilerR952.ExpertRef e=new CandidateCompilerR952.ExpertRef(str(ref,"freezeHash"),str(ref,"modelHash"),str(ref,"featureManifestHash"),integer(ref,"position"));
            need(e.position==position&&expertIds.add(e.freezeHash),"AST3_EXPERT_IDENTITY");experts.add(e);}
        if(kind==CandidateCompilerR952.ModelKind.FIXED_GRAPH){
            need(features.isEmpty()&&prior.containsKey(output)&&experts.isEmpty(),"AST3_FIXED_ROOT");
            need(prior.get(output).valueType==ValueType.SCORE_VECTOR10&&d==10,"AST3_FIXED_VECTOR10");
        }else if(kind==CandidateCompilerR952.ModelKind.EXPERT_HEDGE){
            need(output.equals("@MODEL")&&features.isEmpty()&&experts.size()>=2&&experts.size()<=8,"AST3_HEDGE_ROOT");
            need(d==10*experts.size(),"AST3_HEDGE_DIMENSION");
        }else{
            need(output.equals("@MODEL")&&features.size()>=1&&features.size()<=32&&experts.isEmpty(),"AST3_MODEL_ROOT");
            need(new HashSet<>(features).size()==features.size()&&d==2*features.size(),"AST3_FEATURE_DIMENSION");
            for(String id:features)need(prior.containsKey(id)&&prior.get(id).shape.elements()==1,"AST3_SCALAR_FEATURE");
        }
        need(manifest.orderedFeatureIds.equals(features)&&manifest.inputDimension==d,"AST3_MANIFEST_FEATURES");
        need(manifest.normalizerPolicy.equals(str(root,"normalizerPolicy")),"AST3_NORMALIZER_POLICY");
        for(int i=0;i<features.size();i++)need(manifest.featureTypes.get(i).equals(prior.get(features.get(i)).valueType.name()),"AST3_MANIFEST_TYPES");
        need(manifest.graphHash.equals(hashGraphKernelWithoutManifest(root)),"GRAPH_KERNEL_HASH_MISMATCH");
        GraphSpec spec=new GraphSpec(root,graph,features,experts,maximumDepth,manifest);
        String canonical="AST3:"+spec.canonical;need(canonical.equals(text),"AST3_NON_CANONICAL");
        return new CandidateAstR952(family,spec,canonical);
    }
    static void need(boolean b,String code){if(!b)throw new IllegalArgumentException(code);}
    @SuppressWarnings("unchecked") static Map<String,Object> object(Object o){need(o instanceof Map,"AST3_OBJECT");return (Map<String,Object>)o;}
    static List<?> array(Object o){need(o instanceof List,"AST3_ARRAY");return (List<?>)o;}
    static void keys(Map<String,Object> o,String... names){need(o.keySet().equals(new HashSet<>(Arrays.asList(names))),"AST3_KEYS");}
    static String str(Map<String,Object> o,String key){Object v=o.get(key);need(v instanceof String,"AST3_STRING:"+key);return (String)v;}
    static int integer(Map<String,Object> o,String key){Object v=o.get(key);need(v instanceof Integer||v instanceof Long,"AST3_INTEGER:"+key);
        long n=((Number)v).longValue();need(n>=Integer.MIN_VALUE&&n<=Integer.MAX_VALUE,"AST3_INTEGER_RANGE");return (int)n;}
    static List<String> strings(Object v){ArrayList<String> out=new ArrayList<>();for(Object item:array(v)){need(item instanceof String,"AST3_STRING_ARRAY");out.add((String)item);}return Collections.unmodifiableList(out);}

    private static CandidateAstR952 parse(String value,int level){
        if(value==null||value.length()>4096||level>3)throw new IllegalArgumentException("R974_AST_PARSE_BOUND");
        CandidateAstR952 ast;
        if(value.startsWith("AST2(FUSE,W=")&&value.endsWith(")")){
            int l=value.indexOf(",L=");int w=Integer.parseInt(value.substring(12,l));
            int nesting=0,split=-1;
            for(int i=l+3;i<value.length()-1;i++){
                char ch=value.charAt(i);if(ch=='(')nesting++;else if(ch==')')nesting--;
                if(nesting==0&&value.startsWith(",R=",i)){split=i;break;}
            }
            if(split<0)throw new IllegalArgumentException("R974_AST_FUSION_PARSE");
            ast=fuse(parse(value.substring(l+3,split),level+1),parse(value.substring(split+3,value.length()-1),level+1),w);
        }else{
            if(!value.matches("AST1\\((POSITION_FREQUENCY|RECENCY_BLEND|MARKOV_TRANSITION|CROSS_POSITION_LAG|MOLECULAR_BLEND),POSITION=[0-9]+,LOOKBACK=[0-9]+,K=3\\)"))
                throw new IllegalArgumentException("R952_AST_PARSE");
            ast=new CandidateAstR952(Operator.valueOf(value.substring(5,value.indexOf(','))),number(value,"POSITION="),number(value,"LOOKBACK="));
        }
        if(!ast.canonical.equals(value))throw new IllegalArgumentException("R952_AST_NON_CANONICAL");return ast;
    }
    private static int number(String v,String key){int s=v.indexOf(key)+key.length(),e=s;while(e<v.length()&&Character.isDigit(v.charAt(e)))e++;return Integer.parseInt(v.substring(s,e));}
}
