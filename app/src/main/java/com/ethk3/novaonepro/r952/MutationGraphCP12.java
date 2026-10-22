package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.*;
import com.ethk3.novaonepro.r952.CandidateCompilerR952.*;
import java.util.*;

/** Pure deterministic bounded graph search. It owns neither DB writes nor generation counters. */
public final class MutationGraphCP12 {
    private MutationGraphCP12() {}
    public static final class Wait extends RuntimeException {public final String reason;public Wait(String r){super(r);reason=r;}}
    public static final class Proposal {
        public final CandidateAstR952 ast;public final int attempt;public final String reason;
        Proposal(CandidateAstR952 a,int n,String r){ast=a;attempt=n;reason=r;}
    }
    public static Proposal propose(CandidateAstR952 parent,CandidateAstR952 secondary,String actionId,
            String operator,List<AtomSpecCP4> registry,Set<String> occupied,
            List<ExpertRef> frozenParents,CandidateAstR952.Family requestedFamily,long deadlineNs) {return propose(parent,secondary,actionId,operator,registry,occupied,frozenParents,requestedFamily,deadlineNs,Collections.emptyMap());}
    public static Proposal propose(CandidateAstR952 parent,CandidateAstR952 secondary,String actionId,
            String operator,List<AtomSpecCP4> registry,Set<String> occupied,List<ExpertRef> frozenParents,
            CandidateAstR952.Family requestedFamily,long deadlineNs,Map<String,Double> frozenTrainStd) {return propose(parent,secondary,actionId,operator,registry,occupied,frozenParents,requestedFamily,deadlineNs,frozenTrainStd,Collections.emptyList());}
    public static Proposal propose(CandidateAstR952 parent,CandidateAstR952 secondary,String actionId,String operator,List<AtomSpecCP4> registry,Set<String> occupied,List<ExpertRef> frozenParents,CandidateAstR952.Family requestedFamily,long deadlineNs,Map<String,Double> frozenTrainStd,List<String> preferredSubtreeNodes) {
        if(parent==null||actionId==null||operator==null||registry==null||occupied==null||preferredSubtreeNodes==null||preferredSubtreeNodes.size()>32)throw new IllegalArgumentException("MUTATION_INPUT");
        List<AtomSpecCP4> ordered=new ArrayList<>(registry);Collections.sort(ordered,(a,b)->a.atomId.compareTo(b.atomId));
        CandidateAstR952 modern=parent.formatVersion==3?parent:legacyGraph(parent,ordered);
        String last="NO_LEGAL_CHANGED_GRAPH";
        for(int attempt=0;attempt<32;attempt++) {
            ResearchWorkBudgetR927.checkpoint();if(System.nanoTime()>=deadlineNs)throw new Wait("TIME_BUDGET_EXCEEDED");
            long seed=new java.math.BigInteger(V51ChecksumEngine.sha256(actionId+'|'+attempt).substring(0,16),16).longValue();Random random=new Random(seed);
            try {
                Map<String,Object> root=deep(modern.graphSpec.canonicalObject());
                switch(operator) {
                    case "WINDOW_MUTATION":case "LAG_MUTATION":case "FEATURE_REPLACE":case "ATOM_REPLACE":replaceAtom(root,ordered,operator,random);break;
                    case "WEIGHT_MUTATION":if(!weight(root,random))throw new IllegalArgumentException("NO_WEIGHTED_NODE_FOR_WEIGHT_MUTATION");break;
                    case "THRESHOLD_MUTATION":threshold(root,random,frozenTrainStd);break;
                    case "MODEL_STRUCTURE_MUTATION":reduceCost(root,random);break;
                    case "REGIME_GATE_MUTATION":regimeGate(root,random);break;
                    case "CROSSOVER_SAME_TYPE":crossover(root,secondary,random);break;
                    case "FIXED_TWO_EXPERT_MIX":fusion(root,frozenParents,random);break;
                    case "NEW_FAMILY":newFamily(root,modern,requestedFamily,ordered,frozenParents,random);break;
                    case "FEATURE_ADD":addFeature(root,random);break;
                    case "FEATURE_DROP":dropFeature(root,random);break;
                    case "OPERATOR_MUTATION":operator(root,random);break;
                    case "SUBTREE_MUTATION":subtree(root,random,preferredSubtreeNodes);break;
                    case "HYPERPARAM_MUTATION":hyperparameter(root,ordered,random);break;
                    case "DIVERSITY_INJECTION":diversity(root,ordered,random,preferredSubtreeNodes);break;
                    default:throw new IllegalArgumentException("UNREGISTERED_OPERATOR:"+operator);
                }
                rebuildManifest(root,ordered);CandidateAstR952 child=CandidateAstR952.parse("AST3:"+StrictCanonicalJson.write(root));
                if(child.structuralHash.equals(parent.structuralHash)||semanticHash(child).equals(semanticHash(modern))||occupied.contains(child.structuralHash)){last="NO_OP_EXISTING_STRUCTURE";continue;}
                CandidateCompilerR952.compile(child,child.graphSpec.manifest,usedSpecs(child,ordered));
                return new Proposal(child,attempt,"CHANGED_COMPILED_GRAPH");
            }catch(IllegalArgumentException|IllegalStateException invalid){last=String.valueOf(invalid.getMessage());}
        }
        return new Proposal(null,32,"NO_OP_32_ATTEMPTS:"+last);
    }
    public static String semanticHash(CandidateAstR952 ast){Map<String,Object> r=ast.graphSpec.canonicalObject();Map<String,String> hashes=new HashMap<>();for(Object o:nodes(r)){Map<String,Object> n=map(o);Map<String,Object> value=new TreeMap<>(n);value.remove("id");List<String> dependencies=new ArrayList<>();for(Object parent:list(n.get("parents")))dependencies.add(hashes.get((String)parent));value.put("parents",dependencies);hashes.put((String)n.get("id"),V51ChecksumEngine.sha256(StrictCanonicalJson.write(value)));}Map<String,Object> kernel=new TreeMap<>();kernel.put("modelKind",r.get("modelKind"));kernel.put("position",r.get("position"));kernel.put("experts",r.get("experts"));kernel.put("normalizerPolicy",r.get("normalizerPolicy"));kernel.put("inputDimension",r.get("inputDimension"));List<String> features=new ArrayList<>();for(Object id:list(r.get("featureOutputIds")))features.add(hashes.get((String)id));kernel.put("features",features);kernel.put("output",String.valueOf(r.get("outputId")).equals("@MODEL")?"@MODEL":hashes.get((String)r.get("outputId")));Map<String,Object> manifest=map(r.get("featureManifest"));kernel.put("bindingHashes",manifest.get("bindingHashes"));kernel.put("matrixSpecRefs",manifest.get("matrixSpecRefs"));kernel.put("missingPolicies",manifest.get("missingPolicyPerFeature"));if(!ast.graphSpec.manifest.summarySpecRefs.isEmpty()){List<String> sources=new ArrayList<>();for(LongSummaryBindingsR80.Ref ref:ast.graphSpec.manifest.summarySpecRefs){Map<String,Object> identity=new TreeMap<>(ref.canonicalObject());identity.remove("nodeId");identity.put("nodeKernelHash",hashes.get(ref.nodeId));sources.add(StrictCanonicalJson.write(identity));}Collections.sort(sources);kernel.put("summarySources",sources);}return V51ChecksumEngine.sha256(StrictCanonicalJson.write(kernel));}
    private static CandidateAstR952 legacyGraph(CandidateAstR952 parent,List<AtomSpecCP4> specs) {
        Map<String,Object> root=base(parent.position,CandidateAstR952.Family.FREQUENCY,ModelKind.FIXED_GRAPH);
        List<Object> nodes=nodes(root);List<String> inputs=new ArrayList<>();
        for(int d=0;d<10;d++) {
            AtomSpecCP4 match=null;for(AtomSpecCP4 s:specs)if(s.position==parent.position&&!s.requiresBinding&&s.family.equals(parent.operator==CandidateAstR952.Operator.MARKOV_TRANSITION?"MARKOV_SCORE":"FREQUENCY")&&s.mathSpec.digit==d&&s.mathSpec.w==parent.lookback){match=s;break;}
            if(match==null)throw new Wait("WAIT_REGISTERED_AST3_SEED");
            String id="n"+nodes.size();nodes.add(atom(id,match));inputs.add(id);
        }
        nodes.add(node("n10","PACK10","SCORE_VECTOR10",inputs,Collections.emptyList()));root.put("outputId","n10");rebuildManifest(root,specs);
        CandidateAstR952 a=CandidateAstR952.parse("AST3:"+StrictCanonicalJson.write(root));CandidateCompilerR952.compile(a,a.graphSpec.manifest,usedSpecs(a,specs));return a;
    }
    private static void replaceAtom(Map<String,Object> root,List<AtomSpecCP4> registry,String kind,Random rnd) {
        Set<String> consumed=consumedNodes(root),matrixBound=matrixBoundNodes(root);List<Map<String,Object>> atoms=new ArrayList<>();for(Object o:nodes(root))if("ATOM".equals(map(o).get("operator"))&&consumed.contains(map(o).get("id"))&&!matrixBound.contains(map(o).get("id")))atoms.add(map(o));
        if(atoms.isEmpty())throw new IllegalArgumentException("NO_REPLACEABLE_ATOM");Map<String,Object> n=atoms.get(rnd.nextInt(atoms.size()));AtomSpecCP4 old=spec(registry,(String)n.get("atomId"));
        List<AtomSpecCP4> choices=new ArrayList<>();for(AtomSpecCP4 s:registry) {
            if(s.atomId.equals(old.atomId)||s.position!=old.position||s.requiresBinding||old.requiresBinding||!s.outputType.equals(old.outputType)||!usable(s))continue;
            if(kind.equals("WINDOW_MUTATION")||kind.equals("LAG_MUTATION")) {
                if(!s.family.equals(old.family)||!sameExcept(old,s,kind.equals("WINDOW_MUTATION")?"window":"lag"))continue;
            }
            choices.add(s);
        }
        if(choices.isEmpty())throw new IllegalArgumentException("NO_REGISTERED_TYPED_ATOM_REPLACEMENT");
        Collections.sort(choices,(a,b)->{int x=kind.equals("LAG_MUTATION")?Math.abs(a.mathSpec.lag-old.mathSpec.lag):Math.abs(a.mathSpec.w-old.mathSpec.w),y=kind.equals("LAG_MUTATION")?Math.abs(b.mathSpec.lag-old.mathSpec.lag):Math.abs(b.mathSpec.w-old.mathSpec.w);int d=Integer.compare(x,y);return d!=0?d:a.atomId.compareTo(b.atomId);});
        int nearest=(kind.equals("WINDOW_MUTATION")||kind.equals("LAG_MUTATION"))?Math.min(2,choices.size()):choices.size();n.put("atomId",choices.get(rnd.nextInt(nearest)).atomId);
    }
    /** A true shrinkage hyperparameter changes only registered Dirichlet alpha(.5<->1). */
    private static void hyperparameter(Map<String,Object> root,List<AtomSpecCP4> registry,Random random){
        List<Map<String,Object>> eligible=new ArrayList<>();Map<String,List<AtomSpecCP4>> choices=new TreeMap<>();Set<String> consumed=consumedNodes(root),matrixBound=matrixBoundNodes(root);
        for(Map<String,Object> node:castNodes(root))if(consumed.contains(node.get("id"))&&!matrixBound.contains(node.get("id"))&&"ATOM".equals(node.get("operator"))){AtomSpecCP4 old=spec(registry,(String)node.get("atomId"));if(!old.family.equals("MARKOV_SCORE")||old.requiresBinding||!(old.mathSpec.alpha==.5||old.mathSpec.alpha==1))continue;List<AtomSpecCP4> alternatives=new ArrayList<>();for(AtomSpecCP4 next:registry)if(next.position==old.position&&next.family.equals(old.family)&&!next.requiresBinding&&usable(next)&&next.outputType.equals(old.outputType)&&next.mathSpec.alpha==(old.mathSpec.alpha==.5?1:.5)&&sameExcept(old,next,"alpha"))alternatives.add(next);if(!alternatives.isEmpty()){eligible.add(node);choices.put((String)node.get("id"),alternatives);}}
        if(eligible.isEmpty())throw new IllegalArgumentException("NO_REGISTERED_DIRICHLET_ALPHA_PAIR");Map<String,Object> selected=eligible.get(random.nextInt(eligible.size()));List<AtomSpecCP4> alternatives=choices.get((String)selected.get("id"));selected.put("atomId",alternatives.get(random.nextInt(alternatives.size())).atomId);
    }
    /** Evidence-ranked consumed subtree receives a genuinely different-family typed Atom. */
    private static void diversity(Map<String,Object> root,List<AtomSpecCP4> registry,Random random,List<String> preferred){
        Set<String> consumed=consumedNodes(root),matrixBound=matrixBoundNodes(root);List<Map<String,Object>> eligible=new ArrayList<>();Map<String,List<AtomSpecCP4>> choices=new TreeMap<>();Map<String,Integer> rank=new HashMap<>();
        for(int i=0;i<preferred.size();i++)if(consumed.contains(preferred.get(i))){Set<String> subtree=new HashSet<>();collect(root,preferred.get(i),subtree);for(String id:subtree)if(!rank.containsKey(id))rank.put(id,i);}
        for(Map<String,Object> node:castNodes(root))if(consumed.contains(node.get("id"))&&!matrixBound.contains(node.get("id"))&&"ATOM".equals(node.get("operator"))){AtomSpecCP4 old=spec(registry,(String)node.get("atomId"));if(old.requiresBinding)continue;List<AtomSpecCP4> alternatives=new ArrayList<>();for(AtomSpecCP4 next:registry)if(next.position==old.position&&!next.family.equals(old.family)&&!next.requiresBinding&&usable(next)&&atomGraphType(next.outputType).equals(node.get("outputType")))alternatives.add(next);if(!alternatives.isEmpty()){eligible.add(node);choices.put((String)node.get("id"),alternatives);}}
        if(eligible.isEmpty())throw new IllegalArgumentException("NO_TYPED_DIFFERENT_FAMILY_ATOM");
        Collections.sort(eligible,(a,b)->{int order=Integer.compare(rank.getOrDefault((String)a.get("id"),Integer.MAX_VALUE),rank.getOrDefault((String)b.get("id"),Integer.MAX_VALUE));return order==0?Integer.compare(Integer.parseInt(((String)a.get("id")).substring(1)),Integer.parseInt(((String)b.get("id")).substring(1))):order;});
        Map<String,Object> selected=rank.containsKey((String)eligible.get(0).get("id"))?eligible.get(0):eligible.get(random.nextInt(eligible.size()));List<AtomSpecCP4> alternatives=choices.get((String)selected.get("id"));selected.put("atomId",alternatives.get(random.nextInt(alternatives.size())).atomId);
    }
    private static String atomGraphType(String output){return output.equals("BOOLEAN")?"BOOLEAN":output.equals("PROBABILITY")?"PROBABILITY":"DOUBLE";}
    private static Set<String> matrixBoundNodes(Map<String,Object> root){Set<String> out=new HashSet<>();for(Object raw:list(map(root.get("featureManifest")).get("matrixSpecRefs")))out.add(new MatrixBindingsCP12.Ref(new FeatureManifestR952.MatrixRef(map(raw))).nodeId);return out;}
    private static Set<String> consumedNodes(Map<String,Object> root){Set<String> out=new HashSet<>();for(Object feature:list(root.get("featureOutputIds")))collect(root,(String)feature,out);if(!"@MODEL".equals(root.get("outputId")))collect(root,(String)root.get("outputId"),out);return out;}
    private static boolean sameExcept(AtomSpecCP4 a,AtomSpecCP4 b,String key) {
        Map<String,Object> x=StrictCanonicalJson.readAtomObject(a.canonicalSpec),y=StrictCanonicalJson.readAtomObject(b.canonicalSpec);
        Map<String,Object> xp=new TreeMap<>(map(x.get("params"))),yp=new TreeMap<>(map(y.get("params")));if(!xp.containsKey(key)||!yp.containsKey(key)||xp.get(key).equals(yp.get(key)))return false;xp.remove(key);yp.remove(key);return StrictCanonicalJson.writeAtom(xp).equals(StrictCanonicalJson.writeAtom(yp));
    }
    private static boolean weight(Map<String,Object> root,Random rnd) {
        List<Map<String,Object>> list=new ArrayList<>();for(Object o:nodes(root))if(String.valueOf(map(o).get("operator")).startsWith("WEIGHTED"))list.add(map(o));if(list.isEmpty())return false;
        Map<String,Object> n=list.get(rnd.nextInt(list.size()));List<Object> p=list(n.get("parameters"));int i=rnd.nextInt(p.size()),j=(i+1+rnd.nextInt(p.size()-1))%p.size();double[] w=new double[p.size()];for(int k=0;k<w.length;k++)w[k]=Double.parseDouble((String)p.get(k));
        double delta=rnd.nextBoolean()?1.0/16:-1.0/16;w[i]=Math.max(0,Math.min(1,w[i]+delta));w[j]=Math.max(0,Math.min(1,w[j]-delta));double sum=0;for(double v:w)sum+=v;if(sum<=0)throw new IllegalArgumentException("WEIGHT_ZERO");List<String> next=new ArrayList<>();for(double v:w)next.add(Double.toHexString(v/sum));n.put("parameters",next);return true;
    }
    private static void threshold(Map<String,Object> root,Random rnd,Map<String,Double> frozenTrainStd) {
        List<Map<String,Object>> ns=castNodes(root);List<Map<String,Object>> choices=new ArrayList<>();for(Map<String,Object> n:ns)if(String.valueOf(n.get("operator")).startsWith("THRESHOLD"))choices.add(n);
        if(choices.isEmpty())throw new IllegalArgumentException("NO_THRESHOLD_NODE");Map<String,Object> n=choices.get(rnd.nextInt(choices.size()));String ref=(String)list(n.get("parents")).get(1);Map<String,Object> constant=ns.get(Integer.parseInt(ref.substring(1)));
        if(!"CONSTANT".equals(constant.get("operator"))||list(constant.get("parameters")).size()!=1)throw new IllegalArgumentException("THRESHOLD_REQUIRES_FROZEN_CONSTANT");
        String input=(String)list(n.get("parents")).get(0);Double std=frozenTrainStd.get(input);if(std==null||!Double.isFinite(std)||std<=0)throw new IllegalArgumentException("NO_FROZEN_TRAIN_STD_FOR_THRESHOLD");double v=Double.parseDouble((String)list(constant.get("parameters")).get(0));double next=Math.rint(Math.max(.05,Math.min(.95,v+(rnd.nextBoolean()?.05:-.05)*std))*1024)/1024;constant.put("parameters",Collections.singletonList(Double.toHexString(next)));
    }
    private static void subtree(Map<String,Object> root,Random rnd){subtree(root,rnd,Collections.emptyList());}
    private static void subtree(Map<String,Object> root,Random rnd,List<String> preferred) {
        String kind=(String)root.get("modelKind");if(kind.equals("EXPERT_HEDGE"))throw new IllegalArgumentException("HEDGE_REQUIRES_EXPERT_MUTATION");
        List<Object> ns=nodes(root);List<Object> features=list(root.get("featureOutputIds"));String chosen=kind.equals("FIXED_GRAPH")?(String)root.get("outputId"):(String)features.get(rnd.nextInt(features.size()));for(String measured:preferred)if(kind.equals("FIXED_GRAPH")?measured.equals(root.get("outputId")):features.contains(measured)){chosen=measured;break;}Map<String,Object> old=map(ns.get(Integer.parseInt(chosen.substring(1))));
        String type=(String)old.get("outputType");if(!Arrays.asList("DOUBLE","SCORE_VECTOR10").contains(type))throw new IllegalArgumentException("SUBTREE_TYPE");
        String id="n"+ns.size();ns.add(node(id,type.equals("SCORE_VECTOR10")?"CLIP":"LOG1P_ABS",type,Collections.singletonList(chosen),type.equals("SCORE_VECTOR10")?Arrays.asList(Double.toHexString(0),Double.toHexString(.25+rnd.nextInt(8)/10.0)):Collections.emptyList()));
        if(kind.equals("FIXED_GRAPH"))root.put("outputId",id);else{int at=features.indexOf(chosen);features.set(at,id);}
    }
    private static void operator(Map<String,Object> root,Random rnd) {List<Map<String,Object>> options=new ArrayList<>();for(Map<String,Object> n:castNodes(root))if("ADD".equals(n.get("operator"))||"SUBTRACT".equals(n.get("operator")))options.add(n);if(options.isEmpty())throw new IllegalArgumentException("NO_ALTERNATE_OPERATOR");Map<String,Object> n=options.get(rnd.nextInt(options.size()));n.put("operator","ADD".equals(n.get("operator"))?"SUBTRACT":"ADD");}
    private static void dropFeature(Map<String,Object> root,Random rnd) {List<Object> f=list(root.get("featureOutputIds"));if(f.size()<2)throw new IllegalArgumentException("REQUIRED_FEATURE_CANNOT_DROP");f.remove(rnd.nextInt(f.size()));root.put("inputDimension",2*f.size());}
    private static void addFeature(Map<String,Object> root,Random rnd) {List<Object> f=list(root.get("featureOutputIds"));if(f.isEmpty()||f.size()>=32)throw new IllegalArgumentException("FEATURE_CAP");List<String> options=new ArrayList<>();for(Map<String,Object> n:castNodes(root))if(!f.contains(n.get("id"))&&Arrays.asList("DOUBLE","INTEGER","BOOLEAN","PROBABILITY","REGIME").contains(n.get("outputType")))options.add((String)n.get("id"));if(options.isEmpty())throw new IllegalArgumentException("NO_UNUSED_TYPED_FEATURE_FOR_FEATURE_ADD");f.add(options.get(rnd.nextInt(options.size())));root.put("inputDimension",2*f.size());}
    private static void reduceCost(Map<String,Object> root,Random rnd) {String kind=(String)root.get("modelKind");if(kind.equals("HISTOGRAM_TREE")||kind.equals("MLP_D32_16_10")){root.put("modelKind","SOFTMAX_LINEAR");root.put("family","LINEAR");}else dropFeature(root,rnd);}
    private static void regimeGate(Map<String,Object> root,Random rnd) {List<Map<String,Object>> regimes=new ArrayList<>();for(Map<String,Object> n:castNodes(root))if("REGIME".equals(n.get("outputType")))regimes.add(n);if(regimes.isEmpty())throw new IllegalArgumentException("NO_FROZEN_REGIME_NODE");List<Object> f=list(root.get("featureOutputIds"));String prior=(String)root.get("outputId");if(prior.equals("@MODEL")){if(f.isEmpty())throw new IllegalArgumentException("REGIME_GATE_HEDGE_UNSUPPORTED");prior=(String)f.get(rnd.nextInt(f.size()));}String type=(String)map(nodes(root).get(Integer.parseInt(prior.substring(1)))).get("outputType"),id="n"+nodes(root).size();nodes(root).add(node(id,"REGIME_GATE",type,Arrays.asList((String)regimes.get(rnd.nextInt(regimes.size())).get("id"),prior),Collections.singletonList(Double.toHexString(rnd.nextInt(5)))));if(!f.isEmpty())f.set(f.indexOf(prior),id);else root.put("outputId",id);}
    private static void crossover(Map<String,Object> root,CandidateAstR952 peer,Random rnd) {if(peer==null||peer.formatVersion!=3||peer.position!=((Number)root.get("position")).intValue())throw new IllegalArgumentException("CROSSOVER_PARENT");List<Object> f=list(root.get("featureOutputIds"));if(f.isEmpty())throw new IllegalArgumentException("CROSSOVER_SCALAR_ONLY");String destination=(String)f.get(rnd.nextInt(f.size()));Map<String,Object> dst=map(nodes(root).get(Integer.parseInt(destination.substring(1))));List<String> choices=new ArrayList<>();for(String id:peer.graphSpec.featureOutputIds){Map<String,Object> n=map(list(peer.graphSpec.canonicalObject().get("nodes")).get(Integer.parseInt(id.substring(1))));if(dst.get("outputType").equals(n.get("outputType"))&&dst.get("unit").equals(n.get("unit")))choices.add(id);}if(choices.isEmpty())throw new IllegalArgumentException("CROSSOVER_NO_SAME_TYPE");String selected=choices.get(rnd.nextInt(choices.size()));Map<String,String> mapping=appendReachable(root,peer.graphSpec.canonicalObject(),selected);f.set(f.indexOf(destination),mapping.get(selected));}
    private static Map<String,String> appendReachable(Map<String,Object> destination,Map<String,Object> source,String selected) {Set<String> required=new HashSet<>();collect(source,selected,required);Map<String,String> remap=new HashMap<>();for(Object o:nodes(source)){Map<String,Object> n=map(o);String old=(String)n.get("id");if(!required.contains(old))continue;Map<String,Object> c=deep(n);String id="n"+nodes(destination).size();remap.put(old,id);c.put("id",id);List<String> p=new ArrayList<>();for(Object x:list(n.get("parents")))p.add(remap.get((String)x));c.put("parents",p);nodes(destination).add(c);}return remap;}
    private static void collect(Map<String,Object> root,String id,Set<String> out){if(!out.add(id))return;for(Object p:list(map(nodes(root).get(Integer.parseInt(id.substring(1)))).get("parents")))collect(root,(String)p,out);}
    private static void fusion(Map<String,Object> root,List<ExpertRef> refs,Random rnd) {if(refs==null||refs.size()!=2||refs.get(0).freezeHash.equals(refs.get(1).freezeHash))throw new IllegalArgumentException("FUSION_FROZEN_PARENTS");root.put("family","ENSEMBLE");root.put("modelKind","EXPERT_HEDGE");root.put("nodes",new ArrayList<>(Collections.singletonList(node("n0","CONSTANT","DOUBLE",Collections.emptyList(),Collections.singletonList(Double.toHexString(0))))));Map<String,Object> clean=map(root.get("featureManifest"));clean.put("bindingHashes",new ArrayList<>());clean.put("matrixSpecRefs",new ArrayList<>());root.put("featureOutputIds",new ArrayList<>());root.put("outputId","@MODEL");root.put("inputDimension",20);List<Object> experts=new ArrayList<>();for(ExpertRef e:refs)experts.add(fields("freezeHash",e.freezeHash,"modelHash",e.modelHash,"featureManifestHash",e.featureManifestHash,"position",e.position));root.put("experts",experts);}
    /** All15 family definitions are owned by the same pure builder used by Creator. */
    private static void newFamily(Map<String,Object> root,CandidateAstR952 parent,CandidateAstR952.Family family,List<AtomSpecCP4> registry,List<ExpertRef> refs,Random rnd) {
        if(family==null)throw new IllegalArgumentException("NEW_FAMILY_UNSET");
        try{CandidateAstR952 fresh=ModernFamilyFactoryCP12.newFamilyFromParentCP12(parent,family,registry,refs,rnd.nextInt(40));root.clear();root.putAll(deep(fresh.graphSpec.canonicalObject()));}
        catch(DevelopmentProductionCP12.Wait dependency){throw new Wait(dependency.reason);}
    }

