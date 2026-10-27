package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Executable structural edits with distinct accounting from parameter variants.
 * A changed structure is only a candidate, never an assertion of improvement. */
public final class StructuralProposalR26 {
    private StructuralProposalR26() {}
    public static final class Proposal {
        public final ModelGenomeSU2 genome;public final String parent,operation,category;
        Proposal(ModelGenomeSU2 g,String p,String op,String category){genome=g;parent=p;operation=op;this.category=category;}
    }
    public static String structure(ModelGenomeSU2 g){
        List<Object> members=new ArrayList<>();for(ModelGenomeSU2.Member m:g.members){Map<String,Object> graph=new TreeMap<>();graph.put("kind",m.kind);for(String k:Arrays.asList("sources","lags","atoms","mols","K","text_hex","graph","mode","cross","lags","factors","transition","fragments","slots","retrieval"))if(m.params.containsKey(k))graph.put(k,m.params.get(k));members.add(graph);}
        return PolicyIdentityR26.sha256(PolicyIdentityR26.canonical(Arrays.asList(members,g.pool,g.sink)));
    }
    private static ModelGenomeSU2 copy(ModelGenomeSU2 p,List<ModelGenomeSU2.Member> members,String pool){return new ModelGenomeSU2(members,pool,p.sink,p.gate,p.coverage,p.metaC,p.calibrated);}
    public static List<Proposal> from(ModelGenomeSU2 parent,ModelGenomeSU2.Member complementary){
        if(WarmModelSeedR26.has(parent))return Collections.emptyList();
        List<Proposal> out=new ArrayList<>();ModelGenomeSU2 base=copy(parent,parent.members,parent.pool);
        out.add(new Proposal(base,parent.key(),"PRESERVE_PARENT_STRUCTURE",base.key().equals(parent.key())?"NO_OP":"IDENTITY_MIGRATION"));
        out.addAll(mechanismChildren(parent));
        for(ModelGenomeSU2.Member member:parent.members)if(member.kind.equals("R26F15")||member.kind.equals("R26F16"))return out;
        List<ModelGenomeSU2.Member> mem=new ArrayList<>(parent.members);boolean present=false;for(ModelGenomeSU2.Member m:mem)present|=m.modelKey().equals(complementary.modelKey());
        if(!present&&mem.size()<4){mem.add(complementary);add(out,parent,copy(parent,mem,"BMA"),"ADD_COMPLEMENTARY_MEMBER");}
        if(parent.members.size()>1){mem=new ArrayList<>(parent.members);mem.remove(mem.size()-1);add(out,parent,copy(parent,mem,parent.pool),"REMOVE_MEMBER");}
        if(parent.members.size()>1&&!parent.pool.equals("GATED"))add(out,parent,copy(parent,parent.members,"GATED"),"CONDITIONAL_ROUTER");
        return out;
    }
    /** These parameters alter executed edges, dilation or state transition topology, not just weights. */
    public static List<Proposal> neuralChildren(ModelGenomeSU2 parent){
        if(WarmModelSeedR26.has(parent))return Collections.emptyList();
        List<Proposal> out=new ArrayList<>();if(parent.members.size()!=1)return out;ModelGenomeSU2.Member member=parent.members.get(0);if(!member.kind.matches("R26F0[3-7]"))return out;
        int prior=member.params.containsKey("graph")?Integer.parseInt(member.params.get("graph")):0;
        for(int graph=1;graph<=2;graph++){if(graph==prior)continue;Map<String,String> p=new TreeMap<>(member.params);p.put("graph",Integer.toString(graph));ModelGenomeSU2 child=copy(parent,Collections.singletonList(new ModelGenomeSU2.Member(member.kind,p,member.weight)),parent.pool);String mechanism=member.kind.equals("R26F03")?"REWIRE_HYPEREDGE":member.kind.equals("R26F04")?"REWIRE_CAUSAL_DILATION":member.kind.equals("R26F05")?"SPARSIFY_PAST_ATTENTION":member.kind.equals("R26F06")?"ADD_CROSS_STATE_TRANSITION":"REWIRE_GRAPH_GRU_EDGES";add(out,parent,child,mechanism+":"+graph);}
        return out;
    }
    public static boolean control(ModelGenomeSU2 g){for(ModelGenomeSU2.Member m:g.members)if(m.kind.equals("R26F01")&&"false".equals(m.params.get("cross"))||m.kind.equals("R26F02")&&"false".equals(m.params.get("factors"))||m.kind.equals("R26F08")&&"false".equals(m.params.get("transition"))||m.kind.equals("R26F20")&&m.params.containsKey("fragments")&&!"MATCH".equals(m.params.get("retrieval")))return true;return false;}
    public static List<Proposal> mechanismChildren(ModelGenomeSU2 parent){
        if(WarmModelSeedR26.has(parent))return Collections.emptyList();
        if(parent.members.size()!=1)return Collections.emptyList();ModelGenomeSU2.Member m=parent.members.get(0);if(m.kind.equals("R26F08"))return hiddenStateChildren(parent,m);if(m.kind.equals("R26F20")&&m.params.containsKey("fragments"))return fragmentChildren(parent,m);if(!m.kind.equals("R26F01")&&!m.kind.equals("R26F02"))return neuralChildren(parent);
        List<Proposal> out=new ArrayList<>();for(boolean ablation:new boolean[]{false,true}){Map<String,String> params=new TreeMap<>(m.params);if(m.kind.equals("R26F01")){params.put("lags",ablation?params.getOrDefault("lags","3"):params.getOrDefault("lags","3").equals("3")?"5":"3");params.put("cross",ablation?"false":"true");}else{params.put("graph",ablation?params.getOrDefault("graph","0"):Integer.toString((Integer.parseInt(params.getOrDefault("graph","0"))+1)%3));params.put("factors",ablation?"false":"true");}
            ModelGenomeSU2 child=copy(parent,Collections.singletonList(new ModelGenomeSU2.Member(m.kind,params,m.weight)),parent.pool);String op=ablation?(m.kind.equals("R26F01")?"CUT_CROSS_POSITION_LAG_EDGES":"REMOVE_PAIRWISE_FACTORS"):(m.kind.equals("R26F01")?"ADD_OR_CHANGE_LAG_PATHS":"REWIRE_PAIRWISE_CHAIN");out.add(new Proposal(child,parent.key(),op,child.key().equals(parent.key())?"NO_OP":ablation?"MECHANISM_ABLATION":"STRUCTURAL_CANDIDATE"));}return out;
    }
    private static List<Proposal> fragmentChildren(ModelGenomeSU2 parent,ModelGenomeSU2.Member member){
        List<Proposal> out=new ArrayList<>();int slots=Integer.parseInt(member.params.get("slots"));
        for(String retrieval:new String[]{"MATCH","NONE","MISMATCH","EXPIRED"}){Map<String,String> params=new TreeMap<>(member.params);params.put("slots",Integer.toString(retrieval.equals("MATCH")?(slots<32?slots+8:slots-8):slots));params.put("retrieval",retrieval);
            ModelGenomeSU2 child=copy(parent,Collections.singletonList(new ModelGenomeSU2.Member(member.kind,params,member.weight)),parent.pool);
            out.add(new Proposal(child,parent.key(),retrieval.equals("MATCH")?"ADD_OR_REMOVE_STATE_FRAGMENT_SLOTS":retrieval.equals("NONE")?"REMOVE_STATE_FRAGMENT_MEMORY":retrieval.equals("MISMATCH")?"MISMATCH_STATE_FRAGMENT_RETRIEVAL":"EXPIRE_STATE_FRAGMENT_RETRIEVAL",retrieval.equals("MATCH")?"STRUCTURAL_CANDIDATE":"MECHANISM_ABLATION"));
        }return out;
    }
    private static List<Proposal> hiddenStateChildren(ModelGenomeSU2 parent,ModelGenomeSU2.Member member){
        List<Proposal> out=new ArrayList<>();int original=Integer.parseInt(member.params.getOrDefault("K",ModelEvolutionSU2.centralParams("HMM").get("K")));
        for(boolean cut:new boolean[]{false,true}){Map<String,String> params=new TreeMap<>(member.params);params.put("K",Integer.toString(cut?original:original<5?original+2:original-2));params.put("transition",cut?"false":"true");
            ModelGenomeSU2 child=copy(parent,Collections.singletonList(new ModelGenomeSU2.Member(member.kind,params,member.weight)),parent.pool);
            out.add(new Proposal(child,parent.key(),cut?"REMOVE_HIDDEN_TRANSITION_MEMORY":"ADD_OR_REMOVE_LATENT_STATE_NODES",child.key().equals(parent.key())?"NO_OP":cut?"MECHANISM_ABLATION":"STRUCTURAL_CANDIDATE"));
        }return out;
    }
    private static void add(List<Proposal> out,ModelGenomeSU2 p,ModelGenomeSU2 g,String op){out.add(new Proposal(g,p.key(),op,g.key().equals(p.key())?"NO_OP":structure(g).equals(structure(p))?"PARAMETER_VARIANT":"STRUCTURAL_CANDIDATE"));}
}
