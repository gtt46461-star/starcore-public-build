package com.ethk3.novaonepro.r952;

import java.util.*;
import com.ethk3.novaonepro.V51ChecksumEngine;

/** Reproducible per-physical-input metadata, bound to the new manifest hash. */
public final class FeatureMetadataR82 {
    public static final String REVISION="R82_PHYSICAL_FEATURE_METADATA_2";
    private static final String[] COLUMNS={"id","name","unit","range","lag","window","source","missing","cost","version"};
    private FeatureMetadataR82(){}
    public static final class Dimension {
        public final String id,name,unit,range,source,missing,cost,version;
        public final int lag,window;
        private Dimension(Map<String,Object> row){
            CandidateAstR952.keys(row,"id","name","unit","range","lag","window","source","missing","cost","version");
            id=text(row,"id");name=text(row,"name");unit=text(row,"unit");range=text(row,"range");
            source=text(row,"source");missing=text(row,"missing");cost=text(row,"cost");version=text(row,"version");
            lag=CandidateAstR952.integer(row,"lag");window=CandidateAstR952.integer(row,"window");
            CandidateAstR952.need(lag>=0&&lag<=5000&&window>=0&&window<=43200&&version.equals(REVISION),"FEATURE_METADATA_BOUNDS");
        }
    }
    static List<Dimension> parse(Object raw,int expected){
        Map<String,Object> object=CandidateAstR952.object(raw);CandidateAstR952.keys(object,"revision","dimensionBudget","strings","dimensions");
        CandidateAstR952.need(REVISION.equals(object.get("revision"))&&CandidateAstR952.integer(object,"dimensionBudget")==expected,"FEATURE_METADATA_REVISION");
        List<String> symbols=CandidateAstR952.strings(object.get("strings"));
        CandidateAstR952.need(symbols.size()<=2048&&new HashSet<>(symbols).size()==symbols.size(),"FEATURE_METADATA_SYMBOLS");
        List<Dimension> result=new ArrayList<>();Set<String> ids=new HashSet<>();
        for(Object value:CandidateAstR952.array(object.get("dimensions"))){
            List<?> encoded=CandidateAstR952.array(value);CandidateAstR952.need(encoded.size()==COLUMNS.length,"FEATURE_METADATA_COLUMNS");
            Map<String,Object> row=new TreeMap<>();
            for(int i=0;i<COLUMNS.length;i++){
                Object v=encoded.get(i);CandidateAstR952.need(v instanceof Integer||v instanceof Long,"FEATURE_METADATA_INDEX_TYPE");
                long n=((Number)v).longValue();
                if(i==4||i==5)row.put(COLUMNS[i],v);
                else{CandidateAstR952.need(n>=0&&n<symbols.size(),"FEATURE_METADATA_INDEX_RANGE");row.put(COLUMNS[i],symbols.get((int)n));}
            }
            Dimension d=new Dimension(row);CandidateAstR952.need(ids.add(d.id),"FEATURE_METADATA_DUPLICATE");result.add(d);
        }
        CandidateAstR952.need(result.size()==expected&&expected<=80,"FEATURE_METADATA_DIMENSION");return Collections.unmodifiableList(result);
    }
    public static void decorate(Map<String,Object> root,List<AtomSpecCP4> specs){
        CandidateAstR952.object(root.get("featureManifest")).put("featureMetadataR82",build(root,specs));
    }
    static void validate(CandidateAstR952 ast,FeatureManifestR952 manifest,List<AtomSpecCP4> specs){
        if(!manifest.hasPhysicalMetadataR82)return; // Explicit legacy absence; original identity stays intact.
        Object actual=manifest.canonicalObject().get("featureMetadataR82");
        CandidateAstR952.need(StrictCanonicalJson.write(build(ast.graphSpec.canonicalObject(),specs)).equals(StrictCanonicalJson.write(actual)),"FEATURE_METADATA_SEMANTIC_MISMATCH");
    }
    /** No measured phone milliseconds are invented. Cost exposes declared finite budgets and source windows. */
    public static Map<String,Object> build(Map<String,Object> root,List<AtomSpecCP4> specs){
        Map<String,Map<String,Object>> nodes=new HashMap<>();
        for(Object raw:CandidateAstR952.array(root.get("nodes"))){Map<String,Object> n=CandidateAstR952.object(raw);CandidateAstR952.need(nodes.put(text(n,"id"),n)==null,"FEATURE_METADATA_NODE_ID");}
        CandidateAstR952.need(nodes.size()<=256,"FEATURE_METADATA_GRAPH_BOUND");
        Map<String,Info> memo=new HashMap<>();
        Map<String,AtomSpecCP4> atoms=new HashMap<>();for(AtomSpecCP4 atom:specs)atoms.put(atom.atomId,atom);
        List<Object> dimensions=new ArrayList<>();List<String> featureIds=CandidateAstR952.strings(root.get("featureOutputIds"));
        int physical=CandidateAstR952.integer(root,"inputDimension");
        List<String> policies=CandidateAstR952.strings(CandidateAstR952.object(root.get("featureManifest")).get("missingPolicyPerFeature"));
        if(!featureIds.isEmpty()){
            for(int index=0;index<featureIds.size();index++){String id=featureIds.get(index);Info info=info(id,nodes,atoms,new HashSet<String>(),memo);augmentMatrices(info,root);dimensions.add(dimension(id,id+" "+info.label+" training-only zscore","train_only_zscore_of("+info.unit+")","FINITE_ZSCORE;raw="+info.range,info,policies.get(index).equals("REJECT")?"REJECT_MISSING": "MASK;ABSENT_VALUE_ZERO"));}
            // Modern.normalized() concatenates every value first, then every presence mask.
            for(String id:featureIds){Info info=info(id,nodes,atoms,new HashSet<String>(),memo);augmentMatrices(info,root);dimensions.add(dimension(id+":present",id+" present","boolean","[0,1]",info,"ALWAYS_PRESENT_MASK_BIT"));}
        }else if("EXPERT_HEDGE".equals(root.get("modelKind"))){
            int expert=0;for(Object raw:CandidateAstR952.array(root.get("experts"))){Map<String,Object> ref=CandidateAstR952.object(raw);Info info=new Info();info.sources.add("frozen_expert:"+StrictCanonicalJson.write(ref));info.unit="probability";info.range="[0,1]";
                for(int digit=0;digit<10;digit++)dimensions.add(dimension("expert:"+expert+":digit:"+digit,"expert "+expert+" digit "+digit,info.unit,info.range,info,"REJECT_MISSING_FROZEN_EXPERT"));expert++;}
        }else{
            String id=text(root,"outputId");Info info=info(id,nodes,atoms,new HashSet<String>(),memo);augmentMatrices(info,root);
            for(int digit=0;digit<physical;digit++)dimensions.add(dimension(id+":digit:"+digit,"fixed graph digit "+digit,"probability","[0,1]",info,"REJECT_OR_MASK_PER_FROZEN_GRAPH"));
        }
        CandidateAstR952.need(dimensions.size()==physical&&physical>=1&&physical<=80,"FEATURE_METADATA_DIMENSION");
        // Intern complete strings and encode fixed-order rows. Value/mask and expert
        // dimensions share exact provenance without repeating it up to eighty times.
        // Decoding exposes the same full typed Dimension fields; no metadata is truncated
        // and the existing 32768-byte AST/DDL limits remain unchanged.
        LinkedHashMap<String,Integer> symbols=new LinkedHashMap<>();List<Object> encoded=new ArrayList<>();
        for(Object raw:dimensions){Map<String,Object> row=CandidateAstR952.object(raw);List<Object> tuple=new ArrayList<>();
            for(int i=0;i<COLUMNS.length;i++){
                Object value=row.get(COLUMNS[i]);
                if(i==4||i==5)tuple.add(value);
                else{String string=(String)value;Integer index=symbols.get(string);if(index==null){index=symbols.size();symbols.put(string,index);}tuple.add(index);}
            }
            encoded.add(tuple);
        }
        Map<String,Object> result=new TreeMap<>();result.put("revision",REVISION);result.put("dimensionBudget",physical);result.put("strings",new ArrayList<>(symbols.keySet()));result.put("dimensions",encoded);return result;
    }
    private static Map<String,Object> dimension(String id,String name,String unit,String range,Info info,String missing){
        Map<String,Object> value=new TreeMap<>();value.put("id",id);value.put("name",name);value.put("unit",unit);value.put("range",range);
        value.put("lag",info.lag);value.put("window",info.window);value.put("source",StrictCanonicalJson.write(new ArrayList<>(info.sources)));
        value.put("missing",missing);value.put("cost","ancestor_nodes="+info.nodes.size()+";source_window_issues="+info.window+";shared_state_cap_bytes=8388608;device_ms=UNMEASURED");value.put("version",REVISION);return value;
    }
    private static final class Info{
        String label="graph",unit="UNKNOWN_COMPOSITE_UNIT",range="FINITE_REAL_BOUND_NOT_DECLARED";int lag,window;
        final TreeSet<String> sources=new TreeSet<>(),nodes=new TreeSet<>();
        void merge(Info other){lag=Math.max(lag,other.lag);window=Math.max(window,other.window);sources.addAll(other.sources);nodes.addAll(other.nodes);}
    }
    private static Info info(String id,Map<String,Map<String,Object>> nodes,Map<String,AtomSpecCP4> specs,Set<String> visiting,Map<String,Info> memo){
        if(memo.containsKey(id))return memo.get(id);
        CandidateAstR952.need(visiting.add(id),"FEATURE_METADATA_GRAPH_CYCLE");Map<String,Object> node=nodes.get(id);CandidateAstR952.need(node!=null,"FEATURE_METADATA_NODE_MISSING");
        Info result=new Info();String op=text(node,"operator");result.label=op;List<String> parents=CandidateAstR952.strings(node.get("parents"));List<Info> inputs=new ArrayList<>();
        for(String parent:parents){Info input=info(parent,nodes,specs,visiting,memo);inputs.add(input);result.merge(input);}result.nodes.add(id);
        if(op.equals("ATOM")){
            AtomSpecCP4 atom=specs.get(text(node,"atomId"));CandidateAstR952.need(atom!=null,"FEATURE_METADATA_ATOM_MISSING");
            result.label=atom.family;
            Map<String,Object> params=CandidateAstR952.object(StrictCanonicalJson.readAtomObject(atom.canonicalSpec).get("params"));
            result.lag=integerOr(params,"lag",1);if(atom.family.equals("DELTA_NUMERIC")||atom.family.equals("DIRECTION"))result.lag++;result.window=atom.requiresSummary?atom.summaryWindow:Math.max(integerOr(params,"window",0),Math.max(integerOr(params,"recent",0),integerOr(params,"reference",0)));
            result.window=Math.max(result.window,atom.requiresSummary?atom.summaryWindow:atom.minimumHistory);
            result.sources.add("atom:"+atom.atomId+"|spec:"+atom.specHash+"|context:"+atom.context+"|source_issue<target_issue");
            switch(atom.family){
                case "LAG_DIGIT_EQ":result.unit="boolean";result.range="[0,1]";break;
                case "FREQUENCY":case "OMISSION_RATE":case "TRANSITION_P1":case "MARKOV_SCORE":case "CONFIDENCE_TEMPLATE":case "REGIME_SCORE":case "STRATEGY_DISAGREEMENT_TEMPLATE":case "LONG_FREQUENCY_R80":result.unit="dimensionless";result.range="[0,1]";break;
                case "ROLLING_VARIANCE_INDICATOR":result.unit="dimensionless_squared";result.range="[0,0.25]";break;
                case "DIRECTION":result.unit="ordinal_direction";result.range="[-1,1]";break;
                case "DELTA_NUMERIC":result.unit="digit_difference";result.range="[-9,9]";break;
                case "OMISSION":case "GAP_LAST_GAP":case "GAP_MEAN_GAP":case "LOSS_STREAK_TEMPLATE":result.unit="issues";result.range="[0,"+Math.max(5000,result.window)+"]";break;
                case "GAP_VAR_GAP":result.unit="issues_squared";break;
                case "ENTROPY":case "CONDITIONAL_ENTROPY":case "MUTUAL_INFORMATION":case "LONG_ENTROPY_R80":result.unit="nats";result.range="[0,ln(10)]";break;
                case "ROLLING_MEAN_DIGIT":result.unit="ordinal_digit";result.range="[0,9]";break;
                case "ROLLING_VARIANCE_DIGIT":result.unit="ordinal_digit_squared";result.range="[0,20.25]";break;
                case "RESIDUAL_MEAN_TEMPLATE":result.unit="hit_minus_probability";result.range="[-1,1]";break;
                case "FREQUENCY_SLOPE":result.unit="frequency_per_issue";break;
                case "PERIODICITY":result.unit="dimensionless_periodic_correlation";break;
                case "HOT_COLD_Z":case "GAP_GAP_Z":result.unit="standard_deviations";break;
                case "DRIFT_JS":result.unit="nats";result.range="[0,ln(2)]";break;
                case "DRIFT_PSI":result.unit="dimensionless";result.range="[0,+infinity),finite_only";break;
                default:result.unit="UNKNOWN_ATOM_UNIT:"+atom.family;
            }
        }else if(op.equals("CONSTANT")){result.unit="dimensionless";result.range="FINITE_DECLARED_HEX_CONSTANTS";result.sources.add("constant:"+String.valueOf(node.get("parameters")));}
        else if(op.equals("NUMERIC_TO_DOUBLE")&&!inputs.isEmpty()){result.unit=inputs.get(0).unit;result.range=inputs.get(0).range;}
        else if(op.equals("NORMALIZE_POSITIVE")||op.equals("SOFTMAX")){result.unit="probability";result.range="[0,1]";}
        else if(op.equals("SIGMOID")){result.unit="probability";result.range="[0,1]";}
        else{
            if(!inputs.isEmpty()){String unit=inputs.get(0).unit;boolean same=true;for(Info input:inputs)if(!unit.equals(input.unit))same=false;
                if(same&&(op.equals("ADD")||op.equals("SUB")||op.equals("MAX")||op.equals("MIN")||op.equals("MEAN")||op.equals("EMA")))result.unit=unit;
                else result.unit="COMPOSITE("+op+"):SOURCE_UNITS_IN_ANCESTOR_SPECS";
            }
            if(op.startsWith("MATRIX"))result.sources.add("matrix_binding:"+id+"|manifest_spec_reference");
        }
        visiting.remove(id);memo.put(id,result);return result;
    }
    private static void augmentMatrices(Info info,Map<String,Object> root){
        Map<String,Object> manifest=CandidateAstR952.object(root.get("featureManifest"));
        for(Object raw:CandidateAstR952.array(manifest.get("matrixSpecRefs"))){
            MatrixBindingsCP12.Ref ref=new MatrixBindingsCP12.Ref(new FeatureManifestR952.MatrixRef(CandidateAstR952.object(raw)));
            if(info.nodes.contains(ref.nodeId)){info.sources.add("matrix:"+StrictCanonicalJson.write(raw));info.window=Math.max(info.window,MatrixHistoricalDrawCP12.minimumHistoryCP12(ref));}
        }
    }
    private static int integerOr(Map<String,Object> map,String key,int fallback){Object value=map.get(key);return value instanceof Number?((Number)value).intValue():fallback;}
    private static String text(Map<String,Object> map,String key){String value=CandidateAstR952.str(map,key);CandidateAstR952.need(!value.isEmpty()&&value.length()<=16384,"FEATURE_METADATA_TEXT");return value;}
}