    private static Map<String,Object> base(int position,CandidateAstR952.Family family,ModelKind kind){return fields("compilerRevision","AST3_CP4_1","experts",new ArrayList<>(),"family",family.name(),"featureOutputIds",new ArrayList<>(),"game",ReleaseManifestR926.GAME_CODE,"inputDimension",10,"modelKind",kind.name(),"nodes",new ArrayList<>(),"normalizerPolicy","TRAIN_ONLY_ZSCORE_V1","outputId","@MODEL","position",position,"version",3,"featureManifest",fields("revision","CP4_FEATURE_1","graphHash",V51ChecksumEngine.sha256("pending"),"orderedFeatureIds",new ArrayList<>(),"featureTypes",new ArrayList<>(),"requiredHistory",0,"atomSpecHashes",new ArrayList<>(),"bindingHashes",new ArrayList<>(),"matrixSpecRefs",new ArrayList<>(),"normalizerPolicy","TRAIN_ONLY_ZSCORE_V1","missingPolicyPerFeature",new ArrayList<>(),"inputDimension",10,"estimatedStateBytes",8*1024*1024));}
    public static void rebuildManifest(Map<String,Object> root,List<AtomSpecCP4> registry){Map<String,Object> m=map(root.get("featureManifest"));List<Object> f=list(root.get("featureOutputIds"));List<String> types=new ArrayList<>(),policies=new ArrayList<>();for(Object id:f){types.add((String)map(nodes(root).get(Integer.parseInt(((String)id).substring(1)))).get("outputType"));policies.add("MASK");}TreeSet<String> hashes=new TreeSet<>();int history=0;for(Object o:nodes(root)){Map<String,Object> n=map(o);if("ATOM".equals(n.get("operator"))){AtomSpecCP4 s=spec(registry,(String)n.get("atomId"));hashes.add(s.specHash);history=Math.max(history,s.minimumHistory);}}m.put("orderedFeatureIds",new ArrayList<>(f));m.put("featureTypes",types);m.put("missingPolicyPerFeature",policies);m.put("atomSpecHashes",new ArrayList<>(hashes));history=Math.max(history,MatrixHistoricalDrawCP12.requiredHistoryCP12(m.get("matrixSpecRefs")));m.put("requiredHistory",history);m.put("inputDimension",root.get("inputDimension"));m.put("estimatedStateBytes",8*1024*1024);m.put("graphHash",CandidateAstR952.hashGraphKernelWithoutManifest(root));LongSummaryBindingsR80.decorateManifest(root,registry);CandidateAstR952 temporary=CandidateAstR952.parse("AST3:"+StrictCanonicalJson.write(root));long bytes=CandidateCompilerR952.estimateStateBytes(temporary,usedSpecs(temporary,registry));if(bytes>8L*1024*1024)throw new IllegalArgumentException("MUTATION_STATE_CAPACITY");m.put("estimatedStateBytes",Math.max(4096,bytes));}
    public static List<AtomSpecCP4> usedSpecs(CandidateAstR952 a,List<AtomSpecCP4> registry){TreeMap<String,AtomSpecCP4> out=new TreeMap<>();for(com.ethk3.novaonepro.TypedAtomMoleculeGraphR930.NodeSpec n:a.graphSpec.nodes)if(n.kind==com.ethk3.novaonepro.TypedAtomMoleculeGraphR930.NodeKind.ATOM)out.put(n.source,spec(registry,n.source));return new ArrayList<>(out.values());}
    private static AtomSpecCP4 spec(List<AtomSpecCP4> list,String id){for(AtomSpecCP4 s:list)if(s.atomId.equals(id))return s;throw new IllegalArgumentException("MISSING_ATOM_SPEC:"+id);}
    // DISCOVERED is a registered definition, not a quality verdict. A research
    // proposal must be allowed to test it before binding-specific OOS admission.
    // This never changes the atom state or grants any production authority.
    private static boolean usable(AtomSpecCP4 s){return !s.state.equals("RETIRED")&&!s.state.equals("REQUIRES_BINDING")&&!s.requiresBinding;}
    private static Map<String,Object> atom(String id,AtomSpecCP4 s){Map<String,Object> n=node(id,"ATOM",s.outputType.equals("BOOLEAN")?"BOOLEAN":s.outputType.equals("PROBABILITY")?"PROBABILITY":"DOUBLE",Collections.emptyList(),Collections.emptyList());n.put("atomId",s.atomId);return n;}
    private static Map<String,Object> node(String id,String op,String type,List<String> parents,List<String> parameters){return fields("id",id,"operator",op,"atomId","","outputType",type,"stage","FROZEN_PREDRAW","unit","dimensionless","parameters",new ArrayList<>(parameters),"parents",new ArrayList<>(parents));}
    @SuppressWarnings("unchecked") private static Map<String,Object> map(Object o){return (Map<String,Object>)o;}
    @SuppressWarnings("unchecked") private static List<Object> list(Object o){return (List<Object>)o;}
    private static List<Object> nodes(Map<String,Object> root){return list(root.get("nodes"));}
    private static List<Map<String,Object>> castNodes(Map<String,Object> root){List<Map<String,Object>> out=new ArrayList<>();for(Object o:nodes(root))out.add(map(o));return out;}
    private static Map<String,Object> deep(Map<String,Object> root){return map(mutable(StrictCanonicalJson.readObject(StrictCanonicalJson.write(root))));}
    private static Object mutable(Object value){if(value instanceof Map){Map<String,Object> out=new TreeMap<>();for(Map.Entry<String,Object> e:map(value).entrySet())out.put(e.getKey(),mutable(e.getValue()));return out;}if(value instanceof List){List<Object> out=new ArrayList<>();for(Object o:list(value))out.add(mutable(o));return out;}return value;}
    private static Map<String,Object> fields(Object... values){Map<String,Object> m=new TreeMap<>();for(int i=0;i<values.length;i+=2)m.put((String)values[i],values[i+1]);return m;}
}
